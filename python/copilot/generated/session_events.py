"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: session-events.schema.json
"""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, TypeVar, cast
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
    ASSISTANT_MESSAGE_DELTA = "assistant.message_delta"
    ASSISTANT_TURN_END = "assistant.turn_end"
    ASSISTANT_USAGE = "assistant.usage"
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
    EXTERNAL_TOOL_REQUESTED = "external_tool.requested"
    EXTERNAL_TOOL_COMPLETED = "external_tool.completed"
    COMMAND_QUEUED = "command.queued"
    COMMAND_EXECUTE = "command.execute"
    COMMAND_COMPLETED = "command.completed"
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
class WorkingDirectoryContext:
    "Working directory and git context at session start"
    cwd: str
    git_root: str | None = None
    repository: str | None = None
    host_type: WorkingDirectoryContextHostType | None = None
    branch: str | None = None
    head_commit: str | None = None
    base_commit: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "WorkingDirectoryContext":
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        git_root = from_union([from_none, from_str], obj.get("gitRoot"))
        repository = from_union([from_none, from_str], obj.get("repository"))
        host_type = from_union([from_none, lambda x: parse_enum(WorkingDirectoryContextHostType, x)], obj.get("hostType"))
        branch = from_union([from_none, from_str], obj.get("branch"))
        head_commit = from_union([from_none, from_str], obj.get("headCommit"))
        base_commit = from_union([from_none, from_str], obj.get("baseCommit"))
        return WorkingDirectoryContext(
            cwd=cwd,
            git_root=git_root,
            repository=repository,
            host_type=host_type,
            branch=branch,
            head_commit=head_commit,
            base_commit=base_commit,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_none, from_str], self.git_root)
        if self.repository is not None:
            result["repository"] = from_union([from_none, from_str], self.repository)
        if self.host_type is not None:
            result["hostType"] = from_union([from_none, lambda x: to_enum(WorkingDirectoryContextHostType, x)], self.host_type)
        if self.branch is not None:
            result["branch"] = from_union([from_none, from_str], self.branch)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_none, from_str], self.head_commit)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_none, from_str], self.base_commit)
        return result


@dataclass
class SessionStartData:
    "Session initialization metadata including context and configuration"
    session_id: str
    version: float
    producer: str
    copilot_version: str
    start_time: datetime
    selected_model: str | None = None
    reasoning_effort: str | None = None
    context: WorkingDirectoryContext | None = None
    already_in_use: bool | None = None
    remote_steerable: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionStartData":
        assert isinstance(obj, dict)
        session_id = from_str(obj.get("sessionId"))
        version = from_float(obj.get("version"))
        producer = from_str(obj.get("producer"))
        copilot_version = from_str(obj.get("copilotVersion"))
        start_time = from_datetime(obj.get("startTime"))
        selected_model = from_union([from_none, from_str], obj.get("selectedModel"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        context = from_union([from_none, WorkingDirectoryContext.from_dict], obj.get("context"))
        already_in_use = from_union([from_none, from_bool], obj.get("alreadyInUse"))
        remote_steerable = from_union([from_none, from_bool], obj.get("remoteSteerable"))
        return SessionStartData(
            session_id=session_id,
            version=version,
            producer=producer,
            copilot_version=copilot_version,
            start_time=start_time,
            selected_model=selected_model,
            reasoning_effort=reasoning_effort,
            context=context,
            already_in_use=already_in_use,
            remote_steerable=remote_steerable,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = from_str(self.session_id)
        result["version"] = to_float(self.version)
        result["producer"] = from_str(self.producer)
        result["copilotVersion"] = from_str(self.copilot_version)
        result["startTime"] = to_datetime(self.start_time)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_none, from_str], self.selected_model)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.context is not None:
            result["context"] = from_union([from_none, lambda x: to_class(WorkingDirectoryContext, x)], self.context)
        if self.already_in_use is not None:
            result["alreadyInUse"] = from_union([from_none, from_bool], self.already_in_use)
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_none, from_bool], self.remote_steerable)
        return result


@dataclass
class SessionResumeData:
    "Session resume metadata including current context and event count"
    resume_time: datetime
    event_count: float
    selected_model: str | None = None
    reasoning_effort: str | None = None
    context: WorkingDirectoryContext | None = None
    already_in_use: bool | None = None
    remote_steerable: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionResumeData":
        assert isinstance(obj, dict)
        resume_time = from_datetime(obj.get("resumeTime"))
        event_count = from_float(obj.get("eventCount"))
        selected_model = from_union([from_none, from_str], obj.get("selectedModel"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        context = from_union([from_none, WorkingDirectoryContext.from_dict], obj.get("context"))
        already_in_use = from_union([from_none, from_bool], obj.get("alreadyInUse"))
        remote_steerable = from_union([from_none, from_bool], obj.get("remoteSteerable"))
        return SessionResumeData(
            resume_time=resume_time,
            event_count=event_count,
            selected_model=selected_model,
            reasoning_effort=reasoning_effort,
            context=context,
            already_in_use=already_in_use,
            remote_steerable=remote_steerable,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["resumeTime"] = to_datetime(self.resume_time)
        result["eventCount"] = to_float(self.event_count)
        if self.selected_model is not None:
            result["selectedModel"] = from_union([from_none, from_str], self.selected_model)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        if self.context is not None:
            result["context"] = from_union([from_none, lambda x: to_class(WorkingDirectoryContext, x)], self.context)
        if self.already_in_use is not None:
            result["alreadyInUse"] = from_union([from_none, from_bool], self.already_in_use)
        if self.remote_steerable is not None:
            result["remoteSteerable"] = from_union([from_none, from_bool], self.remote_steerable)
        return result


@dataclass
class SessionRemoteSteerableChangedData:
    "Notifies Mission Control that the session's remote steering capability has changed"
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
class SessionErrorData:
    "Error details for timeline display including message and optional diagnostic information"
    error_type: str
    message: str
    stack: str | None = None
    status_code: int | None = None
    provider_call_id: str | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionErrorData":
        assert isinstance(obj, dict)
        error_type = from_str(obj.get("errorType"))
        message = from_str(obj.get("message"))
        stack = from_union([from_none, from_str], obj.get("stack"))
        status_code = from_union([from_none, from_int], obj.get("statusCode"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionErrorData(
            error_type=error_type,
            message=message,
            stack=stack,
            status_code=status_code,
            provider_call_id=provider_call_id,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["errorType"] = from_str(self.error_type)
        result["message"] = from_str(self.message)
        if self.stack is not None:
            result["stack"] = from_union([from_none, from_str], self.stack)
        if self.status_code is not None:
            result["statusCode"] = from_union([from_none, to_int], self.status_code)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
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
class SessionInfoData:
    "Informational message for timeline display with categorization"
    info_type: str
    message: str
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionInfoData":
        assert isinstance(obj, dict)
        info_type = from_str(obj.get("infoType"))
        message = from_str(obj.get("message"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionInfoData(
            info_type=info_type,
            message=message,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["infoType"] = from_str(self.info_type)
        result["message"] = from_str(self.message)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


@dataclass
class SessionWarningData:
    "Warning message for timeline display with categorization"
    warning_type: str
    message: str
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionWarningData":
        assert isinstance(obj, dict)
        warning_type = from_str(obj.get("warningType"))
        message = from_str(obj.get("message"))
        url = from_union([from_none, from_str], obj.get("url"))
        return SessionWarningData(
            warning_type=warning_type,
            message=message,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["warningType"] = from_str(self.warning_type)
        result["message"] = from_str(self.message)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        return result


@dataclass
class SessionModelChangeData:
    "Model change details including previous and new model identifiers"
    new_model: str
    previous_model: str | None = None
    previous_reasoning_effort: str | None = None
    reasoning_effort: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionModelChangeData":
        assert isinstance(obj, dict)
        new_model = from_str(obj.get("newModel"))
        previous_model = from_union([from_none, from_str], obj.get("previousModel"))
        previous_reasoning_effort = from_union([from_none, from_str], obj.get("previousReasoningEffort"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        return SessionModelChangeData(
            new_model=new_model,
            previous_model=previous_model,
            previous_reasoning_effort=previous_reasoning_effort,
            reasoning_effort=reasoning_effort,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["newModel"] = from_str(self.new_model)
        if self.previous_model is not None:
            result["previousModel"] = from_union([from_none, from_str], self.previous_model)
        if self.previous_reasoning_effort is not None:
            result["previousReasoningEffort"] = from_union([from_none, from_str], self.previous_reasoning_effort)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        return result


@dataclass
class SessionModeChangedData:
    "Agent mode change details including previous and new modes"
    previous_mode: str
    new_mode: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionModeChangedData":
        assert isinstance(obj, dict)
        previous_mode = from_str(obj.get("previousMode"))
        new_mode = from_str(obj.get("newMode"))
        return SessionModeChangedData(
            previous_mode=previous_mode,
            new_mode=new_mode,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["previousMode"] = from_str(self.previous_mode)
        result["newMode"] = from_str(self.new_mode)
        return result


@dataclass
class SessionPlanChangedData:
    "Plan file operation details indicating what changed"
    operation: SessionPlanChangedDataOperation

    @staticmethod
    def from_dict(obj: Any) -> "SessionPlanChangedData":
        assert isinstance(obj, dict)
        operation = parse_enum(SessionPlanChangedDataOperation, obj.get("operation"))
        return SessionPlanChangedData(
            operation=operation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["operation"] = to_enum(SessionPlanChangedDataOperation, self.operation)
        return result


@dataclass
class SessionWorkspaceFileChangedData:
    "Workspace file change details including path and operation type"
    path: str
    operation: SessionWorkspaceFileChangedDataOperation

    @staticmethod
    def from_dict(obj: Any) -> "SessionWorkspaceFileChangedData":
        assert isinstance(obj, dict)
        path = from_str(obj.get("path"))
        operation = parse_enum(SessionWorkspaceFileChangedDataOperation, obj.get("operation"))
        return SessionWorkspaceFileChangedData(
            path=path,
            operation=operation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["path"] = from_str(self.path)
        result["operation"] = to_enum(SessionWorkspaceFileChangedDataOperation, self.operation)
        return result


@dataclass
class HandoffRepository:
    "Repository context for the handed-off session"
    owner: str
    name: str
    branch: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HandoffRepository":
        assert isinstance(obj, dict)
        owner = from_str(obj.get("owner"))
        name = from_str(obj.get("name"))
        branch = from_union([from_none, from_str], obj.get("branch"))
        return HandoffRepository(
            owner=owner,
            name=name,
            branch=branch,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["owner"] = from_str(self.owner)
        result["name"] = from_str(self.name)
        if self.branch is not None:
            result["branch"] = from_union([from_none, from_str], self.branch)
        return result


@dataclass
class SessionHandoffData:
    "Session handoff metadata including source, context, and repository information"
    handoff_time: datetime
    source_type: HandoffSourceType
    repository: HandoffRepository | None = None
    context: str | None = None
    summary: str | None = None
    remote_session_id: str | None = None
    host: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionHandoffData":
        assert isinstance(obj, dict)
        handoff_time = from_datetime(obj.get("handoffTime"))
        source_type = parse_enum(HandoffSourceType, obj.get("sourceType"))
        repository = from_union([from_none, HandoffRepository.from_dict], obj.get("repository"))
        context = from_union([from_none, from_str], obj.get("context"))
        summary = from_union([from_none, from_str], obj.get("summary"))
        remote_session_id = from_union([from_none, from_str], obj.get("remoteSessionId"))
        host = from_union([from_none, from_str], obj.get("host"))
        return SessionHandoffData(
            handoff_time=handoff_time,
            source_type=source_type,
            repository=repository,
            context=context,
            summary=summary,
            remote_session_id=remote_session_id,
            host=host,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["handoffTime"] = to_datetime(self.handoff_time)
        result["sourceType"] = to_enum(HandoffSourceType, self.source_type)
        if self.repository is not None:
            result["repository"] = from_union([from_none, lambda x: to_class(HandoffRepository, x)], self.repository)
        if self.context is not None:
            result["context"] = from_union([from_none, from_str], self.context)
        if self.summary is not None:
            result["summary"] = from_union([from_none, from_str], self.summary)
        if self.remote_session_id is not None:
            result["remoteSessionId"] = from_union([from_none, from_str], self.remote_session_id)
        if self.host is not None:
            result["host"] = from_union([from_none, from_str], self.host)
        return result


@dataclass
class SessionTruncationData:
    "Conversation truncation statistics including token counts and removed content metrics"
    token_limit: float
    pre_truncation_tokens_in_messages: float
    pre_truncation_messages_length: float
    post_truncation_tokens_in_messages: float
    post_truncation_messages_length: float
    tokens_removed_during_truncation: float
    messages_removed_during_truncation: float
    performed_by: str

    @staticmethod
    def from_dict(obj: Any) -> "SessionTruncationData":
        assert isinstance(obj, dict)
        token_limit = from_float(obj.get("tokenLimit"))
        pre_truncation_tokens_in_messages = from_float(obj.get("preTruncationTokensInMessages"))
        pre_truncation_messages_length = from_float(obj.get("preTruncationMessagesLength"))
        post_truncation_tokens_in_messages = from_float(obj.get("postTruncationTokensInMessages"))
        post_truncation_messages_length = from_float(obj.get("postTruncationMessagesLength"))
        tokens_removed_during_truncation = from_float(obj.get("tokensRemovedDuringTruncation"))
        messages_removed_during_truncation = from_float(obj.get("messagesRemovedDuringTruncation"))
        performed_by = from_str(obj.get("performedBy"))
        return SessionTruncationData(
            token_limit=token_limit,
            pre_truncation_tokens_in_messages=pre_truncation_tokens_in_messages,
            pre_truncation_messages_length=pre_truncation_messages_length,
            post_truncation_tokens_in_messages=post_truncation_tokens_in_messages,
            post_truncation_messages_length=post_truncation_messages_length,
            tokens_removed_during_truncation=tokens_removed_during_truncation,
            messages_removed_during_truncation=messages_removed_during_truncation,
            performed_by=performed_by,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenLimit"] = to_float(self.token_limit)
        result["preTruncationTokensInMessages"] = to_float(self.pre_truncation_tokens_in_messages)
        result["preTruncationMessagesLength"] = to_float(self.pre_truncation_messages_length)
        result["postTruncationTokensInMessages"] = to_float(self.post_truncation_tokens_in_messages)
        result["postTruncationMessagesLength"] = to_float(self.post_truncation_messages_length)
        result["tokensRemovedDuringTruncation"] = to_float(self.tokens_removed_during_truncation)
        result["messagesRemovedDuringTruncation"] = to_float(self.messages_removed_during_truncation)
        result["performedBy"] = from_str(self.performed_by)
        return result


@dataclass
class SessionSnapshotRewindData:
    "Session rewind details including target event and count of removed events"
    up_to_event_id: str
    events_removed: float

    @staticmethod
    def from_dict(obj: Any) -> "SessionSnapshotRewindData":
        assert isinstance(obj, dict)
        up_to_event_id = from_str(obj.get("upToEventId"))
        events_removed = from_float(obj.get("eventsRemoved"))
        return SessionSnapshotRewindData(
            up_to_event_id=up_to_event_id,
            events_removed=events_removed,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["upToEventId"] = from_str(self.up_to_event_id)
        result["eventsRemoved"] = to_float(self.events_removed)
        return result


@dataclass
class ShutdownCodeChanges:
    "Aggregate code change metrics for the session"
    lines_added: float
    lines_removed: float
    files_modified: list[str]

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownCodeChanges":
        assert isinstance(obj, dict)
        lines_added = from_float(obj.get("linesAdded"))
        lines_removed = from_float(obj.get("linesRemoved"))
        files_modified = from_list(from_str, obj.get("filesModified"))
        return ShutdownCodeChanges(
            lines_added=lines_added,
            lines_removed=lines_removed,
            files_modified=files_modified,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["linesAdded"] = to_float(self.lines_added)
        result["linesRemoved"] = to_float(self.lines_removed)
        result["filesModified"] = from_list(from_str, self.files_modified)
        return result


@dataclass
class ShutdownModelMetricRequests:
    "Request count and cost metrics"
    count: float
    cost: float

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetricRequests":
        assert isinstance(obj, dict)
        count = from_float(obj.get("count"))
        cost = from_float(obj.get("cost"))
        return ShutdownModelMetricRequests(
            count=count,
            cost=cost,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["count"] = to_float(self.count)
        result["cost"] = to_float(self.cost)
        return result


@dataclass
class ShutdownModelMetricUsage:
    "Token usage breakdown"
    input_tokens: float
    output_tokens: float
    cache_read_tokens: float
    cache_write_tokens: float
    reasoning_tokens: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetricUsage":
        assert isinstance(obj, dict)
        input_tokens = from_float(obj.get("inputTokens"))
        output_tokens = from_float(obj.get("outputTokens"))
        cache_read_tokens = from_float(obj.get("cacheReadTokens"))
        cache_write_tokens = from_float(obj.get("cacheWriteTokens"))
        reasoning_tokens = from_union([from_none, from_float], obj.get("reasoningTokens"))
        return ShutdownModelMetricUsage(
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            cache_read_tokens=cache_read_tokens,
            cache_write_tokens=cache_write_tokens,
            reasoning_tokens=reasoning_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["inputTokens"] = to_float(self.input_tokens)
        result["outputTokens"] = to_float(self.output_tokens)
        result["cacheReadTokens"] = to_float(self.cache_read_tokens)
        result["cacheWriteTokens"] = to_float(self.cache_write_tokens)
        if self.reasoning_tokens is not None:
            result["reasoningTokens"] = from_union([from_none, to_float], self.reasoning_tokens)
        return result


@dataclass
class ShutdownModelMetric:
    requests: ShutdownModelMetricRequests
    usage: ShutdownModelMetricUsage

    @staticmethod
    def from_dict(obj: Any) -> "ShutdownModelMetric":
        assert isinstance(obj, dict)
        requests = ShutdownModelMetricRequests.from_dict(obj.get("requests"))
        usage = ShutdownModelMetricUsage.from_dict(obj.get("usage"))
        return ShutdownModelMetric(
            requests=requests,
            usage=usage,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requests"] = to_class(ShutdownModelMetricRequests, self.requests)
        result["usage"] = to_class(ShutdownModelMetricUsage, self.usage)
        return result


@dataclass
class SessionShutdownData:
    "Session termination metrics including usage statistics, code changes, and shutdown reason"
    shutdown_type: ShutdownType
    total_premium_requests: float
    total_api_duration_ms: float
    session_start_time: float
    code_changes: ShutdownCodeChanges
    model_metrics: dict[str, ShutdownModelMetric]
    error_reason: str | None = None
    current_model: str | None = None
    current_tokens: float | None = None
    system_tokens: float | None = None
    conversation_tokens: float | None = None
    tool_definitions_tokens: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionShutdownData":
        assert isinstance(obj, dict)
        shutdown_type = parse_enum(ShutdownType, obj.get("shutdownType"))
        total_premium_requests = from_float(obj.get("totalPremiumRequests"))
        total_api_duration_ms = from_float(obj.get("totalApiDurationMs"))
        session_start_time = from_float(obj.get("sessionStartTime"))
        code_changes = ShutdownCodeChanges.from_dict(obj.get("codeChanges"))
        model_metrics = from_dict(ShutdownModelMetric.from_dict, obj.get("modelMetrics"))
        error_reason = from_union([from_none, from_str], obj.get("errorReason"))
        current_model = from_union([from_none, from_str], obj.get("currentModel"))
        current_tokens = from_union([from_none, from_float], obj.get("currentTokens"))
        system_tokens = from_union([from_none, from_float], obj.get("systemTokens"))
        conversation_tokens = from_union([from_none, from_float], obj.get("conversationTokens"))
        tool_definitions_tokens = from_union([from_none, from_float], obj.get("toolDefinitionsTokens"))
        return SessionShutdownData(
            shutdown_type=shutdown_type,
            total_premium_requests=total_premium_requests,
            total_api_duration_ms=total_api_duration_ms,
            session_start_time=session_start_time,
            code_changes=code_changes,
            model_metrics=model_metrics,
            error_reason=error_reason,
            current_model=current_model,
            current_tokens=current_tokens,
            system_tokens=system_tokens,
            conversation_tokens=conversation_tokens,
            tool_definitions_tokens=tool_definitions_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["shutdownType"] = to_enum(ShutdownType, self.shutdown_type)
        result["totalPremiumRequests"] = to_float(self.total_premium_requests)
        result["totalApiDurationMs"] = to_float(self.total_api_duration_ms)
        result["sessionStartTime"] = to_float(self.session_start_time)
        result["codeChanges"] = to_class(ShutdownCodeChanges, self.code_changes)
        result["modelMetrics"] = from_dict(lambda x: to_class(ShutdownModelMetric, x), self.model_metrics)
        if self.error_reason is not None:
            result["errorReason"] = from_union([from_none, from_str], self.error_reason)
        if self.current_model is not None:
            result["currentModel"] = from_union([from_none, from_str], self.current_model)
        if self.current_tokens is not None:
            result["currentTokens"] = from_union([from_none, to_float], self.current_tokens)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_float], self.system_tokens)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_float], self.conversation_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_float], self.tool_definitions_tokens)
        return result


@dataclass
class SessionContextChangedData:
    "Working directory and git context at session start"
    cwd: str
    git_root: str | None = None
    repository: str | None = None
    host_type: SessionContextChangedDataHostType | None = None
    branch: str | None = None
    head_commit: str | None = None
    base_commit: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionContextChangedData":
        assert isinstance(obj, dict)
        cwd = from_str(obj.get("cwd"))
        git_root = from_union([from_none, from_str], obj.get("gitRoot"))
        repository = from_union([from_none, from_str], obj.get("repository"))
        host_type = from_union([from_none, lambda x: parse_enum(SessionContextChangedDataHostType, x)], obj.get("hostType"))
        branch = from_union([from_none, from_str], obj.get("branch"))
        head_commit = from_union([from_none, from_str], obj.get("headCommit"))
        base_commit = from_union([from_none, from_str], obj.get("baseCommit"))
        return SessionContextChangedData(
            cwd=cwd,
            git_root=git_root,
            repository=repository,
            host_type=host_type,
            branch=branch,
            head_commit=head_commit,
            base_commit=base_commit,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["cwd"] = from_str(self.cwd)
        if self.git_root is not None:
            result["gitRoot"] = from_union([from_none, from_str], self.git_root)
        if self.repository is not None:
            result["repository"] = from_union([from_none, from_str], self.repository)
        if self.host_type is not None:
            result["hostType"] = from_union([from_none, lambda x: to_enum(SessionContextChangedDataHostType, x)], self.host_type)
        if self.branch is not None:
            result["branch"] = from_union([from_none, from_str], self.branch)
        if self.head_commit is not None:
            result["headCommit"] = from_union([from_none, from_str], self.head_commit)
        if self.base_commit is not None:
            result["baseCommit"] = from_union([from_none, from_str], self.base_commit)
        return result


@dataclass
class SessionUsageInfoData:
    "Current context window usage statistics including token and message counts"
    token_limit: float
    current_tokens: float
    messages_length: float
    system_tokens: float | None = None
    conversation_tokens: float | None = None
    tool_definitions_tokens: float | None = None
    is_initial: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionUsageInfoData":
        assert isinstance(obj, dict)
        token_limit = from_float(obj.get("tokenLimit"))
        current_tokens = from_float(obj.get("currentTokens"))
        messages_length = from_float(obj.get("messagesLength"))
        system_tokens = from_union([from_none, from_float], obj.get("systemTokens"))
        conversation_tokens = from_union([from_none, from_float], obj.get("conversationTokens"))
        tool_definitions_tokens = from_union([from_none, from_float], obj.get("toolDefinitionsTokens"))
        is_initial = from_union([from_none, from_bool], obj.get("isInitial"))
        return SessionUsageInfoData(
            token_limit=token_limit,
            current_tokens=current_tokens,
            messages_length=messages_length,
            system_tokens=system_tokens,
            conversation_tokens=conversation_tokens,
            tool_definitions_tokens=tool_definitions_tokens,
            is_initial=is_initial,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenLimit"] = to_float(self.token_limit)
        result["currentTokens"] = to_float(self.current_tokens)
        result["messagesLength"] = to_float(self.messages_length)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_float], self.system_tokens)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_float], self.conversation_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_float], self.tool_definitions_tokens)
        if self.is_initial is not None:
            result["isInitial"] = from_union([from_none, from_bool], self.is_initial)
        return result


@dataclass
class SessionCompactionStartData:
    "Context window breakdown at the start of LLM-powered conversation compaction"
    system_tokens: float | None = None
    conversation_tokens: float | None = None
    tool_definitions_tokens: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCompactionStartData":
        assert isinstance(obj, dict)
        system_tokens = from_union([from_none, from_float], obj.get("systemTokens"))
        conversation_tokens = from_union([from_none, from_float], obj.get("conversationTokens"))
        tool_definitions_tokens = from_union([from_none, from_float], obj.get("toolDefinitionsTokens"))
        return SessionCompactionStartData(
            system_tokens=system_tokens,
            conversation_tokens=conversation_tokens,
            tool_definitions_tokens=tool_definitions_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_float], self.system_tokens)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_float], self.conversation_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_float], self.tool_definitions_tokens)
        return result


@dataclass
class CompactionCompleteCompactionTokensUsed:
    "Token usage breakdown for the compaction LLM call"
    input: float
    output: float
    cached_input: float

    @staticmethod
    def from_dict(obj: Any) -> "CompactionCompleteCompactionTokensUsed":
        assert isinstance(obj, dict)
        input = from_float(obj.get("input"))
        output = from_float(obj.get("output"))
        cached_input = from_float(obj.get("cachedInput"))
        return CompactionCompleteCompactionTokensUsed(
            input=input,
            output=output,
            cached_input=cached_input,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["input"] = to_float(self.input)
        result["output"] = to_float(self.output)
        result["cachedInput"] = to_float(self.cached_input)
        return result


@dataclass
class SessionCompactionCompleteData:
    "Conversation compaction results including success status, metrics, and optional error details"
    success: bool
    error: str | None = None
    pre_compaction_tokens: float | None = None
    post_compaction_tokens: float | None = None
    pre_compaction_messages_length: float | None = None
    messages_removed: float | None = None
    tokens_removed: float | None = None
    summary_content: str | None = None
    checkpoint_number: float | None = None
    checkpoint_path: str | None = None
    compaction_tokens_used: CompactionCompleteCompactionTokensUsed | None = None
    request_id: str | None = None
    system_tokens: float | None = None
    conversation_tokens: float | None = None
    tool_definitions_tokens: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionCompactionCompleteData":
        assert isinstance(obj, dict)
        success = from_bool(obj.get("success"))
        error = from_union([from_none, from_str], obj.get("error"))
        pre_compaction_tokens = from_union([from_none, from_float], obj.get("preCompactionTokens"))
        post_compaction_tokens = from_union([from_none, from_float], obj.get("postCompactionTokens"))
        pre_compaction_messages_length = from_union([from_none, from_float], obj.get("preCompactionMessagesLength"))
        messages_removed = from_union([from_none, from_float], obj.get("messagesRemoved"))
        tokens_removed = from_union([from_none, from_float], obj.get("tokensRemoved"))
        summary_content = from_union([from_none, from_str], obj.get("summaryContent"))
        checkpoint_number = from_union([from_none, from_float], obj.get("checkpointNumber"))
        checkpoint_path = from_union([from_none, from_str], obj.get("checkpointPath"))
        compaction_tokens_used = from_union([from_none, CompactionCompleteCompactionTokensUsed.from_dict], obj.get("compactionTokensUsed"))
        request_id = from_union([from_none, from_str], obj.get("requestId"))
        system_tokens = from_union([from_none, from_float], obj.get("systemTokens"))
        conversation_tokens = from_union([from_none, from_float], obj.get("conversationTokens"))
        tool_definitions_tokens = from_union([from_none, from_float], obj.get("toolDefinitionsTokens"))
        return SessionCompactionCompleteData(
            success=success,
            error=error,
            pre_compaction_tokens=pre_compaction_tokens,
            post_compaction_tokens=post_compaction_tokens,
            pre_compaction_messages_length=pre_compaction_messages_length,
            messages_removed=messages_removed,
            tokens_removed=tokens_removed,
            summary_content=summary_content,
            checkpoint_number=checkpoint_number,
            checkpoint_path=checkpoint_path,
            compaction_tokens_used=compaction_tokens_used,
            request_id=request_id,
            system_tokens=system_tokens,
            conversation_tokens=conversation_tokens,
            tool_definitions_tokens=tool_definitions_tokens,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["success"] = from_bool(self.success)
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        if self.pre_compaction_tokens is not None:
            result["preCompactionTokens"] = from_union([from_none, to_float], self.pre_compaction_tokens)
        if self.post_compaction_tokens is not None:
            result["postCompactionTokens"] = from_union([from_none, to_float], self.post_compaction_tokens)
        if self.pre_compaction_messages_length is not None:
            result["preCompactionMessagesLength"] = from_union([from_none, to_float], self.pre_compaction_messages_length)
        if self.messages_removed is not None:
            result["messagesRemoved"] = from_union([from_none, to_float], self.messages_removed)
        if self.tokens_removed is not None:
            result["tokensRemoved"] = from_union([from_none, to_float], self.tokens_removed)
        if self.summary_content is not None:
            result["summaryContent"] = from_union([from_none, from_str], self.summary_content)
        if self.checkpoint_number is not None:
            result["checkpointNumber"] = from_union([from_none, to_float], self.checkpoint_number)
        if self.checkpoint_path is not None:
            result["checkpointPath"] = from_union([from_none, from_str], self.checkpoint_path)
        if self.compaction_tokens_used is not None:
            result["compactionTokensUsed"] = from_union([from_none, lambda x: to_class(CompactionCompleteCompactionTokensUsed, x)], self.compaction_tokens_used)
        if self.request_id is not None:
            result["requestId"] = from_union([from_none, from_str], self.request_id)
        if self.system_tokens is not None:
            result["systemTokens"] = from_union([from_none, to_float], self.system_tokens)
        if self.conversation_tokens is not None:
            result["conversationTokens"] = from_union([from_none, to_float], self.conversation_tokens)
        if self.tool_definitions_tokens is not None:
            result["toolDefinitionsTokens"] = from_union([from_none, to_float], self.tool_definitions_tokens)
        return result


@dataclass
class SessionTaskCompleteData:
    "Task completion notification with summary from the agent"
    summary: str | None = None
    success: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionTaskCompleteData":
        assert isinstance(obj, dict)
        summary = from_union([from_none, from_str], obj.get("summary", ""))
        success = from_union([from_none, from_bool], obj.get("success"))
        return SessionTaskCompleteData(
            summary=summary,
            success=success,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.summary is not None:
            result["summary"] = from_union([from_none, from_str], self.summary)
        if self.success is not None:
            result["success"] = from_union([from_none, from_bool], self.success)
        return result


@dataclass
class UserMessageAttachmentFileLineRange:
    "Optional line range to scope the attachment to a specific section of the file"
    start: float
    end: float

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentFileLineRange":
        assert isinstance(obj, dict)
        start = from_float(obj.get("start"))
        end = from_float(obj.get("end"))
        return UserMessageAttachmentFileLineRange(
            start=start,
            end=end,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["start"] = to_float(self.start)
        result["end"] = to_float(self.end)
        return result


@dataclass
class UserMessageAttachmentSelectionDetailsStart:
    "Start position of the selection"
    line: float
    character: float

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelectionDetailsStart":
        assert isinstance(obj, dict)
        line = from_float(obj.get("line"))
        character = from_float(obj.get("character"))
        return UserMessageAttachmentSelectionDetailsStart(
            line=line,
            character=character,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["line"] = to_float(self.line)
        result["character"] = to_float(self.character)
        return result


@dataclass
class UserMessageAttachmentSelectionDetailsEnd:
    "End position of the selection"
    line: float
    character: float

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelectionDetailsEnd":
        assert isinstance(obj, dict)
        line = from_float(obj.get("line"))
        character = from_float(obj.get("character"))
        return UserMessageAttachmentSelectionDetailsEnd(
            line=line,
            character=character,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["line"] = to_float(self.line)
        result["character"] = to_float(self.character)
        return result


@dataclass
class UserMessageAttachmentSelectionDetails:
    "Position range of the selection within the file"
    start: UserMessageAttachmentSelectionDetailsStart
    end: UserMessageAttachmentSelectionDetailsEnd

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachmentSelectionDetails":
        assert isinstance(obj, dict)
        start = UserMessageAttachmentSelectionDetailsStart.from_dict(obj.get("start"))
        end = UserMessageAttachmentSelectionDetailsEnd.from_dict(obj.get("end"))
        return UserMessageAttachmentSelectionDetails(
            start=start,
            end=end,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["start"] = to_class(UserMessageAttachmentSelectionDetailsStart, self.start)
        result["end"] = to_class(UserMessageAttachmentSelectionDetailsEnd, self.end)
        return result


@dataclass
class UserMessageAttachment:
    "A user message attachment — a file, directory, code selection, blob, or GitHub reference"
    type: UserMessageAttachmentType
    path: str | None = None
    display_name: str | None = None
    line_range: UserMessageAttachmentFileLineRange | None = None
    file_path: str | None = None
    text: str | None = None
    selection: UserMessageAttachmentSelectionDetails | None = None
    number: float | None = None
    title: str | None = None
    reference_type: UserMessageAttachmentGithubReferenceType | None = None
    state: str | None = None
    url: str | None = None
    data: str | None = None
    mime_type: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageAttachment":
        assert isinstance(obj, dict)
        type = parse_enum(UserMessageAttachmentType, obj.get("type"))
        path = from_union([from_none, from_str], obj.get("path"))
        display_name = from_union([from_none, from_str], obj.get("displayName"))
        line_range = from_union([from_none, UserMessageAttachmentFileLineRange.from_dict], obj.get("lineRange"))
        file_path = from_union([from_none, from_str], obj.get("filePath"))
        text = from_union([from_none, from_str], obj.get("text"))
        selection = from_union([from_none, UserMessageAttachmentSelectionDetails.from_dict], obj.get("selection"))
        number = from_union([from_none, from_float], obj.get("number"))
        title = from_union([from_none, from_str], obj.get("title"))
        reference_type = from_union([from_none, lambda x: parse_enum(UserMessageAttachmentGithubReferenceType, x)], obj.get("referenceType"))
        state = from_union([from_none, from_str], obj.get("state"))
        url = from_union([from_none, from_str], obj.get("url"))
        data = from_union([from_none, from_str], obj.get("data"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        return UserMessageAttachment(
            type=type,
            path=path,
            display_name=display_name,
            line_range=line_range,
            file_path=file_path,
            text=text,
            selection=selection,
            number=number,
            title=title,
            reference_type=reference_type,
            state=state,
            url=url,
            data=data,
            mime_type=mime_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(UserMessageAttachmentType, self.type)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        if self.display_name is not None:
            result["displayName"] = from_union([from_none, from_str], self.display_name)
        if self.line_range is not None:
            result["lineRange"] = from_union([from_none, lambda x: to_class(UserMessageAttachmentFileLineRange, x)], self.line_range)
        if self.file_path is not None:
            result["filePath"] = from_union([from_none, from_str], self.file_path)
        if self.text is not None:
            result["text"] = from_union([from_none, from_str], self.text)
        if self.selection is not None:
            result["selection"] = from_union([from_none, lambda x: to_class(UserMessageAttachmentSelectionDetails, x)], self.selection)
        if self.number is not None:
            result["number"] = from_union([from_none, to_float], self.number)
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        if self.reference_type is not None:
            result["referenceType"] = from_union([from_none, lambda x: to_enum(UserMessageAttachmentGithubReferenceType, x)], self.reference_type)
        if self.state is not None:
            result["state"] = from_union([from_none, from_str], self.state)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        if self.data is not None:
            result["data"] = from_union([from_none, from_str], self.data)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        return result


@dataclass
class UserMessageData:
    content: str
    transformed_content: str | None = None
    attachments: list[UserMessageAttachment] | None = None
    supported_native_document_mime_types: list[str] | None = None
    native_document_path_fallback_paths: list[str] | None = None
    source: str | None = None
    agent_mode: UserMessageAgentMode | None = None
    interaction_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        transformed_content = from_union([from_none, from_str], obj.get("transformedContent"))
        attachments = from_union([from_none, lambda x: from_list(UserMessageAttachment.from_dict, x)], obj.get("attachments"))
        supported_native_document_mime_types = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("supportedNativeDocumentMimeTypes"))
        native_document_path_fallback_paths = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("nativeDocumentPathFallbackPaths"))
        source = from_union([from_none, from_str], obj.get("source"))
        agent_mode = from_union([from_none, lambda x: parse_enum(UserMessageAgentMode, x)], obj.get("agentMode"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        return UserMessageData(
            content=content,
            transformed_content=transformed_content,
            attachments=attachments,
            supported_native_document_mime_types=supported_native_document_mime_types,
            native_document_path_fallback_paths=native_document_path_fallback_paths,
            source=source,
            agent_mode=agent_mode,
            interaction_id=interaction_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.transformed_content is not None:
            result["transformedContent"] = from_union([from_none, from_str], self.transformed_content)
        if self.attachments is not None:
            result["attachments"] = from_union([from_none, lambda x: from_list(lambda x: to_class(UserMessageAttachment, x), x)], self.attachments)
        if self.supported_native_document_mime_types is not None:
            result["supportedNativeDocumentMimeTypes"] = from_union([from_none, lambda x: from_list(from_str, x)], self.supported_native_document_mime_types)
        if self.native_document_path_fallback_paths is not None:
            result["nativeDocumentPathFallbackPaths"] = from_union([from_none, lambda x: from_list(from_str, x)], self.native_document_path_fallback_paths)
        if self.source is not None:
            result["source"] = from_union([from_none, from_str], self.source)
        if self.agent_mode is not None:
            result["agentMode"] = from_union([from_none, lambda x: to_enum(UserMessageAgentMode, x)], self.agent_mode)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
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
class AssistantReasoningData:
    "Assistant reasoning content for timeline display with complete thinking text"
    reasoning_id: str
    content: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantReasoningData":
        assert isinstance(obj, dict)
        reasoning_id = from_str(obj.get("reasoningId"))
        content = from_str(obj.get("content"))
        return AssistantReasoningData(
            reasoning_id=reasoning_id,
            content=content,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["reasoningId"] = from_str(self.reasoning_id)
        result["content"] = from_str(self.content)
        return result


@dataclass
class AssistantReasoningDeltaData:
    "Streaming reasoning delta for incremental extended thinking updates"
    reasoning_id: str
    delta_content: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantReasoningDeltaData":
        assert isinstance(obj, dict)
        reasoning_id = from_str(obj.get("reasoningId"))
        delta_content = from_str(obj.get("deltaContent"))
        return AssistantReasoningDeltaData(
            reasoning_id=reasoning_id,
            delta_content=delta_content,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["reasoningId"] = from_str(self.reasoning_id)
        result["deltaContent"] = from_str(self.delta_content)
        return result


@dataclass
class AssistantStreamingDeltaData:
    "Streaming response progress with cumulative byte count"
    total_response_size_bytes: float

    @staticmethod
    def from_dict(obj: Any) -> "AssistantStreamingDeltaData":
        assert isinstance(obj, dict)
        total_response_size_bytes = from_float(obj.get("totalResponseSizeBytes"))
        return AssistantStreamingDeltaData(
            total_response_size_bytes=total_response_size_bytes,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["totalResponseSizeBytes"] = to_float(self.total_response_size_bytes)
        return result


@dataclass
class AssistantMessageToolRequest:
    "A tool invocation request from the assistant"
    tool_call_id: str
    name: str
    arguments: Any = None
    type: AssistantMessageToolRequestType | None = None
    tool_title: str | None = None
    mcp_server_name: str | None = None
    intention_summary: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageToolRequest":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        name = from_str(obj.get("name"))
        arguments = obj.get("arguments")
        type = from_union([from_none, lambda x: parse_enum(AssistantMessageToolRequestType, x)], obj.get("type"))
        tool_title = from_union([from_none, from_str], obj.get("toolTitle"))
        mcp_server_name = from_union([from_none, from_str], obj.get("mcpServerName"))
        intention_summary = from_union([from_none, from_str], obj.get("intentionSummary"))
        return AssistantMessageToolRequest(
            tool_call_id=tool_call_id,
            name=name,
            arguments=arguments,
            type=type,
            tool_title=tool_title,
            mcp_server_name=mcp_server_name,
            intention_summary=intention_summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["name"] = from_str(self.name)
        if self.arguments is not None:
            result["arguments"] = self.arguments
        if self.type is not None:
            result["type"] = from_union([from_none, lambda x: to_enum(AssistantMessageToolRequestType, x)], self.type)
        if self.tool_title is not None:
            result["toolTitle"] = from_union([from_none, from_str], self.tool_title)
        if self.mcp_server_name is not None:
            result["mcpServerName"] = from_union([from_none, from_str], self.mcp_server_name)
        if self.intention_summary is not None:
            result["intentionSummary"] = from_union([from_none, from_str], self.intention_summary)
        return result


@dataclass
class AssistantMessageData:
    "Assistant response containing text content, optional tool requests, and interaction metadata"
    message_id: str
    content: str
    tool_requests: list[AssistantMessageToolRequest] | None = None
    reasoning_opaque: str | None = None
    reasoning_text: str | None = None
    encrypted_content: str | None = None
    phase: str | None = None
    output_tokens: float | None = None
    interaction_id: str | None = None
    request_id: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageData":
        assert isinstance(obj, dict)
        message_id = from_str(obj.get("messageId"))
        content = from_str(obj.get("content"))
        tool_requests = from_union([from_none, lambda x: from_list(AssistantMessageToolRequest.from_dict, x)], obj.get("toolRequests"))
        reasoning_opaque = from_union([from_none, from_str], obj.get("reasoningOpaque"))
        reasoning_text = from_union([from_none, from_str], obj.get("reasoningText"))
        encrypted_content = from_union([from_none, from_str], obj.get("encryptedContent"))
        phase = from_union([from_none, from_str], obj.get("phase"))
        output_tokens = from_union([from_none, from_float], obj.get("outputTokens"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        request_id = from_union([from_none, from_str], obj.get("requestId"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return AssistantMessageData(
            message_id=message_id,
            content=content,
            tool_requests=tool_requests,
            reasoning_opaque=reasoning_opaque,
            reasoning_text=reasoning_text,
            encrypted_content=encrypted_content,
            phase=phase,
            output_tokens=output_tokens,
            interaction_id=interaction_id,
            request_id=request_id,
            parent_tool_call_id=parent_tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["messageId"] = from_str(self.message_id)
        result["content"] = from_str(self.content)
        if self.tool_requests is not None:
            result["toolRequests"] = from_union([from_none, lambda x: from_list(lambda x: to_class(AssistantMessageToolRequest, x), x)], self.tool_requests)
        if self.reasoning_opaque is not None:
            result["reasoningOpaque"] = from_union([from_none, from_str], self.reasoning_opaque)
        if self.reasoning_text is not None:
            result["reasoningText"] = from_union([from_none, from_str], self.reasoning_text)
        if self.encrypted_content is not None:
            result["encryptedContent"] = from_union([from_none, from_str], self.encrypted_content)
        if self.phase is not None:
            result["phase"] = from_union([from_none, from_str], self.phase)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([from_none, to_float], self.output_tokens)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.request_id is not None:
            result["requestId"] = from_union([from_none, from_str], self.request_id)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        return result


@dataclass
class AssistantMessageDeltaData:
    "Streaming assistant message delta for incremental response updates"
    message_id: str
    delta_content: str
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantMessageDeltaData":
        assert isinstance(obj, dict)
        message_id = from_str(obj.get("messageId"))
        delta_content = from_str(obj.get("deltaContent"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return AssistantMessageDeltaData(
            message_id=message_id,
            delta_content=delta_content,
            parent_tool_call_id=parent_tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["messageId"] = from_str(self.message_id)
        result["deltaContent"] = from_str(self.delta_content)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
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
class AssistantUsageQuotaSnapshot:
    is_unlimited_entitlement: bool
    entitlement_requests: float
    used_requests: float
    usage_allowed_with_exhausted_quota: bool
    overage: float
    overage_allowed_with_exhausted_quota: bool
    remaining_percentage: float
    reset_date: datetime | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageQuotaSnapshot":
        assert isinstance(obj, dict)
        is_unlimited_entitlement = from_bool(obj.get("isUnlimitedEntitlement"))
        entitlement_requests = from_float(obj.get("entitlementRequests"))
        used_requests = from_float(obj.get("usedRequests"))
        usage_allowed_with_exhausted_quota = from_bool(obj.get("usageAllowedWithExhaustedQuota"))
        overage = from_float(obj.get("overage"))
        overage_allowed_with_exhausted_quota = from_bool(obj.get("overageAllowedWithExhaustedQuota"))
        remaining_percentage = from_float(obj.get("remainingPercentage"))
        reset_date = from_union([from_none, from_datetime], obj.get("resetDate"))
        return AssistantUsageQuotaSnapshot(
            is_unlimited_entitlement=is_unlimited_entitlement,
            entitlement_requests=entitlement_requests,
            used_requests=used_requests,
            usage_allowed_with_exhausted_quota=usage_allowed_with_exhausted_quota,
            overage=overage,
            overage_allowed_with_exhausted_quota=overage_allowed_with_exhausted_quota,
            remaining_percentage=remaining_percentage,
            reset_date=reset_date,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["isUnlimitedEntitlement"] = from_bool(self.is_unlimited_entitlement)
        result["entitlementRequests"] = to_float(self.entitlement_requests)
        result["usedRequests"] = to_float(self.used_requests)
        result["usageAllowedWithExhaustedQuota"] = from_bool(self.usage_allowed_with_exhausted_quota)
        result["overage"] = to_float(self.overage)
        result["overageAllowedWithExhaustedQuota"] = from_bool(self.overage_allowed_with_exhausted_quota)
        result["remainingPercentage"] = to_float(self.remaining_percentage)
        if self.reset_date is not None:
            result["resetDate"] = from_union([from_none, to_datetime], self.reset_date)
        return result


@dataclass
class AssistantUsageCopilotUsageTokenDetail:
    "Token usage detail for a single billing category"
    batch_size: float
    cost_per_batch: float
    token_count: float
    token_type: str

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageCopilotUsageTokenDetail":
        assert isinstance(obj, dict)
        batch_size = from_float(obj.get("batchSize"))
        cost_per_batch = from_float(obj.get("costPerBatch"))
        token_count = from_float(obj.get("tokenCount"))
        token_type = from_str(obj.get("tokenType"))
        return AssistantUsageCopilotUsageTokenDetail(
            batch_size=batch_size,
            cost_per_batch=cost_per_batch,
            token_count=token_count,
            token_type=token_type,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["batchSize"] = to_float(self.batch_size)
        result["costPerBatch"] = to_float(self.cost_per_batch)
        result["tokenCount"] = to_float(self.token_count)
        result["tokenType"] = from_str(self.token_type)
        return result


@dataclass
class AssistantUsageCopilotUsage:
    "Per-request cost and usage data from the CAPI copilot_usage response field"
    token_details: list[AssistantUsageCopilotUsageTokenDetail]
    total_nano_aiu: float

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageCopilotUsage":
        assert isinstance(obj, dict)
        token_details = from_list(AssistantUsageCopilotUsageTokenDetail.from_dict, obj.get("tokenDetails"))
        total_nano_aiu = from_float(obj.get("totalNanoAiu"))
        return AssistantUsageCopilotUsage(
            token_details=token_details,
            total_nano_aiu=total_nano_aiu,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["tokenDetails"] = from_list(lambda x: to_class(AssistantUsageCopilotUsageTokenDetail, x), self.token_details)
        result["totalNanoAiu"] = to_float(self.total_nano_aiu)
        return result


@dataclass
class AssistantUsageData:
    "LLM API call usage metrics including tokens, costs, quotas, and billing information"
    model: str
    input_tokens: float | None = None
    output_tokens: float | None = None
    cache_read_tokens: float | None = None
    cache_write_tokens: float | None = None
    reasoning_tokens: float | None = None
    cost: float | None = None
    duration: float | None = None
    ttft_ms: float | None = None
    inter_token_latency_ms: float | None = None
    initiator: str | None = None
    api_call_id: str | None = None
    provider_call_id: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None
    quota_snapshots: dict[str, AssistantUsageQuotaSnapshot] | None = None
    copilot_usage: AssistantUsageCopilotUsage | None = None
    reasoning_effort: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "AssistantUsageData":
        assert isinstance(obj, dict)
        model = from_str(obj.get("model"))
        input_tokens = from_union([from_none, from_float], obj.get("inputTokens"))
        output_tokens = from_union([from_none, from_float], obj.get("outputTokens"))
        cache_read_tokens = from_union([from_none, from_float], obj.get("cacheReadTokens"))
        cache_write_tokens = from_union([from_none, from_float], obj.get("cacheWriteTokens"))
        reasoning_tokens = from_union([from_none, from_float], obj.get("reasoningTokens"))
        cost = from_union([from_none, from_float], obj.get("cost"))
        duration = from_union([from_none, from_float], obj.get("duration"))
        ttft_ms = from_union([from_none, from_float], obj.get("ttftMs"))
        inter_token_latency_ms = from_union([from_none, from_float], obj.get("interTokenLatencyMs"))
        initiator = from_union([from_none, from_str], obj.get("initiator"))
        api_call_id = from_union([from_none, from_str], obj.get("apiCallId"))
        provider_call_id = from_union([from_none, from_str], obj.get("providerCallId"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        quota_snapshots = from_union([from_none, lambda x: from_dict(AssistantUsageQuotaSnapshot.from_dict, x)], obj.get("quotaSnapshots"))
        copilot_usage = from_union([from_none, AssistantUsageCopilotUsage.from_dict], obj.get("copilotUsage"))
        reasoning_effort = from_union([from_none, from_str], obj.get("reasoningEffort"))
        return AssistantUsageData(
            model=model,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            cache_read_tokens=cache_read_tokens,
            cache_write_tokens=cache_write_tokens,
            reasoning_tokens=reasoning_tokens,
            cost=cost,
            duration=duration,
            ttft_ms=ttft_ms,
            inter_token_latency_ms=inter_token_latency_ms,
            initiator=initiator,
            api_call_id=api_call_id,
            provider_call_id=provider_call_id,
            parent_tool_call_id=parent_tool_call_id,
            quota_snapshots=quota_snapshots,
            copilot_usage=copilot_usage,
            reasoning_effort=reasoning_effort,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["model"] = from_str(self.model)
        if self.input_tokens is not None:
            result["inputTokens"] = from_union([from_none, to_float], self.input_tokens)
        if self.output_tokens is not None:
            result["outputTokens"] = from_union([from_none, to_float], self.output_tokens)
        if self.cache_read_tokens is not None:
            result["cacheReadTokens"] = from_union([from_none, to_float], self.cache_read_tokens)
        if self.cache_write_tokens is not None:
            result["cacheWriteTokens"] = from_union([from_none, to_float], self.cache_write_tokens)
        if self.reasoning_tokens is not None:
            result["reasoningTokens"] = from_union([from_none, to_float], self.reasoning_tokens)
        if self.cost is not None:
            result["cost"] = from_union([from_none, to_float], self.cost)
        if self.duration is not None:
            result["duration"] = from_union([from_none, to_float], self.duration)
        if self.ttft_ms is not None:
            result["ttftMs"] = from_union([from_none, to_float], self.ttft_ms)
        if self.inter_token_latency_ms is not None:
            result["interTokenLatencyMs"] = from_union([from_none, to_float], self.inter_token_latency_ms)
        if self.initiator is not None:
            result["initiator"] = from_union([from_none, from_str], self.initiator)
        if self.api_call_id is not None:
            result["apiCallId"] = from_union([from_none, from_str], self.api_call_id)
        if self.provider_call_id is not None:
            result["providerCallId"] = from_union([from_none, from_str], self.provider_call_id)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        if self.quota_snapshots is not None:
            result["quotaSnapshots"] = from_union([from_none, lambda x: from_dict(lambda x: to_class(AssistantUsageQuotaSnapshot, x), x)], self.quota_snapshots)
        if self.copilot_usage is not None:
            result["copilotUsage"] = from_union([from_none, lambda x: to_class(AssistantUsageCopilotUsage, x)], self.copilot_usage)
        if self.reasoning_effort is not None:
            result["reasoningEffort"] = from_union([from_none, from_str], self.reasoning_effort)
        return result


@dataclass
class AbortData:
    "Turn abort information including the reason for termination"
    reason: str

    @staticmethod
    def from_dict(obj: Any) -> "AbortData":
        assert isinstance(obj, dict)
        reason = from_str(obj.get("reason"))
        return AbortData(
            reason=reason,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["reason"] = from_str(self.reason)
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
class ToolExecutionStartData:
    "Tool execution startup details including MCP server information when applicable"
    tool_call_id: str
    tool_name: str
    arguments: Any = None
    mcp_server_name: str | None = None
    mcp_tool_name: str | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionStartData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        tool_name = from_str(obj.get("toolName"))
        arguments = obj.get("arguments")
        mcp_server_name = from_union([from_none, from_str], obj.get("mcpServerName"))
        mcp_tool_name = from_union([from_none, from_str], obj.get("mcpToolName"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return ToolExecutionStartData(
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
            mcp_server_name=mcp_server_name,
            mcp_tool_name=mcp_tool_name,
            parent_tool_call_id=parent_tool_call_id,
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
        return result


@dataclass
class ToolExecutionPartialResultData:
    "Streaming tool execution output for incremental result display"
    tool_call_id: str
    partial_output: str

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionPartialResultData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        partial_output = from_str(obj.get("partialOutput"))
        return ToolExecutionPartialResultData(
            tool_call_id=tool_call_id,
            partial_output=partial_output,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["partialOutput"] = from_str(self.partial_output)
        return result


@dataclass
class ToolExecutionProgressData:
    "Tool execution progress notification with status message"
    tool_call_id: str
    progress_message: str

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionProgressData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        progress_message = from_str(obj.get("progressMessage"))
        return ToolExecutionProgressData(
            tool_call_id=tool_call_id,
            progress_message=progress_message,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["progressMessage"] = from_str(self.progress_message)
        return result


@dataclass
class ToolExecutionCompleteDataResultContentsItemIconsItem:
    "Icon image for a resource"
    src: str
    mime_type: str | None = None
    sizes: list[str] | None = None
    theme: ToolExecutionCompleteDataResultContentsItemIconsItemTheme | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteDataResultContentsItemIconsItem":
        assert isinstance(obj, dict)
        src = from_str(obj.get("src"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        sizes = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("sizes"))
        theme = from_union([from_none, lambda x: parse_enum(ToolExecutionCompleteDataResultContentsItemIconsItemTheme, x)], obj.get("theme"))
        return ToolExecutionCompleteDataResultContentsItemIconsItem(
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
            result["theme"] = from_union([from_none, lambda x: to_enum(ToolExecutionCompleteDataResultContentsItemIconsItemTheme, x)], self.theme)
        return result


@dataclass
class ToolExecutionCompleteDataResultContentsItem:
    "A content block within a tool result, which may be text, terminal output, image, audio, or a resource"
    type: ToolExecutionCompleteDataResultContentsItemType
    text: str | None = None
    exit_code: float | None = None
    cwd: str | None = None
    data: str | None = None
    mime_type: str | None = None
    icons: list[ToolExecutionCompleteDataResultContentsItemIconsItem] | None = None
    name: str | None = None
    title: str | None = None
    uri: str | None = None
    description: str | None = None
    size: float | None = None
    resource: Any = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteDataResultContentsItem":
        assert isinstance(obj, dict)
        type = parse_enum(ToolExecutionCompleteDataResultContentsItemType, obj.get("type"))
        text = from_union([from_none, from_str], obj.get("text"))
        exit_code = from_union([from_none, from_float], obj.get("exitCode"))
        cwd = from_union([from_none, from_str], obj.get("cwd"))
        data = from_union([from_none, from_str], obj.get("data"))
        mime_type = from_union([from_none, from_str], obj.get("mimeType"))
        icons = from_union([from_none, lambda x: from_list(ToolExecutionCompleteDataResultContentsItemIconsItem.from_dict, x)], obj.get("icons"))
        name = from_union([from_none, from_str], obj.get("name"))
        title = from_union([from_none, from_str], obj.get("title"))
        uri = from_union([from_none, from_str], obj.get("uri"))
        description = from_union([from_none, from_str], obj.get("description"))
        size = from_union([from_none, from_float], obj.get("size"))
        resource = obj.get("resource")
        return ToolExecutionCompleteDataResultContentsItem(
            type=type,
            text=text,
            exit_code=exit_code,
            cwd=cwd,
            data=data,
            mime_type=mime_type,
            icons=icons,
            name=name,
            title=title,
            uri=uri,
            description=description,
            size=size,
            resource=resource,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(ToolExecutionCompleteDataResultContentsItemType, self.type)
        if self.text is not None:
            result["text"] = from_union([from_none, from_str], self.text)
        if self.exit_code is not None:
            result["exitCode"] = from_union([from_none, to_float], self.exit_code)
        if self.cwd is not None:
            result["cwd"] = from_union([from_none, from_str], self.cwd)
        if self.data is not None:
            result["data"] = from_union([from_none, from_str], self.data)
        if self.mime_type is not None:
            result["mimeType"] = from_union([from_none, from_str], self.mime_type)
        if self.icons is not None:
            result["icons"] = from_union([from_none, lambda x: from_list(lambda x: to_class(ToolExecutionCompleteDataResultContentsItemIconsItem, x), x)], self.icons)
        if self.name is not None:
            result["name"] = from_union([from_none, from_str], self.name)
        if self.title is not None:
            result["title"] = from_union([from_none, from_str], self.title)
        if self.uri is not None:
            result["uri"] = from_union([from_none, from_str], self.uri)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.size is not None:
            result["size"] = from_union([from_none, to_float], self.size)
        if self.resource is not None:
            result["resource"] = self.resource
        return result


@dataclass
class ToolExecutionCompleteDataResult:
    "Tool execution result on success"
    content: str
    detailed_content: str | None = None
    contents: list[ToolExecutionCompleteDataResultContentsItem] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteDataResult":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        detailed_content = from_union([from_none, from_str], obj.get("detailedContent"))
        contents = from_union([from_none, lambda x: from_list(ToolExecutionCompleteDataResultContentsItem.from_dict, x)], obj.get("contents"))
        return ToolExecutionCompleteDataResult(
            content=content,
            detailed_content=detailed_content,
            contents=contents,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        if self.detailed_content is not None:
            result["detailedContent"] = from_union([from_none, from_str], self.detailed_content)
        if self.contents is not None:
            result["contents"] = from_union([from_none, lambda x: from_list(lambda x: to_class(ToolExecutionCompleteDataResultContentsItem, x), x)], self.contents)
        return result


@dataclass
class ToolExecutionCompleteDataError:
    "Error details when the tool execution failed"
    message: str
    code: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteDataError":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        code = from_union([from_none, from_str], obj.get("code"))
        return ToolExecutionCompleteDataError(
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
class ToolExecutionCompleteData:
    "Tool execution completion results including success status, detailed output, and error information"
    tool_call_id: str
    success: bool
    model: str | None = None
    interaction_id: str | None = None
    is_user_requested: bool | None = None
    result: ToolExecutionCompleteDataResult | None = None
    error: ToolExecutionCompleteDataError | None = None
    tool_telemetry: dict[str, Any] | None = None
    # Deprecated: this field is deprecated.
    parent_tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ToolExecutionCompleteData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        success = from_bool(obj.get("success"))
        model = from_union([from_none, from_str], obj.get("model"))
        interaction_id = from_union([from_none, from_str], obj.get("interactionId"))
        is_user_requested = from_union([from_none, from_bool], obj.get("isUserRequested"))
        result = from_union([from_none, ToolExecutionCompleteDataResult.from_dict], obj.get("result"))
        error = from_union([from_none, ToolExecutionCompleteDataError.from_dict], obj.get("error"))
        tool_telemetry = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("toolTelemetry"))
        parent_tool_call_id = from_union([from_none, from_str], obj.get("parentToolCallId"))
        return ToolExecutionCompleteData(
            tool_call_id=tool_call_id,
            success=success,
            model=model,
            interaction_id=interaction_id,
            is_user_requested=is_user_requested,
            result=result,
            error=error,
            tool_telemetry=tool_telemetry,
            parent_tool_call_id=parent_tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["success"] = from_bool(self.success)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.interaction_id is not None:
            result["interactionId"] = from_union([from_none, from_str], self.interaction_id)
        if self.is_user_requested is not None:
            result["isUserRequested"] = from_union([from_none, from_bool], self.is_user_requested)
        if self.result is not None:
            result["result"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteDataResult, x)], self.result)
        if self.error is not None:
            result["error"] = from_union([from_none, lambda x: to_class(ToolExecutionCompleteDataError, x)], self.error)
        if self.tool_telemetry is not None:
            result["toolTelemetry"] = from_union([from_none, lambda x: from_dict(lambda x: x, x)], self.tool_telemetry)
        if self.parent_tool_call_id is not None:
            result["parentToolCallId"] = from_union([from_none, from_str], self.parent_tool_call_id)
        return result


@dataclass
class SkillInvokedData:
    "Skill invocation details including content, allowed tools, and plugin metadata"
    name: str
    path: str
    content: str
    allowed_tools: list[str] | None = None
    plugin_name: str | None = None
    plugin_version: str | None = None
    description: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SkillInvokedData":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        path = from_str(obj.get("path"))
        content = from_str(obj.get("content"))
        allowed_tools = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("allowedTools"))
        plugin_name = from_union([from_none, from_str], obj.get("pluginName"))
        plugin_version = from_union([from_none, from_str], obj.get("pluginVersion"))
        description = from_union([from_none, from_str], obj.get("description"))
        return SkillInvokedData(
            name=name,
            path=path,
            content=content,
            allowed_tools=allowed_tools,
            plugin_name=plugin_name,
            plugin_version=plugin_version,
            description=description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["path"] = from_str(self.path)
        result["content"] = from_str(self.content)
        if self.allowed_tools is not None:
            result["allowedTools"] = from_union([from_none, lambda x: from_list(from_str, x)], self.allowed_tools)
        if self.plugin_name is not None:
            result["pluginName"] = from_union([from_none, from_str], self.plugin_name)
        if self.plugin_version is not None:
            result["pluginVersion"] = from_union([from_none, from_str], self.plugin_version)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        return result


@dataclass
class SubagentStartedData:
    "Sub-agent startup details including parent tool call and agent information"
    tool_call_id: str
    agent_name: str
    agent_display_name: str
    agent_description: str

    @staticmethod
    def from_dict(obj: Any) -> "SubagentStartedData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        agent_name = from_str(obj.get("agentName"))
        agent_display_name = from_str(obj.get("agentDisplayName"))
        agent_description = from_str(obj.get("agentDescription"))
        return SubagentStartedData(
            tool_call_id=tool_call_id,
            agent_name=agent_name,
            agent_display_name=agent_display_name,
            agent_description=agent_description,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["agentName"] = from_str(self.agent_name)
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["agentDescription"] = from_str(self.agent_description)
        return result


@dataclass
class SubagentCompletedData:
    "Sub-agent completion details for successful execution"
    tool_call_id: str
    agent_name: str
    agent_display_name: str
    model: str | None = None
    total_tool_calls: float | None = None
    total_tokens: float | None = None
    duration_ms: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentCompletedData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        agent_name = from_str(obj.get("agentName"))
        agent_display_name = from_str(obj.get("agentDisplayName"))
        model = from_union([from_none, from_str], obj.get("model"))
        total_tool_calls = from_union([from_none, from_float], obj.get("totalToolCalls"))
        total_tokens = from_union([from_none, from_float], obj.get("totalTokens"))
        duration_ms = from_union([from_none, from_float], obj.get("durationMs"))
        return SubagentCompletedData(
            tool_call_id=tool_call_id,
            agent_name=agent_name,
            agent_display_name=agent_display_name,
            model=model,
            total_tool_calls=total_tool_calls,
            total_tokens=total_tokens,
            duration_ms=duration_ms,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["agentName"] = from_str(self.agent_name)
        result["agentDisplayName"] = from_str(self.agent_display_name)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.total_tool_calls is not None:
            result["totalToolCalls"] = from_union([from_none, to_float], self.total_tool_calls)
        if self.total_tokens is not None:
            result["totalTokens"] = from_union([from_none, to_float], self.total_tokens)
        if self.duration_ms is not None:
            result["durationMs"] = from_union([from_none, to_float], self.duration_ms)
        return result


@dataclass
class SubagentFailedData:
    "Sub-agent failure details including error message and agent information"
    tool_call_id: str
    agent_name: str
    agent_display_name: str
    error: str
    model: str | None = None
    total_tool_calls: float | None = None
    total_tokens: float | None = None
    duration_ms: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentFailedData":
        assert isinstance(obj, dict)
        tool_call_id = from_str(obj.get("toolCallId"))
        agent_name = from_str(obj.get("agentName"))
        agent_display_name = from_str(obj.get("agentDisplayName"))
        error = from_str(obj.get("error"))
        model = from_union([from_none, from_str], obj.get("model"))
        total_tool_calls = from_union([from_none, from_float], obj.get("totalToolCalls"))
        total_tokens = from_union([from_none, from_float], obj.get("totalTokens"))
        duration_ms = from_union([from_none, from_float], obj.get("durationMs"))
        return SubagentFailedData(
            tool_call_id=tool_call_id,
            agent_name=agent_name,
            agent_display_name=agent_display_name,
            error=error,
            model=model,
            total_tool_calls=total_tool_calls,
            total_tokens=total_tokens,
            duration_ms=duration_ms,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["toolCallId"] = from_str(self.tool_call_id)
        result["agentName"] = from_str(self.agent_name)
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["error"] = from_str(self.error)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        if self.total_tool_calls is not None:
            result["totalToolCalls"] = from_union([from_none, to_float], self.total_tool_calls)
        if self.total_tokens is not None:
            result["totalTokens"] = from_union([from_none, to_float], self.total_tokens)
        if self.duration_ms is not None:
            result["durationMs"] = from_union([from_none, to_float], self.duration_ms)
        return result


@dataclass
class SubagentSelectedData:
    "Custom agent selection details including name and available tools"
    agent_name: str
    agent_display_name: str
    tools: list[str] | None

    @staticmethod
    def from_dict(obj: Any) -> "SubagentSelectedData":
        assert isinstance(obj, dict)
        agent_name = from_str(obj.get("agentName"))
        agent_display_name = from_str(obj.get("agentDisplayName"))
        tools = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("tools"))
        return SubagentSelectedData(
            agent_name=agent_name,
            agent_display_name=agent_display_name,
            tools=tools,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agentName"] = from_str(self.agent_name)
        result["agentDisplayName"] = from_str(self.agent_display_name)
        result["tools"] = from_union([from_none, lambda x: from_list(from_str, x)], self.tools)
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
class HookEndDataError:
    "Error details when the hook failed"
    message: str
    stack: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookEndDataError":
        assert isinstance(obj, dict)
        message = from_str(obj.get("message"))
        stack = from_union([from_none, from_str], obj.get("stack"))
        return HookEndDataError(
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
class HookEndData:
    "Hook invocation completion details including output, success status, and error information"
    hook_invocation_id: str
    hook_type: str
    success: bool
    output: Any = None
    error: HookEndDataError | None = None

    @staticmethod
    def from_dict(obj: Any) -> "HookEndData":
        assert isinstance(obj, dict)
        hook_invocation_id = from_str(obj.get("hookInvocationId"))
        hook_type = from_str(obj.get("hookType"))
        success = from_bool(obj.get("success"))
        output = obj.get("output")
        error = from_union([from_none, HookEndDataError.from_dict], obj.get("error"))
        return HookEndData(
            hook_invocation_id=hook_invocation_id,
            hook_type=hook_type,
            success=success,
            output=output,
            error=error,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["hookInvocationId"] = from_str(self.hook_invocation_id)
        result["hookType"] = from_str(self.hook_type)
        result["success"] = from_bool(self.success)
        if self.output is not None:
            result["output"] = self.output
        if self.error is not None:
            result["error"] = from_union([from_none, lambda x: to_class(HookEndDataError, x)], self.error)
        return result


@dataclass
class SystemMessageDataMetadata:
    "Metadata about the prompt template and its construction"
    prompt_version: str | None = None
    variables: dict[str, Any] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemMessageDataMetadata":
        assert isinstance(obj, dict)
        prompt_version = from_union([from_none, from_str], obj.get("promptVersion"))
        variables = from_union([from_none, lambda x: from_dict(lambda x: x, x)], obj.get("variables"))
        return SystemMessageDataMetadata(
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
class SystemMessageData:
    "System/developer instruction content with role and optional template metadata"
    content: str
    role: SystemMessageDataRole
    name: str | None = None
    metadata: SystemMessageDataMetadata | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemMessageData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        role = parse_enum(SystemMessageDataRole, obj.get("role"))
        name = from_union([from_none, from_str], obj.get("name"))
        metadata = from_union([from_none, SystemMessageDataMetadata.from_dict], obj.get("metadata"))
        return SystemMessageData(
            content=content,
            role=role,
            name=name,
            metadata=metadata,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["role"] = to_enum(SystemMessageDataRole, self.role)
        if self.name is not None:
            result["name"] = from_union([from_none, from_str], self.name)
        if self.metadata is not None:
            result["metadata"] = from_union([from_none, lambda x: to_class(SystemMessageDataMetadata, x)], self.metadata)
        return result


@dataclass
class SystemNotificationDataKind:
    "Structured metadata identifying what triggered this notification"
    type: SystemNotificationDataKindType
    agent_id: str | None = None
    agent_type: str | None = None
    status: SystemNotificationDataKindStatus | None = None
    description: str | None = None
    prompt: str | None = None
    shell_id: str | None = None
    exit_code: float | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationDataKind":
        assert isinstance(obj, dict)
        type = parse_enum(SystemNotificationDataKindType, obj.get("type"))
        agent_id = from_union([from_none, from_str], obj.get("agentId"))
        agent_type = from_union([from_none, from_str], obj.get("agentType"))
        status = from_union([from_none, lambda x: parse_enum(SystemNotificationDataKindStatus, x)], obj.get("status"))
        description = from_union([from_none, from_str], obj.get("description"))
        prompt = from_union([from_none, from_str], obj.get("prompt"))
        shell_id = from_union([from_none, from_str], obj.get("shellId"))
        exit_code = from_union([from_none, from_float], obj.get("exitCode"))
        return SystemNotificationDataKind(
            type=type,
            agent_id=agent_id,
            agent_type=agent_type,
            status=status,
            description=description,
            prompt=prompt,
            shell_id=shell_id,
            exit_code=exit_code,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = to_enum(SystemNotificationDataKindType, self.type)
        if self.agent_id is not None:
            result["agentId"] = from_union([from_none, from_str], self.agent_id)
        if self.agent_type is not None:
            result["agentType"] = from_union([from_none, from_str], self.agent_type)
        if self.status is not None:
            result["status"] = from_union([from_none, lambda x: to_enum(SystemNotificationDataKindStatus, x)], self.status)
        if self.description is not None:
            result["description"] = from_union([from_none, from_str], self.description)
        if self.prompt is not None:
            result["prompt"] = from_union([from_none, from_str], self.prompt)
        if self.shell_id is not None:
            result["shellId"] = from_union([from_none, from_str], self.shell_id)
        if self.exit_code is not None:
            result["exitCode"] = from_union([from_none, to_float], self.exit_code)
        return result


@dataclass
class SystemNotificationData:
    "System-generated notification for runtime events like background task completion"
    content: str
    kind: SystemNotificationDataKind

    @staticmethod
    def from_dict(obj: Any) -> "SystemNotificationData":
        assert isinstance(obj, dict)
        content = from_str(obj.get("content"))
        kind = SystemNotificationDataKind.from_dict(obj.get("kind"))
        return SystemNotificationData(
            content=content,
            kind=kind,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["content"] = from_str(self.content)
        result["kind"] = to_class(SystemNotificationDataKind, self.kind)
        return result


@dataclass
class PermissionRequestShellCommand:
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
class PermissionRequestShellPossibleURL:
    url: str

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestShellPossibleURL":
        assert isinstance(obj, dict)
        url = from_str(obj.get("url"))
        return PermissionRequestShellPossibleURL(
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["url"] = from_str(self.url)
        return result


@dataclass
class PermissionRequest:
    "Details of the permission being requested"
    kind: PermissionRequestedDataPermissionRequestKind
    tool_call_id: str | None = None
    full_command_text: str | None = None
    intention: str | None = None
    commands: list[PermissionRequestShellCommand] | None = None
    possible_paths: list[str] | None = None
    possible_urls: list[PermissionRequestShellPossibleURL] | None = None
    has_write_file_redirection: bool | None = None
    can_offer_session_approval: bool | None = None
    warning: str | None = None
    file_name: str | None = None
    diff: str | None = None
    new_file_contents: str | None = None
    path: str | None = None
    server_name: str | None = None
    tool_name: str | None = None
    tool_title: str | None = None
    args: Any = None
    read_only: bool | None = None
    url: str | None = None
    action: PermissionRequestMemoryAction | None = None
    subject: str | None = None
    fact: str | None = None
    citations: str | None = None
    direction: PermissionRequestMemoryDirection | None = None
    reason: str | None = None
    tool_description: str | None = None
    tool_args: Any = None
    hook_message: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequest":
        assert isinstance(obj, dict)
        kind = parse_enum(PermissionRequestedDataPermissionRequestKind, obj.get("kind"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        full_command_text = from_union([from_none, from_str], obj.get("fullCommandText"))
        intention = from_union([from_none, from_str], obj.get("intention"))
        commands = from_union([from_none, lambda x: from_list(PermissionRequestShellCommand.from_dict, x)], obj.get("commands"))
        possible_paths = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("possiblePaths"))
        possible_urls = from_union([from_none, lambda x: from_list(PermissionRequestShellPossibleURL.from_dict, x)], obj.get("possibleUrls"))
        has_write_file_redirection = from_union([from_none, from_bool], obj.get("hasWriteFileRedirection"))
        can_offer_session_approval = from_union([from_none, from_bool], obj.get("canOfferSessionApproval"))
        warning = from_union([from_none, from_str], obj.get("warning"))
        file_name = from_union([from_none, from_str], obj.get("fileName"))
        diff = from_union([from_none, from_str], obj.get("diff"))
        new_file_contents = from_union([from_none, from_str], obj.get("newFileContents"))
        path = from_union([from_none, from_str], obj.get("path"))
        server_name = from_union([from_none, from_str], obj.get("serverName"))
        tool_name = from_union([from_none, from_str], obj.get("toolName"))
        tool_title = from_union([from_none, from_str], obj.get("toolTitle"))
        args = obj.get("args")
        read_only = from_union([from_none, from_bool], obj.get("readOnly"))
        url = from_union([from_none, from_str], obj.get("url"))
        action = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryAction, x)], obj.get("action", "store"))
        subject = from_union([from_none, from_str], obj.get("subject"))
        fact = from_union([from_none, from_str], obj.get("fact"))
        citations = from_union([from_none, from_str], obj.get("citations"))
        direction = from_union([from_none, lambda x: parse_enum(PermissionRequestMemoryDirection, x)], obj.get("direction"))
        reason = from_union([from_none, from_str], obj.get("reason"))
        tool_description = from_union([from_none, from_str], obj.get("toolDescription"))
        tool_args = obj.get("toolArgs")
        hook_message = from_union([from_none, from_str], obj.get("hookMessage"))
        return PermissionRequest(
            kind=kind,
            tool_call_id=tool_call_id,
            full_command_text=full_command_text,
            intention=intention,
            commands=commands,
            possible_paths=possible_paths,
            possible_urls=possible_urls,
            has_write_file_redirection=has_write_file_redirection,
            can_offer_session_approval=can_offer_session_approval,
            warning=warning,
            file_name=file_name,
            diff=diff,
            new_file_contents=new_file_contents,
            path=path,
            server_name=server_name,
            tool_name=tool_name,
            tool_title=tool_title,
            args=args,
            read_only=read_only,
            url=url,
            action=action,
            subject=subject,
            fact=fact,
            citations=citations,
            direction=direction,
            reason=reason,
            tool_description=tool_description,
            tool_args=tool_args,
            hook_message=hook_message,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionRequestedDataPermissionRequestKind, self.kind)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.full_command_text is not None:
            result["fullCommandText"] = from_union([from_none, from_str], self.full_command_text)
        if self.intention is not None:
            result["intention"] = from_union([from_none, from_str], self.intention)
        if self.commands is not None:
            result["commands"] = from_union([from_none, lambda x: from_list(lambda x: to_class(PermissionRequestShellCommand, x), x)], self.commands)
        if self.possible_paths is not None:
            result["possiblePaths"] = from_union([from_none, lambda x: from_list(from_str, x)], self.possible_paths)
        if self.possible_urls is not None:
            result["possibleUrls"] = from_union([from_none, lambda x: from_list(lambda x: to_class(PermissionRequestShellPossibleURL, x), x)], self.possible_urls)
        if self.has_write_file_redirection is not None:
            result["hasWriteFileRedirection"] = from_union([from_none, from_bool], self.has_write_file_redirection)
        if self.can_offer_session_approval is not None:
            result["canOfferSessionApproval"] = from_union([from_none, from_bool], self.can_offer_session_approval)
        if self.warning is not None:
            result["warning"] = from_union([from_none, from_str], self.warning)
        if self.file_name is not None:
            result["fileName"] = from_union([from_none, from_str], self.file_name)
        if self.diff is not None:
            result["diff"] = from_union([from_none, from_str], self.diff)
        if self.new_file_contents is not None:
            result["newFileContents"] = from_union([from_none, from_str], self.new_file_contents)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        if self.server_name is not None:
            result["serverName"] = from_union([from_none, from_str], self.server_name)
        if self.tool_name is not None:
            result["toolName"] = from_union([from_none, from_str], self.tool_name)
        if self.tool_title is not None:
            result["toolTitle"] = from_union([from_none, from_str], self.tool_title)
        if self.args is not None:
            result["args"] = self.args
        if self.read_only is not None:
            result["readOnly"] = from_union([from_none, from_bool], self.read_only)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
        if self.action is not None:
            result["action"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryAction, x)], self.action)
        if self.subject is not None:
            result["subject"] = from_union([from_none, from_str], self.subject)
        if self.fact is not None:
            result["fact"] = from_union([from_none, from_str], self.fact)
        if self.citations is not None:
            result["citations"] = from_union([from_none, from_str], self.citations)
        if self.direction is not None:
            result["direction"] = from_union([from_none, lambda x: to_enum(PermissionRequestMemoryDirection, x)], self.direction)
        if self.reason is not None:
            result["reason"] = from_union([from_none, from_str], self.reason)
        if self.tool_description is not None:
            result["toolDescription"] = from_union([from_none, from_str], self.tool_description)
        if self.tool_args is not None:
            result["toolArgs"] = self.tool_args
        if self.hook_message is not None:
            result["hookMessage"] = from_union([from_none, from_str], self.hook_message)
        return result


@dataclass
class PermissionRequestedData:
    "Permission request notification requiring client approval with request details"
    request_id: str
    permission_request: PermissionRequest
    resolved_by_hook: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "PermissionRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        permission_request = PermissionRequest.from_dict(obj.get("permissionRequest"))
        resolved_by_hook = from_union([from_none, from_bool], obj.get("resolvedByHook"))
        return PermissionRequestedData(
            request_id=request_id,
            permission_request=permission_request,
            resolved_by_hook=resolved_by_hook,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["permissionRequest"] = to_class(PermissionRequest, self.permission_request)
        if self.resolved_by_hook is not None:
            result["resolvedByHook"] = from_union([from_none, from_bool], self.resolved_by_hook)
        return result


@dataclass
class PermissionCompletedDataResult:
    "The result of the permission request"
    kind: PermissionCompletedKind

    @staticmethod
    def from_dict(obj: Any) -> "PermissionCompletedDataResult":
        assert isinstance(obj, dict)
        kind = parse_enum(PermissionCompletedKind, obj.get("kind"))
        return PermissionCompletedDataResult(
            kind=kind,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = to_enum(PermissionCompletedKind, self.kind)
        return result


@dataclass
class PermissionCompletedData:
    "Permission request completion notification signaling UI dismissal"
    request_id: str
    result: PermissionCompletedDataResult

    @staticmethod
    def from_dict(obj: Any) -> "PermissionCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        result = PermissionCompletedDataResult.from_dict(obj.get("result"))
        return PermissionCompletedData(
            request_id=request_id,
            result=result,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["result"] = to_class(PermissionCompletedDataResult, self.result)
        return result


@dataclass
class UserInputRequestedData:
    "User input request notification with question and optional predefined choices"
    request_id: str
    question: str
    choices: list[str] | None = None
    allow_freeform: bool | None = None
    tool_call_id: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "UserInputRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        question = from_str(obj.get("question"))
        choices = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("choices"))
        allow_freeform = from_union([from_none, from_bool], obj.get("allowFreeform"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        return UserInputRequestedData(
            request_id=request_id,
            question=question,
            choices=choices,
            allow_freeform=allow_freeform,
            tool_call_id=tool_call_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["question"] = from_str(self.question)
        if self.choices is not None:
            result["choices"] = from_union([from_none, lambda x: from_list(from_str, x)], self.choices)
        if self.allow_freeform is not None:
            result["allowFreeform"] = from_union([from_none, from_bool], self.allow_freeform)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
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
class ElicitationRequestedSchema:
    "JSON Schema describing the form fields to present to the user (form mode only)"
    type: str
    properties: dict[str, Any]
    required: list[str] | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ElicitationRequestedSchema":
        assert isinstance(obj, dict)
        type = from_str(obj.get("type"))
        properties = from_dict(lambda x: x, obj.get("properties"))
        required = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("required"))
        return ElicitationRequestedSchema(
            type=type,
            properties=properties,
            required=required,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["type"] = from_str(self.type)
        result["properties"] = from_dict(lambda x: x, self.properties)
        if self.required is not None:
            result["required"] = from_union([from_none, lambda x: from_list(from_str, x)], self.required)
        return result


@dataclass
class ElicitationRequestedData:
    "Elicitation request; may be form-based (structured input) or URL-based (browser redirect)"
    request_id: str
    message: str
    tool_call_id: str | None = None
    elicitation_source: str | None = None
    mode: ElicitationRequestedMode | None = None
    requested_schema: ElicitationRequestedSchema | None = None
    url: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ElicitationRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        message = from_str(obj.get("message"))
        tool_call_id = from_union([from_none, from_str], obj.get("toolCallId"))
        elicitation_source = from_union([from_none, from_str], obj.get("elicitationSource"))
        mode = from_union([from_none, lambda x: parse_enum(ElicitationRequestedMode, x)], obj.get("mode"))
        requested_schema = from_union([from_none, ElicitationRequestedSchema.from_dict], obj.get("requestedSchema"))
        url = from_union([from_none, from_str], obj.get("url"))
        return ElicitationRequestedData(
            request_id=request_id,
            message=message,
            tool_call_id=tool_call_id,
            elicitation_source=elicitation_source,
            mode=mode,
            requested_schema=requested_schema,
            url=url,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["message"] = from_str(self.message)
        if self.tool_call_id is not None:
            result["toolCallId"] = from_union([from_none, from_str], self.tool_call_id)
        if self.elicitation_source is not None:
            result["elicitationSource"] = from_union([from_none, from_str], self.elicitation_source)
        if self.mode is not None:
            result["mode"] = from_union([from_none, lambda x: to_enum(ElicitationRequestedMode, x)], self.mode)
        if self.requested_schema is not None:
            result["requestedSchema"] = from_union([from_none, lambda x: to_class(ElicitationRequestedSchema, x)], self.requested_schema)
        if self.url is not None:
            result["url"] = from_union([from_none, from_str], self.url)
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
class SamplingRequestedData:
    "Sampling request from an MCP server; contains the server name and a requestId for correlation"
    request_id: str
    server_name: str
    mcp_request_id: Any

    @staticmethod
    def from_dict(obj: Any) -> "SamplingRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        mcp_request_id = obj.get("mcpRequestId")
        return SamplingRequestedData(
            request_id=request_id,
            server_name=server_name,
            mcp_request_id=mcp_request_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["serverName"] = from_str(self.server_name)
        result["mcpRequestId"] = self.mcp_request_id
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
class MCPOauthRequiredStaticClientConfig:
    "Static OAuth client configuration, if the server specifies one"
    client_id: str
    public_client: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "MCPOauthRequiredStaticClientConfig":
        assert isinstance(obj, dict)
        client_id = from_str(obj.get("clientId"))
        public_client = from_union([from_none, from_bool], obj.get("publicClient"))
        return MCPOauthRequiredStaticClientConfig(
            client_id=client_id,
            public_client=public_client,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["clientId"] = from_str(self.client_id)
        if self.public_client is not None:
            result["publicClient"] = from_union([from_none, from_bool], self.public_client)
        return result


@dataclass
class McpOauthRequiredData:
    "OAuth authentication request for an MCP server"
    request_id: str
    server_name: str
    server_url: str
    static_client_config: MCPOauthRequiredStaticClientConfig | None = None

    @staticmethod
    def from_dict(obj: Any) -> "McpOauthRequiredData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        server_name = from_str(obj.get("serverName"))
        server_url = from_str(obj.get("serverUrl"))
        static_client_config = from_union([from_none, MCPOauthRequiredStaticClientConfig.from_dict], obj.get("staticClientConfig"))
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
            result["staticClientConfig"] = from_union([from_none, lambda x: to_class(MCPOauthRequiredStaticClientConfig, x)], self.static_client_config)
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
class CommandQueuedData:
    "Queued slash command dispatch request for client execution"
    request_id: str
    command: str

    @staticmethod
    def from_dict(obj: Any) -> "CommandQueuedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        command = from_str(obj.get("command"))
        return CommandQueuedData(
            request_id=request_id,
            command=command,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["command"] = from_str(self.command)
        return result


@dataclass
class CommandExecuteData:
    "Registered command dispatch request routed to the owning client"
    request_id: str
    command: str
    command_name: str
    args: str

    @staticmethod
    def from_dict(obj: Any) -> "CommandExecuteData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        command = from_str(obj.get("command"))
        command_name = from_str(obj.get("commandName"))
        args = from_str(obj.get("args"))
        return CommandExecuteData(
            request_id=request_id,
            command=command,
            command_name=command_name,
            args=args,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["command"] = from_str(self.command)
        result["commandName"] = from_str(self.command_name)
        result["args"] = from_str(self.args)
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
class CommandsChangedCommand:
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
class CapabilitiesChangedUI:
    "UI capability changes"
    elicitation: bool | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CapabilitiesChangedUI":
        assert isinstance(obj, dict)
        elicitation = from_union([from_none, from_bool], obj.get("elicitation"))
        return CapabilitiesChangedUI(
            elicitation=elicitation,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.elicitation is not None:
            result["elicitation"] = from_union([from_none, from_bool], self.elicitation)
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
class ExitPlanModeRequestedData:
    "Plan approval request with plan content and available user actions"
    request_id: str
    summary: str
    plan_content: str
    actions: list[str]
    recommended_action: str

    @staticmethod
    def from_dict(obj: Any) -> "ExitPlanModeRequestedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        summary = from_str(obj.get("summary"))
        plan_content = from_str(obj.get("planContent"))
        actions = from_list(from_str, obj.get("actions"))
        recommended_action = from_str(obj.get("recommendedAction"))
        return ExitPlanModeRequestedData(
            request_id=request_id,
            summary=summary,
            plan_content=plan_content,
            actions=actions,
            recommended_action=recommended_action,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        result["summary"] = from_str(self.summary)
        result["planContent"] = from_str(self.plan_content)
        result["actions"] = from_list(from_str, self.actions)
        result["recommendedAction"] = from_str(self.recommended_action)
        return result


@dataclass
class ExitPlanModeCompletedData:
    "Plan mode exit completion with the user's approval decision and optional feedback"
    request_id: str
    approved: bool | None = None
    selected_action: str | None = None
    auto_approve_edits: bool | None = None
    feedback: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "ExitPlanModeCompletedData":
        assert isinstance(obj, dict)
        request_id = from_str(obj.get("requestId"))
        approved = from_union([from_none, from_bool], obj.get("approved"))
        selected_action = from_union([from_none, from_str], obj.get("selectedAction"))
        auto_approve_edits = from_union([from_none, from_bool], obj.get("autoApproveEdits"))
        feedback = from_union([from_none, from_str], obj.get("feedback"))
        return ExitPlanModeCompletedData(
            request_id=request_id,
            approved=approved,
            selected_action=selected_action,
            auto_approve_edits=auto_approve_edits,
            feedback=feedback,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["requestId"] = from_str(self.request_id)
        if self.approved is not None:
            result["approved"] = from_union([from_none, from_bool], self.approved)
        if self.selected_action is not None:
            result["selectedAction"] = from_union([from_none, from_str], self.selected_action)
        if self.auto_approve_edits is not None:
            result["autoApproveEdits"] = from_union([from_none, from_bool], self.auto_approve_edits)
        if self.feedback is not None:
            result["feedback"] = from_union([from_none, from_str], self.feedback)
        return result


@dataclass
class SessionToolsUpdatedData:
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
class SessionBackgroundTasksChangedData:
    @staticmethod
    def from_dict(obj: Any) -> "SessionBackgroundTasksChangedData":
        assert isinstance(obj, dict)
        return SessionBackgroundTasksChangedData()

    def to_dict(self) -> dict:
        return {}


@dataclass
class SkillsLoadedSkill:
    name: str
    description: str
    source: str
    user_invocable: bool
    enabled: bool
    path: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SkillsLoadedSkill":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        description = from_str(obj.get("description"))
        source = from_str(obj.get("source"))
        user_invocable = from_bool(obj.get("userInvocable"))
        enabled = from_bool(obj.get("enabled"))
        path = from_union([from_none, from_str], obj.get("path"))
        return SkillsLoadedSkill(
            name=name,
            description=description,
            source=source,
            user_invocable=user_invocable,
            enabled=enabled,
            path=path,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["description"] = from_str(self.description)
        result["source"] = from_str(self.source)
        result["userInvocable"] = from_bool(self.user_invocable)
        result["enabled"] = from_bool(self.enabled)
        if self.path is not None:
            result["path"] = from_union([from_none, from_str], self.path)
        return result


@dataclass
class SessionSkillsLoadedData:
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
class CustomAgentsUpdatedAgent:
    id: str
    name: str
    display_name: str
    description: str
    source: str
    tools: list[str]
    user_invocable: bool
    model: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "CustomAgentsUpdatedAgent":
        assert isinstance(obj, dict)
        id = from_str(obj.get("id"))
        name = from_str(obj.get("name"))
        display_name = from_str(obj.get("displayName"))
        description = from_str(obj.get("description"))
        source = from_str(obj.get("source"))
        tools = from_list(from_str, obj.get("tools"))
        user_invocable = from_bool(obj.get("userInvocable"))
        model = from_union([from_none, from_str], obj.get("model"))
        return CustomAgentsUpdatedAgent(
            id=id,
            name=name,
            display_name=display_name,
            description=description,
            source=source,
            tools=tools,
            user_invocable=user_invocable,
            model=model,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = from_str(self.id)
        result["name"] = from_str(self.name)
        result["displayName"] = from_str(self.display_name)
        result["description"] = from_str(self.description)
        result["source"] = from_str(self.source)
        result["tools"] = from_list(from_str, self.tools)
        result["userInvocable"] = from_bool(self.user_invocable)
        if self.model is not None:
            result["model"] = from_union([from_none, from_str], self.model)
        return result


@dataclass
class SessionCustomAgentsUpdatedData:
    agents: list[CustomAgentsUpdatedAgent]
    warnings: list[str]
    errors: list[str]

    @staticmethod
    def from_dict(obj: Any) -> "SessionCustomAgentsUpdatedData":
        assert isinstance(obj, dict)
        agents = from_list(CustomAgentsUpdatedAgent.from_dict, obj.get("agents"))
        warnings = from_list(from_str, obj.get("warnings"))
        errors = from_list(from_str, obj.get("errors"))
        return SessionCustomAgentsUpdatedData(
            agents=agents,
            warnings=warnings,
            errors=errors,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["agents"] = from_list(lambda x: to_class(CustomAgentsUpdatedAgent, x), self.agents)
        result["warnings"] = from_list(from_str, self.warnings)
        result["errors"] = from_list(from_str, self.errors)
        return result


@dataclass
class MCPServersLoadedServer:
    name: str
    status: MCPServerStatus
    source: str | None = None
    error: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "MCPServersLoadedServer":
        assert isinstance(obj, dict)
        name = from_str(obj.get("name"))
        status = parse_enum(MCPServerStatus, obj.get("status"))
        source = from_union([from_none, from_str], obj.get("source"))
        error = from_union([from_none, from_str], obj.get("error"))
        return MCPServersLoadedServer(
            name=name,
            status=status,
            source=source,
            error=error,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["name"] = from_str(self.name)
        result["status"] = to_enum(MCPServerStatus, self.status)
        if self.source is not None:
            result["source"] = from_union([from_none, from_str], self.source)
        if self.error is not None:
            result["error"] = from_union([from_none, from_str], self.error)
        return result


@dataclass
class SessionMcpServersLoadedData:
    servers: list[MCPServersLoadedServer]

    @staticmethod
    def from_dict(obj: Any) -> "SessionMcpServersLoadedData":
        assert isinstance(obj, dict)
        servers = from_list(MCPServersLoadedServer.from_dict, obj.get("servers"))
        return SessionMcpServersLoadedData(
            servers=servers,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["servers"] = from_list(lambda x: to_class(MCPServersLoadedServer, x), self.servers)
        return result


@dataclass
class SessionMcpServerStatusChangedData:
    server_name: str
    status: SessionMcpServerStatusChangedDataStatus

    @staticmethod
    def from_dict(obj: Any) -> "SessionMcpServerStatusChangedData":
        assert isinstance(obj, dict)
        server_name = from_str(obj.get("serverName"))
        status = parse_enum(SessionMcpServerStatusChangedDataStatus, obj.get("status"))
        return SessionMcpServerStatusChangedData(
            server_name=server_name,
            status=status,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["serverName"] = from_str(self.server_name)
        result["status"] = to_enum(SessionMcpServerStatusChangedDataStatus, self.status)
        return result


@dataclass
class ExtensionsLoadedExtension:
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
class SessionExtensionsLoadedData:
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


class WorkingDirectoryContextHostType(Enum):
    "Hosting platform type of the repository (github or ado)"
    GITHUB = "github"
    ADO = "ado"


class SessionPlanChangedDataOperation(Enum):
    "The type of operation performed on the plan file"
    CREATE = "create"
    UPDATE = "update"
    DELETE = "delete"


class SessionWorkspaceFileChangedDataOperation(Enum):
    "Whether the file was newly created or updated"
    CREATE = "create"
    UPDATE = "update"


class HandoffSourceType(Enum):
    "Origin type of the session being handed off"
    REMOTE = "remote"
    LOCAL = "local"


class ShutdownType(Enum):
    "Whether the session ended normally (\"routine\") or due to a crash/fatal error (\"error\")"
    ROUTINE = "routine"
    ERROR = "error"


class SessionContextChangedDataHostType(Enum):
    "Hosting platform type of the repository (github or ado)"
    GITHUB = "github"
    ADO = "ado"


class UserMessageAttachmentType(Enum):
    "A user message attachment — a file, directory, code selection, blob, or GitHub reference discriminator"
    FILE = "file"
    DIRECTORY = "directory"
    SELECTION = "selection"
    GITHUB_REFERENCE = "github_reference"
    BLOB = "blob"


class UserMessageAttachmentGithubReferenceType(Enum):
    "Type of GitHub reference"
    ISSUE = "issue"
    PR = "pr"
    DISCUSSION = "discussion"


class UserMessageAgentMode(Enum):
    "The agent mode that was active when this message was sent"
    INTERACTIVE = "interactive"
    PLAN = "plan"
    AUTOPILOT = "autopilot"
    SHELL = "shell"


class AssistantMessageToolRequestType(Enum):
    "Tool call type: \"function\" for standard tool calls, \"custom\" for grammar-based tool calls. Defaults to \"function\" when absent."
    FUNCTION = "function"
    CUSTOM = "custom"


class ToolExecutionCompleteDataResultContentsItemType(Enum):
    "A content block within a tool result, which may be text, terminal output, image, audio, or a resource discriminator"
    TEXT = "text"
    TERMINAL = "terminal"
    IMAGE = "image"
    AUDIO = "audio"
    RESOURCE_LINK = "resource_link"
    RESOURCE = "resource"


class ToolExecutionCompleteDataResultContentsItemIconsItemTheme(Enum):
    "Theme variant this icon is intended for"
    LIGHT = "light"
    DARK = "dark"


class SystemMessageDataRole(Enum):
    "Message role: \"system\" for system prompts, \"developer\" for developer-injected instructions"
    SYSTEM = "system"
    DEVELOPER = "developer"


class SystemNotificationDataKindType(Enum):
    "Structured metadata identifying what triggered this notification discriminator"
    AGENT_COMPLETED = "agent_completed"
    AGENT_IDLE = "agent_idle"
    SHELL_COMPLETED = "shell_completed"
    SHELL_DETACHED_COMPLETED = "shell_detached_completed"


class SystemNotificationDataKindStatus(Enum):
    "Whether the agent completed successfully or failed"
    COMPLETED = "completed"
    FAILED = "failed"


class PermissionRequestedDataPermissionRequestKind(Enum):
    "Details of the permission being requested discriminator"
    SHELL = "shell"
    WRITE = "write"
    READ = "read"
    MCP = "mcp"
    URL = "url"
    MEMORY = "memory"
    CUSTOM_TOOL = "custom-tool"
    HOOK = "hook"


class PermissionRequestMemoryAction(Enum):
    "Whether this is a store or vote memory operation"
    STORE = "store"
    VOTE = "vote"


class PermissionRequestMemoryDirection(Enum):
    "Vote direction (vote only)"
    UPVOTE = "upvote"
    DOWNVOTE = "downvote"


class PermissionCompletedKind(Enum):
    "The outcome of the permission request"
    APPROVED = "approved"
    DENIED_BY_RULES = "denied-by-rules"
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER = "denied-no-approval-rule-and-could-not-request-from-user"
    DENIED_INTERACTIVELY_BY_USER = "denied-interactively-by-user"
    DENIED_BY_CONTENT_EXCLUSION_POLICY = "denied-by-content-exclusion-policy"
    DENIED_BY_PERMISSION_REQUEST_HOOK = "denied-by-permission-request-hook"


class ElicitationRequestedMode(Enum):
    "Elicitation mode; \"form\" for structured input, \"url\" for browser-based. Defaults to \"form\" when absent."
    FORM = "form"
    URL = "url"


class ElicitationCompletedAction(Enum):
    "The user action: \"accept\" (submitted form), \"decline\" (explicitly refused), or \"cancel\" (dismissed)"
    ACCEPT = "accept"
    DECLINE = "decline"
    CANCEL = "cancel"


class MCPServerStatus(Enum):
    "Connection status: connected, failed, needs-auth, pending, disabled, or not_configured"
    CONNECTED = "connected"
    FAILED = "failed"
    NEEDS_AUTH = "needs-auth"
    PENDING = "pending"
    DISABLED = "disabled"
    NOT_CONFIGURED = "not_configured"


class SessionMcpServerStatusChangedDataStatus(Enum):
    "New connection status: connected, failed, needs-auth, pending, disabled, or not_configured"
    CONNECTED = "connected"
    FAILED = "failed"
    NEEDS_AUTH = "needs-auth"
    PENDING = "pending"
    DISABLED = "disabled"
    NOT_CONFIGURED = "not_configured"


class ExtensionsLoadedExtensionSource(Enum):
    "Discovery source"
    PROJECT = "project"
    USER = "user"


class ExtensionsLoadedExtensionStatus(Enum):
    "Current status: running, disabled, failed, or starting"
    RUNNING = "running"
    DISABLED = "disabled"
    FAILED = "failed"
    STARTING = "starting"


SessionEventData = SessionStartData | SessionResumeData | SessionRemoteSteerableChangedData | SessionErrorData | SessionIdleData | SessionTitleChangedData | SessionInfoData | SessionWarningData | SessionModelChangeData | SessionModeChangedData | SessionPlanChangedData | SessionWorkspaceFileChangedData | SessionHandoffData | SessionTruncationData | SessionSnapshotRewindData | SessionShutdownData | SessionContextChangedData | SessionUsageInfoData | SessionCompactionStartData | SessionCompactionCompleteData | SessionTaskCompleteData | UserMessageData | PendingMessagesModifiedData | AssistantTurnStartData | AssistantIntentData | AssistantReasoningData | AssistantReasoningDeltaData | AssistantStreamingDeltaData | AssistantMessageData | AssistantMessageDeltaData | AssistantTurnEndData | AssistantUsageData | AbortData | ToolUserRequestedData | ToolExecutionStartData | ToolExecutionPartialResultData | ToolExecutionProgressData | ToolExecutionCompleteData | SkillInvokedData | SubagentStartedData | SubagentCompletedData | SubagentFailedData | SubagentSelectedData | SubagentDeselectedData | HookStartData | HookEndData | SystemMessageData | SystemNotificationData | PermissionRequestedData | PermissionCompletedData | UserInputRequestedData | UserInputCompletedData | ElicitationRequestedData | ElicitationCompletedData | SamplingRequestedData | SamplingCompletedData | McpOauthRequiredData | McpOauthCompletedData | ExternalToolRequestedData | ExternalToolCompletedData | CommandQueuedData | CommandExecuteData | CommandCompletedData | CommandsChangedData | CapabilitiesChangedData | ExitPlanModeRequestedData | ExitPlanModeCompletedData | SessionToolsUpdatedData | SessionBackgroundTasksChangedData | SessionSkillsLoadedData | SessionCustomAgentsUpdatedData | SessionMcpServersLoadedData | SessionMcpServerStatusChangedData | SessionExtensionsLoadedData | RawSessionEventData | Data


@dataclass
class SessionEvent:
    data: SessionEventData
    id: UUID
    timestamp: datetime
    type: SessionEventType
    ephemeral: bool | None = None
    parent_id: UUID | None = None
    raw_type: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "SessionEvent":
        assert isinstance(obj, dict)
        raw_type = from_str(obj.get("type"))
        event_type = SessionEventType(raw_type)
        event_id = from_uuid(obj.get("id"))
        timestamp = from_datetime(obj.get("timestamp"))
        ephemeral = from_union([from_bool, from_none], obj.get("ephemeral"))
        parent_id = from_union([from_none, from_uuid], obj.get("parentId"))
        data_obj = obj.get("data")
        match event_type:
            case SessionEventType.SESSION_START: data = SessionStartData.from_dict(data_obj)
            case SessionEventType.SESSION_RESUME: data = SessionResumeData.from_dict(data_obj)
            case SessionEventType.SESSION_REMOTE_STEERABLE_CHANGED: data = SessionRemoteSteerableChangedData.from_dict(data_obj)
            case SessionEventType.SESSION_ERROR: data = SessionErrorData.from_dict(data_obj)
            case SessionEventType.SESSION_IDLE: data = SessionIdleData.from_dict(data_obj)
            case SessionEventType.SESSION_TITLE_CHANGED: data = SessionTitleChangedData.from_dict(data_obj)
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
            case SessionEventType.ASSISTANT_MESSAGE_DELTA: data = AssistantMessageDeltaData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_TURN_END: data = AssistantTurnEndData.from_dict(data_obj)
            case SessionEventType.ASSISTANT_USAGE: data = AssistantUsageData.from_dict(data_obj)
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
            case SessionEventType.EXTERNAL_TOOL_REQUESTED: data = ExternalToolRequestedData.from_dict(data_obj)
            case SessionEventType.EXTERNAL_TOOL_COMPLETED: data = ExternalToolCompletedData.from_dict(data_obj)
            case SessionEventType.COMMAND_QUEUED: data = CommandQueuedData.from_dict(data_obj)
            case SessionEventType.COMMAND_EXECUTE: data = CommandExecuteData.from_dict(data_obj)
            case SessionEventType.COMMAND_COMPLETED: data = CommandCompletedData.from_dict(data_obj)
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
            case _: data = RawSessionEventData.from_dict(data_obj)
        return SessionEvent(
            data=data,
            id=event_id,
            timestamp=timestamp,
            type=event_type,
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
        if self.ephemeral is not None:
            result["ephemeral"] = from_bool(self.ephemeral)
        result["parentId"] = from_union([from_none, to_uuid], self.parent_id)
        return result


def session_event_from_dict(s: Any) -> SessionEvent:
    return SessionEvent.from_dict(s)


def session_event_to_dict(x: SessionEvent) -> Any:
    return x.to_dict()

