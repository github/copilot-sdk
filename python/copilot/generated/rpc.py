"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

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


def from_int(x: Any) -> int:
    assert isinstance(x, int) and not isinstance(x, bool)
    return x

def from_bool(x: Any) -> bool:
    assert isinstance(x, bool)
    return x

def from_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)

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

def to_float(x: Any) -> float:
    assert isinstance(x, (int, float))
    return x

def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:
    assert isinstance(x, dict)
    return { k: f(v) for (k, v) in x.items() }

def to_class(c: type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()

def from_list(f: Callable[[Any], T], x: Any) -> list[T]:
    assert isinstance(x, list)
    return [f(y) for y in x]

def to_enum(c: type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, c)
    return x.value

def from_datetime(x: Any) -> datetime:
    return dateutil.parser.parse(x)

@dataclass
class AccountQuotaSnapshot:
    entitlement_requests: int
    """Number of requests included in the entitlement"""

    is_unlimited_entitlement: bool
    """Whether the user has an unlimited usage entitlement"""

    overage: float
    """Number of overage requests made this period"""

    overage_allowed_with_exhausted_quota: bool
    """Whether overage is allowed when quota is exhausted"""

    remaining_percentage: float
    """Percentage of entitlement remaining"""

    usage_allowed_with_exhausted_quota: bool
    """Whether usage is still permitted after quota exhaustion"""

    used_requests: int
    """Number of requests used so far this period"""

    reset_date: str | None = None
    """Date when the quota resets (ISO 8601 string)"""

    @staticmethod
    def from_dict(obj: Any) -> 'AccountQuotaSnapshot':
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

@dataclass
class AgentInfo:
    """The newly selected custom agent"""

    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentInfo':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return AgentInfo(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentSelectRequest:
    name: str
    """Name of the custom agent to select"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentSelectRequest':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return AgentSelectRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

@dataclass
class CommandsHandlePendingCommandRequest:
    request_id: str
    """Request ID from the command invocation event"""

    error: str | None = None
    """Error message if the command handler failed"""

    @staticmethod
    def from_dict(obj: Any) -> 'CommandsHandlePendingCommandRequest':
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

@dataclass
class CommandsHandlePendingCommandResult:
    success: bool
    """Whether the command was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'CommandsHandlePendingCommandResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return CommandsHandlePendingCommandResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

@dataclass
class CurrentModel:
    model_id: str | None = None
    """Currently active model identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'CurrentModel':
        assert isinstance(obj, dict)
        model_id = from_union([from_str, from_none], obj.get("modelId"))
        return CurrentModel(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model_id is not None:
            result["modelId"] = from_union([from_str, from_none], self.model_id)
        return result

class MCPServerSource(Enum):
    """Configuration source

    Configuration source: user, workspace, plugin, or builtin
    """
    BUILTIN = "builtin"
    PLUGIN = "plugin"
    USER = "user"
    WORKSPACE = "workspace"

class DiscoveredMCPServerType(Enum):
    """Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)"""

    HTTP = "http"
    MEMORY = "memory"
    SSE = "sse"
    STDIO = "stdio"

class ExtensionSource(Enum):
    """Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)"""

    PROJECT = "project"
    USER = "user"

class ExtensionStatus(Enum):
    """Current status: running, disabled, failed, or starting"""

    DISABLED = "disabled"
    FAILED = "failed"
    RUNNING = "running"
    STARTING = "starting"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class ExtensionsDisableRequest:
    id: str
    """Source-qualified extension ID to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'ExtensionsDisableRequest':
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
    id: str
    """Source-qualified extension ID to enable"""

    @staticmethod
    def from_dict(obj: Any) -> 'ExtensionsEnableRequest':
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return ExtensionsEnableRequest(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result

class FilterMappingString(Enum):
    HIDDEN_CHARACTERS = "hidden_characters"
    MARKDOWN = "markdown"
    NONE = "none"

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FleetStartRequest:
    prompt: str | None = None
    """Optional user prompt to combine with fleet instructions"""

    @staticmethod
    def from_dict(obj: Any) -> 'FleetStartRequest':
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
    started: bool
    """Whether fleet mode was successfully activated"""

    @staticmethod
    def from_dict(obj: Any) -> 'FleetStartResult':
        assert isinstance(obj, dict)
        started = from_bool(obj.get("started"))
        return FleetStartResult(started)

    def to_dict(self) -> dict:
        result: dict = {}
        result["started"] = from_bool(self.started)
        return result

@dataclass
class HandleToolCallResult:
    success: bool
    """Whether the tool call result was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'HandleToolCallResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return HandleToolCallResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

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
    def from_dict(obj: Any) -> 'HistoryCompactContextWindow':
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
class HistoryTruncateRequest:
    event_id: str
    """Event ID to truncate to. This event and all events after it are removed from the session."""

    @staticmethod
    def from_dict(obj: Any) -> 'HistoryTruncateRequest':
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
    events_removed: int
    """Number of events that were removed"""

    @staticmethod
    def from_dict(obj: Any) -> 'HistoryTruncateResult':
        assert isinstance(obj, dict)
        events_removed = from_int(obj.get("eventsRemoved"))
        return HistoryTruncateResult(events_removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventsRemoved"] = from_int(self.events_removed)
        return result

class InstructionsSourcesLocation(Enum):
    """Where this source lives — used for UI grouping"""

    REPOSITORY = "repository"
    USER = "user"
    WORKING_DIRECTORY = "working-directory"

class InstructionsSourcesType(Enum):
    """Category of instruction source — used for merge logic"""

    CHILD_INSTRUCTIONS = "child-instructions"
    HOME = "home"
    MODEL = "model"
    NESTED_AGENTS = "nested-agents"
    REPO = "repo"
    VSCODE = "vscode"

class SessionLogLevel(Enum):
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    ERROR = "error"
    INFO = "info"
    WARNING = "warning"

@dataclass
class LogResult:
    event_id: UUID
    """The unique identifier of the emitted session event"""

    @staticmethod
    def from_dict(obj: Any) -> 'LogResult':
        assert isinstance(obj, dict)
        event_id = UUID(obj.get("eventId"))
        return LogResult(event_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventId"] = str(self.event_id)
        return result

class MCPServerConfigType(Enum):
    """Remote transport type. Defaults to "http" when omitted."""

    HTTP = "http"
    LOCAL = "local"
    SSE = "sse"
    STDIO = "stdio"

@dataclass
class MCPConfigRemoveRequest:
    name: str
    """Name of the MCP server to remove"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigRemoveRequest':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return MCPConfigRemoveRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

@dataclass
class MCPDisableRequest:
    server_name: str
    """Name of the MCP server to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPDisableRequest':
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return MCPDisableRequest(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result

@dataclass
class MCPDiscoverRequest:
    working_directory: str | None = None
    """Working directory used as context for discovery (e.g., plugin resolution)"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPDiscoverRequest':
        assert isinstance(obj, dict)
        working_directory = from_union([from_str, from_none], obj.get("workingDirectory"))
        return MCPDiscoverRequest(working_directory)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.working_directory is not None:
            result["workingDirectory"] = from_union([from_str, from_none], self.working_directory)
        return result

@dataclass
class MCPEnableRequest:
    server_name: str
    """Name of the MCP server to enable"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPEnableRequest':
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return MCPEnableRequest(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result

class MCPServerStatus(Enum):
    """Connection status: connected, failed, needs-auth, pending, disabled, or not_configured"""

    CONNECTED = "connected"
    DISABLED = "disabled"
    FAILED = "failed"
    NEEDS_AUTH = "needs-auth"
    NOT_CONFIGURED = "not_configured"
    PENDING = "pending"

class MCPServerConfigHTTPType(Enum):
    """Remote transport type. Defaults to "http" when omitted."""

    HTTP = "http"
    SSE = "sse"

class MCPServerConfigLocalType(Enum):
    LOCAL = "local"
    STDIO = "stdio"

class SessionMode(Enum):
    """The agent mode. Valid values: "interactive", "plan", "autopilot"."""

    AUTOPILOT = "autopilot"
    INTERACTIVE = "interactive"
    PLAN = "plan"

@dataclass
class ModelBilling:
    """Billing information"""

    multiplier: float
    """Billing cost multiplier relative to the base rate"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelBilling':
        assert isinstance(obj, dict)
        multiplier = from_float(obj.get("multiplier"))
        return ModelBilling(multiplier)

    def to_dict(self) -> dict:
        result: dict = {}
        result["multiplier"] = to_float(self.multiplier)
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
    def from_dict(obj: Any) -> 'ModelCapabilitiesLimitsVision':
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
    def from_dict(obj: Any) -> 'ModelCapabilitiesSupports':
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

@dataclass
class ModelPolicy:
    """Policy state (if applicable)"""

    state: str
    """Current policy state for this model"""

    terms: str
    """Usage terms or conditions for this model"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelPolicy':
        assert isinstance(obj, dict)
        state = from_str(obj.get("state"))
        terms = from_str(obj.get("terms"))
        return ModelPolicy(state, terms)

    def to_dict(self) -> dict:
        result: dict = {}
        result["state"] = from_str(self.state)
        result["terms"] = from_str(self.terms)
        return result

@dataclass
class ModelCapabilitiesOverrideLimitsVision:
    max_prompt_image_size: int | None = None
    """Maximum image size in bytes"""

    max_prompt_images: int | None = None
    """Maximum number of images per prompt"""

    supported_media_types: list[str] | None = None
    """MIME types the model accepts"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelCapabilitiesOverrideLimitsVision':
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

@dataclass
class ModelCapabilitiesOverrideSupports:
    """Feature flags indicating what the model supports"""

    reasoning_effort: bool | None = None
    vision: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'ModelCapabilitiesOverrideSupports':
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

@dataclass
class ModelSwitchToResult:
    model_id: str | None = None
    """Currently active model identifier after the switch"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelSwitchToResult':
        assert isinstance(obj, dict)
        model_id = from_union([from_str, from_none], obj.get("modelId"))
        return ModelSwitchToResult(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model_id is not None:
            result["modelId"] = from_union([from_str, from_none], self.model_id)
        return result

@dataclass
class NameGetResult:
    name: str | None = None
    """The session name, falling back to the auto-generated summary, or null if neither exists"""

    @staticmethod
    def from_dict(obj: Any) -> 'NameGetResult':
        assert isinstance(obj, dict)
        name = from_union([from_none, from_str], obj.get("name"))
        return NameGetResult(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_union([from_none, from_str], self.name)
        return result

@dataclass
class NameSetRequest:
    name: str
    """New session name (1–100 characters, trimmed of leading/trailing whitespace)"""

    @staticmethod
    def from_dict(obj: Any) -> 'NameSetRequest':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return NameSetRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

class PermissionDecisionKind(Enum):
    APPROVED = "approved"
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"
    DENIED_BY_PERMISSION_REQUEST_HOOK = "denied-by-permission-request-hook"
    DENIED_BY_RULES = "denied-by-rules"
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"

class PermissionDecisionApprovedKind(Enum):
    APPROVED = "approved"

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

@dataclass
class PermissionRequestResult:
    success: bool
    """Whether the permission request was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionRequestResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return PermissionRequestResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

@dataclass
class PingRequest:
    message: str | None = None
    """Optional message to echo back"""

    @staticmethod
    def from_dict(obj: Any) -> 'PingRequest':
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
    message: str
    """Echoed message (or default greeting)"""

    protocol_version: int
    """Server protocol version number"""

    timestamp: int
    """Server timestamp in milliseconds"""

    @staticmethod
    def from_dict(obj: Any) -> 'PingResult':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        protocol_version = from_int(obj.get("protocolVersion"))
        timestamp = from_int(obj.get("timestamp"))
        return PingResult(message, protocol_version, timestamp)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["protocolVersion"] = from_int(self.protocol_version)
        result["timestamp"] = from_int(self.timestamp)
        return result

@dataclass
class PlanReadResult:
    exists: bool
    """Whether the plan file exists in the workspace"""

    content: str | None = None
    """The content of the plan file, or null if it does not exist"""

    path: str | None = None
    """Absolute file path of the plan file, or null if workspace is not enabled"""

    @staticmethod
    def from_dict(obj: Any) -> 'PlanReadResult':
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

@dataclass
class PlanUpdateRequest:
    content: str
    """The new content for the plan file"""

    @staticmethod
    def from_dict(obj: Any) -> 'PlanUpdateRequest':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return PlanUpdateRequest(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result

@dataclass
class Plugin:
    enabled: bool
    """Whether the plugin is currently enabled"""

    marketplace: str
    """Marketplace the plugin came from"""

    name: str
    """Plugin name"""

    version: str | None = None
    """Installed version"""

    @staticmethod
    def from_dict(obj: Any) -> 'Plugin':
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

@dataclass
class ServerSkill:
    description: str
    """Description of what the skill does"""

    enabled: bool
    """Whether the skill is currently enabled (based on global config)"""

    name: str
    """Unique identifier for the skill"""

    source: str
    """Source location type (e.g., project, personal-copilot, plugin, builtin)"""

    user_invocable: bool
    """Whether the skill can be invoked by the user as a slash command"""

    path: str | None = None
    """Absolute path to the skill file"""

    project_path: str | None = None
    """The project path this skill belongs to (only for project/inherited skills)"""

    @staticmethod
    def from_dict(obj: Any) -> 'ServerSkill':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = from_str(obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        path = from_union([from_str, from_none], obj.get("path"))
        project_path = from_union([from_str, from_none], obj.get("projectPath"))
        return ServerSkill(description, enabled, name, source, user_invocable, path, project_path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = from_str(self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.project_path is not None:
            result["projectPath"] = from_union([from_str, from_none], self.project_path)
        return result

@dataclass
class SessionFSAppendFileRequest:
    content: str
    """Content to append"""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    mode: int | None = None
    """Optional POSIX-style mode for newly created files"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSAppendFileRequest':
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
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSExistsRequest':
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
    exists: bool
    """Whether the path exists"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSExistsResult':
        assert isinstance(obj, dict)
        exists = from_bool(obj.get("exists"))
        return SessionFSExistsResult(exists)

    def to_dict(self) -> dict:
        result: dict = {}
        result["exists"] = from_bool(self.exists)
        return result

@dataclass
class SessionFSMkdirRequest:
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    mode: int | None = None
    """Optional POSIX-style mode for newly created directories"""

    recursive: bool | None = None
    """Create parent directories as needed"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSMkdirRequest':
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
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReadFileRequest':
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
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirRequest':
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
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirWithTypesRequest':
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
    dest: str
    """Destination path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    src: str
    """Source path using SessionFs conventions"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSRenameRequest':
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
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    force: bool | None = None
    """Ignore errors if the path does not exist"""

    recursive: bool | None = None
    """Remove directories and their contents recursively"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSRmRequest':
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

class SessionFSSetProviderConventions(Enum):
    """Path conventions used by this filesystem"""

    POSIX = "posix"
    WINDOWS = "windows"

@dataclass
class SessionFSSetProviderResult:
    success: bool
    """Whether the provider was set successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSSetProviderResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionFSSetProviderResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

@dataclass
class SessionFSStatRequest:
    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSStatRequest':
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
    content: str
    """Content to write"""

    path: str
    """Path using SessionFs conventions"""

    session_id: str
    """Target session identifier"""

    mode: int | None = None
    """Optional POSIX-style mode for newly created files"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSWriteFileRequest':
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
class SessionsForkRequest:
    session_id: str
    """Source session ID to fork from"""

    to_event_id: str | None = None
    """Optional event ID boundary. When provided, the fork includes only events before this ID
    (exclusive). When omitted, all events are included.
    """

    @staticmethod
    def from_dict(obj: Any) -> 'SessionsForkRequest':
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        to_event_id = from_union([from_str, from_none], obj.get("toEventId"))
        return SessionsForkRequest(session_id, to_event_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        if self.to_event_id is not None:
            result["toEventId"] = from_union([from_str, from_none], self.to_event_id)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionsForkResult:
    session_id: str
    """The new forked session's ID"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionsForkResult':
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        return SessionsForkResult(session_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        return result

@dataclass
class ShellExecRequest:
    command: str
    """Shell command to execute"""

    cwd: str | None = None
    """Working directory (defaults to session working directory)"""

    timeout: int | None = None
    """Timeout in milliseconds (default: 30000)"""

    @staticmethod
    def from_dict(obj: Any) -> 'ShellExecRequest':
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

@dataclass
class ShellExecResult:
    process_id: str
    """Unique identifier for tracking streamed output"""

    @staticmethod
    def from_dict(obj: Any) -> 'ShellExecResult':
        assert isinstance(obj, dict)
        process_id = from_str(obj.get("processId"))
        return ShellExecResult(process_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["processId"] = from_str(self.process_id)
        return result

class ShellKillSignal(Enum):
    """Signal to send (default: SIGTERM)"""

    SIGINT = "SIGINT"
    SIGKILL = "SIGKILL"
    SIGTERM = "SIGTERM"

@dataclass
class ShellKillResult:
    killed: bool
    """Whether the signal was sent successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'ShellKillResult':
        assert isinstance(obj, dict)
        killed = from_bool(obj.get("killed"))
        return ShellKillResult(killed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["killed"] = from_bool(self.killed)
        return result

@dataclass
class Skill:
    description: str
    """Description of what the skill does"""

    enabled: bool
    """Whether the skill is currently enabled"""

    name: str
    """Unique identifier for the skill"""

    source: str
    """Source location type (e.g., project, personal, plugin)"""

    user_invocable: bool
    """Whether the skill can be invoked by the user as a slash command"""

    path: str | None = None
    """Absolute path to the skill file"""

    @staticmethod
    def from_dict(obj: Any) -> 'Skill':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = from_str(obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        path = from_union([from_str, from_none], obj.get("path"))
        return Skill(description, enabled, name, source, user_invocable, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = from_str(self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        return result

@dataclass
class SkillsConfigSetDisabledSkillsRequest:
    disabled_skills: list[str]
    """List of skill names to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SkillsConfigSetDisabledSkillsRequest':
        assert isinstance(obj, dict)
        disabled_skills = from_list(from_str, obj.get("disabledSkills"))
        return SkillsConfigSetDisabledSkillsRequest(disabled_skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["disabledSkills"] = from_list(from_str, self.disabled_skills)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SkillsDisableRequest:
    name: str
    """Name of the skill to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SkillsDisableRequest':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SkillsDisableRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

@dataclass
class SkillsDiscoverRequest:
    project_paths: list[str] | None = None
    """Optional list of project directory paths to scan for project-scoped skills"""

    skill_directories: list[str] | None = None
    """Optional list of additional skill directory paths to include"""

    @staticmethod
    def from_dict(obj: Any) -> 'SkillsDiscoverRequest':
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
    name: str
    """Name of the skill to enable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SkillsEnableRequest':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SkillsEnableRequest(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result

@dataclass
class Tool:
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
    def from_dict(obj: Any) -> 'Tool':
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

@dataclass
class ToolCallResult:
    text_result_for_llm: str
    """Text result to send back to the LLM"""

    error: str | None = None
    """Error message if the tool call failed"""

    result_type: str | None = None
    """Type of the tool result"""

    tool_telemetry: dict[str, Any] | None = None
    """Telemetry data from tool execution"""

    @staticmethod
    def from_dict(obj: Any) -> 'ToolCallResult':
        assert isinstance(obj, dict)
        text_result_for_llm = from_str(obj.get("textResultForLlm"))
        error = from_union([from_str, from_none], obj.get("error"))
        result_type = from_union([from_str, from_none], obj.get("resultType"))
        tool_telemetry = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("toolTelemetry"))
        return ToolCallResult(text_result_for_llm, error, result_type, tool_telemetry)

    def to_dict(self) -> dict:
        result: dict = {}
        result["textResultForLlm"] = from_str(self.text_result_for_llm)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.result_type is not None:
            result["resultType"] = from_union([from_str, from_none], self.result_type)
        if self.tool_telemetry is not None:
            result["toolTelemetry"] = from_union([lambda x: from_dict(lambda x: x, x), from_none], self.tool_telemetry)
        return result

@dataclass
class ToolsListRequest:
    model: str | None = None
    """Optional model ID — when provided, the returned tool list reflects model-specific
    overrides
    """

    @staticmethod
    def from_dict(obj: Any) -> 'ToolsListRequest':
        assert isinstance(obj, dict)
        model = from_union([from_str, from_none], obj.get("model"))
        return ToolsListRequest(model)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        return result

@dataclass
class UIElicitationArrayAnyOfFieldItemsAnyOf:
    const: str
    title: str

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayAnyOfFieldItemsAnyOf':
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

class UIElicitationSchemaPropertyStringFormat(Enum):
    DATE = "date"
    DATE_TIME = "date-time"
    EMAIL = "email"
    URI = "uri"

@dataclass
class UIElicitationStringOneOfFieldOneOf:
    const: str
    title: str

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationStringOneOfFieldOneOf':
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
    ARRAY = "array"
    BOOLEAN = "boolean"
    INTEGER = "integer"
    NUMBER = "number"
    STRING = "string"

class UIElicitationSchemaType(Enum):
    OBJECT = "object"

class UIElicitationResponseAction(Enum):
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    ACCEPT = "accept"
    CANCEL = "cancel"
    DECLINE = "decline"

@dataclass
class UIElicitationResult:
    success: bool
    """Whether the response was accepted. False if the request was already resolved by another
    client.
    """

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return UIElicitationResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result

class UIElicitationSchemaPropertyBooleanType(Enum):
    BOOLEAN = "boolean"

class UIElicitationSchemaPropertyNumberType(Enum):
    INTEGER = "integer"
    NUMBER = "number"

@dataclass
class UsageMetricsCodeChanges:
    """Aggregated code change metrics"""

    files_modified_count: int
    """Number of distinct files modified"""

    lines_added: int
    """Total lines of code added"""

    lines_removed: int
    """Total lines of code removed"""

    @staticmethod
    def from_dict(obj: Any) -> 'UsageMetricsCodeChanges':
        assert isinstance(obj, dict)
        files_modified_count = from_int(obj.get("filesModifiedCount"))
        lines_added = from_int(obj.get("linesAdded"))
        lines_removed = from_int(obj.get("linesRemoved"))
        return UsageMetricsCodeChanges(files_modified_count, lines_added, lines_removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["filesModifiedCount"] = from_int(self.files_modified_count)
        result["linesAdded"] = from_int(self.lines_added)
        result["linesRemoved"] = from_int(self.lines_removed)
        return result

@dataclass
class UsageMetricsModelMetricRequests:
    """Request count and cost metrics for this model"""

    cost: float
    """User-initiated premium request cost (with multiplier applied)"""

    count: int
    """Number of API requests made with this model"""

    @staticmethod
    def from_dict(obj: Any) -> 'UsageMetricsModelMetricRequests':
        assert isinstance(obj, dict)
        cost = from_float(obj.get("cost"))
        count = from_int(obj.get("count"))
        return UsageMetricsModelMetricRequests(cost, count)

    def to_dict(self) -> dict:
        result: dict = {}
        result["cost"] = to_float(self.cost)
        result["count"] = from_int(self.count)
        return result

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
    def from_dict(obj: Any) -> 'UsageMetricsModelMetricUsage':
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

@dataclass
class WorkspacesCreateFileRequest:
    content: str
    """File content to write as a UTF-8 string"""

    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspacesCreateFileRequest':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        path = from_str(obj.get("path"))
        return WorkspacesCreateFileRequest(content, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["path"] = from_str(self.path)
        return result

class HostType(Enum):
    ADO = "ado"
    GITHUB = "github"

class SessionSyncLevel(Enum):
    LOCAL = "local"
    REPO_AND_USER = "repo_and_user"
    USER = "user"

@dataclass
class WorkspacesListFilesResult:
    files: list[str]
    """Relative file paths in the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspacesListFilesResult':
        assert isinstance(obj, dict)
        files = from_list(from_str, obj.get("files"))
        return WorkspacesListFilesResult(files)

    def to_dict(self) -> dict:
        result: dict = {}
        result["files"] = from_list(from_str, self.files)
        return result

@dataclass
class WorkspacesReadFileRequest:
    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspacesReadFileRequest':
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return WorkspacesReadFileRequest(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

@dataclass
class WorkspacesReadFileResult:
    content: str
    """File content as a UTF-8 string"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspacesReadFileResult':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return WorkspacesReadFileResult(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result

@dataclass
class AccountGetQuotaResult:
    quota_snapshots: dict[str, AccountQuotaSnapshot]
    """Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)"""

    @staticmethod
    def from_dict(obj: Any) -> 'AccountGetQuotaResult':
        assert isinstance(obj, dict)
        quota_snapshots = from_dict(AccountQuotaSnapshot.from_dict, obj.get("quotaSnapshots"))
        return AccountGetQuotaResult(quota_snapshots)

    def to_dict(self) -> dict:
        result: dict = {}
        result["quotaSnapshots"] = from_dict(lambda x: to_class(AccountQuotaSnapshot, x), self.quota_snapshots)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentGetCurrentResult:
    agent: AgentInfo | None = None
    """Currently selected custom agent, or null if using the default agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentGetCurrentResult':
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
    agents: list[AgentInfo]
    """Available custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentList':
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
    agents: list[AgentInfo]
    """Reloaded custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentReloadResult':
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
    agent: AgentInfo
    """The newly selected custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentSelectResult':
        assert isinstance(obj, dict)
        agent = AgentInfo.from_dict(obj.get("agent"))
        return AgentSelectResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agent"] = to_class(AgentInfo, self.agent)
        return result

@dataclass
class DiscoveredMCPServer:
    enabled: bool
    """Whether the server is enabled (not in the disabled list)"""

    name: str
    """Server name (config key)"""

    source: MCPServerSource
    """Configuration source"""

    type: DiscoveredMCPServerType | None = None
    """Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio)"""

    @staticmethod
    def from_dict(obj: Any) -> 'DiscoveredMCPServer':
        assert isinstance(obj, dict)
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = MCPServerSource(obj.get("source"))
        type = from_union([DiscoveredMCPServerType, from_none], obj.get("type"))
        return DiscoveredMCPServer(enabled, name, source, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(MCPServerSource, self.source)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(DiscoveredMCPServerType, x), from_none], self.type)
        return result

@dataclass
class Extension:
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
    def from_dict(obj: Any) -> 'Extension':
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
class HistoryCompactResult:
    messages_removed: int
    """Number of messages removed during compaction"""

    success: bool
    """Whether compaction completed successfully"""

    tokens_removed: int
    """Number of tokens freed by compaction"""

    context_window: HistoryCompactContextWindow | None = None
    """Post-compaction context window usage breakdown"""

    @staticmethod
    def from_dict(obj: Any) -> 'HistoryCompactResult':
        assert isinstance(obj, dict)
        messages_removed = from_int(obj.get("messagesRemoved"))
        success = from_bool(obj.get("success"))
        tokens_removed = from_int(obj.get("tokensRemoved"))
        context_window = from_union([HistoryCompactContextWindow.from_dict, from_none], obj.get("contextWindow"))
        return HistoryCompactResult(messages_removed, success, tokens_removed, context_window)

    def to_dict(self) -> dict:
        result: dict = {}
        result["messagesRemoved"] = from_int(self.messages_removed)
        result["success"] = from_bool(self.success)
        result["tokensRemoved"] = from_int(self.tokens_removed)
        if self.context_window is not None:
            result["contextWindow"] = from_union([lambda x: to_class(HistoryCompactContextWindow, x), from_none], self.context_window)
        return result

@dataclass
class InstructionsSources:
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

    apply_to: str | None = None
    """Glob pattern from frontmatter — when set, this instruction applies only to matching files"""

    description: str | None = None
    """Short description (body after frontmatter) for use in instruction tables"""

    @staticmethod
    def from_dict(obj: Any) -> 'InstructionsSources':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        id = from_str(obj.get("id"))
        label = from_str(obj.get("label"))
        location = InstructionsSourcesLocation(obj.get("location"))
        source_path = from_str(obj.get("sourcePath"))
        type = InstructionsSourcesType(obj.get("type"))
        apply_to = from_union([from_str, from_none], obj.get("applyTo"))
        description = from_union([from_str, from_none], obj.get("description"))
        return InstructionsSources(content, id, label, location, source_path, type, apply_to, description)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["id"] = from_str(self.id)
        result["label"] = from_str(self.label)
        result["location"] = to_enum(InstructionsSourcesLocation, self.location)
        result["sourcePath"] = from_str(self.source_path)
        result["type"] = to_enum(InstructionsSourcesType, self.type)
        if self.apply_to is not None:
            result["applyTo"] = from_union([from_str, from_none], self.apply_to)
        if self.description is not None:
            result["description"] = from_union([from_str, from_none], self.description)
        return result

@dataclass
class LogRequest:
    message: str
    """Human-readable message"""

    ephemeral: bool | None = None
    """When true, the message is transient and not persisted to the session event log on disk"""

    level: SessionLogLevel | None = None
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    url: str | None = None
    """Optional URL the user can open in their browser for more details"""

    @staticmethod
    def from_dict(obj: Any) -> 'LogRequest':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        ephemeral = from_union([from_bool, from_none], obj.get("ephemeral"))
        level = from_union([SessionLogLevel, from_none], obj.get("level"))
        url = from_union([from_str, from_none], obj.get("url"))
        return LogRequest(message, ephemeral, level, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.ephemeral is not None:
            result["ephemeral"] = from_union([from_bool, from_none], self.ephemeral)
        if self.level is not None:
            result["level"] = from_union([lambda x: to_enum(SessionLogLevel, x), from_none], self.level)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

@dataclass
class MCPServerConfig:
    """MCP server configuration (local/stdio or remote/http)"""

    args: list[str] | None = None
    command: str | None = None
    cwd: str | None = None
    env: dict[str, str] | None = None
    filter_mapping: dict[str, FilterMappingString] | FilterMappingString | None = None
    is_default_server: bool | None = None
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPServerConfigType | None = None
    """Remote transport type. Defaults to "http" when omitted."""

    headers: dict[str, str] | None = None
    oauth_client_id: str | None = None
    oauth_public_client: bool | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'MCPServerConfig':
        assert isinstance(obj, dict)
        args = from_union([lambda x: from_list(from_str, x), from_none], obj.get("args"))
        command = from_union([from_str, from_none], obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(FilterMappingString, x), FilterMappingString, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPServerConfigType, from_none], obj.get("type"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        url = from_union([from_str, from_none], obj.get("url"))
        return MCPServerConfig(args, command, cwd, env, filter_mapping, is_default_server, timeout, tools, type, headers, oauth_client_id, oauth_public_client, url)

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
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(FilterMappingString, x), x), lambda x: to_enum(FilterMappingString, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPServerConfigType, x), from_none], self.type)
        if self.headers is not None:
            result["headers"] = from_union([lambda x: from_dict(from_str, x), from_none], self.headers)
        if self.oauth_client_id is not None:
            result["oauthClientId"] = from_union([from_str, from_none], self.oauth_client_id)
        if self.oauth_public_client is not None:
            result["oauthPublicClient"] = from_union([from_bool, from_none], self.oauth_public_client)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result

@dataclass
class MCPServer:
    name: str
    """Server name (config key)"""

    status: MCPServerStatus
    """Connection status: connected, failed, needs-auth, pending, disabled, or not_configured"""

    error: str | None = None
    """Error message if the server failed to connect"""

    source: MCPServerSource | None = None
    """Configuration source: user, workspace, plugin, or builtin"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPServer':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        status = MCPServerStatus(obj.get("status"))
        error = from_union([from_str, from_none], obj.get("error"))
        source = from_union([MCPServerSource, from_none], obj.get("source"))
        return MCPServer(name, status, error, source)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["status"] = to_enum(MCPServerStatus, self.status)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.source is not None:
            result["source"] = from_union([lambda x: to_enum(MCPServerSource, x), from_none], self.source)
        return result

@dataclass
class MCPServerConfigHTTP:
    url: str
    filter_mapping: dict[str, FilterMappingString] | FilterMappingString | None = None
    headers: dict[str, str] | None = None
    is_default_server: bool | None = None
    oauth_client_id: str | None = None
    oauth_public_client: bool | None = None
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPServerConfigHTTPType | None = None
    """Remote transport type. Defaults to "http" when omitted."""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPServerConfigHTTP':
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        filter_mapping = from_union([lambda x: from_dict(FilterMappingString, x), FilterMappingString, from_none], obj.get("filterMapping"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPServerConfigHTTPType, from_none], obj.get("type"))
        return MCPServerConfigHTTP(url, filter_mapping, headers, is_default_server, oauth_client_id, oauth_public_client, timeout, tools, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["url"] = from_str(self.url)
        if self.filter_mapping is not None:
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(FilterMappingString, x), x), lambda x: to_enum(FilterMappingString, x), from_none], self.filter_mapping)
        if self.headers is not None:
            result["headers"] = from_union([lambda x: from_dict(from_str, x), from_none], self.headers)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.oauth_client_id is not None:
            result["oauthClientId"] = from_union([from_str, from_none], self.oauth_client_id)
        if self.oauth_public_client is not None:
            result["oauthPublicClient"] = from_union([from_bool, from_none], self.oauth_public_client)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPServerConfigHTTPType, x), from_none], self.type)
        return result

@dataclass
class MCPServerConfigLocal:
    args: list[str]
    command: str
    cwd: str | None = None
    env: dict[str, str] | None = None
    filter_mapping: dict[str, FilterMappingString] | FilterMappingString | None = None
    is_default_server: bool | None = None
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPServerConfigLocalType | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'MCPServerConfigLocal':
        assert isinstance(obj, dict)
        args = from_list(from_str, obj.get("args"))
        command = from_str(obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(FilterMappingString, x), FilterMappingString, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPServerConfigLocalType, from_none], obj.get("type"))
        return MCPServerConfigLocal(args, command, cwd, env, filter_mapping, is_default_server, timeout, tools, type)

    def to_dict(self) -> dict:
        result: dict = {}
        result["args"] = from_list(from_str, self.args)
        result["command"] = from_str(self.command)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.env is not None:
            result["env"] = from_union([lambda x: from_dict(from_str, x), from_none], self.env)
        if self.filter_mapping is not None:
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(FilterMappingString, x), x), lambda x: to_enum(FilterMappingString, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPServerConfigLocalType, x), from_none], self.type)
        return result

