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
    assert isinstance(x, int) and not isinstance(x, bool)
    return x


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
    assert milliseconds.is_integer()
    return int(milliseconds)


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
    SESSION_INFO = "session.info"
    SESSION_WARNING = "session.warning"
    SESSION_MODEL_CHANGE = "session.model_change"
    SESSION_MODE_CHANGED = "session.mode_changed"
    SESSION_PLAN_CHANGED = "session.plan_changed"
    SESSION_WORKSPACE_FILE_CHANGED = "session.workspace_file_changed"
    SESSION_HANDOFF = "session.handoff"
    SESSION_TRUNCATION = "session.truncation"
    SESSION_SNAPSHOT_REWIND = "session.snapshot_rewind"
    SESSION_SHUTDOWN = "session.shutdown"
    SESSION_CONTEXT_CHANGED = "session.context_changed"
    SESSION_USAGE_INFO = "session.usage_info"
    SESSION_COMPACTION_START = "session.compaction_start"
    SESSION_COMPACTION_COMPLETE = "session.compaction_complete"
    SESSION_TASK_COMPLETE = "session.task_complete"
    USER_MESSAGE = "user.message"
    PENDING_MESSAGES_MODIFIED = "pending_messages.modified"
    ASSISTANT_TURN_START = "assistant.turn_start"
    ASSISTANT_INTENT = "assistant.intent"
    ASSISTANT_REASONING = "assistant.reasoning"
    ASSISTANT_REASONING_DELTA = "assistant.reasoning_delta"
    ASSISTANT_STREAMING_DELTA = "assistant.streaming_delta"
    ASSISTANT_MESSAGE = "assistant.message"
    ASSISTANT_MESSAGE_START = "assistant.message_start"
    ASSISTANT_MESSAGE_DELTA = "assistant.message_delta"
    ASSISTANT_TURN_END = "assistant.turn_end"
    ASSISTANT_USAGE = "assistant.usage"
    MODEL_CALL_FAILURE = "model.call_failure"
    ABORT = "abort"
    TOOL_USER_REQUESTED = "tool.user_requested"
    TOOL_EXECUTION_START = "tool.execution_start"
    TOOL_EXECUTION_PARTIAL_RESULT = "tool.execution_partial_result"
    TOOL_EXECUTION_PROGRESS = "tool.execution_progress"
    TOOL_EXECUTION_COMPLETE = "tool.execution_complete"
    SKILL_INVOKED = "skill.invoked"
    SUBAGENT_STARTED = "subagent.started"
    SUBAGENT_COMPLETED = "subagent.completed"
    SUBAGENT_FAILED = "subagent.failed"
    SUBAGENT_SELECTED = "subagent.selected"
    SUBAGENT_DESELECTED = "subagent.deselected"
    HOOK_START = "hook.start"
    HOOK_END = "hook.end"
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
    SESSION_CUSTOM_NOTIFICATION = "session.custom_notification"
    EXTERNAL_TOOL_REQUESTED = "external_tool.requested"
    EXTERNAL_TOOL_COMPLETED = "external_tool.completed"
    COMMAND_QUEUED = "command.queued"
    COMMAND_EXECUTE = "command.execute"
    COMMAND_COMPLETED = "command.completed"
    AUTO_MODE_SWITCH_REQUESTED = "auto_mode_switch.requested"
    AUTO_MODE_SWITCH_COMPLETED = "auto_mode_switch.completed"
    COMMANDS_CHANGED = "commands.changed"
    CAPABILITIES_CHANGED = "capabilities.changed"
    EXIT_PLAN_MODE_REQUESTED = "exit_plan_mode.requested"
    EXIT_PLAN_MODE_COMPLETED = "exit_plan_mode.completed"
    SESSION_TOOLS_UPDATED = "session.tools_updated"
    SESSION_BACKGROUND_TASKS_CHANGED = "session.background_tasks_changed"
    SESSION_SKILLS_LOADED = "session.skills_loaded"
    SESSION_CUSTOM_AGENTS_UPDATED = "session.custom_agents_updated"
    SESSION_MCP_SERVERS_LOADED = "session.mcp_servers_loaded"
    SESSION_MCP_SERVER_STATUS_CHANGED = "session.mcp_server_status_changed"
    SESSION_EXTENSIONS_LOADED = "session.extensions_loaded"
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
        for key, value in self._values.items():
            setattr(self, key, value)

    @staticmethod
    def from_dict(obj: Any) -> "Data":
        assert isinstance(obj, dict)
        return Data(**{_compat_to_python_key(key): _compat_from_json_value(value) for key, value in obj.items()})

    def to_dict(self) -> dict:
        return {_compat_to_json_key(key): _compat_to_json_value(value) for key, value in self._values.items() if value is not None}


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
    # Experimental: this field is part of an experimental API and may change or be removed.
    anthropic_advisor_blocks: list[Any] | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    anthropic_advisor_model: str | None = None
    encrypted_content: str | None = None
    interaction_id: str | None = None
    model: str | None = None
    output_tokens: int | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    phase: str | None = None
    reasoning_opaque: str | None = None
    reasoning_text: str | None = None
    request_id: str | None = None
    service_request_id: str | None = None
    tool_requests: list[AssistantMessageToolRequest] | None = None
    turn_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        message_id = from_str(obj.get("messageId"))
        anthropic_advisor_blocks = from_union([from_none, lambda x: from_list(lambda x: x, x)], obj.get("anthropicAdvisorBlocks"))
        anthropic_advisor_model = from_union([from_none, from_str], obj.get("anthropicAdvisorModel"))
        encrypted_content = from_union([from_none, from_str], obj.get("encryptedContent"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        model = from_union([from_none, from_str], obj.get("model"))
        output_tokens = from_union([from_none, from_int], obj.get("outputTokens"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        phase = from_union([from_none, from_str], obj.get("phase"))
        reasoning_opaque = from_union([from_none, from_str], obj.get("reasoningOpaque"))
        reasoning_text = from_union([from_none, from_str], obj.get("reasoningText"))
        request_id = from_union([from_none, from_str], obj.get("requestId"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        tool_requests = from_union([from_none, lambda x: from_list(AssistantMessageToolRequest.from_dict, x)], obj.get("toolRequests"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return AssistantMessageData(
            content=content,
            message_id=message_id,
            anthropic_advisor_blocks=anthropic_advisor_blocks,
            anthropic_advisor_model=anthropic_advisor_model,
            encrypted_content=encrypted_content,
            interaction_id=interaction_id,
            model=model,
            output_tokens=output_tokens,
            parent_tool_call_id=parent_tool_call_id,
            phase=phase,
            reasoning_opaque=reasoning_opaque,
            reasoning_text=reasoning_text,
            request_id=request_id,
            service_request_id=service_request_id,
            tool_requests=tool_requests,
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["messageId"] = from_str(self.message_id)
        if self.anthropic_advisor_blocks is not None:
            result["anthropicAdvisorBlocks"] = from_union([from_none, lambda x: from_list(lambda x: x, x)], self.anthropic_advisor_blocks)
        if self.anthropic_advisor_model is not None:
            result["anthropicAdvisorModel"] = from_union([from_none, from_str], self.anthropic_advisor_model)
        if self.encrypted_content is not None:
            result["encryptedContent"] = from_union([from_none, from_str], self.encrypted_content)
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
        if self.reasoning_opaque is not None:
            result["reasoningOpaque"] = from_union([from_none, from_str], self.reasoning_opaque)
        if self.reasoning_text is not None:
            result["reasoningText"] = from_union([from_none, from_str], self.reasoning_text)
        if self.request_id is not None:
            result["requestId"] = from_union([from_none, from_str], self.request_id)
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
        intention_summary = from_union([from_none, from_str], obj.get("intentionSummary"))
        mcp_server_name = from_union([from_none, from_str], obj.get("mcpServerName"))
        mcp_tool_name = from_union([from_none, from_str], obj.get("mcpToolName"))
        tool_title = from_union([from_none, from_str], obj.get("toolTitle"))
        type = from_union([from_none, lambda x: parse_enum(AssistantMessageToolRequestType, x)], obj.get("type"))
        return AssistantMessageToolRequest(
            name=name,
            tool_call_id=tool_call_id,
            arguments=arguments,
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
class AssistantReasoningData:
    "Assistant reasoning content for timeline display with complete thinking text"
    content: str
    reasoning_id: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantReasoningData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        reasoning_id = from_str(obj.get("reasoningId"))
        return AssistantReasoningData(
            content=content,
            reasoning_id=reasoning_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["reasoningId"] = from_str(self.reasoning_id)
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
class AssistantTurnEndData:
    "Turn completion metadata including the turn identifier"
    turn_id: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantTurnEndData":
        assert isinstance(obj, dict)
        turn_id = from_str(obj.get("turnId"))
        return AssistantTurnEndData(
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["turnId"] = from_str(self.turn_id)
        return result


@dataclass
class AssistantTurnStartData:
    "Turn initialization metadata including identifier and interaction tracking"
    turn_id: str
    interaction_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantTurnStartData":
        assert isinstance(obj, dict)
        turn_id = from_str(obj.get("turnId"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        return AssistantTurnStartData(
            turn_id=turn_id,
            interaction_id=interaction_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["turnId"] = from_str(self.turn_id)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        return result


@dataclass
class _AssistantUsageCopilotUsage:
    "Per-request cost and usage data from the CAPI copilot_usage response field"
    token_details: list[AssistantUsageCopilotUsageTokenDetail]
    total_nano_aiu: float

    @staticmethod
    def from_dict(obj: Any) -> "_AssistantUsageCopilotUsage":
        assert isinstance(obj, dict)
        token_details = from_list(AssistantUsageCopilotUsageTokenDetail.from_dict, obj.get("tokenDetails"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        return _AssistantUsageCopilotUsage(
            token_details=token_details,
            total_nano_aiu=total_nano_aiu,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenDetails"] = from_list(lambda x: to_class(AssistantUsageCopilotUsageTokenDetail, x), self.token_details)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
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
    api_call_id: str | None = None
    api_endpoint: AssistantUsageApiEndpoint | None = None
    cache_read_tokens: int | None = None
    cache_write_tokens: int | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _copilot_usage: _AssistantUsageCopilotUsage | None = None
    # Experimental: this field is part of an experimental API and may change or be removed.
    cost: float | None = None
    duration: timedelta | None = None
    initiator: str | None = None
    input_tokens: int | None = None
    inter_token_latency: timedelta | None = None
    output_tokens: int | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    provider_call_id: str | None = None
    # Internal: this field is an internal SDK API and is not part of the public surface.
    _quota_snapshots: dict[str, _AssistantUsageQuotaSnapshot] | None = None
    reasoning_effort: str | None = None
    reasoning_tokens: int | None = None
    service_request_id: str | None = None
    time_to_first_token: timedelta | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageData":
        assert isinstance(obj, dict)
        model = from_str(obj.get("model"))
        api_call_id = from_union([from_none, from_str], obj.get("apiCallId"))
        api_endpoint = from_union([from_none, lambda x: parse_enum(AssistantUsageApiEndpoint, x)], obj.get("apiEndpoint"))
        cache_read_tokens = from_union([from_none, from_int], obj.get("cacheReadTokens"))
        cache_write_tokens = from_union([from_none, from_int], obj.get("cacheWriteTokens"))
        _copilot_usage = from_union([from_none, _AssistantUsageCopilotUsage.from_dict], obj.get("copilotUsage"))
        cost = from_union([from_none, from_float], obj.get("cost"))
        duration = from_union([from_none, from_timedelta], obj.get("duration"))
        initiator = from_union([from_none, from_str], obj.get("initiator"))
        input_tokens = from_union([from_none, from_int], obj.get("inputTokens"))
        inter_token_latency = from_union([from_none, from_timedelta], obj.get("interTokenLatencyMs"))
        output_tokens = from_union([from_none, from_int], obj.get("outputTokens"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        _quota_snapshots = from_union([from_none, lambda x: from_dict(_AssistantUsageQuotaSnapshot.from_dict, x)], obj.get("quotaSnapshots"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_tokens = from_union([from_none, from_int], obj.get("reasoningTokens"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        time_to_first_token = from_union([from_none, from_timedelta], obj.get("timeToFirstTokenMs"))
        return AssistantUsageData(
            model=model,
            api_call_id=api_call_id,
            api_endpoint=api_endpoint,
            cache_read_tokens=cache_read_tokens,
            cache_write_tokens=cache_write_tokens,
            _copilot_usage=_copilot_usage,
            cost=cost,
            duration=duration,
            initiator=initiator,
            input_tokens=input_tokens,
            inter_token_latency=inter_token_latency,
            output_tokens=output_tokens,
            parent_tool_call_id=parent_tool_call_id,
            provider_call_id=provider_call_id,
            _quota_snapshots=_quota_snapshots,
            reasoning_effort=reasoning_effort,
            reasoning_tokens=reasoning_tokens,
            service_request_id=service_request_id,
            time_to_first_token=time_to_first_token,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["model"] = from_str(self.model)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_none, from_str], self.api_call_id)
        if self.api_endpoint is not None:
            result["apiEndpoint"] = from_union([from_none, lambda x: to_enum(AssistantUsageApiEndpoint, x)], self.api_endpoint)
        if self.cache_read_tokens is not None:
            result["cacheReadTokens"] = from_union([from_none, to_int], self.cache_read_tokens)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([from_none, to_int], self.cache_write_tokens)
        if self._copilot_usage is not None:
            result["copilotUsage"] = from_union([from_none, lambda x: to_class(_AssistantUsageCopilotUsage, x)], self._copilot_usage)
        if self.cost is not None:
            result["cost"] = from_union([from_none, to_float], self.cost)
        if self.duration is not None:
            result["duration"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.initiator is not None:
            result["initiator"] = from_union([from_none, from_str], self.initiator)
        if self.input_tokens is not None:
            result["inputTokens"] = from_union([from_none, to_int], self.input_tokens)
        if self.inter_token_latency is not None:
            result["interTokenLatencyMs"] = from_union([from_none, to_timedelta], self.inter_token_latency)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([from_none, to_int], self.output_tokens)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self._quota_snapshots is not None:
            result["quotaSnapshots"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(_AssistantUsageQuotaSnapshot, x), x)], self._quota_snapshots)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_tokens is not None:
            result["reasoningTokens"] = from_union([from_none, to_int], self.reasoning_tokens)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.time_to_first_token is not None:
            result["timeToFirstTokenMs"] = from_union([from_none, to_timedelta_int], self.time_to_first_token)
        return result


@dataclass
class _AssistantUsageQuotaSnapshot:
    "Schema for the `_AssistantUsageQuotaSnapshot` type."
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
    _reset_date: datetime | None = None

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
        _reset_date = from_union([from_none, from_datetime], obj.get("resetDate"))
        return _AssistantUsageQuotaSnapshot(
            _entitlement_requests=_entitlement_requests,
            _is_unlimited_entitlement=_is_unlimited_entitlement,
            _overage=_overage,
            _overage_allowed_with_exhausted_quota=_overage_allowed_with_exhausted_quota,
            _remaining_percentage=_remaining_percentage,
            _usage_allowed_with_exhausted_quota=_usage_allowed_with_exhausted_quota,
            _used_requests=_used_requests,
            _reset_date=_reset_date,
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
        if self._reset_date is not None:
            result["resetDate"] = from_union([from_none, to_datetime], self._reset_date)
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
    elicitation: bool | None = None
    mcp_apps: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CapabilitiesChangedUI":
        assert isinstance(obj, dict)
        elicitation = from_union([from_none, from_bool], obj.get("elicitation"))
        mcp_apps = from_union([from_none, from_bool], obj.get("mcpApps"))
        return CapabilitiesChangedUI(
            elicitation=elicitation,
            mcp_apps=mcp_apps,
        )

    def to_dict(self) -> dict:
        result: dict = {}
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
    "Schema for the `CommandsChangedCommand` type."
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
    token_details: list[CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail]
    total_nano_aiu: float

    @staticmethod
    def from_dict(obj: Any) -> "_CompactionCompleteCompactionTokensUsedCopilotUsage":
        assert isinstance(obj, dict)
        token_details = from_list(CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail.from_dict, obj.get("tokenDetails"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        return _CompactionCompleteCompactionTokensUsedCopilotUsage(
            token_details=token_details,
            total_nano_aiu=total_nano_aiu,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenDetails"] = from_list(lambda x: to_class(CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail, x), self.token_details)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
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
    "Schema for the `CustomAgentsUpdatedAgent` type."
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
    "Schema for the `EmbeddedBlobResourceContents` type."
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
    "Schema for the `EmbeddedTextResourceContents` type."
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

    @staticmethod
    def from_dict(obj: Any) -> "ExitPlanModeRequestedData":
        assert isinstance(obj, dict)
        actions = from_list(lambda x: parse_enum(ExitPlanModeAction, x), obj.get("actions"))
        plan_content = from_str(obj.get("planContent"))
        recommended_action = parse_enum(ExitPlanModeAction, obj.get("recommendedAction"))
        request_id = from_str(obj.get("requestId"))
        summary = from_str(obj.get("summary"))
        return ExitPlanModeRequestedData(
            actions=actions,
            plan_content=plan_content,
            recommended_action=recommended_action,
            request_id=request_id,
            summary=summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["actions"] = from_list(lambda x: to_enum(ExitPlanModeAction, x), self.actions)
        result["planContent"] = from_str(self.plan_content)
        result["recommendedAction"] = to_enum(ExitPlanModeAction, self.recommended_action)
        result["requestId"] = from_str(self.request_id)
        result["summary"] = from_str(self.summary)
        return result


@dataclass
class ExtensionsLoadedExtension:
    "Schema for the `ExtensionsLoadedExtension` type."
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
    traceparent: str | None = None
    tracestate: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ExternalToolRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        session_id = from_str(obj.get("sessionId"))
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        arguments = obj.get("arguments")
        traceparent = from_union([from_none, from_str], obj.get("traceparent"))
        tracestate = from_union([from_none, from_str], obj.get("tracestate"))
        return ExternalToolRequestedData(
            request_id=request_id,
            session_id=session_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
            traceparent=traceparent,
            tracestate=tracestate,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["sessionId"] = from_str(self.session_id)
        result["toolCallId"] = from_str(self.tool_call_id)
        result["toolName"] = from_str(self.tool_name)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.traceparent is not None:
            result["traceparent"] = from_union([from_none, from_str], self.traceparent)
        if self.tracestate is not None:
            result["tracestate"] = from_union([from_none, from_str], self.tracestate)
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
class HookEndData:
    "Hook invocation completion details including output, success status, and error information"
    hook_invocation_id: str
    hook_type: str
    success: bool
    error: HookEndError | None = None
    output: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "HookEndData":
        assert isinstance(obj, dict)
        hook_invocation_id = from_str(obj.get("hookInvocationId"))
        hook_type = from_str(obj.get("hookType"))
        success = from_bool(obj.get("success"))
        error = from_union([from_none, HookEndError.from_dict], obj.get("error"))
        output = obj.get("output")
        return HookEndData(
            hook_invocation_id=hook_invocation_id,
            hook_type=hook_type,
            success=success,
            error=error,
            output=output,
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
        return result


@dataclass
class HookEndError:
    "Error details when the hook failed"
    message: str
    stack: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookEndError":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        stack = from_union([from_none, from_str], obj.get("stack"))
        return HookEndError(
            message=message,
            stack=stack,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = from_str(self.message)
        if self.stack is not None:
            result["stack"] = from_union([from_none, from_str], self.stack)
        return result


@dataclass
class HookStartData:
    "Hook invocation start details including type and input data"
    hook_invocation_id: str
    hook_type: str
    input: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "HookStartData":
        assert isinstance(obj, dict)
        hook_invocation_id = from_str(obj.get("hookInvocationId"))
        hook_type = from_str(obj.get("hookType"))
        input = obj.get("input")
        return HookStartData(
            hook_invocation_id=hook_invocation_id,
            hook_type=hook_type,
            input=input,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["hookInvocationId"] = from_str(self.hook_invocation_id)
        result["hookType"] = from_str(self.hook_type)
        if self.input is not None:
            result["input"] = self.input
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
    "Schema for the `McpAppToolCallCompleteToolMetaUI` type."
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
class McpOauthCompletedData:
    "MCP OAuth request completion notification"
    request_id: str

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        return McpOauthCompletedData(
            request_id=request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        return result


@dataclass
class McpOauthRequiredData:
    "OAuth authentication request for an MCP server"
    request_id: str
    server_name: str
    server_url: str
    static_client_config: McpOauthRequiredStaticClientConfig | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthRequiredData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        server_url = from_str(obj.get("serverUrl"))
        static_client_config = from_union([from_none, McpOauthRequiredStaticClientConfig.from_dict], obj.get("staticClientConfig"))
        return McpOauthRequiredData(
            request_id=request_id,
            server_name=server_name,
            server_url=server_url,
            static_client_config=static_client_config,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["serverName"] = from_str(self.server_name)
        result["serverUrl"] = from_str(self.server_url)
        if self.static_client_config is not None:
            result["staticClientConfig"] = from_union([from_none, lambda x: to_class(McpOauthRequiredStaticClientConfig, x)], self.static_client_config)
        return result


@dataclass
class McpOauthRequiredStaticClientConfig:
    "Static OAuth client configuration, if the server specifies one"
    client_id: str
    grant_type: str | None = None
    public_client: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthRequiredStaticClientConfig":
        assert isinstance(obj, dict)
        client_id = from_str(obj.get("clientId"))
        grant_type = from_union([from_none, from_str], obj.get("grantType"))
        public_client = from_union([from_none, from_bool], obj.get("publicClient"))
        return McpOauthRequiredStaticClientConfig(
            client_id=client_id,
            grant_type=grant_type,
            public_client=public_client,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["clientId"] = from_str(self.client_id)
        if self.grant_type is not None:
            result["grantType"] = from_union([from_none, from_str], self.grant_type)
        if self.public_client is not None:
            result["publicClient"] = from_union([from_none, from_bool], self.public_client)
        return result


@dataclass
class McpServersLoadedServer:
    "Schema for the `McpServersLoadedServer` type."
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
class ModelCallFailureData:
    "Failed LLM API call metadata for telemetry"
    source: ModelCallFailureSource
    api_call_id: str | None = None
    duration: timedelta | None = None
    error_message: str | None = None
    initiator: str | None = None
    model: str | None = None
    provider_call_id: str | None = None
    service_request_id: str | None = None
    status_code: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ModelCallFailureData":
        assert isinstance(obj, dict)
        source = parse_enum(ModelCallFailureSource, obj.get("source"))
        api_call_id = from_union([from_none, from_str], obj.get("apiCallId"))
        duration = from_union([from_none, from_timedelta], obj.get("durationMs"))
        error_message = from_union([from_none, from_str], obj.get("errorMessage"))
        initiator = from_union([from_none, from_str], obj.get("initiator"))
        model = from_union([from_none, from_str], obj.get("model"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        service_request_id = from_union([from_none, from_str], obj.get("serviceRequestId"))
        status_code = from_union([from_none, from_int], obj.get("statusCode"))
        return ModelCallFailureData(
            source=source,
            api_call_id=api_call_id,
            duration=duration,
            error_message=error_message,
            initiator=initiator,
            model=model,
            provider_call_id=provider_call_id,
            service_request_id=service_request_id,
            status_code=status_code,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["source"] = to_enum(ModelCallFailureSource, self.source)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_none, from_str], self.api_call_id)
        if self.duration is not None:
            result["durationMs"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.error_message is not None:
            result["errorMessage"] = from_union([from_none, from_str], self.error_message)
        if self.initiator is not None:
            result["initiator"] = from_union([from_none, from_str], self.initiator)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self.service_request_id is not None:
            result["serviceRequestId"] = from_union([from_none, from_str], self.service_request_id)
        if self.status_code is not None:
            result["statusCode"] = from_union([from_none, to_int], self.status_code)
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
    "Schema for the `PermissionApproved` type."
    kind: ClassVar[str] = "approved"

    @staticmethod
    def from_dict(obj: Any) -> "PermissionApproved":
        assert isinstance(obj, dict)
        return PermissionApproved(
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        return result


@dataclass
class PermissionApprovedForLocation:
    "Schema for the `PermissionApprovedForLocation` type."
    approval: UserToolSessionApproval
    kind: ClassVar[str] = "approved-for-location"
    location_key: str

    @staticmethod
    def from_dict(obj: Any) -> "PermissionApprovedForLocation":
        assert isinstance(obj, dict)
        approval = _load_UserToolSessionApproval(obj.get("approval"))
        location_key = from_str(obj.get("locationKey"))
        return PermissionApprovedForLocation(
            approval=approval,
            location_key=location_key,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = self.approval.to_dict()
        result["kind"] = self.kind
        result["locationKey"] = from_str(self.location_key)
        return result


@dataclass
class PermissionApprovedForSession:
    "Schema for the `PermissionApprovedForSession` type."
    approval: UserToolSessionApproval
    kind: ClassVar[str] = "approved-for-session"

    @staticmethod
    def from_dict(obj: Any) -> "PermissionApprovedForSession":
        assert isinstance(obj, dict)
        approval = _load_UserToolSessionApproval(obj.get("approval"))
        return PermissionApprovedForSession(
            approval=approval,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["approval"] = self.approval.to_dict()
        result["kind"] = self.kind
        return result


@dataclass
class PermissionCancelled:
    "Schema for the `PermissionCancelled` type."
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
    "Schema for the `PermissionDeniedByContentExclusionPolicy` type."
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
    "Schema for the `PermissionDeniedByPermissionRequestHook` type."
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
    "Schema for the `PermissionDeniedByRules` type."
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
    "Schema for the `PermissionDeniedInteractivelyByUser` type."
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
    "Schema for the `PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type."
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
    tool_call_id: str | None = None
    warning: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestCommands":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        command_identifiers = from_list(from_str, obj.get("commandIdentifiers"))
        full_command_text = from_str(obj.get("fullCommandText"))
        intention = from_str(obj.get("intention"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        warning = from_union([from_none, from_str], obj.get("warning"))
        return PermissionPromptRequestCommands(
            can_offer_session_approval=can_offer_session_approval,
            command_identifiers=command_identifiers,
            full_command_text=full_command_text,
            intention=intention,
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
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestCustomTool":
        assert isinstance(obj, dict)
        tool_description = from_str(obj.get("toolDescription"))
        tool_name = from_str(obj.get("toolName"))
        args = obj.get("args")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestCustomTool(
            tool_description=tool_description,
            tool_name=tool_name,
            args=args,
            tool_call_id=tool_call_id,
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
        return result


@dataclass
class PermissionPromptRequestExtensionManagement:
    "Extension management permission prompt"
    kind: ClassVar[str] = "extension-management"
    operation: str
    extension_name: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestExtensionManagement":
        assert isinstance(obj, dict)
        operation = from_str(obj.get("operation"))
        extension_name = from_union([from_none, from_str], obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestExtensionManagement(
            operation=operation,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["operation"] = from_str(self.operation)
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
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestExtensionPermissionAccess":
        assert isinstance(obj, dict)
        capabilities = from_list(from_str, obj.get("capabilities"))
        extension_name = from_str(obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestExtensionPermissionAccess(
            capabilities=capabilities,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = from_list(from_str, self.capabilities)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestHook:
    "Hook confirmation permission prompt"
    kind: ClassVar[str] = "hook"
    tool_name: str
    hook_message: str | None = None
    tool_args: Any = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestHook":
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        hook_message = from_union([from_none, from_str], obj.get("hookMessage"))
        tool_args = obj.get("toolArgs")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestHook(
            tool_name=tool_name,
            hook_message=hook_message,
            tool_args=tool_args,
            tool_call_id=tool_call_id,
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
        return result


@dataclass
class PermissionPromptRequestMcp:
    "MCP tool invocation permission prompt"
    kind: ClassVar[str] = "mcp"
    server_name: str
    tool_name: str
    tool_title: str
    args: Any | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestMcp":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        tool_name = from_str(obj.get("toolName"))
        tool_title = from_str(obj.get("toolTitle"))
        args = from_union([from_none, lambda x: x], obj.get("args"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestMcp(
            server_name=server_name,
            tool_name=tool_name,
            tool_title=tool_title,
            args=args,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["serverName"] = from_str(self.server_name)
        result["toolName"] = from_str(self.tool_name)
        result["toolTitle"] = from_str(self.tool_title)
        if self.args is not None:
            result["args"] = from_union([from_none, lambda x: x], self.args)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestMemory:
    "Memory operation permission prompt"
    fact: str
    kind: ClassVar[str] = "memory"
    action: PermissionRequestMemoryAction | None = None
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
        citations = from_union([from_none, from_str], obj.get("citations"))
        direction = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryDirection, x)], obj.get("direction"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        subject = from_union([from_none, from_str], obj.get("subject"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestMemory(
            fact=fact,
            action=action,
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
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestPath":
        assert isinstance(obj, dict)
        access_kind = parse_enum(PermissionPromptRequestPathAccessKind, obj.get("accessKind"))
        paths = from_list(from_str, obj.get("paths"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestPath(
            access_kind=access_kind,
            paths=paths,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["accessKind"] = to_enum(PermissionPromptRequestPathAccessKind, self.access_kind)
        result["kind"] = self.kind
        result["paths"] = from_list(from_str, self.paths)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestRead:
    "File read permission prompt"
    intention: str
    kind: ClassVar[str] = "read"
    path: str
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestRead":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        path = from_str(obj.get("path"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestRead(
            intention=intention,
            path=path,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["path"] = from_str(self.path)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionPromptRequestUrl:
    "URL access permission prompt"
    intention: str
    kind: ClassVar[str] = "url"
    url: str
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestUrl":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        url = from_str(obj.get("url"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestUrl(
            intention=intention,
            url=url,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["url"] = from_str(self.url)
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
    new_file_contents: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionPromptRequestWrite":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        diff = from_str(obj.get("diff"))
        file_name = from_str(obj.get("fileName"))
        intention = from_str(obj.get("intention"))
        new_file_contents = from_union([from_none, from_str], obj.get("newFileContents"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionPromptRequestWrite(
            can_offer_session_approval=can_offer_session_approval,
            diff=diff,
            file_name=file_name,
            intention=intention,
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

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestCustomTool":
        assert isinstance(obj, dict)
        tool_description = from_str(obj.get("toolDescription"))
        tool_name = from_str(obj.get("toolName"))
        args = obj.get("args")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestCustomTool(
            tool_description=tool_description,
            tool_name=tool_name,
            args=args,
            tool_call_id=tool_call_id,
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
        return result


@dataclass
class PermissionRequestExtensionManagement:
    "Extension management permission request"
    kind: ClassVar[str] = "extension-management"
    operation: str
    extension_name: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestExtensionManagement":
        assert isinstance(obj, dict)
        operation = from_str(obj.get("operation"))
        extension_name = from_union([from_none, from_str], obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestExtensionManagement(
            operation=operation,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = self.kind
        result["operation"] = from_str(self.operation)
        if self.extension_name is not None:
            result["extensionName"] = from_union([from_none, from_str], self.extension_name)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestExtensionPermissionAccess:
    "Extension permission access request"
    capabilities: list[str]
    extension_name: str
    kind: ClassVar[str] = "extension-permission-access"
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestExtensionPermissionAccess":
        assert isinstance(obj, dict)
        capabilities = from_list(from_str, obj.get("capabilities"))
        extension_name = from_str(obj.get("extensionName"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestExtensionPermissionAccess(
            capabilities=capabilities,
            extension_name=extension_name,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["capabilities"] = from_list(from_str, self.capabilities)
        result["extensionName"] = from_str(self.extension_name)
        result["kind"] = self.kind
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestHook:
    "Hook confirmation permission request"
    kind: ClassVar[str] = "hook"
    tool_name: str
    hook_message: str | None = None
    tool_args: Any = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestHook":
        assert isinstance(obj, dict)
        tool_name = from_str(obj.get("toolName"))
        hook_message = from_union([from_none, from_str], obj.get("hookMessage"))
        tool_args = obj.get("toolArgs")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestHook(
            tool_name=tool_name,
            hook_message=hook_message,
            tool_args=tool_args,
            tool_call_id=tool_call_id,
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
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestMcp":
        assert isinstance(obj, dict)
        read_only = from_bool(obj.get("readOnly"))
        server_name = from_str(obj.get("serverName"))
        tool_name = from_str(obj.get("toolName"))
        tool_title = from_str(obj.get("toolTitle"))
        args = obj.get("args")
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestMcp(
            read_only=read_only,
            server_name=server_name,
            tool_name=tool_name,
            tool_title=tool_title,
            args=args,
            tool_call_id=tool_call_id,
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
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        return result


@dataclass
class PermissionRequestMemory:
    "Memory operation permission request"
    fact: str
    kind: ClassVar[str] = "memory"
    action: PermissionRequestMemoryAction | None = None
    citations: str | None = None
    direction: PermissionRequestMemoryDirection | None = None
    reason: str | None = None
    subject: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestMemory":
        assert isinstance(obj, dict)
        fact = from_str(obj.get("fact"))
        action = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryAction, x)], obj.get("action"))
        citations = from_union([from_none, from_str], obj.get("citations"))
        direction = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryDirection, x)], obj.get("direction"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        subject = from_union([from_none, from_str], obj.get("subject"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestMemory(
            fact=fact,
            action=action,
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
class PermissionRequestRead:
    "File or directory read permission request"
    intention: str
    kind: ClassVar[str] = "read"
    path: str
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestRead":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        path = from_str(obj.get("path"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestRead(
            intention=intention,
            path=path,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["path"] = from_str(self.path)
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
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.warning is not None:
            result["warning"] = from_union([from_none, from_str], self.warning)
        return result


@dataclass
class PermissionRequestShellCommand:
    "Schema for the `PermissionRequestShellCommand` type."
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
class PermissionRequestShellPossibleUrl:
    "Schema for the `PermissionRequestShellPossibleUrl` type."
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
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestUrl":
        assert isinstance(obj, dict)
        intention = from_str(obj.get("intention"))
        url = from_str(obj.get("url"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestUrl(
            intention=intention,
            url=url,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["intention"] = from_str(self.intention)
        result["kind"] = self.kind
        result["url"] = from_str(self.url)
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
    new_file_contents: str | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestWrite":
        assert isinstance(obj, dict)
        can_offer_session_approval = from_bool(obj.get("canOfferSessionApproval"))
        diff = from_str(obj.get("diff"))
        file_name = from_str(obj.get("fileName"))
        intention = from_str(obj.get("intention"))
        new_file_contents = from_union([from_none, from_str], obj.get("newFileContents"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return PermissionRequestWrite(
            can_offer_session_approval=can_offer_session_approval,
            diff=diff,
            file_name=file_name,
            intention=intention,
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
        if self.new_file_contents is not None:
            result["newFileContents"] = from_union([from_none, from_str], self.new_file_contents)
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

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestedData":
        assert isinstance(obj, dict)
        permission_request = _load_PermissionRequest(obj.get("permissionRequest"))
        request_id = from_str(obj.get("requestId"))
        prompt_request = from_union([from_none, _load_PermissionPromptRequest], obj.get("promptRequest"))
        resolved_by_hook = from_union([from_none, from_bool], obj.get("resolvedByHook"))
        return PermissionRequestedData(
            permission_request=permission_request,
            request_id=request_id,
            prompt_request=prompt_request,
            resolved_by_hook=resolved_by_hook,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["permissionRequest"] = self.permission_request.to_dict()
        result["requestId"] = from_str(self.request_id)
        if self.prompt_request is not None:
            result["promptRequest"] = from_union([from_none, lambda x: x.to_dict()], self.prompt_request)
        if self.resolved_by_hook is not None:
            result["resolvedByHook"] = from_union([from_none, from_bool], self.resolved_by_hook)
        return result


@dataclass
class PermissionRule:
    "Schema for the `PermissionRule` type."
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
class SessionBackgroundTasksChangedData:
    "Schema for the `BackgroundTasksChangedData` type."
    @staticmethod
    def from_dict(obj: Any) -> "SessionBackgroundTasksChangedData":
        assert isinstance(obj, dict)
        return SessionBackgroundTasksChangedData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class SessionCompactionCompleteData:
    "Conversation compaction results including success status, metrics, and optional error details"
    success: bool
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
    summary_content: str | None = None
    system_tokens: int | None = None
    tokens_removed: int | None = None
    tool_definitions_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCompactionCompleteData":
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
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
        summary_content = from_union([from_none, from_str], obj.get("summaryContent"))
        system_tokens = from_union([from_none, from_int], obj.get("systemTokens"))
        tokens_removed = from_union([from_none, from_int], obj.get("tokensRemoved"))
        tool_definitions_tokens = from_union([from_none, from_int], obj.get("toolDefinitionsTokens"))
        return SessionCompactionCompleteData(
            success=success,
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
            summary_content=summary_content,
            system_tokens=system_tokens,
            tokens_removed=tokens_removed,
            tool_definitions_tokens=tool_definitions_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
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
        if self.summary_content is not None:
            result["summaryContent"] = from_union([from_none, from_str], self.summary_content)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_int], self.system_tokens)
        if self.tokens_removed is not None:
            result["tokensRemoved"] = from_union([from_none, to_int], self.tokens_removed)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_int], self.tool_definitions_tokens)
        return result


@dataclass
class SessionCompactionStartData:
    "Context window breakdown at the start of LLM-powered conversation compaction"
    conversation_tokens: int | None = None
    system_tokens: int | None = None
    tool_definitions_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCompactionStartData":
        assert isinstance(obj, dict)
        conversation_tokens = from_union([from_none, from_int], obj.get("conversationTokens"))
        system_tokens = from_union([from_none, from_int], obj.get("systemTokens"))
        tool_definitions_tokens = from_union([from_none, from_int], obj.get("toolDefinitionsTokens"))
        return SessionCompactionStartData(
            conversation_tokens=conversation_tokens,
            system_tokens=system_tokens,
            tool_definitions_tokens=tool_definitions_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_int], self.conversation_tokens)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_int], self.system_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_int], self.tool_definitions_tokens)
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
        repository = from_union([from_none, from_str], obj.get("repository"))
        repository_host = from_union([from_none, from_str], obj.get("repositoryHost"))
        return SessionContextChangedData(
            cwd=cwd,
            base_commit=base_commit,
            branch=branch,
            git_root=git_root,
            head_commit=head_commit,
            host_type=host_type,
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
        if self.repository is not None:
            result["repository"] = from_union([from_none, from_str], self.repository)
        if self.repository_host is not None:
            result["repositoryHost"] = from_union([from_none, from_str], self.repository_host)
        return result


@dataclass
class SessionCustomAgentsUpdatedData:
    "Schema for the `CustomAgentsUpdatedData` type."
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
class SessionExtensionsLoadedData:
    "Schema for the `ExtensionsLoadedData` type."
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
    "Payload indicating the session is idle with no background agents in flight"
    aborted: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionIdleData":
        assert isinstance(obj, dict)
        aborted = from_union([from_none, from_bool], obj.get("aborted"))
        return SessionIdleData(
            aborted=aborted,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.aborted is not None:
            result["aborted"] = from_union([from_none, from_bool], self.aborted)
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
class SessionMcpServerStatusChangedData:
    "Schema for the `McpServerStatusChangedData` type."
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
    "Schema for the `McpServersLoadedData` type."
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
    context_tier: SessionModelChangeDataContextTier | None = None
    previous_model: str | None = None
    previous_reasoning_effort: str | None = None
    previous_reasoning_summary: ReasoningSummary | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionModelChangeData":
        assert isinstance(obj, dict)
        new_model = from_str(obj.get("newModel"))
        cause = from_union([from_none, from_str], obj.get("cause"))
        context_tier = from_union([from_none, lambda x: parse_enum(SessionModelChangeDataContextTier, x)], obj.get("contextTier"))
        previous_model = from_union([from_none, from_str], obj.get("previousModel"))
        previous_reasoning_effort = from_union([from_none, from_str], obj.get("previousReasoningEffort"))
        previous_reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("previousReasoningSummary"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        return SessionModelChangeData(
            new_model=new_model,
            cause=cause,
            context_tier=context_tier,
            previous_model=previous_model,
            previous_reasoning_effort=previous_reasoning_effort,
            previous_reasoning_summary=previous_reasoning_summary,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["newModel"] = from_str(self.new_model)
        if self.cause is not None:
            result["cause"] = from_union([from_none, from_str], self.cause)
        if self.context_tier is not None:
            result["contextTier"] = from_union([from_none, lambda x: to_enum(SessionModelChangeDataContextTier, x)], self.context_tier)
        if self.previous_model is not None:
            result["previousModel"] = from_union([from_none, from_str], self.previous_model)
        if self.previous_reasoning_effort is not None:
            result["previousReasoningEffort"] = from_union([from_none, from_str], self.previous_reasoning_effort)
        if self.previous_reasoning_summary is not None:
            result["previousReasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.previous_reasoning_summary)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
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
    context: WorkingDirectoryContext | None = None
    continue_pending_work: bool | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None
    remote_steerable: bool | None = None
    selected_model: str | None = None
    session_was_active: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionResumeData":
        assert isinstance(obj, dict)
        event_count = from_int(obj.get("eventCount"))
        resume_time = from_datetime(obj.get("resumeTime"))
        already_in_use = from_union([from_none, from_bool], obj.get("alreadyInUse"))
        context = from_union([from_none, WorkingDirectoryContext.from_dict], obj.get("context"))
        continue_pending_work = from_union([from_none, from_bool], obj.get("continuePendingWork"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        remote_steerable = from_union([from_none, from_bool], obj.get("remoteSteerable"))
        selected_model = from_union([from_none, from_str], obj.get("selectedModel"))
        session_was_active = from_union([from_none, from_bool], obj.get("sessionWasActive"))
        return SessionResumeData(
            event_count=event_count,
            resume_time=resume_time,
            already_in_use=already_in_use,
            context=context,
            continue_pending_work=continue_pending_work,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
            remote_steerable=remote_steerable,
            selected_model=selected_model,
            session_was_active=session_was_active,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["eventCount"] = to_int(self.event_count)
        result["resumeTime"] = to_datetime(self.resume_time)
        if self.already_in_use is not None:
            result["alreadyInUse"] = from_union([from_none, from_bool], self.already_in_use)
        if self.context is not None:
            result["context"] = from_union([from_none, lambda x: to_class(WorkingDirectoryContext, x)], self.context)
        if self.continue_pending_work is not None:
            result["continuePendingWork"] = from_union([from_none, from_bool], self.continue_pending_work)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_none, from_bool], self.remote_steerable)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_none, from_str], self.selected_model)
        if self.session_was_active is not None:
            result["sessionWasActive"] = from_union([from_none, from_bool], self.session_was_active)
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
    interval: timedelta
    prompt: str
    display_prompt: str | None = None
    recurring: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionScheduleCreatedData":
        assert isinstance(obj, dict)
        id = from_int(obj.get("id"))
        interval = from_timedelta(obj.get("intervalMs"))
        prompt = from_str(obj.get("prompt"))
        display_prompt = from_union([from_none, from_str], obj.get("displayPrompt"))
        recurring = from_union([from_none, from_bool], obj.get("recurring"))
        return SessionScheduleCreatedData(
            id=id,
            interval=interval,
            prompt=prompt,
            display_prompt=display_prompt,
            recurring=recurring,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = to_int(self.id)
        result["intervalMs"] = to_timedelta_int(self.interval)
        result["prompt"] = from_str(self.prompt)
        if self.display_prompt is not None:
            result["displayPrompt"] = from_union([from_none, from_str], self.display_prompt)
        if self.recurring is not None:
            result["recurring"] = from_union([from_none, from_bool], self.recurring)
        return result


@dataclass
class SessionShutdownData:
    "Session termination metrics including usage statistics, code changes, and shutdown reason"
    code_changes: ShutdownCodeChanges
    model_metrics: dict[str, ShutdownModelMetric]
    session_start_time: int
    shutdown_type: ShutdownType
    total_api_duration: timedelta
    conversation_tokens: int | None = None
    current_model: str | None = None
    current_tokens: int | None = None
    error_reason: str | None = None
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
        conversation_tokens = from_union([from_none, from_int], obj.get("conversationTokens"))
        current_model = from_union([from_none, from_str], obj.get("currentModel"))
        current_tokens = from_union([from_none, from_int], obj.get("currentTokens"))
        error_reason = from_union([from_none, from_str], obj.get("errorReason"))
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
            conversation_tokens=conversation_tokens,
            current_model=current_model,
            current_tokens=current_tokens,
            error_reason=error_reason,
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
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_int], self.conversation_tokens)
        if self.current_model is not None:
            result["currentModel"] = from_union([from_none, from_str], self.current_model)
        if self.current_tokens is not None:
            result["currentTokens"] = from_union([from_none, to_int], self.current_tokens)
        if self.error_reason is not None:
            result["errorReason"] = from_union([from_none, from_str], self.error_reason)
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
    "Schema for the `SkillsLoadedData` type."
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
    context: WorkingDirectoryContext | None = None
    detached_from_spawning_parent_session_id: str | None = None
    reasoning_effort: str | None = None
    reasoning_summary: ReasoningSummary | None = None
    remote_steerable: bool | None = None
    selected_model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionStartData":
        assert isinstance(obj, dict)
        copilot_version = from_str(obj.get("copilotVersion"))
        producer = from_str(obj.get("producer"))
        session_id = from_str(obj.get("sessionId"))
        start_time = from_datetime(obj.get("startTime"))
        version = from_int(obj.get("version"))
        already_in_use = from_union([from_none, from_bool], obj.get("alreadyInUse"))
        context = from_union([from_none, WorkingDirectoryContext.from_dict], obj.get("context"))
        detached_from_spawning_parent_session_id = from_union([from_none, from_str], obj.get("detachedFromSpawningParentSessionId"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        reasoning_summary = from_union([from_none, lambda x: parse_enum(ReasoningSummary, x)], obj.get("reasoningSummary"))
        remote_steerable = from_union([from_none, from_bool], obj.get("remoteSteerable"))
        selected_model = from_union([from_none, from_str], obj.get("selectedModel"))
        return SessionStartData(
            copilot_version=copilot_version,
            producer=producer,
            session_id=session_id,
            start_time=start_time,
            version=version,
            already_in_use=already_in_use,
            context=context,
            detached_from_spawning_parent_session_id=detached_from_spawning_parent_session_id,
            reasoning_effort=reasoning_effort,
            reasoning_summary=reasoning_summary,
            remote_steerable=remote_steerable,
            selected_model=selected_model,
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
        if self.context is not None:
            result["context"] = from_union([from_none, lambda x: to_class(WorkingDirectoryContext, x)], self.context)
        if self.detached_from_spawning_parent_session_id is not None:
            result["detachedFromSpawningParentSessionId"] = from_union([from_none, from_str], self.detached_from_spawning_parent_session_id)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.reasoning_summary is not None:
            result["reasoningSummary"] = from_union([from_none, lambda x: to_enum(ReasoningSummary, x)], self.reasoning_summary)
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_none, from_bool], self.remote_steerable)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_none, from_str], self.selected_model)
        return result


@dataclass
class SessionTaskCompleteData:
    "Task completion notification with summary from the agent"
    success: bool | None = None
    summary: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionTaskCompleteData":
        assert isinstance(obj, dict)
        success = from_union([from_none, from_bool], obj.get("success"))
        summary = from_union([from_none, from_str], obj.get("summary"))
        return SessionTaskCompleteData(
            success=success,
            summary=summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
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
class SessionToolsUpdatedData:
    "Schema for the `ToolsUpdatedData` type."
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
    "Schema for the `ShutdownModelMetric` type."
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
    "Schema for the `ShutdownModelMetricTokenDetail` type."
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
    "Schema for the `ShutdownTokenDetail` type."
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
    "Schema for the `SkillsLoadedSkill` type."
    description: str
    enabled: bool
    name: str
    source: SkillSource
    user_invocable: bool
    path: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SkillsLoadedSkill":
        assert isinstance(obj, dict)
        description = from_str(obj.get("description"))
        enabled = from_bool(obj.get("enabled"))
        name = from_str(obj.get("name"))
        source = parse_enum(SkillSource, obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        path = from_union([from_none, from_str], obj.get("path"))
        return SkillsLoadedSkill(
            description=description,
            enabled=enabled,
            name=name,
            source=source,
            user_invocable=user_invocable,
            path=path,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["description"] = from_str(self.description)
        result["enabled"] = from_bool(self.enabled)
        result["name"] = from_str(self.name)
        result["source"] = to_enum(SkillSource, self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        return result


@dataclass
class SubagentCompletedData:
    "Sub-agent completion details for successful execution"
    agent_display_name: str
    agent_name: str
    tool_call_id: str
    duration: timedelta | None = None
    model: str | None = None
    total_tokens: int | None = None
    total_tool_calls: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentCompletedData":
        assert isinstance(obj, dict)
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_name = from_str(obj.get("agentName"))
        tool_call_id = from_str(obj.get("toolCallId"))
        duration = from_union([from_none, from_timedelta], obj.get("durationMs"))
        model = from_union([from_none, from_str], obj.get("model"))
        total_tokens = from_union([from_none, from_int], obj.get("totalTokens"))
        total_tool_calls = from_union([from_none, from_int], obj.get("totalToolCalls"))
        return SubagentCompletedData(
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            tool_call_id=tool_call_id,
            duration=duration,
            model=model,
            total_tokens=total_tokens,
            total_tool_calls=total_tool_calls,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentName"] = from_str(self.agent_name)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.duration is not None:
            result["durationMs"] = from_union([from_none, to_timedelta_int], self.duration)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.total_tokens is not None:
            result["totalTokens"] = from_union([from_none, to_int], self.total_tokens)
        if self.total_tool_calls is not None:
            result["totalToolCalls"] = from_union([from_none, to_int], self.total_tool_calls)
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
    duration: timedelta | None = None
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
        duration = from_union([from_none, from_timedelta], obj.get("durationMs"))
        model = from_union([from_none, from_str], obj.get("model"))
        total_tokens = from_union([from_none, from_int], obj.get("totalTokens"))
        total_tool_calls = from_union([from_none, from_int], obj.get("totalToolCalls"))
        return SubagentFailedData(
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            error=error,
            tool_call_id=tool_call_id,
            duration=duration,
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
        if self.duration is not None:
            result["durationMs"] = from_union([from_none, to_timedelta_int], self.duration)
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
    model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentStartedData":
        assert isinstance(obj, dict)
        agent_description = from_str(obj.get("agentDescription"))
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_name = from_str(obj.get("agentName"))
        tool_call_id = from_str(obj.get("toolCallId"))
        model = from_union([from_none, from_str], obj.get("model"))
        return SubagentStartedData(
            agent_description=agent_description,
            agent_display_name=agent_display_name,
            agent_name=agent_name,
            tool_call_id=tool_call_id,
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentDescription"] = from_str(self.agent_description)
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentName"] = from_str(self.agent_name)
        result["toolCallId"] = from_str(self.tool_call_id)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        return result


@dataclass
class SystemMessageData:
    "System/developer instruction content with role and optional template metadata"
    content: str
    role: SystemMessageRole
    metadata: SystemMessageMetadata | None = None
    name: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        role = parse_enum(SystemMessageRole, obj.get("role"))
        metadata = from_union([from_none, SystemMessageMetadata.from_dict], obj.get("metadata"))
        name = from_union([from_none, from_str], obj.get("name"))
        return SystemMessageData(
            content=content,
            role=role,
            metadata=metadata,
            name=name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["role"] = to_enum(SystemMessageRole, self.role)
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
    "Schema for the `SystemNotificationAgentCompleted` type."
    agent_id: str
    agent_type: str
    status: SystemNotificationAgentCompletedStatus
    type: ClassVar[str] = "agent_completed"
    description: str | None = None
    prompt: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationAgentCompleted":
        assert isinstance(obj, dict)
        agent_id = from_str(obj.get("agentId"))
        agent_type = from_str(obj.get("agentType"))
        status = parse_enum(SystemNotificationAgentCompletedStatus, obj.get("status"))
        description = from_union([from_none, from_str], obj.get("description"))
        prompt = from_union([from_none, from_str], obj.get("prompt"))
        return SystemNotificationAgentCompleted(
            agent_id=agent_id,
            agent_type=agent_type,
            status=status,
            description=description,
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
        if self.prompt is not None:
            result["prompt"] = from_union([from_none, from_str], self.prompt)
        return result


@dataclass
class SystemNotificationAgentIdle:
    "Schema for the `SystemNotificationAgentIdle` type."
    agent_id: str
    agent_type: str
    type: ClassVar[str] = "agent_idle"
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationAgentIdle":
        assert isinstance(obj, dict)
        agent_id = from_str(obj.get("agentId"))
        agent_type = from_str(obj.get("agentType"))
        description = from_union([from_none, from_str], obj.get("description"))
        return SystemNotificationAgentIdle(
            agent_id=agent_id,
            agent_type=agent_type,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentId"] = from_str(self.agent_id)
        result["agentType"] = from_str(self.agent_type)
        result["type"] = self.type
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
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
class SystemNotificationInstructionDiscovered:
    "Schema for the `SystemNotificationInstructionDiscovered` type."
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
    "Schema for the `SystemNotificationNewInboxMessage` type."
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
    "Schema for the `SystemNotificationShellCompleted` type."
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
    "Schema for the `SystemNotificationShellDetachedCompleted` type."
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
class ToolExecutionCompleteContentTerminal:
    "Terminal/shell output content block with optional exit code and working directory"
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
    interaction_id: str | None = None
    is_user_requested: bool | None = None
    model: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    result: ToolExecutionCompleteResult | None = None
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
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        is_user_requested = from_union([from_none, from_bool], obj.get("isUserRequested"))
        model = from_union([from_none, from_str], obj.get("model"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        result = from_union([from_none, ToolExecutionCompleteResult.from_dict], obj.get("result"))
        sandboxed = from_union([from_none, from_bool], obj.get("sandboxed"))
        tool_description = from_union([from_none, ToolExecutionCompleteToolDescription.from_dict], obj.get("toolDescription"))
        tool_telemetry = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("toolTelemetry"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return ToolExecutionCompleteData(
            success=success,
            tool_call_id=tool_call_id,
            error=error,
            interaction_id=interaction_id,
            is_user_requested=is_user_requested,
            model=model,
            parent_tool_call_id=parent_tool_call_id,
            result=result,
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
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.is_user_requested is not None:
            result["isUserRequested"] = from_union([from_none, from_bool], self.is_user_requested)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.result is not None:
            result["result"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteResult, x)], self.result)
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
    contents: list[ToolExecutionCompleteContent] | None = None
    detailed_content: str | None = None
    ui_resource: ToolExecutionCompleteUIResource | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteResult":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        contents = from_union([from_none, lambda x: from_list(_load_ToolExecutionCompleteContent, x)], obj.get("contents"))
        detailed_content = from_union([from_none, from_str], obj.get("detailedContent"))
        ui_resource = from_union([from_none, ToolExecutionCompleteUIResource.from_dict], obj.get("uiResource"))
        return ToolExecutionCompleteResult(
            content=content,
            contents=contents,
            detailed_content=detailed_content,
            ui_resource=ui_resource,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.contents is not None:
            result["contents"] = from_union([from_none, lambda x: from_list(lambda x: x.to_dict(), x)], self.contents)
        if self.detailed_content is not None:
            result["detailedContent"] = from_union([from_none, from_str], self.detailed_content)
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
    "Schema for the `ToolExecutionCompleteToolDescriptionMetaUI` type."
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
    "Schema for the `ToolExecutionCompleteUIResourceMetaUI` type."
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
    "Schema for the `ToolExecutionCompleteUIResourceMetaUICsp` type."
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
    "Schema for the `ToolExecutionCompleteUIResourceMetaUIPermissions` type."
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
    "Schema for the `ToolExecutionCompleteUIResourceMetaUIPermissionsCamera` type."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsCamera":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsCamera()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite:
    "Schema for the `ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite` type."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation:
    "Schema for the `ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation` type."
    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation":
        assert isinstance(obj, dict)
        return ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation()

    def to_dict(self) -> dict:
        return {}


@dataclass
class ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone:
    "Schema for the `ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone` type."
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
    mcp_server_name: str | None = None
    mcp_tool_name: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    turn_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        arguments = obj.get("arguments")
        mcp_server_name = from_union([from_none, from_str], obj.get("mcpServerName"))
        mcp_tool_name = from_union([from_none, from_str], obj.get("mcpToolName"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        turn_id = from_union([from_none, from_str], obj.get("turnId"))
        return ToolExecutionStartData(
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
            mcp_server_name=mcp_server_name,
            mcp_tool_name=mcp_tool_name,
            parent_tool_call_id=parent_tool_call_id,
            turn_id=turn_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["toolName"] = from_str(self.tool_name)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.mcp_server_name is not None:
            result["mcpServerName"] = from_union([from_none, from_str], self.mcp_server_name)
        if self.mcp_tool_name is not None:
            result["mcpToolName"] = from_union([from_none, from_str], self.mcp_tool_name)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.turn_id is not None:
            result["turnId"] = from_union([from_none, from_str], self.turn_id)
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
class UserMessageAttachmentBlob:
    "Blob attachment with inline base64-encoded data"
    data: str
    mime_type: str
    type: ClassVar[str] = "blob"
    display_name: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentBlob":
        assert isinstance(obj, dict)
        data = from_str(obj.get("data"))
        mime_type = from_str(obj.get("mimeType"))
        display_name = from_union([from_none, from_str], obj.get("displayName"))
        return UserMessageAttachmentBlob(
            data=data,
            mime_type=mime_type,
            display_name=display_name,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["data"] = from_str(self.data)
        result["mimeType"] = from_str(self.mime_type)
        result["type"] = self.type
        if self.display_name is not None:
            result["displayName"] = from_union([from_none, from_str], self.display_name)
        return result


@dataclass
class UserMessageAttachmentDirectory:
    "Directory attachment"
    display_name: str
    path: str
    type: ClassVar[str] = "directory"

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentDirectory":
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        path = from_str(obj.get("path"))
        return UserMessageAttachmentDirectory(
            display_name=display_name,
            path=path,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["path"] = from_str(self.path)
        result["type"] = self.type
        return result


@dataclass
class UserMessageAttachmentFile:
    "File attachment"
    display_name: str
    path: str
    type: ClassVar[str] = "file"
    line_range: UserMessageAttachmentFileLineRange | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentFile":
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        path = from_str(obj.get("path"))
        line_range = from_union([from_none, UserMessageAttachmentFileLineRange.from_dict], obj.get("lineRange"))
        return UserMessageAttachmentFile(
            display_name=display_name,
            path=path,
            line_range=line_range,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["path"] = from_str(self.path)
        result["type"] = self.type
        if self.line_range is not None:
            result["lineRange"] = from_union([from_none, lambda x: to_class(UserMessageAttachmentFileLineRange, x)], self.line_range)
        return result


@dataclass
class UserMessageAttachmentFileLineRange:
    "Optional line range to scope the attachment to a specific section of the file"
    end: int
    start: int

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentFileLineRange":
        assert isinstance(obj, dict)
        end = from_int(obj.get("end"))
        start = from_int(obj.get("start"))
        return UserMessageAttachmentFileLineRange(
            end=end,
            start=start,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_int(self.end)
        result["start"] = to_int(self.start)
        return result


@dataclass
class UserMessageAttachmentGithubReference:
    "GitHub issue, pull request, or discussion reference"
    number: int
    reference_type: UserMessageAttachmentGithubReferenceType
    state: str
    title: str
    type: ClassVar[str] = "github_reference"
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentGithubReference":
        assert isinstance(obj, dict)
        number = from_int(obj.get("number"))
        reference_type = parse_enum(UserMessageAttachmentGithubReferenceType, obj.get("referenceType"))
        state = from_str(obj.get("state"))
        title = from_str(obj.get("title"))
        url = from_str(obj.get("url"))
        return UserMessageAttachmentGithubReference(
            number=number,
            reference_type=reference_type,
            state=state,
            title=title,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["number"] = to_int(self.number)
        result["referenceType"] = to_enum(UserMessageAttachmentGithubReferenceType, self.reference_type)
        result["state"] = from_str(self.state)
        result["title"] = from_str(self.title)
        result["type"] = self.type
        result["url"] = from_str(self.url)
        return result


@dataclass
class UserMessageAttachmentSelection:
    "Code selection attachment from an editor"
    display_name: str
    file_path: str
    selection: UserMessageAttachmentSelectionDetails
    text: str
    type: ClassVar[str] = "selection"

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelection":
        assert isinstance(obj, dict)
        display_name = from_str(obj.get("displayName"))
        file_path = from_str(obj.get("filePath"))
        selection = UserMessageAttachmentSelectionDetails.from_dict(obj.get("selection"))
        text = from_str(obj.get("text"))
        return UserMessageAttachmentSelection(
            display_name=display_name,
            file_path=file_path,
            selection=selection,
            text=text,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["displayName"] = from_str(self.display_name)
        result["filePath"] = from_str(self.file_path)
        result["selection"] = to_class(UserMessageAttachmentSelectionDetails, self.selection)
        result["text"] = from_str(self.text)
        result["type"] = self.type
        return result


@dataclass
class UserMessageAttachmentSelectionDetails:
    "Position range of the selection within the file"
    end: UserMessageAttachmentSelectionDetailsEnd
    start: UserMessageAttachmentSelectionDetailsStart

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelectionDetails":
        assert isinstance(obj, dict)
        end = UserMessageAttachmentSelectionDetailsEnd.from_dict(obj.get("end"))
        start = UserMessageAttachmentSelectionDetailsStart.from_dict(obj.get("start"))
        return UserMessageAttachmentSelectionDetails(
            end=end,
            start=start,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["end"] = to_class(UserMessageAttachmentSelectionDetailsEnd, self.end)
        result["start"] = to_class(UserMessageAttachmentSelectionDetailsStart, self.start)
        return result


@dataclass
class UserMessageAttachmentSelectionDetailsEnd:
    "End position of the selection"
    character: int
    line: int

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelectionDetailsEnd":
        assert isinstance(obj, dict)
        character = from_int(obj.get("character"))
        line = from_int(obj.get("line"))
        return UserMessageAttachmentSelectionDetailsEnd(
            character=character,
            line=line,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = to_int(self.character)
        result["line"] = to_int(self.line)
        return result


@dataclass
class UserMessageAttachmentSelectionDetailsStart:
    "Start position of the selection"
    character: int
    line: int

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelectionDetailsStart":
        assert isinstance(obj, dict)
        character = from_int(obj.get("character"))
        line = from_int(obj.get("line"))
        return UserMessageAttachmentSelectionDetailsStart(
            character=character,
            line=line,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["character"] = to_int(self.character)
        result["line"] = to_int(self.line)
        return result


@dataclass
class UserMessageData:
    "Schema for the `UserMessageData` type."
    content: str
    agent_mode: UserMessageAgentMode | None = None
    attachments: list[UserMessageAttachment] | None = None
    interaction_id: str | None = None
    is_autopilot_continuation: bool | None = None
    native_document_path_fallback_paths: list[str] | None = None
    parent_agent_task_id: str | None = None
    source: str | None = None
    supported_native_document_mime_types: list[str] | None = None
    transformed_content: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        agent_mode = from_union([from_none, lambda x: parse_enum(UserMessageAgentMode, x)], obj.get("agentMode"))
        attachments = from_union([from_none, lambda x: from_list(_load_UserMessageAttachment, x)], obj.get("attachments"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        is_autopilot_continuation = from_union([from_none, from_bool], obj.get("isAutopilotContinuation"))
        native_document_path_fallback_paths = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("nativeDocumentPathFallbackPaths"))
        parent_agent_task_id = from_union([from_none, from_str], obj.get("parentAgentTaskId"))
        source = from_union([from_none, from_str], obj.get("source"))
        supported_native_document_mime_types = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("supportedNativeDocumentMimeTypes"))
        transformed_content = from_union([from_none, from_str], obj.get("transformedContent"))
        return UserMessageData(
            content=content,
            agent_mode=agent_mode,
            attachments=attachments,
            interaction_id=interaction_id,
            is_autopilot_continuation=is_autopilot_continuation,
            native_document_path_fallback_paths=native_document_path_fallback_paths,
            parent_agent_task_id=parent_agent_task_id,
            source=source,
            supported_native_document_mime_types=supported_native_document_mime_types,
            transformed_content=transformed_content,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.agent_mode is not None:
            result["agentMode"] = from_union([from_none, lambda x: to_enum(UserMessageAgentMode, x)], self.agent_mode)
        if self.attachments is not None:
            result["attachments"] = from_union([from_none, lambda x: from_list(lambda x: x.to_dict(), x)], self.attachments)
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
        return result


@dataclass
class UserToolSessionApprovalCommands:
    "Schema for the `UserToolSessionApprovalCommands` type."
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
    "Schema for the `UserToolSessionApprovalCustomTool` type."
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
class UserToolSessionApprovalExtensionManagement:
    "Schema for the `UserToolSessionApprovalExtensionManagement` type."
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
    "Schema for the `UserToolSessionApprovalExtensionPermissionAccess` type."
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
class UserToolSessionApprovalMcp:
    "Schema for the `UserToolSessionApprovalMcp` type."
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
    "Schema for the `UserToolSessionApprovalMemory` type."
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
    "Schema for the `UserToolSessionApprovalRead` type."
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
    "Schema for the `UserToolSessionApprovalWrite` type."
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
        repository = from_union([from_none, from_str], obj.get("repository"))
        repository_host = from_union([from_none, from_str], obj.get("repositoryHost"))
        return WorkingDirectoryContext(
            cwd=cwd,
            base_commit=base_commit,
            branch=branch,
            git_root=git_root,
            head_commit=head_commit,
            host_type=host_type,
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
        if self.repository is not None:
            result["repository"] = from_union([from_none, from_str], self.repository)
        if self.repository_host is not None:
            result["repositoryHost"] = from_union([from_none, from_str], self.repository_host)
        return result


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
        case "extension-permission-access": return PermissionPromptRequestExtensionPermissionAccess.from_dict(obj)
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
        case "extension-permission-access": return PermissionRequestExtensionPermissionAccess.from_dict(obj)
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
        case _: raise ValueError(f"Unknown SystemNotification type: {kind!r}")


def _load_ToolExecutionCompleteContent(obj: Any) -> "ToolExecutionCompleteContent":
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "text": return ToolExecutionCompleteContentText.from_dict(obj)
        case "terminal": return ToolExecutionCompleteContentTerminal.from_dict(obj)
        case "image": return ToolExecutionCompleteContentImage.from_dict(obj)
        case "audio": return ToolExecutionCompleteContentAudio.from_dict(obj)
        case "resource_link": return ToolExecutionCompleteContentResourceLink.from_dict(obj)
        case "resource": return ToolExecutionCompleteContentResource.from_dict(obj)
        case _: raise ValueError(f"Unknown ToolExecutionCompleteContent type: {kind!r}")


def _load_UserMessageAttachment(obj: Any) -> "UserMessageAttachment":
    assert isinstance(obj, dict)
    kind = obj.get("type")
    match kind:
        case "file": return UserMessageAttachmentFile.from_dict(obj)
        case "directory": return UserMessageAttachmentDirectory.from_dict(obj)
        case "selection": return UserMessageAttachmentSelection.from_dict(obj)
        case "github_reference": return UserMessageAttachmentGithubReference.from_dict(obj)
        case "blob": return UserMessageAttachmentBlob.from_dict(obj)
        case _: raise ValueError(f"Unknown UserMessageAttachment type: {kind!r}")


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
        case "extension-permission-access": return UserToolSessionApprovalExtensionPermissionAccess.from_dict(obj)
        case _: raise ValueError(f"Unknown UserToolSessionApproval kind: {kind!r}")


# A content block within a tool result, which may be text, terminal output, image, audio, or a resource
ToolExecutionCompleteContent = ToolExecutionCompleteContentText | ToolExecutionCompleteContentTerminal | ToolExecutionCompleteContentImage | ToolExecutionCompleteContentAudio | ToolExecutionCompleteContentResourceLink | ToolExecutionCompleteContentResource


# A user message attachment — a file, directory, code selection, blob, or GitHub reference
UserMessageAttachment = UserMessageAttachmentFile | UserMessageAttachmentDirectory | UserMessageAttachmentSelection | UserMessageAttachmentGithubReference | UserMessageAttachmentBlob


# Derived user-facing permission prompt details for UI consumers
PermissionPromptRequest = PermissionPromptRequestCommands | PermissionPromptRequestWrite | PermissionPromptRequestRead | PermissionPromptRequestMcp | PermissionPromptRequestUrl | PermissionPromptRequestMemory | PermissionPromptRequestCustomTool | PermissionPromptRequestPath | PermissionPromptRequestHook | PermissionPromptRequestExtensionManagement | PermissionPromptRequestExtensionPermissionAccess


# Details of the permission being requested
PermissionRequest = PermissionRequestShell | PermissionRequestWrite | PermissionRequestRead | PermissionRequestMcp | PermissionRequestUrl | PermissionRequestMemory | PermissionRequestCustomTool | PermissionRequestHook | PermissionRequestExtensionManagement | PermissionRequestExtensionPermissionAccess


# Structured metadata identifying what triggered this notification
SystemNotification = SystemNotificationAgentCompleted | SystemNotificationAgentIdle | SystemNotificationNewInboxMessage | SystemNotificationShellCompleted | SystemNotificationShellDetachedCompleted | SystemNotificationInstructionDiscovered


# The approval to add as a session-scoped rule
UserToolSessionApproval = UserToolSessionApprovalCommands | UserToolSessionApprovalRead | UserToolSessionApprovalWrite | UserToolSessionApprovalMcp | UserToolSessionApprovalMemory | UserToolSessionApprovalCustomTool | UserToolSessionApprovalExtensionManagement | UserToolSessionApprovalExtensionPermissionAccess


# The embedded resource contents, either text or base64-encoded binary
ToolExecutionCompleteContentResourceDetails = EmbeddedTextResourceContents | EmbeddedBlobResourceContents


# The result of the permission request
PermissionResult = PermissionApproved | PermissionApprovedForSession | PermissionApprovedForLocation | PermissionCancelled | PermissionDeniedByRules | PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser | PermissionDeniedInteractivelyByUser | PermissionDeniedByContentExclusionPolicy | PermissionDeniedByPermissionRequestHook


class AbortReason(Enum):
    "Finite reason code describing why the current turn was aborted"
    # The local user requested the abort, for example by pressing Ctrl+C in the CLI.
    USER_INITIATED = "user_initiated"
    # A remote command requested the abort.
    REMOTE_COMMAND = "remote_command"
    # An MCP server delivered a user.abort notification.
    USER_ABORT = "user_abort"


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


class AutoModeSwitchResponse(Enum):
    "The user's auto-mode-switch choice"
    # Switch models for this request.
    YES = "yes"
    # Switch models now and keep using the replacement automatically.
    YES_ALWAYS = "yes_always"
    # Do not switch models.
    NO = "no"


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


class HandoffSourceType(Enum):
    "Origin type of the session being handed off"
    # The handoff originated from a remote session.
    REMOTE = "remote"
    # The handoff originated from a local session.
    LOCAL = "local"


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
    "Connection status: connected, failed, needs-auth, pending, disabled, or not_configured"
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


class ModelCallFailureSource(Enum):
    "Where the failed model call originated"
    # Model call from the top-level agent.
    TOP_LEVEL = "top_level"
    # Model call from a sub-agent.
    SUBAGENT = "subagent"
    # Model call from MCP sampling.
    MCP_SAMPLING = "mcp_sampling"


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


class SessionMode(Enum):
    "The session mode the agent is operating in"
    # The agent is responding interactively to the user.
    INTERACTIVE = "interactive"
    # The agent is preparing a plan before making changes.
    PLAN = "plan"
    # The agent is working autonomously toward task completion.
    AUTOPILOT = "autopilot"


class SessionModelChangeDataContextTier(Enum):
    # Default context tier with standard context window size.
    DEFAULT = "default"
    # Extended context tier with a larger context window.
    LONG_CONTEXT = "long_context"


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


class UserMessageAttachmentGithubReferenceType(Enum):
    "Type of GitHub reference"
    # GitHub issue reference.
    ISSUE = "issue"
    # GitHub pull request reference.
    PR = "pr"
    # GitHub discussion reference.
    DISCUSSION = "discussion"


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


SessionEventData = SessionStartData | SessionResumeData | SessionRemoteSteerableChangedData | SessionErrorData | SessionIdleData | SessionTitleChangedData | SessionScheduleCreatedData | SessionScheduleCancelledData | SessionInfoData | SessionWarningData | SessionModelChangeData | SessionModeChangedData | SessionPlanChangedData | SessionWorkspaceFileChangedData | SessionHandoffData | SessionTruncationData | SessionSnapshotRewindData | SessionShutdownData | SessionContextChangedData | SessionUsageInfoData | SessionCompactionStartData | SessionCompactionCompleteData | SessionTaskCompleteData | UserMessageData | PendingMessagesModifiedData | AssistantTurnStartData | AssistantIntentData | AssistantReasoningData | AssistantReasoningDeltaData | AssistantStreamingDeltaData | AssistantMessageData | AssistantMessageStartData | AssistantMessageDeltaData | AssistantTurnEndData | AssistantUsageData | ModelCallFailureData | AbortData | ToolUserRequestedData | ToolExecutionStartData | ToolExecutionPartialResultData | ToolExecutionProgressData | ToolExecutionCompleteData | SkillInvokedData | SubagentStartedData | SubagentCompletedData | SubagentFailedData | SubagentSelectedData | SubagentDeselectedData | HookStartData | HookEndData | SystemMessageData | SystemNotificationData | PermissionRequestedData | PermissionCompletedData | UserInputRequestedData | UserInputCompletedData | ElicitationRequestedData | ElicitationCompletedData | SamplingRequestedData | SamplingCompletedData | McpOauthRequiredData | McpOauthCompletedData | SessionCustomNotificationData | ExternalToolRequestedData | ExternalToolCompletedData | CommandQueuedData | CommandExecuteData | CommandCompletedData | AutoModeSwitchRequestedData | AutoModeSwitchCompletedData | CommandsChangedData | CapabilitiesChangedData | ExitPlanModeRequestedData | ExitPlanModeCompletedData | SessionToolsUpdatedData | SessionBackgroundTasksChangedData | SessionSkillsLoadedData | SessionCustomAgentsUpdatedData | SessionMcpServersLoadedData | SessionMcpServerStatusChangedData | SessionExtensionsLoadedData | McpAppToolCallCompleteData | RawSessionEventData | Data


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
            case SessionEventType.SESSION_INFO: data = SessionInfoData.from_dict(data_obj)
            case SessionEventType.SESSION_WARNING: data = SessionWarningData.from_dict(data_obj)
            case SessionEventType.SESSION_MODEL_CHANGE: data = SessionModelChangeData.from_dict(data_obj)
            case SessionEventType.SESSION_MODE_CHANGED: data = SessionModeChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_PLAN_CHANGED: data = SessionPlanChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_WORKSPACE_FILE_CHANGED: data = SessionWorkspaceFileChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_HANDOFF: data = SessionHandoffData.from_dict(data_obj)
            case SessionEventType.SESSION_TRUNCATION: data = SessionTruncationData.from_dict(data_obj)
            case SessionEventType.SESSION_SNAPSHOT_REWIND: data = SessionSnapshotRewindData.from_dict(data_obj)
            case SessionEventType.SESSION_SHUTDOWN: data = SessionShutdownData.from_dict(data_obj)
            case SessionEventType.SESSION_CONTEXT_CHANGED: data = SessionContextChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_USAGE_INFO: data = SessionUsageInfoData.from_dict(data_obj)
            case SessionEventType.SESSION_COMPACTION_START: data = SessionCompactionStartData.from_dict(data_obj)
            case SessionEventType.SESSION_COMPACTION_COMPLETE: data = SessionCompactionCompleteData.from_dict(data_obj)
            case SessionEventType.SESSION_TASK_COMPLETE: data = SessionTaskCompleteData.from_dict(data_obj)
            case SessionEventType.USER_MESSAGE: data = UserMessageData.from_dict(data_obj)
            case SessionEventType.PENDING_MESSAGES_MODIFIED: data = PendingMessagesModifiedData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TURN_START: data = AssistantTurnStartData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_INTENT: data = AssistantIntentData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_REASONING: data = AssistantReasoningData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_REASONING_DELTA: data = AssistantReasoningDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_STREAMING_DELTA: data = AssistantStreamingDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_MESSAGE: data = AssistantMessageData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_MESSAGE_START: data = AssistantMessageStartData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_MESSAGE_DELTA: data = AssistantMessageDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TURN_END: data = AssistantTurnEndData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_USAGE: data = AssistantUsageData.from_dict(data_obj)
            case SessionEventType.MODEL_CALL_FAILURE: data = ModelCallFailureData.from_dict(data_obj)
            case SessionEventType.ABORT: data = AbortData.from_dict(data_obj)
            case SessionEventType.TOOL_USER_REQUESTED: data = ToolUserRequestedData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_START: data = ToolExecutionStartData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_PARTIAL_RESULT: data = ToolExecutionPartialResultData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_PROGRESS: data = ToolExecutionProgressData.from_dict(data_obj)
            case SessionEventType.TOOL_EXECUTION_COMPLETE: data = ToolExecutionCompleteData.from_dict(data_obj)
            case SessionEventType.SKILL_INVOKED: data = SkillInvokedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_STARTED: data = SubagentStartedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_COMPLETED: data = SubagentCompletedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_FAILED: data = SubagentFailedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_SELECTED: data = SubagentSelectedData.from_dict(data_obj)
            case SessionEventType.SUBAGENT_DESELECTED: data = SubagentDeselectedData.from_dict(data_obj)
            case SessionEventType.HOOK_START: data = HookStartData.from_dict(data_obj)
            case SessionEventType.HOOK_END: data = HookEndData.from_dict(data_obj)
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
            case SessionEventType.SESSION_CUSTOM_NOTIFICATION: data = SessionCustomNotificationData.from_dict(data_obj)
            case SessionEventType.EXTERNAL_TOOL_REQUESTED: data = ExternalToolRequestedData.from_dict(data_obj)
            case SessionEventType.EXTERNAL_TOOL_COMPLETED: data = ExternalToolCompletedData.from_dict(data_obj)
            case SessionEventType.COMMAND_QUEUED: data = CommandQueuedData.from_dict(data_obj)
            case SessionEventType.COMMAND_EXECUTE: data = CommandExecuteData.from_dict(data_obj)
            case SessionEventType.COMMAND_COMPLETED: data = CommandCompletedData.from_dict(data_obj)
            case SessionEventType.AUTO_MODE_SWITCH_REQUESTED: data = AutoModeSwitchRequestedData.from_dict(data_obj)
            case SessionEventType.AUTO_MODE_SWITCH_COMPLETED: data = AutoModeSwitchCompletedData.from_dict(data_obj)
            case SessionEventType.COMMANDS_CHANGED: data = CommandsChangedData.from_dict(data_obj)
            case SessionEventType.CAPABILITIES_CHANGED: data = CapabilitiesChangedData.from_dict(data_obj)
            case SessionEventType.EXIT_PLAN_MODE_REQUESTED: data = ExitPlanModeRequestedData.from_dict(data_obj)
            case SessionEventType.EXIT_PLAN_MODE_COMPLETED: data = ExitPlanModeCompletedData.from_dict(data_obj)
            case SessionEventType.SESSION_TOOLS_UPDATED: data = SessionToolsUpdatedData.from_dict(data_obj)
            case SessionEventType.SESSION_BACKGROUND_TASKS_CHANGED: data = SessionBackgroundTasksChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_SKILLS_LOADED: data = SessionSkillsLoadedData.from_dict(data_obj)
            case SessionEventType.SESSION_CUSTOM_AGENTS_UPDATED: data = SessionCustomAgentsUpdatedData.from_dict(data_obj)
            case SessionEventType.SESSION_MCP_SERVERS_LOADED: data = SessionMcpServersLoadedData.from_dict(data_obj)
            case SessionEventType.SESSION_MCP_SERVER_STATUS_CHANGED: data = SessionMcpServerStatusChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_EXTENSIONS_LOADED: data = SessionExtensionsLoadedData.from_dict(data_obj)
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

