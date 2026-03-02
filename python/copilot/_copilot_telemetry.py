"""
Internal OpenTelemetry instrumentation for the Copilot SDK.

Implements the Semantic Conventions for Generative AI systems.
See https://opentelemetry.io/docs/specs/semconv/gen-ai/

This module is private — do not import from outside the copilot package.
"""

from __future__ import annotations

import json
import os
import threading
import time
from collections.abc import Sequence
from dataclasses import dataclass, field
from typing import Any
from urllib.parse import urlparse

from opentelemetry import context as otel_context
from opentelemetry import metrics, trace

from .generated.session_events import SessionEvent, SessionEventType
from .types import ProviderConfig, SystemMessageConfig, TelemetryConfig, Tool

# ---------------------------------------------------------------------------
# SDK version (read once at import time)
# ---------------------------------------------------------------------------


def _get_sdk_version() -> str | None:
    """Get the SDK version, avoiding circular imports.

    This is called lazily (not at module import time) because
    _copilot_telemetry is imported during copilot.__init__, so
    copilot.__version__ isn't available yet at import time.
    """
    try:
        import importlib.metadata

        return importlib.metadata.version("github-copilot-sdk")
    except Exception:
        pass
    try:
        import copilot

        return getattr(copilot, "__version__", None)
    except Exception:
        pass
    return None


_SDK_VERSION: str = _get_sdk_version() or ""

# ---------------------------------------------------------------------------
# Constants (mirrors .NET OpenTelemetryConsts)
# ---------------------------------------------------------------------------

DEFAULT_SOURCE_NAME = "github.copilot.sdk"
DEFAULT_PROVIDER_NAME = "github"
CAPTURE_MESSAGE_CONTENT_ENV_VAR = "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"

# Semantic convention attribute keys
ATTR_GEN_AI_OPERATION_NAME = "gen_ai.operation.name"
ATTR_GEN_AI_PROVIDER_NAME = "gen_ai.provider.name"
ATTR_GEN_AI_AGENT_ID = "gen_ai.agent.id"
ATTR_GEN_AI_AGENT_NAME = "gen_ai.agent.name"
ATTR_GEN_AI_AGENT_DESCRIPTION = "gen_ai.agent.description"
ATTR_GEN_AI_CONVERSATION_ID = "gen_ai.conversation.id"
ATTR_GEN_AI_REQUEST_MODEL = "gen_ai.request.model"
ATTR_GEN_AI_RESPONSE_MODEL = "gen_ai.response.model"
ATTR_GEN_AI_RESPONSE_ID = "gen_ai.response.id"
ATTR_GEN_AI_RESPONSE_FINISH_REASONS = "gen_ai.response.finish_reasons"
ATTR_GEN_AI_USAGE_INPUT_TOKENS = "gen_ai.usage.input_tokens"
ATTR_GEN_AI_USAGE_OUTPUT_TOKENS = "gen_ai.usage.output_tokens"
ATTR_GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS = "gen_ai.usage.cache_read.input_tokens"
ATTR_GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS = "gen_ai.usage.cache_creation.input_tokens"
ATTR_GEN_AI_TOKEN_TYPE = "gen_ai.token.type"
ATTR_GEN_AI_INPUT_MESSAGES = "gen_ai.input.messages"
ATTR_GEN_AI_OUTPUT_MESSAGES = "gen_ai.output.messages"
ATTR_GEN_AI_SYSTEM_INSTRUCTIONS = "gen_ai.system_instructions"
ATTR_GEN_AI_TOOL_DEFINITIONS = "gen_ai.tool.definitions"
ATTR_GEN_AI_TOOL_CALL_ID = "gen_ai.tool.call.id"
ATTR_GEN_AI_TOOL_CALL_ARGUMENTS = "gen_ai.tool.call.arguments"
ATTR_GEN_AI_TOOL_CALL_RESULT = "gen_ai.tool.call.result"
ATTR_GEN_AI_TOOL_NAME = "gen_ai.tool.name"
ATTR_GEN_AI_TOOL_DESCRIPTION = "gen_ai.tool.description"
ATTR_GEN_AI_TOOL_TYPE = "gen_ai.tool.type"
ATTR_SERVER_ADDRESS = "server.address"
ATTR_SERVER_PORT = "server.port"
ATTR_ERROR_TYPE = "error.type"

# Vendor-prefixed span attributes
ATTR_COPILOT_COST = "github.copilot.cost"
ATTR_COPILOT_SERVER_DURATION = "github.copilot.server_duration"
ATTR_COPILOT_INITIATOR = "github.copilot.initiator"
ATTR_COPILOT_AIU = "github.copilot.aiu"
ATTR_COPILOT_TURN_ID = "github.copilot.turn_id"
ATTR_COPILOT_INTERACTION_ID = "github.copilot.interaction_id"

# Vendor-prefixed span event names and attribute keys
COPILOT_EVENT_SESSION_TRUNCATION = "github.copilot.session.truncation"
COPILOT_EVENT_SESSION_COMPACTION_START = "github.copilot.session.compaction_start"
COPILOT_EVENT_SESSION_COMPACTION_COMPLETE = "github.copilot.session.compaction_complete"
COPILOT_EVENT_SKILL_INVOKED = "github.copilot.skill.invoked"

COPILOT_EVENT_ATTR_MESSAGE = "github.copilot.message"
COPILOT_EVENT_ATTR_TOKEN_LIMIT = "github.copilot.token_limit"
COPILOT_EVENT_ATTR_PRE_TOKENS = "github.copilot.pre_tokens"
COPILOT_EVENT_ATTR_POST_TOKENS = "github.copilot.post_tokens"
COPILOT_EVENT_ATTR_PRE_MESSAGES = "github.copilot.pre_messages"
COPILOT_EVENT_ATTR_POST_MESSAGES = "github.copilot.post_messages"
COPILOT_EVENT_ATTR_TOKENS_REMOVED = "github.copilot.tokens_removed"
COPILOT_EVENT_ATTR_MESSAGES_REMOVED = "github.copilot.messages_removed"
COPILOT_EVENT_ATTR_PERFORMED_BY = "github.copilot.performed_by"
COPILOT_EVENT_ATTR_SUCCESS = "github.copilot.success"
COPILOT_EVENT_ATTR_SKILL_NAME = "github.copilot.skill.name"
COPILOT_EVENT_ATTR_SKILL_PATH = "github.copilot.skill.path"
COPILOT_EVENT_ATTR_SKILL_CONTENT = "github.copilot.skill.content"
COPILOT_EVENT_ATTR_SKILL_PLUGIN_NAME = "github.copilot.skill.plugin_name"
COPILOT_EVENT_ATTR_SKILL_PLUGIN_VERSION = "github.copilot.skill.plugin_version"

# Operation names
OP_INVOKE_AGENT = "invoke_agent"
OP_CHAT = "chat"
OP_EXECUTE_TOOL = "execute_tool"

# Token type values
TOKEN_TYPE_INPUT = "input"
TOKEN_TYPE_OUTPUT = "output"

# Metric names
METRIC_OPERATION_DURATION = "gen_ai.client.operation.duration"
METRIC_TOKEN_USAGE = "gen_ai.client.token.usage"
METRIC_TIME_TO_FIRST_CHUNK = "gen_ai.client.operation.time_to_first_chunk"
METRIC_TIME_PER_OUTPUT_CHUNK = "gen_ai.client.operation.time_per_output_chunk"

# Bucket boundaries (matching .NET)
DURATION_BUCKET_BOUNDARIES: Sequence[float] = (
    0.01,
    0.02,
    0.04,
    0.08,
    0.16,
    0.32,
    0.64,
    1.28,
    2.56,
    5.12,
    10.24,
    20.48,
    40.96,
    81.92,
)
TOKEN_BUCKET_BOUNDARIES: Sequence[int] = (
    1,
    4,
    16,
    64,
    256,
    1024,
    4096,
    16384,
    65536,
    262144,
    1048576,
    4194304,
    16777216,
    67108864,
)