@dataclass
class ModeSetRequest:
    mode: SessionMode
    """The agent mode. Valid values: "interactive", "plan", "autopilot"."""

    @staticmethod
    def from_dict(obj: Any) -> 'ModeSetRequest':
        assert isinstance(obj, dict)
        mode = SessionMode(obj.get("mode"))
        return ModeSetRequest(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(SessionMode, self.mode)
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
    def from_dict(obj: Any) -> 'ModelCapabilitiesLimits':
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
class ModelCapabilitiesOverrideLimits:
    """Token limits for prompts, outputs, and context window"""

    max_context_window_tokens: int | None = None
    """Maximum total context window size in tokens"""

    max_output_tokens: int | None = None
    max_prompt_tokens: int | None = None
    vision: ModelCapabilitiesOverrideLimitsVision | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'ModelCapabilitiesOverrideLimits':
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

@dataclass
class PermissionDecision:
    kind: PermissionDecisionKind
    """The permission request was approved

    Denied because approval rules explicitly blocked it

    Denied because no approval rule matched and user confirmation was unavailable

    Denied by the user during an interactive prompt

    Denied by the organization's content exclusion policy

    Denied by a permission request hook registered by an extension or plugin
    """
    rules: list[Any] | None = None
    """Rules that denied the request"""

    feedback: str | None = None
    """Optional feedback from the user explaining the denial"""

    message: str | None = None
    """Human-readable explanation of why the path was excluded

    Optional message from the hook explaining the denial
    """
    path: str | None = None
    """File path that triggered the exclusion"""

    interrupt: bool | None = None
    """Whether to interrupt the current agent turn"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecision':
        assert isinstance(obj, dict)
        kind = PermissionDecisionKind(obj.get("kind"))
        rules = from_union([lambda x: from_list(lambda x: x, x), from_none], obj.get("rules"))
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        message = from_union([from_str, from_none], obj.get("message"))
        path = from_union([from_str, from_none], obj.get("path"))
        interrupt = from_union([from_bool, from_none], obj.get("interrupt"))
        return PermissionDecision(kind, rules, feedback, message, path, interrupt)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionKind, self.kind)
        if self.rules is not None:
            result["rules"] = from_union([lambda x: from_list(lambda x: x, x), from_none], self.rules)
        if self.feedback is not None:
            result["feedback"] = from_union([from_str, from_none], self.feedback)
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        if self.interrupt is not None:
            result["interrupt"] = from_union([from_bool, from_none], self.interrupt)
        return result

@dataclass
class PermissionDecisionApproved:
    kind: PermissionDecisionApprovedKind
    """The permission request was approved"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionApproved':
        assert isinstance(obj, dict)
        kind = PermissionDecisionApprovedKind(obj.get("kind"))
        return PermissionDecisionApproved(kind)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionApprovedKind, self.kind)
        return result

