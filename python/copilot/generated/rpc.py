"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from .._jsonrpc import JsonRpcClient


from dataclasses import dataclass
from typing import Any, TypeVar, cast
from collections.abc import Callable
from enum import Enum
from uuid import UUID


T = TypeVar("T")
EnumT = TypeVar("EnumT", bound=Enum)


def from_str(x: Any) -> str:
    assert isinstance(x, str)
    return x


def from_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)


def to_float(x: Any) -> float:
    assert isinstance(x, (int, float))
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


def from_bool(x: Any) -> bool:
    assert isinstance(x, bool)
    return x


def to_class(c: type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()


def from_list(f: Callable[[Any], T], x: Any) -> list[T]:
    assert isinstance(x, list)
    return [f(y) for y in x]


def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:
    assert isinstance(x, dict)
    return { k: f(v) for (k, v) in x.items() }


def to_enum(c: type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, c)
    return x.value


def from_int(x: Any) -> int:
    assert isinstance(x, int) and not isinstance(x, bool)
    return x


@dataclass
class PingResult:
    message: str
    """Echoed message (or default greeting)"""

    protocol_version: float
    """Server protocol version number"""

    timestamp: float
    """Server timestamp in milliseconds"""

    @staticmethod
    def from_dict(obj: Any) -> 'PingResult':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        protocol_version = from_float(obj.get("protocolVersion"))
        timestamp = from_float(obj.get("timestamp"))
        return PingResult(message, protocol_version, timestamp)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["protocolVersion"] = to_float(self.protocol_version)
        result["timestamp"] = to_float(self.timestamp)
        return result


@dataclass
class PingParams:
    message: str | None = None
    """Optional message to echo back"""

    @staticmethod
    def from_dict(obj: Any) -> 'PingParams':
        assert isinstance(obj, dict)
        message = from_union([from_str, from_none], obj.get("message"))
        return PingParams(message)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        return result


@dataclass
class Billing:
    """Billing information"""

    multiplier: float
    """Billing cost multiplier relative to the base rate"""

    @staticmethod
    def from_dict(obj: Any) -> 'Billing':
        assert isinstance(obj, dict)
        multiplier = from_float(obj.get("multiplier"))
        return Billing(multiplier)

    def to_dict(self) -> dict:
        result: dict = {}
        result["multiplier"] = to_float(self.multiplier)
        return result


@dataclass
class Limits:
    """Token limits for prompts, outputs, and context window"""

    max_context_window_tokens: float
    """Maximum total context window size in tokens"""

    max_output_tokens: float | None = None
    """Maximum number of output/completion tokens"""

    max_prompt_tokens: float | None = None
    """Maximum number of prompt/input tokens"""

    @staticmethod
    def from_dict(obj: Any) -> 'Limits':
        assert isinstance(obj, dict)
        max_context_window_tokens = from_float(obj.get("max_context_window_tokens"))
        max_output_tokens = from_union([from_float, from_none], obj.get("max_output_tokens"))
        max_prompt_tokens = from_union([from_float, from_none], obj.get("max_prompt_tokens"))
        return Limits(max_context_window_tokens, max_output_tokens, max_prompt_tokens)

    def to_dict(self) -> dict:
        result: dict = {}
        result["max_context_window_tokens"] = to_float(self.max_context_window_tokens)
        if self.max_output_tokens is not None:
            result["max_output_tokens"] = from_union([to_float, from_none], self.max_output_tokens)
        if self.max_prompt_tokens is not None:
            result["max_prompt_tokens"] = from_union([to_float, from_none], self.max_prompt_tokens)
        return result


@dataclass
class Supports:
    """Feature flags indicating what the model supports"""

    reasoning_effort: bool | None = None
    """Whether this model supports reasoning effort configuration"""

    vision: bool | None = None
    """Whether this model supports vision/image input"""

    @staticmethod
    def from_dict(obj: Any) -> 'Supports':
        assert isinstance(obj, dict)
        reasoning_effort = from_union([from_bool, from_none], obj.get("reasoningEffort"))
        vision = from_union([from_bool, from_none], obj.get("vision"))
        return Supports(reasoning_effort, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_bool, from_none], self.reasoning_effort)
        if self.vision is not None:
            result["vision"] = from_union([from_bool, from_none], self.vision)
        return result


@dataclass
class Capabilities:
    """Model capabilities and limits"""

    limits: Limits
    """Token limits for prompts, outputs, and context window"""

    supports: Supports
    """Feature flags indicating what the model supports"""

    @staticmethod
    def from_dict(obj: Any) -> 'Capabilities':
        assert isinstance(obj, dict)
        limits = Limits.from_dict(obj.get("limits"))
        supports = Supports.from_dict(obj.get("supports"))
        return Capabilities(limits, supports)

    def to_dict(self) -> dict:
        result: dict = {}
        result["limits"] = to_class(Limits, self.limits)
        result["supports"] = to_class(Supports, self.supports)
        return result


@dataclass
class Policy:
    """Policy state (if applicable)"""

    state: str
    """Current policy state for this model"""

    terms: str
    """Usage terms or conditions for this model"""

    @staticmethod
    def from_dict(obj: Any) -> 'Policy':
        assert isinstance(obj, dict)
        state = from_str(obj.get("state"))
        terms = from_str(obj.get("terms"))
        return Policy(state, terms)

    def to_dict(self) -> dict:
        result: dict = {}
        result["state"] = from_str(self.state)
        result["terms"] = from_str(self.terms)
        return result


@dataclass
class Model:
    capabilities: Capabilities
    """Model capabilities and limits"""

    id: str
    """Model identifier (e.g., "claude-sonnet-4.5")"""

    name: str
    """Display name"""

    billing: Billing | None = None
    """Billing information"""

    default_reasoning_effort: str | None = None
    """Default reasoning effort level (only present if model supports reasoning effort)"""

    policy: Policy | None = None
    """Policy state (if applicable)"""

    supported_reasoning_efforts: list[str] | None = None
    """Supported reasoning effort levels (only present if model supports reasoning effort)"""

    @staticmethod
    def from_dict(obj: Any) -> 'Model':
        assert isinstance(obj, dict)
        capabilities = Capabilities.from_dict(obj.get("capabilities"))
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        billing = from_union([Billing.from_dict, from_none], obj.get("billing"))
        default_reasoning_effort = from_union([from_str, from_none], obj.get("defaultReasoningEffort"))
        policy = from_union([Policy.from_dict, from_none], obj.get("policy"))
        supported_reasoning_efforts = from_union([lambda x: from_list(from_str, x), from_none], obj.get("supportedReasoningEfforts"))
        return Model(capabilities, id, name, billing, default_reasoning_effort, policy, supported_reasoning_efforts)

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = to_class(Capabilities, self.capabilities)
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        if self.billing is not None:
            result["billing"] = from_union([lambda x: to_class(Billing, x), from_none], self.billing)
        if self.default_reasoning_effort is not None:
            result["defaultReasoningEffort"] = from_union([from_str, from_none], self.default_reasoning_effort)
        if self.policy is not None:
            result["policy"] = from_union([lambda x: to_class(Policy, x), from_none], self.policy)
        if self.supported_reasoning_efforts is not None:
            result["supportedReasoningEfforts"] = from_union([lambda x: from_list(from_str, x), from_none], self.supported_reasoning_efforts)
        return result