# ---------------------------------------------------------------------------
# OTel message types for parts-based JSON
# ---------------------------------------------------------------------------


@dataclass
class OtelPart:
    type: str
    content: str | None = None
    id: str | None = None
    name: str | None = None
    arguments: Any = None
    response: Any = None
    mcp_server_name: str | None = None


@dataclass
class OtelMsg:
    role: str
    parts: list[OtelPart] = field(default_factory=list)
    finish_reason: str | None = None


# ---------------------------------------------------------------------------
# Subagent state
# ---------------------------------------------------------------------------


@dataclass
class _SubagentState:
    invoke_agent_span: trace.Span
    invoke_agent_context: Any  # Context
    invoke_agent_start_time: float
    chat_span: trace.Span | None = None
    agent_name: str | None = None
    response_model: str | None = None
    response_id: str | None = None
    input_tokens: int = 0
    output_tokens: int = 0
    cache_read_tokens: int = 0
    cache_creation_tokens: int = 0
    input_messages: list[OtelMsg] = field(default_factory=list)
    output_messages: list[OtelMsg] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def normalize_provider_name(provider_type: str | None) -> str:
    """Normalize provider type string to OpenTelemetry semantic convention name."""
    if not provider_type or not provider_type.strip():
        return DEFAULT_PROVIDER_NAME
    name = provider_type.strip().lower()
    if name == "anthropic":
        return "anthropic"
    if name == "azure":
        return "azure.ai.openai"
    if name == "openai":
        return "openai"
    return DEFAULT_PROVIDER_NAME


def parse_server_address(base_url: str | None) -> tuple[str | None, int | None]:
    """Extract host and port from a provider base URL."""
    if not base_url or not base_url.strip():
        return None, None
    try:
        parsed = urlparse(base_url)
    except Exception:
        return None, None
    host = parsed.hostname
    if not host:
        return None, None
    port = parsed.port
    if port and port > 0:
        return host, port
    scheme = (parsed.scheme or "").lower()
    if scheme == "https":
        return host, 443
    if scheme == "http":
        return host, 80
    return host, None


def _serialize_value(value: Any) -> str:
    """Convert a value to a string suitable for a span attribute."""
    if isinstance(value, str):
        return value
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, (dict, list)):
        return json.dumps(value, default=str)
    return str(value)


def _get_parent_tool_call_id(event: SessionEvent) -> str | None:
    """Extract parentToolCallId from event data if present."""
    if event.type in (
        SessionEventType.ASSISTANT_USAGE,
        SessionEventType.ASSISTANT_MESSAGE,
        SessionEventType.ASSISTANT_MESSAGE_DELTA,
        SessionEventType.TOOL_EXECUTION_START,
        SessionEventType.TOOL_EXECUTION_COMPLETE,
    ):
        return getattr(event.data, "parent_tool_call_id", None)
    return None


class SessionDestroyedError(Exception):
    """Raised when a session is destroyed while a turn is in progress."""

    ...


class SessionTurnError(Exception):
    """Raised when the session encounters an error or a subagent fails."""

    ...


# ---------------------------------------------------------------------------
# CopilotTelemetry
# ---------------------------------------------------------------------------


