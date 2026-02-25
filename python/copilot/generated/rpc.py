"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..jsonrpc import JsonRpcClient


from dataclasses import dataclass
from typing import Any, Optional, List, Dict, TypeVar, Type, cast, Callable
from enum import Enum


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


def to_class(c: Type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()


def from_list(f: Callable[[Any], T], x: Any) -> List[T]:
    assert isinstance(x, list)
    return [f(y) for y in x]


def from_dict(f: Callable[[Any], T], x: Any) -> Dict[str, T]:
    assert isinstance(x, dict)
    return { k: f(v) for (k, v) in x.items() }


def to_enum(c: Type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, c)
    return x.value


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
    message: Optional[str] = None
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
    max_context_window_tokens: float
    max_output_tokens: Optional[float] = None
    max_prompt_tokens: Optional[float] = None

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
    reasoning_effort: bool
    """Whether this model supports reasoning effort configuration"""

    vision: bool

    @staticmethod
    def from_dict(obj: Any) -> 'Supports':
        assert isinstance(obj, dict)
        reasoning_effort = from_bool(obj.get("reasoningEffort"))
        vision = from_bool(obj.get("vision"))
        return Supports(reasoning_effort, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        result["reasoningEffort"] = from_bool(self.reasoning_effort)
        result["vision"] = from_bool(self.vision)
        return result


@dataclass
class Capabilities:
    """Model capabilities and limits"""

    limits: Limits
    supports: Supports

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
    terms: str

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

    billing: Optional[Billing] = None
    """Billing information"""

    default_reasoning_effort: Optional[str] = None
    """Default reasoning effort level (only present if model supports reasoning effort)"""

    policy: Optional[Policy] = None
    """Policy state (if applicable)"""

    supported_reasoning_efforts: Optional[List[str]] = None
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
    models: List[Model]
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

    instructions: Optional[str] = None
    """Optional instructions for how to use this tool effectively"""

    namespaced_name: Optional[str] = None
    """Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP
    tools)
    """
    parameters: Optional[Dict[str, Any]] = None
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
    tools: List[Tool]
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
    model: Optional[str] = None
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

    reset_date: Optional[str] = None
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
    quota_snapshots: Dict[str, QuotaSnapshot]
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
    model_id: Optional[str] = None

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
    model_id: Optional[str] = None

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

    @staticmethod
    def from_dict(obj: Any) -> 'SessionModelSwitchToParams':
        assert isinstance(obj, dict)
        model_id = from_str(obj.get("modelId"))
        return SessionModelSwitchToParams(model_id)

    def to_dict(self) -> dict:
        result: dict = {}
        result["modelId"] = from_str(self.model_id)
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
    """Whether plan.md exists in the workspace"""

    content: Optional[str] = None
    """The content of plan.md, or null if it does not exist"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionPlanReadResult':
        assert isinstance(obj, dict)
        exists = from_bool(obj.get("exists"))
        content = from_union([from_none, from_str], obj.get("content"))
        return SessionPlanReadResult(exists, content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["exists"] = from_bool(self.exists)
        result["content"] = from_union([from_none, from_str], self.content)
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
    """The new content for plan.md"""

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
    files: List[str]
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


@dataclass
class SessionFleetStartParams:
    prompt: Optional[str] = None
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
class AgentElement:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentElement':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return AgentElement(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result


@dataclass
class SessionAgentListResult:
    agents: List[AgentElement]
    """Available custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionAgentListResult':
        assert isinstance(obj, dict)
        agents = from_list(AgentElement.from_dict, obj.get("agents"))
        return SessionAgentListResult(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(AgentElement, x), self.agents)
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


@dataclass
class SessionAgentGetCurrentResult:
    agent: Optional[SessionAgentGetCurrentResultAgent] = None
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


def session_compaction_compact_result_from_dict(s: Any) -> SessionCompactionCompactResult:
    return SessionCompactionCompactResult.from_dict(s)


def session_compaction_compact_result_to_dict(x: SessionCompactionCompactResult) -> Any:
    return to_class(SessionCompactionCompactResult, x)


class ModelsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self) -> ModelsListResult:
        return ModelsListResult.from_dict(await self._client.request("models.list", {}))


class ToolsApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def list(self, params: ToolsListParams) -> ToolsListResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return ToolsListResult.from_dict(await self._client.request("tools.list", params_dict))


class AccountApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def get_quota(self) -> AccountGetQuotaResult:
        return AccountGetQuotaResult.from_dict(await self._client.request("account.getQuota", {}))


class ServerRpc:
    """Typed server-scoped RPC methods."""
    def __init__(self, client: "JsonRpcClient"):
        self._client = client
        self.models = ModelsApi(client)
        self.tools = ToolsApi(client)
        self.account = AccountApi(client)

    async def ping(self, params: PingParams) -> PingResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return PingResult.from_dict(await self._client.request("ping", params_dict))


class ModelApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get_current(self) -> SessionModelGetCurrentResult:
        return SessionModelGetCurrentResult.from_dict(await self._client.request("session.model.getCurrent", {"sessionId": self._session_id}))

    async def switch_to(self, params: SessionModelSwitchToParams) -> SessionModelSwitchToResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionModelSwitchToResult.from_dict(await self._client.request("session.model.switchTo", params_dict))


class ModeApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def get(self) -> SessionModeGetResult:
        return SessionModeGetResult.from_dict(await self._client.request("session.mode.get", {"sessionId": self._session_id}))

    async def set(self, params: SessionModeSetParams) -> SessionModeSetResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionModeSetResult.from_dict(await self._client.request("session.mode.set", params_dict))


class PlanApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def read(self) -> SessionPlanReadResult:
        return SessionPlanReadResult.from_dict(await self._client.request("session.plan.read", {"sessionId": self._session_id}))

    async def update(self, params: SessionPlanUpdateParams) -> SessionPlanUpdateResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionPlanUpdateResult.from_dict(await self._client.request("session.plan.update", params_dict))

    async def delete(self) -> SessionPlanDeleteResult:
        return SessionPlanDeleteResult.from_dict(await self._client.request("session.plan.delete", {"sessionId": self._session_id}))


class WorkspaceApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list_files(self) -> SessionWorkspaceListFilesResult:
        return SessionWorkspaceListFilesResult.from_dict(await self._client.request("session.workspace.listFiles", {"sessionId": self._session_id}))

    async def read_file(self, params: SessionWorkspaceReadFileParams) -> SessionWorkspaceReadFileResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionWorkspaceReadFileResult.from_dict(await self._client.request("session.workspace.readFile", params_dict))

    async def create_file(self, params: SessionWorkspaceCreateFileParams) -> SessionWorkspaceCreateFileResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionWorkspaceCreateFileResult.from_dict(await self._client.request("session.workspace.createFile", params_dict))


class FleetApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def start(self, params: SessionFleetStartParams) -> SessionFleetStartResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionFleetStartResult.from_dict(await self._client.request("session.fleet.start", params_dict))


class AgentApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list(self) -> SessionAgentListResult:
        return SessionAgentListResult.from_dict(await self._client.request("session.agent.list", {"sessionId": self._session_id}))

    async def get_current(self) -> SessionAgentGetCurrentResult:
        return SessionAgentGetCurrentResult.from_dict(await self._client.request("session.agent.getCurrent", {"sessionId": self._session_id}))

    async def select(self, params: SessionAgentSelectParams) -> SessionAgentSelectResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return SessionAgentSelectResult.from_dict(await self._client.request("session.agent.select", params_dict))

    async def deselect(self) -> SessionAgentDeselectResult:
        return SessionAgentDeselectResult.from_dict(await self._client.request("session.agent.deselect", {"sessionId": self._session_id}))


class CompactionApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def compact(self) -> SessionCompactionCompactResult:
        return SessionCompactionCompactResult.from_dict(await self._client.request("session.compaction.compact", {"sessionId": self._session_id}))


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
        self.compaction = CompactionApi(client, session_id)

