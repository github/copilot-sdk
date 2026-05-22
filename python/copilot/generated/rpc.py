"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""
from __future__ import annotations

from typing import TYPE_CHECKING, ClassVar

from .session_events import (
    AbortReason,
    EmbeddedBlobResourceContents,
    EmbeddedTextResourceContents,
    McpServerSource,
    McpServerStatus,
    PermissionPromptRequest,
    PermissionRule,
    ReasoningSummary,
    SessionEvent,
    SessionMode,
    ShutdownType,
    SkillSource,
    UserToolSessionApproval,
)

if TYPE_CHECKING:
    from .._jsonrpc import JsonRpcClient

from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Protocol, TypeVar, cast
from uuid import UUID

import dateutil.parser

T = TypeVar("T")
EnumT = TypeVar("EnumT", bound=Enum)


def from_str(x: Any) -> str:
    assert isinstance(x, str)
    return x

def from_none(x: Any) -> Any:
    assert x is None
    return x

def from_union(fs, x):
    for f in fs:
        try:
            return f(x)
        except Exception:
            pass
    assert False

def to_class(c: type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()

def from_bool(x: Any) -> bool:
    assert isinstance(x, bool)
    return x

def from_int(x: Any) -> int:
    assert isinstance(x, int) and not isinstance(x, bool)
    return x

def from_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)

def to_float(x: Any) -> float:
    assert isinstance(x, (int, float))
    return x

def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:
    assert isinstance(x, dict)
    return { k: f(v) for (k, v) in x.items() }

def from_list(f: Callable[[Any], T], x: Any) -> list[T]:
    assert isinstance(x, list)
    return [f(y) for y in x]

def to_enum(c: type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, c)
    return x.value

def from_datetime(x: Any) -> datetime:
    return dateutil.parser.parse(x)

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AbortRequest:
    """Parameters for aborting the current turn"""

    reason: AbortReason | None = None
    """Finite reason code describing why the current turn was aborted"""

    @staticmethod
    def from_dict(obj: Any) -> AbortRequest:
        assert isinstance(obj, dict)
        reason = from_union([AbortReason, from_none], obj.get("reason"))
        return AbortRequest(reason)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.reason is not None:
            result["reason"] = from_union([lambda x: to_enum(AbortReason, x), from_none], self.reason)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AbortResult:
    """Result of aborting the current turn"""

    success: bool
    """Whether the abort completed successfully"""

    error: str | None = None
    """Error message if the abort failed"""

    @staticmethod
    def from_dict(obj: Any) -> AbortResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        error = from_union([from_str, from_none], obj.get("error"))
        return AbortResult(success, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        return result

@dataclass
class AccountGetQuotaRequest:
    git_hub_token: str | None = None
    """GitHub token for per-user quota lookup. When provided, resolves this token to determine
    the user's quota instead of using the global auth.
    """

    @staticmethod
    def from_dict(obj: Any) -> AccountGetQuotaRequest:
        assert isinstance(obj, dict)
        git_hub_token = from_union([from_str, from_none], obj.get("gitHubToken"))
        return AccountGetQuotaRequest(git_hub_token)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.git_hub_token is not None:
            result["gitHubToken"] = from_union([from_str, from_none], self.git_hub_token)
        return result

@dataclass
class AccountQuotaSnapshot:
    """Schema for the `AccountQuotaSnapshot` type."""

    entitlement_requests: int
    """Number of requests included in the entitlement, or -1 for unlimited entitlements"""

    is_unlimited_entitlement: bool
    """Whether the user has an unlimited usage entitlement"""

    overage: float
    """Number of additional usage requests made this period"""

    overage_allowed_with_exhausted_quota: bool
    """Whether additional usage is allowed when quota is exhausted"""

    remaining_percentage: float
    """Percentage of entitlement remaining"""

    usage_allowed_with_exhausted_quota: bool
    """Whether usage is still permitted after quota exhaustion"""

    used_requests: int
    """Number of requests used so far this period"""

    reset_date: str | None = None
    """Date when the quota resets (ISO 8601 string)"""

    @staticmethod
    def from_dict(obj: Any) -> AccountQuotaSnapshot:
        assert isinstance(obj, dict)
        entitlement_requests = from_int(obj.get("entitlementRequests"))
        is_unlimited_entitlement = from_bool(obj.get("isUnlimitedEntitlement"))
        overage = from_float(obj.get("overage"))
        overage_allowed_with_exhausted_quota = from_bool(obj.get("overageAllowedWithExhaustedQuota"))
        remaining_percentage = from_float(obj.get("remainingPercentage"))
        usage_allowed_with_exhausted_quota = from_bool(obj.get("usageAllowedWithExhaustedQuota"))
        used_requests = from_int(obj.get("usedRequests"))
        reset_date = from_union([from_str, from_none], obj.get("resetDate"))
        return AccountQuotaSnapshot(entitlement_requests, is_unlimited_entitlement, overage, overage_allowed_with_exhausted_quota, remaining_percentage, usage_allowed_with_exhausted_quota, used_requests, reset_date)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entitlementRequests"] = from_int(self.entitlement_requests)
        result["isUnlimitedEntitlement"] = from_bool(self.is_unlimited_entitlement)
        result["overage"] = to_float(self.overage)
        result["overageAllowedWithExhaustedQuota"] = from_bool(self.overage_allowed_with_exhausted_quota)
        result["remainingPercentage"] = to_float(self.remaining_percentage)
        result["usageAllowedWithExhaustedQuota"] = from_bool(self.usage_allowed_with_exhausted_quota)
        result["usedRequests"] = from_int(self.used_requests)
        if self.reset_date is not None:
            result["resetDate"] = from_union([from_str, from_none], self.reset_date)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class AgentInfoSource(Enum):
    """Where the agent definition was loaded from"""

    BUILTIN = "builtin"
    INHERITED = "inherited"
    PLUGIN = "plugin"
    PROJECT = "project"
    REMOTE = "remote"
    USER = "user"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentSelectRequest:
    """Name of the custom agent to select for subsequent turns."""

    name: str
    """Name of the custom agent to select"""

    @staticmethod
    def from_dict(obj: Any) -> AgentSelectRequest:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return AgentSelectRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotUserResponseEndpoints:
    """Schema for the `CopilotUserResponseEndpoints` type."""

    api: str | None = None
    origin_tracker: str | None = None
    proxy: str | None = None
    telemetry: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> CopilotUserResponseEndpoints:
        assert isinstance(obj, dict)
        api = from_union([from_str, from_none], obj.get("api"))
        origin_tracker = from_union([from_str, from_none], obj.get("origin-tracker"))
        proxy = from_union([from_str, from_none], obj.get("proxy"))
        telemetry = from_union([from_str, from_none], obj.get("telemetry"))
        return CopilotUserResponseEndpoints(api, origin_tracker, proxy, telemetry)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.api is not None:
            result["api"] = from_union([from_str, from_none], self.api)
        if self.origin_tracker is not None:
            result["origin-tracker"] = from_union([from_str, from_none], self.origin_tracker)
        if self.proxy is not None:
            result["proxy"] = from_union([from_str, from_none], self.proxy)
        if self.telemetry is not None:
            result["telemetry"] = from_union([from_str, from_none], self.telemetry)
        return result

class APIKeyAuthInfoType(Enum):
    API_KEY = "api-key"

# Experimental: this type is part of an experimental API and may change or be removed.
class AuthInfoType(Enum):
    """Authentication type"""

    API_KEY = "api-key"
    COPILOT_API_TOKEN = "copilot-api-token"
    ENV = "env"
    GH_CLI = "gh-cli"
    HMAC = "hmac"
    TOKEN = "token"
    USER = "user"

# Experimental: this type is part of an experimental API and may change or be removed.
class SlashCommandInputCompletion(Enum):
    """Optional completion hint for the input (e.g. 'directory' for filesystem path completion)"""

    DIRECTORY = "directory"

# Experimental: this type is part of an experimental API and may change or be removed.
class SlashCommandKind(Enum):
    """Coarse command category for grouping and behavior: runtime built-in, skill-backed
    command, or SDK/client-owned command
    """
    BUILTIN = "builtin"
    CLIENT = "client"
    SKILL = "skill"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandsHandlePendingCommandRequest:
    """Pending command request ID and an optional error if the client handler failed."""

    request_id: str
    """Request ID from the command invocation event"""

    error: str | None = None
    """Error message if the command handler failed"""

    @staticmethod
    def from_dict(obj: Any) -> CommandsHandlePendingCommandRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        error = from_union([from_str, from_none], obj.get("error"))
        return CommandsHandlePendingCommandRequest(request_id, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandsHandlePendingCommandResult:
    """Indicates whether the pending client-handled command was completed successfully."""

    success: bool
    """Whether the command was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> CommandsHandlePendingCommandResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return CommandsHandlePendingCommandResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandsInvokeRequest:
    """Slash command name and optional raw input string to invoke."""

    name: str
    """Command name. Leading slashes are stripped and the name is matched case-insensitively."""

    input: str | None = None
    """Raw input after the command name"""

    @staticmethod
    def from_dict(obj: Any) -> CommandsInvokeRequest:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        input = from_union([from_str, from_none], obj.get("input"))
        return CommandsInvokeRequest(name, input)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        if self.input is not None:
            result["input"] = from_union([from_str, from_none], self.input)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandsListRequest:
    """Optional filters controlling which command sources to include in the listing."""

    include_builtins: bool | None = None
    """Include runtime built-in commands"""

    include_client_commands: bool | None = None
    """Include commands registered by protocol clients, including SDK clients and extensions"""

    include_skills: bool | None = None
    """Include enabled user-invocable skills and commands"""

    @staticmethod
    def from_dict(obj: Any) -> CommandsListRequest:
        assert isinstance(obj, dict)
        include_builtins = from_union([from_bool, from_none], obj.get("includeBuiltins"))
        include_client_commands = from_union([from_bool, from_none], obj.get("includeClientCommands"))
        include_skills = from_union([from_bool, from_none], obj.get("includeSkills"))
        return CommandsListRequest(include_builtins, include_client_commands, include_skills)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.include_builtins is not None:
            result["includeBuiltins"] = from_union([from_bool, from_none], self.include_builtins)
        if self.include_client_commands is not None:
            result["includeClientCommands"] = from_union([from_bool, from_none], self.include_client_commands)
        if self.include_skills is not None:
            result["includeSkills"] = from_union([from_bool, from_none], self.include_skills)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandsRespondToQueuedCommandRequest:
    """Queued-command request ID and the result indicating whether the host executed it (and
    whether to stop processing further queued commands).
    """
    request_id: str
    """Request ID from the `command.queued` event the host is responding to."""

    result: QueuedCommandResult
    """Result of the queued command execution."""

    @staticmethod
    def from_dict(obj: Any) -> CommandsRespondToQueuedCommandRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = _load_QueuedCommandResult(obj.get("result"))
        return CommandsRespondToQueuedCommandRequest(request_id, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = (self.result).to_dict()
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandsRespondToQueuedCommandResult:
    """Indicates whether the queued-command response was matched to a pending request."""

    success: bool
    """Whether a pending queued command with the given request ID was found and resolved. False
    when the request was already resolved, cancelled, or unknown.
    """

    @staticmethod
    def from_dict(obj: Any) -> CommandsRespondToQueuedCommandResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return CommandsRespondToQueuedCommandResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ConnectRemoteSessionParams:
    """Remote session connection parameters."""

    session_id: str
    """Session ID to connect to."""

    @staticmethod
    def from_dict(obj: Any) -> ConnectRemoteSessionParams:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return ConnectRemoteSessionParams(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Internal: this type is an internal SDK API and is not part of the public surface.
@dataclass
class ConnectRequest:
    """Optional connection token presented by the SDK client during the handshake."""

    token: str | None = None
    """Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN"""

    @staticmethod
    def from_dict(obj: Any) -> ConnectRequest:
        assert isinstance(obj, dict)
        token = from_union([from_str, from_none], obj.get("token"))
        return ConnectRequest(token)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.token is not None:
            result["token"] = from_union([from_str, from_none], self.token)
        return result

# Internal: this type is an internal SDK API and is not part of the public surface.
@dataclass
class ConnectResult:
    """Handshake result reporting the server's protocol version and package version on success."""

    ok: bool
    """Always true on success"""

    protocol_version: int
    """Server protocol version number"""

    version: str
    """Server package version"""

    @staticmethod
    def from_dict(obj: Any) -> ConnectResult:
        assert isinstance(obj, dict)
        ok = from_bool(obj.get("ok"))
        protocol_version = from_int(obj.get("protocolVersion"))
        version = from_str(obj.get("version"))
        return ConnectResult(ok, protocol_version, version)

    def to_dict(self) -> dict:
        result: dict = {}
        result["ok"] = from_bool(self.ok)
        result["protocolVersion"] = from_int(self.protocol_version)
        result["version"] = from_str(self.version)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class ConnectedRemoteSessionMetadataKind(Enum):
    """Neutral SDK discriminator for the connected remote session kind."""

    CODING_AGENT = "coding-agent"
    REMOTE_SESSION = "remote-session"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ConnectedRemoteSessionMetadataRepository:
    """Repository associated with the connected remote session."""

    branch: str
    """Branch associated with the remote session."""

    name: str
    """Repository name."""

    owner: str
    """Repository owner or organization login."""

    @staticmethod
    def from_dict(obj: Any) -> ConnectedRemoteSessionMetadataRepository:
        assert isinstance(obj, dict)
        branch = from_str(obj.get("branch"))
        name = from_str(obj.get("name"))
        owner = from_str(obj.get("owner"))
        return ConnectedRemoteSessionMetadataRepository(branch, name, owner)

    def to_dict(self) -> dict:
        result: dict = {}
        result["branch"] = from_str(self.branch)
        result["name"] = from_str(self.name)
        result["owner"] = from_str(self.owner)
        return result

class ContentFilterMode(Enum):
    """Controls how MCP tool result content is filtered: none leaves content unchanged, markdown
    sanitizes HTML while preserving Markdown-friendly output, and hidden_characters removes
    characters that can hide directives.
    """
    HIDDEN_CHARACTERS = "hidden_characters"
    MARKDOWN = "markdown"
    NONE = "none"

class Host(Enum):
    HTTPS_GITHUB_COM = "https://github.com"

class CopilotAPITokenAuthInfoType(Enum):
    COPILOT_API_TOKEN = "copilot-api-token"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotUserResponseQuotaSnapshotsChat:
    """Schema for the `CopilotUserResponseQuotaSnapshotsChat` type."""

    entitlement: float | None = None
    has_quota: bool | None = None
    overage_count: float | None = None
    overage_permitted: bool | None = None
    percent_remaining: float | None = None
    quota_id: str | None = None
    quota_remaining: float | None = None
    quota_reset_at: float | None = None
    remaining: float | None = None
    timestamp_utc: str | None = None
    token_based_billing: bool | None = None
    unlimited: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> CopilotUserResponseQuotaSnapshotsChat:
        assert isinstance(obj, dict)
        entitlement = from_union([from_float, from_none], obj.get("entitlement"))
        has_quota = from_union([from_bool, from_none], obj.get("has_quota"))
        overage_count = from_union([from_float, from_none], obj.get("overage_count"))
        overage_permitted = from_union([from_bool, from_none], obj.get("overage_permitted"))
        percent_remaining = from_union([from_float, from_none], obj.get("percent_remaining"))
        quota_id = from_union([from_str, from_none], obj.get("quota_id"))
        quota_remaining = from_union([from_float, from_none], obj.get("quota_remaining"))
        quota_reset_at = from_union([from_float, from_none], obj.get("quota_reset_at"))
        remaining = from_union([from_float, from_none], obj.get("remaining"))
        timestamp_utc = from_union([from_str, from_none], obj.get("timestamp_utc"))
        token_based_billing = from_union([from_bool, from_none], obj.get("token_based_billing"))
        unlimited = from_union([from_bool, from_none], obj.get("unlimited"))
        return CopilotUserResponseQuotaSnapshotsChat(entitlement, has_quota, overage_count, overage_permitted, percent_remaining, quota_id, quota_remaining, quota_reset_at, remaining, timestamp_utc, token_based_billing, unlimited)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.entitlement is not None:
            result["entitlement"] = from_union([to_float, from_none], self.entitlement)
        if self.has_quota is not None:
            result["has_quota"] = from_union([from_bool, from_none], self.has_quota)
        if self.overage_count is not None:
            result["overage_count"] = from_union([to_float, from_none], self.overage_count)
        if self.overage_permitted is not None:
            result["overage_permitted"] = from_union([from_bool, from_none], self.overage_permitted)
        if self.percent_remaining is not None:
            result["percent_remaining"] = from_union([to_float, from_none], self.percent_remaining)
        if self.quota_id is not None:
            result["quota_id"] = from_union([from_str, from_none], self.quota_id)
        if self.quota_remaining is not None:
            result["quota_remaining"] = from_union([to_float, from_none], self.quota_remaining)
        if self.quota_reset_at is not None:
            result["quota_reset_at"] = from_union([to_float, from_none], self.quota_reset_at)
        if self.remaining is not None:
            result["remaining"] = from_union([to_float, from_none], self.remaining)
        if self.timestamp_utc is not None:
            result["timestamp_utc"] = from_union([from_str, from_none], self.timestamp_utc)
        if self.token_based_billing is not None:
            result["token_based_billing"] = from_union([from_bool, from_none], self.token_based_billing)
        if self.unlimited is not None:
            result["unlimited"] = from_union([from_bool, from_none], self.unlimited)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotUserResponseQuotaSnapshotsCompletions:
    """Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type."""

    entitlement: float | None = None
    has_quota: bool | None = None
    overage_count: float | None = None
    overage_permitted: bool | None = None
    percent_remaining: float | None = None
    quota_id: str | None = None
    quota_remaining: float | None = None
    quota_reset_at: float | None = None
    remaining: float | None = None
    timestamp_utc: str | None = None
    token_based_billing: bool | None = None
    unlimited: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> CopilotUserResponseQuotaSnapshotsCompletions:
        assert isinstance(obj, dict)
        entitlement = from_union([from_float, from_none], obj.get("entitlement"))
        has_quota = from_union([from_bool, from_none], obj.get("has_quota"))
        overage_count = from_union([from_float, from_none], obj.get("overage_count"))
        overage_permitted = from_union([from_bool, from_none], obj.get("overage_permitted"))
        percent_remaining = from_union([from_float, from_none], obj.get("percent_remaining"))
        quota_id = from_union([from_str, from_none], obj.get("quota_id"))
        quota_remaining = from_union([from_float, from_none], obj.get("quota_remaining"))
        quota_reset_at = from_union([from_float, from_none], obj.get("quota_reset_at"))
        remaining = from_union([from_float, from_none], obj.get("remaining"))
        timestamp_utc = from_union([from_str, from_none], obj.get("timestamp_utc"))
        token_based_billing = from_union([from_bool, from_none], obj.get("token_based_billing"))
        unlimited = from_union([from_bool, from_none], obj.get("unlimited"))
        return CopilotUserResponseQuotaSnapshotsCompletions(entitlement, has_quota, overage_count, overage_permitted, percent_remaining, quota_id, quota_remaining, quota_reset_at, remaining, timestamp_utc, token_based_billing, unlimited)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.entitlement is not None:
            result["entitlement"] = from_union([to_float, from_none], self.entitlement)
        if self.has_quota is not None:
            result["has_quota"] = from_union([from_bool, from_none], self.has_quota)
        if self.overage_count is not None:
            result["overage_count"] = from_union([to_float, from_none], self.overage_count)
        if self.overage_permitted is not None:
            result["overage_permitted"] = from_union([from_bool, from_none], self.overage_permitted)
        if self.percent_remaining is not None:
            result["percent_remaining"] = from_union([to_float, from_none], self.percent_remaining)
        if self.quota_id is not None:
            result["quota_id"] = from_union([from_str, from_none], self.quota_id)
        if self.quota_remaining is not None:
            result["quota_remaining"] = from_union([to_float, from_none], self.quota_remaining)
        if self.quota_reset_at is not None:
            result["quota_reset_at"] = from_union([to_float, from_none], self.quota_reset_at)
        if self.remaining is not None:
            result["remaining"] = from_union([to_float, from_none], self.remaining)
        if self.timestamp_utc is not None:
            result["timestamp_utc"] = from_union([from_str, from_none], self.timestamp_utc)
        if self.token_based_billing is not None:
            result["token_based_billing"] = from_union([from_bool, from_none], self.token_based_billing)
        if self.unlimited is not None:
            result["unlimited"] = from_union([from_bool, from_none], self.unlimited)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotUserResponseQuotaSnapshotsPremiumInteractions:
    """Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type."""

    entitlement: float | None = None
    has_quota: bool | None = None
    overage_count: float | None = None
    overage_permitted: bool | None = None
    percent_remaining: float | None = None
    quota_id: str | None = None
    quota_remaining: float | None = None
    quota_reset_at: float | None = None
    remaining: float | None = None
    timestamp_utc: str | None = None
    token_based_billing: bool | None = None
    unlimited: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> CopilotUserResponseQuotaSnapshotsPremiumInteractions:
        assert isinstance(obj, dict)
        entitlement = from_union([from_float, from_none], obj.get("entitlement"))
        has_quota = from_union([from_bool, from_none], obj.get("has_quota"))
        overage_count = from_union([from_float, from_none], obj.get("overage_count"))
        overage_permitted = from_union([from_bool, from_none], obj.get("overage_permitted"))
        percent_remaining = from_union([from_float, from_none], obj.get("percent_remaining"))
        quota_id = from_union([from_str, from_none], obj.get("quota_id"))
        quota_remaining = from_union([from_float, from_none], obj.get("quota_remaining"))
        quota_reset_at = from_union([from_float, from_none], obj.get("quota_reset_at"))
        remaining = from_union([from_float, from_none], obj.get("remaining"))
        timestamp_utc = from_union([from_str, from_none], obj.get("timestamp_utc"))
        token_based_billing = from_union([from_bool, from_none], obj.get("token_based_billing"))
        unlimited = from_union([from_bool, from_none], obj.get("unlimited"))
        return CopilotUserResponseQuotaSnapshotsPremiumInteractions(entitlement, has_quota, overage_count, overage_permitted, percent_remaining, quota_id, quota_remaining, quota_reset_at, remaining, timestamp_utc, token_based_billing, unlimited)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.entitlement is not None:
            result["entitlement"] = from_union([to_float, from_none], self.entitlement)
        if self.has_quota is not None:
            result["has_quota"] = from_union([from_bool, from_none], self.has_quota)
        if self.overage_count is not None:
            result["overage_count"] = from_union([to_float, from_none], self.overage_count)
        if self.overage_permitted is not None:
            result["overage_permitted"] = from_union([from_bool, from_none], self.overage_permitted)
        if self.percent_remaining is not None:
            result["percent_remaining"] = from_union([to_float, from_none], self.percent_remaining)
        if self.quota_id is not None:
            result["quota_id"] = from_union([from_str, from_none], self.quota_id)
        if self.quota_remaining is not None:
            result["quota_remaining"] = from_union([to_float, from_none], self.quota_remaining)
        if self.quota_reset_at is not None:
            result["quota_reset_at"] = from_union([to_float, from_none], self.quota_reset_at)
        if self.remaining is not None:
            result["remaining"] = from_union([to_float, from_none], self.remaining)
        if self.timestamp_utc is not None:
            result["timestamp_utc"] = from_union([from_str, from_none], self.timestamp_utc)
        if self.token_based_billing is not None:
            result["token_based_billing"] = from_union([from_bool, from_none], self.token_based_billing)
        if self.unlimited is not None:
            result["unlimited"] = from_union([from_bool, from_none], self.unlimited)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CurrentModel:
    """The currently selected model and reasoning effort for the session."""

    model_id: str | None = None
    """Currently active model identifier"""

    reasoning_effort: str | None = None
    """Reasoning effort level currently applied to the active model, when one is set. Reads
    `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the
    two values are reported as a snapshot.
    """

    @staticmethod
    def from_dict(obj: Any) -> CurrentModel:
        assert isinstance(obj, dict)
        model_id = from_union([from_str, from_none], obj.get("modelId"))
        reasoning_effort = from_union([from_str, from_none], obj.get("reasoningEffort"))
        return CurrentModel(model_id, reasoning_effort)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model_id is not None:
            result["modelId"] = from_union([from_str, from_none], self.model_id)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_str, from_none], self.reasoning_effort)
        return result

class DiscoveredMCPServerType(Enum):
    """Server transport type: stdio, http, sse, or memory"""

    HTTP = "http"
    MEMORY = "memory"
    SSE = "sse"
    STDIO = "stdio"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EnqueueCommandParams:
    """Slash-prefixed command string to enqueue for FIFO processing."""

    command: str
    """Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO
    with any in-flight items; if the session is idle, processing kicks off immediately.
    """

    @staticmethod
    def from_dict(obj: Any) -> EnqueueCommandParams:
        assert isinstance(obj, dict)
        command = from_str(obj.get("command"))
        return EnqueueCommandParams(command)

    def to_dict(self) -> dict:
        result: dict = {}
        result["command"] = from_str(self.command)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EnqueueCommandResult:
    """Indicates whether the command was accepted into the local execution queue."""

    queued: bool
    """True when the command was accepted into the local execution queue. False when the call
    targets a session that does not support local command queueing (e.g. remote sessions).
    """

    @staticmethod
    def from_dict(obj: Any) -> EnqueueCommandResult:
        assert isinstance(obj, dict)
        queued = from_bool(obj.get("queued"))
        return EnqueueCommandResult(queued)

    def to_dict(self) -> dict:
        result: dict = {}
        result["queued"] = from_bool(self.queued)
        return result

class EnvAuthInfoType(Enum):
    ENV = "env"

# Experimental: this type is part of an experimental API and may change or be removed.
class EventsAgentScope(Enum):
    """Agent-scope filter: 'primary' returns only main-agent events plus events whose type
    starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns
    events from all agents (matching wildcard-subscription behavior). Default is 'all' to
    preserve wildcard semantics for catch-up callers.
    """
    ALL = "all"
    PRIMARY = "primary"

# Experimental: this type is part of an experimental API and may change or be removed.
class EventLogTypes(Enum):
    EMPTY = "*"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EventLogReleaseInterestResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> EventLogReleaseInterestResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return EventLogReleaseInterestResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EventLogTailResult:
    """Snapshot of the current tail cursor without returning any events. Use this when a
    consumer wants to subscribe to live events going forward without first paginating through
    the entire persisted history (which would happen if `read` were called without a cursor
    on a long-lived session).
    """
    cursor: str
    """Opaque cursor pointing at the current tail of the session's persisted-events history.
    Pass back to `read` to receive only events that arrive AFTER this snapshot. When the
    session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent
    to omitting the cursor on a first read).
    """

    @staticmethod
    def from_dict(obj: Any) -> EventLogTailResult:
        assert isinstance(obj, dict)
        cursor = from_str(obj.get("cursor"))
        return EventLogTailResult(cursor)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cursor"] = from_str(self.cursor)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class EventsCursorStatus(Enum):
    """Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor
    referred to an event that no longer exists in history (e.g. truncated or compacted away)
    and the read started from the beginning of the remaining history.
    """
    EXPIRED = "expired"
    OK = "ok"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExecuteCommandParams:
    """Slash command name and argument string to execute synchronously."""

    args: str
    """Argument string to pass to the command (empty string if none)."""

    command_name: str
    """Name of the slash command to invoke (without the leading '/')."""

    @staticmethod
    def from_dict(obj: Any) -> ExecuteCommandParams:
        assert isinstance(obj, dict)
        args = from_str(obj.get("args"))
        command_name = from_str(obj.get("commandName"))
        return ExecuteCommandParams(args, command_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["args"] = from_str(self.args)
        result["commandName"] = from_str(self.command_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExecuteCommandResult:
    """Error message produced while executing the command, if any."""

    error: str | None = None
    """Error message produced while executing the command, if any. Omitted when the handler
    succeeded.
    """

    @staticmethod
    def from_dict(obj: Any) -> ExecuteCommandResult:
        assert isinstance(obj, dict)
        error = from_union([from_str, from_none], obj.get("error"))
        return ExecuteCommandResult(error)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class ExtensionSource(Enum):
    """Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)"""

    PROJECT = "project"
    USER = "user"

# Experimental: this type is part of an experimental API and may change or be removed.
class ExtensionStatus(Enum):
    """Current status: running, disabled, failed, or starting"""

    DISABLED = "disabled"
    FAILED = "failed"
    RUNNING = "running"
    STARTING = "starting"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExtensionsDisableRequest:
    """Source-qualified extension identifier to disable for the session."""

    id: str
    """Source-qualified extension ID to disable"""

    @staticmethod
    def from_dict(obj: Any) -> ExtensionsDisableRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return ExtensionsDisableRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExtensionsEnableRequest:
    """Source-qualified extension identifier to enable for the session."""

    id: str
    """Source-qualified extension ID to enable"""

    @staticmethod
    def from_dict(obj: Any) -> ExtensionsEnableRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return ExtensionsEnableRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class ExternalToolTextResultForLlmBinaryResultsForLlmType(Enum):
    """Binary result type discriminator. Use "image" for images and "resource" for other binary
    data.
    """
    IMAGE = "image"
    RESOURCE = "resource"

# Experimental: this type is part of an experimental API and may change or be removed.
class ExternalToolTextResultForLlmContentResourceLinkIconTheme(Enum):
    """Theme variant this icon is intended for"""

    DARK = "dark"
    LIGHT = "light"

class ExternalToolTextResultForLlmContentType(Enum):
    AUDIO = "audio"
    IMAGE = "image"
    RESOURCE = "resource"
    RESOURCE_LINK = "resource_link"
    TERMINAL = "terminal"
    TEXT = "text"

class ExternalToolTextResultForLlmContentAudioType(Enum):
    AUDIO = "audio"

class ExternalToolTextResultForLlmContentImageType(Enum):
    IMAGE = "image"

class ExternalToolTextResultForLlmContentResourceType(Enum):
    RESOURCE = "resource"

class ExternalToolTextResultForLlmContentResourceLinkType(Enum):
    RESOURCE_LINK = "resource_link"

class ExternalToolTextResultForLlmContentTerminalType(Enum):
    TERMINAL = "terminal"

class KindEnum(Enum):
    TEXT = "text"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FleetStartRequest:
    """Optional user prompt to combine with the fleet orchestration instructions."""

    prompt: str | None = None
    """Optional user prompt to combine with fleet instructions"""

    @staticmethod
    def from_dict(obj: Any) -> FleetStartRequest:
        assert isinstance(obj, dict)
        prompt = from_union([from_str, from_none], obj.get("prompt"))
        return FleetStartRequest(prompt)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.prompt is not None:
            result["prompt"] = from_union([from_str, from_none], self.prompt)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FleetStartResult:
    """Indicates whether fleet mode was successfully activated."""

    started: bool
    """Whether fleet mode was successfully activated"""

    @staticmethod
    def from_dict(obj: Any) -> FleetStartResult:
        assert isinstance(obj, dict)
        started = from_bool(obj.get("started"))
        return FleetStartResult(started)

    def to_dict(self) -> dict:
        result: dict = {}
        result["started"] = from_bool(self.started)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FolderTrustAddParams:
    """Folder path to add to trusted folders."""

    path: str
    """Folder path to mark as trusted"""

    @staticmethod
    def from_dict(obj: Any) -> FolderTrustAddParams:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return FolderTrustAddParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FolderTrustCheckParams:
    """Folder path to check for trust."""

    path: str
    """Folder path to check"""

    @staticmethod
    def from_dict(obj: Any) -> FolderTrustCheckParams:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return FolderTrustCheckParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FolderTrustCheckResult:
    """Folder trust check result."""

    trusted: bool
    """Whether the folder is trusted"""

    @staticmethod
    def from_dict(obj: Any) -> FolderTrustCheckResult:
        assert isinstance(obj, dict)
        trusted = from_bool(obj.get("trusted"))
        return FolderTrustCheckResult(trusted)

    def to_dict(self) -> dict:
        result: dict = {}
        result["trusted"] = from_bool(self.trusted)
        return result

class GhCLIAuthInfoType(Enum):
    GH_CLI = "gh-cli"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HandlePendingToolCallResult:
    """Indicates whether the external tool call result was handled successfully."""

    success: bool
    """Whether the tool call result was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> HandlePendingToolCallResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return HandlePendingToolCallResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryAbortManualCompactionResult:
    """Indicates whether an in-progress manual compaction was aborted."""

    aborted: bool
    """Whether an in-progress manual compaction was aborted. False when no manual compaction was
    running, when its abort controller was already aborted, or when the session is remote.
    """

    @staticmethod
    def from_dict(obj: Any) -> HistoryAbortManualCompactionResult:
        assert isinstance(obj, dict)
        aborted = from_bool(obj.get("aborted"))
        return HistoryAbortManualCompactionResult(aborted)

    def to_dict(self) -> dict:
        result: dict = {}
        result["aborted"] = from_bool(self.aborted)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryCancelBackgroundCompactionResult:
    """Indicates whether an in-progress background compaction was cancelled."""

    cancelled: bool
    """Whether an in-progress background compaction was cancelled. False when no compaction was
    running, when the session is remote, or when the underlying processor was unavailable.
    """

    @staticmethod
    def from_dict(obj: Any) -> HistoryCancelBackgroundCompactionResult:
        assert isinstance(obj, dict)
        cancelled = from_bool(obj.get("cancelled"))
        return HistoryCancelBackgroundCompactionResult(cancelled)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cancelled"] = from_bool(self.cancelled)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryCompactContextWindow:
    """Post-compaction context window usage breakdown"""

    current_tokens: int
    """Current total tokens in the context window (system + conversation + tool definitions)"""

    messages_length: int
    """Current number of messages in the conversation"""

    token_limit: int
    """Maximum token count for the model's context window"""

    conversation_tokens: int | None = None
    """Token count from non-system messages (user, assistant, tool)"""

    system_tokens: int | None = None
    """Token count from system message(s)"""

    tool_definitions_tokens: int | None = None
    """Token count from tool definitions"""

    @staticmethod
    def from_dict(obj: Any) -> HistoryCompactContextWindow:
        assert isinstance(obj, dict)
        current_tokens = from_int(obj.get("currentTokens"))
        messages_length = from_int(obj.get("messagesLength"))
        token_limit = from_int(obj.get("tokenLimit"))
        conversation_tokens = from_union([from_int, from_none], obj.get("conversationTokens"))
        system_tokens = from_union([from_int, from_none], obj.get("systemTokens"))
        tool_definitions_tokens = from_union([from_int, from_none], obj.get("toolDefinitionsTokens"))
        return HistoryCompactContextWindow(current_tokens, messages_length, token_limit, conversation_tokens, system_tokens, tool_definitions_tokens)

    def to_dict(self) -> dict:
        result: dict = {}
        result["currentTokens"] = from_int(self.current_tokens)
        result["messagesLength"] = from_int(self.messages_length)
        result["tokenLimit"] = from_int(self.token_limit)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_int, from_none], self.conversation_tokens)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_int, from_none], self.system_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_int, from_none], self.tool_definitions_tokens)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryCompactRequest:
    """Optional compaction parameters."""

    custom_instructions: str | None = None
    """Optional user-provided instructions to focus the compaction summary"""

    @staticmethod
    def from_dict(obj: Any) -> HistoryCompactRequest:
        assert isinstance(obj, dict)
        custom_instructions = from_union([from_str, from_none], obj.get("customInstructions"))
        return HistoryCompactRequest(custom_instructions)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.custom_instructions is not None:
            result["customInstructions"] = from_union([from_str, from_none], self.custom_instructions)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistorySummarizeForHandoffResult:
    """Markdown summary of the conversation context (empty when not available)."""

    summary: str
    """Markdown summary of the conversation context produced by an LLM. Empty string when there
    are no messages or when the session does not support local summarization.
    """

    @staticmethod
    def from_dict(obj: Any) -> HistorySummarizeForHandoffResult:
        assert isinstance(obj, dict)
        summary = from_str(obj.get("summary"))
        return HistorySummarizeForHandoffResult(summary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["summary"] = from_str(self.summary)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryTruncateRequest:
    """Identifier of the event to truncate to; this event and all later events are removed."""

    event_id: str
    """Event ID to truncate to. This event and all events after it are removed from the session."""

    @staticmethod
    def from_dict(obj: Any) -> HistoryTruncateRequest:
        assert isinstance(obj, dict)
        event_id = from_str(obj.get("eventId"))
        return HistoryTruncateRequest(event_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventId"] = from_str(self.event_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryTruncateResult:
    """Number of events that were removed by the truncation."""

    events_removed: int
    """Number of events that were removed"""

    @staticmethod
    def from_dict(obj: Any) -> HistoryTruncateResult:
        assert isinstance(obj, dict)
        events_removed = from_int(obj.get("eventsRemoved"))
        return HistoryTruncateResult(events_removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventsRemoved"] = from_int(self.events_removed)
        return result

class HMACAuthInfoType(Enum):
    HMAC = "hmac"

class PurpleSource(Enum):
    GITHUB = "github"
    LOCAL = "local"
    URL = "url"

class FluffySource(Enum):
    GITHUB = "github"

class TentacledSource(Enum):
    LOCAL = "local"

class StickySource(Enum):
    URL = "url"

# Experimental: this type is part of an experimental API and may change or be removed.
class InstructionsSourcesLocation(Enum):
    """Where this source lives — used for UI grouping"""

    PLUGIN = "plugin"
    REPOSITORY = "repository"
    USER = "user"
    WORKING_DIRECTORY = "working-directory"

# Experimental: this type is part of an experimental API and may change or be removed.
class InstructionsSourcesType(Enum):
    """Category of instruction source — used for merge logic"""

    CHILD_INSTRUCTIONS = "child-instructions"
    HOME = "home"
    MODEL = "model"
    NESTED_AGENTS = "nested-agents"
    PLUGIN = "plugin"
    REPO = "repo"
    VSCODE = "vscode"

# Experimental: this type is part of an experimental API and may change or be removed.
class SessionLogLevel(Enum):
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    ERROR = "error"
    INFO = "info"
    WARNING = "warning"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class LogResult:
    """Identifier of the session event that was emitted for the log message."""

    event_id: UUID
    """The unique identifier of the emitted session event"""

    @staticmethod
    def from_dict(obj: Any) -> LogResult:
        assert isinstance(obj, dict)
        event_id = UUID(obj.get("eventId"))
        return LogResult(event_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventId"] = str(self.event_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class LspInitializeRequest:
    """Parameters for (re)loading the merged LSP configuration set."""

    force: bool | None = None
    """Force re-initialization even when LSP configs were already loaded for the working
    directory.
    """
    git_root: str | None = None
    """Git root used as the boundary when traversing for project-level LSP configs (supports
    monorepos).
    """
    working_directory: str | None = None
    """Working directory used to load project-level LSP configs. Defaults to the session working
    directory when omitted.
    """

    @staticmethod
    def from_dict(obj: Any) -> LspInitializeRequest:
        assert isinstance(obj, dict)
        force = from_union([from_bool, from_none], obj.get("force"))
        git_root = from_union([from_str, from_none], obj.get("gitRoot"))
        working_directory = from_union([from_str, from_none], obj.get("workingDirectory"))
        return LspInitializeRequest(force, git_root, working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.force is not None:
            result["force"] = from_union([from_bool, from_none], self.force)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_str, from_none], self.git_root)
        if self.working_directory is not None:
            result["workingDirectory"] = from_union([from_str, from_none], self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPCancelSamplingExecutionParams:
    """The requestId previously passed to executeSampling that should be cancelled."""

    request_id: str
    """The requestId previously passed to executeSampling that should be cancelled"""

    @staticmethod
    def from_dict(obj: Any) -> MCPCancelSamplingExecutionParams:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        return MCPCancelSamplingExecutionParams(request_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPCancelSamplingExecutionResult:
    """Indicates whether an in-flight sampling execution with the given requestId was found and
    cancelled.
    """
    cancelled: bool
    """True if an in-flight execution with the given requestId was found and signalled to
    cancel. False when no such execution is in flight (already completed, never started, or
    cancelled by another caller).
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPCancelSamplingExecutionResult:
        assert isinstance(obj, dict)
        cancelled = from_bool(obj.get("cancelled"))
        return MCPCancelSamplingExecutionResult(cancelled)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cancelled"] = from_bool(self.cancelled)
        return result

@dataclass
class MCPServerConfigHTTPAuth:
    """Additional authentication configuration for this server."""

    redirect_port: int | None = None
    """Fixed port for the OAuth redirect callback server."""

    @staticmethod
    def from_dict(obj: Any) -> MCPServerConfigHTTPAuth:
        assert isinstance(obj, dict)
        redirect_port = from_union([from_int, from_none], obj.get("redirectPort"))
        return MCPServerConfigHTTPAuth(redirect_port)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.redirect_port is not None:
            result["redirectPort"] = from_union([from_int, from_none], self.redirect_port)
        return result

class MCPServerConfigHTTPOauthGrantType(Enum):
    """OAuth grant type to use when authenticating to the remote MCP server."""

    AUTHORIZATION_CODE = "authorization_code"
    CLIENT_CREDENTIALS = "client_credentials"

class MCPServerConfigHTTPType(Enum):
    """Remote transport type. Defaults to "http" when omitted."""

    HTTP = "http"
    SSE = "sse"

@dataclass
class MCPConfigDisableRequest:
    """MCP server names to disable for new sessions."""

    names: list[str]
    """Names of MCP servers to disable. Each server is added to the persisted disabled list so
    new sessions skip it. Already-disabled names are ignored. Active sessions keep their
    current connections until they end.
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPConfigDisableRequest:
        assert isinstance(obj, dict)
        names = from_list(from_str, obj.get("names"))
        return MCPConfigDisableRequest(names)

    def to_dict(self) -> dict:
        result: dict = {}
        result["names"] = from_list(from_str, self.names)
        return result

@dataclass
class MCPConfigEnableRequest:
    """MCP server names to enable for new sessions."""

    names: list[str]
    """Names of MCP servers to enable. Each server is removed from the persisted disabled list
    so new sessions spawn it. Unknown or already-enabled names are ignored.
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPConfigEnableRequest:
        assert isinstance(obj, dict)
        names = from_list(from_str, obj.get("names"))
        return MCPConfigEnableRequest(names)

    def to_dict(self) -> dict:
        result: dict = {}
        result["names"] = from_list(from_str, self.names)
        return result

@dataclass
class MCPConfigRemoveRequest:
    """MCP server name to remove from user configuration."""

    name: str
    """Name of the MCP server to remove"""

    @staticmethod
    def from_dict(obj: Any) -> MCPConfigRemoveRequest:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return MCPConfigRemoveRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPDisableRequest:
    """Name of the MCP server to disable for the session."""

    server_name: str
    """Name of the MCP server to disable"""

    @staticmethod
    def from_dict(obj: Any) -> MCPDisableRequest:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return MCPDisableRequest(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result

@dataclass
class MCPDiscoverRequest:
    """Optional working directory used as context for MCP server discovery."""

    working_directory: str | None = None
    """Working directory used as context for discovery (e.g., plugin resolution)"""

    @staticmethod
    def from_dict(obj: Any) -> MCPDiscoverRequest:
        assert isinstance(obj, dict)
        working_directory = from_union([from_str, from_none], obj.get("workingDirectory"))
        return MCPDiscoverRequest(working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.working_directory is not None:
            result["workingDirectory"] = from_union([from_str, from_none], self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPEnableRequest:
    """Name of the MCP server to enable for the session."""

    server_name: str
    """Name of the MCP server to enable"""

    @staticmethod
    def from_dict(obj: Any) -> MCPEnableRequest:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return MCPEnableRequest(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPOauthLoginRequest:
    """Remote MCP server name and optional overrides controlling reauthentication, OAuth client
    display name, and the callback success-page copy.
    """
    server_name: str
    """Name of the remote MCP server to authenticate"""

    callback_success_message: str | None = None
    """Optional override for the body text shown on the OAuth loopback callback success page.
    When omitted, the runtime applies a neutral fallback; callers driving interactive auth
    should pass surface-specific copy telling the user where to return.
    """
    client_name: str | None = None
    """Optional override for the OAuth client display name shown on the consent screen. Applies
    to newly registered dynamic clients only — existing registrations keep the name they were
    created with. When omitted, the runtime applies a neutral fallback; callers driving
    interactive auth should pass their own surface-specific label so the consent screen
    matches the product the user sees.
    """
    force_reauth: bool | None = None
    """When true, clears any cached OAuth token for the server and runs a full new
    authorization. Use when the user explicitly wants to switch accounts or believes their
    session is stuck.
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPOauthLoginRequest:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        callback_success_message = from_union([from_str, from_none], obj.get("callbackSuccessMessage"))
        client_name = from_union([from_str, from_none], obj.get("clientName"))
        force_reauth = from_union([from_bool, from_none], obj.get("forceReauth"))
        return MCPOauthLoginRequest(server_name, callback_success_message, client_name, force_reauth)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        if self.callback_success_message is not None:
            result["callbackSuccessMessage"] = from_union([from_str, from_none], self.callback_success_message)
        if self.client_name is not None:
            result["clientName"] = from_union([from_str, from_none], self.client_name)
        if self.force_reauth is not None:
            result["forceReauth"] = from_union([from_bool, from_none], self.force_reauth)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPOauthLoginResult:
    """OAuth authorization URL the caller should open, or empty when cached tokens already
    authenticated the server.
    """
    authorization_url: str | None = None
    """URL the caller should open in a browser to complete OAuth. Omitted when cached tokens
    were still valid and no browser interaction was needed — the server is already
    reconnected in that case. When present, the runtime starts the callback listener before
    returning and continues the flow in the background; completion is signaled via
    session.mcp_server_status_changed.
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPOauthLoginResult:
        assert isinstance(obj, dict)
        authorization_url = from_union([from_str, from_none], obj.get("authorizationUrl"))
        return MCPOauthLoginResult(authorization_url)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.authorization_url is not None:
            result["authorizationUrl"] = from_union([from_str, from_none], self.authorization_url)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPRemoveGitHubResult:
    """Indicates whether the auto-managed `github` MCP server was removed (false when nothing to
    remove).
    """
    removed: bool
    """True when the auto-managed `github` MCP server was removed; false when no removal
    happened (e.g. user has explicitly configured a `github` server, or the server was not
    registered).
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPRemoveGitHubResult:
        assert isinstance(obj, dict)
        removed = from_bool(obj.get("removed"))
        return MCPRemoveGitHubResult(removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["removed"] = from_bool(self.removed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class MCPSamplingExecutionAction(Enum):
    """Outcome of the sampling inference. 'success' produced a response; 'failure' encountered
    an error (including agent-side rejection by content filter or criteria); 'cancelled' the
    caller cancelled this execution via cancelSamplingExecution.
    """
    CANCELLED = "cancelled"
    FAILURE = "failure"
    SUCCESS = "success"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPServer:
    """Schema for the `McpServer` type."""

    name: str
    """Server name (config key)"""

    status: McpServerStatus
    """Connection status: connected, failed, needs-auth, pending, disabled, or not_configured"""

    error: str | None = None
    """Error message if the server failed to connect"""

    source: McpServerSource | None = None
    """Configuration source: user, workspace, plugin, or builtin"""

    @staticmethod
    def from_dict(obj: Any) -> MCPServer:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        status = McpServerStatus(obj.get("status"))
        error = from_union([from_str, from_none], obj.get("error"))
        source = from_union([McpServerSource, from_none], obj.get("source"))
        return MCPServer(name, status, error, source)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["status"] = to_enum(McpServerStatus, self.status)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_enum(McpServerSource, x), from_none], self.source)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class MCPSetEnvValueModeDetails(Enum):
    """How environment-variable values supplied to MCP servers are resolved. "direct" passes
    literal string values; "indirect" treats values as references (e.g. names of environment
    variables on the host) that the runtime resolves before launch. Defaults to the runtime's
    startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI
    prompt mode and ACP) set this to "direct".

    Mode recorded on the session after the update

    How env values are passed to MCP servers (`direct` inlines literal values; `indirect`
    resolves at launch).
    """
    DIRECT = "direct"
    INDIRECT = "indirect"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionContextInfo:
    """Token-usage breakdown for the session's current context window"""

    buffer_tokens: int
    """Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%)"""

    compaction_threshold: int
    """Token count at which background compaction starts (configurable percentage of
    promptTokenLimit)
    """
    conversation_tokens: int
    """Tokens consumed by user/assistant/tool messages"""

    limit: int
    """Total context limit for /context display. promptTokenLimit + min(32k or 64k,
    outputTokenLimit) depending on model.
    """
    model_name: str
    """The model used for token counting"""

    prompt_token_limit: int
    """Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified)"""

    system_tokens: int
    """Tokens consumed by the system prompt"""

    tool_definitions_tokens: int
    """Tokens consumed by tool definitions sent to the model (excludes deferred tools)"""

    total_tokens: int
    """Sum of system, conversation and tool-definition tokens"""

    @staticmethod
    def from_dict(obj: Any) -> SessionContextInfo:
        assert isinstance(obj, dict)
        buffer_tokens = from_int(obj.get("bufferTokens"))
        compaction_threshold = from_int(obj.get("compactionThreshold"))
        conversation_tokens = from_int(obj.get("conversationTokens"))
        limit = from_int(obj.get("limit"))
        model_name = from_str(obj.get("modelName"))
        prompt_token_limit = from_int(obj.get("promptTokenLimit"))
        system_tokens = from_int(obj.get("systemTokens"))
        tool_definitions_tokens = from_int(obj.get("toolDefinitionsTokens"))
        total_tokens = from_int(obj.get("totalTokens"))
        return SessionContextInfo(buffer_tokens, compaction_threshold, conversation_tokens, limit, model_name, prompt_token_limit, system_tokens, tool_definitions_tokens, total_tokens)

    def to_dict(self) -> dict:
        result: dict = {}
        result["bufferTokens"] = from_int(self.buffer_tokens)
        result["compactionThreshold"] = from_int(self.compaction_threshold)
        result["conversationTokens"] = from_int(self.conversation_tokens)
        result["limit"] = from_int(self.limit)
        result["modelName"] = from_str(self.model_name)
        result["promptTokenLimit"] = from_int(self.prompt_token_limit)
        result["systemTokens"] = from_int(self.system_tokens)
        result["toolDefinitionsTokens"] = from_int(self.tool_definitions_tokens)
        result["totalTokens"] = from_int(self.total_tokens)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataIsProcessingResult:
    """Indicates whether the local session is currently processing a turn or background
    continuation.
    """
    processing: bool
    """Whether the session is currently processing user/agent messages. False for non-local
    sessions (which don't run a local agentic loop). Reflects an in-flight turn or background
    continuation.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataIsProcessingResult:
        assert isinstance(obj, dict)
        processing = from_bool(obj.get("processing"))
        return MetadataIsProcessingResult(processing)

    def to_dict(self) -> dict:
        result: dict = {}
        result["processing"] = from_bool(self.processing)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataRecomputeContextTokensResult:
    """Re-tokenize the session's existing messages against `modelId` and return the token
    totals. Useful for hosts that want an initial estimate of context usage on session
    resume, before the next agent turn fires `session.context_info_changed` events. Returns
    zeros for an empty session.
    """
    messages_token_count: int
    """Tokens contributed by user/assistant/tool messages (excludes system/developer prompts)."""

    system_token_count: int
    """Tokens contributed by system/developer prompt snapshots."""

    total_tokens: int
    """Sum of tokens across chat-context and system-context messages currently held by the
    session.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataRecomputeContextTokensResult:
        assert isinstance(obj, dict)
        messages_token_count = from_int(obj.get("messagesTokenCount"))
        system_token_count = from_int(obj.get("systemTokenCount"))
        total_tokens = from_int(obj.get("totalTokens"))
        return MetadataRecomputeContextTokensResult(messages_token_count, system_token_count, total_tokens)

    def to_dict(self) -> dict:
        result: dict = {}
        result["messagesTokenCount"] = from_int(self.messages_token_count)
        result["systemTokenCount"] = from_int(self.system_token_count)
        result["totalTokens"] = from_int(self.total_tokens)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class HostType(Enum):
    """Hosting platform type of the repository

    Repository host type

    Repository host type, if known

    Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    """
    ADO = "ado"
    GITHUB = "github"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataRecordContextChangeResult:
    """Notify the session that its working directory context has changed. Emits a
    `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline
    UI) can react. Use this when the host has detected a cwd/branch/repo change outside the
    session's normal lifecycle (e.g., after a shell command in interactive mode).
    """
    @staticmethod
    def from_dict(obj: Any) -> MetadataRecordContextChangeResult:
        assert isinstance(obj, dict)
        return MetadataRecordContextChangeResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataSetWorkingDirectoryRequest:
    """Absolute path to set as the session's new working directory."""

    working_directory: str
    """Absolute path to set as the session's working directory. The runtime updates the
    session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry)
    anchor to it.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataSetWorkingDirectoryRequest:
        assert isinstance(obj, dict)
        working_directory = from_str(obj.get("workingDirectory"))
        return MetadataSetWorkingDirectoryRequest(working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        result["workingDirectory"] = from_str(self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataSetWorkingDirectoryResult:
    """Update the session's working directory. Used by the host when the user explicitly changes
    cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any
    related side-effects (file index, etc.); this method only updates the session's own
    recorded path.
    """
    working_directory: str
    """Working directory after the update"""

    @staticmethod
    def from_dict(obj: Any) -> MetadataSetWorkingDirectoryResult:
        assert isinstance(obj, dict)
        working_directory = from_str(obj.get("workingDirectory"))
        return MetadataSetWorkingDirectoryResult(working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        result["workingDirectory"] = from_str(self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class MetadataSnapshotCurrentMode(Enum):
    """The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')"""

    AUTOPILOT = "autopilot"
    INTERACTIVE = "interactive"
    PLAN = "plan"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataSnapshotRemoteMetadataRepository:
    """The repository the remote session targets."""

    branch: str
    """The branch the remote session is operating on."""

    name: str
    """The GitHub repository name (without owner)."""

    owner: str
    """The GitHub owner (user or organization) of the target repository."""

    @staticmethod
    def from_dict(obj: Any) -> MetadataSnapshotRemoteMetadataRepository:
        assert isinstance(obj, dict)
        branch = from_str(obj.get("branch"))
        name = from_str(obj.get("name"))
        owner = from_str(obj.get("owner"))
        return MetadataSnapshotRemoteMetadataRepository(branch, name, owner)

    def to_dict(self) -> dict:
        result: dict = {}
        result["branch"] = from_str(self.branch)
        result["name"] = from_str(self.name)
        result["owner"] = from_str(self.owner)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class MetadataSnapshotRemoteMetadataTaskType(Enum):
    """Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote`
    invocation.
    """
    CCA = "cca"
    CLI = "cli"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModeSetRequest:
    """Agent interaction mode to apply to the session."""

    mode: SessionMode
    """The session mode the agent is operating in"""

    @staticmethod
    def from_dict(obj: Any) -> ModeSetRequest:
        assert isinstance(obj, dict)
        mode = SessionMode(obj.get("mode"))
        return ModeSetRequest(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(SessionMode, self.mode)
        return result

@dataclass
class ModelBillingTokenPrices:
    """Token-level pricing information for this model"""

    batch_size: int | None = None
    """Number of tokens per standard billing batch"""

    cache_price: int | None = None
    """Price per billing batch of cached tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1
    AIU = $0.01 USD)
    """
    input_price: int | None = None
    """Price per billing batch of input tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1 AIU
    = $0.01 USD)
    """
    output_price: int | None = None
    """Price per billing batch of output tokens in nano-AIUs (1 nano-AIU = 0.000000001 AIU, 1
    AIU = $0.01 USD)
    """

    @staticmethod
    def from_dict(obj: Any) -> ModelBillingTokenPrices:
        assert isinstance(obj, dict)
        batch_size = from_union([from_int, from_none], obj.get("batchSize"))
        cache_price = from_union([from_int, from_none], obj.get("cachePrice"))
        input_price = from_union([from_int, from_none], obj.get("inputPrice"))
        output_price = from_union([from_int, from_none], obj.get("outputPrice"))
        return ModelBillingTokenPrices(batch_size, cache_price, input_price, output_price)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.batch_size is not None:
            result["batchSize"] = from_union([from_int, from_none], self.batch_size)
        if self.cache_price is not None:
            result["cachePrice"] = from_union([from_int, from_none], self.cache_price)
        if self.input_price is not None:
            result["inputPrice"] = from_union([from_int, from_none], self.input_price)
        if self.output_price is not None:
            result["outputPrice"] = from_union([from_int, from_none], self.output_price)
        return result

@dataclass
class ModelCapabilitiesLimitsVision:
    """Vision-specific limits"""

    max_prompt_image_size: int
    """Maximum image size in bytes"""

    max_prompt_images: int
    """Maximum number of images per prompt"""

    supported_media_types: list[str]
    """MIME types the model accepts"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesLimitsVision:
        assert isinstance(obj, dict)
        max_prompt_image_size = from_int(obj.get("max_prompt_image_size"))
        max_prompt_images = from_int(obj.get("max_prompt_images"))
        supported_media_types = from_list(from_str, obj.get("supported_media_types"))
        return ModelCapabilitiesLimitsVision(max_prompt_image_size, max_prompt_images, supported_media_types)

    def to_dict(self) -> dict:
        result: dict = {}
        result["max_prompt_image_size"] = from_int(self.max_prompt_image_size)
        result["max_prompt_images"] = from_int(self.max_prompt_images)
        result["supported_media_types"] = from_list(from_str, self.supported_media_types)
        return result

@dataclass
class ModelCapabilitiesSupports:
    """Feature flags indicating what the model supports"""

    reasoning_effort: bool | None = None
    """Whether this model supports reasoning effort configuration"""

    vision: bool | None = None
    """Whether this model supports vision/image input"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesSupports:
        assert isinstance(obj, dict)
        reasoning_effort = from_union([from_bool, from_none], obj.get("reasoningEffort"))
        vision = from_union([from_bool, from_none], obj.get("vision"))
        return ModelCapabilitiesSupports(reasoning_effort, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_bool, from_none], self.reasoning_effort)
        if self.vision is not None:
            result["vision"] = from_union([from_bool, from_none], self.vision)
        return result

class ModelPickerPriceCategory(Enum):
    """Relative cost tier for token-based billing users"""

    HIGH = "high"
    LOW = "low"
    MEDIUM = "medium"
    VERY_HIGH = "very_high"

class ModelPolicyState(Enum):
    """Current policy state for this model"""

    DISABLED = "disabled"
    ENABLED = "enabled"
    UNCONFIGURED = "unconfigured"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelCapabilitiesOverrideLimitsVision:
    """Vision-specific limits"""

    max_prompt_image_size: int | None = None
    """Maximum image size in bytes"""

    max_prompt_images: int | None = None
    """Maximum number of images per prompt"""

    supported_media_types: list[str] | None = None
    """MIME types the model accepts"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesOverrideLimitsVision:
        assert isinstance(obj, dict)
        max_prompt_image_size = from_union([from_int, from_none], obj.get("max_prompt_image_size"))
        max_prompt_images = from_union([from_int, from_none], obj.get("max_prompt_images"))
        supported_media_types = from_union([lambda x: from_list(from_str, x), from_none], obj.get("supported_media_types"))
        return ModelCapabilitiesOverrideLimitsVision(max_prompt_image_size, max_prompt_images, supported_media_types)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.max_prompt_image_size is not None:
            result["max_prompt_image_size"] = from_union([from_int, from_none], self.max_prompt_image_size)
        if self.max_prompt_images is not None:
            result["max_prompt_images"] = from_union([from_int, from_none], self.max_prompt_images)
        if self.supported_media_types is not None:
            result["supported_media_types"] = from_union([lambda x: from_list(from_str, x), from_none], self.supported_media_types)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelCapabilitiesOverrideSupports:
    """Feature flags indicating what the model supports"""

    reasoning_effort: bool | None = None
    """Whether this model supports reasoning effort configuration"""

    vision: bool | None = None
    """Whether this model supports vision/image input"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesOverrideSupports:
        assert isinstance(obj, dict)
        reasoning_effort = from_union([from_bool, from_none], obj.get("reasoningEffort"))
        vision = from_union([from_bool, from_none], obj.get("vision"))
        return ModelCapabilitiesOverrideSupports(reasoning_effort, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_bool, from_none], self.reasoning_effort)
        if self.vision is not None:
            result["vision"] = from_union([from_bool, from_none], self.vision)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelSetReasoningEffortRequest:
    """Reasoning effort level to apply to the currently selected model."""

    reasoning_effort: str
    """Reasoning effort level to apply to the currently selected model. The host is responsible
    for validating the value against the model's supported levels before calling.
    """

    @staticmethod
    def from_dict(obj: Any) -> ModelSetReasoningEffortRequest:
        assert isinstance(obj, dict)
        reasoning_effort = from_str(obj.get("reasoningEffort"))
        return ModelSetReasoningEffortRequest(reasoning_effort)

    def to_dict(self) -> dict:
        result: dict = {}
        result["reasoningEffort"] = from_str(self.reasoning_effort)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelSetReasoningEffortResult:
    """Update the session's reasoning effort without changing the selected model. Use `switchTo`
    instead when you also need to change the model. The runtime stores the effort on the
    session and applies it to subsequent turns.
    """
    reasoning_effort: str
    """Reasoning effort level recorded on the session after the update"""

    @staticmethod
    def from_dict(obj: Any) -> ModelSetReasoningEffortResult:
        assert isinstance(obj, dict)
        reasoning_effort = from_str(obj.get("reasoningEffort"))
        return ModelSetReasoningEffortResult(reasoning_effort)

    def to_dict(self) -> dict:
        result: dict = {}
        result["reasoningEffort"] = from_str(self.reasoning_effort)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelSwitchToResult:
    """The model identifier active on the session after the switch."""

    model_id: str | None = None
    """Currently active model identifier after the switch"""

    @staticmethod
    def from_dict(obj: Any) -> ModelSwitchToResult:
        assert isinstance(obj, dict)
        model_id = from_union([from_str, from_none], obj.get("modelId"))
        return ModelSwitchToResult(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model_id is not None:
            result["modelId"] = from_union([from_str, from_none], self.model_id)
        return result

@dataclass
class ModelsListRequest:
    git_hub_token: str | None = None
    """GitHub token for per-user model listing. When provided, resolves this token to determine
    the user's Copilot plan and available models instead of using the global auth.
    """

    @staticmethod
    def from_dict(obj: Any) -> ModelsListRequest:
        assert isinstance(obj, dict)
        git_hub_token = from_union([from_str, from_none], obj.get("gitHubToken"))
        return ModelsListRequest(git_hub_token)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.git_hub_token is not None:
            result["gitHubToken"] = from_union([from_str, from_none], self.git_hub_token)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class NameGetResult:
    """The session's friendly name, or null when not yet set."""

    name: str | None = None
    """The session name (user-set or auto-generated), or null if not yet set"""

    @staticmethod
    def from_dict(obj: Any) -> NameGetResult:
        assert isinstance(obj, dict)
        name = from_union([from_none, from_str], obj.get("name"))
        return NameGetResult(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_union([from_none, from_str], self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class NameSetAutoRequest:
    """Auto-generated session summary to apply as the session's name when no user-set name
    exists.
    """
    summary: str
    """Auto-generated session summary. Empty/whitespace-only values are ignored; values are
    trimmed before persisting.
    """

    @staticmethod
    def from_dict(obj: Any) -> NameSetAutoRequest:
        assert isinstance(obj, dict)
        summary = from_str(obj.get("summary"))
        return NameSetAutoRequest(summary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["summary"] = from_str(self.summary)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class NameSetAutoResult:
    """Indicates whether the auto-generated summary was applied as the session's name."""

    applied: bool
    """Whether the auto-generated summary was persisted. False if the session already has a
    user-set name, the summary normalized to empty, or the session does not have a workspace.
    """

    @staticmethod
    def from_dict(obj: Any) -> NameSetAutoResult:
        assert isinstance(obj, dict)
        applied = from_bool(obj.get("applied"))
        return NameSetAutoResult(applied)

    def to_dict(self) -> dict:
        result: dict = {}
        result["applied"] = from_bool(self.applied)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class NameSetRequest:
    """New friendly name to apply to the session."""

    name: str
    """New session name (1–100 characters, trimmed of leading/trailing whitespace)"""

    @staticmethod
    def from_dict(obj: Any) -> NameSetRequest:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return NameSetRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PendingPermissionRequest:
    """Schema for the `PendingPermissionRequest` type."""

    request: PermissionPromptRequest
    """The user-facing permission prompt details (commands, write, read, mcp, url, memory,
    custom-tool, path, hook)
    """
    request_id: str
    """Unique identifier for the pending permission request"""

    @staticmethod
    def from_dict(obj: Any) -> PendingPermissionRequest:
        assert isinstance(obj, dict)
        request = PermissionPromptRequest.from_dict(obj.get("request"))
        request_id = from_str(obj.get("requestId"))
        return PendingPermissionRequest(request, request_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["request"] = to_class(PermissionPromptRequest, self.request)
        result["requestId"] = from_str(self.request_id)
        return result

class ApprovalKind(Enum):
    COMMANDS = "commands"
    CUSTOM_TOOL = "custom-tool"
    EXTENSION_MANAGEMENT = "extension-management"
    EXTENSION_PERMISSION_ACCESS = "extension-permission-access"
    MCP = "mcp"
    MCP_SAMPLING = "mcp-sampling"
    MEMORY = "memory"
    READ = "read"
    WRITE = "write"

class PermissionDecisionKind(Enum):
    APPROVED = "approved"
    APPROVED_FOR_LOCATION = "approved-for-location"
    APPROVED_FOR_SESSION = "approved-for-session"
    APPROVE_FOR_LOCATION = "approve-for-location"
    APPROVE_FOR_SESSION = "approve-for-session"
    APPROVE_ONCE = "approve-once"
    APPROVE_PERMANENTLY = "approve-permanently"
    CANCELLED = "cancelled"
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"
    DENIED_BY_PERMISSION_REQUEST_HOOK = "denied-by-permission-request-hook"
    DENIED_BY_RULES = "denied-by-rules"
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"
    REJECT = "reject"
    USER_NOT_AVAILABLE = "user-not-available"

class PermissionDecisionApproveForLocationKind(Enum):
    APPROVE_FOR_LOCATION = "approve-for-location"

class PermissionDecisionApproveForLocationApprovalCommandsKind(Enum):
    COMMANDS = "commands"

class PermissionDecisionApproveForLocationApprovalCustomToolKind(Enum):
    CUSTOM_TOOL = "custom-tool"

class PermissionDecisionApproveForLocationApprovalExtensionManagementKind(Enum):
    EXTENSION_MANAGEMENT = "extension-management"

class PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind(Enum):
    EXTENSION_PERMISSION_ACCESS = "extension-permission-access"

class PermissionDecisionApproveForLocationApprovalMCPKind(Enum):
    MCP = "mcp"

class PermissionDecisionApproveForLocationApprovalMCPSamplingKind(Enum):
    MCP_SAMPLING = "mcp-sampling"

class PermissionDecisionApproveForLocationApprovalMemoryKind(Enum):
    MEMORY = "memory"

class PermissionDecisionApproveForLocationApprovalReadKind(Enum):
    READ = "read"

class PermissionDecisionApproveForLocationApprovalWriteKind(Enum):
    WRITE = "write"

class PermissionDecisionApproveForSessionKind(Enum):
    APPROVE_FOR_SESSION = "approve-for-session"

class PermissionDecisionApproveOnceKind(Enum):
    APPROVE_ONCE = "approve-once"

class PermissionDecisionApprovePermanentlyKind(Enum):
    APPROVE_PERMANENTLY = "approve-permanently"

class PermissionDecisionApprovedKind(Enum):
    APPROVED = "approved"

class PermissionDecisionApprovedForLocationKind(Enum):
    APPROVED_FOR_LOCATION = "approved-for-location"

class PermissionDecisionApprovedForSessionKind(Enum):
    APPROVED_FOR_SESSION = "approved-for-session"

class PermissionDecisionCancelledKind(Enum):
    CANCELLED = "cancelled"

class PermissionDecisionDeniedByContentExclusionPolicyKind(Enum):
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"

class PermissionDecisionDeniedByPermissionRequestHookKind(Enum):
    DENIED_BY_PERMISSION_REQUEST_HOOK = "denied-by-permission-request-hook"

class PermissionDecisionDeniedByRulesKind(Enum):
    DENIED_BY_RULES = "denied-by-rules"

class PermissionDecisionDeniedInteractivelyByUserKind(Enum):
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"

class PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind(Enum):
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"

class PermissionDecisionRejectKind(Enum):
    REJECT = "reject"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionRequest:
    """Pending permission request ID and the decision to apply (approve/reject and scope)."""

    request_id: str
    """Request ID of the pending permission request"""

    result: PermissionDecision
    """The client's response to the pending permission prompt"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = _load_PermissionDecision(obj.get("result"))
        return PermissionDecisionRequest(request_id, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = (self.result).to_dict()
        return result

class PermissionDecisionUserNotAvailableKind(Enum):
    USER_NOT_AVAILABLE = "user-not-available"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionLocationApplyParams:
    """Working directory to load persisted location permissions for."""

    working_directory: str
    """Working directory whose persisted location permissions should be applied"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionLocationApplyParams:
        assert isinstance(obj, dict)
        working_directory = from_str(obj.get("workingDirectory"))
        return PermissionLocationApplyParams(working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        result["workingDirectory"] = from_str(self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class PermissionLocationType(Enum):
    """Whether the location is a git repo or directory"""

    DIR = "dir"
    REPO = "repo"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionLocationResolveParams:
    """Working directory to resolve into a location-permissions key."""

    working_directory: str
    """Working directory whose permission location should be resolved"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionLocationResolveParams:
        assert isinstance(obj, dict)
        working_directory = from_str(obj.get("workingDirectory"))
        return PermissionLocationResolveParams(working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        result["workingDirectory"] = from_str(self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsAddParams:
    """Directory path to add to the session's allowed directories."""

    path: str
    """Directory to add to the allow-list. The runtime resolves and validates the path before
    adding.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsAddParams:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return PermissionPathsAddParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsAllowedCheckParams:
    """Path to evaluate against the session's allowed directories."""

    path: str
    """Path to check against the session's allowed directories"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsAllowedCheckParams:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return PermissionPathsAllowedCheckParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsAllowedCheckResult:
    """Indicates whether the supplied path is within the session's allowed directories."""

    allowed: bool
    """Whether the path is within the session's allowed directories"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsAllowedCheckResult:
        assert isinstance(obj, dict)
        allowed = from_bool(obj.get("allowed"))
        return PermissionPathsAllowedCheckResult(allowed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["allowed"] = from_bool(self.allowed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsList:
    """Snapshot of the session's allow-listed directories and primary working directory."""

    directories: list[str]
    """All directories currently allowed for tool access on this session."""

    primary: str
    """The primary working directory for this session."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsList:
        assert isinstance(obj, dict)
        directories = from_list(from_str, obj.get("directories"))
        primary = from_str(obj.get("primary"))
        return PermissionPathsList(directories, primary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["directories"] = from_list(from_str, self.directories)
        result["primary"] = from_str(self.primary)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsUpdatePrimaryParams:
    """Directory path to set as the session's new primary working directory."""

    path: str
    """Directory to set as the new primary working directory for the session's permission policy."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsUpdatePrimaryParams:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return PermissionPathsUpdatePrimaryParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsWorkspaceCheckParams:
    """Path to evaluate against the session's workspace (primary) directory."""

    path: str
    """Path to check against the session workspace directory"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsWorkspaceCheckParams:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return PermissionPathsWorkspaceCheckParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsWorkspaceCheckResult:
    """Indicates whether the supplied path is within the session's workspace directory."""

    allowed: bool
    """Whether the path is within the session workspace directory"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsWorkspaceCheckResult:
        assert isinstance(obj, dict)
        allowed = from_bool(obj.get("allowed"))
        return PermissionPathsWorkspaceCheckResult(allowed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["allowed"] = from_bool(self.allowed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPromptShownNotification:
    """Notification payload describing the permission prompt that the client just rendered."""

    message: str
    """Human-readable description of the prompt the user is being asked to approve. Used by the
    runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell,
    desktop notification).
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionPromptShownNotification:
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        return PermissionPromptShownNotification(message)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionRequestResult:
    """Indicates whether the permission decision was applied; false when the request was already
    resolved.
    """
    success: bool
    """Whether the permission request was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionRequestResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionRequestResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionRulesSet:
    """If specified, replaces the session's approved/denied permission rules. Omit to leave the
    current rules unchanged.
    """
    approved: list[PermissionRule]
    """Rules that auto-approve matching requests"""

    denied: list[PermissionRule]
    """Rules that auto-deny matching requests"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionRulesSet:
        assert isinstance(obj, dict)
        approved = from_list(PermissionRule.from_dict, obj.get("approved"))
        denied = from_list(PermissionRule.from_dict, obj.get("denied"))
        return PermissionRulesSet(approved, denied)

    def to_dict(self) -> dict:
        result: dict = {}
        result["approved"] = from_list(lambda x: to_class(PermissionRule, x), self.approved)
        result["denied"] = from_list(lambda x: to_class(PermissionRule, x), self.denied)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionUrlsConfig:
    """If specified, replaces the session's URL-permission policy. The runtime constructs a
    fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy
    unchanged.
    """
    initial_allowed: list[str] | None = None
    """Initial list of allowed URL/domain patterns. Patterns may include path components.
    Ignored when `unrestricted` is true.
    """
    unrestricted: bool | None = None
    """If true, the runtime allows access to all URLs without prompting. Initial allow-list is
    ignored when this is true.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionUrlsConfig:
        assert isinstance(obj, dict)
        initial_allowed = from_union([lambda x: from_list(from_str, x), from_none], obj.get("initialAllowed"))
        unrestricted = from_union([from_bool, from_none], obj.get("unrestricted"))
        return PermissionUrlsConfig(initial_allowed, unrestricted)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.initial_allowed is not None:
            result["initialAllowed"] = from_union([lambda x: from_list(from_str, x), from_none], self.initial_allowed)
        if self.unrestricted is not None:
            result["unrestricted"] = from_union([from_bool, from_none], self.unrestricted)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionUrlsSetUnrestrictedModeParams:
    """Whether the URL-permission policy should run in unrestricted mode."""

    enabled: bool
    """Whether to allow access to all URLs without prompting. Toggles the runtime's
    URL-permission policy in place.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionUrlsSetUnrestrictedModeParams:
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        return PermissionUrlsSetUnrestrictedModeParams(enabled)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsConfigureAdditionalContentExclusionPolicyRuleSource:
    """Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type."""

    name: str
    type: str

    @staticmethod
    def from_dict(obj: Any) -> PermissionsConfigureAdditionalContentExclusionPolicyRuleSource:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        type = from_str(obj.get("type"))
        return PermissionsConfigureAdditionalContentExclusionPolicyRuleSource(name, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["type"] = from_str(self.type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class PermissionsConfigureAdditionalContentExclusionPolicyScope(Enum):
    """Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope`
    enumeration.
    """
    ALL = "all"
    REPO = "repo"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsConfigureResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsConfigureResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsConfigureResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsFolderTrustAddTrustedResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsFolderTrustAddTrustedResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsFolderTrustAddTrustedResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsLocationsAddToolApprovalResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class PermissionsModifyRulesScope(Enum):
    """Whether the change applies to ephemeral session-scoped rules (cleared at session end) or
    to location-scoped rules persisted via the location-permissions config file.
    """
    LOCATION = "location"
    SESSION = "session"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsModifyRulesResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsModifyRulesResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsModifyRulesResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsNotifyPromptShownResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsNotifyPromptShownResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsNotifyPromptShownResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsPathsAddResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsPathsAddResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsPathsAddResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsPathsListRequest:
    """No parameters; returns the session's allow-listed directories."""
    @staticmethod
    def from_dict(obj: Any) -> PermissionsPathsListRequest:
        assert isinstance(obj, dict)
        return PermissionsPathsListRequest()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsPathsUpdatePrimaryResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsPathsUpdatePrimaryResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsPathsUpdatePrimaryResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsPendingRequestsRequest:
    """No parameters; returns currently-pending permission requests for the session."""
    @staticmethod
    def from_dict(obj: Any) -> PermissionsPendingRequestsRequest:
        assert isinstance(obj, dict)
        return PermissionsPendingRequestsRequest()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsResetSessionApprovalsRequest:
    """No parameters; clears all session-scoped tool permission approvals."""
    @staticmethod
    def from_dict(obj: Any) -> PermissionsResetSessionApprovalsRequest:
        assert isinstance(obj, dict)
        return PermissionsResetSessionApprovalsRequest()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsResetSessionApprovalsResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsResetSessionApprovalsResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsResetSessionApprovalsResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsSetApproveAllResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsSetApproveAllResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsSetApproveAllResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsSetRequiredRequest:
    """Toggles whether permission prompts should be bridged into session events for this client."""

    required: bool
    """Whether the client wants `permission.requested` events bridged from the session-owned
    permission service. CLI clients that render prompt UI set this to `true` for as long as
    their listener is mounted; headless callers leave it unset (the default is `false`).
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionsSetRequiredRequest:
        assert isinstance(obj, dict)
        required = from_bool(obj.get("required"))
        return PermissionsSetRequiredRequest(required)

    def to_dict(self) -> dict:
        result: dict = {}
        result["required"] = from_bool(self.required)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsSetRequiredResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsSetRequiredResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsSetRequiredResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsUrlsSetUnrestrictedModeResult:
    """Indicates whether the operation succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsUrlsSetUnrestrictedModeResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionsUrlsSetUnrestrictedModeResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

@dataclass
class PingRequest:
    """Optional message to echo back to the caller."""

    message: str | None = None
    """Optional message to echo back"""

    @staticmethod
    def from_dict(obj: Any) -> PingRequest:
        assert isinstance(obj, dict)
        message = from_union([from_str, from_none], obj.get("message"))
        return PingRequest(message)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        return result

@dataclass
class PingResult:
    """Server liveness response, including the echoed message, current server timestamp, and
    protocol version.
    """
    message: str
    """Echoed message (or default greeting)"""

    protocol_version: int
    """Server protocol version number"""

    timestamp: datetime
    """ISO 8601 timestamp when the server handled the ping"""

    @staticmethod
    def from_dict(obj: Any) -> PingResult:
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        protocol_version = from_int(obj.get("protocolVersion"))
        timestamp = from_datetime(obj.get("timestamp"))
        return PingResult(message, protocol_version, timestamp)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["protocolVersion"] = from_int(self.protocol_version)
        result["timestamp"] = self.timestamp.isoformat()
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PlanReadResult:
    """Existence, contents, and resolved path of the session plan file."""

    exists: bool
    """Whether the plan file exists in the workspace"""

    content: str | None = None
    """The content of the plan file, or null if it does not exist"""

    path: str | None = None
    """Absolute file path of the plan file, or null if workspace is not enabled"""

    @staticmethod
    def from_dict(obj: Any) -> PlanReadResult:
        assert isinstance(obj, dict)
        exists = from_bool(obj.get("exists"))
        content = from_union([from_none, from_str], obj.get("content"))
        path = from_union([from_none, from_str], obj.get("path"))
        return PlanReadResult(exists, content, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["exists"] = from_bool(self.exists)
        result["content"] = from_union([from_none, from_str], self.content)
        result["path"] = from_union([from_none, from_str], self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PlanUpdateRequest:
    """Replacement contents to write to the session plan file."""

    content: str
    """The new content for the plan file"""

    @staticmethod
    def from_dict(obj: Any) -> PlanUpdateRequest:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return PlanUpdateRequest(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class Plugin:
    """Schema for the `Plugin` type."""

    enabled: bool
    """Whether the plugin is currently enabled"""

    marketplace: str
    """Marketplace the plugin came from"""

    name: str
    """Plugin name"""

    version: str | None = None
    """Installed version"""

    @staticmethod
    def from_dict(obj: Any) -> Plugin:
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        marketplace = from_str(obj.get("marketplace"))
        name = from_str(obj.get("name"))
        version = from_union([from_str, from_none], obj.get("version"))
        return Plugin(enabled, marketplace, name, version)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        result["marketplace"] = from_str(self.marketplace)
        result["name"] = from_str(self.name)
        if self.version is not None:
            result["version"] = from_union([from_str, from_none], self.version)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class QueuePendingItemsKind(Enum):
    """Whether this item is a queued user message or a queued slash command / model change"""

    COMMAND = "command"
    MESSAGE = "message"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class QueueRemoveMostRecentResult:
    """Indicates whether a user-facing pending item was removed."""

    removed: bool
    """True if a user-facing pending item was removed (LIFO across both queues); false when no
    removable items remained.
    """

    @staticmethod
    def from_dict(obj: Any) -> QueueRemoveMostRecentResult:
        assert isinstance(obj, dict)
        removed = from_bool(obj.get("removed"))
        return QueueRemoveMostRecentResult(removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["removed"] = from_bool(self.removed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class QueuedCommandHandled:
    """Schema for the `QueuedCommandHandled` type."""

    handled: ClassVar[str] = "true"
    """The host actually executed the queued command."""

    stop_processing_queue: bool | None = None
    """When true, the runtime will not process subsequent queued commands until a new request
    comes in.
    """

    @staticmethod
    def from_dict(obj: Any) -> QueuedCommandHandled:
        assert isinstance(obj, dict)
        stop_processing_queue = from_union([from_bool, from_none], obj.get("stopProcessingQueue"))
        return QueuedCommandHandled(stop_processing_queue)

    def to_dict(self) -> dict:
        result: dict = {}
        result["handled"] = self.handled
        if self.stop_processing_queue is not None:
            result["stopProcessingQueue"] = from_union([from_bool, from_none], self.stop_processing_queue)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class QueuedCommandNotHandled:
    """Schema for the `QueuedCommandNotHandled` type."""

    handled: ClassVar[str] = "false"
    """The host did not execute the queued command. Unblocks the queue without claiming the
    command was processed (e.g. when the handler threw before completing).
    """

    @staticmethod
    def from_dict(obj: Any) -> QueuedCommandNotHandled:
        assert isinstance(obj, dict)
        return QueuedCommandNotHandled()

    def to_dict(self) -> dict:
        result: dict = {}
        result["handled"] = self.handled
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RegisterEventInterestParams:
    """Event type to register consumer interest for, used by runtime gating logic."""

    event_type: str
    """The event type the consumer wants the runtime to treat as 'observed' for
    behavior-switching gating. Some runtime code paths inspect whether any consumer is
    interested in a specific event type and choose a different implementation accordingly
    (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates the full
    interactive OAuth flow to the consumer; when no interest is registered the runtime
    installs a browserless fallback that silently reuses cached tokens). SDK clients that
    long-poll events do NOT automatically appear as listeners to these gating checks — they
    must explicitly call `registerInterest` for each event type they want the runtime to
    count as having a consumer. Multiple registrations for the same event type from the same
    or different consumers are tracked independently and must each be released. See:
    `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`,
    `user_input.requested`, `elicitation.requested`, `command.queued`,
    `exit_plan_mode.requested`.
    """

    @staticmethod
    def from_dict(obj: Any) -> RegisterEventInterestParams:
        assert isinstance(obj, dict)
        event_type = from_str(obj.get("eventType"))
        return RegisterEventInterestParams(event_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventType"] = from_str(self.event_type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RegisterEventInterestResult:
    """Opaque handle representing an event-type interest registration."""

    handle: str
    """Opaque handle for this registration. Pass to releaseInterest to release. Each call to
    registerInterest produces a fresh handle, even when the same eventType is registered
    multiple times.
    """

    @staticmethod
    def from_dict(obj: Any) -> RegisterEventInterestResult:
        assert isinstance(obj, dict)
        handle = from_str(obj.get("handle"))
        return RegisterEventInterestResult(handle)

    def to_dict(self) -> dict:
        result: dict = {}
        result["handle"] = from_str(self.handle)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ReleaseEventInterestParams:
    """Opaque handle previously returned by `registerInterest` to release."""

    handle: str
    """Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown
    or already-released handle is a no-op (returns success). When the last outstanding handle
    for an event type is released, the runtime reverts to its 'no consumer' code path for
    that event type.
    """

    @staticmethod
    def from_dict(obj: Any) -> ReleaseEventInterestParams:
        assert isinstance(obj, dict)
        handle = from_str(obj.get("handle"))
        return ReleaseEventInterestParams(handle)

    def to_dict(self) -> dict:
        result: dict = {}
        result["handle"] = from_str(self.handle)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class RemoteSessionMode(Enum):
    """Per-session remote mode. "off" disables remote, "export" exports session events to GitHub
    without enabling remote steering, "on" enables both export and remote steering.
    """
    EXPORT = "export"
    OFF = "off"
    ON = "on"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RemoteEnableResult:
    """GitHub URL for the session and a flag indicating whether remote steering is enabled."""

    remote_steerable: bool
    """Whether remote steering is enabled"""

    url: str | None = None
    """GitHub frontend URL for this session"""

    @staticmethod
    def from_dict(obj: Any) -> RemoteEnableResult:
        assert isinstance(obj, dict)
        remote_steerable = from_bool(obj.get("remoteSteerable"))
        url = from_union([from_str, from_none], obj.get("url"))
        return RemoteEnableResult(remote_steerable, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["remoteSteerable"] = from_bool(self.remote_steerable)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RemoteNotifySteerableChangedRequest:
    """New remote-steerability state to persist as a `session.remote_steerable_changed` event."""

    remote_steerable: bool
    """Whether the session now supports remote steering via GitHub. The runtime persists this as
    a `session.remote_steerable_changed` event so resume/replay sees the up-to-date
    capability.
    """

    @staticmethod
    def from_dict(obj: Any) -> RemoteNotifySteerableChangedRequest:
        assert isinstance(obj, dict)
        remote_steerable = from_bool(obj.get("remoteSteerable"))
        return RemoteNotifySteerableChangedRequest(remote_steerable)

    def to_dict(self) -> dict:
        result: dict = {}
        result["remoteSteerable"] = from_bool(self.remote_steerable)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RemoteNotifySteerableChangedResult:
    """Persist a steerability change as a `session.remote_steerable_changed` event. Used by the
    host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a
    remote exporter that the runtime does not directly own.
    """
    @staticmethod
    def from_dict(obj: Any) -> RemoteNotifySteerableChangedResult:
        assert isinstance(obj, dict)
        return RemoteNotifySteerableChangedResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ScheduleEntry:
    """Schema for the `ScheduleEntry` type.

    The removed entry, or omitted if no entry matched.
    """
    id: int
    """Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt
    from the event log).
    """
    interval_ms: int
    """Interval between scheduled ticks, in milliseconds."""

    next_run_at: datetime
    """ISO 8601 timestamp when the next tick is scheduled to fire."""

    prompt: str
    """Prompt text that gets enqueued on every tick."""

    recurring: bool
    """Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`)."""

    display_prompt: str | None = None
    """Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a
    skill-invocation schedule). The actual enqueued prompt is `prompt`.
    """

    @staticmethod
    def from_dict(obj: Any) -> ScheduleEntry:
        assert isinstance(obj, dict)
        id = from_int(obj.get("id"))
        interval_ms = from_int(obj.get("intervalMs"))
        next_run_at = from_datetime(obj.get("nextRunAt"))
        prompt = from_str(obj.get("prompt"))
        recurring = from_bool(obj.get("recurring"))
        display_prompt = from_union([from_str, from_none], obj.get("displayPrompt"))
        return ScheduleEntry(id, interval_ms, next_run_at, prompt, recurring, display_prompt)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_int(self.id)
        result["intervalMs"] = from_int(self.interval_ms)
        result["nextRunAt"] = self.next_run_at.isoformat()
        result["prompt"] = from_str(self.prompt)
        result["recurring"] = from_bool(self.recurring)
        if self.display_prompt is not None:
            result["displayPrompt"] = from_union([from_str, from_none], self.display_prompt)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ScheduleStopRequest:
    """Identifier of the scheduled prompt to remove."""

    id: int
    """Id of the scheduled prompt to remove."""

    @staticmethod
    def from_dict(obj: Any) -> ScheduleStopRequest:
        assert isinstance(obj, dict)
        id = from_int(obj.get("id"))
        return ScheduleStopRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_int(self.id)
        return result

@dataclass
class SecretsAddFilterValuesRequest:
    """Secret values to add to the redaction filter."""

    values: list[str]
    """Raw secret values to register for redaction"""

    @staticmethod
    def from_dict(obj: Any) -> SecretsAddFilterValuesRequest:
        assert isinstance(obj, dict)
        values = from_list(from_str, obj.get("values"))
        return SecretsAddFilterValuesRequest(values)

    def to_dict(self) -> dict:
        result: dict = {}
        result["values"] = from_list(from_str, self.values)
        return result

@dataclass
class SecretsAddFilterValuesResult:
    """Confirmation that the secret values were registered."""

    ok: bool
    """Whether the values were successfully registered"""

    @staticmethod
    def from_dict(obj: Any) -> SecretsAddFilterValuesResult:
        assert isinstance(obj, dict)
        ok = from_bool(obj.get("ok"))
        return SecretsAddFilterValuesResult(ok)

    def to_dict(self) -> dict:
        result: dict = {}
        result["ok"] = from_bool(self.ok)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class SendAgentMode(Enum):
    """The UI mode the agent was in when this message was sent. Defaults to the session's
    current mode.
    """
    AUTOPILOT = "autopilot"
    INTERACTIVE = "interactive"
    PLAN = "plan"
    SHELL = "shell"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentFileLineRange:
    """Optional line range to scope the attachment to a specific section of the file"""

    end: int
    """End line number (1-based, inclusive)"""

    start: int
    """Start line number (1-based)"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentFileLineRange:
        assert isinstance(obj, dict)
        end = from_int(obj.get("end"))
        start = from_int(obj.get("start"))
        return SendAttachmentFileLineRange(end, start)

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = from_int(self.end)
        result["start"] = from_int(self.start)
        return result

class SendAttachmentGithubReferenceTypeEnum(Enum):
    """Type of GitHub reference"""

    DISCUSSION = "discussion"
    ISSUE = "issue"
    PR = "pr"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentSelectionDetailsEnd:
    """End position of the selection"""

    character: int
    """End character offset within the line (0-based)"""

    line: int
    """End line number (0-based)"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentSelectionDetailsEnd:
        assert isinstance(obj, dict)
        character = from_int(obj.get("character"))
        line = from_int(obj.get("line"))
        return SendAttachmentSelectionDetailsEnd(character, line)

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = from_int(self.character)
        result["line"] = from_int(self.line)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentSelectionDetailsStart:
    """Start position of the selection"""

    character: int
    """Start character offset within the line (0-based)"""

    line: int
    """Start line number (0-based)"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentSelectionDetailsStart:
        assert isinstance(obj, dict)
        character = from_int(obj.get("character"))
        line = from_int(obj.get("line"))
        return SendAttachmentSelectionDetailsStart(character, line)

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = from_int(self.character)
        result["line"] = from_int(self.line)
        return result

class SendAttachmentType(Enum):
    BLOB = "blob"
    DIRECTORY = "directory"
    FILE = "file"
    GITHUB_REFERENCE = "github_reference"
    SELECTION = "selection"

class SendAttachmentBlobType(Enum):
    BLOB = "blob"

class SendAttachmentFileType(Enum):
    FILE = "file"

# Experimental: this type is part of an experimental API and may change or be removed.
class SendAttachmentGithubReferenceType(Enum):
    GITHUB_REFERENCE = "github_reference"

class SendAttachmentSelectionType(Enum):
    SELECTION = "selection"

# Experimental: this type is part of an experimental API and may change or be removed.
class SendMode(Enum):
    """How to deliver the message. `enqueue` (default) appends to the message queue. `immediate`
    interjects during an in-progress turn.
    """
    ENQUEUE = "enqueue"
    IMMEDIATE = "immediate"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendResult:
    """Result of sending a user message"""

    message_id: str
    """Unique identifier assigned to the message"""

    @staticmethod
    def from_dict(obj: Any) -> SendResult:
        assert isinstance(obj, dict)
        message_id = from_str(obj.get("messageId"))
        return SendResult(message_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["messageId"] = from_str(self.message_id)
        return result

@dataclass
class ServerSkill:
    """Schema for the `ServerSkill` type."""

    description: str
    """Description of what the skill does"""

    enabled: bool
    """Whether the skill is currently enabled (based on global config)"""

    name: str
    """Unique identifier for the skill"""

    source: SkillSource
    """Source location type (e.g., project, personal-copilot, plugin, builtin)"""

    user_invocable: bool
    """Whether the skill can be invoked by the user as a slash command"""

    path: str | None = None
    """Absolute path to the skill file"""

    project_path: str | None = None
    """The project path this skill belongs to (only for project/inherited skills)"""

    @staticmethod
    def from_dict(obj: Any) -> ServerSkill:
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = SkillSource(obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        path = from_union([from_str, from_none], obj.get("path"))
        project_path = from_union([from_str, from_none], obj.get("projectPath"))
        return ServerSkill(description, enabled, name, source, user_invocable, path, project_path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(SkillSource, self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.project_path is not None:
            result["projectPath"] = from_union([from_str, from_none], self.project_path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionBulkDeleteResult:
    """Map of sessionId -> bytes freed by removing the session's workspace directory."""

    freed_bytes: dict[str, int]
    """Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions
    whose deletion failed are omitted from this map (failures are logged on the server but
    not surfaced per-id; check the map for absent IDs to detect them).
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionBulkDeleteResult:
        assert isinstance(obj, dict)
        freed_bytes = from_dict(from_int, obj.get("freedBytes"))
        return SessionBulkDeleteResult(freed_bytes)

    def to_dict(self) -> dict:
        result: dict = {}
        result["freedBytes"] = from_dict(from_int, self.freed_bytes)
        return result

@dataclass
class SessionFSAppendFileRequest:
    """File path, content to append, and optional mode for the client-provided session
    filesystem.
    """
    content: str
    """Content to append"""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    mode: int | None = None
    """Optional POSIX-style mode for newly created files"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSAppendFileRequest:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        mode = from_union([from_int, from_none], obj.get("mode"))
        return SessionFSAppendFileRequest(content, path, session_id, mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        if self.mode is not None:
            result["mode"] = from_union([from_int, from_none], self.mode)
        return result

class SessionFSErrorCode(Enum):
    """Error classification"""

    ENOENT = "ENOENT"
    UNKNOWN = "UNKNOWN"

@dataclass
class SessionFSExistsRequest:
    """Path to test for existence in the client-provided session filesystem."""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSExistsRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        return SessionFSExistsRequest(path, session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        return result

@dataclass
class SessionFSExistsResult:
    """Indicates whether the requested path exists in the client-provided session filesystem."""

    exists: bool
    """Whether the path exists"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSExistsResult:
        assert isinstance(obj, dict)
        exists = from_bool(obj.get("exists"))
        return SessionFSExistsResult(exists)

    def to_dict(self) -> dict:
        result: dict = {}
        result["exists"] = from_bool(self.exists)
        return result

@dataclass
class SessionFSMkdirRequest:
    """Directory path to create in the client-provided session filesystem, with options for
    recursive creation and POSIX mode.
    """
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    mode: int | None = None
    """Optional POSIX-style mode for newly created directories"""

    recursive: bool | None = None
    """Create parent directories as needed"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSMkdirRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        mode = from_union([from_int, from_none], obj.get("mode"))
        recursive = from_union([from_bool, from_none], obj.get("recursive"))
        return SessionFSMkdirRequest(path, session_id, mode, recursive)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        if self.mode is not None:
            result["mode"] = from_union([from_int, from_none], self.mode)
        if self.recursive is not None:
            result["recursive"] = from_union([from_bool, from_none], self.recursive)
        return result

@dataclass
class SessionFSReadFileRequest:
    """Path of the file to read from the client-provided session filesystem."""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReadFileRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        return SessionFSReadFileRequest(path, session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        return result

@dataclass
class SessionFSReaddirRequest:
    """Directory path whose entries should be listed from the client-provided session filesystem."""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReaddirRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        return SessionFSReaddirRequest(path, session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        return result

class SessionFSReaddirWithTypesEntryType(Enum):
    """Entry type"""

    DIRECTORY = "directory"
    FILE = "file"

@dataclass
class SessionFSReaddirWithTypesRequest:
    """Directory path whose entries (with type information) should be listed from the
    client-provided session filesystem.
    """
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReaddirWithTypesRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        return SessionFSReaddirWithTypesRequest(path, session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        return result

@dataclass
class SessionFSRenameRequest:
    """Source and destination paths for renaming or moving an entry in the client-provided
    session filesystem.
    """
    dest: str
    """Destination path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    src: str
    """Source path using SessionFs conventions"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSRenameRequest:
        assert isinstance(obj, dict)
        dest = from_str(obj.get("dest"))
        session_id = from_str(obj.get("sessionId"))
        src = from_str(obj.get("src"))
        return SessionFSRenameRequest(dest, session_id, src)

    def to_dict(self) -> dict:
        result: dict = {}
        result["dest"] = from_str(self.dest)
        result["sessionId"] = from_str(self.session_id)
        result["src"] = from_str(self.src)
        return result

@dataclass
class SessionFSRmRequest:
    """Path to remove from the client-provided session filesystem, with options for recursive
    removal and force.
    """
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    force: bool | None = None
    """Ignore errors if the path does not exist"""

    recursive: bool | None = None
    """Remove directories and their contents recursively"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSRmRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        force = from_union([from_bool, from_none], obj.get("force"))
        recursive = from_union([from_bool, from_none], obj.get("recursive"))
        return SessionFSRmRequest(path, session_id, force, recursive)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        if self.force is not None:
            result["force"] = from_union([from_bool, from_none], self.force)
        if self.recursive is not None:
            result["recursive"] = from_union([from_bool, from_none], self.recursive)
        return result

@dataclass
class SessionFSSetProviderCapabilities:
    """Optional capabilities declared by the provider"""

    sqlite: bool | None = None
    """Whether the provider supports SQLite query/exists operations"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSetProviderCapabilities:
        assert isinstance(obj, dict)
        sqlite = from_union([from_bool, from_none], obj.get("sqlite"))
        return SessionFSSetProviderCapabilities(sqlite)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.sqlite is not None:
            result["sqlite"] = from_union([from_bool, from_none], self.sqlite)
        return result

class SessionFSSetProviderConventions(Enum):
    """Path conventions used by this filesystem"""

    POSIX = "posix"
    WINDOWS = "windows"

@dataclass
class SessionFSSetProviderResult:
    """Indicates whether the calling client was registered as the session filesystem provider."""

    success: bool
    """Whether the provider was set successfully"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSetProviderResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionFSSetProviderResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

@dataclass
class SessionFSSqliteExistsRequest:
    """Identifies the target session."""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSqliteExistsRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionFSSqliteExistsRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

@dataclass
class SessionFSSqliteExistsResult:
    """Indicates whether the per-session SQLite database already exists."""

    exists: bool
    """Whether the session database already exists"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSqliteExistsResult:
        assert isinstance(obj, dict)
        exists = from_bool(obj.get("exists"))
        return SessionFSSqliteExistsResult(exists)

    def to_dict(self) -> dict:
        result: dict = {}
        result["exists"] = from_bool(self.exists)
        return result

class SessionFSSqliteQueryType(Enum):
    """How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT
    (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
    """
    EXEC = "exec"
    QUERY = "query"
    RUN = "run"

@dataclass
class SessionFSStatRequest:
    """Path whose metadata should be returned from the client-provided session filesystem."""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSStatRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        return SessionFSStatRequest(path, session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        return result

@dataclass
class SessionFSWriteFileRequest:
    """File path, content to write, and optional mode for the client-provided session filesystem."""

    content: str
    """Content to write"""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    mode: int | None = None
    """Optional POSIX-style mode for newly created files"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSWriteFileRequest:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        path = from_str(obj.get("path"))
        session_id = from_str(obj.get("sessionId"))
        mode = from_union([from_int, from_none], obj.get("mode"))
        return SessionFSWriteFileRequest(content, path, session_id, mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["path"] = from_str(self.path)
        result["sessionId"] = from_str(self.session_id)
        if self.mode is not None:
            result["mode"] = from_union([from_int, from_none], self.mode)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionListFilter:
    """Optional filter applied to the returned sessions"""

    branch: str | None = None
    """Match sessions whose context.branch equals this value"""

    cwd: str | None = None
    """Match sessions whose context.cwd equals this value"""

    git_root: str | None = None
    """Match sessions whose context.gitRoot equals this value"""

    repository: str | None = None
    """Match sessions whose context.repository equals this value"""

    @staticmethod
    def from_dict(obj: Any) -> SessionListFilter:
        assert isinstance(obj, dict)
        branch = from_union([from_str, from_none], obj.get("branch"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        git_root = from_union([from_str, from_none], obj.get("gitRoot"))
        repository = from_union([from_str, from_none], obj.get("repository"))
        return SessionListFilter(branch, cwd, git_root, repository)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_str, from_none], self.git_root)
        if self.repository is not None:
            result["repository"] = from_union([from_str, from_none], self.repository)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionLoadDeferredRepoHooksResult:
    """Queued repo-level startup prompts and the total hook command count after loading."""

    hook_count: int
    """Total hook command count (user + plugin + repo) loaded for the session by this call.
    Captured atomically with startupPrompts so callers don't need to read a separate counter.
    """
    startup_prompts: list[str]
    """Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo
    configs were pending, or when disableAllHooks is set.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionLoadDeferredRepoHooksResult:
        assert isinstance(obj, dict)
        hook_count = from_int(obj.get("hookCount"))
        startup_prompts = from_list(from_str, obj.get("startupPrompts"))
        return SessionLoadDeferredRepoHooksResult(hook_count, startup_prompts)

    def to_dict(self) -> dict:
        result: dict = {}
        result["hookCount"] = from_int(self.hook_count)
        result["startupPrompts"] = from_list(from_str, self.startup_prompts)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionPruneResult:
    """Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes
    freed, and the dry-run flag.
    """
    candidates: list[str]
    """Session IDs that would be deleted in dry-run mode (always empty otherwise)"""

    deleted: list[str]
    """Session IDs that were deleted (always empty in dry-run mode)"""

    dry_run: bool
    """True when no deletions were actually performed"""

    freed_bytes: int
    """Total bytes freed (actual when not dry-run, projected when dry-run)"""

    skipped: list[str]
    """Session IDs that were skipped (e.g., named sessions)"""

    @staticmethod
    def from_dict(obj: Any) -> SessionPruneResult:
        assert isinstance(obj, dict)
        candidates = from_list(from_str, obj.get("candidates"))
        deleted = from_list(from_str, obj.get("deleted"))
        dry_run = from_bool(obj.get("dryRun"))
        freed_bytes = from_int(obj.get("freedBytes"))
        skipped = from_list(from_str, obj.get("skipped"))
        return SessionPruneResult(candidates, deleted, dry_run, freed_bytes, skipped)

    def to_dict(self) -> dict:
        result: dict = {}
        result["candidates"] = from_list(from_str, self.candidates)
        result["deleted"] = from_list(from_str, self.deleted)
        result["dryRun"] = from_bool(self.dry_run)
        result["freedBytes"] = from_int(self.freed_bytes)
        result["skipped"] = from_list(from_str, self.skipped)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSetCredentialsParams:
    """New auth credentials to install on the session. Omit to leave credentials unchanged."""

    credentials: AuthInfo | None = None
    """The new auth credentials to install on the session. When omitted or `undefined`, the call
    is a no-op and the session's existing credentials are preserved. The runtime stores the
    value verbatim and uses it for outbound model/API requests; it does NOT re-validate or
    re-fetch the associated Copilot user response. Several variants carry secret material;
    treat this method's params as containing secrets at rest and in transit.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionSetCredentialsParams:
        assert isinstance(obj, dict)
        credentials = from_union([_load_AuthInfo, from_none], obj.get("credentials"))
        return SessionSetCredentialsParams(credentials)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.credentials is not None:
            result["credentials"] = from_union([lambda x: (x).to_dict(), from_none], self.credentials)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSetCredentialsResult:
    """Indicates whether the credential update succeeded."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> SessionSetCredentialsResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionSetCredentialsResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSizes:
    """Map of sessionId -> on-disk size in bytes for each session's workspace directory."""

    sizes: dict[str, int]
    """Map of sessionId -> on-disk size in bytes for the session's workspace directory"""

    @staticmethod
    def from_dict(obj: Any) -> SessionSizes:
        assert isinstance(obj, dict)
        sizes = from_dict(from_int, obj.get("sizes"))
        return SessionSizes(sizes)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sizes"] = from_dict(from_int, self.sizes)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionUpdateOptionsResult:
    """Indicates whether the session options patch was applied successfully."""

    success: bool
    """Whether the operation succeeded"""

    @staticmethod
    def from_dict(obj: Any) -> SessionUpdateOptionsResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionUpdateOptionsResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsBulkDeleteRequest:
    """Session IDs to close, deactivate, and delete from disk."""

    session_ids: list[str]
    """Session IDs to close, deactivate, and delete from disk"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsBulkDeleteRequest:
        assert isinstance(obj, dict)
        session_ids = from_list(from_str, obj.get("sessionIds"))
        return SessionsBulkDeleteRequest(session_ids)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionIds"] = from_list(from_str, self.session_ids)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsCheckInUseRequest:
    """Session IDs to test for live in-use locks."""

    session_ids: list[str]
    """Session IDs to test for live in-use locks"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsCheckInUseRequest:
        assert isinstance(obj, dict)
        session_ids = from_list(from_str, obj.get("sessionIds"))
        return SessionsCheckInUseRequest(session_ids)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionIds"] = from_list(from_str, self.session_ids)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsCheckInUseResult:
    """Session IDs from the input set that are currently in use by another process."""

    in_use: list[str]
    """Session IDs from the input set that are currently held by another running process via an
    alive lock file
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsCheckInUseResult:
        assert isinstance(obj, dict)
        in_use = from_list(from_str, obj.get("inUse"))
        return SessionsCheckInUseResult(in_use)

    def to_dict(self) -> dict:
        result: dict = {}
        result["inUse"] = from_list(from_str, self.in_use)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsCloseRequest:
    """Session ID to close."""

    session_id: str
    """Session ID to close"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsCloseRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsCloseRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsCloseResult:
    """Closes a session: emits shutdown, flushes pending events to disk, releases the in-use
    lock, disposes the active session. Idempotent: succeeds even if the session is not
    currently active.
    """
    @staticmethod
    def from_dict(obj: Any) -> SessionsCloseResult:
        assert isinstance(obj, dict)
        return SessionsCloseResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsFindByPrefixRequest:
    """UUID prefix to resolve to a unique session ID."""

    prefix: str
    """UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when
    there is no match or the prefix matches multiple sessions.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsFindByPrefixRequest:
        assert isinstance(obj, dict)
        prefix = from_str(obj.get("prefix"))
        return SessionsFindByPrefixRequest(prefix)

    def to_dict(self) -> dict:
        result: dict = {}
        result["prefix"] = from_str(self.prefix)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsFindByPrefixResult:
    """Session ID matching the prefix, omitted when no unique match exists."""

    session_id: str | None = None
    """Omitted when no unique session matches the prefix (no match or ambiguous)"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsFindByPrefixResult:
        assert isinstance(obj, dict)
        session_id = from_union([from_str, from_none], obj.get("sessionId"))
        return SessionsFindByPrefixResult(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.session_id is not None:
            result["sessionId"] = from_union([from_str, from_none], self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsFindByTaskIDRequest:
    """GitHub task ID to look up."""

    task_id: str
    """GitHub task ID to look up"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsFindByTaskIDRequest:
        assert isinstance(obj, dict)
        task_id = from_str(obj.get("taskId"))
        return SessionsFindByTaskIDRequest(task_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["taskId"] = from_str(self.task_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsFindByTaskIDResult:
    """ID of the local session bound to the given GitHub task, or omitted when none."""

    session_id: str | None = None
    """Omitted when no local session is bound to that GitHub task"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsFindByTaskIDResult:
        assert isinstance(obj, dict)
        session_id = from_union([from_str, from_none], obj.get("sessionId"))
        return SessionsFindByTaskIDResult(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.session_id is not None:
            result["sessionId"] = from_union([from_str, from_none], self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsForkRequest:
    """Source session identifier to fork from, optional event-ID boundary, and optional friendly
    name for the new session.
    """
    session_id: str
    """Source session ID to fork from"""

    name: str | None = None
    """Optional friendly name to assign to the forked session."""

    to_event_id: str | None = None
    """Optional event ID boundary. When provided, the fork includes only events before this ID
    (exclusive). When omitted, all events are included.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsForkRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        name = from_union([from_str, from_none], obj.get("name"))
        to_event_id = from_union([from_str, from_none], obj.get("toEventId"))
        return SessionsForkRequest(session_id, name, to_event_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.to_event_id is not None:
            result["toEventId"] = from_union([from_str, from_none], self.to_event_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsForkResult:
    """Identifier and optional friendly name assigned to the newly forked session."""

    session_id: str
    """The new forked session's ID"""

    name: str | None = None
    """Friendly name assigned to the forked session, if any."""

    @staticmethod
    def from_dict(obj: Any) -> SessionsForkResult:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        name = from_union([from_str, from_none], obj.get("name"))
        return SessionsForkResult(session_id, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsGetEventFilePathRequest:
    """Session ID whose event-log file path to compute."""

    session_id: str
    """Session ID whose event-log file path to compute"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsGetEventFilePathRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsGetEventFilePathRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsGetEventFilePathResult:
    """Absolute path to the session's events.jsonl file on disk."""

    file_path: str
    """Absolute path to the session's events.jsonl file"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsGetEventFilePathResult:
        assert isinstance(obj, dict)
        file_path = from_str(obj.get("filePath"))
        return SessionsGetEventFilePathResult(file_path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["filePath"] = from_str(self.file_path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsGetLastForContextResult:
    """Most-relevant session ID for the supplied context, or omitted when no sessions exist."""

    session_id: str | None = None
    """Most-relevant session ID for the supplied context, or omitted when no sessions exist"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsGetLastForContextResult:
        assert isinstance(obj, dict)
        session_id = from_union([from_str, from_none], obj.get("sessionId"))
        return SessionsGetLastForContextResult(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.session_id is not None:
            result["sessionId"] = from_union([from_str, from_none], self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsGetPersistedRemoteSteerableRequest:
    """Session ID to look up the persisted remote-steerable flag for."""

    session_id: str
    """Session ID to look up the persisted remote-steerable flag for"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsGetPersistedRemoteSteerableRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsGetPersistedRemoteSteerableRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsGetPersistedRemoteSteerableResult:
    """The session's persisted remote-steerable flag, or omitted when no value has been
    persisted.
    """
    remote_steerable: bool | None = None
    """The session's persisted remote-steerable flag if recorded; omitted when no value has been
    persisted
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsGetPersistedRemoteSteerableResult:
        assert isinstance(obj, dict)
        remote_steerable = from_union([from_bool, from_none], obj.get("remoteSteerable"))
        return SessionsGetPersistedRemoteSteerableResult(remote_steerable)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_bool, from_none], self.remote_steerable)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsLoadDeferredRepoHooksRequest:
    """Active session ID whose deferred repo-level hooks should be loaded."""

    session_id: str
    """Active session ID whose deferred repo-level hooks should be loaded"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsLoadDeferredRepoHooksRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsLoadDeferredRepoHooksRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsPruneOldRequest:
    """Age threshold and optional flags controlling which old sessions are pruned (or simulated
    when dryRun is true).
    """
    older_than_days: int
    """Delete sessions whose modifiedTime is at least this many days old"""

    dry_run: bool | None = None
    """When true, only report what would be deleted without performing any deletion"""

    exclude_session_ids: list[str] | None = None
    """Session IDs that should never be considered for pruning"""

    include_named: bool | None = None
    """When true, named sessions (set via /rename) are also eligible for pruning"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsPruneOldRequest:
        assert isinstance(obj, dict)
        older_than_days = from_int(obj.get("olderThanDays"))
        dry_run = from_union([from_bool, from_none], obj.get("dryRun"))
        exclude_session_ids = from_union([lambda x: from_list(from_str, x), from_none], obj.get("excludeSessionIds"))
        include_named = from_union([from_bool, from_none], obj.get("includeNamed"))
        return SessionsPruneOldRequest(older_than_days, dry_run, exclude_session_ids, include_named)

    def to_dict(self) -> dict:
        result: dict = {}
        result["olderThanDays"] = from_int(self.older_than_days)
        if self.dry_run is not None:
            result["dryRun"] = from_union([from_bool, from_none], self.dry_run)
        if self.exclude_session_ids is not None:
            result["excludeSessionIds"] = from_union([lambda x: from_list(from_str, x), from_none], self.exclude_session_ids)
        if self.include_named is not None:
            result["includeNamed"] = from_union([from_bool, from_none], self.include_named)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsReleaseLockRequest:
    """Session ID whose in-use lock should be released."""

    session_id: str
    """Session ID whose in-use lock should be released"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsReleaseLockRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsReleaseLockRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsReleaseLockResult:
    """Release the in-use lock held by this process for the given session. No-op when this
    process does not currently hold a lock for the session.
    """
    @staticmethod
    def from_dict(obj: Any) -> SessionsReleaseLockResult:
        assert isinstance(obj, dict)
        return SessionsReleaseLockResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsReloadPluginHooksRequest:
    """Active session ID and an optional flag for deferring repo-level hooks until folder trust."""

    session_id: str
    """Active session ID to reload hooks for"""

    defer_repo_hooks: bool | None = None
    """When true, skip repo-level hooks. Use before folder trust is confirmed;
    loadDeferredRepoHooks loads them post-trust.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsReloadPluginHooksRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        defer_repo_hooks = from_union([from_bool, from_none], obj.get("deferRepoHooks"))
        return SessionsReloadPluginHooksRequest(session_id, defer_repo_hooks)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        if self.defer_repo_hooks is not None:
            result["deferRepoHooks"] = from_union([from_bool, from_none], self.defer_repo_hooks)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsReloadPluginHooksResult:
    """Reload all hooks (user, plugin, optionally repo) and apply them to the active session.
    Call after installing or removing plugins so their hooks take effect immediately. No-op
    when no active session matches the given sessionId.
    """
    @staticmethod
    def from_dict(obj: Any) -> SessionsReloadPluginHooksResult:
        assert isinstance(obj, dict)
        return SessionsReloadPluginHooksResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsSaveRequest:
    """Session ID whose pending events should be flushed to disk."""

    session_id: str
    """Session ID whose pending events should be flushed to disk"""

    @staticmethod
    def from_dict(obj: Any) -> SessionsSaveRequest:
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsSaveRequest(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsSaveResult:
    """Flush a session's pending events to disk. No-op when no writer exists for the session
    (e.g., already closed).
    """
    @staticmethod
    def from_dict(obj: Any) -> SessionsSaveResult:
        assert isinstance(obj, dict)
        return SessionsSaveResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsSetAdditionalPluginsResult:
    """Replace the manager-wide additional plugins. New session creations and subsequent hook
    reloads see the new set; already-running sessions keep their existing hook installation
    until the next reload.
    """
    @staticmethod
    def from_dict(obj: Any) -> SessionsSetAdditionalPluginsResult:
        assert isinstance(obj, dict)
        return SessionsSetAdditionalPluginsResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ShellExecRequest:
    """Shell command to run, with optional working directory and timeout in milliseconds."""

    command: str
    """Shell command to execute"""

    cwd: str | None = None
    """Working directory (defaults to session working directory)"""

    timeout: int | None = None
    """Timeout in milliseconds (default: 30000)"""

    @staticmethod
    def from_dict(obj: Any) -> ShellExecRequest:
        assert isinstance(obj, dict)
        command = from_str(obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        return ShellExecRequest(command, cwd, timeout)

    def to_dict(self) -> dict:
        result: dict = {}
        result["command"] = from_str(self.command)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ShellExecResult:
    """Identifier of the spawned process, used to correlate streamed output and exit
    notifications.
    """
    process_id: str
    """Unique identifier for tracking streamed output"""

    @staticmethod
    def from_dict(obj: Any) -> ShellExecResult:
        assert isinstance(obj, dict)
        process_id = from_str(obj.get("processId"))
        return ShellExecResult(process_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["processId"] = from_str(self.process_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class ShellKillSignal(Enum):
    """Signal to send (default: SIGTERM)"""

    SIGINT = "SIGINT"
    SIGKILL = "SIGKILL"
    SIGTERM = "SIGTERM"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ShellKillResult:
    """Indicates whether the signal was delivered; false if the process was unknown or already
    exited.
    """
    killed: bool
    """Whether the signal was sent successfully"""

    @staticmethod
    def from_dict(obj: Any) -> ShellKillResult:
        assert isinstance(obj, dict)
        killed = from_bool(obj.get("killed"))
        return ShellKillResult(killed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["killed"] = from_bool(self.killed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ShutdownRequest:
    """Parameters for shutting down the session"""

    reason: str | None = None
    """Optional human-readable reason. Typically the message of the error that triggered
    shutdown when type is 'error'.
    """
    type: ShutdownType | None = None
    """Why the session is being shut down. Defaults to "routine" when omitted."""

    @staticmethod
    def from_dict(obj: Any) -> ShutdownRequest:
        assert isinstance(obj, dict)
        reason = from_union([from_str, from_none], obj.get("reason"))
        type = from_union([ShutdownType, from_none], obj.get("type"))
        return ShutdownRequest(reason, type)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.reason is not None:
            result["reason"] = from_union([from_str, from_none], self.reason)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(ShutdownType, x), from_none], self.type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class Skill:
    """Schema for the `Skill` type."""

    description: str
    """Description of what the skill does"""

    enabled: bool
    """Whether the skill is currently enabled"""

    name: str
    """Unique identifier for the skill"""

    source: SkillSource
    """Source location type (e.g., project, personal-copilot, plugin, builtin)"""

    user_invocable: bool
    """Whether the skill can be invoked by the user as a slash command"""

    path: str | None = None
    """Absolute path to the skill file"""

    plugin_name: str | None = None
    """Name of the plugin that provides the skill, when source is 'plugin'"""

    @staticmethod
    def from_dict(obj: Any) -> Skill:
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = SkillSource(obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        path = from_union([from_str, from_none], obj.get("path"))
        plugin_name = from_union([from_str, from_none], obj.get("pluginName"))
        return Skill(description, enabled, name, source, user_invocable, path, plugin_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(SkillSource, self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.plugin_name is not None:
            result["pluginName"] = from_union([from_str, from_none], self.plugin_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillsDisableRequest:
    """Name of the skill to disable for the session."""

    name: str
    """Name of the skill to disable"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsDisableRequest:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SkillsDisableRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

@dataclass
class SkillsDiscoverRequest:
    """Optional project paths and additional skill directories to include in discovery."""

    project_paths: list[str] | None = None
    """Optional list of project directory paths to scan for project-scoped skills"""

    skill_directories: list[str] | None = None
    """Optional list of additional skill directory paths to include"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsDiscoverRequest:
        assert isinstance(obj, dict)
        project_paths = from_union([lambda x: from_list(from_str, x), from_none], obj.get("projectPaths"))
        skill_directories = from_union([lambda x: from_list(from_str, x), from_none], obj.get("skillDirectories"))
        return SkillsDiscoverRequest(project_paths, skill_directories)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.project_paths is not None:
            result["projectPaths"] = from_union([lambda x: from_list(from_str, x), from_none], self.project_paths)
        if self.skill_directories is not None:
            result["skillDirectories"] = from_union([lambda x: from_list(from_str, x), from_none], self.skill_directories)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillsEnableRequest:
    """Name of the skill to enable for the session."""

    name: str
    """Name of the skill to enable"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsEnableRequest:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SkillsEnableRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillsInvokedSkill:
    """Schema for the `SkillsInvokedSkill` type."""

    content: str
    """Full content of the skill file"""

    invoked_at_turn: int
    """Turn number when the skill was invoked"""

    name: str
    """Unique identifier for the skill"""

    path: str
    """Path to the SKILL.md file"""

    allowed_tools: list[str] | None = None
    """Tools that should be auto-approved when this skill is active, captured at invocation time"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsInvokedSkill:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        invoked_at_turn = from_int(obj.get("invokedAtTurn"))
        name = from_str(obj.get("name"))
        path = from_str(obj.get("path"))
        allowed_tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("allowedTools"))
        return SkillsInvokedSkill(content, invoked_at_turn, name, path, allowed_tools)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["invokedAtTurn"] = from_int(self.invoked_at_turn)
        result["name"] = from_str(self.name)
        result["path"] = from_str(self.path)
        if self.allowed_tools is not None:
            result["allowedTools"] = from_union([lambda x: from_list(from_str, x), from_none], self.allowed_tools)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillsLoadDiagnostics:
    """Diagnostics from reloading skill definitions, with warnings and errors as separate lists."""

    errors: list[str]
    """Errors emitted while loading skills (e.g. skills that failed to load entirely)"""

    warnings: list[str]
    """Warnings emitted while loading skills (e.g. skills that loaded but had issues)"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsLoadDiagnostics:
        assert isinstance(obj, dict)
        errors = from_list(from_str, obj.get("errors"))
        warnings = from_list(from_str, obj.get("warnings"))
        return SkillsLoadDiagnostics(errors, warnings)

    def to_dict(self) -> dict:
        result: dict = {}
        result["errors"] = from_list(from_str, self.errors)
        result["warnings"] = from_list(from_str, self.warnings)
        return result

class SlashCommandAgentPromptResultKind(Enum):
    AGENT_PROMPT = "agent-prompt"

class SlashCommandCompletedResultKind(Enum):
    COMPLETED = "completed"

class SlashCommandInvocationResultKind(Enum):
    AGENT_PROMPT = "agent-prompt"
    COMPLETED = "completed"
    SELECT_SUBCOMMAND = "select-subcommand"
    TEXT = "text"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandSelectSubcommandOption:
    """Schema for the `SlashCommandSelectSubcommandOption` type."""

    description: str
    """Human-readable description of the subcommand"""

    name: str
    """Subcommand name to invoke"""

    group: str | None = None
    """Optional group label for organizing options"""

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandSelectSubcommandOption:
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        name = from_str(obj.get("name"))
        group = from_union([from_str, from_none], obj.get("group"))
        return SlashCommandSelectSubcommandOption(description, name, group)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["name"] = from_str(self.name)
        if self.group is not None:
            result["group"] = from_union([from_str, from_none], self.group)
        return result

class SlashCommandSelectSubcommandResultKind(Enum):
    SELECT_SUBCOMMAND = "select-subcommand"

# Experimental: this type is part of an experimental API and may change or be removed.
class TaskExecutionMode(Enum):
    """Whether task execution is synchronously awaited or managed in the background"""

    BACKGROUND = "background"
    SYNC = "sync"

# Experimental: this type is part of an experimental API and may change or be removed.
class TaskStatus(Enum):
    """Current lifecycle status of the task"""

    CANCELLED = "cancelled"
    COMPLETED = "completed"
    FAILED = "failed"
    IDLE = "idle"
    RUNNING = "running"

class TaskAgentInfoType(Enum):
    AGENT = "agent"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskProgressLine:
    """Schema for the `TaskProgressLine` type."""

    message: str
    """Display message, e.g., "▸ bash", "✓ edit src/foo.ts\""""

    timestamp: datetime
    """ISO 8601 timestamp when this event occurred"""

    @staticmethod
    def from_dict(obj: Any) -> TaskProgressLine:
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        timestamp = from_datetime(obj.get("timestamp"))
        return TaskProgressLine(message, timestamp)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["timestamp"] = self.timestamp.isoformat()
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class TaskShellInfoAttachmentMode(Enum):
    """Whether the shell runs inside a managed PTY session or as an independent background
    process
    """
    ATTACHED = "attached"
    DETACHED = "detached"

class TaskInfoType(Enum):
    AGENT = "agent"
    SHELL = "shell"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskList:
    """Background tasks currently tracked by the session."""

    tasks: list[TaskInfo]
    """Currently tracked tasks"""

    @staticmethod
    def from_dict(obj: Any) -> TaskList:
        assert isinstance(obj, dict)
        tasks = from_list(_load_TaskInfo, obj.get("tasks"))
        return TaskList(tasks)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tasks"] = from_list(lambda x: (x).to_dict(), self.tasks)
        return result

class TaskShellInfoType(Enum):
    SHELL = "shell"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksCancelRequest:
    """Identifier of the background task to cancel."""

    id: str
    """Task identifier"""

    @staticmethod
    def from_dict(obj: Any) -> TasksCancelRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return TasksCancelRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksCancelResult:
    """Indicates whether the background task was successfully cancelled."""

    cancelled: bool
    """Whether the task was successfully cancelled"""

    @staticmethod
    def from_dict(obj: Any) -> TasksCancelResult:
        assert isinstance(obj, dict)
        cancelled = from_bool(obj.get("cancelled"))
        return TasksCancelResult(cancelled)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cancelled"] = from_bool(self.cancelled)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksGetCurrentPromotableResult:
    """The first sync-waiting task that can currently be promoted to background mode."""

    task: TaskInfo | None = None
    """The first sync-waiting task (agent first, then shell) that can currently be promoted to
    background mode. Omitted if no such task exists. The returned task is guaranteed to have
    executionMode='sync' and canPromoteToBackground=true at the time of the call.
    """

    @staticmethod
    def from_dict(obj: Any) -> TasksGetCurrentPromotableResult:
        assert isinstance(obj, dict)
        task = from_union([_load_TaskInfo, from_none], obj.get("task"))
        return TasksGetCurrentPromotableResult(task)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.task is not None:
            result["task"] = from_union([lambda x: (x).to_dict(), from_none], self.task)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksGetProgressRequest:
    """Identifier of the background task to fetch progress for."""

    id: str
    """Task identifier (agent ID or shell ID)"""

    @staticmethod
    def from_dict(obj: Any) -> TasksGetProgressRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return TasksGetProgressRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksPromoteCurrentToBackgroundResult:
    """The promoted task as it now exists in background mode, omitted if no promotable task was
    waiting.
    """
    task: TaskInfo | None = None
    """The promoted task as it now exists in background mode, omitted if no promotable task was
    waiting. Atomic operation: avoids the race window of getCurrentPromotable +
    promoteToBackground.
    """

    @staticmethod
    def from_dict(obj: Any) -> TasksPromoteCurrentToBackgroundResult:
        assert isinstance(obj, dict)
        task = from_union([_load_TaskInfo, from_none], obj.get("task"))
        return TasksPromoteCurrentToBackgroundResult(task)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.task is not None:
            result["task"] = from_union([lambda x: (x).to_dict(), from_none], self.task)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksPromoteToBackgroundRequest:
    """Identifier of the task to promote to background mode."""

    id: str
    """Task identifier"""

    @staticmethod
    def from_dict(obj: Any) -> TasksPromoteToBackgroundRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return TasksPromoteToBackgroundRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksPromoteToBackgroundResult:
    """Indicates whether the task was successfully promoted to background mode."""

    promoted: bool
    """Whether the task was successfully promoted to background mode"""

    @staticmethod
    def from_dict(obj: Any) -> TasksPromoteToBackgroundResult:
        assert isinstance(obj, dict)
        promoted = from_bool(obj.get("promoted"))
        return TasksPromoteToBackgroundResult(promoted)

    def to_dict(self) -> dict:
        result: dict = {}
        result["promoted"] = from_bool(self.promoted)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksRefreshResult:
    """Refresh metadata for any detached background shells the runtime knows about. Use after a
    long pause to pick up exit/output state for shells running outside the agent loop.
    """
    @staticmethod
    def from_dict(obj: Any) -> TasksRefreshResult:
        assert isinstance(obj, dict)
        return TasksRefreshResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksRemoveRequest:
    """Identifier of the completed or cancelled task to remove from tracking."""

    id: str
    """Task identifier"""

    @staticmethod
    def from_dict(obj: Any) -> TasksRemoveRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return TasksRemoveRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksRemoveResult:
    """Indicates whether the task was removed. False when the task does not exist or is still
    running/idle.
    """
    removed: bool
    """Whether the task was removed. Returns false if the task does not exist or is still
    running/idle (cancel it first).
    """

    @staticmethod
    def from_dict(obj: Any) -> TasksRemoveResult:
        assert isinstance(obj, dict)
        removed = from_bool(obj.get("removed"))
        return TasksRemoveResult(removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["removed"] = from_bool(self.removed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksSendMessageRequest:
    """Identifier of the target agent task, message content, and optional sender agent ID."""

    id: str
    """Agent task identifier"""

    message: str
    """Message content to send to the agent"""

    from_agent_id: str | None = None
    """Agent ID of the sender, if sent on behalf of another agent"""

    @staticmethod
    def from_dict(obj: Any) -> TasksSendMessageRequest:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        message = from_str(obj.get("message"))
        from_agent_id = from_union([from_str, from_none], obj.get("fromAgentId"))
        return TasksSendMessageRequest(id, message, from_agent_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        result["message"] = from_str(self.message)
        if self.from_agent_id is not None:
            result["fromAgentId"] = from_union([from_str, from_none], self.from_agent_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksSendMessageResult:
    """Indicates whether the message was delivered, with an error message when delivery failed."""

    sent: bool
    """Whether the message was successfully delivered or steered"""

    error: str | None = None
    """Error message if delivery failed"""

    @staticmethod
    def from_dict(obj: Any) -> TasksSendMessageResult:
        assert isinstance(obj, dict)
        sent = from_bool(obj.get("sent"))
        error = from_union([from_str, from_none], obj.get("error"))
        return TasksSendMessageResult(sent, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sent"] = from_bool(self.sent)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksStartAgentRequest:
    """Agent type, prompt, name, and optional description and model override for the new task."""

    agent_type: str
    """Type of agent to start (e.g., 'explore', 'task', 'general-purpose')"""

    name: str
    """Short name for the agent, used to generate a human-readable ID"""

    prompt: str
    """Task prompt for the agent"""

    description: str | None = None
    """Short description of the task"""

    model: str | None = None
    """Optional model override"""

    @staticmethod
    def from_dict(obj: Any) -> TasksStartAgentRequest:
        assert isinstance(obj, dict)
        agent_type = from_str(obj.get("agentType"))
        name = from_str(obj.get("name"))
        prompt = from_str(obj.get("prompt"))
        description = from_union([from_str, from_none], obj.get("description"))
        model = from_union([from_str, from_none], obj.get("model"))
        return TasksStartAgentRequest(agent_type, name, prompt, description, model)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentType"] = from_str(self.agent_type)
        result["name"] = from_str(self.name)
        result["prompt"] = from_str(self.prompt)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksStartAgentResult:
    """Identifier assigned to the newly started background agent task."""

    agent_id: str
    """Generated agent ID for the background task"""

    @staticmethod
    def from_dict(obj: Any) -> TasksStartAgentResult:
        assert isinstance(obj, dict)
        agent_id = from_str(obj.get("agentId"))
        return TasksStartAgentResult(agent_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentId"] = from_str(self.agent_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksWaitForPendingResult:
    """Wait until all in-flight background tasks (agents + shells) and any follow-up turns
    scheduled by their completions have settled. Returns when the runtime is fully drained or
    after an internal timeout (default 10 minutes; configurable via
    COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
    """
    @staticmethod
    def from_dict(obj: Any) -> TasksWaitForPendingResult:
        assert isinstance(obj, dict)
        return TasksWaitForPendingResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TelemetrySetFeatureOverridesRequest:
    """Feature override key/value pairs to attach to subsequent telemetry events from this
    session.
    """
    features: dict[str, str]
    """Override key/value pairs to attach to subsequent telemetry events from this session.
    Replaces any previously-set overrides.
    """

    @staticmethod
    def from_dict(obj: Any) -> TelemetrySetFeatureOverridesRequest:
        assert isinstance(obj, dict)
        features = from_dict(from_str, obj.get("features"))
        return TelemetrySetFeatureOverridesRequest(features)

    def to_dict(self) -> dict:
        result: dict = {}
        result["features"] = from_dict(from_str, self.features)
        return result

class TokenAuthInfoType(Enum):
    TOKEN = "token"

@dataclass
class Tool:
    """Schema for the `Tool` type."""

    description: str
    """Description of what the tool does"""

    name: str
    """Tool identifier (e.g., "bash", "grep", "str_replace_editor")"""

    instructions: str | None = None
    """Optional instructions for how to use this tool effectively"""

    namespaced_name: str | None = None
    """Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP
    tools)
    """
    parameters: dict[str, Any] | None = None
    """JSON Schema for the tool's input parameters"""

    @staticmethod
    def from_dict(obj: Any) -> Tool:
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        name = from_str(obj.get("name"))
        instructions = from_union([from_str, from_none], obj.get("instructions"))
        namespaced_name = from_union([from_str, from_none], obj.get("namespacedName"))
        parameters = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("parameters"))
        return Tool(description, name, instructions, namespaced_name, parameters)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["name"] = from_str(self.name)
        if self.instructions is not None:
            result["instructions"] = from_union([from_str, from_none], self.instructions)
        if self.namespaced_name is not None:
            result["namespacedName"] = from_union([from_str, from_none], self.namespaced_name)
        if self.parameters is not None:
            result["parameters"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.parameters)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ToolsInitializeAndValidateResult:
    """Resolve, build, and validate the runtime tool list for this session. Subagent sessions
    and consumer flows that need an initialized tool set before `send` invoke this. Default
    base-class implementation is a no-op for sessions that don't support tool validation.
    """
    @staticmethod
    def from_dict(obj: Any) -> ToolsInitializeAndValidateResult:
        assert isinstance(obj, dict)
        return ToolsInitializeAndValidateResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result

@dataclass
class ToolsListRequest:
    """Optional model identifier whose tool overrides should be applied to the listing."""

    model: str | None = None
    """Optional model ID — when provided, the returned tool list reflects model-specific
    overrides
    """

    @staticmethod
    def from_dict(obj: Any) -> ToolsListRequest:
        assert isinstance(obj, dict)
        model = from_union([from_str, from_none], obj.get("model"))
        return ToolsListRequest(model)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class UIAutoModeSwitchResponse(Enum):
    """User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist
    as setting), or no (decline).
    """
    NO = "no"
    YES = "yes"
    YES_ALWAYS = "yes_always"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationArrayAnyOfFieldItemsAnyOf:
    """Schema for the `UIElicitationArrayAnyOfFieldItemsAnyOf` type."""

    const: str
    """Value submitted when this option is selected."""

    title: str
    """Display label for this option."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationArrayAnyOfFieldItemsAnyOf:
        assert isinstance(obj, dict)
        const = from_str(obj.get("const"))
        title = from_str(obj.get("title"))
        return UIElicitationArrayAnyOfFieldItemsAnyOf(const, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["const"] = from_str(self.const)
        result["title"] = from_str(self.title)
        return result

class UIElicitationArrayAnyOfFieldType(Enum):
    ARRAY = "array"

class UIElicitationArrayEnumFieldItemsType(Enum):
    STRING = "string"

# Experimental: this type is part of an experimental API and may change or be removed.
class UIElicitationSchemaPropertyStringFormat(Enum):
    """Optional format hint that constrains the accepted input."""

    DATE = "date"
    DATE_TIME = "date-time"
    EMAIL = "email"
    URI = "uri"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationStringOneOfFieldOneOf:
    """Schema for the `UIElicitationStringOneOfFieldOneOf` type."""

    const: str
    """Value submitted when this option is selected."""

    title: str
    """Display label for this option."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationStringOneOfFieldOneOf:
        assert isinstance(obj, dict)
        const = from_str(obj.get("const"))
        title = from_str(obj.get("title"))
        return UIElicitationStringOneOfFieldOneOf(const, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["const"] = from_str(self.const)
        result["title"] = from_str(self.title)
        return result

class UIElicitationSchemaPropertyType(Enum):
    """Numeric type accepted by the field."""

    ARRAY = "array"
    BOOLEAN = "boolean"
    INTEGER = "integer"
    NUMBER = "number"
    STRING = "string"

class UIElicitationSchemaType(Enum):
    OBJECT = "object"

# Experimental: this type is part of an experimental API and may change or be removed.
class UIElicitationResponseAction(Enum):
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    ACCEPT = "accept"
    CANCEL = "cancel"
    DECLINE = "decline"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationResult:
    """Indicates whether the elicitation response was accepted; false if it was already resolved
    by another client.
    """
    success: bool
    """Whether the response was accepted. False if the request was already resolved by another
    client.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return UIElicitationResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

class UIElicitationSchemaPropertyBooleanType(Enum):
    BOOLEAN = "boolean"

# Experimental: this type is part of an experimental API and may change or be removed.
class UIElicitationSchemaPropertyNumberType(Enum):
    """Numeric type accepted by the field."""

    INTEGER = "integer"
    NUMBER = "number"

# Experimental: this type is part of an experimental API and may change or be removed.
class UIExitPlanModeAction(Enum):
    """The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true,
    otherwise 'interactive'.
    """
    AUTOPILOT = "autopilot"
    AUTOPILOT_FLEET = "autopilot_fleet"
    EXIT_ONLY = "exit_only"
    INTERACTIVE = "interactive"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIHandlePendingResult:
    """Indicates whether the pending UI request was resolved by this call."""

    success: bool
    """True if the request was still pending and was resolved by this call. False if the request
    ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise
    no longer pending.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIHandlePendingResult:
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return UIHandlePendingResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIHandlePendingSamplingRequest:
    """Request ID of a pending `sampling.requested` event and an optional sampling result
    payload (omit to reject).
    """
    request_id: str
    """The unique request ID from the sampling.requested event"""

    response: dict[str, Any] | None = None
    """Optional sampling result payload. Omit to reject/cancel the sampling request without
    providing a result.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIHandlePendingSamplingRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        response = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("response"))
        return UIHandlePendingSamplingRequest(request_id, response)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.response is not None:
            result["response"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.response)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIUserInputResponse:
    """Schema for the `UIUserInputResponse` type."""

    answer: str
    """The user's answer text"""

    was_freeform: bool
    """True if the user typed a freeform response, false if they selected a presented choice.
    Used by telemetry to differentiate between free text input and choice selection.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIUserInputResponse:
        assert isinstance(obj, dict)
        answer = from_str(obj.get("answer"))
        was_freeform = from_bool(obj.get("wasFreeform"))
        return UIUserInputResponse(answer, was_freeform)

    def to_dict(self) -> dict:
        result: dict = {}
        result["answer"] = from_str(self.answer)
        result["wasFreeform"] = from_bool(self.was_freeform)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIRegisterDirectAutoModeSwitchHandlerResult:
    """Register an in-process handler for `auto_mode_switch.requested` events. The caller still
    attaches the actual listener via the standard event-subscription mechanism; this
    registration solely tells the server bridge to skip its own dispatch (so a remote client
    doesn't race the in-process handler for the same requestId).
    """
    handle: str
    """Opaque handle representing the registration. Pass this same handle to
    `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active.
    Multiple registrations are reference-counted; the server bridge will only dispatch
    auto-mode-switch requests when no handles are active.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIRegisterDirectAutoModeSwitchHandlerResult:
        assert isinstance(obj, dict)
        handle = from_str(obj.get("handle"))
        return UIRegisterDirectAutoModeSwitchHandlerResult(handle)

    def to_dict(self) -> dict:
        result: dict = {}
        result["handle"] = from_str(self.handle)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIUnregisterDirectAutoModeSwitchHandlerRequest:
    """Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release."""

    handle: str
    """Handle previously returned by `registerDirectAutoModeSwitchHandler`"""

    @staticmethod
    def from_dict(obj: Any) -> UIUnregisterDirectAutoModeSwitchHandlerRequest:
        assert isinstance(obj, dict)
        handle = from_str(obj.get("handle"))
        return UIUnregisterDirectAutoModeSwitchHandlerRequest(handle)

    def to_dict(self) -> dict:
        result: dict = {}
        result["handle"] = from_str(self.handle)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIUnregisterDirectAutoModeSwitchHandlerResult:
    """Indicates whether the handle was active and the registration count was decremented."""

    unregistered: bool
    """True if the handle was active and decremented the counter; false if the handle was
    unknown.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIUnregisterDirectAutoModeSwitchHandlerResult:
        assert isinstance(obj, dict)
        unregistered = from_bool(obj.get("unregistered"))
        return UIUnregisterDirectAutoModeSwitchHandlerResult(unregistered)

    def to_dict(self) -> dict:
        result: dict = {}
        result["unregistered"] = from_bool(self.unregistered)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageMetricsCodeChanges:
    """Aggregated code change metrics"""

    files_modified: list[str]
    """Distinct file paths modified during the session"""

    files_modified_count: int
    """Number of distinct files modified"""

    lines_added: int
    """Total lines of code added"""

    lines_removed: int
    """Total lines of code removed"""

    @staticmethod
    def from_dict(obj: Any) -> UsageMetricsCodeChanges:
        assert isinstance(obj, dict)
        files_modified = from_list(from_str, obj.get("filesModified"))
        files_modified_count = from_int(obj.get("filesModifiedCount"))
        lines_added = from_int(obj.get("linesAdded"))
        lines_removed = from_int(obj.get("linesRemoved"))
        return UsageMetricsCodeChanges(files_modified, files_modified_count, lines_added, lines_removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["filesModified"] = from_list(from_str, self.files_modified)
        result["filesModifiedCount"] = from_int(self.files_modified_count)
        result["linesAdded"] = from_int(self.lines_added)
        result["linesRemoved"] = from_int(self.lines_removed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageMetricsModelMetricRequests:
    """Request count and cost metrics for this model"""

    cost: float
    """User-initiated premium request cost (with multiplier applied)"""

    count: int
    """Number of API requests made with this model"""

    @staticmethod
    def from_dict(obj: Any) -> UsageMetricsModelMetricRequests:
        assert isinstance(obj, dict)
        cost = from_float(obj.get("cost"))
        count = from_int(obj.get("count"))
        return UsageMetricsModelMetricRequests(cost, count)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cost"] = to_float(self.cost)
        result["count"] = from_int(self.count)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageMetricsModelMetricTokenDetail:
    """Schema for the `UsageMetricsModelMetricTokenDetail` type."""

    token_count: int
    """Accumulated token count for this token type"""

    @staticmethod
    def from_dict(obj: Any) -> UsageMetricsModelMetricTokenDetail:
        assert isinstance(obj, dict)
        token_count = from_int(obj.get("tokenCount"))
        return UsageMetricsModelMetricTokenDetail(token_count)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenCount"] = from_int(self.token_count)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageMetricsModelMetricUsage:
    """Token usage metrics for this model"""

    cache_read_tokens: int
    """Total tokens read from prompt cache"""

    cache_write_tokens: int
    """Total tokens written to prompt cache"""

    input_tokens: int
    """Total input tokens consumed"""

    output_tokens: int
    """Total output tokens produced"""

    reasoning_tokens: int | None = None
    """Total output tokens used for reasoning"""

    @staticmethod
    def from_dict(obj: Any) -> UsageMetricsModelMetricUsage:
        assert isinstance(obj, dict)
        cache_read_tokens = from_int(obj.get("cacheReadTokens"))
        cache_write_tokens = from_int(obj.get("cacheWriteTokens"))
        input_tokens = from_int(obj.get("inputTokens"))
        output_tokens = from_int(obj.get("outputTokens"))
        reasoning_tokens = from_union([from_int, from_none], obj.get("reasoningTokens"))
        return UsageMetricsModelMetricUsage(cache_read_tokens, cache_write_tokens, input_tokens, output_tokens, reasoning_tokens)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cacheReadTokens"] = from_int(self.cache_read_tokens)
        result["cacheWriteTokens"] = from_int(self.cache_write_tokens)
        result["inputTokens"] = from_int(self.input_tokens)
        result["outputTokens"] = from_int(self.output_tokens)
        if self.reasoning_tokens is not None:
            result["reasoningTokens"] = from_union([from_int, from_none], self.reasoning_tokens)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageMetricsTokenDetail:
    """Schema for the `UsageMetricsTokenDetail` type."""

    token_count: int
    """Accumulated token count for this token type"""

    @staticmethod
    def from_dict(obj: Any) -> UsageMetricsTokenDetail:
        assert isinstance(obj, dict)
        token_count = from_int(obj.get("tokenCount"))
        return UsageMetricsTokenDetail(token_count)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenCount"] = from_int(self.token_count)
        return result

class UserAuthInfoType(Enum):
    USER = "user"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesCheckpoints:
    """Schema for the `WorkspacesCheckpoints` type."""

    filename: str
    """Filename of the checkpoint within the workspace checkpoints directory"""

    number: int
    """Checkpoint number assigned by the workspace manager"""

    title: str
    """Human-readable checkpoint title"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesCheckpoints:
        assert isinstance(obj, dict)
        filename = from_str(obj.get("filename"))
        number = from_int(obj.get("number"))
        title = from_str(obj.get("title"))
        return WorkspacesCheckpoints(filename, number, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["filename"] = from_str(self.filename)
        result["number"] = from_int(self.number)
        result["title"] = from_str(self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesCreateFileRequest:
    """Relative path and UTF-8 content for the workspace file to create or overwrite."""

    content: str
    """File content to write as a UTF-8 string"""

    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesCreateFileRequest:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        path = from_str(obj.get("path"))
        return WorkspacesCreateFileRequest(content, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesListFilesResult:
    """Relative paths of files stored in the session workspace files directory."""

    files: list[str]
    """Relative file paths in the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesListFilesResult:
        assert isinstance(obj, dict)
        files = from_list(from_str, obj.get("files"))
        return WorkspacesListFilesResult(files)

    def to_dict(self) -> dict:
        result: dict = {}
        result["files"] = from_list(from_str, self.files)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesReadCheckpointRequest:
    """Checkpoint number to read."""

    number: int
    """Checkpoint number to read"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesReadCheckpointRequest:
        assert isinstance(obj, dict)
        number = from_int(obj.get("number"))
        return WorkspacesReadCheckpointRequest(number)

    def to_dict(self) -> dict:
        result: dict = {}
        result["number"] = from_int(self.number)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesReadCheckpointResult:
    """Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing."""

    content: str | None = None
    """Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesReadCheckpointResult:
        assert isinstance(obj, dict)
        content = from_union([from_none, from_str], obj.get("content"))
        return WorkspacesReadCheckpointResult(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_union([from_none, from_str], self.content)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesReadFileRequest:
    """Relative path of the workspace file to read."""

    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesReadFileRequest:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return WorkspacesReadFileRequest(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesReadFileResult:
    """Contents of the requested workspace file as a UTF-8 string."""

    content: str
    """File content as a UTF-8 string"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesReadFileResult:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return WorkspacesReadFileResult(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesSaveLargePasteRequest:
    """Pasted content to save as a UTF-8 file in the session workspace."""

    content: str
    """Pasted content to save as a UTF-8 file"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesSaveLargePasteRequest:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return WorkspacesSaveLargePasteRequest(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result

@dataclass
class Saved:
    filename: str
    """Filename within the workspace files directory"""

    file_path: str
    """Absolute filesystem path to the saved paste file"""

    size_bytes: int
    """Size of the saved file in bytes"""

    @staticmethod
    def from_dict(obj: Any) -> Saved:
        assert isinstance(obj, dict)
        filename = from_str(obj.get("filename"))
        file_path = from_str(obj.get("filePath"))
        size_bytes = from_int(obj.get("sizeBytes"))
        return Saved(filename, file_path, size_bytes)

    def to_dict(self) -> dict:
        result: dict = {}
        result["filename"] = from_str(self.filename)
        result["filePath"] = from_str(self.file_path)
        result["sizeBytes"] = from_int(self.size_bytes)
        return result

@dataclass
class AccountGetQuotaResult:
    """Quota usage snapshots for the resolved user, keyed by quota type."""

    quota_snapshots: dict[str, AccountQuotaSnapshot]
    """Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)"""

    @staticmethod
    def from_dict(obj: Any) -> AccountGetQuotaResult:
        assert isinstance(obj, dict)
        quota_snapshots = from_dict(AccountQuotaSnapshot.from_dict, obj.get("quotaSnapshots"))
        return AccountGetQuotaResult(quota_snapshots)

    def to_dict(self) -> dict:
        result: dict = {}
        result["quotaSnapshots"] = from_dict(lambda x: to_class(AccountQuotaSnapshot, x), self.quota_snapshots)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAuthStatus:
    """Authentication status and account metadata for the session."""

    is_authenticated: bool
    """Whether the session has resolved authentication"""

    auth_type: AuthInfoType | None = None
    """Authentication type"""

    copilot_plan: str | None = None
    """Copilot plan tier (e.g., individual_pro, business)"""

    host: str | None = None
    """Authentication host URL"""

    login: str | None = None
    """Authenticated login/username, if available"""

    status_message: str | None = None
    """Human-readable authentication status description"""

    @staticmethod
    def from_dict(obj: Any) -> SessionAuthStatus:
        assert isinstance(obj, dict)
        is_authenticated = from_bool(obj.get("isAuthenticated"))
        auth_type = from_union([AuthInfoType, from_none], obj.get("authType"))
        copilot_plan = from_union([from_str, from_none], obj.get("copilotPlan"))
        host = from_union([from_str, from_none], obj.get("host"))
        login = from_union([from_str, from_none], obj.get("login"))
        status_message = from_union([from_str, from_none], obj.get("statusMessage"))
        return SessionAuthStatus(is_authenticated, auth_type, copilot_plan, host, login, status_message)

    def to_dict(self) -> dict:
        result: dict = {}
        result["isAuthenticated"] = from_bool(self.is_authenticated)
        if self.auth_type is not None:
            result["authType"] = from_union([lambda x: to_enum(AuthInfoType, x), from_none], self.auth_type)
        if self.copilot_plan is not None:
            result["copilotPlan"] = from_union([from_str, from_none], self.copilot_plan)
        if self.host is not None:
            result["host"] = from_union([from_str, from_none], self.host)
        if self.login is not None:
            result["login"] = from_union([from_str, from_none], self.login)
        if self.status_message is not None:
            result["statusMessage"] = from_union([from_str, from_none], self.status_message)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandInput:
    """Optional unstructured input hint"""

    hint: str
    """Hint to display when command input has not been provided"""

    completion: SlashCommandInputCompletion | None = None
    """Optional completion hint for the input (e.g. 'directory' for filesystem path completion)"""

    preserve_multiline_input: bool | None = None
    """When true, clients should pass the full text after the command name as a single argument
    rather than splitting on whitespace
    """
    required: bool | None = None
    """When true, the command requires non-empty input; clients should render the input hint as
    required
    """

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandInput:
        assert isinstance(obj, dict)
        hint = from_str(obj.get("hint"))
        completion = from_union([SlashCommandInputCompletion, from_none], obj.get("completion"))
        preserve_multiline_input = from_union([from_bool, from_none], obj.get("preserveMultilineInput"))
        required = from_union([from_bool, from_none], obj.get("required"))
        return SlashCommandInput(hint, completion, preserve_multiline_input, required)

    def to_dict(self) -> dict:
        result: dict = {}
        result["hint"] = from_str(self.hint)
        if self.completion is not None:
            result["completion"] = from_union([lambda x: to_enum(SlashCommandInputCompletion, x), from_none], self.completion)
        if self.preserve_multiline_input is not None:
            result["preserveMultilineInput"] = from_union([from_bool, from_none], self.preserve_multiline_input)
        if self.required is not None:
            result["required"] = from_union([from_bool, from_none], self.required)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentDirectory:
    """Directory attachment"""

    display_name: str
    """User-facing display name for the attachment"""

    path: str
    """Absolute directory path"""

    type: ClassVar[str] = "directory"
    """Attachment type discriminator"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentDirectory:
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        path = from_str(obj.get("path"))
        return SendAttachmentDirectory(display_name, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["path"] = from_str(self.path)
        result["type"] = self.type
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ConnectedRemoteSessionMetadata:
    """Metadata for a connected remote session."""

    kind: ConnectedRemoteSessionMetadataKind
    """Neutral SDK discriminator for the connected remote session kind."""

    modified_time: datetime
    """Last session update time as an ISO 8601 string."""

    repository: ConnectedRemoteSessionMetadataRepository
    """Repository associated with the connected remote session."""

    session_id: str
    """SDK session ID for the connected remote session."""

    start_time: datetime
    """Session start time as an ISO 8601 string."""

    name: str | None = None
    """Optional friendly session name."""

    pull_request_number: int | None = None
    """Pull request number associated with the session."""

    resource_id: str | None = None
    """Original remote resource identifier."""

    stale_at: datetime | None = None
    """Remote session staleness deadline as an ISO 8601 string."""

    state: str | None = None
    """Remote session state returned by the backing service."""

    summary: str | None = None
    """Optional session summary."""

    @staticmethod
    def from_dict(obj: Any) -> ConnectedRemoteSessionMetadata:
        assert isinstance(obj, dict)
        kind = ConnectedRemoteSessionMetadataKind(obj.get("kind"))
        modified_time = from_datetime(obj.get("modifiedTime"))
        repository = ConnectedRemoteSessionMetadataRepository.from_dict(obj.get("repository"))
        session_id = from_str(obj.get("sessionId"))
        start_time = from_datetime(obj.get("startTime"))
        name = from_union([from_str, from_none], obj.get("name"))
        pull_request_number = from_union([from_int, from_none], obj.get("pullRequestNumber"))
        resource_id = from_union([from_str, from_none], obj.get("resourceId"))
        stale_at = from_union([from_datetime, from_none], obj.get("staleAt"))
        state = from_union([from_str, from_none], obj.get("state"))
        summary = from_union([from_str, from_none], obj.get("summary"))
        return ConnectedRemoteSessionMetadata(kind, modified_time, repository, session_id, start_time, name, pull_request_number, resource_id, stale_at, state, summary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(ConnectedRemoteSessionMetadataKind, self.kind)
        result["modifiedTime"] = self.modified_time.isoformat()
        result["repository"] = to_class(ConnectedRemoteSessionMetadataRepository, self.repository)
        result["sessionId"] = from_str(self.session_id)
        result["startTime"] = self.start_time.isoformat()
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.pull_request_number is not None:
            result["pullRequestNumber"] = from_union([from_int, from_none], self.pull_request_number)
        if self.resource_id is not None:
            result["resourceId"] = from_union([from_str, from_none], self.resource_id)
        if self.stale_at is not None:
            result["staleAt"] = from_union([lambda x: x.isoformat(), from_none], self.stale_at)
        if self.state is not None:
            result["state"] = from_union([from_str, from_none], self.state)
        if self.summary is not None:
            result["summary"] = from_union([from_str, from_none], self.summary)
        return result

@dataclass
class MCPServerConfigStdio:
    """Stdio MCP server configuration launched as a child process."""

    command: str
    """Executable command used to start the Stdio MCP server process."""

    args: list[str] | None = None
    """Command-line arguments passed to the Stdio MCP server process."""

    cwd: str | None = None
    """Working directory for the Stdio MCP server process."""

    env: dict[str, str] | None = None
    """Environment variables to pass to the Stdio MCP server process."""

    filter_mapping: dict[str, ContentFilterMode] | ContentFilterMode | None = None
    """Content filtering mode to apply to all tools, or a map of tool name to content filtering
    mode.
    """
    is_default_server: bool | None = None
    """Whether this server is a built-in fallback used when the user has not configured their
    own server.
    """
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    @staticmethod
    def from_dict(obj: Any) -> MCPServerConfigStdio:
        assert isinstance(obj, dict)
        command = from_str(obj.get("command"))
        args = from_union([lambda x: from_list(from_str, x), from_none], obj.get("args"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(ContentFilterMode, x), ContentFilterMode, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        return MCPServerConfigStdio(command, args, cwd, env, filter_mapping, is_default_server, timeout, tools)

    def to_dict(self) -> dict:
        result: dict = {}
        result["command"] = from_str(self.command)
        if self.args is not None:
            result["args"] = from_union([lambda x: from_list(from_str, x), from_none], self.args)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.env is not None:
            result["env"] = from_union([lambda x: from_dict(from_str, x), from_none], self.env)
        if self.filter_mapping is not None:
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(ContentFilterMode, x), x), lambda x: to_enum(ContentFilterMode, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotUserResponseQuotaSnapshots:
    """Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.

    Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type.

    Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type.
    """
    entitlement: float | None = None
    has_quota: bool | None = None
    overage_count: float | None = None
    overage_permitted: bool | None = None
    percent_remaining: float | None = None
    quota_id: str | None = None
    quota_remaining: float | None = None
    quota_reset_at: float | None = None
    remaining: float | None = None
    timestamp_utc: str | None = None
    token_based_billing: bool | None = None
    unlimited: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> CopilotUserResponseQuotaSnapshots:
        assert isinstance(obj, dict)
        entitlement = from_union([from_float, from_none], obj.get("entitlement"))
        has_quota = from_union([from_bool, from_none], obj.get("has_quota"))
        overage_count = from_union([from_float, from_none], obj.get("overage_count"))
        overage_permitted = from_union([from_bool, from_none], obj.get("overage_permitted"))
        percent_remaining = from_union([from_float, from_none], obj.get("percent_remaining"))
        quota_id = from_union([from_str, from_none], obj.get("quota_id"))
        quota_remaining = from_union([from_float, from_none], obj.get("quota_remaining"))
        quota_reset_at = from_union([from_float, from_none], obj.get("quota_reset_at"))
        remaining = from_union([from_float, from_none], obj.get("remaining"))
        timestamp_utc = from_union([from_str, from_none], obj.get("timestamp_utc"))
        token_based_billing = from_union([from_bool, from_none], obj.get("token_based_billing"))
        unlimited = from_union([from_bool, from_none], obj.get("unlimited"))
        return CopilotUserResponseQuotaSnapshots(entitlement, has_quota, overage_count, overage_permitted, percent_remaining, quota_id, quota_remaining, quota_reset_at, remaining, timestamp_utc, token_based_billing, unlimited)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.entitlement is not None:
            result["entitlement"] = from_union([to_float, from_none], self.entitlement)
        if self.has_quota is not None:
            result["has_quota"] = from_union([from_bool, from_none], self.has_quota)
        if self.overage_count is not None:
            result["overage_count"] = from_union([to_float, from_none], self.overage_count)
        if self.overage_permitted is not None:
            result["overage_permitted"] = from_union([from_bool, from_none], self.overage_permitted)
        if self.percent_remaining is not None:
            result["percent_remaining"] = from_union([to_float, from_none], self.percent_remaining)
        if self.quota_id is not None:
            result["quota_id"] = from_union([from_str, from_none], self.quota_id)
        if self.quota_remaining is not None:
            result["quota_remaining"] = from_union([to_float, from_none], self.quota_remaining)
        if self.quota_reset_at is not None:
            result["quota_reset_at"] = from_union([to_float, from_none], self.quota_reset_at)
        if self.remaining is not None:
            result["remaining"] = from_union([to_float, from_none], self.remaining)
        if self.timestamp_utc is not None:
            result["timestamp_utc"] = from_union([from_str, from_none], self.timestamp_utc)
        if self.token_based_billing is not None:
            result["token_based_billing"] = from_union([from_bool, from_none], self.token_based_billing)
        if self.unlimited is not None:
            result["unlimited"] = from_union([from_bool, from_none], self.unlimited)
        return result

@dataclass
class DiscoveredMCPServer:
    """Schema for the `DiscoveredMcpServer` type."""

    enabled: bool
    """Whether the server is enabled (not in the disabled list)"""

    name: str
    """Server name (config key)"""

    source: McpServerSource
    """Configuration source: user, workspace, plugin, or builtin"""

    type: DiscoveredMCPServerType | None = None
    """Server transport type: stdio, http, sse, or memory"""

    @staticmethod
    def from_dict(obj: Any) -> DiscoveredMCPServer:
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = McpServerSource(obj.get("source"))
        type = from_union([DiscoveredMCPServerType, from_none], obj.get("type"))
        return DiscoveredMCPServer(enabled, name, source, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(McpServerSource, self.source)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(DiscoveredMCPServerType, x), from_none], self.type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EventLogReadRequest:
    """Cursor, batch size, and optional long-poll/filter parameters for reading session events."""

    agent_scope: EventsAgentScope | None = None
    """Agent-scope filter: 'primary' returns only main-agent events plus events whose type
    starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns
    events from all agents (matching wildcard-subscription behavior). Default is 'all' to
    preserve wildcard semantics for catch-up callers.
    """
    cursor: str | None = None
    """Opaque cursor returned by a previous read. Omit on the first call to start from the
    beginning of the session's persisted history.
    """
    max: int | None = None
    """Maximum number of events to return in this batch (1–1000, default 200)."""

    types: list[str] | EventLogTypes | None = None
    """Either '*' to receive all event types, or a non-empty list of event types to receive"""

    wait_ms: int | None = None
    """Milliseconds to wait for new events when the cursor is at the tail of history. 0
    (default) returns immediately even if no events are available. Capped at 30000ms.
    Ephemeral events that arrive during the wait are delivered in this batch but are NOT
    replayable on a subsequent read (use a non-zero waitMs in your next call to capture
    future ephemerals as they happen).
    """

    @staticmethod
    def from_dict(obj: Any) -> EventLogReadRequest:
        assert isinstance(obj, dict)
        agent_scope = from_union([EventsAgentScope, from_none], obj.get("agentScope"))
        cursor = from_union([from_str, from_none], obj.get("cursor"))
        max = from_union([from_int, from_none], obj.get("max"))
        types = from_union([lambda x: from_list(from_str, x), EventLogTypes, from_none], obj.get("types"))
        wait_ms = from_union([from_int, from_none], obj.get("waitMs"))
        return EventLogReadRequest(agent_scope, cursor, max, types, wait_ms)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.agent_scope is not None:
            result["agentScope"] = from_union([lambda x: to_enum(EventsAgentScope, x), from_none], self.agent_scope)
        if self.cursor is not None:
            result["cursor"] = from_union([from_str, from_none], self.cursor)
        if self.max is not None:
            result["max"] = from_union([from_int, from_none], self.max)
        if self.types is not None:
            result["types"] = from_union([lambda x: from_list(from_str, x), lambda x: to_enum(EventLogTypes, x), from_none], self.types)
        if self.wait_ms is not None:
            result["waitMs"] = from_union([from_int, from_none], self.wait_ms)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EventsReadResult:
    """Batch of session events returned by a read, with cursor and continuation metadata."""

    cursor: str
    """Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue
    from where this read left off. Always present, even when no events were returned.
    """
    cursor_status: EventsCursorStatus
    """Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor
    referred to an event that no longer exists in history (e.g. truncated or compacted away)
    and the read started from the beginning of the remaining history.
    """
    events: list[SessionEvent]
    """Events are delivered in two batches per read: persisted events first (in append order),
    then ephemeral events (in seq order). When `waitMs > 0` and the catch-up batches were
    empty, post-wait events follow the same two-batch ordering. Persisted and ephemeral
    events do not interleave within a single read.
    """
    has_more: bool
    """True when the read returned `max` events and more events are available immediately. When
    false, the next read with a non-zero `waitMs` will block until a new event arrives or the
    wait expires.
    """

    @staticmethod
    def from_dict(obj: Any) -> EventsReadResult:
        assert isinstance(obj, dict)
        cursor = from_str(obj.get("cursor"))
        cursor_status = EventsCursorStatus(obj.get("cursorStatus"))
        events = from_list(SessionEvent.from_dict, obj.get("events"))
        has_more = from_bool(obj.get("hasMore"))
        return EventsReadResult(cursor, cursor_status, events, has_more)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cursor"] = from_str(self.cursor)
        result["cursorStatus"] = to_enum(EventsCursorStatus, self.cursor_status)
        result["events"] = from_list(lambda x: to_class(SessionEvent, x), self.events)
        result["hasMore"] = from_bool(self.has_more)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class Extension:
    """Schema for the `Extension` type."""

    id: str
    """Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')"""

    name: str
    """Extension name (directory name)"""

    source: ExtensionSource
    """Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)"""

    status: ExtensionStatus
    """Current status: running, disabled, failed, or starting"""

    pid: int | None = None
    """Process ID if the extension is running"""

    @staticmethod
    def from_dict(obj: Any) -> Extension:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        source = ExtensionSource(obj.get("source"))
        status = ExtensionStatus(obj.get("status"))
        pid = from_union([from_int, from_none], obj.get("pid"))
        return Extension(id, name, source, status, pid)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(ExtensionSource, self.source)
        result["status"] = to_enum(ExtensionStatus, self.status)
        if self.pid is not None:
            result["pid"] = from_union([from_int, from_none], self.pid)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmBinaryResultsForLlm:
    """Binary result returned by a tool for the model"""

    data: str
    """Base64-encoded binary data"""

    mime_type: str
    """MIME type of the binary data"""

    type: ExternalToolTextResultForLlmBinaryResultsForLlmType
    """Binary result type discriminator. Use "image" for images and "resource" for other binary
    data.
    """
    description: str | None = None
    """Human-readable description of the binary data"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmBinaryResultsForLlm:
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        type = ExternalToolTextResultForLlmBinaryResultsForLlmType(obj.get("type"))
        description = from_union([from_str, from_none], obj.get("description"))
        return ExternalToolTextResultForLlmBinaryResultsForLlm(data, mime_type, type, description)

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = to_enum(ExternalToolTextResultForLlmBinaryResultsForLlmType, self.type)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentResourceLinkIcon:
    """Icon image for a resource"""

    src: str
    """URL or path to the icon image"""

    mime_type: str | None = None
    """MIME type of the icon image"""

    sizes: list[str] | None = None
    """Available icon sizes (e.g., ['16x16', '32x32'])"""

    theme: ExternalToolTextResultForLlmContentResourceLinkIconTheme | None = None
    """Theme variant this icon is intended for"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentResourceLinkIcon:
        assert isinstance(obj, dict)
        src = from_str(obj.get("src"))
        mime_type = from_union([from_str, from_none], obj.get("mimeType"))
        sizes = from_union([lambda x: from_list(from_str, x), from_none], obj.get("sizes"))
        theme = from_union([ExternalToolTextResultForLlmContentResourceLinkIconTheme, from_none], obj.get("theme"))
        return ExternalToolTextResultForLlmContentResourceLinkIcon(src, mime_type, sizes, theme)

    def to_dict(self) -> dict:
        result: dict = {}
        result["src"] = from_str(self.src)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_str, from_none], self.mime_type)
        if self.sizes is not None:
            result["sizes"] = from_union([lambda x: from_list(from_str, x), from_none], self.sizes)
        if self.theme is not None:
            result["theme"] = from_union([lambda x: to_enum(ExternalToolTextResultForLlmContentResourceLinkIconTheme, x), from_none], self.theme)
        return result

ExternalToolTextResultForLlmContentResourceDetails = EmbeddedTextResourceContents | EmbeddedBlobResourceContents

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentAudio:
    """Audio content block with base64-encoded data"""

    data: str
    """Base64-encoded audio data"""

    mime_type: str
    """MIME type of the audio (e.g., audio/wav, audio/mpeg)"""

    type: ClassVar[str] = "audio"
    """Content block type discriminator"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentAudio:
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        return ExternalToolTextResultForLlmContentAudio(data, mime_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentImage:
    """Image content block with base64-encoded data"""

    data: str
    """Base64-encoded image data"""

    mime_type: str
    """MIME type of the image (e.g., image/png, image/jpeg)"""

    type: ClassVar[str] = "image"
    """Content block type discriminator"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentImage:
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        return ExternalToolTextResultForLlmContentImage(data, mime_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentResource:
    """Embedded resource content block with inline text or binary data"""

    resource: ExternalToolTextResultForLlmContentResourceDetails
    """The embedded resource contents, either text or base64-encoded binary"""

    type: ClassVar[str] = "resource"
    """Content block type discriminator"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentResource:
        assert isinstance(obj, dict)
        resource = (lambda x: from_union([EmbeddedTextResourceContents.from_dict, EmbeddedBlobResourceContents.from_dict], x))(obj.get("resource"))
        return ExternalToolTextResultForLlmContentResource(resource)

    def to_dict(self) -> dict:
        result: dict = {}
        result["resource"] = from_union([lambda x: to_class(EmbeddedTextResourceContents, x), lambda x: to_class(EmbeddedBlobResourceContents, x)], self.resource)
        result["type"] = self.type
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentTerminal:
    """Terminal/shell output content block with optional exit code and working directory"""

    text: str
    """Terminal/shell output text"""

    type: ClassVar[str] = "terminal"
    """Content block type discriminator"""

    cwd: str | None = None
    """Working directory where the command was executed"""

    exit_code: int | None = None
    """Process exit code, if the command has completed"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentTerminal:
        assert isinstance(obj, dict)
        text = from_str(obj.get("text"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        exit_code = from_union([from_int, from_none], obj.get("exitCode"))
        return ExternalToolTextResultForLlmContentTerminal(text, cwd, exit_code)

    def to_dict(self) -> dict:
        result: dict = {}
        result["text"] = from_str(self.text)
        result["type"] = self.type
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.exit_code is not None:
            result["exitCode"] = from_union([from_int, from_none], self.exit_code)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentText:
    """Plain text content block"""

    text: str
    """The text content"""

    type: ClassVar[str] = "text"
    """Content block type discriminator"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentText:
        assert isinstance(obj, dict)
        text = from_str(obj.get("text"))
        return ExternalToolTextResultForLlmContentText(text)

    def to_dict(self) -> dict:
        result: dict = {}
        result["text"] = from_str(self.text)
        result["type"] = self.type
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandTextResult:
    """Schema for the `SlashCommandTextResult` type."""

    kind: ClassVar[str] = "text"
    """Text result discriminator"""

    text: str
    """Text output for the client to render"""

    markdown: bool | None = None
    """Whether text contains Markdown"""

    preserve_ansi: bool | None = None
    """Whether ANSI sequences should be preserved"""

    runtime_settings_changed: bool | None = None
    """True when the invocation mutated user runtime settings; consumers caching settings should
    refresh
    """

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandTextResult:
        assert isinstance(obj, dict)
        text = from_str(obj.get("text"))
        markdown = from_union([from_bool, from_none], obj.get("markdown"))
        preserve_ansi = from_union([from_bool, from_none], obj.get("preserveAnsi"))
        runtime_settings_changed = from_union([from_bool, from_none], obj.get("runtimeSettingsChanged"))
        return SlashCommandTextResult(text, markdown, preserve_ansi, runtime_settings_changed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["text"] = from_str(self.text)
        if self.markdown is not None:
            result["markdown"] = from_union([from_bool, from_none], self.markdown)
        if self.preserve_ansi is not None:
            result["preserveAnsi"] = from_union([from_bool, from_none], self.preserve_ansi)
        if self.runtime_settings_changed is not None:
            result["runtimeSettingsChanged"] = from_union([from_bool, from_none], self.runtime_settings_changed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HistoryCompactResult:
    """Compaction outcome with the number of tokens and messages removed, summary text, and the
    resulting context window breakdown.
    """
    messages_removed: int
    """Number of messages removed during compaction"""

    success: bool
    """Whether compaction completed successfully"""

    tokens_removed: int
    """Number of tokens freed by compaction"""

    context_window: HistoryCompactContextWindow | None = None
    """Post-compaction context window usage breakdown"""

    summary_content: str | None = None
    """Summary text produced by compaction. Omitted when compaction did not produce a summary
    (e.g. failure path).
    """

    @staticmethod
    def from_dict(obj: Any) -> HistoryCompactResult:
        assert isinstance(obj, dict)
        messages_removed = from_int(obj.get("messagesRemoved"))
        success = from_bool(obj.get("success"))
        tokens_removed = from_int(obj.get("tokensRemoved"))
        context_window = from_union([HistoryCompactContextWindow.from_dict, from_none], obj.get("contextWindow"))
        summary_content = from_union([from_str, from_none], obj.get("summaryContent"))
        return HistoryCompactResult(messages_removed, success, tokens_removed, context_window, summary_content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["messagesRemoved"] = from_int(self.messages_removed)
        result["success"] = from_bool(self.success)
        result["tokensRemoved"] = from_int(self.tokens_removed)
        if self.context_window is not None:
            result["contextWindow"] = from_union([lambda x: to_class(HistoryCompactContextWindow, x), from_none], self.context_window)
        if self.summary_content is not None:
            result["summaryContent"] = from_union([from_str, from_none], self.summary_content)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstalledPluginSourceGithub:
    """Schema for the `InstalledPluginSourceGithub` type."""

    repo: str
    source: FluffySource
    """Constant value. Always "github"."""

    path: str | None = None
    ref: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> InstalledPluginSourceGithub:
        assert isinstance(obj, dict)
        repo = from_str(obj.get("repo"))
        source = FluffySource(obj.get("source"))
        path = from_union([from_str, from_none], obj.get("path"))
        ref = from_union([from_str, from_none], obj.get("ref"))
        return InstalledPluginSourceGithub(repo, source, path, ref)

    def to_dict(self) -> dict:
        result: dict = {}
        result["repo"] = from_str(self.repo)
        result["source"] = to_enum(FluffySource, self.source)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.ref is not None:
            result["ref"] = from_union([from_str, from_none], self.ref)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionInstalledPluginSourceGithub:
    """Schema for the `SessionInstalledPluginSourceGithub` type."""

    repo: str
    source: FluffySource
    """Constant value. Always "github"."""

    path: str | None = None
    ref: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> SessionInstalledPluginSourceGithub:
        assert isinstance(obj, dict)
        repo = from_str(obj.get("repo"))
        source = FluffySource(obj.get("source"))
        path = from_union([from_str, from_none], obj.get("path"))
        ref = from_union([from_str, from_none], obj.get("ref"))
        return SessionInstalledPluginSourceGithub(repo, source, path, ref)

    def to_dict(self) -> dict:
        result: dict = {}
        result["repo"] = from_str(self.repo)
        result["source"] = to_enum(FluffySource, self.source)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.ref is not None:
            result["ref"] = from_union([from_str, from_none], self.ref)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstalledPluginSourceLocal:
    """Schema for the `InstalledPluginSourceLocal` type."""

    path: str
    source: TentacledSource
    """Constant value. Always "local"."""

    @staticmethod
    def from_dict(obj: Any) -> InstalledPluginSourceLocal:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        source = TentacledSource(obj.get("source"))
        return InstalledPluginSourceLocal(path, source)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["source"] = to_enum(TentacledSource, self.source)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionInstalledPluginSourceLocal:
    """Schema for the `SessionInstalledPluginSourceLocal` type."""

    path: str
    source: TentacledSource
    """Constant value. Always "local"."""

    @staticmethod
    def from_dict(obj: Any) -> SessionInstalledPluginSourceLocal:
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        source = TentacledSource(obj.get("source"))
        return SessionInstalledPluginSourceLocal(path, source)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["source"] = to_enum(TentacledSource, self.source)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstalledPluginSourceURL:
    """Schema for the `InstalledPluginSourceUrl` type."""

    source: StickySource
    """Constant value. Always "url"."""

    url: str
    path: str | None = None
    ref: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> InstalledPluginSourceURL:
        assert isinstance(obj, dict)
        source = StickySource(obj.get("source"))
        url = from_str(obj.get("url"))
        path = from_union([from_str, from_none], obj.get("path"))
        ref = from_union([from_str, from_none], obj.get("ref"))
        return InstalledPluginSourceURL(source, url, path, ref)

    def to_dict(self) -> dict:
        result: dict = {}
        result["source"] = to_enum(StickySource, self.source)
        result["url"] = from_str(self.url)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.ref is not None:
            result["ref"] = from_union([from_str, from_none], self.ref)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionInstalledPluginSourceURL:
    """Schema for the `SessionInstalledPluginSourceUrl` type."""

    source: StickySource
    """Constant value. Always "url"."""

    url: str
    path: str | None = None
    ref: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> SessionInstalledPluginSourceURL:
        assert isinstance(obj, dict)
        source = StickySource(obj.get("source"))
        url = from_str(obj.get("url"))
        path = from_union([from_str, from_none], obj.get("path"))
        ref = from_union([from_str, from_none], obj.get("ref"))
        return SessionInstalledPluginSourceURL(source, url, path, ref)

    def to_dict(self) -> dict:
        result: dict = {}
        result["source"] = to_enum(StickySource, self.source)
        result["url"] = from_str(self.url)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.ref is not None:
            result["ref"] = from_union([from_str, from_none], self.ref)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstructionsSources:
    """Schema for the `InstructionsSources` type."""

    content: str
    """Raw content of the instruction file"""

    id: str
    """Unique identifier for this source (used for toggling)"""

    label: str
    """Human-readable label"""

    location: InstructionsSourcesLocation
    """Where this source lives — used for UI grouping"""

    source_path: str
    """File path relative to repo or absolute for home"""

    type: InstructionsSourcesType
    """Category of instruction source — used for merge logic"""

    apply_to: list[str] | None = None
    """Glob pattern(s) from frontmatter — when set, this instruction applies only to matching
    files
    """
    default_disabled: bool | None = None
    """When true, this source starts disabled and must be toggled on by the user"""

    description: str | None = None
    """Short description (body after frontmatter) for use in instruction tables"""

    @staticmethod
    def from_dict(obj: Any) -> InstructionsSources:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        id = from_str(obj.get("id"))
        label = from_str(obj.get("label"))
        location = InstructionsSourcesLocation(obj.get("location"))
        source_path = from_str(obj.get("sourcePath"))
        type = InstructionsSourcesType(obj.get("type"))
        apply_to = from_union([lambda x: from_list(from_str, x), from_none], obj.get("applyTo"))
        default_disabled = from_union([from_bool, from_none], obj.get("defaultDisabled"))
        description = from_union([from_str, from_none], obj.get("description"))
        return InstructionsSources(content, id, label, location, source_path, type, apply_to, default_disabled, description)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["id"] = from_str(self.id)
        result["label"] = from_str(self.label)
        result["location"] = to_enum(InstructionsSourcesLocation, self.location)
        result["sourcePath"] = from_str(self.source_path)
        result["type"] = to_enum(InstructionsSourcesType, self.type)
        if self.apply_to is not None:
            result["applyTo"] = from_union([lambda x: from_list(from_str, x), from_none], self.apply_to)
        if self.default_disabled is not None:
            result["defaultDisabled"] = from_union([from_bool, from_none], self.default_disabled)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class LogRequest:
    """Message text, optional severity level, persistence flag, optional follow-up URL, and
    optional tip.
    """
    message: str
    """Human-readable message"""

    ephemeral: bool | None = None
    """When true, the message is transient and not persisted to the session event log on disk"""

    level: SessionLogLevel | None = None
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    tip: str | None = None
    """Optional actionable tip displayed alongside the message. Only honored on `level: "info"`."""

    type: str | None = None
    """Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps
    to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".
    """
    url: str | None = None
    """Optional URL the user can open in their browser for more details"""

    @staticmethod
    def from_dict(obj: Any) -> LogRequest:
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        ephemeral = from_union([from_bool, from_none], obj.get("ephemeral"))
        level = from_union([SessionLogLevel, from_none], obj.get("level"))
        tip = from_union([from_str, from_none], obj.get("tip"))
        type = from_union([from_str, from_none], obj.get("type"))
        url = from_union([from_str, from_none], obj.get("url"))
        return LogRequest(message, ephemeral, level, tip, type, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.ephemeral is not None:
            result["ephemeral"] = from_union([from_bool, from_none], self.ephemeral)
        if self.level is not None:
            result["level"] = from_union([lambda x: to_enum(SessionLogLevel, x), from_none], self.level)
        if self.tip is not None:
            result["tip"] = from_union([from_str, from_none], self.tip)
        if self.type is not None:
            result["type"] = from_union([from_str, from_none], self.type)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

@dataclass
class MCPServerConfig:
    """MCP server configuration (stdio process or remote HTTP/SSE)

    Stdio MCP server configuration launched as a child process.

    Remote MCP server configuration accessed over HTTP or SSE.
    """
    args: list[str] | None = None
    """Command-line arguments passed to the Stdio MCP server process."""

    command: str | None = None
    """Executable command used to start the Stdio MCP server process."""

    cwd: str | None = None
    """Working directory for the Stdio MCP server process."""

    env: dict[str, str] | None = None
    """Environment variables to pass to the Stdio MCP server process."""

    filter_mapping: dict[str, ContentFilterMode] | ContentFilterMode | None = None
    """Content filtering mode to apply to all tools, or a map of tool name to content filtering
    mode.
    """
    is_default_server: bool | None = None
    """Whether this server is a built-in fallback used when the user has not configured their
    own server.
    """
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    auth: MCPServerConfigHTTPAuth | None = None
    """Additional authentication configuration for this server."""

    headers: dict[str, str] | None = None
    """HTTP headers to include in requests to the remote MCP server."""

    oauth_client_id: str | None = None
    """OAuth client ID for a pre-registered remote MCP OAuth client."""

    oauth_grant_type: MCPServerConfigHTTPOauthGrantType | None = None
    """OAuth grant type to use when authenticating to the remote MCP server."""

    oauth_public_client: bool | None = None
    """Whether the configured OAuth client is public and does not require a client secret."""

    type: MCPServerConfigHTTPType | None = None
    """Remote transport type. Defaults to "http" when omitted."""

    url: str | None = None
    """URL of the remote MCP server endpoint."""

    @staticmethod
    def from_dict(obj: Any) -> MCPServerConfig:
        assert isinstance(obj, dict)
        args = from_union([lambda x: from_list(from_str, x), from_none], obj.get("args"))
        command = from_union([from_str, from_none], obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(ContentFilterMode, x), ContentFilterMode, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        auth = from_union([MCPServerConfigHTTPAuth.from_dict, from_none], obj.get("auth"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_grant_type = from_union([MCPServerConfigHTTPOauthGrantType, from_none], obj.get("oauthGrantType"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        type = from_union([MCPServerConfigHTTPType, from_none], obj.get("type"))
        url = from_union([from_str, from_none], obj.get("url"))
        return MCPServerConfig(args, command, cwd, env, filter_mapping, is_default_server, timeout, tools, auth, headers, oauth_client_id, oauth_grant_type, oauth_public_client, type, url)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.args is not None:
            result["args"] = from_union([lambda x: from_list(from_str, x), from_none], self.args)
        if self.command is not None:
            result["command"] = from_union([from_str, from_none], self.command)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.env is not None:
            result["env"] = from_union([lambda x: from_dict(from_str, x), from_none], self.env)
        if self.filter_mapping is not None:
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(ContentFilterMode, x), x), lambda x: to_enum(ContentFilterMode, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.auth is not None:
            result["auth"] = from_union([lambda x: to_class(MCPServerConfigHTTPAuth, x), from_none], self.auth)
        if self.headers is not None:
            result["headers"] = from_union([lambda x: from_dict(from_str, x), from_none], self.headers)
        if self.oauth_client_id is not None:
            result["oauthClientId"] = from_union([from_str, from_none], self.oauth_client_id)
        if self.oauth_grant_type is not None:
            result["oauthGrantType"] = from_union([lambda x: to_enum(MCPServerConfigHTTPOauthGrantType, x), from_none], self.oauth_grant_type)
        if self.oauth_public_client is not None:
            result["oauthPublicClient"] = from_union([from_bool, from_none], self.oauth_public_client)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPServerConfigHTTPType, x), from_none], self.type)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

@dataclass
class MCPServerConfigHTTP:
    """Remote MCP server configuration accessed over HTTP or SSE."""

    url: str
    """URL of the remote MCP server endpoint."""

    auth: MCPServerConfigHTTPAuth | None = None
    """Additional authentication configuration for this server."""

    filter_mapping: dict[str, ContentFilterMode] | ContentFilterMode | None = None
    """Content filtering mode to apply to all tools, or a map of tool name to content filtering
    mode.
    """
    headers: dict[str, str] | None = None
    """HTTP headers to include in requests to the remote MCP server."""

    is_default_server: bool | None = None
    """Whether this server is a built-in fallback used when the user has not configured their
    own server.
    """
    oauth_client_id: str | None = None
    """OAuth client ID for a pre-registered remote MCP OAuth client."""

    oauth_grant_type: MCPServerConfigHTTPOauthGrantType | None = None
    """OAuth grant type to use when authenticating to the remote MCP server."""

    oauth_public_client: bool | None = None
    """Whether the configured OAuth client is public and does not require a client secret."""

    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPServerConfigHTTPType | None = None
    """Remote transport type. Defaults to "http" when omitted."""

    @staticmethod
    def from_dict(obj: Any) -> MCPServerConfigHTTP:
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        auth = from_union([MCPServerConfigHTTPAuth.from_dict, from_none], obj.get("auth"))
        filter_mapping = from_union([lambda x: from_dict(ContentFilterMode, x), ContentFilterMode, from_none], obj.get("filterMapping"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_grant_type = from_union([MCPServerConfigHTTPOauthGrantType, from_none], obj.get("oauthGrantType"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPServerConfigHTTPType, from_none], obj.get("type"))
        return MCPServerConfigHTTP(url, auth, filter_mapping, headers, is_default_server, oauth_client_id, oauth_grant_type, oauth_public_client, timeout, tools, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["url"] = from_str(self.url)
        if self.auth is not None:
            result["auth"] = from_union([lambda x: to_class(MCPServerConfigHTTPAuth, x), from_none], self.auth)
        if self.filter_mapping is not None:
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(ContentFilterMode, x), x), lambda x: to_enum(ContentFilterMode, x), from_none], self.filter_mapping)
        if self.headers is not None:
            result["headers"] = from_union([lambda x: from_dict(from_str, x), from_none], self.headers)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.oauth_client_id is not None:
            result["oauthClientId"] = from_union([from_str, from_none], self.oauth_client_id)
        if self.oauth_grant_type is not None:
            result["oauthGrantType"] = from_union([lambda x: to_enum(MCPServerConfigHTTPOauthGrantType, x), from_none], self.oauth_grant_type)
        if self.oauth_public_client is not None:
            result["oauthPublicClient"] = from_union([from_bool, from_none], self.oauth_public_client)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPServerConfigHTTPType, x), from_none], self.type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPSamplingExecutionResult:
    """Outcome of an MCP sampling execution: success result, failure error, or cancellation."""

    action: MCPSamplingExecutionAction
    """Outcome of the sampling inference. 'success' produced a response; 'failure' encountered
    an error (including agent-side rejection by content filter or criteria); 'cancelled' the
    caller cancelled this execution via cancelSamplingExecution.
    """
    error: str | None = None
    """Error description, present when action='failure'."""

    result: dict[str, Any] | None = None
    """MCP CreateMessageResult payload (with optional 'tools' extension), present when
    action='success'. Treated as opaque at the schema layer; consumers should
    construct/consume it per the MCP CreateMessageResult shape.
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPSamplingExecutionResult:
        assert isinstance(obj, dict)
        action = MCPSamplingExecutionAction(obj.get("action"))
        error = from_union([from_str, from_none], obj.get("error"))
        result = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("result"))
        return MCPSamplingExecutionResult(action, error, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["action"] = to_enum(MCPSamplingExecutionAction, self.action)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.result is not None:
            result["result"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.result)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPServerList:
    """MCP servers configured for the session, with their connection status."""

    servers: list[MCPServer]
    """Configured MCP servers"""

    @staticmethod
    def from_dict(obj: Any) -> MCPServerList:
        assert isinstance(obj, dict)
        servers = from_list(MCPServer.from_dict, obj.get("servers"))
        return MCPServerList(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_list(lambda x: to_class(MCPServer, x), self.servers)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPSetEnvValueModeParams:
    """Mode controlling how MCP server env values are resolved (`direct` or `indirect`)."""

    mode: MCPSetEnvValueModeDetails
    """How environment-variable values supplied to MCP servers are resolved. "direct" passes
    literal string values; "indirect" treats values as references (e.g. names of environment
    variables on the host) that the runtime resolves before launch. Defaults to the runtime's
    startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI
    prompt mode and ACP) set this to "direct".
    """

    @staticmethod
    def from_dict(obj: Any) -> MCPSetEnvValueModeParams:
        assert isinstance(obj, dict)
        mode = MCPSetEnvValueModeDetails(obj.get("mode"))
        return MCPSetEnvValueModeParams(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(MCPSetEnvValueModeDetails, self.mode)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPSetEnvValueModeResult:
    """Env-value mode recorded on the session after the update."""

    mode: MCPSetEnvValueModeDetails
    """Mode recorded on the session after the update"""

    @staticmethod
    def from_dict(obj: Any) -> MCPSetEnvValueModeResult:
        assert isinstance(obj, dict)
        mode = MCPSetEnvValueModeDetails(obj.get("mode"))
        return MCPSetEnvValueModeResult(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(MCPSetEnvValueModeDetails, self.mode)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataContextInfoResult:
    """Token breakdown for the session's current context window, or null if uninitialized."""

    context_info: SessionContextInfo | None = None
    """Token breakdown for the current context window, or null if the session has not yet been
    initialized (no system prompt or tool metadata cached).
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataContextInfoResult:
        assert isinstance(obj, dict)
        context_info = from_union([SessionContextInfo.from_dict, from_none], obj.get("contextInfo"))
        return MetadataContextInfoResult(context_info)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.context_info is not None:
            result["contextInfo"] = from_union([lambda x: to_class(SessionContextInfo, x), from_none], self.context_info)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionWorkingDirectoryContext:
    """Updated working directory and git context. Emitted as the new payload of
    `session.context_changed`.
    """
    cwd: str
    """Current working directory path"""

    base_commit: str | None = None
    """Merge-base commit SHA (fork point from the remote default branch)"""

    branch: str | None = None
    """Current git branch name"""

    git_root: str | None = None
    """Root directory of the git repository, resolved via git rev-parse"""

    head_commit: str | None = None
    """Head commit of the current git branch"""

    host_type: HostType | None = None
    """Hosting platform type of the repository"""

    repository: str | None = None
    """Repository identifier derived from the git remote URL ("owner/name" for GitHub,
    "org/project/repo" for Azure DevOps)
    """
    repository_host: str | None = None
    """Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com")"""

    @staticmethod
    def from_dict(obj: Any) -> SessionWorkingDirectoryContext:
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        base_commit = from_union([from_str, from_none], obj.get("baseCommit"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        git_root = from_union([from_str, from_none], obj.get("gitRoot"))
        head_commit = from_union([from_str, from_none], obj.get("headCommit"))
        host_type = from_union([HostType, from_none], obj.get("hostType"))
        repository = from_union([from_str, from_none], obj.get("repository"))
        repository_host = from_union([from_str, from_none], obj.get("repositoryHost"))
        return SessionWorkingDirectoryContext(cwd, base_commit, branch, git_root, head_commit, host_type, repository, repository_host)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_str, from_none], self.base_commit)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_str, from_none], self.git_root)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_str, from_none], self.head_commit)
        if self.host_type is not None:
            result["hostType"] = from_union([lambda x: to_enum(HostType, x), from_none], self.host_type)
        if self.repository is not None:
            result["repository"] = from_union([from_str, from_none], self.repository)
        if self.repository_host is not None:
            result["repositoryHost"] = from_union([from_str, from_none], self.repository_host)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionContext:
    """Schema for the `SessionContext` type.

    Optional working-directory context used to score session relevance. When omitted the
    most-recently-modified session wins.
    """
    cwd: str
    """Most recent working directory for this session"""

    branch: str | None = None
    """Active git branch"""

    git_root: str | None = None
    """Git repository root, if the cwd was inside a git repo"""

    host_type: HostType | None = None
    """Repository host type"""

    repository: str | None = None
    """Repository slug in `owner/name` form, when known"""

    @staticmethod
    def from_dict(obj: Any) -> SessionContext:
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        git_root = from_union([from_str, from_none], obj.get("gitRoot"))
        host_type = from_union([HostType, from_none], obj.get("hostType"))
        repository = from_union([from_str, from_none], obj.get("repository"))
        return SessionContext(cwd, branch, git_root, host_type, repository)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_str, from_none], self.git_root)
        if self.host_type is not None:
            result["hostType"] = from_union([lambda x: to_enum(HostType, x), from_none], self.host_type)
        if self.repository is not None:
            result["repository"] = from_union([from_str, from_none], self.repository)
        return result

@dataclass
class Workspace:
    id: str
    branch: str | None = None
    chronicle_sync_dismissed: bool | None = None
    created_at: datetime | None = None
    cwd: str | None = None
    git_root: str | None = None
    host_type: HostType | None = None
    """Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration."""

    mc_last_event_id: str | None = None
    mc_session_id: str | None = None
    mc_task_id: str | None = None
    name: str | None = None
    remote_steerable: bool | None = None
    repository: str | None = None
    summary_count: int | None = None
    updated_at: datetime | None = None
    user_named: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> Workspace:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        chronicle_sync_dismissed = from_union([from_bool, from_none], obj.get("chronicle_sync_dismissed"))
        created_at = from_union([from_datetime, from_none], obj.get("created_at"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        git_root = from_union([from_str, from_none], obj.get("git_root"))
        host_type = from_union([HostType, from_none], obj.get("host_type"))
        mc_last_event_id = from_union([from_str, from_none], obj.get("mc_last_event_id"))
        mc_session_id = from_union([from_str, from_none], obj.get("mc_session_id"))
        mc_task_id = from_union([from_str, from_none], obj.get("mc_task_id"))
        name = from_union([from_str, from_none], obj.get("name"))
        remote_steerable = from_union([from_bool, from_none], obj.get("remote_steerable"))
        repository = from_union([from_str, from_none], obj.get("repository"))
        summary_count = from_union([from_int, from_none], obj.get("summary_count"))
        updated_at = from_union([from_datetime, from_none], obj.get("updated_at"))
        user_named = from_union([from_bool, from_none], obj.get("user_named"))
        return Workspace(id, branch, chronicle_sync_dismissed, created_at, cwd, git_root, host_type, mc_last_event_id, mc_session_id, mc_task_id, name, remote_steerable, repository, summary_count, updated_at, user_named)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.chronicle_sync_dismissed is not None:
            result["chronicle_sync_dismissed"] = from_union([from_bool, from_none], self.chronicle_sync_dismissed)
        if self.created_at is not None:
            result["created_at"] = from_union([lambda x: x.isoformat(), from_none], self.created_at)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.git_root is not None:
            result["git_root"] = from_union([from_str, from_none], self.git_root)
        if self.host_type is not None:
            result["host_type"] = from_union([lambda x: to_enum(HostType, x), from_none], self.host_type)
        if self.mc_last_event_id is not None:
            result["mc_last_event_id"] = from_union([from_str, from_none], self.mc_last_event_id)
        if self.mc_session_id is not None:
            result["mc_session_id"] = from_union([from_str, from_none], self.mc_session_id)
        if self.mc_task_id is not None:
            result["mc_task_id"] = from_union([from_str, from_none], self.mc_task_id)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.remote_steerable is not None:
            result["remote_steerable"] = from_union([from_bool, from_none], self.remote_steerable)
        if self.repository is not None:
            result["repository"] = from_union([from_str, from_none], self.repository)
        if self.summary_count is not None:
            result["summary_count"] = from_union([from_int, from_none], self.summary_count)
        if self.updated_at is not None:
            result["updated_at"] = from_union([lambda x: x.isoformat(), from_none], self.updated_at)
        if self.user_named is not None:
            result["user_named"] = from_union([from_bool, from_none], self.user_named)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataSnapshotRemoteMetadata:
    """Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are
    immutable for the lifetime of the session.
    """
    repository: MetadataSnapshotRemoteMetadataRepository
    """The repository the remote session targets."""

    pull_request_number: int | None = None
    """The pull request number the remote session is associated with, if any."""

    resource_id: str | None = None
    """The original resource identifier (task ID or PR node ID), preserved across event-replay
    reconstructions. Falls back to `sessionId` when absent.
    """
    task_type: MetadataSnapshotRemoteMetadataTaskType | None = None
    """Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote`
    invocation.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataSnapshotRemoteMetadata:
        assert isinstance(obj, dict)
        repository = MetadataSnapshotRemoteMetadataRepository.from_dict(obj.get("repository"))
        pull_request_number = from_union([from_int, from_none], obj.get("pullRequestNumber"))
        resource_id = from_union([from_str, from_none], obj.get("resourceId"))
        task_type = from_union([MetadataSnapshotRemoteMetadataTaskType, from_none], obj.get("taskType"))
        return MetadataSnapshotRemoteMetadata(repository, pull_request_number, resource_id, task_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["repository"] = to_class(MetadataSnapshotRemoteMetadataRepository, self.repository)
        if self.pull_request_number is not None:
            result["pullRequestNumber"] = from_union([from_int, from_none], self.pull_request_number)
        if self.resource_id is not None:
            result["resourceId"] = from_union([from_str, from_none], self.resource_id)
        if self.task_type is not None:
            result["taskType"] = from_union([lambda x: to_enum(MetadataSnapshotRemoteMetadataTaskType, x), from_none], self.task_type)
        return result

@dataclass
class ModelBilling:
    """Billing information"""

    multiplier: float | None = None
    """Billing cost multiplier relative to the base rate"""

    token_prices: ModelBillingTokenPrices | None = None
    """Token-level pricing information for this model"""

    @staticmethod
    def from_dict(obj: Any) -> ModelBilling:
        assert isinstance(obj, dict)
        multiplier = from_union([from_float, from_none], obj.get("multiplier"))
        token_prices = from_union([ModelBillingTokenPrices.from_dict, from_none], obj.get("tokenPrices"))
        return ModelBilling(multiplier, token_prices)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.multiplier is not None:
            result["multiplier"] = from_union([to_float, from_none], self.multiplier)
        if self.token_prices is not None:
            result["tokenPrices"] = from_union([lambda x: to_class(ModelBillingTokenPrices, x), from_none], self.token_prices)
        return result

@dataclass
class ModelCapabilitiesLimits:
    """Token limits for prompts, outputs, and context window"""

    max_context_window_tokens: int | None = None
    """Maximum total context window size in tokens"""

    max_output_tokens: int | None = None
    """Maximum number of output/completion tokens"""

    max_prompt_tokens: int | None = None
    """Maximum number of prompt/input tokens"""

    vision: ModelCapabilitiesLimitsVision | None = None
    """Vision-specific limits"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesLimits:
        assert isinstance(obj, dict)
        max_context_window_tokens = from_union([from_int, from_none], obj.get("max_context_window_tokens"))
        max_output_tokens = from_union([from_int, from_none], obj.get("max_output_tokens"))
        max_prompt_tokens = from_union([from_int, from_none], obj.get("max_prompt_tokens"))
        vision = from_union([ModelCapabilitiesLimitsVision.from_dict, from_none], obj.get("vision"))
        return ModelCapabilitiesLimits(max_context_window_tokens, max_output_tokens, max_prompt_tokens, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.max_context_window_tokens is not None:
            result["max_context_window_tokens"] = from_union([from_int, from_none], self.max_context_window_tokens)
        if self.max_output_tokens is not None:
            result["max_output_tokens"] = from_union([from_int, from_none], self.max_output_tokens)
        if self.max_prompt_tokens is not None:
            result["max_prompt_tokens"] = from_union([from_int, from_none], self.max_prompt_tokens)
        if self.vision is not None:
            result["vision"] = from_union([lambda x: to_class(ModelCapabilitiesLimitsVision, x), from_none], self.vision)
        return result

@dataclass
class ModelPolicy:
    """Policy state (if applicable)"""

    state: ModelPolicyState
    """Current policy state for this model"""

    terms: str | None = None
    """Usage terms or conditions for this model"""

    @staticmethod
    def from_dict(obj: Any) -> ModelPolicy:
        assert isinstance(obj, dict)
        state = ModelPolicyState(obj.get("state"))
        terms = from_union([from_str, from_none], obj.get("terms"))
        return ModelPolicy(state, terms)

    def to_dict(self) -> dict:
        result: dict = {}
        result["state"] = to_enum(ModelPolicyState, self.state)
        if self.terms is not None:
            result["terms"] = from_union([from_str, from_none], self.terms)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelCapabilitiesOverrideLimits:
    """Token limits for prompts, outputs, and context window"""

    max_context_window_tokens: int | None = None
    """Maximum total context window size in tokens"""

    max_output_tokens: int | None = None
    """Maximum number of output/completion tokens"""

    max_prompt_tokens: int | None = None
    """Maximum number of prompt/input tokens"""

    vision: ModelCapabilitiesOverrideLimitsVision | None = None
    """Vision-specific limits"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesOverrideLimits:
        assert isinstance(obj, dict)
        max_context_window_tokens = from_union([from_int, from_none], obj.get("max_context_window_tokens"))
        max_output_tokens = from_union([from_int, from_none], obj.get("max_output_tokens"))
        max_prompt_tokens = from_union([from_int, from_none], obj.get("max_prompt_tokens"))
        vision = from_union([ModelCapabilitiesOverrideLimitsVision.from_dict, from_none], obj.get("vision"))
        return ModelCapabilitiesOverrideLimits(max_context_window_tokens, max_output_tokens, max_prompt_tokens, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.max_context_window_tokens is not None:
            result["max_context_window_tokens"] = from_union([from_int, from_none], self.max_context_window_tokens)
        if self.max_output_tokens is not None:
            result["max_output_tokens"] = from_union([from_int, from_none], self.max_output_tokens)
        if self.max_prompt_tokens is not None:
            result["max_prompt_tokens"] = from_union([from_int, from_none], self.max_prompt_tokens)
        if self.vision is not None:
            result["vision"] = from_union([lambda x: to_class(ModelCapabilitiesOverrideLimitsVision, x), from_none], self.vision)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PendingPermissionRequestList:
    """List of pending permission requests reconstructed from event history."""

    items: list[PendingPermissionRequest]
    """Pending permission prompts reconstructed from the session's event history. Equivalent to
    the set of `permission.requested` events that have not yet been followed by a matching
    `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts
    that were emitted before the client attached to the session.
    """

    @staticmethod
    def from_dict(obj: Any) -> PendingPermissionRequestList:
        assert isinstance(obj, dict)
        items = from_list(PendingPermissionRequest.from_dict, obj.get("items"))
        return PendingPermissionRequestList(items)

    def to_dict(self) -> dict:
        result: dict = {}
        result["items"] = from_list(lambda x: to_class(PendingPermissionRequest, x), self.items)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocation:
    """Schema for the `PermissionDecisionApproveForLocation` type."""

    approval: PermissionDecisionApproveForLocationApproval
    """Approval to persist for this location"""

    kind: ClassVar[str] = "approve-for-location"
    """Approve and persist for this project location"""

    location_key: str
    """Location key (git root or cwd) to persist the approval to"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocation:
        assert isinstance(obj, dict)
        approval = _load_PermissionDecisionApproveForLocationApproval(obj.get("approval"))
        location_key = from_str(obj.get("locationKey"))
        return PermissionDecisionApproveForLocation(approval, location_key)

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = (self.approval).to_dict()
        result["kind"] = self.kind
        result["locationKey"] = from_str(self.location_key)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalCommands:
    """Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type."""

    command_identifiers: list[str]
    """Command identifiers covered by this approval."""

    kind: ClassVar[str] = "commands"
    """Approval scoped to specific command identifiers."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalCommands:
        assert isinstance(obj, dict)
        command_identifiers = from_list(from_str, obj.get("commandIdentifiers"))
        return PermissionDecisionApproveForLocationApprovalCommands(command_identifiers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["commandIdentifiers"] = from_list(from_str, self.command_identifiers)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalCommands:
    """Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type."""

    command_identifiers: list[str]
    """Command identifiers covered by this approval."""

    kind: ClassVar[str] = "commands"
    """Approval scoped to specific command identifiers."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalCommands:
        assert isinstance(obj, dict)
        command_identifiers = from_list(from_str, obj.get("commandIdentifiers"))
        return PermissionDecisionApproveForSessionApprovalCommands(command_identifiers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["commandIdentifiers"] = from_list(from_str, self.command_identifiers)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsCommands:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsCommands` type."""

    command_identifiers: list[str]
    """Command identifiers covered by this approval."""

    kind: ClassVar[str] = "commands"
    """Approval scoped to specific command identifiers."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsCommands:
        assert isinstance(obj, dict)
        command_identifiers = from_list(from_str, obj.get("commandIdentifiers"))
        return PermissionsLocationsAddToolApprovalDetailsCommands(command_identifiers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["commandIdentifiers"] = from_list(from_str, self.command_identifiers)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalCustomTool:
    """Schema for the `PermissionDecisionApproveForLocationApprovalCustomTool` type."""

    kind: ClassVar[str] = "custom-tool"
    """Approval covering a custom tool."""

    tool_name: str
    """Custom tool name."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalCustomTool:
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        return PermissionDecisionApproveForLocationApprovalCustomTool(tool_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolName"] = from_str(self.tool_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalCustomTool:
    """Schema for the `PermissionDecisionApproveForSessionApprovalCustomTool` type."""

    kind: ClassVar[str] = "custom-tool"
    """Approval covering a custom tool."""

    tool_name: str
    """Custom tool name."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalCustomTool:
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        return PermissionDecisionApproveForSessionApprovalCustomTool(tool_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolName"] = from_str(self.tool_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsCustomTool:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsCustomTool` type."""

    kind: ClassVar[str] = "custom-tool"
    """Approval covering a custom tool."""

    tool_name: str
    """Custom tool name."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsCustomTool:
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        return PermissionsLocationsAddToolApprovalDetailsCustomTool(tool_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolName"] = from_str(self.tool_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalExtensionManagement:
    """Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type."""

    kind: ClassVar[str] = "extension-management"
    """Approval covering extension lifecycle operations such as enable, disable, or reload."""

    operation: str | None = None
    """Optional operation identifier; when omitted, the approval covers all extension management
    operations.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalExtensionManagement:
        assert isinstance(obj, dict)
        operation = from_union([from_str, from_none], obj.get("operation"))
        return PermissionDecisionApproveForLocationApprovalExtensionManagement(operation)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.operation is not None:
            result["operation"] = from_union([from_str, from_none], self.operation)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalExtensionManagement:
    """Schema for the `PermissionDecisionApproveForSessionApprovalExtensionManagement` type."""

    kind: ClassVar[str] = "extension-management"
    """Approval covering extension lifecycle operations such as enable, disable, or reload."""

    operation: str | None = None
    """Optional operation identifier; when omitted, the approval covers all extension management
    operations.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalExtensionManagement:
        assert isinstance(obj, dict)
        operation = from_union([from_str, from_none], obj.get("operation"))
        return PermissionDecisionApproveForSessionApprovalExtensionManagement(operation)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.operation is not None:
            result["operation"] = from_union([from_str, from_none], self.operation)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsExtensionManagement:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionManagement` type."""

    kind: ClassVar[str] = "extension-management"
    """Approval covering extension lifecycle operations such as enable, disable, or reload."""

    operation: str | None = None
    """Optional operation identifier; when omitted, the approval covers all extension management
    operations.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsExtensionManagement:
        assert isinstance(obj, dict)
        operation = from_union([from_str, from_none], obj.get("operation"))
        return PermissionsLocationsAddToolApprovalDetailsExtensionManagement(operation)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.operation is not None:
            result["operation"] = from_union([from_str, from_none], self.operation)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalMCP:
    """Schema for the `PermissionDecisionApproveForLocationApprovalMcp` type."""

    kind: ClassVar[str] = "mcp"
    """Approval covering an MCP tool."""

    server_name: str
    """MCP server name."""

    tool_name: str | None = None
    """MCP tool name, or null to cover every tool on the server."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalMCP:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        return PermissionDecisionApproveForLocationApprovalMCP(server_name, tool_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_union([from_none, from_str], self.tool_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalMCP:
    """Schema for the `PermissionDecisionApproveForSessionApprovalMcp` type."""

    kind: ClassVar[str] = "mcp"
    """Approval covering an MCP tool."""

    server_name: str
    """MCP server name."""

    tool_name: str | None = None
    """MCP tool name, or null to cover every tool on the server."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalMCP:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        return PermissionDecisionApproveForSessionApprovalMCP(server_name, tool_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_union([from_none, from_str], self.tool_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsMCP:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsMcp` type."""

    kind: ClassVar[str] = "mcp"
    """Approval covering an MCP tool."""

    server_name: str
    """MCP server name."""

    tool_name: str | None = None
    """MCP tool name, or null to cover every tool on the server."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsMCP:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        return PermissionsLocationsAddToolApprovalDetailsMCP(server_name, tool_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_union([from_none, from_str], self.tool_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalMCPSampling:
    """Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type."""

    kind: ClassVar[str] = "mcp-sampling"
    """Approval covering MCP sampling requests for a server."""

    server_name: str
    """MCP server name."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalMCPSampling:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return PermissionDecisionApproveForLocationApprovalMCPSampling(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalMCPSampling:
    """Schema for the `PermissionDecisionApproveForSessionApprovalMcpSampling` type."""

    kind: ClassVar[str] = "mcp-sampling"
    """Approval covering MCP sampling requests for a server."""

    server_name: str
    """MCP server name."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalMCPSampling:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return PermissionDecisionApproveForSessionApprovalMCPSampling(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsMCPSampling:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsMcpSampling` type."""

    kind: ClassVar[str] = "mcp-sampling"
    """Approval covering MCP sampling requests for a server."""

    server_name: str
    """MCP server name."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsMCPSampling:
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return PermissionsLocationsAddToolApprovalDetailsMCPSampling(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalMemory:
    """Schema for the `PermissionDecisionApproveForLocationApprovalMemory` type."""

    kind: ClassVar[str] = "memory"
    """Approval covering writes to long-term memory."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalMemory:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveForLocationApprovalMemory()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalMemory:
    """Schema for the `PermissionDecisionApproveForSessionApprovalMemory` type."""

    kind: ClassVar[str] = "memory"
    """Approval covering writes to long-term memory."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalMemory:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveForSessionApprovalMemory()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsMemory:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsMemory` type."""

    kind: ClassVar[str] = "memory"
    """Approval covering writes to long-term memory."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsMemory:
        assert isinstance(obj, dict)
        return PermissionsLocationsAddToolApprovalDetailsMemory()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalRead:
    """Schema for the `PermissionDecisionApproveForLocationApprovalRead` type."""

    kind: ClassVar[str] = "read"
    """Approval covering read-only filesystem operations."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalRead:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveForLocationApprovalRead()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalRead:
    """Schema for the `PermissionDecisionApproveForSessionApprovalRead` type."""

    kind: ClassVar[str] = "read"
    """Approval covering read-only filesystem operations."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalRead:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveForSessionApprovalRead()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsRead:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsRead` type."""

    kind: ClassVar[str] = "read"
    """Approval covering read-only filesystem operations."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsRead:
        assert isinstance(obj, dict)
        return PermissionsLocationsAddToolApprovalDetailsRead()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalWrite:
    """Schema for the `PermissionDecisionApproveForLocationApprovalWrite` type."""

    kind: ClassVar[str] = "write"
    """Approval covering filesystem write operations."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalWrite:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveForLocationApprovalWrite()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalWrite:
    """Schema for the `PermissionDecisionApproveForSessionApprovalWrite` type."""

    kind: ClassVar[str] = "write"
    """Approval covering filesystem write operations."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalWrite:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveForSessionApprovalWrite()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsWrite:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsWrite` type."""

    kind: ClassVar[str] = "write"
    """Approval covering filesystem write operations."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsWrite:
        assert isinstance(obj, dict)
        return PermissionsLocationsAddToolApprovalDetailsWrite()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSession:
    """Schema for the `PermissionDecisionApproveForSession` type."""

    kind: ClassVar[str] = "approve-for-session"
    """Approve and remember for the rest of the session"""

    approval: PermissionDecisionApproveForSessionApproval | None = None
    """Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)"""

    domain: str | None = None
    """URL domain to approve for the rest of the session (URL prompts only)"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSession:
        assert isinstance(obj, dict)
        approval = from_union([_load_PermissionDecisionApproveForSessionApproval, from_none], obj.get("approval"))
        domain = from_union([from_str, from_none], obj.get("domain"))
        return PermissionDecisionApproveForSession(approval, domain)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.approval is not None:
            result["approval"] = from_union([lambda x: (x).to_dict(), from_none], self.approval)
        if self.domain is not None:
            result["domain"] = from_union([from_str, from_none], self.domain)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveOnce:
    """Schema for the `PermissionDecisionApproveOnce` type."""

    kind: ClassVar[str] = "approve-once"
    """Approve this single request only"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveOnce:
        assert isinstance(obj, dict)
        return PermissionDecisionApproveOnce()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApprovePermanently:
    """Schema for the `PermissionDecisionApprovePermanently` type."""

    domain: str
    """URL domain to approve permanently"""

    kind: ClassVar[str] = "approve-permanently"
    """Approve and persist across sessions (URL prompts only)"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApprovePermanently:
        assert isinstance(obj, dict)
        domain = from_str(obj.get("domain"))
        return PermissionDecisionApprovePermanently(domain)

    def to_dict(self) -> dict:
        result: dict = {}
        result["domain"] = from_str(self.domain)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproved:
    """Schema for the `PermissionDecisionApproved` type."""

    kind: ClassVar[str] = "approved"
    """The permission request was approved"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproved:
        assert isinstance(obj, dict)
        return PermissionDecisionApproved()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApprovedForLocation:
    """Schema for the `PermissionDecisionApprovedForLocation` type."""

    approval: UserToolSessionApproval
    """The approval to persist for this location"""

    kind: ClassVar[str] = "approved-for-location"
    """Approved and persisted for this project location"""

    location_key: str
    """The location key (git root or cwd) to persist the approval to"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApprovedForLocation:
        assert isinstance(obj, dict)
        approval = UserToolSessionApproval.from_dict(obj.get("approval"))
        location_key = from_str(obj.get("locationKey"))
        return PermissionDecisionApprovedForLocation(approval, location_key)

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = to_class(UserToolSessionApproval, self.approval)
        result["kind"] = self.kind
        result["locationKey"] = from_str(self.location_key)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApprovedForSession:
    """Schema for the `PermissionDecisionApprovedForSession` type."""

    approval: UserToolSessionApproval
    """The approval to add as a session-scoped rule"""

    kind: ClassVar[str] = "approved-for-session"
    """Approved and remembered for the rest of the session"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApprovedForSession:
        assert isinstance(obj, dict)
        approval = UserToolSessionApproval.from_dict(obj.get("approval"))
        return PermissionDecisionApprovedForSession(approval)

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = to_class(UserToolSessionApproval, self.approval)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionCancelled:
    """Schema for the `PermissionDecisionCancelled` type."""

    kind: ClassVar[str] = "cancelled"
    """The permission request was cancelled before a response was used"""

    reason: str | None = None
    """Optional explanation of why the request was cancelled"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionCancelled:
        assert isinstance(obj, dict)
        reason = from_union([from_str, from_none], obj.get("reason"))
        return PermissionDecisionCancelled(reason)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.reason is not None:
            result["reason"] = from_union([from_str, from_none], self.reason)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionDeniedByContentExclusionPolicy:
    """Schema for the `PermissionDecisionDeniedByContentExclusionPolicy` type."""

    kind: ClassVar[str] = "denied-by-content-exclusion-policy"
    """Denied by the organization's content exclusion policy"""

    message: str
    """Human-readable explanation of why the path was excluded"""

    path: str
    """File path that triggered the exclusion"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionDeniedByContentExclusionPolicy:
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        path = from_str(obj.get("path"))
        return PermissionDecisionDeniedByContentExclusionPolicy(message, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["message"] = from_str(self.message)
        result["path"] = from_str(self.path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionDeniedByPermissionRequestHook:
    """Schema for the `PermissionDecisionDeniedByPermissionRequestHook` type."""

    kind: ClassVar[str] = "denied-by-permission-request-hook"
    """Denied by a permission request hook registered by an extension or plugin"""

    interrupt: bool | None = None
    """Whether to interrupt the current agent turn"""

    message: str | None = None
    """Optional message from the hook explaining the denial"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionDeniedByPermissionRequestHook:
        assert isinstance(obj, dict)
        interrupt = from_union([from_bool, from_none], obj.get("interrupt"))
        message = from_union([from_str, from_none], obj.get("message"))
        return PermissionDecisionDeniedByPermissionRequestHook(interrupt, message)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.interrupt is not None:
            result["interrupt"] = from_union([from_bool, from_none], self.interrupt)
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionDeniedByRules:
    """Schema for the `PermissionDecisionDeniedByRules` type."""

    kind: ClassVar[str] = "denied-by-rules"
    """Denied because approval rules explicitly blocked it"""

    rules: list[PermissionRule]
    """Rules that denied the request"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionDeniedByRules:
        assert isinstance(obj, dict)
        rules = from_list(PermissionRule.from_dict, obj.get("rules"))
        return PermissionDecisionDeniedByRules(rules)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["rules"] = from_list(lambda x: to_class(PermissionRule, x), self.rules)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionDeniedInteractivelyByUser:
    """Schema for the `PermissionDecisionDeniedInteractivelyByUser` type."""

    kind: ClassVar[str] = "denied-interactively-by-user"
    """Denied by the user during an interactive prompt"""

    feedback: str | None = None
    """Optional feedback from the user explaining the denial"""

    force_reject: bool | None = None
    """Whether to force-reject the current agent turn"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionDeniedInteractivelyByUser:
        assert isinstance(obj, dict)
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        force_reject = from_union([from_bool, from_none], obj.get("forceReject"))
        return PermissionDecisionDeniedInteractivelyByUser(feedback, force_reject)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.feedback is not None:
            result["feedback"] = from_union([from_str, from_none], self.feedback)
        if self.force_reject is not None:
            result["forceReject"] = from_union([from_bool, from_none], self.force_reject)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser:
    """Schema for the `PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type."""

    kind: ClassVar[str] = "denied-no-approval-rule-and-could-not-request-from-user"
    """Denied because no approval rule matched and user confirmation was unavailable"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser:
        assert isinstance(obj, dict)
        return PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionReject:
    """Schema for the `PermissionDecisionReject` type."""

    kind: ClassVar[str] = "reject"
    """Reject the request"""

    feedback: str | None = None
    """Optional feedback explaining the rejection"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionReject:
        assert isinstance(obj, dict)
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        return PermissionDecisionReject(feedback)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.feedback is not None:
            result["feedback"] = from_union([from_str, from_none], self.feedback)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionUserNotAvailable:
    """Schema for the `PermissionDecisionUserNotAvailable` type."""

    kind: ClassVar[str] = "user-not-available"
    """No user is available to confirm the request"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionUserNotAvailable:
        assert isinstance(obj, dict)
        return PermissionDecisionUserNotAvailable()

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionLocationApplyResult:
    """Summary of persisted location permissions applied to the session."""

    applied_directory_count: int
    """Number of persisted allowed directories added to the live path manager"""

    applied_rule_count: int
    """Number of location-scoped rules added to the live permission service"""

    applied_rules: list[PermissionRule]
    """Location-scoped rules applied to the live permission service"""

    changed: bool
    """Whether a different location was applied since the previous apply call"""

    location_key: str
    """Location key used in the location-permissions store"""

    location_type: PermissionLocationType
    """Whether the location is a git repo or directory"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionLocationApplyResult:
        assert isinstance(obj, dict)
        applied_directory_count = from_int(obj.get("appliedDirectoryCount"))
        applied_rule_count = from_int(obj.get("appliedRuleCount"))
        applied_rules = from_list(PermissionRule.from_dict, obj.get("appliedRules"))
        changed = from_bool(obj.get("changed"))
        location_key = from_str(obj.get("locationKey"))
        location_type = PermissionLocationType(obj.get("locationType"))
        return PermissionLocationApplyResult(applied_directory_count, applied_rule_count, applied_rules, changed, location_key, location_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["appliedDirectoryCount"] = from_int(self.applied_directory_count)
        result["appliedRuleCount"] = from_int(self.applied_rule_count)
        result["appliedRules"] = from_list(lambda x: to_class(PermissionRule, x), self.applied_rules)
        result["changed"] = from_bool(self.changed)
        result["locationKey"] = from_str(self.location_key)
        result["locationType"] = to_enum(PermissionLocationType, self.location_type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionLocationResolveResult:
    """Resolved location-permissions key and type."""

    location_key: str
    """Location key used in the location-permissions store"""

    location_type: PermissionLocationType
    """Whether the location is a git repo or directory"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionLocationResolveResult:
        assert isinstance(obj, dict)
        location_key = from_str(obj.get("locationKey"))
        location_type = PermissionLocationType(obj.get("locationType"))
        return PermissionLocationResolveResult(location_key, location_type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["locationKey"] = from_str(self.location_key)
        result["locationType"] = to_enum(PermissionLocationType, self.location_type)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsConfigureAdditionalContentExclusionPolicyRule:
    """Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRule` type."""

    paths: list[str]
    source: PermissionsConfigureAdditionalContentExclusionPolicyRuleSource
    """Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type."""

    if_any_match: list[str] | None = None
    if_none_match: list[str] | None = None

    @staticmethod
    def from_dict(obj: Any) -> PermissionsConfigureAdditionalContentExclusionPolicyRule:
        assert isinstance(obj, dict)
        paths = from_list(from_str, obj.get("paths"))
        source = PermissionsConfigureAdditionalContentExclusionPolicyRuleSource.from_dict(obj.get("source"))
        if_any_match = from_union([lambda x: from_list(from_str, x), from_none], obj.get("ifAnyMatch"))
        if_none_match = from_union([lambda x: from_list(from_str, x), from_none], obj.get("ifNoneMatch"))
        return PermissionsConfigureAdditionalContentExclusionPolicyRule(paths, source, if_any_match, if_none_match)

    def to_dict(self) -> dict:
        result: dict = {}
        result["paths"] = from_list(from_str, self.paths)
        result["source"] = to_class(PermissionsConfigureAdditionalContentExclusionPolicyRuleSource, self.source)
        if self.if_any_match is not None:
            result["ifAnyMatch"] = from_union([lambda x: from_list(from_str, x), from_none], self.if_any_match)
        if self.if_none_match is not None:
            result["ifNoneMatch"] = from_union([lambda x: from_list(from_str, x), from_none], self.if_none_match)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsModifyRulesParams:
    """Scope and add/remove instructions for modifying session- or location-scoped permission
    rules.
    """
    scope: PermissionsModifyRulesScope
    """Whether the change applies to ephemeral session-scoped rules (cleared at session end) or
    to location-scoped rules persisted via the location-permissions config file.
    """
    add: list[PermissionRule] | None = None
    """Rules to add to the scope. Applied before `remove`/`removeAll`."""

    remove: list[PermissionRule] | None = None
    """Specific rules to remove from the scope. Ignored when `removeAll` is true."""

    remove_all: bool | None = None
    """When true, removes every rule currently in the scope (after any `add` is applied). Useful
    for clearing the location scope wholesale.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionsModifyRulesParams:
        assert isinstance(obj, dict)
        scope = PermissionsModifyRulesScope(obj.get("scope"))
        add = from_union([lambda x: from_list(PermissionRule.from_dict, x), from_none], obj.get("add"))
        remove = from_union([lambda x: from_list(PermissionRule.from_dict, x), from_none], obj.get("remove"))
        remove_all = from_union([from_bool, from_none], obj.get("removeAll"))
        return PermissionsModifyRulesParams(scope, add, remove, remove_all)

    def to_dict(self) -> dict:
        result: dict = {}
        result["scope"] = to_enum(PermissionsModifyRulesScope, self.scope)
        if self.add is not None:
            result["add"] = from_union([lambda x: from_list(lambda x: to_class(PermissionRule, x), x), from_none], self.add)
        if self.remove is not None:
            result["remove"] = from_union([lambda x: from_list(lambda x: to_class(PermissionRule, x), x), from_none], self.remove)
        if self.remove_all is not None:
            result["removeAll"] = from_union([from_bool, from_none], self.remove_all)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PluginList:
    """Plugins installed for the session, with their enabled state and version metadata."""

    plugins: list[Plugin]
    """Installed plugins"""

    @staticmethod
    def from_dict(obj: Any) -> PluginList:
        assert isinstance(obj, dict)
        plugins = from_list(Plugin.from_dict, obj.get("plugins"))
        return PluginList(plugins)

    def to_dict(self) -> dict:
        result: dict = {}
        result["plugins"] = from_list(lambda x: to_class(Plugin, x), self.plugins)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class QueuePendingItems:
    """Schema for the `QueuePendingItems` type."""

    display_text: str
    """Human-readable text to display for this queue entry in the UI"""

    kind: QueuePendingItemsKind
    """Whether this item is a queued user message or a queued slash command / model change"""

    @staticmethod
    def from_dict(obj: Any) -> QueuePendingItems:
        assert isinstance(obj, dict)
        display_text = from_str(obj.get("displayText"))
        kind = QueuePendingItemsKind(obj.get("kind"))
        return QueuePendingItems(display_text, kind)

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayText"] = from_str(self.display_text)
        result["kind"] = to_enum(QueuePendingItemsKind, self.kind)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RemoteEnableRequest:
    """Optional remote session mode ("off", "export", or "on"); defaults to enabling both export
    and remote steering.
    """
    mode: RemoteSessionMode | None = None
    """Per-session remote mode. "off" disables remote, "export" exports session events to GitHub
    without enabling remote steering, "on" enables both export and remote steering.
    """

    @staticmethod
    def from_dict(obj: Any) -> RemoteEnableRequest:
        assert isinstance(obj, dict)
        mode = from_union([RemoteSessionMode, from_none], obj.get("mode"))
        return RemoteEnableRequest(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.mode is not None:
            result["mode"] = from_union([lambda x: to_enum(RemoteSessionMode, x), from_none], self.mode)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ScheduleList:
    """Snapshot of the currently active recurring prompts for this session."""

    entries: list[ScheduleEntry]
    """Active scheduled prompts, ordered by id."""

    @staticmethod
    def from_dict(obj: Any) -> ScheduleList:
        assert isinstance(obj, dict)
        entries = from_list(ScheduleEntry.from_dict, obj.get("entries"))
        return ScheduleList(entries)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entries"] = from_list(lambda x: to_class(ScheduleEntry, x), self.entries)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ScheduleStopResult:
    """Remove a scheduled prompt by id. The result entry is omitted if the id was unknown."""

    entry: ScheduleEntry | None = None
    """The removed entry, or omitted if no entry matched."""

    @staticmethod
    def from_dict(obj: Any) -> ScheduleStopResult:
        assert isinstance(obj, dict)
        entry = from_union([ScheduleEntry.from_dict, from_none], obj.get("entry"))
        return ScheduleStopResult(entry)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.entry is not None:
            result["entry"] = from_union([lambda x: to_class(ScheduleEntry, x), from_none], self.entry)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentSelectionDetails:
    """Position range of the selection within the file"""

    end: SendAttachmentSelectionDetailsEnd
    """End position of the selection"""

    start: SendAttachmentSelectionDetailsStart
    """Start position of the selection"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentSelectionDetails:
        assert isinstance(obj, dict)
        end = SendAttachmentSelectionDetailsEnd.from_dict(obj.get("end"))
        start = SendAttachmentSelectionDetailsStart.from_dict(obj.get("start"))
        return SendAttachmentSelectionDetails(end, start)

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_class(SendAttachmentSelectionDetailsEnd, self.end)
        result["start"] = to_class(SendAttachmentSelectionDetailsStart, self.start)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentBlob:
    """Blob attachment with inline base64-encoded data"""

    data: str
    """Base64-encoded content"""

    mime_type: str
    """MIME type of the inline data"""

    type: ClassVar[str] = "blob"
    """Attachment type discriminator"""

    display_name: str | None = None
    """User-facing display name for the attachment"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentBlob:
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        display_name = from_union([from_str, from_none], obj.get("displayName"))
        return SendAttachmentBlob(data, mime_type, display_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        if self.display_name is not None:
            result["displayName"] = from_union([from_str, from_none], self.display_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentFile:
    """File attachment"""

    display_name: str
    """User-facing display name for the attachment"""

    path: str
    """Absolute file path"""

    type: ClassVar[str] = "file"
    """Attachment type discriminator"""

    line_range: SendAttachmentFileLineRange | None = None
    """Optional line range to scope the attachment to a specific section of the file"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentFile:
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        path = from_str(obj.get("path"))
        line_range = from_union([SendAttachmentFileLineRange.from_dict, from_none], obj.get("lineRange"))
        return SendAttachmentFile(display_name, path, line_range)

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["path"] = from_str(self.path)
        result["type"] = self.type
        if self.line_range is not None:
            result["lineRange"] = from_union([lambda x: to_class(SendAttachmentFileLineRange, x), from_none], self.line_range)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentGithubReference:
    """GitHub issue, pull request, or discussion reference"""

    number: int
    """Issue, pull request, or discussion number"""

    reference_type: SendAttachmentGithubReferenceTypeEnum
    """Type of GitHub reference"""

    state: str
    """Current state of the referenced item (e.g., open, closed, merged)"""

    title: str
    """Title of the referenced item"""

    type: ClassVar[str] = "github_reference"
    """Attachment type discriminator"""

    url: str
    """URL to the referenced item on GitHub"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentGithubReference:
        assert isinstance(obj, dict)
        number = from_int(obj.get("number"))
        reference_type = SendAttachmentGithubReferenceTypeEnum(obj.get("referenceType"))
        state = from_str(obj.get("state"))
        title = from_str(obj.get("title"))
        url = from_str(obj.get("url"))
        return SendAttachmentGithubReference(number, reference_type, state, title, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["number"] = from_int(self.number)
        result["referenceType"] = to_enum(SendAttachmentGithubReferenceTypeEnum, self.reference_type)
        result["state"] = from_str(self.state)
        result["title"] = from_str(self.title)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendRequest:
    """Parameters for sending a user message to the session"""

    prompt: str
    """The user message text"""

    agent_mode: SendAgentMode | None = None
    """The UI mode the agent was in when this message was sent. Defaults to the session's
    current mode.
    """
    attachments: list[SendAttachment] | None = None
    """Optional attachments (files, directories, selections, blobs, GitHub references) to
    include with the message
    """
    billable: bool | None = None
    """If false, this message will not trigger a Premium Request Unit charge. User messages
    default to billable.
    """
    display_prompt: str | None = None
    """If provided, this is shown in the timeline instead of `prompt`"""

    mode: SendMode | None = None
    """How to deliver the message. `enqueue` (default) appends to the message queue. `immediate`
    interjects during an in-progress turn.
    """
    prepend: bool | None = None
    """If true, adds the message to the front of the queue instead of the end"""

    request_headers: dict[str, str] | None = None
    """Custom HTTP headers to include in outbound model requests for this turn. Merged with
    session-level provider headers; per-turn headers augment and overwrite session-level
    headers with the same key.
    """
    required_tool: str | None = None
    """If set, the request will fail if the named tool is not available when this message is
    among the user messages at the start of the current exchange
    """
    source: Any = None
    """Optional provenance tag copied to the resulting user.message event. Supported values are
    `system`, `command-*`, and `schedule-*`.
    """
    traceparent: str | None = None
    """W3C Trace Context traceparent header for distributed tracing of this agent turn"""

    tracestate: str | None = None
    """W3C Trace Context tracestate header for distributed tracing"""

    wait: bool | None = None
    """If true, await completion of the agentic loop for this message before returning. Defaults
    to false (fire-and-forget). When true, the result still contains the same `messageId`;
    the caller can rely on the agent having processed the message before the call resolves.
    """

    @staticmethod
    def from_dict(obj: Any) -> SendRequest:
        assert isinstance(obj, dict)
        prompt = from_str(obj.get("prompt"))
        agent_mode = from_union([SendAgentMode, from_none], obj.get("agentMode"))
        attachments = from_union([lambda x: from_list(_load_SendAttachment, x), from_none], obj.get("attachments"))
        billable = from_union([from_bool, from_none], obj.get("billable"))
        display_prompt = from_union([from_str, from_none], obj.get("displayPrompt"))
        mode = from_union([SendMode, from_none], obj.get("mode"))
        prepend = from_union([from_bool, from_none], obj.get("prepend"))
        request_headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("requestHeaders"))
        required_tool = from_union([from_str, from_none], obj.get("requiredTool"))
        source = obj.get("source")
        traceparent = from_union([from_str, from_none], obj.get("traceparent"))
        tracestate = from_union([from_str, from_none], obj.get("tracestate"))
        wait = from_union([from_bool, from_none], obj.get("wait"))
        return SendRequest(prompt, agent_mode, attachments, billable, display_prompt, mode, prepend, request_headers, required_tool, source, traceparent, tracestate, wait)

    def to_dict(self) -> dict:
        result: dict = {}
        result["prompt"] = from_str(self.prompt)
        if self.agent_mode is not None:
            result["agentMode"] = from_union([lambda x: to_enum(SendAgentMode, x), from_none], self.agent_mode)
        if self.attachments is not None:
            result["attachments"] = from_union([lambda x: from_list(lambda x: (x).to_dict(), x), from_none], self.attachments)
        if self.billable is not None:
            result["billable"] = from_union([from_bool, from_none], self.billable)
        if self.display_prompt is not None:
            result["displayPrompt"] = from_union([from_str, from_none], self.display_prompt)
        if self.mode is not None:
            result["mode"] = from_union([lambda x: to_enum(SendMode, x), from_none], self.mode)
        if self.prepend is not None:
            result["prepend"] = from_union([from_bool, from_none], self.prepend)
        if self.request_headers is not None:
            result["requestHeaders"] = from_union([lambda x: from_dict(from_str, x), from_none], self.request_headers)
        if self.required_tool is not None:
            result["requiredTool"] = from_union([from_str, from_none], self.required_tool)
        if self.source is not None:
            result["source"] = self.source
        if self.traceparent is not None:
            result["traceparent"] = from_union([from_str, from_none], self.traceparent)
        if self.tracestate is not None:
            result["tracestate"] = from_union([from_str, from_none], self.tracestate)
        if self.wait is not None:
            result["wait"] = from_union([from_bool, from_none], self.wait)
        return result

@dataclass
class ServerSkillList:
    """Skills discovered across global and project sources."""

    skills: list[ServerSkill]
    """All discovered skills across all sources"""

    @staticmethod
    def from_dict(obj: Any) -> ServerSkillList:
        assert isinstance(obj, dict)
        skills = from_list(ServerSkill.from_dict, obj.get("skills"))
        return ServerSkillList(skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["skills"] = from_list(lambda x: to_class(ServerSkill, x), self.skills)
        return result

@dataclass
class SessionFSError:
    """Describes a filesystem error."""

    code: SessionFSErrorCode
    """Error classification"""

    message: str | None = None
    """Free-form detail about the error, for logging/diagnostics"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSError:
        assert isinstance(obj, dict)
        code = SessionFSErrorCode(obj.get("code"))
        message = from_union([from_str, from_none], obj.get("message"))
        return SessionFSError(code, message)

    def to_dict(self) -> dict:
        result: dict = {}
        result["code"] = to_enum(SessionFSErrorCode, self.code)
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        return result

@dataclass
class SessionFSReaddirWithTypesEntry:
    """Schema for the `SessionFsReaddirWithTypesEntry` type."""

    name: str
    """Entry name"""

    type: SessionFSReaddirWithTypesEntryType
    """Entry type"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReaddirWithTypesEntry:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        type = SessionFSReaddirWithTypesEntryType(obj.get("type"))
        return SessionFSReaddirWithTypesEntry(name, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["type"] = to_enum(SessionFSReaddirWithTypesEntryType, self.type)
        return result

@dataclass
class SessionFSSetProviderRequest:
    """Initial working directory, session-state path layout, and path conventions used to
    register the calling SDK client as the session filesystem provider.
    """
    conventions: SessionFSSetProviderConventions
    """Path conventions used by this filesystem"""

    initial_cwd: str
    """Initial working directory for sessions"""

    session_state_path: str
    """Path within each session's SessionFs where the runtime stores files for that session"""

    capabilities: SessionFSSetProviderCapabilities | None = None
    """Optional capabilities declared by the provider"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSetProviderRequest:
        assert isinstance(obj, dict)
        conventions = SessionFSSetProviderConventions(obj.get("conventions"))
        initial_cwd = from_str(obj.get("initialCwd"))
        session_state_path = from_str(obj.get("sessionStatePath"))
        capabilities = from_union([SessionFSSetProviderCapabilities.from_dict, from_none], obj.get("capabilities"))
        return SessionFSSetProviderRequest(conventions, initial_cwd, session_state_path, capabilities)

    def to_dict(self) -> dict:
        result: dict = {}
        result["conventions"] = to_enum(SessionFSSetProviderConventions, self.conventions)
        result["initialCwd"] = from_str(self.initial_cwd)
        result["sessionStatePath"] = from_str(self.session_state_path)
        if self.capabilities is not None:
            result["capabilities"] = from_union([lambda x: to_class(SessionFSSetProviderCapabilities, x), from_none], self.capabilities)
        return result

@dataclass
class SessionFSSqliteQueryRequest:
    """SQL query, query type, and optional bind parameters for executing a SQLite query against
    the per-session database.
    """
    query: str
    """SQL query to execute"""

    query_type: SessionFSSqliteQueryType
    """How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT
    (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
    """
    session_id: str
    """Target session identifier"""

    params: dict[str, float | str | None] | None = None
    """Optional named bind parameters"""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSqliteQueryRequest:
        assert isinstance(obj, dict)
        query = from_str(obj.get("query"))
        query_type = SessionFSSqliteQueryType(obj.get("queryType"))
        session_id = from_str(obj.get("sessionId"))
        params = from_union([lambda x: from_dict(lambda x: from_union([from_none, from_float, from_str], x), x), from_none], obj.get("params"))
        return SessionFSSqliteQueryRequest(query, query_type, session_id, params)

    def to_dict(self) -> dict:
        result: dict = {}
        result["query"] = from_str(self.query)
        result["queryType"] = to_enum(SessionFSSqliteQueryType, self.query_type)
        result["sessionId"] = from_str(self.session_id)
        if self.params is not None:
            result["params"] = from_union([lambda x: from_dict(lambda x: from_union([from_none, to_float, from_str], x), x), from_none], self.params)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsListRequest:
    """Optional metadata-load limit and context filter applied to the returned sessions."""

    filter: SessionListFilter | None = None
    """Optional filter applied to the returned sessions"""

    metadata_limit: int | None = None
    """When provided, only the first N sessions (sorted by modification time, newest first) load
    full metadata; remaining sessions return basic info only. Use 0 to return only basic info
    for every session.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsListRequest:
        assert isinstance(obj, dict)
        filter = from_union([SessionListFilter.from_dict, from_none], obj.get("filter"))
        metadata_limit = from_union([from_int, from_none], obj.get("metadataLimit"))
        return SessionsListRequest(filter, metadata_limit)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.filter is not None:
            result["filter"] = from_union([lambda x: to_class(SessionListFilter, x), from_none], self.filter)
        if self.metadata_limit is not None:
            result["metadataLimit"] = from_union([from_int, from_none], self.metadata_limit)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ShellKillRequest:
    """Identifier of a process previously returned by "shell.exec" and the signal to send."""

    process_id: str
    """Process identifier returned by shell.exec"""

    signal: ShellKillSignal | None = None
    """Signal to send (default: SIGTERM)"""

    @staticmethod
    def from_dict(obj: Any) -> ShellKillRequest:
        assert isinstance(obj, dict)
        process_id = from_str(obj.get("processId"))
        signal = from_union([ShellKillSignal, from_none], obj.get("signal"))
        return ShellKillRequest(process_id, signal)

    def to_dict(self) -> dict:
        result: dict = {}
        result["processId"] = from_str(self.process_id)
        if self.signal is not None:
            result["signal"] = from_union([lambda x: to_enum(ShellKillSignal, x), from_none], self.signal)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentInfo:
    """Schema for the `AgentInfo` type.

    The newly selected custom agent
    """
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    id: str
    """Stable identifier for selection. For most agents this is the same as `name`; for
    plugin/builtin agents it may differ. Always populated; defaults to `name` when no
    distinct id was assigned.
    """
    name: str
    """Unique identifier of the custom agent"""

    mcp_servers: dict[str, Any] | None = None
    """MCP server configurations attached to this agent, keyed by server name. Server config
    shape mirrors the MCP `mcpServers` schema.
    """
    model: str | None = None
    """Preferred model id for this agent. When omitted, inherits the outer agent's model."""

    path: str | None = None
    """Absolute local file path of the agent definition. Only set for file-based agents loaded
    from disk; remote agents do not have a path.
    """
    skills: list[str] | None = None
    """Skill names preloaded into this agent's context. Omitted means none."""

    source: AgentInfoSource | None = None
    """Where the agent definition was loaded from"""

    tools: list[str] | None = None
    """Allowed tool names for this agent. Empty array means none; omitted means inherit defaults."""

    user_invocable: bool | None = None
    """Whether the agent can be selected directly by the user. Agents marked `false` are
    subagent-only.
    """

    @staticmethod
    def from_dict(obj: Any) -> AgentInfo:
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        mcp_servers = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("mcpServers"))
        model = from_union([from_str, from_none], obj.get("model"))
        path = from_union([from_str, from_none], obj.get("path"))
        skills = from_union([lambda x: from_list(from_str, x), from_none], obj.get("skills"))
        source = from_union([AgentInfoSource, from_none], obj.get("source"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        user_invocable = from_union([from_bool, from_none], obj.get("userInvocable"))
        return AgentInfo(description, display_name, id, name, mcp_servers, model, path, skills, source, tools, user_invocable)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        if self.mcp_servers is not None:
            result["mcpServers"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.mcp_servers)
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.skills is not None:
            result["skills"] = from_union([lambda x: from_list(from_str, x), from_none], self.skills)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_enum(AgentInfoSource, x), from_none], self.source)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.user_invocable is not None:
            result["userInvocable"] = from_union([from_bool, from_none], self.user_invocable)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillList:
    """Skills available to the session, with their enabled state."""

    skills: list[Skill]
    """Available skills"""

    @staticmethod
    def from_dict(obj: Any) -> SkillList:
        assert isinstance(obj, dict)
        skills = from_list(Skill.from_dict, obj.get("skills"))
        return SkillList(skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["skills"] = from_list(lambda x: to_class(Skill, x), self.skills)
        return result

@dataclass
class SkillsConfigSetDisabledSkillsRequest:
    """Skill names to mark as disabled in global configuration, replacing any previous list."""

    disabled_skills: list[str]
    """List of skill names to disable"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsConfigSetDisabledSkillsRequest:
        assert isinstance(obj, dict)
        disabled_skills = from_list(from_str, obj.get("disabledSkills"))
        return SkillsConfigSetDisabledSkillsRequest(disabled_skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["disabledSkills"] = from_list(from_str, self.disabled_skills)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillsGetInvokedResult:
    """Skills invoked during this session, ordered by invocation time (most recent last)."""

    skills: list[SkillsInvokedSkill]
    """Skills invoked during this session, ordered by invocation time (most recent last)"""

    @staticmethod
    def from_dict(obj: Any) -> SkillsGetInvokedResult:
        assert isinstance(obj, dict)
        skills = from_list(SkillsInvokedSkill.from_dict, obj.get("skills"))
        return SkillsGetInvokedResult(skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["skills"] = from_list(lambda x: to_class(SkillsInvokedSkill, x), self.skills)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandAgentPromptResult:
    """Schema for the `SlashCommandAgentPromptResult` type."""

    display_prompt: str
    """Prompt text to display to the user"""

    kind: ClassVar[str] = "agent-prompt"
    """Agent prompt result discriminator"""

    prompt: str
    """Prompt to submit to the agent"""

    mode: SessionMode | None = None
    """Optional target session mode for the agent prompt"""

    runtime_settings_changed: bool | None = None
    """True when the invocation mutated user runtime settings; consumers caching settings should
    refresh
    """

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandAgentPromptResult:
        assert isinstance(obj, dict)
        display_prompt = from_str(obj.get("displayPrompt"))
        prompt = from_str(obj.get("prompt"))
        mode = from_union([SessionMode, from_none], obj.get("mode"))
        runtime_settings_changed = from_union([from_bool, from_none], obj.get("runtimeSettingsChanged"))
        return SlashCommandAgentPromptResult(display_prompt, prompt, mode, runtime_settings_changed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayPrompt"] = from_str(self.display_prompt)
        result["kind"] = self.kind
        result["prompt"] = from_str(self.prompt)
        if self.mode is not None:
            result["mode"] = from_union([lambda x: to_enum(SessionMode, x), from_none], self.mode)
        if self.runtime_settings_changed is not None:
            result["runtimeSettingsChanged"] = from_union([from_bool, from_none], self.runtime_settings_changed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandCompletedResult:
    """Schema for the `SlashCommandCompletedResult` type."""

    kind: ClassVar[str] = "completed"
    """Completed result discriminator"""

    message: str | None = None
    """Optional user-facing message describing the completed command"""

    runtime_settings_changed: bool | None = None
    """True when the invocation mutated user runtime settings; consumers caching settings should
    refresh
    """

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandCompletedResult:
        assert isinstance(obj, dict)
        message = from_union([from_str, from_none], obj.get("message"))
        runtime_settings_changed = from_union([from_bool, from_none], obj.get("runtimeSettingsChanged"))
        return SlashCommandCompletedResult(message, runtime_settings_changed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        if self.runtime_settings_changed is not None:
            result["runtimeSettingsChanged"] = from_union([from_bool, from_none], self.runtime_settings_changed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandSelectSubcommandResult:
    """Schema for the `SlashCommandSelectSubcommandResult` type."""

    command: str
    """Parent command name that requires subcommand selection"""

    kind: ClassVar[str] = "select-subcommand"
    """Select subcommand result discriminator"""

    options: list[SlashCommandSelectSubcommandOption]
    """Available subcommand options for the client to present"""

    title: str
    """Human-readable title for the selection UI"""

    runtime_settings_changed: bool | None = None
    """True when the invocation mutated user runtime settings; consumers caching settings should
    refresh
    """

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandSelectSubcommandResult:
        assert isinstance(obj, dict)
        command = from_str(obj.get("command"))
        options = from_list(SlashCommandSelectSubcommandOption.from_dict, obj.get("options"))
        title = from_str(obj.get("title"))
        runtime_settings_changed = from_union([from_bool, from_none], obj.get("runtimeSettingsChanged"))
        return SlashCommandSelectSubcommandResult(command, options, title, runtime_settings_changed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["command"] = from_str(self.command)
        result["kind"] = self.kind
        result["options"] = from_list(lambda x: to_class(SlashCommandSelectSubcommandOption, x), self.options)
        result["title"] = from_str(self.title)
        if self.runtime_settings_changed is not None:
            result["runtimeSettingsChanged"] = from_union([from_bool, from_none], self.runtime_settings_changed)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskAgentProgress:
    """Schema for the `TaskAgentProgress` type."""

    recent_activity: list[TaskProgressLine]
    """Recent tool execution events converted to display lines"""

    type: TaskAgentInfoType
    """Progress kind"""

    latest_intent: str | None = None
    """The most recent intent reported by the agent"""

    @staticmethod
    def from_dict(obj: Any) -> TaskAgentProgress:
        assert isinstance(obj, dict)
        recent_activity = from_list(TaskProgressLine.from_dict, obj.get("recentActivity"))
        type = TaskAgentInfoType(obj.get("type"))
        latest_intent = from_union([from_str, from_none], obj.get("latestIntent"))
        return TaskAgentProgress(recent_activity, type, latest_intent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["recentActivity"] = from_list(lambda x: to_class(TaskProgressLine, x), self.recent_activity)
        result["type"] = to_enum(TaskAgentInfoType, self.type)
        if self.latest_intent is not None:
            result["latestIntent"] = from_union([from_str, from_none], self.latest_intent)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskShellInfo:
    """Schema for the `TaskShellInfo` type."""

    attachment_mode: TaskShellInfoAttachmentMode
    """Whether the shell runs inside a managed PTY session or as an independent background
    process
    """
    command: str
    """Command being executed"""

    description: str
    """Short description of the task"""

    id: str
    """Unique task identifier"""

    started_at: datetime
    """ISO 8601 timestamp when the task was started"""

    status: TaskStatus
    """Current lifecycle status of the task"""

    type: ClassVar[str] = "shell"
    """Task kind"""

    can_promote_to_background: bool | None = None
    """Whether this shell task can be promoted to background mode"""

    completed_at: datetime | None = None
    """ISO 8601 timestamp when the task finished"""

    execution_mode: TaskExecutionMode | None = None
    """Whether task execution is synchronously awaited or managed in the background"""

    log_path: str | None = None
    """Path to the detached shell log, when available"""

    pid: int | None = None
    """Process ID when available"""

    @staticmethod
    def from_dict(obj: Any) -> TaskShellInfo:
        assert isinstance(obj, dict)
        attachment_mode = TaskShellInfoAttachmentMode(obj.get("attachmentMode"))
        command = from_str(obj.get("command"))
        description = from_str(obj.get("description"))
        id = from_str(obj.get("id"))
        started_at = from_datetime(obj.get("startedAt"))
        status = TaskStatus(obj.get("status"))
        can_promote_to_background = from_union([from_bool, from_none], obj.get("canPromoteToBackground"))
        completed_at = from_union([from_datetime, from_none], obj.get("completedAt"))
        execution_mode = from_union([TaskExecutionMode, from_none], obj.get("executionMode"))
        log_path = from_union([from_str, from_none], obj.get("logPath"))
        pid = from_union([from_int, from_none], obj.get("pid"))
        return TaskShellInfo(attachment_mode, command, description, id, started_at, status, can_promote_to_background, completed_at, execution_mode, log_path, pid)

    def to_dict(self) -> dict:
        result: dict = {}
        result["attachmentMode"] = to_enum(TaskShellInfoAttachmentMode, self.attachment_mode)
        result["command"] = from_str(self.command)
        result["description"] = from_str(self.description)
        result["id"] = from_str(self.id)
        result["startedAt"] = self.started_at.isoformat()
        result["status"] = to_enum(TaskStatus, self.status)
        result["type"] = self.type
        if self.can_promote_to_background is not None:
            result["canPromoteToBackground"] = from_union([from_bool, from_none], self.can_promote_to_background)
        if self.completed_at is not None:
            result["completedAt"] = from_union([lambda x: x.isoformat(), from_none], self.completed_at)
        if self.execution_mode is not None:
            result["executionMode"] = from_union([lambda x: to_enum(TaskExecutionMode, x), from_none], self.execution_mode)
        if self.log_path is not None:
            result["logPath"] = from_union([from_str, from_none], self.log_path)
        if self.pid is not None:
            result["pid"] = from_union([from_int, from_none], self.pid)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskShellProgress:
    """Schema for the `TaskShellProgress` type."""

    recent_output: str
    """Recent stdout/stderr lines from the running shell command"""

    type: TaskShellInfoType
    """Progress kind"""

    pid: int | None = None
    """Process ID when available"""

    @staticmethod
    def from_dict(obj: Any) -> TaskShellProgress:
        assert isinstance(obj, dict)
        recent_output = from_str(obj.get("recentOutput"))
        type = TaskShellInfoType(obj.get("type"))
        pid = from_union([from_int, from_none], obj.get("pid"))
        return TaskShellProgress(recent_output, type, pid)

    def to_dict(self) -> dict:
        result: dict = {}
        result["recentOutput"] = from_str(self.recent_output)
        result["type"] = to_enum(TaskShellInfoType, self.type)
        if self.pid is not None:
            result["pid"] = from_union([from_int, from_none], self.pid)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionLocationAddToolApprovalParams:
    """Location-scoped tool approval to persist."""

    approval: PermissionsLocationsAddToolApprovalDetails
    """Tool approval to persist and apply"""

    location_key: str
    """Location key (git root or cwd) to persist the approval to"""

    @staticmethod
    def from_dict(obj: Any) -> PermissionLocationAddToolApprovalParams:
        assert isinstance(obj, dict)
        approval = _load_PermissionsLocationsAddToolApprovalDetails(obj.get("approval"))
        location_key = from_str(obj.get("locationKey"))
        return PermissionLocationAddToolApprovalParams(approval, location_key)

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = (self.approval).to_dict()
        result["locationKey"] = from_str(self.location_key)
        return result

@dataclass
class ToolList:
    """Built-in tools available for the requested model, with their parameters and instructions."""

    tools: list[Tool]
    """List of available built-in tools with metadata"""

    @staticmethod
    def from_dict(obj: Any) -> ToolList:
        assert isinstance(obj, dict)
        tools = from_list(Tool.from_dict, obj.get("tools"))
        return ToolList(tools)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tools"] = from_list(lambda x: to_class(Tool, x), self.tools)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIHandlePendingAutoModeSwitchRequest:
    """Request ID of a pending `auto_mode_switch.requested` event and the user's response."""

    request_id: str
    """The unique request ID from the auto_mode_switch.requested event"""

    response: UIAutoModeSwitchResponse
    """User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist
    as setting), or no (decline).
    """

    @staticmethod
    def from_dict(obj: Any) -> UIHandlePendingAutoModeSwitchRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        response = UIAutoModeSwitchResponse(obj.get("response"))
        return UIHandlePendingAutoModeSwitchRequest(request_id, response)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["response"] = to_enum(UIAutoModeSwitchResponse, self.response)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationArrayAnyOfFieldItems:
    """Schema applied to each item in the array."""

    any_of: list[UIElicitationArrayAnyOfFieldItemsAnyOf]
    """Selectable options, each with a value and a display label."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationArrayAnyOfFieldItems:
        assert isinstance(obj, dict)
        any_of = from_list(UIElicitationArrayAnyOfFieldItemsAnyOf.from_dict, obj.get("anyOf"))
        return UIElicitationArrayAnyOfFieldItems(any_of)

    def to_dict(self) -> dict:
        result: dict = {}
        result["anyOf"] = from_list(lambda x: to_class(UIElicitationArrayAnyOfFieldItemsAnyOf, x), self.any_of)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationArrayEnumFieldItems:
    """Schema applied to each item in the array."""

    enum: list[str]
    """Allowed string values for each selected item."""

    type: UIElicitationArrayEnumFieldItemsType
    """Type discriminator. Always "string"."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationArrayEnumFieldItems:
        assert isinstance(obj, dict)
        enum = from_list(from_str, obj.get("enum"))
        type = UIElicitationArrayEnumFieldItemsType(obj.get("type"))
        return UIElicitationArrayEnumFieldItems(enum, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enum"] = from_list(from_str, self.enum)
        result["type"] = to_enum(UIElicitationArrayEnumFieldItemsType, self.type)
        return result

@dataclass
class UIElicitationArrayFieldItems:
    """Schema applied to each item in the array."""

    enum: list[str] | None = None
    """Allowed string values for each selected item."""

    type: UIElicitationArrayEnumFieldItemsType | None = None
    """Type discriminator. Always "string"."""

    any_of: list[UIElicitationArrayAnyOfFieldItemsAnyOf] | None = None
    """Selectable options, each with a value and a display label."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationArrayFieldItems:
        assert isinstance(obj, dict)
        enum = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enum"))
        type = from_union([UIElicitationArrayEnumFieldItemsType, from_none], obj.get("type"))
        any_of = from_union([lambda x: from_list(UIElicitationArrayAnyOfFieldItemsAnyOf.from_dict, x), from_none], obj.get("anyOf"))
        return UIElicitationArrayFieldItems(enum, type, any_of)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.enum is not None:
            result["enum"] = from_union([lambda x: from_list(from_str, x), from_none], self.enum)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(UIElicitationArrayEnumFieldItemsType, x), from_none], self.type)
        if self.any_of is not None:
            result["anyOf"] = from_union([lambda x: from_list(lambda x: to_class(UIElicitationArrayAnyOfFieldItemsAnyOf, x), x), from_none], self.any_of)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationStringEnumField:
    """Single-select string field whose allowed values are defined inline."""

    enum: list[str]
    """Allowed string values."""

    type: UIElicitationArrayEnumFieldItemsType
    """Type discriminator. Always "string"."""

    default: str | None = None
    """Default value selected when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    enum_names: list[str] | None = None
    """Optional display labels for each enum value, in the same order as `enum`."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationStringEnumField:
        assert isinstance(obj, dict)
        enum = from_list(from_str, obj.get("enum"))
        type = UIElicitationArrayEnumFieldItemsType(obj.get("type"))
        default = from_union([from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        enum_names = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enumNames"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationStringEnumField(enum, type, default, description, enum_names, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enum"] = from_list(from_str, self.enum)
        result["type"] = to_enum(UIElicitationArrayEnumFieldItemsType, self.type)
        if self.default is not None:
            result["default"] = from_union([from_str, from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.enum_names is not None:
            result["enumNames"] = from_union([lambda x: from_list(from_str, x), from_none], self.enum_names)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationSchemaPropertyString:
    """Free-text string field with optional length and format constraints."""

    type: UIElicitationArrayEnumFieldItemsType
    """Type discriminator. Always "string"."""

    default: str | None = None
    """Default value populated in the input when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    format: UIElicitationSchemaPropertyStringFormat | None = None
    """Optional format hint that constrains the accepted input."""

    max_length: int | None = None
    """Maximum number of characters allowed."""

    min_length: int | None = None
    """Minimum number of characters required."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationSchemaPropertyString:
        assert isinstance(obj, dict)
        type = UIElicitationArrayEnumFieldItemsType(obj.get("type"))
        default = from_union([from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        format = from_union([UIElicitationSchemaPropertyStringFormat, from_none], obj.get("format"))
        max_length = from_union([from_int, from_none], obj.get("maxLength"))
        min_length = from_union([from_int, from_none], obj.get("minLength"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationSchemaPropertyString(type, default, description, format, max_length, min_length, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(UIElicitationArrayEnumFieldItemsType, self.type)
        if self.default is not None:
            result["default"] = from_union([from_str, from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.format is not None:
            result["format"] = from_union([lambda x: to_enum(UIElicitationSchemaPropertyStringFormat, x), from_none], self.format)
        if self.max_length is not None:
            result["maxLength"] = from_union([from_int, from_none], self.max_length)
        if self.min_length is not None:
            result["minLength"] = from_union([from_int, from_none], self.min_length)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationStringOneOfField:
    """Single-select string field where each option pairs a value with a display label."""

    one_of: list[UIElicitationStringOneOfFieldOneOf]
    """Selectable options, each with a value and a display label."""

    type: UIElicitationArrayEnumFieldItemsType
    """Type discriminator. Always "string"."""

    default: str | None = None
    """Default value selected when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationStringOneOfField:
        assert isinstance(obj, dict)
        one_of = from_list(UIElicitationStringOneOfFieldOneOf.from_dict, obj.get("oneOf"))
        type = UIElicitationArrayEnumFieldItemsType(obj.get("type"))
        default = from_union([from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationStringOneOfField(one_of, type, default, description, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["oneOf"] = from_list(lambda x: to_class(UIElicitationStringOneOfFieldOneOf, x), self.one_of)
        result["type"] = to_enum(UIElicitationArrayEnumFieldItemsType, self.type)
        if self.default is not None:
            result["default"] = from_union([from_str, from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationResponse:
    """The elicitation response (accept with form values, decline, or cancel)"""

    action: UIElicitationResponseAction
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    content: dict[str, float | bool | list[str] | str] | None = None
    """The form values submitted by the user (present when action is 'accept')"""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationResponse:
        assert isinstance(obj, dict)
        action = UIElicitationResponseAction(obj.get("action"))
        content = from_union([lambda x: from_dict(lambda x: from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str], x), x), from_none], obj.get("content"))
        return UIElicitationResponse(action, content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["action"] = to_enum(UIElicitationResponseAction, self.action)
        if self.content is not None:
            result["content"] = from_union([lambda x: from_dict(lambda x: from_union([to_float, from_bool, lambda x: from_list(from_str, x), from_str], x), x), from_none], self.content)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationSchemaPropertyBoolean:
    """Boolean field rendered as a yes/no toggle."""

    type: UIElicitationSchemaPropertyBooleanType
    """Type discriminator. Always "boolean"."""

    default: bool | None = None
    """Default value selected when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationSchemaPropertyBoolean:
        assert isinstance(obj, dict)
        type = UIElicitationSchemaPropertyBooleanType(obj.get("type"))
        default = from_union([from_bool, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationSchemaPropertyBoolean(type, default, description, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(UIElicitationSchemaPropertyBooleanType, self.type)
        if self.default is not None:
            result["default"] = from_union([from_bool, from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationSchemaPropertyNumber:
    """Numeric field accepting either a number or an integer."""

    type: UIElicitationSchemaPropertyNumberType
    """Numeric type accepted by the field."""

    default: float | None = None
    """Default value populated in the input when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    maximum: float | None = None
    """Maximum allowed value (inclusive)."""

    minimum: float | None = None
    """Minimum allowed value (inclusive)."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationSchemaPropertyNumber:
        assert isinstance(obj, dict)
        type = UIElicitationSchemaPropertyNumberType(obj.get("type"))
        default = from_union([from_float, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        maximum = from_union([from_float, from_none], obj.get("maximum"))
        minimum = from_union([from_float, from_none], obj.get("minimum"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationSchemaPropertyNumber(type, default, description, maximum, minimum, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(UIElicitationSchemaPropertyNumberType, self.type)
        if self.default is not None:
            result["default"] = from_union([to_float, from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.maximum is not None:
            result["maximum"] = from_union([to_float, from_none], self.maximum)
        if self.minimum is not None:
            result["minimum"] = from_union([to_float, from_none], self.minimum)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIExitPlanModeResponse:
    """Schema for the `UIExitPlanModeResponse` type."""

    approved: bool
    """Whether the plan was approved."""

    auto_approve_edits: bool | None = None
    """Whether subsequent edits should be auto-approved without confirmation."""

    feedback: str | None = None
    """Feedback from the user when they declined the plan or requested changes."""

    selected_action: UIExitPlanModeAction | None = None
    """The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true,
    otherwise 'interactive'.
    """

    @staticmethod
    def from_dict(obj: Any) -> UIExitPlanModeResponse:
        assert isinstance(obj, dict)
        approved = from_bool(obj.get("approved"))
        auto_approve_edits = from_union([from_bool, from_none], obj.get("autoApproveEdits"))
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        selected_action = from_union([UIExitPlanModeAction, from_none], obj.get("selectedAction"))
        return UIExitPlanModeResponse(approved, auto_approve_edits, feedback, selected_action)

    def to_dict(self) -> dict:
        result: dict = {}
        result["approved"] = from_bool(self.approved)
        if self.auto_approve_edits is not None:
            result["autoApproveEdits"] = from_union([from_bool, from_none], self.auto_approve_edits)
        if self.feedback is not None:
            result["feedback"] = from_union([from_str, from_none], self.feedback)
        if self.selected_action is not None:
            result["selectedAction"] = from_union([lambda x: to_enum(UIExitPlanModeAction, x), from_none], self.selected_action)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIHandlePendingUserInputRequest:
    """Request ID of a pending `user_input.requested` event and the user's response."""

    request_id: str
    """The unique request ID from the user_input.requested event"""

    response: UIUserInputResponse
    """Schema for the `UIUserInputResponse` type."""

    @staticmethod
    def from_dict(obj: Any) -> UIHandlePendingUserInputRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        response = UIUserInputResponse.from_dict(obj.get("response"))
        return UIHandlePendingUserInputRequest(request_id, response)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["response"] = to_class(UIUserInputResponse, self.response)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageMetricsModelMetric:
    """Schema for the `UsageMetricsModelMetric` type."""

    requests: UsageMetricsModelMetricRequests
    """Request count and cost metrics for this model"""

    usage: UsageMetricsModelMetricUsage
    """Token usage metrics for this model"""

    token_details: dict[str, UsageMetricsModelMetricTokenDetail] | None = None
    """Token count details per type"""

    total_nano_aiu: float | None = None
    """Accumulated nano-AI units cost for this model"""

    @staticmethod
    def from_dict(obj: Any) -> UsageMetricsModelMetric:
        assert isinstance(obj, dict)
        requests = UsageMetricsModelMetricRequests.from_dict(obj.get("requests"))
        usage = UsageMetricsModelMetricUsage.from_dict(obj.get("usage"))
        token_details = from_union([lambda x: from_dict(UsageMetricsModelMetricTokenDetail.from_dict, x), from_none], obj.get("tokenDetails"))
        total_nano_aiu = from_union([from_float, from_none], obj.get("totalNanoAiu"))
        return UsageMetricsModelMetric(requests, usage, token_details, total_nano_aiu)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requests"] = to_class(UsageMetricsModelMetricRequests, self.requests)
        result["usage"] = to_class(UsageMetricsModelMetricUsage, self.usage)
        if self.token_details is not None:
            result["tokenDetails"] = from_union([lambda x: from_dict(lambda x: to_class(UsageMetricsModelMetricTokenDetail, x), x), from_none], self.token_details)
        if self.total_nano_aiu is not None:
            result["totalNanoAiu"] = from_union([to_float, from_none], self.total_nano_aiu)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesSaveLargePasteResult:
    """Descriptor for the saved paste file, or null when the workspace is unavailable."""

    saved: Saved | None = None
    """Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime,
    non-infinite sessions, remote sessions)
    """

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesSaveLargePasteResult:
        assert isinstance(obj, dict)
        saved = from_union([Saved.from_dict, from_none], obj.get("saved"))
        return WorkspacesSaveLargePasteResult(saved)

    def to_dict(self) -> dict:
        result: dict = {}
        result["saved"] = from_union([lambda x: to_class(Saved, x), from_none], self.saved)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SlashCommandInfo:
    """Schema for the `SlashCommandInfo` type."""

    allow_during_agent_execution: bool
    """Whether the command may run while an agent turn is active"""

    description: str
    """Human-readable command description"""

    kind: SlashCommandKind
    """Coarse command category for grouping and behavior: runtime built-in, skill-backed
    command, or SDK/client-owned command
    """
    name: str
    """Canonical command name without a leading slash"""

    aliases: list[str] | None = None
    """Canonical aliases without leading slashes"""

    experimental: bool | None = None
    """Whether the command is experimental"""

    input: SlashCommandInput | None = None
    """Optional unstructured input hint"""

    @staticmethod
    def from_dict(obj: Any) -> SlashCommandInfo:
        assert isinstance(obj, dict)
        allow_during_agent_execution = from_bool(obj.get("allowDuringAgentExecution"))
        description = from_str(obj.get("description"))
        kind = SlashCommandKind(obj.get("kind"))
        name = from_str(obj.get("name"))
        aliases = from_union([lambda x: from_list(from_str, x), from_none], obj.get("aliases"))
        experimental = from_union([from_bool, from_none], obj.get("experimental"))
        input = from_union([SlashCommandInput.from_dict, from_none], obj.get("input"))
        return SlashCommandInfo(allow_during_agent_execution, description, kind, name, aliases, experimental, input)

    def to_dict(self) -> dict:
        result: dict = {}
        result["allowDuringAgentExecution"] = from_bool(self.allow_during_agent_execution)
        result["description"] = from_str(self.description)
        result["kind"] = to_enum(SlashCommandKind, self.kind)
        result["name"] = from_str(self.name)
        if self.aliases is not None:
            result["aliases"] = from_union([lambda x: from_list(from_str, x), from_none], self.aliases)
        if self.experimental is not None:
            result["experimental"] = from_union([from_bool, from_none], self.experimental)
        if self.input is not None:
            result["input"] = from_union([lambda x: to_class(SlashCommandInput, x), from_none], self.input)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RemoteSessionConnectionResult:
    """Remote session connection result."""

    metadata: ConnectedRemoteSessionMetadata
    """Metadata for a connected remote session."""

    session_id: str
    """SDK session ID for the connected remote session."""

    @staticmethod
    def from_dict(obj: Any) -> RemoteSessionConnectionResult:
        assert isinstance(obj, dict)
        metadata = ConnectedRemoteSessionMetadata.from_dict(obj.get("metadata"))
        session_id = from_str(obj.get("sessionId"))
        return RemoteSessionConnectionResult(metadata, session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["metadata"] = to_class(ConnectedRemoteSessionMetadata, self.metadata)
        result["sessionId"] = from_str(self.session_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotUserResponse:
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """
    access_type_sku: str | None = None
    analytics_tracking_id: str | None = None
    assigned_date: Any = None
    can_signup_for_limited: bool | None = None
    chat_enabled: bool | None = None
    cli_remote_control_enabled: bool | None = None
    cloud_session_storage_enabled: bool | None = None
    codex_agent_enabled: bool | None = None
    copilot_plan: str | None = None
    copilotignore_enabled: bool | None = None
    endpoints: CopilotUserResponseEndpoints | None = None
    """Schema for the `CopilotUserResponseEndpoints` type."""

    is_mcp_enabled: Any = None
    limited_user_quotas: dict[str, float] | None = None
    limited_user_reset_date: str | None = None
    login: str | None = None
    monthly_quotas: dict[str, float] | None = None
    organization_list: Any = None
    organization_login_list: list[str] | None = None
    quota_reset_date: str | None = None
    quota_reset_date_utc: str | None = None
    quota_snapshots: dict[str, CopilotUserResponseQuotaSnapshots | None] | None = None
    """Schema for the `CopilotUserResponseQuotaSnapshots` type."""

    restricted_telemetry: bool | None = None
    token_based_billing: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> CopilotUserResponse:
        assert isinstance(obj, dict)
        access_type_sku = from_union([from_str, from_none], obj.get("access_type_sku"))
        analytics_tracking_id = from_union([from_str, from_none], obj.get("analytics_tracking_id"))
        assigned_date = obj.get("assigned_date")
        can_signup_for_limited = from_union([from_bool, from_none], obj.get("can_signup_for_limited"))
        chat_enabled = from_union([from_bool, from_none], obj.get("chat_enabled"))
        cli_remote_control_enabled = from_union([from_bool, from_none], obj.get("cli_remote_control_enabled"))
        cloud_session_storage_enabled = from_union([from_bool, from_none], obj.get("cloud_session_storage_enabled"))
        codex_agent_enabled = from_union([from_bool, from_none], obj.get("codex_agent_enabled"))
        copilot_plan = from_union([from_str, from_none], obj.get("copilot_plan"))
        copilotignore_enabled = from_union([from_bool, from_none], obj.get("copilotignore_enabled"))
        endpoints = from_union([CopilotUserResponseEndpoints.from_dict, from_none], obj.get("endpoints"))
        is_mcp_enabled = obj.get("is_mcp_enabled")
        limited_user_quotas = from_union([lambda x: from_dict(from_float, x), from_none], obj.get("limited_user_quotas"))
        limited_user_reset_date = from_union([from_str, from_none], obj.get("limited_user_reset_date"))
        login = from_union([from_str, from_none], obj.get("login"))
        monthly_quotas = from_union([lambda x: from_dict(from_float, x), from_none], obj.get("monthly_quotas"))
        organization_list = obj.get("organization_list")
        organization_login_list = from_union([lambda x: from_list(from_str, x), from_none], obj.get("organization_login_list"))
        quota_reset_date = from_union([from_str, from_none], obj.get("quota_reset_date"))
        quota_reset_date_utc = from_union([from_str, from_none], obj.get("quota_reset_date_utc"))
        quota_snapshots = from_union([lambda x: from_dict(lambda x: from_union([CopilotUserResponseQuotaSnapshots.from_dict, from_none], x), x), from_none], obj.get("quota_snapshots"))
        restricted_telemetry = from_union([from_bool, from_none], obj.get("restricted_telemetry"))
        token_based_billing = from_union([from_bool, from_none], obj.get("token_based_billing"))
        return CopilotUserResponse(access_type_sku, analytics_tracking_id, assigned_date, can_signup_for_limited, chat_enabled, cli_remote_control_enabled, cloud_session_storage_enabled, codex_agent_enabled, copilot_plan, copilotignore_enabled, endpoints, is_mcp_enabled, limited_user_quotas, limited_user_reset_date, login, monthly_quotas, organization_list, organization_login_list, quota_reset_date, quota_reset_date_utc, quota_snapshots, restricted_telemetry, token_based_billing)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.access_type_sku is not None:
            result["access_type_sku"] = from_union([from_str, from_none], self.access_type_sku)
        if self.analytics_tracking_id is not None:
            result["analytics_tracking_id"] = from_union([from_str, from_none], self.analytics_tracking_id)
        if self.assigned_date is not None:
            result["assigned_date"] = self.assigned_date
        if self.can_signup_for_limited is not None:
            result["can_signup_for_limited"] = from_union([from_bool, from_none], self.can_signup_for_limited)
        if self.chat_enabled is not None:
            result["chat_enabled"] = from_union([from_bool, from_none], self.chat_enabled)
        if self.cli_remote_control_enabled is not None:
            result["cli_remote_control_enabled"] = from_union([from_bool, from_none], self.cli_remote_control_enabled)
        if self.cloud_session_storage_enabled is not None:
            result["cloud_session_storage_enabled"] = from_union([from_bool, from_none], self.cloud_session_storage_enabled)
        if self.codex_agent_enabled is not None:
            result["codex_agent_enabled"] = from_union([from_bool, from_none], self.codex_agent_enabled)
        if self.copilot_plan is not None:
            result["copilot_plan"] = from_union([from_str, from_none], self.copilot_plan)
        if self.copilotignore_enabled is not None:
            result["copilotignore_enabled"] = from_union([from_bool, from_none], self.copilotignore_enabled)
        if self.endpoints is not None:
            result["endpoints"] = from_union([lambda x: to_class(CopilotUserResponseEndpoints, x), from_none], self.endpoints)
        if self.is_mcp_enabled is not None:
            result["is_mcp_enabled"] = self.is_mcp_enabled
        if self.limited_user_quotas is not None:
            result["limited_user_quotas"] = from_union([lambda x: from_dict(to_float, x), from_none], self.limited_user_quotas)
        if self.limited_user_reset_date is not None:
            result["limited_user_reset_date"] = from_union([from_str, from_none], self.limited_user_reset_date)
        if self.login is not None:
            result["login"] = from_union([from_str, from_none], self.login)
        if self.monthly_quotas is not None:
            result["monthly_quotas"] = from_union([lambda x: from_dict(to_float, x), from_none], self.monthly_quotas)
        if self.organization_list is not None:
            result["organization_list"] = self.organization_list
        if self.organization_login_list is not None:
            result["organization_login_list"] = from_union([lambda x: from_list(from_str, x), from_none], self.organization_login_list)
        if self.quota_reset_date is not None:
            result["quota_reset_date"] = from_union([from_str, from_none], self.quota_reset_date)
        if self.quota_reset_date_utc is not None:
            result["quota_reset_date_utc"] = from_union([from_str, from_none], self.quota_reset_date_utc)
        if self.quota_snapshots is not None:
            result["quota_snapshots"] = from_union([lambda x: from_dict(lambda x: from_union([lambda x: to_class(CopilotUserResponseQuotaSnapshots, x), from_none], x), x), from_none], self.quota_snapshots)
        if self.restricted_telemetry is not None:
            result["restricted_telemetry"] = from_union([from_bool, from_none], self.restricted_telemetry)
        if self.token_based_billing is not None:
            result["token_based_billing"] = from_union([from_bool, from_none], self.token_based_billing)
        return result

@dataclass
class MCPDiscoverResult:
    """MCP servers discovered from user, workspace, plugin, and built-in sources."""

    servers: list[DiscoveredMCPServer]
    """MCP servers discovered from all sources"""

    @staticmethod
    def from_dict(obj: Any) -> MCPDiscoverResult:
        assert isinstance(obj, dict)
        servers = from_list(DiscoveredMCPServer.from_dict, obj.get("servers"))
        return MCPDiscoverResult(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_list(lambda x: to_class(DiscoveredMCPServer, x), self.servers)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExtensionList:
    """Extensions discovered for the session, with their current status."""

    extensions: list[Extension]
    """Discovered extensions and their current status"""

    @staticmethod
    def from_dict(obj: Any) -> ExtensionList:
        assert isinstance(obj, dict)
        extensions = from_list(Extension.from_dict, obj.get("extensions"))
        return ExtensionList(extensions)

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensions"] = from_list(lambda x: to_class(Extension, x), self.extensions)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess:
    """Schema for the `PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess`
    type.
    """
    extension_name: str
    """Extension name."""

    kind: ClassVar[str] = "extension-permission-access"
    """Approval covering an extension's request to access a permission-gated capability."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess:
        assert isinstance(obj, dict)
        extension_name = from_str(obj.get("extensionName"))
        return PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess(extension_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess:
    """Schema for the `PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess`
    type.
    """
    extension_name: str
    """Extension name."""

    kind: ClassVar[str] = "extension-permission-access"
    """Approval covering an extension's request to access a permission-gated capability."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess:
        assert isinstance(obj, dict)
        extension_name = from_str(obj.get("extensionName"))
        return PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess(extension_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess:
    """Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess` type."""

    extension_name: str
    """Extension name."""

    kind: ClassVar[str] = "extension-permission-access"
    """Approval covering an extension's request to access a permission-gated capability."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess:
        assert isinstance(obj, dict)
        extension_name = from_str(obj.get("extensionName"))
        return PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess(extension_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlm:
    """Expanded external tool result payload"""

    text_result_for_llm: str
    """Text result returned to the model"""

    binary_results_for_llm: list[ExternalToolTextResultForLlmBinaryResultsForLlm] | None = None
    """Base64-encoded binary results returned to the model"""

    contents: list[ExternalToolTextResultForLlmContent] | None = None
    """Structured content blocks from the tool"""

    error: str | None = None
    """Optional error message for failed executions"""

    result_type: str | None = None
    """Execution outcome classification. Optional for back-compat; normalized to 'success' (or
    'failure' when error is present) when missing or unrecognized.
    """
    session_log: str | None = None
    """Detailed log content for timeline display"""

    tool_telemetry: dict[str, Any] | None = None
    """Optional tool-specific telemetry"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlm:
        assert isinstance(obj, dict)
        text_result_for_llm = from_str(obj.get("textResultForLlm"))
        binary_results_for_llm = from_union([lambda x: from_list(ExternalToolTextResultForLlmBinaryResultsForLlm.from_dict, x), from_none], obj.get("binaryResultsForLlm"))
        contents = from_union([lambda x: from_list(_load_ExternalToolTextResultForLlmContent, x), from_none], obj.get("contents"))
        error = from_union([from_str, from_none], obj.get("error"))
        result_type = from_union([from_str, from_none], obj.get("resultType"))
        session_log = from_union([from_str, from_none], obj.get("sessionLog"))
        tool_telemetry = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("toolTelemetry"))
        return ExternalToolTextResultForLlm(text_result_for_llm, binary_results_for_llm, contents, error, result_type, session_log, tool_telemetry)

    def to_dict(self) -> dict:
        result: dict = {}
        result["textResultForLlm"] = from_str(self.text_result_for_llm)
        if self.binary_results_for_llm is not None:
            result["binaryResultsForLlm"] = from_union([lambda x: from_list(lambda x: to_class(ExternalToolTextResultForLlmBinaryResultsForLlm, x), x), from_none], self.binary_results_for_llm)
        if self.contents is not None:
            result["contents"] = from_union([lambda x: from_list(lambda x: (x).to_dict(), x), from_none], self.contents)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.result_type is not None:
            result["resultType"] = from_union([from_str, from_none], self.result_type)
        if self.session_log is not None:
            result["sessionLog"] = from_union([from_str, from_none], self.session_log)
        if self.tool_telemetry is not None:
            result["toolTelemetry"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.tool_telemetry)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExternalToolTextResultForLlmContentResourceLink:
    """Resource link content block referencing an external resource"""

    name: str
    """Resource name identifier"""

    type: ClassVar[str] = "resource_link"
    """Content block type discriminator"""

    uri: str
    """URI identifying the resource"""

    description: str | None = None
    """Human-readable description of the resource"""

    icons: list[ExternalToolTextResultForLlmContentResourceLinkIcon] | None = None
    """Icons associated with this resource"""

    mime_type: str | None = None
    """MIME type of the resource content"""

    size: int | None = None
    """Size of the resource in bytes"""

    title: str | None = None
    """Human-readable display title for the resource"""

    @staticmethod
    def from_dict(obj: Any) -> ExternalToolTextResultForLlmContentResourceLink:
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        uri = from_str(obj.get("uri"))
        description = from_union([from_str, from_none], obj.get("description"))
        icons = from_union([lambda x: from_list(ExternalToolTextResultForLlmContentResourceLinkIcon.from_dict, x), from_none], obj.get("icons"))
        mime_type = from_union([from_str, from_none], obj.get("mimeType"))
        size = from_union([from_int, from_none], obj.get("size"))
        title = from_union([from_str, from_none], obj.get("title"))
        return ExternalToolTextResultForLlmContentResourceLink(name, uri, description, icons, mime_type, size, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["type"] = self.type
        result["uri"] = from_str(self.uri)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.icons is not None:
            result["icons"] = from_union([lambda x: from_list(lambda x: to_class(ExternalToolTextResultForLlmContentResourceLinkIcon, x), x), from_none], self.icons)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_str, from_none], self.mime_type)
        if self.size is not None:
            result["size"] = from_union([from_int, from_none], self.size)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstalledPluginSource:
    """Schema for the `InstalledPluginSourceGithub` type.

    Schema for the `InstalledPluginSourceUrl` type.

    Schema for the `InstalledPluginSourceLocal` type.
    """
    source: PurpleSource
    """Constant value. Always "github".

    Constant value. Always "url".

    Constant value. Always "local".
    """
    path: str | None = None
    ref: str | None = None
    repo: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> InstalledPluginSource:
        assert isinstance(obj, dict)
        source = PurpleSource(obj.get("source"))
        path = from_union([from_str, from_none], obj.get("path"))
        ref = from_union([from_str, from_none], obj.get("ref"))
        repo = from_union([from_str, from_none], obj.get("repo"))
        url = from_union([from_str, from_none], obj.get("url"))
        return InstalledPluginSource(source, path, ref, repo, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["source"] = to_enum(PurpleSource, self.source)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.ref is not None:
            result["ref"] = from_union([from_str, from_none], self.ref)
        if self.repo is not None:
            result["repo"] = from_union([from_str, from_none], self.repo)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionInstalledPluginSource:
    """Schema for the `SessionInstalledPluginSourceGithub` type.

    Schema for the `SessionInstalledPluginSourceUrl` type.

    Schema for the `SessionInstalledPluginSourceLocal` type.
    """
    source: PurpleSource
    """Constant value. Always "github".

    Constant value. Always "url".

    Constant value. Always "local".
    """
    path: str | None = None
    ref: str | None = None
    repo: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> SessionInstalledPluginSource:
        assert isinstance(obj, dict)
        source = PurpleSource(obj.get("source"))
        path = from_union([from_str, from_none], obj.get("path"))
        ref = from_union([from_str, from_none], obj.get("ref"))
        repo = from_union([from_str, from_none], obj.get("repo"))
        url = from_union([from_str, from_none], obj.get("url"))
        return SessionInstalledPluginSource(source, path, ref, repo, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["source"] = to_enum(PurpleSource, self.source)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.ref is not None:
            result["ref"] = from_union([from_str, from_none], self.ref)
        if self.repo is not None:
            result["repo"] = from_union([from_str, from_none], self.repo)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstructionsGetSourcesResult:
    """Instruction sources loaded for the session, in merge order."""

    sources: list[InstructionsSources]
    """Instruction sources for the session"""

    @staticmethod
    def from_dict(obj: Any) -> InstructionsGetSourcesResult:
        assert isinstance(obj, dict)
        sources = from_list(InstructionsSources.from_dict, obj.get("sources"))
        return InstructionsGetSourcesResult(sources)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sources"] = from_list(lambda x: to_class(InstructionsSources, x), self.sources)
        return result

@dataclass
class MCPConfigAddRequest:
    """MCP server name and configuration to add to user configuration."""

    config: MCPServerConfig
    """MCP server configuration (stdio process or remote HTTP/SSE)"""

    name: str
    """Unique name for the MCP server"""

    @staticmethod
    def from_dict(obj: Any) -> MCPConfigAddRequest:
        assert isinstance(obj, dict)
        config = MCPServerConfig.from_dict(obj.get("config"))
        name = from_str(obj.get("name"))
        return MCPConfigAddRequest(config, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["config"] = to_class(MCPServerConfig, self.config)
        result["name"] = from_str(self.name)
        return result

@dataclass
class MCPConfigList:
    """User-configured MCP servers, keyed by server name."""

    servers: dict[str, MCPServerConfig]
    """All MCP servers from user config, keyed by name"""

    @staticmethod
    def from_dict(obj: Any) -> MCPConfigList:
        assert isinstance(obj, dict)
        servers = from_dict(MCPServerConfig.from_dict, obj.get("servers"))
        return MCPConfigList(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_dict(lambda x: to_class(MCPServerConfig, x), self.servers)
        return result

@dataclass
class MCPConfigUpdateRequest:
    """MCP server name and replacement configuration to write to user configuration."""

    config: MCPServerConfig
    """MCP server configuration (stdio process or remote HTTP/SSE)"""

    name: str
    """Name of the MCP server to update"""

    @staticmethod
    def from_dict(obj: Any) -> MCPConfigUpdateRequest:
        assert isinstance(obj, dict)
        config = MCPServerConfig.from_dict(obj.get("config"))
        name = from_str(obj.get("name"))
        return MCPConfigUpdateRequest(config, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["config"] = to_class(MCPServerConfig, self.config)
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataRecordContextChangeRequest:
    """Updated working-directory/git context to record on the session."""

    context: SessionWorkingDirectoryContext
    """Updated working directory and git context. Emitted as the new payload of
    `session.context_changed`.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataRecordContextChangeRequest:
        assert isinstance(obj, dict)
        context = SessionWorkingDirectoryContext.from_dict(obj.get("context"))
        return MetadataRecordContextChangeRequest(context)

    def to_dict(self) -> dict:
        result: dict = {}
        result["context"] = to_class(SessionWorkingDirectoryContext, self.context)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionMetadata:
    """Schema for the `SessionMetadata` type."""

    is_remote: bool
    """True for remote (GitHub) sessions; false for local"""

    modified_time: str
    """Last-modified time of the session's persisted state, as ISO 8601"""

    session_id: str
    """Stable session identifier"""

    start_time: str
    """Session creation time as an ISO 8601 timestamp"""

    context: SessionContext | None = None
    """Schema for the `SessionContext` type."""

    mc_task_id: str | None = None
    """GitHub task ID, when this local session is bound to one. Only present for local sessions
    exported to remote control.
    """
    name: str | None = None
    """Optional human-friendly name set via /rename"""

    summary: str | None = None
    """Short summary of the session, when one has been derived"""

    @staticmethod
    def from_dict(obj: Any) -> SessionMetadata:
        assert isinstance(obj, dict)
        is_remote = from_bool(obj.get("isRemote"))
        modified_time = from_str(obj.get("modifiedTime"))
        session_id = from_str(obj.get("sessionId"))
        start_time = from_str(obj.get("startTime"))
        context = from_union([SessionContext.from_dict, from_none], obj.get("context"))
        mc_task_id = from_union([from_str, from_none], obj.get("mcTaskId"))
        name = from_union([from_str, from_none], obj.get("name"))
        summary = from_union([from_str, from_none], obj.get("summary"))
        return SessionMetadata(is_remote, modified_time, session_id, start_time, context, mc_task_id, name, summary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["isRemote"] = from_bool(self.is_remote)
        result["modifiedTime"] = from_str(self.modified_time)
        result["sessionId"] = from_str(self.session_id)
        result["startTime"] = from_str(self.start_time)
        if self.context is not None:
            result["context"] = from_union([lambda x: to_class(SessionContext, x), from_none], self.context)
        if self.mc_task_id is not None:
            result["mcTaskId"] = from_union([from_str, from_none], self.mc_task_id)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.summary is not None:
            result["summary"] = from_union([from_str, from_none], self.summary)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsGetLastForContextRequest:
    """Optional working-directory context used to score session relevance."""

    context: SessionContext | None = None
    """Optional working-directory context used to score session relevance. When omitted the
    most-recently-modified session wins.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsGetLastForContextRequest:
        assert isinstance(obj, dict)
        context = from_union([SessionContext.from_dict, from_none], obj.get("context"))
        return SessionsGetLastForContextRequest(context)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.context is not None:
            result["context"] = from_union([lambda x: to_class(SessionContext, x), from_none], self.context)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionPathsConfig:
    """If specified, replaces the session's path-permission policy. The runtime constructs the
    appropriate PathManager based on these inputs (rooted at the session's working
    directory). Omit to leave the current path policy unchanged.
    """
    additional_directories: list[str] | None = None
    """Additional directories to allow tool access to (in addition to the session's working
    directory). When `unrestricted` is true, these are still pre-populated on the
    UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention
    completion).
    """
    include_temp_directory: bool | None = None
    """Whether to include the system temp directory in the allowed list (defaults to true).
    Ignored when `unrestricted` is true.
    """
    unrestricted: bool | None = None
    """If true, the runtime allows access to all paths without prompting. Equivalent to
    constructing an UnrestrictedPathManager.
    """
    workspace_path: str | None = None
    """Workspace root path (special-cased to be allowed even before the directory exists).
    Ignored when `unrestricted` is true.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionPathsConfig:
        assert isinstance(obj, dict)
        additional_directories = from_union([lambda x: from_list(from_str, x), from_none], obj.get("additionalDirectories"))
        include_temp_directory = from_union([from_bool, from_none], obj.get("includeTempDirectory"))
        unrestricted = from_union([from_bool, from_none], obj.get("unrestricted"))
        workspace_path = from_union([from_str, from_none], obj.get("workspacePath"))
        return PermissionPathsConfig(additional_directories, include_temp_directory, unrestricted, workspace_path)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.additional_directories is not None:
            result["additionalDirectories"] = from_union([lambda x: from_list(from_str, x), from_none], self.additional_directories)
        if self.include_temp_directory is not None:
            result["includeTempDirectory"] = from_union([from_bool, from_none], self.include_temp_directory)
        if self.unrestricted is not None:
            result["unrestricted"] = from_union([from_bool, from_none], self.unrestricted)
        if self.workspace_path is not None:
            result["workspacePath"] = from_union([from_str, from_none], self.workspace_path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspaceSummary:
    """Public-facing projection of workspace metadata for SDK / TUI consumers"""

    id: str
    """Workspace identifier (1:1 with sessionId)"""

    branch: str | None = None
    """Branch checked out at session start, if any"""

    created_at: datetime | None = None
    """ISO 8601 timestamp when the workspace was created"""

    cwd: str | None = None
    """Current working directory at session start"""

    git_root: str | None = None
    """Resolved git root for cwd, if any"""

    host_type: HostType | None = None
    """Repository host type, if known"""

    name: str | None = None
    """Display name for the session, if set"""

    repository: str | None = None
    """Repository identifier in 'owner/repo' or 'org/project/repo' format, if any"""

    updated_at: datetime | None = None
    """ISO 8601 timestamp when the workspace was last updated"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspaceSummary:
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        branch = from_union([from_str, from_none], obj.get("branch"))
        created_at = from_union([from_datetime, from_none], obj.get("created_at"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        git_root = from_union([from_str, from_none], obj.get("git_root"))
        host_type = from_union([HostType, from_none], obj.get("host_type"))
        name = from_union([from_str, from_none], obj.get("name"))
        repository = from_union([from_str, from_none], obj.get("repository"))
        updated_at = from_union([from_datetime, from_none], obj.get("updated_at"))
        return WorkspaceSummary(id, branch, created_at, cwd, git_root, host_type, name, repository, updated_at)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        if self.branch is not None:
            result["branch"] = from_union([from_str, from_none], self.branch)
        if self.created_at is not None:
            result["created_at"] = from_union([lambda x: x.isoformat(), from_none], self.created_at)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.git_root is not None:
            result["git_root"] = from_union([from_str, from_none], self.git_root)
        if self.host_type is not None:
            result["host_type"] = from_union([lambda x: to_enum(HostType, x), from_none], self.host_type)
        if self.name is not None:
            result["name"] = from_union([from_str, from_none], self.name)
        if self.repository is not None:
            result["repository"] = from_union([from_str, from_none], self.repository)
        if self.updated_at is not None:
            result["updated_at"] = from_union([lambda x: x.isoformat(), from_none], self.updated_at)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesGetWorkspaceResult:
    """Current workspace metadata for the session, including its absolute filesystem path when
    available.
    """
    path: str | None = None
    """Absolute filesystem path to the workspace directory. Omitted when the session has no
    workspace (e.g. remote sessions).
    """
    workspace: Workspace | None = None
    """Current workspace metadata, or null if not available"""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesGetWorkspaceResult:
        assert isinstance(obj, dict)
        path = from_union([from_str, from_none], obj.get("path"))
        workspace = from_union([Workspace.from_dict, from_none], obj.get("workspace"))
        return WorkspacesGetWorkspaceResult(path, workspace)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        result["workspace"] = from_union([lambda x: to_class(Workspace, x), from_none], self.workspace)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class WorkspacesListCheckpointsResult:
    """Workspace checkpoints in chronological order; empty when the workspace is not enabled."""

    checkpoints: list[WorkspacesCheckpoints]
    """Workspace checkpoints in chronological order. Empty when workspace is not enabled."""

    @staticmethod
    def from_dict(obj: Any) -> WorkspacesListCheckpointsResult:
        assert isinstance(obj, dict)
        checkpoints = from_list(WorkspacesCheckpoints.from_dict, obj.get("checkpoints"))
        return WorkspacesListCheckpointsResult(checkpoints)

    def to_dict(self) -> dict:
        result: dict = {}
        result["checkpoints"] = from_list(lambda x: to_class(WorkspacesCheckpoints, x), self.checkpoints)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelCapabilitiesOverride:
    """Override individual model capabilities resolved by the runtime"""

    limits: ModelCapabilitiesOverrideLimits | None = None
    """Token limits for prompts, outputs, and context window"""

    supports: ModelCapabilitiesOverrideSupports | None = None
    """Feature flags indicating what the model supports"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilitiesOverride:
        assert isinstance(obj, dict)
        limits = from_union([ModelCapabilitiesOverrideLimits.from_dict, from_none], obj.get("limits"))
        supports = from_union([ModelCapabilitiesOverrideSupports.from_dict, from_none], obj.get("supports"))
        return ModelCapabilitiesOverride(limits, supports)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.limits is not None:
            result["limits"] = from_union([lambda x: to_class(ModelCapabilitiesOverrideLimits, x), from_none], self.limits)
        if self.supports is not None:
            result["supports"] = from_union([lambda x: to_class(ModelCapabilitiesOverrideSupports, x), from_none], self.supports)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsConfigureAdditionalContentExclusionPolicy:
    """Schema for the `PermissionsConfigureAdditionalContentExclusionPolicy` type."""

    last_updated_at: float | str
    rules: list[PermissionsConfigureAdditionalContentExclusionPolicyRule]
    scope: PermissionsConfigureAdditionalContentExclusionPolicyScope
    """Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope`
    enumeration.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionsConfigureAdditionalContentExclusionPolicy:
        assert isinstance(obj, dict)
        last_updated_at = from_union([from_float, from_str], obj.get("last_updated_at"))
        rules = from_list(PermissionsConfigureAdditionalContentExclusionPolicyRule.from_dict, obj.get("rules"))
        scope = PermissionsConfigureAdditionalContentExclusionPolicyScope(obj.get("scope"))
        return PermissionsConfigureAdditionalContentExclusionPolicy(last_updated_at, rules, scope)

    def to_dict(self) -> dict:
        result: dict = {}
        result["last_updated_at"] = from_union([to_float, from_str], self.last_updated_at)
        result["rules"] = from_list(lambda x: to_class(PermissionsConfigureAdditionalContentExclusionPolicyRule, x), self.rules)
        result["scope"] = to_enum(PermissionsConfigureAdditionalContentExclusionPolicyScope, self.scope)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class QueuePendingItemsResult:
    """Snapshot of the session's pending queued items and immediate-steering messages."""

    items: list[QueuePendingItems]
    """Pending queued items in submission order. Includes user messages, queued slash commands,
    and queued model changes; omits internal system items.
    """
    steering_messages: list[str]
    """Display text for messages currently in the immediate steering queue (interjections sent
    during a running turn).
    """

    @staticmethod
    def from_dict(obj: Any) -> QueuePendingItemsResult:
        assert isinstance(obj, dict)
        items = from_list(QueuePendingItems.from_dict, obj.get("items"))
        steering_messages = from_list(from_str, obj.get("steeringMessages"))
        return QueuePendingItemsResult(items, steering_messages)

    def to_dict(self) -> dict:
        result: dict = {}
        result["items"] = from_list(lambda x: to_class(QueuePendingItems, x), self.items)
        result["steeringMessages"] = from_list(from_str, self.steering_messages)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SendAttachmentSelection:
    """Code selection attachment from an editor"""

    display_name: str
    """User-facing display name for the selection"""

    file_path: str
    """Absolute path to the file containing the selection"""

    selection: SendAttachmentSelectionDetails
    """Position range of the selection within the file"""

    text: str
    """The selected text content"""

    type: ClassVar[str] = "selection"
    """Attachment type discriminator"""

    @staticmethod
    def from_dict(obj: Any) -> SendAttachmentSelection:
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        file_path = from_str(obj.get("filePath"))
        selection = SendAttachmentSelectionDetails.from_dict(obj.get("selection"))
        text = from_str(obj.get("text"))
        return SendAttachmentSelection(display_name, file_path, selection, text)

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["filePath"] = from_str(self.file_path)
        result["selection"] = to_class(SendAttachmentSelectionDetails, self.selection)
        result["text"] = from_str(self.text)
        result["type"] = self.type
        return result

@dataclass
class SessionFSReadFileResult:
    """File content as a UTF-8 string, or a filesystem error if the read failed."""

    content: str
    """File content as UTF-8 string"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReadFileResult:
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        error = from_union([SessionFSError.from_dict, from_none], obj.get("error"))
        return SessionFSReadFileResult(content, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.error is not None:
            result["error"] = from_union([lambda x: to_class(SessionFSError, x), from_none], self.error)
        return result

@dataclass
class SessionFSReaddirResult:
    """Names of entries in the requested directory, or a filesystem error if the read failed."""

    entries: list[str]
    """Entry names in the directory"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReaddirResult:
        assert isinstance(obj, dict)
        entries = from_list(from_str, obj.get("entries"))
        error = from_union([SessionFSError.from_dict, from_none], obj.get("error"))
        return SessionFSReaddirResult(entries, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entries"] = from_list(from_str, self.entries)
        if self.error is not None:
            result["error"] = from_union([lambda x: to_class(SessionFSError, x), from_none], self.error)
        return result

@dataclass
class SessionFSSqliteQueryResult:
    """Query results including rows, columns, and rows affected, or a filesystem error if
    execution failed.
    """
    columns: list[str]
    """Column names from the result set"""

    rows: list[dict[str, Any]]
    """For SELECT: array of row objects. For others: empty array."""

    rows_affected: int
    """Number of rows affected (for INSERT/UPDATE/DELETE)"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    last_insert_rowid: int | None = None
    """SQLite last_insert_rowid() value for INSERT."""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSSqliteQueryResult:
        assert isinstance(obj, dict)
        columns = from_list(from_str, obj.get("columns"))
        rows = from_list(lambda x: from_dict(lambda x: x, x), obj.get("rows"))
        rows_affected = from_int(obj.get("rowsAffected"))
        error = from_union([SessionFSError.from_dict, from_none], obj.get("error"))
        last_insert_rowid = from_union([from_int, from_none], obj.get("lastInsertRowid"))
        return SessionFSSqliteQueryResult(columns, rows, rows_affected, error, last_insert_rowid)

    def to_dict(self) -> dict:
        result: dict = {}
        result["columns"] = from_list(from_str, self.columns)
        result["rows"] = from_list(lambda x: from_dict(lambda x: x, x), self.rows)
        result["rowsAffected"] = from_int(self.rows_affected)
        if self.error is not None:
            result["error"] = from_union([lambda x: to_class(SessionFSError, x), from_none], self.error)
        if self.last_insert_rowid is not None:
            result["lastInsertRowid"] = from_union([from_int, from_none], self.last_insert_rowid)
        return result

@dataclass
class SessionFSStatResult:
    """Filesystem metadata for the requested path, or a filesystem error if the stat failed."""

    birthtime: datetime
    """ISO 8601 timestamp of creation"""

    is_directory: bool
    """Whether the path is a directory"""

    is_file: bool
    """Whether the path is a file"""

    mtime: datetime
    """ISO 8601 timestamp of last modification"""

    size: int
    """File size in bytes"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSStatResult:
        assert isinstance(obj, dict)
        birthtime = from_datetime(obj.get("birthtime"))
        is_directory = from_bool(obj.get("isDirectory"))
        is_file = from_bool(obj.get("isFile"))
        mtime = from_datetime(obj.get("mtime"))
        size = from_int(obj.get("size"))
        error = from_union([SessionFSError.from_dict, from_none], obj.get("error"))
        return SessionFSStatResult(birthtime, is_directory, is_file, mtime, size, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["birthtime"] = self.birthtime.isoformat()
        result["isDirectory"] = from_bool(self.is_directory)
        result["isFile"] = from_bool(self.is_file)
        result["mtime"] = self.mtime.isoformat()
        result["size"] = from_int(self.size)
        if self.error is not None:
            result["error"] = from_union([lambda x: to_class(SessionFSError, x), from_none], self.error)
        return result

@dataclass
class SessionFSReaddirWithTypesResult:
    """Entries in the requested directory paired with file/directory type information, or a
    filesystem error if the read failed.
    """
    entries: list[SessionFSReaddirWithTypesEntry]
    """Directory entries with type information"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> SessionFSReaddirWithTypesResult:
        assert isinstance(obj, dict)
        entries = from_list(SessionFSReaddirWithTypesEntry.from_dict, obj.get("entries"))
        error = from_union([SessionFSError.from_dict, from_none], obj.get("error"))
        return SessionFSReaddirWithTypesResult(entries, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entries"] = from_list(lambda x: to_class(SessionFSReaddirWithTypesEntry, x), self.entries)
        if self.error is not None:
            result["error"] = from_union([lambda x: to_class(SessionFSError, x), from_none], self.error)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentGetCurrentResult:
    """The currently selected custom agent, or null when using the default agent."""

    agent: AgentInfo | None = None
    """Currently selected custom agent, or null if using the default agent"""

    @staticmethod
    def from_dict(obj: Any) -> AgentGetCurrentResult:
        assert isinstance(obj, dict)
        agent = from_union([AgentInfo.from_dict, from_none], obj.get("agent"))
        return AgentGetCurrentResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.agent is not None:
            result["agent"] = from_union([lambda x: to_class(AgentInfo, x), from_none], self.agent)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentList:
    """Custom agents available to the session."""

    agents: list[AgentInfo]
    """Available custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> AgentList:
        assert isinstance(obj, dict)
        agents = from_list(AgentInfo.from_dict, obj.get("agents"))
        return AgentList(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(AgentInfo, x), self.agents)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentReloadResult:
    """Custom agents available to the session after reloading definitions from disk."""

    agents: list[AgentInfo]
    """Reloaded custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> AgentReloadResult:
        assert isinstance(obj, dict)
        agents = from_list(AgentInfo.from_dict, obj.get("agents"))
        return AgentReloadResult(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(AgentInfo, x), self.agents)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentSelectResult:
    """The newly selected custom agent."""

    agent: AgentInfo
    """The newly selected custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> AgentSelectResult:
        assert isinstance(obj, dict)
        agent = AgentInfo.from_dict(obj.get("agent"))
        return AgentSelectResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agent"] = to_class(AgentInfo, self.agent)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskProgress:
    """Schema for the `TaskAgentProgress` type.

    Schema for the `TaskShellProgress` type.
    """
    type: TaskInfoType
    """Progress kind"""

    latest_intent: str | None = None
    """The most recent intent reported by the agent"""

    recent_activity: list[TaskProgressLine] | None = None
    """Recent tool execution events converted to display lines"""

    pid: int | None = None
    """Process ID when available"""

    recent_output: str | None = None
    """Recent stdout/stderr lines from the running shell command"""

    @staticmethod
    def from_dict(obj: Any) -> TaskProgress:
        assert isinstance(obj, dict)
        type = TaskInfoType(obj.get("type"))
        latest_intent = from_union([from_str, from_none], obj.get("latestIntent"))
        recent_activity = from_union([lambda x: from_list(TaskProgressLine.from_dict, x), from_none], obj.get("recentActivity"))
        pid = from_union([from_int, from_none], obj.get("pid"))
        recent_output = from_union([from_str, from_none], obj.get("recentOutput"))
        return TaskProgress(type, latest_intent, recent_activity, pid, recent_output)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(TaskInfoType, self.type)
        if self.latest_intent is not None:
            result["latestIntent"] = from_union([from_str, from_none], self.latest_intent)
        if self.recent_activity is not None:
            result["recentActivity"] = from_union([lambda x: from_list(lambda x: to_class(TaskProgressLine, x), x), from_none], self.recent_activity)
        if self.pid is not None:
            result["pid"] = from_union([from_int, from_none], self.pid)
        if self.recent_output is not None:
            result["recentOutput"] = from_union([from_str, from_none], self.recent_output)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationArrayAnyOfField:
    """Multi-select string field where each option pairs a value with a display label."""

    items: UIElicitationArrayAnyOfFieldItems
    """Schema applied to each item in the array."""

    type: UIElicitationArrayAnyOfFieldType
    """Type discriminator. Always "array"."""

    default: list[str] | None = None
    """Default values selected when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    max_items: int | None = None
    """Maximum number of items the user may select."""

    min_items: int | None = None
    """Minimum number of items the user must select."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationArrayAnyOfField:
        assert isinstance(obj, dict)
        items = UIElicitationArrayAnyOfFieldItems.from_dict(obj.get("items"))
        type = UIElicitationArrayAnyOfFieldType(obj.get("type"))
        default = from_union([lambda x: from_list(from_str, x), from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        max_items = from_union([from_int, from_none], obj.get("maxItems"))
        min_items = from_union([from_int, from_none], obj.get("minItems"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationArrayAnyOfField(items, type, default, description, max_items, min_items, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["items"] = to_class(UIElicitationArrayAnyOfFieldItems, self.items)
        result["type"] = to_enum(UIElicitationArrayAnyOfFieldType, self.type)
        if self.default is not None:
            result["default"] = from_union([lambda x: from_list(from_str, x), from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.max_items is not None:
            result["maxItems"] = from_union([from_int, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([from_int, from_none], self.min_items)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationArrayEnumField:
    """Multi-select string field whose allowed values are defined inline."""

    items: UIElicitationArrayEnumFieldItems
    """Schema applied to each item in the array."""

    type: UIElicitationArrayAnyOfFieldType
    """Type discriminator. Always "array"."""

    default: list[str] | None = None
    """Default values selected when the form is first shown."""

    description: str | None = None
    """Help text describing the field."""

    max_items: int | None = None
    """Maximum number of items the user may select."""

    min_items: int | None = None
    """Minimum number of items the user must select."""

    title: str | None = None
    """Human-readable label for the field."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationArrayEnumField:
        assert isinstance(obj, dict)
        items = UIElicitationArrayEnumFieldItems.from_dict(obj.get("items"))
        type = UIElicitationArrayAnyOfFieldType(obj.get("type"))
        default = from_union([lambda x: from_list(from_str, x), from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        max_items = from_union([from_int, from_none], obj.get("maxItems"))
        min_items = from_union([from_int, from_none], obj.get("minItems"))
        title = from_union([from_str, from_none], obj.get("title"))
        return UIElicitationArrayEnumField(items, type, default, description, max_items, min_items, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["items"] = to_class(UIElicitationArrayEnumFieldItems, self.items)
        result["type"] = to_enum(UIElicitationArrayAnyOfFieldType, self.type)
        if self.default is not None:
            result["default"] = from_union([lambda x: from_list(from_str, x), from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.max_items is not None:
            result["maxItems"] = from_union([from_int, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([from_int, from_none], self.min_items)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationSchemaProperty:
    """Definition for a single elicitation form field.

    Single-select string field whose allowed values are defined inline.

    Single-select string field where each option pairs a value with a display label.

    Multi-select string field whose allowed values are defined inline.

    Multi-select string field where each option pairs a value with a display label.

    Boolean field rendered as a yes/no toggle.

    Free-text string field with optional length and format constraints.

    Numeric field accepting either a number or an integer.
    """
    type: UIElicitationSchemaPropertyType
    """Type discriminator. Always "string".

    Type discriminator. Always "array".

    Type discriminator. Always "boolean".

    Numeric type accepted by the field.
    """
    default: float | bool | list[str] | str | None = None
    """Default value selected when the form is first shown.

    Default values selected when the form is first shown.

    Default value populated in the input when the form is first shown.
    """
    description: str | None = None
    """Help text describing the field."""

    enum: list[str] | None = None
    """Allowed string values."""

    enum_names: list[str] | None = None
    """Optional display labels for each enum value, in the same order as `enum`."""

    title: str | None = None
    """Human-readable label for the field."""

    one_of: list[UIElicitationStringOneOfFieldOneOf] | None = None
    """Selectable options, each with a value and a display label."""

    items: UIElicitationArrayFieldItems | None = None
    """Schema applied to each item in the array."""

    max_items: int | None = None
    """Maximum number of items the user may select."""

    min_items: int | None = None
    """Minimum number of items the user must select."""

    format: UIElicitationSchemaPropertyStringFormat | None = None
    """Optional format hint that constrains the accepted input."""

    max_length: int | None = None
    """Maximum number of characters allowed."""

    min_length: int | None = None
    """Minimum number of characters required."""

    maximum: float | None = None
    """Maximum allowed value (inclusive)."""

    minimum: float | None = None
    """Minimum allowed value (inclusive)."""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationSchemaProperty:
        assert isinstance(obj, dict)
        type = UIElicitationSchemaPropertyType(obj.get("type"))
        default = from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        enum = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enum"))
        enum_names = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enumNames"))
        title = from_union([from_str, from_none], obj.get("title"))
        one_of = from_union([lambda x: from_list(UIElicitationStringOneOfFieldOneOf.from_dict, x), from_none], obj.get("oneOf"))
        items = from_union([UIElicitationArrayFieldItems.from_dict, from_none], obj.get("items"))
        max_items = from_union([from_int, from_none], obj.get("maxItems"))
        min_items = from_union([from_int, from_none], obj.get("minItems"))
        format = from_union([UIElicitationSchemaPropertyStringFormat, from_none], obj.get("format"))
        max_length = from_union([from_int, from_none], obj.get("maxLength"))
        min_length = from_union([from_int, from_none], obj.get("minLength"))
        maximum = from_union([from_float, from_none], obj.get("maximum"))
        minimum = from_union([from_float, from_none], obj.get("minimum"))
        return UIElicitationSchemaProperty(type, default, description, enum, enum_names, title, one_of, items, max_items, min_items, format, max_length, min_length, maximum, minimum)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(UIElicitationSchemaPropertyType, self.type)
        if self.default is not None:
            result["default"] = from_union([to_float, from_bool, lambda x: from_list(from_str, x), from_str, from_none], self.default)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        if self.enum is not None:
            result["enum"] = from_union([lambda x: from_list(from_str, x), from_none], self.enum)
        if self.enum_names is not None:
            result["enumNames"] = from_union([lambda x: from_list(from_str, x), from_none], self.enum_names)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        if self.one_of is not None:
            result["oneOf"] = from_union([lambda x: from_list(lambda x: to_class(UIElicitationStringOneOfFieldOneOf, x), x), from_none], self.one_of)
        if self.items is not None:
            result["items"] = from_union([lambda x: to_class(UIElicitationArrayFieldItems, x), from_none], self.items)
        if self.max_items is not None:
            result["maxItems"] = from_union([from_int, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([from_int, from_none], self.min_items)
        if self.format is not None:
            result["format"] = from_union([lambda x: to_enum(UIElicitationSchemaPropertyStringFormat, x), from_none], self.format)
        if self.max_length is not None:
            result["maxLength"] = from_union([from_int, from_none], self.max_length)
        if self.min_length is not None:
            result["minLength"] = from_union([from_int, from_none], self.min_length)
        if self.maximum is not None:
            result["maximum"] = from_union([to_float, from_none], self.maximum)
        if self.minimum is not None:
            result["minimum"] = from_union([to_float, from_none], self.minimum)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIHandlePendingElicitationRequest:
    """Pending elicitation request ID and the user's response (accept/decline/cancel + form
    values).
    """
    request_id: str
    """The unique request ID from the elicitation.requested event"""

    result: UIElicitationResponse
    """The elicitation response (accept with form values, decline, or cancel)"""

    @staticmethod
    def from_dict(obj: Any) -> UIHandlePendingElicitationRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = UIElicitationResponse.from_dict(obj.get("result"))
        return UIHandlePendingElicitationRequest(request_id, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = to_class(UIElicitationResponse, self.result)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIHandlePendingExitPlanModeRequest:
    """Request ID of a pending `exit_plan_mode.requested` event and the user's response."""

    request_id: str
    """The unique request ID from the exit_plan_mode.requested event"""

    response: UIExitPlanModeResponse
    """Schema for the `UIExitPlanModeResponse` type."""

    @staticmethod
    def from_dict(obj: Any) -> UIHandlePendingExitPlanModeRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        response = UIExitPlanModeResponse.from_dict(obj.get("response"))
        return UIHandlePendingExitPlanModeRequest(request_id, response)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["response"] = to_class(UIExitPlanModeResponse, self.response)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UsageGetMetricsResult:
    """Accumulated session usage metrics, including premium request cost, token counts, model
    breakdown, and code-change totals.
    """
    code_changes: UsageMetricsCodeChanges
    """Aggregated code change metrics"""

    last_call_input_tokens: int
    """Input tokens from the most recent main-agent API call"""

    last_call_output_tokens: int
    """Output tokens from the most recent main-agent API call"""

    model_metrics: dict[str, UsageMetricsModelMetric]
    """Per-model token and request metrics, keyed by model identifier"""

    session_start_time: datetime
    """ISO 8601 timestamp when the session started"""

    total_api_duration_ms: int
    """Total time spent in model API calls (milliseconds)"""

    total_premium_request_cost: float
    """Total user-initiated premium request cost across all models (may be fractional due to
    multipliers)
    """
    total_user_requests: int
    """Raw count of user-initiated API requests"""

    current_model: str | None = None
    """Currently active model identifier"""

    token_details: dict[str, UsageMetricsTokenDetail] | None = None
    """Session-wide per-token-type accumulated token counts"""

    total_nano_aiu: float | None = None
    """Session-wide accumulated nano-AI units cost"""

    @staticmethod
    def from_dict(obj: Any) -> UsageGetMetricsResult:
        assert isinstance(obj, dict)
        code_changes = UsageMetricsCodeChanges.from_dict(obj.get("codeChanges"))
        last_call_input_tokens = from_int(obj.get("lastCallInputTokens"))
        last_call_output_tokens = from_int(obj.get("lastCallOutputTokens"))
        model_metrics = from_dict(UsageMetricsModelMetric.from_dict, obj.get("modelMetrics"))
        session_start_time = from_datetime(obj.get("sessionStartTime"))
        total_api_duration_ms = from_int(obj.get("totalApiDurationMs"))
        total_premium_request_cost = from_float(obj.get("totalPremiumRequestCost"))
        total_user_requests = from_int(obj.get("totalUserRequests"))
        current_model = from_union([from_str, from_none], obj.get("currentModel"))
        token_details = from_union([lambda x: from_dict(UsageMetricsTokenDetail.from_dict, x), from_none], obj.get("tokenDetails"))
        total_nano_aiu = from_union([from_float, from_none], obj.get("totalNanoAiu"))
        return UsageGetMetricsResult(code_changes, last_call_input_tokens, last_call_output_tokens, model_metrics, session_start_time, total_api_duration_ms, total_premium_request_cost, total_user_requests, current_model, token_details, total_nano_aiu)

    def to_dict(self) -> dict:
        result: dict = {}
        result["codeChanges"] = to_class(UsageMetricsCodeChanges, self.code_changes)
        result["lastCallInputTokens"] = from_int(self.last_call_input_tokens)
        result["lastCallOutputTokens"] = from_int(self.last_call_output_tokens)
        result["modelMetrics"] = from_dict(lambda x: to_class(UsageMetricsModelMetric, x), self.model_metrics)
        result["sessionStartTime"] = self.session_start_time.isoformat()
        result["totalApiDurationMs"] = from_int(self.total_api_duration_ms)
        result["totalPremiumRequestCost"] = to_float(self.total_premium_request_cost)
        result["totalUserRequests"] = from_int(self.total_user_requests)
        if self.current_model is not None:
            result["currentModel"] = from_union([from_str, from_none], self.current_model)
        if self.token_details is not None:
            result["tokenDetails"] = from_union([lambda x: from_dict(lambda x: to_class(UsageMetricsTokenDetail, x), x), from_none], self.token_details)
        if self.total_nano_aiu is not None:
            result["totalNanoAiu"] = from_union([to_float, from_none], self.total_nano_aiu)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CommandList:
    """Slash commands available in the session, after applying any include/exclude filters."""

    commands: list[SlashCommandInfo]
    """Commands available in this session"""

    @staticmethod
    def from_dict(obj: Any) -> CommandList:
        assert isinstance(obj, dict)
        commands = from_list(SlashCommandInfo.from_dict, obj.get("commands"))
        return CommandList(commands)

    def to_dict(self) -> dict:
        result: dict = {}
        result["commands"] = from_list(lambda x: to_class(SlashCommandInfo, x), self.commands)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class APIKeyAuthInfo:
    """Schema for the `ApiKeyAuthInfo` type."""

    api_key: str
    """The API key. Treat as a secret."""

    host: str
    """Authentication host."""

    type: ClassVar[str] = "api-key"
    """API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style)."""

    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """

    @staticmethod
    def from_dict(obj: Any) -> APIKeyAuthInfo:
        assert isinstance(obj, dict)
        api_key = from_str(obj.get("apiKey"))
        host = from_str(obj.get("host"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        return APIKeyAuthInfo(api_key, host, copilot_user)

    def to_dict(self) -> dict:
        result: dict = {}
        result["apiKey"] = from_str(self.api_key)
        result["host"] = from_str(self.host)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CopilotAPITokenAuthInfo:
    """Schema for the `CopilotApiTokenAuthInfo` type."""

    host: Host
    """Authentication host (always the public GitHub host)."""

    type: ClassVar[str] = "copilot-api-token"
    """Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL`
    environment-variable pair. The token itself is read from the environment by the runtime,
    not carried in this struct.
    """
    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """

    @staticmethod
    def from_dict(obj: Any) -> CopilotAPITokenAuthInfo:
        assert isinstance(obj, dict)
        host = Host(obj.get("host"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        return CopilotAPITokenAuthInfo(host, copilot_user)

    def to_dict(self) -> dict:
        result: dict = {}
        result["host"] = to_enum(Host, self.host)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class EnvAuthInfo:
    """Schema for the `EnvAuthInfo` type."""

    env_var: str
    """Name of the environment variable the token was sourced from."""

    host: str
    """Authentication host (e.g. https://github.com or a GHES host)."""

    token: str
    """The token value itself. Treat as a secret."""

    type: ClassVar[str] = "env"
    """Personal access token (PAT) or server-to-server token sourced from an environment
    variable.
    """
    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """
    login: str | None = None
    """User login associated with the token. Undefined for server-to-server tokens (those
    starting with `ghs_`).
    """

    @staticmethod
    def from_dict(obj: Any) -> EnvAuthInfo:
        assert isinstance(obj, dict)
        env_var = from_str(obj.get("envVar"))
        host = from_str(obj.get("host"))
        token = from_str(obj.get("token"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        login = from_union([from_str, from_none], obj.get("login"))
        return EnvAuthInfo(env_var, host, token, copilot_user, login)

    def to_dict(self) -> dict:
        result: dict = {}
        result["envVar"] = from_str(self.env_var)
        result["host"] = from_str(self.host)
        result["token"] = from_str(self.token)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        if self.login is not None:
            result["login"] = from_union([from_str, from_none], self.login)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class GhCLIAuthInfo:
    """Schema for the `GhCliAuthInfo` type."""

    host: str
    """Authentication host."""

    login: str
    """User login as reported by `gh auth status`."""

    token: str
    """The token returned by `gh auth token`. Treat as a secret."""

    type: ClassVar[str] = "gh-cli"
    """Authentication via the `gh` CLI's saved credentials."""

    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """

    @staticmethod
    def from_dict(obj: Any) -> GhCLIAuthInfo:
        assert isinstance(obj, dict)
        host = from_str(obj.get("host"))
        login = from_str(obj.get("login"))
        token = from_str(obj.get("token"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        return GhCLIAuthInfo(host, login, token, copilot_user)

    def to_dict(self) -> dict:
        result: dict = {}
        result["host"] = from_str(self.host)
        result["login"] = from_str(self.login)
        result["token"] = from_str(self.token)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HMACAuthInfo:
    """Schema for the `HMACAuthInfo` type."""

    hmac: str
    """HMAC secret used to sign requests."""

    host: Host
    """Authentication host. HMAC auth always targets the public GitHub host."""

    type: ClassVar[str] = "hmac"
    """HMAC-based authentication used by GitHub-internal services."""

    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """

    @staticmethod
    def from_dict(obj: Any) -> HMACAuthInfo:
        assert isinstance(obj, dict)
        hmac = from_str(obj.get("hmac"))
        host = Host(obj.get("host"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        return HMACAuthInfo(hmac, host, copilot_user)

    def to_dict(self) -> dict:
        result: dict = {}
        result["hmac"] = from_str(self.hmac)
        result["host"] = to_enum(Host, self.host)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TokenAuthInfo:
    """Schema for the `TokenAuthInfo` type."""

    host: str
    """Authentication host."""

    token: str
    """The token value itself. Treat as a secret."""

    type: ClassVar[str] = "token"
    """SDK-side token authentication; the host configured the token directly via the SDK."""

    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """

    @staticmethod
    def from_dict(obj: Any) -> TokenAuthInfo:
        assert isinstance(obj, dict)
        host = from_str(obj.get("host"))
        token = from_str(obj.get("token"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        return TokenAuthInfo(host, token, copilot_user)

    def to_dict(self) -> dict:
        result: dict = {}
        result["host"] = from_str(self.host)
        result["token"] = from_str(self.token)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UserAuthInfo:
    """Schema for the `UserAuthInfo` type."""

    host: str
    """Authentication host."""

    login: str
    """OAuth user login."""

    type: ClassVar[str] = "user"
    """OAuth user authentication. The token itself is held in the runtime's secret token store
    (keyed by host+login) and is NOT carried in this struct.
    """
    copilot_user: CopilotUserResponse | None = None
    """Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the
    GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this
    verbatim and does not re-fetch when set.
    """

    @staticmethod
    def from_dict(obj: Any) -> UserAuthInfo:
        assert isinstance(obj, dict)
        host = from_str(obj.get("host"))
        login = from_str(obj.get("login"))
        copilot_user = from_union([CopilotUserResponse.from_dict, from_none], obj.get("copilotUser"))
        return UserAuthInfo(host, login, copilot_user)

    def to_dict(self) -> dict:
        result: dict = {}
        result["host"] = from_str(self.host)
        result["login"] = from_str(self.login)
        result["type"] = self.type
        if self.copilot_user is not None:
            result["copilotUser"] = from_union([lambda x: to_class(CopilotUserResponse, x), from_none], self.copilot_user)
        return result

@dataclass
class PermissionDecisionApproveForIonApproval:
    """Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)

    Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalRead` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalWrite` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalMcp` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalMcpSampling` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalMemory` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalCustomTool` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalExtensionManagement` type.

    Schema for the `PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess`
    type.

    Approval to persist for this location

    Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalRead` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalWrite` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalMcp` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalMemory` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalCustomTool` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type.

    Schema for the `PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess`
    type.

    The approval to add as a session-scoped rule

    The approval to persist for this location
    """
    command_identifiers: list[str] | None = None
    """Command identifiers covered by this approval."""

    kind: ApprovalKind | None = None
    """Approval scoped to specific command identifiers.

    Approval covering read-only filesystem operations.

    Approval covering filesystem write operations.

    Approval covering an MCP tool.

    Approval covering MCP sampling requests for a server.

    Approval covering writes to long-term memory.

    Approval covering a custom tool.

    Approval covering extension lifecycle operations such as enable, disable, or reload.

    Approval covering an extension's request to access a permission-gated capability.
    """
    server_name: str | None = None
    """MCP server name."""

    tool_name: str | None = None
    """MCP tool name, or null to cover every tool on the server.

    Custom tool name.
    """
    operation: str | None = None
    """Optional operation identifier; when omitted, the approval covers all extension management
    operations.
    """
    extension_name: str | None = None
    """Extension name."""

    external_ref_marker_external_ref_user_tool_session_approval: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> PermissionDecisionApproveForIonApproval:
        assert isinstance(obj, dict)
        command_identifiers = from_union([lambda x: from_list(from_str, x), from_none], obj.get("commandIdentifiers"))
        kind = from_union([ApprovalKind, from_none], obj.get("kind"))
        server_name = from_union([from_str, from_none], obj.get("serverName"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        operation = from_union([from_str, from_none], obj.get("operation"))
        extension_name = from_union([from_str, from_none], obj.get("extensionName"))
        external_ref_marker_external_ref_user_tool_session_approval = from_union([from_str, from_none], obj.get("__externalRefMarker___ExternalRef_UserToolSessionApproval"))
        return PermissionDecisionApproveForIonApproval(command_identifiers, kind, server_name, tool_name, operation, extension_name, external_ref_marker_external_ref_user_tool_session_approval)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.command_identifiers is not None:
            result["commandIdentifiers"] = from_union([lambda x: from_list(from_str, x), from_none], self.command_identifiers)
        if self.kind is not None:
            result["kind"] = from_union([lambda x: to_enum(ApprovalKind, x), from_none], self.kind)
        if self.server_name is not None:
            result["serverName"] = from_union([from_str, from_none], self.server_name)
        if self.tool_name is not None:
            result["toolName"] = from_union([from_none, from_str], self.tool_name)
        if self.operation is not None:
            result["operation"] = from_union([from_str, from_none], self.operation)
        if self.extension_name is not None:
            result["extensionName"] = from_union([from_str, from_none], self.extension_name)
        if self.external_ref_marker_external_ref_user_tool_session_approval is not None:
            result["__externalRefMarker___ExternalRef_UserToolSessionApproval"] = from_union([from_str, from_none], self.external_ref_marker_external_ref_user_tool_session_approval)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class HandlePendingToolCallRequest:
    """Pending external tool call request ID, with the tool result or an error describing why it
    failed.
    """
    request_id: str
    """Request ID of the pending tool call"""

    error: str | None = None
    """Error message if the tool call failed"""

    result: ExternalToolTextResultForLlm | str | None = None
    """Tool call result (string or expanded result object)"""

    @staticmethod
    def from_dict(obj: Any) -> HandlePendingToolCallRequest:
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        error = from_union([from_str, from_none], obj.get("error"))
        result = from_union([ExternalToolTextResultForLlm.from_dict, from_str, from_none], obj.get("result"))
        return HandlePendingToolCallRequest(request_id, error, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.result is not None:
            result["result"] = from_union([lambda x: to_class(ExternalToolTextResultForLlm, x), from_str, from_none], self.result)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class InstalledPlugin:
    """Schema for the `InstalledPlugin` type."""

    enabled: bool
    """Whether the plugin is currently enabled"""

    installed_at: str
    """Installation timestamp"""

    marketplace: str
    """Marketplace the plugin came from (empty string for direct repo installs)"""

    name: str
    """Plugin name"""

    cache_path: str | None = None
    """Path where the plugin is cached locally"""

    source: InstalledPluginSource | str | None = None
    """Source for direct repo installs (when marketplace is empty)"""

    version: str | None = None
    """Version installed (if available)"""

    @staticmethod
    def from_dict(obj: Any) -> InstalledPlugin:
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        installed_at = from_str(obj.get("installed_at"))
        marketplace = from_str(obj.get("marketplace"))
        name = from_str(obj.get("name"))
        cache_path = from_union([from_str, from_none], obj.get("cache_path"))
        source = from_union([InstalledPluginSource.from_dict, from_str, from_none], obj.get("source"))
        version = from_union([from_str, from_none], obj.get("version"))
        return InstalledPlugin(enabled, installed_at, marketplace, name, cache_path, source, version)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        result["installed_at"] = from_str(self.installed_at)
        result["marketplace"] = from_str(self.marketplace)
        result["name"] = from_str(self.name)
        if self.cache_path is not None:
            result["cache_path"] = from_union([from_str, from_none], self.cache_path)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_class(InstalledPluginSource, x), from_str, from_none], self.source)
        if self.version is not None:
            result["version"] = from_union([from_str, from_none], self.version)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionInstalledPlugin:
    """Schema for the `SessionInstalledPlugin` type."""

    enabled: bool
    """Whether the plugin is currently enabled"""

    installed_at: str
    """Installation timestamp (ISO-8601)"""

    marketplace: str
    """Marketplace the plugin came from (empty string for direct repo installs)"""

    name: str
    """Plugin name"""

    cache_path: str | None = None
    """Path where the plugin is cached locally"""

    source: SessionInstalledPluginSource | str | None = None
    """Source descriptor for direct repo installs (when marketplace is empty)"""

    version: str | None = None
    """Installed version, if known"""

    @staticmethod
    def from_dict(obj: Any) -> SessionInstalledPlugin:
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        installed_at = from_str(obj.get("installed_at"))
        marketplace = from_str(obj.get("marketplace"))
        name = from_str(obj.get("name"))
        cache_path = from_union([from_str, from_none], obj.get("cache_path"))
        source = from_union([SessionInstalledPluginSource.from_dict, from_str, from_none], obj.get("source"))
        version = from_union([from_str, from_none], obj.get("version"))
        return SessionInstalledPlugin(enabled, installed_at, marketplace, name, cache_path, source, version)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        result["installed_at"] = from_str(self.installed_at)
        result["marketplace"] = from_str(self.marketplace)
        result["name"] = from_str(self.name)
        if self.cache_path is not None:
            result["cache_path"] = from_union([from_str, from_none], self.cache_path)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_class(SessionInstalledPluginSource, x), from_str, from_none], self.source)
        if self.version is not None:
            result["version"] = from_union([from_str, from_none], self.version)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionEnrichMetadataResult:
    """The same metadata records, with summary and context fields backfilled where available."""

    sessions: list[SessionMetadata]
    """Same records, with summary and context backfilled"""

    @staticmethod
    def from_dict(obj: Any) -> SessionEnrichMetadataResult:
        assert isinstance(obj, dict)
        sessions = from_list(SessionMetadata.from_dict, obj.get("sessions"))
        return SessionEnrichMetadataResult(sessions)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessions"] = from_list(lambda x: to_class(SessionMetadata, x), self.sessions)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionList:
    """Persisted sessions matching the filter, ordered most-recently-modified first."""

    sessions: list[SessionMetadata]
    """Sessions ordered most-recently-modified first"""

    @staticmethod
    def from_dict(obj: Any) -> SessionList:
        assert isinstance(obj, dict)
        sessions = from_list(SessionMetadata.from_dict, obj.get("sessions"))
        return SessionList(sessions)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessions"] = from_list(lambda x: to_class(SessionMetadata, x), self.sessions)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsEnrichMetadataRequest:
    """Session metadata records to enrich with summary and context information."""

    sessions: list[SessionMetadata]
    """Session metadata records to enrich. Records that already have summary and context are
    returned unchanged.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsEnrichMetadataRequest:
        assert isinstance(obj, dict)
        sessions = from_list(SessionMetadata.from_dict, obj.get("sessions"))
        return SessionsEnrichMetadataRequest(sessions)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessions"] = from_list(lambda x: to_class(SessionMetadata, x), self.sessions)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionMetadataSnapshot:
    """Point-in-time snapshot of slow-changing session identifier and state fields"""

    already_in_use: bool
    """True when the session was detected to be in use by another process at construction time.
    Local consumers may surface a confirmation prompt before fully attaching. Always false
    for new sessions.
    """
    current_mode: MetadataSnapshotCurrentMode
    """The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')"""

    is_remote: bool
    """Whether this is a remote session (i.e., one whose runtime executes elsewhere and is
    steered through this process)
    """
    modified_time: datetime
    """ISO 8601 timestamp of when the session's persisted state was last modified on disk. For
    new sessions, equals startTime. For resumed sessions, reflects the previous modification
    time at construction.
    """
    session_id: str
    """The unique identifier of the session"""

    start_time: datetime
    """ISO 8601 timestamp of when the session started"""

    working_directory: str
    """Absolute path to the session's current working directory"""

    initial_name: str | None = None
    """User-provided name supplied at session construction (via `--name`), if any. Immutable
    after construction.
    """
    remote_metadata: MetadataSnapshotRemoteMetadata | None = None
    """Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are
    immutable for the lifetime of the session.
    """
    selected_model: str | None = None
    """Currently selected model identifier, if any"""

    summary: str | None = None
    """Short human-readable summary of the session, if known. Omitted when no summary has been
    generated.
    """
    workspace: WorkspaceSummary | None = None
    """Public-facing workspace metadata for this session, or null if the session has no
    associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count,
    internal flags).
    """
    workspace_path: str | None = None
    """Absolute path to the session's workspace directory on disk, or null if the session has no
    associated workspace
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionMetadataSnapshot:
        assert isinstance(obj, dict)
        already_in_use = from_bool(obj.get("alreadyInUse"))
        current_mode = MetadataSnapshotCurrentMode(obj.get("currentMode"))
        is_remote = from_bool(obj.get("isRemote"))
        modified_time = from_datetime(obj.get("modifiedTime"))
        session_id = from_str(obj.get("sessionId"))
        start_time = from_datetime(obj.get("startTime"))
        working_directory = from_str(obj.get("workingDirectory"))
        initial_name = from_union([from_str, from_none], obj.get("initialName"))
        remote_metadata = from_union([MetadataSnapshotRemoteMetadata.from_dict, from_none], obj.get("remoteMetadata"))
        selected_model = from_union([from_str, from_none], obj.get("selectedModel"))
        summary = from_union([from_str, from_none], obj.get("summary"))
        workspace = from_union([WorkspaceSummary.from_dict, from_none], obj.get("workspace"))
        workspace_path = from_union([from_none, from_str], obj.get("workspacePath"))
        return SessionMetadataSnapshot(already_in_use, current_mode, is_remote, modified_time, session_id, start_time, working_directory, initial_name, remote_metadata, selected_model, summary, workspace, workspace_path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["alreadyInUse"] = from_bool(self.already_in_use)
        result["currentMode"] = to_enum(MetadataSnapshotCurrentMode, self.current_mode)
        result["isRemote"] = from_bool(self.is_remote)
        result["modifiedTime"] = self.modified_time.isoformat()
        result["sessionId"] = from_str(self.session_id)
        result["startTime"] = self.start_time.isoformat()
        result["workingDirectory"] = from_str(self.working_directory)
        if self.initial_name is not None:
            result["initialName"] = from_union([from_str, from_none], self.initial_name)
        if self.remote_metadata is not None:
            result["remoteMetadata"] = from_union([lambda x: to_class(MetadataSnapshotRemoteMetadata, x), from_none], self.remote_metadata)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_str, from_none], self.selected_model)
        if self.summary is not None:
            result["summary"] = from_union([from_str, from_none], self.summary)
        if self.workspace is not None:
            result["workspace"] = from_union([lambda x: to_class(WorkspaceSummary, x), from_none], self.workspace)
        result["workspacePath"] = from_union([from_none, from_str], self.workspace_path)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsConfigureParams:
    """Patch of permission policy fields to apply (omit a field to leave it unchanged)."""

    additional_content_exclusion_policies: list[PermissionsConfigureAdditionalContentExclusionPolicy] | None = None
    """If specified, replaces the host-supplied GitHub Content Exclusion policies on the session
    (combined with natively-discovered policies when evaluating tool/file access). Omit to
    leave the current policies unchanged.
    """
    approve_all_read_permission_requests: bool | None = None
    """If specified, sets whether path/URL read permission requests are auto-approved. Omit to
    leave the current value unchanged.
    """
    approve_all_tool_permission_requests: bool | None = None
    """If specified, sets whether tool permission requests are auto-approved without prompting.
    Omit to leave the current value unchanged.
    """
    paths: PermissionPathsConfig | None = None
    """If specified, replaces the session's path-permission policy. The runtime constructs the
    appropriate PathManager based on these inputs (rooted at the session's working
    directory). Omit to leave the current path policy unchanged.
    """
    rules: PermissionRulesSet | None = None
    """If specified, replaces the session's approved/denied permission rules. Omit to leave the
    current rules unchanged.
    """
    urls: PermissionUrlsConfig | None = None
    """If specified, replaces the session's URL-permission policy. The runtime constructs a
    fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy
    unchanged.
    """

    @staticmethod
    def from_dict(obj: Any) -> PermissionsConfigureParams:
        assert isinstance(obj, dict)
        additional_content_exclusion_policies = from_union([lambda x: from_list(PermissionsConfigureAdditionalContentExclusionPolicy.from_dict, x), from_none], obj.get("additionalContentExclusionPolicies"))
        approve_all_read_permission_requests = from_union([from_bool, from_none], obj.get("approveAllReadPermissionRequests"))
        approve_all_tool_permission_requests = from_union([from_bool, from_none], obj.get("approveAllToolPermissionRequests"))
        paths = from_union([PermissionPathsConfig.from_dict, from_none], obj.get("paths"))
        rules = from_union([PermissionRulesSet.from_dict, from_none], obj.get("rules"))
        urls = from_union([PermissionUrlsConfig.from_dict, from_none], obj.get("urls"))
        return PermissionsConfigureParams(additional_content_exclusion_policies, approve_all_read_permission_requests, approve_all_tool_permission_requests, paths, rules, urls)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.additional_content_exclusion_policies is not None:
            result["additionalContentExclusionPolicies"] = from_union([lambda x: from_list(lambda x: to_class(PermissionsConfigureAdditionalContentExclusionPolicy, x), x), from_none], self.additional_content_exclusion_policies)
        if self.approve_all_read_permission_requests is not None:
            result["approveAllReadPermissionRequests"] = from_union([from_bool, from_none], self.approve_all_read_permission_requests)
        if self.approve_all_tool_permission_requests is not None:
            result["approveAllToolPermissionRequests"] = from_union([from_bool, from_none], self.approve_all_tool_permission_requests)
        if self.paths is not None:
            result["paths"] = from_union([lambda x: to_class(PermissionPathsConfig, x), from_none], self.paths)
        if self.rules is not None:
            result["rules"] = from_union([lambda x: to_class(PermissionRulesSet, x), from_none], self.rules)
        if self.urls is not None:
            result["urls"] = from_union([lambda x: to_class(PermissionUrlsConfig, x), from_none], self.urls)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TasksGetProgressResult:
    """Progress information for the task, or null when no task with that ID is tracked."""

    progress: TaskProgress | None = None
    """Progress information for the task, discriminated by type. Returns null when no task with
    this ID is currently tracked.
    """

    @staticmethod
    def from_dict(obj: Any) -> TasksGetProgressResult:
        assert isinstance(obj, dict)
        progress = from_union([TaskProgress.from_dict, from_none], obj.get("progress"))
        return TasksGetProgressResult(progress)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.progress is not None:
            result["progress"] = from_union([lambda x: to_class(TaskProgress, x), from_none], self.progress)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationSchema:
    """JSON Schema describing the form fields to present to the user"""

    properties: dict[str, UIElicitationSchemaProperty]
    """Form field definitions, keyed by field name"""

    type: UIElicitationSchemaType
    """Schema type indicator (always 'object')"""

    required: list[str] | None = None
    """List of required field names"""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationSchema:
        assert isinstance(obj, dict)
        properties = from_dict(UIElicitationSchemaProperty.from_dict, obj.get("properties"))
        type = UIElicitationSchemaType(obj.get("type"))
        required = from_union([lambda x: from_list(from_str, x), from_none], obj.get("required"))
        return UIElicitationSchema(properties, type, required)

    def to_dict(self) -> dict:
        result: dict = {}
        result["properties"] = from_dict(lambda x: to_class(UIElicitationSchemaProperty, x), self.properties)
        result["type"] = to_enum(UIElicitationSchemaType, self.type)
        if self.required is not None:
            result["required"] = from_union([lambda x: from_list(from_str, x), from_none], self.required)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsSetAdditionalPluginsRequest:
    """Manager-wide additional plugins to register; replaces any previously-configured set."""

    plugins: list[InstalledPlugin]
    """Manager-wide additional plugins to register. Replaces any previously-configured set. Pass
    an empty array to clear.
    """

    @staticmethod
    def from_dict(obj: Any) -> SessionsSetAdditionalPluginsRequest:
        assert isinstance(obj, dict)
        plugins = from_list(InstalledPlugin.from_dict, obj.get("plugins"))
        return SessionsSetAdditionalPluginsRequest(plugins)

    def to_dict(self) -> dict:
        result: dict = {}
        result["plugins"] = from_list(lambda x: to_class(InstalledPlugin, x), self.plugins)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionUpdateOptionsParams:
    """Patch of mutable session options to apply to the running session."""

    additional_content_exclusion_policies: list[Any] | None = None
    """Additional content-exclusion policies to merge into the session's policy set. Opaque
    shape; see `ContentExclusionApiResponse` in the runtime.
    """
    agent_context: str | None = None
    """Runtime context discriminator (e.g., `cli`, `actions`)."""

    ask_user_disabled: bool | None = None
    """Whether to disable the `ask_user` tool (encourages autonomous behavior)."""

    available_tools: list[str] | None = None
    """Allowlist of tool names available to this session."""

    client_name: str | None = None
    """Identifier of the client driving the session."""

    coauthor_enabled: bool | None = None
    """Whether to include the `Co-authored-by` trailer in commit messages."""

    continue_on_auto_mode: bool | None = None
    """Whether to allow auto-mode continuation across turns."""

    copilot_url: str | None = None
    """Override URL for the Copilot API endpoint."""

    custom_agents_local_only: bool | None = None
    """Whether to default custom agents to local-only execution."""

    disabled_instruction_sources: list[str] | None = None
    """Instruction source IDs to exclude from the system prompt."""

    disabled_skills: list[str] | None = None
    """Skill IDs that should be excluded from this session."""

    enable_on_demand_instruction_discovery: bool | None = None
    """Whether to discover custom instructions on demand after successful file views (AGENTS.md
    / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with
    `skipCustomInstructions` and the runtime-side `ON_DEMAND_INSTRUCTIONS` feature flag.
    """
    enable_reasoning_summaries: bool | None = None
    """Whether to surface reasoning-summary events from the model."""

    enable_script_safety: bool | None = None
    """Whether shell-script safety heuristics are enabled."""

    enable_streaming: bool | None = None
    """Whether to stream model responses."""

    env_value_mode: MCPSetEnvValueModeDetails | None = None
    """How env values are passed to MCP servers (`direct` inlines literal values; `indirect`
    resolves at launch).
    """
    events_log_directory: str | None = None
    """Override directory for the session-events log. When unset, the runtime's default events
    log directory is used.
    """
    excluded_tools: list[str] | None = None
    """Denylist of tool names for this session."""

    feature_flags: dict[str, bool] | None = None
    """Map of feature-flag IDs to their boolean enabled state."""

    installed_plugins: list[SessionInstalledPlugin] | None = None
    """Full set of installed plugins for the session. Replaces the existing list; the runtime
    invalidates the skills cache only when the list materially changes.
    """
    integration_id: str | None = None
    """Stable integration identifier used for analytics and rate-limit attribution."""

    is_experimental_mode: bool | None = None
    """Whether experimental capabilities are enabled."""

    log_interactive_shells: bool | None = None
    """Whether interactive shell sessions are logged."""

    lsp_client_name: str | None = None
    """Identifier sent to LSP-style integrations."""

    manage_schedule_enabled: bool | None = None
    """Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the
    per-session schedule registry; this flag only controls tool exposure (typically gated to
    staff users).
    """
    model: str | None = None
    """The model ID to use for assistant turns."""

    provider: Any = None
    """Custom model-provider configuration (BYOK). Opaque shape; see `ProviderConfig` in the
    runtime.
    """
    reasoning_effort: str | None = None
    """Reasoning effort for the selected model (model-defined enum)."""

    running_in_interactive_mode: bool | None = None
    """Whether the session is running in an interactive UI."""

    sandbox_config: Any = None
    """Sandbox configuration shape; opaque to SDK consumers. See `SandboxConfig` in the runtime."""

    shell_init_profile: str | None = None
    """Shell init profile (`None` or `NonInteractive`)."""

    shell_process_flags: list[str] | None = None
    """Per-shell process flags (e.g., `pwsh` arguments)."""

    skill_directories: list[str] | None = None
    """Additional directories to search for skills."""

    skip_custom_instructions: bool | None = None
    """Whether to skip loading custom instruction sources."""

    trajectory_file: str | None = None
    """Optional path for trajectory output."""

    working_directory: str | None = None
    """Absolute working-directory path for shell tools."""

    @staticmethod
    def from_dict(obj: Any) -> SessionUpdateOptionsParams:
        assert isinstance(obj, dict)
        additional_content_exclusion_policies = from_union([lambda x: from_list(lambda x: x, x), from_none], obj.get("additionalContentExclusionPolicies"))
        agent_context = from_union([from_str, from_none], obj.get("agentContext"))
        ask_user_disabled = from_union([from_bool, from_none], obj.get("askUserDisabled"))
        available_tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("availableTools"))
        client_name = from_union([from_str, from_none], obj.get("clientName"))
        coauthor_enabled = from_union([from_bool, from_none], obj.get("coauthorEnabled"))
        continue_on_auto_mode = from_union([from_bool, from_none], obj.get("continueOnAutoMode"))
        copilot_url = from_union([from_str, from_none], obj.get("copilotUrl"))
        custom_agents_local_only = from_union([from_bool, from_none], obj.get("customAgentsLocalOnly"))
        disabled_instruction_sources = from_union([lambda x: from_list(from_str, x), from_none], obj.get("disabledInstructionSources"))
        disabled_skills = from_union([lambda x: from_list(from_str, x), from_none], obj.get("disabledSkills"))
        enable_on_demand_instruction_discovery = from_union([from_bool, from_none], obj.get("enableOnDemandInstructionDiscovery"))
        enable_reasoning_summaries = from_union([from_bool, from_none], obj.get("enableReasoningSummaries"))
        enable_script_safety = from_union([from_bool, from_none], obj.get("enableScriptSafety"))
        enable_streaming = from_union([from_bool, from_none], obj.get("enableStreaming"))
        env_value_mode = from_union([MCPSetEnvValueModeDetails, from_none], obj.get("envValueMode"))
        events_log_directory = from_union([from_str, from_none], obj.get("eventsLogDirectory"))
        excluded_tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("excludedTools"))
        feature_flags = from_union([lambda x: from_dict(from_bool, x), from_none], obj.get("featureFlags"))
        installed_plugins = from_union([lambda x: from_list(SessionInstalledPlugin.from_dict, x), from_none], obj.get("installedPlugins"))
        integration_id = from_union([from_str, from_none], obj.get("integrationId"))
        is_experimental_mode = from_union([from_bool, from_none], obj.get("isExperimentalMode"))
        log_interactive_shells = from_union([from_bool, from_none], obj.get("logInteractiveShells"))
        lsp_client_name = from_union([from_str, from_none], obj.get("lspClientName"))
        manage_schedule_enabled = from_union([from_bool, from_none], obj.get("manageScheduleEnabled"))
        model = from_union([from_str, from_none], obj.get("model"))
        provider = obj.get("provider")
        reasoning_effort = from_union([from_str, from_none], obj.get("reasoningEffort"))
        running_in_interactive_mode = from_union([from_bool, from_none], obj.get("runningInInteractiveMode"))
        sandbox_config = obj.get("sandboxConfig")
        shell_init_profile = from_union([from_str, from_none], obj.get("shellInitProfile"))
        shell_process_flags = from_union([lambda x: from_list(from_str, x), from_none], obj.get("shellProcessFlags"))
        skill_directories = from_union([lambda x: from_list(from_str, x), from_none], obj.get("skillDirectories"))
        skip_custom_instructions = from_union([from_bool, from_none], obj.get("skipCustomInstructions"))
        trajectory_file = from_union([from_str, from_none], obj.get("trajectoryFile"))
        working_directory = from_union([from_str, from_none], obj.get("workingDirectory"))
        return SessionUpdateOptionsParams(additional_content_exclusion_policies, agent_context, ask_user_disabled, available_tools, client_name, coauthor_enabled, continue_on_auto_mode, copilot_url, custom_agents_local_only, disabled_instruction_sources, disabled_skills, enable_on_demand_instruction_discovery, enable_reasoning_summaries, enable_script_safety, enable_streaming, env_value_mode, events_log_directory, excluded_tools, feature_flags, installed_plugins, integration_id, is_experimental_mode, log_interactive_shells, lsp_client_name, manage_schedule_enabled, model, provider, reasoning_effort, running_in_interactive_mode, sandbox_config, shell_init_profile, shell_process_flags, skill_directories, skip_custom_instructions, trajectory_file, working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.additional_content_exclusion_policies is not None:
            result["additionalContentExclusionPolicies"] = from_union([lambda x: from_list(lambda x: x, x), from_none], self.additional_content_exclusion_policies)
        if self.agent_context is not None:
            result["agentContext"] = from_union([from_str, from_none], self.agent_context)
        if self.ask_user_disabled is not None:
            result["askUserDisabled"] = from_union([from_bool, from_none], self.ask_user_disabled)
        if self.available_tools is not None:
            result["availableTools"] = from_union([lambda x: from_list(from_str, x), from_none], self.available_tools)
        if self.client_name is not None:
            result["clientName"] = from_union([from_str, from_none], self.client_name)
        if self.coauthor_enabled is not None:
            result["coauthorEnabled"] = from_union([from_bool, from_none], self.coauthor_enabled)
        if self.continue_on_auto_mode is not None:
            result["continueOnAutoMode"] = from_union([from_bool, from_none], self.continue_on_auto_mode)
        if self.copilot_url is not None:
            result["copilotUrl"] = from_union([from_str, from_none], self.copilot_url)
        if self.custom_agents_local_only is not None:
            result["customAgentsLocalOnly"] = from_union([from_bool, from_none], self.custom_agents_local_only)
        if self.disabled_instruction_sources is not None:
            result["disabledInstructionSources"] = from_union([lambda x: from_list(from_str, x), from_none], self.disabled_instruction_sources)
        if self.disabled_skills is not None:
            result["disabledSkills"] = from_union([lambda x: from_list(from_str, x), from_none], self.disabled_skills)
        if self.enable_on_demand_instruction_discovery is not None:
            result["enableOnDemandInstructionDiscovery"] = from_union([from_bool, from_none], self.enable_on_demand_instruction_discovery)
        if self.enable_reasoning_summaries is not None:
            result["enableReasoningSummaries"] = from_union([from_bool, from_none], self.enable_reasoning_summaries)
        if self.enable_script_safety is not None:
            result["enableScriptSafety"] = from_union([from_bool, from_none], self.enable_script_safety)
        if self.enable_streaming is not None:
            result["enableStreaming"] = from_union([from_bool, from_none], self.enable_streaming)
        if self.env_value_mode is not None:
            result["envValueMode"] = from_union([lambda x: to_enum(MCPSetEnvValueModeDetails, x), from_none], self.env_value_mode)
        if self.events_log_directory is not None:
            result["eventsLogDirectory"] = from_union([from_str, from_none], self.events_log_directory)
        if self.excluded_tools is not None:
            result["excludedTools"] = from_union([lambda x: from_list(from_str, x), from_none], self.excluded_tools)
        if self.feature_flags is not None:
            result["featureFlags"] = from_union([lambda x: from_dict(from_bool, x), from_none], self.feature_flags)
        if self.installed_plugins is not None:
            result["installedPlugins"] = from_union([lambda x: from_list(lambda x: to_class(SessionInstalledPlugin, x), x), from_none], self.installed_plugins)
        if self.integration_id is not None:
            result["integrationId"] = from_union([from_str, from_none], self.integration_id)
        if self.is_experimental_mode is not None:
            result["isExperimentalMode"] = from_union([from_bool, from_none], self.is_experimental_mode)
        if self.log_interactive_shells is not None:
            result["logInteractiveShells"] = from_union([from_bool, from_none], self.log_interactive_shells)
        if self.lsp_client_name is not None:
            result["lspClientName"] = from_union([from_str, from_none], self.lsp_client_name)
        if self.manage_schedule_enabled is not None:
            result["manageScheduleEnabled"] = from_union([from_bool, from_none], self.manage_schedule_enabled)
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        if self.provider is not None:
            result["provider"] = self.provider
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_str, from_none], self.reasoning_effort)
        if self.running_in_interactive_mode is not None:
            result["runningInInteractiveMode"] = from_union([from_bool, from_none], self.running_in_interactive_mode)
        if self.sandbox_config is not None:
            result["sandboxConfig"] = self.sandbox_config
        if self.shell_init_profile is not None:
            result["shellInitProfile"] = from_union([from_str, from_none], self.shell_init_profile)
        if self.shell_process_flags is not None:
            result["shellProcessFlags"] = from_union([lambda x: from_list(from_str, x), from_none], self.shell_process_flags)
        if self.skill_directories is not None:
            result["skillDirectories"] = from_union([lambda x: from_list(from_str, x), from_none], self.skill_directories)
        if self.skip_custom_instructions is not None:
            result["skipCustomInstructions"] = from_union([from_bool, from_none], self.skip_custom_instructions)
        if self.trajectory_file is not None:
            result["trajectoryFile"] = from_union([from_str, from_none], self.trajectory_file)
        if self.working_directory is not None:
            result["workingDirectory"] = from_union([from_str, from_none], self.working_directory)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UIElicitationRequest:
    """Prompt message and JSON schema describing the form fields to elicit from the user."""

    message: str
    """Message describing what information is needed from the user"""

    requested_schema: UIElicitationSchema
    """JSON Schema describing the form fields to present to the user"""

    @staticmethod
    def from_dict(obj: Any) -> UIElicitationRequest:
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        requested_schema = UIElicitationSchema.from_dict(obj.get("requestedSchema"))
        return UIElicitationRequest(message, requested_schema)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["requestedSchema"] = to_class(UIElicitationSchema, self.requested_schema)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MCPExecuteSamplingParams:
    """Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference."""

    mcp_request_id: float | str
    """The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate
    the inference with the originating MCP request for telemetry; this is distinct from
    `requestId` (which is the schema-level cancellation handle).
    """
    request: dict[str, Any]
    """Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event.
    Treated as opaque at the schema layer; the runtime converts the embedded MCP messages
    into the OpenAI chat-completion shape internally.
    """
    request_id: str
    """Caller-provided unique identifier for this sampling execution. Use this same ID with
    cancelSamplingExecution to cancel the in-flight call. Must be unique within the session
    for the lifetime of the call.
    """
    server_name: str
    """Name of the MCP server that initiated the sampling request"""

    @staticmethod
    def from_dict(obj: Any) -> MCPExecuteSamplingParams:
        assert isinstance(obj, dict)
        mcp_request_id = from_union([from_float, from_str], obj.get("mcpRequestId"))
        request = from_dict(lambda x: x, obj.get("request"))
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        return MCPExecuteSamplingParams(mcp_request_id, request, request_id, server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mcpRequestId"] = from_union([to_float, from_str], self.mcp_request_id)
        result["request"] = from_dict(lambda x: x, self.request)
        result["requestId"] = from_str(self.request_id)
        result["serverName"] = from_str(self.server_name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataContextInfoRequest:
    """Model identifier and token limits used to compute the context-info breakdown."""

    output_token_limit: int
    """Maximum output tokens allowed by the target model. Pass 0 if unknown."""

    prompt_token_limit: int
    """Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default."""

    selected_model: str | None = None
    """Model identifier used for tokenization. Omit to use the session default. Used both for
    token counting and to compute display values.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataContextInfoRequest:
        assert isinstance(obj, dict)
        output_token_limit = from_int(obj.get("outputTokenLimit"))
        prompt_token_limit = from_int(obj.get("promptTokenLimit"))
        selected_model = from_union([from_str, from_none], obj.get("selectedModel"))
        return MetadataContextInfoRequest(output_token_limit, prompt_token_limit, selected_model)

    def to_dict(self) -> dict:
        result: dict = {}
        result["outputTokenLimit"] = from_int(self.output_token_limit)
        result["promptTokenLimit"] = from_int(self.prompt_token_limit)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_str, from_none], self.selected_model)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class MetadataRecomputeContextTokensRequest:
    """Model identifier to use when re-tokenizing the session's existing messages."""

    model_id: str
    """Model identifier used for tokenization. The runtime token-counts both chat-context and
    system-context messages against this model.
    """

    @staticmethod
    def from_dict(obj: Any) -> MetadataRecomputeContextTokensRequest:
        assert isinstance(obj, dict)
        model_id = from_str(obj.get("modelId"))
        return MetadataRecomputeContextTokensRequest(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["modelId"] = from_str(self.model_id)
        return result

@dataclass
class ModelCapabilities:
    """Model capabilities and limits"""

    limits: ModelCapabilitiesLimits | None = None
    """Token limits for prompts, outputs, and context window"""

    supports: ModelCapabilitiesSupports | None = None
    """Feature flags indicating what the model supports"""

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilities:
        assert isinstance(obj, dict)
        limits = from_union([ModelCapabilitiesLimits.from_dict, from_none], obj.get("limits"))
        supports = from_union([ModelCapabilitiesSupports.from_dict, from_none], obj.get("supports"))
        return ModelCapabilities(limits, supports)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.limits is not None:
            result["limits"] = from_union([lambda x: to_class(ModelCapabilitiesLimits, x), from_none], self.limits)
        if self.supports is not None:
            result["supports"] = from_union([lambda x: to_class(ModelCapabilitiesSupports, x), from_none], self.supports)
        return result

class ModelPickerCategory(Enum):
    """Model capability category for grouping in the model picker"""

    LIGHTWEIGHT = "lightweight"
    POWERFUL = "powerful"
    VERSATILE = "versatile"

@dataclass
class Model:
    """Schema for the `Model` type."""

    capabilities: ModelCapabilities
    """Model capabilities and limits"""

    id: str
    """Model identifier (e.g., "claude-sonnet-4.5")"""

    name: str
    """Display name"""

    billing: ModelBilling | None = None
    """Billing information"""

    default_reasoning_effort: str | None = None
    """Default reasoning effort level (only present if model supports reasoning effort)"""

    model_picker_category: ModelPickerCategory | None = None
    """Model capability category for grouping in the model picker"""

    model_picker_price_category: ModelPickerPriceCategory | None = None
    """Relative cost tier for token-based billing users"""

    policy: ModelPolicy | None = None
    """Policy state (if applicable)"""

    supported_reasoning_efforts: list[str] | None = None
    """Supported reasoning effort levels (only present if model supports reasoning effort)"""

    @staticmethod
    def from_dict(obj: Any) -> Model:
        assert isinstance(obj, dict)
        capabilities = ModelCapabilities.from_dict(obj.get("capabilities"))
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        billing = from_union([ModelBilling.from_dict, from_none], obj.get("billing"))
        default_reasoning_effort = from_union([from_str, from_none], obj.get("defaultReasoningEffort"))
        model_picker_category = from_union([ModelPickerCategory, from_none], obj.get("modelPickerCategory"))
        model_picker_price_category = from_union([ModelPickerPriceCategory, from_none], obj.get("modelPickerPriceCategory"))
        policy = from_union([ModelPolicy.from_dict, from_none], obj.get("policy"))
        supported_reasoning_efforts = from_union([lambda x: from_list(from_str, x), from_none], obj.get("supportedReasoningEfforts"))
        return Model(capabilities, id, name, billing, default_reasoning_effort, model_picker_category, model_picker_price_category, policy, supported_reasoning_efforts)

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = to_class(ModelCapabilities, self.capabilities)
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        if self.billing is not None:
            result["billing"] = from_union([lambda x: to_class(ModelBilling, x), from_none], self.billing)
        if self.default_reasoning_effort is not None:
            result["defaultReasoningEffort"] = from_union([from_str, from_none], self.default_reasoning_effort)
        if self.model_picker_category is not None:
            result["modelPickerCategory"] = from_union([lambda x: to_enum(ModelPickerCategory, x), from_none], self.model_picker_category)
        if self.model_picker_price_category is not None:
            result["modelPickerPriceCategory"] = from_union([lambda x: to_enum(ModelPickerPriceCategory, x), from_none], self.model_picker_price_category)
        if self.policy is not None:
            result["policy"] = from_union([lambda x: to_class(ModelPolicy, x), from_none], self.policy)
        if self.supported_reasoning_efforts is not None:
            result["supportedReasoningEfforts"] = from_union([lambda x: from_list(from_str, x), from_none], self.supported_reasoning_efforts)
        return result

@dataclass
class ModelList:
    """List of Copilot models available to the resolved user, including capabilities and billing
    metadata.
    """
    models: list[Model]
    """List of available models with full metadata"""

    @staticmethod
    def from_dict(obj: Any) -> ModelList:
        assert isinstance(obj, dict)
        models = from_list(Model.from_dict, obj.get("models"))
        return ModelList(models)

    def to_dict(self) -> dict:
        result: dict = {}
        result["models"] = from_list(lambda x: to_class(Model, x), self.models)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ModelSwitchToRequest:
    """Target model identifier and optional reasoning effort, summary, and capability overrides."""

    model_id: str
    """Model identifier to switch to"""

    model_capabilities: ModelCapabilitiesOverride | None = None
    """Override individual model capabilities resolved by the runtime"""

    reasoning_effort: str | None = None
    """Reasoning effort level to use for the model. "none" disables reasoning."""

    reasoning_summary: ReasoningSummary | None = None
    """Reasoning summary mode to request for supported model clients"""

    @staticmethod
    def from_dict(obj: Any) -> ModelSwitchToRequest:
        assert isinstance(obj, dict)
        model_id = from_str(obj.get("modelId"))
        model_capabilities = from_union([ModelCapabilitiesOverride.from_dict, from_none], obj.get("modelCapabilities"))
        reasoning_effort = from_union([from_str, from_none], obj.get("reasoningEffort"))
        reasoning_summary = from_union([ReasoningSummary, from_none], obj.get("reasoningSummary"))
        return ModelSwitchToRequest(model_id, model_capabilities, reasoning_effort, reasoning_summary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["modelId"] = from_str(self.model_id)
        if self.model_capabilities is not None:
            result["modelCapabilities"] = from_union([lambda x: to_class(ModelCapabilitiesOverride, x), from_none], self.model_capabilities)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_str, from_none], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([lambda x: to_enum(ReasoningSummary, x), from_none], self.reasoning_summary)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
class PermissionsSetApproveAllSource(Enum):
    """Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers."""

    AUTOPILOT_CONFIRMATION = "autopilot_confirmation"
    CLI_FLAG = "cli_flag"
    RPC = "rpc"
    SLASH_COMMAND = "slash_command"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionsSetApproveAllRequest:
    """Allow-all toggle for tool permission requests, with an optional telemetry source."""

    enabled: bool
    """Whether to auto-approve all tool permission requests"""

    source: PermissionsSetApproveAllSource | None = None
    """Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers."""

    @staticmethod
    def from_dict(obj: Any) -> PermissionsSetApproveAllRequest:
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        source = from_union([PermissionsSetApproveAllSource, from_none], obj.get("source"))
        return PermissionsSetApproveAllRequest(enabled, source)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_enum(PermissionsSetApproveAllSource, x), from_none], self.source)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class TaskAgentInfo:
    """Schema for the `TaskAgentInfo` type."""

    agent_type: str
    """Type of agent running this task"""

    description: str
    """Short description of the task"""

    id: str
    """Unique task identifier"""

    prompt: str
    """Prompt passed to the agent"""

    started_at: datetime
    """ISO 8601 timestamp when the task was started"""

    status: TaskStatus
    """Current lifecycle status of the task"""

    tool_call_id: str
    """Tool call ID associated with this agent task"""

    type: ClassVar[str] = "agent"
    """Task kind"""

    active_started_at: datetime | None = None
    """ISO 8601 timestamp when the current active period began"""

    active_time_ms: int | None = None
    """Accumulated active execution time in milliseconds"""

    can_promote_to_background: bool | None = None
    """Whether the task is currently in the original sync wait and can be moved to background
    mode. False once it is already backgrounded, idle, finished, or no longer has a
    promotable sync waiter.
    """
    completed_at: datetime | None = None
    """ISO 8601 timestamp when the task finished"""

    error: str | None = None
    """Error message when the task failed"""

    execution_mode: TaskExecutionMode | None = None
    """Whether task execution is synchronously awaited or managed in the background"""

    idle_since: datetime | None = None
    """ISO 8601 timestamp when the agent entered idle state"""

    latest_response: str | None = None
    """Most recent response text from the agent"""

    model: str | None = None
    """Model used for the task when specified"""

    result: str | None = None
    """Result text from the task when available"""

    @staticmethod
    def from_dict(obj: Any) -> TaskAgentInfo:
        assert isinstance(obj, dict)
        agent_type = from_str(obj.get("agentType"))
        description = from_str(obj.get("description"))
        id = from_str(obj.get("id"))
        prompt = from_str(obj.get("prompt"))
        started_at = from_datetime(obj.get("startedAt"))
        status = TaskStatus(obj.get("status"))
        tool_call_id = from_str(obj.get("toolCallId"))
        active_started_at = from_union([from_datetime, from_none], obj.get("activeStartedAt"))
        active_time_ms = from_union([from_int, from_none], obj.get("activeTimeMs"))
        can_promote_to_background = from_union([from_bool, from_none], obj.get("canPromoteToBackground"))
        completed_at = from_union([from_datetime, from_none], obj.get("completedAt"))
        error = from_union([from_str, from_none], obj.get("error"))
        execution_mode = from_union([TaskExecutionMode, from_none], obj.get("executionMode"))
        idle_since = from_union([from_datetime, from_none], obj.get("idleSince"))
        latest_response = from_union([from_str, from_none], obj.get("latestResponse"))
        model = from_union([from_str, from_none], obj.get("model"))
        result = from_union([from_str, from_none], obj.get("result"))
        return TaskAgentInfo(agent_type, description, id, prompt, started_at, status, tool_call_id, active_started_at, active_time_ms, can_promote_to_background, completed_at, error, execution_mode, idle_since, latest_response, model, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentType"] = from_str(self.agent_type)
        result["description"] = from_str(self.description)
        result["id"] = from_str(self.id)
        result["prompt"] = from_str(self.prompt)
        result["startedAt"] = self.started_at.isoformat()
        result["status"] = to_enum(TaskStatus, self.status)
        result["toolCallId"] = from_str(self.tool_call_id)
        result["type"] = self.type
        if self.active_started_at is not None:
            result["activeStartedAt"] = from_union([lambda x: x.isoformat(), from_none], self.active_started_at)
        if self.active_time_ms is not None:
            result["activeTimeMs"] = from_union([from_int, from_none], self.active_time_ms)
        if self.can_promote_to_background is not None:
            result["canPromoteToBackground"] = from_union([from_bool, from_none], self.can_promote_to_background)
        if self.completed_at is not None:
            result["completedAt"] = from_union([lambda x: x.isoformat(), from_none], self.completed_at)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.execution_mode is not None:
            result["executionMode"] = from_union([lambda x: to_enum(TaskExecutionMode, x), from_none], self.execution_mode)
        if self.idle_since is not None:
            result["idleSince"] = from_union([lambda x: x.isoformat(), from_none], self.idle_since)
        if self.latest_response is not None:
            result["latestResponse"] = from_union([from_str, from_none], self.latest_response)
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        if self.result is not None:
            result["result"] = from_union([from_str, from_none], self.result)
        return result

@dataclass
class RPC:
    abort_request: AbortRequest
    abort_result: AbortResult
    account_get_quota_request: AccountGetQuotaRequest
    account_get_quota_result: AccountGetQuotaResult
    account_quota_snapshot: AccountQuotaSnapshot
    agent_get_current_result: AgentGetCurrentResult
    agent_info: AgentInfo
    agent_info_source: AgentInfoSource
    agent_list: AgentList
    agent_reload_result: AgentReloadResult
    agent_select_request: AgentSelectRequest
    agent_select_result: AgentSelectResult
    api_key_auth_info: APIKeyAuthInfo
    auth_info: AuthInfo
    auth_info_type: AuthInfoType
    command_list: CommandList
    commands_handle_pending_command_request: CommandsHandlePendingCommandRequest
    commands_handle_pending_command_result: CommandsHandlePendingCommandResult
    commands_invoke_request: CommandsInvokeRequest
    commands_list_request: CommandsListRequest
    commands_respond_to_queued_command_request: CommandsRespondToQueuedCommandRequest
    commands_respond_to_queued_command_result: CommandsRespondToQueuedCommandResult
    connected_remote_session_metadata: ConnectedRemoteSessionMetadata
    connected_remote_session_metadata_kind: ConnectedRemoteSessionMetadataKind
    connected_remote_session_metadata_repository: ConnectedRemoteSessionMetadataRepository
    connect_remote_session_params: ConnectRemoteSessionParams
    connect_request: ConnectRequest
    connect_result: ConnectResult
    content_filter_mode: ContentFilterMode
    copilot_api_token_auth_info: CopilotAPITokenAuthInfo
    copilot_user_response: CopilotUserResponse
    copilot_user_response_endpoints: CopilotUserResponseEndpoints
    copilot_user_response_quota_snapshots: dict[str, CopilotUserResponseQuotaSnapshots | None]
    copilot_user_response_quota_snapshots_chat: CopilotUserResponseQuotaSnapshotsChat
    copilot_user_response_quota_snapshots_completions: CopilotUserResponseQuotaSnapshotsCompletions
    copilot_user_response_quota_snapshots_premium_interactions: CopilotUserResponseQuotaSnapshotsPremiumInteractions
    current_model: CurrentModel
    discovered_mcp_server: DiscoveredMCPServer
    discovered_mcp_server_type: DiscoveredMCPServerType
    enqueue_command_params: EnqueueCommandParams
    enqueue_command_result: EnqueueCommandResult
    env_auth_info: EnvAuthInfo
    event_log_read_request: EventLogReadRequest
    event_log_release_interest_result: EventLogReleaseInterestResult
    event_log_tail_result: EventLogTailResult
    event_log_types: list[str] | EventLogTypes
    events_agent_scope: EventsAgentScope
    events_cursor_status: EventsCursorStatus
    events_read_result: EventsReadResult
    execute_command_params: ExecuteCommandParams
    execute_command_result: ExecuteCommandResult
    extension: Extension
    extension_list: ExtensionList
    extensions_disable_request: ExtensionsDisableRequest
    extensions_enable_request: ExtensionsEnableRequest
    extension_source: ExtensionSource
    extension_status: ExtensionStatus
    external_tool_result: ExternalToolTextResultForLlm | str
    external_tool_text_result_for_llm: ExternalToolTextResultForLlm
    external_tool_text_result_for_llm_binary_results_for_llm: ExternalToolTextResultForLlmBinaryResultsForLlm
    external_tool_text_result_for_llm_binary_results_for_llm_type: ExternalToolTextResultForLlmBinaryResultsForLlmType
    external_tool_text_result_for_llm_content: ExternalToolTextResultForLlmContent
    external_tool_text_result_for_llm_content_audio: ExternalToolTextResultForLlmContentAudio
    external_tool_text_result_for_llm_content_image: ExternalToolTextResultForLlmContentImage
    external_tool_text_result_for_llm_content_resource: ExternalToolTextResultForLlmContentResource
    external_tool_text_result_for_llm_content_resource_details: ExternalToolTextResultForLlmContentResourceDetails
    external_tool_text_result_for_llm_content_resource_link: ExternalToolTextResultForLlmContentResourceLink
    external_tool_text_result_for_llm_content_resource_link_icon: ExternalToolTextResultForLlmContentResourceLinkIcon
    external_tool_text_result_for_llm_content_resource_link_icon_theme: ExternalToolTextResultForLlmContentResourceLinkIconTheme
    external_tool_text_result_for_llm_content_terminal: ExternalToolTextResultForLlmContentTerminal
    external_tool_text_result_for_llm_content_text: ExternalToolTextResultForLlmContentText
    filter_mapping: dict[str, ContentFilterMode] | ContentFilterMode
    fleet_start_request: FleetStartRequest
    fleet_start_result: FleetStartResult
    folder_trust_add_params: FolderTrustAddParams
    folder_trust_check_params: FolderTrustCheckParams
    folder_trust_check_result: FolderTrustCheckResult
    gh_cli_auth_info: GhCLIAuthInfo
    handle_pending_tool_call_request: HandlePendingToolCallRequest
    handle_pending_tool_call_result: HandlePendingToolCallResult
    history_abort_manual_compaction_result: HistoryAbortManualCompactionResult
    history_cancel_background_compaction_result: HistoryCancelBackgroundCompactionResult
    history_compact_context_window: HistoryCompactContextWindow
    history_compact_request: HistoryCompactRequest
    history_compact_result: HistoryCompactResult
    history_summarize_for_handoff_result: HistorySummarizeForHandoffResult
    history_truncate_request: HistoryTruncateRequest
    history_truncate_result: HistoryTruncateResult
    hmac_auth_info: HMACAuthInfo
    installed_plugin: InstalledPlugin
    installed_plugin_source: InstalledPluginSource | str
    installed_plugin_source_github: InstalledPluginSourceGithub
    installed_plugin_source_local: InstalledPluginSourceLocal
    installed_plugin_source_url: InstalledPluginSourceURL
    instructions_get_sources_result: InstructionsGetSourcesResult
    instructions_sources: InstructionsSources
    instructions_sources_location: InstructionsSourcesLocation
    instructions_sources_type: InstructionsSourcesType
    log_request: LogRequest
    log_result: LogResult
    lsp_initialize_request: LspInitializeRequest
    mcp_cancel_sampling_execution_params: MCPCancelSamplingExecutionParams
    mcp_cancel_sampling_execution_result: MCPCancelSamplingExecutionResult
    mcp_config_add_request: MCPConfigAddRequest
    mcp_config_disable_request: MCPConfigDisableRequest
    mcp_config_enable_request: MCPConfigEnableRequest
    mcp_config_list: MCPConfigList
    mcp_config_remove_request: MCPConfigRemoveRequest
    mcp_config_update_request: MCPConfigUpdateRequest
    mcp_disable_request: MCPDisableRequest
    mcp_discover_request: MCPDiscoverRequest
    mcp_discover_result: MCPDiscoverResult
    mcp_enable_request: MCPEnableRequest
    mcp_execute_sampling_params: MCPExecuteSamplingParams
    mcp_execute_sampling_request: dict[str, Any]
    mcp_execute_sampling_result: dict[str, Any]
    mcp_oauth_login_request: MCPOauthLoginRequest
    mcp_oauth_login_result: MCPOauthLoginResult
    mcp_remove_git_hub_result: MCPRemoveGitHubResult
    mcp_sampling_execution_action: MCPSamplingExecutionAction
    mcp_sampling_execution_result: MCPSamplingExecutionResult
    mcp_server: MCPServer
    mcp_server_config: MCPServerConfig
    mcp_server_config_http: MCPServerConfigHTTP
    mcp_server_config_http_auth: MCPServerConfigHTTPAuth
    mcp_server_config_http_oauth_grant_type: MCPServerConfigHTTPOauthGrantType
    mcp_server_config_http_type: MCPServerConfigHTTPType
    mcp_server_config_stdio: MCPServerConfigStdio
    mcp_server_list: MCPServerList
    mcp_set_env_value_mode_details: MCPSetEnvValueModeDetails
    mcp_set_env_value_mode_params: MCPSetEnvValueModeParams
    mcp_set_env_value_mode_result: MCPSetEnvValueModeResult
    metadata_context_info_request: MetadataContextInfoRequest
    metadata_context_info_result: MetadataContextInfoResult
    metadata_is_processing_result: MetadataIsProcessingResult
    metadata_recompute_context_tokens_request: MetadataRecomputeContextTokensRequest
    metadata_recompute_context_tokens_result: MetadataRecomputeContextTokensResult
    metadata_record_context_change_request: MetadataRecordContextChangeRequest
    metadata_record_context_change_result: MetadataRecordContextChangeResult
    metadata_set_working_directory_request: MetadataSetWorkingDirectoryRequest
    metadata_set_working_directory_result: MetadataSetWorkingDirectoryResult
    metadata_snapshot_current_mode: MetadataSnapshotCurrentMode
    metadata_snapshot_remote_metadata: MetadataSnapshotRemoteMetadata
    metadata_snapshot_remote_metadata_repository: MetadataSnapshotRemoteMetadataRepository
    metadata_snapshot_remote_metadata_task_type: MetadataSnapshotRemoteMetadataTaskType
    model: Model
    model_billing: ModelBilling
    model_billing_token_prices: ModelBillingTokenPrices
    model_capabilities: ModelCapabilities
    model_capabilities_limits: ModelCapabilitiesLimits
    model_capabilities_limits_vision: ModelCapabilitiesLimitsVision
    model_capabilities_override: ModelCapabilitiesOverride
    model_capabilities_override_limits: ModelCapabilitiesOverrideLimits
    model_capabilities_override_limits_vision: ModelCapabilitiesOverrideLimitsVision
    model_capabilities_override_supports: ModelCapabilitiesOverrideSupports
    model_capabilities_supports: ModelCapabilitiesSupports
    model_list: ModelList
    model_picker_category: ModelPickerCategory
    model_picker_price_category: ModelPickerPriceCategory
    model_policy: ModelPolicy
    model_policy_state: ModelPolicyState
    model_set_reasoning_effort_request: ModelSetReasoningEffortRequest
    model_set_reasoning_effort_result: ModelSetReasoningEffortResult
    models_list_request: ModelsListRequest
    model_switch_to_request: ModelSwitchToRequest
    model_switch_to_result: ModelSwitchToResult
    mode_set_request: ModeSetRequest
    name_get_result: NameGetResult
    name_set_auto_request: NameSetAutoRequest
    name_set_auto_result: NameSetAutoResult
    name_set_request: NameSetRequest
    options_update_env_value_mode: MCPSetEnvValueModeDetails
    pending_permission_request: PendingPermissionRequest
    pending_permission_request_list: PendingPermissionRequestList
    permission_decision: PermissionDecision
    permission_decision_approved: PermissionDecisionApproved
    permission_decision_approved_for_location: PermissionDecisionApprovedForLocation
    permission_decision_approved_for_session: PermissionDecisionApprovedForSession
    permission_decision_approve_for_location: PermissionDecisionApproveForLocation
    permission_decision_approve_for_location_approval: PermissionDecisionApproveForLocationApproval
    permission_decision_approve_for_location_approval_commands: PermissionDecisionApproveForLocationApprovalCommands
    permission_decision_approve_for_location_approval_custom_tool: PermissionDecisionApproveForLocationApprovalCustomTool
    permission_decision_approve_for_location_approval_extension_management: PermissionDecisionApproveForLocationApprovalExtensionManagement
    permission_decision_approve_for_location_approval_extension_permission_access: PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess
    permission_decision_approve_for_location_approval_mcp: PermissionDecisionApproveForLocationApprovalMCP
    permission_decision_approve_for_location_approval_mcp_sampling: PermissionDecisionApproveForLocationApprovalMCPSampling
    permission_decision_approve_for_location_approval_memory: PermissionDecisionApproveForLocationApprovalMemory
    permission_decision_approve_for_location_approval_read: PermissionDecisionApproveForLocationApprovalRead
    permission_decision_approve_for_location_approval_write: PermissionDecisionApproveForLocationApprovalWrite
    permission_decision_approve_for_session: PermissionDecisionApproveForSession
    permission_decision_approve_for_session_approval: PermissionDecisionApproveForSessionApproval
    permission_decision_approve_for_session_approval_commands: PermissionDecisionApproveForSessionApprovalCommands
    permission_decision_approve_for_session_approval_custom_tool: PermissionDecisionApproveForSessionApprovalCustomTool
    permission_decision_approve_for_session_approval_extension_management: PermissionDecisionApproveForSessionApprovalExtensionManagement
    permission_decision_approve_for_session_approval_extension_permission_access: PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess
    permission_decision_approve_for_session_approval_mcp: PermissionDecisionApproveForSessionApprovalMCP
    permission_decision_approve_for_session_approval_mcp_sampling: PermissionDecisionApproveForSessionApprovalMCPSampling
    permission_decision_approve_for_session_approval_memory: PermissionDecisionApproveForSessionApprovalMemory
    permission_decision_approve_for_session_approval_read: PermissionDecisionApproveForSessionApprovalRead
    permission_decision_approve_for_session_approval_write: PermissionDecisionApproveForSessionApprovalWrite
    permission_decision_approve_once: PermissionDecisionApproveOnce
    permission_decision_approve_permanently: PermissionDecisionApprovePermanently
    permission_decision_cancelled: PermissionDecisionCancelled
    permission_decision_denied_by_content_exclusion_policy: PermissionDecisionDeniedByContentExclusionPolicy
    permission_decision_denied_by_permission_request_hook: PermissionDecisionDeniedByPermissionRequestHook
    permission_decision_denied_by_rules: PermissionDecisionDeniedByRules
    permission_decision_denied_interactively_by_user: PermissionDecisionDeniedInteractivelyByUser
    permission_decision_denied_no_approval_rule_and_could_not_request_from_user: PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser
    permission_decision_reject: PermissionDecisionReject
    permission_decision_request: PermissionDecisionRequest
    permission_decision_user_not_available: PermissionDecisionUserNotAvailable
    permission_location_add_tool_approval_params: PermissionLocationAddToolApprovalParams
    permission_location_apply_params: PermissionLocationApplyParams
    permission_location_apply_result: PermissionLocationApplyResult
    permission_location_resolve_params: PermissionLocationResolveParams
    permission_location_resolve_result: PermissionLocationResolveResult
    permission_location_type: PermissionLocationType
    permission_paths_add_params: PermissionPathsAddParams
    permission_paths_allowed_check_params: PermissionPathsAllowedCheckParams
    permission_paths_allowed_check_result: PermissionPathsAllowedCheckResult
    permission_paths_config: PermissionPathsConfig
    permission_paths_list: PermissionPathsList
    permission_paths_update_primary_params: PermissionPathsUpdatePrimaryParams
    permission_paths_workspace_check_params: PermissionPathsWorkspaceCheckParams
    permission_paths_workspace_check_result: PermissionPathsWorkspaceCheckResult
    permission_prompt_shown_notification: PermissionPromptShownNotification
    permission_request_result: PermissionRequestResult
    permission_rules_set: PermissionRulesSet
    permissions_configure_additional_content_exclusion_policy: PermissionsConfigureAdditionalContentExclusionPolicy
    permissions_configure_additional_content_exclusion_policy_rule: PermissionsConfigureAdditionalContentExclusionPolicyRule
    permissions_configure_additional_content_exclusion_policy_rule_source: PermissionsConfigureAdditionalContentExclusionPolicyRuleSource
    permissions_configure_additional_content_exclusion_policy_scope: PermissionsConfigureAdditionalContentExclusionPolicyScope
    permissions_configure_params: PermissionsConfigureParams
    permissions_configure_result: PermissionsConfigureResult
    permissions_folder_trust_add_trusted_result: PermissionsFolderTrustAddTrustedResult
    permissions_locations_add_tool_approval_details: PermissionsLocationsAddToolApprovalDetails
    permissions_locations_add_tool_approval_details_commands: PermissionsLocationsAddToolApprovalDetailsCommands
    permissions_locations_add_tool_approval_details_custom_tool: PermissionsLocationsAddToolApprovalDetailsCustomTool
    permissions_locations_add_tool_approval_details_extension_management: PermissionsLocationsAddToolApprovalDetailsExtensionManagement
    permissions_locations_add_tool_approval_details_extension_permission_access: PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess
    permissions_locations_add_tool_approval_details_mcp: PermissionsLocationsAddToolApprovalDetailsMCP
    permissions_locations_add_tool_approval_details_mcp_sampling: PermissionsLocationsAddToolApprovalDetailsMCPSampling
    permissions_locations_add_tool_approval_details_memory: PermissionsLocationsAddToolApprovalDetailsMemory
    permissions_locations_add_tool_approval_details_read: PermissionsLocationsAddToolApprovalDetailsRead
    permissions_locations_add_tool_approval_details_write: PermissionsLocationsAddToolApprovalDetailsWrite
    permissions_locations_add_tool_approval_result: PermissionsLocationsAddToolApprovalResult
    permissions_modify_rules_params: PermissionsModifyRulesParams
    permissions_modify_rules_result: PermissionsModifyRulesResult
    permissions_modify_rules_scope: PermissionsModifyRulesScope
    permissions_notify_prompt_shown_result: PermissionsNotifyPromptShownResult
    permissions_paths_add_result: PermissionsPathsAddResult
    permissions_paths_list_request: PermissionsPathsListRequest
    permissions_paths_update_primary_result: PermissionsPathsUpdatePrimaryResult
    permissions_pending_requests_request: PermissionsPendingRequestsRequest
    permissions_reset_session_approvals_request: PermissionsResetSessionApprovalsRequest
    permissions_reset_session_approvals_result: PermissionsResetSessionApprovalsResult
    permissions_set_approve_all_request: PermissionsSetApproveAllRequest
    permissions_set_approve_all_result: PermissionsSetApproveAllResult
    permissions_set_approve_all_source: PermissionsSetApproveAllSource
    permissions_set_required_request: PermissionsSetRequiredRequest
    permissions_set_required_result: PermissionsSetRequiredResult
    permissions_urls_set_unrestricted_mode_result: PermissionsUrlsSetUnrestrictedModeResult
    permission_urls_config: PermissionUrlsConfig
    permission_urls_set_unrestricted_mode_params: PermissionUrlsSetUnrestrictedModeParams
    ping_request: PingRequest
    ping_result: PingResult
    plan_read_result: PlanReadResult
    plan_update_request: PlanUpdateRequest
    plugin: Plugin
    plugin_list: PluginList
    queued_command_handled: QueuedCommandHandled
    queued_command_not_handled: QueuedCommandNotHandled
    queued_command_result: QueuedCommandResult
    queue_pending_items: QueuePendingItems
    queue_pending_items_kind: QueuePendingItemsKind
    queue_pending_items_result: QueuePendingItemsResult
    queue_remove_most_recent_result: QueueRemoveMostRecentResult
    register_event_interest_params: RegisterEventInterestParams
    register_event_interest_result: RegisterEventInterestResult
    release_event_interest_params: ReleaseEventInterestParams
    remote_enable_request: RemoteEnableRequest
    remote_enable_result: RemoteEnableResult
    remote_notify_steerable_changed_request: RemoteNotifySteerableChangedRequest
    remote_notify_steerable_changed_result: RemoteNotifySteerableChangedResult
    remote_session_connection_result: RemoteSessionConnectionResult
    remote_session_mode: RemoteSessionMode
    schedule_entry: ScheduleEntry
    schedule_list: ScheduleList
    schedule_stop_request: ScheduleStopRequest
    schedule_stop_result: ScheduleStopResult
    secrets_add_filter_values_request: SecretsAddFilterValuesRequest
    secrets_add_filter_values_result: SecretsAddFilterValuesResult
    send_agent_mode: SendAgentMode
    send_attachment: SendAttachment
    send_attachment_blob: SendAttachmentBlob
    send_attachment_directory: SendAttachmentDirectory
    send_attachment_file: SendAttachmentFile
    send_attachment_file_line_range: SendAttachmentFileLineRange
    send_attachment_github_reference: SendAttachmentGithubReference
    send_attachment_github_reference_type: SendAttachmentGithubReferenceTypeEnum
    send_attachment_selection: SendAttachmentSelection
    send_attachment_selection_details: SendAttachmentSelectionDetails
    send_attachment_selection_details_end: SendAttachmentSelectionDetailsEnd
    send_attachment_selection_details_start: SendAttachmentSelectionDetailsStart
    send_mode: SendMode
    send_request: SendRequest
    send_result: SendResult
    server_skill: ServerSkill
    server_skill_list: ServerSkillList
    session_auth_status: SessionAuthStatus
    session_bulk_delete_result: SessionBulkDeleteResult
    session_context: SessionContext
    session_context_host_type: HostType
    session_enrich_metadata_result: SessionEnrichMetadataResult
    session_fs_append_file_request: SessionFSAppendFileRequest
    session_fs_error: SessionFSError
    session_fs_error_code: SessionFSErrorCode
    session_fs_exists_request: SessionFSExistsRequest
    session_fs_exists_result: SessionFSExistsResult
    session_fs_mkdir_request: SessionFSMkdirRequest
    session_fs_readdir_request: SessionFSReaddirRequest
    session_fs_readdir_result: SessionFSReaddirResult
    session_fs_readdir_with_types_entry: SessionFSReaddirWithTypesEntry
    session_fs_readdir_with_types_entry_type: SessionFSReaddirWithTypesEntryType
    session_fs_readdir_with_types_request: SessionFSReaddirWithTypesRequest
    session_fs_readdir_with_types_result: SessionFSReaddirWithTypesResult
    session_fs_read_file_request: SessionFSReadFileRequest
    session_fs_read_file_result: SessionFSReadFileResult
    session_fs_rename_request: SessionFSRenameRequest
    session_fs_rm_request: SessionFSRmRequest
    session_fs_set_provider_capabilities: SessionFSSetProviderCapabilities
    session_fs_set_provider_conventions: SessionFSSetProviderConventions
    session_fs_set_provider_request: SessionFSSetProviderRequest
    session_fs_set_provider_result: SessionFSSetProviderResult
    session_fs_sqlite_exists_request: SessionFSSqliteExistsRequest
    session_fs_sqlite_exists_result: SessionFSSqliteExistsResult
    session_fs_sqlite_query_request: SessionFSSqliteQueryRequest
    session_fs_sqlite_query_result: SessionFSSqliteQueryResult
    session_fs_sqlite_query_type: SessionFSSqliteQueryType
    session_fs_stat_request: SessionFSStatRequest
    session_fs_stat_result: SessionFSStatResult
    session_fs_write_file_request: SessionFSWriteFileRequest
    session_installed_plugin: SessionInstalledPlugin
    session_installed_plugin_source: SessionInstalledPluginSource | str
    session_installed_plugin_source_github: SessionInstalledPluginSourceGithub
    session_installed_plugin_source_local: SessionInstalledPluginSourceLocal
    session_installed_plugin_source_url: SessionInstalledPluginSourceURL
    session_list: SessionList
    session_list_filter: SessionListFilter
    session_load_deferred_repo_hooks_result: SessionLoadDeferredRepoHooksResult
    session_log_level: SessionLogLevel
    session_metadata: SessionMetadata
    session_metadata_snapshot: SessionMetadataSnapshot
    session_mode: SessionMode
    session_prune_result: SessionPruneResult
    sessions_bulk_delete_request: SessionsBulkDeleteRequest
    sessions_check_in_use_request: SessionsCheckInUseRequest
    sessions_check_in_use_result: SessionsCheckInUseResult
    sessions_close_request: SessionsCloseRequest
    sessions_close_result: SessionsCloseResult
    sessions_enrich_metadata_request: SessionsEnrichMetadataRequest
    session_set_credentials_params: SessionSetCredentialsParams
    session_set_credentials_result: SessionSetCredentialsResult
    sessions_find_by_prefix_request: SessionsFindByPrefixRequest
    sessions_find_by_prefix_result: SessionsFindByPrefixResult
    sessions_find_by_task_id_request: SessionsFindByTaskIDRequest
    sessions_find_by_task_id_result: SessionsFindByTaskIDResult
    sessions_fork_request: SessionsForkRequest
    sessions_fork_result: SessionsForkResult
    sessions_get_event_file_path_request: SessionsGetEventFilePathRequest
    sessions_get_event_file_path_result: SessionsGetEventFilePathResult
    sessions_get_last_for_context_request: SessionsGetLastForContextRequest
    sessions_get_last_for_context_result: SessionsGetLastForContextResult
    sessions_get_persisted_remote_steerable_request: SessionsGetPersistedRemoteSteerableRequest
    sessions_get_persisted_remote_steerable_result: SessionsGetPersistedRemoteSteerableResult
    session_sizes: SessionSizes
    sessions_list_request: SessionsListRequest
    sessions_load_deferred_repo_hooks_request: SessionsLoadDeferredRepoHooksRequest
    sessions_prune_old_request: SessionsPruneOldRequest
    sessions_release_lock_request: SessionsReleaseLockRequest
    sessions_release_lock_result: SessionsReleaseLockResult
    sessions_reload_plugin_hooks_request: SessionsReloadPluginHooksRequest
    sessions_reload_plugin_hooks_result: SessionsReloadPluginHooksResult
    sessions_save_request: SessionsSaveRequest
    sessions_save_result: SessionsSaveResult
    sessions_set_additional_plugins_request: SessionsSetAdditionalPluginsRequest
    sessions_set_additional_plugins_result: SessionsSetAdditionalPluginsResult
    session_update_options_params: SessionUpdateOptionsParams
    session_update_options_result: SessionUpdateOptionsResult
    session_working_directory_context: SessionWorkingDirectoryContext
    session_working_directory_context_host_type: HostType
    shell_exec_request: ShellExecRequest
    shell_exec_result: ShellExecResult
    shell_kill_request: ShellKillRequest
    shell_kill_result: ShellKillResult
    shell_kill_signal: ShellKillSignal
    shutdown_request: ShutdownRequest
    skill: Skill
    skill_list: SkillList
    skills_config_set_disabled_skills_request: SkillsConfigSetDisabledSkillsRequest
    skills_disable_request: SkillsDisableRequest
    skills_discover_request: SkillsDiscoverRequest
    skills_enable_request: SkillsEnableRequest
    skills_get_invoked_result: SkillsGetInvokedResult
    skills_invoked_skill: SkillsInvokedSkill
    skills_load_diagnostics: SkillsLoadDiagnostics
    slash_command_agent_prompt_result: SlashCommandAgentPromptResult
    slash_command_completed_result: SlashCommandCompletedResult
    slash_command_info: SlashCommandInfo
    slash_command_input: SlashCommandInput
    slash_command_input_completion: SlashCommandInputCompletion
    slash_command_invocation_result: SlashCommandInvocationResult
    slash_command_kind: SlashCommandKind
    slash_command_select_subcommand_option: SlashCommandSelectSubcommandOption
    slash_command_select_subcommand_result: SlashCommandSelectSubcommandResult
    slash_command_text_result: SlashCommandTextResult
    task_agent_info: TaskAgentInfo
    task_agent_progress: TaskAgentProgress
    task_execution_mode: TaskExecutionMode
    task_info: TaskInfo
    task_list: TaskList
    task_progress_line: TaskProgressLine
    tasks_cancel_request: TasksCancelRequest
    tasks_cancel_result: TasksCancelResult
    tasks_get_current_promotable_result: TasksGetCurrentPromotableResult
    tasks_get_progress_request: TasksGetProgressRequest
    tasks_get_progress_result: TasksGetProgressResult
    task_shell_info: TaskShellInfo
    task_shell_info_attachment_mode: TaskShellInfoAttachmentMode
    task_shell_progress: TaskShellProgress
    tasks_promote_current_to_background_result: TasksPromoteCurrentToBackgroundResult
    tasks_promote_to_background_request: TasksPromoteToBackgroundRequest
    tasks_promote_to_background_result: TasksPromoteToBackgroundResult
    tasks_refresh_result: TasksRefreshResult
    tasks_remove_request: TasksRemoveRequest
    tasks_remove_result: TasksRemoveResult
    tasks_send_message_request: TasksSendMessageRequest
    tasks_send_message_result: TasksSendMessageResult
    tasks_start_agent_request: TasksStartAgentRequest
    tasks_start_agent_result: TasksStartAgentResult
    task_status: TaskStatus
    tasks_wait_for_pending_result: TasksWaitForPendingResult
    telemetry_set_feature_overrides_request: TelemetrySetFeatureOverridesRequest
    token_auth_info: TokenAuthInfo
    tool: Tool
    tool_list: ToolList
    tools_initialize_and_validate_result: ToolsInitializeAndValidateResult
    tools_list_request: ToolsListRequest
    ui_auto_mode_switch_response: UIAutoModeSwitchResponse
    ui_elicitation_array_any_of_field: UIElicitationArrayAnyOfField
    ui_elicitation_array_any_of_field_items: UIElicitationArrayAnyOfFieldItems
    ui_elicitation_array_any_of_field_items_any_of: UIElicitationArrayAnyOfFieldItemsAnyOf
    ui_elicitation_array_enum_field: UIElicitationArrayEnumField
    ui_elicitation_array_enum_field_items: UIElicitationArrayEnumFieldItems
    ui_elicitation_field_value: float | bool | list[str] | str
    ui_elicitation_request: UIElicitationRequest
    ui_elicitation_response: UIElicitationResponse
    ui_elicitation_response_action: UIElicitationResponseAction
    ui_elicitation_response_content: dict[str, float | bool | list[str] | str]
    ui_elicitation_result: UIElicitationResult
    ui_elicitation_schema: UIElicitationSchema
    ui_elicitation_schema_property: UIElicitationSchemaProperty
    ui_elicitation_schema_property_boolean: UIElicitationSchemaPropertyBoolean
    ui_elicitation_schema_property_number: UIElicitationSchemaPropertyNumber
    ui_elicitation_schema_property_number_type: UIElicitationSchemaPropertyNumberType
    ui_elicitation_schema_property_string: UIElicitationSchemaPropertyString
    ui_elicitation_schema_property_string_format: UIElicitationSchemaPropertyStringFormat
    ui_elicitation_string_enum_field: UIElicitationStringEnumField
    ui_elicitation_string_one_of_field: UIElicitationStringOneOfField
    ui_elicitation_string_one_of_field_one_of: UIElicitationStringOneOfFieldOneOf
    ui_exit_plan_mode_action: UIExitPlanModeAction
    ui_exit_plan_mode_response: UIExitPlanModeResponse
    ui_handle_pending_auto_mode_switch_request: UIHandlePendingAutoModeSwitchRequest
    ui_handle_pending_elicitation_request: UIHandlePendingElicitationRequest
    ui_handle_pending_exit_plan_mode_request: UIHandlePendingExitPlanModeRequest
    ui_handle_pending_result: UIHandlePendingResult
    ui_handle_pending_sampling_request: UIHandlePendingSamplingRequest
    ui_handle_pending_sampling_response: dict[str, Any]
    ui_handle_pending_user_input_request: UIHandlePendingUserInputRequest
    ui_register_direct_auto_mode_switch_handler_result: UIRegisterDirectAutoModeSwitchHandlerResult
    ui_unregister_direct_auto_mode_switch_handler_request: UIUnregisterDirectAutoModeSwitchHandlerRequest
    ui_unregister_direct_auto_mode_switch_handler_result: UIUnregisterDirectAutoModeSwitchHandlerResult
    ui_user_input_response: UIUserInputResponse
    usage_get_metrics_result: UsageGetMetricsResult
    usage_metrics_code_changes: UsageMetricsCodeChanges
    usage_metrics_model_metric: UsageMetricsModelMetric
    usage_metrics_model_metric_requests: UsageMetricsModelMetricRequests
    usage_metrics_model_metric_token_detail: UsageMetricsModelMetricTokenDetail
    usage_metrics_model_metric_usage: UsageMetricsModelMetricUsage
    usage_metrics_token_detail: UsageMetricsTokenDetail
    user_auth_info: UserAuthInfo
    workspaces_checkpoints: WorkspacesCheckpoints
    workspaces_create_file_request: WorkspacesCreateFileRequest
    workspaces_get_workspace_result: WorkspacesGetWorkspaceResult
    workspaces_list_checkpoints_result: WorkspacesListCheckpointsResult
    workspaces_list_files_result: WorkspacesListFilesResult
    workspaces_read_checkpoint_request: WorkspacesReadCheckpointRequest
    workspaces_read_checkpoint_result: WorkspacesReadCheckpointResult
    workspaces_read_file_request: WorkspacesReadFileRequest
    workspaces_read_file_result: WorkspacesReadFileResult
    workspaces_save_large_paste_request: WorkspacesSaveLargePasteRequest
    workspaces_save_large_paste_result: WorkspacesSaveLargePasteResult
    workspace_summary_host_type: HostType
    workspaces_workspace_details_host_type: HostType
    session_context_info: SessionContextInfo | None = None
    task_progress: TaskProgress | None = None
    workspace_summary: WorkspaceSummary | None = None

    @staticmethod
    def from_dict(obj: Any) -> RPC:
        assert isinstance(obj, dict)
        abort_request = AbortRequest.from_dict(obj.get("AbortRequest"))
        abort_result = AbortResult.from_dict(obj.get("AbortResult"))
        account_get_quota_request = AccountGetQuotaRequest.from_dict(obj.get("AccountGetQuotaRequest"))
        account_get_quota_result = AccountGetQuotaResult.from_dict(obj.get("AccountGetQuotaResult"))
        account_quota_snapshot = AccountQuotaSnapshot.from_dict(obj.get("AccountQuotaSnapshot"))
        agent_get_current_result = AgentGetCurrentResult.from_dict(obj.get("AgentGetCurrentResult"))
        agent_info = AgentInfo.from_dict(obj.get("AgentInfo"))
        agent_info_source = AgentInfoSource(obj.get("AgentInfoSource"))
        agent_list = AgentList.from_dict(obj.get("AgentList"))
        agent_reload_result = AgentReloadResult.from_dict(obj.get("AgentReloadResult"))
        agent_select_request = AgentSelectRequest.from_dict(obj.get("AgentSelectRequest"))
        agent_select_result = AgentSelectResult.from_dict(obj.get("AgentSelectResult"))
        api_key_auth_info = APIKeyAuthInfo.from_dict(obj.get("ApiKeyAuthInfo"))
        auth_info = _load_AuthInfo(obj.get("AuthInfo"))
        auth_info_type = AuthInfoType(obj.get("AuthInfoType"))
        command_list = CommandList.from_dict(obj.get("CommandList"))
        commands_handle_pending_command_request = CommandsHandlePendingCommandRequest.from_dict(obj.get("CommandsHandlePendingCommandRequest"))
        commands_handle_pending_command_result = CommandsHandlePendingCommandResult.from_dict(obj.get("CommandsHandlePendingCommandResult"))
        commands_invoke_request = CommandsInvokeRequest.from_dict(obj.get("CommandsInvokeRequest"))
        commands_list_request = CommandsListRequest.from_dict(obj.get("CommandsListRequest"))
        commands_respond_to_queued_command_request = CommandsRespondToQueuedCommandRequest.from_dict(obj.get("CommandsRespondToQueuedCommandRequest"))
        commands_respond_to_queued_command_result = CommandsRespondToQueuedCommandResult.from_dict(obj.get("CommandsRespondToQueuedCommandResult"))
        connected_remote_session_metadata = ConnectedRemoteSessionMetadata.from_dict(obj.get("ConnectedRemoteSessionMetadata"))
        connected_remote_session_metadata_kind = ConnectedRemoteSessionMetadataKind(obj.get("ConnectedRemoteSessionMetadataKind"))
        connected_remote_session_metadata_repository = ConnectedRemoteSessionMetadataRepository.from_dict(obj.get("ConnectedRemoteSessionMetadataRepository"))
        connect_remote_session_params = ConnectRemoteSessionParams.from_dict(obj.get("ConnectRemoteSessionParams"))
        connect_request = ConnectRequest.from_dict(obj.get("ConnectRequest"))
        connect_result = ConnectResult.from_dict(obj.get("ConnectResult"))
        content_filter_mode = ContentFilterMode(obj.get("ContentFilterMode"))
        copilot_api_token_auth_info = CopilotAPITokenAuthInfo.from_dict(obj.get("CopilotApiTokenAuthInfo"))
        copilot_user_response = CopilotUserResponse.from_dict(obj.get("CopilotUserResponse"))
        copilot_user_response_endpoints = CopilotUserResponseEndpoints.from_dict(obj.get("CopilotUserResponseEndpoints"))
        copilot_user_response_quota_snapshots = from_dict(lambda x: from_union([CopilotUserResponseQuotaSnapshots.from_dict, from_none], x), obj.get("CopilotUserResponseQuotaSnapshots"))
        copilot_user_response_quota_snapshots_chat = CopilotUserResponseQuotaSnapshotsChat.from_dict(obj.get("CopilotUserResponseQuotaSnapshotsChat"))
        copilot_user_response_quota_snapshots_completions = CopilotUserResponseQuotaSnapshotsCompletions.from_dict(obj.get("CopilotUserResponseQuotaSnapshotsCompletions"))
        copilot_user_response_quota_snapshots_premium_interactions = CopilotUserResponseQuotaSnapshotsPremiumInteractions.from_dict(obj.get("CopilotUserResponseQuotaSnapshotsPremiumInteractions"))
        current_model = CurrentModel.from_dict(obj.get("CurrentModel"))
        discovered_mcp_server = DiscoveredMCPServer.from_dict(obj.get("DiscoveredMcpServer"))
        discovered_mcp_server_type = DiscoveredMCPServerType(obj.get("DiscoveredMcpServerType"))
        enqueue_command_params = EnqueueCommandParams.from_dict(obj.get("EnqueueCommandParams"))
        enqueue_command_result = EnqueueCommandResult.from_dict(obj.get("EnqueueCommandResult"))
        env_auth_info = EnvAuthInfo.from_dict(obj.get("EnvAuthInfo"))
        event_log_read_request = EventLogReadRequest.from_dict(obj.get("EventLogReadRequest"))
        event_log_release_interest_result = EventLogReleaseInterestResult.from_dict(obj.get("EventLogReleaseInterestResult"))
        event_log_tail_result = EventLogTailResult.from_dict(obj.get("EventLogTailResult"))
        event_log_types = from_union([lambda x: from_list(from_str, x), EventLogTypes], obj.get("EventLogTypes"))
        events_agent_scope = EventsAgentScope(obj.get("EventsAgentScope"))
        events_cursor_status = EventsCursorStatus(obj.get("EventsCursorStatus"))
        events_read_result = EventsReadResult.from_dict(obj.get("EventsReadResult"))
        execute_command_params = ExecuteCommandParams.from_dict(obj.get("ExecuteCommandParams"))
        execute_command_result = ExecuteCommandResult.from_dict(obj.get("ExecuteCommandResult"))
        extension = Extension.from_dict(obj.get("Extension"))
        extension_list = ExtensionList.from_dict(obj.get("ExtensionList"))
        extensions_disable_request = ExtensionsDisableRequest.from_dict(obj.get("ExtensionsDisableRequest"))
        extensions_enable_request = ExtensionsEnableRequest.from_dict(obj.get("ExtensionsEnableRequest"))
        extension_source = ExtensionSource(obj.get("ExtensionSource"))
        extension_status = ExtensionStatus(obj.get("ExtensionStatus"))
        external_tool_result = from_union([ExternalToolTextResultForLlm.from_dict, from_str], obj.get("ExternalToolResult"))
        external_tool_text_result_for_llm = ExternalToolTextResultForLlm.from_dict(obj.get("ExternalToolTextResultForLlm"))
        external_tool_text_result_for_llm_binary_results_for_llm = ExternalToolTextResultForLlmBinaryResultsForLlm.from_dict(obj.get("ExternalToolTextResultForLlmBinaryResultsForLlm"))
        external_tool_text_result_for_llm_binary_results_for_llm_type = ExternalToolTextResultForLlmBinaryResultsForLlmType(obj.get("ExternalToolTextResultForLlmBinaryResultsForLlmType"))
        external_tool_text_result_for_llm_content = _load_ExternalToolTextResultForLlmContent(obj.get("ExternalToolTextResultForLlmContent"))
        external_tool_text_result_for_llm_content_audio = ExternalToolTextResultForLlmContentAudio.from_dict(obj.get("ExternalToolTextResultForLlmContentAudio"))
        external_tool_text_result_for_llm_content_image = ExternalToolTextResultForLlmContentImage.from_dict(obj.get("ExternalToolTextResultForLlmContentImage"))
        external_tool_text_result_for_llm_content_resource = ExternalToolTextResultForLlmContentResource.from_dict(obj.get("ExternalToolTextResultForLlmContentResource"))
        external_tool_text_result_for_llm_content_resource_details = (lambda x: from_union([EmbeddedTextResourceContents.from_dict, EmbeddedBlobResourceContents.from_dict], x))(obj.get("ExternalToolTextResultForLlmContentResourceDetails"))
        external_tool_text_result_for_llm_content_resource_link = ExternalToolTextResultForLlmContentResourceLink.from_dict(obj.get("ExternalToolTextResultForLlmContentResourceLink"))
        external_tool_text_result_for_llm_content_resource_link_icon = ExternalToolTextResultForLlmContentResourceLinkIcon.from_dict(obj.get("ExternalToolTextResultForLlmContentResourceLinkIcon"))
        external_tool_text_result_for_llm_content_resource_link_icon_theme = ExternalToolTextResultForLlmContentResourceLinkIconTheme(obj.get("ExternalToolTextResultForLlmContentResourceLinkIconTheme"))
        external_tool_text_result_for_llm_content_terminal = ExternalToolTextResultForLlmContentTerminal.from_dict(obj.get("ExternalToolTextResultForLlmContentTerminal"))
        external_tool_text_result_for_llm_content_text = ExternalToolTextResultForLlmContentText.from_dict(obj.get("ExternalToolTextResultForLlmContentText"))
        filter_mapping = from_union([lambda x: from_dict(ContentFilterMode, x), ContentFilterMode], obj.get("FilterMapping"))
        fleet_start_request = FleetStartRequest.from_dict(obj.get("FleetStartRequest"))
        fleet_start_result = FleetStartResult.from_dict(obj.get("FleetStartResult"))
        folder_trust_add_params = FolderTrustAddParams.from_dict(obj.get("FolderTrustAddParams"))
        folder_trust_check_params = FolderTrustCheckParams.from_dict(obj.get("FolderTrustCheckParams"))
        folder_trust_check_result = FolderTrustCheckResult.from_dict(obj.get("FolderTrustCheckResult"))
        gh_cli_auth_info = GhCLIAuthInfo.from_dict(obj.get("GhCliAuthInfo"))
        handle_pending_tool_call_request = HandlePendingToolCallRequest.from_dict(obj.get("HandlePendingToolCallRequest"))
        handle_pending_tool_call_result = HandlePendingToolCallResult.from_dict(obj.get("HandlePendingToolCallResult"))
        history_abort_manual_compaction_result = HistoryAbortManualCompactionResult.from_dict(obj.get("HistoryAbortManualCompactionResult"))
        history_cancel_background_compaction_result = HistoryCancelBackgroundCompactionResult.from_dict(obj.get("HistoryCancelBackgroundCompactionResult"))
        history_compact_context_window = HistoryCompactContextWindow.from_dict(obj.get("HistoryCompactContextWindow"))
        history_compact_request = HistoryCompactRequest.from_dict(obj.get("HistoryCompactRequest"))
        history_compact_result = HistoryCompactResult.from_dict(obj.get("HistoryCompactResult"))
        history_summarize_for_handoff_result = HistorySummarizeForHandoffResult.from_dict(obj.get("HistorySummarizeForHandoffResult"))
        history_truncate_request = HistoryTruncateRequest.from_dict(obj.get("HistoryTruncateRequest"))
        history_truncate_result = HistoryTruncateResult.from_dict(obj.get("HistoryTruncateResult"))
        hmac_auth_info = HMACAuthInfo.from_dict(obj.get("HMACAuthInfo"))
        installed_plugin = InstalledPlugin.from_dict(obj.get("InstalledPlugin"))
        installed_plugin_source = from_union([InstalledPluginSource.from_dict, from_str], obj.get("InstalledPluginSource"))
        installed_plugin_source_github = InstalledPluginSourceGithub.from_dict(obj.get("InstalledPluginSourceGithub"))
        installed_plugin_source_local = InstalledPluginSourceLocal.from_dict(obj.get("InstalledPluginSourceLocal"))
        installed_plugin_source_url = InstalledPluginSourceURL.from_dict(obj.get("InstalledPluginSourceUrl"))
        instructions_get_sources_result = InstructionsGetSourcesResult.from_dict(obj.get("InstructionsGetSourcesResult"))
        instructions_sources = InstructionsSources.from_dict(obj.get("InstructionsSources"))
        instructions_sources_location = InstructionsSourcesLocation(obj.get("InstructionsSourcesLocation"))
        instructions_sources_type = InstructionsSourcesType(obj.get("InstructionsSourcesType"))
        log_request = LogRequest.from_dict(obj.get("LogRequest"))
        log_result = LogResult.from_dict(obj.get("LogResult"))
        lsp_initialize_request = LspInitializeRequest.from_dict(obj.get("LspInitializeRequest"))
        mcp_cancel_sampling_execution_params = MCPCancelSamplingExecutionParams.from_dict(obj.get("McpCancelSamplingExecutionParams"))
        mcp_cancel_sampling_execution_result = MCPCancelSamplingExecutionResult.from_dict(obj.get("McpCancelSamplingExecutionResult"))
        mcp_config_add_request = MCPConfigAddRequest.from_dict(obj.get("McpConfigAddRequest"))
        mcp_config_disable_request = MCPConfigDisableRequest.from_dict(obj.get("McpConfigDisableRequest"))
        mcp_config_enable_request = MCPConfigEnableRequest.from_dict(obj.get("McpConfigEnableRequest"))
        mcp_config_list = MCPConfigList.from_dict(obj.get("McpConfigList"))
        mcp_config_remove_request = MCPConfigRemoveRequest.from_dict(obj.get("McpConfigRemoveRequest"))
        mcp_config_update_request = MCPConfigUpdateRequest.from_dict(obj.get("McpConfigUpdateRequest"))
        mcp_disable_request = MCPDisableRequest.from_dict(obj.get("McpDisableRequest"))
        mcp_discover_request = MCPDiscoverRequest.from_dict(obj.get("McpDiscoverRequest"))
        mcp_discover_result = MCPDiscoverResult.from_dict(obj.get("McpDiscoverResult"))
        mcp_enable_request = MCPEnableRequest.from_dict(obj.get("McpEnableRequest"))
        mcp_execute_sampling_params = MCPExecuteSamplingParams.from_dict(obj.get("McpExecuteSamplingParams"))
        mcp_execute_sampling_request = from_dict(lambda x: x, obj.get("McpExecuteSamplingRequest"))
        mcp_execute_sampling_result = from_dict(lambda x: x, obj.get("McpExecuteSamplingResult"))
        mcp_oauth_login_request = MCPOauthLoginRequest.from_dict(obj.get("McpOauthLoginRequest"))
        mcp_oauth_login_result = MCPOauthLoginResult.from_dict(obj.get("McpOauthLoginResult"))
        mcp_remove_git_hub_result = MCPRemoveGitHubResult.from_dict(obj.get("McpRemoveGitHubResult"))
        mcp_sampling_execution_action = MCPSamplingExecutionAction(obj.get("McpSamplingExecutionAction"))
        mcp_sampling_execution_result = MCPSamplingExecutionResult.from_dict(obj.get("McpSamplingExecutionResult"))
        mcp_server = MCPServer.from_dict(obj.get("McpServer"))
        mcp_server_config = MCPServerConfig.from_dict(obj.get("McpServerConfig"))
        mcp_server_config_http = MCPServerConfigHTTP.from_dict(obj.get("McpServerConfigHttp"))
        mcp_server_config_http_auth = MCPServerConfigHTTPAuth.from_dict(obj.get("McpServerConfigHttpAuth"))
        mcp_server_config_http_oauth_grant_type = MCPServerConfigHTTPOauthGrantType(obj.get("McpServerConfigHttpOauthGrantType"))
        mcp_server_config_http_type = MCPServerConfigHTTPType(obj.get("McpServerConfigHttpType"))
        mcp_server_config_stdio = MCPServerConfigStdio.from_dict(obj.get("McpServerConfigStdio"))
        mcp_server_list = MCPServerList.from_dict(obj.get("McpServerList"))
        mcp_set_env_value_mode_details = MCPSetEnvValueModeDetails(obj.get("McpSetEnvValueModeDetails"))
        mcp_set_env_value_mode_params = MCPSetEnvValueModeParams.from_dict(obj.get("McpSetEnvValueModeParams"))
        mcp_set_env_value_mode_result = MCPSetEnvValueModeResult.from_dict(obj.get("McpSetEnvValueModeResult"))
        metadata_context_info_request = MetadataContextInfoRequest.from_dict(obj.get("MetadataContextInfoRequest"))
        metadata_context_info_result = MetadataContextInfoResult.from_dict(obj.get("MetadataContextInfoResult"))
        metadata_is_processing_result = MetadataIsProcessingResult.from_dict(obj.get("MetadataIsProcessingResult"))
        metadata_recompute_context_tokens_request = MetadataRecomputeContextTokensRequest.from_dict(obj.get("MetadataRecomputeContextTokensRequest"))
        metadata_recompute_context_tokens_result = MetadataRecomputeContextTokensResult.from_dict(obj.get("MetadataRecomputeContextTokensResult"))
        metadata_record_context_change_request = MetadataRecordContextChangeRequest.from_dict(obj.get("MetadataRecordContextChangeRequest"))
        metadata_record_context_change_result = MetadataRecordContextChangeResult.from_dict(obj.get("MetadataRecordContextChangeResult"))
        metadata_set_working_directory_request = MetadataSetWorkingDirectoryRequest.from_dict(obj.get("MetadataSetWorkingDirectoryRequest"))
        metadata_set_working_directory_result = MetadataSetWorkingDirectoryResult.from_dict(obj.get("MetadataSetWorkingDirectoryResult"))
        metadata_snapshot_current_mode = MetadataSnapshotCurrentMode(obj.get("MetadataSnapshotCurrentMode"))
        metadata_snapshot_remote_metadata = MetadataSnapshotRemoteMetadata.from_dict(obj.get("MetadataSnapshotRemoteMetadata"))
        metadata_snapshot_remote_metadata_repository = MetadataSnapshotRemoteMetadataRepository.from_dict(obj.get("MetadataSnapshotRemoteMetadataRepository"))
        metadata_snapshot_remote_metadata_task_type = MetadataSnapshotRemoteMetadataTaskType(obj.get("MetadataSnapshotRemoteMetadataTaskType"))
        model = Model.from_dict(obj.get("Model"))
        model_billing = ModelBilling.from_dict(obj.get("ModelBilling"))
        model_billing_token_prices = ModelBillingTokenPrices.from_dict(obj.get("ModelBillingTokenPrices"))
        model_capabilities = ModelCapabilities.from_dict(obj.get("ModelCapabilities"))
        model_capabilities_limits = ModelCapabilitiesLimits.from_dict(obj.get("ModelCapabilitiesLimits"))
        model_capabilities_limits_vision = ModelCapabilitiesLimitsVision.from_dict(obj.get("ModelCapabilitiesLimitsVision"))
        model_capabilities_override = ModelCapabilitiesOverride.from_dict(obj.get("ModelCapabilitiesOverride"))
        model_capabilities_override_limits = ModelCapabilitiesOverrideLimits.from_dict(obj.get("ModelCapabilitiesOverrideLimits"))
        model_capabilities_override_limits_vision = ModelCapabilitiesOverrideLimitsVision.from_dict(obj.get("ModelCapabilitiesOverrideLimitsVision"))
        model_capabilities_override_supports = ModelCapabilitiesOverrideSupports.from_dict(obj.get("ModelCapabilitiesOverrideSupports"))
        model_capabilities_supports = ModelCapabilitiesSupports.from_dict(obj.get("ModelCapabilitiesSupports"))
        model_list = ModelList.from_dict(obj.get("ModelList"))
        model_picker_category = ModelPickerCategory(obj.get("ModelPickerCategory"))
        model_picker_price_category = ModelPickerPriceCategory(obj.get("ModelPickerPriceCategory"))
        model_policy = ModelPolicy.from_dict(obj.get("ModelPolicy"))
        model_policy_state = ModelPolicyState(obj.get("ModelPolicyState"))
        model_set_reasoning_effort_request = ModelSetReasoningEffortRequest.from_dict(obj.get("ModelSetReasoningEffortRequest"))
        model_set_reasoning_effort_result = ModelSetReasoningEffortResult.from_dict(obj.get("ModelSetReasoningEffortResult"))
        models_list_request = ModelsListRequest.from_dict(obj.get("ModelsListRequest"))
        model_switch_to_request = ModelSwitchToRequest.from_dict(obj.get("ModelSwitchToRequest"))
        model_switch_to_result = ModelSwitchToResult.from_dict(obj.get("ModelSwitchToResult"))
        mode_set_request = ModeSetRequest.from_dict(obj.get("ModeSetRequest"))
        name_get_result = NameGetResult.from_dict(obj.get("NameGetResult"))
        name_set_auto_request = NameSetAutoRequest.from_dict(obj.get("NameSetAutoRequest"))
        name_set_auto_result = NameSetAutoResult.from_dict(obj.get("NameSetAutoResult"))
        name_set_request = NameSetRequest.from_dict(obj.get("NameSetRequest"))
        options_update_env_value_mode = MCPSetEnvValueModeDetails(obj.get("OptionsUpdateEnvValueMode"))
        pending_permission_request = PendingPermissionRequest.from_dict(obj.get("PendingPermissionRequest"))
        pending_permission_request_list = PendingPermissionRequestList.from_dict(obj.get("PendingPermissionRequestList"))
        permission_decision = _load_PermissionDecision(obj.get("PermissionDecision"))
        permission_decision_approved = PermissionDecisionApproved.from_dict(obj.get("PermissionDecisionApproved"))
        permission_decision_approved_for_location = PermissionDecisionApprovedForLocation.from_dict(obj.get("PermissionDecisionApprovedForLocation"))
        permission_decision_approved_for_session = PermissionDecisionApprovedForSession.from_dict(obj.get("PermissionDecisionApprovedForSession"))
        permission_decision_approve_for_location = PermissionDecisionApproveForLocation.from_dict(obj.get("PermissionDecisionApproveForLocation"))
        permission_decision_approve_for_location_approval = _load_PermissionDecisionApproveForLocationApproval(obj.get("PermissionDecisionApproveForLocationApproval"))
        permission_decision_approve_for_location_approval_commands = PermissionDecisionApproveForLocationApprovalCommands.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalCommands"))
        permission_decision_approve_for_location_approval_custom_tool = PermissionDecisionApproveForLocationApprovalCustomTool.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalCustomTool"))
        permission_decision_approve_for_location_approval_extension_management = PermissionDecisionApproveForLocationApprovalExtensionManagement.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalExtensionManagement"))
        permission_decision_approve_for_location_approval_extension_permission_access = PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess"))
        permission_decision_approve_for_location_approval_mcp = PermissionDecisionApproveForLocationApprovalMCP.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalMcp"))
        permission_decision_approve_for_location_approval_mcp_sampling = PermissionDecisionApproveForLocationApprovalMCPSampling.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalMcpSampling"))
        permission_decision_approve_for_location_approval_memory = PermissionDecisionApproveForLocationApprovalMemory.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalMemory"))
        permission_decision_approve_for_location_approval_read = PermissionDecisionApproveForLocationApprovalRead.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalRead"))
        permission_decision_approve_for_location_approval_write = PermissionDecisionApproveForLocationApprovalWrite.from_dict(obj.get("PermissionDecisionApproveForLocationApprovalWrite"))
        permission_decision_approve_for_session = PermissionDecisionApproveForSession.from_dict(obj.get("PermissionDecisionApproveForSession"))
        permission_decision_approve_for_session_approval = _load_PermissionDecisionApproveForSessionApproval(obj.get("PermissionDecisionApproveForSessionApproval"))
        permission_decision_approve_for_session_approval_commands = PermissionDecisionApproveForSessionApprovalCommands.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalCommands"))
        permission_decision_approve_for_session_approval_custom_tool = PermissionDecisionApproveForSessionApprovalCustomTool.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalCustomTool"))
        permission_decision_approve_for_session_approval_extension_management = PermissionDecisionApproveForSessionApprovalExtensionManagement.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalExtensionManagement"))
        permission_decision_approve_for_session_approval_extension_permission_access = PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess"))
        permission_decision_approve_for_session_approval_mcp = PermissionDecisionApproveForSessionApprovalMCP.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalMcp"))
        permission_decision_approve_for_session_approval_mcp_sampling = PermissionDecisionApproveForSessionApprovalMCPSampling.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalMcpSampling"))
        permission_decision_approve_for_session_approval_memory = PermissionDecisionApproveForSessionApprovalMemory.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalMemory"))
        permission_decision_approve_for_session_approval_read = PermissionDecisionApproveForSessionApprovalRead.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalRead"))
        permission_decision_approve_for_session_approval_write = PermissionDecisionApproveForSessionApprovalWrite.from_dict(obj.get("PermissionDecisionApproveForSessionApprovalWrite"))
        permission_decision_approve_once = PermissionDecisionApproveOnce.from_dict(obj.get("PermissionDecisionApproveOnce"))
        permission_decision_approve_permanently = PermissionDecisionApprovePermanently.from_dict(obj.get("PermissionDecisionApprovePermanently"))
        permission_decision_cancelled = PermissionDecisionCancelled.from_dict(obj.get("PermissionDecisionCancelled"))
        permission_decision_denied_by_content_exclusion_policy = PermissionDecisionDeniedByContentExclusionPolicy.from_dict(obj.get("PermissionDecisionDeniedByContentExclusionPolicy"))
        permission_decision_denied_by_permission_request_hook = PermissionDecisionDeniedByPermissionRequestHook.from_dict(obj.get("PermissionDecisionDeniedByPermissionRequestHook"))
        permission_decision_denied_by_rules = PermissionDecisionDeniedByRules.from_dict(obj.get("PermissionDecisionDeniedByRules"))
        permission_decision_denied_interactively_by_user = PermissionDecisionDeniedInteractivelyByUser.from_dict(obj.get("PermissionDecisionDeniedInteractivelyByUser"))
        permission_decision_denied_no_approval_rule_and_could_not_request_from_user = PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser.from_dict(obj.get("PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser"))
        permission_decision_reject = PermissionDecisionReject.from_dict(obj.get("PermissionDecisionReject"))
        permission_decision_request = PermissionDecisionRequest.from_dict(obj.get("PermissionDecisionRequest"))
        permission_decision_user_not_available = PermissionDecisionUserNotAvailable.from_dict(obj.get("PermissionDecisionUserNotAvailable"))
        permission_location_add_tool_approval_params = PermissionLocationAddToolApprovalParams.from_dict(obj.get("PermissionLocationAddToolApprovalParams"))
        permission_location_apply_params = PermissionLocationApplyParams.from_dict(obj.get("PermissionLocationApplyParams"))
        permission_location_apply_result = PermissionLocationApplyResult.from_dict(obj.get("PermissionLocationApplyResult"))
        permission_location_resolve_params = PermissionLocationResolveParams.from_dict(obj.get("PermissionLocationResolveParams"))
        permission_location_resolve_result = PermissionLocationResolveResult.from_dict(obj.get("PermissionLocationResolveResult"))
        permission_location_type = PermissionLocationType(obj.get("PermissionLocationType"))
        permission_paths_add_params = PermissionPathsAddParams.from_dict(obj.get("PermissionPathsAddParams"))
        permission_paths_allowed_check_params = PermissionPathsAllowedCheckParams.from_dict(obj.get("PermissionPathsAllowedCheckParams"))
        permission_paths_allowed_check_result = PermissionPathsAllowedCheckResult.from_dict(obj.get("PermissionPathsAllowedCheckResult"))
        permission_paths_config = PermissionPathsConfig.from_dict(obj.get("PermissionPathsConfig"))
        permission_paths_list = PermissionPathsList.from_dict(obj.get("PermissionPathsList"))
        permission_paths_update_primary_params = PermissionPathsUpdatePrimaryParams.from_dict(obj.get("PermissionPathsUpdatePrimaryParams"))
        permission_paths_workspace_check_params = PermissionPathsWorkspaceCheckParams.from_dict(obj.get("PermissionPathsWorkspaceCheckParams"))
        permission_paths_workspace_check_result = PermissionPathsWorkspaceCheckResult.from_dict(obj.get("PermissionPathsWorkspaceCheckResult"))
        permission_prompt_shown_notification = PermissionPromptShownNotification.from_dict(obj.get("PermissionPromptShownNotification"))
        permission_request_result = PermissionRequestResult.from_dict(obj.get("PermissionRequestResult"))
        permission_rules_set = PermissionRulesSet.from_dict(obj.get("PermissionRulesSet"))
        permissions_configure_additional_content_exclusion_policy = PermissionsConfigureAdditionalContentExclusionPolicy.from_dict(obj.get("PermissionsConfigureAdditionalContentExclusionPolicy"))
        permissions_configure_additional_content_exclusion_policy_rule = PermissionsConfigureAdditionalContentExclusionPolicyRule.from_dict(obj.get("PermissionsConfigureAdditionalContentExclusionPolicyRule"))
        permissions_configure_additional_content_exclusion_policy_rule_source = PermissionsConfigureAdditionalContentExclusionPolicyRuleSource.from_dict(obj.get("PermissionsConfigureAdditionalContentExclusionPolicyRuleSource"))
        permissions_configure_additional_content_exclusion_policy_scope = PermissionsConfigureAdditionalContentExclusionPolicyScope(obj.get("PermissionsConfigureAdditionalContentExclusionPolicyScope"))
        permissions_configure_params = PermissionsConfigureParams.from_dict(obj.get("PermissionsConfigureParams"))
        permissions_configure_result = PermissionsConfigureResult.from_dict(obj.get("PermissionsConfigureResult"))
        permissions_folder_trust_add_trusted_result = PermissionsFolderTrustAddTrustedResult.from_dict(obj.get("PermissionsFolderTrustAddTrustedResult"))
        permissions_locations_add_tool_approval_details = _load_PermissionsLocationsAddToolApprovalDetails(obj.get("PermissionsLocationsAddToolApprovalDetails"))
        permissions_locations_add_tool_approval_details_commands = PermissionsLocationsAddToolApprovalDetailsCommands.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsCommands"))
        permissions_locations_add_tool_approval_details_custom_tool = PermissionsLocationsAddToolApprovalDetailsCustomTool.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsCustomTool"))
        permissions_locations_add_tool_approval_details_extension_management = PermissionsLocationsAddToolApprovalDetailsExtensionManagement.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsExtensionManagement"))
        permissions_locations_add_tool_approval_details_extension_permission_access = PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess"))
        permissions_locations_add_tool_approval_details_mcp = PermissionsLocationsAddToolApprovalDetailsMCP.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsMcp"))
        permissions_locations_add_tool_approval_details_mcp_sampling = PermissionsLocationsAddToolApprovalDetailsMCPSampling.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsMcpSampling"))
        permissions_locations_add_tool_approval_details_memory = PermissionsLocationsAddToolApprovalDetailsMemory.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsMemory"))
        permissions_locations_add_tool_approval_details_read = PermissionsLocationsAddToolApprovalDetailsRead.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsRead"))
        permissions_locations_add_tool_approval_details_write = PermissionsLocationsAddToolApprovalDetailsWrite.from_dict(obj.get("PermissionsLocationsAddToolApprovalDetailsWrite"))
        permissions_locations_add_tool_approval_result = PermissionsLocationsAddToolApprovalResult.from_dict(obj.get("PermissionsLocationsAddToolApprovalResult"))
        permissions_modify_rules_params = PermissionsModifyRulesParams.from_dict(obj.get("PermissionsModifyRulesParams"))
        permissions_modify_rules_result = PermissionsModifyRulesResult.from_dict(obj.get("PermissionsModifyRulesResult"))
        permissions_modify_rules_scope = PermissionsModifyRulesScope(obj.get("PermissionsModifyRulesScope"))
        permissions_notify_prompt_shown_result = PermissionsNotifyPromptShownResult.from_dict(obj.get("PermissionsNotifyPromptShownResult"))
        permissions_paths_add_result = PermissionsPathsAddResult.from_dict(obj.get("PermissionsPathsAddResult"))
        permissions_paths_list_request = PermissionsPathsListRequest.from_dict(obj.get("PermissionsPathsListRequest"))
        permissions_paths_update_primary_result = PermissionsPathsUpdatePrimaryResult.from_dict(obj.get("PermissionsPathsUpdatePrimaryResult"))
        permissions_pending_requests_request = PermissionsPendingRequestsRequest.from_dict(obj.get("PermissionsPendingRequestsRequest"))
        permissions_reset_session_approvals_request = PermissionsResetSessionApprovalsRequest.from_dict(obj.get("PermissionsResetSessionApprovalsRequest"))
        permissions_reset_session_approvals_result = PermissionsResetSessionApprovalsResult.from_dict(obj.get("PermissionsResetSessionApprovalsResult"))
        permissions_set_approve_all_request = PermissionsSetApproveAllRequest.from_dict(obj.get("PermissionsSetApproveAllRequest"))
        permissions_set_approve_all_result = PermissionsSetApproveAllResult.from_dict(obj.get("PermissionsSetApproveAllResult"))
        permissions_set_approve_all_source = PermissionsSetApproveAllSource(obj.get("PermissionsSetApproveAllSource"))
        permissions_set_required_request = PermissionsSetRequiredRequest.from_dict(obj.get("PermissionsSetRequiredRequest"))
        permissions_set_required_result = PermissionsSetRequiredResult.from_dict(obj.get("PermissionsSetRequiredResult"))
        permissions_urls_set_unrestricted_mode_result = PermissionsUrlsSetUnrestrictedModeResult.from_dict(obj.get("PermissionsUrlsSetUnrestrictedModeResult"))
        permission_urls_config = PermissionUrlsConfig.from_dict(obj.get("PermissionUrlsConfig"))
        permission_urls_set_unrestricted_mode_params = PermissionUrlsSetUnrestrictedModeParams.from_dict(obj.get("PermissionUrlsSetUnrestrictedModeParams"))
        ping_request = PingRequest.from_dict(obj.get("PingRequest"))
        ping_result = PingResult.from_dict(obj.get("PingResult"))
        plan_read_result = PlanReadResult.from_dict(obj.get("PlanReadResult"))
        plan_update_request = PlanUpdateRequest.from_dict(obj.get("PlanUpdateRequest"))
        plugin = Plugin.from_dict(obj.get("Plugin"))
        plugin_list = PluginList.from_dict(obj.get("PluginList"))
        queued_command_handled = QueuedCommandHandled.from_dict(obj.get("QueuedCommandHandled"))
        queued_command_not_handled = QueuedCommandNotHandled.from_dict(obj.get("QueuedCommandNotHandled"))
        queued_command_result = _load_QueuedCommandResult(obj.get("QueuedCommandResult"))
        queue_pending_items = QueuePendingItems.from_dict(obj.get("QueuePendingItems"))
        queue_pending_items_kind = QueuePendingItemsKind(obj.get("QueuePendingItemsKind"))
        queue_pending_items_result = QueuePendingItemsResult.from_dict(obj.get("QueuePendingItemsResult"))
        queue_remove_most_recent_result = QueueRemoveMostRecentResult.from_dict(obj.get("QueueRemoveMostRecentResult"))
        register_event_interest_params = RegisterEventInterestParams.from_dict(obj.get("RegisterEventInterestParams"))
        register_event_interest_result = RegisterEventInterestResult.from_dict(obj.get("RegisterEventInterestResult"))
        release_event_interest_params = ReleaseEventInterestParams.from_dict(obj.get("ReleaseEventInterestParams"))
        remote_enable_request = RemoteEnableRequest.from_dict(obj.get("RemoteEnableRequest"))
        remote_enable_result = RemoteEnableResult.from_dict(obj.get("RemoteEnableResult"))
        remote_notify_steerable_changed_request = RemoteNotifySteerableChangedRequest.from_dict(obj.get("RemoteNotifySteerableChangedRequest"))
        remote_notify_steerable_changed_result = RemoteNotifySteerableChangedResult.from_dict(obj.get("RemoteNotifySteerableChangedResult"))
        remote_session_connection_result = RemoteSessionConnectionResult.from_dict(obj.get("RemoteSessionConnectionResult"))
        remote_session_mode = RemoteSessionMode(obj.get("RemoteSessionMode"))
        schedule_entry = ScheduleEntry.from_dict(obj.get("ScheduleEntry"))
        schedule_list = ScheduleList.from_dict(obj.get("ScheduleList"))
        schedule_stop_request = ScheduleStopRequest.from_dict(obj.get("ScheduleStopRequest"))
        schedule_stop_result = ScheduleStopResult.from_dict(obj.get("ScheduleStopResult"))
        secrets_add_filter_values_request = SecretsAddFilterValuesRequest.from_dict(obj.get("SecretsAddFilterValuesRequest"))
        secrets_add_filter_values_result = SecretsAddFilterValuesResult.from_dict(obj.get("SecretsAddFilterValuesResult"))
        send_agent_mode = SendAgentMode(obj.get("SendAgentMode"))
        send_attachment = _load_SendAttachment(obj.get("SendAttachment"))
        send_attachment_blob = SendAttachmentBlob.from_dict(obj.get("SendAttachmentBlob"))
        send_attachment_directory = SendAttachmentDirectory.from_dict(obj.get("SendAttachmentDirectory"))
        send_attachment_file = SendAttachmentFile.from_dict(obj.get("SendAttachmentFile"))
        send_attachment_file_line_range = SendAttachmentFileLineRange.from_dict(obj.get("SendAttachmentFileLineRange"))
        send_attachment_github_reference = SendAttachmentGithubReference.from_dict(obj.get("SendAttachmentGithubReference"))
        send_attachment_github_reference_type = SendAttachmentGithubReferenceTypeEnum(obj.get("SendAttachmentGithubReferenceType"))
        send_attachment_selection = SendAttachmentSelection.from_dict(obj.get("SendAttachmentSelection"))
        send_attachment_selection_details = SendAttachmentSelectionDetails.from_dict(obj.get("SendAttachmentSelectionDetails"))
        send_attachment_selection_details_end = SendAttachmentSelectionDetailsEnd.from_dict(obj.get("SendAttachmentSelectionDetailsEnd"))
        send_attachment_selection_details_start = SendAttachmentSelectionDetailsStart.from_dict(obj.get("SendAttachmentSelectionDetailsStart"))
        send_mode = SendMode(obj.get("SendMode"))
        send_request = SendRequest.from_dict(obj.get("SendRequest"))
        send_result = SendResult.from_dict(obj.get("SendResult"))
        server_skill = ServerSkill.from_dict(obj.get("ServerSkill"))
        server_skill_list = ServerSkillList.from_dict(obj.get("ServerSkillList"))
        session_auth_status = SessionAuthStatus.from_dict(obj.get("SessionAuthStatus"))
        session_bulk_delete_result = SessionBulkDeleteResult.from_dict(obj.get("SessionBulkDeleteResult"))
        session_context = SessionContext.from_dict(obj.get("SessionContext"))
        session_context_host_type = HostType(obj.get("SessionContextHostType"))
        session_enrich_metadata_result = SessionEnrichMetadataResult.from_dict(obj.get("SessionEnrichMetadataResult"))
        session_fs_append_file_request = SessionFSAppendFileRequest.from_dict(obj.get("SessionFsAppendFileRequest"))
        session_fs_error = SessionFSError.from_dict(obj.get("SessionFsError"))
        session_fs_error_code = SessionFSErrorCode(obj.get("SessionFsErrorCode"))
        session_fs_exists_request = SessionFSExistsRequest.from_dict(obj.get("SessionFsExistsRequest"))
        session_fs_exists_result = SessionFSExistsResult.from_dict(obj.get("SessionFsExistsResult"))
        session_fs_mkdir_request = SessionFSMkdirRequest.from_dict(obj.get("SessionFsMkdirRequest"))
        session_fs_readdir_request = SessionFSReaddirRequest.from_dict(obj.get("SessionFsReaddirRequest"))
        session_fs_readdir_result = SessionFSReaddirResult.from_dict(obj.get("SessionFsReaddirResult"))
        session_fs_readdir_with_types_entry = SessionFSReaddirWithTypesEntry.from_dict(obj.get("SessionFsReaddirWithTypesEntry"))
        session_fs_readdir_with_types_entry_type = SessionFSReaddirWithTypesEntryType(obj.get("SessionFsReaddirWithTypesEntryType"))
        session_fs_readdir_with_types_request = SessionFSReaddirWithTypesRequest.from_dict(obj.get("SessionFsReaddirWithTypesRequest"))
        session_fs_readdir_with_types_result = SessionFSReaddirWithTypesResult.from_dict(obj.get("SessionFsReaddirWithTypesResult"))
        session_fs_read_file_request = SessionFSReadFileRequest.from_dict(obj.get("SessionFsReadFileRequest"))
        session_fs_read_file_result = SessionFSReadFileResult.from_dict(obj.get("SessionFsReadFileResult"))
        session_fs_rename_request = SessionFSRenameRequest.from_dict(obj.get("SessionFsRenameRequest"))
        session_fs_rm_request = SessionFSRmRequest.from_dict(obj.get("SessionFsRmRequest"))
        session_fs_set_provider_capabilities = SessionFSSetProviderCapabilities.from_dict(obj.get("SessionFsSetProviderCapabilities"))
        session_fs_set_provider_conventions = SessionFSSetProviderConventions(obj.get("SessionFsSetProviderConventions"))
        session_fs_set_provider_request = SessionFSSetProviderRequest.from_dict(obj.get("SessionFsSetProviderRequest"))
        session_fs_set_provider_result = SessionFSSetProviderResult.from_dict(obj.get("SessionFsSetProviderResult"))
        session_fs_sqlite_exists_request = SessionFSSqliteExistsRequest.from_dict(obj.get("SessionFsSqliteExistsRequest"))
        session_fs_sqlite_exists_result = SessionFSSqliteExistsResult.from_dict(obj.get("SessionFsSqliteExistsResult"))
        session_fs_sqlite_query_request = SessionFSSqliteQueryRequest.from_dict(obj.get("SessionFsSqliteQueryRequest"))
        session_fs_sqlite_query_result = SessionFSSqliteQueryResult.from_dict(obj.get("SessionFsSqliteQueryResult"))
        session_fs_sqlite_query_type = SessionFSSqliteQueryType(obj.get("SessionFsSqliteQueryType"))
        session_fs_stat_request = SessionFSStatRequest.from_dict(obj.get("SessionFsStatRequest"))
        session_fs_stat_result = SessionFSStatResult.from_dict(obj.get("SessionFsStatResult"))
        session_fs_write_file_request = SessionFSWriteFileRequest.from_dict(obj.get("SessionFsWriteFileRequest"))
        session_installed_plugin = SessionInstalledPlugin.from_dict(obj.get("SessionInstalledPlugin"))
        session_installed_plugin_source = from_union([SessionInstalledPluginSource.from_dict, from_str], obj.get("SessionInstalledPluginSource"))
        session_installed_plugin_source_github = SessionInstalledPluginSourceGithub.from_dict(obj.get("SessionInstalledPluginSourceGithub"))
        session_installed_plugin_source_local = SessionInstalledPluginSourceLocal.from_dict(obj.get("SessionInstalledPluginSourceLocal"))
        session_installed_plugin_source_url = SessionInstalledPluginSourceURL.from_dict(obj.get("SessionInstalledPluginSourceUrl"))
        session_list = SessionList.from_dict(obj.get("SessionList"))
        session_list_filter = SessionListFilter.from_dict(obj.get("SessionListFilter"))
        session_load_deferred_repo_hooks_result = SessionLoadDeferredRepoHooksResult.from_dict(obj.get("SessionLoadDeferredRepoHooksResult"))
        session_log_level = SessionLogLevel(obj.get("SessionLogLevel"))
        session_metadata = SessionMetadata.from_dict(obj.get("SessionMetadata"))
        session_metadata_snapshot = SessionMetadataSnapshot.from_dict(obj.get("SessionMetadataSnapshot"))
        session_mode = SessionMode(obj.get("SessionMode"))
        session_prune_result = SessionPruneResult.from_dict(obj.get("SessionPruneResult"))
        sessions_bulk_delete_request = SessionsBulkDeleteRequest.from_dict(obj.get("SessionsBulkDeleteRequest"))
        sessions_check_in_use_request = SessionsCheckInUseRequest.from_dict(obj.get("SessionsCheckInUseRequest"))
        sessions_check_in_use_result = SessionsCheckInUseResult.from_dict(obj.get("SessionsCheckInUseResult"))
        sessions_close_request = SessionsCloseRequest.from_dict(obj.get("SessionsCloseRequest"))
        sessions_close_result = SessionsCloseResult.from_dict(obj.get("SessionsCloseResult"))
        sessions_enrich_metadata_request = SessionsEnrichMetadataRequest.from_dict(obj.get("SessionsEnrichMetadataRequest"))
        session_set_credentials_params = SessionSetCredentialsParams.from_dict(obj.get("SessionSetCredentialsParams"))
        session_set_credentials_result = SessionSetCredentialsResult.from_dict(obj.get("SessionSetCredentialsResult"))
        sessions_find_by_prefix_request = SessionsFindByPrefixRequest.from_dict(obj.get("SessionsFindByPrefixRequest"))
        sessions_find_by_prefix_result = SessionsFindByPrefixResult.from_dict(obj.get("SessionsFindByPrefixResult"))
        sessions_find_by_task_id_request = SessionsFindByTaskIDRequest.from_dict(obj.get("SessionsFindByTaskIDRequest"))
        sessions_find_by_task_id_result = SessionsFindByTaskIDResult.from_dict(obj.get("SessionsFindByTaskIDResult"))
        sessions_fork_request = SessionsForkRequest.from_dict(obj.get("SessionsForkRequest"))
        sessions_fork_result = SessionsForkResult.from_dict(obj.get("SessionsForkResult"))
        sessions_get_event_file_path_request = SessionsGetEventFilePathRequest.from_dict(obj.get("SessionsGetEventFilePathRequest"))
        sessions_get_event_file_path_result = SessionsGetEventFilePathResult.from_dict(obj.get("SessionsGetEventFilePathResult"))
        sessions_get_last_for_context_request = SessionsGetLastForContextRequest.from_dict(obj.get("SessionsGetLastForContextRequest"))
        sessions_get_last_for_context_result = SessionsGetLastForContextResult.from_dict(obj.get("SessionsGetLastForContextResult"))
        sessions_get_persisted_remote_steerable_request = SessionsGetPersistedRemoteSteerableRequest.from_dict(obj.get("SessionsGetPersistedRemoteSteerableRequest"))
        sessions_get_persisted_remote_steerable_result = SessionsGetPersistedRemoteSteerableResult.from_dict(obj.get("SessionsGetPersistedRemoteSteerableResult"))
        session_sizes = SessionSizes.from_dict(obj.get("SessionSizes"))
        sessions_list_request = SessionsListRequest.from_dict(obj.get("SessionsListRequest"))
        sessions_load_deferred_repo_hooks_request = SessionsLoadDeferredRepoHooksRequest.from_dict(obj.get("SessionsLoadDeferredRepoHooksRequest"))
        sessions_prune_old_request = SessionsPruneOldRequest.from_dict(obj.get("SessionsPruneOldRequest"))
        sessions_release_lock_request = SessionsReleaseLockRequest.from_dict(obj.get("SessionsReleaseLockRequest"))
        sessions_release_lock_result = SessionsReleaseLockResult.from_dict(obj.get("SessionsReleaseLockResult"))
        sessions_reload_plugin_hooks_request = SessionsReloadPluginHooksRequest.from_dict(obj.get("SessionsReloadPluginHooksRequest"))
        sessions_reload_plugin_hooks_result = SessionsReloadPluginHooksResult.from_dict(obj.get("SessionsReloadPluginHooksResult"))
        sessions_save_request = SessionsSaveRequest.from_dict(obj.get("SessionsSaveRequest"))
        sessions_save_result = SessionsSaveResult.from_dict(obj.get("SessionsSaveResult"))
        sessions_set_additional_plugins_request = SessionsSetAdditionalPluginsRequest.from_dict(obj.get("SessionsSetAdditionalPluginsRequest"))
        sessions_set_additional_plugins_result = SessionsSetAdditionalPluginsResult.from_dict(obj.get("SessionsSetAdditionalPluginsResult"))
        session_update_options_params = SessionUpdateOptionsParams.from_dict(obj.get("SessionUpdateOptionsParams"))
        session_update_options_result = SessionUpdateOptionsResult.from_dict(obj.get("SessionUpdateOptionsResult"))
        session_working_directory_context = SessionWorkingDirectoryContext.from_dict(obj.get("SessionWorkingDirectoryContext"))
        session_working_directory_context_host_type = HostType(obj.get("SessionWorkingDirectoryContextHostType"))
        shell_exec_request = ShellExecRequest.from_dict(obj.get("ShellExecRequest"))
        shell_exec_result = ShellExecResult.from_dict(obj.get("ShellExecResult"))
        shell_kill_request = ShellKillRequest.from_dict(obj.get("ShellKillRequest"))
        shell_kill_result = ShellKillResult.from_dict(obj.get("ShellKillResult"))
        shell_kill_signal = ShellKillSignal(obj.get("ShellKillSignal"))
        shutdown_request = ShutdownRequest.from_dict(obj.get("ShutdownRequest"))
        skill = Skill.from_dict(obj.get("Skill"))
        skill_list = SkillList.from_dict(obj.get("SkillList"))
        skills_config_set_disabled_skills_request = SkillsConfigSetDisabledSkillsRequest.from_dict(obj.get("SkillsConfigSetDisabledSkillsRequest"))
        skills_disable_request = SkillsDisableRequest.from_dict(obj.get("SkillsDisableRequest"))
        skills_discover_request = SkillsDiscoverRequest.from_dict(obj.get("SkillsDiscoverRequest"))
        skills_enable_request = SkillsEnableRequest.from_dict(obj.get("SkillsEnableRequest"))
        skills_get_invoked_result = SkillsGetInvokedResult.from_dict(obj.get("SkillsGetInvokedResult"))
        skills_invoked_skill = SkillsInvokedSkill.from_dict(obj.get("SkillsInvokedSkill"))
        skills_load_diagnostics = SkillsLoadDiagnostics.from_dict(obj.get("SkillsLoadDiagnostics"))
        slash_command_agent_prompt_result = SlashCommandAgentPromptResult.from_dict(obj.get("SlashCommandAgentPromptResult"))
        slash_command_completed_result = SlashCommandCompletedResult.from_dict(obj.get("SlashCommandCompletedResult"))
        slash_command_info = SlashCommandInfo.from_dict(obj.get("SlashCommandInfo"))
        slash_command_input = SlashCommandInput.from_dict(obj.get("SlashCommandInput"))
        slash_command_input_completion = SlashCommandInputCompletion(obj.get("SlashCommandInputCompletion"))
        slash_command_invocation_result = _load_SlashCommandInvocationResult(obj.get("SlashCommandInvocationResult"))
        slash_command_kind = SlashCommandKind(obj.get("SlashCommandKind"))
        slash_command_select_subcommand_option = SlashCommandSelectSubcommandOption.from_dict(obj.get("SlashCommandSelectSubcommandOption"))
        slash_command_select_subcommand_result = SlashCommandSelectSubcommandResult.from_dict(obj.get("SlashCommandSelectSubcommandResult"))
        slash_command_text_result = SlashCommandTextResult.from_dict(obj.get("SlashCommandTextResult"))
        task_agent_info = TaskAgentInfo.from_dict(obj.get("TaskAgentInfo"))
        task_agent_progress = TaskAgentProgress.from_dict(obj.get("TaskAgentProgress"))
        task_execution_mode = TaskExecutionMode(obj.get("TaskExecutionMode"))
        task_info = _load_TaskInfo(obj.get("TaskInfo"))
        task_list = TaskList.from_dict(obj.get("TaskList"))
        task_progress_line = TaskProgressLine.from_dict(obj.get("TaskProgressLine"))
        tasks_cancel_request = TasksCancelRequest.from_dict(obj.get("TasksCancelRequest"))
        tasks_cancel_result = TasksCancelResult.from_dict(obj.get("TasksCancelResult"))
        tasks_get_current_promotable_result = TasksGetCurrentPromotableResult.from_dict(obj.get("TasksGetCurrentPromotableResult"))
        tasks_get_progress_request = TasksGetProgressRequest.from_dict(obj.get("TasksGetProgressRequest"))
        tasks_get_progress_result = TasksGetProgressResult.from_dict(obj.get("TasksGetProgressResult"))
        task_shell_info = TaskShellInfo.from_dict(obj.get("TaskShellInfo"))
        task_shell_info_attachment_mode = TaskShellInfoAttachmentMode(obj.get("TaskShellInfoAttachmentMode"))
        task_shell_progress = TaskShellProgress.from_dict(obj.get("TaskShellProgress"))
        tasks_promote_current_to_background_result = TasksPromoteCurrentToBackgroundResult.from_dict(obj.get("TasksPromoteCurrentToBackgroundResult"))
        tasks_promote_to_background_request = TasksPromoteToBackgroundRequest.from_dict(obj.get("TasksPromoteToBackgroundRequest"))
        tasks_promote_to_background_result = TasksPromoteToBackgroundResult.from_dict(obj.get("TasksPromoteToBackgroundResult"))
        tasks_refresh_result = TasksRefreshResult.from_dict(obj.get("TasksRefreshResult"))
        tasks_remove_request = TasksRemoveRequest.from_dict(obj.get("TasksRemoveRequest"))
        tasks_remove_result = TasksRemoveResult.from_dict(obj.get("TasksRemoveResult"))
        tasks_send_message_request = TasksSendMessageRequest.from_dict(obj.get("TasksSendMessageRequest"))
        tasks_send_message_result = TasksSendMessageResult.from_dict(obj.get("TasksSendMessageResult"))
        tasks_start_agent_request = TasksStartAgentRequest.from_dict(obj.get("TasksStartAgentRequest"))
        tasks_start_agent_result = TasksStartAgentResult.from_dict(obj.get("TasksStartAgentResult"))
        task_status = TaskStatus(obj.get("TaskStatus"))
        tasks_wait_for_pending_result = TasksWaitForPendingResult.from_dict(obj.get("TasksWaitForPendingResult"))
        telemetry_set_feature_overrides_request = TelemetrySetFeatureOverridesRequest.from_dict(obj.get("TelemetrySetFeatureOverridesRequest"))
        token_auth_info = TokenAuthInfo.from_dict(obj.get("TokenAuthInfo"))
        tool = Tool.from_dict(obj.get("Tool"))
        tool_list = ToolList.from_dict(obj.get("ToolList"))
        tools_initialize_and_validate_result = ToolsInitializeAndValidateResult.from_dict(obj.get("ToolsInitializeAndValidateResult"))
        tools_list_request = ToolsListRequest.from_dict(obj.get("ToolsListRequest"))
        ui_auto_mode_switch_response = UIAutoModeSwitchResponse(obj.get("UIAutoModeSwitchResponse"))
        ui_elicitation_array_any_of_field = UIElicitationArrayAnyOfField.from_dict(obj.get("UIElicitationArrayAnyOfField"))
        ui_elicitation_array_any_of_field_items = UIElicitationArrayAnyOfFieldItems.from_dict(obj.get("UIElicitationArrayAnyOfFieldItems"))
        ui_elicitation_array_any_of_field_items_any_of = UIElicitationArrayAnyOfFieldItemsAnyOf.from_dict(obj.get("UIElicitationArrayAnyOfFieldItemsAnyOf"))
        ui_elicitation_array_enum_field = UIElicitationArrayEnumField.from_dict(obj.get("UIElicitationArrayEnumField"))
        ui_elicitation_array_enum_field_items = UIElicitationArrayEnumFieldItems.from_dict(obj.get("UIElicitationArrayEnumFieldItems"))
        ui_elicitation_field_value = from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str], obj.get("UIElicitationFieldValue"))
        ui_elicitation_request = UIElicitationRequest.from_dict(obj.get("UIElicitationRequest"))
        ui_elicitation_response = UIElicitationResponse.from_dict(obj.get("UIElicitationResponse"))
        ui_elicitation_response_action = UIElicitationResponseAction(obj.get("UIElicitationResponseAction"))
        ui_elicitation_response_content = from_dict(lambda x: from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str], x), obj.get("UIElicitationResponseContent"))
        ui_elicitation_result = UIElicitationResult.from_dict(obj.get("UIElicitationResult"))
        ui_elicitation_schema = UIElicitationSchema.from_dict(obj.get("UIElicitationSchema"))
        ui_elicitation_schema_property = UIElicitationSchemaProperty.from_dict(obj.get("UIElicitationSchemaProperty"))
        ui_elicitation_schema_property_boolean = UIElicitationSchemaPropertyBoolean.from_dict(obj.get("UIElicitationSchemaPropertyBoolean"))
        ui_elicitation_schema_property_number = UIElicitationSchemaPropertyNumber.from_dict(obj.get("UIElicitationSchemaPropertyNumber"))
        ui_elicitation_schema_property_number_type = UIElicitationSchemaPropertyNumberType(obj.get("UIElicitationSchemaPropertyNumberType"))
        ui_elicitation_schema_property_string = UIElicitationSchemaPropertyString.from_dict(obj.get("UIElicitationSchemaPropertyString"))
        ui_elicitation_schema_property_string_format = UIElicitationSchemaPropertyStringFormat(obj.get("UIElicitationSchemaPropertyStringFormat"))
        ui_elicitation_string_enum_field = UIElicitationStringEnumField.from_dict(obj.get("UIElicitationStringEnumField"))
        ui_elicitation_string_one_of_field = UIElicitationStringOneOfField.from_dict(obj.get("UIElicitationStringOneOfField"))
        ui_elicitation_string_one_of_field_one_of = UIElicitationStringOneOfFieldOneOf.from_dict(obj.get("UIElicitationStringOneOfFieldOneOf"))
        ui_exit_plan_mode_action = UIExitPlanModeAction(obj.get("UIExitPlanModeAction"))
        ui_exit_plan_mode_response = UIExitPlanModeResponse.from_dict(obj.get("UIExitPlanModeResponse"))
        ui_handle_pending_auto_mode_switch_request = UIHandlePendingAutoModeSwitchRequest.from_dict(obj.get("UIHandlePendingAutoModeSwitchRequest"))
        ui_handle_pending_elicitation_request = UIHandlePendingElicitationRequest.from_dict(obj.get("UIHandlePendingElicitationRequest"))
        ui_handle_pending_exit_plan_mode_request = UIHandlePendingExitPlanModeRequest.from_dict(obj.get("UIHandlePendingExitPlanModeRequest"))
        ui_handle_pending_result = UIHandlePendingResult.from_dict(obj.get("UIHandlePendingResult"))
        ui_handle_pending_sampling_request = UIHandlePendingSamplingRequest.from_dict(obj.get("UIHandlePendingSamplingRequest"))
        ui_handle_pending_sampling_response = from_dict(lambda x: x, obj.get("UIHandlePendingSamplingResponse"))
        ui_handle_pending_user_input_request = UIHandlePendingUserInputRequest.from_dict(obj.get("UIHandlePendingUserInputRequest"))
        ui_register_direct_auto_mode_switch_handler_result = UIRegisterDirectAutoModeSwitchHandlerResult.from_dict(obj.get("UIRegisterDirectAutoModeSwitchHandlerResult"))
        ui_unregister_direct_auto_mode_switch_handler_request = UIUnregisterDirectAutoModeSwitchHandlerRequest.from_dict(obj.get("UIUnregisterDirectAutoModeSwitchHandlerRequest"))
        ui_unregister_direct_auto_mode_switch_handler_result = UIUnregisterDirectAutoModeSwitchHandlerResult.from_dict(obj.get("UIUnregisterDirectAutoModeSwitchHandlerResult"))
        ui_user_input_response = UIUserInputResponse.from_dict(obj.get("UIUserInputResponse"))
        usage_get_metrics_result = UsageGetMetricsResult.from_dict(obj.get("UsageGetMetricsResult"))
        usage_metrics_code_changes = UsageMetricsCodeChanges.from_dict(obj.get("UsageMetricsCodeChanges"))
        usage_metrics_model_metric = UsageMetricsModelMetric.from_dict(obj.get("UsageMetricsModelMetric"))
        usage_metrics_model_metric_requests = UsageMetricsModelMetricRequests.from_dict(obj.get("UsageMetricsModelMetricRequests"))
        usage_metrics_model_metric_token_detail = UsageMetricsModelMetricTokenDetail.from_dict(obj.get("UsageMetricsModelMetricTokenDetail"))
        usage_metrics_model_metric_usage = UsageMetricsModelMetricUsage.from_dict(obj.get("UsageMetricsModelMetricUsage"))
        usage_metrics_token_detail = UsageMetricsTokenDetail.from_dict(obj.get("UsageMetricsTokenDetail"))
        user_auth_info = UserAuthInfo.from_dict(obj.get("UserAuthInfo"))
        workspaces_checkpoints = WorkspacesCheckpoints.from_dict(obj.get("WorkspacesCheckpoints"))
        workspaces_create_file_request = WorkspacesCreateFileRequest.from_dict(obj.get("WorkspacesCreateFileRequest"))
        workspaces_get_workspace_result = WorkspacesGetWorkspaceResult.from_dict(obj.get("WorkspacesGetWorkspaceResult"))
        workspaces_list_checkpoints_result = WorkspacesListCheckpointsResult.from_dict(obj.get("WorkspacesListCheckpointsResult"))
        workspaces_list_files_result = WorkspacesListFilesResult.from_dict(obj.get("WorkspacesListFilesResult"))
        workspaces_read_checkpoint_request = WorkspacesReadCheckpointRequest.from_dict(obj.get("WorkspacesReadCheckpointRequest"))
        workspaces_read_checkpoint_result = WorkspacesReadCheckpointResult.from_dict(obj.get("WorkspacesReadCheckpointResult"))
        workspaces_read_file_request = WorkspacesReadFileRequest.from_dict(obj.get("WorkspacesReadFileRequest"))
        workspaces_read_file_result = WorkspacesReadFileResult.from_dict(obj.get("WorkspacesReadFileResult"))
        workspaces_save_large_paste_request = WorkspacesSaveLargePasteRequest.from_dict(obj.get("WorkspacesSaveLargePasteRequest"))
        workspaces_save_large_paste_result = WorkspacesSaveLargePasteResult.from_dict(obj.get("WorkspacesSaveLargePasteResult"))
        workspace_summary_host_type = HostType(obj.get("WorkspaceSummaryHostType"))
        workspaces_workspace_details_host_type = HostType(obj.get("WorkspacesWorkspaceDetailsHostType"))
        session_context_info = from_union([SessionContextInfo.from_dict, from_none], obj.get("SessionContextInfo"))
        task_progress = from_union([TaskProgress.from_dict, from_none], obj.get("TaskProgress"))
        workspace_summary = from_union([WorkspaceSummary.from_dict, from_none], obj.get("WorkspaceSummary"))
        return RPC(abort_request, abort_result, account_get_quota_request, account_get_quota_result, account_quota_snapshot, agent_get_current_result, agent_info, agent_info_source, agent_list, agent_reload_result, agent_select_request, agent_select_result, api_key_auth_info, auth_info, auth_info_type, command_list, commands_handle_pending_command_request, commands_handle_pending_command_result, commands_invoke_request, commands_list_request, commands_respond_to_queued_command_request, commands_respond_to_queued_command_result, connected_remote_session_metadata, connected_remote_session_metadata_kind, connected_remote_session_metadata_repository, connect_remote_session_params, connect_request, connect_result, content_filter_mode, copilot_api_token_auth_info, copilot_user_response, copilot_user_response_endpoints, copilot_user_response_quota_snapshots, copilot_user_response_quota_snapshots_chat, copilot_user_response_quota_snapshots_completions, copilot_user_response_quota_snapshots_premium_interactions, current_model, discovered_mcp_server, discovered_mcp_server_type, enqueue_command_params, enqueue_command_result, env_auth_info, event_log_read_request, event_log_release_interest_result, event_log_tail_result, event_log_types, events_agent_scope, events_cursor_status, events_read_result, execute_command_params, execute_command_result, extension, extension_list, extensions_disable_request, extensions_enable_request, extension_source, extension_status, external_tool_result, external_tool_text_result_for_llm, external_tool_text_result_for_llm_binary_results_for_llm, external_tool_text_result_for_llm_binary_results_for_llm_type, external_tool_text_result_for_llm_content, external_tool_text_result_for_llm_content_audio, external_tool_text_result_for_llm_content_image, external_tool_text_result_for_llm_content_resource, external_tool_text_result_for_llm_content_resource_details, external_tool_text_result_for_llm_content_resource_link, external_tool_text_result_for_llm_content_resource_link_icon, external_tool_text_result_for_llm_content_resource_link_icon_theme, external_tool_text_result_for_llm_content_terminal, external_tool_text_result_for_llm_content_text, filter_mapping, fleet_start_request, fleet_start_result, folder_trust_add_params, folder_trust_check_params, folder_trust_check_result, gh_cli_auth_info, handle_pending_tool_call_request, handle_pending_tool_call_result, history_abort_manual_compaction_result, history_cancel_background_compaction_result, history_compact_context_window, history_compact_request, history_compact_result, history_summarize_for_handoff_result, history_truncate_request, history_truncate_result, hmac_auth_info, installed_plugin, installed_plugin_source, installed_plugin_source_github, installed_plugin_source_local, installed_plugin_source_url, instructions_get_sources_result, instructions_sources, instructions_sources_location, instructions_sources_type, log_request, log_result, lsp_initialize_request, mcp_cancel_sampling_execution_params, mcp_cancel_sampling_execution_result, mcp_config_add_request, mcp_config_disable_request, mcp_config_enable_request, mcp_config_list, mcp_config_remove_request, mcp_config_update_request, mcp_disable_request, mcp_discover_request, mcp_discover_result, mcp_enable_request, mcp_execute_sampling_params, mcp_execute_sampling_request, mcp_execute_sampling_result, mcp_oauth_login_request, mcp_oauth_login_result, mcp_remove_git_hub_result, mcp_sampling_execution_action, mcp_sampling_execution_result, mcp_server, mcp_server_config, mcp_server_config_http, mcp_server_config_http_auth, mcp_server_config_http_oauth_grant_type, mcp_server_config_http_type, mcp_server_config_stdio, mcp_server_list, mcp_set_env_value_mode_details, mcp_set_env_value_mode_params, mcp_set_env_value_mode_result, metadata_context_info_request, metadata_context_info_result, metadata_is_processing_result, metadata_recompute_context_tokens_request, metadata_recompute_context_tokens_result, metadata_record_context_change_request, metadata_record_context_change_result, metadata_set_working_directory_request, metadata_set_working_directory_result, metadata_snapshot_current_mode, metadata_snapshot_remote_metadata, metadata_snapshot_remote_metadata_repository, metadata_snapshot_remote_metadata_task_type, model, model_billing, model_billing_token_prices, model_capabilities, model_capabilities_limits, model_capabilities_limits_vision, model_capabilities_override, model_capabilities_override_limits, model_capabilities_override_limits_vision, model_capabilities_override_supports, model_capabilities_supports, model_list, model_picker_category, model_picker_price_category, model_policy, model_policy_state, model_set_reasoning_effort_request, model_set_reasoning_effort_result, models_list_request, model_switch_to_request, model_switch_to_result, mode_set_request, name_get_result, name_set_auto_request, name_set_auto_result, name_set_request, options_update_env_value_mode, pending_permission_request, pending_permission_request_list, permission_decision, permission_decision_approved, permission_decision_approved_for_location, permission_decision_approved_for_session, permission_decision_approve_for_location, permission_decision_approve_for_location_approval, permission_decision_approve_for_location_approval_commands, permission_decision_approve_for_location_approval_custom_tool, permission_decision_approve_for_location_approval_extension_management, permission_decision_approve_for_location_approval_extension_permission_access, permission_decision_approve_for_location_approval_mcp, permission_decision_approve_for_location_approval_mcp_sampling, permission_decision_approve_for_location_approval_memory, permission_decision_approve_for_location_approval_read, permission_decision_approve_for_location_approval_write, permission_decision_approve_for_session, permission_decision_approve_for_session_approval, permission_decision_approve_for_session_approval_commands, permission_decision_approve_for_session_approval_custom_tool, permission_decision_approve_for_session_approval_extension_management, permission_decision_approve_for_session_approval_extension_permission_access, permission_decision_approve_for_session_approval_mcp, permission_decision_approve_for_session_approval_mcp_sampling, permission_decision_approve_for_session_approval_memory, permission_decision_approve_for_session_approval_read, permission_decision_approve_for_session_approval_write, permission_decision_approve_once, permission_decision_approve_permanently, permission_decision_cancelled, permission_decision_denied_by_content_exclusion_policy, permission_decision_denied_by_permission_request_hook, permission_decision_denied_by_rules, permission_decision_denied_interactively_by_user, permission_decision_denied_no_approval_rule_and_could_not_request_from_user, permission_decision_reject, permission_decision_request, permission_decision_user_not_available, permission_location_add_tool_approval_params, permission_location_apply_params, permission_location_apply_result, permission_location_resolve_params, permission_location_resolve_result, permission_location_type, permission_paths_add_params, permission_paths_allowed_check_params, permission_paths_allowed_check_result, permission_paths_config, permission_paths_list, permission_paths_update_primary_params, permission_paths_workspace_check_params, permission_paths_workspace_check_result, permission_prompt_shown_notification, permission_request_result, permission_rules_set, permissions_configure_additional_content_exclusion_policy, permissions_configure_additional_content_exclusion_policy_rule, permissions_configure_additional_content_exclusion_policy_rule_source, permissions_configure_additional_content_exclusion_policy_scope, permissions_configure_params, permissions_configure_result, permissions_folder_trust_add_trusted_result, permissions_locations_add_tool_approval_details, permissions_locations_add_tool_approval_details_commands, permissions_locations_add_tool_approval_details_custom_tool, permissions_locations_add_tool_approval_details_extension_management, permissions_locations_add_tool_approval_details_extension_permission_access, permissions_locations_add_tool_approval_details_mcp, permissions_locations_add_tool_approval_details_mcp_sampling, permissions_locations_add_tool_approval_details_memory, permissions_locations_add_tool_approval_details_read, permissions_locations_add_tool_approval_details_write, permissions_locations_add_tool_approval_result, permissions_modify_rules_params, permissions_modify_rules_result, permissions_modify_rules_scope, permissions_notify_prompt_shown_result, permissions_paths_add_result, permissions_paths_list_request, permissions_paths_update_primary_result, permissions_pending_requests_request, permissions_reset_session_approvals_request, permissions_reset_session_approvals_result, permissions_set_approve_all_request, permissions_set_approve_all_result, permissions_set_approve_all_source, permissions_set_required_request, permissions_set_required_result, permissions_urls_set_unrestricted_mode_result, permission_urls_config, permission_urls_set_unrestricted_mode_params, ping_request, ping_result, plan_read_result, plan_update_request, plugin, plugin_list, queued_command_handled, queued_command_not_handled, queued_command_result, queue_pending_items, queue_pending_items_kind, queue_pending_items_result, queue_remove_most_recent_result, register_event_interest_params, register_event_interest_result, release_event_interest_params, remote_enable_request, remote_enable_result, remote_notify_steerable_changed_request, remote_notify_steerable_changed_result, remote_session_connection_result, remote_session_mode, schedule_entry, schedule_list, schedule_stop_request, schedule_stop_result, secrets_add_filter_values_request, secrets_add_filter_values_result, send_agent_mode, send_attachment, send_attachment_blob, send_attachment_directory, send_attachment_file, send_attachment_file_line_range, send_attachment_github_reference, send_attachment_github_reference_type, send_attachment_selection, send_attachment_selection_details, send_attachment_selection_details_end, send_attachment_selection_details_start, send_mode, send_request, send_result, server_skill, server_skill_list, session_auth_status, session_bulk_delete_result, session_context, session_context_host_type, session_enrich_metadata_result, session_fs_append_file_request, session_fs_error, session_fs_error_code, session_fs_exists_request, session_fs_exists_result, session_fs_mkdir_request, session_fs_readdir_request, session_fs_readdir_result, session_fs_readdir_with_types_entry, session_fs_readdir_with_types_entry_type, session_fs_readdir_with_types_request, session_fs_readdir_with_types_result, session_fs_read_file_request, session_fs_read_file_result, session_fs_rename_request, session_fs_rm_request, session_fs_set_provider_capabilities, session_fs_set_provider_conventions, session_fs_set_provider_request, session_fs_set_provider_result, session_fs_sqlite_exists_request, session_fs_sqlite_exists_result, session_fs_sqlite_query_request, session_fs_sqlite_query_result, session_fs_sqlite_query_type, session_fs_stat_request, session_fs_stat_result, session_fs_write_file_request, session_installed_plugin, session_installed_plugin_source, session_installed_plugin_source_github, session_installed_plugin_source_local, session_installed_plugin_source_url, session_list, session_list_filter, session_load_deferred_repo_hooks_result, session_log_level, session_metadata, session_metadata_snapshot, session_mode, session_prune_result, sessions_bulk_delete_request, sessions_check_in_use_request, sessions_check_in_use_result, sessions_close_request, sessions_close_result, sessions_enrich_metadata_request, session_set_credentials_params, session_set_credentials_result, sessions_find_by_prefix_request, sessions_find_by_prefix_result, sessions_find_by_task_id_request, sessions_find_by_task_id_result, sessions_fork_request, sessions_fork_result, sessions_get_event_file_path_request, sessions_get_event_file_path_result, sessions_get_last_for_context_request, sessions_get_last_for_context_result, sessions_get_persisted_remote_steerable_request, sessions_get_persisted_remote_steerable_result, session_sizes, sessions_list_request, sessions_load_deferred_repo_hooks_request, sessions_prune_old_request, sessions_release_lock_request, sessions_release_lock_result, sessions_reload_plugin_hooks_request, sessions_reload_plugin_hooks_result, sessions_save_request, sessions_save_result, sessions_set_additional_plugins_request, sessions_set_additional_plugins_result, session_update_options_params, session_update_options_result, session_working_directory_context, session_working_directory_context_host_type, shell_exec_request, shell_exec_result, shell_kill_request, shell_kill_result, shell_kill_signal, shutdown_request, skill, skill_list, skills_config_set_disabled_skills_request, skills_disable_request, skills_discover_request, skills_enable_request, skills_get_invoked_result, skills_invoked_skill, skills_load_diagnostics, slash_command_agent_prompt_result, slash_command_completed_result, slash_command_info, slash_command_input, slash_command_input_completion, slash_command_invocation_result, slash_command_kind, slash_command_select_subcommand_option, slash_command_select_subcommand_result, slash_command_text_result, task_agent_info, task_agent_progress, task_execution_mode, task_info, task_list, task_progress_line, tasks_cancel_request, tasks_cancel_result, tasks_get_current_promotable_result, tasks_get_progress_request, tasks_get_progress_result, task_shell_info, task_shell_info_attachment_mode, task_shell_progress, tasks_promote_current_to_background_result, tasks_promote_to_background_request, tasks_promote_to_background_result, tasks_refresh_result, tasks_remove_request, tasks_remove_result, tasks_send_message_request, tasks_send_message_result, tasks_start_agent_request, tasks_start_agent_result, task_status, tasks_wait_for_pending_result, telemetry_set_feature_overrides_request, token_auth_info, tool, tool_list, tools_initialize_and_validate_result, tools_list_request, ui_auto_mode_switch_response, ui_elicitation_array_any_of_field, ui_elicitation_array_any_of_field_items, ui_elicitation_array_any_of_field_items_any_of, ui_elicitation_array_enum_field, ui_elicitation_array_enum_field_items, ui_elicitation_field_value, ui_elicitation_request, ui_elicitation_response, ui_elicitation_response_action, ui_elicitation_response_content, ui_elicitation_result, ui_elicitation_schema, ui_elicitation_schema_property, ui_elicitation_schema_property_boolean, ui_elicitation_schema_property_number, ui_elicitation_schema_property_number_type, ui_elicitation_schema_property_string, ui_elicitation_schema_property_string_format, ui_elicitation_string_enum_field, ui_elicitation_string_one_of_field, ui_elicitation_string_one_of_field_one_of, ui_exit_plan_mode_action, ui_exit_plan_mode_response, ui_handle_pending_auto_mode_switch_request, ui_handle_pending_elicitation_request, ui_handle_pending_exit_plan_mode_request, ui_handle_pending_result, ui_handle_pending_sampling_request, ui_handle_pending_sampling_response, ui_handle_pending_user_input_request, ui_register_direct_auto_mode_switch_handler_result, ui_unregister_direct_auto_mode_switch_handler_request, ui_unregister_direct_auto_mode_switch_handler_result, ui_user_input_response, usage_get_metrics_result, usage_metrics_code_changes, usage_metrics_model_metric, usage_metrics_model_metric_requests, usage_metrics_model_metric_token_detail, usage_metrics_model_metric_usage, usage_metrics_token_detail, user_auth_info, workspaces_checkpoints, workspaces_create_file_request, workspaces_get_workspace_result, workspaces_list_checkpoints_result, workspaces_list_files_result, workspaces_read_checkpoint_request, workspaces_read_checkpoint_result, workspaces_read_file_request, workspaces_read_file_result, workspaces_save_large_paste_request, workspaces_save_large_paste_result, workspace_summary_host_type, workspaces_workspace_details_host_type, session_context_info, task_progress, workspace_summary)

    def to_dict(self) -> dict:
        result: dict = {}
        result["AbortRequest"] = to_class(AbortRequest, self.abort_request)
        result["AbortResult"] = to_class(AbortResult, self.abort_result)
        result["AccountGetQuotaRequest"] = to_class(AccountGetQuotaRequest, self.account_get_quota_request)
        result["AccountGetQuotaResult"] = to_class(AccountGetQuotaResult, self.account_get_quota_result)
        result["AccountQuotaSnapshot"] = to_class(AccountQuotaSnapshot, self.account_quota_snapshot)
        result["AgentGetCurrentResult"] = to_class(AgentGetCurrentResult, self.agent_get_current_result)
        result["AgentInfo"] = to_class(AgentInfo, self.agent_info)
        result["AgentInfoSource"] = to_enum(AgentInfoSource, self.agent_info_source)
        result["AgentList"] = to_class(AgentList, self.agent_list)
        result["AgentReloadResult"] = to_class(AgentReloadResult, self.agent_reload_result)
        result["AgentSelectRequest"] = to_class(AgentSelectRequest, self.agent_select_request)
        result["AgentSelectResult"] = to_class(AgentSelectResult, self.agent_select_result)
        result["ApiKeyAuthInfo"] = to_class(APIKeyAuthInfo, self.api_key_auth_info)
        result["AuthInfo"] = (self.auth_info).to_dict()
        result["AuthInfoType"] = to_enum(AuthInfoType, self.auth_info_type)
        result["CommandList"] = to_class(CommandList, self.command_list)
        result["CommandsHandlePendingCommandRequest"] = to_class(CommandsHandlePendingCommandRequest, self.commands_handle_pending_command_request)
        result["CommandsHandlePendingCommandResult"] = to_class(CommandsHandlePendingCommandResult, self.commands_handle_pending_command_result)
        result["CommandsInvokeRequest"] = to_class(CommandsInvokeRequest, self.commands_invoke_request)
        result["CommandsListRequest"] = to_class(CommandsListRequest, self.commands_list_request)
        result["CommandsRespondToQueuedCommandRequest"] = to_class(CommandsRespondToQueuedCommandRequest, self.commands_respond_to_queued_command_request)
        result["CommandsRespondToQueuedCommandResult"] = to_class(CommandsRespondToQueuedCommandResult, self.commands_respond_to_queued_command_result)
        result["ConnectedRemoteSessionMetadata"] = to_class(ConnectedRemoteSessionMetadata, self.connected_remote_session_metadata)
        result["ConnectedRemoteSessionMetadataKind"] = to_enum(ConnectedRemoteSessionMetadataKind, self.connected_remote_session_metadata_kind)
        result["ConnectedRemoteSessionMetadataRepository"] = to_class(ConnectedRemoteSessionMetadataRepository, self.connected_remote_session_metadata_repository)
        result["ConnectRemoteSessionParams"] = to_class(ConnectRemoteSessionParams, self.connect_remote_session_params)
        result["ConnectRequest"] = to_class(ConnectRequest, self.connect_request)
        result["ConnectResult"] = to_class(ConnectResult, self.connect_result)
        result["ContentFilterMode"] = to_enum(ContentFilterMode, self.content_filter_mode)
        result["CopilotApiTokenAuthInfo"] = to_class(CopilotAPITokenAuthInfo, self.copilot_api_token_auth_info)
        result["CopilotUserResponse"] = to_class(CopilotUserResponse, self.copilot_user_response)
        result["CopilotUserResponseEndpoints"] = to_class(CopilotUserResponseEndpoints, self.copilot_user_response_endpoints)
        result["CopilotUserResponseQuotaSnapshots"] = from_dict(lambda x: from_union([lambda x: to_class(CopilotUserResponseQuotaSnapshots, x), from_none], x), self.copilot_user_response_quota_snapshots)
        result["CopilotUserResponseQuotaSnapshotsChat"] = to_class(CopilotUserResponseQuotaSnapshotsChat, self.copilot_user_response_quota_snapshots_chat)
        result["CopilotUserResponseQuotaSnapshotsCompletions"] = to_class(CopilotUserResponseQuotaSnapshotsCompletions, self.copilot_user_response_quota_snapshots_completions)
        result["CopilotUserResponseQuotaSnapshotsPremiumInteractions"] = to_class(CopilotUserResponseQuotaSnapshotsPremiumInteractions, self.copilot_user_response_quota_snapshots_premium_interactions)
        result["CurrentModel"] = to_class(CurrentModel, self.current_model)
        result["DiscoveredMcpServer"] = to_class(DiscoveredMCPServer, self.discovered_mcp_server)
        result["DiscoveredMcpServerType"] = to_enum(DiscoveredMCPServerType, self.discovered_mcp_server_type)
        result["EnqueueCommandParams"] = to_class(EnqueueCommandParams, self.enqueue_command_params)
        result["EnqueueCommandResult"] = to_class(EnqueueCommandResult, self.enqueue_command_result)
        result["EnvAuthInfo"] = to_class(EnvAuthInfo, self.env_auth_info)
        result["EventLogReadRequest"] = to_class(EventLogReadRequest, self.event_log_read_request)
        result["EventLogReleaseInterestResult"] = to_class(EventLogReleaseInterestResult, self.event_log_release_interest_result)
        result["EventLogTailResult"] = to_class(EventLogTailResult, self.event_log_tail_result)
        result["EventLogTypes"] = from_union([lambda x: from_list(from_str, x), lambda x: to_enum(EventLogTypes, x)], self.event_log_types)
        result["EventsAgentScope"] = to_enum(EventsAgentScope, self.events_agent_scope)
        result["EventsCursorStatus"] = to_enum(EventsCursorStatus, self.events_cursor_status)
        result["EventsReadResult"] = to_class(EventsReadResult, self.events_read_result)
        result["ExecuteCommandParams"] = to_class(ExecuteCommandParams, self.execute_command_params)
        result["ExecuteCommandResult"] = to_class(ExecuteCommandResult, self.execute_command_result)
        result["Extension"] = to_class(Extension, self.extension)
        result["ExtensionList"] = to_class(ExtensionList, self.extension_list)
        result["ExtensionsDisableRequest"] = to_class(ExtensionsDisableRequest, self.extensions_disable_request)
        result["ExtensionsEnableRequest"] = to_class(ExtensionsEnableRequest, self.extensions_enable_request)
        result["ExtensionSource"] = to_enum(ExtensionSource, self.extension_source)
        result["ExtensionStatus"] = to_enum(ExtensionStatus, self.extension_status)
        result["ExternalToolResult"] = from_union([lambda x: to_class(ExternalToolTextResultForLlm, x), from_str], self.external_tool_result)
        result["ExternalToolTextResultForLlm"] = to_class(ExternalToolTextResultForLlm, self.external_tool_text_result_for_llm)
        result["ExternalToolTextResultForLlmBinaryResultsForLlm"] = to_class(ExternalToolTextResultForLlmBinaryResultsForLlm, self.external_tool_text_result_for_llm_binary_results_for_llm)
        result["ExternalToolTextResultForLlmBinaryResultsForLlmType"] = to_enum(ExternalToolTextResultForLlmBinaryResultsForLlmType, self.external_tool_text_result_for_llm_binary_results_for_llm_type)
        result["ExternalToolTextResultForLlmContent"] = (self.external_tool_text_result_for_llm_content).to_dict()
        result["ExternalToolTextResultForLlmContentAudio"] = to_class(ExternalToolTextResultForLlmContentAudio, self.external_tool_text_result_for_llm_content_audio)
        result["ExternalToolTextResultForLlmContentImage"] = to_class(ExternalToolTextResultForLlmContentImage, self.external_tool_text_result_for_llm_content_image)
        result["ExternalToolTextResultForLlmContentResource"] = to_class(ExternalToolTextResultForLlmContentResource, self.external_tool_text_result_for_llm_content_resource)
        result["ExternalToolTextResultForLlmContentResourceDetails"] = from_union([lambda x: to_class(EmbeddedTextResourceContents, x), lambda x: to_class(EmbeddedBlobResourceContents, x)], self.external_tool_text_result_for_llm_content_resource_details)
        result["ExternalToolTextResultForLlmContentResourceLink"] = to_class(ExternalToolTextResultForLlmContentResourceLink, self.external_tool_text_result_for_llm_content_resource_link)
        result["ExternalToolTextResultForLlmContentResourceLinkIcon"] = to_class(ExternalToolTextResultForLlmContentResourceLinkIcon, self.external_tool_text_result_for_llm_content_resource_link_icon)
        result["ExternalToolTextResultForLlmContentResourceLinkIconTheme"] = to_enum(ExternalToolTextResultForLlmContentResourceLinkIconTheme, self.external_tool_text_result_for_llm_content_resource_link_icon_theme)
        result["ExternalToolTextResultForLlmContentTerminal"] = to_class(ExternalToolTextResultForLlmContentTerminal, self.external_tool_text_result_for_llm_content_terminal)
        result["ExternalToolTextResultForLlmContentText"] = to_class(ExternalToolTextResultForLlmContentText, self.external_tool_text_result_for_llm_content_text)
        result["FilterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(ContentFilterMode, x), x), lambda x: to_enum(ContentFilterMode, x)], self.filter_mapping)
        result["FleetStartRequest"] = to_class(FleetStartRequest, self.fleet_start_request)
        result["FleetStartResult"] = to_class(FleetStartResult, self.fleet_start_result)
        result["FolderTrustAddParams"] = to_class(FolderTrustAddParams, self.folder_trust_add_params)
        result["FolderTrustCheckParams"] = to_class(FolderTrustCheckParams, self.folder_trust_check_params)
        result["FolderTrustCheckResult"] = to_class(FolderTrustCheckResult, self.folder_trust_check_result)
        result["GhCliAuthInfo"] = to_class(GhCLIAuthInfo, self.gh_cli_auth_info)
        result["HandlePendingToolCallRequest"] = to_class(HandlePendingToolCallRequest, self.handle_pending_tool_call_request)
        result["HandlePendingToolCallResult"] = to_class(HandlePendingToolCallResult, self.handle_pending_tool_call_result)
        result["HistoryAbortManualCompactionResult"] = to_class(HistoryAbortManualCompactionResult, self.history_abort_manual_compaction_result)
        result["HistoryCancelBackgroundCompactionResult"] = to_class(HistoryCancelBackgroundCompactionResult, self.history_cancel_background_compaction_result)
        result["HistoryCompactContextWindow"] = to_class(HistoryCompactContextWindow, self.history_compact_context_window)
        result["HistoryCompactRequest"] = to_class(HistoryCompactRequest, self.history_compact_request)
        result["HistoryCompactResult"] = to_class(HistoryCompactResult, self.history_compact_result)
        result["HistorySummarizeForHandoffResult"] = to_class(HistorySummarizeForHandoffResult, self.history_summarize_for_handoff_result)
        result["HistoryTruncateRequest"] = to_class(HistoryTruncateRequest, self.history_truncate_request)
        result["HistoryTruncateResult"] = to_class(HistoryTruncateResult, self.history_truncate_result)
        result["HMACAuthInfo"] = to_class(HMACAuthInfo, self.hmac_auth_info)
        result["InstalledPlugin"] = to_class(InstalledPlugin, self.installed_plugin)
        result["InstalledPluginSource"] = from_union([lambda x: to_class(InstalledPluginSource, x), from_str], self.installed_plugin_source)
        result["InstalledPluginSourceGithub"] = to_class(InstalledPluginSourceGithub, self.installed_plugin_source_github)
        result["InstalledPluginSourceLocal"] = to_class(InstalledPluginSourceLocal, self.installed_plugin_source_local)
        result["InstalledPluginSourceUrl"] = to_class(InstalledPluginSourceURL, self.installed_plugin_source_url)
        result["InstructionsGetSourcesResult"] = to_class(InstructionsGetSourcesResult, self.instructions_get_sources_result)
        result["InstructionsSources"] = to_class(InstructionsSources, self.instructions_sources)
        result["InstructionsSourcesLocation"] = to_enum(InstructionsSourcesLocation, self.instructions_sources_location)
        result["InstructionsSourcesType"] = to_enum(InstructionsSourcesType, self.instructions_sources_type)
        result["LogRequest"] = to_class(LogRequest, self.log_request)
        result["LogResult"] = to_class(LogResult, self.log_result)
        result["LspInitializeRequest"] = to_class(LspInitializeRequest, self.lsp_initialize_request)
        result["McpCancelSamplingExecutionParams"] = to_class(MCPCancelSamplingExecutionParams, self.mcp_cancel_sampling_execution_params)
        result["McpCancelSamplingExecutionResult"] = to_class(MCPCancelSamplingExecutionResult, self.mcp_cancel_sampling_execution_result)
        result["McpConfigAddRequest"] = to_class(MCPConfigAddRequest, self.mcp_config_add_request)
        result["McpConfigDisableRequest"] = to_class(MCPConfigDisableRequest, self.mcp_config_disable_request)
        result["McpConfigEnableRequest"] = to_class(MCPConfigEnableRequest, self.mcp_config_enable_request)
        result["McpConfigList"] = to_class(MCPConfigList, self.mcp_config_list)
        result["McpConfigRemoveRequest"] = to_class(MCPConfigRemoveRequest, self.mcp_config_remove_request)
        result["McpConfigUpdateRequest"] = to_class(MCPConfigUpdateRequest, self.mcp_config_update_request)
        result["McpDisableRequest"] = to_class(MCPDisableRequest, self.mcp_disable_request)
        result["McpDiscoverRequest"] = to_class(MCPDiscoverRequest, self.mcp_discover_request)
        result["McpDiscoverResult"] = to_class(MCPDiscoverResult, self.mcp_discover_result)
        result["McpEnableRequest"] = to_class(MCPEnableRequest, self.mcp_enable_request)
        result["McpExecuteSamplingParams"] = to_class(MCPExecuteSamplingParams, self.mcp_execute_sampling_params)
        result["McpExecuteSamplingRequest"] = from_dict(lambda x: x, self.mcp_execute_sampling_request)
        result["McpExecuteSamplingResult"] = from_dict(lambda x: x, self.mcp_execute_sampling_result)
        result["McpOauthLoginRequest"] = to_class(MCPOauthLoginRequest, self.mcp_oauth_login_request)
        result["McpOauthLoginResult"] = to_class(MCPOauthLoginResult, self.mcp_oauth_login_result)
        result["McpRemoveGitHubResult"] = to_class(MCPRemoveGitHubResult, self.mcp_remove_git_hub_result)
        result["McpSamplingExecutionAction"] = to_enum(MCPSamplingExecutionAction, self.mcp_sampling_execution_action)
        result["McpSamplingExecutionResult"] = to_class(MCPSamplingExecutionResult, self.mcp_sampling_execution_result)
        result["McpServer"] = to_class(MCPServer, self.mcp_server)
        result["McpServerConfig"] = to_class(MCPServerConfig, self.mcp_server_config)
        result["McpServerConfigHttp"] = to_class(MCPServerConfigHTTP, self.mcp_server_config_http)
        result["McpServerConfigHttpAuth"] = to_class(MCPServerConfigHTTPAuth, self.mcp_server_config_http_auth)
        result["McpServerConfigHttpOauthGrantType"] = to_enum(MCPServerConfigHTTPOauthGrantType, self.mcp_server_config_http_oauth_grant_type)
        result["McpServerConfigHttpType"] = to_enum(MCPServerConfigHTTPType, self.mcp_server_config_http_type)
        result["McpServerConfigStdio"] = to_class(MCPServerConfigStdio, self.mcp_server_config_stdio)
        result["McpServerList"] = to_class(MCPServerList, self.mcp_server_list)
        result["McpSetEnvValueModeDetails"] = to_enum(MCPSetEnvValueModeDetails, self.mcp_set_env_value_mode_details)
        result["McpSetEnvValueModeParams"] = to_class(MCPSetEnvValueModeParams, self.mcp_set_env_value_mode_params)
        result["McpSetEnvValueModeResult"] = to_class(MCPSetEnvValueModeResult, self.mcp_set_env_value_mode_result)
        result["MetadataContextInfoRequest"] = to_class(MetadataContextInfoRequest, self.metadata_context_info_request)
        result["MetadataContextInfoResult"] = to_class(MetadataContextInfoResult, self.metadata_context_info_result)
        result["MetadataIsProcessingResult"] = to_class(MetadataIsProcessingResult, self.metadata_is_processing_result)
        result["MetadataRecomputeContextTokensRequest"] = to_class(MetadataRecomputeContextTokensRequest, self.metadata_recompute_context_tokens_request)
        result["MetadataRecomputeContextTokensResult"] = to_class(MetadataRecomputeContextTokensResult, self.metadata_recompute_context_tokens_result)
        result["MetadataRecordContextChangeRequest"] = to_class(MetadataRecordContextChangeRequest, self.metadata_record_context_change_request)
        result["MetadataRecordContextChangeResult"] = to_class(MetadataRecordContextChangeResult, self.metadata_record_context_change_result)
        result["MetadataSetWorkingDirectoryRequest"] = to_class(MetadataSetWorkingDirectoryRequest, self.metadata_set_working_directory_request)
        result["MetadataSetWorkingDirectoryResult"] = to_class(MetadataSetWorkingDirectoryResult, self.metadata_set_working_directory_result)
        result["MetadataSnapshotCurrentMode"] = to_enum(MetadataSnapshotCurrentMode, self.metadata_snapshot_current_mode)
        result["MetadataSnapshotRemoteMetadata"] = to_class(MetadataSnapshotRemoteMetadata, self.metadata_snapshot_remote_metadata)
        result["MetadataSnapshotRemoteMetadataRepository"] = to_class(MetadataSnapshotRemoteMetadataRepository, self.metadata_snapshot_remote_metadata_repository)
        result["MetadataSnapshotRemoteMetadataTaskType"] = to_enum(MetadataSnapshotRemoteMetadataTaskType, self.metadata_snapshot_remote_metadata_task_type)
        result["Model"] = to_class(Model, self.model)
        result["ModelBilling"] = to_class(ModelBilling, self.model_billing)
        result["ModelBillingTokenPrices"] = to_class(ModelBillingTokenPrices, self.model_billing_token_prices)
        result["ModelCapabilities"] = to_class(ModelCapabilities, self.model_capabilities)
        result["ModelCapabilitiesLimits"] = to_class(ModelCapabilitiesLimits, self.model_capabilities_limits)
        result["ModelCapabilitiesLimitsVision"] = to_class(ModelCapabilitiesLimitsVision, self.model_capabilities_limits_vision)
        result["ModelCapabilitiesOverride"] = to_class(ModelCapabilitiesOverride, self.model_capabilities_override)
        result["ModelCapabilitiesOverrideLimits"] = to_class(ModelCapabilitiesOverrideLimits, self.model_capabilities_override_limits)
        result["ModelCapabilitiesOverrideLimitsVision"] = to_class(ModelCapabilitiesOverrideLimitsVision, self.model_capabilities_override_limits_vision)
        result["ModelCapabilitiesOverrideSupports"] = to_class(ModelCapabilitiesOverrideSupports, self.model_capabilities_override_supports)
        result["ModelCapabilitiesSupports"] = to_class(ModelCapabilitiesSupports, self.model_capabilities_supports)
        result["ModelList"] = to_class(ModelList, self.model_list)
        result["ModelPickerCategory"] = to_enum(ModelPickerCategory, self.model_picker_category)
        result["ModelPickerPriceCategory"] = to_enum(ModelPickerPriceCategory, self.model_picker_price_category)
        result["ModelPolicy"] = to_class(ModelPolicy, self.model_policy)
        result["ModelPolicyState"] = to_enum(ModelPolicyState, self.model_policy_state)
        result["ModelSetReasoningEffortRequest"] = to_class(ModelSetReasoningEffortRequest, self.model_set_reasoning_effort_request)
        result["ModelSetReasoningEffortResult"] = to_class(ModelSetReasoningEffortResult, self.model_set_reasoning_effort_result)
        result["ModelsListRequest"] = to_class(ModelsListRequest, self.models_list_request)
        result["ModelSwitchToRequest"] = to_class(ModelSwitchToRequest, self.model_switch_to_request)
        result["ModelSwitchToResult"] = to_class(ModelSwitchToResult, self.model_switch_to_result)
        result["ModeSetRequest"] = to_class(ModeSetRequest, self.mode_set_request)
        result["NameGetResult"] = to_class(NameGetResult, self.name_get_result)
        result["NameSetAutoRequest"] = to_class(NameSetAutoRequest, self.name_set_auto_request)
        result["NameSetAutoResult"] = to_class(NameSetAutoResult, self.name_set_auto_result)
        result["NameSetRequest"] = to_class(NameSetRequest, self.name_set_request)
        result["OptionsUpdateEnvValueMode"] = to_enum(MCPSetEnvValueModeDetails, self.options_update_env_value_mode)
        result["PendingPermissionRequest"] = to_class(PendingPermissionRequest, self.pending_permission_request)
        result["PendingPermissionRequestList"] = to_class(PendingPermissionRequestList, self.pending_permission_request_list)
        result["PermissionDecision"] = (self.permission_decision).to_dict()
        result["PermissionDecisionApproved"] = to_class(PermissionDecisionApproved, self.permission_decision_approved)
        result["PermissionDecisionApprovedForLocation"] = to_class(PermissionDecisionApprovedForLocation, self.permission_decision_approved_for_location)
        result["PermissionDecisionApprovedForSession"] = to_class(PermissionDecisionApprovedForSession, self.permission_decision_approved_for_session)
        result["PermissionDecisionApproveForLocation"] = to_class(PermissionDecisionApproveForLocation, self.permission_decision_approve_for_location)
        result["PermissionDecisionApproveForLocationApproval"] = (self.permission_decision_approve_for_location_approval).to_dict()
        result["PermissionDecisionApproveForLocationApprovalCommands"] = to_class(PermissionDecisionApproveForLocationApprovalCommands, self.permission_decision_approve_for_location_approval_commands)
        result["PermissionDecisionApproveForLocationApprovalCustomTool"] = to_class(PermissionDecisionApproveForLocationApprovalCustomTool, self.permission_decision_approve_for_location_approval_custom_tool)
        result["PermissionDecisionApproveForLocationApprovalExtensionManagement"] = to_class(PermissionDecisionApproveForLocationApprovalExtensionManagement, self.permission_decision_approve_for_location_approval_extension_management)
        result["PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess"] = to_class(PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess, self.permission_decision_approve_for_location_approval_extension_permission_access)
        result["PermissionDecisionApproveForLocationApprovalMcp"] = to_class(PermissionDecisionApproveForLocationApprovalMCP, self.permission_decision_approve_for_location_approval_mcp)
        result["PermissionDecisionApproveForLocationApprovalMcpSampling"] = to_class(PermissionDecisionApproveForLocationApprovalMCPSampling, self.permission_decision_approve_for_location_approval_mcp_sampling)
        result["PermissionDecisionApproveForLocationApprovalMemory"] = to_class(PermissionDecisionApproveForLocationApprovalMemory, self.permission_decision_approve_for_location_approval_memory)
        result["PermissionDecisionApproveForLocationApprovalRead"] = to_class(PermissionDecisionApproveForLocationApprovalRead, self.permission_decision_approve_for_location_approval_read)
        result["PermissionDecisionApproveForLocationApprovalWrite"] = to_class(PermissionDecisionApproveForLocationApprovalWrite, self.permission_decision_approve_for_location_approval_write)
        result["PermissionDecisionApproveForSession"] = to_class(PermissionDecisionApproveForSession, self.permission_decision_approve_for_session)
        result["PermissionDecisionApproveForSessionApproval"] = (self.permission_decision_approve_for_session_approval).to_dict()
        result["PermissionDecisionApproveForSessionApprovalCommands"] = to_class(PermissionDecisionApproveForSessionApprovalCommands, self.permission_decision_approve_for_session_approval_commands)
        result["PermissionDecisionApproveForSessionApprovalCustomTool"] = to_class(PermissionDecisionApproveForSessionApprovalCustomTool, self.permission_decision_approve_for_session_approval_custom_tool)
        result["PermissionDecisionApproveForSessionApprovalExtensionManagement"] = to_class(PermissionDecisionApproveForSessionApprovalExtensionManagement, self.permission_decision_approve_for_session_approval_extension_management)
        result["PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess"] = to_class(PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess, self.permission_decision_approve_for_session_approval_extension_permission_access)
        result["PermissionDecisionApproveForSessionApprovalMcp"] = to_class(PermissionDecisionApproveForSessionApprovalMCP, self.permission_decision_approve_for_session_approval_mcp)
        result["PermissionDecisionApproveForSessionApprovalMcpSampling"] = to_class(PermissionDecisionApproveForSessionApprovalMCPSampling, self.permission_decision_approve_for_session_approval_mcp_sampling)
        result["PermissionDecisionApproveForSessionApprovalMemory"] = to_class(PermissionDecisionApproveForSessionApprovalMemory, self.permission_decision_approve_for_session_approval_memory)
        result["PermissionDecisionApproveForSessionApprovalRead"] = to_class(PermissionDecisionApproveForSessionApprovalRead, self.permission_decision_approve_for_session_approval_read)
        result["PermissionDecisionApproveForSessionApprovalWrite"] = to_class(PermissionDecisionApproveForSessionApprovalWrite, self.permission_decision_approve_for_session_approval_write)
        result["PermissionDecisionApproveOnce"] = to_class(PermissionDecisionApproveOnce, self.permission_decision_approve_once)
        result["PermissionDecisionApprovePermanently"] = to_class(PermissionDecisionApprovePermanently, self.permission_decision_approve_permanently)
        result["PermissionDecisionCancelled"] = to_class(PermissionDecisionCancelled, self.permission_decision_cancelled)
        result["PermissionDecisionDeniedByContentExclusionPolicy"] = to_class(PermissionDecisionDeniedByContentExclusionPolicy, self.permission_decision_denied_by_content_exclusion_policy)
        result["PermissionDecisionDeniedByPermissionRequestHook"] = to_class(PermissionDecisionDeniedByPermissionRequestHook, self.permission_decision_denied_by_permission_request_hook)
        result["PermissionDecisionDeniedByRules"] = to_class(PermissionDecisionDeniedByRules, self.permission_decision_denied_by_rules)
        result["PermissionDecisionDeniedInteractivelyByUser"] = to_class(PermissionDecisionDeniedInteractivelyByUser, self.permission_decision_denied_interactively_by_user)
        result["PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser"] = to_class(PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser, self.permission_decision_denied_no_approval_rule_and_could_not_request_from_user)
        result["PermissionDecisionReject"] = to_class(PermissionDecisionReject, self.permission_decision_reject)
        result["PermissionDecisionRequest"] = to_class(PermissionDecisionRequest, self.permission_decision_request)
        result["PermissionDecisionUserNotAvailable"] = to_class(PermissionDecisionUserNotAvailable, self.permission_decision_user_not_available)
        result["PermissionLocationAddToolApprovalParams"] = to_class(PermissionLocationAddToolApprovalParams, self.permission_location_add_tool_approval_params)
        result["PermissionLocationApplyParams"] = to_class(PermissionLocationApplyParams, self.permission_location_apply_params)
        result["PermissionLocationApplyResult"] = to_class(PermissionLocationApplyResult, self.permission_location_apply_result)
        result["PermissionLocationResolveParams"] = to_class(PermissionLocationResolveParams, self.permission_location_resolve_params)
        result["PermissionLocationResolveResult"] = to_class(PermissionLocationResolveResult, self.permission_location_resolve_result)
        result["PermissionLocationType"] = to_enum(PermissionLocationType, self.permission_location_type)
        result["PermissionPathsAddParams"] = to_class(PermissionPathsAddParams, self.permission_paths_add_params)
        result["PermissionPathsAllowedCheckParams"] = to_class(PermissionPathsAllowedCheckParams, self.permission_paths_allowed_check_params)
        result["PermissionPathsAllowedCheckResult"] = to_class(PermissionPathsAllowedCheckResult, self.permission_paths_allowed_check_result)
        result["PermissionPathsConfig"] = to_class(PermissionPathsConfig, self.permission_paths_config)
        result["PermissionPathsList"] = to_class(PermissionPathsList, self.permission_paths_list)
        result["PermissionPathsUpdatePrimaryParams"] = to_class(PermissionPathsUpdatePrimaryParams, self.permission_paths_update_primary_params)
        result["PermissionPathsWorkspaceCheckParams"] = to_class(PermissionPathsWorkspaceCheckParams, self.permission_paths_workspace_check_params)
        result["PermissionPathsWorkspaceCheckResult"] = to_class(PermissionPathsWorkspaceCheckResult, self.permission_paths_workspace_check_result)
        result["PermissionPromptShownNotification"] = to_class(PermissionPromptShownNotification, self.permission_prompt_shown_notification)
        result["PermissionRequestResult"] = to_class(PermissionRequestResult, self.permission_request_result)
        result["PermissionRulesSet"] = to_class(PermissionRulesSet, self.permission_rules_set)
        result["PermissionsConfigureAdditionalContentExclusionPolicy"] = to_class(PermissionsConfigureAdditionalContentExclusionPolicy, self.permissions_configure_additional_content_exclusion_policy)
        result["PermissionsConfigureAdditionalContentExclusionPolicyRule"] = to_class(PermissionsConfigureAdditionalContentExclusionPolicyRule, self.permissions_configure_additional_content_exclusion_policy_rule)
        result["PermissionsConfigureAdditionalContentExclusionPolicyRuleSource"] = to_class(PermissionsConfigureAdditionalContentExclusionPolicyRuleSource, self.permissions_configure_additional_content_exclusion_policy_rule_source)
        result["PermissionsConfigureAdditionalContentExclusionPolicyScope"] = to_enum(PermissionsConfigureAdditionalContentExclusionPolicyScope, self.permissions_configure_additional_content_exclusion_policy_scope)
        result["PermissionsConfigureParams"] = to_class(PermissionsConfigureParams, self.permissions_configure_params)
        result["PermissionsConfigureResult"] = to_class(PermissionsConfigureResult, self.permissions_configure_result)
        result["PermissionsFolderTrustAddTrustedResult"] = to_class(PermissionsFolderTrustAddTrustedResult, self.permissions_folder_trust_add_trusted_result)
        result["PermissionsLocationsAddToolApprovalDetails"] = (self.permissions_locations_add_tool_approval_details).to_dict()
        result["PermissionsLocationsAddToolApprovalDetailsCommands"] = to_class(PermissionsLocationsAddToolApprovalDetailsCommands, self.permissions_locations_add_tool_approval_details_commands)
        result["PermissionsLocationsAddToolApprovalDetailsCustomTool"] = to_class(PermissionsLocationsAddToolApprovalDetailsCustomTool, self.permissions_locations_add_tool_approval_details_custom_tool)
        result["PermissionsLocationsAddToolApprovalDetailsExtensionManagement"] = to_class(PermissionsLocationsAddToolApprovalDetailsExtensionManagement, self.permissions_locations_add_tool_approval_details_extension_management)
        result["PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess"] = to_class(PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess, self.permissions_locations_add_tool_approval_details_extension_permission_access)
        result["PermissionsLocationsAddToolApprovalDetailsMcp"] = to_class(PermissionsLocationsAddToolApprovalDetailsMCP, self.permissions_locations_add_tool_approval_details_mcp)
        result["PermissionsLocationsAddToolApprovalDetailsMcpSampling"] = to_class(PermissionsLocationsAddToolApprovalDetailsMCPSampling, self.permissions_locations_add_tool_approval_details_mcp_sampling)
        result["PermissionsLocationsAddToolApprovalDetailsMemory"] = to_class(PermissionsLocationsAddToolApprovalDetailsMemory, self.permissions_locations_add_tool_approval_details_memory)
        result["PermissionsLocationsAddToolApprovalDetailsRead"] = to_class(PermissionsLocationsAddToolApprovalDetailsRead, self.permissions_locations_add_tool_approval_details_read)
        result["PermissionsLocationsAddToolApprovalDetailsWrite"] = to_class(PermissionsLocationsAddToolApprovalDetailsWrite, self.permissions_locations_add_tool_approval_details_write)
        result["PermissionsLocationsAddToolApprovalResult"] = to_class(PermissionsLocationsAddToolApprovalResult, self.permissions_locations_add_tool_approval_result)
        result["PermissionsModifyRulesParams"] = to_class(PermissionsModifyRulesParams, self.permissions_modify_rules_params)
        result["PermissionsModifyRulesResult"] = to_class(PermissionsModifyRulesResult, self.permissions_modify_rules_result)
        result["PermissionsModifyRulesScope"] = to_enum(PermissionsModifyRulesScope, self.permissions_modify_rules_scope)
        result["PermissionsNotifyPromptShownResult"] = to_class(PermissionsNotifyPromptShownResult, self.permissions_notify_prompt_shown_result)
        result["PermissionsPathsAddResult"] = to_class(PermissionsPathsAddResult, self.permissions_paths_add_result)
        result["PermissionsPathsListRequest"] = to_class(PermissionsPathsListRequest, self.permissions_paths_list_request)
        result["PermissionsPathsUpdatePrimaryResult"] = to_class(PermissionsPathsUpdatePrimaryResult, self.permissions_paths_update_primary_result)
        result["PermissionsPendingRequestsRequest"] = to_class(PermissionsPendingRequestsRequest, self.permissions_pending_requests_request)
        result["PermissionsResetSessionApprovalsRequest"] = to_class(PermissionsResetSessionApprovalsRequest, self.permissions_reset_session_approvals_request)
        result["PermissionsResetSessionApprovalsResult"] = to_class(PermissionsResetSessionApprovalsResult, self.permissions_reset_session_approvals_result)
        result["PermissionsSetApproveAllRequest"] = to_class(PermissionsSetApproveAllRequest, self.permissions_set_approve_all_request)
        result["PermissionsSetApproveAllResult"] = to_class(PermissionsSetApproveAllResult, self.permissions_set_approve_all_result)
        result["PermissionsSetApproveAllSource"] = to_enum(PermissionsSetApproveAllSource, self.permissions_set_approve_all_source)
        result["PermissionsSetRequiredRequest"] = to_class(PermissionsSetRequiredRequest, self.permissions_set_required_request)
        result["PermissionsSetRequiredResult"] = to_class(PermissionsSetRequiredResult, self.permissions_set_required_result)
        result["PermissionsUrlsSetUnrestrictedModeResult"] = to_class(PermissionsUrlsSetUnrestrictedModeResult, self.permissions_urls_set_unrestricted_mode_result)
        result["PermissionUrlsConfig"] = to_class(PermissionUrlsConfig, self.permission_urls_config)
        result["PermissionUrlsSetUnrestrictedModeParams"] = to_class(PermissionUrlsSetUnrestrictedModeParams, self.permission_urls_set_unrestricted_mode_params)
        result["PingRequest"] = to_class(PingRequest, self.ping_request)
        result["PingResult"] = to_class(PingResult, self.ping_result)
        result["PlanReadResult"] = to_class(PlanReadResult, self.plan_read_result)
        result["PlanUpdateRequest"] = to_class(PlanUpdateRequest, self.plan_update_request)
        result["Plugin"] = to_class(Plugin, self.plugin)
        result["PluginList"] = to_class(PluginList, self.plugin_list)
        result["QueuedCommandHandled"] = to_class(QueuedCommandHandled, self.queued_command_handled)
        result["QueuedCommandNotHandled"] = to_class(QueuedCommandNotHandled, self.queued_command_not_handled)
        result["QueuedCommandResult"] = (self.queued_command_result).to_dict()
        result["QueuePendingItems"] = to_class(QueuePendingItems, self.queue_pending_items)
        result["QueuePendingItemsKind"] = to_enum(QueuePendingItemsKind, self.queue_pending_items_kind)
        result["QueuePendingItemsResult"] = to_class(QueuePendingItemsResult, self.queue_pending_items_result)
        result["QueueRemoveMostRecentResult"] = to_class(QueueRemoveMostRecentResult, self.queue_remove_most_recent_result)
        result["RegisterEventInterestParams"] = to_class(RegisterEventInterestParams, self.register_event_interest_params)
        result["RegisterEventInterestResult"] = to_class(RegisterEventInterestResult, self.register_event_interest_result)
        result["ReleaseEventInterestParams"] = to_class(ReleaseEventInterestParams, self.release_event_interest_params)
        result["RemoteEnableRequest"] = to_class(RemoteEnableRequest, self.remote_enable_request)
        result["RemoteEnableResult"] = to_class(RemoteEnableResult, self.remote_enable_result)
        result["RemoteNotifySteerableChangedRequest"] = to_class(RemoteNotifySteerableChangedRequest, self.remote_notify_steerable_changed_request)
        result["RemoteNotifySteerableChangedResult"] = to_class(RemoteNotifySteerableChangedResult, self.remote_notify_steerable_changed_result)
        result["RemoteSessionConnectionResult"] = to_class(RemoteSessionConnectionResult, self.remote_session_connection_result)
        result["RemoteSessionMode"] = to_enum(RemoteSessionMode, self.remote_session_mode)
        result["ScheduleEntry"] = to_class(ScheduleEntry, self.schedule_entry)
        result["ScheduleList"] = to_class(ScheduleList, self.schedule_list)
        result["ScheduleStopRequest"] = to_class(ScheduleStopRequest, self.schedule_stop_request)
        result["ScheduleStopResult"] = to_class(ScheduleStopResult, self.schedule_stop_result)
        result["SecretsAddFilterValuesRequest"] = to_class(SecretsAddFilterValuesRequest, self.secrets_add_filter_values_request)
        result["SecretsAddFilterValuesResult"] = to_class(SecretsAddFilterValuesResult, self.secrets_add_filter_values_result)
        result["SendAgentMode"] = to_enum(SendAgentMode, self.send_agent_mode)
        result["SendAttachment"] = (self.send_attachment).to_dict()
        result["SendAttachmentBlob"] = to_class(SendAttachmentBlob, self.send_attachment_blob)
        result["SendAttachmentDirectory"] = to_class(SendAttachmentDirectory, self.send_attachment_directory)
        result["SendAttachmentFile"] = to_class(SendAttachmentFile, self.send_attachment_file)
        result["SendAttachmentFileLineRange"] = to_class(SendAttachmentFileLineRange, self.send_attachment_file_line_range)
        result["SendAttachmentGithubReference"] = to_class(SendAttachmentGithubReference, self.send_attachment_github_reference)
        result["SendAttachmentGithubReferenceType"] = to_enum(SendAttachmentGithubReferenceTypeEnum, self.send_attachment_github_reference_type)
        result["SendAttachmentSelection"] = to_class(SendAttachmentSelection, self.send_attachment_selection)
        result["SendAttachmentSelectionDetails"] = to_class(SendAttachmentSelectionDetails, self.send_attachment_selection_details)
        result["SendAttachmentSelectionDetailsEnd"] = to_class(SendAttachmentSelectionDetailsEnd, self.send_attachment_selection_details_end)
        result["SendAttachmentSelectionDetailsStart"] = to_class(SendAttachmentSelectionDetailsStart, self.send_attachment_selection_details_start)
        result["SendMode"] = to_enum(SendMode, self.send_mode)
        result["SendRequest"] = to_class(SendRequest, self.send_request)
        result["SendResult"] = to_class(SendResult, self.send_result)
        result["ServerSkill"] = to_class(ServerSkill, self.server_skill)
        result["ServerSkillList"] = to_class(ServerSkillList, self.server_skill_list)
        result["SessionAuthStatus"] = to_class(SessionAuthStatus, self.session_auth_status)
        result["SessionBulkDeleteResult"] = to_class(SessionBulkDeleteResult, self.session_bulk_delete_result)
        result["SessionContext"] = to_class(SessionContext, self.session_context)
        result["SessionContextHostType"] = to_enum(HostType, self.session_context_host_type)
        result["SessionEnrichMetadataResult"] = to_class(SessionEnrichMetadataResult, self.session_enrich_metadata_result)
        result["SessionFsAppendFileRequest"] = to_class(SessionFSAppendFileRequest, self.session_fs_append_file_request)
        result["SessionFsError"] = to_class(SessionFSError, self.session_fs_error)
        result["SessionFsErrorCode"] = to_enum(SessionFSErrorCode, self.session_fs_error_code)
        result["SessionFsExistsRequest"] = to_class(SessionFSExistsRequest, self.session_fs_exists_request)
        result["SessionFsExistsResult"] = to_class(SessionFSExistsResult, self.session_fs_exists_result)
        result["SessionFsMkdirRequest"] = to_class(SessionFSMkdirRequest, self.session_fs_mkdir_request)
        result["SessionFsReaddirRequest"] = to_class(SessionFSReaddirRequest, self.session_fs_readdir_request)
        result["SessionFsReaddirResult"] = to_class(SessionFSReaddirResult, self.session_fs_readdir_result)
        result["SessionFsReaddirWithTypesEntry"] = to_class(SessionFSReaddirWithTypesEntry, self.session_fs_readdir_with_types_entry)
        result["SessionFsReaddirWithTypesEntryType"] = to_enum(SessionFSReaddirWithTypesEntryType, self.session_fs_readdir_with_types_entry_type)
        result["SessionFsReaddirWithTypesRequest"] = to_class(SessionFSReaddirWithTypesRequest, self.session_fs_readdir_with_types_request)
        result["SessionFsReaddirWithTypesResult"] = to_class(SessionFSReaddirWithTypesResult, self.session_fs_readdir_with_types_result)
        result["SessionFsReadFileRequest"] = to_class(SessionFSReadFileRequest, self.session_fs_read_file_request)
        result["SessionFsReadFileResult"] = to_class(SessionFSReadFileResult, self.session_fs_read_file_result)
        result["SessionFsRenameRequest"] = to_class(SessionFSRenameRequest, self.session_fs_rename_request)
        result["SessionFsRmRequest"] = to_class(SessionFSRmRequest, self.session_fs_rm_request)
        result["SessionFsSetProviderCapabilities"] = to_class(SessionFSSetProviderCapabilities, self.session_fs_set_provider_capabilities)
        result["SessionFsSetProviderConventions"] = to_enum(SessionFSSetProviderConventions, self.session_fs_set_provider_conventions)
        result["SessionFsSetProviderRequest"] = to_class(SessionFSSetProviderRequest, self.session_fs_set_provider_request)
        result["SessionFsSetProviderResult"] = to_class(SessionFSSetProviderResult, self.session_fs_set_provider_result)
        result["SessionFsSqliteExistsRequest"] = to_class(SessionFSSqliteExistsRequest, self.session_fs_sqlite_exists_request)
        result["SessionFsSqliteExistsResult"] = to_class(SessionFSSqliteExistsResult, self.session_fs_sqlite_exists_result)
        result["SessionFsSqliteQueryRequest"] = to_class(SessionFSSqliteQueryRequest, self.session_fs_sqlite_query_request)
        result["SessionFsSqliteQueryResult"] = to_class(SessionFSSqliteQueryResult, self.session_fs_sqlite_query_result)
        result["SessionFsSqliteQueryType"] = to_enum(SessionFSSqliteQueryType, self.session_fs_sqlite_query_type)
        result["SessionFsStatRequest"] = to_class(SessionFSStatRequest, self.session_fs_stat_request)
        result["SessionFsStatResult"] = to_class(SessionFSStatResult, self.session_fs_stat_result)
        result["SessionFsWriteFileRequest"] = to_class(SessionFSWriteFileRequest, self.session_fs_write_file_request)
        result["SessionInstalledPlugin"] = to_class(SessionInstalledPlugin, self.session_installed_plugin)
        result["SessionInstalledPluginSource"] = from_union([lambda x: to_class(SessionInstalledPluginSource, x), from_str], self.session_installed_plugin_source)
        result["SessionInstalledPluginSourceGithub"] = to_class(SessionInstalledPluginSourceGithub, self.session_installed_plugin_source_github)
        result["SessionInstalledPluginSourceLocal"] = to_class(SessionInstalledPluginSourceLocal, self.session_installed_plugin_source_local)
        result["SessionInstalledPluginSourceUrl"] = to_class(SessionInstalledPluginSourceURL, self.session_installed_plugin_source_url)
        result["SessionList"] = to_class(SessionList, self.session_list)
        result["SessionListFilter"] = to_class(SessionListFilter, self.session_list_filter)
        result["SessionLoadDeferredRepoHooksResult"] = to_class(SessionLoadDeferredRepoHooksResult, self.session_load_deferred_repo_hooks_result)
        result["SessionLogLevel"] = to_enum(SessionLogLevel, self.session_log_level)
        result["SessionMetadata"] = to_class(SessionMetadata, self.session_metadata)
        result["SessionMetadataSnapshot"] = to_class(SessionMetadataSnapshot, self.session_metadata_snapshot)
        result["SessionMode"] = to_enum(SessionMode, self.session_mode)
        result["SessionPruneResult"] = to_class(SessionPruneResult, self.session_prune_result)
        result["SessionsBulkDeleteRequest"] = to_class(SessionsBulkDeleteRequest, self.sessions_bulk_delete_request)
        result["SessionsCheckInUseRequest"] = to_class(SessionsCheckInUseRequest, self.sessions_check_in_use_request)
        result["SessionsCheckInUseResult"] = to_class(SessionsCheckInUseResult, self.sessions_check_in_use_result)
        result["SessionsCloseRequest"] = to_class(SessionsCloseRequest, self.sessions_close_request)
        result["SessionsCloseResult"] = to_class(SessionsCloseResult, self.sessions_close_result)
        result["SessionsEnrichMetadataRequest"] = to_class(SessionsEnrichMetadataRequest, self.sessions_enrich_metadata_request)
        result["SessionSetCredentialsParams"] = to_class(SessionSetCredentialsParams, self.session_set_credentials_params)
        result["SessionSetCredentialsResult"] = to_class(SessionSetCredentialsResult, self.session_set_credentials_result)
        result["SessionsFindByPrefixRequest"] = to_class(SessionsFindByPrefixRequest, self.sessions_find_by_prefix_request)
        result["SessionsFindByPrefixResult"] = to_class(SessionsFindByPrefixResult, self.sessions_find_by_prefix_result)
        result["SessionsFindByTaskIDRequest"] = to_class(SessionsFindByTaskIDRequest, self.sessions_find_by_task_id_request)
        result["SessionsFindByTaskIDResult"] = to_class(SessionsFindByTaskIDResult, self.sessions_find_by_task_id_result)
        result["SessionsForkRequest"] = to_class(SessionsForkRequest, self.sessions_fork_request)
        result["SessionsForkResult"] = to_class(SessionsForkResult, self.sessions_fork_result)
        result["SessionsGetEventFilePathRequest"] = to_class(SessionsGetEventFilePathRequest, self.sessions_get_event_file_path_request)
        result["SessionsGetEventFilePathResult"] = to_class(SessionsGetEventFilePathResult, self.sessions_get_event_file_path_result)
        result["SessionsGetLastForContextRequest"] = to_class(SessionsGetLastForContextRequest, self.sessions_get_last_for_context_request)
        result["SessionsGetLastForContextResult"] = to_class(SessionsGetLastForContextResult, self.sessions_get_last_for_context_result)
        result["SessionsGetPersistedRemoteSteerableRequest"] = to_class(SessionsGetPersistedRemoteSteerableRequest, self.sessions_get_persisted_remote_steerable_request)
        result["SessionsGetPersistedRemoteSteerableResult"] = to_class(SessionsGetPersistedRemoteSteerableResult, self.sessions_get_persisted_remote_steerable_result)
        result["SessionSizes"] = to_class(SessionSizes, self.session_sizes)
        result["SessionsListRequest"] = to_class(SessionsListRequest, self.sessions_list_request)
        result["SessionsLoadDeferredRepoHooksRequest"] = to_class(SessionsLoadDeferredRepoHooksRequest, self.sessions_load_deferred_repo_hooks_request)
        result["SessionsPruneOldRequest"] = to_class(SessionsPruneOldRequest, self.sessions_prune_old_request)
        result["SessionsReleaseLockRequest"] = to_class(SessionsReleaseLockRequest, self.sessions_release_lock_request)
        result["SessionsReleaseLockResult"] = to_class(SessionsReleaseLockResult, self.sessions_release_lock_result)
        result["SessionsReloadPluginHooksRequest"] = to_class(SessionsReloadPluginHooksRequest, self.sessions_reload_plugin_hooks_request)
        result["SessionsReloadPluginHooksResult"] = to_class(SessionsReloadPluginHooksResult, self.sessions_reload_plugin_hooks_result)
        result["SessionsSaveRequest"] = to_class(SessionsSaveRequest, self.sessions_save_request)
        result["SessionsSaveResult"] = to_class(SessionsSaveResult, self.sessions_save_result)
        result["SessionsSetAdditionalPluginsRequest"] = to_class(SessionsSetAdditionalPluginsRequest, self.sessions_set_additional_plugins_request)
        result["SessionsSetAdditionalPluginsResult"] = to_class(SessionsSetAdditionalPluginsResult, self.sessions_set_additional_plugins_result)
        result["SessionUpdateOptionsParams"] = to_class(SessionUpdateOptionsParams, self.session_update_options_params)
        result["SessionUpdateOptionsResult"] = to_class(SessionUpdateOptionsResult, self.session_update_options_result)
        result["SessionWorkingDirectoryContext"] = to_class(SessionWorkingDirectoryContext, self.session_working_directory_context)
        result["SessionWorkingDirectoryContextHostType"] = to_enum(HostType, self.session_working_directory_context_host_type)
        result["ShellExecRequest"] = to_class(ShellExecRequest, self.shell_exec_request)
        result["ShellExecResult"] = to_class(ShellExecResult, self.shell_exec_result)
        result["ShellKillRequest"] = to_class(ShellKillRequest, self.shell_kill_request)
        result["ShellKillResult"] = to_class(ShellKillResult, self.shell_kill_result)
        result["ShellKillSignal"] = to_enum(ShellKillSignal, self.shell_kill_signal)
        result["ShutdownRequest"] = to_class(ShutdownRequest, self.shutdown_request)
        result["Skill"] = to_class(Skill, self.skill)
        result["SkillList"] = to_class(SkillList, self.skill_list)
        result["SkillsConfigSetDisabledSkillsRequest"] = to_class(SkillsConfigSetDisabledSkillsRequest, self.skills_config_set_disabled_skills_request)
        result["SkillsDisableRequest"] = to_class(SkillsDisableRequest, self.skills_disable_request)
        result["SkillsDiscoverRequest"] = to_class(SkillsDiscoverRequest, self.skills_discover_request)
        result["SkillsEnableRequest"] = to_class(SkillsEnableRequest, self.skills_enable_request)
        result["SkillsGetInvokedResult"] = to_class(SkillsGetInvokedResult, self.skills_get_invoked_result)
        result["SkillsInvokedSkill"] = to_class(SkillsInvokedSkill, self.skills_invoked_skill)
        result["SkillsLoadDiagnostics"] = to_class(SkillsLoadDiagnostics, self.skills_load_diagnostics)
        result["SlashCommandAgentPromptResult"] = to_class(SlashCommandAgentPromptResult, self.slash_command_agent_prompt_result)
        result["SlashCommandCompletedResult"] = to_class(SlashCommandCompletedResult, self.slash_command_completed_result)
        result["SlashCommandInfo"] = to_class(SlashCommandInfo, self.slash_command_info)
        result["SlashCommandInput"] = to_class(SlashCommandInput, self.slash_command_input)
        result["SlashCommandInputCompletion"] = to_enum(SlashCommandInputCompletion, self.slash_command_input_completion)
        result["SlashCommandInvocationResult"] = (self.slash_command_invocation_result).to_dict()
        result["SlashCommandKind"] = to_enum(SlashCommandKind, self.slash_command_kind)
        result["SlashCommandSelectSubcommandOption"] = to_class(SlashCommandSelectSubcommandOption, self.slash_command_select_subcommand_option)
        result["SlashCommandSelectSubcommandResult"] = to_class(SlashCommandSelectSubcommandResult, self.slash_command_select_subcommand_result)
        result["SlashCommandTextResult"] = to_class(SlashCommandTextResult, self.slash_command_text_result)
        result["TaskAgentInfo"] = to_class(TaskAgentInfo, self.task_agent_info)
        result["TaskAgentProgress"] = to_class(TaskAgentProgress, self.task_agent_progress)
        result["TaskExecutionMode"] = to_enum(TaskExecutionMode, self.task_execution_mode)
        result["TaskInfo"] = (self.task_info).to_dict()
        result["TaskList"] = to_class(TaskList, self.task_list)
        result["TaskProgressLine"] = to_class(TaskProgressLine, self.task_progress_line)
        result["TasksCancelRequest"] = to_class(TasksCancelRequest, self.tasks_cancel_request)
        result["TasksCancelResult"] = to_class(TasksCancelResult, self.tasks_cancel_result)
        result["TasksGetCurrentPromotableResult"] = to_class(TasksGetCurrentPromotableResult, self.tasks_get_current_promotable_result)
        result["TasksGetProgressRequest"] = to_class(TasksGetProgressRequest, self.tasks_get_progress_request)
        result["TasksGetProgressResult"] = to_class(TasksGetProgressResult, self.tasks_get_progress_result)
        result["TaskShellInfo"] = to_class(TaskShellInfo, self.task_shell_info)
        result["TaskShellInfoAttachmentMode"] = to_enum(TaskShellInfoAttachmentMode, self.task_shell_info_attachment_mode)
        result["TaskShellProgress"] = to_class(TaskShellProgress, self.task_shell_progress)
        result["TasksPromoteCurrentToBackgroundResult"] = to_class(TasksPromoteCurrentToBackgroundResult, self.tasks_promote_current_to_background_result)
        result["TasksPromoteToBackgroundRequest"] = to_class(TasksPromoteToBackgroundRequest, self.tasks_promote_to_background_request)
        result["TasksPromoteToBackgroundResult"] = to_class(TasksPromoteToBackgroundResult, self.tasks_promote_to_background_result)
        result["TasksRefreshResult"] = to_class(TasksRefreshResult, self.tasks_refresh_result)
        result["TasksRemoveRequest"] = to_class(TasksRemoveRequest, self.tasks_remove_request)
        result["TasksRemoveResult"] = to_class(TasksRemoveResult, self.tasks_remove_result)
        result["TasksSendMessageRequest"] = to_class(TasksSendMessageRequest, self.tasks_send_message_request)
        result["TasksSendMessageResult"] = to_class(TasksSendMessageResult, self.tasks_send_message_result)
        result["TasksStartAgentRequest"] = to_class(TasksStartAgentRequest, self.tasks_start_agent_request)
        result["TasksStartAgentResult"] = to_class(TasksStartAgentResult, self.tasks_start_agent_result)
        result["TaskStatus"] = to_enum(TaskStatus, self.task_status)
        result["TasksWaitForPendingResult"] = to_class(TasksWaitForPendingResult, self.tasks_wait_for_pending_result)
        result["TelemetrySetFeatureOverridesRequest"] = to_class(TelemetrySetFeatureOverridesRequest, self.telemetry_set_feature_overrides_request)
        result["TokenAuthInfo"] = to_class(TokenAuthInfo, self.token_auth_info)
        result["Tool"] = to_class(Tool, self.tool)
        result["ToolList"] = to_class(ToolList, self.tool_list)
        result["ToolsInitializeAndValidateResult"] = to_class(ToolsInitializeAndValidateResult, self.tools_initialize_and_validate_result)
        result["ToolsListRequest"] = to_class(ToolsListRequest, self.tools_list_request)
        result["UIAutoModeSwitchResponse"] = to_enum(UIAutoModeSwitchResponse, self.ui_auto_mode_switch_response)
        result["UIElicitationArrayAnyOfField"] = to_class(UIElicitationArrayAnyOfField, self.ui_elicitation_array_any_of_field)
        result["UIElicitationArrayAnyOfFieldItems"] = to_class(UIElicitationArrayAnyOfFieldItems, self.ui_elicitation_array_any_of_field_items)
        result["UIElicitationArrayAnyOfFieldItemsAnyOf"] = to_class(UIElicitationArrayAnyOfFieldItemsAnyOf, self.ui_elicitation_array_any_of_field_items_any_of)
        result["UIElicitationArrayEnumField"] = to_class(UIElicitationArrayEnumField, self.ui_elicitation_array_enum_field)
        result["UIElicitationArrayEnumFieldItems"] = to_class(UIElicitationArrayEnumFieldItems, self.ui_elicitation_array_enum_field_items)
        result["UIElicitationFieldValue"] = from_union([to_float, from_bool, lambda x: from_list(from_str, x), from_str], self.ui_elicitation_field_value)
        result["UIElicitationRequest"] = to_class(UIElicitationRequest, self.ui_elicitation_request)
        result["UIElicitationResponse"] = to_class(UIElicitationResponse, self.ui_elicitation_response)
        result["UIElicitationResponseAction"] = to_enum(UIElicitationResponseAction, self.ui_elicitation_response_action)
        result["UIElicitationResponseContent"] = from_dict(lambda x: from_union([to_float, from_bool, lambda x: from_list(from_str, x), from_str], x), self.ui_elicitation_response_content)
        result["UIElicitationResult"] = to_class(UIElicitationResult, self.ui_elicitation_result)
        result["UIElicitationSchema"] = to_class(UIElicitationSchema, self.ui_elicitation_schema)
        result["UIElicitationSchemaProperty"] = to_class(UIElicitationSchemaProperty, self.ui_elicitation_schema_property)
        result["UIElicitationSchemaPropertyBoolean"] = to_class(UIElicitationSchemaPropertyBoolean, self.ui_elicitation_schema_property_boolean)
        result["UIElicitationSchemaPropertyNumber"] = to_class(UIElicitationSchemaPropertyNumber, self.ui_elicitation_schema_property_number)
        result["UIElicitationSchemaPropertyNumberType"] = to_enum(UIElicitationSchemaPropertyNumberType, self.ui_elicitation_schema_property_number_type)
        result["UIElicitationSchemaPropertyString"] = to_class(UIElicitationSchemaPropertyString, self.ui_elicitation_schema_property_string)
        result["UIElicitationSchemaPropertyStringFormat"] = to_enum(UIElicitationSchemaPropertyStringFormat, self.ui_elicitation_schema_property_string_format)
        result["UIElicitationStringEnumField"] = to_class(UIElicitationStringEnumField, self.ui_elicitation_string_enum_field)
        result["UIElicitationStringOneOfField"] = to_class(UIElicitationStringOneOfField, self.ui_elicitation_string_one_of_field)
        result["UIElicitationStringOneOfFieldOneOf"] = to_class(UIElicitationStringOneOfFieldOneOf, self.ui_elicitation_string_one_of_field_one_of)
        result["UIExitPlanModeAction"] = to_enum(UIExitPlanModeAction, self.ui_exit_plan_mode_action)
        result["UIExitPlanModeResponse"] = to_class(UIExitPlanModeResponse, self.ui_exit_plan_mode_response)
        result["UIHandlePendingAutoModeSwitchRequest"] = to_class(UIHandlePendingAutoModeSwitchRequest, self.ui_handle_pending_auto_mode_switch_request)
        result["UIHandlePendingElicitationRequest"] = to_class(UIHandlePendingElicitationRequest, self.ui_handle_pending_elicitation_request)
        result["UIHandlePendingExitPlanModeRequest"] = to_class(UIHandlePendingExitPlanModeRequest, self.ui_handle_pending_exit_plan_mode_request)
        result["UIHandlePendingResult"] = to_class(UIHandlePendingResult, self.ui_handle_pending_result)
        result["UIHandlePendingSamplingRequest"] = to_class(UIHandlePendingSamplingRequest, self.ui_handle_pending_sampling_request)
        result["UIHandlePendingSamplingResponse"] = from_dict(lambda x: x, self.ui_handle_pending_sampling_response)
        result["UIHandlePendingUserInputRequest"] = to_class(UIHandlePendingUserInputRequest, self.ui_handle_pending_user_input_request)
        result["UIRegisterDirectAutoModeSwitchHandlerResult"] = to_class(UIRegisterDirectAutoModeSwitchHandlerResult, self.ui_register_direct_auto_mode_switch_handler_result)
        result["UIUnregisterDirectAutoModeSwitchHandlerRequest"] = to_class(UIUnregisterDirectAutoModeSwitchHandlerRequest, self.ui_unregister_direct_auto_mode_switch_handler_request)
        result["UIUnregisterDirectAutoModeSwitchHandlerResult"] = to_class(UIUnregisterDirectAutoModeSwitchHandlerResult, self.ui_unregister_direct_auto_mode_switch_handler_result)
        result["UIUserInputResponse"] = to_class(UIUserInputResponse, self.ui_user_input_response)
        result["UsageGetMetricsResult"] = to_class(UsageGetMetricsResult, self.usage_get_metrics_result)
        result["UsageMetricsCodeChanges"] = to_class(UsageMetricsCodeChanges, self.usage_metrics_code_changes)
        result["UsageMetricsModelMetric"] = to_class(UsageMetricsModelMetric, self.usage_metrics_model_metric)
        result["UsageMetricsModelMetricRequests"] = to_class(UsageMetricsModelMetricRequests, self.usage_metrics_model_metric_requests)
        result["UsageMetricsModelMetricTokenDetail"] = to_class(UsageMetricsModelMetricTokenDetail, self.usage_metrics_model_metric_token_detail)
        result["UsageMetricsModelMetricUsage"] = to_class(UsageMetricsModelMetricUsage, self.usage_metrics_model_metric_usage)
        result["UsageMetricsTokenDetail"] = to_class(UsageMetricsTokenDetail, self.usage_metrics_token_detail)
        result["UserAuthInfo"] = to_class(UserAuthInfo, self.user_auth_info)
        result["WorkspacesCheckpoints"] = to_class(WorkspacesCheckpoints, self.workspaces_checkpoints)
        result["WorkspacesCreateFileRequest"] = to_class(WorkspacesCreateFileRequest, self.workspaces_create_file_request)
        result["WorkspacesGetWorkspaceResult"] = to_class(WorkspacesGetWorkspaceResult, self.workspaces_get_workspace_result)
        result["WorkspacesListCheckpointsResult"] = to_class(WorkspacesListCheckpointsResult, self.workspaces_list_checkpoints_result)
        result["WorkspacesListFilesResult"] = to_class(WorkspacesListFilesResult, self.workspaces_list_files_result)
        result["WorkspacesReadCheckpointRequest"] = to_class(WorkspacesReadCheckpointRequest, self.workspaces_read_checkpoint_request)
        result["WorkspacesReadCheckpointResult"] = to_class(WorkspacesReadCheckpointResult, self.workspaces_read_checkpoint_result)
        result["WorkspacesReadFileRequest"] = to_class(WorkspacesReadFileRequest, self.workspaces_read_file_request)
        result["WorkspacesReadFileResult"] = to_class(WorkspacesReadFileResult, self.workspaces_read_file_result)
        result["WorkspacesSaveLargePasteRequest"] = to_class(WorkspacesSaveLargePasteRequest, self.workspaces_save_large_paste_request)
        result["WorkspacesSaveLargePasteResult"] = to_class(WorkspacesSaveLargePasteResult, self.workspaces_save_large_paste_result)
        result["WorkspaceSummaryHostType"] = to_enum(HostType, self.workspace_summary_host_type)
        result["WorkspacesWorkspaceDetailsHostType"] = to_enum(HostType, self.workspaces_workspace_details_host_type)
        result["SessionContextInfo"] = from_union([lambda x: to_class(SessionContextInfo, x), from_none], self.session_context_info)
        result["TaskProgress"] = from_union([lambda x: to_class(TaskProgress, x), from_none], self.task_progress)
        result["WorkspaceSummary"] = from_union([lambda x: to_class(WorkspaceSummary, x), from_none], self.workspace_summary)
        return result

def rpc_from_dict(s: Any) -> RPC:
    return RPC.from_dict(s)

def rpc_to_dict(x: RPC) -> Any:
    return to_class(RPC, x)

# The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime stores the value verbatim and uses it for outbound model/API requests; it does NOT re-validate or re-fetch the associated Copilot user response. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.
AuthInfo = HMACAuthInfo | EnvAuthInfo | TokenAuthInfo | CopilotAPITokenAuthInfo | UserAuthInfo | GhCLIAuthInfo | APIKeyAuthInfo

def _load_AuthInfo(obj: Any) -> AuthInfo:
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "hmac": return HMACAuthInfo.from_dict(obj)
        case "env": return EnvAuthInfo.from_dict(obj)
        case "token": return TokenAuthInfo.from_dict(obj)
        case "copilot-api-token": return CopilotAPITokenAuthInfo.from_dict(obj)
        case "user": return UserAuthInfo.from_dict(obj)
        case "gh-cli": return GhCLIAuthInfo.from_dict(obj)
        case "api-key": return APIKeyAuthInfo.from_dict(obj)
        case _: raise ValueError(f"Unknown AuthInfo type: {kind!r}")

# A content block within a tool result, which may be text, terminal output, image, audio, or a resource
ExternalToolTextResultForLlmContent = ExternalToolTextResultForLlmContentText | ExternalToolTextResultForLlmContentTerminal | ExternalToolTextResultForLlmContentImage | ExternalToolTextResultForLlmContentAudio | ExternalToolTextResultForLlmContentResourceLink | ExternalToolTextResultForLlmContentResource

def _load_ExternalToolTextResultForLlmContent(obj: Any) -> ExternalToolTextResultForLlmContent:
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "text": return ExternalToolTextResultForLlmContentText.from_dict(obj)
        case "terminal": return ExternalToolTextResultForLlmContentTerminal.from_dict(obj)
        case "image": return ExternalToolTextResultForLlmContentImage.from_dict(obj)
        case "audio": return ExternalToolTextResultForLlmContentAudio.from_dict(obj)
        case "resource_link": return ExternalToolTextResultForLlmContentResourceLink.from_dict(obj)
        case "resource": return ExternalToolTextResultForLlmContentResource.from_dict(obj)
        case _: raise ValueError(f"Unknown ExternalToolTextResultForLlmContent type: {kind!r}")

# The client's response to the pending permission prompt
PermissionDecision = PermissionDecisionApproveOnce | PermissionDecisionApproveForSession | PermissionDecisionApproveForLocation | PermissionDecisionApprovePermanently | PermissionDecisionReject | PermissionDecisionUserNotAvailable | PermissionDecisionApproved | PermissionDecisionApprovedForSession | PermissionDecisionApprovedForLocation | PermissionDecisionCancelled | PermissionDecisionDeniedByRules | PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser | PermissionDecisionDeniedInteractivelyByUser | PermissionDecisionDeniedByContentExclusionPolicy | PermissionDecisionDeniedByPermissionRequestHook

def _load_PermissionDecision(obj: Any) -> PermissionDecision:
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "approve-once": return PermissionDecisionApproveOnce.from_dict(obj)
        case "approve-for-session": return PermissionDecisionApproveForSession.from_dict(obj)
        case "approve-for-location": return PermissionDecisionApproveForLocation.from_dict(obj)
        case "approve-permanently": return PermissionDecisionApprovePermanently.from_dict(obj)
        case "reject": return PermissionDecisionReject.from_dict(obj)
        case "user-not-available": return PermissionDecisionUserNotAvailable.from_dict(obj)
        case "approved": return PermissionDecisionApproved.from_dict(obj)
        case "approved-for-session": return PermissionDecisionApprovedForSession.from_dict(obj)
        case "approved-for-location": return PermissionDecisionApprovedForLocation.from_dict(obj)
        case "cancelled": return PermissionDecisionCancelled.from_dict(obj)
        case "denied-by-rules": return PermissionDecisionDeniedByRules.from_dict(obj)
        case "denied-no-approval-rule-and-could-not-request-from-user": return PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser.from_dict(obj)
        case "denied-interactively-by-user": return PermissionDecisionDeniedInteractivelyByUser.from_dict(obj)
        case "denied-by-content-exclusion-policy": return PermissionDecisionDeniedByContentExclusionPolicy.from_dict(obj)
        case "denied-by-permission-request-hook": return PermissionDecisionDeniedByPermissionRequestHook.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionDecision kind: {kind!r}")

# Approval to persist for this location
PermissionDecisionApproveForLocationApproval = PermissionDecisionApproveForLocationApprovalCommands | PermissionDecisionApproveForLocationApprovalRead | PermissionDecisionApproveForLocationApprovalWrite | PermissionDecisionApproveForLocationApprovalMCP | PermissionDecisionApproveForLocationApprovalMCPSampling | PermissionDecisionApproveForLocationApprovalMemory | PermissionDecisionApproveForLocationApprovalCustomTool | PermissionDecisionApproveForLocationApprovalExtensionManagement | PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess

def _load_PermissionDecisionApproveForLocationApproval(obj: Any) -> PermissionDecisionApproveForLocationApproval:
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "commands": return PermissionDecisionApproveForLocationApprovalCommands.from_dict(obj)
        case "read": return PermissionDecisionApproveForLocationApprovalRead.from_dict(obj)
        case "write": return PermissionDecisionApproveForLocationApprovalWrite.from_dict(obj)
        case "mcp": return PermissionDecisionApproveForLocationApprovalMCP.from_dict(obj)
        case "mcp-sampling": return PermissionDecisionApproveForLocationApprovalMCPSampling.from_dict(obj)
        case "memory": return PermissionDecisionApproveForLocationApprovalMemory.from_dict(obj)
        case "custom-tool": return PermissionDecisionApproveForLocationApprovalCustomTool.from_dict(obj)
        case "extension-management": return PermissionDecisionApproveForLocationApprovalExtensionManagement.from_dict(obj)
        case "extension-permission-access": return PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionDecisionApproveForLocationApproval kind: {kind!r}")

# Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
PermissionDecisionApproveForSessionApproval = PermissionDecisionApproveForSessionApprovalCommands | PermissionDecisionApproveForSessionApprovalRead | PermissionDecisionApproveForSessionApprovalWrite | PermissionDecisionApproveForSessionApprovalMCP | PermissionDecisionApproveForSessionApprovalMCPSampling | PermissionDecisionApproveForSessionApprovalMemory | PermissionDecisionApproveForSessionApprovalCustomTool | PermissionDecisionApproveForSessionApprovalExtensionManagement | PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess

def _load_PermissionDecisionApproveForSessionApproval(obj: Any) -> PermissionDecisionApproveForSessionApproval:
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "commands": return PermissionDecisionApproveForSessionApprovalCommands.from_dict(obj)
        case "read": return PermissionDecisionApproveForSessionApprovalRead.from_dict(obj)
        case "write": return PermissionDecisionApproveForSessionApprovalWrite.from_dict(obj)
        case "mcp": return PermissionDecisionApproveForSessionApprovalMCP.from_dict(obj)
        case "mcp-sampling": return PermissionDecisionApproveForSessionApprovalMCPSampling.from_dict(obj)
        case "memory": return PermissionDecisionApproveForSessionApprovalMemory.from_dict(obj)
        case "custom-tool": return PermissionDecisionApproveForSessionApprovalCustomTool.from_dict(obj)
        case "extension-management": return PermissionDecisionApproveForSessionApprovalExtensionManagement.from_dict(obj)
        case "extension-permission-access": return PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionDecisionApproveForSessionApproval kind: {kind!r}")

# Tool approval to persist and apply
PermissionsLocationsAddToolApprovalDetails = PermissionsLocationsAddToolApprovalDetailsCommands | PermissionsLocationsAddToolApprovalDetailsRead | PermissionsLocationsAddToolApprovalDetailsWrite | PermissionsLocationsAddToolApprovalDetailsMCP | PermissionsLocationsAddToolApprovalDetailsMCPSampling | PermissionsLocationsAddToolApprovalDetailsMemory | PermissionsLocationsAddToolApprovalDetailsCustomTool | PermissionsLocationsAddToolApprovalDetailsExtensionManagement | PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess

def _load_PermissionsLocationsAddToolApprovalDetails(obj: Any) -> PermissionsLocationsAddToolApprovalDetails:
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "commands": return PermissionsLocationsAddToolApprovalDetailsCommands.from_dict(obj)
        case "read": return PermissionsLocationsAddToolApprovalDetailsRead.from_dict(obj)
        case "write": return PermissionsLocationsAddToolApprovalDetailsWrite.from_dict(obj)
        case "mcp": return PermissionsLocationsAddToolApprovalDetailsMCP.from_dict(obj)
        case "mcp-sampling": return PermissionsLocationsAddToolApprovalDetailsMCPSampling.from_dict(obj)
        case "memory": return PermissionsLocationsAddToolApprovalDetailsMemory.from_dict(obj)
        case "custom-tool": return PermissionsLocationsAddToolApprovalDetailsCustomTool.from_dict(obj)
        case "extension-management": return PermissionsLocationsAddToolApprovalDetailsExtensionManagement.from_dict(obj)
        case "extension-permission-access": return PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionsLocationsAddToolApprovalDetails kind: {kind!r}")

# Result of the queued command execution.
QueuedCommandResult = QueuedCommandHandled | QueuedCommandNotHandled

def _load_QueuedCommandResult(obj: Any) -> QueuedCommandResult:
    assert isinstance(obj, dict)
    kind = obj.get("handled")
    match kind:
        case "true": return QueuedCommandHandled.from_dict(obj)
        case "false": return QueuedCommandNotHandled.from_dict(obj)
        case _: raise ValueError(f"Unknown QueuedCommandResult handled: {kind!r}")

# A user message attachment — a file, directory, code selection, blob, or GitHub reference
SendAttachment = SendAttachmentFile | SendAttachmentDirectory | SendAttachmentSelection | SendAttachmentGithubReference | SendAttachmentBlob

def _load_SendAttachment(obj: Any) -> SendAttachment:
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "file": return SendAttachmentFile.from_dict(obj)
        case "directory": return SendAttachmentDirectory.from_dict(obj)
        case "selection": return SendAttachmentSelection.from_dict(obj)
        case "github_reference": return SendAttachmentGithubReference.from_dict(obj)
        case "blob": return SendAttachmentBlob.from_dict(obj)
        case _: raise ValueError(f"Unknown SendAttachment type: {kind!r}")

# Result of invoking the slash command (text output, prompt to send to the agent, or completion).
SlashCommandInvocationResult = SlashCommandTextResult | SlashCommandAgentPromptResult | SlashCommandCompletedResult | SlashCommandSelectSubcommandResult

def _load_SlashCommandInvocationResult(obj: Any) -> SlashCommandInvocationResult:
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "text": return SlashCommandTextResult.from_dict(obj)
        case "agent-prompt": return SlashCommandAgentPromptResult.from_dict(obj)
        case "completed": return SlashCommandCompletedResult.from_dict(obj)
        case "select-subcommand": return SlashCommandSelectSubcommandResult.from_dict(obj)
        case _: raise ValueError(f"Unknown SlashCommandInvocationResult kind: {kind!r}")

# Schema for the `TaskInfo` type.
TaskInfo = TaskAgentInfo | TaskShellInfo

def _load_TaskInfo(obj: Any) -> TaskInfo:
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "agent": return TaskAgentInfo.from_dict(obj)
        case "shell": return TaskShellInfo.from_dict(obj)
        case _: raise ValueError(f"Unknown TaskInfo type: {kind!r}")


ExternalToolResult = ExternalToolTextResultForLlm
FilterMapping = dict
McpExecuteSamplingRequest = dict
McpExecuteSamplingResult = dict
OptionsUpdateEnvValueMode = MCPSetEnvValueModeDetails
SessionContextHostType = HostType
SessionWorkingDirectoryContextHostType = HostType
TaskInfoExecutionMode = TaskExecutionMode
TaskInfoStatus = TaskStatus
WorkspaceSummaryHostType = HostType
WorkspacesWorkspaceDetailsHostType = HostType

def _timeout_kwargs(timeout: float | None) -> dict:
    """Build keyword arguments for optional timeout forwarding."""
    if timeout is not None:
        return {"timeout": timeout}
    return {}

def _patch_model_capabilities(data: dict) -> dict:
    """Ensure model capabilities have required fields.

    TODO: Remove once the runtime schema correctly marks these fields as optional.
    Some models (e.g. embedding models) may omit 'limits' or 'supports' in their
    capabilities, or omit 'max_context_window_tokens' within limits. The generated
    deserializer requires these fields, so we supply defaults here.
    """
    for model in data.get("models", []):
        caps = model.get("capabilities")
        if caps is None:
            model["capabilities"] = {"supports": {}, "limits": {"max_context_window_tokens": 0}}
            continue
        if "supports" not in caps:
            caps["supports"] = {}
        if "limits" not in caps:
            caps["limits"] = {"max_context_window_tokens": 0}
        elif "max_context_window_tokens" not in caps["limits"]:
            caps["limits"]["max_context_window_tokens"] = 0
    return data


class ServerModelsApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def list(self, params: ModelsListRequest, *, timeout: float | None = None) -> ModelList:
        "Lists Copilot models available to the authenticated user.\n\nArgs:\n    params: Optional GitHub token used to list models for a specific user instead of the global auth context.\n\nReturns:\n    List of Copilot models available to the resolved user, including capabilities and billing metadata."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ModelList.from_dict(_patch_model_capabilities(await self._client.request("models.list", params_dict, **_timeout_kwargs(timeout))))


class ServerToolsApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def list(self, params: ToolsListRequest, *, timeout: float | None = None) -> ToolList:
        "Lists built-in tools available for a model.\n\nArgs:\n    params: Optional model identifier whose tool overrides should be applied to the listing.\n\nReturns:\n    Built-in tools available for the requested model, with their parameters and instructions."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ToolList.from_dict(await self._client.request("tools.list", params_dict, **_timeout_kwargs(timeout)))


class ServerAccountApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def get_quota(self, params: AccountGetQuotaRequest, *, timeout: float | None = None) -> AccountGetQuotaResult:
        "Gets Copilot quota usage for the authenticated user or supplied GitHub token.\n\nArgs:\n    params: Optional GitHub token used to look up quota for a specific user instead of the global auth context.\n\nReturns:\n    Quota usage snapshots for the resolved user, keyed by quota type."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return AccountGetQuotaResult.from_dict(await self._client.request("account.getQuota", params_dict, **_timeout_kwargs(timeout)))


class ServerSecretsApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def add_filter_values(self, params: SecretsAddFilterValuesRequest, *, timeout: float | None = None) -> SecretsAddFilterValuesResult:
        "Registers secret values for redaction in session logs and exports. The SDK calls this to inject dynamically generated secret values (e.g., OIDC tokens).\n\nArgs:\n    params: Secret values to add to the redaction filter.\n\nReturns:\n    Confirmation that the secret values were registered."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SecretsAddFilterValuesResult.from_dict(await self._client.request("secrets.addFilterValues", params_dict, **_timeout_kwargs(timeout)))


class ServerMcpConfigApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def list(self, *, timeout: float | None = None) -> MCPConfigList:
        "Lists MCP servers from user configuration.\n\nReturns:\n    User-configured MCP servers, keyed by server name."
        return MCPConfigList.from_dict(await self._client.request("mcp.config.list", {}, **_timeout_kwargs(timeout)))

    async def add(self, params: MCPConfigAddRequest, *, timeout: float | None = None) -> None:
        "Adds an MCP server to user configuration.\n\nArgs:\n    params: MCP server name and configuration to add to user configuration."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.add", params_dict, **_timeout_kwargs(timeout))

    async def update(self, params: MCPConfigUpdateRequest, *, timeout: float | None = None) -> None:
        "Updates an MCP server in user configuration.\n\nArgs:\n    params: MCP server name and replacement configuration to write to user configuration."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.update", params_dict, **_timeout_kwargs(timeout))

    async def remove(self, params: MCPConfigRemoveRequest, *, timeout: float | None = None) -> None:
        "Removes an MCP server from user configuration.\n\nArgs:\n    params: MCP server name to remove from user configuration."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.remove", params_dict, **_timeout_kwargs(timeout))

    async def enable(self, params: MCPConfigEnableRequest, *, timeout: float | None = None) -> None:
        "Enables MCP servers in user configuration for new sessions.\n\nArgs:\n    params: MCP server names to enable for new sessions."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: MCPConfigDisableRequest, *, timeout: float | None = None) -> None:
        "Disables MCP servers in user configuration for new sessions.\n\nArgs:\n    params: MCP server names to disable for new sessions."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.disable", params_dict, **_timeout_kwargs(timeout))


class ServerMcpApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client
        self.config = ServerMcpConfigApi(client)

    async def discover(self, params: MCPDiscoverRequest, *, timeout: float | None = None) -> MCPDiscoverResult:
        "Discovers MCP servers from user, workspace, plugin, and builtin sources.\n\nArgs:\n    params: Optional working directory used as context for MCP server discovery.\n\nReturns:\n    MCP servers discovered from user, workspace, plugin, and built-in sources."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return MCPDiscoverResult.from_dict(await self._client.request("mcp.discover", params_dict, **_timeout_kwargs(timeout)))


class ServerSkillsConfigApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def set_disabled_skills(self, params: SkillsConfigSetDisabledSkillsRequest, *, timeout: float | None = None) -> None:
        "Replaces the global list of disabled skills.\n\nArgs:\n    params: Skill names to mark as disabled in global configuration, replacing any previous list."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("skills.config.setDisabledSkills", params_dict, **_timeout_kwargs(timeout))


class ServerSkillsApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client
        self.config = ServerSkillsConfigApi(client)

    async def discover(self, params: SkillsDiscoverRequest, *, timeout: float | None = None) -> ServerSkillList:
        "Discovers skills across global and project sources.\n\nArgs:\n    params: Optional project paths and additional skill directories to include in discovery.\n\nReturns:\n    Skills discovered across global and project sources."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ServerSkillList.from_dict(await self._client.request("skills.discover", params_dict, **_timeout_kwargs(timeout)))


class ServerSessionFsApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def set_provider(self, params: SessionFSSetProviderRequest, *, timeout: float | None = None) -> SessionFSSetProviderResult:
        "Registers an SDK client as the session filesystem provider.\n\nArgs:\n    params: Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.\n\nReturns:\n    Indicates whether the calling client was registered as the session filesystem provider."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionFSSetProviderResult.from_dict(await self._client.request("sessionFs.setProvider", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ServerSessionsApi:
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def fork(self, params: SessionsForkRequest, *, timeout: float | None = None) -> SessionsForkResult:
        "Creates a new session by forking persisted history from an existing session.\n\nArgs:\n    params: Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.\n\nReturns:\n    Identifier and optional friendly name assigned to the newly forked session."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsForkResult.from_dict(await self._client.request("sessions.fork", params_dict, **_timeout_kwargs(timeout)))

    async def connect(self, params: ConnectRemoteSessionParams, *, timeout: float | None = None) -> RemoteSessionConnectionResult:
        "Connects to an existing remote session and exposes it as an SDK session.\n\nArgs:\n    params: Remote session connection parameters.\n\nReturns:\n    Remote session connection result."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return RemoteSessionConnectionResult.from_dict(await self._client.request("sessions.connect", params_dict, **_timeout_kwargs(timeout)))

    async def list(self, params: SessionsListRequest, *, timeout: float | None = None) -> SessionList:
        "Lists persisted sessions, optionally filtered by working-directory context.\n\nArgs:\n    params: Optional metadata-load limit and context filter applied to the returned sessions.\n\nReturns:\n    Persisted sessions matching the filter, ordered most-recently-modified first."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionList.from_dict(await self._client.request("sessions.list", params_dict, **_timeout_kwargs(timeout)))

    async def find_by_task_id(self, params: SessionsFindByTaskIDRequest, *, timeout: float | None = None) -> SessionsFindByTaskIDResult:
        "Finds the local session bound to a GitHub task ID, if any.\n\nArgs:\n    params: GitHub task ID to look up.\n\nReturns:\n    ID of the local session bound to the given GitHub task, or omitted when none."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsFindByTaskIDResult.from_dict(await self._client.request("sessions.findByTaskId", params_dict, **_timeout_kwargs(timeout)))

    async def find_by_prefix(self, params: SessionsFindByPrefixRequest, *, timeout: float | None = None) -> SessionsFindByPrefixResult:
        "Resolves a UUID prefix to a unique session ID, if exactly one session matches.\n\nArgs:\n    params: UUID prefix to resolve to a unique session ID.\n\nReturns:\n    Session ID matching the prefix, omitted when no unique match exists."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsFindByPrefixResult.from_dict(await self._client.request("sessions.findByPrefix", params_dict, **_timeout_kwargs(timeout)))

    async def get_last_for_context(self, params: SessionsGetLastForContextRequest, *, timeout: float | None = None) -> SessionsGetLastForContextResult:
        "Returns the most-relevant prior session for a given working-directory context.\n\nArgs:\n    params: Optional working-directory context used to score session relevance.\n\nReturns:\n    Most-relevant session ID for the supplied context, or omitted when no sessions exist."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsGetLastForContextResult.from_dict(await self._client.request("sessions.getLastForContext", params_dict, **_timeout_kwargs(timeout)))

    async def get_event_file_path(self, params: SessionsGetEventFilePathRequest, *, timeout: float | None = None) -> SessionsGetEventFilePathResult:
        "Computes the absolute path to a session's persisted events.jsonl file.\n\nArgs:\n    params: Session ID whose event-log file path to compute.\n\nReturns:\n    Absolute path to the session's events.jsonl file on disk."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsGetEventFilePathResult.from_dict(await self._client.request("sessions.getEventFilePath", params_dict, **_timeout_kwargs(timeout)))

    async def get_sizes(self, *, timeout: float | None = None) -> SessionSizes:
        "Returns the on-disk byte size of each session's workspace directory.\n\nReturns:\n    Map of sessionId -> on-disk size in bytes for each session's workspace directory."
        return SessionSizes.from_dict(await self._client.request("sessions.getSizes", {}, **_timeout_kwargs(timeout)))

    async def check_in_use(self, params: SessionsCheckInUseRequest, *, timeout: float | None = None) -> SessionsCheckInUseResult:
        "Returns the subset of the supplied session IDs that are currently held by another running process.\n\nArgs:\n    params: Session IDs to test for live in-use locks.\n\nReturns:\n    Session IDs from the input set that are currently in use by another process."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsCheckInUseResult.from_dict(await self._client.request("sessions.checkInUse", params_dict, **_timeout_kwargs(timeout)))

    async def get_persisted_remote_steerable(self, params: SessionsGetPersistedRemoteSteerableRequest, *, timeout: float | None = None) -> SessionsGetPersistedRemoteSteerableResult:
        "Returns a session's persisted remote-steerable flag, if any has been recorded.\n\nArgs:\n    params: Session ID to look up the persisted remote-steerable flag for.\n\nReturns:\n    The session's persisted remote-steerable flag, or omitted when no value has been persisted."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsGetPersistedRemoteSteerableResult.from_dict(await self._client.request("sessions.getPersistedRemoteSteerable", params_dict, **_timeout_kwargs(timeout)))

    async def close(self, params: SessionsCloseRequest, *, timeout: float | None = None) -> SessionsCloseResult:
        "Closes a session: emits shutdown, flushes pending events, releases the in-use lock, and disposes the active session.\n\nArgs:\n    params: Session ID to close.\n\nReturns:\n    Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsCloseResult.from_dict(await self._client.request("sessions.close", params_dict, **_timeout_kwargs(timeout)))

    async def bulk_delete(self, params: SessionsBulkDeleteRequest, *, timeout: float | None = None) -> SessionBulkDeleteResult:
        "Closes, deactivates, and deletes a set of sessions, returning the bytes freed per session.\n\nArgs:\n    params: Session IDs to close, deactivate, and delete from disk.\n\nReturns:\n    Map of sessionId -> bytes freed by removing the session's workspace directory."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionBulkDeleteResult.from_dict(await self._client.request("sessions.bulkDelete", params_dict, **_timeout_kwargs(timeout)))

    async def prune_old(self, params: SessionsPruneOldRequest, *, timeout: float | None = None) -> SessionPruneResult:
        "Deletes sessions older than the given threshold, with optional dry-run and exclusion list.\n\nArgs:\n    params: Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).\n\nReturns:\n    Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionPruneResult.from_dict(await self._client.request("sessions.pruneOld", params_dict, **_timeout_kwargs(timeout)))

    async def save(self, params: SessionsSaveRequest, *, timeout: float | None = None) -> SessionsSaveResult:
        "Flushes a session's pending events to disk.\n\nArgs:\n    params: Session ID whose pending events should be flushed to disk.\n\nReturns:\n    Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed)."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsSaveResult.from_dict(await self._client.request("sessions.save", params_dict, **_timeout_kwargs(timeout)))

    async def release_lock(self, params: SessionsReleaseLockRequest, *, timeout: float | None = None) -> SessionsReleaseLockResult:
        "Releases the in-use lock held by this process for a session.\n\nArgs:\n    params: Session ID whose in-use lock should be released.\n\nReturns:\n    Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsReleaseLockResult.from_dict(await self._client.request("sessions.releaseLock", params_dict, **_timeout_kwargs(timeout)))

    async def enrich_metadata(self, params: SessionsEnrichMetadataRequest, *, timeout: float | None = None) -> SessionEnrichMetadataResult:
        "Backfills missing summary and context fields on the supplied session metadata records.\n\nArgs:\n    params: Session metadata records to enrich with summary and context information.\n\nReturns:\n    The same metadata records, with summary and context fields backfilled where available."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionEnrichMetadataResult.from_dict(await self._client.request("sessions.enrichMetadata", params_dict, **_timeout_kwargs(timeout)))

    async def reload_plugin_hooks(self, params: SessionsReloadPluginHooksRequest, *, timeout: float | None = None) -> SessionsReloadPluginHooksResult:
        "Reloads user, plugin, and (optionally) repo hooks on the active session.\n\nArgs:\n    params: Active session ID and an optional flag for deferring repo-level hooks until folder trust.\n\nReturns:\n    Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsReloadPluginHooksResult.from_dict(await self._client.request("sessions.reloadPluginHooks", params_dict, **_timeout_kwargs(timeout)))

    async def load_deferred_repo_hooks(self, params: SessionsLoadDeferredRepoHooksRequest, *, timeout: float | None = None) -> SessionLoadDeferredRepoHooksResult:
        "Loads previously-deferred repo-level hooks on the active session, returning queued startup prompts.\n\nArgs:\n    params: Active session ID whose deferred repo-level hooks should be loaded.\n\nReturns:\n    Queued repo-level startup prompts and the total hook command count after loading."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionLoadDeferredRepoHooksResult.from_dict(await self._client.request("sessions.loadDeferredRepoHooks", params_dict, **_timeout_kwargs(timeout)))

    async def set_additional_plugins(self, params: SessionsSetAdditionalPluginsRequest, *, timeout: float | None = None) -> SessionsSetAdditionalPluginsResult:
        "Replaces the manager-wide additional plugins registered with the session manager.\n\nArgs:\n    params: Manager-wide additional plugins to register; replaces any previously-configured set.\n\nReturns:\n    Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsSetAdditionalPluginsResult.from_dict(await self._client.request("sessions.setAdditionalPlugins", params_dict, **_timeout_kwargs(timeout)))


class ServerRpc:
    """Typed server-scoped RPC methods."""
    def __init__(self, client: JsonRpcClient):
        self._client = client
        self.models = ServerModelsApi(client)
        self.tools = ServerToolsApi(client)
        self.account = ServerAccountApi(client)
        self.secrets = ServerSecretsApi(client)
        self.mcp = ServerMcpApi(client)
        self.skills = ServerSkillsApi(client)
        self.session_fs = ServerSessionFsApi(client)
        self.sessions = ServerSessionsApi(client)

    async def ping(self, params: PingRequest, *, timeout: float | None = None) -> PingResult:
        "Checks server responsiveness and returns protocol information.\n\nArgs:\n    params: Optional message to echo back to the caller.\n\nReturns:\n    Server liveness response, including the echoed message, current server timestamp, and protocol version."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return PingResult.from_dict(await self._client.request("ping", params_dict, **_timeout_kwargs(timeout)))


class _InternalServerRpc:
    """Internal SDK server-scoped RPC methods. Not part of the public API."""
    def __init__(self, client: JsonRpcClient):
        self._client = client

    async def connect(self, params: ConnectRequest, *, timeout: float | None = None) -> ConnectResult:
        "Performs the SDK server connection handshake and validates the optional connection token.\n\nArgs:\n    params: Optional connection token presented by the SDK client during the handshake.\n\nReturns:\n    Handshake result reporting the server's protocol version and package version on success.\n\n:meta private:\n\nInternal SDK API; not part of the public surface."
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ConnectResult.from_dict(await self._client.request("connect", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class AuthApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_status(self, *, timeout: float | None = None) -> SessionAuthStatus:
        "Gets authentication status and account metadata for the session.\n\nReturns:\n    Authentication status and account metadata for the session."
        return SessionAuthStatus.from_dict(await self._client.request("session.auth.getStatus", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set_credentials(self, params: SessionSetCredentialsParams, *, timeout: float | None = None) -> SessionSetCredentialsResult:
        "Updates the session's auth credentials used for outbound model and API requests.\n\nArgs:\n    params: New auth credentials to install on the session. Omit to leave credentials unchanged.\n\nReturns:\n    Indicates whether the credential update succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionSetCredentialsResult.from_dict(await self._client.request("session.auth.setCredentials", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ModelApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_current(self, *, timeout: float | None = None) -> CurrentModel:
        "Gets the currently selected model for the session.\n\nReturns:\n    The currently selected model and reasoning effort for the session."
        return CurrentModel.from_dict(await self._client.request("session.model.getCurrent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def switch_to(self, params: ModelSwitchToRequest, *, timeout: float | None = None) -> ModelSwitchToResult:
        "Switches the session to a model and optional reasoning configuration.\n\nArgs:\n    params: Target model identifier and optional reasoning effort, summary, and capability overrides.\n\nReturns:\n    The model identifier active on the session after the switch."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ModelSwitchToResult.from_dict(await self._client.request("session.model.switchTo", params_dict, **_timeout_kwargs(timeout)))

    async def set_reasoning_effort(self, params: ModelSetReasoningEffortRequest, *, timeout: float | None = None) -> ModelSetReasoningEffortResult:
        "Updates the session's reasoning effort without changing the selected model.\n\nArgs:\n    params: Reasoning effort level to apply to the currently selected model.\n\nReturns:\n    Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ModelSetReasoningEffortResult.from_dict(await self._client.request("session.model.setReasoningEffort", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ModeApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get(self, *, timeout: float | None = None) -> SessionMode:
        "Gets the current agent interaction mode.\n\nReturns:\n    The session mode the agent is operating in"
        return SessionMode(await self._client.request("session.mode.get", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set(self, params: ModeSetRequest, *, timeout: float | None = None) -> None:
        "Sets the current agent interaction mode.\n\nArgs:\n    params: Agent interaction mode to apply to the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.mode.set", params_dict, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class NameApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get(self, *, timeout: float | None = None) -> NameGetResult:
        "Gets the session's friendly name.\n\nReturns:\n    The session's friendly name, or null when not yet set."
        return NameGetResult.from_dict(await self._client.request("session.name.get", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set(self, params: NameSetRequest, *, timeout: float | None = None) -> None:
        "Sets the session's friendly name.\n\nArgs:\n    params: New friendly name to apply to the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.name.set", params_dict, **_timeout_kwargs(timeout))

    async def set_auto(self, params: NameSetAutoRequest, *, timeout: float | None = None) -> NameSetAutoResult:
        "Persists an auto-generated session summary as the session's name when no user-set name exists.\n\nArgs:\n    params: Auto-generated session summary to apply as the session's name when no user-set name exists.\n\nReturns:\n    Indicates whether the auto-generated summary was applied as the session's name."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return NameSetAutoResult.from_dict(await self._client.request("session.name.setAuto", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PlanApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def read(self, *, timeout: float | None = None) -> PlanReadResult:
        "Reads the session plan file from the workspace.\n\nReturns:\n    Existence, contents, and resolved path of the session plan file."
        return PlanReadResult.from_dict(await self._client.request("session.plan.read", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def update(self, params: PlanUpdateRequest, *, timeout: float | None = None) -> None:
        "Writes new content to the session plan file.\n\nArgs:\n    params: Replacement contents to write to the session plan file."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.plan.update", params_dict, **_timeout_kwargs(timeout))

    async def delete(self, *, timeout: float | None = None) -> None:
        "Deletes the session plan file from the workspace."
        await self._client.request("session.plan.delete", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class WorkspacesApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_workspace(self, *, timeout: float | None = None) -> WorkspacesGetWorkspaceResult:
        "Gets current workspace metadata for the session.\n\nReturns:\n    Current workspace metadata for the session, including its absolute filesystem path when available."
        return WorkspacesGetWorkspaceResult.from_dict(await self._client.request("session.workspaces.getWorkspace", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def list_files(self, *, timeout: float | None = None) -> WorkspacesListFilesResult:
        "Lists files stored in the session workspace files directory.\n\nReturns:\n    Relative paths of files stored in the session workspace files directory."
        return WorkspacesListFilesResult.from_dict(await self._client.request("session.workspaces.listFiles", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def read_file(self, params: WorkspacesReadFileRequest, *, timeout: float | None = None) -> WorkspacesReadFileResult:
        "Reads a file from the session workspace files directory.\n\nArgs:\n    params: Relative path of the workspace file to read.\n\nReturns:\n    Contents of the requested workspace file as a UTF-8 string."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return WorkspacesReadFileResult.from_dict(await self._client.request("session.workspaces.readFile", params_dict, **_timeout_kwargs(timeout)))

    async def create_file(self, params: WorkspacesCreateFileRequest, *, timeout: float | None = None) -> None:
        "Creates or overwrites a file in the session workspace files directory.\n\nArgs:\n    params: Relative path and UTF-8 content for the workspace file to create or overwrite."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.workspaces.createFile", params_dict, **_timeout_kwargs(timeout))

    async def list_checkpoints(self, *, timeout: float | None = None) -> WorkspacesListCheckpointsResult:
        "Lists workspace checkpoints in chronological order.\n\nReturns:\n    Workspace checkpoints in chronological order; empty when the workspace is not enabled."
        return WorkspacesListCheckpointsResult.from_dict(await self._client.request("session.workspaces.listCheckpoints", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def read_checkpoint(self, params: WorkspacesReadCheckpointRequest, *, timeout: float | None = None) -> WorkspacesReadCheckpointResult:
        "Reads the content of a workspace checkpoint by number.\n\nArgs:\n    params: Checkpoint number to read.\n\nReturns:\n    Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return WorkspacesReadCheckpointResult.from_dict(await self._client.request("session.workspaces.readCheckpoint", params_dict, **_timeout_kwargs(timeout)))

    async def save_large_paste(self, params: WorkspacesSaveLargePasteRequest, *, timeout: float | None = None) -> WorkspacesSaveLargePasteResult:
        "Saves pasted content as a UTF-8 file in the session workspace.\n\nArgs:\n    params: Pasted content to save as a UTF-8 file in the session workspace.\n\nReturns:\n    Descriptor for the saved paste file, or null when the workspace is unavailable."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return WorkspacesSaveLargePasteResult.from_dict(await self._client.request("session.workspaces.saveLargePaste", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class InstructionsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_sources(self, *, timeout: float | None = None) -> InstructionsGetSourcesResult:
        "Gets instruction sources loaded for the session.\n\nReturns:\n    Instruction sources loaded for the session, in merge order."
        return InstructionsGetSourcesResult.from_dict(await self._client.request("session.instructions.getSources", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class FleetApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def start(self, params: FleetStartRequest, *, timeout: float | None = None) -> FleetStartResult:
        "Starts fleet mode by submitting the fleet orchestration prompt to the session.\n\nArgs:\n    params: Optional user prompt to combine with the fleet orchestration instructions.\n\nReturns:\n    Indicates whether fleet mode was successfully activated."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return FleetStartResult.from_dict(await self._client.request("session.fleet.start", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class AgentApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> AgentList:
        "Lists custom agents available to the session.\n\nReturns:\n    Custom agents available to the session."
        return AgentList.from_dict(await self._client.request("session.agent.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def get_current(self, *, timeout: float | None = None) -> AgentGetCurrentResult:
        "Gets the currently selected custom agent for the session.\n\nReturns:\n    The currently selected custom agent, or null when using the default agent."
        return AgentGetCurrentResult.from_dict(await self._client.request("session.agent.getCurrent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def select(self, params: AgentSelectRequest, *, timeout: float | None = None) -> AgentSelectResult:
        "Selects a custom agent for subsequent turns in the session.\n\nArgs:\n    params: Name of the custom agent to select for subsequent turns.\n\nReturns:\n    The newly selected custom agent."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return AgentSelectResult.from_dict(await self._client.request("session.agent.select", params_dict, **_timeout_kwargs(timeout)))

    async def deselect(self, *, timeout: float | None = None) -> None:
        "Clears the selected custom agent and returns the session to the default agent."
        await self._client.request("session.agent.deselect", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> AgentReloadResult:
        "Reloads custom agent definitions and returns the refreshed list.\n\nReturns:\n    Custom agents available to the session after reloading definitions from disk."
        return AgentReloadResult.from_dict(await self._client.request("session.agent.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class TasksApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def start_agent(self, params: TasksStartAgentRequest, *, timeout: float | None = None) -> TasksStartAgentResult:
        "Starts a background agent task in the session.\n\nArgs:\n    params: Agent type, prompt, name, and optional description and model override for the new task.\n\nReturns:\n    Identifier assigned to the newly started background agent task."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return TasksStartAgentResult.from_dict(await self._client.request("session.tasks.startAgent", params_dict, **_timeout_kwargs(timeout)))

    async def list(self, *, timeout: float | None = None) -> TaskList:
        "Lists background tasks tracked by the session.\n\nReturns:\n    Background tasks currently tracked by the session."
        return TaskList.from_dict(await self._client.request("session.tasks.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def refresh(self, *, timeout: float | None = None) -> TasksRefreshResult:
        "Refreshes metadata for any detached background shells the runtime knows about.\n\nReturns:\n    Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop."
        return TasksRefreshResult.from_dict(await self._client.request("session.tasks.refresh", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def wait_for_pending(self, *, timeout: float | None = None) -> TasksWaitForPendingResult:
        "Waits for all in-flight background tasks and any follow-up turns to settle.\n\nReturns:\n    Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS)."
        return TasksWaitForPendingResult.from_dict(await self._client.request("session.tasks.waitForPending", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def get_progress(self, params: TasksGetProgressRequest, *, timeout: float | None = None) -> TasksGetProgressResult:
        "Returns progress information for a background task by ID.\n\nArgs:\n    params: Identifier of the background task to fetch progress for.\n\nReturns:\n    Progress information for the task, or null when no task with that ID is tracked."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return TasksGetProgressResult.from_dict(await self._client.request("session.tasks.getProgress", params_dict, **_timeout_kwargs(timeout)))

    async def get_current_promotable(self, *, timeout: float | None = None) -> TasksGetCurrentPromotableResult:
        "Returns the first sync-waiting task that can currently be promoted to background mode.\n\nReturns:\n    The first sync-waiting task that can currently be promoted to background mode."
        return TasksGetCurrentPromotableResult.from_dict(await self._client.request("session.tasks.getCurrentPromotable", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def promote_to_background(self, params: TasksPromoteToBackgroundRequest, *, timeout: float | None = None) -> TasksPromoteToBackgroundResult:
        "Promotes an eligible synchronously-waited task so it continues running in the background.\n\nArgs:\n    params: Identifier of the task to promote to background mode.\n\nReturns:\n    Indicates whether the task was successfully promoted to background mode."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return TasksPromoteToBackgroundResult.from_dict(await self._client.request("session.tasks.promoteToBackground", params_dict, **_timeout_kwargs(timeout)))

    async def promote_current_to_background(self, *, timeout: float | None = None) -> TasksPromoteCurrentToBackgroundResult:
        "Atomically promotes the first promotable sync-waiting task to background mode and returns it.\n\nReturns:\n    The promoted task as it now exists in background mode, omitted if no promotable task was waiting."
        return TasksPromoteCurrentToBackgroundResult.from_dict(await self._client.request("session.tasks.promoteCurrentToBackground", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def cancel(self, params: TasksCancelRequest, *, timeout: float | None = None) -> TasksCancelResult:
        "Cancels a background task.\n\nArgs:\n    params: Identifier of the background task to cancel.\n\nReturns:\n    Indicates whether the background task was successfully cancelled."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return TasksCancelResult.from_dict(await self._client.request("session.tasks.cancel", params_dict, **_timeout_kwargs(timeout)))

    async def remove(self, params: TasksRemoveRequest, *, timeout: float | None = None) -> TasksRemoveResult:
        "Removes a completed or cancelled background task from tracking.\n\nArgs:\n    params: Identifier of the completed or cancelled task to remove from tracking.\n\nReturns:\n    Indicates whether the task was removed. False when the task does not exist or is still running/idle."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return TasksRemoveResult.from_dict(await self._client.request("session.tasks.remove", params_dict, **_timeout_kwargs(timeout)))

    async def send_message(self, params: TasksSendMessageRequest, *, timeout: float | None = None) -> TasksSendMessageResult:
        "Sends a message to a background agent task.\n\nArgs:\n    params: Identifier of the target agent task, message content, and optional sender agent ID.\n\nReturns:\n    Indicates whether the message was delivered, with an error message when delivery failed."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return TasksSendMessageResult.from_dict(await self._client.request("session.tasks.sendMessage", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class SkillsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SkillList:
        "Lists skills available to the session.\n\nReturns:\n    Skills available to the session, with their enabled state."
        return SkillList.from_dict(await self._client.request("session.skills.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def get_invoked(self, *, timeout: float | None = None) -> SkillsGetInvokedResult:
        "Returns the skills that have been invoked during this session.\n\nReturns:\n    Skills invoked during this session, ordered by invocation time (most recent last)."
        return SkillsGetInvokedResult.from_dict(await self._client.request("session.skills.getInvoked", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: SkillsEnableRequest, *, timeout: float | None = None) -> None:
        "Enables a skill for the session.\n\nArgs:\n    params: Name of the skill to enable for the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.skills.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: SkillsDisableRequest, *, timeout: float | None = None) -> None:
        "Disables a skill for the session.\n\nArgs:\n    params: Name of the skill to disable for the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.skills.disable", params_dict, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> SkillsLoadDiagnostics:
        "Reloads skill definitions for the session.\n\nReturns:\n    Diagnostics from reloading skill definitions, with warnings and errors as separate lists."
        return SkillsLoadDiagnostics.from_dict(await self._client.request("session.skills.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def ensure_loaded(self, *, timeout: float | None = None) -> None:
        "Ensures the session's skill definitions have been loaded from disk."
        await self._client.request("session.skills.ensureLoaded", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class McpOauthApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def login(self, params: MCPOauthLoginRequest, *, timeout: float | None = None) -> MCPOauthLoginResult:
        "Starts OAuth authentication for a remote MCP server.\n\nArgs:\n    params: Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.\n\nReturns:\n    OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MCPOauthLoginResult.from_dict(await self._client.request("session.mcp.oauth.login", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class McpApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id
        self.oauth = McpOauthApi(client, session_id)

    async def list(self, *, timeout: float | None = None) -> MCPServerList:
        "Lists MCP servers configured for the session and their connection status.\n\nReturns:\n    MCP servers configured for the session, with their connection status."
        return MCPServerList.from_dict(await self._client.request("session.mcp.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: MCPEnableRequest, *, timeout: float | None = None) -> None:
        "Enables an MCP server for the session.\n\nArgs:\n    params: Name of the MCP server to enable for the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.mcp.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: MCPDisableRequest, *, timeout: float | None = None) -> None:
        "Disables an MCP server for the session.\n\nArgs:\n    params: Name of the MCP server to disable for the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.mcp.disable", params_dict, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> None:
        "Reloads MCP server connections for the session."
        await self._client.request("session.mcp.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))

    async def execute_sampling(self, params: MCPExecuteSamplingParams, *, timeout: float | None = None) -> MCPSamplingExecutionResult:
        "Runs an MCP sampling inference on behalf of an MCP server.\n\nArgs:\n    params: Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.\n\nReturns:\n    Outcome of an MCP sampling execution: success result, failure error, or cancellation."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MCPSamplingExecutionResult.from_dict(await self._client.request("session.mcp.executeSampling", params_dict, **_timeout_kwargs(timeout)))

    async def cancel_sampling_execution(self, params: MCPCancelSamplingExecutionParams, *, timeout: float | None = None) -> MCPCancelSamplingExecutionResult:
        "Cancels an in-flight MCP sampling execution by request ID.\n\nArgs:\n    params: The requestId previously passed to executeSampling that should be cancelled.\n\nReturns:\n    Indicates whether an in-flight sampling execution with the given requestId was found and cancelled."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MCPCancelSamplingExecutionResult.from_dict(await self._client.request("session.mcp.cancelSamplingExecution", params_dict, **_timeout_kwargs(timeout)))

    async def set_env_value_mode(self, params: MCPSetEnvValueModeParams, *, timeout: float | None = None) -> MCPSetEnvValueModeResult:
        "Sets how environment-variable values supplied to MCP servers are resolved (direct or indirect).\n\nArgs:\n    params: Mode controlling how MCP server env values are resolved (`direct` or `indirect`).\n\nReturns:\n    Env-value mode recorded on the session after the update."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MCPSetEnvValueModeResult.from_dict(await self._client.request("session.mcp.setEnvValueMode", params_dict, **_timeout_kwargs(timeout)))

    async def remove_git_hub(self, *, timeout: float | None = None) -> MCPRemoveGitHubResult:
        "Removes the auto-managed `github` MCP server when present.\n\nReturns:\n    Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove)."
        return MCPRemoveGitHubResult.from_dict(await self._client.request("session.mcp.removeGitHub", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PluginsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> PluginList:
        "Lists plugins installed for the session.\n\nReturns:\n    Plugins installed for the session, with their enabled state and version metadata."
        return PluginList.from_dict(await self._client.request("session.plugins.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class OptionsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def update(self, params: SessionUpdateOptionsParams, *, timeout: float | None = None) -> SessionUpdateOptionsResult:
        "Patches the genuinely-mutable subset of session options.\n\nArgs:\n    params: Patch of mutable session options to apply to the running session.\n\nReturns:\n    Indicates whether the session options patch was applied successfully."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionUpdateOptionsResult.from_dict(await self._client.request("session.options.update", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class LspApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def initialize(self, params: LspInitializeRequest, *, timeout: float | None = None) -> None:
        "Loads the merged LSP configuration set for the session's working directory.\n\nArgs:\n    params: Parameters for (re)loading the merged LSP configuration set."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.lsp.initialize", params_dict, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class ExtensionsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> ExtensionList:
        "Lists extensions discovered for the session and their current status.\n\nReturns:\n    Extensions discovered for the session, with their current status."
        return ExtensionList.from_dict(await self._client.request("session.extensions.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: ExtensionsEnableRequest, *, timeout: float | None = None) -> None:
        "Enables an extension for the session.\n\nArgs:\n    params: Source-qualified extension identifier to enable for the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.extensions.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: ExtensionsDisableRequest, *, timeout: float | None = None) -> None:
        "Disables an extension for the session.\n\nArgs:\n    params: Source-qualified extension identifier to disable for the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.extensions.disable", params_dict, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> None:
        "Reloads extension definitions and processes for the session."
        await self._client.request("session.extensions.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class ToolsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_tool_call(self, params: HandlePendingToolCallRequest, *, timeout: float | None = None) -> HandlePendingToolCallResult:
        "Provides the result for a pending external tool call.\n\nArgs:\n    params: Pending external tool call request ID, with the tool result or an error describing why it failed.\n\nReturns:\n    Indicates whether the external tool call result was handled successfully."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return HandlePendingToolCallResult.from_dict(await self._client.request("session.tools.handlePendingToolCall", params_dict, **_timeout_kwargs(timeout)))

    async def initialize_and_validate(self, *, timeout: float | None = None) -> ToolsInitializeAndValidateResult:
        "Resolves, builds, and validates the runtime tool list for the session.\n\nReturns:\n    Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation."
        return ToolsInitializeAndValidateResult.from_dict(await self._client.request("session.tools.initializeAndValidate", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class CommandsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, params: CommandsListRequest | None = None, *, timeout: float | None = None) -> CommandList:
        "Lists slash commands available in the session.\n\nArgs:\n    params: Optional filters controlling which command sources to include in the listing.\n\nReturns:\n    Slash commands available in the session, after applying any include/exclude filters."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None} if params is not None else {}
        params_dict["sessionId"] = self._session_id
        return CommandList.from_dict(await self._client.request("session.commands.list", params_dict, **_timeout_kwargs(timeout)))

    async def invoke(self, params: CommandsInvokeRequest, *, timeout: float | None = None) -> SlashCommandInvocationResult:
        "Invokes a slash command in the session.\n\nArgs:\n    params: Slash command name and optional raw input string to invoke.\n\nReturns:\n    Result of invoking the slash command (text output, prompt to send to the agent, or completion)."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return _load_SlashCommandInvocationResult(await self._client.request("session.commands.invoke", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_command(self, params: CommandsHandlePendingCommandRequest, *, timeout: float | None = None) -> CommandsHandlePendingCommandResult:
        "Reports completion of a pending client-handled slash command.\n\nArgs:\n    params: Pending command request ID and an optional error if the client handler failed.\n\nReturns:\n    Indicates whether the pending client-handled command was completed successfully."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return CommandsHandlePendingCommandResult.from_dict(await self._client.request("session.commands.handlePendingCommand", params_dict, **_timeout_kwargs(timeout)))

    async def execute(self, params: ExecuteCommandParams, *, timeout: float | None = None) -> ExecuteCommandResult:
        "Executes a slash command synchronously and returns any error.\n\nArgs:\n    params: Slash command name and argument string to execute synchronously.\n\nReturns:\n    Error message produced while executing the command, if any."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ExecuteCommandResult.from_dict(await self._client.request("session.commands.execute", params_dict, **_timeout_kwargs(timeout)))

    async def enqueue(self, params: EnqueueCommandParams, *, timeout: float | None = None) -> EnqueueCommandResult:
        "Enqueues a slash command for FIFO processing on the local session.\n\nArgs:\n    params: Slash-prefixed command string to enqueue for FIFO processing.\n\nReturns:\n    Indicates whether the command was accepted into the local execution queue."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return EnqueueCommandResult.from_dict(await self._client.request("session.commands.enqueue", params_dict, **_timeout_kwargs(timeout)))

    async def respond_to_queued_command(self, params: CommandsRespondToQueuedCommandRequest, *, timeout: float | None = None) -> CommandsRespondToQueuedCommandResult:
        "Reports whether the host actually executed a queued command and whether to continue processing.\n\nArgs:\n    params: Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).\n\nReturns:\n    Indicates whether the queued-command response was matched to a pending request."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return CommandsRespondToQueuedCommandResult.from_dict(await self._client.request("session.commands.respondToQueuedCommand", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class TelemetryApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def set_feature_overrides(self, params: TelemetrySetFeatureOverridesRequest, *, timeout: float | None = None) -> None:
        "Sets feature override key/value pairs to attach to subsequent telemetry events for the session.\n\nArgs:\n    params: Feature override key/value pairs to attach to subsequent telemetry events from this session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.telemetry.setFeatureOverrides", params_dict, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class UiApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def elicitation(self, params: UIElicitationRequest, *, timeout: float | None = None) -> UIElicitationResponse:
        "Requests structured input from a UI-capable client.\n\nArgs:\n    params: Prompt message and JSON schema describing the form fields to elicit from the user.\n\nReturns:\n    The elicitation response (accept with form values, decline, or cancel)"
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIElicitationResponse.from_dict(await self._client.request("session.ui.elicitation", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_elicitation(self, params: UIHandlePendingElicitationRequest, *, timeout: float | None = None) -> UIElicitationResult:
        "Provides the user response for a pending elicitation request.\n\nArgs:\n    params: Pending elicitation request ID and the user's response (accept/decline/cancel + form values).\n\nReturns:\n    Indicates whether the elicitation response was accepted; false if it was already resolved by another client."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIElicitationResult.from_dict(await self._client.request("session.ui.handlePendingElicitation", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_user_input(self, params: UIHandlePendingUserInputRequest, *, timeout: float | None = None) -> UIHandlePendingResult:
        "Resolves a pending `user_input.requested` event with the user's response.\n\nArgs:\n    params: Request ID of a pending `user_input.requested` event and the user's response.\n\nReturns:\n    Indicates whether the pending UI request was resolved by this call."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIHandlePendingResult.from_dict(await self._client.request("session.ui.handlePendingUserInput", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_sampling(self, params: UIHandlePendingSamplingRequest, *, timeout: float | None = None) -> UIHandlePendingResult:
        "Resolves a pending `sampling.requested` event with a sampling result, or rejects it.\n\nArgs:\n    params: Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).\n\nReturns:\n    Indicates whether the pending UI request was resolved by this call."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIHandlePendingResult.from_dict(await self._client.request("session.ui.handlePendingSampling", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_auto_mode_switch(self, params: UIHandlePendingAutoModeSwitchRequest, *, timeout: float | None = None) -> UIHandlePendingResult:
        "Resolves a pending `auto_mode_switch.requested` event with the user's accept/decline decision.\n\nArgs:\n    params: Request ID of a pending `auto_mode_switch.requested` event and the user's response.\n\nReturns:\n    Indicates whether the pending UI request was resolved by this call."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIHandlePendingResult.from_dict(await self._client.request("session.ui.handlePendingAutoModeSwitch", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_exit_plan_mode(self, params: UIHandlePendingExitPlanModeRequest, *, timeout: float | None = None) -> UIHandlePendingResult:
        "Resolves a pending `exit_plan_mode.requested` event with the user's response.\n\nArgs:\n    params: Request ID of a pending `exit_plan_mode.requested` event and the user's response.\n\nReturns:\n    Indicates whether the pending UI request was resolved by this call."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIHandlePendingResult.from_dict(await self._client.request("session.ui.handlePendingExitPlanMode", params_dict, **_timeout_kwargs(timeout)))

    async def register_direct_auto_mode_switch_handler(self, *, timeout: float | None = None) -> UIRegisterDirectAutoModeSwitchHandlerResult:
        "Registers an in-process handler for auto-mode-switch requests so the server bridge skips dispatch.\n\nReturns:\n    Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId)."
        return UIRegisterDirectAutoModeSwitchHandlerResult.from_dict(await self._client.request("session.ui.registerDirectAutoModeSwitchHandler", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def unregister_direct_auto_mode_switch_handler(self, params: UIUnregisterDirectAutoModeSwitchHandlerRequest, *, timeout: float | None = None) -> UIUnregisterDirectAutoModeSwitchHandlerResult:
        "Unregisters a previously-registered in-process auto-mode-switch handler by its opaque handle.\n\nArgs:\n    params: Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.\n\nReturns:\n    Indicates whether the handle was active and the registration count was decremented."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIUnregisterDirectAutoModeSwitchHandlerResult.from_dict(await self._client.request("session.ui.unregisterDirectAutoModeSwitchHandler", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PermissionsPathsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> PermissionPathsList:
        "Returns the session's allowed directories and primary working directory.\n\nReturns:\n    Snapshot of the session's allow-listed directories and primary working directory."
        return PermissionPathsList.from_dict(await self._client.request("session.permissions.paths.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def add(self, params: PermissionPathsAddParams, *, timeout: float | None = None) -> PermissionsPathsAddResult:
        "Adds a directory to the session's allow-list.\n\nArgs:\n    params: Directory path to add to the session's allowed directories.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsPathsAddResult.from_dict(await self._client.request("session.permissions.paths.add", params_dict, **_timeout_kwargs(timeout)))

    async def update_primary(self, params: PermissionPathsUpdatePrimaryParams, *, timeout: float | None = None) -> PermissionsPathsUpdatePrimaryResult:
        "Updates the session's primary working directory used by the permission policy.\n\nArgs:\n    params: Directory path to set as the session's new primary working directory.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsPathsUpdatePrimaryResult.from_dict(await self._client.request("session.permissions.paths.updatePrimary", params_dict, **_timeout_kwargs(timeout)))

    async def is_path_within_allowed_directories(self, params: PermissionPathsAllowedCheckParams, *, timeout: float | None = None) -> PermissionPathsAllowedCheckResult:
        "Reports whether a path falls within any of the session's allowed directories.\n\nArgs:\n    params: Path to evaluate against the session's allowed directories.\n\nReturns:\n    Indicates whether the supplied path is within the session's allowed directories."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionPathsAllowedCheckResult.from_dict(await self._client.request("session.permissions.paths.isPathWithinAllowedDirectories", params_dict, **_timeout_kwargs(timeout)))

    async def is_path_within_workspace(self, params: PermissionPathsWorkspaceCheckParams, *, timeout: float | None = None) -> PermissionPathsWorkspaceCheckResult:
        "Reports whether a path falls within the session's workspace (primary) directory.\n\nArgs:\n    params: Path to evaluate against the session's workspace (primary) directory.\n\nReturns:\n    Indicates whether the supplied path is within the session's workspace directory."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionPathsWorkspaceCheckResult.from_dict(await self._client.request("session.permissions.paths.isPathWithinWorkspace", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PermissionsLocationsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def resolve(self, params: PermissionLocationResolveParams, *, timeout: float | None = None) -> PermissionLocationResolveResult:
        "Resolves the permission location key and type for a working directory.\n\nArgs:\n    params: Working directory to resolve into a location-permissions key.\n\nReturns:\n    Resolved location-permissions key and type."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionLocationResolveResult.from_dict(await self._client.request("session.permissions.locations.resolve", params_dict, **_timeout_kwargs(timeout)))

    async def apply(self, params: PermissionLocationApplyParams, *, timeout: float | None = None) -> PermissionLocationApplyResult:
        "Applies persisted location-scoped tool approvals and allowed directories for a working directory to this session's permission service.\n\nArgs:\n    params: Working directory to load persisted location permissions for.\n\nReturns:\n    Summary of persisted location permissions applied to the session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionLocationApplyResult.from_dict(await self._client.request("session.permissions.locations.apply", params_dict, **_timeout_kwargs(timeout)))

    async def add_tool_approval(self, params: PermissionLocationAddToolApprovalParams, *, timeout: float | None = None) -> PermissionsLocationsAddToolApprovalResult:
        "Persists a tool approval for a permission location and applies its rules to this session's live permission service.\n\nArgs:\n    params: Location-scoped tool approval to persist.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsLocationsAddToolApprovalResult.from_dict(await self._client.request("session.permissions.locations.addToolApproval", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PermissionsFolderTrustApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def is_trusted(self, params: FolderTrustCheckParams, *, timeout: float | None = None) -> FolderTrustCheckResult:
        "Reports whether a folder is trusted according to the user's folder trust state.\n\nArgs:\n    params: Folder path to check for trust.\n\nReturns:\n    Folder trust check result."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return FolderTrustCheckResult.from_dict(await self._client.request("session.permissions.folderTrust.isTrusted", params_dict, **_timeout_kwargs(timeout)))

    async def add_trusted(self, params: FolderTrustAddParams, *, timeout: float | None = None) -> PermissionsFolderTrustAddTrustedResult:
        "Adds a folder to the user's trusted folders list.\n\nArgs:\n    params: Folder path to add to trusted folders.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsFolderTrustAddTrustedResult.from_dict(await self._client.request("session.permissions.folderTrust.addTrusted", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PermissionsUrlsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def set_unrestricted_mode(self, params: PermissionUrlsSetUnrestrictedModeParams, *, timeout: float | None = None) -> PermissionsUrlsSetUnrestrictedModeResult:
        "Toggles the runtime's URL-permission policy between unrestricted and restricted modes.\n\nArgs:\n    params: Whether the URL-permission policy should run in unrestricted mode.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsUrlsSetUnrestrictedModeResult.from_dict(await self._client.request("session.permissions.urls.setUnrestrictedMode", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PermissionsApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id
        self.paths = PermissionsPathsApi(client, session_id)
        self.locations = PermissionsLocationsApi(client, session_id)
        self.folder_trust = PermissionsFolderTrustApi(client, session_id)
        self.urls = PermissionsUrlsApi(client, session_id)

    async def configure(self, params: PermissionsConfigureParams, *, timeout: float | None = None) -> PermissionsConfigureResult:
        "Replaces selected permission policy fields (rules, paths, URLs, exclusions, allow-all flags) on the session.\n\nArgs:\n    params: Patch of permission policy fields to apply (omit a field to leave it unchanged).\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsConfigureResult.from_dict(await self._client.request("session.permissions.configure", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_permission_request(self, params: PermissionDecisionRequest, *, timeout: float | None = None) -> PermissionRequestResult:
        "Provides a decision for a pending tool permission request.\n\nArgs:\n    params: Pending permission request ID and the decision to apply (approve/reject and scope).\n\nReturns:\n    Indicates whether the permission decision was applied; false when the request was already resolved."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionRequestResult.from_dict(await self._client.request("session.permissions.handlePendingPermissionRequest", params_dict, **_timeout_kwargs(timeout)))

    async def pending_requests(self, *, timeout: float | None = None) -> PendingPermissionRequestList:
        "Reconstructs the set of pending tool permission requests from the session's event history.\n\nReturns:\n    List of pending permission requests reconstructed from event history."
        return PendingPermissionRequestList.from_dict(await self._client.request("session.permissions.pendingRequests", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set_approve_all(self, params: PermissionsSetApproveAllRequest, *, timeout: float | None = None) -> PermissionsSetApproveAllResult:
        "Enables or disables automatic approval of tool permission requests for the session.\n\nArgs:\n    params: Allow-all toggle for tool permission requests, with an optional telemetry source.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsSetApproveAllResult.from_dict(await self._client.request("session.permissions.setApproveAll", params_dict, **_timeout_kwargs(timeout)))

    async def modify_rules(self, params: PermissionsModifyRulesParams, *, timeout: float | None = None) -> PermissionsModifyRulesResult:
        "Adds or removes session-scoped or location-scoped permission rules.\n\nArgs:\n    params: Scope and add/remove instructions for modifying session- or location-scoped permission rules.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsModifyRulesResult.from_dict(await self._client.request("session.permissions.modifyRules", params_dict, **_timeout_kwargs(timeout)))

    async def set_required(self, params: PermissionsSetRequiredRequest, *, timeout: float | None = None) -> PermissionsSetRequiredResult:
        "Sets whether the client wants permission prompts bridged into session events.\n\nArgs:\n    params: Toggles whether permission prompts should be bridged into session events for this client.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsSetRequiredResult.from_dict(await self._client.request("session.permissions.setRequired", params_dict, **_timeout_kwargs(timeout)))

    async def reset_session_approvals(self, *, timeout: float | None = None) -> PermissionsResetSessionApprovalsResult:
        "Clears session-scoped tool permission approvals.\n\nReturns:\n    Indicates whether the operation succeeded."
        return PermissionsResetSessionApprovalsResult.from_dict(await self._client.request("session.permissions.resetSessionApprovals", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def notify_prompt_shown(self, params: PermissionPromptShownNotification, *, timeout: float | None = None) -> PermissionsNotifyPromptShownResult:
        "Notifies the runtime that a permission prompt UI has been shown to the user.\n\nArgs:\n    params: Notification payload describing the permission prompt that the client just rendered.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionsNotifyPromptShownResult.from_dict(await self._client.request("session.permissions.notifyPromptShown", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class MetadataApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def snapshot(self, *, timeout: float | None = None) -> SessionMetadataSnapshot:
        "Returns a snapshot of the session's identifying metadata, mode, agent, and remote info.\n\nReturns:\n    Point-in-time snapshot of slow-changing session identifier and state fields"
        return SessionMetadataSnapshot.from_dict(await self._client.request("session.metadata.snapshot", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def is_processing(self, *, timeout: float | None = None) -> MetadataIsProcessingResult:
        "Reports whether the local session is currently processing user/agent messages.\n\nReturns:\n    Indicates whether the local session is currently processing a turn or background continuation."
        return MetadataIsProcessingResult.from_dict(await self._client.request("session.metadata.isProcessing", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def context_info(self, params: MetadataContextInfoRequest, *, timeout: float | None = None) -> MetadataContextInfoResult:
        "Returns the token breakdown for the session's current context window for a given model.\n\nArgs:\n    params: Model identifier and token limits used to compute the context-info breakdown.\n\nReturns:\n    Token breakdown for the session's current context window, or null if uninitialized."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MetadataContextInfoResult.from_dict(await self._client.request("session.metadata.contextInfo", params_dict, **_timeout_kwargs(timeout)))

    async def record_context_change(self, params: MetadataRecordContextChangeRequest, *, timeout: float | None = None) -> MetadataRecordContextChangeResult:
        "Records a working-directory/git context change and emits a `session.context_changed` event.\n\nArgs:\n    params: Updated working-directory/git context to record on the session.\n\nReturns:\n    Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode)."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MetadataRecordContextChangeResult.from_dict(await self._client.request("session.metadata.recordContextChange", params_dict, **_timeout_kwargs(timeout)))

    async def set_working_directory(self, params: MetadataSetWorkingDirectoryRequest, *, timeout: float | None = None) -> MetadataSetWorkingDirectoryResult:
        "Updates the session's recorded working directory.\n\nArgs:\n    params: Absolute path to set as the session's new working directory.\n\nReturns:\n    Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MetadataSetWorkingDirectoryResult.from_dict(await self._client.request("session.metadata.setWorkingDirectory", params_dict, **_timeout_kwargs(timeout)))

    async def recompute_context_tokens(self, params: MetadataRecomputeContextTokensRequest, *, timeout: float | None = None) -> MetadataRecomputeContextTokensResult:
        "Re-tokenizes the session's existing messages against a model and returns aggregate token totals.\n\nArgs:\n    params: Model identifier to use when re-tokenizing the session's existing messages.\n\nReturns:\n    Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return MetadataRecomputeContextTokensResult.from_dict(await self._client.request("session.metadata.recomputeContextTokens", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ShellApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def exec(self, params: ShellExecRequest, *, timeout: float | None = None) -> ShellExecResult:
        "Starts a shell command and streams output through session notifications.\n\nArgs:\n    params: Shell command to run, with optional working directory and timeout in milliseconds.\n\nReturns:\n    Identifier of the spawned process, used to correlate streamed output and exit notifications."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ShellExecResult.from_dict(await self._client.request("session.shell.exec", params_dict, **_timeout_kwargs(timeout)))

    async def kill(self, params: ShellKillRequest, *, timeout: float | None = None) -> ShellKillResult:
        "Sends a signal to a shell process previously started via \"shell.exec\".\n\nArgs:\n    params: Identifier of a process previously returned by \"shell.exec\" and the signal to send.\n\nReturns:\n    Indicates whether the signal was delivered; false if the process was unknown or already exited."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ShellKillResult.from_dict(await self._client.request("session.shell.kill", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class HistoryApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def compact(self, params: HistoryCompactRequest | None = None, *, timeout: float | None = None) -> HistoryCompactResult:
        "Compacts the session history to reduce context usage.\n\nArgs:\n    params: Optional compaction parameters.\n\nReturns:\n    Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None} if params is not None else {}
        params_dict["sessionId"] = self._session_id
        return HistoryCompactResult.from_dict(await self._client.request("session.history.compact", params_dict, **_timeout_kwargs(timeout)))

    async def truncate(self, params: HistoryTruncateRequest, *, timeout: float | None = None) -> HistoryTruncateResult:
        "Truncates persisted session history to a specific event.\n\nArgs:\n    params: Identifier of the event to truncate to; this event and all later events are removed.\n\nReturns:\n    Number of events that were removed by the truncation."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return HistoryTruncateResult.from_dict(await self._client.request("session.history.truncate", params_dict, **_timeout_kwargs(timeout)))

    async def cancel_background_compaction(self, *, timeout: float | None = None) -> HistoryCancelBackgroundCompactionResult:
        "Cancels any in-progress background compaction on a local session.\n\nReturns:\n    Indicates whether an in-progress background compaction was cancelled."
        return HistoryCancelBackgroundCompactionResult.from_dict(await self._client.request("session.history.cancelBackgroundCompaction", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def abort_manual_compaction(self, *, timeout: float | None = None) -> HistoryAbortManualCompactionResult:
        "Aborts any in-progress manual compaction on a local session.\n\nReturns:\n    Indicates whether an in-progress manual compaction was aborted."
        return HistoryAbortManualCompactionResult.from_dict(await self._client.request("session.history.abortManualCompaction", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def summarize_for_handoff(self, *, timeout: float | None = None) -> HistorySummarizeForHandoffResult:
        "Produces a markdown summary of the session's conversation context for hand-off scenarios.\n\nReturns:\n    Markdown summary of the conversation context (empty when not available)."
        return HistorySummarizeForHandoffResult.from_dict(await self._client.request("session.history.summarizeForHandoff", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class QueueApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def pending_items(self, *, timeout: float | None = None) -> QueuePendingItemsResult:
        "Returns the local session's pending user-facing queued items and steering messages.\n\nReturns:\n    Snapshot of the session's pending queued items and immediate-steering messages."
        return QueuePendingItemsResult.from_dict(await self._client.request("session.queue.pendingItems", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def remove_most_recent(self, *, timeout: float | None = None) -> QueueRemoveMostRecentResult:
        "Removes the most recently queued user-facing item (LIFO).\n\nReturns:\n    Indicates whether a user-facing pending item was removed."
        return QueueRemoveMostRecentResult.from_dict(await self._client.request("session.queue.removeMostRecent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def clear(self, *, timeout: float | None = None) -> None:
        "Clears all pending queued items on the local session."
        await self._client.request("session.queue.clear", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class EventLogApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def read(self, params: EventLogReadRequest, *, timeout: float | None = None) -> EventsReadResult:
        "Reads a batch of session events from a cursor, optionally waiting for new events.\n\nArgs:\n    params: Cursor, batch size, and optional long-poll/filter parameters for reading session events.\n\nReturns:\n    Batch of session events returned by a read, with cursor and continuation metadata."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return EventsReadResult.from_dict(await self._client.request("session.eventLog.read", params_dict, **_timeout_kwargs(timeout)))

    async def tail(self, *, timeout: float | None = None) -> EventLogTailResult:
        "Returns a snapshot of the current tail cursor without consuming events.\n\nReturns:\n    Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session)."
        return EventLogTailResult.from_dict(await self._client.request("session.eventLog.tail", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def register_interest(self, params: RegisterEventInterestParams, *, timeout: float | None = None) -> RegisterEventInterestResult:
        "Registers consumer interest in an event type for runtime gating purposes.\n\nArgs:\n    params: Event type to register consumer interest for, used by runtime gating logic.\n\nReturns:\n    Opaque handle representing an event-type interest registration."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return RegisterEventInterestResult.from_dict(await self._client.request("session.eventLog.registerInterest", params_dict, **_timeout_kwargs(timeout)))

    async def release_interest(self, params: ReleaseEventInterestParams, *, timeout: float | None = None) -> EventLogReleaseInterestResult:
        "Releases a consumer's previously-registered interest in an event type.\n\nArgs:\n    params: Opaque handle previously returned by `registerInterest` to release.\n\nReturns:\n    Indicates whether the operation succeeded."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return EventLogReleaseInterestResult.from_dict(await self._client.request("session.eventLog.releaseInterest", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class UsageApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_metrics(self, *, timeout: float | None = None) -> UsageGetMetricsResult:
        "Gets accumulated usage metrics for the session.\n\nReturns:\n    Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals."
        return UsageGetMetricsResult.from_dict(await self._client.request("session.usage.getMetrics", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class RemoteApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def enable(self, params: RemoteEnableRequest, *, timeout: float | None = None) -> RemoteEnableResult:
        "Enables remote session export or steering.\n\nArgs:\n    params: Optional remote session mode (\"off\", \"export\", or \"on\"); defaults to enabling both export and remote steering.\n\nReturns:\n    GitHub URL for the session and a flag indicating whether remote steering is enabled."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return RemoteEnableResult.from_dict(await self._client.request("session.remote.enable", params_dict, **_timeout_kwargs(timeout)))

    async def disable(self, *, timeout: float | None = None) -> None:
        "Disables remote session export and steering."
        await self._client.request("session.remote.disable", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))

    async def notify_steerable_changed(self, params: RemoteNotifySteerableChangedRequest, *, timeout: float | None = None) -> RemoteNotifySteerableChangedResult:
        "Persists a remote-steerability change emitted by the host as a session event.\n\nArgs:\n    params: New remote-steerability state to persist as a `session.remote_steerable_changed` event.\n\nReturns:\n    Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return RemoteNotifySteerableChangedResult.from_dict(await self._client.request("session.remote.notifySteerableChanged", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ScheduleApi:
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> ScheduleList:
        "Lists the session's currently active scheduled prompts.\n\nReturns:\n    Snapshot of the currently active recurring prompts for this session."
        return ScheduleList.from_dict(await self._client.request("session.schedule.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def stop(self, params: ScheduleStopRequest, *, timeout: float | None = None) -> ScheduleStopResult:
        "Removes a scheduled prompt by id.\n\nArgs:\n    params: Identifier of the scheduled prompt to remove.\n\nReturns:\n    Remove a scheduled prompt by id. The result entry is omitted if the id was unknown."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ScheduleStopResult.from_dict(await self._client.request("session.schedule.stop", params_dict, **_timeout_kwargs(timeout)))


class SessionRpc:
    """Typed session-scoped RPC methods."""
    def __init__(self, client: JsonRpcClient, session_id: str):
        self._client = client
        self._session_id = session_id
        self.auth = AuthApi(client, session_id)
        self.model = ModelApi(client, session_id)
        self.mode = ModeApi(client, session_id)
        self.name = NameApi(client, session_id)
        self.plan = PlanApi(client, session_id)
        self.workspaces = WorkspacesApi(client, session_id)
        self.instructions = InstructionsApi(client, session_id)
        self.fleet = FleetApi(client, session_id)
        self.agent = AgentApi(client, session_id)
        self.tasks = TasksApi(client, session_id)
        self.skills = SkillsApi(client, session_id)
        self.mcp = McpApi(client, session_id)
        self.plugins = PluginsApi(client, session_id)
        self.options = OptionsApi(client, session_id)
        self.lsp = LspApi(client, session_id)
        self.extensions = ExtensionsApi(client, session_id)
        self.tools = ToolsApi(client, session_id)
        self.commands = CommandsApi(client, session_id)
        self.telemetry = TelemetryApi(client, session_id)
        self.ui = UiApi(client, session_id)
        self.permissions = PermissionsApi(client, session_id)
        self.metadata = MetadataApi(client, session_id)
        self.shell = ShellApi(client, session_id)
        self.history = HistoryApi(client, session_id)
        self.queue = QueueApi(client, session_id)
        self.event_log = EventLogApi(client, session_id)
        self.usage = UsageApi(client, session_id)
        self.remote = RemoteApi(client, session_id)
        self.schedule = ScheduleApi(client, session_id)

    async def suspend(self, *, timeout: float | None = None) -> None:
        "Suspends the session while preserving persisted state for later resume.\n\n.. warning:: This API is experimental and may change or be removed in future versions."
        await self._client.request("session.suspend", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))

    async def send(self, params: SendRequest, *, timeout: float | None = None) -> SendResult:
        "Sends a user message to the session and returns its message ID.\n\nArgs:\n    params: Parameters for sending a user message to the session\n\nReturns:\n    Result of sending a user message\n\n.. warning:: This API is experimental and may change or be removed in future versions."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SendResult.from_dict(await self._client.request("session.send", params_dict, **_timeout_kwargs(timeout)))

    async def abort(self, params: AbortRequest, *, timeout: float | None = None) -> AbortResult:
        "Aborts the current agent turn.\n\nArgs:\n    params: Parameters for aborting the current turn\n\nReturns:\n    Result of aborting the current turn\n\n.. warning:: This API is experimental and may change or be removed in future versions."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return AbortResult.from_dict(await self._client.request("session.abort", params_dict, **_timeout_kwargs(timeout)))

    async def shutdown(self, params: ShutdownRequest, *, timeout: float | None = None) -> None:
        "Shuts down the session and persists its final state. Awaits any deferred sessionEnd hooks before resolving so user-supplied hook scripts complete before the runtime tears down.\n\nArgs:\n    params: Parameters for shutting down the session\n\n.. warning:: This API is experimental and may change or be removed in future versions."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.shutdown", params_dict, **_timeout_kwargs(timeout))

    async def log(self, params: LogRequest, *, timeout: float | None = None) -> LogResult:
        "Emits a user-visible session log event.\n\nArgs:\n    params: Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.\n\nReturns:\n    Identifier of the session event that was emitted for the log message.\n\n.. warning:: This API is experimental and may change or be removed in future versions."
        params_dict: dict[str, Any] = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return LogResult.from_dict(await self._client.request("session.log", params_dict, **_timeout_kwargs(timeout)))


class SessionFsHandler(Protocol):
    async def read_file(self, params: SessionFSReadFileRequest) -> SessionFSReadFileResult:
        "Reads a file from the client-provided session filesystem.\n\nArgs:\n    params: Path of the file to read from the client-provided session filesystem.\n\nReturns:\n    File content as a UTF-8 string, or a filesystem error if the read failed."
        pass
    async def write_file(self, params: SessionFSWriteFileRequest) -> SessionFSError | None:
        "Writes a file in the client-provided session filesystem.\n\nArgs:\n    params: File path, content to write, and optional mode for the client-provided session filesystem.\n\nReturns:\n    Describes a filesystem error."
        pass
    async def append_file(self, params: SessionFSAppendFileRequest) -> SessionFSError | None:
        "Appends content to a file in the client-provided session filesystem.\n\nArgs:\n    params: File path, content to append, and optional mode for the client-provided session filesystem.\n\nReturns:\n    Describes a filesystem error."
        pass
    async def exists(self, params: SessionFSExistsRequest) -> SessionFSExistsResult:
        "Checks whether a path exists in the client-provided session filesystem.\n\nArgs:\n    params: Path to test for existence in the client-provided session filesystem.\n\nReturns:\n    Indicates whether the requested path exists in the client-provided session filesystem."
        pass
    async def stat(self, params: SessionFSStatRequest) -> SessionFSStatResult:
        "Gets metadata for a path in the client-provided session filesystem.\n\nArgs:\n    params: Path whose metadata should be returned from the client-provided session filesystem.\n\nReturns:\n    Filesystem metadata for the requested path, or a filesystem error if the stat failed."
        pass
    async def mkdir(self, params: SessionFSMkdirRequest) -> SessionFSError | None:
        "Creates a directory in the client-provided session filesystem.\n\nArgs:\n    params: Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.\n\nReturns:\n    Describes a filesystem error."
        pass
    async def readdir(self, params: SessionFSReaddirRequest) -> SessionFSReaddirResult:
        "Lists entry names in a directory from the client-provided session filesystem.\n\nArgs:\n    params: Directory path whose entries should be listed from the client-provided session filesystem.\n\nReturns:\n    Names of entries in the requested directory, or a filesystem error if the read failed."
        pass
    async def readdir_with_types(self, params: SessionFSReaddirWithTypesRequest) -> SessionFSReaddirWithTypesResult:
        "Lists directory entries with type information from the client-provided session filesystem.\n\nArgs:\n    params: Directory path whose entries (with type information) should be listed from the client-provided session filesystem.\n\nReturns:\n    Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed."
        pass
    async def rm(self, params: SessionFSRmRequest) -> SessionFSError | None:
        "Removes a file or directory from the client-provided session filesystem.\n\nArgs:\n    params: Path to remove from the client-provided session filesystem, with options for recursive removal and force.\n\nReturns:\n    Describes a filesystem error."
        pass
    async def rename(self, params: SessionFSRenameRequest) -> SessionFSError | None:
        "Renames or moves a path in the client-provided session filesystem.\n\nArgs:\n    params: Source and destination paths for renaming or moving an entry in the client-provided session filesystem.\n\nReturns:\n    Describes a filesystem error."
        pass
    async def sqlite_query(self, params: SessionFSSqliteQueryRequest) -> SessionFSSqliteQueryResult:
        "Executes a SQLite query against the per-session database.\n\nArgs:\n    params: SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database.\n\nReturns:\n    Query results including rows, columns, and rows affected, or a filesystem error if execution failed."
        pass
    async def sqlite_exists(self, params: SessionFSSqliteExistsRequest) -> SessionFSSqliteExistsResult:
        "Checks whether the per-session SQLite database already exists, without creating it.\n\nArgs:\n    params: Identifies the target session.\n\nReturns:\n    Indicates whether the per-session SQLite database already exists."
        pass

@dataclass
class ClientSessionApiHandlers:
    session_fs: SessionFsHandler | None = None

def register_client_session_api_handlers(
    client: JsonRpcClient,
    get_handlers: Callable[[str], ClientSessionApiHandlers],
) -> None:
    """Register client-session request handlers on a JSON-RPC connection."""
    async def handle_session_fs_read_file(params: dict) -> dict | None:
        request = SessionFSReadFileRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.read_file(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.readFile", handle_session_fs_read_file)
    async def handle_session_fs_write_file(params: dict) -> dict | None:
        request = SessionFSWriteFileRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.write_file(request)
        return result.to_dict() if result is not None else None
    client.set_request_handler("sessionFs.writeFile", handle_session_fs_write_file)
    async def handle_session_fs_append_file(params: dict) -> dict | None:
        request = SessionFSAppendFileRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.append_file(request)
        return result.to_dict() if result is not None else None
    client.set_request_handler("sessionFs.appendFile", handle_session_fs_append_file)
    async def handle_session_fs_exists(params: dict) -> dict | None:
        request = SessionFSExistsRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.exists(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.exists", handle_session_fs_exists)
    async def handle_session_fs_stat(params: dict) -> dict | None:
        request = SessionFSStatRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.stat(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.stat", handle_session_fs_stat)
    async def handle_session_fs_mkdir(params: dict) -> dict | None:
        request = SessionFSMkdirRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.mkdir(request)
        return result.to_dict() if result is not None else None
    client.set_request_handler("sessionFs.mkdir", handle_session_fs_mkdir)
    async def handle_session_fs_readdir(params: dict) -> dict | None:
        request = SessionFSReaddirRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.readdir(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.readdir", handle_session_fs_readdir)
    async def handle_session_fs_readdir_with_types(params: dict) -> dict | None:
        request = SessionFSReaddirWithTypesRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.readdir_with_types(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.readdirWithTypes", handle_session_fs_readdir_with_types)
    async def handle_session_fs_rm(params: dict) -> dict | None:
        request = SessionFSRmRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.rm(request)
        return result.to_dict() if result is not None else None
    client.set_request_handler("sessionFs.rm", handle_session_fs_rm)
    async def handle_session_fs_rename(params: dict) -> dict | None:
        request = SessionFSRenameRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.rename(request)
        return result.to_dict() if result is not None else None
    client.set_request_handler("sessionFs.rename", handle_session_fs_rename)
    async def handle_session_fs_sqlite_query(params: dict) -> dict | None:
        request = SessionFSSqliteQueryRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.sqlite_query(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.sqliteQuery", handle_session_fs_sqlite_query)
    async def handle_session_fs_sqlite_exists(params: dict) -> dict | None:
        request = SessionFSSqliteExistsRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        result = await handler.sqlite_exists(request)
        return result.to_dict()
    client.set_request_handler("sessionFs.sqliteExists", handle_session_fs_sqlite_exists)