@dataclass
class ModelsListResult:
    models: list[Model]
    """List of available models with full metadata"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelsListResult':
        assert isinstance(obj, dict)
        models = from_list(Model.from_dict, obj.get("models"))
        return ModelsListResult(models)

    def to_dict(self) -> dict:
        result: dict = {}
        result["models"] = from_list(lambda x: to_class(Model, x), self.models)
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
class ToolsListResult:
    tools: list[Tool]
    """List of available built-in tools with metadata"""

    @staticmethod
    def from_dict(obj: Any) -> 'ToolsListResult':
        assert isinstance(obj, dict)
        tools = from_list(Tool.from_dict, obj.get("tools"))
        return ToolsListResult(tools)

    def to_dict(self) -> dict:
        result: dict = {}
        result["tools"] = from_list(lambda x: to_class(Tool, x), self.tools)
        return result


@dataclass
class ToolsListParams:
    model: str | None = None
    """Optional model ID — when provided, the returned tool list reflects model-specific
    overrides
    """

    @staticmethod
    def from_dict(obj: Any) -> 'ToolsListParams':
        assert isinstance(obj, dict)
        model = from_union([from_str, from_none], obj.get("model"))
        return ToolsListParams(model)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model is not None:
            result["model"] = from_union([from_str, from_none], self.model)
        return result


@dataclass
class QuotaSnapshot:
    entitlement_requests: float
    """Number of requests included in the entitlement"""

    overage: float
    """Number of overage requests made this period"""

    overage_allowed_with_exhausted_quota: bool
    """Whether pay-per-request usage is allowed when quota is exhausted"""

    remaining_percentage: float
    """Percentage of entitlement remaining"""

    used_requests: float
    """Number of requests used so far this period"""

    reset_date: str | None = None
    """Date when the quota resets (ISO 8601)"""

    @staticmethod
    def from_dict(obj: Any) -> 'QuotaSnapshot':
        assert isinstance(obj, dict)
        entitlement_requests = from_float(obj.get("entitlementRequests"))
        overage = from_float(obj.get("overage"))
        overage_allowed_with_exhausted_quota = from_bool(obj.get("overageAllowedWithExhaustedQuota"))
        remaining_percentage = from_float(obj.get("remainingPercentage"))
        used_requests = from_float(obj.get("usedRequests"))
        reset_date = from_union([from_str, from_none], obj.get("resetDate"))
        return QuotaSnapshot(entitlement_requests, overage, overage_allowed_with_exhausted_quota, remaining_percentage, used_requests, reset_date)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entitlementRequests"] = to_float(self.entitlement_requests)
        result["overage"] = to_float(self.overage)
        result["overageAllowedWithExhaustedQuota"] = from_bool(self.overage_allowed_with_exhausted_quota)
        result["remainingPercentage"] = to_float(self.remaining_percentage)
        result["usedRequests"] = to_float(self.used_requests)
        if self.reset_date is not None:
            result["resetDate"] = from_union([from_str, from_none], self.reset_date)
        return result


@dataclass
class AccountGetQuotaResult:
    quota_snapshots: dict[str, QuotaSnapshot]
    """Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)"""

    @staticmethod
    def from_dict(obj: Any) -> 'AccountGetQuotaResult':
        assert isinstance(obj, dict)
        quota_snapshots = from_dict(QuotaSnapshot.from_dict, obj.get("quotaSnapshots"))
        return AccountGetQuotaResult(quota_snapshots)

    def to_dict(self) -> dict:
        result: dict = {}
        result["quotaSnapshots"] = from_dict(lambda x: to_class(QuotaSnapshot, x), self.quota_snapshots)
        return result


@dataclass
class SessionModelGetCurrentResult:
    model_id: str | None = None
    """Currently active model identifier"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModelGetCurrentResult':
        assert isinstance(obj, dict)
        model_id = from_union([from_str, from_none], obj.get("modelId"))
        return SessionModelGetCurrentResult(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model_id is not None:
            result["modelId"] = from_union([from_str, from_none], self.model_id)
        return result


@dataclass
class SessionModelSwitchToResult:
    model_id: str | None = None
    """Currently active model identifier after the switch"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModelSwitchToResult':
        assert isinstance(obj, dict)
        model_id = from_union([from_str, from_none], obj.get("modelId"))
        return SessionModelSwitchToResult(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.model_id is not None:
            result["modelId"] = from_union([from_str, from_none], self.model_id)
        return result


@dataclass
class SessionModelSwitchToParams:
    model_id: str
    """Model identifier to switch to"""

    reasoning_effort: str | None = None
    """Reasoning effort level to use for the model"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModelSwitchToParams':
        assert isinstance(obj, dict)
        model_id = from_str(obj.get("modelId"))
        reasoning_effort = from_union([from_str, from_none], obj.get("reasoningEffort"))
        return SessionModelSwitchToParams(model_id, reasoning_effort)

    def to_dict(self) -> dict:
        result: dict = {}
        result["modelId"] = from_str(self.model_id)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_str, from_none], self.reasoning_effort)
        return result


class Mode(Enum):
    """The current agent mode.
    
    The agent mode after switching.
    
    The mode to switch to. Valid values: "interactive", "plan", "autopilot".
    """
    AUTOPILOT = "autopilot"
    INTERACTIVE = "interactive"
    PLAN = "plan"