class CopilotTelemetry:
    """
    Provides OpenTelemetry instrumentation for the Copilot SDK.

    Implements the Semantic Conventions for Generative AI systems.
    The specification is experimental and subject to change.
    """

    def __init__(
        self,
        config: TelemetryConfig,
        tracer_provider: Any | None = None,
        meter_provider: Any | None = None,
    ) -> None:
        source_name = config.get("source_name") or DEFAULT_SOURCE_NAME

        enable_sensitive = config.get("enable_sensitive_data")
        if enable_sensitive is None:
            env_val = os.environ.get(CAPTURE_MESSAGE_CONTENT_ENV_VAR, "")
            enable_sensitive = env_val.lower() == "true"
        self.enable_sensitive_data: bool = enable_sensitive

        if tracer_provider is not None:
            self._tracer = tracer_provider.get_tracer(source_name, _SDK_VERSION)
        else:
            self._tracer = trace.get_tracer(source_name, _SDK_VERSION)

        if meter_provider is not None:
            self._meter = meter_provider.get_meter(source_name, _SDK_VERSION)
        else:
            self._meter = metrics.get_meter(source_name, _SDK_VERSION)

        self.operation_duration_histogram = self._meter.create_histogram(
            name=METRIC_OPERATION_DURATION,
            unit="s",
            description="GenAI operation duration.",
            explicit_bucket_boundaries_advisory=DURATION_BUCKET_BOUNDARIES,
        )
        self.token_usage_histogram = self._meter.create_histogram(
            name=METRIC_TOKEN_USAGE,
            unit="{token}",
            description="Number of input and output tokens used.",
            explicit_bucket_boundaries_advisory=TOKEN_BUCKET_BOUNDARIES,
        )
        self.time_to_first_chunk_histogram = self._meter.create_histogram(
            name=METRIC_TIME_TO_FIRST_CHUNK,
            unit="s",
            description="Time to receive the first chunk from a streaming response.",
            explicit_bucket_boundaries_advisory=DURATION_BUCKET_BOUNDARIES,
        )
        self.time_per_output_chunk_histogram = self._meter.create_histogram(
            name=METRIC_TIME_PER_OUTPUT_CHUNK,
            unit="s",
            description="Time elapsed between streamed output chunks after the first chunk.",
            explicit_bucket_boundaries_advisory=DURATION_BUCKET_BOUNDARIES,
        )

    # -- Span creation -------------------------------------------------------

    def start_invoke_agent_span(
        self,
        session_id: str,
        model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
        agent_name: str | None = None,
        agent_description: str | None = None,
        parent_context: Any | None = None,
    ) -> tuple[trace.Span, Any]:
        """Start an invoke_agent span (CLIENT kind) for a session turn.

        Returns:
            A tuple of (span, context_with_span) where the context carries
            the span for child span parenting.
        """
        display_name = f"{OP_INVOKE_AGENT} {agent_name}" if agent_name else OP_INVOKE_AGENT

        attributes: dict[str, Any] = {
            ATTR_GEN_AI_OPERATION_NAME: OP_INVOKE_AGENT,
            ATTR_GEN_AI_PROVIDER_NAME: provider_name,
            ATTR_GEN_AI_AGENT_ID: session_id,
            ATTR_GEN_AI_CONVERSATION_ID: session_id,
        }
        if model:
            attributes[ATTR_GEN_AI_REQUEST_MODEL] = model
        if agent_name:
            attributes[ATTR_GEN_AI_AGENT_NAME] = agent_name
        if agent_description:
            attributes[ATTR_GEN_AI_AGENT_DESCRIPTION] = agent_description
        if server_address:
            attributes[ATTR_SERVER_ADDRESS] = server_address
            if server_port is not None:
                attributes[ATTR_SERVER_PORT] = server_port

        span = self._tracer.start_span(
            name=display_name,
            kind=trace.SpanKind.CLIENT,
            attributes=attributes,
            context=parent_context,
        )

        span_context = trace.set_span_in_context(span)
        return span, span_context

    def start_chat_span(
        self,
        model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
        parent_context: Any,
        conversation_id: str | None = None,
    ) -> trace.Span:
        """Start a chat child span for an individual LLM turn."""
        display_name = f"{OP_CHAT} {model}" if model else OP_CHAT

        attributes: dict[str, Any] = {
            ATTR_GEN_AI_OPERATION_NAME: OP_CHAT,
            ATTR_GEN_AI_PROVIDER_NAME: provider_name,
        }
        if model:
            attributes[ATTR_GEN_AI_REQUEST_MODEL] = model
        if conversation_id:
            attributes[ATTR_GEN_AI_CONVERSATION_ID] = conversation_id
        if server_address:
            attributes[ATTR_SERVER_ADDRESS] = server_address
            if server_port is not None:
                attributes[ATTR_SERVER_PORT] = server_port

        return self._tracer.start_span(
            name=display_name,
            kind=trace.SpanKind.CLIENT,
            attributes=attributes,
            context=parent_context,
        )

    def start_execute_tool_span(
        self,
        tool_name: str,
        tool_call_id: str,
        description: str | None,
        arguments: Any,
        parent_context: Any | None = None,
    ) -> trace.Span:
        """Start an execute_tool span (INTERNAL kind) for a tool call."""
        display_name = f"{OP_EXECUTE_TOOL} {tool_name}"

        attributes: dict[str, Any] = {
            ATTR_GEN_AI_OPERATION_NAME: OP_EXECUTE_TOOL,
            ATTR_GEN_AI_TOOL_NAME: tool_name,
            ATTR_GEN_AI_TOOL_CALL_ID: tool_call_id,
            ATTR_GEN_AI_TOOL_TYPE: "function",
        }
        if description:
            attributes[ATTR_GEN_AI_TOOL_DESCRIPTION] = description
        if self.enable_sensitive_data and arguments is not None:
            attributes[ATTR_GEN_AI_TOOL_CALL_ARGUMENTS] = _serialize_value(arguments)

        span = self._tracer.start_span(
            name=display_name,
            kind=trace.SpanKind.INTERNAL,
            context=parent_context or otel_context.get_current(),
            attributes=attributes,
        )

        return span

    # -- Span enrichment -----------------------------------------------------

    def record_token_usage_metrics(
        self,
        input_tokens: int | None,
        output_tokens: int | None,
        request_model: str | None,
        response_model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
        error: Exception | None = None,
        operation_name: str = OP_INVOKE_AGENT,
    ) -> None:
        """Record token usage metrics at turn completion (so error.type can be included)."""
        base_attrs = self._create_metric_attributes(
            operation_name,
            request_model,
            response_model,
            provider_name,
            server_address,
            server_port,
        )
        if error is not None:
            base_attrs[ATTR_ERROR_TYPE] = type(error).__name__
        if input_tokens is not None:
            attrs = dict(base_attrs)
            attrs[ATTR_GEN_AI_TOKEN_TYPE] = TOKEN_TYPE_INPUT
            self.token_usage_histogram.record(int(input_tokens), attrs)
        if output_tokens is not None:
            attrs = dict(base_attrs)
            attrs[ATTR_GEN_AI_TOKEN_TYPE] = TOKEN_TYPE_OUTPUT
            self.token_usage_histogram.record(int(output_tokens), attrs)

    def set_execute_tool_result(self, span: trace.Span, result: Any) -> None:
        """Set tool call result on span (sensitive only)."""
        if not self.enable_sensitive_data or result is None:
            return
        span.set_attribute(ATTR_GEN_AI_TOOL_CALL_RESULT, _serialize_value(result))

    @staticmethod
    def record_error(span: trace.Span, error: Exception) -> None:
        """Record an error on a span."""
        span.set_attribute(ATTR_ERROR_TYPE, type(error).__name__)
        span.set_status(trace.StatusCode.ERROR, str(error))

    # -- Metrics recording ---------------------------------------------------

    def record_operation_duration(
        self,
        duration_seconds: float,
        request_model: str | None,
        response_model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
        error: Exception | None = None,
        operation_name: str = OP_INVOKE_AGENT,
    ) -> None:
        """Record operation duration metric."""
        attrs = self._create_metric_attributes(
            operation_name,
            request_model,
            response_model,
            provider_name,
            server_address,
            server_port,
        )
        if error is not None:
            attrs[ATTR_ERROR_TYPE] = type(error).__name__
        self.operation_duration_histogram.record(duration_seconds, attrs)

    def record_time_to_first_chunk(
        self,
        duration_seconds: float,
        request_model: str | None,
        response_model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
    ) -> None:
        """Record time to first chunk metric."""
        attrs = self._create_metric_attributes(
            OP_CHAT,
            request_model,
            response_model,
            provider_name,
            server_address,
            server_port,
        )
        self.time_to_first_chunk_histogram.record(duration_seconds, attrs)

    def record_time_per_output_chunk(
        self,
        duration_seconds: float,
        request_model: str | None,
        response_model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
    ) -> None:
        """Record time per output chunk metric."""
        attrs = self._create_metric_attributes(
            OP_CHAT,
            request_model,
            response_model,
            provider_name,
            server_address,
            server_port,
        )
        self.time_per_output_chunk_histogram.record(duration_seconds, attrs)

    # -- Internal ------------------------------------------------------------

    @staticmethod
    def _create_metric_attributes(
        operation_name: str,
        request_model: str | None,
        response_model: str | None,
        provider_name: str,
        server_address: str | None,
        server_port: int | None,
    ) -> dict[str, Any]:
        attrs: dict[str, Any] = {
            ATTR_GEN_AI_OPERATION_NAME: operation_name,
            ATTR_GEN_AI_PROVIDER_NAME: provider_name,
        }
        if request_model:
            attrs[ATTR_GEN_AI_REQUEST_MODEL] = request_model
        if response_model:
            attrs[ATTR_GEN_AI_RESPONSE_MODEL] = response_model
        if server_address:
            attrs[ATTR_SERVER_ADDRESS] = server_address
            if server_port is not None:
                attrs[ATTR_SERVER_PORT] = server_port
        return attrs


# ---------------------------------------------------------------------------
# Module-level helpers
# ---------------------------------------------------------------------------


def _build_messages_json(
    messages: list[OtelMsg] | None,
    finish_reason: str | None = None,
) -> str | None:
    """Build JSON array of messages in the GenAI parts-based convention format."""
    if not messages:
        return None

    result: list[dict[str, Any]] = []
    for message in messages:
        if not message.parts:
            continue

        parts: list[dict[str, Any]] = []
        for part in message.parts:
            obj: dict[str, Any] = {"type": part.type}

            if part.type == "server_tool_call":
                if part.id is not None:
                    obj["id"] = part.id
                if part.name is not None:
                    obj["name"] = part.name
                nested: dict[str, Any] = {"type": "mcp"}
                if part.mcp_server_name is not None:
                    nested["server_name"] = part.mcp_server_name
                if part.arguments is not None:
                    nested["arguments"] = part.arguments
                obj["server_tool_call"] = nested
            elif part.type == "server_tool_call_response":
                if part.id is not None:
                    obj["id"] = part.id
                nested = {"type": "mcp"}
                if part.mcp_server_name is not None:
                    nested["server_name"] = part.mcp_server_name
                if part.response is not None:
                    nested["response"] = part.response
                obj["server_tool_call_response"] = nested
            else:
                if part.content is not None:
                    obj["content"] = part.content
                if part.id is not None:
                    obj["id"] = part.id
                if part.name is not None:
                    obj["name"] = part.name
                if part.arguments is not None:
                    obj["arguments"] = part.arguments
                if part.response is not None:
                    obj["response"] = part.response

            parts.append(obj)

        msg: dict[str, Any] = {"role": message.role, "parts": parts}
        effective_finish_reason = message.finish_reason or finish_reason
        if effective_finish_reason:
            msg["finish_reason"] = effective_finish_reason
        result.append(msg)

    return json.dumps(result) if result else None


def _build_system_instructions_json(
    system_message: SystemMessageConfig | None,
) -> str | None:
    """Build JSON for system instructions."""
    if not system_message:
        return None
    content = system_message.get("content")
    if not content:
        return None
    return json.dumps([{"type": "text", "content": content}])