@dataclass
class PermissionDecisionDeniedByContentExclusionPolicy:
    kind: PermissionDecisionDeniedByContentExclusionPolicyKind
    """Denied by the organization's content exclusion policy"""

    message: str
    """Human-readable explanation of why the path was excluded"""

    path: str
    """File path that triggered the exclusion"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionDeniedByContentExclusionPolicy':
        assert isinstance(obj, dict)
        kind = PermissionDecisionDeniedByContentExclusionPolicyKind(obj.get("kind"))
        message = from_str(obj.get("message"))
        path = from_str(obj.get("path"))
        return PermissionDecisionDeniedByContentExclusionPolicy(kind, message, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionDeniedByContentExclusionPolicyKind, self.kind)
        result["message"] = from_str(self.message)
        result["path"] = from_str(self.path)
        return result

@dataclass
class PermissionDecisionDeniedByPermissionRequestHook:
    kind: PermissionDecisionDeniedByPermissionRequestHookKind
    """Denied by a permission request hook registered by an extension or plugin"""

    interrupt: bool | None = None
    """Whether to interrupt the current agent turn"""

    message: str | None = None
    """Optional message from the hook explaining the denial"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionDeniedByPermissionRequestHook':
        assert isinstance(obj, dict)
        kind = PermissionDecisionDeniedByPermissionRequestHookKind(obj.get("kind"))
        interrupt = from_union([from_bool, from_none], obj.get("interrupt"))
        message = from_union([from_str, from_none], obj.get("message"))
        return PermissionDecisionDeniedByPermissionRequestHook(kind, interrupt, message)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionDeniedByPermissionRequestHookKind, self.kind)
        if self.interrupt is not None:
            result["interrupt"] = from_union([from_bool, from_none], self.interrupt)
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        return result

@dataclass
class PermissionDecisionDeniedByRules:
    kind: PermissionDecisionDeniedByRulesKind
    """Denied because approval rules explicitly blocked it"""

    rules: list[Any]
    """Rules that denied the request"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionDeniedByRules':
        assert isinstance(obj, dict)
        kind = PermissionDecisionDeniedByRulesKind(obj.get("kind"))
        rules = from_list(lambda x: x, obj.get("rules"))
        return PermissionDecisionDeniedByRules(kind, rules)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionDeniedByRulesKind, self.kind)
        result["rules"] = from_list(lambda x: x, self.rules)
        return result

@dataclass
class PermissionDecisionDeniedInteractivelyByUser:
    kind: PermissionDecisionDeniedInteractivelyByUserKind
    """Denied by the user during an interactive prompt"""

    feedback: str | None = None
    """Optional feedback from the user explaining the denial"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionDeniedInteractivelyByUser':
        assert isinstance(obj, dict)
        kind = PermissionDecisionDeniedInteractivelyByUserKind(obj.get("kind"))
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        return PermissionDecisionDeniedInteractivelyByUser(kind, feedback)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionDeniedInteractivelyByUserKind, self.kind)
        if self.feedback is not None:
            result["feedback"] = from_union([from_str, from_none], self.feedback)
        return result

@dataclass
class PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser:
    kind: PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind
    """Denied because no approval rule matched and user confirmation was unavailable"""

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser':
        assert isinstance(obj, dict)
        kind = PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind(obj.get("kind"))
        return PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser(kind)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind, self.kind)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PluginList:
    plugins: list[Plugin]
    """Installed plugins"""

    @staticmethod
    def from_dict(obj: Any) -> 'PluginList':
        assert isinstance(obj, dict)
        plugins = from_list(Plugin.from_dict, obj.get("plugins"))
        return PluginList(plugins)

    def to_dict(self) -> dict:
        result: dict = {}
        result["plugins"] = from_list(lambda x: to_class(Plugin, x), self.plugins)
        return result