@dataclass
class SessionModeGetResult:
    mode: Mode
    """The current agent mode."""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModeGetResult':
        assert isinstance(obj, dict)
        mode = Mode(obj.get("mode"))
        return SessionModeGetResult(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(Mode, self.mode)
        return result


@dataclass
class SessionModeSetResult:
    mode: Mode
    """The agent mode after switching."""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModeSetResult':
        assert isinstance(obj, dict)
        mode = Mode(obj.get("mode"))
        return SessionModeSetResult(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(Mode, self.mode)
        return result


@dataclass
class SessionModeSetParams:
    mode: Mode
    """The mode to switch to. Valid values: "interactive", "plan", "autopilot"."""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModeSetParams':
        assert isinstance(obj, dict)
        mode = Mode(obj.get("mode"))
        return SessionModeSetParams(mode)

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(Mode, self.mode)
        return result


@dataclass
class SessionPlanReadResult:
    exists: bool
    """Whether the plan file exists in the workspace"""

    content: str | None = None
    """The content of the plan file, or null if it does not exist"""

    path: str | None = None
    """Absolute file path of the plan file, or null if workspace is not enabled"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPlanReadResult':
        assert isinstance(obj, dict)
        exists = from_bool(obj.get("exists"))
        content = from_union([from_none, from_str], obj.get("content"))
        path = from_union([from_none, from_str], obj.get("path"))
        return SessionPlanReadResult(exists, content, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["exists"] = from_bool(self.exists)
        result["content"] = from_union([from_none, from_str], self.content)
        result["path"] = from_union([from_none, from_str], self.path)
        return result


@dataclass
class SessionPlanUpdateResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionPlanUpdateResult':
        assert isinstance(obj, dict)
        return SessionPlanUpdateResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


@dataclass
class SessionPlanUpdateParams:
    content: str
    """The new content for the plan file"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPlanUpdateParams':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return SessionPlanUpdateParams(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result


@dataclass
class SessionPlanDeleteResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionPlanDeleteResult':
        assert isinstance(obj, dict)
        return SessionPlanDeleteResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


@dataclass
class SessionWorkspaceListFilesResult:
    files: list[str]
    """Relative file paths in the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionWorkspaceListFilesResult':
        assert isinstance(obj, dict)
        files = from_list(from_str, obj.get("files"))
        return SessionWorkspaceListFilesResult(files)

    def to_dict(self) -> dict:
        result: dict = {}
        result["files"] = from_list(from_str, self.files)
        return result


@dataclass
class SessionWorkspaceReadFileResult:
    content: str
    """File content as a UTF-8 string"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionWorkspaceReadFileResult':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return SessionWorkspaceReadFileResult(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result


@dataclass
class SessionWorkspaceReadFileParams:
    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionWorkspaceReadFileParams':
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return SessionWorkspaceReadFileParams(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result


@dataclass
class SessionWorkspaceCreateFileResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionWorkspaceCreateFileResult':
        assert isinstance(obj, dict)
        return SessionWorkspaceCreateFileResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


@dataclass
class SessionWorkspaceCreateFileParams:
    content: str
    """File content to write as a UTF-8 string"""

    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionWorkspaceCreateFileParams':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        path = from_str(obj.get("path"))
        return SessionWorkspaceCreateFileParams(content, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["path"] = from_str(self.path)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionFleetStartResult:
    started: bool
    """Whether fleet mode was successfully activated"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFleetStartResult':
        assert isinstance(obj, dict)
        started = from_bool(obj.get("started"))
        return SessionFleetStartResult(started)

    def to_dict(self) -> dict:
        result: dict = {}
        result["started"] = from_bool(self.started)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionFleetStartParams:
    prompt: str | None = None
    """Optional user prompt to combine with fleet instructions"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFleetStartParams':
        assert isinstance(obj, dict)
        prompt = from_union([from_str, from_none], obj.get("prompt"))
        return SessionFleetStartParams(prompt)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.prompt is not None:
            result["prompt"] = from_union([from_str, from_none], self.prompt)
        return result


@dataclass
class SessionAgentListResultAgent:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentListResultAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return SessionAgentListResultAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAgentListResult:
    agents: list[SessionAgentListResultAgent]
    """Available custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentListResult':
        assert isinstance(obj, dict)
        agents = from_list(SessionAgentListResultAgent.from_dict, obj.get("agents"))
        return SessionAgentListResult(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(SessionAgentListResultAgent, x), self.agents)
        return result


@dataclass
class SessionAgentGetCurrentResultAgent:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentGetCurrentResultAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return SessionAgentGetCurrentResultAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAgentGetCurrentResult:
    agent: SessionAgentGetCurrentResultAgent | None = None
    """Currently selected custom agent, or null if using the default agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentGetCurrentResult':
        assert isinstance(obj, dict)
        agent = from_union([SessionAgentGetCurrentResultAgent.from_dict, from_none], obj.get("agent"))
        return SessionAgentGetCurrentResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agent"] = from_union([lambda x: to_class(SessionAgentGetCurrentResultAgent, x), from_none], self.agent)
        return result


@dataclass
class SessionAgentSelectResultAgent:
    """The newly selected custom agent"""

    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentSelectResultAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return SessionAgentSelectResultAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAgentSelectResult:
    agent: SessionAgentSelectResultAgent
    """The newly selected custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentSelectResult':
        assert isinstance(obj, dict)
        agent = SessionAgentSelectResultAgent.from_dict(obj.get("agent"))
        return SessionAgentSelectResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agent"] = to_class(SessionAgentSelectResultAgent, self.agent)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAgentSelectParams:
    name: str
    """Name of the custom agent to select"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentSelectParams':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SessionAgentSelectParams(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAgentDeselectResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentDeselectResult':
        assert isinstance(obj, dict)
        return SessionAgentDeselectResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


@dataclass
class SessionAgentReloadResultAgent:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentReloadResultAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return SessionAgentReloadResultAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAgentReloadResult:
    agents: list[SessionAgentReloadResultAgent]
    """Reloaded custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentReloadResult':
        assert isinstance(obj, dict)
        agents = from_list(SessionAgentReloadResultAgent.from_dict, obj.get("agents"))
        return SessionAgentReloadResult(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(SessionAgentReloadResultAgent, x), self.agents)
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


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSkillsListResult:
    skills: list[Skill]
    """Available skills"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionSkillsListResult':
        assert isinstance(obj, dict)
        skills = from_list(Skill.from_dict, obj.get("skills"))
        return SessionSkillsListResult(skills)

    def to_dict(self) -> dict:
        result: dict = {}
        result["skills"] = from_list(lambda x: to_class(Skill, x), self.skills)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSkillsEnableResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionSkillsEnableResult':
        assert isinstance(obj, dict)
        return SessionSkillsEnableResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSkillsEnableParams:
    name: str
    """Name of the skill to enable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionSkillsEnableParams':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SessionSkillsEnableParams(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSkillsDisableResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionSkillsDisableResult':
        assert isinstance(obj, dict)
        return SessionSkillsDisableResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSkillsDisableParams:
    name: str
    """Name of the skill to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionSkillsDisableParams':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        return SessionSkillsDisableParams(name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionSkillsReloadResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionSkillsReloadResult':
        assert isinstance(obj, dict)
        return SessionSkillsReloadResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


class ServerStatus(Enum):
    """Connection status: connected, failed, pending, disabled, or not_configured"""

    CONNECTED = "connected"
    DISABLED = "disabled"
    FAILED = "failed"
    NOT_CONFIGURED = "not_configured"
    PENDING = "pending"


@dataclass
class Server:
    name: str
    """Server name (config key)"""

    status: ServerStatus
    """Connection status: connected, failed, pending, disabled, or not_configured"""

    error: str | None = None
    """Error message if the server failed to connect"""

    source: str | None = None
    """Configuration source: user, workspace, plugin, or builtin"""

    @staticmethod
    def from_dict(obj: Any) -> 'Server':
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        status = ServerStatus(obj.get("status"))
        error = from_union([from_str, from_none], obj.get("error"))
        source = from_union([from_str, from_none], obj.get("source"))
        return Server(name, status, error, source)

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["status"] = to_enum(ServerStatus, self.status)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.source is not None:
            result["source"] = from_union([from_str, from_none], self.source)
        return result


@dataclass
class SessionMCPListResult:
    servers: list[Server]
    """Configured MCP servers"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionMCPListResult':
        assert isinstance(obj, dict)
        servers = from_list(Server.from_dict, obj.get("servers"))
        return SessionMCPListResult(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_list(lambda x: to_class(Server, x), self.servers)
        return result


@dataclass
class SessionMCPEnableResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionMCPEnableResult':
        assert isinstance(obj, dict)
        return SessionMCPEnableResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


@dataclass
class SessionMCPEnableParams:
    server_name: str
    """Name of the MCP server to enable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionMCPEnableParams':
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return SessionMCPEnableParams(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result


@dataclass
class SessionMCPDisableResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionMCPDisableResult':
        assert isinstance(obj, dict)
        return SessionMCPDisableResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


@dataclass
class SessionMCPDisableParams:
    server_name: str
    """Name of the MCP server to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionMCPDisableParams':
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return SessionMCPDisableParams(server_name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result


@dataclass
class SessionMCPReloadResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionMCPReloadResult':
        assert isinstance(obj, dict)
        return SessionMCPReloadResult()

    def to_dict(self) -> dict:
        result: dict = {}
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


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionPluginsListResult:
    plugins: list[Plugin]
    """Installed plugins"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPluginsListResult':
        assert isinstance(obj, dict)
        plugins = from_list(Plugin.from_dict, obj.get("plugins"))
        return SessionPluginsListResult(plugins)

    def to_dict(self) -> dict:
        result: dict = {}
        result["plugins"] = from_list(lambda x: to_class(Plugin, x), self.plugins)
        return result


class Source(Enum):
    """Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/)"""

    PROJECT = "project"
    USER = "user"


class ExtensionStatus(Enum):
    """Current status: running, disabled, failed, or starting"""

    DISABLED = "disabled"
    FAILED = "failed"
    RUNNING = "running"
    STARTING = "starting"


@dataclass
class Extension:
    id: str
    """Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper')"""

    name: str
    """Extension name (directory name)"""

    source: Source
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
        source = Source(obj.get("source"))
        status = ExtensionStatus(obj.get("status"))
        pid = from_union([from_int, from_none], obj.get("pid"))
        return Extension(id, name, source, status, pid)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(Source, self.source)
        result["status"] = to_enum(ExtensionStatus, self.status)
        if self.pid is not None:
            result["pid"] = from_union([from_int, from_none], self.pid)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionExtensionsListResult:
    extensions: list[Extension]
    """Discovered extensions and their current status"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionExtensionsListResult':
        assert isinstance(obj, dict)
        extensions = from_list(Extension.from_dict, obj.get("extensions"))
        return SessionExtensionsListResult(extensions)

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensions"] = from_list(lambda x: to_class(Extension, x), self.extensions)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionExtensionsEnableResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionExtensionsEnableResult':
        assert isinstance(obj, dict)
        return SessionExtensionsEnableResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionExtensionsEnableParams:
    id: str
    """Source-qualified extension ID to enable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionExtensionsEnableParams':
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return SessionExtensionsEnableParams(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionExtensionsDisableResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionExtensionsDisableResult':
        assert isinstance(obj, dict)
        return SessionExtensionsDisableResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionExtensionsDisableParams:
    id: str
    """Source-qualified extension ID to disable"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionExtensionsDisableParams':
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        return SessionExtensionsDisableParams(id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionExtensionsReloadResult:
    @staticmethod
    def from_dict(obj: Any) -> 'SessionExtensionsReloadResult':
        assert isinstance(obj, dict)
        return SessionExtensionsReloadResult()

    def to_dict(self) -> dict:
        result: dict = {}
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCompactionCompactResult:
    messages_removed: float
    """Number of messages removed during compaction"""

    success: bool
    """Whether compaction completed successfully"""

    tokens_removed: float
    """Number of tokens freed by compaction"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionCompactionCompactResult':
        assert isinstance(obj, dict)
        messages_removed = from_float(obj.get("messagesRemoved"))
        success = from_bool(obj.get("success"))
        tokens_removed = from_float(obj.get("tokensRemoved"))
        return SessionCompactionCompactResult(messages_removed, success, tokens_removed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["messagesRemoved"] = to_float(self.messages_removed)
        result["success"] = from_bool(self.success)
        result["tokensRemoved"] = to_float(self.tokens_removed)
        return result


@dataclass
class SessionToolsHandlePendingToolCallResult:
    success: bool
    """Whether the tool call result was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionToolsHandlePendingToolCallResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionToolsHandlePendingToolCallResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result


@dataclass
class ResultResult:
    text_result_for_llm: str
    error: str | None = None
    result_type: str | None = None
    tool_telemetry: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'ResultResult':
        assert isinstance(obj, dict)
        text_result_for_llm = from_str(obj.get("textResultForLlm"))
        error = from_union([from_str, from_none], obj.get("error"))
        result_type = from_union([from_str, from_none], obj.get("resultType"))
        tool_telemetry = from_union([lambda x: from_dict(lambda x: x, x), from_none], obj.get("toolTelemetry"))
        return ResultResult(text_result_for_llm, error, result_type, tool_telemetry)

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
class SessionToolsHandlePendingToolCallParams:
    request_id: str
    error: str | None = None
    result: ResultResult | str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'SessionToolsHandlePendingToolCallParams':
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        error = from_union([from_str, from_none], obj.get("error"))
        result = from_union([ResultResult.from_dict, from_str, from_none], obj.get("result"))
        return SessionToolsHandlePendingToolCallParams(request_id, error, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        if self.result is not None:
            result["result"] = from_union([lambda x: to_class(ResultResult, x), from_str, from_none], self.result)
        return result


@dataclass
class SessionCommandsHandlePendingCommandResult:
    success: bool

    @staticmethod
    def from_dict(obj: Any) -> 'SessionCommandsHandlePendingCommandResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionCommandsHandlePendingCommandResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result


@dataclass
class SessionCommandsHandlePendingCommandParams:
    request_id: str
    """Request ID from the command invocation event"""

    error: str | None = None
    """Error message if the command handler failed"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionCommandsHandlePendingCommandParams':
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        error = from_union([from_str, from_none], obj.get("error"))
        return SessionCommandsHandlePendingCommandParams(request_id, error)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.error is not None:
            result["error"] = from_union([from_str, from_none], self.error)
        return result


class Action(Enum):
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    ACCEPT = "accept"
    CANCEL = "cancel"
    DECLINE = "decline"


@dataclass
class SessionUIElicitationResult:
    action: Action
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    content: dict[str, float | bool | list[str] | str] | None = None
    """The form values submitted by the user (present when action is 'accept')"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionUIElicitationResult':
        assert isinstance(obj, dict)
        action = Action(obj.get("action"))
        content = from_union([lambda x: from_dict(lambda x: from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str], x), x), from_none], obj.get("content"))
        return SessionUIElicitationResult(action, content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["action"] = to_enum(Action, self.action)
        if self.content is not None:
            result["content"] = from_union([lambda x: from_dict(lambda x: from_union([to_float, from_bool, lambda x: from_list(from_str, x), from_str], x), x), from_none], self.content)
        return result


class Format(Enum):
    DATE = "date"
    DATE_TIME = "date-time"
    EMAIL = "email"
    URI = "uri"


@dataclass
class AnyOf:
    const: str
    title: str

    @staticmethod
    def from_dict(obj: Any) -> 'AnyOf':
        assert isinstance(obj, dict)
        const = from_str(obj.get("const"))
        title = from_str(obj.get("title"))
        return AnyOf(const, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["const"] = from_str(self.const)
        result["title"] = from_str(self.title)
        return result


class ItemsType(Enum):
    STRING = "string"


@dataclass
class Items:
    enum: list[str] | None = None
    type: ItemsType | None = None
    any_of: list[AnyOf] | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'Items':
        assert isinstance(obj, dict)
        enum = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enum"))
        type = from_union([ItemsType, from_none], obj.get("type"))
        any_of = from_union([lambda x: from_list(AnyOf.from_dict, x), from_none], obj.get("anyOf"))
        return Items(enum, type, any_of)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.enum is not None:
            result["enum"] = from_union([lambda x: from_list(from_str, x), from_none], self.enum)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(ItemsType, x), from_none], self.type)
        if self.any_of is not None:
            result["anyOf"] = from_union([lambda x: from_list(lambda x: to_class(AnyOf, x), x), from_none], self.any_of)
        return result


@dataclass
class OneOf:
    const: str
    title: str

    @staticmethod
    def from_dict(obj: Any) -> 'OneOf':
        assert isinstance(obj, dict)
        const = from_str(obj.get("const"))
        title = from_str(obj.get("title"))
        return OneOf(const, title)

    def to_dict(self) -> dict:
        result: dict = {}
        result["const"] = from_str(self.const)
        result["title"] = from_str(self.title)
        return result


class PropertyType(Enum):
    ARRAY = "array"
    BOOLEAN = "boolean"
    INTEGER = "integer"
    NUMBER = "number"
    STRING = "string"


@dataclass
class Property:
    type: PropertyType
    default: float | bool | list[str] | str | None = None
    description: str | None = None
    enum: list[str] | None = None
    enum_names: list[str] | None = None
    title: str | None = None
    one_of: list[OneOf] | None = None
    items: Items | None = None
    max_items: float | None = None
    min_items: float | None = None
    format: Format | None = None
    max_length: float | None = None
    min_length: float | None = None
    maximum: float | None = None
    minimum: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'Property':
        assert isinstance(obj, dict)
        type = PropertyType(obj.get("type"))
        default = from_union([from_float, from_bool, lambda x: from_list(from_str, x), from_str, from_none], obj.get("default"))
        description = from_union([from_str, from_none], obj.get("description"))
        enum = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enum"))
        enum_names = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enumNames"))
        title = from_union([from_str, from_none], obj.get("title"))
        one_of = from_union([lambda x: from_list(OneOf.from_dict, x), from_none], obj.get("oneOf"))
        items = from_union([Items.from_dict, from_none], obj.get("items"))
        max_items = from_union([from_float, from_none], obj.get("maxItems"))
        min_items = from_union([from_float, from_none], obj.get("minItems"))
        format = from_union([Format, from_none], obj.get("format"))
        max_length = from_union([from_float, from_none], obj.get("maxLength"))
        min_length = from_union([from_float, from_none], obj.get("minLength"))
        maximum = from_union([from_float, from_none], obj.get("maximum"))
        minimum = from_union([from_float, from_none], obj.get("minimum"))
        return Property(type, default, description, enum, enum_names, title, one_of, items, max_items, min_items, format, max_length, min_length, maximum, minimum)

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(PropertyType, self.type)
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
            result["oneOf"] = from_union([lambda x: from_list(lambda x: to_class(OneOf, x), x), from_none], self.one_of)
        if self.items is not None:
            result["items"] = from_union([lambda x: to_class(Items, x), from_none], self.items)
        if self.max_items is not None:
            result["maxItems"] = from_union([to_float, from_none], self.max_items)
        if self.min_items is not None:
            result["minItems"] = from_union([to_float, from_none], self.min_items)
        if self.format is not None:
            result["format"] = from_union([lambda x: to_enum(Format, x), from_none], self.format)
        if self.max_length is not None:
            result["maxLength"] = from_union([to_float, from_none], self.max_length)
        if self.min_length is not None:
            result["minLength"] = from_union([to_float, from_none], self.min_length)
        if self.maximum is not None:
            result["maximum"] = from_union([to_float, from_none], self.maximum)
        if self.minimum is not None:
            result["minimum"] = from_union([to_float, from_none], self.minimum)
        return result


class RequestedSchemaType(Enum):
    OBJECT = "object"


@dataclass
class RequestedSchema:
    """JSON Schema describing the form fields to present to the user"""

    properties: dict[str, Property]
    """Form field definitions, keyed by field name"""

    type: RequestedSchemaType
    """Schema type indicator (always 'object')"""

    required: list[str] | None = None
    """List of required field names"""

    @staticmethod
    def from_dict(obj: Any) -> 'RequestedSchema':
        assert isinstance(obj, dict)
        properties = from_dict(Property.from_dict, obj.get("properties"))
        type = RequestedSchemaType(obj.get("type"))
        required = from_union([lambda x: from_list(from_str, x), from_none], obj.get("required"))
        return RequestedSchema(properties, type, required)

    def to_dict(self) -> dict:
        result: dict = {}
        result["properties"] = from_dict(lambda x: to_class(Property, x), self.properties)
        result["type"] = to_enum(RequestedSchemaType, self.type)
        if self.required is not None:
            result["required"] = from_union([lambda x: from_list(from_str, x), from_none], self.required)
        return result


@dataclass
class SessionUIElicitationParams:
    message: str
    """Message describing what information is needed from the user"""

    requested_schema: RequestedSchema
    """JSON Schema describing the form fields to present to the user"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionUIElicitationParams':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        requested_schema = RequestedSchema.from_dict(obj.get("requestedSchema"))
        return SessionUIElicitationParams(message, requested_schema)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["requestedSchema"] = to_class(RequestedSchema, self.requested_schema)
        return result


@dataclass
class SessionPermissionsHandlePendingPermissionRequestResult:
    success: bool
    """Whether the permission request was handled successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPermissionsHandlePendingPermissionRequestResult':
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        return SessionPermissionsHandlePendingPermissionRequestResult(success)

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        return result


class Kind(Enum):
    APPROVED = "approved"
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"
    DENIED_BY_RULES = "denied-by-rules"
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"


@dataclass
class SessionPermissionsHandlePendingPermissionRequestParamsResult:
    kind: Kind
    rules: list[Any] | None = None
    feedback: str | None = None
    message: str | None = None
    path: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPermissionsHandlePendingPermissionRequestParamsResult':
        assert isinstance(obj, dict)
        kind = Kind(obj.get("kind"))
        rules = from_union([lambda x: from_list(lambda x: x, x), from_none], obj.get("rules"))
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        message = from_union([from_str, from_none], obj.get("message"))
        path = from_union([from_str, from_none], obj.get("path"))
        return SessionPermissionsHandlePendingPermissionRequestParamsResult(kind, rules, feedback, message, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(Kind, self.kind)
        if self.rules is not None:
            result["rules"] = from_union([lambda x: from_list(lambda x: x, x), from_none], self.rules)
        if self.feedback is not None:
            result["feedback"] = from_union([from_str, from_none], self.feedback)
        if self.message is not None:
            result["message"] = from_union([from_str, from_none], self.message)
        if self.path is not None:
            result["path"] = from_union([from_str, from_none], self.path)
        return result


@dataclass
class SessionPermissionsHandlePendingPermissionRequestParams:
    request_id: str
    result: SessionPermissionsHandlePendingPermissionRequestParamsResult

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPermissionsHandlePendingPermissionRequestParams':
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = SessionPermissionsHandlePendingPermissionRequestParamsResult.from_dict(obj.get("result"))
        return SessionPermissionsHandlePendingPermissionRequestParams(request_id, result)

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = to_class(SessionPermissionsHandlePendingPermissionRequestParamsResult, self.result)
        return result


@dataclass
class SessionLogResult:
    event_id: UUID
    """The unique identifier of the emitted session event"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionLogResult':
        assert isinstance(obj, dict)
        event_id = UUID(obj.get("eventId"))
        return SessionLogResult(event_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventId"] = str(self.event_id)
        return result


class Level(Enum):
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    ERROR = "error"
    INFO = "info"
    WARNING = "warning"


@dataclass
class SessionLogParams:
    message: str
    """Human-readable message"""

    ephemeral: bool | None = None
    """When true, the message is transient and not persisted to the session event log on disk"""

    level: Level | None = None
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    url: str | None = None
    """Optional URL the user can open in their browser for more details"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionLogParams':
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        ephemeral = from_union([from_bool, from_none], obj.get("ephemeral"))
        level = from_union([Level, from_none], obj.get("level"))
        url = from_union([from_str, from_none], obj.get("url"))
        return SessionLogParams(message, ephemeral, level, url)

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.ephemeral is not None:
            result["ephemeral"] = from_union([from_bool, from_none], self.ephemeral)
        if self.level is not None:
            result["level"] = from_union([lambda x: to_enum(Level, x), from_none], self.level)
        if self.url is not None:
            result["url"] = from_union([from_str, from_none], self.url)
        return result


@dataclass
class SessionShellExecResult:
    process_id: str
    """Unique identifier for tracking streamed output"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionShellExecResult':
        assert isinstance(obj, dict)
        process_id = from_str(obj.get("processId"))
        return SessionShellExecResult(process_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["processId"] = from_str(self.process_id)
        return result


@dataclass
class SessionShellExecParams:
    command: str
    """Shell command to execute"""

    cwd: str | None = None
    """Working directory (defaults to session working directory)"""

    timeout: float | None = None
    """Timeout in milliseconds (default: 30000)"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionShellExecParams':
        assert isinstance(obj, dict)
        command = from_str(obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        timeout = from_union([from_float, from_none], obj.get("timeout"))
        return SessionShellExecParams(command, cwd, timeout)

    def to_dict(self) -> dict:
        result: dict = {}
        result["command"] = from_str(self.command)
        if self.cwd is not None:
            result["cwd"] = from_union([from_str, from_none], self.cwd)
        if self.timeout is not None:
            result["timeout"] = from_union([to_float, from_none], self.timeout)
        return result


@dataclass
class SessionShellKillResult:
    killed: bool
    """Whether the signal was sent successfully"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionShellKillResult':
        assert isinstance(obj, dict)
        killed = from_bool(obj.get("killed"))
        return SessionShellKillResult(killed)

    def to_dict(self) -> dict:
        result: dict = {}
        result["killed"] = from_bool(self.killed)
        return result


class Signal(Enum):
    """Signal to send (default: SIGTERM)"""

    SIGINT = "SIGINT"
    SIGKILL = "SIGKILL"
    SIGTERM = "SIGTERM"


@dataclass
class SessionShellKillParams:
    process_id: str
    """Process identifier returned by shell.exec"""

    signal: Signal | None = None
    """Signal to send (default: SIGTERM)"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionShellKillParams':
        assert isinstance(obj, dict)
        process_id = from_str(obj.get("processId"))
        signal = from_union([Signal, from_none], obj.get("signal"))
        return SessionShellKillParams(process_id, signal)

    def to_dict(self) -> dict:
        result: dict = {}
        result["processId"] = from_str(self.process_id)
        if self.signal is not None:
            result["signal"] = from_union([lambda x: to_enum(Signal, x), from_none], self.signal)
        return result


def ping_result_from_dict(s: Any) -> PingResult:
    return PingResult.from_dict(s)


def ping_result_to_dict(x: PingResult) -> Any:
    return to_class(PingResult, x)


def ping_params_from_dict(s: Any) -> PingParams:
    return PingParams.from_dict(s)


def ping_params_to_dict(x: PingParams) -> Any:
    return to_class(PingParams, x)


def models_list_result_from_dict(s: Any) -> ModelsListResult:
    return ModelsListResult.from_dict(s)


def models_list_result_to_dict(x: ModelsListResult) -> Any:
    return to_class(ModelsListResult, x)


def tools_list_result_from_dict(s: Any) -> ToolsListResult:
    return ToolsListResult.from_dict(s)


def tools_list_result_to_dict(x: ToolsListResult) -> Any:
    return to_class(ToolsListResult, x)


def tools_list_params_from_dict(s: Any) -> ToolsListParams:
    return ToolsListParams.from_dict(s)


def tools_list_params_to_dict(x: ToolsListParams) -> Any:
    return to_class(ToolsListParams, x)


def account_get_quota_result_from_dict(s: Any) -> AccountGetQuotaResult:
    return AccountGetQuotaResult.from_dict(s)


def account_get_quota_result_to_dict(x: AccountGetQuotaResult) -> Any:
    return to_class(AccountGetQuotaResult, x)


def session_model_get_current_result_from_dict(s: Any) -> SessionModelGetCurrentResult:
    return SessionModelGetCurrentResult.from_dict(s)


def session_model_get_current_result_to_dict(x: SessionModelGetCurrentResult) -> Any:
    return to_class(SessionModelGetCurrentResult, x)


def session_model_switch_to_result_from_dict(s: Any) -> SessionModelSwitchToResult:
    return SessionModelSwitchToResult.from_dict(s)


def session_model_switch_to_result_to_dict(x: SessionModelSwitchToResult) -> Any:
    return to_class(SessionModelSwitchToResult, x)


def session_model_switch_to_params_from_dict(s: Any) -> SessionModelSwitchToParams:
    return SessionModelSwitchToParams.from_dict(s)


def session_model_switch_to_params_to_dict(x: SessionModelSwitchToParams) -> Any:
    return to_class(SessionModelSwitchToParams, x)


def session_mode_get_result_from_dict(s: Any) -> SessionModeGetResult:
    return SessionModeGetResult.from_dict(s)


def session_mode_get_result_to_dict(x: SessionModeGetResult) -> Any:
    return to_class(SessionModeGetResult, x)


def session_mode_set_result_from_dict(s: Any) -> SessionModeSetResult:
    return SessionModeSetResult.from_dict(s)


def session_mode_set_result_to_dict(x: SessionModeSetResult) -> Any:
    return to_class(SessionModeSetResult, x)


def session_mode_set_params_from_dict(s: Any) -> SessionModeSetParams:
    return SessionModeSetParams.from_dict(s)


def session_mode_set_params_to_dict(x: SessionModeSetParams) -> Any:
    return to_class(SessionModeSetParams, x)


def session_plan_read_result_from_dict(s: Any) -> SessionPlanReadResult:
    return SessionPlanReadResult.from_dict(s)


def session_plan_read_result_to_dict(x: SessionPlanReadResult) -> Any:
    return to_class(SessionPlanReadResult, x)


def session_plan_update_result_from_dict(s: Any) -> SessionPlanUpdateResult:
    return SessionPlanUpdateResult.from_dict(s)


def session_plan_update_result_to_dict(x: SessionPlanUpdateResult) -> Any:
    return to_class(SessionPlanUpdateResult, x)


def session_plan_update_params_from_dict(s: Any) -> SessionPlanUpdateParams:
    return SessionPlanUpdateParams.from_dict(s)


def session_plan_update_params_to_dict(x: SessionPlanUpdateParams) -> Any:
    return to_class(SessionPlanUpdateParams, x)


def session_plan_delete_result_from_dict(s: Any) -> SessionPlanDeleteResult:
    return SessionPlanDeleteResult.from_dict(s)


def session_plan_delete_result_to_dict(x: SessionPlanDeleteResult) -> Any:
    return to_class(SessionPlanDeleteResult, x)


def session_workspace_list_files_result_from_dict(s: Any) -> SessionWorkspaceListFilesResult:
    return SessionWorkspaceListFilesResult.from_dict(s)


def session_workspace_list_files_result_to_dict(x: SessionWorkspaceListFilesResult) -> Any:
    return to_class(SessionWorkspaceListFilesResult, x)


def session_workspace_read_file_result_from_dict(s: Any) -> SessionWorkspaceReadFileResult:
    return SessionWorkspaceReadFileResult.from_dict(s)


def session_workspace_read_file_result_to_dict(x: SessionWorkspaceReadFileResult) -> Any:
    return to_class(SessionWorkspaceReadFileResult, x)


def session_workspace_read_file_params_from_dict(s: Any) -> SessionWorkspaceReadFileParams:
    return SessionWorkspaceReadFileParams.from_dict(s)


def session_workspace_read_file_params_to_dict(x: SessionWorkspaceReadFileParams) -> Any:
    return to_class(SessionWorkspaceReadFileParams, x)


def session_workspace_create_file_result_from_dict(s: Any) -> SessionWorkspaceCreateFileResult:
    return SessionWorkspaceCreateFileResult.from_dict(s)


def session_workspace_create_file_result_to_dict(x: SessionWorkspaceCreateFileResult) -> Any:
    return to_class(SessionWorkspaceCreateFileResult, x)


def session_workspace_create_file_params_from_dict(s: Any) -> SessionWorkspaceCreateFileParams:
    return SessionWorkspaceCreateFileParams.from_dict(s)


def session_workspace_create_file_params_to_dict(x: SessionWorkspaceCreateFileParams) -> Any:
    return to_class(SessionWorkspaceCreateFileParams, x)


def session_fleet_start_result_from_dict(s: Any) -> SessionFleetStartResult:
    return SessionFleetStartResult.from_dict(s)


def session_fleet_start_result_to_dict(x: SessionFleetStartResult) -> Any:
    return to_class(SessionFleetStartResult, x)


def session_fleet_start_params_from_dict(s: Any) -> SessionFleetStartParams:
    return SessionFleetStartParams.from_dict(s)


def session_fleet_start_params_to_dict(x: SessionFleetStartParams) -> Any:
    return to_class(SessionFleetStartParams, x)


def session_agent_list_result_from_dict(s: Any) -> SessionAgentListResult:
    return SessionAgentListResult.from_dict(s)


def session_agent_list_result_to_dict(x: SessionAgentListResult) -> Any:
    return to_class(SessionAgentListResult, x)


def session_agent_get_current_result_from_dict(s: Any) -> SessionAgentGetCurrentResult:
    return SessionAgentGetCurrentResult.from_dict(s)


def session_agent_get_current_result_to_dict(x: SessionAgentGetCurrentResult) -> Any:
    return to_class(SessionAgentGetCurrentResult, x)


def session_agent_select_result_from_dict(s: Any) -> SessionAgentSelectResult:
    return SessionAgentSelectResult.from_dict(s)


def session_agent_select_result_to_dict(x: SessionAgentSelectResult) -> Any:
    return to_class(SessionAgentSelectResult, x)


def session_agent_select_params_from_dict(s: Any) -> SessionAgentSelectParams:
    return SessionAgentSelectParams.from_dict(s)


def session_agent_select_params_to_dict(x: SessionAgentSelectParams) -> Any:
    return to_class(SessionAgentSelectParams, x)


def session_agent_deselect_result_from_dict(s: Any) -> SessionAgentDeselectResult:
    return SessionAgentDeselectResult.from_dict(s)


def session_agent_deselect_result_to_dict(x: SessionAgentDeselectResult) -> Any:
    return to_class(SessionAgentDeselectResult, x)


def session_agent_reload_result_from_dict(s: Any) -> SessionAgentReloadResult:
    return SessionAgentReloadResult.from_dict(s)


def session_agent_reload_result_to_dict(x: SessionAgentReloadResult) -> Any:
    return to_class(SessionAgentReloadResult, x)


def session_skills_list_result_from_dict(s: Any) -> SessionSkillsListResult:
    return SessionSkillsListResult.from_dict(s)


def session_skills_list_result_to_dict(x: SessionSkillsListResult) -> Any:
    return to_class(SessionSkillsListResult, x)


def session_skills_enable_result_from_dict(s: Any) -> SessionSkillsEnableResult:
    return SessionSkillsEnableResult.from_dict(s)


def session_skills_enable_result_to_dict(x: SessionSkillsEnableResult) -> Any:
    return to_class(SessionSkillsEnableResult, x)


def session_skills_enable_params_from_dict(s: Any) -> SessionSkillsEnableParams:
    return SessionSkillsEnableParams.from_dict(s)


def session_skills_enable_params_to_dict(x: SessionSkillsEnableParams) -> Any:
    return to_class(SessionSkillsEnableParams, x)


def session_skills_disable_result_from_dict(s: Any) -> SessionSkillsDisableResult:
    return SessionSkillsDisableResult.from_dict(s)


def session_skills_disable_result_to_dict(x: SessionSkillsDisableResult) -> Any:
    return to_class(SessionSkillsDisableResult, x)


def session_skills_disable_params_from_dict(s: Any) -> SessionSkillsDisableParams:
    return SessionSkillsDisableParams.from_dict(s)


def session_skills_disable_params_to_dict(x: SessionSkillsDisableParams) -> Any:
    return to_class(SessionSkillsDisableParams, x)


def session_skills_reload_result_from_dict(s: Any) -> SessionSkillsReloadResult:
    return SessionSkillsReloadResult.from_dict(s)


def session_skills_reload_result_to_dict(x: SessionSkillsReloadResult) -> Any:
    return to_class(SessionSkillsReloadResult, x)


def session_mcp_list_result_from_dict(s: Any) -> SessionMCPListResult:
    return SessionMCPListResult.from_dict(s)


def session_mcp_list_result_to_dict(x: SessionMCPListResult) -> Any:
    return to_class(SessionMCPListResult, x)


def session_mcp_enable_result_from_dict(s: Any) -> SessionMCPEnableResult:
    return SessionMCPEnableResult.from_dict(s)


def session_mcp_enable_result_to_dict(x: SessionMCPEnableResult) -> Any:
    return to_class(SessionMCPEnableResult, x)


def session_mcp_enable_params_from_dict(s: Any) -> SessionMCPEnableParams:
    return SessionMCPEnableParams.from_dict(s)


def session_mcp_enable_params_to_dict(x: SessionMCPEnableParams) -> Any:
    return to_class(SessionMCPEnableParams, x)


def session_mcp_disable_result_from_dict(s: Any) -> SessionMCPDisableResult:
    return SessionMCPDisableResult.from_dict(s)


def session_mcp_disable_result_to_dict(x: SessionMCPDisableResult) -> Any:
    return to_class(SessionMCPDisableResult, x)


def session_mcp_disable_params_from_dict(s: Any) -> SessionMCPDisableParams:
    return SessionMCPDisableParams.from_dict(s)


def session_mcp_disable_params_to_dict(x: SessionMCPDisableParams) -> Any:
    return to_class(SessionMCPDisableParams, x)


def session_mcp_reload_result_from_dict(s: Any) -> SessionMCPReloadResult:
    return SessionMCPReloadResult.from_dict(s)


def session_mcp_reload_result_to_dict(x: SessionMCPReloadResult) -> Any:
    return to_class(SessionMCPReloadResult, x)


def session_plugins_list_result_from_dict(s: Any) -> SessionPluginsListResult:
    return SessionPluginsListResult.from_dict(s)


def session_plugins_list_result_to_dict(x: SessionPluginsListResult) -> Any:
    return to_class(SessionPluginsListResult, x)


def session_extensions_list_result_from_dict(s: Any) -> SessionExtensionsListResult:
    return SessionExtensionsListResult.from_dict(s)


def session_extensions_list_result_to_dict(x: SessionExtensionsListResult) -> Any:
    return to_class(SessionExtensionsListResult, x)


def session_extensions_enable_result_from_dict(s: Any) -> SessionExtensionsEnableResult:
    return SessionExtensionsEnableResult.from_dict(s)


def session_extensions_enable_result_to_dict(x: SessionExtensionsEnableResult) -> Any:
    return to_class(SessionExtensionsEnableResult, x)


def session_extensions_enable_params_from_dict(s: Any) -> SessionExtensionsEnableParams:
    return SessionExtensionsEnableParams.from_dict(s)


def session_extensions_enable_params_to_dict(x: SessionExtensionsEnableParams) -> Any:
    return to_class(SessionExtensionsEnableParams, x)


def session_extensions_disable_result_from_dict(s: Any) -> SessionExtensionsDisableResult:
    return SessionExtensionsDisableResult.from_dict(s)


def session_extensions_disable_result_to_dict(x: SessionExtensionsDisableResult) -> Any:
    return to_class(SessionExtensionsDisableResult, x)


def session_extensions_disable_params_from_dict(s: Any) -> SessionExtensionsDisableParams:
    return SessionExtensionsDisableParams.from_dict(s)


def session_extensions_disable_params_to_dict(x: SessionExtensionsDisableParams) -> Any:
    return to_class(SessionExtensionsDisableParams, x)


def session_extensions_reload_result_from_dict(s: Any) -> SessionExtensionsReloadResult:
    return SessionExtensionsReloadResult.from_dict(s)


def session_extensions_reload_result_to_dict(x: SessionExtensionsReloadResult) -> Any:
    return to_class(SessionExtensionsReloadResult, x)


def session_compaction_compact_result_from_dict(s: Any) -> SessionCompactionCompactResult:
    return SessionCompactionCompactResult.from_dict(s)


def session_compaction_compact_result_to_dict(x: SessionCompactionCompactResult) -> Any:
    return to_class(SessionCompactionCompactResult, x)


def session_tools_handle_pending_tool_call_result_from_dict(s: Any) -> SessionToolsHandlePendingToolCallResult:
    return SessionToolsHandlePendingToolCallResult.from_dict(s)


def session_tools_handle_pending_tool_call_result_to_dict(x: SessionToolsHandlePendingToolCallResult) -> Any:
    return to_class(SessionToolsHandlePendingToolCallResult, x)


def session_tools_handle_pending_tool_call_params_from_dict(s: Any) -> SessionToolsHandlePendingToolCallParams:
    return SessionToolsHandlePendingToolCallParams.from_dict(s)


def session_tools_handle_pending_tool_call_params_to_dict(x: SessionToolsHandlePendingToolCallParams) -> Any:
    return to_class(SessionToolsHandlePendingToolCallParams, x)


def session_commands_handle_pending_command_result_from_dict(s: Any) -> SessionCommandsHandlePendingCommandResult:
    return SessionCommandsHandlePendingCommandResult.from_dict(s)


def session_commands_handle_pending_command_result_to_dict(x: SessionCommandsHandlePendingCommandResult) -> Any:
    return to_class(SessionCommandsHandlePendingCommandResult, x)


def session_commands_handle_pending_command_params_from_dict(s: Any) -> SessionCommandsHandlePendingCommandParams:
    return SessionCommandsHandlePendingCommandParams.from_dict(s)


def session_commands_handle_pending_command_params_to_dict(x: SessionCommandsHandlePendingCommandParams) -> Any:
    return to_class(SessionCommandsHandlePendingCommandParams, x)


def session_ui_elicitation_result_from_dict(s: Any) -> SessionUIElicitationResult:
    return SessionUIElicitationResult.from_dict(s)


def session_ui_elicitation_result_to_dict(x: SessionUIElicitationResult) -> Any:
    return to_class(SessionUIElicitationResult, x)


def session_ui_elicitation_params_from_dict(s: Any) -> SessionUIElicitationParams:
    return SessionUIElicitationParams.from_dict(s)


def session_ui_elicitation_params_to_dict(x: SessionUIElicitationParams) -> Any:
    return to_class(SessionUIElicitationParams, x)


def session_permissions_handle_pending_permission_request_result_from_dict(s: Any) -> SessionPermissionsHandlePendingPermissionRequestResult:
    return SessionPermissionsHandlePendingPermissionRequestResult.from_dict(s)


def session_permissions_handle_pending_permission_request_result_to_dict(x: SessionPermissionsHandlePendingPermissionRequestResult) -> Any:
    return to_class(SessionPermissionsHandlePendingPermissionRequestResult, x)


def session_permissions_handle_pending_permission_request_params_from_dict(s: Any) -> SessionPermissionsHandlePendingPermissionRequestParams:
    return SessionPermissionsHandlePendingPermissionRequestParams.from_dict(s)


def session_permissions_handle_pending_permission_request_params_to_dict(x: SessionPermissionsHandlePendingPermissionRequestParams) -> Any:
    return to_class(SessionPermissionsHandlePendingPermissionRequestParams, x)


def session_log_result_from_dict(s: Any) -> SessionLogResult:
    return SessionLogResult.from_dict(s)


def session_log_result_to_dict(x: SessionLogResult) -> Any:
    return to_class(SessionLogResult, x)


def session_log_params_from_dict(s: Any) -> SessionLogParams:
    return SessionLogParams.from_dict(s)


def session_log_params_to_dict(x: SessionLogParams) -> Any:
    return to_class(SessionLogParams, x)


def session_shell_exec_result_from_dict(s: Any) -> SessionShellExecResult:
    return SessionShellExecResult.from_dict(s)


def session_shell_exec_result_to_dict(x: SessionShellExecResult) -> Any:
    return to_class(SessionShellExecResult, x)


def session_shell_exec_params_from_dict(s: Any) -> SessionShellExecParams:
    return SessionShellExecParams.from_dict(s)


def session_shell_exec_params_to_dict(x: SessionShellExecParams) -> Any:
    return to_class(SessionShellExecParams, x)


def session_shell_kill_result_from_dict(s: Any) -> SessionShellKillResult:
    return SessionShellKillResult.from_dict(s)


def session_shell_kill_result_to_dict(x: SessionShellKillResult) -> Any:
    return to_class(SessionShellKillResult, x)


def session_shell_kill_params_from_dict(s: Any) -> SessionShellKillParams:
    return SessionShellKillParams.from_dict(s)


def session_shell_kill_params_to_dict(x: SessionShellKillParams) -> Any:
    return to_class(SessionShellKillParams, x)


def _timeout_kwargs(timeout: float | None) -> dict:
    """Build keyword arguments for optional timeout forwarding."""
    if timeout is not None:
        return {"timeout": timeout}
    return {}


class ServerModelsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self, *, timeout: float | None = None) -> ModelsListResult:
        return ModelsListResult.from_dict(await self._client.request("models.list", {}, **_timeout_kwargs(timeout)))


class ServerToolsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self, params: ToolsListParams, *, timeout: float | None = None) -> ToolsListResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ToolsListResult.from_dict(await self._client.request("tools.list", params_dict, **_timeout_kwargs(timeout)))


class ServerAccountApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def get_quota(self, *, timeout: float | None = None) -> AccountGetQuotaResult:
        return AccountGetQuotaResult.from_dict(await self._client.request("account.getQuota", {}, **_timeout_kwargs(timeout)))


class ServerRpc:
    """Typed server-scoped RPC methods."""
    def __init__(self, client: "JsonRpcClient"):
        self._client = client
        self.models = ServerModelsApi(client)
        self.tools = ServerToolsApi(client)
        self.account = ServerAccountApi(client)

    async def ping(self, params: PingParams, *, timeout: float | None = None) -> PingResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return PingResult.from_dict(await self._client.request("ping", params_dict, **_timeout_kwargs(timeout)))


class ModelApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_current(self, *, timeout: float | None = None) -> SessionModelGetCurrentResult:
        return SessionModelGetCurrentResult.from_dict(await self._client.request("session.model.getCurrent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def switch_to(self, params: SessionModelSwitchToParams, *, timeout: float | None = None) -> SessionModelSwitchToResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionModelSwitchToResult.from_dict(await self._client.request("session.model.switchTo", params_dict, **_timeout_kwargs(timeout)))


class ModeApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get(self, *, timeout: float | None = None) -> SessionModeGetResult:
        return SessionModeGetResult.from_dict(await self._client.request("session.mode.get", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def set(self, params: SessionModeSetParams, *, timeout: float | None = None) -> SessionModeSetResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionModeSetResult.from_dict(await self._client.request("session.mode.set", params_dict, **_timeout_kwargs(timeout)))


class PlanApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def read(self, *, timeout: float | None = None) -> SessionPlanReadResult:
        return SessionPlanReadResult.from_dict(await self._client.request("session.plan.read", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def update(self, params: SessionPlanUpdateParams, *, timeout: float | None = None) -> SessionPlanUpdateResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionPlanUpdateResult.from_dict(await self._client.request("session.plan.update", params_dict, **_timeout_kwargs(timeout)))

    async def delete(self, *, timeout: float | None = None) -> SessionPlanDeleteResult:
        return SessionPlanDeleteResult.from_dict(await self._client.request("session.plan.delete", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


class WorkspaceApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list_files(self, *, timeout: float | None = None) -> SessionWorkspaceListFilesResult:
        return SessionWorkspaceListFilesResult.from_dict(await self._client.request("session.workspace.listFiles", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def read_file(self, params: SessionWorkspaceReadFileParams, *, timeout: float | None = None) -> SessionWorkspaceReadFileResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionWorkspaceReadFileResult.from_dict(await self._client.request("session.workspace.readFile", params_dict, **_timeout_kwargs(timeout)))

    async def create_file(self, params: SessionWorkspaceCreateFileParams, *, timeout: float | None = None) -> SessionWorkspaceCreateFileResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionWorkspaceCreateFileResult.from_dict(await self._client.request("session.workspace.createFile", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class FleetApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def start(self, params: SessionFleetStartParams, *, timeout: float | None = None) -> SessionFleetStartResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionFleetStartResult.from_dict(await self._client.request("session.fleet.start", params_dict, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class AgentApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SessionAgentListResult:
        return SessionAgentListResult.from_dict(await self._client.request("session.agent.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def get_current(self, *, timeout: float | None = None) -> SessionAgentGetCurrentResult:
        return SessionAgentGetCurrentResult.from_dict(await self._client.request("session.agent.getCurrent", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def select(self, params: SessionAgentSelectParams, *, timeout: float | None = None) -> SessionAgentSelectResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionAgentSelectResult.from_dict(await self._client.request("session.agent.select", params_dict, **_timeout_kwargs(timeout)))

    async def deselect(self, *, timeout: float | None = None) -> SessionAgentDeselectResult:
        return SessionAgentDeselectResult.from_dict(await self._client.request("session.agent.deselect", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def reload(self, *, timeout: float | None = None) -> SessionAgentReloadResult:
        return SessionAgentReloadResult.from_dict(await self._client.request("session.agent.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class SkillsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SessionSkillsListResult:
        return SessionSkillsListResult.from_dict(await self._client.request("session.skills.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: SessionSkillsEnableParams, *, timeout: float | None = None) -> SessionSkillsEnableResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionSkillsEnableResult.from_dict(await self._client.request("session.skills.enable", params_dict, **_timeout_kwargs(timeout)))

    async def disable(self, params: SessionSkillsDisableParams, *, timeout: float | None = None) -> SessionSkillsDisableResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionSkillsDisableResult.from_dict(await self._client.request("session.skills.disable", params_dict, **_timeout_kwargs(timeout)))

    async def reload(self, *, timeout: float | None = None) -> SessionSkillsReloadResult:
        return SessionSkillsReloadResult.from_dict(await self._client.request("session.skills.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class McpApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SessionMCPListResult:
        return SessionMCPListResult.from_dict(await self._client.request("session.mcp.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: SessionMCPEnableParams, *, timeout: float | None = None) -> SessionMCPEnableResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionMCPEnableResult.from_dict(await self._client.request("session.mcp.enable", params_dict, **_timeout_kwargs(timeout)))

    async def disable(self, params: SessionMCPDisableParams, *, timeout: float | None = None) -> SessionMCPDisableResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionMCPDisableResult.from_dict(await self._client.request("session.mcp.disable", params_dict, **_timeout_kwargs(timeout)))

    async def reload(self, *, timeout: float | None = None) -> SessionMCPReloadResult:
        return SessionMCPReloadResult.from_dict(await self._client.request("session.mcp.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class PluginsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SessionPluginsListResult:
        return SessionPluginsListResult.from_dict(await self._client.request("session.plugins.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class ExtensionsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self, *, timeout: float | None = None) -> SessionExtensionsListResult:
        return SessionExtensionsListResult.from_dict(await self._client.request("session.extensions.list", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def enable(self, params: SessionExtensionsEnableParams, *, timeout: float | None = None) -> SessionExtensionsEnableResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionExtensionsEnableResult.from_dict(await self._client.request("session.extensions.enable", params_dict, **_timeout_kwargs(timeout)))

    async def disable(self, params: SessionExtensionsDisableParams, *, timeout: float | None = None) -> SessionExtensionsDisableResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionExtensionsDisableResult.from_dict(await self._client.request("session.extensions.disable", params_dict, **_timeout_kwargs(timeout)))

    async def reload(self, *, timeout: float | None = None) -> SessionExtensionsReloadResult:
        return SessionExtensionsReloadResult.from_dict(await self._client.request("session.extensions.reload", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


# Experimental: this API group is experimental and may change or be removed.
class CompactionApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def compact(self, *, timeout: float | None = None) -> SessionCompactionCompactResult:
        return SessionCompactionCompactResult.from_dict(await self._client.request("session.compaction.compact", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))


class ToolsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_tool_call(self, params: SessionToolsHandlePendingToolCallParams, *, timeout: float | None = None) -> SessionToolsHandlePendingToolCallResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionToolsHandlePendingToolCallResult.from_dict(await self._client.request("session.tools.handlePendingToolCall", params_dict, **_timeout_kwargs(timeout)))


class CommandsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_command(self, params: SessionCommandsHandlePendingCommandParams, *, timeout: float | None = None) -> SessionCommandsHandlePendingCommandResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionCommandsHandlePendingCommandResult.from_dict(await self._client.request("session.commands.handlePendingCommand", params_dict, **_timeout_kwargs(timeout)))


class UiApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def elicitation(self, params: SessionUIElicitationParams, *, timeout: float | None = None) -> SessionUIElicitationResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionUIElicitationResult.from_dict(await self._client.request("session.ui.elicitation", params_dict, **_timeout_kwargs(timeout)))


class PermissionsApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def handle_pending_permission_request(self, params: SessionPermissionsHandlePendingPermissionRequestParams, *, timeout: float | None = None) -> SessionPermissionsHandlePendingPermissionRequestResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionPermissionsHandlePendingPermissionRequestResult.from_dict(await self._client.request("session.permissions.handlePendingPermissionRequest", params_dict, **_timeout_kwargs(timeout)))


class ShellApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def exec(self, params: SessionShellExecParams, *, timeout: float | None = None) -> SessionShellExecResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionShellExecResult.from_dict(await self._client.request("session.shell.exec", params_dict, **_timeout_kwargs(timeout)))

    async def kill(self, params: SessionShellKillParams, *, timeout: float | None = None) -> SessionShellKillResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionShellKillResult.from_dict(await self._client.request("session.shell.kill", params_dict, **_timeout_kwargs(timeout)))


class SessionRpc:
    """Typed session-scoped RPC methods."""
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id
        self.model = ModelApi(client, session_id)
        self.mode = ModeApi(client, session_id)
        self.plan = PlanApi(client, session_id)
        self.workspace = WorkspaceApi(client, session_id)
        self.fleet = FleetApi(client, session_id)
        self.agent = AgentApi(client, session_id)
        self.skills = SkillsApi(client, session_id)
        self.mcp = McpApi(client, session_id)
        self.plugins = PluginsApi(client, session_id)
        self.extensions = ExtensionsApi(client, session_id)
        self.compaction = CompactionApi(client, session_id)
        self.tools = ToolsApi(client, session_id)
        self.commands = CommandsApi(client, session_id)
        self.ui = UiApi(client, session_id)
        self.permissions = PermissionsApi(client, session_id)
        self.shell = ShellApi(client, session_id)

    async def log(self, params: SessionLogParams, *, timeout: float | None = None) -> SessionLogResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionLogResult.from_dict(await self._client.request("session.log", params_dict, **_timeout_kwargs(timeout)))