def _build_tool_definitions_json(
    tools: list[Tool] | None,
) -> str | None:
    """Build JSON for tool definitions."""
    if not tools:
        return None
    result = []
    for tool in tools:
        defn: dict[str, Any] = {"type": "function", "name": tool.name}
        if tool.description:
            defn["description"] = tool.description
        if tool.parameters:
            defn["parameters"] = tool.parameters
        result.append(defn)
    return json.dumps(result) if result else None


# ---------------------------------------------------------------------------
# AgentTurnTracker
# ---------------------------------------------------------------------------


class AgentTurnTracker:
    """Encapsulates per-turn telemetry state and logic for a session.

    Implements a two-level span hierarchy: invoke_agent → chat, matching the
    TypeScript/C# AgentTurnTracker. Supports subagent spans and tool call parenting.

    All public methods acquire ``_lock`` for thread safety.
    """

    def __init__(
        self,
        telemetry: CopilotTelemetry,
        session_id: str,
        model: str | None = None,
        provider: ProviderConfig | None = None,
        system_message: SystemMessageConfig | None = None,
        tools: list[Tool] | None = None,
        streaming: bool = False,
        agent_name: str | None = None,
        agent_description: str | None = None,
    ) -> None:
        self._telemetry = telemetry
        self._session_id = session_id
        self._lock = threading.Lock()

        # Session-level context (effectively immutable after construction)
        self._request_model = model
        self._agent_name = agent_name
        self._agent_description = agent_description
        provider_type = provider.get("type") if provider else None
        self._provider_name = normalize_provider_name(provider_type)
        base_url = provider.get("base_url") if provider else None
        self._server_address, self._server_port = parse_server_address(base_url)
        self._system_instructions_json = _build_system_instructions_json(system_message)
        self._tool_definitions_json = _build_tool_definitions_json(tools)
        self._is_streaming = streaming

        # Per-invoke_agent mutable state
        self._agent_span: trace.Span | None = None
        self._agent_span_context: Any | None = None
        self._agent_start_time: float | None = None
        self._agent_input_messages: list[OtelMsg] | None = None
        self._agent_output_messages: list[OtelMsg] | None = None
        self._active_subagents: dict[str, _SubagentState] | None = None

        # Pending tool call parent contexts
        self._pending_tool_parents: dict[str, Any] | None = None

        # MCP server tool tracking: toolCallId → mcpServerName
        self._server_tool_call_ids: dict[str, str] | None = None

        # Agent-level accumulated usage across all chat turns
        self._agent_response_model: str | None = None
        self._agent_response_id: str | None = None
        self._agent_total_input_tokens: int = 0
        self._agent_total_output_tokens: int = 0
        self._agent_total_cache_read_tokens: int = 0
        self._agent_total_cache_creation_tokens: int = 0
        self._agent_total_cost: float = 0
        self._agent_total_aiu: float = 0

        # Per-chat-turn mutable state (reset on each assistant.turn_start)
        self._turn_span: trace.Span | None = None
        self._turn_start_time: float | None = None
        self._first_output_chunk_recorded: bool = False
        self._last_output_chunk_time: float = 0.0
        self._response_model: str | None = None
        self._response_id: str | None = None
        self._input_tokens: int = 0
        self._output_tokens: int = 0
        self._cache_read_tokens: int = 0
        self._cache_creation_tokens: int = 0
        self._input_messages: list[OtelMsg] | None = None
        self._output_messages: list[OtelMsg] | None = None

        # Copilot-specific per-turn attributes
        self._turn_cost: float | None = None
        self._turn_server_duration: float | None = None
        self._turn_initiator: str | None = None
        self._turn_aiu: float | None = None
        self._turn_id: str | None = None
        self._turn_interaction_id: str | None = None

        # Stashed user prompt for the first chat turn
        self._pending_user_prompt: str | None = None

    # -- Property accessors --------------------------------------------------

    @property
    def telemetry(self) -> CopilotTelemetry:
        return self._telemetry

    @property
    def provider_name(self) -> str:
        return self._provider_name

    @property
    def server_address(self) -> str | None:
        return self._server_address

    @property
    def server_port(self) -> int | None:
        return self._server_port

    def get_span_context(self) -> Any | None:
        """Return the current invoke_agent span context (for parenting tool spans)."""
        with self._lock:
            return self._agent_span_context

    def get_tool_call_parent_context(self, tool_call_id: str) -> Any | None:
        """Gets the parent context for a tool call span. Consumes the stored context (one-time use).

        For subagent tool calls this returns the subagent's invoke_agent context;
        for main agent tool calls this returns the root invoke_agent context.
        """
        with self._lock:
            if self._pending_tool_parents:
                ctx = self._pending_tool_parents.get(tool_call_id)
                if ctx is not None:
                    del self._pending_tool_parents[tool_call_id]
                    return ctx
            return self._agent_span_context

    # -- Public methods ------------------------------------------------------

    def begin_send(self, prompt: str | None) -> None:
        """Called at the start of send(); starts the invoke_agent span if needed."""
        with self._lock:
            if self._agent_span is None:
                self._agent_span, self._agent_span_context = (
                    self._telemetry.start_invoke_agent_span(
                        self._session_id,
                        self._request_model,
                        self._provider_name,
                        self._server_address,
                        self._server_port,
                        self._agent_name,
                        self._agent_description,
                    )
                )
                self._agent_start_time = time.monotonic()
                self._agent_input_messages = []

            # Agent-level input = what the caller sent (all user prompts).
            if self._agent_input_messages is not None and prompt:
                self._agent_input_messages.append(
                    OtelMsg(role="user", parts=[OtelPart(type="text", content=prompt)])
                )

            # Stash user prompt for the first chat turn's input messages.
            self._pending_user_prompt = prompt

    def process_event(self, event: SessionEvent) -> None:
        """Called from _dispatch_event; handles telemetry enrichment and turn completion."""
        with self._lock:
            self._process_event_locked(event)

    def complete_turn_with_error(self, error: Exception) -> None:
        """Called from send() error path; completes turn with error."""
        with self._lock:
            self._complete_chat_turn(error)
            self._complete_agent_turn(error)

    def complete_on_dispose(self) -> None:
        """Closes any active spans with an error status.

        Called when the session is destroyed while a turn may still be in progress.
        """
        with self._lock:
            if self._agent_span is not None:
                dispose_error = SessionDestroyedError(
                    "Session destroyed while agent turn was in progress"
                )
                self._complete_chat_turn(dispose_error)
                self._complete_agent_turn(dispose_error)

    # -- Private methods (caller must hold _lock) ----------------------------

    def _process_event_locked(self, event: SessionEvent) -> None:
        """Process a single event under the lock."""
        # Route subagent events by parentToolCallId.
        parent_tool_call_id = _get_parent_tool_call_id(event)
        if parent_tool_call_id:
            if self._active_subagents:
                subagent = self._active_subagents.get(parent_tool_call_id)
                if subagent:
                    self._process_subagent_event(subagent, event)
            return

        # Handle subagent lifecycle events.
        if event.type == SessionEventType.SUBAGENT_STARTED:
            self._begin_subagent(event)
            return
        if event.type == SessionEventType.SUBAGENT_COMPLETED:
            tool_call_id = getattr(event.data, "tool_call_id", None)
            if tool_call_id:
                self._complete_subagent(tool_call_id, None)
            return
        if event.type == SessionEventType.SUBAGENT_FAILED:
            tool_call_id = getattr(event.data, "tool_call_id", None)
            agent_name = getattr(event.data, "agent_name", None) or "unknown"
            error_msg = getattr(event.data, "error", None)
            if isinstance(error_msg, str):
                err_str = error_msg
            elif hasattr(error_msg, "message"):
                err_str = error_msg.message
            else:
                err_str = str(error_msg) if error_msg else "unknown"
            if tool_call_id:
                self._complete_subagent(
                    tool_call_id,
                    SessionTurnError(f"Subagent '{agent_name}' failed: {err_str}"),
                )
            return

        # Record chunk timing for main agent events during a turn.
        self._record_output_chunk_metric()

        # Per-turn event processing (writes to the chat child span).
        if self._turn_span is not None:
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                parts: list[OtelPart] = []
                reasoning_text = getattr(event.data, "reasoning_text", None)
                if reasoning_text:
                    parts.append(OtelPart(type="reasoning", content=reasoning_text))
                content = getattr(event.data, "content", None)
                if content:
                    parts.append(OtelPart(type="text", content=content))
                if parts and self._output_messages is not None:
                    self._output_messages.append(OtelMsg(role="assistant", parts=parts))

            elif event.type == SessionEventType.ASSISTANT_USAGE:
                data = event.data
                model = getattr(data, "model", None)
                if model:
                    self._response_model = model
                api_call_id = getattr(data, "api_call_id", None)
                provider_call_id = getattr(data, "provider_call_id", None)
                if api_call_id:
                    self._response_id = api_call_id
                elif provider_call_id:
                    self._response_id = provider_call_id
                input_tok = getattr(data, "input_tokens", None)
                output_tok = getattr(data, "output_tokens", None)
                cache_read = getattr(data, "cache_read_tokens", None)
                cache_write = getattr(data, "cache_write_tokens", None)
                if input_tok is not None:
                    self._input_tokens += int(input_tok)
                if output_tok is not None:
                    self._output_tokens += int(output_tok)
                if cache_read is not None:
                    self._cache_read_tokens += int(cache_read)
                if cache_write is not None:
                    self._cache_creation_tokens += int(cache_write)

                # Copilot-specific vendor attributes
                cost = getattr(data, "cost", None)
                if cost is not None:
                    self._turn_cost = (self._turn_cost or 0) + cost
                duration = getattr(data, "duration", None)
                if duration is not None:
                    self._turn_server_duration = (self._turn_server_duration or 0) + duration
                initiator = getattr(data, "initiator", None)
                if initiator:
                    self._turn_initiator = initiator
                copilot_usage = getattr(data, "copilot_usage", None)
                if copilot_usage:
                    nano_aiu = getattr(copilot_usage, "total_nano_aiu", None)
                    if nano_aiu is not None:
                        self._turn_aiu = (self._turn_aiu or 0) + nano_aiu

            elif event.type == SessionEventType.SESSION_MODEL_CHANGE:
                new_model = getattr(event.data, "new_model", None)
                if new_model:
                    self._response_model = new_model

            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                data = event.data
                mcp_server_name = getattr(data, "mcp_server_name", None)
                tool_call_id = getattr(data, "tool_call_id", None)
                tool_name = getattr(data, "tool_name", None)
                arguments = getattr(data, "arguments", None)
                is_server_tool = bool(mcp_server_name)

                if is_server_tool and tool_call_id:
                    if self._server_tool_call_ids is None:
                        self._server_tool_call_ids = {}
                    self._server_tool_call_ids[tool_call_id] = str(mcp_server_name)

                if self._output_messages is not None:
                    self._output_messages.append(
                        OtelMsg(
                            role="assistant",
                            parts=[
                                OtelPart(
                                    type="server_tool_call" if is_server_tool else "tool_call",
                                    id=tool_call_id,
                                    name=tool_name,
                                    arguments=arguments,
                                    mcp_server_name=mcp_server_name,
                                )
                            ],
                        )
                    )

                # Store parent context for tool call span parenting.
                if self._agent_span is not None and tool_call_id:
                    if self._pending_tool_parents is None:
                        self._pending_tool_parents = {}
                    self._pending_tool_parents[tool_call_id] = self._agent_span_context

            elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
                data = event.data
                tool_call_id = getattr(data, "tool_call_id", None)
                result_obj = getattr(data, "result", None)
                error_obj = getattr(data, "error", None)
                result_content = None
                if result_obj is not None:
                    result_content = getattr(result_obj, "content", None)
                if result_content is None and error_obj is not None:
                    if isinstance(error_obj, str):
                        result_content = error_obj
                    else:
                        result_content = getattr(error_obj, "message", None)

                server_name: str | None = None
                is_server_tool = False
                if tool_call_id and self._server_tool_call_ids:
                    is_server_tool = tool_call_id in self._server_tool_call_ids
                    if is_server_tool:
                        server_name = self._server_tool_call_ids.pop(tool_call_id, None)

                if self._input_messages is not None:
                    self._input_messages.append(
                        OtelMsg(
                            role="tool",
                            parts=[
                                OtelPart(
                                    type="server_tool_call_response"
                                    if is_server_tool
                                    else "tool_call_response",
                                    id=tool_call_id,
                                    response=result_content,
                                    mcp_server_name=server_name,
                                )
                            ],
                        )
                    )

        # Copilot-specific lifecycle events emitted as span events on the
        # current activity (chat turn if active, otherwise invoke_agent).
        target = self._turn_span or self._agent_span
        if target is not None:
            if event.type == SessionEventType.SESSION_TRUNCATION:
                data = event.data
                attrs: dict[str, Any] = {}
                token_limit = getattr(data, "token_limit", None)
                if token_limit is not None:
                    attrs[COPILOT_EVENT_ATTR_TOKEN_LIMIT] = int(token_limit)
                pre_tokens = getattr(data, "pre_truncation_tokens_in_messages", None)
                if pre_tokens is not None:
                    attrs[COPILOT_EVENT_ATTR_PRE_TOKENS] = int(pre_tokens)
                post_tokens = getattr(data, "post_truncation_tokens_in_messages", None)
                if post_tokens is not None:
                    attrs[COPILOT_EVENT_ATTR_POST_TOKENS] = int(post_tokens)
                pre_messages = getattr(data, "pre_truncation_messages_length", None)
                if pre_messages is not None:
                    attrs[COPILOT_EVENT_ATTR_PRE_MESSAGES] = int(pre_messages)
                post_messages = getattr(data, "post_truncation_messages_length", None)
                if post_messages is not None:
                    attrs[COPILOT_EVENT_ATTR_POST_MESSAGES] = int(post_messages)
                tokens_removed = getattr(data, "tokens_removed_during_truncation", None)
                if tokens_removed is not None:
                    attrs[COPILOT_EVENT_ATTR_TOKENS_REMOVED] = int(tokens_removed)
                messages_removed = getattr(data, "messages_removed_during_truncation", None)
                if messages_removed is not None:
                    attrs[COPILOT_EVENT_ATTR_MESSAGES_REMOVED] = int(messages_removed)
                performed_by = getattr(data, "performed_by", None)
                if performed_by is not None:
                    attrs[COPILOT_EVENT_ATTR_PERFORMED_BY] = performed_by
                target.add_event(COPILOT_EVENT_SESSION_TRUNCATION, attrs)

            elif event.type == SessionEventType.SESSION_COMPACTION_START:
                target.add_event(COPILOT_EVENT_SESSION_COMPACTION_START)

            elif event.type == SessionEventType.SESSION_COMPACTION_COMPLETE:
                data = event.data
                attrs: dict[str, Any] = {
                    COPILOT_EVENT_ATTR_SUCCESS: getattr(data, "success", False),
                }
                if self._telemetry.enable_sensitive_data:
                    error_val = getattr(data, "error", None)
                    if error_val is not None:
                        if isinstance(error_val, str):
                            attrs[COPILOT_EVENT_ATTR_MESSAGE] = error_val
                        elif hasattr(error_val, "message"):
                            attrs[COPILOT_EVENT_ATTR_MESSAGE] = error_val.message
                pre_tokens = getattr(data, "pre_compaction_tokens", None)
                if pre_tokens is not None:
                    attrs[COPILOT_EVENT_ATTR_PRE_TOKENS] = int(pre_tokens)
                post_tokens = getattr(data, "post_compaction_tokens", None)
                if post_tokens is not None:
                    attrs[COPILOT_EVENT_ATTR_POST_TOKENS] = int(post_tokens)
                tokens_removed = getattr(data, "tokens_removed", None)
                if tokens_removed is not None:
                    attrs[COPILOT_EVENT_ATTR_TOKENS_REMOVED] = int(tokens_removed)
                messages_removed = getattr(data, "messages_removed", None)
                if messages_removed is not None:
                    attrs[COPILOT_EVENT_ATTR_MESSAGES_REMOVED] = int(messages_removed)
                target.add_event(COPILOT_EVENT_SESSION_COMPACTION_COMPLETE, attrs)

            elif event.type == SessionEventType.SKILL_INVOKED:
                data = event.data
                skill_attrs: dict[str, str] = {
                    COPILOT_EVENT_ATTR_SKILL_NAME: getattr(data, "name", "") or "",
                    COPILOT_EVENT_ATTR_SKILL_PATH: getattr(data, "path", "") or "",
                }
                plugin_name = getattr(data, "plugin_name", None)
                if plugin_name is not None:
                    skill_attrs[COPILOT_EVENT_ATTR_SKILL_PLUGIN_NAME] = plugin_name
                plugin_version = getattr(data, "plugin_version", None)
                if plugin_version is not None:
                    skill_attrs[COPILOT_EVENT_ATTR_SKILL_PLUGIN_VERSION] = plugin_version
                if self._telemetry.enable_sensitive_data:
                    skill_content = getattr(data, "content", None)
                    if skill_content is not None:
                        skill_attrs[COPILOT_EVENT_ATTR_SKILL_CONTENT] = skill_content
                target.add_event(COPILOT_EVENT_SKILL_INVOKED, skill_attrs)

        # Lifecycle events that start/end turns or the overall agent operation.
        if event.type == SessionEventType.ASSISTANT_TURN_START:
            self._begin_chat_turn()
            self._turn_id = getattr(event.data, "turn_id", None)
            self._turn_interaction_id = getattr(event.data, "interaction_id", None)

        elif event.type == SessionEventType.ASSISTANT_TURN_END:
            self._complete_chat_turn(None)

        elif event.type == SessionEventType.SESSION_IDLE:
            self._complete_chat_turn(None)
            self._complete_agent_turn(None)

        elif event.type == SessionEventType.SESSION_ERROR:
            msg = getattr(event.data, "message", None) or "unknown error"
            ex = SessionTurnError(f"Session error: {msg}")
            self._complete_chat_turn(ex)
            self._complete_agent_turn(ex)

    # ========================================================================
    # Chat turn lifecycle
    # ========================================================================

    def _begin_chat_turn(self) -> None:
        """Starts a new chat child span for an LLM turn."""
        # If there's already an active turn, complete it first.
        self._complete_chat_turn(None)

        self._response_model = None
        self._response_id = None
        self._input_tokens = 0
        self._output_tokens = 0
        self._cache_read_tokens = 0
        self._cache_creation_tokens = 0
        self._first_output_chunk_recorded = False
        self._last_output_chunk_time = 0.0
        self._input_messages = []
        self._output_messages = []
        self._turn_cost = None
        self._turn_server_duration = None
        self._turn_initiator = None
        self._turn_aiu = None
        self._turn_id = None
        self._turn_interaction_id = None

        # Add stashed user prompt as input message for the first turn.
        if self._pending_user_prompt:
            self._input_messages.append(
                OtelMsg(
                    role="user",
                    parts=[OtelPart(type="text", content=self._pending_user_prompt)],
                )
            )
            self._pending_user_prompt = None

        parent_context = self._agent_span_context or otel_context.get_current()
        self._turn_span = self._telemetry.start_chat_span(
            self._request_model,
            self._provider_name,
            self._server_address,
            self._server_port,
            parent_context,
            self._session_id,
        )
        self._turn_start_time = time.monotonic()

    def _complete_chat_turn(self, error: Exception | None) -> None:
        """Completes the current chat child span with per-turn attributes and metrics."""
        span = self._turn_span
        if span is None:
            return

        start_time = self._turn_start_time
        input_messages = self._input_messages
        output_messages = self._output_messages
        response_model = self._response_model
        response_id = self._response_id
        input_tokens = self._input_tokens
        output_tokens = self._output_tokens
        cache_read_tokens = self._cache_read_tokens
        cache_creation_tokens = self._cache_creation_tokens
        turn_cost = self._turn_cost
        turn_server_duration = self._turn_server_duration
        turn_initiator = self._turn_initiator
        turn_aiu = self._turn_aiu
        turn_id = self._turn_id
        turn_interaction_id = self._turn_interaction_id

        # Reset per-turn state
        self._turn_span = None
        self._turn_start_time = None
        self._input_messages = None
        self._output_messages = None
        self._response_model = None
        self._response_id = None
        self._input_tokens = 0
        self._output_tokens = 0
        self._cache_read_tokens = 0
        self._cache_creation_tokens = 0
        self._turn_cost = None
        self._turn_server_duration = None
        self._turn_initiator = None
        self._turn_aiu = None
        self._turn_id = None
        self._turn_interaction_id = None

        if error is not None:
            CopilotTelemetry.record_error(span, error)

        finish_reason = "error" if error is not None else "stop"
        span.set_attribute(ATTR_GEN_AI_RESPONSE_FINISH_REASONS, [finish_reason])

        # Agent-level output = only the final turn's output.
        if output_messages:
            self._agent_output_messages = [
                OtelMsg(role=m.role, parts=m.parts, finish_reason=finish_reason)
                for m in output_messages
            ]

        # Accumulate agent-level usage across turns.
        if response_model:
            self._agent_response_model = response_model
        if response_id:
            self._agent_response_id = response_id
        self._agent_total_input_tokens += input_tokens
        self._agent_total_output_tokens += output_tokens
        self._agent_total_cache_read_tokens += cache_read_tokens
        self._agent_total_cache_creation_tokens += cache_creation_tokens
        if turn_cost is not None:
            self._agent_total_cost += turn_cost
        if turn_aiu is not None:
            self._agent_total_aiu += turn_aiu

        # Set usage-related span attributes for this LLM turn
        if response_model:
            span.set_attribute(ATTR_GEN_AI_RESPONSE_MODEL, response_model)
        if response_id:
            span.set_attribute(ATTR_GEN_AI_RESPONSE_ID, response_id)
        if input_tokens > 0:
            span.set_attribute(ATTR_GEN_AI_USAGE_INPUT_TOKENS, input_tokens)
        if output_tokens > 0:
            span.set_attribute(ATTR_GEN_AI_USAGE_OUTPUT_TOKENS, output_tokens)
        if cache_read_tokens > 0:
            span.set_attribute(ATTR_GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, cache_read_tokens)
        if cache_creation_tokens > 0:
            span.set_attribute(ATTR_GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS, cache_creation_tokens)

        # Copilot vendor-prefixed attributes on chat spans
        if turn_cost is not None:
            span.set_attribute(ATTR_COPILOT_COST, turn_cost)
        if turn_server_duration is not None:
            span.set_attribute(ATTR_COPILOT_SERVER_DURATION, turn_server_duration)
        if turn_initiator is not None:
            span.set_attribute(ATTR_COPILOT_INITIATOR, turn_initiator)
        if turn_aiu is not None:
            span.set_attribute(ATTR_COPILOT_AIU, turn_aiu)
        if turn_id is not None:
            span.set_attribute(ATTR_COPILOT_TURN_ID, turn_id)
        if turn_interaction_id is not None:
            span.set_attribute(ATTR_COPILOT_INTERACTION_ID, turn_interaction_id)

        # Set input/output message content as span attributes (sensitive)
        if self._telemetry.enable_sensitive_data:
            input_json = _build_messages_json(input_messages)
            if input_json:
                span.set_attribute(ATTR_GEN_AI_INPUT_MESSAGES, input_json)
            output_json = _build_messages_json(output_messages, finish_reason)
            if output_json:
                span.set_attribute(ATTR_GEN_AI_OUTPUT_MESSAGES, output_json)

        # Token usage metrics (per-turn)
        self._telemetry.record_token_usage_metrics(
            input_tokens if input_tokens > 0 else None,
            output_tokens if output_tokens > 0 else None,
            self._request_model,
            response_model,
            self._provider_name,
            self._server_address,
            self._server_port,
            error,
            OP_CHAT,
        )

        # Per-turn operation duration
        if start_time is not None:
            duration = time.monotonic() - start_time
            self._telemetry.record_operation_duration(
                duration,
                self._request_model,
                response_model,
                self._provider_name,
                self._server_address,
                self._server_port,
                error,
                OP_CHAT,
            )

        self._first_output_chunk_recorded = False
        self._last_output_chunk_time = 0.0
        span.end()

    # ========================================================================
    # Agent turn lifecycle
    # ========================================================================

    def _complete_agent_turn(self, error: Exception | None) -> None:
        """Completes the invoke_agent span and records overall operation duration."""
        span = self._agent_span
        if span is None:
            return

        start_time = self._agent_start_time
        agent_input_messages = self._agent_input_messages
        agent_output_messages = self._agent_output_messages

        # Reset agent state
        self._agent_span = None
        self._agent_span_context = None
        self._agent_start_time = None
        self._pending_user_prompt = None
        self._agent_input_messages = None
        self._agent_output_messages = None

        # Complete any remaining subagents before closing the parent.
        if self._active_subagents:
            for key in list(self._active_subagents.keys()):
                self._complete_subagent(key, error)
        self._active_subagents = None
        self._pending_tool_parents = None
        self._server_tool_call_ids = None

        if error is not None:
            CopilotTelemetry.record_error(span, error)

        finish_reason = "error" if error is not None else "stop"
        span.set_attribute(ATTR_GEN_AI_RESPONSE_FINISH_REASONS, [finish_reason])

        # Set accumulated usage across all chat turns on the invoke_agent span.
        agent_response_model = self._agent_response_model
        agent_response_id = self._agent_response_id
        agent_total_input_tokens = self._agent_total_input_tokens
        agent_total_output_tokens = self._agent_total_output_tokens
        agent_total_cache_read_tokens = self._agent_total_cache_read_tokens
        agent_total_cache_creation_tokens = self._agent_total_cache_creation_tokens
        agent_total_cost = self._agent_total_cost
        agent_total_aiu = self._agent_total_aiu

        self._agent_response_model = None
        self._agent_response_id = None
        self._agent_total_input_tokens = 0
        self._agent_total_output_tokens = 0
        self._agent_total_cache_read_tokens = 0
        self._agent_total_cache_creation_tokens = 0
        self._agent_total_cost = 0
        self._agent_total_aiu = 0

        if agent_response_model:
            span.set_attribute(ATTR_GEN_AI_RESPONSE_MODEL, agent_response_model)
        if agent_response_id:
            span.set_attribute(ATTR_GEN_AI_RESPONSE_ID, agent_response_id)
        if agent_total_input_tokens > 0:
            span.set_attribute(ATTR_GEN_AI_USAGE_INPUT_TOKENS, agent_total_input_tokens)
        if agent_total_output_tokens > 0:
            span.set_attribute(ATTR_GEN_AI_USAGE_OUTPUT_TOKENS, agent_total_output_tokens)
        if agent_total_cache_read_tokens > 0:
            span.set_attribute(
                ATTR_GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, agent_total_cache_read_tokens
            )
        if agent_total_cache_creation_tokens > 0:
            span.set_attribute(
                ATTR_GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS, agent_total_cache_creation_tokens
            )

        # Copilot vendor-prefixed attributes on invoke_agent span
        if agent_total_cost > 0:
            span.set_attribute(ATTR_COPILOT_COST, agent_total_cost)
        if agent_total_aiu > 0:
            span.set_attribute(ATTR_COPILOT_AIU, agent_total_aiu)

        # Agent-level input = caller's message; output = agent's final response.
        if self._telemetry.enable_sensitive_data:
            input_json = _build_messages_json(agent_input_messages)
            if input_json:
                span.set_attribute(ATTR_GEN_AI_INPUT_MESSAGES, input_json)
            output_json = _build_messages_json(agent_output_messages)
            if output_json:
                span.set_attribute(ATTR_GEN_AI_OUTPUT_MESSAGES, output_json)
            if self._system_instructions_json:
                span.set_attribute(ATTR_GEN_AI_SYSTEM_INSTRUCTIONS, self._system_instructions_json)
        if self._tool_definitions_json:
            span.set_attribute(ATTR_GEN_AI_TOOL_DEFINITIONS, self._tool_definitions_json)

        if start_time is not None:
            duration = time.monotonic() - start_time
            self._telemetry.record_operation_duration(
                duration,
                self._request_model,
                agent_response_model,
                self._provider_name,
                self._server_address,
                self._server_port,
                error,
                OP_INVOKE_AGENT,
            )

        span.end()

    # ========================================================================
    # Streaming chunk timing
    # ========================================================================

    def _record_output_chunk_metric(self) -> None:
        """Records streaming chunk timing metrics. Uses the chat turn stopwatch."""
        if not self._is_streaming or self._turn_start_time is None:
            return

        now = time.monotonic()
        elapsed = now - self._turn_start_time

        if not self._first_output_chunk_recorded:
            self._first_output_chunk_recorded = True
            self._last_output_chunk_time = now
            self._telemetry.record_time_to_first_chunk(
                elapsed,
                self._request_model,
                None,  # response model not yet known during streaming
                self._provider_name,
                self._server_address,
                self._server_port,
            )
            return

        delta = now - self._last_output_chunk_time
        self._last_output_chunk_time = now
        self._telemetry.record_time_per_output_chunk(
            delta,
            self._request_model,
            None,  # response model not yet known during streaming
            self._provider_name,
            self._server_address,
            self._server_port,
        )

    # ========================================================================
    # Subagent support
    # ========================================================================

    def _begin_subagent(self, event: SessionEvent) -> None:
        """Creates nested invoke_agent + chat spans for a subagent."""
        data = event.data
        if data is None:
            return

        tool_call_id = getattr(data, "tool_call_id", None)
        if not tool_call_id:
            return

        agent_name = getattr(data, "agent_name", None)
        agent_description = getattr(data, "agent_description", None)

        parent_context = self._agent_span_context or otel_context.get_current()
        invoke_span, invoke_ctx = self._telemetry.start_invoke_agent_span(
            self._session_id,
            self._request_model,
            self._provider_name,
            self._server_address,
            self._server_port,
            agent_name,
            agent_description,
            parent_context,
        )

        chat_span = self._telemetry.start_chat_span(
            self._request_model,
            self._provider_name,
            self._server_address,
            self._server_port,
            invoke_ctx,
            self._session_id,
        )

        state = _SubagentState(
            invoke_agent_span=invoke_span,
            invoke_agent_context=invoke_ctx,
            invoke_agent_start_time=time.monotonic(),
            chat_span=chat_span,
            agent_name=agent_name,
        )

        if self._active_subagents is None:
            self._active_subagents = {}
        self._active_subagents[tool_call_id] = state

    def _process_subagent_event(self, subagent: _SubagentState, event: SessionEvent) -> None:
        """Routes an event to its owning subagent's spans."""
        if event.type == SessionEventType.ASSISTANT_USAGE:
            data = event.data
            model = getattr(data, "model", None)
            if model:
                subagent.response_model = model
                if subagent.chat_span is not None:
                    subagent.chat_span.set_attribute(ATTR_GEN_AI_RESPONSE_MODEL, model)

            api_call_id = getattr(data, "api_call_id", None)
            provider_call_id = getattr(data, "provider_call_id", None)
            if api_call_id:
                subagent.response_id = api_call_id
            elif provider_call_id:
                subagent.response_id = provider_call_id
            input_tok = getattr(data, "input_tokens", None)
            output_tok = getattr(data, "output_tokens", None)
            cache_read = getattr(data, "cache_read_tokens", None)
            cache_write = getattr(data, "cache_write_tokens", None)
            if input_tok is not None:
                subagent.input_tokens += int(input_tok)
            if output_tok is not None:
                subagent.output_tokens += int(output_tok)
            if cache_read is not None:
                subagent.cache_read_tokens += int(cache_read)
            if cache_write is not None:
                subagent.cache_creation_tokens += int(cache_write)

        elif event.type == SessionEventType.ASSISTANT_MESSAGE:
            data = event.data
            parts: list[OtelPart] = []
            reasoning_text = getattr(data, "reasoning_text", None)
            if reasoning_text:
                parts.append(OtelPart(type="reasoning", content=reasoning_text))
            content = getattr(data, "content", None)
            if content:
                parts.append(OtelPart(type="text", content=content))
            if parts:
                subagent.output_messages.append(OtelMsg(role="assistant", parts=parts))

        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            data = event.data
            mcp_server_name = getattr(data, "mcp_server_name", None)
            tool_call_id = getattr(data, "tool_call_id", None)
            is_server_tool = bool(mcp_server_name)
            if is_server_tool and tool_call_id:
                if self._server_tool_call_ids is None:
                    self._server_tool_call_ids = {}
                self._server_tool_call_ids[tool_call_id] = str(mcp_server_name)

            subagent.output_messages.append(
                OtelMsg(
                    role="assistant",
                    parts=[
                        OtelPart(
                            type="server_tool_call" if is_server_tool else "tool_call",
                            id=tool_call_id,
                            name=getattr(data, "tool_name", None),
                            arguments=getattr(data, "arguments", None),
                            mcp_server_name=mcp_server_name,
                        )
                    ],
                )
            )

            # For subagent tool calls, parent is the subagent's invoke_agent.
            if tool_call_id:
                if self._pending_tool_parents is None:
                    self._pending_tool_parents = {}
                self._pending_tool_parents[tool_call_id] = subagent.invoke_agent_context

        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            data = event.data
            tool_call_id = getattr(data, "tool_call_id", None)
            result_obj = getattr(data, "result", None)
            error_obj = getattr(data, "error", None)
            result_content = None
            if result_obj is not None:
                result_content = getattr(result_obj, "content", None)
            if result_content is None and error_obj is not None:
                if isinstance(error_obj, str):
                    result_content = error_obj
                else:
                    result_content = getattr(error_obj, "message", None)

            server_name: str | None = None
            is_server_tool = False
            if tool_call_id and self._server_tool_call_ids:
                is_server_tool = tool_call_id in self._server_tool_call_ids
                if is_server_tool:
                    server_name = self._server_tool_call_ids.pop(tool_call_id, None)

            subagent.input_messages.append(
                OtelMsg(
                    role="tool",
                    parts=[
                        OtelPart(
                            type="server_tool_call_response"
                            if is_server_tool
                            else "tool_call_response",
                            id=tool_call_id,
                            response=result_content,
                            mcp_server_name=server_name,
                        )
                    ],
                )
            )

    def _complete_subagent(self, tool_call_id: str, error: Exception | None) -> None:
        """Completes a subagent's chat and invoke_agent spans."""
        if not self._active_subagents:
            return
        subagent = self._active_subagents.pop(tool_call_id, None)
        if subagent is None:
            return

        finish_reason = "error" if error else "stop"

        # -- Complete the chat child span --
        chat_span = subagent.chat_span
        if chat_span is not None:
            if error:
                CopilotTelemetry.record_error(chat_span, error)
            chat_span.set_attribute(ATTR_GEN_AI_RESPONSE_FINISH_REASONS, [finish_reason])

            if subagent.response_model:
                chat_span.set_attribute(ATTR_GEN_AI_RESPONSE_MODEL, subagent.response_model)
            if subagent.response_id:
                chat_span.set_attribute(ATTR_GEN_AI_RESPONSE_ID, subagent.response_id)
            if subagent.input_tokens > 0:
                chat_span.set_attribute(ATTR_GEN_AI_USAGE_INPUT_TOKENS, subagent.input_tokens)
            if subagent.output_tokens > 0:
                chat_span.set_attribute(ATTR_GEN_AI_USAGE_OUTPUT_TOKENS, subagent.output_tokens)
            if subagent.cache_read_tokens > 0:
                chat_span.set_attribute(
                    ATTR_GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, subagent.cache_read_tokens
                )
            if subagent.cache_creation_tokens > 0:
                chat_span.set_attribute(
                    ATTR_GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS, subagent.cache_creation_tokens
                )

            if self._telemetry.enable_sensitive_data:
                if subagent.input_messages:
                    input_json = _build_messages_json(subagent.input_messages)
                    if input_json:
                        chat_span.set_attribute(ATTR_GEN_AI_INPUT_MESSAGES, input_json)
                if subagent.output_messages:
                    output_json = _build_messages_json(subagent.output_messages, finish_reason)
                    if output_json:
                        chat_span.set_attribute(ATTR_GEN_AI_OUTPUT_MESSAGES, output_json)

            self._telemetry.record_token_usage_metrics(
                subagent.input_tokens if subagent.input_tokens > 0 else None,
                subagent.output_tokens if subagent.output_tokens > 0 else None,
                subagent.response_model or self._request_model,
                subagent.response_model,
                self._provider_name,
                self._server_address,
                self._server_port,
                error,
                OP_CHAT,
            )

            chat_span.end()

        # -- Complete the invoke_agent span --
        invoke_span = subagent.invoke_agent_span
        if error:
            CopilotTelemetry.record_error(invoke_span, error)
        invoke_span.set_attribute(ATTR_GEN_AI_RESPONSE_FINISH_REASONS, [finish_reason])

        if subagent.response_model:
            invoke_span.set_attribute(ATTR_GEN_AI_RESPONSE_MODEL, subagent.response_model)
        if subagent.response_id:
            invoke_span.set_attribute(ATTR_GEN_AI_RESPONSE_ID, subagent.response_id)
        if subagent.input_tokens > 0:
            invoke_span.set_attribute(ATTR_GEN_AI_USAGE_INPUT_TOKENS, subagent.input_tokens)
        if subagent.output_tokens > 0:
            invoke_span.set_attribute(ATTR_GEN_AI_USAGE_OUTPUT_TOKENS, subagent.output_tokens)
        if subagent.cache_read_tokens > 0:
            invoke_span.set_attribute(
                ATTR_GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, subagent.cache_read_tokens
            )
        if subagent.cache_creation_tokens > 0:
            invoke_span.set_attribute(
                ATTR_GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS, subagent.cache_creation_tokens
            )

        if self._telemetry.enable_sensitive_data and subagent.output_messages:
            output_json = _build_messages_json(
                [
                    OtelMsg(role=m.role, parts=m.parts, finish_reason=finish_reason)
                    for m in subagent.output_messages
                ]
            )
            if output_json:
                invoke_span.set_attribute(ATTR_GEN_AI_OUTPUT_MESSAGES, output_json)

        duration = time.monotonic() - subagent.invoke_agent_start_time
        self._telemetry.record_operation_duration(
            duration,
            subagent.response_model or self._request_model,
            subagent.response_model,
            self._provider_name,
            self._server_address,
            self._server_port,
            error,
            OP_INVOKE_AGENT,
        )

        invoke_span.end()
