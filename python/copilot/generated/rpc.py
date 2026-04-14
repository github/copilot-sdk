"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from .._jsonrpc import JsonRpcClient

from collections.abc import Callable
from dataclasses import dataclass
from typing import Protocol


from dataclasses import dataclass
from typing import Any, TypeVar, Callable, cast
from datetime import datetime
from enum import Enum
from uuid import UUID
import dateutil.parser

T = TypeVar("T")
EnumT = TypeVar("EnumT", bound=Enum)

def from_str(x: Any) -> str:
    assert isinstance(x, str)
    return x

def from_int(x: Any) -> int:
    assert isinstance(x, int) and not isinstance(x, bool)
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

def from_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)

def to_float(x: Any) -> float:
    assert isinstance(x, (int, float))
    return x

def from_list(f: Callable[[Any], T], x: Any) -> list[T]:
    assert isinstance(x, list)
    return [f(y) for y in x]

def to_class(c: type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()

def from_bool(x: Any) -> bool:
    assert isinstance(x, bool)
    return x

def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:
    assert isinstance(x, dict)
    return { k: f(v) for (k, v) in x.items() }

def from_datetime(x: Any) -> datetime:
    return dateutil.parser.parse(x)

def to_enum(c: type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, c)
    return x.value

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
class ModelCapabilitiesLimits:
    """Token limits for prompts, outputs, and context window"""

    max_context_window_tokens: int
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
        max_context_window_tokens = from_int(obj.get("max_context_window_tokens"))
        max_output_tokens = from_union([from_int, from_none], obj.get("max_output_tokens"))
        max_prompt_tokens = from_union([from_int, from_none], obj.get("max_prompt_tokens"))
        vision = from_union([ModelCapabilitiesLimitsVision.from_dict, from_none], obj.get("vision"))
        return ModelCapabilitiesLimits(max_context_window_tokens, max_output_tokens, max_prompt_tokens, vision)

    def to_dict(self) -> dict:
        result: dict = {}
        result["max_context_window_tokens"] = from_int(self.max_context_window_tokens)
        if self.max_output_tokens is not None:
            result["max_output_tokens"] = from_union([from_int, from_none], self.max_output_tokens)
        if self.max_prompt_tokens is not None:
            result["max_prompt_tokens"] = from_union([from_int, from_none], self.max_prompt_tokens)
        if self.vision is not None:
            result["vision"] = from_union([lambda x: to_class(ModelCapabilitiesLimitsVision, x), from_none], self.vision)
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
class ModelCapabilities:
    """Model capabilities and limits"""

    limits: ModelCapabilitiesLimits
    """Token limits for prompts, outputs, and context window"""

    supports: ModelCapabilitiesSupports
    """Feature flags indicating what the model supports"""

    @staticmethod
    def from_dict(obj: Any) -> 'ModelCapabilities':
        assert isinstance(obj, dict)
        limits = ModelCapabilitiesLimits.from_dict(obj.get("limits"))
        supports = ModelCapabilitiesSupports.from_dict(obj.get("supports"))
        return ModelCapabilities(limits, supports)

    def to_dict(self) -> dict:
        result: dict = {}
        result["limits"] = to_class(ModelCapabilitiesLimits, self.limits)
        result["supports"] = to_class(ModelCapabilitiesSupports, self.supports)
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
class AccountQuotaSnapshot:
    entitlement_requests: int
    """Number of requests included in the entitlement"""

    overage: int
    """Number of overage requests made this period"""

    overage_allowed_with_exhausted_quota: bool
    """Whether pay-per-request usage is allowed when quota is exhausted"""

    remaining_percentage: float
    """Percentage of entitlement remaining"""

    used_requests: int
    """Number of requests used so far this period"""

    reset_date: datetime | None = None
    """Date when the quota resets (ISO 8601)"""

    @staticmethod
    def from_dict(obj: Any) -> 'AccountQuotaSnapshot':
        assert isinstance(obj, dict)
        entitlement_requests = from_int(obj.get("entitlementRequests"))
        overage = from_int(obj.get("overage"))
        overage_allowed_with_exhausted_quota = from_bool(obj.get("overageAllowedWithExhaustedQuota"))
        remaining_percentage = from_float(obj.get("remainingPercentage"))
        used_requests = from_int(obj.get("usedRequests"))
        reset_date = from_union([from_datetime, from_none], obj.get("resetDate"))
        return AccountQuotaSnapshot(entitlement_requests, overage, overage_allowed_with_exhausted_quota, remaining_percentage, used_requests, reset_date)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entitlementRequests"] = from_int(self.entitlement_requests)
        result["overage"] = from_int(self.overage)
        result["overageAllowedWithExhaustedQuota"] = from_bool(self.overage_allowed_with_exhausted_quota)
        result["remainingPercentage"] = to_float(self.remaining_percentage)
        result["usedRequests"] = from_int(self.used_requests)
        if self.reset_date is not None:
            result["resetDate"] = from_union([lambda x: x.isoformat(), from_none], self.reset_date)
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

class MCPConfigFilterMappingString(Enum):
    HIDDEN_CHARACTERS = "hidden_characters"
    MARKDOWN = "markdown"
    NONE = "none"

class MCPConfigType(Enum):
    HTTP = "http"
    LOCAL = "local"
    SSE = "sse"
    STDIO = "stdio"

@dataclass
class MCPConfigServer:
    """MCP server configuration (local/stdio or remote/http)"""

    args: list[str] | None = None
    command: str | None = None
    cwd: str | None = None
    env: dict[str, str] | None = None
    filter_mapping: dict[str, MCPConfigFilterMappingString] | MCPConfigFilterMappingString | None = None
    is_default_server: bool | None = None
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPConfigType | None = None
    headers: dict[str, str] | None = None
    oauth_client_id: str | None = None
    oauth_public_client: bool | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigServer':
        assert isinstance(obj, dict)
        args = from_union([lambda x: from_list(from_str, x), from_none], obj.get("args"))
        command = from_union([from_str, from_none], obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(MCPConfigFilterMappingString, x), MCPConfigFilterMappingString, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPConfigType, from_none], obj.get("type"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        url = from_union([from_str, from_none], obj.get("url"))
        return MCPConfigServer(args, command, cwd, env, filter_mapping, is_default_server, timeout, tools, type, headers, oauth_client_id, oauth_public_client, url)

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
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(MCPConfigFilterMappingString, x), x), lambda x: to_enum(MCPConfigFilterMappingString, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPConfigType, x), from_none], self.type)
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
class MCPConfigList:
    servers: dict[str, MCPConfigServer]
    """All MCP servers from user config, keyed by name"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigList':
        assert isinstance(obj, dict)
        servers = from_dict(MCPConfigServer.from_dict, obj.get("servers"))
        return MCPConfigList(servers)

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_dict(lambda x: to_class(MCPConfigServer, x), self.servers)
        return result

@dataclass
class MCPConfigAddConfig:
    """MCP server configuration (local/stdio or remote/http)"""

    args: list[str] | None = None
    command: str | None = None
    cwd: str | None = None
    env: dict[str, str] | None = None
    filter_mapping: dict[str, MCPConfigFilterMappingString] | MCPConfigFilterMappingString | None = None
    is_default_server: bool | None = None
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPConfigType | None = None
    headers: dict[str, str] | None = None
    oauth_client_id: str | None = None
    oauth_public_client: bool | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigAddConfig':
        assert isinstance(obj, dict)
        args = from_union([lambda x: from_list(from_str, x), from_none], obj.get("args"))
        command = from_union([from_str, from_none], obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(MCPConfigFilterMappingString, x), MCPConfigFilterMappingString, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPConfigType, from_none], obj.get("type"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        url = from_union([from_str, from_none], obj.get("url"))
        return MCPConfigAddConfig(args, command, cwd, env, filter_mapping, is_default_server, timeout, tools, type, headers, oauth_client_id, oauth_public_client, url)

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
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(MCPConfigFilterMappingString, x), x), lambda x: to_enum(MCPConfigFilterMappingString, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPConfigType, x), from_none], self.type)
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
class MCPConfigAddRequest:
    config: MCPConfigAddConfig
    """MCP server configuration (local/stdio or remote/http)"""

    name: str
    """Unique name for the MCP server"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigAddRequest':
        assert isinstance(obj, dict)
        config = MCPConfigAddConfig.from_dict(obj.get("config"))
        name = from_str(obj.get("name"))
        return MCPConfigAddRequest(config, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["config"] = to_class(MCPConfigAddConfig, self.config)
        result["name"] = from_str(self.name)
        return result

@dataclass
class MCPConfigUpdateConfig:
    """MCP server configuration (local/stdio or remote/http)"""

    args: list[str] | None = None
    command: str | None = None
    cwd: str | None = None
    env: dict[str, str] | None = None
    filter_mapping: dict[str, MCPConfigFilterMappingString] | MCPConfigFilterMappingString | None = None
    is_default_server: bool | None = None
    timeout: int | None = None
    """Timeout in milliseconds for tool calls to this server."""

    tools: list[str] | None = None
    """Tools to include. Defaults to all tools if not specified."""

    type: MCPConfigType | None = None
    headers: dict[str, str] | None = None
    oauth_client_id: str | None = None
    oauth_public_client: bool | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigUpdateConfig':
        assert isinstance(obj, dict)
        args = from_union([lambda x: from_list(from_str, x), from_none], obj.get("args"))
        command = from_union([from_str, from_none], obj.get("command"))
        cwd = from_union([from_str, from_none], obj.get("cwd"))
        env = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("env"))
        filter_mapping = from_union([lambda x: from_dict(MCPConfigFilterMappingString, x), MCPConfigFilterMappingString, from_none], obj.get("filterMapping"))
        is_default_server = from_union([from_bool, from_none], obj.get("isDefaultServer"))
        timeout = from_union([from_int, from_none], obj.get("timeout"))
        tools = from_union([lambda x: from_list(from_str, x), from_none], obj.get("tools"))
        type = from_union([MCPConfigType, from_none], obj.get("type"))
        headers = from_union([lambda x: from_dict(from_str, x), from_none], obj.get("headers"))
        oauth_client_id = from_union([from_str, from_none], obj.get("oauthClientId"))
        oauth_public_client = from_union([from_bool, from_none], obj.get("oauthPublicClient"))
        url = from_union([from_str, from_none], obj.get("url"))
        return MCPConfigUpdateConfig(args, command, cwd, env, filter_mapping, is_default_server, timeout, tools, type, headers, oauth_client_id, oauth_public_client, url)

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
            result["filterMapping"] = from_union([lambda x: from_dict(lambda x: to_enum(MCPConfigFilterMappingString, x), x), lambda x: to_enum(MCPConfigFilterMappingString, x), from_none], self.filter_mapping)
        if self.is_default_server is not None:
            result["isDefaultServer"] = from_union([from_bool, from_none], self.is_default_server)
        if self.timeout is not None:
            result["timeout"] = from_union([from_int, from_none], self.timeout)
        if self.tools is not None:
            result["tools"] = from_union([lambda x: from_list(from_str, x), from_none], self.tools)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(MCPConfigType, x), from_none], self.type)
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
class MCPConfigUpdateRequest:
    config: MCPConfigUpdateConfig
    """MCP server configuration (local/stdio or remote/http)"""

    name: str
    """Name of the MCP server to update"""

    @staticmethod
    def from_dict(obj: Any) -> 'MCPConfigUpdateRequest':
        assert isinstance(obj, dict)
        config = MCPConfigUpdateConfig.from_dict(obj.get("config"))
        name = from_str(obj.get("name"))
        return MCPConfigUpdateRequest(config, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["config"] = to_class(MCPConfigUpdateConfig, self.config)
        result["name"] = from_str(self.name)
        return result

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

class SessionFSSetProviderConventions(Enum):
    """Path conventions used by this filesystem"""

    POSIX = "posix"
    WINDOWS = "windows"

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

class SessionMode(Enum):
    """The agent mode. Valid values: "interactive", "plan", "autopilot"."""

    AUTOPILOT = "autopilot"
    INTERACTIVE = "interactive"
    PLAN = "plan"

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
class WorkspaceListFilesResult:
    files: list[str]
    """Relative file paths in the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspaceListFilesResult':
        assert isinstance(obj, dict)
        files = from_list(from_str, obj.get("files"))
        return WorkspaceListFilesResult(files)

    def to_dict(self) -> dict:
        result: dict = {}
        result["files"] = from_list(from_str, self.files)
        return result

@dataclass
class WorkspaceReadFileResult:
    content: str
    """File content as a UTF-8 string"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspaceReadFileResult':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return WorkspaceReadFileResult(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        return result

@dataclass
class WorkspaceReadFileRequest:
    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspaceReadFileRequest':
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        return WorkspaceReadFileRequest(path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        return result

@dataclass
class WorkspaceCreateFileRequest:
    content: str
    """File content to write as a UTF-8 string"""

    path: str
    """Relative path within the workspace files directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'WorkspaceCreateFileRequest':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        path = from_str(obj.get("path"))
        return WorkspaceCreateFileRequest(content, path)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["path"] = from_str(self.path)
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

@dataclass
class Agent:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'Agent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return Agent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentList:
    agents: list[Agent]
    """Available custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentList':
        assert isinstance(obj, dict)
        agents = from_list(Agent.from_dict, obj.get("agents"))
        return AgentList(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(Agent, x), self.agents)
        return result

@dataclass
class AgentGetCurrentResultAgent:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentGetCurrentResultAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return AgentGetCurrentResultAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentGetCurrentResult:
    agent: AgentGetCurrentResultAgent | None = None
    """Currently selected custom agent, or null if using the default agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentGetCurrentResult':
        assert isinstance(obj, dict)
        agent = from_union([AgentGetCurrentResultAgent.from_dict, from_none], obj.get("agent"))
        return AgentGetCurrentResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agent"] = from_union([lambda x: to_class(AgentGetCurrentResultAgent, x), from_none], self.agent)
        return result

@dataclass
class AgentSelectAgent:
    """The newly selected custom agent"""

    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentSelectAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return AgentSelectAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentSelectResult:
    agent: AgentSelectAgent
    """The newly selected custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentSelectResult':
        assert isinstance(obj, dict)
        agent = AgentSelectAgent.from_dict(obj.get("agent"))
        return AgentSelectResult(agent)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agent"] = to_class(AgentSelectAgent, self.agent)
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
class AgentReloadAgent:
    description: str
    """Description of the agent's purpose"""

    display_name: str
    """Human-readable display name"""

    name: str
    """Unique identifier of the custom agent"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentReloadAgent':
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        name = from_str(obj.get("name"))
        return AgentReloadAgent(description, display_name, name)

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["name"] = from_str(self.name)
        return result

# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AgentReloadResult:
    agents: list[AgentReloadAgent]
    """Reloaded custom agents"""

    @staticmethod
    def from_dict(obj: Any) -> 'AgentReloadResult':
        assert isinstance(obj, dict)
        agents = from_list(AgentReloadAgent.from_dict, obj.get("agents"))
        return AgentReloadResult(agents)

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(AgentReloadAgent, x), self.agents)
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

class MCPServerStatus(Enum):
    """Connection status: connected, failed, needs-auth, pending, disabled, or not_configured"""

    CONNECTED = "connected"
    DISABLED = "disabled"
    FAILED = "failed"
    NEEDS_AUTH = "needs-auth"
    NOT_CONFIGURED = "not_configured"
    PENDING = "pending"

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

class UIElicitationResponseAction(Enum):
    """The user's response: accept (submitted), decline (rejected), or cancel (dismissed)"""

    ACCEPT = "accept"
    CANCEL = "cancel"
    DECLINE = "decline"

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

class UIElicitationSchemaPropertyStringFormat(Enum):
    DATE = "date"
    DATE_TIME = "date-time"
    EMAIL = "email"
    URI = "uri"

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

class ItemsType(Enum):
    STRING = "string"

@dataclass
class UIElicitationArrayFieldItems:
    enum: list[str] | None = None
    type: ItemsType | None = None
    any_of: list[UIElicitationArrayAnyOfFieldItemsAnyOf] | None = None

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationArrayFieldItems':
        assert isinstance(obj, dict)
        enum = from_union([lambda x: from_list(from_str, x), from_none], obj.get("enum"))
        type = from_union([ItemsType, from_none], obj.get("type"))
        any_of = from_union([lambda x: from_list(UIElicitationArrayAnyOfFieldItemsAnyOf.from_dict, x), from_none], obj.get("anyOf"))
        return UIElicitationArrayFieldItems(enum, type, any_of)

    def to_dict(self) -> dict:
        result: dict = {}
        if self.enum is not None:
            result["enum"] = from_union([lambda x: from_list(from_str, x), from_none], self.enum)
        if self.type is not None:
            result["type"] = from_union([lambda x: to_enum(ItemsType, x), from_none], self.type)
        if self.any_of is not None:
            result["anyOf"] = from_union([lambda x: from_list(lambda x: to_class(UIElicitationArrayAnyOfFieldItemsAnyOf, x), x), from_none], self.any_of)
        return result

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

class UIElicitationSchemaPropertyNumberType(Enum):
    ARRAY = "array"
    BOOLEAN = "boolean"
    INTEGER = "integer"
    NUMBER = "number"
    STRING = "string"

@dataclass
class UIElicitationSchemaProperty:
    type: UIElicitationSchemaPropertyNumberType
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
        type = UIElicitationSchemaPropertyNumberType(obj.get("type"))
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
        result["type"] = to_enum(UIElicitationSchemaPropertyNumberType, self.type)
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

class RequestedSchemaType(Enum):
    OBJECT = "object"

@dataclass
class UIElicitationSchema:
    """JSON Schema describing the form fields to present to the user"""

    properties: dict[str, UIElicitationSchemaProperty]
    """Form field definitions, keyed by field name"""

    type: RequestedSchemaType
    """Schema type indicator (always 'object')"""

    required: list[str] | None = None
    """List of required field names"""

    @staticmethod
    def from_dict(obj: Any) -> 'UIElicitationSchema':
        assert isinstance(obj, dict)
        properties = from_dict(UIElicitationSchemaProperty.from_dict, obj.get("properties"))
        type = RequestedSchemaType(obj.get("type"))
        required = from_union([lambda x: from_list(from_str, x), from_none], obj.get("required"))
        return UIElicitationSchema(properties, type, required)

    def to_dict(self) -> dict:
        result: dict = {}
        result["properties"] = from_dict(lambda x: to_class(UIElicitationSchemaProperty, x), self.properties)
        result["type"] = to_enum(RequestedSchemaType, self.type)
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

class Kind(Enum):
    APPROVED = "approved"
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"
    DENIED_BY_PERMISSION_REQUEST_HOOK = "denied-by-permission-request-hook"
    DENIED_BY_RULES = "denied-by-rules"
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"

@dataclass
class PermissionDecision:
    kind: Kind
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
        kind = Kind(obj.get("kind"))
        rules = from_union([lambda x: from_list(lambda x: x, x), from_none], obj.get("rules"))
        feedback = from_union([from_str, from_none], obj.get("feedback"))
        message = from_union([from_str, from_none], obj.get("message"))
        path = from_union([from_str, from_none], obj.get("path"))
        interrupt = from_union([from_bool, from_none], obj.get("interrupt"))
        return PermissionDecision(kind, rules, feedback, message, path, interrupt)

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
        if self.interrupt is not None:
            result["interrupt"] = from_union([from_bool, from_none], self.interrupt)
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

class SessionLogLevel(Enum):
    """Log severity level. Determines how the message is displayed in the timeline. Defaults to
    "info".
    """
    ERROR = "error"
    INFO = "info"
    WARNING = "warning"

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

class ShellKillSignal(Enum):
    """Signal to send (default: SIGTERM)"""

    SIGINT = "SIGINT"
    SIGKILL = "SIGKILL"
    SIGTERM = "SIGTERM"

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
class SessionFSReadFileResult:
    content: str
    """File content as UTF-8 string"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReadFileResult':
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        return SessionFSReadFileResult(content)

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
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

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSStatResult':
        assert isinstance(obj, dict)
        birthtime = from_datetime(obj.get("birthtime"))
        is_directory = from_bool(obj.get("isDirectory"))
        is_file = from_bool(obj.get("isFile"))
        mtime = from_datetime(obj.get("mtime"))
        size = from_int(obj.get("size"))
        return SessionFSStatResult(birthtime, is_directory, is_file, mtime, size)

    def to_dict(self) -> dict:
        result: dict = {}
        result["birthtime"] = self.birthtime.isoformat()
        result["isDirectory"] = from_bool(self.is_directory)
        result["isFile"] = from_bool(self.is_file)
        result["mtime"] = self.mtime.isoformat()
        result["size"] = from_int(self.size)
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
class SessionFSReaddirResult:
    entries: list[str]
    """Entry names in the directory"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirResult':
        assert isinstance(obj, dict)
        entries = from_list(from_str, obj.get("entries"))
        return SessionFSReaddirResult(entries)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entries"] = from_list(from_str, self.entries)
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
class SessionFSReaddirWithTypesResult:
    entries: list[SessionFSReaddirWithTypesEntry]
    """Directory entries with type information"""

    @staticmethod
    def from_dict(obj: Any) -> 'SessionFSReaddirWithTypesResult':
        assert isinstance(obj, dict)
        entries = from_list(SessionFSReaddirWithTypesEntry.from_dict, obj.get("entries"))
        return SessionFSReaddirWithTypesResult(entries)

    def to_dict(self) -> dict:
        result: dict = {}
        result["entries"] = from_list(lambda x: to_class(SessionFSReaddirWithTypesEntry, x), self.entries)
        return result

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

def ping_result_from_dict(s: Any) -> PingResult:
    return PingResult.from_dict(s)

def ping_result_to_dict(x: PingResult) -> Any:
    return to_class(PingResult, x)

def ping_request_from_dict(s: Any) -> PingRequest:
    return PingRequest.from_dict(s)

def ping_request_to_dict(x: PingRequest) -> Any:
    return to_class(PingRequest, x)

def model_list_from_dict(s: Any) -> ModelList:
    return ModelList.from_dict(s)

def model_list_to_dict(x: ModelList) -> Any:
    return to_class(ModelList, x)

def tool_list_from_dict(s: Any) -> ToolList:
    return ToolList.from_dict(s)

def tool_list_to_dict(x: ToolList) -> Any:
    return to_class(ToolList, x)

def tools_list_request_from_dict(s: Any) -> ToolsListRequest:
    return ToolsListRequest.from_dict(s)

def tools_list_request_to_dict(x: ToolsListRequest) -> Any:
    return to_class(ToolsListRequest, x)

def account_get_quota_result_from_dict(s: Any) -> AccountGetQuotaResult:
    return AccountGetQuotaResult.from_dict(s)

def account_get_quota_result_to_dict(x: AccountGetQuotaResult) -> Any:
    return to_class(AccountGetQuotaResult, x)

def mcp_config_list_from_dict(s: Any) -> MCPConfigList:
    return MCPConfigList.from_dict(s)

def mcp_config_list_to_dict(x: MCPConfigList) -> Any:
    return to_class(MCPConfigList, x)

def mcp_config_add_request_from_dict(s: Any) -> MCPConfigAddRequest:
    return MCPConfigAddRequest.from_dict(s)

def mcp_config_add_request_to_dict(x: MCPConfigAddRequest) -> Any:
    return to_class(MCPConfigAddRequest, x)

def mcp_config_update_request_from_dict(s: Any) -> MCPConfigUpdateRequest:
    return MCPConfigUpdateRequest.from_dict(s)

def mcp_config_update_request_to_dict(x: MCPConfigUpdateRequest) -> Any:
    return to_class(MCPConfigUpdateRequest, x)

def mcp_config_remove_request_from_dict(s: Any) -> MCPConfigRemoveRequest:
    return MCPConfigRemoveRequest.from_dict(s)

def mcp_config_remove_request_to_dict(x: MCPConfigRemoveRequest) -> Any:
    return to_class(MCPConfigRemoveRequest, x)

def mcp_discover_result_from_dict(s: Any) -> MCPDiscoverResult:
    return MCPDiscoverResult.from_dict(s)

def mcp_discover_result_to_dict(x: MCPDiscoverResult) -> Any:
    return to_class(MCPDiscoverResult, x)

def mcp_discover_request_from_dict(s: Any) -> MCPDiscoverRequest:
    return MCPDiscoverRequest.from_dict(s)

def mcp_discover_request_to_dict(x: MCPDiscoverRequest) -> Any:
    return to_class(MCPDiscoverRequest, x)

def session_fs_set_provider_result_from_dict(s: Any) -> SessionFSSetProviderResult:
    return SessionFSSetProviderResult.from_dict(s)

def session_fs_set_provider_result_to_dict(x: SessionFSSetProviderResult) -> Any:
    return to_class(SessionFSSetProviderResult, x)

def session_fs_set_provider_request_from_dict(s: Any) -> SessionFSSetProviderRequest:
    return SessionFSSetProviderRequest.from_dict(s)

def session_fs_set_provider_request_to_dict(x: SessionFSSetProviderRequest) -> Any:
    return to_class(SessionFSSetProviderRequest, x)

def sessions_fork_result_from_dict(s: Any) -> SessionsForkResult:
    return SessionsForkResult.from_dict(s)

def sessions_fork_result_to_dict(x: SessionsForkResult) -> Any:
    return to_class(SessionsForkResult, x)

def sessions_fork_request_from_dict(s: Any) -> SessionsForkRequest:
    return SessionsForkRequest.from_dict(s)

def sessions_fork_request_to_dict(x: SessionsForkRequest) -> Any:
    return to_class(SessionsForkRequest, x)

def current_model_from_dict(s: Any) -> CurrentModel:
    return CurrentModel.from_dict(s)

def current_model_to_dict(x: CurrentModel) -> Any:
    return to_class(CurrentModel, x)

def model_switch_to_result_from_dict(s: Any) -> ModelSwitchToResult:
    return ModelSwitchToResult.from_dict(s)

def model_switch_to_result_to_dict(x: ModelSwitchToResult) -> Any:
    return to_class(ModelSwitchToResult, x)

def model_switch_to_request_from_dict(s: Any) -> ModelSwitchToRequest:
    return ModelSwitchToRequest.from_dict(s)

def model_switch_to_request_to_dict(x: ModelSwitchToRequest) -> Any:
    return to_class(ModelSwitchToRequest, x)

def session_mode_from_dict(s: Any) -> SessionMode:
    return SessionMode(s)

def session_mode_to_dict(x: SessionMode) -> Any:
    return to_enum(SessionMode, x)

def mode_set_request_from_dict(s: Any) -> ModeSetRequest:
    return ModeSetRequest.from_dict(s)

def mode_set_request_to_dict(x: ModeSetRequest) -> Any:
    return to_class(ModeSetRequest, x)

def plan_read_result_from_dict(s: Any) -> PlanReadResult:
    return PlanReadResult.from_dict(s)

def plan_read_result_to_dict(x: PlanReadResult) -> Any:
    return to_class(PlanReadResult, x)

def plan_update_request_from_dict(s: Any) -> PlanUpdateRequest:
    return PlanUpdateRequest.from_dict(s)

def plan_update_request_to_dict(x: PlanUpdateRequest) -> Any:
    return to_class(PlanUpdateRequest, x)

def workspace_list_files_result_from_dict(s: Any) -> WorkspaceListFilesResult:
    return WorkspaceListFilesResult.from_dict(s)

def workspace_list_files_result_to_dict(x: WorkspaceListFilesResult) -> Any:
    return to_class(WorkspaceListFilesResult, x)

def workspace_read_file_result_from_dict(s: Any) -> WorkspaceReadFileResult:
    return WorkspaceReadFileResult.from_dict(s)

def workspace_read_file_result_to_dict(x: WorkspaceReadFileResult) -> Any:
    return to_class(WorkspaceReadFileResult, x)

def workspace_read_file_request_from_dict(s: Any) -> WorkspaceReadFileRequest:
    return WorkspaceReadFileRequest.from_dict(s)

def workspace_read_file_request_to_dict(x: WorkspaceReadFileRequest) -> Any:
    return to_class(WorkspaceReadFileRequest, x)

def workspace_create_file_request_from_dict(s: Any) -> WorkspaceCreateFileRequest:
    return WorkspaceCreateFileRequest.from_dict(s)

def workspace_create_file_request_to_dict(x: WorkspaceCreateFileRequest) -> Any:
    return to_class(WorkspaceCreateFileRequest, x)

def fleet_start_result_from_dict(s: Any) -> FleetStartResult:
    return FleetStartResult.from_dict(s)

def fleet_start_result_to_dict(x: FleetStartResult) -> Any:
    return to_class(FleetStartResult, x)

def fleet_start_request_from_dict(s: Any) -> FleetStartRequest:
    return FleetStartRequest.from_dict(s)

def fleet_start_request_to_dict(x: FleetStartRequest) -> Any:
    return to_class(FleetStartRequest, x)

def agent_list_from_dict(s: Any) -> AgentList:
    return AgentList.from_dict(s)

def agent_list_to_dict(x: AgentList) -> Any:
    return to_class(AgentList, x)

def agent_get_current_result_from_dict(s: Any) -> AgentGetCurrentResult:
    return AgentGetCurrentResult.from_dict(s)

def agent_get_current_result_to_dict(x: AgentGetCurrentResult) -> Any:
    return to_class(AgentGetCurrentResult, x)

def agent_select_result_from_dict(s: Any) -> AgentSelectResult:
    return AgentSelectResult.from_dict(s)

def agent_select_result_to_dict(x: AgentSelectResult) -> Any:
    return to_class(AgentSelectResult, x)

def agent_select_request_from_dict(s: Any) -> AgentSelectRequest:
    return AgentSelectRequest.from_dict(s)

def agent_select_request_to_dict(x: AgentSelectRequest) -> Any:
    return to_class(AgentSelectRequest, x)

def agent_reload_result_from_dict(s: Any) -> AgentReloadResult:
    return AgentReloadResult.from_dict(s)

def agent_reload_result_to_dict(x: AgentReloadResult) -> Any:
    return to_class(AgentReloadResult, x)

def skill_list_from_dict(s: Any) -> SkillList:
    return SkillList.from_dict(s)

def skill_list_to_dict(x: SkillList) -> Any:
    return to_class(SkillList, x)

def skills_enable_request_from_dict(s: Any) -> SkillsEnableRequest:
    return SkillsEnableRequest.from_dict(s)

def skills_enable_request_to_dict(x: SkillsEnableRequest) -> Any:
    return to_class(SkillsEnableRequest, x)

def skills_disable_request_from_dict(s: Any) -> SkillsDisableRequest:
    return SkillsDisableRequest.from_dict(s)

def skills_disable_request_to_dict(x: SkillsDisableRequest) -> Any:
    return to_class(SkillsDisableRequest, x)

def mcp_server_list_from_dict(s: Any) -> MCPServerList:
    return MCPServerList.from_dict(s)

def mcp_server_list_to_dict(x: MCPServerList) -> Any:
    return to_class(MCPServerList, x)

def mcp_enable_request_from_dict(s: Any) -> MCPEnableRequest:
    return MCPEnableRequest.from_dict(s)

def mcp_enable_request_to_dict(x: MCPEnableRequest) -> Any:
    return to_class(MCPEnableRequest, x)

def mcp_disable_request_from_dict(s: Any) -> MCPDisableRequest:
    return MCPDisableRequest.from_dict(s)

def mcp_disable_request_to_dict(x: MCPDisableRequest) -> Any:
    return to_class(MCPDisableRequest, x)

def plugin_list_from_dict(s: Any) -> PluginList:
    return PluginList.from_dict(s)

def plugin_list_to_dict(x: PluginList) -> Any:
    return to_class(PluginList, x)

def extension_list_from_dict(s: Any) -> ExtensionList:
    return ExtensionList.from_dict(s)

def extension_list_to_dict(x: ExtensionList) -> Any:
    return to_class(ExtensionList, x)

def extensions_enable_request_from_dict(s: Any) -> ExtensionsEnableRequest:
    return ExtensionsEnableRequest.from_dict(s)

def extensions_enable_request_to_dict(x: ExtensionsEnableRequest) -> Any:
    return to_class(ExtensionsEnableRequest, x)

def extensions_disable_request_from_dict(s: Any) -> ExtensionsDisableRequest:
    return ExtensionsDisableRequest.from_dict(s)

def extensions_disable_request_to_dict(x: ExtensionsDisableRequest) -> Any:
    return to_class(ExtensionsDisableRequest, x)

def handle_tool_call_result_from_dict(s: Any) -> HandleToolCallResult:
    return HandleToolCallResult.from_dict(s)

def handle_tool_call_result_to_dict(x: HandleToolCallResult) -> Any:
    return to_class(HandleToolCallResult, x)

def tools_handle_pending_tool_call_request_from_dict(s: Any) -> ToolsHandlePendingToolCallRequest:
    return ToolsHandlePendingToolCallRequest.from_dict(s)

def tools_handle_pending_tool_call_request_to_dict(x: ToolsHandlePendingToolCallRequest) -> Any:
    return to_class(ToolsHandlePendingToolCallRequest, x)

def commands_handle_pending_command_result_from_dict(s: Any) -> CommandsHandlePendingCommandResult:
    return CommandsHandlePendingCommandResult.from_dict(s)

def commands_handle_pending_command_result_to_dict(x: CommandsHandlePendingCommandResult) -> Any:
    return to_class(CommandsHandlePendingCommandResult, x)

def commands_handle_pending_command_request_from_dict(s: Any) -> CommandsHandlePendingCommandRequest:
    return CommandsHandlePendingCommandRequest.from_dict(s)

def commands_handle_pending_command_request_to_dict(x: CommandsHandlePendingCommandRequest) -> Any:
    return to_class(CommandsHandlePendingCommandRequest, x)

def ui_elicitation_response_from_dict(s: Any) -> UIElicitationResponse:
    return UIElicitationResponse.from_dict(s)

def ui_elicitation_response_to_dict(x: UIElicitationResponse) -> Any:
    return to_class(UIElicitationResponse, x)

def ui_elicitation_request_from_dict(s: Any) -> UIElicitationRequest:
    return UIElicitationRequest.from_dict(s)

def ui_elicitation_request_to_dict(x: UIElicitationRequest) -> Any:
    return to_class(UIElicitationRequest, x)

def ui_elicitation_result_from_dict(s: Any) -> UIElicitationResult:
    return UIElicitationResult.from_dict(s)

def ui_elicitation_result_to_dict(x: UIElicitationResult) -> Any:
    return to_class(UIElicitationResult, x)

def ui_handle_pending_elicitation_request_from_dict(s: Any) -> UIHandlePendingElicitationRequest:
    return UIHandlePendingElicitationRequest.from_dict(s)

def ui_handle_pending_elicitation_request_to_dict(x: UIHandlePendingElicitationRequest) -> Any:
    return to_class(UIHandlePendingElicitationRequest, x)

def permission_request_result_from_dict(s: Any) -> PermissionRequestResult:
    return PermissionRequestResult.from_dict(s)

def permission_request_result_to_dict(x: PermissionRequestResult) -> Any:
    return to_class(PermissionRequestResult, x)

def permission_decision_request_from_dict(s: Any) -> PermissionDecisionRequest:
    return PermissionDecisionRequest.from_dict(s)

def permission_decision_request_to_dict(x: PermissionDecisionRequest) -> Any:
    return to_class(PermissionDecisionRequest, x)

def log_result_from_dict(s: Any) -> LogResult:
    return LogResult.from_dict(s)

def log_result_to_dict(x: LogResult) -> Any:
    return to_class(LogResult, x)

def log_request_from_dict(s: Any) -> LogRequest:
    return LogRequest.from_dict(s)

def log_request_to_dict(x: LogRequest) -> Any:
    return to_class(LogRequest, x)

def shell_exec_result_from_dict(s: Any) -> ShellExecResult:
    return ShellExecResult.from_dict(s)

def shell_exec_result_to_dict(x: ShellExecResult) -> Any:
    return to_class(ShellExecResult, x)

def shell_exec_request_from_dict(s: Any) -> ShellExecRequest:
    return ShellExecRequest.from_dict(s)

def shell_exec_request_to_dict(x: ShellExecRequest) -> Any:
    return to_class(ShellExecRequest, x)

def shell_kill_result_from_dict(s: Any) -> ShellKillResult:
    return ShellKillResult.from_dict(s)

def shell_kill_result_to_dict(x: ShellKillResult) -> Any:
    return to_class(ShellKillResult, x)

def shell_kill_request_from_dict(s: Any) -> ShellKillRequest:
    return ShellKillRequest.from_dict(s)

def shell_kill_request_to_dict(x: ShellKillRequest) -> Any:
    return to_class(ShellKillRequest, x)

def history_compact_result_from_dict(s: Any) -> HistoryCompactResult:
    return HistoryCompactResult.from_dict(s)

def history_compact_result_to_dict(x: HistoryCompactResult) -> Any:
    return to_class(HistoryCompactResult, x)

def history_truncate_result_from_dict(s: Any) -> HistoryTruncateResult:
    return HistoryTruncateResult.from_dict(s)

def history_truncate_result_to_dict(x: HistoryTruncateResult) -> Any:
    return to_class(HistoryTruncateResult, x)

def history_truncate_request_from_dict(s: Any) -> HistoryTruncateRequest:
    return HistoryTruncateRequest.from_dict(s)

def history_truncate_request_to_dict(x: HistoryTruncateRequest) -> Any:
    return to_class(HistoryTruncateRequest, x)

def usage_get_metrics_result_from_dict(s: Any) -> UsageGetMetricsResult:
    return UsageGetMetricsResult.from_dict(s)

def usage_get_metrics_result_to_dict(x: UsageGetMetricsResult) -> Any:
    return to_class(UsageGetMetricsResult, x)

def session_fs_read_file_result_from_dict(s: Any) -> SessionFSReadFileResult:
    return SessionFSReadFileResult.from_dict(s)

def session_fs_read_file_result_to_dict(x: SessionFSReadFileResult) -> Any:
    return to_class(SessionFSReadFileResult, x)

def session_fs_read_file_request_from_dict(s: Any) -> SessionFSReadFileRequest:
    return SessionFSReadFileRequest.from_dict(s)

def session_fs_read_file_request_to_dict(x: SessionFSReadFileRequest) -> Any:
    return to_class(SessionFSReadFileRequest, x)

def session_fs_write_file_request_from_dict(s: Any) -> SessionFSWriteFileRequest:
    return SessionFSWriteFileRequest.from_dict(s)

def session_fs_write_file_request_to_dict(x: SessionFSWriteFileRequest) -> Any:
    return to_class(SessionFSWriteFileRequest, x)

def session_fs_append_file_request_from_dict(s: Any) -> SessionFSAppendFileRequest:
    return SessionFSAppendFileRequest.from_dict(s)

def session_fs_append_file_request_to_dict(x: SessionFSAppendFileRequest) -> Any:
    return to_class(SessionFSAppendFileRequest, x)

def session_fs_exists_result_from_dict(s: Any) -> SessionFSExistsResult:
    return SessionFSExistsResult.from_dict(s)

def session_fs_exists_result_to_dict(x: SessionFSExistsResult) -> Any:
    return to_class(SessionFSExistsResult, x)

def session_fs_exists_request_from_dict(s: Any) -> SessionFSExistsRequest:
    return SessionFSExistsRequest.from_dict(s)

def session_fs_exists_request_to_dict(x: SessionFSExistsRequest) -> Any:
    return to_class(SessionFSExistsRequest, x)

def session_fs_stat_result_from_dict(s: Any) -> SessionFSStatResult:
    return SessionFSStatResult.from_dict(s)

def session_fs_stat_result_to_dict(x: SessionFSStatResult) -> Any:
    return to_class(SessionFSStatResult, x)

def session_fs_stat_request_from_dict(s: Any) -> SessionFSStatRequest:
    return SessionFSStatRequest.from_dict(s)

def session_fs_stat_request_to_dict(x: SessionFSStatRequest) -> Any:
    return to_class(SessionFSStatRequest, x)

def session_fs_mkdir_request_from_dict(s: Any) -> SessionFSMkdirRequest:
    return SessionFSMkdirRequest.from_dict(s)

def session_fs_mkdir_request_to_dict(x: SessionFSMkdirRequest) -> Any:
    return to_class(SessionFSMkdirRequest, x)

def session_fs_readdir_result_from_dict(s: Any) -> SessionFSReaddirResult:
    return SessionFSReaddirResult.from_dict(s)

def session_fs_readdir_result_to_dict(x: SessionFSReaddirResult) -> Any:
    return to_class(SessionFSReaddirResult, x)

def session_fs_readdir_request_from_dict(s: Any) -> SessionFSReaddirRequest:
    return SessionFSReaddirRequest.from_dict(s)

def session_fs_readdir_request_to_dict(x: SessionFSReaddirRequest) -> Any:
    return to_class(SessionFSReaddirRequest, x)

def session_fs_readdir_with_types_result_from_dict(s: Any) -> SessionFSReaddirWithTypesResult:
    return SessionFSReaddirWithTypesResult.from_dict(s)

def session_fs_readdir_with_types_result_to_dict(x: SessionFSReaddirWithTypesResult) -> Any:
    return to_class(SessionFSReaddirWithTypesResult, x)

def session_fs_readdir_with_types_request_from_dict(s: Any) -> SessionFSReaddirWithTypesRequest:
    return SessionFSReaddirWithTypesRequest.from_dict(s)

def session_fs_readdir_with_types_request_to_dict(x: SessionFSReaddirWithTypesRequest) -> Any:
    return to_class(SessionFSReaddirWithTypesRequest, x)

def session_fs_rm_request_from_dict(s: Any) -> SessionFSRmRequest:
    return SessionFSRmRequest.from_dict(s)

def session_fs_rm_request_to_dict(x: SessionFSRmRequest) -> Any:
    return to_class(SessionFSRmRequest, x)

def session_fs_rename_request_from_dict(s: Any) -> SessionFSRenameRequest:
    return SessionFSRenameRequest.from_dict(s)

def session_fs_rename_request_to_dict(x: SessionFSRenameRequest) -> Any:
    return to_class(SessionFSRenameRequest, x)


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


class ServerMcpApi:
    def __init__(self, client: "JsonRpcClient"):
        self._client = client

    async def discover(self, params: MCPDiscoverRequest, *, timeout: float | None = None) -> MCPDiscoverResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        return MCPDiscoverResult.from_dict(await self._client.request("mcp.discover", params_dict, **_timeout_kwargs(timeout)))


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


class WorkspaceApi:
    def __init__(self, client: "JsonRpcClient", session_id: str):
        self._client = client
        self._session_id = session_id

    async def list_files(self, *, timeout: float | None = None) -> WorkspaceListFilesResult:
        return WorkspaceListFilesResult.from_dict(await self._client.request("session.workspace.listFiles", {"sessionId": self._session_id}, **_timeout_kwargs(timeout)))

    async def read_file(self, params: WorkspaceReadFileRequest, *, timeout: float | None = None) -> WorkspaceReadFileResult:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        return WorkspaceReadFileResult.from_dict(await self._client.request("session.workspace.readFile", params_dict, **_timeout_kwargs(timeout)))

    async def create_file(self, params: WorkspaceCreateFileRequest, *, timeout: float | None = None) -> None:
        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}
        params_dict["sessionId"] = self._session_id
        await self._client.request("session.workspace.createFile", params_dict, **_timeout_kwargs(timeout))


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
        self.plan = PlanApi(client, session_id)
        self.workspace = WorkspaceApi(client, session_id)
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
    async def write_file(self, params: SessionFSWriteFileRequest) -> None:
        pass
    async def append_file(self, params: SessionFSAppendFileRequest) -> None:
        pass
    async def exists(self, params: SessionFSExistsRequest) -> SessionFSExistsResult:
        pass
    async def stat(self, params: SessionFSStatRequest) -> SessionFSStatResult:
        pass
    async def mkdir(self, params: SessionFSMkdirRequest) -> None:
        pass
    async def readdir(self, params: SessionFSReaddirRequest) -> SessionFSReaddirResult:
        pass
    async def readdir_with_types(self, params: SessionFSReaddirWithTypesRequest) -> SessionFSReaddirWithTypesResult:
        pass
    async def rm(self, params: SessionFSRmRequest) -> None:
        pass
    async def rename(self, params: SessionFSRenameRequest) -> None:
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
        await handler.write_file(request)
        return None
    client.set_request_handler("sessionFs.writeFile", handle_session_fs_write_file)
    async def handle_session_fs_append_file(params: dict) -> dict | None:
        request = SessionFSAppendFileRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        await handler.append_file(request)
        return None
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
        await handler.mkdir(request)
        return None
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
        await handler.rm(request)
        return None
    client.set_request_handler("sessionFs.rm", handle_session_fs_rm)
    async def handle_session_fs_rename(params: dict) -> dict | None:
        request = SessionFSRenameRequest.from_dict(params)
        handler = get_handlers(request.session_id).session_fs
        if handler is None: raise RuntimeError(f"No session_fs handler registered for session: {request.session_id}")
        await handler.rename(request)
        return None
    client.set_request_handler("sessionFs.rename", handle_session_fs_rename)