@dataclass
class ServerSkillList:
    skills: list[ServerSkill]
    """All discovered skills across all sources"""

    @staticmethod
    def from_dict(obj: Any) -> 'ServerSkillList':
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
    def from_dict(obj: Any) -> 'SessionFSError':
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
    name: str
    """Entry name"""

    type: SessionFSReaddirWithTypesEntryType
    """Entry type"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirWithTypesEntry':
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
    conventions: SessionFSSetProviderConventions
    """Path conventions used by this filesystem"""

    initial_cwd: str
    """Initial working directory for sessions"""

    session_state_path: str
    """Path within each session's SessionFs where the runtime stores files for that session"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSSetProviderRequest':
        assert isinstance(obj, dict)
        conventions = SessionFSSetProviderConventions(obj.get("conventions"))
        initial_cwd = from_str(obj.get("initialCwd"))
        session_state_path = from_str(obj.get("sessionStatePath"))
        return SessionFSSetProviderRequest(conventions, initial_cwd, session_state_path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["conventions"] = to_enum(SessionFSSetProviderConventions, self.conventions)
        result["initialCwd"] = from_str(self.initial_cwd)
        result["sessionStatePath"] = from_str(self.session_state_path)
        return result

@dataclass
class ShellKillRequest:
    process_id: str
    """Process identifier returned by shell.exec"""

    signal: ShellKillSignal | None = None
    """Signal to send (default: SIGTERM)"""

    @staticmethod
    def from_dict(obj: Any) -> 'ShellKillRequest':
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
class SkillList:
    skills: list[Skill]
    """Available skills"""

    @staticmethod
    def from_dict(obj: Any) -> 'SkillList':
        assert isinstance(obj, dict)
        skills = from_list(Skill.from_dict, obj.get("skills"))
        return SkillList(skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["skills"] = from_list(lambda x: to_class(Skill, x), self.skills)
        return result

@dataclass
class ToolList:
    tools: list[Tool]
    """List of available built-in tools with metadata"""

    @staticmethod
    def from_dict(obj: Any) -> 'ToolList':
        assert isinstance(obj, dict)
        tools = from_list(Tool.from_dict, obj.get("tools"))
        return ToolList(tools)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tools"] = from_list(lambda x: to_class(Tool, x), self.tools)
        return result

@dataclass
class ToolsHandlePendingToolCallRequest:
    request_id: str
    """Request ID of the pending tool call"""

    error: str | None = None
    """Error message if the tool call failed"""

    result: ToolCallResult | str | None = None
    """Tool call result (string or expanded result object)"""

    @staticmethod
    def from_dict(obj: Any) -> 'ToolsHandlePendingToolCallRequest':
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        error = from_union([from_str, from_none], obj.get("error"))
        result = from_union([ToolCallResult.from_dict, from_str, from_none], obj.get("result"))
        return ToolsHandlePendingToolCallRequest(request_id, error, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.result is not None:
            result["result"] = from_union([lambda x: to_class(ToolCallResult, x), from_str, from_none], self.result)
        return result

@dataclass
class UIElicitationArrayAnyOfFieldItems:
    any_of: list[UIElicitationArrayAnyOfFieldItemsAnyOf]

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayAnyOfFieldItems':
        assert isinstance(obj, dict)
        any_of = from_list(UIElicitationArrayAnyOfFieldItemsAnyOf.from_dict, obj.get("anyOf"))
        return UIElicitationArrayAnyOfFieldItems(any_of)

    def to_dict(self) -> dict:
        result: dict = {}
        result["anyOf"] = from_list(lambda x: to_class(UIElicitationArrayAnyOfFieldItemsAnyOf, x), self.any_of)
        return result

@dataclass
class UIElicitationArrayEnumFieldItems:
    enum: list[str]
    type: UIElicitationArrayEnumFieldItemsType

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayEnumFieldItems':
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
    enum: list[str] | None = None
    type: UIElicitationArrayEnumFieldItemsType | None = None
    any_of: list[UIElicitationArrayAnyOfFieldItemsAnyOf] | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayFieldItems':
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

@dataclass
class UIElicitationStringEnumField:
    enum: list[str]
    type: UIElicitationArrayEnumFieldItemsType
    default: str | None = None
    description: str | None = None
    enum_names: list[str] | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationStringEnumField':
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

@dataclass
class UIElicitationSchemaPropertyString:
    type: UIElicitationArrayEnumFieldItemsType
    default: str | None = None
    description: str | None = None
    format: UIElicitationSchemaPropertyStringFormat | None = None
    max_length: float | None = None
    min_length: float | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationSchemaPropertyString':
        assert isinstance(obj, dict)
        type = UIElicitationArrayEnumFieldItemsType(obj.get("type"))
        default = from_union([from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        format = from_union([UIElicitationSchemaPropertyStringFormat, from_none], obj.get("format"))
        max_length = from_union([from_float, from_none], obj.get("maxLength"))
        min_length = from_union([from_float, from_none], obj.get("minLength"))
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
            result["maxLength"] = from_union([to_float, from_none], self.max_length)
        if self.min_length is not None:
            result["minLength"] = from_union([to_float, from_none], self.min_length)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

@dataclass
class UIElicitationStringOneOfField:
    one_of: list[UIElicitationStringOneOfFieldOneOf]
    type: UIElicitationArrayEnumFieldItemsType
    default: str | None = None
    description: str | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationStringOneOfField':
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

@dataclass
class UIElicitationResponse:
    """The elicitation response (accept with form values, decline, or cancel)"""

    action: UIElicitationResponseAction
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    content: dict[str, float | bool | list[str] | str] | None = None
    """The form values submitted by the user (present when action is 'accept')"""

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationResponse':
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

@dataclass
class UIElicitationSchemaPropertyBoolean:
    type: UIElicitationSchemaPropertyBooleanType
    default: bool | None = None
    description: str | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationSchemaPropertyBoolean':
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

@dataclass
class UIElicitationSchemaPropertyNumber:
    type: UIElicitationSchemaPropertyNumberType
    default: float | None = None
    description: str | None = None
    maximum: float | None = None
    minimum: float | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationSchemaPropertyNumber':
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

@dataclass
class UsageMetricsModelMetric:
    requests: UsageMetricsModelMetricRequests
    """Request count and cost metrics for this model"""

    usage: UsageMetricsModelMetricUsage
    """Token usage metrics for this model"""

    @staticmethod
    def from_dict(obj: Any) -> 'UsageMetricsModelMetric':
        assert isinstance(obj, dict)
        requests = UsageMetricsModelMetricRequests.from_dict(obj.get("requests"))
        usage = UsageMetricsModelMetricUsage.from_dict(obj.get("usage"))
        return UsageMetricsModelMetric(requests, usage)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requests"] = to_class(UsageMetricsModelMetricRequests, self.requests)
        result["usage"] = to_class(UsageMetricsModelMetricUsage, self.usage)
        return result

@dataclass
class Workspace:
    id: UUID
    branch: str | None = None
    chronicle_sync_dismissed: bool | None = None
    created_at: datetime | None = None
    cwd: str | None = None
    git_root: str | None = None
    host_type: HostType | None = None
    mc_last_event_id: str | None = None
    mc_session_id: str | None = None
    mc_task_id: str | None = None
    name: str | None = None
    remote_steerable: bool | None = None
    repository: str | None = None
    session_sync_level: SessionSyncLevel | None = None
    summary: str | None = None
    summary_count: int | None = None
    updated_at: datetime | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'Workspace':
        assert isinstance(obj, dict)
        id = UUID(obj.get("id"))
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
        session_sync_level = from_union([SessionSyncLevel, from_none], obj.get("session_sync_level"))
        summary = from_union([from_str, from_none], obj.get("summary"))
        summary_count = from_union([from_int, from_none], obj.get("summary_count"))
        updated_at = from_union([from_datetime, from_none], obj.get("updated_at"))
        return Workspace(id, branch, chronicle_sync_dismissed, created_at, cwd, git_root, host_type, mc_last_event_id, mc_session_id, mc_task_id, name, remote_steerable, repository, session_sync_level, summary, summary_count, updated_at)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = str(self.id)
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
        if self.session_sync_level is not None:
            result["session_sync_level"] = from_union([lambda x: to_enum(SessionSyncLevel, x), from_none], self.session_sync_level)
        if self.summary is not None:
            result["summary"] = from_union([from_str, from_none], self.summary)
        if self.summary_count is not None:
            result["summary_count"] = from_union([from_int, from_none], self.summary_count)
        if self.updated_at is not None:
            result["updated_at"] = from_union([lambda x: x.isoformat(), from_none], self.updated_at)
        return result

@dataclass
class MCPDiscoverResult:
    servers: list[DiscoveredMCPServer]
    """MCP servers discovered from all sources"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPDiscoverResult':
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
    extensions: list[Extension]
    """Discovered extensions and their current status"""

    @staticmethod
    def from_dict(obj: Any) -> 'ExtensionList':
        assert isinstance(obj, dict)
        extensions = from_list(Extension.from_dict, obj.get("extensions"))
        return ExtensionList(extensions)

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensions"] = from_list(lambda x: to_class(Extension, x), self.extensions)
        return result

@dataclass
class InstructionsGetSourcesResult:
    sources: list[InstructionsSources]
    """Instruction sources for the session"""

    @staticmethod
    def from_dict(obj: Any) -> 'InstructionsGetSourcesResult':
        assert isinstance(obj, dict)
        sources = from_list(InstructionsSources.from_dict, obj.get("sources"))
        return InstructionsGetSourcesResult(sources)

    def to_dict(self) -> dict:
        result: dict = {}
        result["sources"] = from_list(lambda x: to_class(InstructionsSources, x), self.sources)
        return result

@dataclass
class MCPConfigAddRequest:
    config: MCPServerConfig
    """MCP server configuration (local/stdio or remote/http)"""

    name: str
    """Unique name for the MCP server"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigAddRequest':
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
    servers: dict[str, MCPServerConfig]
    """All MCP servers from user config, keyed by name"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigList':
        assert isinstance(obj, dict)
        servers = from_dict(MCPServerConfig.from_dict, obj.get("servers"))
        return MCPConfigList(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_dict(lambda x: to_class(MCPServerConfig, x), self.servers)
        return result

@dataclass
class MCPConfigUpdateRequest:
    config: MCPServerConfig
    """MCP server configuration (local/stdio or remote/http)"""

    name: str
    """Name of the MCP server to update"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigUpdateRequest':
        assert isinstance(obj, dict)
        config = MCPServerConfig.from_dict(obj.get("config"))
        name = from_str(obj.get("name"))
        return MCPConfigUpdateRequest(config, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["config"] = to_class(MCPServerConfig, self.config)
        result["name"] = from_str(self.name)
        return result

@dataclass
class MCPServerList:
    servers: list[MCPServer]
    """Configured MCP servers"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPServerList':
        assert isinstance(obj, dict)
        servers = from_list(MCPServer.from_dict, obj.get("servers"))
        return MCPServerList(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_list(lambda x: to_class(MCPServer, x), self.servers)
        return result

@dataclass
class ModelCapabilitiesOverride:
    """Override individual model capabilities resolved by the runtime"""

    limits: ModelCapabilitiesOverrideLimits | None = None
    """Token limits for prompts, outputs, and context window"""

    supports: ModelCapabilitiesOverrideSupports | None = None
    """Feature flags indicating what the model supports"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelCapabilitiesOverride':
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

@dataclass
class PermissionDecisionRequest:
    request_id: str
    """Request ID of the pending permission request"""

    result: PermissionDecision

    @staticmethod
    def from_dict(obj: Any) -> 'PermissionDecisionRequest':
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = PermissionDecision.from_dict(obj.get("result"))
        return PermissionDecisionRequest(request_id, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = to_class(PermissionDecision, self.result)
        return result

@dataclass
class SessionFSReadFileResult:
    content: str
    """File content as UTF-8 string"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReadFileResult':
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
    entries: list[str]
    """Entry names in the directory"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirResult':
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
class SessionFSStatResult:
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
    def from_dict(obj: Any) -> 'SessionFSStatResult':
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
    entries: list[SessionFSReaddirWithTypesEntry]
    """Directory entries with type information"""

    error: SessionFSError | None = None
    """Describes a filesystem error."""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirWithTypesResult':
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

@dataclass
class UIElicitationArrayAnyOfField:
    items: UIElicitationArrayAnyOfFieldItems
    type: UIElicitationArrayAnyOfFieldType
    default: list[str] | None = None
    description: str | None = None
    max_items: float | None = None
    min_items: float | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayAnyOfField':
        assert isinstance(obj, dict)
        items = UIElicitationArrayAnyOfFieldItems.from_dict(obj.get("items"))
        type = UIElicitationArrayAnyOfFieldType(obj.get("type"))
        default = from_union([lambda x: from_list(from_str, x), from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        max_items = from_union([from_float, from_none], obj.get("maxItems"))
        min_items = from_union([from_float, from_none], obj.get("minItems"))
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
            result["maxItems"] = from_union([to_float, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([to_float, from_none], self.min_items)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

@dataclass
class UIElicitationArrayEnumField:
    items: UIElicitationArrayEnumFieldItems
    type: UIElicitationArrayAnyOfFieldType
    default: list[str] | None = None
    description: str | None = None
    max_items: float | None = None
    min_items: float | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayEnumField':
        assert isinstance(obj, dict)
        items = UIElicitationArrayEnumFieldItems.from_dict(obj.get("items"))
        type = UIElicitationArrayAnyOfFieldType(obj.get("type"))
        default = from_union([lambda x: from_list(from_str, x), from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        max_items = from_union([from_float, from_none], obj.get("maxItems"))
        min_items = from_union([from_float, from_none], obj.get("minItems"))
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
            result["maxItems"] = from_union([to_float, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([to_float, from_none], self.min_items)
        if self.title is not None:
            result["title"] = from_union([from_str, from_none], self.title)
        return result

@dataclass
class UIElicitationSchemaProperty:
    type: UIElicitationSchemaPropertyType
    default: float | bool | list[str] | str | None = None
    description: str | None = None
    enum: list[str] | None = None
    enum_names: list[str] | None = None
    title: str | None = None
    one_of: list[UIElicitationStringOneOfFieldOneOf] | None = None
    items: UIElicitationArrayFieldItems | None = None
    max_items: float | None = None
    min_items: float | None = None
    format: UIElicitationSchemaPropertyStringFormat | None = None
    max_length: float | None = None
    min_length: float | None = None
    maximum: float | None = None
    minimum: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationSchemaProperty':
        assert isinstance(obj, dict)
        type = UIElicitationSchemaPropertyType(obj.get("type"))
        default = from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        enum = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enum"))
        enum_names = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enumNames"))
        title = from_union([from_str, from_none], obj.get("title"))
        one_of = from_union([lambda x: from_list(UIElicitationStringOneOfFieldOneOf.from_dict, x), from_none], obj.get("oneOf"))
        items = from_union([UIElicitationArrayFieldItems.from_dict, from_none], obj.get("items"))
        max_items = from_union([from_float, from_none], obj.get("maxItems"))
        min_items = from_union([from_float, from_none], obj.get("minItems"))
        format = from_union([UIElicitationSchemaPropertyStringFormat, from_none], obj.get("format"))
        max_length = from_union([from_float, from_none], obj.get("maxLength"))
        min_length = from_union([from_float, from_none], obj.get("minLength"))
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
            result["maxItems"] = from_union([to_float, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([to_float, from_none], self.min_items)
        if self.format is not None:
            result["format"] = from_union([lambda x: to_enum(UIElicitationSchemaPropertyStringFormat, x), from_none], self.format)
        if self.max_length is not None:
            result["maxLength"] = from_union([to_float, from_none], self.max_length)
        if self.min_length is not None:
            result["minLength"] = from_union([to_float, from_none], self.min_length)
        if self.maximum is not None:
            result["maximum"] = from_union([to_float, from_none], self.maximum)
        if self.minimum is not None:
            result["minimum"] = from_union([to_float, from_none], self.minimum)
        return result

@dataclass
class UIHandlePendingElicitationRequest:
    request_id: str
    """The unique request ID from the elicitation.requested event"""

    result: UIElicitationResponse
    """The elicitation response (accept with form values, decline, or cancel)"""

    @staticmethod
    def from_dict(obj: Any) -> 'UIHandlePendingElicitationRequest':
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
class UsageGetMetricsResult:
    code_changes: UsageMetricsCodeChanges
    """Aggregated code change metrics"""

    last_call_input_tokens: int
    """Input tokens from the most recent main-agent API call"""

    last_call_output_tokens: int
    """Output tokens from the most recent main-agent API call"""

    model_metrics: dict[str, UsageMetricsModelMetric]
    """Per-model token and request metrics, keyed by model identifier"""

    session_start_time: int
    """Session start timestamp (epoch milliseconds)"""

    total_api_duration_ms: float
    """Total time spent in model API calls (milliseconds)"""

    total_premium_request_cost: float
    """Total user-initiated premium request cost across all models (may be fractional due to
    multipliers)
    """
    total_user_requests: int
    """Raw count of user-initiated API requests"""

    current_model: str | None = None
    """Currently active model identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'UsageGetMetricsResult':
        assert isinstance(obj, dict)
        code_changes = UsageMetricsCodeChanges.from_dict(obj.get("codeChanges"))
        last_call_input_tokens = from_int(obj.get("lastCallInputTokens"))
        last_call_output_tokens = from_int(obj.get("lastCallOutputTokens"))
        model_metrics = from_dict(UsageMetricsModelMetric.from_dict, obj.get("modelMetrics"))
        session_start_time = from_int(obj.get("sessionStartTime"))
        total_api_duration_ms = from_float(obj.get("totalApiDurationMs"))
        total_premium_request_cost = from_float(obj.get("totalPremiumRequestCost"))
        total_user_requests = from_int(obj.get("totalUserRequests"))
        current_model = from_union([from_str, from_none], obj.get("currentModel"))
        return UsageGetMetricsResult(code_changes, last_call_input_tokens, last_call_output_tokens, model_metrics, session_start_time, total_api_duration_ms, total_premium_request_cost, total_user_requests, current_model)

    def to_dict(self) -> dict:
        result: dict = {}
        result["codeChanges"] = to_class(UsageMetricsCodeChanges, self.code_changes)
        result["lastCallInputTokens"] = from_int(self.last_call_input_tokens)
        result["lastCallOutputTokens"] = from_int(self.last_call_output_tokens)
        result["modelMetrics"] = from_dict(lambda x: to_class(UsageMetricsModelMetric, x), self.model_metrics)
        result["sessionStartTime"] = from_int(self.session_start_time)
        result["totalApiDurationMs"] = to_float(self.total_api_duration_ms)
        result["totalPremiumRequestCost"] = to_float(self.total_premium_request_cost)
        result["totalUserRequests"] = from_int(self.total_user_requests)
        if self.current_model is not None:
            result["currentModel"] = from_union([from_str, from_none], self.current_model)
        return result

@dataclass
class WorkspacesGetWorkspaceResult:
    workspace: Workspace | None = None
    """Current workspace metadata, or null if not available"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspacesGetWorkspaceResult':
        assert isinstance(obj, dict)
        workspace = from_union([Workspace.from_dict, from_none], obj.get("workspace"))
        return WorkspacesGetWorkspaceResult(workspace)

    def to_dict(self) -> dict:
        result: dict = {}
        result["workspace"] = from_union([lambda x: to_class(Workspace, x), from_none], self.workspace)
        return result

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
    def from_dict(obj: Any) -> 'UIElicitationSchema':
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

@dataclass
class UIElicitationRequest:
    message: str
    """Message describing what information is needed from the user"""

    requested_schema: UIElicitationSchema
    """JSON Schema describing the form fields to present to the user"""

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationRequest':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        requested_schema = UIElicitationSchema.from_dict(obj.get("requestedSchema"))
        return UIElicitationRequest(message, requested_schema)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["requestedSchema"] = to_class(UIElicitationSchema, self.requested_schema)
        return result

@dataclass
class ModelCapabilities:
    """Model capabilities and limits"""

    limits: ModelCapabilitiesLimits | None = None
    """Token limits for prompts, outputs, and context window"""

    supports: ModelCapabilitiesSupports | None = None
    """Feature flags indicating what the model supports"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelCapabilities':
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

@dataclass
class Model:
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

    policy: ModelPolicy | None = None
    """Policy state (if applicable)"""

    supported_reasoning_efforts: list[str] | None = None
    """Supported reasoning effort levels (only present if model supports reasoning effort)"""

    @staticmethod
    def from_dict(obj: Any) -> 'Model':
        assert isinstance(obj, dict)
        capabilities = ModelCapabilities.from_dict(obj.get("capabilities"))
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        billing = from_union([ModelBilling.from_dict, from_none], obj.get("billing"))
        default_reasoning_effort = from_union([from_str, from_none], obj.get("defaultReasoningEffort"))
        policy = from_union([ModelPolicy.from_dict, from_none], obj.get("policy"))
        supported_reasoning_efforts = from_union([lambda x: from_list(from_str, x), from_none], obj.get("supportedReasoningEfforts"))
        return Model(capabilities, id, name, billing, default_reasoning_effort, policy, supported_reasoning_efforts)

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = to_class(ModelCapabilities, self.capabilities)
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        if self.billing is not None:
            result["billing"] = from_union([lambda x: to_class(ModelBilling, x), from_none], self.billing)
        if self.default_reasoning_effort is not None:
            result["defaultReasoningEffort"] = from_union([from_str, from_none], self.default_reasoning_effort)
        if self.policy is not None:
            result["policy"] = from_union([lambda x: to_class(ModelPolicy, x), from_none], self.policy)
        if self.supported_reasoning_efforts is not None:
            result["supportedReasoningEfforts"] = from_union([lambda x: from_list(from_str, x), from_none], self.supported_reasoning_efforts)
        return result

@dataclass
class ModelList:
    models: list[Model]
    """List of available models with full metadata"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelList':
        assert isinstance(obj, dict)
        models = from_list(Model.from_dict, obj.get("models"))
        return ModelList(models)

    def to_dict(self) -> dict:
        result: dict = {}
        result["models"] = from_list(lambda x: to_class(Model, x), self.models)
        return result

@dataclass
class ModelSwitchToRequest:
    model_id: str
    """Model identifier to switch to"""

    model_capabilities: ModelCapabilitiesOverride | None = None
    """Override individual model capabilities resolved by the runtime"""

    reasoning_effort: str | None = None
    """Reasoning effort level to use for the model"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelSwitchToRequest':
        assert isinstance(obj, dict)
        model_id = from_str(obj.get("modelId"))
        model_capabilities = from_union([ModelCapabilitiesOverride.from_dict, from_none], obj.get("modelCapabilities"))
        reasoning_effort = from_union([from_str, from_none], obj.get("reasoningEffort"))
        return ModelSwitchToRequest(model_id, model_capabilities, reasoning_effort)

    def to_dict(self) -> dict:
        result: dict = {}
        result["modelId"] = from_str(self.model_id)
        if self.model_capabilities is not None:
            result["modelCapabilities"] = from_union([lambda x: to_class(ModelCapabilitiesOverride, x), from_none], self.model_capabilities)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_str, from_none], self.reasoning_effort)
        return result

@dataclass
class RPC:
    account_get_quota_result: AccountGetQuotaResult
    account_quota_snapshot: AccountQuotaSnapshot
    agent_get_current_result: AgentGetCurrentResult
    agent_info: AgentInfo
    agent_list: AgentList
    agent_reload_result: AgentReloadResult
    agent_select_request: AgentSelectRequest
    agent_select_result: AgentSelectResult
    commands_handle_pending_command_request: CommandsHandlePendingCommandRequest
    commands_handle_pending_command_result: CommandsHandlePendingCommandResult
    current_model: CurrentModel
    discovered_mcp_server: DiscoveredMCPServer
    discovered_mcp_server_source: MCPServerSource
    discovered_mcp_server_type: DiscoveredMCPServerType
    extension: Extension
    extension_list: ExtensionList
    extensions_disable_request: ExtensionsDisableRequest
    extensions_enable_request: ExtensionsEnableRequest
    extension_source: ExtensionSource
    extension_status: ExtensionStatus
    filter_mapping: dict[str, FilterMappingString] | FilterMappingString
    filter_mapping_string: FilterMappingString
    filter_mapping_value: FilterMappingString
    fleet_start_request: FleetStartRequest
    fleet_start_result: FleetStartResult
    handle_tool_call_result: HandleToolCallResult
    history_compact_context_window: HistoryCompactContextWindow
    history_compact_result: HistoryCompactResult
    history_truncate_request: HistoryTruncateRequest
    history_truncate_result: HistoryTruncateResult
    instructions_get_sources_result: InstructionsGetSourcesResult
    instructions_sources: InstructionsSources
    instructions_sources_location: InstructionsSourcesLocation
    instructions_sources_type: InstructionsSourcesType
    log_request: LogRequest
    log_result: LogResult
    mcp_config_add_request: MCPConfigAddRequest
    mcp_config_list: MCPConfigList
    mcp_config_remove_request: MCPConfigRemoveRequest
    mcp_config_update_request: MCPConfigUpdateRequest
    mcp_disable_request: MCPDisableRequest
    mcp_discover_request: MCPDiscoverRequest
    mcp_discover_result: MCPDiscoverResult
    mcp_enable_request: MCPEnableRequest
    mcp_server: MCPServer
    mcp_server_config: MCPServerConfig
    mcp_server_config_http: MCPServerConfigHTTP
    mcp_server_config_http_type: MCPServerConfigHTTPType
    mcp_server_config_local: MCPServerConfigLocal
    mcp_server_config_local_type: MCPServerConfigLocalType
    mcp_server_list: MCPServerList
    mcp_server_source: MCPServerSource
    mcp_server_status: MCPServerStatus
    model: Model
    model_billing: ModelBilling
    model_capabilities: ModelCapabilities
    model_capabilities_limits: ModelCapabilitiesLimits
    model_capabilities_limits_vision: ModelCapabilitiesLimitsVision
    model_capabilities_override: ModelCapabilitiesOverride
    model_capabilities_override_limits: ModelCapabilitiesOverrideLimits
    model_capabilities_override_limits_vision: ModelCapabilitiesOverrideLimitsVision
    model_capabilities_override_supports: ModelCapabilitiesOverrideSupports
    model_capabilities_supports: ModelCapabilitiesSupports
    model_list: ModelList
    model_policy: ModelPolicy
    model_switch_to_request: ModelSwitchToRequest
    model_switch_to_result: ModelSwitchToResult
    mode_set_request: ModeSetRequest
    name_get_result: NameGetResult
    name_set_request: NameSetRequest
    permission_decision: PermissionDecision
    permission_decision_approved: PermissionDecisionApproved
    permission_decision_denied_by_content_exclusion_policy: PermissionDecisionDeniedByContentExclusionPolicy
    permission_decision_denied_by_permission_request_hook: PermissionDecisionDeniedByPermissionRequestHook
    permission_decision_denied_by_rules: PermissionDecisionDeniedByRules
    permission_decision_denied_interactively_by_user: PermissionDecisionDeniedInteractivelyByUser
    permission_decision_denied_no_approval_rule_and_could_not_request_from_user: PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser
    permission_decision_request: PermissionDecisionRequest
    permission_request_result: PermissionRequestResult
    ping_request: PingRequest
    ping_result: PingResult
    plan_read_result: PlanReadResult
    plan_update_request: PlanUpdateRequest
    plugin: Plugin
    plugin_list: PluginList
    server_skill: ServerSkill
    server_skill_list: ServerSkillList
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
    session_fs_set_provider_conventions: SessionFSSetProviderConventions
    session_fs_set_provider_request: SessionFSSetProviderRequest
    session_fs_set_provider_result: SessionFSSetProviderResult
    session_fs_stat_request: SessionFSStatRequest
    session_fs_stat_result: SessionFSStatResult
    session_fs_write_file_request: SessionFSWriteFileRequest
    session_log_level: SessionLogLevel
    session_mode: SessionMode
    sessions_fork_request: SessionsForkRequest
    sessions_fork_result: SessionsForkResult
    shell_exec_request: ShellExecRequest
    shell_exec_result: ShellExecResult
    shell_kill_request: ShellKillRequest
    shell_kill_result: ShellKillResult
    shell_kill_signal: ShellKillSignal
    skill: Skill
    skill_list: SkillList
    skills_config_set_disabled_skills_request: SkillsConfigSetDisabledSkillsRequest
    skills_disable_request: SkillsDisableRequest
    skills_discover_request: SkillsDiscoverRequest
    skills_enable_request: SkillsEnableRequest
    tool: Tool
    tool_call_result: ToolCallResult
    tool_list: ToolList
    tools_handle_pending_tool_call: ToolCallResult | str
    tools_handle_pending_tool_call_request: ToolsHandlePendingToolCallRequest
    tools_list_request: ToolsListRequest
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
    ui_handle_pending_elicitation_request: UIHandlePendingElicitationRequest
    usage_get_metrics_result: UsageGetMetricsResult
    usage_metrics_code_changes: UsageMetricsCodeChanges
    usage_metrics_model_metric: UsageMetricsModelMetric
    usage_metrics_model_metric_requests: UsageMetricsModelMetricRequests
    usage_metrics_model_metric_usage: UsageMetricsModelMetricUsage
    workspaces_create_file_request: WorkspacesCreateFileRequest
    workspaces_get_workspace_result: WorkspacesGetWorkspaceResult
    workspaces_list_files_result: WorkspacesListFilesResult
    workspaces_read_file_request: WorkspacesReadFileRequest
    workspaces_read_file_result: WorkspacesReadFileResult

    @staticmethod
    def from_dict(obj: Any) -> 'RPC':
        assert isinstance(obj, dict)
        account_get_quota_result = AccountGetQuotaResult.from_dict(obj.get("AccountGetQuotaResult"))
        account_quota_snapshot = AccountQuotaSnapshot.from_dict(obj.get("AccountQuotaSnapshot"))
        agent_get_current_result = AgentGetCurrentResult.from_dict(obj.get("AgentGetCurrentResult"))
        agent_info = AgentInfo.from_dict(obj.get("AgentInfo"))
        agent_list = AgentList.from_dict(obj.get("AgentList"))
        agent_reload_result = AgentReloadResult.from_dict(obj.get("AgentReloadResult"))
        agent_select_request = AgentSelectRequest.from_dict(obj.get("AgentSelectRequest"))
        agent_select_result = AgentSelectResult.from_dict(obj.get("AgentSelectResult"))
        commands_handle_pending_command_request = CommandsHandlePendingCommandRequest.from_dict(obj.get("CommandsHandlePendingCommandRequest"))
        commands_handle_pending_command_result = CommandsHandlePendingCommandResult.from_dict(obj.get("CommandsHandlePendingCommandResult"))
        current_model = CurrentModel.from_dict(obj.get("CurrentModel"))
        discovered_mcp_server = DiscoveredMCPServer.from_dict(obj.get("DiscoveredMcpServer"))
        discovered_mcp_server_source = MCPServerSource(obj.get("DiscoveredMcpServerSource"))
        discovered_mcp_server_type = DiscoveredMCPServerType(obj.get("DiscoveredMcpServerType"))
        extension = Extension.from_dict(obj.get("Extension"))
        extension_list = ExtensionList.from_dict(obj.get("ExtensionList"))
        extensions_disable_request = ExtensionsDisableRequest.from_dict(obj.get("ExtensionsDisableRequest"))
        extensions_enable_request = ExtensionsEnableRequest.from_dict(obj.get("ExtensionsEnableRequest"))
        extension_source = ExtensionSource(obj.get("ExtensionSource"))
        extension_status = ExtensionStatus(obj.get("ExtensionStatus"))
        filter_mapping = from_union([lambda x: from_dict(FilterMappingString, x), FilterMappingString], obj.get("FilterMapping"))
        filter_mapping_string = FilterMappingString(obj.get("FilterMappingString"))
        filter_mapping_value = FilterMappingString(obj.get("FilterMappingValue"))
        fleet_start_request = FleetStartRequest.from_dict(obj.get("FleetStartRequest"))
        fleet_start_result = FleetStartResult.from_dict(obj.get("FleetStartResult"))
        handle_tool_call_result = HandleToolCallResult.from_dict(obj.get("HandleToolCallResult"))
        history_compact_context_window = HistoryCompactContextWindow.from_dict(obj.get("HistoryCompactContextWindow"))
        history_compact_result = HistoryCompactResult.from_dict(obj.get("HistoryCompactResult"))
        history_truncate_request = HistoryTruncateRequest.from_dict(obj.get("HistoryTruncateRequest"))
        history_truncate_result = HistoryTruncateResult.from_dict(obj.get("HistoryTruncateResult"))
        instructions_get_sources_result = InstructionsGetSourcesResult.from_dict(obj.get("InstructionsGetSourcesResult"))
        instructions_sources = InstructionsSources.from_dict(obj.get("InstructionsSources"))
        instructions_sources_location = InstructionsSourcesLocation(obj.get("InstructionsSourcesLocation"))
        instructions_sources_type = InstructionsSourcesType(obj.get("InstructionsSourcesType"))
        log_request = LogRequest.from_dict(obj.get("LogRequest"))
        log_result = LogResult.from_dict(obj.get("LogResult"))
        mcp_config_add_request = MCPConfigAddRequest.from_dict(obj.get("McpConfigAddRequest"))
        mcp_config_list = MCPConfigList.from_dict(obj.get("McpConfigList"))
        mcp_config_remove_request = MCPConfigRemoveRequest.from_dict(obj.get("McpConfigRemoveRequest"))
        mcp_config_update_request = MCPConfigUpdateRequest.from_dict(obj.get("McpConfigUpdateRequest"))
        mcp_disable_request = MCPDisableRequest.from_dict(obj.get("McpDisableRequest"))
        mcp_discover_request = MCPDiscoverRequest.from_dict(obj.get("McpDiscoverRequest"))
        mcp_discover_result = MCPDiscoverResult.from_dict(obj.get("McpDiscoverResult"))
        mcp_enable_request = MCPEnableRequest.from_dict(obj.get("McpEnableRequest"))
        mcp_server = MCPServer.from_dict(obj.get("McpServer"))
        mcp_server_config = MCPServerConfig.from_dict(obj.get("McpServerConfig"))
        mcp_server_config_http = MCPServerConfigHTTP.from_dict(obj.get("McpServerConfigHttp"))
        mcp_server_config_http_type = MCPServerConfigHTTPType(obj.get("McpServerConfigHttpType"))
        mcp_server_config_local = MCPServerConfigLocal.from_dict(obj.get("McpServerConfigLocal"))
        mcp_server_config_local_type = MCPServerConfigLocalType(obj.get("McpServerConfigLocalType"))
        mcp_server_list = MCPServerList.from_dict(obj.get("McpServerList"))
        mcp_server_source = MCPServerSource(obj.get("McpServerSource"))
        mcp_server_status = MCPServerStatus(obj.get("McpServerStatus"))
        model = Model.from_dict(obj.get("Model"))
        model_billing = ModelBilling.from_dict(obj.get("ModelBilling"))
        model_capabilities = ModelCapabilities.from_dict(obj.get("ModelCapabilities"))
        model_capabilities_limits = ModelCapabilitiesLimits.from_dict(obj.get("ModelCapabilitiesLimits"))
        model_capabilities_limits_vision = ModelCapabilitiesLimitsVision.from_dict(obj.get("ModelCapabilitiesLimitsVision"))
        model_capabilities_override = ModelCapabilitiesOverride.from_dict(obj.get("ModelCapabilitiesOverride"))
        model_capabilities_override_limits = ModelCapabilitiesOverrideLimits.from_dict(obj.get("ModelCapabilitiesOverrideLimits"))
        model_capabilities_override_limits_vision = ModelCapabilitiesOverrideLimitsVision.from_dict(obj.get("ModelCapabilitiesOverrideLimitsVision"))
        model_capabilities_override_supports = ModelCapabilitiesOverrideSupports.from_dict(obj.get("ModelCapabilitiesOverrideSupports"))
        model_capabilities_supports = ModelCapabilitiesSupports.from_dict(obj.get("ModelCapabilitiesSupports"))
        model_list = ModelList.from_dict(obj.get("ModelList"))
        model_policy = ModelPolicy.from_dict(obj.get("ModelPolicy"))
        model_switch_to_request = ModelSwitchToRequest.from_dict(obj.get("ModelSwitchToRequest"))
        model_switch_to_result = ModelSwitchToResult.from_dict(obj.get("ModelSwitchToResult"))
        mode_set_request = ModeSetRequest.from_dict(obj.get("ModeSetRequest"))
        name_get_result = NameGetResult.from_dict(obj.get("NameGetResult"))
        name_set_request = NameSetRequest.from_dict(obj.get("NameSetRequest"))
        permission_decision = PermissionDecision.from_dict(obj.get("PermissionDecision"))
        permission_decision_approved = PermissionDecisionApproved.from_dict(obj.get("PermissionDecisionApproved"))
        permission_decision_denied_by_content_exclusion_policy = PermissionDecisionDeniedByContentExclusionPolicy.from_dict(obj.get("PermissionDecisionDeniedByContentExclusionPolicy"))
        permission_decision_denied_by_permission_request_hook = PermissionDecisionDeniedByPermissionRequestHook.from_dict(obj.get("PermissionDecisionDeniedByPermissionRequestHook"))
        permission_decision_denied_by_rules = PermissionDecisionDeniedByRules.from_dict(obj.get("PermissionDecisionDeniedByRules"))
        permission_decision_denied_interactively_by_user = PermissionDecisionDeniedInteractivelyByUser.from_dict(obj.get("PermissionDecisionDeniedInteractivelyByUser"))
        permission_decision_denied_no_approval_rule_and_could_not_request_from_user = PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser.from_dict(obj.get("PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser"))
        permission_decision_request = PermissionDecisionRequest.from_dict(obj.get("PermissionDecisionRequest"))
        permission_request_result = PermissionRequestResult.from_dict(obj.get("PermissionRequestResult"))
        ping_request = PingRequest.from_dict(obj.get("PingRequest"))
        ping_result = PingResult.from_dict(obj.get("PingResult"))
        plan_read_result = PlanReadResult.from_dict(obj.get("PlanReadResult"))
        plan_update_request = PlanUpdateRequest.from_dict(obj.get("PlanUpdateRequest"))
        plugin = Plugin.from_dict(obj.get("Plugin"))
        plugin_list = PluginList.from_dict(obj.get("PluginList"))
        server_skill = ServerSkill.from_dict(obj.get("ServerSkill"))
        server_skill_list = ServerSkillList.from_dict(obj.get("ServerSkillList"))
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
        session_fs_set_provider_conventions = SessionFSSetProviderConventions(obj.get("SessionFsSetProviderConventions"))
        session_fs_set_provider_request = SessionFSSetProviderRequest.from_dict(obj.get("SessionFsSetProviderRequest"))
        session_fs_set_provider_result = SessionFSSetProviderResult.from_dict(obj.get("SessionFsSetProviderResult"))
        session_fs_stat_request = SessionFSStatRequest.from_dict(obj.get("SessionFsStatRequest"))
        session_fs_stat_result = SessionFSStatResult.from_dict(obj.get("SessionFsStatResult"))
        session_fs_write_file_request = SessionFSWriteFileRequest.from_dict(obj.get("SessionFsWriteFileRequest"))
        session_log_level = SessionLogLevel(obj.get("SessionLogLevel"))
        session_mode = SessionMode(obj.get("SessionMode"))
        sessions_fork_request = SessionsForkRequest.from_dict(obj.get("SessionsForkRequest"))
        sessions_fork_result = SessionsForkResult.from_dict(obj.get("SessionsForkResult"))
        shell_exec_request = ShellExecRequest.from_dict(obj.get("ShellExecRequest"))
        shell_exec_result = ShellExecResult.from_dict(obj.get("ShellExecResult"))
        shell_kill_request = ShellKillRequest.from_dict(obj.get("ShellKillRequest"))
        shell_kill_result = ShellKillResult.from_dict(obj.get("ShellKillResult"))
        shell_kill_signal = ShellKillSignal(obj.get("ShellKillSignal"))
        skill = Skill.from_dict(obj.get("Skill"))
        skill_list = SkillList.from_dict(obj.get("SkillList"))
        skills_config_set_disabled_skills_request = SkillsConfigSetDisabledSkillsRequest.from_dict(obj.get("SkillsConfigSetDisabledSkillsRequest"))
        skills_disable_request = SkillsDisableRequest.from_dict(obj.get("SkillsDisableRequest"))
        skills_discover_request = SkillsDiscoverRequest.from_dict(obj.get("SkillsDiscoverRequest"))
        skills_enable_request = SkillsEnableRequest.from_dict(obj.get("SkillsEnableRequest"))
        tool = Tool.from_dict(obj.get("Tool"))
        tool_call_result = ToolCallResult.from_dict(obj.get("ToolCallResult"))
        tool_list = ToolList.from_dict(obj.get("ToolList"))
        tools_handle_pending_tool_call = from_union([ToolCallResult.from_dict, from_str], obj.get("ToolsHandlePendingToolCall"))
        tools_handle_pending_tool_call_request = ToolsHandlePendingToolCallRequest.from_dict(obj.get("ToolsHandlePendingToolCallRequest"))
        tools_list_request = ToolsListRequest.from_dict(obj.get("ToolsListRequest"))
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
        ui_handle_pending_elicitation_request = UIHandlePendingElicitationRequest.from_dict(obj.get("UIHandlePendingElicitationRequest"))
        usage_get_metrics_result = UsageGetMetricsResult.from_dict(obj.get("UsageGetMetricsResult"))
        usage_metrics_code_changes = UsageMetricsCodeChanges.from_dict(obj.get("UsageMetricsCodeChanges"))
        usage_metrics_model_metric = UsageMetricsModelMetric.from_dict(obj.get("UsageMetricsModelMetric"))
        usage_metrics_model_metric_requests = UsageMetricsModelMetricRequests.from_dict(obj.get("UsageMetricsModelMetricRequests"))
        usage_metrics_model_metric_usage = UsageMetricsModelMetricUsage.from_dict(obj.get("UsageMetricsModelMetricUsage"))
        workspaces_create_file_request = WorkspacesCreateFileRequest.from_dict(obj.get("WorkspacesCreateFileRequest"))
        workspaces_get_workspace_result = WorkspacesGetWorkspaceResult.from_dict(obj.get("WorkspacesGetWorkspaceResult"))
        workspaces_list_files_result = WorkspacesListFilesResult.from_dict(obj.get("WorkspacesListFilesResult"))
        workspaces_read_file_request = WorkspacesReadFileRequest.from_dict(obj.get("WorkspacesReadFileRequest"))
        workspaces_read_file_result = WorkspacesReadFileResult.from_dict(obj.get("WorkspacesReadFileResult"))
        return RPC(account_get_quota_result, account_quota_snapshot, agent_get_current_result, agent_info, agent_list, agent_reload_result, agent_select_request, agent_select_result, commands_handle_pending_command_request, commands_handle_pending_command_result, current_model, discovered_mcp_server, discovered_mcp_server_source, discovered_mcp_server_type, extension, extension_list, extensions_disable_request, extensions_enable_request, extension_source, extension_status, filter_mapping, filter_mapping_string, filter_mapping_value, fleet_start_request, fleet_start_result, handle_tool_call_result, history_compact_context_window, history_compact_result, history_truncate_request, history_truncate_result, instructions_get_sources_result, instructions_sources, instructions_sources_location, instructions_sources_type, log_request, log_result, mcp_config_add_request, mcp_config_list, mcp_config_remove_request, mcp_config_update_request, mcp_disable_request, mcp_discover_request, mcp_discover_result, mcp_enable_request, mcp_server, mcp_server_config, mcp_server_config_http, mcp_server_config_http_type, mcp_server_config_local, mcp_server_config_local_type, mcp_server_list, mcp_server_source, mcp_server_status, model, model_billing, model_capabilities, model_capabilities_limits, model_capabilities_limits_vision, model_capabilities_override, model_capabilities_override_limits, model_capabilities_override_limits_vision, model_capabilities_override_supports, model_capabilities_supports, model_list, model_policy, model_switch_to_request, model_switch_to_result, mode_set_request, name_get_result, name_set_request, permission_decision, permission_decision_approved, permission_decision_denied_by_content_exclusion_policy, permission_decision_denied_by_permission_request_hook, permission_decision_denied_by_rules, permission_decision_denied_interactively_by_user, permission_decision_denied_no_approval_rule_and_could_not_request_from_user, permission_decision_request, permission_request_result, ping_request, ping_result, plan_read_result, plan_update_request, plugin, plugin_list, server_skill, server_skill_list, session_fs_append_file_request, session_fs_error, session_fs_error_code, session_fs_exists_request, session_fs_exists_result, session_fs_mkdir_request, session_fs_readdir_request, session_fs_readdir_result, session_fs_readdir_with_types_entry, session_fs_readdir_with_types_entry_type, session_fs_readdir_with_types_request, session_fs_readdir_with_types_result, session_fs_read_file_request, session_fs_read_file_result, session_fs_rename_request, session_fs_rm_request, session_fs_set_provider_conventions, session_fs_set_provider_request, session_fs_set_provider_result, session_fs_stat_request, session_fs_stat_result, session_fs_write_file_request, session_log_level, session_mode, sessions_fork_request, sessions_fork_result, shell_exec_request, shell_exec_result, shell_kill_request, shell_kill_result, shell_kill_signal, skill, skill_list, skills_config_set_disabled_skills_request, skills_disable_request, skills_discover_request, skills_enable_request, tool, tool_call_result, tool_list, tools_handle_pending_tool_call, tools_handle_pending_tool_call_request, tools_list_request, ui_elicitation_array_any_of_field, ui_elicitation_array_any_of_field_items, ui_elicitation_array_any_of_field_items_any_of, ui_elicitation_array_enum_field, ui_elicitation_array_enum_field_items, ui_elicitation_field_value, ui_elicitation_request, ui_elicitation_response, ui_elicitation_response_action, ui_elicitation_response_content, ui_elicitation_result, ui_elicitation_schema, ui_elicitation_schema_property, ui_elicitation_schema_property_boolean, ui_elicitation_schema_property_number, ui_elicitation_schema_property_number_type, ui_elicitation_schema_property_string, ui_elicitation_schema_property_string_format, ui_elicitation_string_enum_field, ui_elicitation_string_one_of_field, ui_elicitation_string_one_of_field_one_of, ui_handle_pending_elicitation_request, usage_get_metrics_result, usage_metrics_code_changes, usage_metrics_model_metric, usage_metrics_model_metric_requests, usage_metrics_model_metric_usage, workspaces_create_file_request, workspaces_get_workspace_result, workspaces_list_files_result, workspaces_read_file_request, workspaces_read_file_result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["AccountGetQuotaResult"] = to_class(AccountGetQuotaResult, self.account_get_quota_result)
        result["AccountQuotaSnapshot"] = to_class(AccountQuotaSnapshot, self.account_quota_snapshot)
        result["AgentGetCurrentResult"] = to_class(AgentGetCurrentResult, self.agent_get_current_result)
        result["AgentInfo"] = to_class(AgentInfo, self.agent_info)
        result["AgentList"] = to_class(AgentList, self.agent_list)
        result["AgentReloadResult"] = to_class(AgentReloadResult, self.agent_reload_result)
        result["AgentSelectRequest"] = to_class(AgentSelectRequest, self.agent_select_request)
        result["AgentSelectResult"] = to_class(AgentSelectResult, self.agent_select_result)
        result["CommandsHandlePendingCommandRequest"] = to_class(CommandsHandlePendingCommandRequest, self.commands_handle_pending_command_request)
        result["CommandsHandlePendingCommandResult"] = to_class(CommandsHandlePendingCommandResult, self.commands_handle_pending_command_result)
        result["CurrentModel"] = to_class(CurrentModel, self.current_model)
        result["DiscoveredMcpServer"] = to_class(DiscoveredMCPServer, self.discovered_mcp_server)
        result["DiscoveredMcpServerSource"] = to_enum(MCPServerSource, self.discovered_mcp_server_source)
        result["DiscoveredMcpServerType"] = to_enum(DiscoveredMCPServerType, self.discovered_mcp_server_type)
        result["Extension"] = to_class(Extension, self.extension)
        result["ExtensionList"] = to_class(ExtensionList, self.extension_list)
        result["ExtensionsDisableRequest"] = to_class(ExtensionsDisableRequest, self.extensions_disable_request)
        result["ExtensionsEnableRequest"] = to_class(ExtensionsEnableRequest, self.extensions_enable_request)
        result["ExtensionSource"] = to_enum(ExtensionSource, self.extension_source)
        result["ExtensionStatus"] = to_enum(ExtensionStatus, self.extension_status)
        result["FilterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(FilterMappingString, x), x), lambda x: to_enum(FilterMappingString, x)], self.filter_mapping)
        result["FilterMappingString"] = to_enum(FilterMappingString, self.filter_mapping_string)
        result["FilterMappingValue"] = to_enum(FilterMappingString, self.filter_mapping_value)
        result["FleetStartRequest"] = to_class(FleetStartRequest, self.fleet_start_request)
        result["FleetStartResult"] = to_class(FleetStartResult, self.fleet_start_result)
        result["HandleToolCallResult"] = to_class(HandleToolCallResult, self.handle_tool_call_result)
        result["HistoryCompactContextWindow"] = to_class(HistoryCompactContextWindow, self.history_compact_context_window)
        result["HistoryCompactResult"] = to_class(HistoryCompactResult, self.history_compact_result)
        result["HistoryTruncateRequest"] = to_class(HistoryTruncateRequest, self.history_truncate_request)
        result["HistoryTruncateResult"] = to_class(HistoryTruncateResult, self.history_truncate_result)
        result["InstructionsGetSourcesResult"] = to_class(InstructionsGetSourcesResult, self.instructions_get_sources_result)
        result["InstructionsSources"] = to_class(InstructionsSources, self.instructions_sources)
        result["InstructionsSourcesLocation"] = to_enum(InstructionsSourcesLocation, self.instructions_sources_location)
        result["InstructionsSourcesType"] = to_enum(InstructionsSourcesType, self.instructions_sources_type)
        result["LogRequest"] = to_class(LogRequest, self.log_request)
        result["LogResult"] = to_class(LogResult, self.log_result)
        result["McpConfigAddRequest"] = to_class(MCPConfigAddRequest, self.mcp_config_add_request)
        result["McpConfigList"] = to_class(MCPConfigList, self.mcp_config_list)
        result["McpConfigRemoveRequest"] = to_class(MCPConfigRemoveRequest, self.mcp_config_remove_request)
        result["McpConfigUpdateRequest"] = to_class(MCPConfigUpdateRequest, self.mcp_config_update_request)
        result["McpDisableRequest"] = to_class(MCPDisableRequest, self.mcp_disable_request)
        result["McpDiscoverRequest"] = to_class(MCPDiscoverRequest, self.mcp_discover_request)
        result["McpDiscoverResult"] = to_class(MCPDiscoverResult, self.mcp_discover_result)
        result["McpEnableRequest"] = to_class(MCPEnableRequest, self.mcp_enable_request)
        result["McpServer"] = to_class(MCPServer, self.mcp_server)
        result["McpServerConfig"] = to_class(MCPServerConfig, self.mcp_server_config)
        result["McpServerConfigHttp"] = to_class(MCPServerConfigHTTP, self.mcp_server_config_http)
        result["McpServerConfigHttpType"] = to_enum(MCPServerConfigHTTPType, self.mcp_server_config_http_type)
        result["McpServerConfigLocal"] = to_class(MCPServerConfigLocal, self.mcp_server_config_local)
        result["McpServerConfigLocalType"] = to_enum(MCPServerConfigLocalType, self.mcp_server_config_local_type)
        result["McpServerList"] = to_class(MCPServerList, self.mcp_server_list)
        result["McpServerSource"] = to_enum(MCPServerSource, self.mcp_server_source)
        result["McpServerStatus"] = to_enum(MCPServerStatus, self.mcp_server_status)
        result["Model"] = to_class(Model, self.model)
        result["ModelBilling"] = to_class(ModelBilling, self.model_billing)
        result["ModelCapabilities"] = to_class(ModelCapabilities, self.model_capabilities)
        result["ModelCapabilitiesLimits"] = to_class(ModelCapabilitiesLimits, self.model_capabilities_limits)
        result["ModelCapabilitiesLimitsVision"] = to_class(ModelCapabilitiesLimitsVision, self.model_capabilities_limits_vision)
        result["ModelCapabilitiesOverride"] = to_class(ModelCapabilitiesOverride, self.model_capabilities_override)
        result["ModelCapabilitiesOverrideLimits"] = to_class(ModelCapabilitiesOverrideLimits, self.model_capabilities_override_limits)
        result["ModelCapabilitiesOverrideLimitsVision"] = to_class(ModelCapabilitiesOverrideLimitsVision, self.model_capabilities_override_limits_vision)
        result["ModelCapabilitiesOverrideSupports"] = to_class(ModelCapabilitiesOverrideSupports, self.model_capabilities_override_supports)
        result["ModelCapabilitiesSupports"] = to_class(ModelCapabilitiesSupports, self.model_capabilities_supports)
        result["ModelList"] = to_class(ModelList, self.model_list)
        result["ModelPolicy"] = to_class(ModelPolicy, self.model_policy)
        result["ModelSwitchToRequest"] = to_class(ModelSwitchToRequest, self.model_switch_to_request)
        result["ModelSwitchToResult"] = to_class(ModelSwitchToResult, self.model_switch_to_result)
        result["ModeSetRequest"] = to_class(ModeSetRequest, self.mode_set_request)
        result["NameGetResult"] = to_class(NameGetResult, self.name_get_result)
        result["NameSetRequest"] = to_class(NameSetRequest, self.name_set_request)
        result["PermissionDecision"] = to_class(PermissionDecision, self.permission_decision)
        result["PermissionDecisionApproved"] = to_class(PermissionDecisionApproved, self.permission_decision_approved)
        result["PermissionDecisionDeniedByContentExclusionPolicy"] = to_class(PermissionDecisionDeniedByContentExclusionPolicy, self.permission_decision_denied_by_content_exclusion_policy)
        result["PermissionDecisionDeniedByPermissionRequestHook"] = to_class(PermissionDecisionDeniedByPermissionRequestHook, self.permission_decision_denied_by_permission_request_hook)
        result["PermissionDecisionDeniedByRules"] = to_class(PermissionDecisionDeniedByRules, self.permission_decision_denied_by_rules)
        result["PermissionDecisionDeniedInteractivelyByUser"] = to_class(PermissionDecisionDeniedInteractivelyByUser, self.permission_decision_denied_interactively_by_user)
        result["PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser"] = to_class(PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser, self.permission_decision_denied_no_approval_rule_and_could_not_request_from_user)
        result["PermissionDecisionRequest"] = to_class(PermissionDecisionRequest, self.permission_decision_request)
        result["PermissionRequestResult"] = to_class(PermissionRequestResult, self.permission_request_result)
        result["PingRequest"] = to_class(PingRequest, self.ping_request)
        result["PingResult"] = to_class(PingResult, self.ping_result)
        result["PlanReadResult"] = to_class(PlanReadResult, self.plan_read_result)
        result["PlanUpdateRequest"] = to_class(PlanUpdateRequest, self.plan_update_request)
        result["Plugin"] = to_class(Plugin, self.plugin)
        result["PluginList"] = to_class(PluginList, self.plugin_list)
        result["ServerSkill"] = to_class(ServerSkill, self.server_skill)
        result["ServerSkillList"] = to_class(ServerSkillList, self.server_skill_list)
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
        result["SessionFsSetProviderConventions"] = to_enum(SessionFSSetProviderConventions, self.session_fs_set_provider_conventions)
        result["SessionFsSetProviderRequest"] = to_class(SessionFSSetProviderRequest, self.session_fs_set_provider_request)
        result["SessionFsSetProviderResult"] = to_class(SessionFSSetProviderResult, self.session_fs_set_provider_result)
        result["SessionFsStatRequest"] = to_class(SessionFSStatRequest, self.session_fs_stat_request)
        result["SessionFsStatResult"] = to_class(SessionFSStatResult, self.session_fs_stat_result)
        result["SessionFsWriteFileRequest"] = to_class(SessionFSWriteFileRequest, self.session_fs_write_file_request)
        result["SessionLogLevel"] = to_enum(SessionLogLevel, self.session_log_level)
        result["SessionMode"] = to_enum(SessionMode, self.session_mode)
        result["SessionsForkRequest"] = to_class(SessionsForkRequest, self.sessions_fork_request)
        result["SessionsForkResult"] = to_class(SessionsForkResult, self.sessions_fork_result)
        result["ShellExecRequest"] = to_class(ShellExecRequest, self.shell_exec_request)
        result["ShellExecResult"] = to_class(ShellExecResult, self.shell_exec_result)
        result["ShellKillRequest"] = to_class(ShellKillRequest, self.shell_kill_request)
        result["ShellKillResult"] = to_class(ShellKillResult, self.shell_kill_result)
        result["ShellKillSignal"] = to_enum(ShellKillSignal, self.shell_kill_signal)
        result["Skill"] = to_class(Skill, self.skill)
        result["SkillList"] = to_class(SkillList, self.skill_list)
        result["SkillsConfigSetDisabledSkillsRequest"] = to_class(SkillsConfigSetDisabledSkillsRequest, self.skills_config_set_disabled_skills_request)
        result["SkillsDisableRequest"] = to_class(SkillsDisableRequest, self.skills_disable_request)
        result["SkillsDiscoverRequest"] = to_class(SkillsDiscoverRequest, self.skills_discover_request)
        result["SkillsEnableRequest"] = to_class(SkillsEnableRequest, self.skills_enable_request)
        result["Tool"] = to_class(Tool, self.tool)
        result["ToolCallResult"] = to_class(ToolCallResult, self.tool_call_result)
        result["ToolList"] = to_class(ToolList, self.tool_list)
        result["ToolsHandlePendingToolCall"] = from_union([lambda x: to_class(ToolCallResult, x), from_str], self.tools_handle_pending_tool_call)
        result["ToolsHandlePendingToolCallRequest"] = to_class(ToolsHandlePendingToolCallRequest, self.tools_handle_pending_tool_call_request)
        result["ToolsListRequest"] = to_class(ToolsListRequest, self.tools_list_request)
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
        result["UIHandlePendingElicitationRequest"] = to_class(UIHandlePendingElicitationRequest, self.ui_handle_pending_elicitation_request)
        result["UsageGetMetricsResult"] = to_class(UsageGetMetricsResult, self.usage_get_metrics_result)
        result["UsageMetricsCodeChanges"] = to_class(UsageMetricsCodeChanges, self.usage_metrics_code_changes)
        result["UsageMetricsModelMetric"] = to_class(UsageMetricsModelMetric, self.usage_metrics_model_metric)
        result["UsageMetricsModelMetricRequests"] = to_class(UsageMetricsModelMetricRequests, self.usage_metrics_model_metric_requests)
        result["UsageMetricsModelMetricUsage"] = to_class(UsageMetricsModelMetricUsage, self.usage_metrics_model_metric_usage)
        result["WorkspacesCreateFileRequest"] = to_class(WorkspacesCreateFileRequest, self.workspaces_create_file_request)
        result["WorkspacesGetWorkspaceResult"] = to_class(WorkspacesGetWorkspaceResult, self.workspaces_get_workspace_result)
        result["WorkspacesListFilesResult"] = to_class(WorkspacesListFilesResult, self.workspaces_list_files_result)
        result["WorkspacesReadFileRequest"] = to_class(WorkspacesReadFileRequest, self.workspaces_read_file_request)
        result["WorkspacesReadFileResult"] = to_class(WorkspacesReadFileResult, self.workspaces_read_file_result)
        return result

def rpc_from_dict(s: Any) -> RPC:
    return RPC.from_dict(s)

def rpc_to_dict(x: RPC) -> Any:
    return to_class(RPC, x)


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
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self, *, timeout: float | None = None) -> ModelList:
        return ModelList.from_dict(_patch_model_capabilities(await self._client.request("models.list", {}, **_timeout_kwargs(timeout))))


class ServerToolsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self, params: ToolsListRequest, *, timeout: float | None = None) -> ToolList:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ToolList.from_dict(await self._client.request("tools.list", params_dict, **_timeout_kwargs(timeout)))


class ServerAccountApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def get_quota(self, *, timeout: float | None = None) -> AccountGetQuotaResult:
        return AccountGetQuotaResult.from_dict(await self._client.request("account.getQuota", {}, **_timeout_kwargs(timeout)))


class ServerMcpConfigApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self, *, timeout: float | None = None) -> MCPConfigList:
        return MCPConfigList.from_dict(await self._client.request("mcp.config.list", {}, **_timeout_kwargs(timeout)))

    async def add(self, params: MCPConfigAddRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.add", params_dict, **_timeout_kwargs(timeout))

    async def update(self, params: MCPConfigUpdateRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.update", params_dict, **_timeout_kwargs(timeout))

    async def remove(self, params: MCPConfigRemoveRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("mcp.config.remove", params_dict, **_timeout_kwargs(timeout))


class ServerMcpApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client
        self.config = ServerMcpConfigApi(client)

    async def discover(self, params: MCPDiscoverRequest, *, timeout: float | None = None) -> MCPDiscoverResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return MCPDiscoverResult.from_dict(await self._client.request("mcp.discover", params_dict, **_timeout_kwargs(timeout)))


class ServerSkillsConfigApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def set_disabled_skills(self, params: SkillsConfigSetDisabledSkillsRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        await self._client.request("skills.config.setDisabledSkills", params_dict, **_timeout_kwargs(timeout))


class ServerSkillsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client
        self.config = ServerSkillsConfigApi(client)

    async def discover(self, params: SkillsDiscoverRequest, *, timeout: float | None = None) -> ServerSkillList:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ServerSkillList.from_dict(await self._client.request("skills.discover", params_dict, **_timeout_kwargs(timeout)))


class ServerSessionFsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def set_provider(self, params: SessionFSSetProviderRequest, *, timeout: float | None = None) -> SessionFSSetProviderResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionFSSetProviderResult.from_dict(await self._client.request("sessionFs.setProvider", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ServerSessionsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def fork(self, params: SessionsForkRequest, *, timeout: float | None = None) -> SessionsForkResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return SessionsForkResult.from_dict(await self._client.request("sessions.fork", params_dict, **_timeout_kwargs(timeout)))


class ServerRpc:
    """Typed server-scoped RPC methods."""
    def __init__(self, client: "JsonRpcClient"):
        self._client = client
        self.models = ServerModelsApi(client)
        self.tools = ServerToolsApi(client)
        self.account = ServerAccountApi(client)
        self.mcp = ServerMcpApi(client)
        self.skills = ServerSkillsApi(client)
        self.session_fs = ServerSessionFsApi(client)
        self.sessions = ServerSessionsApi(client)

    async def ping(self, params: PingRequest, *, timeout: float | None = None) -> PingResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return PingResult.from_dict(await self._client.request("ping", params_dict, **_timeout_kwargs(timeout)))


class ModelApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_current(self, *, timeout: float | None = None) -> CurrentModel:
        return CurrentModel.from_dict(await self._client.request("session.model.getCurrent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def switch_to(self, params: ModelSwitchToRequest, *, timeout: float | None = None) -> ModelSwitchToResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ModelSwitchToResult.from_dict(await self._client.request("session.model.switchTo", params_dict, **_timeout_kwargs(timeout)))


class ModeApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get(self, *, timeout: float | None = None) -> SessionMode:
        return SessionMode(await self._client.request("session.mode.get", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set(self, params: ModeSetRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.mode.set", params_dict, **_timeout_kwargs(timeout))


class NameApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get(self, *, timeout: float | None = None) -> NameGetResult:
        return NameGetResult.from_dict(await self._client.request("session.name.get", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set(self, params: NameSetRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.name.set", params_dict, **_timeout_kwargs(timeout))


class PlanApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def read(self, *, timeout: float | None = None) -> PlanReadResult:
        return PlanReadResult.from_dict(await self._client.request("session.plan.read", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def update(self, params: PlanUpdateRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.plan.update", params_dict, **_timeout_kwargs(timeout))

    async def delete(self, *, timeout: float | None = None) -> None:
        await self._client.request("session.plan.delete", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


class WorkspacesApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_workspace(self, *, timeout: float | None = None) -> WorkspacesGetWorkspaceResult:
        return WorkspacesGetWorkspaceResult.from_dict(await self._client.request("session.workspaces.getWorkspace", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def list_files(self, *, timeout: float | None = None) -> WorkspacesListFilesResult:
        return WorkspacesListFilesResult.from_dict(await self._client.request("session.workspaces.listFiles", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def read_file(self, params: WorkspacesReadFileRequest, *, timeout: float | None = None) -> WorkspacesReadFileResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return WorkspacesReadFileResult.from_dict(await self._client.request("session.workspaces.readFile", params_dict, **_timeout_kwargs(timeout)))

    async def create_file(self, params: WorkspacesCreateFileRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.workspaces.createFile", params_dict, **_timeout_kwargs(timeout))


class InstructionsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_sources(self, *, timeout: float | None = None) -> InstructionsGetSourcesResult:
        return InstructionsGetSourcesResult.from_dict(await self._client.request("session.instructions.getSources", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class FleetApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def start(self, params: FleetStartRequest, *, timeout: float | None = None) -> FleetStartResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return FleetStartResult.from_dict(await self._client.request("session.fleet.start", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class AgentApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> AgentList:
        return AgentList.from_dict(await self._client.request("session.agent.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def get_current(self, *, timeout: float | None = None) -> AgentGetCurrentResult:
        return AgentGetCurrentResult.from_dict(await self._client.request("session.agent.getCurrent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def select(self, params: AgentSelectRequest, *, timeout: float | None = None) -> AgentSelectResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return AgentSelectResult.from_dict(await self._client.request("session.agent.select", params_dict, **_timeout_kwargs(timeout)))

    async def deselect(self, *, timeout: float | None = None) -> None:
        await self._client.request("session.agent.deselect", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> AgentReloadResult:
        return AgentReloadResult.from_dict(await self._client.request("session.agent.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class SkillsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SkillList:
        return SkillList.from_dict(await self._client.request("session.skills.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: SkillsEnableRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.skills.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: SkillsDisableRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.skills.disable", params_dict, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> None:
        await self._client.request("session.skills.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class McpApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> MCPServerList:
        return MCPServerList.from_dict(await self._client.request("session.mcp.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: MCPEnableRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.mcp.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: MCPDisableRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.mcp.disable", params_dict, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> None:
        await self._client.request("session.mcp.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


# Experimental: this API group is experimental and may change or be removed.
class PluginsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> PluginList:
        return PluginList.from_dict(await self._client.request("session.plugins.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ExtensionsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> ExtensionList:
        return ExtensionList.from_dict(await self._client.request("session.extensions.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: ExtensionsEnableRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.extensions.enable", params_dict, **_timeout_kwargs(timeout))

    async def disable(self, params: ExtensionsDisableRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.extensions.disable", params_dict, **_timeout_kwargs(timeout))

    async def reload(self, *, timeout: float | None = None) -> None:
        await self._client.request("session.extensions.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))


class ToolsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_tool_call(self, params: ToolsHandlePendingToolCallRequest, *, timeout: float | None = None) -> HandleToolCallResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return HandleToolCallResult.from_dict(await self._client.request("session.tools.handlePendingToolCall", params_dict, **_timeout_kwargs(timeout)))


class CommandsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_command(self, params: CommandsHandlePendingCommandRequest, *, timeout: float | None = None) -> CommandsHandlePendingCommandResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return CommandsHandlePendingCommandResult.from_dict(await self._client.request("session.commands.handlePendingCommand", params_dict, **_timeout_kwargs(timeout)))


class UiApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def elicitation(self, params: UIElicitationRequest, *, timeout: float | None = None) -> UIElicitationResponse:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIElicitationResponse.from_dict(await self._client.request("session.ui.elicitation", params_dict, **_timeout_kwargs(timeout)))

    async def handle_pending_elicitation(self, params: UIHandlePendingElicitationRequest, *, timeout: float | None = None) -> UIElicitationResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return UIElicitationResult.from_dict(await self._client.request("session.ui.handlePendingElicitation", params_dict, **_timeout_kwargs(timeout)))


class PermissionsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_permission_request(self, params: PermissionDecisionRequest, *, timeout: float | None = None) -> PermissionRequestResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return PermissionRequestResult.from_dict(await self._client.request("session.permissions.handlePendingPermissionRequest", params_dict, **_timeout_kwargs(timeout)))


class ShellApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def exec(self, params: ShellExecRequest, *, timeout: float | None = None) -> ShellExecResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ShellExecResult.from_dict(await self._client.request("session.shell.exec", params_dict, **_timeout_kwargs(timeout)))

    async def kill(self, params: ShellKillRequest, *, timeout: float | None = None) -> ShellKillResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return ShellKillResult.from_dict(await self._client.request("session.shell.kill", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class HistoryApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def compact(self, *, timeout: float | None = None) -> HistoryCompactResult:
        return HistoryCompactResult.from_dict(await self._client.request("session.history.compact", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def truncate(self, params: HistoryTruncateRequest, *, timeout: float | None = None) -> HistoryTruncateResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return HistoryTruncateResult.from_dict(await self._client.request("session.history.truncate", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class UsageApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_metrics(self, *, timeout: float | None = None) -> UsageGetMetricsResult:
        return UsageGetMetricsResult.from_dict(await self._client.request("session.usage.getMetrics", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


class SessionRpc:
    """Typed session-scoped RPC methods."""
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id
        self.model = ModelApi(client, session_id)
        self.mode = ModeApi(client, session_id)
        self.name = NameApi(client, session_id)
        self.plan = PlanApi(client, session_id)
        self.workspaces = WorkspacesApi(client, session_id)
        self.instructions = InstructionsApi(client, session_id)
        self.fleet = FleetApi(client, session_id)
        self.agent = AgentApi(client, session_id)
        self.skills = SkillsApi(client, session_id)
        self.mcp = McpApi(client, session_id)
        self.plugins = PluginsApi(client, session_id)
        self.extensions = ExtensionsApi(client, session_id)
        self.tools = ToolsApi(client, session_id)
        self.commands = CommandsApi(client, session_id)
        self.ui = UiApi(client, session_id)
        self.permissions = PermissionsApi(client, session_id)
        self.shell = ShellApi(client, session_id)
        self.history = HistoryApi(client, session_id)
        self.usage = UsageApi(client, session_id)

    async def log(self, params: LogRequest, *, timeout: float | None = None) -> LogResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return LogResult.from_dict(await self._client.request("session.log", params_dict, **_timeout_kwargs(timeout)))


class SessionFsHandler(Protocol):
    async def read_file(self, params: SessionFSReadFileRequest) -> SessionFSReadFileResult:
        pass
    async def write_file(self, params: SessionFSWriteFileRequest) -> SessionFSError | None:
        pass
    async def append_file(self, params: SessionFSAppendFileRequest) -> SessionFSError | None:
        pass
    async def exists(self, params: SessionFSExistsRequest) -> SessionFSExistsResult:
        pass
    async def stat(self, params: SessionFSStatRequest) -> SessionFSStatResult:
        pass
    async def mkdir(self, params: SessionFSMkdirRequest) -> SessionFSError | None:
        pass
    async def readdir(self, params: SessionFSReaddirRequest) -> SessionFSReaddirResult:
        pass
    async def readdir_with_types(self, params: SessionFSReaddirWithTypesRequest) -> SessionFSReaddirWithTypesResult:
        pass
    async def rm(self, params: SessionFSRmRequest) -> SessionFSError | None:
        pass
    async def rename(self, params: SessionFSRenameRequest) -> SessionFSError | None:
        pass

@dataclass
class ClientSessionApiHandlers:
    session_fs: SessionFsHandler | None = None

def register_client_session_api_handlers(
    client: "JsonRpcClient",
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
