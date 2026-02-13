"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..jsonrpc import JsonRpcClient


from dataclasses import dataclass
from typing import Any, Optional, List, Dict, TypeVar, Type, cast, Callable


T = TypeVar("T")


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


class SessionRpc:
    """Typed session-scoped RPC methods."""
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id
        self.model = ModelApi(client, session_id)

