"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: session-events.schema.json
"""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import Enum
from typing import Any, ClassVar, TypeVar, cast
from uuid import UUID

import dateutil.parser

T = TypeVar("T")
EnumT = TypeVar("EnumT", bound=Enum)


def from_str(x: Any) -> str:
    assert isinstance(x, str)
    return x


def from_int(x: Any) -> int:
    assert isinstance(x, (int, float)) and not isinstance(x, bool)
    assert not isinstance(x, float) or x.is_integer()
    return int(x)


def to_int(x: Any) -> int:
    assert isinstance(x, int) and not isinstance(x, bool)
    return x


def from_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)


def to_float(x: Any) -> float:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return float(x)


def from_timedelta(x: Any) -> timedelta:
    assert isinstance(x, (float, int)) and not isinstance(x, bool)
    return timedelta(milliseconds=float(x))


def to_timedelta_int(x: timedelta) -> int:
    assert isinstance(x, timedelta)
    milliseconds = x.total_seconds() * 1000.0
    # Durations can carry sub-millisecond precision; round to the nearest whole ms
    # using Python's default banker's rounding (round-half-to-even).
    return round(milliseconds)


def to_timedelta(x: timedelta) -> float:
    assert isinstance(x, timedelta)
    return x.total_seconds() * 1000.0


def from_bool(x: Any) -> bool:
    assert isinstance(x, bool)
    return x


def from_none(x: Any) -> Any:
    assert x is None
    return x


def from_union(fs: list[Callable[[Any], T]], x: Any) -> T:
    for f in fs:
        try:
            return f(x)
        except Exception:
            pass
    assert False


def from_list(f: Callable[[Any], T], x: Any) -> list[T]:
    assert isinstance(x, list)
    return [f(item) for item in x]


def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:
    assert isinstance(x, dict)
    return {key: f(value) for key, value in x.items()}


def from_datetime(x: Any) -> datetime:
    return dateutil.parser.parse(from_str(x))


def to_datetime(x: datetime) -> str:
    return x.isoformat()


def from_uuid(x: Any) -> UUID:
    return UUID(from_str(x))


def to_uuid(x: UUID) -> str:
    return str(x)


def parse_enum(c: type[EnumT], x: Any) -> EnumT:
    assert isinstance(x, str)
    return c(x)


def to_class(c: type[T], x: Any) -> dict:
    assert isinstance(x, c)
    return cast(Any, x).to_dict()


def to_enum(c: type[EnumT], x: Any) -> str:
    assert isinstance(x, c)
    return cast(str, x.value)


class SessionEventType(Enum):
    SESSION_START = "session.start"
    SESSION_RESUME = "session.resume"
    SESSION_REMOTE_STEERABLE_CHANGED = "session.remote_steerable_changed"
    SESSION_ERROR = "session.error"
    SESSION_IDLE = "session.idle"
    SESSION_TITLE_CHANGED = "session.title_changed"
    SESSION_SCHEDULE_CREATED = "session.schedule_created"
    SESSION_SCHEDULE_CANCELLED = "session.schedule_cancelled"
    SESSION_SCHEDULE_REARMED = "session.schedule_rearmed"
    SESSION_AUTOPILOT_OBJECTIVE_CHANGED = "session.autopilot_objective_changed"
    SESSION_INFO = "session.info"
    SESSION_WARNING = "session.warning"
    SESSION_MODEL_CHANGE = "session.model_change"
    SESSION_MODE_CHANGED = "session.mode_changed"
    SESSION_SESSION_LIMITS_CHANGED = "session.session_limits_changed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_PERMISSIONS_CHANGED = "session.permissions_changed"
    SESSION_PLAN_CHANGED = "session.plan_changed"
    SESSION_TODOS_CHANGED = "session.todos_changed"
    SESSION_WORKSPACE_FILE_CHANGED = "session.workspace_file_changed"
    SESSION_HANDOFF = "session.handoff"
    SESSION_TRUNCATION = "session.truncation"
    SESSION_SNAPSHOT_REWIND = "session.snapshot_rewind"
    SESSION_SHUTDOWN = "session.shutdown"
    SESSION_USAGE_CHECKPOINT = "session.usage_checkpoint"
    SESSION_CONTEXT_CHANGED = "session.context_changed"
    SESSION_USAGE_INFO = "session.usage_info"
    SESSION_CONTEXT_CLEARED = "session.context_cleared"
    SESSION_COMPACTION_START = "session.compaction_start"
    SESSION_COMPACTION_COMPLETE = "session.compaction_complete"
    SESSION_TASK_COMPLETE = "session.task_complete"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_FUSION_ROUTE_STARTED = "session.fusion_route_started"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_FUSION_ROUTE_FAILED = "session.fusion_route_failed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_FUSION_RESOLVED = "session.fusion_resolved"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_FUSION_COMPLETED = "session.fusion_completed"
    USER_MESSAGE = "user.message"
    PENDING_MESSAGES_MODIFIED = "pending_messages.modified"
    ASSISTANT_TURN_START = "assistant.turn_start"
    ASSISTANT_TURN_RETRY = "assistant.turn_retry"
    AGENT_INTERRUPTED = "agent.interrupted"
    ASSISTANT_INTENT = "assistant.intent"
    # Experimental: this event is part of an experimental API and may change or be removed.
    ASSISTANT_FUSION_PHASE_STARTED = "assistant.fusion_phase_started"
    # Experimental: this event is part of an experimental API and may change or be removed.
    ASSISTANT_FUSION_PHASE_COMPLETED = "assistant.fusion_phase_completed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    ASSISTANT_FUSION_PHASE_FAILED = "assistant.fusion_phase_failed"
    ASSISTANT_SERVER_TOOL_PROGRESS = "assistant.server_tool_progress"
    ASSISTANT_REASONING = "assistant.reasoning"
    ASSISTANT_REASONING_DELTA = "assistant.reasoning_delta"
    ASSISTANT_TOOL_CALL_DELTA = "assistant.tool_call_delta"
    ASSISTANT_STREAMING_DELTA = "assistant.streaming_delta"
    ASSISTANT_MESSAGE = "assistant.message"
    ASSISTANT_MESSAGE_START = "assistant.message_start"
    ASSISTANT_MESSAGE_DELTA = "assistant.message_delta"
    ASSISTANT_TURN_END = "assistant.turn_end"
    ASSISTANT_IDLE = "assistant.idle"
    ASSISTANT_USAGE = "assistant.usage"
    PROMPT_CACHE_BREAK = "prompt_cache_break"
    MODEL_CALL_FAILURE = "model.call_failure"
    MODEL_CALL_FINISHED = "model.call_finished"
    MODEL_CALL_START = "model.call_start"
    ABORT = "abort"
    TOOL_USER_REQUESTED = "tool.user_requested"
    TOOL_EXECUTION_START = "tool.execution_start"
    TOOL_EXECUTION_PARTIAL_RESULT = "tool.execution_partial_result"
    TOOL_EXECUTION_PROGRESS = "tool.execution_progress"
    TOOL_EXECUTION_COMPLETE = "tool.execution_complete"
    TOOL_SEARCH_ACTIVATED = "tool_search.activated"
    SKILL_INVOKED = "skill.invoked"
    SANDBOX_DECISION = "sandbox.decision"
    SUBAGENT_STARTED = "subagent.started"
    SUBAGENT_CONFIGURED = "subagent.configured"
    SUBAGENT_COMPLETED = "subagent.completed"
    SUBAGENT_FAILED = "subagent.failed"
    SUBAGENT_SELECTED = "subagent.selected"
    SUBAGENT_DESELECTED = "subagent.deselected"
    HOOK_START = "hook.start"
    HOOK_END = "hook.end"
    HOOK_PROGRESS = "hook.progress"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_BINARY_ASSET = "session.binary_asset"
    SYSTEM_MESSAGE = "system.message"
    SYSTEM_NOTIFICATION = "system.notification"
    PERMISSION_REQUESTED = "permission.requested"
    PERMISSION_COMPLETED = "permission.completed"
    USER_INPUT_REQUESTED = "user_input.requested"
    USER_INPUT_COMPLETED = "user_input.completed"
    ELICITATION_REQUESTED = "elicitation.requested"
    ELICITATION_COMPLETED = "elicitation.completed"
    SAMPLING_REQUESTED = "sampling.requested"
    SAMPLING_COMPLETED = "sampling.completed"
    MCP_OAUTH_REQUIRED = "mcp.oauth_required"
    MCP_OAUTH_COMPLETED = "mcp.oauth_completed"
    MCP_HEADERS_REFRESH_REQUIRED = "mcp.headers_refresh_required"
    MCP_HEADERS_REFRESH_COMPLETED = "mcp.headers_refresh_completed"
    SESSION_CUSTOM_NOTIFICATION = "session.custom_notification"
    # Experimental: this event is part of an experimental API and may change or be removed.
    UI_EPHEMERAL_QUERY = "ui.ephemeral_query"
    EXTERNAL_TOOL_REQUESTED = "external_tool.requested"
    EXTERNAL_TOOL_COMPLETED = "external_tool.completed"
    COMMAND_QUEUED = "command.queued"
    COMMAND_EXECUTE = "command.execute"
    COMMAND_COMPLETED = "command.completed"
    AUTO_MODE_SWITCH_REQUESTED = "auto_mode_switch.requested"
    AUTO_MODE_SWITCH_COMPLETED = "auto_mode_switch.completed"
    SESSION_LIMITS_EXHAUSTED_REQUESTED = "session_limits_exhausted.requested"
    SESSION_LIMITS_EXHAUSTED_COMPLETED = "session_limits_exhausted.completed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_AUTO_MODE_RESOLVED = "session.auto_mode_resolved"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_MANAGED_SETTINGS_RESOLVED = "session.managed_settings_resolved"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_MANAGED_SETTINGS_ENFORCED = "session.managed_settings_enforced"
    COMMANDS_CHANGED = "commands.changed"
    CAPABILITIES_CHANGED = "capabilities.changed"
    EXIT_PLAN_MODE_REQUESTED = "exit_plan_mode.requested"
    EXIT_PLAN_MODE_COMPLETED = "exit_plan_mode.completed"
    SESSION_TOOLS_UPDATED = "session.tools_updated"
    SESSION_BACKGROUND_TASKS_CHANGED = "session.background_tasks_changed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    FACTORY_RUN_UPDATED = "factory.run_updated"
    # Experimental: this event is part of an experimental API and may change or be removed.
    FACTORY_RUN_STARTED = "factory.run_started"
    # Experimental: this event is part of an experimental API and may change or be removed.
    FACTORY_RUN_SETTLED = "factory.run_settled"
    SESSION_SKILLS_LOADED = "session.skills_loaded"
    SESSION_CUSTOM_AGENTS_UPDATED = "session.custom_agents_updated"
    SESSION_MCP_SERVERS_LOADED = "session.mcp_servers_loaded"
    SESSION_MCP_SERVER_STATUS_CHANGED = "session.mcp_server_status_changed"
    MCP_TOOLS_LIST_CHANGED = "mcp.tools.list_changed"
    MCP_RESOURCES_LIST_CHANGED = "mcp.resources.list_changed"
    MCP_PROMPTS_LIST_CHANGED = "mcp.prompts.list_changed"
    SESSION_EXTENSIONS_LOADED = "session.extensions_loaded"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_CANVAS_OPENED = "session.canvas.opened"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_CANVAS_REGISTRY_CHANGED = "session.canvas.registry_changed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_CANVAS_CLOSED = "session.canvas.closed"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_CANVAS_UNAVAILABLE = "session.canvas.unavailable"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_CANVAS_RECORDED = "session.canvas.recorded"
    # Experimental: this event is part of an experimental API and may change or be removed.
    SESSION_CANVAS_REMOVED = "session.canvas.removed"
    SESSION_EXTENSIONS_ATTACHMENTS_PUSHED = "session.extensions.attachments_pushed"
    MCP_APP_TOOL_CALL_COMPLETE = "mcp_app.tool_call_complete"
    UNKNOWN = "unknown"

    @classmethod
    def _missing_(cls, value: object) -> "SessionEventType":
        return cls.UNKNOWN


@dataclass
class RawSessionEventData:
    raw: Any

    @staticmethod
    def from_dict(obj: Any) -> "RawSessionEventData":
        return RawSessionEventData(obj)

    def to_dict(self) -> Any:
        return self.raw


def _compat_to_python_key(name: str) -> str:
    normalized = name.replace(".", "_")
    result: list[str] = []
    for index, char in enumerate(normalized):
        if char.isupper() and index > 0 and (not normalized[index - 1].isupper() or (index + 1 < len(normalized) and normalized[index + 1].islower())):
            result.append("_")
        result.append(char.lower())
    return "".join(result)


def _compat_to_json_key(name: str) -> str:
    parts = name.split("_")
    if not parts:
        return name
    return parts[0] + "".join(part[:1].upper() + part[1:] for part in parts[1:])


def _compat_to_json_value(value: Any) -> Any:
    if hasattr(value, "to_dict"):
        return cast(Any, value).to_dict()
    if isinstance(value, Enum):
        return value.value
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, timedelta):
        return value.total_seconds() * 1000.0
    if isinstance(value, UUID):
        return str(value)
    if isinstance(value, list):
        return [_compat_to_json_value(item) for item in value]
    if isinstance(value, dict):
        return {key: _compat_to_json_value(item) for key, item in value.items()}
    return value


def _compat_from_json_value(value: Any) -> Any:
    return value


class Data:
    """Backward-compatible shim for manually constructed event payloads."""

    def __init__(self, **kwargs: Any):
        self._values = {key: _compat_from_json_value(value) for key, value in kwargs.items()}
        self._json_keys: dict[str, str] = {}
        self._json_values: dict[str, Any] | None = None
        for key, value in self._values.items():
            setattr(self, key, value)

    @staticmethod
    def from_dict(obj: Any) -> "Data":
        assert isinstance(obj, dict)
        data = Data()
        data._values = {}
        data._json_keys = {}
        data._json_values = {}
        for key, value in obj.items():
            py_key = _compat_to_python_key(key)
            json_value = _compat_from_json_value(value)
            data._values[py_key] = json_value
            data._json_keys[py_key] = key
            data._json_values[key] = json_value
            setattr(data, py_key, data._values[py_key])
        return data

    def to_dict(self) -> dict:
        if self._json_values is not None:
            return {key: _compat_to_json_value(value) for key, value in self._json_values.items() if value is not None}
        return {(self._json_keys.get(key) or _compat_to_json_key(key)): _compat_to_json_value(value) for key, value in self._values.items() if value is not None}


# Deprecated: this type is deprecated and will be removed in a future version.
@dataclass
class ToolExecutionCompleteContentTerminal:
    "Deprecated for shell command exit metadata. Use ToolExecutionCompleteContentShellExit instead."
    text: str
    type: ClassVar[str] = "terminal"
    cwd: str | None = None
    exit_code: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentTerminal":
        assert isinstance(obj, dict)
        text = from_str(obj.get("text"))
        cwd = from_union([from_none, from_str], obj.get("cwd"))
        exit_code = from_union([from_none, from_int], obj.get("exitCode"))
        return ToolExecutionCompleteContentTerminal(
            text=text,
            cwd=cwd,
            exit_code=exit_code,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["text"] = from_str(self.text)
        result["type"] = self.type
        if self.cwd is not None:
            result["cwd"] = from_union([from_none, from_str], self.cwd)
        if self.exit_code is not None:
            result["exitCode"] = from_union([from_none, to_int], self.exit_code)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AssistantFusionPhaseCompletedData:
    "Experimental durable HydraFusion phase output and lossless replay checkpoint."
    content: str
    conversation_scope: FusionConversationScope
    duration_ms: float
    fusion_id: str
    model: str
    phase_id: str
    phase_kind: FusionPhaseKind
    role: str
    status: FusionPhaseStatus
    usage: FusionPhaseUsage
    verdict: str | None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _projection_message: Any = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _projection_mode: _FusionProjectionMode | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _staged_terminal: _FusionStagedTerminal | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantFusionPhaseCompletedData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        conversation_scope = parse_enum(FusionConversationScope, obj.get("conversationScope"))
        duration_ms = from_float(obj.get("durationMs"))
        fusion_id = from_str(obj.get("fusionId"))
        model = from_str(obj.get("model"))
        phase_id = from_str(obj.get("phaseId"))
        phase_kind = parse_enum(FusionPhaseKind, obj.get("phaseKind"))
        role = from_str(obj.get("role"))
        status = parse_enum(FusionPhaseStatus, obj.get("status"))
        usage = FusionPhaseUsage.from_dict(obj.get("usage"))
        verdict = from_union([from_none, from_str], obj.get("verdict"))
        _projection_message = obj.get("projectionMessage")
        _projection_mode = from_union([from_none, lambda x: parse_enum(_FusionProjectionMode, x)], obj.get("projectionMode"))
        _staged_terminal = from_union([from_none, _FusionStagedTerminal.from_dict], obj.get("stagedTerminal"))
        return AssistantFusionPhaseCompletedData(
            content=content,
            conversation_scope=conversation_scope,
            duration_ms=duration_ms,
            fusion_id=fusion_id,
            model=model,
            phase_id=phase_id,
            phase_kind=phase_kind,
            role=role,
            status=status,
            usage=usage,
            verdict=verdict,
            _projection_message=_projection_message,
            _projection_mode=_projection_mode,
            _staged_terminal=_staged_terminal,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["conversationScope"] = to_enum(FusionConversationScope, self.conversation_scope)
        result["durationMs"] = to_float(self.duration_ms)
        result["fusionId"] = from_str(self.fusion_id)
        result["model"] = from_str(self.model)
        result["phaseId"] = from_str(self.phase_id)
        result["phaseKind"] = to_enum(FusionPhaseKind, self.phase_kind)
        result["role"] = from_str(self.role)
        result["status"] = to_enum(FusionPhaseStatus, self.status)
        result["usage"] = to_class(FusionPhaseUsage, self.usage)
        result["verdict"] = from_union([from_none, from_str], self.verdict)
        if self._projection_message is not None:
            result["projectionMessage"] = self._projection_message
        if self._projection_mode is not None:
            result["projectionMode"] = from_union([from_none, lambda x: to_enum(_FusionProjectionMode, x)], self._projection_mode)
        if self._staged_terminal is not None:
            result["stagedTerminal"] = from_union([from_none, lambda x: to_class(_FusionStagedTerminal, x)], self._staged_terminal)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AssistantFusionPhaseFailedData:
    "Experimental durable typed HydraFusion phase failure and degradation transition."
    conversation_scope: FusionConversationScope
    duration_ms: float
    fusion_id: str
    model: str
    phase_id: str
    phase_kind: FusionPhaseKind
    reason: str
    role: str
    status: FusionPhaseStatus
    usage: FusionPhaseUsage
    degraded_to_phase_id: str | None = None
    error_message: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantFusionPhaseFailedData":
        assert isinstance(obj, dict)
        conversation_scope = parse_enum(FusionConversationScope, obj.get("conversationScope"))
        duration_ms = from_float(obj.get("durationMs"))
        fusion_id = from_str(obj.get("fusionId"))
        model = from_str(obj.get("model"))
        phase_id = from_str(obj.get("phaseId"))
        phase_kind = parse_enum(FusionPhaseKind, obj.get("phaseKind"))
        reason = from_str(obj.get("reason"))
        role = from_str(obj.get("role"))
        status = parse_enum(FusionPhaseStatus, obj.get("status"))
        usage = FusionPhaseUsage.from_dict(obj.get("usage"))
        degraded_to_phase_id = from_union([from_none, from_str], obj.get("degradedToPhaseId"))
        error_message = from_union([from_none, from_str], obj.get("errorMessage"))
        return AssistantFusionPhaseFailedData(
            conversation_scope=conversation_scope,
            duration_ms=duration_ms,
            fusion_id=fusion_id,
            model=model,
            phase_id=phase_id,
            phase_kind=phase_kind,
            reason=reason,
            role=role,
            status=status,
            usage=usage,
            degraded_to_phase_id=degraded_to_phase_id,
            error_message=error_message,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["conversationScope"] = to_enum(FusionConversationScope, self.conversation_scope)
        result["durationMs"] = to_float(self.duration_ms)
        result["fusionId"] = from_str(self.fusion_id)
        result["model"] = from_str(self.model)
        result["phaseId"] = from_str(self.phase_id)
        result["phaseKind"] = to_enum(FusionPhaseKind, self.phase_kind)
        result["reason"] = from_str(self.reason)
        result["role"] = from_str(self.role)
        result["status"] = to_enum(FusionPhaseStatus, self.status)
        result["usage"] = to_class(FusionPhaseUsage, self.usage)
        if self.degraded_to_phase_id is not None:
            result["degradedToPhaseId"] = from_union([from_none, from_str], self.degraded_to_phase_id)
        if self.error_message is not None:
            result["errorMessage"] = from_union([from_none, from_str], self.error_message)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AssistantFusionPhaseStartedData:
    "Experimental transient HydraFusion phase/model/role signal."
    conversation_scope: FusionConversationScope
    fusion_id: str
    model: str
    pattern: FusionPattern
    phase_id: str
    phase_kind: FusionPhaseKind
    role: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantFusionPhaseStartedData":
        assert isinstance(obj, dict)
        conversation_scope = parse_enum(FusionConversationScope, obj.get("conversationScope"))
        fusion_id = from_str(obj.get("fusionId"))
        model = from_str(obj.get("model"))
        pattern = parse_enum(FusionPattern, obj.get("pattern"))
        phase_id = from_str(obj.get("phaseId"))
        phase_kind = parse_enum(FusionPhaseKind, obj.get("phaseKind"))
        role = from_str(obj.get("role"))
        return AssistantFusionPhaseStartedData(
            conversation_scope=conversation_scope,
            fusion_id=fusion_id,
            model=model,
            pattern=pattern,
            phase_id=phase_id,
            phase_kind=phase_kind,
            role=role,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["conversationScope"] = to_enum(FusionConversationScope, self.conversation_scope)
        result["fusionId"] = from_str(self.fusion_id)
        result["model"] = from_str(self.model)
        result["pattern"] = to_enum(FusionPattern, self.pattern)
        result["phaseId"] = from_str(self.phase_id)
        result["phaseKind"] = to_enum(FusionPhaseKind, self.phase_kind)
        result["role"] = from_str(self.role)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AssistantMessageReasoningBlocks:
    "Neutral provider-tagged reasoning content blocks preserved verbatim for round-tripping"
    provider: str
    blocks: list[Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageReasoningBlocks":
        assert isinstance(obj, dict)
        provider = from_str(obj.get("provider"))
        blocks = from_union([from_none, lambda x: from_list(lambda x: x, x)], obj.get("blocks"))
        return AssistantMessageReasoningBlocks(
            provider=provider,
            blocks=blocks,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["provider"] = from_str(self.provider)
        if self.blocks is not None:
            result["blocks"] = from_union([from_none, lambda x: from_list(lambda x: x, x)], self.blocks)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class AssistantMessageServerTools:
    "Neutral provider-tagged server-side tool-use payload (tool search, advisor) for verbatim round-tripping"
    provider: str
    advisor_model: str | None = None
    function_call_namespaces: dict[str, str] | None = None
    items: list[Any] | None = None
    raw_content_blocks: list[Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageServerTools":
        assert isinstance(obj, dict)
        provider = from_str(obj.get("provider"))
        advisor_model = from_union([from_none, from_str], obj.get("advisorModel"))
        function_call_namespaces = from_union([from_none, lambda x: from_dict(from_str, x)], obj.get("functionCallNamespaces"))
        items = from_union([from_none, lambda x: from_list(lambda x: x, x)], obj.get("items"))
        raw_content_blocks = from_union([from_none, lambda x: from_list(lambda x: x, x)], obj.get("rawContentBlocks"))
        return AssistantMessageServerTools(
            provider=provider,
            advisor_model=advisor_model,
            function_call_namespaces=function_call_namespaces,
            items=items,
            raw_content_blocks=raw_content_blocks,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["provider"] = from_str(self.provider)
        if self.advisor_model is not None:
            result["advisorModel"] = from_union([from_none, from_str], self.advisor_model)
        if self.function_call_namespaces is not None:
            result["functionCallNamespaces"] = from_union([from_none, lambda x: from_dict(from_str, x)], self.function_call_namespaces)
        if self.items is not None:
            result["items"] = from_union([from_none, lambda x: from_list(lambda x: x, x)], self.items)
        if self.raw_content_blocks is not None:
            result["rawContentBlocks"] = from_union([from_none, lambda x: from_list(lambda x: x, x)], self.raw_content_blocks)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class BinaryAssetReference:
    "A reference to binary data persisted once on a session.binary_asset event and shared by id"
    asset_id: str
    byte_length: int
    mime_type: str
    type: BinaryAssetReferenceType
    description: str | None = None
    metadata: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "BinaryAssetReference":
        assert isinstance(obj, dict)
        asset_id = from_str(obj.get("assetId"))
        byte_length = from_int(obj.get("byteLength"))
        mime_type = from_str(obj.get("mimeType"))
        type = parse_enum(BinaryAssetReferenceType, obj.get("type"))
        description = from_union([from_none, from_str], obj.get("description"))
        metadata = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("metadata"))
        return BinaryAssetReference(
            asset_id=asset_id,
            byte_length=byte_length,
            mime_type=mime_type,
            type=type,
            description=description,
            metadata=metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["assetId"] = from_str(self.asset_id)
        result["byteLength"] = to_int(self.byte_length)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = to_enum(BinaryAssetReferenceType, self.type)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.metadata is not None:
            result["metadata"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.metadata)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CanvasRegistryChangedCanvas:
    "A single canvas declaration in `session.canvas.registry_changed`, including provider IDs, display metadata, input schema, and actions."
    canvas_id: str
    description: str
    display_name: str
    extension_id: str
    actions: list[CanvasRegistryChangedCanvasAction] | None = None
    extension_name: str | None = None
    icon: str | None = None
    input_schema: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "CanvasRegistryChangedCanvas":
        assert isinstance(obj, dict)
        canvas_id = from_str(obj.get("canvasId"))
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        extension_id = from_str(obj.get("extensionId"))
        actions = from_union([from_none, lambda x: from_list(CanvasRegistryChangedCanvasAction.from_dict, x)], obj.get("actions"))
        extension_name = from_union([from_none, from_str], obj.get("extensionName"))
        icon = from_union([from_none, from_str], obj.get("icon"))
        input_schema = obj.get("inputSchema")
        return CanvasRegistryChangedCanvas(
            canvas_id=canvas_id,
            description=description,
            display_name=display_name,
            extension_id=extension_id,
            actions=actions,
            extension_name=extension_name,
            icon=icon,
            input_schema=input_schema,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvasId"] = from_str(self.canvas_id)
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["extensionId"] = from_str(self.extension_id)
        if self.actions is not None:
            result["actions"] = from_union([from_none, lambda x: from_list(lambda x: to_class(CanvasRegistryChangedCanvasAction, x), x)], self.actions)
        if self.extension_name is not None:
            result["extensionName"] = from_union([from_none, from_str], self.extension_name)
        if self.icon is not None:
            result["icon"] = from_union([from_none, from_str], self.icon)
        if self.input_schema is not None:
            result["inputSchema"] = self.input_schema
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CanvasRegistryChangedCanvasAction:
    "A single action within a canvas declaration, with its name, optional description, and optional input schema."
    name: str
    description: str | None = None
    input_schema: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "CanvasRegistryChangedCanvasAction":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        description = from_union([from_none, from_str], obj.get("description"))
        input_schema = obj.get("inputSchema")
        return CanvasRegistryChangedCanvasAction(
            name=name,
            description=description,
            input_schema=input_schema,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.input_schema is not None:
            result["inputSchema"] = self.input_schema
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitableSource:
    "A source supplied by a tool that should be made available to the model as citable content."
    content: str
    id: str
    path: str | None = None
    title: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CitableSource":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        id = from_str(obj.get("id"))
        path = from_union([from_none, from_str], obj.get("path"))
        title = from_union([from_none, from_str], obj.get("title"))
        url = from_union([from_none, from_str], obj.get("url"))
        return CitableSource(
            content=content,
            id=id,
            path=path,
            title=title,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["id"] = from_str(self.id)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitationLocationBlock:
    "A content-block range within a structured source document."
    end_block: int
    start_block: int
    type: ClassVar[str] = "block"

    @staticmethod
    def from_dict(obj: Any) -> "CitationLocationBlock":
        assert isinstance(obj, dict)
        end_block = from_int(obj.get("endBlock"))
        start_block = from_int(obj.get("startBlock"))
        return CitationLocationBlock(
            end_block=end_block,
            start_block=start_block,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["endBlock"] = to_int(self.end_block)
        result["startBlock"] = to_int(self.start_block)
        result["type"] = self.type
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitationLocationChar:
    "A character range within the source's text content."
    end_index: int
    start_index: int
    type: ClassVar[str] = "char"

    @staticmethod
    def from_dict(obj: Any) -> "CitationLocationChar":
        assert isinstance(obj, dict)
        end_index = from_int(obj.get("endIndex"))
        start_index = from_int(obj.get("startIndex"))
        return CitationLocationChar(
            end_index=end_index,
            start_index=start_index,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["endIndex"] = to_int(self.end_index)
        result["startIndex"] = to_int(self.start_index)
        result["type"] = self.type
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitationLocationPage:
    "A page range within a paginated source document."
    end_page: int
    start_page: int
    type: ClassVar[str] = "page"

    @staticmethod
    def from_dict(obj: Any) -> "CitationLocationPage":
        assert isinstance(obj, dict)
        end_page = from_int(obj.get("endPage"))
        start_page = from_int(obj.get("startPage"))
        return CitationLocationPage(
            end_page=end_page,
            start_page=start_page,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["endPage"] = to_int(self.end_page)
        result["startPage"] = to_int(self.start_page)
        result["type"] = self.type
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitationReference:
    "A single citation occurrence linking a span of generated text to a supporting source."
    source_id: str
    cited_text: str | None = None
    location: CitationLocation | None = None
    provider_metadata: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "CitationReference":
        assert isinstance(obj, dict)
        source_id = from_str(obj.get("sourceId"))
        cited_text = from_union([from_none, from_str], obj.get("citedText"))
        location = from_union([from_none, _load_CitationLocation], obj.get("location"))
        provider_metadata = obj.get("providerMetadata")
        return CitationReference(
            source_id=source_id,
            cited_text=cited_text,
            location=location,
            provider_metadata=provider_metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sourceId"] = from_str(self.source_id)
        if self.cited_text is not None:
            result["citedText"] = from_union([from_none, from_str], self.cited_text)
        if self.location is not None:
            result["location"] = from_union([from_none, lambda x: x.to_dict()], self.location)
        if self.provider_metadata is not None:
            result["providerMetadata"] = self.provider_metadata
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitationSource:
    "A source that backs one or more cited spans in the assistant's response."
    id: str
    provider: CitationProvider
    path: str | None = None
    title: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CitationSource":
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        provider = parse_enum(CitationProvider, obj.get("provider"))
        path = from_union([from_none, from_str], obj.get("path"))
        title = from_union([from_none, from_str], obj.get("title"))
        url = from_union([from_none, from_str], obj.get("url"))
        return CitationSource(
            id=id,
            provider=provider,
            path=path,
            title=title,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        result["provider"] = to_enum(CitationProvider, self.provider)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class CitationSpan:
    "A contiguous span of generated assistant text and the source references that support it."
    end_index: int
    references: list[CitationReference]
    start_index: int

    @staticmethod
    def from_dict(obj: Any) -> "CitationSpan":
        assert isinstance(obj, dict)
        end_index = from_int(obj.get("endIndex"))
        references = from_list(CitationReference.from_dict, obj.get("references"))
        start_index = from_int(obj.get("startIndex"))
        return CitationSpan(
            end_index=end_index,
            references=references,
            start_index=start_index,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["endIndex"] = to_int(self.end_index)
        result["references"] = from_list(lambda x: to_class(CitationReference, x), self.references)
        result["startIndex"] = to_int(self.start_index)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class Citations:
    "Provider-agnostic citations linking spans of the assistant's response to their supporting sources."
    sources: list[CitationSource]
    spans: list[CitationSpan]

    @staticmethod
    def from_dict(obj: Any) -> "Citations":
        assert isinstance(obj, dict)
        sources = from_list(CitationSource.from_dict, obj.get("sources"))
        spans = from_list(CitationSpan.from_dict, obj.get("spans"))
        return Citations(
            sources=sources,
            spans=spans,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sources"] = from_list(lambda x: to_class(CitationSource, x), self.sources)
        result["spans"] = from_list(lambda x: to_class(CitationSpan, x), self.spans)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FactoryRunSettledData:
    "Ephemeral signal that a factory run reached a terminal status."
    consumed_nano_aiu: int
    consumed_subagents: int
    elapsed_ms: int
    run_id: str
    status: FactoryRunSettledStatus
    failure_type: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "FactoryRunSettledData":
        assert isinstance(obj, dict)
        consumed_nano_aiu = from_int(obj.get("consumedNanoAiu"))
        consumed_subagents = from_int(obj.get("consumedSubagents"))
        elapsed_ms = from_int(obj.get("elapsedMs"))
        run_id = from_str(obj.get("runId"))
        status = parse_enum(FactoryRunSettledStatus, obj.get("status"))
        failure_type = from_union([from_none, from_str], obj.get("failureType"))
        return FactoryRunSettledData(
            consumed_nano_aiu=consumed_nano_aiu,
            consumed_subagents=consumed_subagents,
            elapsed_ms=elapsed_ms,
            run_id=run_id,
            status=status,
            failure_type=failure_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["consumedNanoAiu"] = to_int(self.consumed_nano_aiu)
        result["consumedSubagents"] = to_int(self.consumed_subagents)
        result["elapsedMs"] = to_int(self.elapsed_ms)
        result["runId"] = from_str(self.run_id)
        result["status"] = to_enum(FactoryRunSettledStatus, self.status)
        if self.failure_type is not None:
            result["failureType"] = from_union([from_none, from_str], self.failure_type)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FactoryRunStartedData:
    "Ephemeral signal that a factory run attempt began executing."
    attempt: int
    factory_name: str
    run_id: str

    @staticmethod
    def from_dict(obj: Any) -> "FactoryRunStartedData":
        assert isinstance(obj, dict)
        attempt = from_int(obj.get("attempt"))
        factory_name = from_str(obj.get("factoryName"))
        run_id = from_str(obj.get("runId"))
        return FactoryRunStartedData(
            attempt=attempt,
            factory_name=factory_name,
            run_id=run_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["attempt"] = to_int(self.attempt)
        result["factoryName"] = from_str(self.factory_name)
        result["runId"] = from_str(self.run_id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FactoryRunUpdatedData:
    "Ephemeral invalidation signal for a changed factory run."
    revision: int
    run_id: str

    @staticmethod
    def from_dict(obj: Any) -> "FactoryRunUpdatedData":
        assert isinstance(obj, dict)
        revision = from_int(obj.get("revision"))
        run_id = from_str(obj.get("runId"))
        return FactoryRunUpdatedData(
            revision=revision,
            run_id=run_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["revision"] = to_int(self.revision)
        result["runId"] = from_str(self.run_id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FusionAttribution:
    "Experimental attribution linking an ordinary event to the HydraFusion turn, phase, and concrete source that produced it."
    fusion_id: str
    pattern: str
    policy: str
    synthetic_model: str
    commit_id: str | None = None
    conversation_scope: str | None = None
    phase_id: str | None = None
    phase_kind: str | None = None
    role: str | None = None
    source_model: str | None = None
    source_phase_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "FusionAttribution":
        assert isinstance(obj, dict)
        fusion_id = from_str(obj.get("fusionId"))
        pattern = from_str(obj.get("pattern"))
        policy = from_str(obj.get("policy"))
        synthetic_model = from_str(obj.get("syntheticModel"))
        commit_id = from_union([from_none, from_str], obj.get("commitId"))
        conversation_scope = from_union([from_none, from_str], obj.get("conversationScope"))
        phase_id = from_union([from_none, from_str], obj.get("phaseId"))
        phase_kind = from_union([from_none, from_str], obj.get("phaseKind"))
        role = from_union([from_none, from_str], obj.get("role"))
        source_model = from_union([from_none, from_str], obj.get("sourceModel"))
        source_phase_id = from_union([from_none, from_str], obj.get("sourcePhaseId"))
        return FusionAttribution(
            fusion_id=fusion_id,
            pattern=pattern,
            policy=policy,
            synthetic_model=synthetic_model,
            commit_id=commit_id,
            conversation_scope=conversation_scope,
            phase_id=phase_id,
            phase_kind=phase_kind,
            role=role,
            source_model=source_model,
            source_phase_id=source_phase_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["fusionId"] = from_str(self.fusion_id)
        result["pattern"] = from_str(self.pattern)
        result["policy"] = from_str(self.policy)
        result["syntheticModel"] = from_str(self.synthetic_model)
        if self.commit_id is not None:
            result["commitId"] = from_union([from_none, from_str], self.commit_id)
        if self.conversation_scope is not None:
            result["conversationScope"] = from_union([from_none, from_str], self.conversation_scope)
        if self.phase_id is not None:
            result["phaseId"] = from_union([from_none, from_str], self.phase_id)
        if self.phase_kind is not None:
            result["phaseKind"] = from_union([from_none, from_str], self.phase_kind)
        if self.role is not None:
            result["role"] = from_union([from_none, from_str], self.role)
        if self.source_model is not None:
            result["sourceModel"] = from_union([from_none, from_str], self.source_model)
        if self.source_phase_id is not None:
            result["sourcePhaseId"] = from_union([from_none, from_str], self.source_phase_id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FusionFollowUpRecommendation:
    "Durable server recommendation for subsequent HydraFusion turns."
    compaction_turn: FusionFollowUpAction
    user_turn: FusionFollowUpAction

    @staticmethod
    def from_dict(obj: Any) -> "FusionFollowUpRecommendation":
        assert isinstance(obj, dict)
        compaction_turn = parse_enum(FusionFollowUpAction, obj.get("compactionTurn"))
        user_turn = parse_enum(FusionFollowUpAction, obj.get("userTurn"))
        return FusionFollowUpRecommendation(
            compaction_turn=compaction_turn,
            user_turn=user_turn,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["compactionTurn"] = to_enum(FusionFollowUpAction, self.compaction_turn)
        result["userTurn"] = to_enum(FusionFollowUpAction, self.user_turn)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FusionPhaseUsage:
    "Aggregate concrete-model usage for one HydraFusion phase."
    cached_tokens: int
    input_tokens: int
    output_tokens: int
    request_count: int
    total_nano_aiu: float
    cache_write_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "FusionPhaseUsage":
        assert isinstance(obj, dict)
        cached_tokens = from_int(obj.get("cachedTokens"))
        input_tokens = from_int(obj.get("inputTokens"))
        output_tokens = from_int(obj.get("outputTokens"))
        request_count = from_int(obj.get("requestCount"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        cache_write_tokens = from_union([from_none, from_int], obj.get("cacheWriteTokens"))
        return FusionPhaseUsage(
            cached_tokens=cached_tokens,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            request_count=request_count,
            total_nano_aiu=total_nano_aiu,
            cache_write_tokens=cache_write_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cachedTokens"] = to_int(self.cached_tokens)
        result["inputTokens"] = to_int(self.input_tokens)
        result["outputTokens"] = to_int(self.output_tokens)
        result["requestCount"] = to_int(self.request_count)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([from_none, to_int], self.cache_write_tokens)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class FusionScores:
    "Validated HydraFusion routing capability scores."
    code_gen: float
    debugging: float
    reasoning: float
    tool_use: float

    @staticmethod
    def from_dict(obj: Any) -> "FusionScores":
        assert isinstance(obj, dict)
        code_gen = from_float(obj.get("codeGen"))
        debugging = from_float(obj.get("debugging"))
        reasoning = from_float(obj.get("reasoning"))
        tool_use = from_float(obj.get("toolUse"))
        return FusionScores(
            code_gen=code_gen,
            debugging=debugging,
            reasoning=reasoning,
            tool_use=tool_use,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["codeGen"] = to_float(self.code_gen)
        result["debugging"] = to_float(self.debugging)
        result["reasoning"] = to_float(self.reasoning)
        result["toolUse"] = to_float(self.tool_use)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class _FusionStagedTerminal:
    "Internal durable terminal request staged by a HydraFusion phase until an idempotent final commit selects it."
    arguments: str
    assistant_message: Any
    phase_id: str
    tool_call_id: str
    tool_name: str

    @staticmethod
    def from_dict(obj: Any) -> "_FusionStagedTerminal":
        assert isinstance(obj, dict)
        arguments = from_str(obj.get("arguments"))
        assistant_message = obj.get("assistantMessage")
        phase_id = from_str(obj.get("phaseId"))
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        return _FusionStagedTerminal(
            arguments=arguments,
            assistant_message=assistant_message,
            phase_id=phase_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["arguments"] = from_str(self.arguments)
        result["assistantMessage"] = self.assistant_message
        result["phaseId"] = from_str(self.phase_id)
        result["toolCallId"] = from_str(self.tool_call_id)
        result["toolName"] = from_str(self.tool_name)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class OmittedBinaryResult:
    "A binary result whose data was omitted from persistence due to the inline size limit"
    byte_length: int
    mime_type: str
    omitted_reason: OmittedBinaryOmittedReason
    type: OmittedBinaryType
    description: str | None = None
    metadata: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "OmittedBinaryResult":
        assert isinstance(obj, dict)
        byte_length = from_int(obj.get("byteLength"))
        mime_type = from_str(obj.get("mimeType"))
        omitted_reason = parse_enum(OmittedBinaryOmittedReason, obj.get("omittedReason"))
        type = parse_enum(OmittedBinaryType, obj.get("type"))
        description = from_union([from_none, from_str], obj.get("description"))
        metadata = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("metadata"))
        return OmittedBinaryResult(
            byte_length=byte_length,
            mime_type=mime_type,
            omitted_reason=omitted_reason,
            type=type,
            description=description,
            metadata=metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["byteLength"] = to_int(self.byte_length)
        result["mimeType"] = from_str(self.mime_type)
        result["omittedReason"] = to_enum(OmittedBinaryOmittedReason, self.omitted_reason)
        result["type"] = to_enum(OmittedBinaryType, self.type)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.metadata is not None:
            result["metadata"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.metadata)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class PermissionAssistedApproval:
    "Assisted-approval judge information attached to a permission request. Present only in assisted mode; its absence means the judge did not evaluate the request. The `recommendation` conveys the judge's disposition for this request."
    recommendation: AssistedApprovalRecommendation
    failure_reason: AssistedApprovalJudgeFailureReason | None = None
    model: str | None = None
    reason: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionAssistedApproval":
        assert isinstance(obj, dict)
        recommendation = parse_enum(AssistedApprovalRecommendation, obj.get("recommendation"))
        failure_reason = from_union([from_none, lambda x: parse_enum(AssistedApprovalJudgeFailureReason, x)], obj.get("failureReason"))
        model = from_union([from_none, from_str], obj.get("model"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        return PermissionAssistedApproval(
            recommendation=recommendation,
            failure_reason=failure_reason,
            model=model,
            reason=reason,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["recommendation"] = to_enum(AssistedApprovalRecommendation, self.recommendation)
        if self.failure_reason is not None:
            result["failureReason"] = from_union([from_none, lambda x: to_enum(AssistedApprovalJudgeFailureReason, x)], self.failure_reason)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionAutoModeResolvedData:
    "Auto Intent resolution: the concrete model the session settled on for the first prompt of an auto-mode session, and why. Lets SDK clients render the chosen model and the full reason it was picked. The core selection fields (chosenModel/reasoningBucket/categoryScores) are stable; the routing-analytics fields (predictedLabel/confidence/candidateModels) mirror the upstream intent service and may evolve, hence the event's experimental stability."
    chosen_model: str
    available_models: list[str] | None = None
    candidate_models: list[str] | None = None
    category_scores: dict[str, float] | None = None
    chosen_shortfall: float | None = None
    confidence: float | None = None
    end_to_end_latency_ms: float | None = None
    fallback: bool | None = None
    fallback_reason: str | None = None
    has_image: bool | None = None
    predicted_label: str | None = None
    reasoning_bucket: AutoModeResolvedReasoningBucket | None = None
    router_latency_ms: float | None = None
    routing_method: str | None = None
    sticky_override: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionAutoModeResolvedData":
        assert isinstance(obj, dict)
        chosen_model = from_str(obj.get("chosenModel"))
        available_models = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("availableModels"))
        candidate_models = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("candidateModels"))
        category_scores = from_union([from_none, lambda x: from_dict(from_float, x)], obj.get("categoryScores"))
        chosen_shortfall = from_union([from_none, from_float], obj.get("chosenShortfall"))
        confidence = from_union([from_none, from_float], obj.get("confidence"))
        end_to_end_latency_ms = from_union([from_none, from_float], obj.get("endToEndLatencyMs"))
        fallback = from_union([from_none, from_bool], obj.get("fallback"))
        fallback_reason = from_union([from_none, from_str], obj.get("fallbackReason"))
        has_image = from_union([from_none, from_bool], obj.get("hasImage"))
        predicted_label = from_union([from_none, from_str], obj.get("predictedLabel"))
        reasoning_bucket = from_union([from_none, lambda x: parse_enum(AutoModeResolvedReasoningBucket, x)], obj.get("reasoningBucket"))
        router_latency_ms = from_union([from_none, from_float], obj.get("routerLatencyMs"))
        routing_method = from_union([from_none, from_str], obj.get("routingMethod"))
        sticky_override = from_union([from_none, from_bool], obj.get("stickyOverride"))
        return SessionAutoModeResolvedData(
            chosen_model=chosen_model,
            available_models=available_models,
            candidate_models=candidate_models,
            category_scores=category_scores,
            chosen_shortfall=chosen_shortfall,
            confidence=confidence,
            end_to_end_latency_ms=end_to_end_latency_ms,
            fallback=fallback,
            fallback_reason=fallback_reason,
            has_image=has_image,
            predicted_label=predicted_label,
            reasoning_bucket=reasoning_bucket,
            router_latency_ms=router_latency_ms,
            routing_method=routing_method,
            sticky_override=sticky_override,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["chosenModel"] = from_str(self.chosen_model)
        if self.available_models is not None:
            result["availableModels"] = from_union([from_none, lambda x: from_list(from_str, x)], self.available_models)
        if self.candidate_models is not None:
            result["candidateModels"] = from_union([from_none, lambda x: from_list(from_str, x)], self.candidate_models)
        if self.category_scores is not None:
            result["categoryScores"] = from_union([from_none, lambda x: from_dict(to_float, x)], self.category_scores)
        if self.chosen_shortfall is not None:
            result["chosenShortfall"] = from_union([from_none, to_float], self.chosen_shortfall)
        if self.confidence is not None:
            result["confidence"] = from_union([from_none, to_float], self.confidence)
        if self.end_to_end_latency_ms is not None:
            result["endToEndLatencyMs"] = from_union([from_none, to_float], self.end_to_end_latency_ms)
        if self.fallback is not None:
            result["fallback"] = from_union([from_none, from_bool], self.fallback)
        if self.fallback_reason is not None:
            result["fallbackReason"] = from_union([from_none, from_str], self.fallback_reason)
        if self.has_image is not None:
            result["hasImage"] = from_union([from_none, from_bool], self.has_image)
        if self.predicted_label is not None:
            result["predictedLabel"] = from_union([from_none, from_str], self.predicted_label)
        if self.reasoning_bucket is not None:
            result["reasoningBucket"] = from_union([from_none, lambda x: to_enum(AutoModeResolvedReasoningBucket, x)], self.reasoning_bucket)
        if self.router_latency_ms is not None:
            result["routerLatencyMs"] = from_union([from_none, to_float], self.router_latency_ms)
        if self.routing_method is not None:
            result["routingMethod"] = from_union([from_none, from_str], self.routing_method)
        if self.sticky_override is not None:
            result["stickyOverride"] = from_union([from_none, from_bool], self.sticky_override)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCanvasClosedData:
    "Payload of `session.canvas.closed` with the closed canvas instance ID, provider ID, and canvas ID."
    canvas_id: str
    extension_id: str
    instance_id: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionCanvasClosedData":
        assert isinstance(obj, dict)
        canvas_id = from_str(obj.get("canvasId"))
        extension_id = from_str(obj.get("extensionId"))
        instance_id = from_str(obj.get("instanceId"))
        return SessionCanvasClosedData(
            canvas_id=canvas_id,
            extension_id=extension_id,
            instance_id=instance_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvasId"] = from_str(self.canvas_id)
        result["extensionId"] = from_str(self.extension_id)
        result["instanceId"] = from_str(self.instance_id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCanvasOpenedData:
    "Payload of `session.canvas.opened` with canvas instance and provider IDs plus optional icon, title, status, URL, and input."
    canvas_id: str
    extension_id: str
    instance_id: str
    extension_name: str | None = None
    icon: str | None = None
    input: Any = None
    status: str | None = None
    title: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCanvasOpenedData":
        assert isinstance(obj, dict)
        canvas_id = from_str(obj.get("canvasId"))
        extension_id = from_str(obj.get("extensionId"))
        instance_id = from_str(obj.get("instanceId"))
        extension_name = from_union([from_none, from_str], obj.get("extensionName"))
        icon = from_union([from_none, from_str], obj.get("icon"))
        input = obj.get("input")
        status = from_union([from_none, from_str], obj.get("status"))
        title = from_union([from_none, from_str], obj.get("title"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionCanvasOpenedData(
            canvas_id=canvas_id,
            extension_id=extension_id,
            instance_id=instance_id,
            extension_name=extension_name,
            icon=icon,
            input=input,
            status=status,
            title=title,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvasId"] = from_str(self.canvas_id)
        result["extensionId"] = from_str(self.extension_id)
        result["instanceId"] = from_str(self.instance_id)
        if self.extension_name is not None:
            result["extensionName"] = from_union([from_none, from_str], self.extension_name)
        if self.icon is not None:
            result["icon"] = from_union([from_none, from_str], self.icon)
        if self.input is not None:
            result["input"] = self.input
        if self.status is not None:
            result["status"] = from_union([from_none, from_str], self.status)
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCanvasRecordedData:
    "Durable record that a canvas instance is open, used to restore open canvases on cold session resume. Intentionally omits the transient url and availability."
    canvas_id: str
    extension_id: str
    instance_id: str
    input: Any = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCanvasRecordedData":
        assert isinstance(obj, dict)
        canvas_id = from_str(obj.get("canvasId"))
        extension_id = from_str(obj.get("extensionId"))
        instance_id = from_str(obj.get("instanceId"))
        input = obj.get("input")
        title = from_union([from_none, from_str], obj.get("title"))
        return SessionCanvasRecordedData(
            canvas_id=canvas_id,
            extension_id=extension_id,
            instance_id=instance_id,
            input=input,
            title=title,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvasId"] = from_str(self.canvas_id)
        result["extensionId"] = from_str(self.extension_id)
        result["instanceId"] = from_str(self.instance_id)
        if self.input is not None:
            result["input"] = self.input
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCanvasRegistryChangedData:
    "Payload of `session.canvas.registry_changed` listing the canvas declarations currently available."
    canvases: list[CanvasRegistryChangedCanvas]

    @staticmethod
    def from_dict(obj: Any) -> "SessionCanvasRegistryChangedData":
        assert isinstance(obj, dict)
        canvases = from_list(CanvasRegistryChangedCanvas.from_dict, obj.get("canvases"))
        return SessionCanvasRegistryChangedData(
            canvases=canvases,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvases"] = from_list(lambda x: to_class(CanvasRegistryChangedCanvas, x), self.canvases)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCanvasRemovedData:
    "Durable record that a canvas instance was closed, superseding a prior instance_recorded during resume replay."
    canvas_id: str
    extension_id: str
    instance_id: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionCanvasRemovedData":
        assert isinstance(obj, dict)
        canvas_id = from_str(obj.get("canvasId"))
        extension_id = from_str(obj.get("extensionId"))
        instance_id = from_str(obj.get("instanceId"))
        return SessionCanvasRemovedData(
            canvas_id=canvas_id,
            extension_id=extension_id,
            instance_id=instance_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvasId"] = from_str(self.canvas_id)
        result["extensionId"] = from_str(self.extension_id)
        result["instanceId"] = from_str(self.instance_id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionCanvasUnavailableData:
    "Transient signal that an open canvas instance's provider has dropped (for example the extension is reloading mid-session). The host should keep the panel mounted and surface a reconnecting affordance rather than tearing it down; a subsequent `session.canvas.opened` for the same instanceId clears the affordance once the provider reconnects with a fresh url. Ephemeral and never persisted, so it is never replayed on cold resume."
    canvas_id: str
    extension_id: str
    instance_id: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionCanvasUnavailableData":
        assert isinstance(obj, dict)
        canvas_id = from_str(obj.get("canvasId"))
        extension_id = from_str(obj.get("extensionId"))
        instance_id = from_str(obj.get("instanceId"))
        return SessionCanvasUnavailableData(
            canvas_id=canvas_id,
            extension_id=extension_id,
            instance_id=instance_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canvasId"] = from_str(self.canvas_id)
        result["extensionId"] = from_str(self.extension_id)
        result["instanceId"] = from_str(self.instance_id)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionFusionCompletedData:
    "Experimental durable aggregate outcome of a HydraFusion turn."
    cached_tokens: int
    commit_id: str
    degraded_reason: str | None
    duration_ms: float
    final_source_model: str | None
    final_source_phase_id: str | None
    follow_up_model: str
    fusion_id: str
    input_tokens: int
    outcome: str
    output_tokens: int
    pattern: FusionPattern
    phase_count: int
    request_count: int
    synthetic_model: str
    total_nano_aiu: float
    turn_id: str
    cache_write_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionFusionCompletedData":
        assert isinstance(obj, dict)
        cached_tokens = from_int(obj.get("cachedTokens"))
        commit_id = from_str(obj.get("commitId"))
        degraded_reason = from_union([from_none, from_str], obj.get("degradedReason"))
        duration_ms = from_float(obj.get("durationMs"))
        final_source_model = from_union([from_none, from_str], obj.get("finalSourceModel"))
        final_source_phase_id = from_union([from_none, from_str], obj.get("finalSourcePhaseId"))
        follow_up_model = from_str(obj.get("followUpModel"))
        fusion_id = from_str(obj.get("fusionId"))
        input_tokens = from_int(obj.get("inputTokens"))
        outcome = from_str(obj.get("outcome"))
        output_tokens = from_int(obj.get("outputTokens"))
        pattern = parse_enum(FusionPattern, obj.get("pattern"))
        phase_count = from_int(obj.get("phaseCount"))
        request_count = from_int(obj.get("requestCount"))
        synthetic_model = from_str(obj.get("syntheticModel"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        turn_id = from_str(obj.get("turnId"))
        cache_write_tokens = from_union([from_none, from_int], obj.get("cacheWriteTokens"))
        return SessionFusionCompletedData(
            cached_tokens=cached_tokens,
            commit_id=commit_id,
            degraded_reason=degraded_reason,
            duration_ms=duration_ms,
            final_source_model=final_source_model,
            final_source_phase_id=final_source_phase_id,
            follow_up_model=follow_up_model,
            fusion_id=fusion_id,
            input_tokens=input_tokens,
            outcome=outcome,
            output_tokens=output_tokens,
            pattern=pattern,
            phase_count=phase_count,
            request_count=request_count,
            synthetic_model=synthetic_model,
            total_nano_aiu=total_nano_aiu,
            turn_id=turn_id,
            cache_write_tokens=cache_write_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cachedTokens"] = to_int(self.cached_tokens)
        result["commitId"] = from_str(self.commit_id)
        result["degradedReason"] = from_union([from_none, from_str], self.degraded_reason)
        result["durationMs"] = to_float(self.duration_ms)
        result["finalSourceModel"] = from_union([from_none, from_str], self.final_source_model)
        result["finalSourcePhaseId"] = from_union([from_none, from_str], self.final_source_phase_id)
        result["followUpModel"] = from_str(self.follow_up_model)
        result["fusionId"] = from_str(self.fusion_id)
        result["inputTokens"] = to_int(self.input_tokens)
        result["outcome"] = from_str(self.outcome)
        result["outputTokens"] = to_int(self.output_tokens)
        result["pattern"] = to_enum(FusionPattern, self.pattern)
        result["phaseCount"] = to_int(self.phase_count)
        result["requestCount"] = to_int(self.request_count)
        result["syntheticModel"] = from_str(self.synthetic_model)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        result["turnId"] = from_str(self.turn_id)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([from_none, to_int], self.cache_write_tokens)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionFusionResolvedData:
    "Experimental durable validated HydraFusion route and turn policy."
    contract_version: int
    fallback_model: str
    follow_up_model: str
    fusion_id: str
    pattern: FusionPattern
    policy: str
    primary_model: str
    secondary_model: str | None
    synthetic_model: str
    turn_id: str
    follow_up: FusionFollowUpRecommendation | None = None
    model_universe_version: str | None = None
    plan_version: str | None = None
    policy_version: str | None = None
    route_source: str | None = None
    routing_latency_ms: float | None = None
    rule_id: str | None = None
    rule_index: int | None = None
    rule_name: str | None = None
    scores: FusionScores | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionFusionResolvedData":
        assert isinstance(obj, dict)
        contract_version = from_int(obj.get("contractVersion"))
        fallback_model = from_str(obj.get("fallbackModel"))
        follow_up_model = from_str(obj.get("followUpModel"))
        fusion_id = from_str(obj.get("fusionId"))
        pattern = parse_enum(FusionPattern, obj.get("pattern"))
        policy = from_str(obj.get("policy"))
        primary_model = from_str(obj.get("primaryModel"))
        secondary_model = from_union([from_none, from_str], obj.get("secondaryModel"))
        synthetic_model = from_str(obj.get("syntheticModel"))
        turn_id = from_str(obj.get("turnId"))
        follow_up = from_union([from_none, FusionFollowUpRecommendation.from_dict], obj.get("followUp"))
        model_universe_version = from_union([from_none, from_str], obj.get("modelUniverseVersion"))
        plan_version = from_union([from_none, from_str], obj.get("planVersion"))
        policy_version = from_union([from_none, from_str], obj.get("policyVersion"))
        route_source = from_union([from_none, from_str], obj.get("routeSource"))
        routing_latency_ms = from_union([from_none, from_float], obj.get("routingLatencyMs"))
        rule_id = from_union([from_none, from_str], obj.get("ruleId"))
        rule_index = from_union([from_none, from_int], obj.get("ruleIndex"))
        rule_name = from_union([from_none, from_str], obj.get("ruleName"))
        scores = from_union([from_none, FusionScores.from_dict], obj.get("scores"))
        return SessionFusionResolvedData(
            contract_version=contract_version,
            fallback_model=fallback_model,
            follow_up_model=follow_up_model,
            fusion_id=fusion_id,
            pattern=pattern,
            policy=policy,
            primary_model=primary_model,
            secondary_model=secondary_model,
            synthetic_model=synthetic_model,
            turn_id=turn_id,
            follow_up=follow_up,
            model_universe_version=model_universe_version,
            plan_version=plan_version,
            policy_version=policy_version,
            route_source=route_source,
            routing_latency_ms=routing_latency_ms,
            rule_id=rule_id,
            rule_index=rule_index,
            rule_name=rule_name,
            scores=scores,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["contractVersion"] = to_int(self.contract_version)
        result["fallbackModel"] = from_str(self.fallback_model)
        result["followUpModel"] = from_str(self.follow_up_model)
        result["fusionId"] = from_str(self.fusion_id)
        result["pattern"] = to_enum(FusionPattern, self.pattern)
        result["policy"] = from_str(self.policy)
        result["primaryModel"] = from_str(self.primary_model)
        result["secondaryModel"] = from_union([from_none, from_str], self.secondary_model)
        result["syntheticModel"] = from_str(self.synthetic_model)
        result["turnId"] = from_str(self.turn_id)
        if self.follow_up is not None:
            result["followUp"] = from_union([from_none, lambda x: to_class(FusionFollowUpRecommendation, x)], self.follow_up)
        if self.model_universe_version is not None:
            result["modelUniverseVersion"] = from_union([from_none, from_str], self.model_universe_version)
        if self.plan_version is not None:
            result["planVersion"] = from_union([from_none, from_str], self.plan_version)
        if self.policy_version is not None:
            result["policyVersion"] = from_union([from_none, from_str], self.policy_version)
        if self.route_source is not None:
            result["routeSource"] = from_union([from_none, from_str], self.route_source)
        if self.routing_latency_ms is not None:
            result["routingLatencyMs"] = from_union([from_none, to_float], self.routing_latency_ms)
        if self.rule_id is not None:
            result["ruleId"] = from_union([from_none, from_str], self.rule_id)
        if self.rule_index is not None:
            result["ruleIndex"] = from_union([from_none, to_int], self.rule_index)
        if self.rule_name is not None:
            result["ruleName"] = from_union([from_none, from_str], self.rule_name)
        if self.scores is not None:
            result["scores"] = from_union([from_none, lambda x: to_class(FusionScores, x)], self.scores)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionFusionRouteFailedData:
    "Experimental durable HydraFusion routing failure and the deterministic concrete fallback selected for the turn."
    attempt_id: str
    fallback_model: str
    policy: str
    reason: str
    synthetic_model: str
    error_message: str | None = None
    routing_latency_ms: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionFusionRouteFailedData":
        assert isinstance(obj, dict)
        attempt_id = from_str(obj.get("attemptId"))
        fallback_model = from_str(obj.get("fallbackModel"))
        policy = from_str(obj.get("policy"))
        reason = from_str(obj.get("reason"))
        synthetic_model = from_str(obj.get("syntheticModel"))
        error_message = from_union([from_none, from_str], obj.get("errorMessage"))
        routing_latency_ms = from_union([from_none, from_float], obj.get("routingLatencyMs"))
        return SessionFusionRouteFailedData(
            attempt_id=attempt_id,
            fallback_model=fallback_model,
            policy=policy,
            reason=reason,
            synthetic_model=synthetic_model,
            error_message=error_message,
            routing_latency_ms=routing_latency_ms,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["attemptId"] = from_str(self.attempt_id)
        result["fallbackModel"] = from_str(self.fallback_model)
        result["policy"] = from_str(self.policy)
        result["reason"] = from_str(self.reason)
        result["syntheticModel"] = from_str(self.synthetic_model)
        if self.error_message is not None:
            result["errorMessage"] = from_union([from_none, from_str], self.error_message)
        if self.routing_latency_ms is not None:
            result["routingLatencyMs"] = from_union([from_none, to_float], self.routing_latency_ms)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionFusionRouteStartedData:
    "Experimental transient signal that HydraFusion routing has started for an eligible turn."
    attempt_id: str
    turn_kind: FusionTurnKind
    policy: str | None = None
    synthetic_model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionFusionRouteStartedData":
        assert isinstance(obj, dict)
        attempt_id = from_str(obj.get("attemptId"))
        turn_kind = parse_enum(FusionTurnKind, obj.get("turnKind"))
        policy = from_union([from_none, from_str], obj.get("policy"))
        synthetic_model = from_union([from_none, from_str], obj.get("syntheticModel"))
        return SessionFusionRouteStartedData(
            attempt_id=attempt_id,
            turn_kind=turn_kind,
            policy=policy,
            synthetic_model=synthetic_model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["attemptId"] = from_str(self.attempt_id)
        result["turnKind"] = to_enum(FusionTurnKind, self.turn_kind)
        if self.policy is not None:
            result["policy"] = from_union([from_none, from_str], self.policy)
        if self.synthetic_model is not None:
            result["syntheticModel"] = from_union([from_none, from_str], self.synthetic_model)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionManagedSettingsEnforcedData:
    "Runtime enforcement of enterprise managed settings: fires when the session blocks or caps a runtime action because enterprise policy governs it, so SDK clients can explain *why* an action was governed. Unlike `session.managed_settings_resolved` (which reports *what* is managed), this reports a concrete governed action — e.g. a user or host tried to turn on a bypass-permissions escalation while policy disables it. Emitted live (not persisted to the session event log) on user/host-initiated attempts only, never for silent policy application. Marked experimental while the managed-settings surface stabilizes."
    action: ManagedSettingsEnforcedAction
    fail_closed: bool
    message: str
    setting: str
    escalation: ManagedSettingsEnforcedEscalation | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionManagedSettingsEnforcedData":
        assert isinstance(obj, dict)
        action = parse_enum(ManagedSettingsEnforcedAction, obj.get("action"))
        fail_closed = from_bool(obj.get("failClosed"))
        message = from_str(obj.get("message"))
        setting = from_str(obj.get("setting"))
        escalation = from_union([from_none, lambda x: parse_enum(ManagedSettingsEnforcedEscalation, x)], obj.get("escalation"))
        return SessionManagedSettingsEnforcedData(
            action=action,
            fail_closed=fail_closed,
            message=message,
            setting=setting,
            escalation=escalation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["action"] = to_enum(ManagedSettingsEnforcedAction, self.action)
        result["failClosed"] = from_bool(self.fail_closed)
        result["message"] = from_str(self.message)
        result["setting"] = from_str(self.setting)
        if self.escalation is not None:
            result["escalation"] = from_union([from_none, lambda x: to_enum(ManagedSettingsEnforcedEscalation, x)], self.escalation)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionManagedSettingsResolvedData:
    "Enterprise managed-settings resolution: the effective managed settings the session applied and which channels contributed, so SDK clients can show users what is enterprise-managed. Fires whenever managed policy is (re)applied — at session start, on resume, and on account switch. This is an ephemeral live snapshot (delivered to subscribers but not persisted to the session event log), because at session start it resolves before `session.start` is emitted. Device values take precedence over server values per ordinary key, while permissions compose restrictively across device, server, and SDK-client layers. The account-scoped `getManagedSettings()` API does not include session-local client injection. Marked experimental while the managed-settings surface stabilizes."
    bypass_permissions_disabled: bool
    device_managed: bool
    fail_closed: bool
    managed_keys: list[str]
    server_managed: bool
    source: ManagedSettingsResolvedSource
    client_managed: bool | None = None
    permissions_allow_intersected: bool | None = None
    sandbox_enabled_by_undetermined_policy: bool | None = None
    settings: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionManagedSettingsResolvedData":
        assert isinstance(obj, dict)
        bypass_permissions_disabled = from_bool(obj.get("bypassPermissionsDisabled"))
        device_managed = from_bool(obj.get("deviceManaged"))
        fail_closed = from_bool(obj.get("failClosed"))
        managed_keys = from_list(from_str, obj.get("managedKeys"))
        server_managed = from_bool(obj.get("serverManaged"))
        source = parse_enum(ManagedSettingsResolvedSource, obj.get("source"))
        client_managed = from_union([from_none, from_bool], obj.get("clientManaged"))
        permissions_allow_intersected = from_union([from_none, from_bool], obj.get("permissionsAllowIntersected"))
        sandbox_enabled_by_undetermined_policy = from_union([from_none, from_bool], obj.get("sandboxEnabledByUndeterminedPolicy"))
        settings = obj.get("settings")
        return SessionManagedSettingsResolvedData(
            bypass_permissions_disabled=bypass_permissions_disabled,
            device_managed=device_managed,
            fail_closed=fail_closed,
            managed_keys=managed_keys,
            server_managed=server_managed,
            source=source,
            client_managed=client_managed,
            permissions_allow_intersected=permissions_allow_intersected,
            sandbox_enabled_by_undetermined_policy=sandbox_enabled_by_undetermined_policy,
            settings=settings,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["bypassPermissionsDisabled"] = from_bool(self.bypass_permissions_disabled)
        result["deviceManaged"] = from_bool(self.device_managed)
        result["failClosed"] = from_bool(self.fail_closed)
        result["managedKeys"] = from_list(from_str, self.managed_keys)
        result["serverManaged"] = from_bool(self.server_managed)
        result["source"] = to_enum(ManagedSettingsResolvedSource, self.source)
        if self.client_managed is not None:
            result["clientManaged"] = from_union([from_none, from_bool], self.client_managed)
        if self.permissions_allow_intersected is not None:
            result["permissionsAllowIntersected"] = from_union([from_none, from_bool], self.permissions_allow_intersected)
        if self.sandbox_enabled_by_undetermined_policy is not None:
            result["sandboxEnabledByUndeterminedPolicy"] = from_union([from_none, from_bool], self.sandbox_enabled_by_undetermined_policy)
        if self.settings is not None:
            result["settings"] = self.settings
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class SessionPermissionsChangedData:
    "Permission-mode transition details."
    # Experimental: this field is part of an experimental API and may change or be removed.
    mode: PermissionMode
    # Experimental: this field is part of an experimental API and may change or be removed.
    previous_mode: PermissionMode
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval_model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionPermissionsChangedData":
        assert isinstance(obj, dict)
        mode = parse_enum(PermissionMode, obj.get("mode"))
        previous_mode = parse_enum(PermissionMode, obj.get("previousMode"))
        assisted_approval_model = from_union([from_none, from_str], obj.get("assistedApprovalModel"))
        return SessionPermissionsChangedData(
            mode=mode,
            previous_mode=previous_mode,
            assisted_approval_model=assisted_approval_model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["mode"] = to_enum(PermissionMode, self.mode)
        result["previousMode"] = to_enum(PermissionMode, self.previous_mode)
        if self.assisted_approval_model is not None:
            result["assistedApprovalModel"] = from_union([from_none, from_str], self.assisted_approval_model)
        return result


# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class UiEphemeralQueryData:
    "Ordered output and terminal state for a transient query that does not modify conversation history."
    phase: UIEphemeralQueryPhase
    request_id: str
    answer: str | None = None
    chunk: str | None = None
    error: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UiEphemeralQueryData":
        assert isinstance(obj, dict)
        phase = parse_enum(UIEphemeralQueryPhase, obj.get("phase"))
        request_id = from_str(obj.get("requestId"))
        answer = from_union([from_none, from_str], obj.get("answer"))
        chunk = from_union([from_none, from_str], obj.get("chunk"))
        error = from_union([from_none, from_str], obj.get("error"))
        return UiEphemeralQueryData(
            phase=phase,
            request_id=request_id,
            answer=answer,
            chunk=chunk,
            error=error,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["phase"] = to_enum(UIEphemeralQueryPhase, self.phase)
        result["requestId"] = from_str(self.request_id)
        if self.answer is not None:
            result["answer"] = from_union([from_none, from_str], self.answer)
        if self.chunk is not None:
            result["chunk"] = from_union([from_none, from_str], self.chunk)
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        return result


@dataclass
class AbortData:
    "Turn abort information including the reason for termination"
    reason: AbortReason

    @staticmethod
    def from_dict(obj: Any) -> "AbortData":
        assert isinstance(obj, dict)
        reason = parse_enum(AbortReason, obj.get("reason"))
        return AbortData(
            reason=reason,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["reason"] = to_enum(AbortReason, self.reason)
        return result


@dataclass
class AgentInterruptedData:
    "Metadata for work the user interrupted while the agent was running"
    activity: AgentInterruptedActivity
    elapsed: timedelta
    turn: int
    api_endpoint: str | None = None
    cancel_phase: AgentInterruptedCancelPhase | None = None
    interrupted_agent_count: int | None = None
    model: str | None = None
    output_ttft: timedelta | None = None
    reasoning_effort: str | None = None
    safe_tool_names: list[str] | None = None
    tool_call_ids: list[str] | None = None
    tool_names: list[str] | None = None
    transport: ModelCallFailureTransport | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AgentInterruptedData":
        assert isinstance(obj, dict)
        activity = parse_enum(AgentInterruptedActivity, obj.get("activity"))
        elapsed = from_timedelta(obj.get("elapsedMs"))
        turn = from_int(obj.get("turn"))
        api_endpoint = from_union([from_none, from_str], obj.get("apiEndpoint"))
        cancel_phase = from_union([from_none, lambda x: parse_enum(AgentInterruptedCancelPhase, x)], obj.get("cancelPhase"))
        interrupted_agent_count = from_union([from_none, from_int], obj.get("interruptedAgentCount"))
        model = from_union([from_none, from_str], obj.get("model"))
        output_ttft = from_union([from_none, from_timedelta], obj.get("outputTtftMs"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        safe_tool_names = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("safeToolNames"))
        tool_call_ids = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolCallIds"))
        tool_names = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolNames"))
        transport = from_union([from_none, lambda x: parse_enum(ModelCallFailureTransport, x)], obj.get("transport"))
        return AgentInterruptedData(
            activity=activity,
            elapsed=elapsed,
            turn=turn,
            api_endpoint=api_endpoint,
            cancel_phase=cancel_phase,
            interrupted_agent_count=interrupted_agent_count,
            model=model,
            output_ttft=output_ttft,
            reasoning_effort=reasoning_effort,
            safe_tool_names=safe_tool_names,
            tool_call_ids=tool_call_ids,
            tool_names=tool_names,
            transport=transport,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["activity"] = to_enum(AgentInterruptedActivity, self.activity)
        result["elapsedMs"] = to_timedelta(self.elapsed)
        result["turn"] = to_int(self.turn)
        if self.api_endpoint is not None:
            result["apiEndpoint"] = from_union([from_none, from_str], self.api_endpoint)
        if self.cancel_phase is not None:
            result["cancelPhase"] = from_union([from_none, lambda x: to_enum(AgentInterruptedCancelPhase, x)], self.cancel_phase)
        if self.interrupted_agent_count is not None:
            result["interruptedAgentCount"] = from_union([from_none, to_int], self.interrupted_agent_count)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.output_ttft is not None:
            result["outputTtftMs"] = from_union([from_none, to_timedelta], self.output_ttft)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.safe_tool_names is not None:
            result["safeToolNames"] = from_union([from_none, lambda x: from_list(from_str, x)], self.safe_tool_names)
        if self.tool_call_ids is not None:
            result["toolCallIds"] = from_union([from_none, lambda x: from_list(from_str, x)], self.tool_call_ids)
        if self.tool_names is not None:
            result["toolNames"] = from_union([from_none, lambda x: from_list(from_str, x)], self.tool_names)
        if self.transport is not None:
            result["transport"] = from_union([from_none, lambda x: to_enum(ModelCallFailureTransport, x)], self.transport)
        return result


@dataclass
class AssistantIdleData:
    "Payload emitted whenever the main agent's processing loop goes idle, including while related background work (running agents or in-flight attached shell commands) is still pending and the session-level idle event is therefore deferred"
    aborted: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantIdleData":
        assert isinstance(obj, dict)
        aborted = from_union([from_none, from_bool], obj.get("aborted"))
        return AssistantIdleData(
            aborted=aborted,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.aborted is not None:
            result["aborted"] = from_union([from_none, from_bool], self.aborted)
        return result


@dataclass
class AssistantIntentData:
    "Agent intent description for current activity or plan"
    intent: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantIntentData":
        assert isinstance(obj, dict)
        intent = from_str(obj.get("intent"))
        return AssistantIntentData(
            intent=intent,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intent"] = from_str(self.intent)
        return result


@dataclass
class AssistantMessageData:
    "Assistant response containing text content, optional tool requests, and interaction metadata"
    content: str
    message_id: str
    api_call_id: str | None = None
    chunk_count: int | None = None
    chunk_index: int | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    citations: Citations | None = None
    client_request_id: str | None = None
    encrypted_content: str | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    fusion: FusionAttribution | None = None
    interaction_id: str | None = None
    model: str | None = None
    output_tokens: int | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    phase: str | None = None
    reasoning_blocks: AssistantMessageReasoningBlocks | None = None
    reasoning_opaque: str | None = None
    reasoning_text: str | None = None
    reasoning_wire_field: str | None = None
    request_id: str | None = None
    rte: bool | None = None
    server_tools: AssistantMessageServerTools | None = None
    service_request_id: str | None = None
    tool_requests: list[AssistantMessageToolRequest] | None = None
    turn_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        message_id = from_str(obj.get("messageId"))
        api_call_id = from_union([from_none, from_str], obj.get("apiCallId"))
        chunk_count = from_union([from_none, from_int], obj.get("chunkCount"))
        chunk_index = from_union([from_none, from_int], obj.get("chunkIndex"))
        citations = from_union([from_none, Citations.from_dict], obj.get("citations"))
        client_request_id = from_union([from_none, from_str], obj.get("clientRequestId"))
        encrypted_content = from_union([from_none, from_str], obj.get("encryptedContent"))
        fusion = from_union([from_none, FusionAttribution.from_dict], obj.get("fusion"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        model = from_union([from_none, from_str], obj.get("model"))
        output_tokens = from_union([from_none, from_int], obj.get("outputTokens"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        phase = from_union([from_none, from_str], obj.get("phase"))
        reasoning_blocks = from_union([from_none, AssistantMessageReasoningBlocks.from_dict], obj.get("reasoningBlocks"))
        reasoning_opaque = from_union([from_none, from_str], obj.get("reasoningOpaque"))
        reasoning_text = from_union([from_none, from_str], obj.get("reasoningText"))
        reasoning_wire_field = from_union([from_none, from_str], obj.get("reasoningWireField"))
        request_id = from_union([from_none, from_str], obj.get("requestId"))
        rte = from_union([from_none, from_bool], obj.get("rte"))
        server_tools = from_union([from_none, AssistantMessageServerTools.from_dict], obj.get("serverTools"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        tool_requests = from_union([from_none, lambda x: from_list(AssistantMessageToolRequest.from_dict, x)], obj.get("toolRequests"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return AssistantMessageData(
            content=content,
            message_id=message_id,
            api_call_id=api_call_id,
            chunk_count=chunk_count,
            chunk_index=chunk_index,
            citations=citations,
            client_request_id=client_request_id,
            encrypted_content=encrypted_content,
            fusion=fusion,
            interaction_id=interaction_id,
            model=model,
            output_tokens=output_tokens,
            parent_tool_call_id=parent_tool_call_id,
            phase=phase,
            reasoning_blocks=reasoning_blocks,
            reasoning_opaque=reasoning_opaque,
            reasoning_text=reasoning_text,
            reasoning_wire_field=reasoning_wire_field,
            request_id=request_id,
            rte=rte,
            server_tools=server_tools,
            service_request_id=service_request_id,
            tool_requests=tool_requests,
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["messageId"] = from_str(self.message_id)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_none, from_str], self.api_call_id)
        if self.chunk_count is not None:
            result["chunkCount"] = from_union([from_none, to_int], self.chunk_count)
        if self.chunk_index is not None:
            result["chunkIndex"] = from_union([from_none, to_int], self.chunk_index)
        if self.citations is not None:
            result["citations"] = from_union([from_none, lambda x: to_class(Citations, x)], self.citations)
        if self.client_request_id is not None:
            result["clientRequestId"] = from_union([from_none, from_str], self.client_request_id)
        if self.encrypted_content is not None:
            result["encryptedContent"] = from_union([from_none, from_str], self.encrypted_content)
        if self.fusion is not None:
            result["fusion"] = from_union([from_none, lambda x: to_class(FusionAttribution, x)], self.fusion)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([from_none, to_int], self.output_tokens)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.phase is not None:
            result["phase"] = from_union([from_none, from_str], self.phase)
        if self.reasoning_blocks is not None:
            result["reasoningBlocks"] = from_union([from_none, lambda x: to_class(AssistantMessageReasoningBlocks, x)], self.reasoning_blocks)
        if self.reasoning_opaque is not None:
            result["reasoningOpaque"] = from_union([from_none, from_str], self.reasoning_opaque)
        if self.reasoning_text is not None:
            result["reasoningText"] = from_union([from_none, from_str], self.reasoning_text)
        if self.reasoning_wire_field is not None:
            result["reasoningWireField"] = from_union([from_none, from_str], self.reasoning_wire_field)
        if self.request_id is not None:
            result["requestId"] = from_union([from_none, from_str], self.request_id)
        if self.rte is not None:
            result["rte"] = from_union([from_none, from_bool], self.rte)
        if self.server_tools is not None:
            result["serverTools"] = from_union([from_none, lambda x: to_class(AssistantMessageServerTools, x)], self.server_tools)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.tool_requests is not None:
            result["toolRequests"] = from_union([from_none, lambda x: from_list(lambda x: to_class(AssistantMessageToolRequest, x), x)], self.tool_requests)
        if self.turn_id is not None:
            result["turnId"] = from_union([from_none, from_str], self.turn_id)
        return result


@dataclass
class AssistantMessageDeltaData:
    "Streaming assistant message delta for incremental response updates"
    delta_content: str
    message_id: str
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageDeltaData":
        assert isinstance(obj, dict)
        delta_content = from_str(obj.get("deltaContent"))
        message_id = from_str(obj.get("messageId"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return AssistantMessageDeltaData(
            delta_content=delta_content,
            message_id=message_id,
            parent_tool_call_id=parent_tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["deltaContent"] = from_str(self.delta_content)
        result["messageId"] = from_str(self.message_id)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        return result


@dataclass
class AssistantMessageStartData:
    "Streaming assistant message start metadata"
    message_id: str
    phase: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageStartData":
        assert isinstance(obj, dict)
        message_id = from_str(obj.get("messageId"))
        phase = from_union([from_none, from_str], obj.get("phase"))
        return AssistantMessageStartData(
            message_id=message_id,
            phase=phase,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["messageId"] = from_str(self.message_id)
        if self.phase is not None:
            result["phase"] = from_union([from_none, from_str], self.phase)
        return result


@dataclass
class AssistantMessageToolRequest:
    "A tool invocation request from the assistant"
    name: str
    tool_call_id: str
    arguments: Any = None
    caller: AssistantMessageToolRequestCaller | None = None
    intention_summary: str | None = None
    mcp_server_name: str | None = None
    mcp_tool_name: str | None = None
    tool_title: str | None = None
    type: AssistantMessageToolRequestType | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageToolRequest":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        tool_call_id = from_str(obj.get("toolCallId"))
        arguments = obj.get("arguments")
        caller = from_union([from_none, AssistantMessageToolRequestCaller.from_dict], obj.get("caller"))
        intention_summary = from_union([from_none, from_str], obj.get("intentionSummary"))
        mcp_server_name = from_union([from_none, from_str], obj.get("mcpServerName"))
        mcp_tool_name = from_union([from_none, from_str], obj.get("mcpToolName"))
        tool_title = from_union([from_none, from_str], obj.get("toolTitle"))
        type = from_union([from_none, lambda x: parse_enum(AssistantMessageToolRequestType, x)], obj.get("type"))
        return AssistantMessageToolRequest(
            name=name,
            tool_call_id=tool_call_id,
            arguments=arguments,
            caller=caller,
            intention_summary=intention_summary,
            mcp_server_name=mcp_server_name,
            mcp_tool_name=mcp_tool_name,
            tool_title=tool_title,
            type=type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.caller is not None:
            result["caller"] = from_union([from_none, lambda x: to_class(AssistantMessageToolRequestCaller, x)], self.caller)
        if self.intention_summary is not None:
            result["intentionSummary"] = from_union([from_none, from_str], self.intention_summary)
        if self.mcp_server_name is not None:
            result["mcpServerName"] = from_union([from_none, from_str], self.mcp_server_name)
        if self.mcp_tool_name is not None:
            result["mcpToolName"] = from_union([from_none, from_str], self.mcp_tool_name)
        if self.tool_title is not None:
            result["toolTitle"] = from_union([from_none, from_str], self.tool_title)
        if self.type is not None:
            result["type"] = from_union([from_none, lambda x: to_enum(AssistantMessageToolRequestType, x)], self.type)
        return result


@dataclass
class AssistantMessageToolRequestCaller:
    "Hosted program that requested this client tool call"
    caller_id: str
    type: AssistantMessageToolRequestCallerType

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageToolRequestCaller":
        assert isinstance(obj, dict)
        caller_id = from_str(obj.get("callerId"))
        type = parse_enum(AssistantMessageToolRequestCallerType, obj.get("type"))
        return AssistantMessageToolRequestCaller(
            caller_id=caller_id,
            type=type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["callerId"] = from_str(self.caller_id)
        result["type"] = to_enum(AssistantMessageToolRequestCallerType, self.type)
        return result


@dataclass
class AssistantReasoningData:
    "Assistant reasoning content for timeline display with complete thinking text"
    content: str
    reasoning_id: str
    rte: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantReasoningData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        reasoning_id = from_str(obj.get("reasoningId"))
        rte = from_union([from_none, from_bool], obj.get("rte"))
        return AssistantReasoningData(
            content=content,
            reasoning_id=reasoning_id,
            rte=rte,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["reasoningId"] = from_str(self.reasoning_id)
        if self.rte is not None:
            result["rte"] = from_union([from_none, from_bool], self.rte)
        return result


@dataclass
class AssistantReasoningDeltaData:
    "Streaming reasoning delta for incremental extended thinking updates"
    delta_content: str
    reasoning_id: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantReasoningDeltaData":
        assert isinstance(obj, dict)
        delta_content = from_str(obj.get("deltaContent"))
        reasoning_id = from_str(obj.get("reasoningId"))
        return AssistantReasoningDeltaData(
            delta_content=delta_content,
            reasoning_id=reasoning_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["deltaContent"] = from_str(self.delta_content)
        result["reasoningId"] = from_str(self.reasoning_id)
        return result


@dataclass
class AssistantServerToolProgressData:
    "Live progress signal for a provider-hosted server tool (e.g. hosted web search) while it runs, before the finalized serverTools envelope lands on the terminal assistant.message"
    kind: str
    output_index: int
    status: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantServerToolProgressData":
        assert isinstance(obj, dict)
        kind = from_str(obj.get("kind"))
        output_index = from_int(obj.get("outputIndex"))
        status = from_str(obj.get("status"))
        return AssistantServerToolProgressData(
            kind=kind,
            output_index=output_index,
            status=status,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = from_str(self.kind)
        result["outputIndex"] = to_int(self.output_index)
        result["status"] = from_str(self.status)
        return result


@dataclass
class AssistantStreamingDeltaData:
    "Streaming response progress with cumulative byte count"
    total_response_size_bytes: int

    @staticmethod
    def from_dict(obj: Any) -> "AssistantStreamingDeltaData":
        assert isinstance(obj, dict)
        total_response_size_bytes = from_int(obj.get("totalResponseSizeBytes"))
        return AssistantStreamingDeltaData(
            total_response_size_bytes=total_response_size_bytes,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["totalResponseSizeBytes"] = to_int(self.total_response_size_bytes)
        return result


@dataclass
class AssistantToolCallDeltaData:
    "Streaming tool-call input delta for incremental tool-call updates"
    input_delta: str
    tool_call_id: str
    tool_name: str | None = None
    tool_type: AssistantMessageToolRequestType | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantToolCallDeltaData":
        assert isinstance(obj, dict)
        input_delta = from_str(obj.get("inputDelta"))
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        tool_type = from_union([from_none, lambda x: parse_enum(AssistantMessageToolRequestType, x)], obj.get("toolType"))
        return AssistantToolCallDeltaData(
            input_delta=input_delta,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            tool_type=tool_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["inputDelta"] = from_str(self.input_delta)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.tool_name is not None:
            result["toolName"] = from_union([from_none, from_str], self.tool_name)
        if self.tool_type is not None:
            result["toolType"] = from_union([from_none, lambda x: to_enum(AssistantMessageToolRequestType, x)], self.tool_type)
        return result


@dataclass
class AssistantTurnEndData:
    "Turn completion metadata including the turn identifier"
    turn_id: str
    model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantTurnEndData":
        assert isinstance(obj, dict)
        turn_id = from_str(obj.get("turnId"))
        model = from_union([from_none, from_str], obj.get("model"))
        return AssistantTurnEndData(
            turn_id=turn_id,
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["turnId"] = from_str(self.turn_id)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        return result


@dataclass
class AssistantTurnRetryData:
    "Metadata for an additional model inference attempt within an existing assistant turn"
    turn_id: str
    model: str | None = None
    reason: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantTurnRetryData":
        assert isinstance(obj, dict)
        turn_id = from_str(obj.get("turnId"))
        model = from_union([from_none, from_str], obj.get("model"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        return AssistantTurnRetryData(
            turn_id=turn_id,
            model=model,
            reason=reason,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["turnId"] = from_str(self.turn_id)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        return result


@dataclass
class AssistantTurnStartData:
    "Turn initialization metadata including identifier and interaction tracking"
    turn_id: str
    interaction_id: str | None = None
    model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantTurnStartData":
        assert isinstance(obj, dict)
        turn_id = from_str(obj.get("turnId"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        model = from_union([from_none, from_str], obj.get("model"))
        return AssistantTurnStartData(
            turn_id=turn_id,
            interaction_id=interaction_id,
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["turnId"] = from_str(self.turn_id)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        return result


@dataclass
class AssistantUsageCopilotUsage:
    "Per-request cost and usage data from the CAPI copilot_usage response field"
    total_nano_aiu: float
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _token_details: list[AssistantUsageCopilotUsageTokenDetail] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageCopilotUsage":
        assert isinstance(obj, dict)
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        _token_details = from_union([from_none, lambda x: from_list(AssistantUsageCopilotUsageTokenDetail.from_dict, x)], obj.get("tokenDetails"))
        return AssistantUsageCopilotUsage(
            total_nano_aiu=total_nano_aiu,
            _token_details=_token_details,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        if self._token_details is not None:
            result["tokenDetails"] = from_union([from_none, lambda x: from_list(lambda x: to_class(AssistantUsageCopilotUsageTokenDetail, x), x)], self._token_details)
        return result


@dataclass
class AssistantUsageCopilotUsageTokenDetail:
    "Token usage detail for a single billing category"
    batch_size: int
    cost_per_batch: int
    token_count: int
    token_type: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageCopilotUsageTokenDetail":
        assert isinstance(obj, dict)
        batch_size = from_int(obj.get("batchSize"))
        cost_per_batch = from_int(obj.get("costPerBatch"))
        token_count = from_int(obj.get("tokenCount"))
        token_type = from_str(obj.get("tokenType"))
        return AssistantUsageCopilotUsageTokenDetail(
            batch_size=batch_size,
            cost_per_batch=cost_per_batch,
            token_count=token_count,
            token_type=token_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["batchSize"] = to_int(self.batch_size)
        result["costPerBatch"] = to_int(self.cost_per_batch)
        result["tokenCount"] = to_int(self.token_count)
        result["tokenType"] = from_str(self.token_type)
        return result


@dataclass
class AssistantUsageData:
    "LLM API call usage metrics including tokens, costs, quotas, and billing information"
    model: str
    accepted_prediction_tokens: int | None = None
    api_call_id: str | None = None
    api_endpoint: AssistantUsageApiEndpoint | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _available_tool_count: int | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _cache_details_reported: bool | None = None
    cache_expires_at: datetime | None = None
    cache_read_tokens: int | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _cache_ttl_seconds: int | None = None
    cache_write_tokens: int | None = None
    content_filter_triggered: bool | None = None
    copilot_usage: AssistantUsageCopilotUsage | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    cost: float | None = None
    duration: timedelta | None = None
    finish_reason: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _frontier_source: str | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    fusion: FusionAttribution | None = None
    initiator: str | None = None
    input_tokens: int | None = None
    interaction_type: str | None = None
    inter_token_latency: timedelta | None = None
    is_auto: bool | None = None
    is_byok: bool | None = None
    max_output_tokens: int | None = None
    max_prompt_tokens: int | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _num_tool_calls: int | None = None
    output_tokens: int | None = None
    output_ttft: timedelta | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    provider_call_id: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _quota_snapshots: dict[str, _AssistantUsageQuotaSnapshot] | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None
    reasoning_tokens: int | None = None
    rejected_prediction_tokens: int | None = None
    rte: bool | None = None
    service_request_id: str | None = None
    time_to_first_token: timedelta | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tool_counts: dict[str, int] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tool_token_count: int | None = None
    transport: AssistantUsageTransport | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageData":
        assert isinstance(obj, dict)
        model = from_str(obj.get("model"))
        accepted_prediction_tokens = from_union([from_none, from_int], obj.get("acceptedPredictionTokens"))
        api_call_id = from_union([from_none, from_str], obj.get("apiCallId"))
        api_endpoint = from_union([from_none, lambda x: parse_enum(AssistantUsageApiEndpoint, x)], obj.get("apiEndpoint"))
        _available_tool_count = from_union([from_none, from_int], obj.get("availableToolCount"))
        _cache_details_reported = from_union([from_none, from_bool], obj.get("cacheDetailsReported"))
        cache_expires_at = from_union([from_none, from_datetime], obj.get("cacheExpiresAt"))
        cache_read_tokens = from_union([from_none, from_int], obj.get("cacheReadTokens"))
        _cache_ttl_seconds = from_union([from_none, from_int], obj.get("cacheTtlSeconds"))
        cache_write_tokens = from_union([from_none, from_int], obj.get("cacheWriteTokens"))
        content_filter_triggered = from_union([from_none, from_bool], obj.get("contentFilterTriggered"))
        copilot_usage = from_union([from_none, AssistantUsageCopilotUsage.from_dict], obj.get("copilotUsage"))
        cost = from_union([from_none, from_float], obj.get("cost"))
        duration = from_union([from_none, from_timedelta], obj.get("duration"))
        finish_reason = from_union([from_none, from_str], obj.get("finishReason"))
        _frontier_source = from_union([from_none, from_str], obj.get("frontierSource"))
        fusion = from_union([from_none, FusionAttribution.from_dict], obj.get("fusion"))
        initiator = from_union([from_none, from_str], obj.get("initiator"))
        input_tokens = from_union([from_none, from_int], obj.get("inputTokens"))
        interaction_type = from_union([from_none, from_str], obj.get("interactionType"))
        inter_token_latency = from_union([from_none, from_timedelta], obj.get("interTokenLatencyMs"))
        is_auto = from_union([from_none, from_bool], obj.get("isAuto"))
        is_byok = from_union([from_none, from_bool], obj.get("isByok"))
        max_output_tokens = from_union([from_none, from_int], obj.get("maxOutputTokens"))
        max_prompt_tokens = from_union([from_none, from_int], obj.get("maxPromptTokens"))
        _num_tool_calls = from_union([from_none, from_int], obj.get("numToolCalls"))
        output_tokens = from_union([from_none, from_int], obj.get("outputTokens"))
        output_ttft = from_union([from_none, from_timedelta], obj.get("outputTtftMs"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        _quota_snapshots = from_union([from_none, lambda x: from_dict(_AssistantUsageQuotaSnapshot.from_dict, x)], obj.get("quotaSnapshots"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        reasoning_tokens = from_union([from_none, from_int], obj.get("reasoningTokens"))
        rejected_prediction_tokens = from_union([from_none, from_int], obj.get("rejectedPredictionTokens"))
        rte = from_union([from_none, from_bool], obj.get("rte"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        time_to_first_token = from_union([from_none, from_timedelta], obj.get("timeToFirstTokenMs"))
        _tool_counts = from_union([from_none, lambda x: from_dict(from_int, x)], obj.get("toolCounts"))
        _tool_token_count = from_union([from_none, from_int], obj.get("toolTokenCount"))
        transport = from_union([from_none, lambda x: parse_enum(AssistantUsageTransport, x)], obj.get("transport"))
        return AssistantUsageData(
            model=model,
            accepted_prediction_tokens=accepted_prediction_tokens,
            api_call_id=api_call_id,
            api_endpoint=api_endpoint,
            _available_tool_count=_available_tool_count,
            _cache_details_reported=_cache_details_reported,
            cache_expires_at=cache_expires_at,
            cache_read_tokens=cache_read_tokens,
            _cache_ttl_seconds=_cache_ttl_seconds,
            cache_write_tokens=cache_write_tokens,
            content_filter_triggered=content_filter_triggered,
            copilot_usage=copilot_usage,
            cost=cost,
            duration=duration,
            finish_reason=finish_reason,
            _frontier_source=_frontier_source,
            fusion=fusion,
            initiator=initiator,
            input_tokens=input_tokens,
            interaction_type=interaction_type,
            inter_token_latency=inter_token_latency,
            is_auto=is_auto,
            is_byok=is_byok,
            max_output_tokens=max_output_tokens,
            max_prompt_tokens=max_prompt_tokens,
            _num_tool_calls=_num_tool_calls,
            output_tokens=output_tokens,
            output_ttft=output_ttft,
            parent_tool_call_id=parent_tool_call_id,
            provider_call_id=provider_call_id,
            _quota_snapshots=_quota_snapshots,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
            reasoning_tokens=reasoning_tokens,
            rejected_prediction_tokens=rejected_prediction_tokens,
            rte=rte,
            service_request_id=service_request_id,
            time_to_first_token=time_to_first_token,
            _tool_counts=_tool_counts,
            _tool_token_count=_tool_token_count,
            transport=transport,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["model"] = from_str(self.model)
        if self.accepted_prediction_tokens is not None:
            result["acceptedPredictionTokens"] = from_union([from_none, to_int], self.accepted_prediction_tokens)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_none, from_str], self.api_call_id)
        if self.api_endpoint is not None:
            result["apiEndpoint"] = from_union([from_none, lambda x: to_enum(AssistantUsageApiEndpoint, x)], self.api_endpoint)
        if self._available_tool_count is not None:
            result["availableToolCount"] = from_union([from_none, to_int], self._available_tool_count)
        if self._cache_details_reported is not None:
            result["cacheDetailsReported"] = from_union([from_none, from_bool], self._cache_details_reported)
        if self.cache_expires_at is not None:
            result["cacheExpiresAt"] = from_union([from_none, to_datetime], self.cache_expires_at)
        if self.cache_read_tokens is not None:
            result["cacheReadTokens"] = from_union([from_none, to_int], self.cache_read_tokens)
        if self._cache_ttl_seconds is not None:
            result["cacheTtlSeconds"] = from_union([from_none, to_int], self._cache_ttl_seconds)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([from_none, to_int], self.cache_write_tokens)
        if self.content_filter_triggered is not None:
            result["contentFilterTriggered"] = from_union([from_none, from_bool], self.content_filter_triggered)
        if self.copilot_usage is not None:
            result["copilotUsage"] = from_union([from_none, lambda x: to_class(AssistantUsageCopilotUsage, x)], self.copilot_usage)
        if self.cost is not None:
            result["cost"] = from_union([from_none, to_float], self.cost)
        if self.duration is not None:
            result["duration"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.finish_reason is not None:
            result["finishReason"] = from_union([from_none, from_str], self.finish_reason)
        if self._frontier_source is not None:
            result["frontierSource"] = from_union([from_none, from_str], self._frontier_source)
        if self.fusion is not None:
            result["fusion"] = from_union([from_none, lambda x: to_class(FusionAttribution, x)], self.fusion)
        if self.initiator is not None:
            result["initiator"] = from_union([from_none, from_str], self.initiator)
        if self.input_tokens is not None:
            result["inputTokens"] = from_union([from_none, to_int], self.input_tokens)
        if self.interaction_type is not None:
            result["interactionType"] = from_union([from_none, from_str], self.interaction_type)
        if self.inter_token_latency is not None:
            result["interTokenLatencyMs"] = from_union([from_none, to_timedelta], self.inter_token_latency)
        if self.is_auto is not None:
            result["isAuto"] = from_union([from_none, from_bool], self.is_auto)
        if self.is_byok is not None:
            result["isByok"] = from_union([from_none, from_bool], self.is_byok)
        if self.max_output_tokens is not None:
            result["maxOutputTokens"] = from_union([from_none, to_int], self.max_output_tokens)
        if self.max_prompt_tokens is not None:
            result["maxPromptTokens"] = from_union([from_none, to_int], self.max_prompt_tokens)
        if self._num_tool_calls is not None:
            result["numToolCalls"] = from_union([from_none, to_int], self._num_tool_calls)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([from_none, to_int], self.output_tokens)
        if self.output_ttft is not None:
            result["outputTtftMs"] = from_union([from_none, to_timedelta], self.output_ttft)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self._quota_snapshots is not None:
            result["quotaSnapshots"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(_AssistantUsageQuotaSnapshot, x), x)], self._quota_snapshots)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
        if self.reasoning_tokens is not None:
            result["reasoningTokens"] = from_union([from_none, to_int], self.reasoning_tokens)
        if self.rejected_prediction_tokens is not None:
            result["rejectedPredictionTokens"] = from_union([from_none, to_int], self.rejected_prediction_tokens)
        if self.rte is not None:
            result["rte"] = from_union([from_none, from_bool], self.rte)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.time_to_first_token is not None:
            result["timeToFirstTokenMs"] = from_union([from_none, to_timedelta], self.time_to_first_token)
        if self._tool_counts is not None:
            result["toolCounts"] = from_union([from_none, lambda x: from_dict(to_int, x)], self._tool_counts)
        if self._tool_token_count is not None:
            result["toolTokenCount"] = from_union([from_none, to_int], self._tool_token_count)
        if self.transport is not None:
            result["transport"] = from_union([from_none, lambda x: to_enum(AssistantUsageTransport, x)], self.transport)
        return result


@dataclass
class _AssistantUsageQuotaSnapshot:
    "Internal per-quota snapshot for assistant usage, including entitlement, consumed requests, overage, reset date, and remaining quota."
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _entitlement_requests: int
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _is_unlimited_entitlement: bool
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _overage: float
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _overage_allowed_with_exhausted_quota: bool
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _remaining_percentage: float
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _usage_allowed_with_exhausted_quota: bool
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _used_requests: int
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _has_quota: bool | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _overage_entitlement: float | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _reset_date: datetime | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _token_based_billing: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "_AssistantUsageQuotaSnapshot":
        assert isinstance(obj, dict)
        _entitlement_requests = from_int(obj.get("entitlementRequests"))
        _is_unlimited_entitlement = from_bool(obj.get("isUnlimitedEntitlement"))
        _overage = from_float(obj.get("overage"))
        _overage_allowed_with_exhausted_quota = from_bool(obj.get("overageAllowedWithExhaustedQuota"))
        _remaining_percentage = from_float(obj.get("remainingPercentage"))
        _usage_allowed_with_exhausted_quota = from_bool(obj.get("usageAllowedWithExhaustedQuota"))
        _used_requests = from_int(obj.get("usedRequests"))
        _has_quota = from_union([from_none, from_bool], obj.get("hasQuota"))
        _overage_entitlement = from_union([from_none, from_float], obj.get("overageEntitlement"))
        _reset_date = from_union([from_none, from_datetime], obj.get("resetDate"))
        _token_based_billing = from_union([from_none, from_bool], obj.get("tokenBasedBilling"))
        return _AssistantUsageQuotaSnapshot(
            _entitlement_requests=_entitlement_requests,
            _is_unlimited_entitlement=_is_unlimited_entitlement,
            _overage=_overage,
            _overage_allowed_with_exhausted_quota=_overage_allowed_with_exhausted_quota,
            _remaining_percentage=_remaining_percentage,
            _usage_allowed_with_exhausted_quota=_usage_allowed_with_exhausted_quota,
            _used_requests=_used_requests,
            _has_quota=_has_quota,
            _overage_entitlement=_overage_entitlement,
            _reset_date=_reset_date,
            _token_based_billing=_token_based_billing,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["entitlementRequests"] = to_int(self._entitlement_requests)
        result["isUnlimitedEntitlement"] = from_bool(self._is_unlimited_entitlement)
        result["overage"] = to_float(self._overage)
        result["overageAllowedWithExhaustedQuota"] = from_bool(self._overage_allowed_with_exhausted_quota)
        result["remainingPercentage"] = to_float(self._remaining_percentage)
        result["usageAllowedWithExhaustedQuota"] = from_bool(self._usage_allowed_with_exhausted_quota)
        result["usedRequests"] = to_int(self._used_requests)
        if self._has_quota is not None:
            result["hasQuota"] = from_union([from_none, from_bool], self._has_quota)
        if self._overage_entitlement is not None:
            result["overageEntitlement"] = from_union([from_none, to_float], self._overage_entitlement)
        if self._reset_date is not None:
            result["resetDate"] = from_union([from_none, to_datetime], self._reset_date)
        if self._token_based_billing is not None:
            result["tokenBasedBilling"] = from_union([from_none, from_bool], self._token_based_billing)
        return result


@dataclass
class AttachmentBlob:
    "Blob attachment with inline base64-encoded data"
    mime_type: str
    type: ClassVar[str] = "blob"
    asset_id: str | None = None
    byte_length: int | None = None
    data: str | None = None
    display_name: str | None = None
    omitted_reason: OmittedBinaryOmittedReason | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentBlob":
        assert isinstance(obj, dict)
        mime_type = from_str(obj.get("mimeType"))
        asset_id = from_union([from_none, from_str], obj.get("assetId"))
        byte_length = from_union([from_none, from_int], obj.get("byteLength"))
        data = from_union([from_none, from_str], obj.get("data"))
        display_name = from_union([from_none, from_str], obj.get("displayName"))
        omitted_reason = from_union([from_none, lambda x: parse_enum(OmittedBinaryOmittedReason, x)], obj.get("omittedReason"))
        return AttachmentBlob(
            mime_type=mime_type,
            asset_id=asset_id,
            byte_length=byte_length,
            data=data,
            display_name=display_name,
            omitted_reason=omitted_reason,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        if self.asset_id is not None:
            result["assetId"] = from_union([from_none, from_str], self.asset_id)
        if self.byte_length is not None:
            result["byteLength"] = from_union([from_none, to_int], self.byte_length)
        if self.data is not None:
            result["data"] = from_union([from_none, from_str], self.data)
        if self.display_name is not None:
            result["displayName"] = from_union([from_none, from_str], self.display_name)
        if self.omitted_reason is not None:
            result["omittedReason"] = from_union([from_none, lambda x: to_enum(OmittedBinaryOmittedReason, x)], self.omitted_reason)
        return result


@dataclass
class AttachmentDirectory:
    "Directory attachment"
    display_name: str
    path: str
    type: ClassVar[str] = "directory"
    tagged_files_entry: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentDirectory":
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        path = from_str(obj.get("path"))
        tagged_files_entry = from_union([from_none, from_str], obj.get("taggedFilesEntry"))
        return AttachmentDirectory(
            display_name=display_name,
            path=path,
            tagged_files_entry=tagged_files_entry,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["path"] = from_str(self.path)
        result["type"] = self.type
        if self.tagged_files_entry is not None:
            result["taggedFilesEntry"] = from_union([from_none, from_str], self.tagged_files_entry)
        return result


@dataclass
class AttachmentExtensionContext:
    "Structured context contributed by an extension. Composer pills displayed in the host are forwarded back through session.send.attachments, then rendered into the model prompt as an <extension_context> XML block."
    captured_at: datetime
    extension_id: str
    title: str
    type: ClassVar[str] = "extension_context"
    canvas_id: str | None = None
    instance_id: str | None = None
    payload: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentExtensionContext":
        assert isinstance(obj, dict)
        captured_at = from_datetime(obj.get("capturedAt"))
        extension_id = from_str(obj.get("extensionId"))
        title = from_str(obj.get("title"))
        canvas_id = from_union([from_none, from_str], obj.get("canvasId"))
        instance_id = from_union([from_none, from_str], obj.get("instanceId"))
        payload = obj.get("payload")
        return AttachmentExtensionContext(
            captured_at=captured_at,
            extension_id=extension_id,
            title=title,
            canvas_id=canvas_id,
            instance_id=instance_id,
            payload=payload,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["capturedAt"] = to_datetime(self.captured_at)
        result["extensionId"] = from_str(self.extension_id)
        result["title"] = from_str(self.title)
        result["type"] = self.type
        if self.canvas_id is not None:
            result["canvasId"] = from_union([from_none, from_str], self.canvas_id)
        if self.instance_id is not None:
            result["instanceId"] = from_union([from_none, from_str], self.instance_id)
        if self.payload is not None:
            result["payload"] = self.payload
        return result


@dataclass
class AttachmentFile:
    "File attachment"
    display_name: str
    path: str
    type: ClassVar[str] = "file"
    asset_id: str | None = None
    byte_length: int | None = None
    line_range: AttachmentFileLineRange | None = None
    mime_type: str | None = None
    omitted_reason: OmittedBinaryOmittedReason | None = None
    tagged_files_entry: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentFile":
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        path = from_str(obj.get("path"))
        asset_id = from_union([from_none, from_str], obj.get("assetId"))
        byte_length = from_union([from_none, from_int], obj.get("byteLength"))
        line_range = from_union([from_none, AttachmentFileLineRange.from_dict], obj.get("lineRange"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        omitted_reason = from_union([from_none, lambda x: parse_enum(OmittedBinaryOmittedReason, x)], obj.get("omittedReason"))
        tagged_files_entry = from_union([from_none, from_str], obj.get("taggedFilesEntry"))
        return AttachmentFile(
            display_name=display_name,
            path=path,
            asset_id=asset_id,
            byte_length=byte_length,
            line_range=line_range,
            mime_type=mime_type,
            omitted_reason=omitted_reason,
            tagged_files_entry=tagged_files_entry,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["path"] = from_str(self.path)
        result["type"] = self.type
        if self.asset_id is not None:
            result["assetId"] = from_union([from_none, from_str], self.asset_id)
        if self.byte_length is not None:
            result["byteLength"] = from_union([from_none, to_int], self.byte_length)
        if self.line_range is not None:
            result["lineRange"] = from_union([from_none, lambda x: to_class(AttachmentFileLineRange, x)], self.line_range)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        if self.omitted_reason is not None:
            result["omittedReason"] = from_union([from_none, lambda x: to_enum(OmittedBinaryOmittedReason, x)], self.omitted_reason)
        if self.tagged_files_entry is not None:
            result["taggedFilesEntry"] = from_union([from_none, from_str], self.tagged_files_entry)
        return result


@dataclass
class AttachmentFileLineRange:
    "Optional line range to scope the attachment to a specific section of the file"
    end: int
    start: int

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentFileLineRange":
        assert isinstance(obj, dict)
        end = from_int(obj.get("end"))
        start = from_int(obj.get("start"))
        return AttachmentFileLineRange(
            end=end,
            start=start,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_int(self.end)
        result["start"] = to_int(self.start)
        return result


@dataclass
class AttachmentGitHubActionsJob:
    "Pointer to a GitHub Actions job."
    job_id: int
    job_name: str
    repo: GitHubRepoRef
    type: ClassVar[str] = "github_actions_job"
    url: str
    workflow_name: str
    conclusion: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubActionsJob":
        assert isinstance(obj, dict)
        job_id = from_int(obj.get("jobId"))
        job_name = from_str(obj.get("jobName"))
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        url = from_str(obj.get("url"))
        workflow_name = from_str(obj.get("workflowName"))
        conclusion = from_union([from_none, from_str], obj.get("conclusion"))
        return AttachmentGitHubActionsJob(
            job_id=job_id,
            job_name=job_name,
            repo=repo,
            url=url,
            workflow_name=workflow_name,
            conclusion=conclusion,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["jobId"] = to_int(self.job_id)
        result["jobName"] = from_str(self.job_name)
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        result["workflowName"] = from_str(self.workflow_name)
        if self.conclusion is not None:
            result["conclusion"] = from_union([from_none, from_str], self.conclusion)
        return result


@dataclass
class AttachmentGitHubCommit:
    "Pointer to a GitHub commit."
    message: str
    oid: str
    repo: GitHubRepoRef
    type: ClassVar[str] = "github_commit"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubCommit":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        oid = from_str(obj.get("oid"))
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        url = from_str(obj.get("url"))
        return AttachmentGitHubCommit(
            message=message,
            oid=oid,
            repo=repo,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["oid"] = from_str(self.oid)
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentGitHubFile:
    "Pointer to a file in a GitHub repository at a specific ref."
    path: str
    ref: str
    repo: GitHubRepoRef
    type: ClassVar[str] = "github_file"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubFile":
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        ref = from_str(obj.get("ref"))
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        url = from_str(obj.get("url"))
        return AttachmentGitHubFile(
            path=path,
            ref=ref,
            repo=repo,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["ref"] = from_str(self.ref)
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentGitHubFileDiff:
    "Pointer to a single-file diff. At least one of `head` and `base` must be present."
    type: ClassVar[str] = "github_file_diff"
    url: str
    base: AttachmentGitHubFileDiffSide | None = None
    head: AttachmentGitHubFileDiffSide | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubFileDiff":
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        base = from_union([from_none, AttachmentGitHubFileDiffSide.from_dict], obj.get("base"))
        head = from_union([from_none, AttachmentGitHubFileDiffSide.from_dict], obj.get("head"))
        return AttachmentGitHubFileDiff(
            url=url,
            base=base,
            head=head,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = self.type
        result["url"] = from_str(self.url)
        if self.base is not None:
            result["base"] = from_union([from_none, lambda x: to_class(AttachmentGitHubFileDiffSide, x)], self.base)
        if self.head is not None:
            result["head"] = from_union([from_none, lambda x: to_class(AttachmentGitHubFileDiffSide, x)], self.head)
        return result


@dataclass
class AttachmentGitHubFileDiffSide:
    "One side of a file diff (head or base)"
    path: str
    ref: str
    repo: GitHubRepoRef

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubFileDiffSide":
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        ref = from_str(obj.get("ref"))
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        return AttachmentGitHubFileDiffSide(
            path=path,
            ref=ref,
            repo=repo,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["ref"] = from_str(self.ref)
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        return result


@dataclass
class AttachmentGitHubReference:
    "GitHub issue, pull request, or discussion reference"
    number: int
    reference_type: AttachmentGitHubReferenceType
    state: str
    title: str
    type: ClassVar[str] = "github_reference"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubReference":
        assert isinstance(obj, dict)
        number = from_int(obj.get("number"))
        reference_type = parse_enum(AttachmentGitHubReferenceType, obj.get("referenceType"))
        state = from_str(obj.get("state"))
        title = from_str(obj.get("title"))
        url = from_str(obj.get("url"))
        return AttachmentGitHubReference(
            number=number,
            reference_type=reference_type,
            state=state,
            title=title,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["number"] = to_int(self.number)
        result["referenceType"] = to_enum(AttachmentGitHubReferenceType, self.reference_type)
        result["state"] = from_str(self.state)
        result["title"] = from_str(self.title)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentGitHubRelease:
    "Pointer to a GitHub release."
    name: str
    repo: GitHubRepoRef
    tag_name: str
    type: ClassVar[str] = "github_release"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubRelease":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        tag_name = from_str(obj.get("tagName"))
        url = from_str(obj.get("url"))
        return AttachmentGitHubRelease(
            name=name,
            repo=repo,
            tag_name=tag_name,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["tagName"] = from_str(self.tag_name)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentGitHubRepository:
    "Pointer to a GitHub repository."
    repo: GitHubRepoRef
    type: ClassVar[str] = "github_repository"
    url: str
    description: str | None = None
    ref: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubRepository":
        assert isinstance(obj, dict)
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        url = from_str(obj.get("url"))
        description = from_union([from_none, from_str], obj.get("description"))
        ref = from_union([from_none, from_str], obj.get("ref"))
        return AttachmentGitHubRepository(
            repo=repo,
            url=url,
            description=description,
            ref=ref,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.ref is not None:
            result["ref"] = from_union([from_none, from_str], self.ref)
        return result


@dataclass
class AttachmentGitHubSnippet:
    "Pointer to a line range inside a file in a GitHub repository."
    line_range: AttachmentFileLineRange
    path: str
    ref: str
    repo: GitHubRepoRef
    type: ClassVar[str] = "github_snippet"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubSnippet":
        assert isinstance(obj, dict)
        line_range = AttachmentFileLineRange.from_dict(obj.get("lineRange"))
        path = from_str(obj.get("path"))
        ref = from_str(obj.get("ref"))
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        url = from_str(obj.get("url"))
        return AttachmentGitHubSnippet(
            line_range=line_range,
            path=path,
            ref=ref,
            repo=repo,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["lineRange"] = to_class(AttachmentFileLineRange, self.line_range)
        result["path"] = from_str(self.path)
        result["ref"] = from_str(self.ref)
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentGitHubTreeComparison:
    "Pointer to a comparison between two git revisions."
    base: AttachmentGitHubTreeComparisonSide
    head: AttachmentGitHubTreeComparisonSide
    type: ClassVar[str] = "github_tree_comparison"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubTreeComparison":
        assert isinstance(obj, dict)
        base = AttachmentGitHubTreeComparisonSide.from_dict(obj.get("base"))
        head = AttachmentGitHubTreeComparisonSide.from_dict(obj.get("head"))
        url = from_str(obj.get("url"))
        return AttachmentGitHubTreeComparison(
            base=base,
            head=head,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["base"] = to_class(AttachmentGitHubTreeComparisonSide, self.base)
        result["head"] = to_class(AttachmentGitHubTreeComparisonSide, self.head)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentGitHubTreeComparisonSide:
    "One side of a tree comparison (head or base)"
    repo: GitHubRepoRef
    revision: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubTreeComparisonSide":
        assert isinstance(obj, dict)
        repo = GitHubRepoRef.from_dict(obj.get("repo"))
        revision = from_str(obj.get("revision"))
        return AttachmentGitHubTreeComparisonSide(
            repo=repo,
            revision=revision,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["repo"] = to_class(GitHubRepoRef, self.repo)
        result["revision"] = from_str(self.revision)
        return result


@dataclass
class AttachmentGitHubUrl:
    "Generic GitHub URL reference."
    type: ClassVar[str] = "github_url"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentGitHubUrl":
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        return AttachmentGitHubUrl(
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class AttachmentSelection:
    "Code selection attachment from an editor"
    display_name: str
    file_path: str
    selection: AttachmentSelectionDetails
    text: str
    type: ClassVar[str] = "selection"

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentSelection":
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        file_path = from_str(obj.get("filePath"))
        selection = AttachmentSelectionDetails.from_dict(obj.get("selection"))
        text = from_str(obj.get("text"))
        return AttachmentSelection(
            display_name=display_name,
            file_path=file_path,
            selection=selection,
            text=text,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["filePath"] = from_str(self.file_path)
        result["selection"] = to_class(AttachmentSelectionDetails, self.selection)
        result["text"] = from_str(self.text)
        result["type"] = self.type
        return result


@dataclass
class AttachmentSelectionDetails:
    "Position range of the selection within the file"
    end: AttachmentSelectionDetailsEnd
    start: AttachmentSelectionDetailsStart

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentSelectionDetails":
        assert isinstance(obj, dict)
        end = AttachmentSelectionDetailsEnd.from_dict(obj.get("end"))
        start = AttachmentSelectionDetailsStart.from_dict(obj.get("start"))
        return AttachmentSelectionDetails(
            end=end,
            start=start,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_class(AttachmentSelectionDetailsEnd, self.end)
        result["start"] = to_class(AttachmentSelectionDetailsStart, self.start)
        return result


@dataclass
class AttachmentSelectionDetailsEnd:
    "End position of the selection"
    character: int
    line: int

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentSelectionDetailsEnd":
        assert isinstance(obj, dict)
        character = from_int(obj.get("character"))
        line = from_int(obj.get("line"))
        return AttachmentSelectionDetailsEnd(
            character=character,
            line=line,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = to_int(self.character)
        result["line"] = to_int(self.line)
        return result


@dataclass
class AttachmentSelectionDetailsStart:
    "Start position of the selection"
    character: int
    line: int

    @staticmethod
    def from_dict(obj: Any) -> "AttachmentSelectionDetailsStart":
        assert isinstance(obj, dict)
        character = from_int(obj.get("character"))
        line = from_int(obj.get("line"))
        return AttachmentSelectionDetailsStart(
            character=character,
            line=line,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = to_int(self.character)
        result["line"] = to_int(self.line)
        return result


@dataclass
class AutoModeSwitchCompletedData:
    "Auto mode switch completion notification"
    request_id: str
    response: AutoModeSwitchResponse

    @staticmethod
    def from_dict(obj: Any) -> "AutoModeSwitchCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        response = parse_enum(AutoModeSwitchResponse, obj.get("response"))
        return AutoModeSwitchCompletedData(
            request_id=request_id,
            response=response,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["response"] = to_enum(AutoModeSwitchResponse, self.response)
        return result


@dataclass
class AutoModeSwitchRequestedData:
    "Auto mode switch request notification requiring user approval"
    request_id: str
    error_code: str | None = None
    retry_after_seconds: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AutoModeSwitchRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        error_code = from_union([from_none, from_str], obj.get("errorCode"))
        retry_after_seconds = from_union([from_none, from_int], obj.get("retryAfterSeconds"))
        return AutoModeSwitchRequestedData(
            request_id=request_id,
            error_code=error_code,
            retry_after_seconds=retry_after_seconds,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.error_code is not None:
            result["errorCode"] = from_union([from_none, from_str], self.error_code)
        if self.retry_after_seconds is not None:
            result["retryAfterSeconds"] = from_union([from_none, to_int], self.retry_after_seconds)
        return result


@dataclass
class CapabilitiesChangedData:
    "Session capability change notification"
    ui: CapabilitiesChangedUI | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CapabilitiesChangedData":
        assert isinstance(obj, dict)
        ui = from_union([from_none, CapabilitiesChangedUI.from_dict], obj.get("ui"))
        return CapabilitiesChangedData(
            ui=ui,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.ui is not None:
            result["ui"] = from_union([from_none, lambda x: to_class(CapabilitiesChangedUI, x)], self.ui)
        return result


@dataclass
class CapabilitiesChangedUI:
    "UI capability changes"
    canvases: bool | None = None
    elicitation: bool | None = None
    mcp_apps: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CapabilitiesChangedUI":
        assert isinstance(obj, dict)
        canvases = from_union([from_none, from_bool], obj.get("canvases"))
        elicitation = from_union([from_none, from_bool], obj.get("elicitation"))
        mcp_apps = from_union([from_none, from_bool], obj.get("mcpApps"))
        return CapabilitiesChangedUI(
            canvases=canvases,
            elicitation=elicitation,
            mcp_apps=mcp_apps,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.canvases is not None:
            result["canvases"] = from_union([from_none, from_bool], self.canvases)
        if self.elicitation is not None:
            result["elicitation"] = from_union([from_none, from_bool], self.elicitation)
        if self.mcp_apps is not None:
            result["mcpApps"] = from_union([from_none, from_bool], self.mcp_apps)
        return result


@dataclass
class CommandCompletedData:
    "Queued command completion notification signaling UI dismissal"
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "CommandCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        return CommandCompletedData(
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class CommandExecuteData:
    "Registered command dispatch request routed to the owning client"
    args: str
    command: str
    command_name: str
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "CommandExecuteData":
        assert isinstance(obj, dict)
        args = from_str(obj.get("args"))
        command = from_str(obj.get("command"))
        command_name = from_str(obj.get("commandName"))
        request_id = from_str(obj.get("requestId"))
        return CommandExecuteData(
            args=args,
            command=command,
            command_name=command_name,
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["args"] = from_str(self.args)
        result["command"] = from_str(self.command)
        result["commandName"] = from_str(self.command_name)
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class CommandQueuedData:
    "Queued slash command dispatch request for client execution"
    command: str
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "CommandQueuedData":
        assert isinstance(obj, dict)
        command = from_str(obj.get("command"))
        request_id = from_str(obj.get("requestId"))
        return CommandQueuedData(
            command=command,
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["command"] = from_str(self.command)
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class CommandsChangedCommand:
    "A single slash command available in the session, as listed by the `commands.changed` event."
    name: str
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CommandsChangedCommand":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        description = from_union([from_none, from_str], obj.get("description"))
        return CommandsChangedCommand(
            name=name,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        return result


@dataclass
class CommandsChangedData:
    "SDK command registration change notification"
    commands: list[CommandsChangedCommand]

    @staticmethod
    def from_dict(obj: Any) -> "CommandsChangedData":
        assert isinstance(obj, dict)
        commands = from_list(CommandsChangedCommand.from_dict, obj.get("commands"))
        return CommandsChangedData(
            commands=commands,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["commands"] = from_list(lambda x: to_class(CommandsChangedCommand, x), self.commands)
        return result


@dataclass
class CompactionCompleteCompactionTokensUsed:
    "Token usage breakdown for the compaction LLM call (aligned with assistant.usage format)"
    cache_read_tokens: int | None = None
    cache_write_tokens: int | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _copilot_usage: _CompactionCompleteCompactionTokensUsedCopilotUsage | None = None
    duration: timedelta | None = None
    input_tokens: int | None = None
    model: str | None = None
    output_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CompactionCompleteCompactionTokensUsed":
        assert isinstance(obj, dict)
        cache_read_tokens = from_union([from_none, from_int], obj.get("cacheReadTokens"))
        cache_write_tokens = from_union([from_none, from_int], obj.get("cacheWriteTokens"))
        _copilot_usage = from_union([from_none, _CompactionCompleteCompactionTokensUsedCopilotUsage.from_dict], obj.get("copilotUsage"))
        duration = from_union([from_none, from_timedelta], obj.get("duration"))
        input_tokens = from_union([from_none, from_int], obj.get("inputTokens"))
        model = from_union([from_none, from_str], obj.get("model"))
        output_tokens = from_union([from_none, from_int], obj.get("outputTokens"))
        return CompactionCompleteCompactionTokensUsed(
            cache_read_tokens=cache_read_tokens,
            cache_write_tokens=cache_write_tokens,
            _copilot_usage=_copilot_usage,
            duration=duration,
            input_tokens=input_tokens,
            model=model,
            output_tokens=output_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.cache_read_tokens is not None:
            result["cacheReadTokens"] = from_union([from_none, to_int], self.cache_read_tokens)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([from_none, to_int], self.cache_write_tokens)
        if self._copilot_usage is not None:
            result["copilotUsage"] = from_union([from_none, lambda x: to_class(_CompactionCompleteCompactionTokensUsedCopilotUsage, x)], self._copilot_usage)
        if self.duration is not None:
            result["duration"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.input_tokens is not None:
            result["inputTokens"] = from_union([from_none, to_int], self.input_tokens)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([from_none, to_int], self.output_tokens)
        return result


@dataclass
class _CompactionCompleteCompactionTokensUsedCopilotUsage:
    "Per-request cost and usage data from the CAPI copilot_usage response field"
    total_nano_aiu: float
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _token_details: list[CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "_CompactionCompleteCompactionTokensUsedCopilotUsage":
        assert isinstance(obj, dict)
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        _token_details = from_union([from_none, lambda x: from_list(CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail.from_dict, x)], obj.get("tokenDetails"))
        return _CompactionCompleteCompactionTokensUsedCopilotUsage(
            total_nano_aiu=total_nano_aiu,
            _token_details=_token_details,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        if self._token_details is not None:
            result["tokenDetails"] = from_union([from_none, lambda x: from_list(lambda x: to_class(CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail, x), x)], self._token_details)
        return result


@dataclass
class CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail:
    "Token usage detail for a single billing category"
    batch_size: int
    cost_per_batch: int
    token_count: int
    token_type: str

    @staticmethod
    def from_dict(obj: Any) -> "CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail":
        assert isinstance(obj, dict)
        batch_size = from_int(obj.get("batchSize"))
        cost_per_batch = from_int(obj.get("costPerBatch"))
        token_count = from_int(obj.get("tokenCount"))
        token_type = from_str(obj.get("tokenType"))
        return CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail(
            batch_size=batch_size,
            cost_per_batch=cost_per_batch,
            token_count=token_count,
            token_type=token_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["batchSize"] = to_int(self.batch_size)
        result["costPerBatch"] = to_int(self.cost_per_batch)
        result["tokenCount"] = to_int(self.token_count)
        result["tokenType"] = from_str(self.token_type)
        return result


@dataclass
class CustomAgentsUpdatedAgent:
    "A single loaded custom agent in `session.custom_agents_updated`, with identity, source, tools, invocability, and model override."
    description: str
    display_name: str
    id: str
    name: str
    source: str
    tools: list[str] | None
    user_invocable: bool
    model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CustomAgentsUpdatedAgent":
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        display_name = from_str(obj.get("displayName"))
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        source = from_str(obj.get("source"))
        tools = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("tools"))
        user_invocable = from_bool(obj.get("userInvocable"))
        model = from_union([from_none, from_str], obj.get("model"))
        return CustomAgentsUpdatedAgent(
            description=description,
            display_name=display_name,
            id=id,
            name=name,
            source=source,
            tools=tools,
            user_invocable=user_invocable,
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["displayName"] = from_str(self.display_name)
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        result["source"] = from_str(self.source)
        result["tools"] = from_union([from_none, lambda x: from_list(from_str, x)], self.tools)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        return result


@dataclass
class ElicitationCompletedData:
    "Elicitation request completion with the user's response"
    request_id: str
    action: ElicitationCompletedAction | None = None
    content: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ElicitationCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        action = from_union([from_none, lambda x: parse_enum(ElicitationCompletedAction, x)], obj.get("action"))
        content = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("content"))
        return ElicitationCompletedData(
            request_id=request_id,
            action=action,
            content=content,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.action is not None:
            result["action"] = from_union([from_none, lambda x: to_enum(ElicitationCompletedAction, x)], self.action)
        if self.content is not None:
            result["content"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.content)
        return result


@dataclass
class ElicitationRequestedData:
    "Elicitation request; may be form-based (structured input) or URL-based (browser redirect)"
    message: str
    request_id: str
    elicitation_source: str | None = None
    mode: ElicitationRequestedMode | None = None
    requested_schema: ElicitationRequestedSchema | None = None
    tool_call_id: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ElicitationRequestedData":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        request_id = from_str(obj.get("requestId"))
        elicitation_source = from_union([from_none, from_str], obj.get("elicitationSource"))
        mode = from_union([from_none, lambda x: parse_enum(ElicitationRequestedMode, x)], obj.get("mode"))
        requested_schema = from_union([from_none, ElicitationRequestedSchema.from_dict], obj.get("requestedSchema"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        url = from_union([from_none, from_str], obj.get("url"))
        return ElicitationRequestedData(
            message=message,
            request_id=request_id,
            elicitation_source=elicitation_source,
            mode=mode,
            requested_schema=requested_schema,
            tool_call_id=tool_call_id,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["requestId"] = from_str(self.request_id)
        if self.elicitation_source is not None:
            result["elicitationSource"] = from_union([from_none, from_str], self.elicitation_source)
        if self.mode is not None:
            result["mode"] = from_union([from_none, lambda x: to_enum(ElicitationRequestedMode, x)], self.mode)
        if self.requested_schema is not None:
            result["requestedSchema"] = from_union([from_none, lambda x: to_class(ElicitationRequestedSchema, x)], self.requested_schema)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


@dataclass
class ElicitationRequestedSchema:
    "JSON Schema describing the form fields to present to the user (form mode only)"
    properties: dict[str, Any]
    type: str
    required: list[str] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ElicitationRequestedSchema":
        assert isinstance(obj, dict)
        properties = from_dict(lambda x: x, obj.get("properties"))
        type = from_str(obj.get("type"))
        required = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("required"))
        return ElicitationRequestedSchema(
            properties=properties,
            type=type,
            required=required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["properties"] = from_dict(lambda x: x, self.properties)
        result["type"] = from_str(self.type)
        if self.required is not None:
            result["required"] = from_union([from_none, lambda x: from_list(from_str, x)], self.required)
        return result


@dataclass
class EmbeddedBlobResourceContents:
    "Embedded binary resource contents identified by a URI, with an optional MIME type and a base64-encoded blob."
    blob: str
    uri: str
    mime_type: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "EmbeddedBlobResourceContents":
        assert isinstance(obj, dict)
        blob = from_str(obj.get("blob"))
        uri = from_str(obj.get("uri"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        return EmbeddedBlobResourceContents(
            blob=blob,
            uri=uri,
            mime_type=mime_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["blob"] = from_str(self.blob)
        result["uri"] = from_str(self.uri)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        return result


@dataclass
class EmbeddedTextResourceContents:
    "Embedded text resource contents identified by a URI, with an optional MIME type and a text payload."
    text: str
    uri: str
    mime_type: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "EmbeddedTextResourceContents":
        assert isinstance(obj, dict)
        text = from_str(obj.get("text"))
        uri = from_str(obj.get("uri"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        return EmbeddedTextResourceContents(
            text=text,
            uri=uri,
            mime_type=mime_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["text"] = from_str(self.text)
        result["uri"] = from_str(self.uri)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        return result


@dataclass
class ExitPlanModeCompletedData:
    "Plan mode exit completion with the user's approval decision and optional feedback"
    request_id: str
    approved: bool | None = None
    auto_approve_edits: bool | None = None
    feedback: str | None = None
    selected_action: ExitPlanModeAction | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ExitPlanModeCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        approved = from_union([from_none, from_bool], obj.get("approved"))
        auto_approve_edits = from_union([from_none, from_bool], obj.get("autoApproveEdits"))
        feedback = from_union([from_none, from_str], obj.get("feedback"))
        selected_action = from_union([from_none, lambda x: parse_enum(ExitPlanModeAction, x)], obj.get("selectedAction"))
        return ExitPlanModeCompletedData(
            request_id=request_id,
            approved=approved,
            auto_approve_edits=auto_approve_edits,
            feedback=feedback,
            selected_action=selected_action,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.approved is not None:
            result["approved"] = from_union([from_none, from_bool], self.approved)
        if self.auto_approve_edits is not None:
            result["autoApproveEdits"] = from_union([from_none, from_bool], self.auto_approve_edits)
        if self.feedback is not None:
            result["feedback"] = from_union([from_none, from_str], self.feedback)
        if self.selected_action is not None:
            result["selectedAction"] = from_union([from_none, lambda x: to_enum(ExitPlanModeAction, x)], self.selected_action)
        return result


@dataclass
class ExitPlanModeRequestedData:
    "Plan approval request with plan content and available user actions"
    actions: list[ExitPlanModeAction]
    plan_content: str
    recommended_action: ExitPlanModeAction
    request_id: str
    summary: str
    model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ExitPlanModeRequestedData":
        assert isinstance(obj, dict)
        actions = from_list(lambda x: parse_enum(ExitPlanModeAction, x), obj.get("actions"))
        plan_content = from_str(obj.get("planContent"))
        recommended_action = parse_enum(ExitPlanModeAction, obj.get("recommendedAction"))
        request_id = from_str(obj.get("requestId"))
        summary = from_str(obj.get("summary"))
        model = from_union([from_none, from_str], obj.get("model"))
        return ExitPlanModeRequestedData(
            actions=actions,
            plan_content=plan_content,
            recommended_action=recommended_action,
            request_id=request_id,
            summary=summary,
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["actions"] = from_list(lambda x: to_enum(ExitPlanModeAction, x), self.actions)
        result["planContent"] = from_str(self.plan_content)
        result["recommendedAction"] = to_enum(ExitPlanModeAction, self.recommended_action)
        result["requestId"] = from_str(self.request_id)
        result["summary"] = from_str(self.summary)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        return result


@dataclass
class ExtensionsLoadedExtension:
    "A single extension discovered by `session.extensions_loaded`, including qualified ID, source, and current status."
    id: str
    name: str
    source: ExtensionsLoadedExtensionSource
    status: ExtensionsLoadedExtensionStatus

    @staticmethod
    def from_dict(obj: Any) -> "ExtensionsLoadedExtension":
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        source = parse_enum(ExtensionsLoadedExtensionSource, obj.get("source"))
        status = parse_enum(ExtensionsLoadedExtensionStatus, obj.get("status"))
        return ExtensionsLoadedExtension(
            id=id,
            name=name,
            source=source,
            status=status,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(ExtensionsLoadedExtensionSource, self.source)
        result["status"] = to_enum(ExtensionsLoadedExtensionStatus, self.status)
        return result


@dataclass
class ExternalToolCompletedData:
    "External tool completion notification signaling UI dismissal"
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "ExternalToolCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        return ExternalToolCompletedData(
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class ExternalToolRequestedData:
    "External tool invocation request for client-side tool execution"
    request_id: str
    session_id: str
    tool_call_id: str
    tool_name: str
    arguments: Any = None
    provider_id: str | None = None
    traceparent: str | None = None
    tracestate: str | None = None
    working_directory: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ExternalToolRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        session_id = from_str(obj.get("sessionId"))
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        arguments = obj.get("arguments")
        provider_id = from_union([from_none, from_str], obj.get("providerId"))
        traceparent = from_union([from_none, from_str], obj.get("traceparent"))
        tracestate = from_union([from_none, from_str], obj.get("tracestate"))
        working_directory = from_union([from_none, from_str], obj.get("workingDirectory"))
        return ExternalToolRequestedData(
            request_id=request_id,
            session_id=session_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
            provider_id=provider_id,
            traceparent=traceparent,
            tracestate=tracestate,
            working_directory=working_directory,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["sessionId"] = from_str(self.session_id)
        result["toolCallId"] = from_str(self.tool_call_id)
        result["toolName"] = from_str(self.tool_name)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.provider_id is not None:
            result["providerId"] = from_union([from_none, from_str], self.provider_id)
        if self.traceparent is not None:
            result["traceparent"] = from_union([from_none, from_str], self.traceparent)
        if self.tracestate is not None:
            result["tracestate"] = from_union([from_none, from_str], self.tracestate)
        if self.working_directory is not None:
            result["workingDirectory"] = from_union([from_none, from_str], self.working_directory)
        return result


@dataclass
class FactoryPermissionPhase:
    "A declared phase shown in a factory permission prompt."
    title: str
    detail: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "FactoryPermissionPhase":
        assert isinstance(obj, dict)
        title = from_str(obj.get("title"))
        detail = from_union([from_none, from_str], obj.get("detail"))
        return FactoryPermissionPhase(
            title=title,
            detail=detail,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["title"] = from_str(self.title)
        if self.detail is not None:
            result["detail"] = from_union([from_none, from_str], self.detail)
        return result


@dataclass
class GitHubMcpToolConfig:
    "Per-session configuration for the built-in GitHub MCP server"
    additional_tools: list[str] | None = None
    additional_toolsets: list[str] | None = None
    enable_all_tools: bool | None = None
    enable_insiders_mode: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "GitHubMcpToolConfig":
        assert isinstance(obj, dict)
        additional_tools = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("additionalTools"))
        additional_toolsets = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("additionalToolsets"))
        enable_all_tools = from_union([from_none, from_bool], obj.get("enableAllTools"))
        enable_insiders_mode = from_union([from_none, from_bool], obj.get("enableInsidersMode"))
        return GitHubMcpToolConfig(
            additional_tools=additional_tools,
            additional_toolsets=additional_toolsets,
            enable_all_tools=enable_all_tools,
            enable_insiders_mode=enable_insiders_mode,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.additional_tools is not None:
            result["additionalTools"] = from_union([from_none, lambda x: from_list(from_str, x)], self.additional_tools)
        if self.additional_toolsets is not None:
            result["additionalToolsets"] = from_union([from_none, lambda x: from_list(from_str, x)], self.additional_toolsets)
        if self.enable_all_tools is not None:
            result["enableAllTools"] = from_union([from_none, from_bool], self.enable_all_tools)
        if self.enable_insiders_mode is not None:
            result["enableInsidersMode"] = from_union([from_none, from_bool], self.enable_insiders_mode)
        return result


@dataclass
class GitHubRepoRef:
    "Pointer to a GitHub repository."
    name: str
    owner: str
    id: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "GitHubRepoRef":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        owner = from_str(obj.get("owner"))
        id = from_union([from_none, from_int], obj.get("id"))
        return GitHubRepoRef(
            name=name,
            owner=owner,
            id=id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["owner"] = from_str(self.owner)
        if self.id is not None:
            result["id"] = from_union([from_none, to_int], self.id)
        return result


@dataclass
class HandoffRepository:
    "Repository context for the handed-off session"
    name: str
    owner: str
    branch: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HandoffRepository":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        owner = from_str(obj.get("owner"))
        branch = from_union([from_none, from_str], obj.get("branch"))
        return HandoffRepository(
            name=name,
            owner=owner,
            branch=branch,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["owner"] = from_str(self.owner)
        if self.branch is not None:
            result["branch"] = from_union([from_none, from_str], self.branch)
        return result


@dataclass
class HeaderEntry:
    "Single HTTP header entry as a name/value pair."
    name: str
    value: str

    @staticmethod
    def from_dict(obj: Any) -> "HeaderEntry":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        value = from_str(obj.get("value"))
        return HeaderEntry(
            name=name,
            value=value,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["value"] = from_str(self.value)
        return result


@dataclass
class HookEndData:
    "Hook invocation completion details including output, success status, and error information"
    hook_invocation_id: str
    hook_type: str
    success: bool
    error: HookEndError | None = None
    output: Any = None
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookEndData":
        assert isinstance(obj, dict)
        hook_invocation_id = from_str(obj.get("hookInvocationId"))
        hook_type = from_str(obj.get("hookType"))
        success = from_bool(obj.get("success"))
        error = from_union([from_none, HookEndError.from_dict], obj.get("error"))
        output = obj.get("output")
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return HookEndData(
            hook_invocation_id=hook_invocation_id,
            hook_type=hook_type,
            success=success,
            error=error,
            output=output,
            parent_tool_call_id=parent_tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["hookInvocationId"] = from_str(self.hook_invocation_id)
        result["hookType"] = from_str(self.hook_type)
        result["success"] = from_bool(self.success)
        if self.error is not None:
            result["error"] = from_union([from_none, lambda x: to_class(HookEndError, x)], self.error)
        if self.output is not None:
            result["output"] = self.output
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        return result


@dataclass
class HookEndError:
    "Error details when the hook failed"
    message: str
    source: str | None = None
    stack: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookEndError":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        source = from_union([from_none, from_str], obj.get("source"))
        stack = from_union([from_none, from_str], obj.get("stack"))
        return HookEndError(
            message=message,
            source=source,
            stack=stack,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.source is not None:
            result["source"] = from_union([from_none, from_str], self.source)
        if self.stack is not None:
            result["stack"] = from_union([from_none, from_str], self.stack)
        return result


@dataclass
class HookProgressData:
    "Ephemeral progress update from a running hook process"
    message: str
    temporary: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookProgressData":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        temporary = from_union([from_none, from_bool], obj.get("temporary"))
        return HookProgressData(
            message=message,
            temporary=temporary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.temporary is not None:
            result["temporary"] = from_union([from_none, from_bool], self.temporary)
        return result


@dataclass
class HookStartData:
    "Hook invocation start details including type and input data"
    hook_invocation_id: str
    hook_type: str
    input: Any = None
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookStartData":
        assert isinstance(obj, dict)
        hook_invocation_id = from_str(obj.get("hookInvocationId"))
        hook_type = from_str(obj.get("hookType"))
        input = obj.get("input")
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return HookStartData(
            hook_invocation_id=hook_invocation_id,
            hook_type=hook_type,
            input=input,
            parent_tool_call_id=parent_tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["hookInvocationId"] = from_str(self.hook_invocation_id)
        result["hookType"] = from_str(self.hook_type)
        if self.input is not None:
            result["input"] = self.input
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        return result


@dataclass
class McpAppToolCallCompleteData:
    "MCP App view called a tool on a connected MCP server (SEP-1865)"
    duration_ms: float
    server_name: str
    success: bool
    tool_name: str
    arguments: dict[str, Any] | None = None
    error: McpAppToolCallCompleteError | None = None
    result: dict[str, Any] | None = None
    tool_meta: McpAppToolCallCompleteToolMeta | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpAppToolCallCompleteData":
        assert isinstance(obj, dict)
        duration_ms = from_float(obj.get("durationMs"))
        server_name = from_str(obj.get("serverName"))
        success = from_bool(obj.get("success"))
        tool_name = from_str(obj.get("toolName"))
        arguments = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("arguments"))
        error = from_union([from_none, McpAppToolCallCompleteError.from_dict], obj.get("error"))
        result = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("result"))
        tool_meta = from_union([from_none, McpAppToolCallCompleteToolMeta.from_dict], obj.get("toolMeta"))
        return McpAppToolCallCompleteData(
            duration_ms=duration_ms,
            server_name=server_name,
            success=success,
            tool_name=tool_name,
            arguments=arguments,
            error=error,
            result=result,
            tool_meta=tool_meta,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["durationMs"] = to_float(self.duration_ms)
        result["serverName"] = from_str(self.server_name)
        result["success"] = from_bool(self.success)
        result["toolName"] = from_str(self.tool_name)
        if self.arguments is not None:
            result["arguments"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.arguments)
        if self.error is not None:
            result["error"] = from_union([from_none, lambda x: to_class(McpAppToolCallCompleteError, x)], self.error)
        if self.result is not None:
            result["result"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.result)
        if self.tool_meta is not None:
            result["toolMeta"] = from_union([from_none, lambda x: to_class(McpAppToolCallCompleteToolMeta, x)], self.tool_meta)
        return result


@dataclass
class McpAppToolCallCompleteError:
    "Set when the underlying tools/call threw an error before returning a CallToolResult"
    message: str

    @staticmethod
    def from_dict(obj: Any) -> "McpAppToolCallCompleteError":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        return McpAppToolCallCompleteError(
            message=message,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        return result


@dataclass
class McpAppToolCallCompleteToolMeta:
    "The tool's `_meta.ui` block at the time of the call, so consumers can decide whether to forward the result to the model without re-listing tools."
    ui: McpAppToolCallCompleteToolMetaUI | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpAppToolCallCompleteToolMeta":
        assert isinstance(obj, dict)
        ui = from_union([from_none, McpAppToolCallCompleteToolMetaUI.from_dict], obj.get("ui"))
        return McpAppToolCallCompleteToolMeta(
            ui=ui,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.ui is not None:
            result["ui"] = from_union([from_none, lambda x: to_class(McpAppToolCallCompleteToolMetaUI, x)], self.ui)
        return result


@dataclass
class McpAppToolCallCompleteToolMetaUI:
    "MCP App tool `_meta.ui` resource URI and SEP-1865 visibility captured with an `mcp_app.tool_call_complete` result."
    resource_uri: str | None = None
    visibility: list[str] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpAppToolCallCompleteToolMetaUI":
        assert isinstance(obj, dict)
        resource_uri = from_union([from_none, from_str], obj.get("resourceUri"))
        visibility = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("visibility"))
        return McpAppToolCallCompleteToolMetaUI(
            resource_uri=resource_uri,
            visibility=visibility,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.resource_uri is not None:
            result["resourceUri"] = from_union([from_none, from_str], self.resource_uri)
        if self.visibility is not None:
            result["visibility"] = from_union([from_none, lambda x: from_list(from_str, x)], self.visibility)
        return result


@dataclass
class McpHeadersRefreshCompletedData:
    "MCP headers refresh request completion notification"
    outcome: McpHeadersRefreshCompletedOutcome
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "McpHeadersRefreshCompletedData":
        assert isinstance(obj, dict)
        outcome = parse_enum(McpHeadersRefreshCompletedOutcome, obj.get("outcome"))
        request_id = from_str(obj.get("requestId"))
        return McpHeadersRefreshCompletedData(
            outcome=outcome,
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["outcome"] = to_enum(McpHeadersRefreshCompletedOutcome, self.outcome)
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class McpHeadersRefreshRequiredData:
    "Dynamic headers refresh request for a remote MCP server"
    reason: McpHeadersRefreshRequiredReason
    request_id: str
    server_name: str
    server_url: str

    @staticmethod
    def from_dict(obj: Any) -> "McpHeadersRefreshRequiredData":
        assert isinstance(obj, dict)
        reason = parse_enum(McpHeadersRefreshRequiredReason, obj.get("reason"))
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        server_url = from_str(obj.get("serverUrl"))
        return McpHeadersRefreshRequiredData(
            reason=reason,
            request_id=request_id,
            server_name=server_name,
            server_url=server_url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["reason"] = to_enum(McpHeadersRefreshRequiredReason, self.reason)
        result["requestId"] = from_str(self.request_id)
        result["serverName"] = from_str(self.server_name)
        result["serverUrl"] = from_str(self.server_url)
        return result


@dataclass
class McpOauthCompletedData:
    "MCP OAuth request completion notification"
    outcome: McpOauthCompletionOutcome
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthCompletedData":
        assert isinstance(obj, dict)
        outcome = parse_enum(McpOauthCompletionOutcome, obj.get("outcome"))
        request_id = from_str(obj.get("requestId"))
        return McpOauthCompletedData(
            outcome=outcome,
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["outcome"] = to_enum(McpOauthCompletionOutcome, self.outcome)
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class McpOauthHttpResponse:
    "Raw HTTP response details from the OAuth auth challenge, as observed by the runtime."
    headers: list[HeaderEntry]
    status_code: int
    body: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthHttpResponse":
        assert isinstance(obj, dict)
        headers = from_list(HeaderEntry.from_dict, obj.get("headers"))
        status_code = from_int(obj.get("statusCode"))
        body = from_union([from_none, from_str], obj.get("body"))
        return McpOauthHttpResponse(
            headers=headers,
            status_code=status_code,
            body=body,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["headers"] = from_list(lambda x: to_class(HeaderEntry, x), self.headers)
        result["statusCode"] = to_int(self.status_code)
        if self.body is not None:
            result["body"] = from_union([from_none, from_str], self.body)
        return result


@dataclass
class McpOauthRequiredData:
    "OAuth authentication request for an MCP server"
    reason: McpOauthRequestReason
    request_id: str
    server_name: str
    server_url: str
    http_response: McpOauthHttpResponse | None = None
    resource_metadata: str | None = None
    static_client_config: McpOauthRequiredStaticClientConfig | None = None
    www_authenticate_params: McpOauthWWWAuthenticateParams | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthRequiredData":
        assert isinstance(obj, dict)
        reason = parse_enum(McpOauthRequestReason, obj.get("reason"))
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        server_url = from_str(obj.get("serverUrl"))
        http_response = from_union([from_none, McpOauthHttpResponse.from_dict], obj.get("httpResponse"))
        resource_metadata = from_union([from_none, from_str], obj.get("resourceMetadata"))
        static_client_config = from_union([from_none, McpOauthRequiredStaticClientConfig.from_dict], obj.get("staticClientConfig"))
        www_authenticate_params = from_union([from_none, McpOauthWWWAuthenticateParams.from_dict], obj.get("wwwAuthenticateParams"))
        return McpOauthRequiredData(
            reason=reason,
            request_id=request_id,
            server_name=server_name,
            server_url=server_url,
            http_response=http_response,
            resource_metadata=resource_metadata,
            static_client_config=static_client_config,
            www_authenticate_params=www_authenticate_params,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["reason"] = to_enum(McpOauthRequestReason, self.reason)
        result["requestId"] = from_str(self.request_id)
        result["serverName"] = from_str(self.server_name)
        result["serverUrl"] = from_str(self.server_url)
        if self.http_response is not None:
            result["httpResponse"] = from_union([from_none, lambda x: to_class(McpOauthHttpResponse, x)], self.http_response)
        if self.resource_metadata is not None:
            result["resourceMetadata"] = from_union([from_none, from_str], self.resource_metadata)
        if self.static_client_config is not None:
            result["staticClientConfig"] = from_union([from_none, lambda x: to_class(McpOauthRequiredStaticClientConfig, x)], self.static_client_config)
        if self.www_authenticate_params is not None:
            result["wwwAuthenticateParams"] = from_union([from_none, lambda x: to_class(McpOauthWWWAuthenticateParams, x)], self.www_authenticate_params)
        return result


@dataclass
class McpOauthRequiredStaticClientConfig:
    "Static OAuth client configuration, if the server specifies one"
    client_id: str
    client_secret: str | None = None
    grant_type: str | None = None
    public_client: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthRequiredStaticClientConfig":
        assert isinstance(obj, dict)
        client_id = from_str(obj.get("clientId"))
        client_secret = from_union([from_none, from_str], obj.get("clientSecret"))
        grant_type = from_union([from_none, from_str], obj.get("grantType"))
        public_client = from_union([from_none, from_bool], obj.get("publicClient"))
        return McpOauthRequiredStaticClientConfig(
            client_id=client_id,
            client_secret=client_secret,
            grant_type=grant_type,
            public_client=public_client,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["clientId"] = from_str(self.client_id)
        if self.client_secret is not None:
            result["clientSecret"] = from_union([from_none, from_str], self.client_secret)
        if self.grant_type is not None:
            result["grantType"] = from_union([from_none, from_str], self.grant_type)
        if self.public_client is not None:
            result["publicClient"] = from_union([from_none, from_bool], self.public_client)
        return result


@dataclass
class McpOauthWWWAuthenticateParams:
    "OAuth WWW-Authenticate parameters parsed from an MCP auth challenge"
    error: str | None = None
    resource_metadata_url: str | None = None
    scope: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthWWWAuthenticateParams":
        assert isinstance(obj, dict)
        error = from_union([from_none, from_str], obj.get("error"))
        resource_metadata_url = from_union([from_none, from_str], obj.get("resourceMetadataUrl"))
        scope = from_union([from_none, from_str], obj.get("scope"))
        return McpOauthWWWAuthenticateParams(
            error=error,
            resource_metadata_url=resource_metadata_url,
            scope=scope,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        if self.resource_metadata_url is not None:
            result["resourceMetadataUrl"] = from_union([from_none, from_str], self.resource_metadata_url)
        if self.scope is not None:
            result["scope"] = from_union([from_none, from_str], self.scope)
        return result


@dataclass
class McpPromptsListChangedData:
    "Payload identifying the MCP server associated with a list change."
    server_name: str

    @staticmethod
    def from_dict(obj: Any) -> "McpPromptsListChangedData":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return McpPromptsListChangedData(
            server_name=server_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result


@dataclass
class McpResourcesListChangedData:
    "Payload identifying the MCP server associated with a list change."
    server_name: str

    @staticmethod
    def from_dict(obj: Any) -> "McpResourcesListChangedData":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return McpResourcesListChangedData(
            server_name=server_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result


@dataclass
class McpServersLoadedServer:
    "A single MCP server status summary in `session.mcp_servers_loaded`, including name, status, source, transport, and plugin metadata."
    name: str
    status: McpServerStatus
    error: str | None = None
    plugin_name: str | None = None
    plugin_version: str | None = None
    source: McpServerSource | None = None
    transport: McpServerTransport | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpServersLoadedServer":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        status = parse_enum(McpServerStatus, obj.get("status"))
        error = from_union([from_none, from_str], obj.get("error"))
        plugin_name = from_union([from_none, from_str], obj.get("pluginName"))
        plugin_version = from_union([from_none, from_str], obj.get("pluginVersion"))
        source = from_union([from_none, lambda x: parse_enum(McpServerSource, x)], obj.get("source"))
        transport = from_union([from_none, lambda x: parse_enum(McpServerTransport, x)], obj.get("transport"))
        return McpServersLoadedServer(
            name=name,
            status=status,
            error=error,
            plugin_name=plugin_name,
            plugin_version=plugin_version,
            source=source,
            transport=transport,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["status"] = to_enum(McpServerStatus, self.status)
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        if self.plugin_name is not None:
            result["pluginName"] = from_union([from_none, from_str], self.plugin_name)
        if self.plugin_version is not None:
            result["pluginVersion"] = from_union([from_none, from_str], self.plugin_version)
        if self.source is not None:
            result["source"] = from_union([from_none, lambda x: to_enum(McpServerSource, x)], self.source)
        if self.transport is not None:
            result["transport"] = from_union([from_none, lambda x: to_enum(McpServerTransport, x)], self.transport)
        return result


@dataclass
class McpToolsListChangedData:
    "Payload identifying the MCP server associated with a list change."
    server_name: str

    @staticmethod
    def from_dict(obj: Any) -> "McpToolsListChangedData":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        return McpToolsListChangedData(
            server_name=server_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        return result


@dataclass
class ModelCallFailureData:
    "Failed LLM API call metadata for telemetry"
    source: ModelCallFailureSource
    api_call_id: str | None = None
    api_endpoint: AssistantUsageApiEndpoint | None = None
    bad_request_kind: ModelCallFailureBadRequestKind | None = None
    duration: timedelta | None = None
    error_code: str | None = None
    error_message: str | None = None
    error_type: str | None = None
    failure_kind: ModelCallFailureKind | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    fusion: FusionAttribution | None = None
    initiator: str | None = None
    interaction_type: str | None = None
    is_auto: bool | None = None
    is_byok: bool | None = None
    max_output_tokens: int | None = None
    max_prompt_tokens: int | None = None
    model: str | None = None
    provider_call_id: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _quota_snapshots: dict[str, _AssistantUsageQuotaSnapshot] | None = None
    reasoning_effort: str | None = None
    request_fingerprint: ModelCallFailureRequestFingerprint | None = None
    rte: bool | None = None
    service_request_id: str | None = None
    status_code: int | None = None
    transport: ModelCallFailureTransport | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ModelCallFailureData":
        assert isinstance(obj, dict)
        source = parse_enum(ModelCallFailureSource, obj.get("source"))
        api_call_id = from_union([from_none, from_str], obj.get("apiCallId"))
        api_endpoint = from_union([from_none, lambda x: parse_enum(AssistantUsageApiEndpoint, x)], obj.get("apiEndpoint"))
        bad_request_kind = from_union([from_none, lambda x: parse_enum(ModelCallFailureBadRequestKind, x)], obj.get("badRequestKind"))
        duration = from_union([from_none, from_timedelta], obj.get("durationMs"))
        error_code = from_union([from_none, from_str], obj.get("errorCode"))
        error_message = from_union([from_none, from_str], obj.get("errorMessage"))
        error_type = from_union([from_none, from_str], obj.get("errorType"))
        failure_kind = from_union([from_none, lambda x: parse_enum(ModelCallFailureKind, x)], obj.get("failureKind"))
        fusion = from_union([from_none, FusionAttribution.from_dict], obj.get("fusion"))
        initiator = from_union([from_none, from_str], obj.get("initiator"))
        interaction_type = from_union([from_none, from_str], obj.get("interactionType"))
        is_auto = from_union([from_none, from_bool], obj.get("isAuto"))
        is_byok = from_union([from_none, from_bool], obj.get("isByok"))
        max_output_tokens = from_union([from_none, from_int], obj.get("maxOutputTokens"))
        max_prompt_tokens = from_union([from_none, from_int], obj.get("maxPromptTokens"))
        model = from_union([from_none, from_str], obj.get("model"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        _quota_snapshots = from_union([from_none, lambda x: from_dict(_AssistantUsageQuotaSnapshot.from_dict, x)], obj.get("quotaSnapshots"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        request_fingerprint = from_union([from_none, ModelCallFailureRequestFingerprint.from_dict], obj.get("requestFingerprint"))
        rte = from_union([from_none, from_bool], obj.get("rte"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        status_code = from_union([from_none, from_int], obj.get("statusCode"))
        transport = from_union([from_none, lambda x: parse_enum(ModelCallFailureTransport, x)], obj.get("transport"))
        return ModelCallFailureData(
            source=source,
            api_call_id=api_call_id,
            api_endpoint=api_endpoint,
            bad_request_kind=bad_request_kind,
            duration=duration,
            error_code=error_code,
            error_message=error_message,
            error_type=error_type,
            failure_kind=failure_kind,
            fusion=fusion,
            initiator=initiator,
            interaction_type=interaction_type,
            is_auto=is_auto,
            is_byok=is_byok,
            max_output_tokens=max_output_tokens,
            max_prompt_tokens=max_prompt_tokens,
            model=model,
            provider_call_id=provider_call_id,
            _quota_snapshots=_quota_snapshots,
            reasoning_effort=reasoning_effort,
            request_fingerprint=request_fingerprint,
            rte=rte,
            service_request_id=service_request_id,
            status_code=status_code,
            transport=transport,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["source"] = to_enum(ModelCallFailureSource, self.source)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_none, from_str], self.api_call_id)
        if self.api_endpoint is not None:
            result["apiEndpoint"] = from_union([from_none, lambda x: to_enum(AssistantUsageApiEndpoint, x)], self.api_endpoint)
        if self.bad_request_kind is not None:
            result["badRequestKind"] = from_union([from_none, lambda x: to_enum(ModelCallFailureBadRequestKind, x)], self.bad_request_kind)
        if self.duration is not None:
            result["durationMs"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.error_code is not None:
            result["errorCode"] = from_union([from_none, from_str], self.error_code)
        if self.error_message is not None:
            result["errorMessage"] = from_union([from_none, from_str], self.error_message)
        if self.error_type is not None:
            result["errorType"] = from_union([from_none, from_str], self.error_type)
        if self.failure_kind is not None:
            result["failureKind"] = from_union([from_none, lambda x: to_enum(ModelCallFailureKind, x)], self.failure_kind)
        if self.fusion is not None:
            result["fusion"] = from_union([from_none, lambda x: to_class(FusionAttribution, x)], self.fusion)
        if self.initiator is not None:
            result["initiator"] = from_union([from_none, from_str], self.initiator)
        if self.interaction_type is not None:
            result["interactionType"] = from_union([from_none, from_str], self.interaction_type)
        if self.is_auto is not None:
            result["isAuto"] = from_union([from_none, from_bool], self.is_auto)
        if self.is_byok is not None:
            result["isByok"] = from_union([from_none, from_bool], self.is_byok)
        if self.max_output_tokens is not None:
            result["maxOutputTokens"] = from_union([from_none, to_int], self.max_output_tokens)
        if self.max_prompt_tokens is not None:
            result["maxPromptTokens"] = from_union([from_none, to_int], self.max_prompt_tokens)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self._quota_snapshots is not None:
            result["quotaSnapshots"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(_AssistantUsageQuotaSnapshot, x), x)], self._quota_snapshots)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.request_fingerprint is not None:
            result["requestFingerprint"] = from_union([from_none, lambda x: to_class(ModelCallFailureRequestFingerprint, x)], self.request_fingerprint)
        if self.rte is not None:
            result["rte"] = from_union([from_none, from_bool], self.rte)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.status_code is not None:
            result["statusCode"] = from_union([from_none, to_int], self.status_code)
        if self.transport is not None:
            result["transport"] = from_union([from_none, lambda x: to_enum(ModelCallFailureTransport, x)], self.transport)
        return result


@dataclass
class ModelCallFailureRequestFingerprint:
    "Content-free structural summary of the failing request for diagnosing malformed 4xx calls"
    image_part_count: int
    image_parts_missing_media_type: int
    message_count: int
    nameless_tool_call_count: int
    tool_call_count: int
    tool_result_message_count: int
    last_message_role: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ModelCallFailureRequestFingerprint":
        assert isinstance(obj, dict)
        image_part_count = from_int(obj.get("imagePartCount"))
        image_parts_missing_media_type = from_int(obj.get("imagePartsMissingMediaType"))
        message_count = from_int(obj.get("messageCount"))
        nameless_tool_call_count = from_int(obj.get("namelessToolCallCount"))
        tool_call_count = from_int(obj.get("toolCallCount"))
        tool_result_message_count = from_int(obj.get("toolResultMessageCount"))
        last_message_role = from_union([from_none, from_str], obj.get("lastMessageRole"))
        return ModelCallFailureRequestFingerprint(
            image_part_count=image_part_count,
            image_parts_missing_media_type=image_parts_missing_media_type,
            message_count=message_count,
            nameless_tool_call_count=nameless_tool_call_count,
            tool_call_count=tool_call_count,
            tool_result_message_count=tool_result_message_count,
            last_message_role=last_message_role,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["imagePartCount"] = to_int(self.image_part_count)
        result["imagePartsMissingMediaType"] = to_int(self.image_parts_missing_media_type)
        result["messageCount"] = to_int(self.message_count)
        result["namelessToolCallCount"] = to_int(self.nameless_tool_call_count)
        result["toolCallCount"] = to_int(self.tool_call_count)
        result["toolResultMessageCount"] = to_int(self.tool_result_message_count)
        if self.last_message_role is not None:
            result["lastMessageRole"] = from_union([from_none, from_str], self.last_message_role)
        return result


@dataclass
class ModelCallFinishedData:
    "Final lifecycle outcome for one logical model dispatch. A logical dispatch may include internal reconnect or fallback work, so event count is not provider HTTP-request count."
    dispatch_duration: timedelta
    edit_classifier_version: int
    outcome: ModelCallFinishedOutcome
    turn_id: str
    contains_built_in_file_edit_request: bool | None = None
    interaction_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ModelCallFinishedData":
        assert isinstance(obj, dict)
        dispatch_duration = from_timedelta(obj.get("dispatchDurationMs"))
        edit_classifier_version = from_int(obj.get("editClassifierVersion"))
        outcome = parse_enum(ModelCallFinishedOutcome, obj.get("outcome"))
        turn_id = from_str(obj.get("turnId"))
        contains_built_in_file_edit_request = from_union([from_none, from_bool], obj.get("containsBuiltInFileEditRequest"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        return ModelCallFinishedData(
            dispatch_duration=dispatch_duration,
            edit_classifier_version=edit_classifier_version,
            outcome=outcome,
            turn_id=turn_id,
            contains_built_in_file_edit_request=contains_built_in_file_edit_request,
            interaction_id=interaction_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["dispatchDurationMs"] = to_timedelta(self.dispatch_duration)
        result["editClassifierVersion"] = to_int(self.edit_classifier_version)
        result["outcome"] = to_enum(ModelCallFinishedOutcome, self.outcome)
        result["turnId"] = from_str(self.turn_id)
        if self.contains_built_in_file_edit_request is not None:
            result["containsBuiltInFileEditRequest"] = from_union([from_none, from_bool], self.contains_built_in_file_edit_request)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        return result


@dataclass
class ModelCallStartData:
    "Model API dispatch metadata for internal telemetry"
    turn_id: str
    # Experimental: this field is part of an experimental API and may change or be removed.
    fusion: FusionAttribution | None = None
    model: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _previous_response_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ModelCallStartData":
        assert isinstance(obj, dict)
        turn_id = from_str(obj.get("turnId"))
        fusion = from_union([from_none, FusionAttribution.from_dict], obj.get("fusion"))
        model = from_union([from_none, from_str], obj.get("model"))
        _previous_response_id = from_union([from_none, from_str], obj.get("previousResponseId"))
        return ModelCallStartData(
            turn_id=turn_id,
            fusion=fusion,
            model=model,
            _previous_response_id=_previous_response_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["turnId"] = from_str(self.turn_id)
        if self.fusion is not None:
            result["fusion"] = from_union([from_none, lambda x: to_class(FusionAttribution, x)], self.fusion)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self._previous_response_id is not None:
            result["previousResponseId"] = from_union([from_none, from_str], self._previous_response_id)
        return result


@dataclass
class PendingMessagesModifiedData:
    "Empty payload; the event signals that the pending message queue has changed"
    @staticmethod
    def from_dict(obj: Any) -> "PendingMessagesModifiedData":
        assert isinstance(obj, dict)
        return PendingMessagesModifiedData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class PermissionApproved:
    "Permission response variant indicating the request was approved without persisting an approval rule."
    kind: ClassVar[str] = "approved"
    managed_approval_handled: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionApproved":
        assert isinstance(obj, dict)
        managed_approval_handled = from_union([from_none, from_bool], obj.get("managedApprovalHandled"))
        return PermissionApproved(
            managed_approval_handled=managed_approval_handled,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.managed_approval_handled is not None:
            result["managedApprovalHandled"] = from_union([from_none, from_bool], self.managed_approval_handled)
        return result


@dataclass
class PermissionApprovedForLocation:
    "Permission response variant that approves a request and persists the provided approval to a project location key."
    approval: UserToolSessionApproval
    kind: ClassVar[str] = "approved-for-location"
    location_key: str
    managed_approval_handled: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionApprovedForLocation":
        assert isinstance(obj, dict)
        approval = _load_UserToolSessionApproval(obj.get("approval"))
        location_key = from_str(obj.get("locationKey"))
        managed_approval_handled = from_union([from_none, from_bool], obj.get("managedApprovalHandled"))
        return PermissionApprovedForLocation(
            approval=approval,
            location_key=location_key,
            managed_approval_handled=managed_approval_handled,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = self.approval.to_dict()
        result["kind"] = self.kind
        result["locationKey"] = from_str(self.location_key)
        if self.managed_approval_handled is not None:
            result["managedApprovalHandled"] = from_union([from_none, from_bool], self.managed_approval_handled)
        return result


@dataclass
class PermissionApprovedForSession:
    "Permission response variant that approves a request and remembers the provided approval for the rest of the session."
    approval: UserToolSessionApproval
    kind: ClassVar[str] = "approved-for-session"
    managed_approval_handled: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionApprovedForSession":
        assert isinstance(obj, dict)
        approval = _load_UserToolSessionApproval(obj.get("approval"))
        managed_approval_handled = from_union([from_none, from_bool], obj.get("managedApprovalHandled"))
        return PermissionApprovedForSession(
            approval=approval,
            managed_approval_handled=managed_approval_handled,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = self.approval.to_dict()
        result["kind"] = self.kind
        if self.managed_approval_handled is not None:
            result["managedApprovalHandled"] = from_union([from_none, from_bool], self.managed_approval_handled)
        return result


@dataclass
class PermissionCancelled:
    "Permission response variant indicating the request was cancelled before use, with an optional reason."
    kind: ClassVar[str] = "cancelled"
    reason: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionCancelled":
        assert isinstance(obj, dict)
        reason = from_union([from_none, from_str], obj.get("reason"))
        return PermissionCancelled(
            reason=reason,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        return result


@dataclass
class PermissionCompletedData:
    "Permission request completion notification signaling UI dismissal"
    request_id: str
    result: PermissionResult
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = _load_PermissionResult(obj.get("result"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionCompletedData(
            request_id=request_id,
            result=result,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = self.result.to_dict()
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionDeniedByContentExclusionPolicy:
    "Permission response variant denying a path under content exclusion policy, with the path and message."
    kind: ClassVar[str] = "denied-by-content-exclusion-policy"
    message: str
    path: str

    @staticmethod
    def from_dict(obj: Any) -> "PermissionDeniedByContentExclusionPolicy":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        path = from_str(obj.get("path"))
        return PermissionDeniedByContentExclusionPolicy(
            message=message,
            path=path,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["message"] = from_str(self.message)
        result["path"] = from_str(self.path)
        return result


@dataclass
class PermissionDeniedByPermissionRequestHook:
    "Permission response variant denied by a permission-request hook, with optional message and interrupt flag."
    kind: ClassVar[str] = "denied-by-permission-request-hook"
    interrupt: bool | None = None
    message: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionDeniedByPermissionRequestHook":
        assert isinstance(obj, dict)
        interrupt = from_union([from_none, from_bool], obj.get("interrupt"))
        message = from_union([from_none, from_str], obj.get("message"))
        return PermissionDeniedByPermissionRequestHook(
            interrupt=interrupt,
            message=message,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.interrupt is not None:
            result["interrupt"] = from_union([from_none, from_bool], self.interrupt)
        if self.message is not None:
            result["message"] = from_union([from_none, from_str], self.message)
        return result


@dataclass
class PermissionDeniedByRules:
    "Permission response variant denied because matching approval rules explicitly blocked the request."
    kind: ClassVar[str] = "denied-by-rules"
    rules: list[PermissionRule]

    @staticmethod
    def from_dict(obj: Any) -> "PermissionDeniedByRules":
        assert isinstance(obj, dict)
        rules = from_list(PermissionRule.from_dict, obj.get("rules"))
        return PermissionDeniedByRules(
            rules=rules,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["rules"] = from_list(lambda x: to_class(PermissionRule, x), self.rules)
        return result


@dataclass
class PermissionDeniedInteractivelyByUser:
    "Permission response variant denied in an interactive user prompt, with optional feedback and force-reject flag."
    kind: ClassVar[str] = "denied-interactively-by-user"
    feedback: str | None = None
    force_reject: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionDeniedInteractivelyByUser":
        assert isinstance(obj, dict)
        feedback = from_union([from_none, from_str], obj.get("feedback"))
        force_reject = from_union([from_none, from_bool], obj.get("forceReject"))
        return PermissionDeniedInteractivelyByUser(
            feedback=feedback,
            force_reject=force_reject,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.feedback is not None:
            result["feedback"] = from_union([from_none, from_str], self.feedback)
        if self.force_reject is not None:
            result["forceReject"] = from_union([from_none, from_bool], self.force_reject)
        return result


@dataclass
class PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser:
    "Permission response variant denied because no approval rule matched and user confirmation was unavailable."
    kind: ClassVar[str] = "denied-no-approval-rule-and-could-not-request-from-user"

    @staticmethod
    def from_dict(obj: Any) -> "PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser":
        assert isinstance(obj, dict)
        return PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser(
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result


@dataclass
class PermissionPromptRequestCommands:
    "Shell command permission prompt"
    can_offer_session_approval: bool
    command_identifiers: list[str]
    full_command_text: str
    intention: str
    kind: ClassVar[str] = "commands"
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    managed_approval_required: bool | None = None
    tool_call_id: str | None = None
    warning: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestCommands":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        command_identifiers = from_list(from_str, obj.get("commandIdentifiers"))
        full_command_text = from_str(obj.get("fullCommandText"))
        intention = from_str(obj.get("intention"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        warning = from_union([from_none, from_str], obj.get("warning"))
        return PermissionPromptRequestCommands(
            can_offer_session_approval=can_offer_session_approval,
            command_identifiers=command_identifiers,
            full_command_text=full_command_text,
            intention=intention,
            assisted_approval=assisted_approval,
            managed_approval_required=managed_approval_required,
            tool_call_id=tool_call_id,
            warning=warning,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canOfferSessionApproval"] = from_bool(self.can_offer_session_approval)
        result["commandIdentifiers"] = from_list(from_str, self.command_identifiers)
        result["fullCommandText"] = from_str(self.full_command_text)
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.warning is not None:
            result["warning"] = from_union([from_none, from_str], self.warning)
        return result


@dataclass
class PermissionPromptRequestCustomTool:
    "Custom tool invocation permission prompt"
    kind: ClassVar[str] = "custom-tool"
    tool_description: str
    tool_name: str
    args: Any = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestCustomTool":
        assert isinstance(obj, dict)
        tool_description = from_str(obj.get("toolDescription"))
        tool_name = from_str(obj.get("toolName"))
        args = obj.get("args")
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestCustomTool(
            tool_description=tool_description,
            tool_name=tool_name,
            args=args,
            assisted_approval=assisted_approval,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolDescription"] = from_str(self.tool_description)
        result["toolName"] = from_str(self.tool_name)
        if self.args is not None:
            result["args"] = self.args
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestExtensionEnvAccess:
    "Extension sensitive environment variable access prompt"
    environment_variables: list[str]
    extension_name: str
    kind: ClassVar[str] = "extension-env-access"
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestExtensionEnvAccess":
        assert isinstance(obj, dict)
        environment_variables = from_list(from_str, obj.get("environmentVariables"))
        extension_name = from_str(obj.get("extensionName"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestExtensionEnvAccess(
            environment_variables=environment_variables,
            extension_name=extension_name,
            assisted_approval=assisted_approval,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["environmentVariables"] = from_list(from_str, self.environment_variables)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestExtensionManagement:
    "Extension management permission prompt"
    kind: ClassVar[str] = "extension-management"
    operation: str
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    extension_name: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestExtensionManagement":
        assert isinstance(obj, dict)
        operation = from_str(obj.get("operation"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        extension_name = from_union([from_none, from_str], obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestExtensionManagement(
            operation=operation,
            assisted_approval=assisted_approval,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["operation"] = from_str(self.operation)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.extension_name is not None:
            result["extensionName"] = from_union([from_none, from_str], self.extension_name)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestExtensionPermissionAccess:
    "Extension permission access prompt"
    capabilities: list[str]
    extension_name: str
    kind: ClassVar[str] = "extension-permission-access"
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestExtensionPermissionAccess":
        assert isinstance(obj, dict)
        capabilities = from_list(from_str, obj.get("capabilities"))
        extension_name = from_str(obj.get("extensionName"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestExtensionPermissionAccess(
            capabilities=capabilities,
            extension_name=extension_name,
            assisted_approval=assisted_approval,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = from_list(from_str, self.capabilities)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestFactory:
    "Factory run or authoring permission prompt"
    approval_key: str
    can_persist_approval: bool
    description: str
    kind: ClassVar[str] = "factory"
    name: str
    operation: FactoryPermissionOperation
    phases: list[FactoryPermissionPhase]
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    declared_max_ai_credits: float | None = None
    declared_max_concurrent_subagents: int | None = None
    declared_max_total_subagents: int | None = None
    declared_timeout_seconds: float | None = None
    managed_approval_required: bool | None = None
    max_ai_credits: float | None = None
    max_concurrent_subagents: int | None = None
    max_total_subagents: int | None = None
    timeout_seconds: float | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestFactory":
        assert isinstance(obj, dict)
        approval_key = from_str(obj.get("approvalKey"))
        can_persist_approval = from_bool(obj.get("canPersistApproval"))
        description = from_str(obj.get("description"))
        name = from_str(obj.get("name"))
        operation = parse_enum(FactoryPermissionOperation, obj.get("operation"))
        phases = from_list(FactoryPermissionPhase.from_dict, obj.get("phases"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        declared_max_ai_credits = from_union([from_none, from_float], obj.get("declaredMaxAiCredits"))
        declared_max_concurrent_subagents = from_union([from_none, from_int], obj.get("declaredMaxConcurrentSubagents"))
        declared_max_total_subagents = from_union([from_none, from_int], obj.get("declaredMaxTotalSubagents"))
        declared_timeout_seconds = from_union([from_none, from_float], obj.get("declaredTimeoutSeconds"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        max_ai_credits = from_union([from_none, from_float], obj.get("maxAiCredits"))
        max_concurrent_subagents = from_union([from_none, from_int], obj.get("maxConcurrentSubagents"))
        max_total_subagents = from_union([from_none, from_int], obj.get("maxTotalSubagents"))
        timeout_seconds = from_union([from_none, from_float], obj.get("timeoutSeconds"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestFactory(
            approval_key=approval_key,
            can_persist_approval=can_persist_approval,
            description=description,
            name=name,
            operation=operation,
            phases=phases,
            assisted_approval=assisted_approval,
            declared_max_ai_credits=declared_max_ai_credits,
            declared_max_concurrent_subagents=declared_max_concurrent_subagents,
            declared_max_total_subagents=declared_max_total_subagents,
            declared_timeout_seconds=declared_timeout_seconds,
            managed_approval_required=managed_approval_required,
            max_ai_credits=max_ai_credits,
            max_concurrent_subagents=max_concurrent_subagents,
            max_total_subagents=max_total_subagents,
            timeout_seconds=timeout_seconds,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["approvalKey"] = from_str(self.approval_key)
        result["canPersistApproval"] = from_bool(self.can_persist_approval)
        result["description"] = from_str(self.description)
        result["kind"] = self.kind
        result["name"] = from_str(self.name)
        result["operation"] = to_enum(FactoryPermissionOperation, self.operation)
        result["phases"] = from_list(lambda x: to_class(FactoryPermissionPhase, x), self.phases)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.declared_max_ai_credits is not None:
            result["declaredMaxAiCredits"] = from_union([from_none, to_float], self.declared_max_ai_credits)
        if self.declared_max_concurrent_subagents is not None:
            result["declaredMaxConcurrentSubagents"] = from_union([from_none, to_int], self.declared_max_concurrent_subagents)
        if self.declared_max_total_subagents is not None:
            result["declaredMaxTotalSubagents"] = from_union([from_none, to_int], self.declared_max_total_subagents)
        if self.declared_timeout_seconds is not None:
            result["declaredTimeoutSeconds"] = from_union([from_none, to_float], self.declared_timeout_seconds)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.max_ai_credits is not None:
            result["maxAiCredits"] = from_union([from_none, to_float], self.max_ai_credits)
        if self.max_concurrent_subagents is not None:
            result["maxConcurrentSubagents"] = from_union([from_none, to_int], self.max_concurrent_subagents)
        if self.max_total_subagents is not None:
            result["maxTotalSubagents"] = from_union([from_none, to_int], self.max_total_subagents)
        if self.timeout_seconds is not None:
            result["timeoutSeconds"] = from_union([from_none, to_float], self.timeout_seconds)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestHook:
    "Hook confirmation permission prompt"
    kind: ClassVar[str] = "hook"
    tool_name: str
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    hook_message: str | None = None
    tool_args: Any = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestHook":
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        hook_message = from_union([from_none, from_str], obj.get("hookMessage"))
        tool_args = obj.get("toolArgs")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestHook(
            tool_name=tool_name,
            assisted_approval=assisted_approval,
            hook_message=hook_message,
            tool_args=tool_args,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolName"] = from_str(self.tool_name)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.hook_message is not None:
            result["hookMessage"] = from_union([from_none, from_str], self.hook_message)
        if self.tool_args is not None:
            result["toolArgs"] = self.tool_args
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestMcp:
    "MCP tool invocation permission prompt"
    kind: ClassVar[str] = "mcp"
    server_name: str
    tool_name: str
    tool_title: str
    args: Any = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    can_offer_server_wide_approval: bool | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    permission_recommendation: PermissionRecommendation | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestMcp":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        tool_name = from_str(obj.get("toolName"))
        tool_title = from_str(obj.get("toolTitle"))
        args = obj.get("args")
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        can_offer_server_wide_approval = from_union([from_none, from_bool], obj.get("canOfferServerWideApproval"))
        permission_recommendation = from_union([from_none, lambda x: parse_enum(PermissionRecommendation, x)], obj.get("permissionRecommendation"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestMcp(
            server_name=server_name,
            tool_name=tool_name,
            tool_title=tool_title,
            args=args,
            assisted_approval=assisted_approval,
            can_offer_server_wide_approval=can_offer_server_wide_approval,
            permission_recommendation=permission_recommendation,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_str(self.tool_name)
        result["toolTitle"] = from_str(self.tool_title)
        if self.args is not None:
            result["args"] = self.args
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.can_offer_server_wide_approval is not None:
            result["canOfferServerWideApproval"] = from_union([from_none, from_bool], self.can_offer_server_wide_approval)
        if self.permission_recommendation is not None:
            result["permissionRecommendation"] = from_union([from_none, lambda x: to_enum(PermissionRecommendation, x)], self.permission_recommendation)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestMemory:
    "Memory operation permission prompt"
    fact: str
    kind: ClassVar[str] = "memory"
    action: PermissionRequestMemoryAction | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    citations: str | None = None
    direction: PermissionRequestMemoryDirection | None = None
    reason: str | None = None
    subject: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestMemory":
        assert isinstance(obj, dict)
        fact = from_str(obj.get("fact"))
        action = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryAction, x)], obj.get("action"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        citations = from_union([from_none, from_str], obj.get("citations"))
        direction = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryDirection, x)], obj.get("direction"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        subject = from_union([from_none, from_str], obj.get("subject"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestMemory(
            fact=fact,
            action=action,
            assisted_approval=assisted_approval,
            citations=citations,
            direction=direction,
            reason=reason,
            subject=subject,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["fact"] = from_str(self.fact)
        result["kind"] = self.kind
        if self.action is not None:
            result["action"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryAction, x)], self.action)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.citations is not None:
            result["citations"] = from_union([from_none, from_str], self.citations)
        if self.direction is not None:
            result["direction"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryDirection, x)], self.direction)
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        if self.subject is not None:
            result["subject"] = from_union([from_none, from_str], self.subject)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestPath:
    "Path access permission prompt"
    access_kind: PermissionPromptRequestPathAccessKind
    kind: ClassVar[str] = "path"
    paths: list[str]
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestPath":
        assert isinstance(obj, dict)
        access_kind = parse_enum(PermissionPromptRequestPathAccessKind, obj.get("accessKind"))
        paths = from_list(from_str, obj.get("paths"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestPath(
            access_kind=access_kind,
            paths=paths,
            assisted_approval=assisted_approval,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["accessKind"] = to_enum(PermissionPromptRequestPathAccessKind, self.access_kind)
        result["kind"] = self.kind
        result["paths"] = from_list(from_str, self.paths)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestRead:
    "File read permission prompt"
    intention: str
    kind: ClassVar[str] = "read"
    path: str
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    managed_approval_required: bool | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestRead":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        path = from_str(obj.get("path"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestRead(
            intention=intention,
            path=path,
            assisted_approval=assisted_approval,
            managed_approval_required=managed_approval_required,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["path"] = from_str(self.path)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestUrl:
    "URL access permission prompt"
    intention: str
    kind: ClassVar[str] = "url"
    url: str
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    managed_approval_required: bool | None = None
    redirected_from: str | None = None
    request_sandbox_bypass: bool | None = None
    request_sandbox_bypass_reason: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestUrl":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        url = from_str(obj.get("url"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        redirected_from = from_union([from_none, from_str], obj.get("redirectedFrom"))
        request_sandbox_bypass = from_union([from_none, from_bool], obj.get("requestSandboxBypass"))
        request_sandbox_bypass_reason = from_union([from_none, from_str], obj.get("requestSandboxBypassReason"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestUrl(
            intention=intention,
            url=url,
            assisted_approval=assisted_approval,
            managed_approval_required=managed_approval_required,
            redirected_from=redirected_from,
            request_sandbox_bypass=request_sandbox_bypass,
            request_sandbox_bypass_reason=request_sandbox_bypass_reason,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["url"] = from_str(self.url)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.redirected_from is not None:
            result["redirectedFrom"] = from_union([from_none, from_str], self.redirected_from)
        if self.request_sandbox_bypass is not None:
            result["requestSandboxBypass"] = from_union([from_none, from_bool], self.request_sandbox_bypass)
        if self.request_sandbox_bypass_reason is not None:
            result["requestSandboxBypassReason"] = from_union([from_none, from_str], self.request_sandbox_bypass_reason)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestWrite:
    "File write permission prompt"
    can_offer_session_approval: bool
    diff: str
    file_name: str
    intention: str
    kind: ClassVar[str] = "write"
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    managed_approval_required: bool | None = None
    new_file_contents: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestWrite":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        diff = from_str(obj.get("diff"))
        file_name = from_str(obj.get("fileName"))
        intention = from_str(obj.get("intention"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        new_file_contents = from_union([from_none, from_str], obj.get("newFileContents"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestWrite(
            can_offer_session_approval=can_offer_session_approval,
            diff=diff,
            file_name=file_name,
            intention=intention,
            assisted_approval=assisted_approval,
            managed_approval_required=managed_approval_required,
            new_file_contents=new_file_contents,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canOfferSessionApproval"] = from_bool(self.can_offer_session_approval)
        result["diff"] = from_str(self.diff)
        result["fileName"] = from_str(self.file_name)
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.new_file_contents is not None:
            result["newFileContents"] = from_union([from_none, from_str], self.new_file_contents)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestCustomTool:
    "Custom tool invocation permission request"
    kind: ClassVar[str] = "custom-tool"
    tool_description: str
    tool_name: str
    args: Any = None
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None
    skip_permission: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestCustomTool":
        assert isinstance(obj, dict)
        tool_description = from_str(obj.get("toolDescription"))
        tool_name = from_str(obj.get("toolName"))
        args = obj.get("args")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        skip_permission = from_union([from_none, from_bool], obj.get("skipPermission"))
        return PermissionRequestCustomTool(
            tool_description=tool_description,
            tool_name=tool_name,
            args=args,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
            skip_permission=skip_permission,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolDescription"] = from_str(self.tool_description)
        result["toolName"] = from_str(self.tool_name)
        if self.args is not None:
            result["args"] = self.args
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.skip_permission is not None:
            result["skipPermission"] = from_union([from_none, from_bool], self.skip_permission)
        return result


@dataclass
class PermissionRequestExtensionEnvAccess:
    "Extension sensitive environment variable access request"
    environment_variables: list[str]
    extension_name: str
    kind: ClassVar[str] = "extension-env-access"
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestExtensionEnvAccess":
        assert isinstance(obj, dict)
        environment_variables = from_list(from_str, obj.get("environmentVariables"))
        extension_name = from_str(obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestExtensionEnvAccess(
            environment_variables=environment_variables,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["environmentVariables"] = from_list(from_str, self.environment_variables)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestExtensionManagement:
    "Extension management permission request"
    kind: ClassVar[str] = "extension-management"
    operation: str
    extension_name: str | None = None
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestExtensionManagement":
        assert isinstance(obj, dict)
        operation = from_str(obj.get("operation"))
        extension_name = from_union([from_none, from_str], obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestExtensionManagement(
            operation=operation,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["operation"] = from_str(self.operation)
        if self.extension_name is not None:
            result["extensionName"] = from_union([from_none, from_str], self.extension_name)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestExtensionPermissionAccess:
    "Extension permission access request"
    capabilities: list[str]
    extension_name: str
    kind: ClassVar[str] = "extension-permission-access"
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestExtensionPermissionAccess":
        assert isinstance(obj, dict)
        capabilities = from_list(from_str, obj.get("capabilities"))
        extension_name = from_str(obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestExtensionPermissionAccess(
            capabilities=capabilities,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = from_list(from_str, self.capabilities)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestFactory:
    "Factory run or authoring permission request"
    approval_key: str
    can_persist_approval: bool
    description: str
    kind: ClassVar[str] = "factory"
    name: str
    operation: FactoryPermissionOperation
    phases: list[FactoryPermissionPhase]
    declared_max_ai_credits: float | None = None
    declared_max_concurrent_subagents: int | None = None
    declared_max_total_subagents: int | None = None
    declared_timeout_seconds: float | None = None
    max_ai_credits: float | None = None
    max_concurrent_subagents: int | None = None
    max_total_subagents: int | None = None
    timeout_seconds: float | None = None
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestFactory":
        assert isinstance(obj, dict)
        approval_key = from_str(obj.get("approvalKey"))
        can_persist_approval = from_bool(obj.get("canPersistApproval"))
        description = from_str(obj.get("description"))
        name = from_str(obj.get("name"))
        operation = parse_enum(FactoryPermissionOperation, obj.get("operation"))
        phases = from_list(FactoryPermissionPhase.from_dict, obj.get("phases"))
        declared_max_ai_credits = from_union([from_none, from_float], obj.get("declaredMaxAiCredits"))
        declared_max_concurrent_subagents = from_union([from_none, from_int], obj.get("declaredMaxConcurrentSubagents"))
        declared_max_total_subagents = from_union([from_none, from_int], obj.get("declaredMaxTotalSubagents"))
        declared_timeout_seconds = from_union([from_none, from_float], obj.get("declaredTimeoutSeconds"))
        max_ai_credits = from_union([from_none, from_float], obj.get("maxAiCredits"))
        max_concurrent_subagents = from_union([from_none, from_int], obj.get("maxConcurrentSubagents"))
        max_total_subagents = from_union([from_none, from_int], obj.get("maxTotalSubagents"))
        timeout_seconds = from_union([from_none, from_float], obj.get("timeoutSeconds"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestFactory(
            approval_key=approval_key,
            can_persist_approval=can_persist_approval,
            description=description,
            name=name,
            operation=operation,
            phases=phases,
            declared_max_ai_credits=declared_max_ai_credits,
            declared_max_concurrent_subagents=declared_max_concurrent_subagents,
            declared_max_total_subagents=declared_max_total_subagents,
            declared_timeout_seconds=declared_timeout_seconds,
            max_ai_credits=max_ai_credits,
            max_concurrent_subagents=max_concurrent_subagents,
            max_total_subagents=max_total_subagents,
            timeout_seconds=timeout_seconds,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["approvalKey"] = from_str(self.approval_key)
        result["canPersistApproval"] = from_bool(self.can_persist_approval)
        result["description"] = from_str(self.description)
        result["kind"] = self.kind
        result["name"] = from_str(self.name)
        result["operation"] = to_enum(FactoryPermissionOperation, self.operation)
        result["phases"] = from_list(lambda x: to_class(FactoryPermissionPhase, x), self.phases)
        if self.declared_max_ai_credits is not None:
            result["declaredMaxAiCredits"] = from_union([from_none, to_float], self.declared_max_ai_credits)
        if self.declared_max_concurrent_subagents is not None:
            result["declaredMaxConcurrentSubagents"] = from_union([from_none, to_int], self.declared_max_concurrent_subagents)
        if self.declared_max_total_subagents is not None:
            result["declaredMaxTotalSubagents"] = from_union([from_none, to_int], self.declared_max_total_subagents)
        if self.declared_timeout_seconds is not None:
            result["declaredTimeoutSeconds"] = from_union([from_none, to_float], self.declared_timeout_seconds)
        if self.max_ai_credits is not None:
            result["maxAiCredits"] = from_union([from_none, to_float], self.max_ai_credits)
        if self.max_concurrent_subagents is not None:
            result["maxConcurrentSubagents"] = from_union([from_none, to_int], self.max_concurrent_subagents)
        if self.max_total_subagents is not None:
            result["maxTotalSubagents"] = from_union([from_none, to_int], self.max_total_subagents)
        if self.timeout_seconds is not None:
            result["timeoutSeconds"] = from_union([from_none, to_float], self.timeout_seconds)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestHook:
    "Hook confirmation permission request"
    kind: ClassVar[str] = "hook"
    tool_name: str
    hook_message: str | None = None
    tool_args: Any = None
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestHook":
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        hook_message = from_union([from_none, from_str], obj.get("hookMessage"))
        tool_args = obj.get("toolArgs")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestHook(
            tool_name=tool_name,
            hook_message=hook_message,
            tool_args=tool_args,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolName"] = from_str(self.tool_name)
        if self.hook_message is not None:
            result["hookMessage"] = from_union([from_none, from_str], self.hook_message)
        if self.tool_args is not None:
            result["toolArgs"] = self.tool_args
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestMcp:
    "MCP tool invocation permission request"
    kind: ClassVar[str] = "mcp"
    read_only: bool
    server_name: str
    tool_name: str
    tool_title: str
    args: Any = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    permission_recommendation: PermissionRecommendation | None = None
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestMcp":
        assert isinstance(obj, dict)
        read_only = from_bool(obj.get("readOnly"))
        server_name = from_str(obj.get("serverName"))
        tool_name = from_str(obj.get("toolName"))
        tool_title = from_str(obj.get("toolTitle"))
        args = obj.get("args")
        permission_recommendation = from_union([from_none, lambda x: parse_enum(PermissionRecommendation, x)], obj.get("permissionRecommendation"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestMcp(
            read_only=read_only,
            server_name=server_name,
            tool_name=tool_name,
            tool_title=tool_title,
            args=args,
            permission_recommendation=permission_recommendation,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["readOnly"] = from_bool(self.read_only)
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_str(self.tool_name)
        result["toolTitle"] = from_str(self.tool_title)
        if self.args is not None:
            result["args"] = self.args
        if self.permission_recommendation is not None:
            result["permissionRecommendation"] = from_union([from_none, lambda x: to_enum(PermissionRecommendation, x)], self.permission_recommendation)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestMemory:
    "Memory operation permission request"
    fact: str
    kind: ClassVar[str] = "memory"
    action: PermissionRequestMemoryAction | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    assisted_approval: PermissionAssistedApproval | None = None
    citations: str | None = None
    direction: PermissionRequestMemoryDirection | None = None
    reason: str | None = None
    repo_nwo: str | None = None
    scope: PermissionRequestMemoryScope | None = None
    subject: str | None = None
    tool_call_id: str | None = None
    managed_approval_required: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestMemory":
        assert isinstance(obj, dict)
        fact = from_str(obj.get("fact"))
        action = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryAction, x)], obj.get("action"))
        assisted_approval = from_union([from_none, PermissionAssistedApproval.from_dict], obj.get("assistedApproval"))
        citations = from_union([from_none, from_str], obj.get("citations"))
        direction = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryDirection, x)], obj.get("direction"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        repo_nwo = from_union([from_none, from_str], obj.get("repoNwo"))
        scope = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryScope, x)], obj.get("scope"))
        subject = from_union([from_none, from_str], obj.get("subject"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        return PermissionRequestMemory(
            fact=fact,
            action=action,
            assisted_approval=assisted_approval,
            citations=citations,
            direction=direction,
            reason=reason,
            repo_nwo=repo_nwo,
            scope=scope,
            subject=subject,
            tool_call_id=tool_call_id,
            managed_approval_required=managed_approval_required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["fact"] = from_str(self.fact)
        result["kind"] = self.kind
        if self.action is not None:
            result["action"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryAction, x)], self.action)
        if self.assisted_approval is not None:
            result["assistedApproval"] = from_union([from_none, lambda x: to_class(PermissionAssistedApproval, x)], self.assisted_approval)
        if self.citations is not None:
            result["citations"] = from_union([from_none, from_str], self.citations)
        if self.direction is not None:
            result["direction"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryDirection, x)], self.direction)
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        if self.repo_nwo is not None:
            result["repoNwo"] = from_union([from_none, from_str], self.repo_nwo)
        if self.scope is not None:
            result["scope"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryScope, x)], self.scope)
        if self.subject is not None:
            result["subject"] = from_union([from_none, from_str], self.subject)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        return result


@dataclass
class PermissionRequestRead:
    "File or directory read permission request"
    intention: str
    kind: ClassVar[str] = "read"
    path: str
    managed_approval_required: bool | None = None
    request_sandbox_bypass: bool | None = None
    request_sandbox_bypass_reason: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestRead":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        path = from_str(obj.get("path"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        request_sandbox_bypass = from_union([from_none, from_bool], obj.get("requestSandboxBypass"))
        request_sandbox_bypass_reason = from_union([from_none, from_str], obj.get("requestSandboxBypassReason"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestRead(
            intention=intention,
            path=path,
            managed_approval_required=managed_approval_required,
            request_sandbox_bypass=request_sandbox_bypass,
            request_sandbox_bypass_reason=request_sandbox_bypass_reason,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["path"] = from_str(self.path)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.request_sandbox_bypass is not None:
            result["requestSandboxBypass"] = from_union([from_none, from_bool], self.request_sandbox_bypass)
        if self.request_sandbox_bypass_reason is not None:
            result["requestSandboxBypassReason"] = from_union([from_none, from_str], self.request_sandbox_bypass_reason)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestShell:
    "Shell command permission request"
    can_offer_session_approval: bool
    commands: list[PermissionRequestShellCommand]
    full_command_text: str
    has_write_file_redirection: bool
    intention: str
    kind: ClassVar[str] = "shell"
    possible_paths: list[str]
    possible_urls: list[PermissionRequestShellPossibleUrl]
    command_segments: list[PermissionRequestShellCommandSegment] | None = None
    managed_approval_required: bool | None = None
    request_sandbox_bypass: bool | None = None
    request_sandbox_bypass_reason: str | None = None
    tool_call_id: str | None = None
    warning: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestShell":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        commands = from_list(PermissionRequestShellCommand.from_dict, obj.get("commands"))
        full_command_text = from_str(obj.get("fullCommandText"))
        has_write_file_redirection = from_bool(obj.get("hasWriteFileRedirection"))
        intention = from_str(obj.get("intention"))
        possible_paths = from_list(from_str, obj.get("possiblePaths"))
        possible_urls = from_list(PermissionRequestShellPossibleUrl.from_dict, obj.get("possibleUrls"))
        command_segments = from_union([from_none, lambda x: from_list(PermissionRequestShellCommandSegment.from_dict, x)], obj.get("commandSegments"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        request_sandbox_bypass = from_union([from_none, from_bool], obj.get("requestSandboxBypass"))
        request_sandbox_bypass_reason = from_union([from_none, from_str], obj.get("requestSandboxBypassReason"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        warning = from_union([from_none, from_str], obj.get("warning"))
        return PermissionRequestShell(
            can_offer_session_approval=can_offer_session_approval,
            commands=commands,
            full_command_text=full_command_text,
            has_write_file_redirection=has_write_file_redirection,
            intention=intention,
            possible_paths=possible_paths,
            possible_urls=possible_urls,
            command_segments=command_segments,
            managed_approval_required=managed_approval_required,
            request_sandbox_bypass=request_sandbox_bypass,
            request_sandbox_bypass_reason=request_sandbox_bypass_reason,
            tool_call_id=tool_call_id,
            warning=warning,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canOfferSessionApproval"] = from_bool(self.can_offer_session_approval)
        result["commands"] = from_list(lambda x: to_class(PermissionRequestShellCommand, x), self.commands)
        result["fullCommandText"] = from_str(self.full_command_text)
        result["hasWriteFileRedirection"] = from_bool(self.has_write_file_redirection)
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["possiblePaths"] = from_list(from_str, self.possible_paths)
        result["possibleUrls"] = from_list(lambda x: to_class(PermissionRequestShellPossibleUrl, x), self.possible_urls)
        if self.command_segments is not None:
            result["commandSegments"] = from_union([from_none, lambda x: from_list(lambda x: to_class(PermissionRequestShellCommandSegment, x), x)], self.command_segments)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.request_sandbox_bypass is not None:
            result["requestSandboxBypass"] = from_union([from_none, from_bool], self.request_sandbox_bypass)
        if self.request_sandbox_bypass_reason is not None:
            result["requestSandboxBypassReason"] = from_union([from_none, from_str], self.request_sandbox_bypass_reason)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.warning is not None:
            result["warning"] = from_union([from_none, from_str], self.warning)
        return result


@dataclass
class PermissionRequestShellCommand:
    "A parsed command identifier in a shell permission request, including whether it is read-only."
    identifier: str
    read_only: bool

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestShellCommand":
        assert isinstance(obj, dict)
        identifier = from_str(obj.get("identifier"))
        read_only = from_bool(obj.get("readOnly"))
        return PermissionRequestShellCommand(
            identifier=identifier,
            read_only=read_only,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["identifier"] = from_str(self.identifier)
        result["readOnly"] = from_bool(self.read_only)
        return result


@dataclass
class PermissionRequestShellCommandSegment:
    "A parsed shell command segment used for argument-aware managed policy matching."
    full_command_text: str
    identifier: str

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestShellCommandSegment":
        assert isinstance(obj, dict)
        full_command_text = from_str(obj.get("fullCommandText"))
        identifier = from_str(obj.get("identifier"))
        return PermissionRequestShellCommandSegment(
            full_command_text=full_command_text,
            identifier=identifier,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["fullCommandText"] = from_str(self.full_command_text)
        result["identifier"] = from_str(self.identifier)
        return result


@dataclass
class PermissionRequestShellPossibleUrl:
    "A URL that may be accessed by a command in a shell permission request."
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestShellPossibleUrl":
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        return PermissionRequestShellPossibleUrl(
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["url"] = from_str(self.url)
        return result


@dataclass
class PermissionRequestUrl:
    "URL access permission request"
    intention: str
    kind: ClassVar[str] = "url"
    url: str
    managed_approval_required: bool | None = None
    redirected_from: str | None = None
    request_sandbox_bypass: bool | None = None
    request_sandbox_bypass_reason: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestUrl":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        url = from_str(obj.get("url"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        redirected_from = from_union([from_none, from_str], obj.get("redirectedFrom"))
        request_sandbox_bypass = from_union([from_none, from_bool], obj.get("requestSandboxBypass"))
        request_sandbox_bypass_reason = from_union([from_none, from_str], obj.get("requestSandboxBypassReason"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestUrl(
            intention=intention,
            url=url,
            managed_approval_required=managed_approval_required,
            redirected_from=redirected_from,
            request_sandbox_bypass=request_sandbox_bypass,
            request_sandbox_bypass_reason=request_sandbox_bypass_reason,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["url"] = from_str(self.url)
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.redirected_from is not None:
            result["redirectedFrom"] = from_union([from_none, from_str], self.redirected_from)
        if self.request_sandbox_bypass is not None:
            result["requestSandboxBypass"] = from_union([from_none, from_bool], self.request_sandbox_bypass)
        if self.request_sandbox_bypass_reason is not None:
            result["requestSandboxBypassReason"] = from_union([from_none, from_str], self.request_sandbox_bypass_reason)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestWrite:
    "File write permission request"
    can_offer_session_approval: bool
    diff: str
    file_name: str
    intention: str
    kind: ClassVar[str] = "write"
    managed_approval_required: bool | None = None
    new_file_contents: str | None = None
    request_sandbox_bypass: bool | None = None
    request_sandbox_bypass_reason: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestWrite":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        diff = from_str(obj.get("diff"))
        file_name = from_str(obj.get("fileName"))
        intention = from_str(obj.get("intention"))
        managed_approval_required = from_union([from_none, from_bool], obj.get("managedApprovalRequired"))
        new_file_contents = from_union([from_none, from_str], obj.get("newFileContents"))
        request_sandbox_bypass = from_union([from_none, from_bool], obj.get("requestSandboxBypass"))
        request_sandbox_bypass_reason = from_union([from_none, from_str], obj.get("requestSandboxBypassReason"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestWrite(
            can_offer_session_approval=can_offer_session_approval,
            diff=diff,
            file_name=file_name,
            intention=intention,
            managed_approval_required=managed_approval_required,
            new_file_contents=new_file_contents,
            request_sandbox_bypass=request_sandbox_bypass,
            request_sandbox_bypass_reason=request_sandbox_bypass_reason,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["canOfferSessionApproval"] = from_bool(self.can_offer_session_approval)
        result["diff"] = from_str(self.diff)
        result["fileName"] = from_str(self.file_name)
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        if self.managed_approval_required is not None:
            result["managedApprovalRequired"] = from_union([from_none, from_bool], self.managed_approval_required)
        if self.new_file_contents is not None:
            result["newFileContents"] = from_union([from_none, from_str], self.new_file_contents)
        if self.request_sandbox_bypass is not None:
            result["requestSandboxBypass"] = from_union([from_none, from_bool], self.request_sandbox_bypass)
        if self.request_sandbox_bypass_reason is not None:
            result["requestSandboxBypassReason"] = from_union([from_none, from_str], self.request_sandbox_bypass_reason)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestedData:
    "Permission request notification requiring client approval with request details"
    permission_request: PermissionRequest
    request_id: str
    prompt_request: PermissionPromptRequest | None = None
    resolved_by_hook: bool | None = None
    risk_assessment: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestedData":
        assert isinstance(obj, dict)
        permission_request = _load_PermissionRequest(obj.get("permissionRequest"))
        request_id = from_str(obj.get("requestId"))
        prompt_request = from_union([from_none, _load_PermissionPromptRequest], obj.get("promptRequest"))
        resolved_by_hook = from_union([from_none, from_bool], obj.get("resolvedByHook"))
        risk_assessment = obj.get("riskAssessment")
        return PermissionRequestedData(
            permission_request=permission_request,
            request_id=request_id,
            prompt_request=prompt_request,
            resolved_by_hook=resolved_by_hook,
            risk_assessment=risk_assessment,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["permissionRequest"] = self.permission_request.to_dict()
        result["requestId"] = from_str(self.request_id)
        if self.prompt_request is not None:
            result["promptRequest"] = from_union([from_none, lambda x: x.to_dict()], self.prompt_request)
        if self.resolved_by_hook is not None:
            result["resolvedByHook"] = from_union([from_none, from_bool], self.resolved_by_hook)
        if self.risk_assessment is not None:
            result["riskAssessment"] = self.risk_assessment
        return result


@dataclass
class PermissionRule:
    "A permission approval or denial rule matched against a tool request, identified by a rule kind with an optional argument value."
    argument: str | None
    kind: str

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRule":
        assert isinstance(obj, dict)
        argument = from_union([from_none, from_str], obj.get("argument"))
        kind = from_str(obj.get("kind"))
        return PermissionRule(
            argument=argument,
            kind=kind,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["argument"] = from_union([from_none, from_str], self.argument)
        result["kind"] = from_str(self.kind)
        return result


@dataclass
class PersistedBinaryImage:
    "Binary result returned by a tool for the model"
    data: str
    mime_type: str
    type: PersistedBinaryImageType
    description: str | None = None
    metadata: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PersistedBinaryImage":
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        type = parse_enum(PersistedBinaryImageType, obj.get("type"))
        description = from_union([from_none, from_str], obj.get("description"))
        metadata = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("metadata"))
        return PersistedBinaryImage(
            data=data,
            mime_type=mime_type,
            type=type,
            description=description,
            metadata=metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = to_enum(PersistedBinaryImageType, self.type)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.metadata is not None:
            result["metadata"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.metadata)
        return result


@dataclass
class PromptCacheBreakData:
    "A detected loss of a previously cached prompt prefix"
    contributing_reasons: list[str]
    frontier_tokens: int
    primary_reason: str
    retention_ratio: float
    shortfall_tokens: int
    survived_tokens: int
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _after_request: Any = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _agent_name: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _before_request: Any = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _cache_config_changed_fields: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _model_from: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _model_to: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _rewrite_message_index: int | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _rewrite_shape: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _rewrite_source: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _system_segments_changed: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_added: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_added_raw: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_redefined: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_redefined_raw: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_removed: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_removed_raw: list[str] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _tools_reordered: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PromptCacheBreakData":
        assert isinstance(obj, dict)
        contributing_reasons = from_list(from_str, obj.get("contributingReasons"))
        frontier_tokens = from_int(obj.get("frontierTokens"))
        primary_reason = from_str(obj.get("primaryReason"))
        retention_ratio = from_float(obj.get("retentionRatio"))
        shortfall_tokens = from_int(obj.get("shortfallTokens"))
        survived_tokens = from_int(obj.get("survivedTokens"))
        _after_request = obj.get("afterRequest")
        _agent_name = from_union([from_none, from_str], obj.get("agentName"))
        _before_request = obj.get("beforeRequest")
        _cache_config_changed_fields = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("cacheConfigChangedFields"))
        _model_from = from_union([from_none, from_str], obj.get("modelFrom"))
        _model_to = from_union([from_none, from_str], obj.get("modelTo"))
        _rewrite_message_index = from_union([from_none, from_int], obj.get("rewriteMessageIndex"))
        _rewrite_shape = from_union([from_none, from_str], obj.get("rewriteShape"))
        _rewrite_source = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("rewriteSource"))
        _system_segments_changed = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("systemSegmentsChanged"))
        _tools_added = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolsAdded"))
        _tools_added_raw = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolsAddedRaw"))
        _tools_redefined = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolsRedefined"))
        _tools_redefined_raw = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolsRedefinedRaw"))
        _tools_removed = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolsRemoved"))
        _tools_removed_raw = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("toolsRemovedRaw"))
        _tools_reordered = from_union([from_none, from_bool], obj.get("toolsReordered"))
        return PromptCacheBreakData(
            contributing_reasons=contributing_reasons,
            frontier_tokens=frontier_tokens,
            primary_reason=primary_reason,
            retention_ratio=retention_ratio,
            shortfall_tokens=shortfall_tokens,
            survived_tokens=survived_tokens,
            _after_request=_after_request,
            _agent_name=_agent_name,
            _before_request=_before_request,
            _cache_config_changed_fields=_cache_config_changed_fields,
            _model_from=_model_from,
            _model_to=_model_to,
            _rewrite_message_index=_rewrite_message_index,
            _rewrite_shape=_rewrite_shape,
            _rewrite_source=_rewrite_source,
            _system_segments_changed=_system_segments_changed,
            _tools_added=_tools_added,
            _tools_added_raw=_tools_added_raw,
            _tools_redefined=_tools_redefined,
            _tools_redefined_raw=_tools_redefined_raw,
            _tools_removed=_tools_removed,
            _tools_removed_raw=_tools_removed_raw,
            _tools_reordered=_tools_reordered,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["contributingReasons"] = from_list(from_str, self.contributing_reasons)
        result["frontierTokens"] = to_int(self.frontier_tokens)
        result["primaryReason"] = from_str(self.primary_reason)
        result["retentionRatio"] = to_float(self.retention_ratio)
        result["shortfallTokens"] = to_int(self.shortfall_tokens)
        result["survivedTokens"] = to_int(self.survived_tokens)
        if self._after_request is not None:
            result["afterRequest"] = self._after_request
        if self._agent_name is not None:
            result["agentName"] = from_union([from_none, from_str], self._agent_name)
        if self._before_request is not None:
            result["beforeRequest"] = self._before_request
        if self._cache_config_changed_fields is not None:
            result["cacheConfigChangedFields"] = from_union([from_none, lambda x: from_list(from_str, x)], self._cache_config_changed_fields)
        if self._model_from is not None:
            result["modelFrom"] = from_union([from_none, from_str], self._model_from)
        if self._model_to is not None:
            result["modelTo"] = from_union([from_none, from_str], self._model_to)
        if self._rewrite_message_index is not None:
            result["rewriteMessageIndex"] = from_union([from_none, to_int], self._rewrite_message_index)
        if self._rewrite_shape is not None:
            result["rewriteShape"] = from_union([from_none, from_str], self._rewrite_shape)
        if self._rewrite_source is not None:
            result["rewriteSource"] = from_union([from_none, lambda x: from_list(from_str, x)], self._rewrite_source)
        if self._system_segments_changed is not None:
            result["systemSegmentsChanged"] = from_union([from_none, lambda x: from_list(from_str, x)], self._system_segments_changed)
        if self._tools_added is not None:
            result["toolsAdded"] = from_union([from_none, lambda x: from_list(from_str, x)], self._tools_added)
        if self._tools_added_raw is not None:
            result["toolsAddedRaw"] = from_union([from_none, lambda x: from_list(from_str, x)], self._tools_added_raw)
        if self._tools_redefined is not None:
            result["toolsRedefined"] = from_union([from_none, lambda x: from_list(from_str, x)], self._tools_redefined)
        if self._tools_redefined_raw is not None:
            result["toolsRedefinedRaw"] = from_union([from_none, lambda x: from_list(from_str, x)], self._tools_redefined_raw)
        if self._tools_removed is not None:
            result["toolsRemoved"] = from_union([from_none, lambda x: from_list(from_str, x)], self._tools_removed)
        if self._tools_removed_raw is not None:
            result["toolsRemovedRaw"] = from_union([from_none, lambda x: from_list(from_str, x)], self._tools_removed_raw)
        if self._tools_reordered is not None:
            result["toolsReordered"] = from_union([from_none, from_bool], self._tools_reordered)
        return result


@dataclass
class SamplingCompletedData:
    "Sampling request completion notification signaling UI dismissal"
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "SamplingCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        return SamplingCompletedData(
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class SamplingRequestedData:
    "Sampling request from an MCP server; contains the server name and a requestId for correlation"
    mcp_request_id: Any
    request_id: str
    server_name: str

    @staticmethod
    def from_dict(obj: Any) -> "SamplingRequestedData":
        assert isinstance(obj, dict)
        mcp_request_id = obj.get("mcpRequestId")
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        return SamplingRequestedData(
            mcp_request_id=mcp_request_id,
            request_id=request_id,
            server_name=server_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["mcpRequestId"] = self.mcp_request_id
        result["requestId"] = from_str(self.request_id)
        result["serverName"] = from_str(self.server_name)
        return result


@dataclass
class SandboxDecisionData:
    "Payload of `sandbox.decision`, a bounded governance record of what the process sandbox was configured to do and whether it took effect. Discriminated by `kind`."
    @staticmethod
    def from_dict(obj: Any) -> "SandboxDecisionData":
        assert isinstance(obj, dict)
        return SandboxDecisionData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class SessionAutopilotObjectiveChangedData:
    "Autopilot objective state file operation details indicating what changed"
    operation: AutopilotObjectiveChangedOperation
    id: int | None = None
    status: AutopilotObjectiveChangedStatus | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionAutopilotObjectiveChangedData":
        assert isinstance(obj, dict)
        operation = parse_enum(AutopilotObjectiveChangedOperation, obj.get("operation"))
        id = from_union([from_none, from_int], obj.get("id"))
        status = from_union([from_none, lambda x: parse_enum(AutopilotObjectiveChangedStatus, x)], obj.get("status"))
        return SessionAutopilotObjectiveChangedData(
            operation=operation,
            id=id,
            status=status,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["operation"] = to_enum(AutopilotObjectiveChangedOperation, self.operation)
        if self.id is not None:
            result["id"] = from_union([from_none, to_int], self.id)
        if self.status is not None:
            result["status"] = from_union([from_none, lambda x: to_enum(AutopilotObjectiveChangedStatus, x)], self.status)
        return result


@dataclass
class SessionBackgroundTasksChangedData:
    "Empty payload for `session.background_tasks_changed`, indicating background task state changed."
    @staticmethod
    def from_dict(obj: Any) -> "SessionBackgroundTasksChangedData":
        assert isinstance(obj, dict)
        return SessionBackgroundTasksChangedData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class SessionBinaryAssetData:
    "Canonical bytes for a content-addressed binary asset shared by reference across events"
    asset_id: str
    byte_length: int
    data: str
    mime_type: str
    type: BinaryAssetType
    description: str | None = None
    metadata: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionBinaryAssetData":
        assert isinstance(obj, dict)
        asset_id = from_str(obj.get("assetId"))
        byte_length = from_int(obj.get("byteLength"))
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        type = parse_enum(BinaryAssetType, obj.get("type"))
        description = from_union([from_none, from_str], obj.get("description"))
        metadata = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("metadata"))
        return SessionBinaryAssetData(
            asset_id=asset_id,
            byte_length=byte_length,
            data=data,
            mime_type=mime_type,
            type=type,
            description=description,
            metadata=metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["assetId"] = from_str(self.asset_id)
        result["byteLength"] = to_int(self.byte_length)
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = to_enum(BinaryAssetType, self.type)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.metadata is not None:
            result["metadata"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.metadata)
        return result


@dataclass
class SessionCompactionCompleteData:
    "Conversation compaction results including success status, metrics, and optional error details"
    success: bool
    behavior_model_id: str | None = None
    checkpoint_number: int | None = None
    checkpoint_path: str | None = None
    compaction_tokens_used: CompactionCompleteCompactionTokensUsed | None = None
    conversation_tokens: int | None = None
    custom_instructions: str | None = None
    error: str | None = None
    messages_removed: int | None = None
    post_compaction_tokens: int | None = None
    pre_compaction_messages_length: int | None = None
    pre_compaction_tokens: int | None = None
    request_id: str | None = None
    service_request_id: str | None = None
    status_code: int | None = None
    summary_content: str | None = None
    system_tokens: int | None = None
    token_limit: int | None = None
    tokens_removed: int | None = None
    tool_definitions_tokens: int | None = None
    trigger: CompactionTrigger | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCompactionCompleteData":
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        behavior_model_id = from_union([from_none, from_str], obj.get("behaviorModelId"))
        checkpoint_number = from_union([from_none, from_int], obj.get("checkpointNumber"))
        checkpoint_path = from_union([from_none, from_str], obj.get("checkpointPath"))
        compaction_tokens_used = from_union([from_none, CompactionCompleteCompactionTokensUsed.from_dict], obj.get("compactionTokensUsed"))
        conversation_tokens = from_union([from_none, from_int], obj.get("conversationTokens"))
        custom_instructions = from_union([from_none, from_str], obj.get("customInstructions"))
        error = from_union([from_none, from_str], obj.get("error"))
        messages_removed = from_union([from_none, from_int], obj.get("messagesRemoved"))
        post_compaction_tokens = from_union([from_none, from_int], obj.get("postCompactionTokens"))
        pre_compaction_messages_length = from_union([from_none, from_int], obj.get("preCompactionMessagesLength"))
        pre_compaction_tokens = from_union([from_none, from_int], obj.get("preCompactionTokens"))
        request_id = from_union([from_none, from_str], obj.get("requestId"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        status_code = from_union([from_none, from_int], obj.get("statusCode"))
        summary_content = from_union([from_none, from_str], obj.get("summaryContent"))
        system_tokens = from_union([from_none, from_int], obj.get("systemTokens"))
        token_limit = from_union([from_none, from_int], obj.get("tokenLimit"))
        tokens_removed = from_union([from_none, from_int], obj.get("tokensRemoved"))
        tool_definitions_tokens = from_union([from_none, from_int], obj.get("toolDefinitionsTokens"))
        trigger = from_union([from_none, lambda x: parse_enum(CompactionTrigger, x)], obj.get("trigger"))
        return SessionCompactionCompleteData(
            success=success,
            behavior_model_id=behavior_model_id,
            checkpoint_number=checkpoint_number,
            checkpoint_path=checkpoint_path,
            compaction_tokens_used=compaction_tokens_used,
            conversation_tokens=conversation_tokens,
            custom_instructions=custom_instructions,
            error=error,
            messages_removed=messages_removed,
            post_compaction_tokens=post_compaction_tokens,
            pre_compaction_messages_length=pre_compaction_messages_length,
            pre_compaction_tokens=pre_compaction_tokens,
            request_id=request_id,
            service_request_id=service_request_id,
            status_code=status_code,
            summary_content=summary_content,
            system_tokens=system_tokens,
            token_limit=token_limit,
            tokens_removed=tokens_removed,
            tool_definitions_tokens=tool_definitions_tokens,
            trigger=trigger,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        if self.behavior_model_id is not None:
            result["behaviorModelId"] = from_union([from_none, from_str], self.behavior_model_id)
        if self.checkpoint_number is not None:
            result["checkpointNumber"] = from_union([from_none, to_int], self.checkpoint_number)
        if self.checkpoint_path is not None:
            result["checkpointPath"] = from_union([from_none, from_str], self.checkpoint_path)
        if self.compaction_tokens_used is not None:
            result["compactionTokensUsed"] = from_union([from_none, lambda x: to_class(CompactionCompleteCompactionTokensUsed, x)], self.compaction_tokens_used)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_int], self.conversation_tokens)
        if self.custom_instructions is not None:
            result["customInstructions"] = from_union([from_none, from_str], self.custom_instructions)
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        if self.messages_removed is not None:
            result["messagesRemoved"] = from_union([from_none, to_int], self.messages_removed)
        if self.post_compaction_tokens is not None:
            result["postCompactionTokens"] = from_union([from_none, to_int], self.post_compaction_tokens)
        if self.pre_compaction_messages_length is not None:
            result["preCompactionMessagesLength"] = from_union([from_none, to_int], self.pre_compaction_messages_length)
        if self.pre_compaction_tokens is not None:
            result["preCompactionTokens"] = from_union([from_none, to_int], self.pre_compaction_tokens)
        if self.request_id is not None:
            result["requestId"] = from_union([from_none, from_str], self.request_id)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.status_code is not None:
            result["statusCode"] = from_union([from_none, to_int], self.status_code)
        if self.summary_content is not None:
            result["summaryContent"] = from_union([from_none, from_str], self.summary_content)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_int], self.system_tokens)
        if self.token_limit is not None:
            result["tokenLimit"] = from_union([from_none, to_int], self.token_limit)
        if self.tokens_removed is not None:
            result["tokensRemoved"] = from_union([from_none, to_int], self.tokens_removed)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_int], self.tool_definitions_tokens)
        if self.trigger is not None:
            result["trigger"] = from_union([from_none, lambda x: to_enum(CompactionTrigger, x)], self.trigger)
        return result


@dataclass
class SessionCompactionStartData:
    "Context window breakdown at the start of LLM-powered conversation compaction"
    conversation_tokens: int | None = None
    current_tokens: int | None = None
    model: str | None = None
    system_tokens: int | None = None
    token_limit: int | None = None
    tool_definitions_tokens: int | None = None
    trigger: CompactionTrigger | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCompactionStartData":
        assert isinstance(obj, dict)
        conversation_tokens = from_union([from_none, from_int], obj.get("conversationTokens"))
        current_tokens = from_union([from_none, from_int], obj.get("currentTokens"))
        model = from_union([from_none, from_str], obj.get("model"))
        system_tokens = from_union([from_none, from_int], obj.get("systemTokens"))
        token_limit = from_union([from_none, from_int], obj.get("tokenLimit"))
        tool_definitions_tokens = from_union([from_none, from_int], obj.get("toolDefinitionsTokens"))
        trigger = from_union([from_none, lambda x: parse_enum(CompactionTrigger, x)], obj.get("trigger"))
        return SessionCompactionStartData(
            conversation_tokens=conversation_tokens,
            current_tokens=current_tokens,
            model=model,
            system_tokens=system_tokens,
            token_limit=token_limit,
            tool_definitions_tokens=tool_definitions_tokens,
            trigger=trigger,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_int], self.conversation_tokens)
        if self.current_tokens is not None:
            result["currentTokens"] = from_union([from_none, to_int], self.current_tokens)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_int], self.system_tokens)
        if self.token_limit is not None:
            result["tokenLimit"] = from_union([from_none, to_int], self.token_limit)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_int], self.tool_definitions_tokens)
        if self.trigger is not None:
            result["trigger"] = from_union([from_none, lambda x: to_enum(CompactionTrigger, x)], self.trigger)
        return result


@dataclass
class SessionContextChangedData:
    "Working directory and git context at session start"
    cwd: str
    base_commit: str | None = None
    branch: str | None = None
    git_root: str | None = None
    head_commit: str | None = None
    host_type: WorkingDirectoryContextHostType | None = None
    pending_git_context: bool | None = None
    repository: str | None = None
    repository_host: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionContextChangedData":
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        base_commit = from_union([from_none, from_str], obj.get("baseCommit"))
        branch = from_union([from_none, from_str], obj.get("branch"))
        git_root = from_union([from_none, from_str], obj.get("gitRoot"))
        head_commit = from_union([from_none, from_str], obj.get("headCommit"))
        host_type = from_union([from_none, lambda x: parse_enum(WorkingDirectoryContextHostType, x)], obj.get("hostType"))
        pending_git_context = from_union([from_none, from_bool], obj.get("pendingGitContext"))
        repository = from_union([from_none, from_str], obj.get("repository"))
        repository_host = from_union([from_none, from_str], obj.get("repositoryHost"))
        return SessionContextChangedData(
            cwd=cwd,
            base_commit=base_commit,
            branch=branch,
            git_root=git_root,
            head_commit=head_commit,
            host_type=host_type,
            pending_git_context=pending_git_context,
            repository=repository,
            repository_host=repository_host,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_none, from_str], self.base_commit)
        if self.branch is not None:
            result["branch"] = from_union([from_none, from_str], self.branch)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_none, from_str], self.git_root)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_none, from_str], self.head_commit)
        if self.host_type is not None:
            result["hostType"] = from_union([from_none, lambda x: to_enum(WorkingDirectoryContextHostType, x)], self.host_type)
        if self.pending_git_context is not None:
            result["pendingGitContext"] = from_union([from_none, from_bool], self.pending_git_context)
        if self.repository is not None:
            result["repository"] = from_union([from_none, from_str], self.repository)
        if self.repository_host is not None:
            result["repositoryHost"] = from_union([from_none, from_str], self.repository_host)
        return result


@dataclass
class SessionContextClearedData:
    "Context-cleared details emitted when the host clears the conversation (the session.history.clearContext RPC / Session.clearContextMessages)"
    messages_cleared: int
    initial_message: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionContextClearedData":
        assert isinstance(obj, dict)
        messages_cleared = from_int(obj.get("messagesCleared"))
        initial_message = from_union([from_none, from_str], obj.get("initialMessage"))
        return SessionContextClearedData(
            messages_cleared=messages_cleared,
            initial_message=initial_message,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["messagesCleared"] = to_int(self.messages_cleared)
        if self.initial_message is not None:
            result["initialMessage"] = from_union([from_none, from_str], self.initial_message)
        return result


@dataclass
class SessionCustomAgentsUpdatedData:
    "Payload of `session.custom_agents_updated` with loaded custom agents plus non-fatal warnings and fatal errors."
    agents: list[CustomAgentsUpdatedAgent]
    errors: list[str]
    warnings: list[str]

    @staticmethod
    def from_dict(obj: Any) -> "SessionCustomAgentsUpdatedData":
        assert isinstance(obj, dict)
        agents = from_list(CustomAgentsUpdatedAgent.from_dict, obj.get("agents"))
        errors = from_list(from_str, obj.get("errors"))
        warnings = from_list(from_str, obj.get("warnings"))
        return SessionCustomAgentsUpdatedData(
            agents=agents,
            errors=errors,
            warnings=warnings,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(CustomAgentsUpdatedAgent, x), self.agents)
        result["errors"] = from_list(from_str, self.errors)
        result["warnings"] = from_list(from_str, self.warnings)
        return result


@dataclass
class SessionCustomNotificationData:
    "Opaque custom notification data. Consumers may branch on source and name, but payload semantics are source-defined."
    name: str
    payload: Any
    source: str
    subject: dict[str, str] | None = None
    version: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCustomNotificationData":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        payload = obj.get("payload")
        source = from_str(obj.get("source"))
        subject = from_union([from_none, lambda x: from_dict(from_str, x)], obj.get("subject"))
        version = from_union([from_none, from_int], obj.get("version"))
        return SessionCustomNotificationData(
            name=name,
            payload=payload,
            source=source,
            subject=subject,
            version=version,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["payload"] = self.payload
        result["source"] = from_str(self.source)
        if self.subject is not None:
            result["subject"] = from_union([from_none, lambda x: from_dict(from_str, x)], self.subject)
        if self.version is not None:
            result["version"] = from_union([from_none, to_int], self.version)
        return result


@dataclass
class SessionErrorData:
    "Error details for timeline display including message and optional diagnostic information"
    error_type: str
    message: str
    eligible_for_auto_switch: bool | None = None
    error_code: str | None = None
    provider_call_id: str | None = None
    service_request_id: str | None = None
    stack: str | None = None
    status_code: int | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionErrorData":
        assert isinstance(obj, dict)
        error_type = from_str(obj.get("errorType"))
        message = from_str(obj.get("message"))
        eligible_for_auto_switch = from_union([from_none, from_bool], obj.get("eligibleForAutoSwitch"))
        error_code = from_union([from_none, from_str], obj.get("errorCode"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        stack = from_union([from_none, from_str], obj.get("stack"))
        status_code = from_union([from_none, from_int], obj.get("statusCode"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionErrorData(
            error_type=error_type,
            message=message,
            eligible_for_auto_switch=eligible_for_auto_switch,
            error_code=error_code,
            provider_call_id=provider_call_id,
            service_request_id=service_request_id,
            stack=stack,
            status_code=status_code,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["errorType"] = from_str(self.error_type)
        result["message"] = from_str(self.message)
        if self.eligible_for_auto_switch is not None:
            result["eligibleForAutoSwitch"] = from_union([from_none, from_bool], self.eligible_for_auto_switch)
        if self.error_code is not None:
            result["errorCode"] = from_union([from_none, from_str], self.error_code)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.stack is not None:
            result["stack"] = from_union([from_none, from_str], self.stack)
        if self.status_code is not None:
            result["statusCode"] = from_union([from_none, to_int], self.status_code)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


@dataclass
class SessionExtensionsAttachmentsPushedData:
    "Payload of `session.extensions.attachments_pushed` with extension-contributed attachments for the next send."
    attachments: list[Attachment]

    @staticmethod
    def from_dict(obj: Any) -> "SessionExtensionsAttachmentsPushedData":
        assert isinstance(obj, dict)
        attachments = from_list(_load_Attachment, obj.get("attachments"))
        return SessionExtensionsAttachmentsPushedData(
            attachments=attachments,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["attachments"] = from_list(lambda x: x.to_dict(), self.attachments)
        return result


@dataclass
class SessionExtensionsLoadedData:
    "Payload of `session.extensions_loaded` listing discovered extensions and their statuses."
    extensions: list[ExtensionsLoadedExtension]

    @staticmethod
    def from_dict(obj: Any) -> "SessionExtensionsLoadedData":
        assert isinstance(obj, dict)
        extensions = from_list(ExtensionsLoadedExtension.from_dict, obj.get("extensions"))
        return SessionExtensionsLoadedData(
            extensions=extensions,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensions"] = from_list(lambda x: to_class(ExtensionsLoadedExtension, x), self.extensions)
        return result


@dataclass
class SessionHandoffData:
    "Session handoff metadata including source, context, and repository information"
    handoff_time: datetime
    source_type: HandoffSourceType
    context: str | None = None
    host: str | None = None
    remote_session_id: str | None = None
    repository: HandoffRepository | None = None
    summary: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionHandoffData":
        assert isinstance(obj, dict)
        handoff_time = from_datetime(obj.get("handoffTime"))
        source_type = parse_enum(HandoffSourceType, obj.get("sourceType"))
        context = from_union([from_none, from_str], obj.get("context"))
        host = from_union([from_none, from_str], obj.get("host"))
        remote_session_id = from_union([from_none, from_str], obj.get("remoteSessionId"))
        repository = from_union([from_none, HandoffRepository.from_dict], obj.get("repository"))
        summary = from_union([from_none, from_str], obj.get("summary"))
        return SessionHandoffData(
            handoff_time=handoff_time,
            source_type=source_type,
            context=context,
            host=host,
            remote_session_id=remote_session_id,
            repository=repository,
            summary=summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["handoffTime"] = to_datetime(self.handoff_time)
        result["sourceType"] = to_enum(HandoffSourceType, self.source_type)
        if self.context is not None:
            result["context"] = from_union([from_none, from_str], self.context)
        if self.host is not None:
            result["host"] = from_union([from_none, from_str], self.host)
        if self.remote_session_id is not None:
            result["remoteSessionId"] = from_union([from_none, from_str], self.remote_session_id)
        if self.repository is not None:
            result["repository"] = from_union([from_none, lambda x: to_class(HandoffRepository, x)], self.repository)
        if self.summary is not None:
            result["summary"] = from_union([from_none, from_str], self.summary)
        return result


@dataclass
class SessionIdleData:
    "Payload indicating the session is idle with no background agents or attached shell commands in flight"
    aborted: bool | None = None
    mode: SessionMode | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionIdleData":
        assert isinstance(obj, dict)
        aborted = from_union([from_none, from_bool], obj.get("aborted"))
        mode = from_union([from_none, lambda x: parse_enum(SessionMode, x)], obj.get("mode"))
        return SessionIdleData(
            aborted=aborted,
            mode=mode,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.aborted is not None:
            result["aborted"] = from_union([from_none, from_bool], self.aborted)
        if self.mode is not None:
            result["mode"] = from_union([from_none, lambda x: to_enum(SessionMode, x)], self.mode)
        return result


@dataclass
class SessionInfoData:
    "Informational message for timeline display with categorization"
    info_type: str
    message: str
    tip: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionInfoData":
        assert isinstance(obj, dict)
        info_type = from_str(obj.get("infoType"))
        message = from_str(obj.get("message"))
        tip = from_union([from_none, from_str], obj.get("tip"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionInfoData(
            info_type=info_type,
            message=message,
            tip=tip,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["infoType"] = from_str(self.info_type)
        result["message"] = from_str(self.message)
        if self.tip is not None:
            result["tip"] = from_union([from_none, from_str], self.tip)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


@dataclass
class SessionLimitsConfig:
    "Optional session limits."
    max_ai_credits: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionLimitsConfig":
        assert isinstance(obj, dict)
        max_ai_credits = from_union([from_none, from_float], obj.get("maxAiCredits"))
        return SessionLimitsConfig(
            max_ai_credits=max_ai_credits,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.max_ai_credits is not None:
            result["maxAiCredits"] = from_union([from_none, to_float], self.max_ai_credits)
        return result


@dataclass
class SessionLimitsExhaustedCompletedData:
    "Session limit exhaustion prompt completion notification."
    request_id: str
    response: SessionLimitsExhaustedResponse

    @staticmethod
    def from_dict(obj: Any) -> "SessionLimitsExhaustedCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        response = SessionLimitsExhaustedResponse.from_dict(obj.get("response"))
        return SessionLimitsExhaustedCompletedData(
            request_id=request_id,
            response=response,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["response"] = to_class(SessionLimitsExhaustedResponse, self.response)
        return result


@dataclass
class SessionLimitsExhaustedRequestedData:
    "Session limit exhaustion notification requiring user action."
    max_ai_credits: float
    request_id: str
    used_ai_credits: float

    @staticmethod
    def from_dict(obj: Any) -> "SessionLimitsExhaustedRequestedData":
        assert isinstance(obj, dict)
        max_ai_credits = from_float(obj.get("maxAiCredits"))
        request_id = from_str(obj.get("requestId"))
        used_ai_credits = from_float(obj.get("usedAiCredits"))
        return SessionLimitsExhaustedRequestedData(
            max_ai_credits=max_ai_credits,
            request_id=request_id,
            used_ai_credits=used_ai_credits,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["maxAiCredits"] = to_float(self.max_ai_credits)
        result["requestId"] = from_str(self.request_id)
        result["usedAiCredits"] = to_float(self.used_ai_credits)
        return result


@dataclass
class SessionLimitsExhaustedResponse:
    "The user's selected action for an exhausted session limit."
    action: SessionLimitsExhaustedResponseAction
    additional_ai_credits: float | None = None
    max_ai_credits: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionLimitsExhaustedResponse":
        assert isinstance(obj, dict)
        action = parse_enum(SessionLimitsExhaustedResponseAction, obj.get("action"))
        additional_ai_credits = from_union([from_none, from_float], obj.get("additionalAiCredits"))
        max_ai_credits = from_union([from_none, from_float], obj.get("maxAiCredits"))
        return SessionLimitsExhaustedResponse(
            action=action,
            additional_ai_credits=additional_ai_credits,
            max_ai_credits=max_ai_credits,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["action"] = to_enum(SessionLimitsExhaustedResponseAction, self.action)
        if self.additional_ai_credits is not None:
            result["additionalAiCredits"] = from_union([from_none, to_float], self.additional_ai_credits)
        if self.max_ai_credits is not None:
            result["maxAiCredits"] = from_union([from_none, to_float], self.max_ai_credits)
        return result


@dataclass
class SessionMcpServerStatusChangedData:
    "Payload of `session.mcp_server_status_changed` for one MCP server's status and optional failure error."
    server_name: str
    status: McpServerStatus
    error: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionMcpServerStatusChangedData":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        status = parse_enum(McpServerStatus, obj.get("status"))
        error = from_union([from_none, from_str], obj.get("error"))
        return SessionMcpServerStatusChangedData(
            server_name=server_name,
            status=status,
            error=error,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        result["status"] = to_enum(McpServerStatus, self.status)
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        return result


@dataclass
class SessionMcpServersLoadedData:
    "Payload of `session.mcp_servers_loaded` listing MCP server status summaries."
    servers: list[McpServersLoadedServer]

    @staticmethod
    def from_dict(obj: Any) -> "SessionMcpServersLoadedData":
        assert isinstance(obj, dict)
        servers = from_list(McpServersLoadedServer.from_dict, obj.get("servers"))
        return SessionMcpServersLoadedData(
            servers=servers,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_list(lambda x: to_class(McpServersLoadedServer, x), self.servers)
        return result


@dataclass
class SessionModeChangedData:
    "Agent mode change details including previous and new modes"
    new_mode: SessionMode
    previous_mode: SessionMode

    @staticmethod
    def from_dict(obj: Any) -> "SessionModeChangedData":
        assert isinstance(obj, dict)
        new_mode = parse_enum(SessionMode, obj.get("newMode"))
        previous_mode = parse_enum(SessionMode, obj.get("previousMode"))
        return SessionModeChangedData(
            new_mode=new_mode,
            previous_mode=previous_mode,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["newMode"] = to_enum(SessionMode, self.new_mode)
        result["previousMode"] = to_enum(SessionMode, self.previous_mode)
        return result


@dataclass
class SessionModelChangeData:
    "Model change details including previous and new model identifiers"
    new_model: str
    cause: str | None = None
    context_tier: ContextTier | None = None
    previous_model: str | None = None
    previous_reasoning_effort: str | None = None
    previous_reasoning_summary: ReasoningSummary | None = None
    previous_verbosity: Verbosity | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None
    source: ModelChangeSource | None = None
    verbosity: Verbosity | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionModelChangeData":
        assert isinstance(obj, dict)
        new_model = from_str(obj.get("newModel"))
        cause = from_union([from_none, from_str], obj.get("cause"))
        context_tier = from_union([from_none, lambda x: parse_enum(ContextTier, x)], obj.get("contextTier"))
        previous_model = from_union([from_none, from_str], obj.get("previousModel"))
        previous_reasoning_effort = from_union([from_none, from_str], obj.get("previousReasoningEffort"))
        previous_reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("previousReasoningSummary"))
        previous_verbosity = from_union([from_none, lambda x: parse_enum(Verbosity, x)], obj.get("previousVerbosity"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        source = from_union([from_none, lambda x: parse_enum(ModelChangeSource, x)], obj.get("source"))
        verbosity = from_union([from_none, lambda x: parse_enum(Verbosity, x)], obj.get("verbosity"))
        return SessionModelChangeData(
            new_model=new_model,
            cause=cause,
            context_tier=context_tier,
            previous_model=previous_model,
            previous_reasoning_effort=previous_reasoning_effort,
            previous_reasoning_summary=previous_reasoning_summary,
            previous_verbosity=previous_verbosity,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
            source=source,
            verbosity=verbosity,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["newModel"] = from_str(self.new_model)
        if self.cause is not None:
            result["cause"] = from_union([from_none, from_str], self.cause)
        if self.context_tier is not None:
            result["contextTier"] = from_union([from_none, lambda x: to_enum(ContextTier, x)], self.context_tier)
        if self.previous_model is not None:
            result["previousModel"] = from_union([from_none, from_str], self.previous_model)
        if self.previous_reasoning_effort is not None:
            result["previousReasoningEffort"] = from_union([from_none, from_str], self.previous_reasoning_effort)
        if self.previous_reasoning_summary is not None:
            result["previousReasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.previous_reasoning_summary)
        if self.previous_verbosity is not None:
            result["previousVerbosity"] = from_union([from_none, lambda x: to_enum(Verbosity, x)], self.previous_verbosity)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
        if self.source is not None:
            result["source"] = from_union([from_none, lambda x: to_enum(ModelChangeSource, x)], self.source)
        if self.verbosity is not None:
            result["verbosity"] = from_union([from_none, lambda x: to_enum(Verbosity, x)], self.verbosity)
        return result


@dataclass
class SessionPlanChangedData:
    "Plan file operation details indicating what changed"
    operation: PlanChangedOperation

    @staticmethod
    def from_dict(obj: Any) -> "SessionPlanChangedData":
        assert isinstance(obj, dict)
        operation = parse_enum(PlanChangedOperation, obj.get("operation"))
        return SessionPlanChangedData(
            operation=operation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["operation"] = to_enum(PlanChangedOperation, self.operation)
        return result


@dataclass
class SessionRemoteSteerableChangedData:
    "Notifies that the session's remote steering capability has changed"
    remote_steerable: bool

    @staticmethod
    def from_dict(obj: Any) -> "SessionRemoteSteerableChangedData":
        assert isinstance(obj, dict)
        remote_steerable = from_bool(obj.get("remoteSteerable"))
        return SessionRemoteSteerableChangedData(
            remote_steerable=remote_steerable,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["remoteSteerable"] = from_bool(self.remote_steerable)
        return result


@dataclass
class SessionResumeData:
    "Session resume metadata including current context and event count"
    event_count: int
    resume_time: datetime
    already_in_use: bool | None = None
    auto_tier: AutoTier | None = None
    context: WorkingDirectoryContext | None = None
    context_tier: ContextTier | None = None
    continue_pending_work: bool | None = None
    events_file_size_bytes: int | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None
    remote_steerable: bool | None = None
    selected_model: str | None = None
    session_limits: SessionLimitsConfig | None = None
    session_was_active: bool | None = None
    verbosity: Verbosity | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionResumeData":
        assert isinstance(obj, dict)
        event_count = from_int(obj.get("eventCount"))
        resume_time = from_datetime(obj.get("resumeTime"))
        already_in_use = from_union([from_none, from_bool], obj.get("alreadyInUse"))
        auto_tier = from_union([from_none, lambda x: parse_enum(AutoTier, x)], obj.get("autoTier"))
        context = from_union([from_none, WorkingDirectoryContext.from_dict], obj.get("context"))
        context_tier = from_union([from_none, lambda x: parse_enum(ContextTier, x)], obj.get("contextTier"))
        continue_pending_work = from_union([from_none, from_bool], obj.get("continuePendingWork"))
        events_file_size_bytes = from_union([from_none, from_int], obj.get("eventsFileSizeBytes"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        remote_steerable = from_union([from_none, from_bool], obj.get("remoteSteerable"))
        selected_model = from_union([from_none, from_str], obj.get("selectedModel"))
        session_limits = from_union([from_none, SessionLimitsConfig.from_dict], obj.get("sessionLimits"))
        session_was_active = from_union([from_none, from_bool], obj.get("sessionWasActive"))
        verbosity = from_union([from_none, lambda x: parse_enum(Verbosity, x)], obj.get("verbosity"))
        return SessionResumeData(
            event_count=event_count,
            resume_time=resume_time,
            already_in_use=already_in_use,
            auto_tier=auto_tier,
            context=context,
            context_tier=context_tier,
            continue_pending_work=continue_pending_work,
            events_file_size_bytes=events_file_size_bytes,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
            remote_steerable=remote_steerable,
            selected_model=selected_model,
            session_limits=session_limits,
            session_was_active=session_was_active,
            verbosity=verbosity,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventCount"] = to_int(self.event_count)
        result["resumeTime"] = to_datetime(self.resume_time)
        if self.already_in_use is not None:
            result["alreadyInUse"] = from_union([from_none, from_bool], self.already_in_use)
        if self.auto_tier is not None:
            result["autoTier"] = from_union([from_none, lambda x: to_enum(AutoTier, x)], self.auto_tier)
        if self.context is not None:
            result["context"] = from_union([from_none, lambda x: to_class(WorkingDirectoryContext, x)], self.context)
        if self.context_tier is not None:
            result["contextTier"] = from_union([from_none, lambda x: to_enum(ContextTier, x)], self.context_tier)
        if self.continue_pending_work is not None:
            result["continuePendingWork"] = from_union([from_none, from_bool], self.continue_pending_work)
        if self.events_file_size_bytes is not None:
            result["eventsFileSizeBytes"] = from_union([from_none, to_int], self.events_file_size_bytes)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_none, from_bool], self.remote_steerable)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_none, from_str], self.selected_model)
        if self.session_limits is not None:
            result["sessionLimits"] = from_union([from_none, lambda x: to_class(SessionLimitsConfig, x)], self.session_limits)
        if self.session_was_active is not None:
            result["sessionWasActive"] = from_union([from_none, from_bool], self.session_was_active)
        if self.verbosity is not None:
            result["verbosity"] = from_union([from_none, lambda x: to_enum(Verbosity, x)], self.verbosity)
        return result


@dataclass
class SessionScheduleCancelledData:
    "Scheduled prompt cancelled from the schedule manager dialog"
    id: int

    @staticmethod
    def from_dict(obj: Any) -> "SessionScheduleCancelledData":
        assert isinstance(obj, dict)
        id = from_int(obj.get("id"))
        return SessionScheduleCancelledData(
            id=id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = to_int(self.id)
        return result


@dataclass
class SessionScheduleCreatedData:
    "Scheduled prompt registered via /every or /after"
    id: int
    prompt: str
    at: int | None = None
    cron: str | None = None
    display_prompt: str | None = None
    interval: timedelta | None = None
    origin: ScheduleOrigin | None = None
    recurring: bool | None = None
    self_paced: bool | None = None
    tz: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionScheduleCreatedData":
        assert isinstance(obj, dict)
        id = from_int(obj.get("id"))
        prompt = from_str(obj.get("prompt"))
        at = from_union([from_none, from_int], obj.get("at"))
        cron = from_union([from_none, from_str], obj.get("cron"))
        display_prompt = from_union([from_none, from_str], obj.get("displayPrompt"))
        interval = from_union([from_none, from_timedelta], obj.get("intervalMs"))
        origin = from_union([from_none, lambda x: parse_enum(ScheduleOrigin, x)], obj.get("origin"))
        recurring = from_union([from_none, from_bool], obj.get("recurring"))
        self_paced = from_union([from_none, from_bool], obj.get("selfPaced"))
        tz = from_union([from_none, from_str], obj.get("tz"))
        return SessionScheduleCreatedData(
            id=id,
            prompt=prompt,
            at=at,
            cron=cron,
            display_prompt=display_prompt,
            interval=interval,
            origin=origin,
            recurring=recurring,
            self_paced=self_paced,
            tz=tz,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = to_int(self.id)
        result["prompt"] = from_str(self.prompt)
        if self.at is not None:
            result["at"] = from_union([from_none, to_int], self.at)
        if self.cron is not None:
            result["cron"] = from_union([from_none, from_str], self.cron)
        if self.display_prompt is not None:
            result["displayPrompt"] = from_union([from_none, from_str], self.display_prompt)
        if self.interval is not None:
            result["intervalMs"] = from_union([from_none, to_timedelta_int], self.interval)
        if self.origin is not None:
            result["origin"] = from_union([from_none, lambda x: to_enum(ScheduleOrigin, x)], self.origin)
        if self.recurring is not None:
            result["recurring"] = from_union([from_none, from_bool], self.recurring)
        if self.self_paced is not None:
            result["selfPaced"] = from_union([from_none, from_bool], self.self_paced)
        if self.tz is not None:
            result["tz"] = from_union([from_none, from_str], self.tz)
        return result


@dataclass
class SessionScheduleRearmedData:
    "Self-paced schedule re-armed for its next run"
    id: int
    next_run_at: int

    @staticmethod
    def from_dict(obj: Any) -> "SessionScheduleRearmedData":
        assert isinstance(obj, dict)
        id = from_int(obj.get("id"))
        next_run_at = from_int(obj.get("nextRunAt"))
        return SessionScheduleRearmedData(
            id=id,
            next_run_at=next_run_at,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = to_int(self.id)
        result["nextRunAt"] = to_int(self.next_run_at)
        return result


@dataclass
class SessionSessionLimitsChangedData:
    "Session limits update details. Null clears the limits."
    session_limits: SessionLimitsConfig | None

    @staticmethod
    def from_dict(obj: Any) -> "SessionSessionLimitsChangedData":
        assert isinstance(obj, dict)
        session_limits = from_union([from_none, SessionLimitsConfig.from_dict], obj.get("sessionLimits"))
        return SessionSessionLimitsChangedData(
            session_limits=session_limits,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionLimits"] = from_union([from_none, lambda x: to_class(SessionLimitsConfig, x)], self.session_limits)
        return result


@dataclass
class SessionShutdownData:
    "Session termination metrics including usage statistics, code changes, and shutdown reason"
    code_changes: ShutdownCodeChanges
    model_metrics: dict[str, ShutdownModelMetric]
    session_start_time: int
    shutdown_type: ShutdownType
    total_api_duration: timedelta
    agent_metrics: dict[str, ShutdownAgentMetric] | None = None
    conversation_tokens: int | None = None
    current_model: str | None = None
    current_tokens: int | None = None
    error_reason: str | None = None
    events_file_size_bytes: int | None = None
    system_tokens: int | None = None
    token_details: dict[str, ShutdownTokenDetail] | None = None
    tool_definitions_tokens: int | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    total_nano_aiu: float | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _total_premium_requests: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionShutdownData":
        assert isinstance(obj, dict)
        code_changes = ShutdownCodeChanges.from_dict(obj.get("codeChanges"))
        model_metrics = from_dict(ShutdownModelMetric.from_dict, obj.get("modelMetrics"))
        session_start_time = from_int(obj.get("sessionStartTime"))
        shutdown_type = parse_enum(ShutdownType, obj.get("shutdownType"))
        total_api_duration = from_timedelta(obj.get("totalApiDurationMs"))
        agent_metrics = from_union([from_none, lambda x: from_dict(ShutdownAgentMetric.from_dict, x)], obj.get("agentMetrics"))
        conversation_tokens = from_union([from_none, from_int], obj.get("conversationTokens"))
        current_model = from_union([from_none, from_str], obj.get("currentModel"))
        current_tokens = from_union([from_none, from_int], obj.get("currentTokens"))
        error_reason = from_union([from_none, from_str], obj.get("errorReason"))
        events_file_size_bytes = from_union([from_none, from_int], obj.get("eventsFileSizeBytes"))
        system_tokens = from_union([from_none, from_int], obj.get("systemTokens"))
        token_details = from_union([from_none, lambda x: from_dict(ShutdownTokenDetail.from_dict, x)], obj.get("tokenDetails"))
        tool_definitions_tokens = from_union([from_none, from_int], obj.get("toolDefinitionsTokens"))
        total_nano_aiu = from_union([from_none, from_float], obj.get("totalNanoAiu"))
        _total_premium_requests = from_union([from_none, from_float], obj.get("totalPremiumRequests"))
        return SessionShutdownData(
            code_changes=code_changes,
            model_metrics=model_metrics,
            session_start_time=session_start_time,
            shutdown_type=shutdown_type,
            total_api_duration=total_api_duration,
            agent_metrics=agent_metrics,
            conversation_tokens=conversation_tokens,
            current_model=current_model,
            current_tokens=current_tokens,
            error_reason=error_reason,
            events_file_size_bytes=events_file_size_bytes,
            system_tokens=system_tokens,
            token_details=token_details,
            tool_definitions_tokens=tool_definitions_tokens,
            total_nano_aiu=total_nano_aiu,
            _total_premium_requests=_total_premium_requests,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["codeChanges"] = to_class(ShutdownCodeChanges, self.code_changes)
        result["modelMetrics"] = from_dict(lambda x: to_class(ShutdownModelMetric, x), self.model_metrics)
        result["sessionStartTime"] = to_int(self.session_start_time)
        result["shutdownType"] = to_enum(ShutdownType, self.shutdown_type)
        result["totalApiDurationMs"] = to_timedelta_int(self.total_api_duration)
        if self.agent_metrics is not None:
            result["agentMetrics"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(ShutdownAgentMetric, x), x)], self.agent_metrics)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_int], self.conversation_tokens)
        if self.current_model is not None:
            result["currentModel"] = from_union([from_none, from_str], self.current_model)
        if self.current_tokens is not None:
            result["currentTokens"] = from_union([from_none, to_int], self.current_tokens)
        if self.error_reason is not None:
            result["errorReason"] = from_union([from_none, from_str], self.error_reason)
        if self.events_file_size_bytes is not None:
            result["eventsFileSizeBytes"] = from_union([from_none, to_int], self.events_file_size_bytes)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_int], self.system_tokens)
        if self.token_details is not None:
            result["tokenDetails"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(ShutdownTokenDetail, x), x)], self.token_details)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_int], self.tool_definitions_tokens)
        if self.total_nano_aiu is not None:
            result["totalNanoAiu"] = from_union([from_none, to_float], self.total_nano_aiu)
        if self._total_premium_requests is not None:
            result["totalPremiumRequests"] = from_union([from_none, to_float], self._total_premium_requests)
        return result


@dataclass
class SessionSkillsLoadedData:
    "Payload of `session.skills_loaded` listing resolved skill metadata."
    skills: list[SkillsLoadedSkill]

    @staticmethod
    def from_dict(obj: Any) -> "SessionSkillsLoadedData":
        assert isinstance(obj, dict)
        skills = from_list(SkillsLoadedSkill.from_dict, obj.get("skills"))
        return SessionSkillsLoadedData(
            skills=skills,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["skills"] = from_list(lambda x: to_class(SkillsLoadedSkill, x), self.skills)
        return result


@dataclass
class SessionSnapshotRewindData:
    "Session rewind details including target event and count of removed events"
    events_removed: int
    up_to_event_id: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionSnapshotRewindData":
        assert isinstance(obj, dict)
        events_removed = from_int(obj.get("eventsRemoved"))
        up_to_event_id = from_str(obj.get("upToEventId"))
        return SessionSnapshotRewindData(
            events_removed=events_removed,
            up_to_event_id=up_to_event_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventsRemoved"] = to_int(self.events_removed)
        result["upToEventId"] = from_str(self.up_to_event_id)
        return result


@dataclass
class SessionStartData:
    "Session initialization metadata including context and configuration"
    copilot_version: str
    producer: str
    session_id: str
    start_time: datetime
    version: int
    already_in_use: bool | None = None
    auto_tier: AutoTier | None = None
    context: WorkingDirectoryContext | None = None
    context_tier: ContextTier | None = None
    detached_from_spawning_parent_session_id: str | None = None
    github_mcp_tool_config: GitHubMcpToolConfig | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None
    remote_steerable: bool | None = None
    selected_model: str | None = None
    session_limits: SessionLimitsConfig | None = None
    verbosity: Verbosity | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionStartData":
        assert isinstance(obj, dict)
        copilot_version = from_str(obj.get("copilotVersion"))
        producer = from_str(obj.get("producer"))
        session_id = from_str(obj.get("sessionId"))
        start_time = from_datetime(obj.get("startTime"))
        version = from_int(obj.get("version"))
        already_in_use = from_union([from_none, from_bool], obj.get("alreadyInUse"))
        auto_tier = from_union([from_none, lambda x: parse_enum(AutoTier, x)], obj.get("autoTier"))
        context = from_union([from_none, WorkingDirectoryContext.from_dict], obj.get("context"))
        context_tier = from_union([from_none, lambda x: parse_enum(ContextTier, x)], obj.get("contextTier"))
        detached_from_spawning_parent_session_id = from_union([from_none, from_str], obj.get("detachedFromSpawningParentSessionId"))
        github_mcp_tool_config = from_union([from_none, GitHubMcpToolConfig.from_dict], obj.get("githubMcpToolConfig"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        remote_steerable = from_union([from_none, from_bool], obj.get("remoteSteerable"))
        selected_model = from_union([from_none, from_str], obj.get("selectedModel"))
        session_limits = from_union([from_none, SessionLimitsConfig.from_dict], obj.get("sessionLimits"))
        verbosity = from_union([from_none, lambda x: parse_enum(Verbosity, x)], obj.get("verbosity"))
        return SessionStartData(
            copilot_version=copilot_version,
            producer=producer,
            session_id=session_id,
            start_time=start_time,
            version=version,
            already_in_use=already_in_use,
            auto_tier=auto_tier,
            context=context,
            context_tier=context_tier,
            detached_from_spawning_parent_session_id=detached_from_spawning_parent_session_id,
            github_mcp_tool_config=github_mcp_tool_config,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
            remote_steerable=remote_steerable,
            selected_model=selected_model,
            session_limits=session_limits,
            verbosity=verbosity,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["copilotVersion"] = from_str(self.copilot_version)
        result["producer"] = from_str(self.producer)
        result["sessionId"] = from_str(self.session_id)
        result["startTime"] = to_datetime(self.start_time)
        result["version"] = to_int(self.version)
        if self.already_in_use is not None:
            result["alreadyInUse"] = from_union([from_none, from_bool], self.already_in_use)
        if self.auto_tier is not None:
            result["autoTier"] = from_union([from_none, lambda x: to_enum(AutoTier, x)], self.auto_tier)
        if self.context is not None:
            result["context"] = from_union([from_none, lambda x: to_class(WorkingDirectoryContext, x)], self.context)
        if self.context_tier is not None:
            result["contextTier"] = from_union([from_none, lambda x: to_enum(ContextTier, x)], self.context_tier)
        if self.detached_from_spawning_parent_session_id is not None:
            result["detachedFromSpawningParentSessionId"] = from_union([from_none, from_str], self.detached_from_spawning_parent_session_id)
        if self.github_mcp_tool_config is not None:
            result["githubMcpToolConfig"] = from_union([from_none, lambda x: to_class(GitHubMcpToolConfig, x)], self.github_mcp_tool_config)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_none, from_bool], self.remote_steerable)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_none, from_str], self.selected_model)
        if self.session_limits is not None:
            result["sessionLimits"] = from_union([from_none, lambda x: to_class(SessionLimitsConfig, x)], self.session_limits)
        if self.verbosity is not None:
            result["verbosity"] = from_union([from_none, lambda x: to_enum(Verbosity, x)], self.verbosity)
        return result


@dataclass
class SessionTaskCompleteData:
    "Task completion notification with summary from the agent"
    objective_id: int | None = None
    outcome: TaskCompletionOutcome | None = None
    reason: str | None = None
    success: bool | None = None
    summary: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionTaskCompleteData":
        assert isinstance(obj, dict)
        objective_id = from_union([from_none, from_int], obj.get("objectiveId"))
        outcome = from_union([from_none, lambda x: parse_enum(TaskCompletionOutcome, x)], obj.get("outcome"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        success = from_union([from_none, from_bool], obj.get("success"))
        summary = from_union([from_none, from_str], obj.get("summary"))
        return SessionTaskCompleteData(
            objective_id=objective_id,
            outcome=outcome,
            reason=reason,
            success=success,
            summary=summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.objective_id is not None:
            result["objectiveId"] = from_union([from_none, to_int], self.objective_id)
        if self.outcome is not None:
            result["outcome"] = from_union([from_none, lambda x: to_enum(TaskCompletionOutcome, x)], self.outcome)
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        if self.success is not None:
            result["success"] = from_union([from_none, from_bool], self.success)
        if self.summary is not None:
            result["summary"] = from_union([from_none, from_str], self.summary)
        return result


@dataclass
class SessionTitleChangedData:
    "Session title change payload containing the new display title"
    title: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionTitleChangedData":
        assert isinstance(obj, dict)
        title = from_str(obj.get("title"))
        return SessionTitleChangedData(
            title=title,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["title"] = from_str(self.title)
        return result


@dataclass
class SessionTodosChangedData:
    "Signal-only event: the agent's todos or todo_deps table was written to. No payload — clients should call session.plan.readSqlTodosWithDependencies() to fetch the current state. Events arrive in order; clients can debounce on arrival if needed."
    @staticmethod
    def from_dict(obj: Any) -> "SessionTodosChangedData":
        assert isinstance(obj, dict)
        return SessionTodosChangedData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class SessionToolsUpdatedData:
    "Payload of `session.tools_updated` identifying the model whose resolved tools were updated."
    model: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionToolsUpdatedData":
        assert isinstance(obj, dict)
        model = from_str(obj.get("model"))
        return SessionToolsUpdatedData(
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["model"] = from_str(self.model)
        return result


@dataclass
class SessionTruncationData:
    "Conversation truncation statistics including token counts and removed content metrics"
    messages_removed_during_truncation: int
    performed_by: str
    post_truncation_messages_length: int
    post_truncation_tokens_in_messages: int
    pre_truncation_messages_length: int
    pre_truncation_tokens_in_messages: int
    token_limit: int
    tokens_removed_during_truncation: int

    @staticmethod
    def from_dict(obj: Any) -> "SessionTruncationData":
        assert isinstance(obj, dict)
        messages_removed_during_truncation = from_int(obj.get("messagesRemovedDuringTruncation"))
        performed_by = from_str(obj.get("performedBy"))
        post_truncation_messages_length = from_int(obj.get("postTruncationMessagesLength"))
        post_truncation_tokens_in_messages = from_int(obj.get("postTruncationTokensInMessages"))
        pre_truncation_messages_length = from_int(obj.get("preTruncationMessagesLength"))
        pre_truncation_tokens_in_messages = from_int(obj.get("preTruncationTokensInMessages"))
        token_limit = from_int(obj.get("tokenLimit"))
        tokens_removed_during_truncation = from_int(obj.get("tokensRemovedDuringTruncation"))
        return SessionTruncationData(
            messages_removed_during_truncation=messages_removed_during_truncation,
            performed_by=performed_by,
            post_truncation_messages_length=post_truncation_messages_length,
            post_truncation_tokens_in_messages=post_truncation_tokens_in_messages,
            pre_truncation_messages_length=pre_truncation_messages_length,
            pre_truncation_tokens_in_messages=pre_truncation_tokens_in_messages,
            token_limit=token_limit,
            tokens_removed_during_truncation=tokens_removed_during_truncation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["messagesRemovedDuringTruncation"] = to_int(self.messages_removed_during_truncation)
        result["performedBy"] = from_str(self.performed_by)
        result["postTruncationMessagesLength"] = to_int(self.post_truncation_messages_length)
        result["postTruncationTokensInMessages"] = to_int(self.post_truncation_tokens_in_messages)
        result["preTruncationMessagesLength"] = to_int(self.pre_truncation_messages_length)
        result["preTruncationTokensInMessages"] = to_int(self.pre_truncation_tokens_in_messages)
        result["tokenLimit"] = to_int(self.token_limit)
        result["tokensRemovedDuringTruncation"] = to_int(self.tokens_removed_during_truncation)
        return result


@dataclass
class SessionUsageCheckpointData:
    "Durable session usage checkpoint for reconstructing aggregate accounting on resume"
    total_nano_aiu: float
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _model_cache_state: list[_UsageCheckpointModelCacheState] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _prompt_cache_break_state: list[Any] | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _total_premium_requests: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionUsageCheckpointData":
        assert isinstance(obj, dict)
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        _model_cache_state = from_union([from_none, lambda x: from_list(_UsageCheckpointModelCacheState.from_dict, x)], obj.get("modelCacheState"))
        _prompt_cache_break_state = from_union([from_none, lambda x: from_list(lambda x: x, x)], obj.get("promptCacheBreakState"))
        _total_premium_requests = from_union([from_none, from_float], obj.get("totalPremiumRequests"))
        return SessionUsageCheckpointData(
            total_nano_aiu=total_nano_aiu,
            _model_cache_state=_model_cache_state,
            _prompt_cache_break_state=_prompt_cache_break_state,
            _total_premium_requests=_total_premium_requests,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        if self._model_cache_state is not None:
            result["modelCacheState"] = from_union([from_none, lambda x: from_list(lambda x: to_class(_UsageCheckpointModelCacheState, x), x)], self._model_cache_state)
        if self._prompt_cache_break_state is not None:
            result["promptCacheBreakState"] = from_union([from_none, lambda x: from_list(lambda x: x, x)], self._prompt_cache_break_state)
        if self._total_premium_requests is not None:
            result["totalPremiumRequests"] = from_union([from_none, to_float], self._total_premium_requests)
        return result


@dataclass
class SessionUsageInfoData:
    "Current context window usage statistics including token and message counts"
    current_tokens: int
    messages_length: int
    token_limit: int
    conversation_tokens: int | None = None
    is_initial: bool | None = None
    system_tokens: int | None = None
    tool_definitions_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionUsageInfoData":
        assert isinstance(obj, dict)
        current_tokens = from_int(obj.get("currentTokens"))
        messages_length = from_int(obj.get("messagesLength"))
        token_limit = from_int(obj.get("tokenLimit"))
        conversation_tokens = from_union([from_none, from_int], obj.get("conversationTokens"))
        is_initial = from_union([from_none, from_bool], obj.get("isInitial"))
        system_tokens = from_union([from_none, from_int], obj.get("systemTokens"))
        tool_definitions_tokens = from_union([from_none, from_int], obj.get("toolDefinitionsTokens"))
        return SessionUsageInfoData(
            current_tokens=current_tokens,
            messages_length=messages_length,
            token_limit=token_limit,
            conversation_tokens=conversation_tokens,
            is_initial=is_initial,
            system_tokens=system_tokens,
            tool_definitions_tokens=tool_definitions_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["currentTokens"] = to_int(self.current_tokens)
        result["messagesLength"] = to_int(self.messages_length)
        result["tokenLimit"] = to_int(self.token_limit)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_int], self.conversation_tokens)
        if self.is_initial is not None:
            result["isInitial"] = from_union([from_none, from_bool], self.is_initial)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_int], self.system_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_int], self.tool_definitions_tokens)
        return result


@dataclass
class SessionWarningData:
    "Warning message for timeline display with categorization"
    message: str
    warning_type: str
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionWarningData":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        warning_type = from_str(obj.get("warningType"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionWarningData(
            message=message,
            warning_type=warning_type,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        result["warningType"] = from_str(self.warning_type)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


@dataclass
class SessionWorkspaceFileChangedData:
    "Workspace file change details including path and operation type"
    operation: WorkspaceFileChangedOperation
    path: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionWorkspaceFileChangedData":
        assert isinstance(obj, dict)
        operation = parse_enum(WorkspaceFileChangedOperation, obj.get("operation"))
        path = from_str(obj.get("path"))
        return SessionWorkspaceFileChangedData(
            operation=operation,
            path=path,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["operation"] = to_enum(WorkspaceFileChangedOperation, self.operation)
        result["path"] = from_str(self.path)
        return result


@dataclass
class ShutdownAgentMetric:
    "Usage attributed to one agent instance at session shutdown."
    model_metrics: dict[str, ShutdownModelMetric]
    total_api_duration: timedelta
    total_nano_aiu: float
    agent_display_name: str | None = None
    agent_name: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownAgentMetric":
        assert isinstance(obj, dict)
        model_metrics = from_dict(ShutdownModelMetric.from_dict, obj.get("modelMetrics"))
        total_api_duration = from_timedelta(obj.get("totalApiDurationMs"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        agent_display_name = from_union([from_none, from_str], obj.get("agentDisplayName"))
        agent_name = from_union([from_none, from_str], obj.get("agentName"))
        return ShutdownAgentMetric(
            model_metrics=model_metrics,
            total_api_duration=total_api_duration,
            total_nano_aiu=total_nano_aiu,
            agent_display_name=agent_display_name,
            agent_name=agent_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["modelMetrics"] = from_dict(lambda x: to_class(ShutdownModelMetric, x), self.model_metrics)
        result["totalApiDurationMs"] = to_timedelta_int(self.total_api_duration)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        if self.agent_display_name is not None:
            result["agentDisplayName"] = from_union([from_none, from_str], self.agent_display_name)
        if self.agent_name is not None:
            result["agentName"] = from_union([from_none, from_str], self.agent_name)
        return result


@dataclass
class ShutdownCodeChanges:
    "Aggregate code change metrics for the session"
    files_modified: list[str]
    lines_added: int
    lines_removed: int

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownCodeChanges":
        assert isinstance(obj, dict)
        files_modified = from_list(from_str, obj.get("filesModified"))
        lines_added = from_int(obj.get("linesAdded"))
        lines_removed = from_int(obj.get("linesRemoved"))
        return ShutdownCodeChanges(
            files_modified=files_modified,
            lines_added=lines_added,
            lines_removed=lines_removed,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["filesModified"] = from_list(from_str, self.files_modified)
        result["linesAdded"] = to_int(self.lines_added)
        result["linesRemoved"] = to_int(self.lines_removed)
        return result


@dataclass
class ShutdownModelMetric:
    "Per-model shutdown metrics with request counts, token usage, nano-AI units, and token details."
    requests: ShutdownModelMetricRequests
    usage: ShutdownModelMetricUsage
    token_details: dict[str, ShutdownModelMetricTokenDetail] | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    total_nano_aiu: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetric":
        assert isinstance(obj, dict)
        requests = ShutdownModelMetricRequests.from_dict(obj.get("requests"))
        usage = ShutdownModelMetricUsage.from_dict(obj.get("usage"))
        token_details = from_union([from_none, lambda x: from_dict(ShutdownModelMetricTokenDetail.from_dict, x)], obj.get("tokenDetails"))
        total_nano_aiu = from_union([from_none, from_float], obj.get("totalNanoAiu"))
        return ShutdownModelMetric(
            requests=requests,
            usage=usage,
            token_details=token_details,
            total_nano_aiu=total_nano_aiu,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requests"] = to_class(ShutdownModelMetricRequests, self.requests)
        result["usage"] = to_class(ShutdownModelMetricUsage, self.usage)
        if self.token_details is not None:
            result["tokenDetails"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(ShutdownModelMetricTokenDetail, x), x)], self.token_details)
        if self.total_nano_aiu is not None:
            result["totalNanoAiu"] = from_union([from_none, to_float], self.total_nano_aiu)
        return result


@dataclass
class ShutdownModelMetricRequests:
    "Request count and cost metrics"
    # Experimental: this field is part of an experimental API and may change or be removed.
    cost: float | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    count: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetricRequests":
        assert isinstance(obj, dict)
        cost = from_union([from_none, from_float], obj.get("cost"))
        count = from_union([from_none, from_int], obj.get("count"))
        return ShutdownModelMetricRequests(
            cost=cost,
            count=count,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.cost is not None:
            result["cost"] = from_union([from_none, to_float], self.cost)
        if self.count is not None:
            result["count"] = from_union([from_none, to_int], self.count)
        return result


@dataclass
class ShutdownModelMetricTokenDetail:
    "A token-type entry in a shutdown model metric, storing the accumulated token count."
    token_count: int

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetricTokenDetail":
        assert isinstance(obj, dict)
        token_count = from_int(obj.get("tokenCount"))
        return ShutdownModelMetricTokenDetail(
            token_count=token_count,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenCount"] = to_int(self.token_count)
        return result


@dataclass
class ShutdownModelMetricUsage:
    "Token usage breakdown"
    cache_read_tokens: int
    cache_write_tokens: int
    input_tokens: int
    output_tokens: int
    reasoning_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetricUsage":
        assert isinstance(obj, dict)
        cache_read_tokens = from_int(obj.get("cacheReadTokens"))
        cache_write_tokens = from_int(obj.get("cacheWriteTokens"))
        input_tokens = from_int(obj.get("inputTokens"))
        output_tokens = from_int(obj.get("outputTokens"))
        reasoning_tokens = from_union([from_none, from_int], obj.get("reasoningTokens"))
        return ShutdownModelMetricUsage(
            cache_read_tokens=cache_read_tokens,
            cache_write_tokens=cache_write_tokens,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            reasoning_tokens=reasoning_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cacheReadTokens"] = to_int(self.cache_read_tokens)
        result["cacheWriteTokens"] = to_int(self.cache_write_tokens)
        result["inputTokens"] = to_int(self.input_tokens)
        result["outputTokens"] = to_int(self.output_tokens)
        if self.reasoning_tokens is not None:
            result["reasoningTokens"] = from_union([from_none, to_int], self.reasoning_tokens)
        return result


@dataclass
class ShutdownTokenDetail:
    "A session-wide shutdown token-type entry storing the accumulated token count."
    token_count: int

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownTokenDetail":
        assert isinstance(obj, dict)
        token_count = from_int(obj.get("tokenCount"))
        return ShutdownTokenDetail(
            token_count=token_count,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenCount"] = to_int(self.token_count)
        return result


@dataclass
class SkillInvokedData:
    "Skill invocation details including content, allowed tools, and plugin metadata"
    content: str
    name: str
    path: str
    allowed_tools: list[str] | None = None
    description: str | None = None
    model: str | None = None
    plugin_name: str | None = None
    plugin_version: str | None = None
    source: str | None = None
    trigger: SkillInvokedTrigger | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SkillInvokedData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        name = from_str(obj.get("name"))
        path = from_str(obj.get("path"))
        allowed_tools = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("allowedTools"))
        description = from_union([from_none, from_str], obj.get("description"))
        model = from_union([from_none, from_str], obj.get("model"))
        plugin_name = from_union([from_none, from_str], obj.get("pluginName"))
        plugin_version = from_union([from_none, from_str], obj.get("pluginVersion"))
        source = from_union([from_none, from_str], obj.get("source"))
        trigger = from_union([from_none, lambda x: parse_enum(SkillInvokedTrigger, x)], obj.get("trigger"))
        return SkillInvokedData(
            content=content,
            name=name,
            path=path,
            allowed_tools=allowed_tools,
            description=description,
            model=model,
            plugin_name=plugin_name,
            plugin_version=plugin_version,
            source=source,
            trigger=trigger,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["name"] = from_str(self.name)
        result["path"] = from_str(self.path)
        if self.allowed_tools is not None:
            result["allowedTools"] = from_union([from_none, lambda x: from_list(from_str, x)], self.allowed_tools)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.plugin_name is not None:
            result["pluginName"] = from_union([from_none, from_str], self.plugin_name)
        if self.plugin_version is not None:
            result["pluginVersion"] = from_union([from_none, from_str], self.plugin_version)
        if self.source is not None:
            result["source"] = from_union([from_none, from_str], self.source)
        if self.trigger is not None:
            result["trigger"] = from_union([from_none, lambda x: to_enum(SkillInvokedTrigger, x)], self.trigger)
        return result


@dataclass
class SkillsLoadedSkill:
    "A single resolved skill in `session.skills_loaded`, including source, invocability, enabled state, path, and argument hint."
    description: str
    enabled: bool
    name: str
    source: SkillSource
    user_invocable: bool
    argument_hint: str | None = None
    command_name: str | None = None
    path: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SkillsLoadedSkill":
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = parse_enum(SkillSource, obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        argument_hint = from_union([from_none, from_str], obj.get("argumentHint"))
        command_name = from_union([from_none, from_str], obj.get("commandName"))
        path = from_union([from_none, from_str], obj.get("path"))
        return SkillsLoadedSkill(
            description=description,
            enabled=enabled,
            name=name,
            source=source,
            user_invocable=user_invocable,
            argument_hint=argument_hint,
            command_name=command_name,
            path=path,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(SkillSource, self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.argument_hint is not None:
            result["argumentHint"] = from_union([from_none, from_str], self.argument_hint)
        if self.command_name is not None:
            result["commandName"] = from_union([from_none, from_str], self.command_name)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        return result


@dataclass
class SubagentCompletedData:
    "Sub-agent completion details for successful execution"
    agent_display_name: str
    agent_name: str
    tool_call_id: str
    cancelled: bool | None = None
    configured_model_matches_actual: bool | None = None
    configured_model_preference: str | None = None
    duration: timedelta | None = None
    explicit_model_matches_preference: bool | None = None
    explicit_model_override: str | None = None
    first_dispatched_model: str | None = None
    model: str | None = None
    total_tokens: int | None = None
    total_tool_calls: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentCompletedData":
        assert isinstance(obj, dict)
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_name = from_str(obj.get("agentName"))
        tool_call_id = from_str(obj.get("toolCallId"))
        cancelled = from_union([from_none, from_bool], obj.get("cancelled"))
        configured_model_matches_actual = from_union([from_none, from_bool], obj.get("configuredModelMatchesActual"))
        configured_model_preference = from_union([from_none, from_str], obj.get("configuredModelPreference"))
        duration = from_union([from_none, from_timedelta], obj.get("durationMs"))
        explicit_model_matches_preference = from_union([from_none, from_bool], obj.get("explicitModelMatchesPreference"))
        explicit_model_override = from_union([from_none, from_str], obj.get("explicitModelOverride"))
        first_dispatched_model = from_union([from_none, from_str], obj.get("firstDispatchedModel"))
        model = from_union([from_none, from_str], obj.get("model"))
        total_tokens = from_union([from_none, from_int], obj.get("totalTokens"))
        total_tool_calls = from_union([from_none, from_int], obj.get("totalToolCalls"))
        return SubagentCompletedData(
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            tool_call_id=tool_call_id,
            cancelled=cancelled,
            configured_model_matches_actual=configured_model_matches_actual,
            configured_model_preference=configured_model_preference,
            duration=duration,
            explicit_model_matches_preference=explicit_model_matches_preference,
            explicit_model_override=explicit_model_override,
            first_dispatched_model=first_dispatched_model,
            model=model,
            total_tokens=total_tokens,
            total_tool_calls=total_tool_calls,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentName"] = from_str(self.agent_name)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.cancelled is not None:
            result["cancelled"] = from_union([from_none, from_bool], self.cancelled)
        if self.configured_model_matches_actual is not None:
            result["configuredModelMatchesActual"] = from_union([from_none, from_bool], self.configured_model_matches_actual)
        if self.configured_model_preference is not None:
            result["configuredModelPreference"] = from_union([from_none, from_str], self.configured_model_preference)
        if self.duration is not None:
            result["durationMs"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.explicit_model_matches_preference is not None:
            result["explicitModelMatchesPreference"] = from_union([from_none, from_bool], self.explicit_model_matches_preference)
        if self.explicit_model_override is not None:
            result["explicitModelOverride"] = from_union([from_none, from_str], self.explicit_model_override)
        if self.first_dispatched_model is not None:
            result["firstDispatchedModel"] = from_union([from_none, from_str], self.first_dispatched_model)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.total_tokens is not None:
            result["totalTokens"] = from_union([from_none, to_int], self.total_tokens)
        if self.total_tool_calls is not None:
            result["totalToolCalls"] = from_union([from_none, to_int], self.total_tool_calls)
        return result


@dataclass
class SubagentConfiguredData:
    "Resolved runtime configuration for a configured sub-agent"
    model: str
    multi_turn: bool
    context_tier: str | None = None
    reasoning_effort: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentConfiguredData":
        assert isinstance(obj, dict)
        model = from_str(obj.get("model"))
        multi_turn = from_bool(obj.get("multiTurn"))
        context_tier = from_union([from_none, from_str], obj.get("contextTier"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        return SubagentConfiguredData(
            model=model,
            multi_turn=multi_turn,
            context_tier=context_tier,
            reasoning_effort=reasoning_effort,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["model"] = from_str(self.model)
        result["multiTurn"] = from_bool(self.multi_turn)
        if self.context_tier is not None:
            result["contextTier"] = from_union([from_none, from_str], self.context_tier)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        return result


@dataclass
class SubagentDeselectedData:
    "Empty payload; the event signals that the custom agent was deselected, returning to the default agent"
    @staticmethod
    def from_dict(obj: Any) -> "SubagentDeselectedData":
        assert isinstance(obj, dict)
        return SubagentDeselectedData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class SubagentFailedData:
    "Sub-agent failure details including error message and agent information"
    agent_display_name: str
    agent_name: str
    error: str
    tool_call_id: str
    configured_model_matches_actual: bool | None = None
    configured_model_preference: str | None = None
    duration: timedelta | None = None
    explicit_model_matches_preference: bool | None = None
    explicit_model_override: str | None = None
    first_dispatched_model: str | None = None
    model: str | None = None
    total_tokens: int | None = None
    total_tool_calls: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentFailedData":
        assert isinstance(obj, dict)
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_name = from_str(obj.get("agentName"))
        error = from_str(obj.get("error"))
        tool_call_id = from_str(obj.get("toolCallId"))
        configured_model_matches_actual = from_union([from_none, from_bool], obj.get("configuredModelMatchesActual"))
        configured_model_preference = from_union([from_none, from_str], obj.get("configuredModelPreference"))
        duration = from_union([from_none, from_timedelta], obj.get("durationMs"))
        explicit_model_matches_preference = from_union([from_none, from_bool], obj.get("explicitModelMatchesPreference"))
        explicit_model_override = from_union([from_none, from_str], obj.get("explicitModelOverride"))
        first_dispatched_model = from_union([from_none, from_str], obj.get("firstDispatchedModel"))
        model = from_union([from_none, from_str], obj.get("model"))
        total_tokens = from_union([from_none, from_int], obj.get("totalTokens"))
        total_tool_calls = from_union([from_none, from_int], obj.get("totalToolCalls"))
        return SubagentFailedData(
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            error=error,
            tool_call_id=tool_call_id,
            configured_model_matches_actual=configured_model_matches_actual,
            configured_model_preference=configured_model_preference,
            duration=duration,
            explicit_model_matches_preference=explicit_model_matches_preference,
            explicit_model_override=explicit_model_override,
            first_dispatched_model=first_dispatched_model,
            model=model,
            total_tokens=total_tokens,
            total_tool_calls=total_tool_calls,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentName"] = from_str(self.agent_name)
        result["error"] = from_str(self.error)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.configured_model_matches_actual is not None:
            result["configuredModelMatchesActual"] = from_union([from_none, from_bool], self.configured_model_matches_actual)
        if self.configured_model_preference is not None:
            result["configuredModelPreference"] = from_union([from_none, from_str], self.configured_model_preference)
        if self.duration is not None:
            result["durationMs"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.explicit_model_matches_preference is not None:
            result["explicitModelMatchesPreference"] = from_union([from_none, from_bool], self.explicit_model_matches_preference)
        if self.explicit_model_override is not None:
            result["explicitModelOverride"] = from_union([from_none, from_str], self.explicit_model_override)
        if self.first_dispatched_model is not None:
            result["firstDispatchedModel"] = from_union([from_none, from_str], self.first_dispatched_model)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.total_tokens is not None:
            result["totalTokens"] = from_union([from_none, to_int], self.total_tokens)
        if self.total_tool_calls is not None:
            result["totalToolCalls"] = from_union([from_none, to_int], self.total_tool_calls)
        return result


@dataclass
class SubagentSelectedData:
    "Custom agent selection details including name and available tools"
    agent_display_name: str
    agent_name: str
    tools: list[str] | None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentSelectedData":
        assert isinstance(obj, dict)
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_name = from_str(obj.get("agentName"))
        tools = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("tools"))
        return SubagentSelectedData(
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            tools=tools,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentName"] = from_str(self.agent_name)
        result["tools"] = from_union([from_none, lambda x: from_list(from_str, x)], self.tools)
        return result


@dataclass
class SubagentStartedData:
    "Sub-agent startup details including parent tool call and agent information"
    agent_description: str
    agent_display_name: str
    agent_name: str
    tool_call_id: str
    agent_type: str | None = None
    execution_mode: str | None = None
    factory_run_id: str | None = None
    model: str | None = None
    parent_id: str | None = None
    resumable: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentStartedData":
        assert isinstance(obj, dict)
        agent_description = from_str(obj.get("agentDescription"))
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_name = from_str(obj.get("agentName"))
        tool_call_id = from_str(obj.get("toolCallId"))
        agent_type = from_union([from_none, from_str], obj.get("agentType"))
        execution_mode = from_union([from_none, from_str], obj.get("executionMode"))
        factory_run_id = from_union([from_none, from_str], obj.get("factoryRunId"))
        model = from_union([from_none, from_str], obj.get("model"))
        parent_id = from_union([from_none, from_str], obj.get("parentId"))
        resumable = from_union([from_none, from_bool], obj.get("resumable"))
        return SubagentStartedData(
            agent_description=agent_description,
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            tool_call_id=tool_call_id,
            agent_type=agent_type,
            execution_mode=execution_mode,
            factory_run_id=factory_run_id,
            model=model,
            parent_id=parent_id,
            resumable=resumable,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentDescription"] = from_str(self.agent_description)
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentName"] = from_str(self.agent_name)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.agent_type is not None:
            result["agentType"] = from_union([from_none, from_str], self.agent_type)
        if self.execution_mode is not None:
            result["executionMode"] = from_union([from_none, from_str], self.execution_mode)
        if self.factory_run_id is not None:
            result["factoryRunId"] = from_union([from_none, from_str], self.factory_run_id)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.parent_id is not None:
            result["parentId"] = from_union([from_none, from_str], self.parent_id)
        if self.resumable is not None:
            result["resumable"] = from_union([from_none, from_bool], self.resumable)
        return result


@dataclass
class SystemMessageData:
    "System/developer instruction content with role and optional template metadata"
    content: str
    role: SystemMessageRole
    interaction_id: str | None = None
    metadata: SystemMessageMetadata | None = None
    name: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        role = parse_enum(SystemMessageRole, obj.get("role"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        metadata = from_union([from_none, SystemMessageMetadata.from_dict], obj.get("metadata"))
        name = from_union([from_none, from_str], obj.get("name"))
        return SystemMessageData(
            content=content,
            role=role,
            interaction_id=interaction_id,
            metadata=metadata,
            name=name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["role"] = to_enum(SystemMessageRole, self.role)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.metadata is not None:
            result["metadata"] = from_union([from_none, lambda x: to_class(SystemMessageMetadata, x)], self.metadata)
        if self.name is not None:
            result["name"] = from_union([from_none, from_str], self.name)
        return result


@dataclass
class SystemMessageMetadata:
    "Metadata about the prompt template and its construction"
    prompt_version: str | None = None
    variables: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemMessageMetadata":
        assert isinstance(obj, dict)
        prompt_version = from_union([from_none, from_str], obj.get("promptVersion"))
        variables = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("variables"))
        return SystemMessageMetadata(
            prompt_version=prompt_version,
            variables=variables,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.prompt_version is not None:
            result["promptVersion"] = from_union([from_none, from_str], self.prompt_version)
        if self.variables is not None:
            result["variables"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.variables)
        return result


@dataclass
class SystemNotificationAgentCompleted:
    "System notification metadata for a background agent that completed or failed, including agent ID, type, status, description, and prompt."
    agent_id: str
    agent_type: str
    status: SystemNotificationAgentCompletedStatus
    type: ClassVar[str] = "agent_completed"
    description: str | None = None
    display_name: str | None = None
    prompt: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationAgentCompleted":
        assert isinstance(obj, dict)
        agent_id = from_str(obj.get("agentId"))
        agent_type = from_str(obj.get("agentType"))
        status = parse_enum(SystemNotificationAgentCompletedStatus, obj.get("status"))
        description = from_union([from_none, from_str], obj.get("description"))
        display_name = from_union([from_none, from_str], obj.get("displayName"))
        prompt = from_union([from_none, from_str], obj.get("prompt"))
        return SystemNotificationAgentCompleted(
            agent_id=agent_id,
            agent_type=agent_type,
            status=status,
            description=description,
            display_name=display_name,
            prompt=prompt,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentId"] = from_str(self.agent_id)
        result["agentType"] = from_str(self.agent_type)
        result["status"] = to_enum(SystemNotificationAgentCompletedStatus, self.status)
        result["type"] = self.type
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.display_name is not None:
            result["displayName"] = from_union([from_none, from_str], self.display_name)
        if self.prompt is not None:
            result["prompt"] = from_union([from_none, from_str], self.prompt)
        return result


@dataclass
class SystemNotificationAgentIdle:
    "System notification metadata for a background agent that became idle, including agent ID, type, and description."
    agent_id: str
    agent_type: str
    type: ClassVar[str] = "agent_idle"
    description: str | None = None
    display_name: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationAgentIdle":
        assert isinstance(obj, dict)
        agent_id = from_str(obj.get("agentId"))
        agent_type = from_str(obj.get("agentType"))
        description = from_union([from_none, from_str], obj.get("description"))
        display_name = from_union([from_none, from_str], obj.get("displayName"))
        return SystemNotificationAgentIdle(
            agent_id=agent_id,
            agent_type=agent_type,
            description=description,
            display_name=display_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentId"] = from_str(self.agent_id)
        result["agentType"] = from_str(self.agent_type)
        result["type"] = self.type
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.display_name is not None:
            result["displayName"] = from_union([from_none, from_str], self.display_name)
        return result


@dataclass
class SystemNotificationData:
    "System-generated notification for runtime events like background task completion"
    content: str
    kind: SystemNotification

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        kind = _load_SystemNotification(obj.get("kind"))
        return SystemNotificationData(
            content=content,
            kind=kind,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["kind"] = self.kind.to_dict()
        return result


@dataclass
class SystemNotificationFactoryCompleted:
    "System notification metadata for a factory execution attempt that reached a terminal state."
    attempt: int
    consumed_nano_aiu: int
    consumed_subagents: int
    elapsed_ms: int
    factory_name: str
    run_id: str
    status: SystemNotificationFactoryCompletedStatus
    type: ClassVar[str] = "factory_completed"
    failure: Any = None
    result_preview: str | None = None
    retry_guidance: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationFactoryCompleted":
        assert isinstance(obj, dict)
        attempt = from_int(obj.get("attempt"))
        consumed_nano_aiu = from_int(obj.get("consumedNanoAiu"))
        consumed_subagents = from_int(obj.get("consumedSubagents"))
        elapsed_ms = from_int(obj.get("elapsedMs"))
        factory_name = from_str(obj.get("factoryName"))
        run_id = from_str(obj.get("runId"))
        status = parse_enum(SystemNotificationFactoryCompletedStatus, obj.get("status"))
        failure = obj.get("failure")
        result_preview = from_union([from_none, from_str], obj.get("resultPreview"))
        retry_guidance = from_union([from_none, from_str], obj.get("retryGuidance"))
        return SystemNotificationFactoryCompleted(
            attempt=attempt,
            consumed_nano_aiu=consumed_nano_aiu,
            consumed_subagents=consumed_subagents,
            elapsed_ms=elapsed_ms,
            factory_name=factory_name,
            run_id=run_id,
            status=status,
            failure=failure,
            result_preview=result_preview,
            retry_guidance=retry_guidance,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["attempt"] = to_int(self.attempt)
        result["consumedNanoAiu"] = to_int(self.consumed_nano_aiu)
        result["consumedSubagents"] = to_int(self.consumed_subagents)
        result["elapsedMs"] = to_int(self.elapsed_ms)
        result["factoryName"] = from_str(self.factory_name)
        result["runId"] = from_str(self.run_id)
        result["status"] = to_enum(SystemNotificationFactoryCompletedStatus, self.status)
        result["type"] = self.type
        if self.failure is not None:
            result["failure"] = self.failure
        if self.result_preview is not None:
            result["resultPreview"] = from_union([from_none, from_str], self.result_preview)
        if self.retry_guidance is not None:
            result["retryGuidance"] = from_union([from_none, from_str], self.retry_guidance)
        return result


@dataclass
class SystemNotificationInstructionDiscovered:
    "System notification metadata for an instruction file discovered during tool access, including source, trigger file, and tool."
    source_path: str
    trigger_file: str
    trigger_tool: str
    type: ClassVar[str] = "instruction_discovered"
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationInstructionDiscovered":
        assert isinstance(obj, dict)
        source_path = from_str(obj.get("sourcePath"))
        trigger_file = from_str(obj.get("triggerFile"))
        trigger_tool = from_str(obj.get("triggerTool"))
        description = from_union([from_none, from_str], obj.get("description"))
        return SystemNotificationInstructionDiscovered(
            source_path=source_path,
            trigger_file=trigger_file,
            trigger_tool=trigger_tool,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sourcePath"] = from_str(self.source_path)
        result["triggerFile"] = from_str(self.trigger_file)
        result["triggerTool"] = from_str(self.trigger_tool)
        result["type"] = self.type
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        return result


@dataclass
class SystemNotificationNewInboxMessage:
    "System notification metadata for a new inbox message, including entry ID, sender details, and summary."
    entry_id: str
    sender_name: str
    sender_type: str
    summary: str
    type: ClassVar[str] = "new_inbox_message"

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationNewInboxMessage":
        assert isinstance(obj, dict)
        entry_id = from_str(obj.get("entryId"))
        sender_name = from_str(obj.get("senderName"))
        sender_type = from_str(obj.get("senderType"))
        summary = from_str(obj.get("summary"))
        return SystemNotificationNewInboxMessage(
            entry_id=entry_id,
            sender_name=sender_name,
            sender_type=sender_type,
            summary=summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["entryId"] = from_str(self.entry_id)
        result["senderName"] = from_str(self.sender_name)
        result["senderType"] = from_str(self.sender_type)
        result["summary"] = from_str(self.summary)
        result["type"] = self.type
        return result


@dataclass
class SystemNotificationShellCompleted:
    "System notification metadata for a shell session that completed, including shell ID, optional exit code, and description."
    shell_id: str
    type: ClassVar[str] = "shell_completed"
    description: str | None = None
    exit_code: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationShellCompleted":
        assert isinstance(obj, dict)
        shell_id = from_str(obj.get("shellId"))
        description = from_union([from_none, from_str], obj.get("description"))
        exit_code = from_union([from_none, from_int], obj.get("exitCode"))
        return SystemNotificationShellCompleted(
            shell_id=shell_id,
            description=description,
            exit_code=exit_code,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["shellId"] = from_str(self.shell_id)
        result["type"] = self.type
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.exit_code is not None:
            result["exitCode"] = from_union([from_none, to_int], self.exit_code)
        return result


@dataclass
class SystemNotificationShellDetachedCompleted:
    "System notification metadata for a detached shell session that completed, including shell ID and description."
    shell_id: str
    type: ClassVar[str] = "shell_detached_completed"
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationShellDetachedCompleted":
        assert isinstance(obj, dict)
        shell_id = from_str(obj.get("shellId"))
        description = from_union([from_none, from_str], obj.get("description"))
        return SystemNotificationShellDetachedCompleted(
            shell_id=shell_id,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["shellId"] = from_str(self.shell_id)
        result["type"] = self.type
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        return result


@dataclass
class SystemNotificationUnclassified:
    "System notification metadata from an external host that does not match a runtime-owned notification kind."
    type: ClassVar[str] = "unclassified"
    metadata: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationUnclassified":
        assert isinstance(obj, dict)
        metadata = obj.get("metadata")
        return SystemNotificationUnclassified(
            metadata=metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = self.type
        if self.metadata is not None:
            result["metadata"] = self.metadata
        return result


@dataclass
class ToolExecutionCompleteContentAudio:
    "Audio content block with base64-encoded data"
    data: str
    mime_type: str
    type: ClassVar[str] = "audio"

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentAudio":
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        return ToolExecutionCompleteContentAudio(
            data=data,
            mime_type=mime_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        return result


@dataclass
class ToolExecutionCompleteContentImage:
    "Image content block with base64-encoded data"
    data: str
    mime_type: str
    type: ClassVar[str] = "image"

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentImage":
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        return ToolExecutionCompleteContentImage(
            data=data,
            mime_type=mime_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        return result


@dataclass
class ToolExecutionCompleteContentResource:
    "Embedded resource content block with inline text or binary data"
    resource: ToolExecutionCompleteContentResourceDetails
    type: ClassVar[str] = "resource"

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentResource":
        assert isinstance(obj, dict)
        resource = from_union([EmbeddedTextResourceContents.from_dict, EmbeddedBlobResourceContents.from_dict], obj.get("resource"))
        return ToolExecutionCompleteContentResource(
            resource=resource,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["resource"] = from_union([lambda x: to_class(EmbeddedTextResourceContents, x), lambda x: to_class(EmbeddedBlobResourceContents, x)], self.resource)
        result["type"] = self.type
        return result


@dataclass
class ToolExecutionCompleteContentResourceLink:
    "Resource link content block referencing an external resource"
    name: str
    type: ClassVar[str] = "resource_link"
    uri: str
    description: str | None = None
    icons: list[ToolExecutionCompleteContentResourceLinkIcon] | None = None
    mime_type: str | None = None
    size: int | None = None
    title: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentResourceLink":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        uri = from_str(obj.get("uri"))
        description = from_union([from_none, from_str], obj.get("description"))
        icons = from_union([from_none, lambda x: from_list(ToolExecutionCompleteContentResourceLinkIcon.from_dict, x)], obj.get("icons"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        size = from_union([from_none, from_int], obj.get("size"))
        title = from_union([from_none, from_str], obj.get("title"))
        return ToolExecutionCompleteContentResourceLink(
            name=name,
            uri=uri,
            description=description,
            icons=icons,
            mime_type=mime_type,
            size=size,
            title=title,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["type"] = self.type
        result["uri"] = from_str(self.uri)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.icons is not None:
            result["icons"] = from_union([from_none, lambda x: from_list(lambda x: to_class(ToolExecutionCompleteContentResourceLinkIcon, x), x)], self.icons)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        if self.size is not None:
            result["size"] = from_union([from_none, to_int], self.size)
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        return result


@dataclass
class ToolExecutionCompleteContentResourceLinkIcon:
    "Icon image for a resource"
    src: str
    mime_type: str | None = None
    sizes: list[str] | None = None
    theme: ToolExecutionCompleteContentResourceLinkIconTheme | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentResourceLinkIcon":
        assert isinstance(obj, dict)
        src = from_str(obj.get("src"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        sizes = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("sizes"))
        theme = from_union([from_none, lambda x: parse_enum(ToolExecutionCompleteContentResourceLinkIconTheme, x)], obj.get("theme"))
        return ToolExecutionCompleteContentResourceLinkIcon(
            src=src,
            mime_type=mime_type,
            sizes=sizes,
            theme=theme,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["src"] = from_str(self.src)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        if self.sizes is not None:
            result["sizes"] = from_union([from_none, lambda x: from_list(from_str, x)], self.sizes)
        if self.theme is not None:
            result["theme"] = from_union([from_none, lambda x: to_enum(ToolExecutionCompleteContentResourceLinkIconTheme, x)], self.theme)
        return result


@dataclass
class ToolExecutionCompleteContentShellExit:
    "Shell command exit metadata with optional output preview"
    exit_code: int
    shell_id: str
    type: ClassVar[str] = "shell_exit"
    cwd: str | None = None
    output_file_path: str | None = None
    output_preview: str | None = None
    output_truncated: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentShellExit":
        assert isinstance(obj, dict)
        exit_code = from_int(obj.get("exitCode"))
        shell_id = from_str(obj.get("shellId"))
        cwd = from_union([from_none, from_str], obj.get("cwd"))
        output_file_path = from_union([from_none, from_str], obj.get("outputFilePath"))
        output_preview = from_union([from_none, from_str], obj.get("outputPreview"))
        output_truncated = from_union([from_none, from_bool], obj.get("outputTruncated"))
        return ToolExecutionCompleteContentShellExit(
            exit_code=exit_code,
            shell_id=shell_id,
            cwd=cwd,
            output_file_path=output_file_path,
            output_preview=output_preview,
            output_truncated=output_truncated,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["exitCode"] = to_int(self.exit_code)
        result["shellId"] = from_str(self.shell_id)
        result["type"] = self.type
        if self.cwd is not None:
            result["cwd"] = from_union([from_none, from_str], self.cwd)
        if self.output_file_path is not None:
            result["outputFilePath"] = from_union([from_none, from_str], self.output_file_path)
        if self.output_preview is not None:
            result["outputPreview"] = from_union([from_none, from_str], self.output_preview)
        if self.output_truncated is not None:
            result["outputTruncated"] = from_union([from_none, from_bool], self.output_truncated)
        return result


@dataclass
class ToolExecutionCompleteContentText:
    "Plain text content block"
    text: str
    type: ClassVar[str] = "text"

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteContentText":
        assert isinstance(obj, dict)
        text = from_str(obj.get("text"))
        return ToolExecutionCompleteContentText(
            text=text,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["text"] = from_str(self.text)
        result["type"] = self.type
        return result


@dataclass
class ToolExecutionCompleteData:
    "Tool execution completion results including success status, detailed output, and error information"
    success: bool
    tool_call_id: str
    error: ToolExecutionCompleteError | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    fusion: FusionAttribution | None = None
    interaction_id: str | None = None
    is_user_requested: bool | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    mcp_meta: Any = None
    model: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    result: ToolExecutionCompleteResult | None = None
    rte: bool | None = None
    sandboxed: bool | None = None
    tool_description: ToolExecutionCompleteToolDescription | None = None
    tool_telemetry: dict[str, Any] | None = None
    turn_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteData":
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        tool_call_id = from_str(obj.get("toolCallId"))
        error = from_union([from_none, ToolExecutionCompleteError.from_dict], obj.get("error"))
        fusion = from_union([from_none, FusionAttribution.from_dict], obj.get("fusion"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        is_user_requested = from_union([from_none, from_bool], obj.get("isUserRequested"))
        mcp_meta = obj.get("mcpMeta")
        model = from_union([from_none, from_str], obj.get("model"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        result = from_union([from_none, ToolExecutionCompleteResult.from_dict], obj.get("result"))
        rte = from_union([from_none, from_bool], obj.get("rte"))
        sandboxed = from_union([from_none, from_bool], obj.get("sandboxed"))
        tool_description = from_union([from_none, ToolExecutionCompleteToolDescription.from_dict], obj.get("toolDescription"))
        tool_telemetry = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("toolTelemetry"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return ToolExecutionCompleteData(
            success=success,
            tool_call_id=tool_call_id,
            error=error,
            fusion=fusion,
            interaction_id=interaction_id,
            is_user_requested=is_user_requested,
            mcp_meta=mcp_meta,
            model=model,
            parent_tool_call_id=parent_tool_call_id,
            result=result,
            rte=rte,
            sandboxed=sandboxed,
            tool_description=tool_description,
            tool_telemetry=tool_telemetry,
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.error is not None:
            result["error"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteError, x)], self.error)
        if self.fusion is not None:
            result["fusion"] = from_union([from_none, lambda x: to_class(FusionAttribution, x)], self.fusion)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.is_user_requested is not None:
            result["isUserRequested"] = from_union([from_none, from_bool], self.is_user_requested)
        if self.mcp_meta is not None:
            result["mcpMeta"] = self.mcp_meta
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.result is not None:
            result["result"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteResult, x)], self.result)
        if self.rte is not None:
            result["rte"] = from_union([from_none, from_bool], self.rte)
        if self.sandboxed is not None:
            result["sandboxed"] = from_union([from_none, from_bool], self.sandboxed)
        if self.tool_description is not None:
            result["toolDescription"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteToolDescription, x)], self.tool_description)
        if self.tool_telemetry is not None:
            result["toolTelemetry"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.tool_telemetry)
        if self.turn_id is not None:
            result["turnId"] = from_union([from_none, from_str], self.turn_id)
        return result


@dataclass
class ToolExecutionCompleteError:
    "Error details when the tool execution failed"
    message: str
    code: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteError":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        code = from_union([from_none, from_str], obj.get("code"))
        return ToolExecutionCompleteError(
            message=message,
            code=code,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.code is not None:
            result["code"] = from_union([from_none, from_str], self.code)
        return result


@dataclass
class ToolExecutionCompleteResult:
    "Tool execution result on success"
    content: str
    # Experimental: this field is part of an experimental API and may change or be removed.
    binary_results_for_llm: list[PersistedBinaryResult] | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    citable_sources: list[CitableSource] | None = None
    contents: list[ToolExecutionCompleteContent] | None = None
    detailed_content: str | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    mcp_meta: Any = None
    structured_content: Any = None
    ui_resource: ToolExecutionCompleteUIResource | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteResult":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        binary_results_for_llm = from_union([from_none, lambda x: from_list(lambda x: from_union([PersistedBinaryImage.from_dict, OmittedBinaryResult.from_dict, BinaryAssetReference.from_dict], x), x)], obj.get("binaryResultsForLlm"))
        citable_sources = from_union([from_none, lambda x: from_list(CitableSource.from_dict, x)], obj.get("citableSources"))
        contents = from_union([from_none, lambda x: from_list(_load_ToolExecutionCompleteContent, x)], obj.get("contents"))
        detailed_content = from_union([from_none, from_str], obj.get("detailedContent"))
        mcp_meta = obj.get("mcpMeta")
        structured_content = obj.get("structuredContent")
        ui_resource = from_union([from_none, ToolExecutionCompleteUIResource.from_dict], obj.get("uiResource"))
        return ToolExecutionCompleteResult(
            content=content,
            binary_results_for_llm=binary_results_for_llm,
            citable_sources=citable_sources,
            contents=contents,
            detailed_content=detailed_content,
            mcp_meta=mcp_meta,
            structured_content=structured_content,
            ui_resource=ui_resource,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.binary_results_for_llm is not None:
            result["binaryResultsForLlm"] = from_union([from_none, lambda x: from_list(lambda x: from_union([lambda x: to_class(PersistedBinaryImage, x), lambda x: to_class(OmittedBinaryResult, x), lambda x: to_class(BinaryAssetReference, x)], x), x)], self.binary_results_for_llm)
        if self.citable_sources is not None:
            result["citableSources"] = from_union([from_none, lambda x: from_list(lambda x: to_class(CitableSource, x), x)], self.citable_sources)
        if self.contents is not None:
            result["contents"] = from_union([from_none, lambda x: from_list(lambda x: x.to_dict(), x)], self.contents)
        if self.detailed_content is not None:
            result["detailedContent"] = from_union([from_none, from_str], self.detailed_content)
        if self.mcp_meta is not None:
            result["mcpMeta"] = self.mcp_meta
        if self.structured_content is not None:
            result["structuredContent"] = self.structured_content
        if self.ui_resource is not None:
            result["uiResource"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResource, x)], self.ui_resource)
        return result


@dataclass
class ToolExecutionCompleteToolDescription:
    "Tool definition metadata, present for MCP tools with MCP Apps support"
    name: str
    _meta: ToolExecutionCompleteToolDescriptionMeta | None = None
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteToolDescription":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        _meta = from_union([from_none, ToolExecutionCompleteToolDescriptionMeta.from_dict], obj.get("_meta"))
        description = from_union([from_none, from_str], obj.get("description"))
        return ToolExecutionCompleteToolDescription(
            name=name,
            _meta=_meta,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        if self._meta is not None:
            result["_meta"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteToolDescriptionMeta, x)], self._meta)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        return result


@dataclass
class ToolExecutionCompleteToolDescriptionMeta:
    "MCP Apps metadata for UI resource association"
    ui: ToolExecutionCompleteToolDescriptionMetaUI | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteToolDescriptionMeta":
        assert isinstance(obj, dict)
        ui = from_union([from_none, ToolExecutionCompleteToolDescriptionMetaUI.from_dict], obj.get("ui"))
        return ToolExecutionCompleteToolDescriptionMeta(
            ui=ui,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.ui is not None:
            result["ui"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteToolDescriptionMetaUI, x)], self.ui)
        return result


@dataclass
class ToolExecutionCompleteToolDescriptionMetaUI:
    "MCP Apps tool `_meta.ui` resource URI and visibility captured on `tool.execution_complete`."
    resource_uri: str | None = None
    visibility: list[ToolExecutionCompleteToolDescriptionMetaUIVisibility] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteToolDescriptionMetaUI":
        assert isinstance(obj, dict)
        resource_uri = from_union([from_none, from_str], obj.get("resourceUri"))
        visibility = from_union([from_none, lambda x: from_list(lambda x: parse_enum(ToolExecutionCompleteToolDescriptionMetaUIVisibility, x), x)], obj.get("visibility"))
        return ToolExecutionCompleteToolDescriptionMetaUI(
            resource_uri=resource_uri,
            visibility=visibility,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.resource_uri is not None:
            result["resourceUri"] = from_union([from_none, from_str], self.resource_uri)
        if self.visibility is not None:
            result["visibility"] = from_union([from_none, lambda x: from_list(lambda x: to_enum(ToolExecutionCompleteToolDescriptionMetaUIVisibility, x), x)], self.visibility)
        return result


@dataclass
class ToolExecutionCompleteUIResource:
    "MCP Apps UI resource content for rendering in a sandboxed iframe"
    mime_type: str
    uri: str
    _meta: ToolExecutionCompleteUIResourceMeta | None = None
    blob: str | None = None
    text: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResource":
        assert isinstance(obj, dict)
        mime_type = from_str(obj.get("mimeType"))
        uri = from_str(obj.get("uri"))
        _meta = from_union([from_none, ToolExecutionCompleteUIResourceMeta.from_dict], obj.get("_meta"))
        blob = from_union([from_none, from_str], obj.get("blob"))
        text = from_union([from_none, from_str], obj.get("text"))
        return ToolExecutionCompleteUIResource(
            mime_type=mime_type,
            uri=uri,
            _meta=_meta,
            blob=blob,
            text=text,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["mimeType"] = from_str(self.mime_type)
        result["uri"] = from_str(self.uri)
        if self._meta is not None:
            result["_meta"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMeta, x)], self._meta)
        if self.blob is not None:
            result["blob"] = from_union([from_none, from_str], self.blob)
        if self.text is not None:
            result["text"] = from_union([from_none, from_str], self.text)
        return result


@dataclass
class ToolExecutionCompleteUIResourceMeta:
    "Resource-level UI metadata (CSP, permissions, visual preferences)"
    ui: ToolExecutionCompleteUIResourceMetaUI | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMeta":
        assert isinstance(obj, dict)
        ui = from_union([from_none, ToolExecutionCompleteUIResourceMetaUI.from_dict], obj.get("ui"))
        return ToolExecutionCompleteUIResourceMeta(
            ui=ui,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.ui is not None:
            result["ui"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUI, x)], self.ui)
        return result


@dataclass
class ToolExecutionCompleteUIResourceMetaUI:
    "MCP Apps UI resource metadata for a completed tool result, including CSP, permissions, domain, and border preference."
    csp: ToolExecutionCompleteUIResourceMetaUICsp | None = None
    domain: str | None = None
    permissions: ToolExecutionCompleteUIResourceMetaUIPermissions | None = None
    prefers_border: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUI":
        assert isinstance(obj, dict)
        csp = from_union([from_none, ToolExecutionCompleteUIResourceMetaUICsp.from_dict], obj.get("csp"))
        domain = from_union([from_none, from_str], obj.get("domain"))
        permissions = from_union([from_none, ToolExecutionCompleteUIResourceMetaUIPermissions.from_dict], obj.get("permissions"))
        prefers_border = from_union([from_none, from_bool], obj.get("prefersBorder"))
        return ToolExecutionCompleteUIResourceMetaUI(
            csp=csp,
            domain=domain,
            permissions=permissions,
            prefers_border=prefers_border,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.csp is not None:
            result["csp"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUICsp, x)], self.csp)
        if self.domain is not None:
            result["domain"] = from_union([from_none, from_str], self.domain)
        if self.permissions is not None:
            result["permissions"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUIPermissions, x)], self.permissions)
        if self.prefers_border is not None:
            result["prefersBorder"] = from_union([from_none, from_bool], self.prefers_border)
        return result


@dataclass
class ToolExecutionCompleteUIResourceMetaUICsp:
    "CSP domain allowlists for an MCP Apps UI resource, including connect, resource, frame, and base URI domains."
    base_uri_domains: list[str] | None = None
    connect_domains: list[str] | None = None
    frame_domains: list[str] | None = None
    resource_domains: list[str] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUICsp":
        assert isinstance(obj, dict)
        base_uri_domains = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("baseUriDomains"))
        connect_domains = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("connectDomains"))
        frame_domains = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("frameDomains"))
        resource_domains = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("resourceDomains"))
        return ToolExecutionCompleteUIResourceMetaUICsp(
            base_uri_domains=base_uri_domains,
            connect_domains=connect_domains,
            frame_domains=frame_domains,
            resource_domains=resource_domains,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.base_uri_domains is not None:
            result["baseUriDomains"] = from_union([from_none, lambda x: from_list(from_str, x)], self.base_uri_domains)
        if self.connect_domains is not None:
            result["connectDomains"] = from_union([from_none, lambda x: from_list(from_str, x)], self.connect_domains)
        if self.frame_domains is not None:
            result["frameDomains"] = from_union([from_none, lambda x: from_list(from_str, x)], self.frame_domains)
        if self.resource_domains is not None:
            result["resourceDomains"] = from_union([from_none, lambda x: from_list(from_str, x)], self.resource_domains)
        return result


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissions:
    "Browser permission metadata for an MCP Apps UI resource, including camera, microphone, geolocation, and clipboard-write."
    camera: ToolExecutionCompleteUIResourceMetaUIPermissionsCamera | None = None
    clipboard_write: ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite | None = None
    geolocation: ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation | None = None
    microphone: ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissions":
        assert isinstance(obj, dict)
        camera = from_union([from_none, ToolExecutionCompleteUIResourceMetaUIPermissionsCamera.from_dict], obj.get("camera"))
        clipboard_write = from_union([from_none, ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite.from_dict], obj.get("clipboardWrite"))
        geolocation = from_union([from_none, ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation.from_dict], obj.get("geolocation"))
        microphone = from_union([from_none, ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone.from_dict], obj.get("microphone"))
        return ToolExecutionCompleteUIResourceMetaUIPermissions(
            camera=camera,
            clipboard_write=clipboard_write,
            geolocation=geolocation,
            microphone=microphone,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.camera is not None:
            result["camera"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUIPermissionsCamera, x)], self.camera)
        if self.clipboard_write is not None:
            result["clipboardWrite"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite, x)], self.clipboard_write)
        if self.geolocation is not None:
            result["geolocation"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation, x)], self.geolocation)
        if self.microphone is not None:
            result["microphone"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone, x)], self.microphone)
        return result


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsCamera:
    "Marker object for camera permission on an MCP Apps UI resource."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsCamera":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsCamera()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite:
    "Marker object for clipboard-write permission on an MCP Apps UI resource."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation:
    "Marker object for geolocation permission on an MCP Apps UI resource."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone:
    "Marker object for microphone permission on an MCP Apps UI resource."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionPartialResultData:
    "Streaming tool execution output for incremental result display"
    partial_output: str
    tool_call_id: str

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionPartialResultData":
        assert isinstance(obj, dict)
        partial_output = from_str(obj.get("partialOutput"))
        tool_call_id = from_str(obj.get("toolCallId"))
        return ToolExecutionPartialResultData(
            partial_output=partial_output,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["partialOutput"] = from_str(self.partial_output)
        result["toolCallId"] = from_str(self.tool_call_id)
        return result


@dataclass
class ToolExecutionProgressData:
    "Tool execution progress notification with status message"
    progress_message: str
    tool_call_id: str

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionProgressData":
        assert isinstance(obj, dict)
        progress_message = from_str(obj.get("progressMessage"))
        tool_call_id = from_str(obj.get("toolCallId"))
        return ToolExecutionProgressData(
            progress_message=progress_message,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["progressMessage"] = from_str(self.progress_message)
        result["toolCallId"] = from_str(self.tool_call_id)
        return result


@dataclass
class ToolExecutionStartData:
    "Tool execution startup details including MCP server information when applicable"
    tool_call_id: str
    tool_name: str
    arguments: Any = None
    display_verbatim: bool | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    fusion: FusionAttribution | None = None
    mcp_server_name: str | None = None
    mcp_tool_name: str | None = None
    model: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    rte: bool | None = None
    shell_tool_info: ToolExecutionStartShellToolInfo | None = None
    tool_description: ToolExecutionStartToolDescription | None = None
    turn_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        arguments = obj.get("arguments")
        display_verbatim = from_union([from_none, from_bool], obj.get("displayVerbatim"))
        fusion = from_union([from_none, FusionAttribution.from_dict], obj.get("fusion"))
        mcp_server_name = from_union([from_none, from_str], obj.get("mcpServerName"))
        mcp_tool_name = from_union([from_none, from_str], obj.get("mcpToolName"))
        model = from_union([from_none, from_str], obj.get("model"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        rte = from_union([from_none, from_bool], obj.get("rte"))
        shell_tool_info = from_union([from_none, ToolExecutionStartShellToolInfo.from_dict], obj.get("shellToolInfo"))
        tool_description = from_union([from_none, ToolExecutionStartToolDescription.from_dict], obj.get("toolDescription"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return ToolExecutionStartData(
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
            display_verbatim=display_verbatim,
            fusion=fusion,
            mcp_server_name=mcp_server_name,
            mcp_tool_name=mcp_tool_name,
            model=model,
            parent_tool_call_id=parent_tool_call_id,
            rte=rte,
            shell_tool_info=shell_tool_info,
            tool_description=tool_description,
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["toolName"] = from_str(self.tool_name)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.display_verbatim is not None:
            result["displayVerbatim"] = from_union([from_none, from_bool], self.display_verbatim)
        if self.fusion is not None:
            result["fusion"] = from_union([from_none, lambda x: to_class(FusionAttribution, x)], self.fusion)
        if self.mcp_server_name is not None:
            result["mcpServerName"] = from_union([from_none, from_str], self.mcp_server_name)
        if self.mcp_tool_name is not None:
            result["mcpToolName"] = from_union([from_none, from_str], self.mcp_tool_name)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.rte is not None:
            result["rte"] = from_union([from_none, from_bool], self.rte)
        if self.shell_tool_info is not None:
            result["shellToolInfo"] = from_union([from_none, lambda x: to_class(ToolExecutionStartShellToolInfo, x)], self.shell_tool_info)
        if self.tool_description is not None:
            result["toolDescription"] = from_union([from_none, lambda x: to_class(ToolExecutionStartToolDescription, x)], self.tool_description)
        if self.turn_id is not None:
            result["turnId"] = from_union([from_none, from_str], self.turn_id)
        return result


@dataclass
class ToolExecutionStartShellToolInfo:
    "Shell-aware path hints for a shell tool's command, captured at start time so consumers can snapshot a file's pre-image before the tool runs."
    has_write_file_redirection: bool
    possible_paths: list[str]
    # Experimental: this field is part of an experimental API and may change or be removed.
    display_command: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartShellToolInfo":
        assert isinstance(obj, dict)
        has_write_file_redirection = from_bool(obj.get("hasWriteFileRedirection"))
        possible_paths = from_list(from_str, obj.get("possiblePaths"))
        display_command = from_union([from_none, from_str], obj.get("displayCommand"))
        return ToolExecutionStartShellToolInfo(
            has_write_file_redirection=has_write_file_redirection,
            possible_paths=possible_paths,
            display_command=display_command,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["hasWriteFileRedirection"] = from_bool(self.has_write_file_redirection)
        result["possiblePaths"] = from_list(from_str, self.possible_paths)
        if self.display_command is not None:
            result["displayCommand"] = from_union([from_none, from_str], self.display_command)
        return result


@dataclass
class ToolExecutionStartToolDescription:
    "Tool definition metadata, present for MCP tools with MCP Apps support"
    name: str
    _meta: ToolExecutionStartToolDescriptionMeta | None = None
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartToolDescription":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        _meta = from_union([from_none, ToolExecutionStartToolDescriptionMeta.from_dict], obj.get("_meta"))
        description = from_union([from_none, from_str], obj.get("description"))
        return ToolExecutionStartToolDescription(
            name=name,
            _meta=_meta,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        if self._meta is not None:
            result["_meta"] = from_union([from_none, lambda x: to_class(ToolExecutionStartToolDescriptionMeta, x)], self._meta)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        return result


@dataclass
class ToolExecutionStartToolDescriptionMeta:
    "MCP Apps metadata for UI resource association"
    ui: ToolExecutionStartToolDescriptionMetaUI | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartToolDescriptionMeta":
        assert isinstance(obj, dict)
        ui = from_union([from_none, ToolExecutionStartToolDescriptionMetaUI.from_dict], obj.get("ui"))
        return ToolExecutionStartToolDescriptionMeta(
            ui=ui,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.ui is not None:
            result["ui"] = from_union([from_none, lambda x: to_class(ToolExecutionStartToolDescriptionMetaUI, x)], self.ui)
        return result


@dataclass
class ToolExecutionStartToolDescriptionMetaUI:
    "MCP Apps tool `_meta.ui` resource URI and visibility captured on `tool.execution_start`."
    resource_uri: str | None = None
    visibility: list[ToolExecutionStartToolDescriptionMetaUIVisibility] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartToolDescriptionMetaUI":
        assert isinstance(obj, dict)
        resource_uri = from_union([from_none, from_str], obj.get("resourceUri"))
        visibility = from_union([from_none, lambda x: from_list(lambda x: parse_enum(ToolExecutionStartToolDescriptionMetaUIVisibility, x), x)], obj.get("visibility"))
        return ToolExecutionStartToolDescriptionMetaUI(
            resource_uri=resource_uri,
            visibility=visibility,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.resource_uri is not None:
            result["resourceUri"] = from_union([from_none, from_str], self.resource_uri)
        if self.visibility is not None:
            result["visibility"] = from_union([from_none, lambda x: from_list(lambda x: to_enum(ToolExecutionStartToolDescriptionMetaUIVisibility, x), x)], self.visibility)
        return result


@dataclass
class ToolSearchActivatedData:
    "Persisted generic client-side tool activations restored when a session resumes."
    strategy: str
    tool_names: list[str]

    @staticmethod
    def from_dict(obj: Any) -> "ToolSearchActivatedData":
        assert isinstance(obj, dict)
        strategy = from_str(obj.get("strategy"))
        tool_names = from_list(from_str, obj.get("toolNames"))
        return ToolSearchActivatedData(
            strategy=strategy,
            tool_names=tool_names,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["strategy"] = from_str(self.strategy)
        result["toolNames"] = from_list(from_str, self.tool_names)
        return result


@dataclass
class ToolUserRequestedData:
    "User-initiated tool invocation request with tool name and arguments"
    tool_call_id: str
    tool_name: str
    arguments: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolUserRequestedData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        arguments = obj.get("arguments")
        return ToolUserRequestedData(
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["toolName"] = from_str(self.tool_name)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        return result


@dataclass
class _UsageCheckpointModelCacheState:
    "Internal prompt-cache expiration state for one model"
    cache_expires_at: datetime
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _cache_ttl_seconds: int
    model_id: str

    @staticmethod
    def from_dict(obj: Any) -> "_UsageCheckpointModelCacheState":
        assert isinstance(obj, dict)
        cache_expires_at = from_datetime(obj.get("cacheExpiresAt"))
        _cache_ttl_seconds = from_int(obj.get("cacheTtlSeconds"))
        model_id = from_str(obj.get("modelId"))
        return _UsageCheckpointModelCacheState(
            cache_expires_at=cache_expires_at,
            _cache_ttl_seconds=_cache_ttl_seconds,
            model_id=model_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cacheExpiresAt"] = to_datetime(self.cache_expires_at)
        result["cacheTtlSeconds"] = to_int(self._cache_ttl_seconds)
        result["modelId"] = from_str(self.model_id)
        return result


@dataclass
class UserInputCompletedData:
    "User input request completion with the user's response"
    request_id: str
    answer: str | None = None
    was_freeform: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserInputCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        answer = from_union([from_none, from_str], obj.get("answer"))
        was_freeform = from_union([from_none, from_bool], obj.get("wasFreeform"))
        return UserInputCompletedData(
            request_id=request_id,
            answer=answer,
            was_freeform=was_freeform,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.answer is not None:
            result["answer"] = from_union([from_none, from_str], self.answer)
        if self.was_freeform is not None:
            result["wasFreeform"] = from_union([from_none, from_bool], self.was_freeform)
        return result


@dataclass
class UserInputRequestedData:
    "User input request notification with question and optional predefined choices"
    question: str
    request_id: str
    allow_freeform: bool | None = None
    choices: list[str] | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserInputRequestedData":
        assert isinstance(obj, dict)
        question = from_str(obj.get("question"))
        request_id = from_str(obj.get("requestId"))
        allow_freeform = from_union([from_none, from_bool], obj.get("allowFreeform"))
        choices = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("choices"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return UserInputRequestedData(
            question=question,
            request_id=request_id,
            allow_freeform=allow_freeform,
            choices=choices,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["question"] = from_str(self.question)
        result["requestId"] = from_str(self.request_id)
        if self.allow_freeform is not None:
            result["allowFreeform"] = from_union([from_none, from_bool], self.allow_freeform)
        if self.choices is not None:
            result["choices"] = from_union([from_none, lambda x: from_list(from_str, x)], self.choices)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class UserMessageData:
    "Payload of `user.message` with displayed and model-transformed content, attachments, source/delivery metadata, mode, and telemetry IDs."
    content: str
    agent_mode: UserMessageAgentMode | None = None
    attachments: list[Attachment] | None = None
    delivery: UserMessageDelivery | None = None
    interaction_id: str | None = None
    is_autopilot_continuation: bool | None = None
    native_document_path_fallback_paths: list[str] | None = None
    parent_agent_task_id: str | None = None
    source: str | None = None
    supported_native_document_mime_types: list[str] | None = None
    transformed_content: str | None = None
    turn_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        agent_mode = from_union([from_none, lambda x: parse_enum(UserMessageAgentMode, x)], obj.get("agentMode"))
        attachments = from_union([from_none, lambda x: from_list(_load_Attachment, x)], obj.get("attachments"))
        delivery = from_union([from_none, lambda x: parse_enum(UserMessageDelivery, x)], obj.get("delivery"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        is_autopilot_continuation = from_union([from_none, from_bool], obj.get("isAutopilotContinuation"))
        native_document_path_fallback_paths = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("nativeDocumentPathFallbackPaths"))
        parent_agent_task_id = from_union([from_none, from_str], obj.get("parentAgentTaskId"))
        source = from_union([from_none, from_str], obj.get("source"))
        supported_native_document_mime_types = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("supportedNativeDocumentMimeTypes"))
        transformed_content = from_union([from_none, from_str], obj.get("transformedContent"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return UserMessageData(
            content=content,
            agent_mode=agent_mode,
            attachments=attachments,
            delivery=delivery,
            interaction_id=interaction_id,
            is_autopilot_continuation=is_autopilot_continuation,
            native_document_path_fallback_paths=native_document_path_fallback_paths,
            parent_agent_task_id=parent_agent_task_id,
            source=source,
            supported_native_document_mime_types=supported_native_document_mime_types,
            transformed_content=transformed_content,
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.agent_mode is not None:
            result["agentMode"] = from_union([from_none, lambda x: to_enum(UserMessageAgentMode, x)], self.agent_mode)
        if self.attachments is not None:
            result["attachments"] = from_union([from_none, lambda x: from_list(lambda x: x.to_dict(), x)], self.attachments)
        if self.delivery is not None:
            result["delivery"] = from_union([from_none, lambda x: to_enum(UserMessageDelivery, x)], self.delivery)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.is_autopilot_continuation is not None:
            result["isAutopilotContinuation"] = from_union([from_none, from_bool], self.is_autopilot_continuation)
        if self.native_document_path_fallback_paths is not None:
            result["nativeDocumentPathFallbackPaths"] = from_union([from_none, lambda x: from_list(from_str, x)], self.native_document_path_fallback_paths)
        if self.parent_agent_task_id is not None:
            result["parentAgentTaskId"] = from_union([from_none, from_str], self.parent_agent_task_id)
        if self.source is not None:
            result["source"] = from_union([from_none, from_str], self.source)
        if self.supported_native_document_mime_types is not None:
            result["supportedNativeDocumentMimeTypes"] = from_union([from_none, lambda x: from_list(from_str, x)], self.supported_native_document_mime_types)
        if self.transformed_content is not None:
            result["transformedContent"] = from_union([from_none, from_str], self.transformed_content)
        if self.turn_id is not None:
            result["turnId"] = from_union([from_none, from_str], self.turn_id)
        return result


@dataclass
class UserToolSessionApprovalCommands:
    "Session-scoped tool-approval rule for specific shell command identifiers."
    command_identifiers: list[str]
    kind: ClassVar[str] = "commands"

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalCommands":
        assert isinstance(obj, dict)
        command_identifiers = from_list(from_str, obj.get("commandIdentifiers"))
        return UserToolSessionApprovalCommands(
            command_identifiers=command_identifiers,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["commandIdentifiers"] = from_list(from_str, self.command_identifiers)
        result["kind"] = self.kind
        return result


@dataclass
class UserToolSessionApprovalCustomTool:
    "Session-scoped tool-approval rule for a custom tool, keyed by tool name."
    kind: ClassVar[str] = "custom-tool"
    tool_name: str

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalCustomTool":
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        return UserToolSessionApprovalCustomTool(
            tool_name=tool_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["toolName"] = from_str(self.tool_name)
        return result


@dataclass
class UserToolSessionApprovalExtensionEnvAccess:
    "Session-scoped tool-approval rule for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names."
    environment_variables: list[str]
    extension_name: str
    kind: ClassVar[str] = "extension-env-access"

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalExtensionEnvAccess":
        assert isinstance(obj, dict)
        environment_variables = from_list(from_str, obj.get("environmentVariables"))
        extension_name = from_str(obj.get("extensionName"))
        return UserToolSessionApprovalExtensionEnvAccess(
            environment_variables=environment_variables,
            extension_name=extension_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["environmentVariables"] = from_list(from_str, self.environment_variables)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        return result


@dataclass
class UserToolSessionApprovalExtensionManagement:
    "Session-scoped tool-approval rule for extension-management operations, optionally narrowed by operation."
    kind: ClassVar[str] = "extension-management"
    operation: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalExtensionManagement":
        assert isinstance(obj, dict)
        operation = from_union([from_none, from_str], obj.get("operation"))
        return UserToolSessionApprovalExtensionManagement(
            operation=operation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.operation is not None:
            result["operation"] = from_union([from_none, from_str], self.operation)
        return result


@dataclass
class UserToolSessionApprovalExtensionPermissionAccess:
    "Session-scoped tool-approval rule for an extension's permission-gated capability access, keyed by extension name."
    extension_name: str
    kind: ClassVar[str] = "extension-permission-access"

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalExtensionPermissionAccess":
        assert isinstance(obj, dict)
        extension_name = from_str(obj.get("extensionName"))
        return UserToolSessionApprovalExtensionPermissionAccess(
            extension_name=extension_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        return result


@dataclass
class UserToolSessionApprovalFactory:
    "Session-scoped factory approval, optionally narrowed by approval key."
    kind: ClassVar[str] = "factory"
    approval_key: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalFactory":
        assert isinstance(obj, dict)
        approval_key = from_union([from_none, from_str], obj.get("approvalKey"))
        return UserToolSessionApprovalFactory(
            approval_key=approval_key,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        if self.approval_key is not None:
            result["approvalKey"] = from_union([from_none, from_str], self.approval_key)
        return result


@dataclass
class UserToolSessionApprovalMcp:
    "Session-scoped tool-approval rule for an MCP server tool, or all tools on the server when `toolName` is null."
    kind: ClassVar[str] = "mcp"
    server_name: str
    tool_name: str | None

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalMcp":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        return UserToolSessionApprovalMcp(
            server_name=server_name,
            tool_name=tool_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_union([from_none, from_str], self.tool_name)
        return result


@dataclass
class UserToolSessionApprovalMemory:
    "Session-scoped tool-approval rule for writes to long-term memory."
    kind: ClassVar[str] = "memory"

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalMemory":
        assert isinstance(obj, dict)
        return UserToolSessionApprovalMemory(
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result


@dataclass
class UserToolSessionApprovalRead:
    "Session-scoped tool-approval rule for read-only filesystem operations."
    kind: ClassVar[str] = "read"

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalRead":
        assert isinstance(obj, dict)
        return UserToolSessionApprovalRead(
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result


@dataclass
class UserToolSessionApprovalWrite:
    "Session-scoped tool-approval rule for filesystem write operations."
    kind: ClassVar[str] = "write"

    @staticmethod
    def from_dict(obj: Any) -> "UserToolSessionApprovalWrite":
        assert isinstance(obj, dict)
        return UserToolSessionApprovalWrite(
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result


@dataclass
class WorkingDirectoryContext:
    "Working directory and git context at session start"
    cwd: str
    base_commit: str | None = None
    branch: str | None = None
    git_root: str | None = None
    head_commit: str | None = None
    host_type: WorkingDirectoryContextHostType | None = None
    pending_git_context: bool | None = None
    repository: str | None = None
    repository_host: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "WorkingDirectoryContext":
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        base_commit = from_union([from_none, from_str], obj.get("baseCommit"))
        branch = from_union([from_none, from_str], obj.get("branch"))
        git_root = from_union([from_none, from_str], obj.get("gitRoot"))
        head_commit = from_union([from_none, from_str], obj.get("headCommit"))
        host_type = from_union([from_none, lambda x: parse_enum(WorkingDirectoryContextHostType, x)], obj.get("hostType"))
        pending_git_context = from_union([from_none, from_bool], obj.get("pendingGitContext"))
        repository = from_union([from_none, from_str], obj.get("repository"))
        repository_host = from_union([from_none, from_str], obj.get("repositoryHost"))
        return WorkingDirectoryContext(
            cwd=cwd,
            base_commit=base_commit,
            branch=branch,
            git_root=git_root,
            head_commit=head_commit,
            host_type=host_type,
            pending_git_context=pending_git_context,
            repository=repository,
            repository_host=repository_host,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_none, from_str], self.base_commit)
        if self.branch is not None:
            result["branch"] = from_union([from_none, from_str], self.branch)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_none, from_str], self.git_root)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_none, from_str], self.head_commit)
        if self.host_type is not None:
            result["hostType"] = from_union([from_none, lambda x: to_enum(WorkingDirectoryContextHostType, x)], self.host_type)
        if self.pending_git_context is not None:
            result["pendingGitContext"] = from_union([from_none, from_bool], self.pending_git_context)
        if self.repository is not None:
            result["repository"] = from_union([from_none, from_str], self.repository)
        if self.repository_host is not None:
            result["repositoryHost"] = from_union([from_none, from_str], self.repository_host)
        return result


def _load_Attachment(obj: Any) -> "Attachment":
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "file": return AttachmentFile.from_dict(obj)
        case "directory": return AttachmentDirectory.from_dict(obj)
        case "selection": return AttachmentSelection.from_dict(obj)
        case "github_reference": return AttachmentGitHubReference.from_dict(obj)
        case "github_commit": return AttachmentGitHubCommit.from_dict(obj)
        case "github_release": return AttachmentGitHubRelease.from_dict(obj)
        case "github_actions_job": return AttachmentGitHubActionsJob.from_dict(obj)
        case "github_repository": return AttachmentGitHubRepository.from_dict(obj)
        case "github_file_diff": return AttachmentGitHubFileDiff.from_dict(obj)
        case "github_tree_comparison": return AttachmentGitHubTreeComparison.from_dict(obj)
        case "github_url": return AttachmentGitHubUrl.from_dict(obj)
        case "github_file": return AttachmentGitHubFile.from_dict(obj)
        case "github_snippet": return AttachmentGitHubSnippet.from_dict(obj)
        case "blob": return AttachmentBlob.from_dict(obj)
        case "extension_context": return AttachmentExtensionContext.from_dict(obj)
        case _: raise ValueError(f"Unknown Attachment type: {kind!r}")


def _load_CitationLocation(obj: Any) -> "CitationLocation":
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "char": return CitationLocationChar.from_dict(obj)
        case "page": return CitationLocationPage.from_dict(obj)
        case "block": return CitationLocationBlock.from_dict(obj)
        case _: raise ValueError(f"Unknown CitationLocation type: {kind!r}")


def _load_PermissionPromptRequest(obj: Any) -> "PermissionPromptRequest":
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "commands": return PermissionPromptRequestCommands.from_dict(obj)
        case "write": return PermissionPromptRequestWrite.from_dict(obj)
        case "read": return PermissionPromptRequestRead.from_dict(obj)
        case "mcp": return PermissionPromptRequestMcp.from_dict(obj)
        case "url": return PermissionPromptRequestUrl.from_dict(obj)
        case "memory": return PermissionPromptRequestMemory.from_dict(obj)
        case "custom-tool": return PermissionPromptRequestCustomTool.from_dict(obj)
        case "path": return PermissionPromptRequestPath.from_dict(obj)
        case "hook": return PermissionPromptRequestHook.from_dict(obj)
        case "extension-management": return PermissionPromptRequestExtensionManagement.from_dict(obj)
        case "factory": return PermissionPromptRequestFactory.from_dict(obj)
        case "extension-permission-access": return PermissionPromptRequestExtensionPermissionAccess.from_dict(obj)
        case "extension-env-access": return PermissionPromptRequestExtensionEnvAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionPromptRequest kind: {kind!r}")


def _load_PermissionRequest(obj: Any) -> "PermissionRequest":
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "shell": return PermissionRequestShell.from_dict(obj)
        case "write": return PermissionRequestWrite.from_dict(obj)
        case "read": return PermissionRequestRead.from_dict(obj)
        case "mcp": return PermissionRequestMcp.from_dict(obj)
        case "url": return PermissionRequestUrl.from_dict(obj)
        case "memory": return PermissionRequestMemory.from_dict(obj)
        case "custom-tool": return PermissionRequestCustomTool.from_dict(obj)
        case "hook": return PermissionRequestHook.from_dict(obj)
        case "extension-management": return PermissionRequestExtensionManagement.from_dict(obj)
        case "factory": return PermissionRequestFactory.from_dict(obj)
        case "extension-permission-access": return PermissionRequestExtensionPermissionAccess.from_dict(obj)
        case "extension-env-access": return PermissionRequestExtensionEnvAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionRequest kind: {kind!r}")


def _load_PermissionResult(obj: Any) -> "PermissionResult":
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "approved": return PermissionApproved.from_dict(obj)
        case "approved-for-session": return PermissionApprovedForSession.from_dict(obj)
        case "approved-for-location": return PermissionApprovedForLocation.from_dict(obj)
        case "cancelled": return PermissionCancelled.from_dict(obj)
        case "denied-by-rules": return PermissionDeniedByRules.from_dict(obj)
        case "denied-no-approval-rule-and-could-not-request-from-user": return PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser.from_dict(obj)
        case "denied-interactively-by-user": return PermissionDeniedInteractivelyByUser.from_dict(obj)
        case "denied-by-content-exclusion-policy": return PermissionDeniedByContentExclusionPolicy.from_dict(obj)
        case "denied-by-permission-request-hook": return PermissionDeniedByPermissionRequestHook.from_dict(obj)
        case _: raise ValueError(f"Unknown PermissionResult kind: {kind!r}")


def _load_SystemNotification(obj: Any) -> "SystemNotification":
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "agent_completed": return SystemNotificationAgentCompleted.from_dict(obj)
        case "agent_idle": return SystemNotificationAgentIdle.from_dict(obj)
        case "new_inbox_message": return SystemNotificationNewInboxMessage.from_dict(obj)
        case "shell_completed": return SystemNotificationShellCompleted.from_dict(obj)
        case "shell_detached_completed": return SystemNotificationShellDetachedCompleted.from_dict(obj)
        case "instruction_discovered": return SystemNotificationInstructionDiscovered.from_dict(obj)
        case "factory_completed": return SystemNotificationFactoryCompleted.from_dict(obj)
        case "unclassified": return SystemNotificationUnclassified.from_dict(obj)
        case _: raise ValueError(f"Unknown SystemNotification type: {kind!r}")


def _load_ToolExecutionCompleteContent(obj: Any) -> "ToolExecutionCompleteContent":
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "text": return ToolExecutionCompleteContentText.from_dict(obj)
        case "terminal": return ToolExecutionCompleteContentTerminal.from_dict(obj)
        case "shell_exit": return ToolExecutionCompleteContentShellExit.from_dict(obj)
        case "image": return ToolExecutionCompleteContentImage.from_dict(obj)
        case "audio": return ToolExecutionCompleteContentAudio.from_dict(obj)
        case "resource_link": return ToolExecutionCompleteContentResourceLink.from_dict(obj)
        case "resource": return ToolExecutionCompleteContentResource.from_dict(obj)
        case _: raise ValueError(f"Unknown ToolExecutionCompleteContent type: {kind!r}")


def _load_UserToolSessionApproval(obj: Any) -> "UserToolSessionApproval":
    assert isinstance(obj, dict)
    kind = obj.get("kind")
    match kind:
        case "commands": return UserToolSessionApprovalCommands.from_dict(obj)
        case "read": return UserToolSessionApprovalRead.from_dict(obj)
        case "write": return UserToolSessionApprovalWrite.from_dict(obj)
        case "mcp": return UserToolSessionApprovalMcp.from_dict(obj)
        case "memory": return UserToolSessionApprovalMemory.from_dict(obj)
        case "custom-tool": return UserToolSessionApprovalCustomTool.from_dict(obj)
        case "extension-management": return UserToolSessionApprovalExtensionManagement.from_dict(obj)
        case "factory": return UserToolSessionApprovalFactory.from_dict(obj)
        case "extension-permission-access": return UserToolSessionApprovalExtensionPermissionAccess.from_dict(obj)
        case "extension-env-access": return UserToolSessionApprovalExtensionEnvAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown UserToolSessionApproval kind: {kind!r}")


# A content block within a tool result, which may be text, terminal output, image, audio, or a resource
ToolExecutionCompleteContent = ToolExecutionCompleteContentText | ToolExecutionCompleteContentTerminal | ToolExecutionCompleteContentShellExit | ToolExecutionCompleteContentImage | ToolExecutionCompleteContentAudio | ToolExecutionCompleteContentResourceLink | ToolExecutionCompleteContentResource


# A model-facing binary result as persisted: full inline data, a size-omitted marker, or a deduplicated asset reference
PersistedBinaryResult = PersistedBinaryImage | OmittedBinaryResult | BinaryAssetReference


# A user message attachment — a file, directory, code selection, blob, GitHub reference, GitHub-anchored pointer, or extension-supplied context payload
Attachment = AttachmentFile | AttachmentDirectory | AttachmentSelection | AttachmentGitHubReference | AttachmentGitHubCommit | AttachmentGitHubRelease | AttachmentGitHubActionsJob | AttachmentGitHubRepository | AttachmentGitHubFileDiff | AttachmentGitHubTreeComparison | AttachmentGitHubUrl | AttachmentGitHubFile | AttachmentGitHubSnippet | AttachmentBlob | AttachmentExtensionContext


# Derived user-facing permission prompt details for UI consumers
PermissionPromptRequest = PermissionPromptRequestCommands | PermissionPromptRequestWrite | PermissionPromptRequestRead | PermissionPromptRequestMcp | PermissionPromptRequestUrl | PermissionPromptRequestMemory | PermissionPromptRequestCustomTool | PermissionPromptRequestPath | PermissionPromptRequestHook | PermissionPromptRequestExtensionManagement | PermissionPromptRequestFactory | PermissionPromptRequestExtensionPermissionAccess | PermissionPromptRequestExtensionEnvAccess


# Details of the permission being requested
PermissionRequest = PermissionRequestShell | PermissionRequestWrite | PermissionRequestRead | PermissionRequestMcp | PermissionRequestUrl | PermissionRequestMemory | PermissionRequestCustomTool | PermissionRequestHook | PermissionRequestExtensionManagement | PermissionRequestFactory | PermissionRequestExtensionPermissionAccess | PermissionRequestExtensionEnvAccess


# Location within a cited source (character, page, or content-block range) that supports a span.
CitationLocation = CitationLocationChar | CitationLocationPage | CitationLocationBlock


# Structured metadata identifying what triggered this notification
SystemNotification = SystemNotificationAgentCompleted | SystemNotificationAgentIdle | SystemNotificationNewInboxMessage | SystemNotificationShellCompleted | SystemNotificationShellDetachedCompleted | SystemNotificationInstructionDiscovered | SystemNotificationFactoryCompleted | SystemNotificationUnclassified


# The approval to add as a session-scoped rule
UserToolSessionApproval = UserToolSessionApprovalCommands | UserToolSessionApprovalRead | UserToolSessionApprovalWrite | UserToolSessionApprovalMcp | UserToolSessionApprovalMemory | UserToolSessionApprovalCustomTool | UserToolSessionApprovalExtensionManagement | UserToolSessionApprovalFactory | UserToolSessionApprovalExtensionPermissionAccess | UserToolSessionApprovalExtensionEnvAccess


# The embedded resource contents, either text or base64-encoded binary
ToolExecutionCompleteContentResourceDetails = EmbeddedTextResourceContents | EmbeddedBlobResourceContents


# The result of the permission request
PermissionResult = PermissionApproved | PermissionApprovedForSession | PermissionApprovedForLocation | PermissionCancelled | PermissionDeniedByRules | PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser | PermissionDeniedInteractivelyByUser | PermissionDeniedByContentExclusionPolicy | PermissionDeniedByPermissionRequestHook


# Experimental: this enum is part of an experimental API and may change or be removed.
class AssistedApprovalJudgeFailureReason(Enum):
    "Why the assisted-approval judge produced no usable recommendation. Present only alongside an `error` recommendation, where the human-readable reason is a fixed string and therefore cannot distinguish these cases. Intended to make a judge failure reportable by a consumer that has no access to the host's logs."
    # The judge model call exceeded its deadline.
    TIMEOUT = "timeout"
    # The judge model call was cancelled before it returned.
    ABORT = "abort"
    # The judge model call completed but returned no content.
    EMPTY_RESPONSE = "empty_response"
    # The judge model call failed (for example a transport, authentication, or rate-limit error).
    MODEL_ERROR = "model_error"
    # The judge model replied, but the reply carried no ALLOW/DENY verdict.
    PARSE_ERROR = "parse_error"


# Experimental: this enum is part of an experimental API and may change or be removed.
class AssistedApprovalRecommendation(Enum):
    "Outcome of the assisted-approval safety judge for a permission request. Present only in assisted mode; its absence means the judge did not evaluate the request."
    # The judge evaluated the request and recommends automatically approving it.
    APPROVE = "approve"
    # The judge evaluated the request and does not recommend automatically approving it; explicit approval is required. Whether that means prompting, denying, or something else is the consumer's decision.
    REQUIRE_APPROVAL = "requireApproval"
    # Assisted mode is enabled, but this request category is never automatically approvable (for example, sandbox-bypass requests), so the judge was not consulted.
    EXCLUDED = "excluded"
    # The judge was consulted but did not return a usable recommendation, so the request requires explicit approval.
    ERROR = "error"


# Experimental: this enum is part of an experimental API and may change or be removed.
class CitationProvider(Enum):
    "The system that produced a citation."
    # Citation produced by an Anthropic (Claude) model response.
    ANTHROPIC = "anthropic"
    # Citation produced by an OpenAI model response.
    OPENAI = "openai"
    # Citation synthesized client-side by the runtime from tool output.
    CLIENT = "client"


# Experimental: this enum is part of an experimental API and may change or be removed.
class FusionConversationScope(Enum):
    "Conversation scope in which a HydraFusion phase executes."
    # Canonical root conversation history.
    ROOT = "root"
    # Isolated read-only review history that does not enter the root conversation.
    REVIEW = "review"


# Experimental: this enum is part of an experimental API and may change or be removed.
class FusionFollowUpAction(Enum):
    "Server-recommended routing behavior for a later HydraFusion turn."
    # Reuse the durable primary model without routing.
    REUSE_PRIMARY = "reuse_primary"
    # Request a new routing decision.
    REROUTE = "reroute"


# Experimental: this enum is part of an experimental API and may change or be removed.
class FusionPattern(Enum):
    "Validated HydraFusion execution pattern."
    # Run one primary solver phase.
    SINGLE = "single"
    # Run a primary phase, a judge, and an optional repair.
    CASCADE = "cascade"
    # Run a primary draft, a read-only critique, and a revision.
    CRITIQUE = "critique"


# Experimental: this enum is part of an experimental API and may change or be removed.
class FusionPhaseKind(Enum):
    "HydraFusion phase kind."
    # Primary solver phase.
    PRIMARY = "primary"
    # Read-only cascade judge phase.
    JUDGE = "judge"
    # Cascade repair phase.
    REPAIR = "repair"
    # Initial critique-pattern draft phase.
    DRAFT = "draft"
    # Read-only critique phase.
    CRITIC = "critic"
    # Critique-pattern revision phase.
    REVISION = "revision"
    # Follow-up phase continuing from the resolved model.
    FOLLOW_UP = "follow_up"


# Experimental: this enum is part of an experimental API and may change or be removed.
class FusionPhaseStatus(Enum):
    "Durable outcome status of a HydraFusion phase."
    # The phase completed successfully.
    SUCCEEDED = "succeeded"
    # The phase failed.
    FAILED = "failed"
    # The phase was cancelled.
    CANCELLED = "cancelled"


# Experimental: this enum is part of an experimental API and may change or be removed.
class _FusionProjectionMode(Enum):
    "How a durable phase checkpoint contributes its exact message to canonical root history."
    # Append the exact root message immediately.
    APPEND = "append"
    # Hold a terminal message outside canonical history until the final commit selects it.
    STAGED = "staged"
    # Do not project the checkpoint into root history.
    NONE = "none"


# Experimental: this enum is part of an experimental API and may change or be removed.
class FusionTurnKind(Enum):
    "Kind of turn for which HydraFusion routing is running."
    # A user-message turn.
    USER = "user"
    # A conversation-compaction turn.
    COMPACTION = "compaction"


# Experimental: this enum is part of an experimental API and may change or be removed.
class PermissionMode(Enum):
    "Permission mode for the session."
    # Permission requests follow the normal approval flow.
    MANUAL = "manual"
    # Permission requests include an LLM safety recommendation; clients may automatically approve requests judged acceptable.
    ASSISTED = "assisted"
    # Tool, path, and URL permission requests are automatically approved.
    ALLOW_ALL = "allow-all"


# Experimental: this enum is part of an experimental API and may change or be removed.
class PermissionRecommendation(Enum):
    "Advisory recommendation the runtime attaches to a permission request whose origin it can vouch for by construction. Unlike the auto-approval judge this does not depend on auto mode and does not evaluate what the tool call does; its absence simply means the runtime has no opinion and the request follows the host's normal approval flow."
    # The runtime vouches for the request's origin and recommends approving it without prompting. The host still owns the decision and may deny it; deny rules, managed policy, and the auto-approval safety judge all outrank this recommendation.
    APPROVE = "approve"


# Experimental: this enum is part of an experimental API and may change or be removed.
class UIEphemeralQueryPhase(Enum):
    "Lifecycle phase for a Rust-owned ephemeral query stream."
    # The ephemeral query stream has begun.
    STARTED = "started"
    # A partial result chunk was produced by the stream.
    CHUNK = "chunk"
    # The ephemeral query stream finished successfully.
    COMPLETED = "completed"
    # The ephemeral query stream ended with an error.
    FAILED = "failed"
    # The ephemeral query stream was cancelled before completing.
    ABORTED = "aborted"


class AbortReason(Enum):
    "Finite reason code describing why the current turn was aborted"
    # The local user requested the abort, for example by pressing Ctrl+C in the CLI.
    USER_INITIATED = "user_initiated"
    # A remote command requested the abort.
    REMOTE_COMMAND = "remote_command"
    # An MCP server delivered a user.abort notification.
    USER_ABORT = "user_abort"
    # Autopilot stopped the run because the active objective reached its user-set --max-ai-credits limit.
    AUTOPILOT_CREDIT_LIMIT = "autopilot_credit_limit"


class AgentInterruptedActivity(Enum):
    "What the agent was doing when the user interrupted it."
    # A request to the model was open.
    MODEL_CALL = "model_call"
    # The turn was sleeping between retry attempts.
    RETRY_BACKOFF = "retry_backoff"
    # One or more tools were executing.
    TOOL_CALL = "tool_call"
    # Background sub-agents were running while the main loop was idle.
    BACKGROUND_AGENT = "background_agent"


class AgentInterruptedCancelPhase(Enum):
    "Where the interruption landed relative to the first streamed token."
    # No output had been produced when the request was cancelled.
    PRE_FIRST_TOKEN = "pre_first_token"
    # The response was already streaming when the request was cancelled.
    MID_STREAM = "mid_stream"


class AssistantMessageToolRequestCallerType(Enum):
    "Hosted program caller type"
    PROGRAM = "program"


class AssistantMessageToolRequestType(Enum):
    "Tool call type: \"function\" for standard tool calls, \"custom\" for grammar-based tool calls. Defaults to \"function\" when absent."
    # Standard function-style tool call.
    FUNCTION = "function"
    # Custom grammar-based tool call.
    CUSTOM = "custom"


class AssistantUsageApiEndpoint(Enum):
    "API endpoint used for this model call, matching CAPI supported_endpoints vocabulary"
    # Chat Completions API endpoint.
    CHAT_COMPLETIONS = "/chat/completions"
    # Anthropic Messages API endpoint.
    V1_MESSAGES = "/v1/messages"
    # Responses API endpoint.
    RESPONSES = "/responses"
    # WebSocket Responses API endpoint.
    WS_RESPONSES = "ws:/responses"


class AssistantUsageTransport(Enum):
    "Transport used for a successful model call"
    # HTTP transport, including SSE streams.
    HTTP = "http"
    # WebSocket transport.
    WEBSOCKET = "websocket"


class AttachmentGitHubReferenceType(Enum):
    "Type of GitHub reference"
    # GitHub issue reference.
    ISSUE = "issue"
    # GitHub pull request reference.
    PR = "pr"
    # GitHub discussion reference.
    DISCUSSION = "discussion"


class AutoModeResolvedReasoningBucket(Enum):
    "Coarse request-difficulty bucket for UX explainability"
    # The request looks low-reasoning; a lighter model is appropriate.
    LOW = "low"
    # The request needs a moderate amount of reasoning.
    MEDIUM = "medium"
    # The request looks high-reasoning; a stronger model is appropriate.
    HIGH = "high"


class AutoModeSwitchResponse(Enum):
    "The user's auto-mode-switch choice"
    # Switch models for this request.
    YES = "yes"
    # Switch models now and keep using the replacement automatically.
    YES_ALWAYS = "yes_always"
    # Do not switch models.
    NO = "no"


class AutoTier(Enum):
    "Routing preference used when the session model is `auto`."
    # Optimize for efficiency.
    EFFICIENCY = "efficiency"
    # Balance efficiency and intelligence.
    BALANCE = "balance"
    # Optimize for intelligence.
    INTELLIGENCE = "intelligence"


class AutopilotObjectiveChangedOperation(Enum):
    "The type of operation performed on the autopilot objective state file"
    # Autopilot objective state file was created for a new objective.
    CREATE = "create"
    # Autopilot objective state file was updated for an existing objective.
    UPDATE = "update"
    # Autopilot objective state file was deleted or cleared.
    DELETE = "delete"


class AutopilotObjectiveChangedStatus(Enum):
    "Current autopilot objective status, if one exists"
    # Objective is active and can drive autopilot continuations.
    ACTIVE = "active"
    # Objective is paused and will not drive autopilot continuations.
    PAUSED = "paused"
    # Legacy objective state indicating the previous continuation cap was reached.
    CAP_REACHED = "cap_reached"
    # Objective was completed by the agent.
    COMPLETED = "completed"


class BinaryAssetReferenceType(Enum):
    "Binary result type discriminator. Use \"image\" for images and \"resource\" for other binary data."
    # Binary image data.
    IMAGE = "image"
    # Other binary resource data.
    RESOURCE = "resource"


class BinaryAssetType(Enum):
    "Binary asset type discriminator. Use \"image\" for images and \"resource\" otherwise."
    # Binary image data.
    IMAGE = "image"
    # Other binary resource data.
    RESOURCE = "resource"


class CompactionTrigger(Enum):
    "What initiated a conversation compaction"
    # Background compaction started automatically because context utilization crossed the background threshold.
    THRESHOLD = "threshold"
    # Compaction forced by a context-limit model response (e.g. HTTP 413) before retrying the request.
    CONTEXT_LIMIT_RETRY = "context_limit_retry"
    # User-requested compaction, e.g. the /compact command or the history.compact API.
    MANUAL = "manual"
    # Emergency compaction triggered by high process memory usage.
    MEMORY_PRESSURE = "memory_pressure"
    # Compaction requested while switching to a model with a smaller context window.
    MODEL_SWITCH = "model_switch"


class ContextTier(Enum):
    "Allowed values for the `ContextTier` enumeration."
    # Default context tier with standard context window size.
    DEFAULT = "default"
    # Extended context tier with a larger context window.
    LONG_CONTEXT = "long_context"


class ElicitationCompletedAction(Enum):
    "The user action: \"accept\" (submitted form), \"decline\" (explicitly refused), or \"cancel\" (dismissed)"
    # The user submitted the requested form.
    ACCEPT = "accept"
    # The user explicitly declined the request.
    DECLINE = "decline"
    # The user dismissed the request.
    CANCEL = "cancel"


class ElicitationRequestedMode(Enum):
    "Elicitation mode; \"form\" for structured input, \"url\" for browser-based. Defaults to \"form\" when absent."
    # Structured form-based elicitation.
    FORM = "form"
    # Browser URL-based elicitation.
    URL = "url"


class ExitPlanModeAction(Enum):
    "Exit plan mode action"
    # Exit plan mode without starting implementation.
    EXIT_ONLY = "exit_only"
    # Exit plan mode and continue in interactive mode.
    INTERACTIVE = "interactive"
    # Exit plan mode and continue autonomously.
    AUTOPILOT = "autopilot"
    # Exit plan mode and continue with parallel autonomous workers.
    AUTOPILOT_FLEET = "autopilot_fleet"


class ExtensionsLoadedExtensionSource(Enum):
    "Discovery source"
    # Extension discovered from the current project.
    PROJECT = "project"
    # Extension discovered from the user's extension directory.
    USER = "user"
    # Extension contributed by an installed plugin.
    PLUGIN = "plugin"
    # Extension discovered from the current session's state directory.
    SESSION = "session"


class ExtensionsLoadedExtensionStatus(Enum):
    "Current status: running, disabled, failed, or starting"
    # The extension process is running.
    RUNNING = "running"
    # The extension is installed but disabled.
    DISABLED = "disabled"
    # The extension failed to start or crashed.
    FAILED = "failed"
    # The extension process is starting.
    STARTING = "starting"


class FactoryPermissionOperation(Enum):
    "Operation gated by a factory permission request."
    # Running a registered factory, which spends subagents, active time, and AI credits under the approved limits.
    RUN = "run"
    # Authoring a factory, which writes JavaScript into a session-scoped extension and loads it.
    AUTHOR = "author"


class FactoryRunSettledStatus(Enum):
    "Terminal status a factory run committed. A settled run is never `pending` or `running`, so those two members of the run-status domain are deliberately absent."
    # The factory body resolved and its result was committed.
    COMPLETED = "completed"
    # The run was stopped by a limit, an approval refusal or another policy decision.
    HALTED = "halted"
    # The run was cancelled by its caller or by session disposal.
    CANCELLED = "cancelled"
    # The run failed, with `failureType` carrying the class when it has one.
    ERROR = "error"


class HandoffSourceType(Enum):
    "Origin type of the session being handed off"
    # The handoff originated from a remote session.
    REMOTE = "remote"
    # The handoff originated from a local session.
    LOCAL = "local"


class ManagedSettingsEnforcedAction(Enum):
    "The category of runtime action that enterprise managed settings governed (blocked or capped)"
    # An attempt to turn on a bypass-permissions ("yolo") escalation was refused or capped because policy disables bypass-permissions mode.
    BYPASS_PERMISSIONS_BLOCKED = "bypass_permissions_blocked"


class ManagedSettingsEnforcedEscalation(Enum):
    "For a `bypass_permissions_blocked` action, which permission-escalation primitive was refused"
    # Full allow-all permissions — automatically approving tools, paths, and URLs.
    ALLOW_ALL = "allow_all"
    # Automatic approval of all tool permission requests.
    APPROVE_ALL = "approve_all"
    # Assisted mode — keeps normal prompt paths and adds an LLM recommendation, distinct from allow-all.
    ASSISTED_APPROVAL = "assisted_approval"
    # Unrestricted filesystem access outside the session's allowed directories.
    UNRESTRICTED_PATHS = "unrestricted_paths"
    # Unrestricted URL fetch access.
    UNRESTRICTED_URLS = "unrestricted_urls"
    # A server-wide MCP "Always Allow" (or `--allow-tool <server>`) blanket that would auto-approve every tool from an MCP server. Capped to per-tool approval; each tool still prompts.
    SERVER_WIDE_MCP_APPROVAL = "server_wide_mcp_approval"


class ManagedSettingsResolvedSource(Enum):
    "Summary of which managed-settings channels contributed to the effective session policy. Use the per-channel booleans for exact provenance."
    # Only the server/account channel contributed.
    SERVER = "server"
    # Only the device MDM/plist/registry/file channel contributed.
    DEVICE = "device"
    # Only session-local SDK-host injection contributed.
    CLIENT = "client"
    # More than one channel contributed. Ordinary keys resolve device over server per key, while permissions compose restrictively across all present layers.
    MIXED = "mixed"
    # No managed policy is in force (no channel contributed).
    NONE = "none"


class McpHeadersRefreshCompletedOutcome(Enum):
    "How the pending MCP headers refresh request resolved."
    # The host supplied dynamic headers.
    HEADERS = "headers"
    # The host responded with no dynamic headers.
    NONE = "none"
    # No response arrived within the bounded window.
    TIMEOUT = "timeout"


class McpHeadersRefreshRequiredReason(Enum):
    "Why dynamic headers are being requested."
    # The transport is making its first dynamic header request for this server.
    STARTUP = "startup"
    # The previously cached dynamic headers expired.
    TTL_EXPIRED = "ttl-expired"
    # The server returned 401 and stale dynamic headers were invalidated.
    AUTH_FAILED = "auth-failed"


class McpOauthCompletionOutcome(Enum):
    "How the pending MCP OAuth request was completed"
    # The request completed with a token-backed OAuth provider.
    TOKEN = "token"
    # The request completed without an OAuth provider.
    CANCELLED = "cancelled"


class McpOauthRequestReason(Enum):
    "Reason the runtime is requesting host-provided MCP OAuth credentials"
    # Initial credentials are required before connecting to the MCP server.
    INITIAL = "initial"
    # The current host-provided credential was rejected and a replacement is requested.
    REFRESH = "refresh"
    # The server requires a new host authorization flow before continuing.
    REAUTH = "reauth"
    # The server requires a credential with additional scope or audience.
    UPSCOPE = "upscope"


class McpServerSource(Enum):
    "Configuration source: user, workspace, plugin, or builtin"
    # Server configured in the user's global MCP configuration.
    USER = "user"
    # Server configured by the current workspace.
    WORKSPACE = "workspace"
    # Server contributed by an installed plugin.
    PLUGIN = "plugin"
    # Server bundled with the runtime.
    BUILTIN = "builtin"


class McpServerStatus(Enum):
    "Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured"
    # The server is connected and available.
    CONNECTED = "connected"
    # The server failed to connect or initialize.
    FAILED = "failed"
    # The server requires authentication before it can connect.
    NEEDS_AUTH = "needs-auth"
    # The server connection is still being established.
    PENDING = "pending"
    # The server is configured but disabled.
    DISABLED = "disabled"
    # The server was intentionally stopped and can be restarted on demand when policy permits; a server quarantined by restrictive managed policy stays stopped and cannot be restarted until the policy allows it.
    STOPPED = "stopped"
    # The server is not configured for this session.
    NOT_CONFIGURED = "not_configured"


class McpServerTransport(Enum):
    "Transport mechanism: stdio, http, sse (deprecated), or memory (in-process MCP server)"
    # Server communicates over stdio with a local child process.
    STDIO = "stdio"
    # Server communicates over streamable HTTP.
    HTTP = "http"
    # Server communicates over Server-Sent Events (deprecated).
    SSE = "sse"
    # Server is backed by an in-memory runtime implementation.
    MEMORY = "memory"


class ModelCallFailureBadRequestKind(Enum):
    "For HTTP 400 failures only: whether the response carried a structured CAPI error envelope (structured_error, a deterministic validation failure) or no error body (bodyless, the transient gateway/proxy signature). Absent for non-400 failures."
    # The 400 response carried no error body (transient gateway/proxy signature).
    BODYLESS = "bodyless"
    # The 400 response carried a structured CAPI error envelope (deterministic validation failure).
    STRUCTURED_ERROR = "structured_error"


class ModelCallFailureKind(Enum):
    "Boundary that produced a model call failure"
    # The provider returned an API error response.
    API = "api"
    # The request transport failed before a usable API response completed.
    TRANSPORT = "transport"


class ModelCallFailureSource(Enum):
    "Where the failed model call originated"
    # Model call from the top-level agent.
    TOP_LEVEL = "top_level"
    # Model call from a sub-agent.
    SUBAGENT = "subagent"
    # Model call from MCP sampling.
    MCP_SAMPLING = "mcp_sampling"


class ModelCallFailureTransport(Enum):
    "Transport used for a failed model call"
    # HTTP transport, including SSE streams.
    HTTP = "http"
    # WebSocket transport.
    WEBSOCKET = "websocket"


class ModelCallFinishedOutcome(Enum):
    "Final outcome of one logical model dispatch after response acceptance processing"
    # The provider response was accepted for continued agent processing.
    SUCCESS = "success"
    # The dispatch ended with a provider or transport error.
    ERROR = "error"
    # The dispatch was cancelled before an accepted response was produced.
    CANCELLED = "cancelled"
    # The provider response was rejected during post-response acceptance processing.
    REJECTED = "rejected"


class ModelChangeSource(Enum):
    "Origin of an effective session model change."
    # The user selected a model directly with `/model <id>`.
    MODEL_COMMAND = "model_command"
    # The user selected the model with `/settings`.
    SETTINGS_COMMAND = "settings_command"
    # The user selected the model with the `/config` alias.
    CONFIG_COMMAND = "config_command"
    # The user selected the model in the model picker, including the picker opened by bare `/model`.
    MODEL_PICKER = "model_picker"
    # Organization-managed settings selected the model.
    MANAGED_SETTINGS = "managed_settings"
    # Repository settings selected the model.
    REPO_SETTINGS = "repo_settings"
    # Startup model resolution selected the model.
    STARTUP = "startup"
    # Selecting an agent selected its configured model.
    AGENT = "agent"
    # Entering, leaving, or reconfiguring plan mode selected the model.
    PLAN_MODE = "plan_mode"
    # The runtime selected the model automatically, such as rate-limit recovery or refusal fallback.
    AUTOMATIC = "automatic"
    # An SDK or RPC caller selected the model.
    SDK = "sdk"


class OmittedBinaryOmittedReason(Enum):
    "Why the binary data is absent: it exceeded the inline size limit, or its asset was unavailable"
    # Bytes exceeded the session's inline size limit.
    TOO_LARGE = "too_large"
    # The referenced binary asset could not be found (e.g. a truncated log).
    ASSET_UNAVAILABLE = "asset_unavailable"


class OmittedBinaryType(Enum):
    "Binary result type discriminator. Use \"image\" for images and \"resource\" for other binary data."
    # Binary image data.
    IMAGE = "image"
    # Other binary resource data.
    RESOURCE = "resource"


class PermissionPromptRequestPathAccessKind(Enum):
    "Underlying permission kind that needs path approval"
    # Read access to a filesystem path.
    READ = "read"
    # Shell command access involving a filesystem path.
    SHELL = "shell"
    # Write access to a filesystem path.
    WRITE = "write"


class PermissionRequestMemoryAction(Enum):
    "Whether this is a store or vote memory operation"
    # Store a new memory.
    STORE = "store"
    # Vote on an existing memory.
    VOTE = "vote"


class PermissionRequestMemoryDirection(Enum):
    "Vote direction (vote only)"
    # Vote that the memory is useful or accurate.
    UPVOTE = "upvote"
    # Vote that the memory is incorrect or outdated.
    DOWNVOTE = "downvote"


class PermissionRequestMemoryScope(Enum):
    "Scope of a stored memory."
    # Store the memory for the current repository.
    REPOSITORY = "repository"
    # Store the memory for the current user.
    USER = "user"


class PersistedBinaryImageType(Enum):
    "Binary result type discriminator. Use \"image\" for images and \"resource\" for other binary data."
    # Binary image data.
    IMAGE = "image"
    # Other binary resource data.
    RESOURCE = "resource"


class PlanChangedOperation(Enum):
    "The type of operation performed on the plan file"
    # The plan file was created.
    CREATE = "create"
    # The plan file was updated.
    UPDATE = "update"
    # The plan file was deleted.
    DELETE = "delete"


class ReasoningSummary(Enum):
    "Reasoning summary mode used for model calls, if applicable (e.g. \"none\", \"concise\", \"detailed\")"
    # Do not request reasoning summaries from the model.
    NONE = "none"
    # Request a concise summary of the model's reasoning.
    CONCISE = "concise"
    # Request a detailed summary of the model's reasoning.
    DETAILED = "detailed"


class ScheduleOrigin(Enum):
    "Who created the schedule: `user` (an explicit user action such as `/every` or `/after`) or `model` (the agent via the `manage_schedule` tool). Gates whether a scheduled skill that opted out of model invocation may fire: only user-created schedules may."
    # The schedule was created by an explicit user action, such as `/every` or `/after`.
    USER = "user"
    # The schedule was created by the agent via the `manage_schedule` tool.
    MODEL = "model"


class SessionLimitsExhaustedResponseAction(Enum):
    "User action selected for an exhausted session limit."
    # Increase the current max by an exact AI Credits amount.
    ADD = "add"
    # Set a new absolute max AI Credits value.
    SET = "set"
    # Remove the current session limit.
    UNSET = "unset"
    # Leave the limit unchanged and cancel the blocked model request.
    CANCEL = "cancel"


class SessionMode(Enum):
    "The session mode the agent is operating in"
    # The agent is responding interactively to the user.
    INTERACTIVE = "interactive"
    # The agent is preparing a plan before making changes.
    PLAN = "plan"
    # The agent is working autonomously toward task completion.
    AUTOPILOT = "autopilot"


class ShutdownType(Enum):
    "Whether the session ended normally (\"routine\") or due to a crash/fatal error (\"error\")"
    # The session ended normally.
    ROUTINE = "routine"
    # The session ended because of a crash or fatal error.
    ERROR = "error"


class SkillInvokedTrigger(Enum):
    "What triggered the skill invocation: `user-invoked` (explicit user action, such as via a slash command or UI affordance), `agent-invoked` (agent requested the skill), or `context-load` (loaded as part of another context, such as preloading skills configured on a custom agent or subagent)"
    # Skill invocation requested explicitly by the user, such as via a slash command or UI affordance.
    USER_INVOKED = "user-invoked"
    # Skill invocation requested by the agent.
    AGENT_INVOKED = "agent-invoked"
    # Skill content loaded as part of another context, such as a configured custom agent or subagent.
    CONTEXT_LOAD = "context-load"


class SkillSource(Enum):
    "Source location type (e.g., project, personal-copilot, plugin, builtin)"
    # Skill defined in the current project's skill directories.
    PROJECT = "project"
    # Skill discovered from a parent directory in the current workspace tree.
    INHERITED = "inherited"
    # Skill defined in the user's Copilot skill directory.
    PERSONAL_COPILOT = "personal-copilot"
    # Skill defined in the user's personal agents skill directory.
    PERSONAL_AGENTS = "personal-agents"
    # Skill provided by an installed plugin.
    PLUGIN = "plugin"
    # Skill loaded from a configured custom skill directory.
    CUSTOM = "custom"
    # Skill bundled with the runtime.
    BUILTIN = "builtin"


class SystemMessageRole(Enum):
    "Message role: \"system\" for system prompts, \"developer\" for developer-injected instructions"
    # System prompt message.
    SYSTEM = "system"
    # Developer instruction message.
    DEVELOPER = "developer"


class SystemNotificationAgentCompletedStatus(Enum):
    "Whether the agent completed successfully or failed"
    # The agent completed successfully.
    COMPLETED = "completed"
    # The agent failed.
    FAILED = "failed"


class SystemNotificationFactoryCompletedStatus(Enum):
    "Terminal status reached by a factory execution attempt."
    # The factory completed successfully.
    COMPLETED = "completed"
    # The factory was halted.
    HALTED = "halted"
    # The factory was cancelled.
    CANCELLED = "cancelled"
    # The factory failed.
    ERROR = "error"


class TaskCompletionOutcome(Enum):
    "Semantic result of evaluating a task completion request"
    # The completion request was accepted and the objective is complete.
    COMPLETED = "completed"
    # The completion request was rejected because more work or validation remains.
    CONTINUE = "continue"
    # Completion cannot proceed without intervention; the active objective is paused when one is identified.
    BLOCKED = "blocked"


class ToolExecutionCompleteContentResourceLinkIconTheme(Enum):
    "Theme variant this icon is intended for"
    # Icon intended for light themes.
    LIGHT = "light"
    # Icon intended for dark themes.
    DARK = "dark"


class ToolExecutionCompleteToolDescriptionMetaUIVisibility(Enum):
    "Allowed values for the `ToolExecutionCompleteToolDescriptionMetaUIVisibility` enumeration."
    # Tool is callable by the model (LLM tool surface)
    MODEL = "model"
    # Tool is callable by the MCP App view (iframe) via session.mcp.apps.callTool
    APP = "app"


class ToolExecutionStartToolDescriptionMetaUIVisibility(Enum):
    "Allowed values for the `ToolExecutionStartToolDescriptionMetaUIVisibility` enumeration."
    # Tool is callable by the model (LLM tool surface)
    MODEL = "model"
    # Tool is callable by the MCP App view (iframe) via session.mcp.apps.callTool
    APP = "app"


class UserMessageAgentMode(Enum):
    "The agent mode that was active when this message was sent"
    # The agent is responding interactively to the user.
    INTERACTIVE = "interactive"
    # The agent is preparing a plan before making changes.
    PLAN = "plan"
    # The agent is working autonomously toward task completion.
    AUTOPILOT = "autopilot"
    # The agent is in shell-focused UI mode.
    SHELL = "shell"


class UserMessageDelivery(Enum):
    "How this user message was delivered to the agentic loop, relative to whether the loop was already running. This is the timing axis only; the message's origin (human vs. system/command/schedule/skill/etc.) is carried separately by `source`. A system-injected message has a delivery too — e.g. a background-task notification waking an idle agent is `idle`, the same mechanism as a human starting a fresh turn."
    # Delivered while the loop was idle; starts its own run immediately (a human's fresh turn, or a system notification waking an idle agent).
    IDLE = "idle"
    # Injected into the current in-flight run while the agent was busy (immediate mode).
    STEERING = "steering"
    # Enqueued while the agent was busy; processed as its own run afterward.
    QUEUED = "queued"


class Verbosity(Enum):
    "Output verbosity level used for supported model calls (e.g. \"low\", \"medium\", \"high\")"
    # A terse response was requested.
    LOW = "low"
    # A medium amount of response detail was requested.
    MEDIUM = "medium"
    # A more detailed response was requested.
    HIGH = "high"


class WorkingDirectoryContextHostType(Enum):
    "Hosting platform type of the repository (github or ado)"
    # Repository is hosted on GitHub.
    GITHUB = "github"
    # Repository is hosted on Azure DevOps.
    ADO = "ado"


class WorkspaceFileChangedOperation(Enum):
    "Whether the file was newly created or updated"
    # The workspace file was created.
    CREATE = "create"
    # The workspace file was updated.
    UPDATE = "update"


SessionEventData = SessionStartData | SessionResumeData | SessionRemoteSteerableChangedData | SessionErrorData | SessionIdleData | SessionTitleChangedData | SessionScheduleCreatedData | SessionScheduleCancelledData | SessionScheduleRearmedData | SessionAutopilotObjectiveChangedData | SessionInfoData | SessionWarningData | SessionModelChangeData | SessionModeChangedData | SessionSessionLimitsChangedData | SessionPermissionsChangedData | SessionPlanChangedData | SessionTodosChangedData | SessionWorkspaceFileChangedData | SessionHandoffData | SessionTruncationData | SessionSnapshotRewindData | SessionShutdownData | SessionUsageCheckpointData | SessionContextChangedData | SessionUsageInfoData | SessionContextClearedData | SessionCompactionStartData | SessionCompactionCompleteData | SessionTaskCompleteData | SessionFusionRouteStartedData | SessionFusionRouteFailedData | SessionFusionResolvedData | SessionFusionCompletedData | UserMessageData | PendingMessagesModifiedData | AssistantTurnStartData | AssistantTurnRetryData | AgentInterruptedData | AssistantIntentData | AssistantFusionPhaseStartedData | AssistantFusionPhaseCompletedData | AssistantFusionPhaseFailedData | AssistantServerToolProgressData | AssistantReasoningData | AssistantReasoningDeltaData | AssistantToolCallDeltaData | AssistantStreamingDeltaData | AssistantMessageData | AssistantMessageStartData | AssistantMessageDeltaData | AssistantTurnEndData | AssistantIdleData | AssistantUsageData | PromptCacheBreakData | ModelCallFailureData | ModelCallFinishedData | ModelCallStartData | AbortData | ToolUserRequestedData | ToolExecutionStartData | ToolExecutionPartialResultData | ToolExecutionProgressData | ToolExecutionCompleteData | ToolSearchActivatedData | SkillInvokedData | SandboxDecisionData | SubagentStartedData | SubagentConfiguredData | SubagentCompletedData | SubagentFailedData | SubagentSelectedData | SubagentDeselectedData | HookStartData | HookEndData | HookProgressData | SessionBinaryAssetData | SystemMessageData | SystemNotificationData | PermissionRequestedData | PermissionCompletedData | UserInputRequestedData | UserInputCompletedData | ElicitationRequestedData | ElicitationCompletedData | SamplingRequestedData | SamplingCompletedData | McpOauthRequiredData | McpOauthCompletedData | McpHeadersRefreshRequiredData | McpHeadersRefreshCompletedData | SessionCustomNotificationData | UiEphemeralQueryData | ExternalToolRequestedData | ExternalToolCompletedData | CommandQueuedData | CommandExecuteData | CommandCompletedData | AutoModeSwitchRequestedData | AutoModeSwitchCompletedData | SessionLimitsExhaustedRequestedData | SessionLimitsExhaustedCompletedData | SessionAutoModeResolvedData | SessionManagedSettingsResolvedData | SessionManagedSettingsEnforcedData | CommandsChangedData | CapabilitiesChangedData | ExitPlanModeRequestedData | ExitPlanModeCompletedData | SessionToolsUpdatedData | SessionBackgroundTasksChangedData | FactoryRunUpdatedData | FactoryRunStartedData | FactoryRunSettledData | SessionSkillsLoadedData | SessionCustomAgentsUpdatedData | SessionMcpServersLoadedData | SessionMcpServerStatusChangedData | McpToolsListChangedData | McpResourcesListChangedData | McpPromptsListChangedData | SessionExtensionsLoadedData | SessionCanvasOpenedData | SessionCanvasRegistryChangedData | SessionCanvasClosedData | SessionCanvasUnavailableData | SessionCanvasRecordedData | SessionCanvasRemovedData | SessionExtensionsAttachmentsPushedData | McpAppToolCallCompleteData | RawSessionEventData | Data


@dataclass
class SessionEvent:
    data: SessionEventData
    id: UUID
    timestamp: datetime
    type: SessionEventType
    agent_id: str | None = None
    ephemeral: bool | None = None
    parent_id: UUID | None = None
    raw_type: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionEvent":
        assert isinstance(obj, dict)
        raw_type = from_str(obj.get("type"))
        event_type = SessionEventType(raw_type)
        agent_id = from_union([from_none, from_str], obj.get("agentId"))
        ephemeral = from_union([from_none, from_bool], obj.get("ephemeral"))
        id = from_uuid(obj.get("id"))
        parent_id = from_union([from_none, from_uuid], obj.get("parentId"))
        timestamp = from_datetime(obj.get("timestamp"))
        data_obj = obj.get("data")
        match event_type:
            case SessionEventType.SESSION_START: data = SessionStartData.from_dict(data_obj)
            case SessionEventType.SESSION_RESUME: data = SessionResumeData.from_dict(data_obj)
            case SessionEventType.SESSION_REMOTE_STEERABLE_CHANGED: data = SessionRemoteSteerableChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_ERROR: data = SessionErrorData.from_dict(data_obj)
            case SessionEventType.SESSION_IDLE: data = SessionIdleData.from_dict(data_obj)
            case SessionEventType.SESSION_TITLE_CHANGED: data = SessionTitleChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_SCHEDULE_CREATED: data = SessionScheduleCreatedData.from_dict(data_obj)
            case SessionEventType.SESSION_SCHEDULE_CANCELLED: data = SessionScheduleCancelledData.from_dict(data_obj)
            case SessionEventType.SESSION_SCHEDULE_REARMED: data = SessionScheduleRearmedData.from_dict(data_obj)
            case SessionEventType.SESSION_AUTOPILOT_OBJECTIVE_CHANGED: data = SessionAutopilotObjectiveChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_INFO: data = SessionInfoData.from_dict(data_obj)
            case SessionEventType.SESSION_WARNING: data = SessionWarningData.from_dict(data_obj)
            case SessionEventType.SESSION_MODEL_CHANGE: data = SessionModelChangeData.from_dict(data_obj)
            case SessionEventType.SESSION_MODE_CHANGED: data = SessionModeChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_SESSION_LIMITS_CHANGED: data = SessionSessionLimitsChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_PERMISSIONS_CHANGED: data = SessionPermissionsChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_PLAN_CHANGED: data = SessionPlanChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_TODOS_CHANGED: data = SessionTodosChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_WORKSPACE_FILE_CHANGED: data = SessionWorkspaceFileChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_HANDOFF: data = SessionHandoffData.from_dict(data_obj)
            case SessionEventType.SESSION_TRUNCATION: data = SessionTruncationData.from_dict(data_obj)
            case SessionEventType.SESSION_SNAPSHOT_REWIND: data = SessionSnapshotRewindData.from_dict(data_obj)
            case SessionEventType.SESSION_SHUTDOWN: data = SessionShutdownData.from_dict(data_obj)
            case SessionEventType.SESSION_USAGE_CHECKPOINT: data = SessionUsageCheckpointData.from_dict(data_obj)
            case SessionEventType.SESSION_CONTEXT_CHANGED: data = SessionContextChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_USAGE_INFO: data = SessionUsageInfoData.from_dict(data_obj)
            case SessionEventType.SESSION_CONTEXT_CLEARED: data = SessionContextClearedData.from_dict(data_obj)
            case SessionEventType.SESSION_COMPACTION_START: data = SessionCompactionStartData.from_dict(data_obj)
            case SessionEventType.SESSION_COMPACTION_COMPLETE: data = SessionCompactionCompleteData.from_dict(data_obj)
            case SessionEventType.SESSION_TASK_COMPLETE: data = SessionTaskCompleteData.from_dict(data_obj)
            case SessionEventType.SESSION_FUSION_ROUTE_STARTED: data = SessionFusionRouteStartedData.from_dict(data_obj)
            case SessionEventType.SESSION_FUSION_ROUTE_FAILED: data = SessionFusionRouteFailedData.from_dict(data_obj)
            case SessionEventType.SESSION_FUSION_RESOLVED: data = SessionFusionResolvedData.from_dict(data_obj)
            case SessionEventType.SESSION_FUSION_COMPLETED: data = SessionFusionCompletedData.from_dict(data_obj)
            case SessionEventType.USER_MESSAGE: data = UserMessageData.from_dict(data_obj)
            case SessionEventType.PENDING_MESSAGES_MODIFIED: data = PendingMessagesModifiedData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TURN_START: data = AssistantTurnStartData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TURN_RETRY: data = AssistantTurnRetryData.from_dict(data_obj)
            case SessionEventType.AGENT_INTERRUPTED: data = AgentInterruptedData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_INTENT: data = AssistantIntentData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_FUSION_PHASE_STARTED: data = AssistantFusionPhaseStartedData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_FUSION_PHASE_COMPLETED: data = AssistantFusionPhaseCompletedData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_FUSION_PHASE_FAILED: data = AssistantFusionPhaseFailedData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_SERVER_TOOL_PROGRESS: data = AssistantServerToolProgressData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_REASONING: data = AssistantReasoningData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_REASONING_DELTA: data = AssistantReasoningDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TOOL_CALL_DELTA: data = AssistantToolCallDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_STREAMING_DELTA: data = AssistantStreamingDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_MESSAGE: data = AssistantMessageData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_MESSAGE_START: data = AssistantMessageStartData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_MESSAGE_DELTA: data = AssistantMessageDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TURN_END: data = AssistantTurnEndData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_IDLE: data = AssistantIdleData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_USAGE: data = AssistantUsageData.from_dict(data_obj)
            case SessionEventType.PROMPT_CACHE_BREAK: data = PromptCacheBreakData.from_dict(data_obj)
            case SessionEventType.MODEL_CALL_FAILURE: data = ModelCallFailureData.from_dict(data_obj)
            case SessionEventType.MODEL_CALL_FINISHED: data = ModelCallFinishedData.from_dict(data_obj)
            case SessionEventType.MODEL_CALL_START: data = ModelCallStartData.from_dict(data_obj)
            case SessionEventType.ABORT: data = AbortData.from_dict(data_obj)
            case SessionEventType.TOOL_USER_REQUESTED: data = ToolUserRequestedData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_START: data = ToolExecutionStartData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_PARTIAL_RESULT: data = ToolExecutionPartialResultData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_PROGRESS: data = ToolExecutionProgressData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_COMPLETE: data = ToolExecutionCompleteData.from_dict(data_obj)
            case SessionEventType.TOOL_SEARCH_ACTIVATED: data = ToolSearchActivatedData.from_dict(data_obj)
            case SessionEventType.SKILL_INVOKED: data = SkillInvokedData.from_dict(data_obj)
            case SessionEventType.SANDBOX_DECISION: data = SandboxDecisionData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_STARTED: data = SubagentStartedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_CONFIGURED: data = SubagentConfiguredData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_COMPLETED: data = SubagentCompletedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_FAILED: data = SubagentFailedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_SELECTED: data = SubagentSelectedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_DESELECTED: data = SubagentDeselectedData.from_dict(data_obj)
            case SessionEventType.HOOK_START: data = HookStartData.from_dict(data_obj)
            case SessionEventType.HOOK_END: data = HookEndData.from_dict(data_obj)
            case SessionEventType.HOOK_PROGRESS: data = HookProgressData.from_dict(data_obj)
            case SessionEventType.SESSION_BINARY_ASSET: data = SessionBinaryAssetData.from_dict(data_obj)
            case SessionEventType.SYSTEM_MESSAGE: data = SystemMessageData.from_dict(data_obj)
            case SessionEventType.SYSTEM_NOTIFICATION: data = SystemNotificationData.from_dict(data_obj)
            case SessionEventType.PERMISSION_REQUESTED: data = PermissionRequestedData.from_dict(data_obj)
            case SessionEventType.PERMISSION_COMPLETED: data = PermissionCompletedData.from_dict(data_obj)
            case SessionEventType.USER_INPUT_REQUESTED: data = UserInputRequestedData.from_dict(data_obj)
            case SessionEventType.USER_INPUT_COMPLETED: data = UserInputCompletedData.from_dict(data_obj)
            case SessionEventType.ELICITATION_REQUESTED: data = ElicitationRequestedData.from_dict(data_obj)
            case SessionEventType.ELICITATION_COMPLETED: data = ElicitationCompletedData.from_dict(data_obj)
            case SessionEventType.SAMPLING_REQUESTED: data = SamplingRequestedData.from_dict(data_obj)
            case SessionEventType.SAMPLING_COMPLETED: data = SamplingCompletedData.from_dict(data_obj)
            case SessionEventType.MCP_OAUTH_REQUIRED: data = McpOauthRequiredData.from_dict(data_obj)
            case SessionEventType.MCP_OAUTH_COMPLETED: data = McpOauthCompletedData.from_dict(data_obj)
            case SessionEventType.MCP_HEADERS_REFRESH_REQUIRED: data = McpHeadersRefreshRequiredData.from_dict(data_obj)
            case SessionEventType.MCP_HEADERS_REFRESH_COMPLETED: data = McpHeadersRefreshCompletedData.from_dict(data_obj)
            case SessionEventType.SESSION_CUSTOM_NOTIFICATION: data = SessionCustomNotificationData.from_dict(data_obj)
            case SessionEventType.UI_EPHEMERAL_QUERY: data = UiEphemeralQueryData.from_dict(data_obj)
            case SessionEventType.EXTERNAL_TOOL_REQUESTED: data = ExternalToolRequestedData.from_dict(data_obj)
            case SessionEventType.EXTERNAL_TOOL_COMPLETED: data = ExternalToolCompletedData.from_dict(data_obj)
            case SessionEventType.COMMAND_QUEUED: data = CommandQueuedData.from_dict(data_obj)
            case SessionEventType.COMMAND_EXECUTE: data = CommandExecuteData.from_dict(data_obj)
            case SessionEventType.COMMAND_COMPLETED: data = CommandCompletedData.from_dict(data_obj)
            case SessionEventType.AUTO_MODE_SWITCH_REQUESTED: data = AutoModeSwitchRequestedData.from_dict(data_obj)
            case SessionEventType.AUTO_MODE_SWITCH_COMPLETED: data = AutoModeSwitchCompletedData.from_dict(data_obj)
            case SessionEventType.SESSION_LIMITS_EXHAUSTED_REQUESTED: data = SessionLimitsExhaustedRequestedData.from_dict(data_obj)
            case SessionEventType.SESSION_LIMITS_EXHAUSTED_COMPLETED: data = SessionLimitsExhaustedCompletedData.from_dict(data_obj)
            case SessionEventType.SESSION_AUTO_MODE_RESOLVED: data = SessionAutoModeResolvedData.from_dict(data_obj)
            case SessionEventType.SESSION_MANAGED_SETTINGS_RESOLVED: data = SessionManagedSettingsResolvedData.from_dict(data_obj)
            case SessionEventType.SESSION_MANAGED_SETTINGS_ENFORCED: data = SessionManagedSettingsEnforcedData.from_dict(data_obj)
            case SessionEventType.COMMANDS_CHANGED: data = CommandsChangedData.from_dict(data_obj)
            case SessionEventType.CAPABILITIES_CHANGED: data = CapabilitiesChangedData.from_dict(data_obj)
            case SessionEventType.EXIT_PLAN_MODE_REQUESTED: data = ExitPlanModeRequestedData.from_dict(data_obj)
            case SessionEventType.EXIT_PLAN_MODE_COMPLETED: data = ExitPlanModeCompletedData.from_dict(data_obj)
            case SessionEventType.SESSION_TOOLS_UPDATED: data = SessionToolsUpdatedData.from_dict(data_obj)
            case SessionEventType.SESSION_BACKGROUND_TASKS_CHANGED: data = SessionBackgroundTasksChangedData.from_dict(data_obj)
            case SessionEventType.FACTORY_RUN_UPDATED: data = FactoryRunUpdatedData.from_dict(data_obj)
            case SessionEventType.FACTORY_RUN_STARTED: data = FactoryRunStartedData.from_dict(data_obj)
            case SessionEventType.FACTORY_RUN_SETTLED: data = FactoryRunSettledData.from_dict(data_obj)
            case SessionEventType.SESSION_SKILLS_LOADED: data = SessionSkillsLoadedData.from_dict(data_obj)
            case SessionEventType.SESSION_CUSTOM_AGENTS_UPDATED: data = SessionCustomAgentsUpdatedData.from_dict(data_obj)
            case SessionEventType.SESSION_MCP_SERVERS_LOADED: data = SessionMcpServersLoadedData.from_dict(data_obj)
            case SessionEventType.SESSION_MCP_SERVER_STATUS_CHANGED: data = SessionMcpServerStatusChangedData.from_dict(data_obj)
            case SessionEventType.MCP_TOOLS_LIST_CHANGED: data = McpToolsListChangedData.from_dict(data_obj)
            case SessionEventType.MCP_RESOURCES_LIST_CHANGED: data = McpResourcesListChangedData.from_dict(data_obj)
            case SessionEventType.MCP_PROMPTS_LIST_CHANGED: data = McpPromptsListChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_EXTENSIONS_LOADED: data = SessionExtensionsLoadedData.from_dict(data_obj)
            case SessionEventType.SESSION_CANVAS_OPENED: data = SessionCanvasOpenedData.from_dict(data_obj)
            case SessionEventType.SESSION_CANVAS_REGISTRY_CHANGED: data = SessionCanvasRegistryChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_CANVAS_CLOSED: data = SessionCanvasClosedData.from_dict(data_obj)
            case SessionEventType.SESSION_CANVAS_UNAVAILABLE: data = SessionCanvasUnavailableData.from_dict(data_obj)
            case SessionEventType.SESSION_CANVAS_RECORDED: data = SessionCanvasRecordedData.from_dict(data_obj)
            case SessionEventType.SESSION_CANVAS_REMOVED: data = SessionCanvasRemovedData.from_dict(data_obj)
            case SessionEventType.SESSION_EXTENSIONS_ATTACHMENTS_PUSHED: data = SessionExtensionsAttachmentsPushedData.from_dict(data_obj)
            case SessionEventType.MCP_APP_TOOL_CALL_COMPLETE: data = McpAppToolCallCompleteData.from_dict(data_obj)
            case _: data = RawSessionEventData.from_dict(data_obj)
        return SessionEvent(
            data=data,
            id=id,
            timestamp=timestamp,
            type=event_type,
            agent_id=agent_id,
            ephemeral=ephemeral,
            parent_id=parent_id,
            raw_type=raw_type if event_type == SessionEventType.UNKNOWN else None,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = self.data.to_dict()
        result["id"] = to_uuid(self.id)
        result["timestamp"] = to_datetime(self.timestamp)
        result["type"] = self.raw_type if self.type == SessionEventType.UNKNOWN and self.raw_type is not None else to_enum(SessionEventType, self.type)
        if self.agent_id is not None:
            result["agentId"] = from_union([from_none, from_str], self.agent_id)
        if self.ephemeral is not None:
            result["ephemeral"] = from_union([from_none, from_bool], self.ephemeral)
        result["parentId"] = from_union([from_none, to_uuid], self.parent_id)
        return result


def session_event_from_dict(s: Any) -> SessionEvent:
    return SessionEvent.from_dict(s)


def session_event_to_dict(x: SessionEvent) -> Any:
    return x.to_dict()

__all__ = [
    "AbortData",
    "AbortReason",
    "AgentInterruptedActivity",
    "AgentInterruptedCancelPhase",
    "AgentInterruptedData",
    "AssistantFusionPhaseCompletedData",
    "AssistantFusionPhaseFailedData",
    "AssistantFusionPhaseStartedData",
    "AssistantIdleData",
    "AssistantIntentData",
    "AssistantMessageData",
    "AssistantMessageDeltaData",
    "AssistantMessageReasoningBlocks",
    "AssistantMessageServerTools",
    "AssistantMessageStartData",
    "AssistantMessageToolRequest",
    "AssistantMessageToolRequestCaller",
    "AssistantMessageToolRequestCallerType",
    "AssistantMessageToolRequestType",
    "AssistantReasoningData",
    "AssistantReasoningDeltaData",
    "AssistantServerToolProgressData",
    "AssistantStreamingDeltaData",
    "AssistantToolCallDeltaData",
    "AssistantTurnEndData",
    "AssistantTurnRetryData",
    "AssistantTurnStartData",
    "AssistantUsageApiEndpoint",
    "AssistantUsageCopilotUsage",
    "AssistantUsageCopilotUsageTokenDetail",
    "AssistantUsageData",
    "AssistantUsageTransport",
    "AssistedApprovalJudgeFailureReason",
    "AssistedApprovalRecommendation",
    "Attachment",
    "AttachmentBlob",
    "AttachmentDirectory",
    "AttachmentExtensionContext",
    "AttachmentFile",
    "AttachmentFileLineRange",
    "AttachmentGitHubActionsJob",
    "AttachmentGitHubCommit",
    "AttachmentGitHubFile",
    "AttachmentGitHubFileDiff",
    "AttachmentGitHubFileDiffSide",
    "AttachmentGitHubReference",
    "AttachmentGitHubReferenceType",
    "AttachmentGitHubRelease",
    "AttachmentGitHubRepository",
    "AttachmentGitHubSnippet",
    "AttachmentGitHubTreeComparison",
    "AttachmentGitHubTreeComparisonSide",
    "AttachmentGitHubUrl",
    "AttachmentSelection",
    "AttachmentSelectionDetails",
    "AttachmentSelectionDetailsEnd",
    "AttachmentSelectionDetailsStart",
    "AutoModeResolvedReasoningBucket",
    "AutoModeSwitchCompletedData",
    "AutoModeSwitchRequestedData",
    "AutoModeSwitchResponse",
    "AutoTier",
    "AutopilotObjectiveChangedOperation",
    "AutopilotObjectiveChangedStatus",
    "BinaryAssetReference",
    "BinaryAssetReferenceType",
    "BinaryAssetType",
    "CanvasRegistryChangedCanvas",
    "CanvasRegistryChangedCanvasAction",
    "CapabilitiesChangedData",
    "CapabilitiesChangedUI",
    "CitableSource",
    "CitationLocation",
    "CitationLocationBlock",
    "CitationLocationChar",
    "CitationLocationPage",
    "CitationProvider",
    "CitationReference",
    "CitationSource",
    "CitationSpan",
    "Citations",
    "CommandCompletedData",
    "CommandExecuteData",
    "CommandQueuedData",
    "CommandsChangedCommand",
    "CommandsChangedData",
    "CompactionCompleteCompactionTokensUsed",
    "CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail",
    "CompactionTrigger",
    "ContextTier",
    "CustomAgentsUpdatedAgent",
    "Data",
    "ElicitationCompletedAction",
    "ElicitationCompletedData",
    "ElicitationRequestedData",
    "ElicitationRequestedMode",
    "ElicitationRequestedSchema",
    "EmbeddedBlobResourceContents",
    "EmbeddedTextResourceContents",
    "ExitPlanModeAction",
    "ExitPlanModeCompletedData",
    "ExitPlanModeRequestedData",
    "ExtensionsLoadedExtension",
    "ExtensionsLoadedExtensionSource",
    "ExtensionsLoadedExtensionStatus",
    "ExternalToolCompletedData",
    "ExternalToolRequestedData",
    "FactoryPermissionOperation",
    "FactoryPermissionPhase",
    "FactoryRunSettledData",
    "FactoryRunSettledStatus",
    "FactoryRunStartedData",
    "FactoryRunUpdatedData",
    "FusionAttribution",
    "FusionConversationScope",
    "FusionFollowUpAction",
    "FusionFollowUpRecommendation",
    "FusionPattern",
    "FusionPhaseKind",
    "FusionPhaseStatus",
    "FusionPhaseUsage",
    "FusionScores",
    "FusionTurnKind",
    "GitHubMcpToolConfig",
    "GitHubRepoRef",
    "HandoffRepository",
    "HandoffSourceType",
    "HeaderEntry",
    "HookEndData",
    "HookEndError",
    "HookProgressData",
    "HookStartData",
    "ManagedSettingsEnforcedAction",
    "ManagedSettingsEnforcedEscalation",
    "ManagedSettingsResolvedSource",
    "McpAppToolCallCompleteData",
    "McpAppToolCallCompleteError",
    "McpAppToolCallCompleteToolMeta",
    "McpAppToolCallCompleteToolMetaUI",
    "McpHeadersRefreshCompletedData",
    "McpHeadersRefreshCompletedOutcome",
    "McpHeadersRefreshRequiredData",
    "McpHeadersRefreshRequiredReason",
    "McpOauthCompletedData",
    "McpOauthCompletionOutcome",
    "McpOauthHttpResponse",
    "McpOauthRequestReason",
    "McpOauthRequiredData",
    "McpOauthRequiredStaticClientConfig",
    "McpOauthWWWAuthenticateParams",
    "McpPromptsListChangedData",
    "McpResourcesListChangedData",
    "McpServerSource",
    "McpServerStatus",
    "McpServerTransport",
    "McpServersLoadedServer",
    "McpToolsListChangedData",
    "ModelCallFailureBadRequestKind",
    "ModelCallFailureData",
    "ModelCallFailureKind",
    "ModelCallFailureRequestFingerprint",
    "ModelCallFailureSource",
    "ModelCallFailureTransport",
    "ModelCallFinishedData",
    "ModelCallFinishedOutcome",
    "ModelCallStartData",
    "ModelChangeSource",
    "OmittedBinaryOmittedReason",
    "OmittedBinaryResult",
    "OmittedBinaryType",
    "PendingMessagesModifiedData",
    "PermissionApproved",
    "PermissionApprovedForLocation",
    "PermissionApprovedForSession",
    "PermissionAssistedApproval",
    "PermissionCancelled",
    "PermissionCompletedData",
    "PermissionDeniedByContentExclusionPolicy",
    "PermissionDeniedByPermissionRequestHook",
    "PermissionDeniedByRules",
    "PermissionDeniedInteractivelyByUser",
    "PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser",
    "PermissionMode",
    "PermissionPromptRequest",
    "PermissionPromptRequestCommands",
    "PermissionPromptRequestCustomTool",
    "PermissionPromptRequestExtensionEnvAccess",
    "PermissionPromptRequestExtensionManagement",
    "PermissionPromptRequestExtensionPermissionAccess",
    "PermissionPromptRequestFactory",
    "PermissionPromptRequestHook",
    "PermissionPromptRequestMcp",
    "PermissionPromptRequestMemory",
    "PermissionPromptRequestPath",
    "PermissionPromptRequestPathAccessKind",
    "PermissionPromptRequestRead",
    "PermissionPromptRequestUrl",
    "PermissionPromptRequestWrite",
    "PermissionRecommendation",
    "PermissionRequest",
    "PermissionRequestCustomTool",
    "PermissionRequestExtensionEnvAccess",
    "PermissionRequestExtensionManagement",
    "PermissionRequestExtensionPermissionAccess",
    "PermissionRequestFactory",
    "PermissionRequestHook",
    "PermissionRequestMcp",
    "PermissionRequestMemory",
    "PermissionRequestMemoryAction",
    "PermissionRequestMemoryDirection",
    "PermissionRequestMemoryScope",
    "PermissionRequestRead",
    "PermissionRequestShell",
    "PermissionRequestShellCommand",
    "PermissionRequestShellCommandSegment",
    "PermissionRequestShellPossibleUrl",
    "PermissionRequestUrl",
    "PermissionRequestWrite",
    "PermissionRequestedData",
    "PermissionResult",
    "PermissionRule",
    "PersistedBinaryImage",
    "PersistedBinaryImageType",
    "PersistedBinaryResult",
    "PlanChangedOperation",
    "PromptCacheBreakData",
    "RawSessionEventData",
    "ReasoningSummary",
    "SamplingCompletedData",
    "SamplingRequestedData",
    "SandboxDecisionData",
    "ScheduleOrigin",
    "SessionAutoModeResolvedData",
    "SessionAutopilotObjectiveChangedData",
    "SessionBackgroundTasksChangedData",
    "SessionBinaryAssetData",
    "SessionCanvasClosedData",
    "SessionCanvasOpenedData",
    "SessionCanvasRecordedData",
    "SessionCanvasRegistryChangedData",
    "SessionCanvasRemovedData",
    "SessionCanvasUnavailableData",
    "SessionCompactionCompleteData",
    "SessionCompactionStartData",
    "SessionContextChangedData",
    "SessionContextClearedData",
    "SessionCustomAgentsUpdatedData",
    "SessionCustomNotificationData",
    "SessionErrorData",
    "SessionEvent",
    "SessionEventData",
    "SessionEventType",
    "SessionExtensionsAttachmentsPushedData",
    "SessionExtensionsLoadedData",
    "SessionFusionCompletedData",
    "SessionFusionResolvedData",
    "SessionFusionRouteFailedData",
    "SessionFusionRouteStartedData",
    "SessionHandoffData",
    "SessionIdleData",
    "SessionInfoData",
    "SessionLimitsConfig",
    "SessionLimitsExhaustedCompletedData",
    "SessionLimitsExhaustedRequestedData",
    "SessionLimitsExhaustedResponse",
    "SessionLimitsExhaustedResponseAction",
    "SessionManagedSettingsEnforcedData",
    "SessionManagedSettingsResolvedData",
    "SessionMcpServerStatusChangedData",
    "SessionMcpServersLoadedData",
    "SessionMode",
    "SessionModeChangedData",
    "SessionModelChangeData",
    "SessionPermissionsChangedData",
    "SessionPlanChangedData",
    "SessionRemoteSteerableChangedData",
    "SessionResumeData",
    "SessionScheduleCancelledData",
    "SessionScheduleCreatedData",
    "SessionScheduleRearmedData",
    "SessionSessionLimitsChangedData",
    "SessionShutdownData",
    "SessionSkillsLoadedData",
    "SessionSnapshotRewindData",
    "SessionStartData",
    "SessionTaskCompleteData",
    "SessionTitleChangedData",
    "SessionTodosChangedData",
    "SessionToolsUpdatedData",
    "SessionTruncationData",
    "SessionUsageCheckpointData",
    "SessionUsageInfoData",
    "SessionWarningData",
    "SessionWorkspaceFileChangedData",
    "ShutdownAgentMetric",
    "ShutdownCodeChanges",
    "ShutdownModelMetric",
    "ShutdownModelMetricRequests",
    "ShutdownModelMetricTokenDetail",
    "ShutdownModelMetricUsage",
    "ShutdownTokenDetail",
    "ShutdownType",
    "SkillInvokedData",
    "SkillInvokedTrigger",
    "SkillSource",
    "SkillsLoadedSkill",
    "SubagentCompletedData",
    "SubagentConfiguredData",
    "SubagentDeselectedData",
    "SubagentFailedData",
    "SubagentSelectedData",
    "SubagentStartedData",
    "SystemMessageData",
    "SystemMessageMetadata",
    "SystemMessageRole",
    "SystemNotification",
    "SystemNotificationAgentCompleted",
    "SystemNotificationAgentCompletedStatus",
    "SystemNotificationAgentIdle",
    "SystemNotificationData",
    "SystemNotificationFactoryCompleted",
    "SystemNotificationFactoryCompletedStatus",
    "SystemNotificationInstructionDiscovered",
    "SystemNotificationNewInboxMessage",
    "SystemNotificationShellCompleted",
    "SystemNotificationShellDetachedCompleted",
    "SystemNotificationUnclassified",
    "TaskCompletionOutcome",
    "ToolExecutionCompleteContent",
    "ToolExecutionCompleteContentAudio",
    "ToolExecutionCompleteContentImage",
    "ToolExecutionCompleteContentResource",
    "ToolExecutionCompleteContentResourceDetails",
    "ToolExecutionCompleteContentResourceLink",
    "ToolExecutionCompleteContentResourceLinkIcon",
    "ToolExecutionCompleteContentResourceLinkIconTheme",
    "ToolExecutionCompleteContentShellExit",
    "ToolExecutionCompleteContentTerminal",
    "ToolExecutionCompleteContentText",
    "ToolExecutionCompleteData",
    "ToolExecutionCompleteError",
    "ToolExecutionCompleteResult",
    "ToolExecutionCompleteToolDescription",
    "ToolExecutionCompleteToolDescriptionMeta",
    "ToolExecutionCompleteToolDescriptionMetaUI",
    "ToolExecutionCompleteToolDescriptionMetaUIVisibility",
    "ToolExecutionCompleteUIResource",
    "ToolExecutionCompleteUIResourceMeta",
    "ToolExecutionCompleteUIResourceMetaUI",
    "ToolExecutionCompleteUIResourceMetaUICsp",
    "ToolExecutionCompleteUIResourceMetaUIPermissions",
    "ToolExecutionCompleteUIResourceMetaUIPermissionsCamera",
    "ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite",
    "ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation",
    "ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone",
    "ToolExecutionPartialResultData",
    "ToolExecutionProgressData",
    "ToolExecutionStartData",
    "ToolExecutionStartShellToolInfo",
    "ToolExecutionStartToolDescription",
    "ToolExecutionStartToolDescriptionMeta",
    "ToolExecutionStartToolDescriptionMetaUI",
    "ToolExecutionStartToolDescriptionMetaUIVisibility",
    "ToolSearchActivatedData",
    "ToolUserRequestedData",
    "UIEphemeralQueryPhase",
    "UiEphemeralQueryData",
    "UserInputCompletedData",
    "UserInputRequestedData",
    "UserMessageAgentMode",
    "UserMessageData",
    "UserMessageDelivery",
    "UserToolSessionApproval",
    "UserToolSessionApprovalCommands",
    "UserToolSessionApprovalCustomTool",
    "UserToolSessionApprovalExtensionEnvAccess",
    "UserToolSessionApprovalExtensionManagement",
    "UserToolSessionApprovalExtensionPermissionAccess",
    "UserToolSessionApprovalFactory",
    "UserToolSessionApprovalMcp",
    "UserToolSessionApprovalMemory",
    "UserToolSessionApprovalRead",
    "UserToolSessionApprovalWrite",
    "Verbosity",
    "WorkingDirectoryContext",
    "WorkingDirectoryContextHostType",
    "WorkspaceFileChangedOperation",
    "session_event_from_dict",
    "session_event_to_dict",
]
