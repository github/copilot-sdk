"""
E2E coverage for OpenTelemetry file-exporter integration.

Mirrors ``dotnet/test/TelemetryExportTests.cs`` (snapshot category ``telemetry``):
configures a dedicated client with file-based telemetry, runs a single SDK turn
that calls a custom tool, and validates the exported JSONL spans (root
``invoke_agent``, child ``chat`` and ``execute_tool`` spans, attributes).

Also includes the unit-style coverage from ``dotnet/test/TelemetryTests.cs``:
``TelemetryConfig`` defaults / setters, ``SubprocessConfig.telemetry`` default,
and W3C trace context propagation via ``copilot._telemetry``.
"""

from __future__ import annotations

import json
import os
import uuid
from pathlib import Path
from typing import Any

import pytest

from copilot import CopilotClient, RuntimeConnection, TelemetryConfig
from copilot._telemetry import get_trace_context, trace_context
from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _string_attribute(entry: dict[str, Any], name: str) -> str | None:
    attrs = entry.get("attributes") or {}
    value = attrs.get(name)
    if value is None:
        return None
    return value if isinstance(value, str) else json.dumps(value)


def _is_root_span(entry: dict[str, Any]) -> bool:
    parent = entry.get("parentSpanId") or ""
    return parent in ("", "0000000000000000")


def _read_telemetry_entries(path: Path) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        entries.append(json.loads(line))
    return entries


class TestTelemetryExport:
    async def test_should_export_file_telemetry_for_sdk_interactions(self, ctx: E2ETestContext):
        telemetry_path = Path(ctx.work_dir) / f"telemetry-{uuid.uuid4().hex}.jsonl"
        marker = "copilot-sdk-telemetry-e2e"
        source_name = "python-sdk-telemetry-e2e"
        tool_name = "echo_telemetry_marker"
        prompt = (
            f"Use the {tool_name} tool with value '{marker}', then respond with TELEMETRY_E2E_DONE."
        )

        def echo(invocation: ToolInvocation) -> ToolResult:
            args = invocation.arguments or {}
            return ToolResult(text_result_for_llm=str(args.get("value", "")))

        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
            working_directory=ctx.work_dir,
            env=ctx.get_env(),
            github_token=github_token,
            telemetry=TelemetryConfig(
                file_path=str(telemetry_path),
                exporter_type="file",
                source_name=source_name,
                capture_content=True,
            ),
        )

        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                tools=[
                    Tool(
                        name=tool_name,
                        description="Echoes a marker string for telemetry validation.",
                        parameters={
                            "type": "object",
                            "properties": {"value": {"type": "string", "description": "Marker"}},
                            "required": ["value"],
                        },
                        handler=echo,
                    )
                ],
            )
            session_id = session.session_id

            answer = await session.send_and_wait(prompt, timeout=60.0)
            assert answer is not None
            assert "TELEMETRY_E2E_DONE" in (answer.data.content or "")

            await session.disconnect()
        finally:
            await client.stop()

        entries = _read_telemetry_entries(telemetry_path)
        spans = [item for item in entries if item.get("type") == "span"]
        assert spans

        for span in spans:
            scope = span.get("instrumentationScope") or {}
            assert scope.get("name") == source_name

        for span in spans:
            status = (span.get("status") or {}).get("code", 0)
            assert status != 2, f"span in error state: {span}"

        invoke_agent = next(
            s for s in spans if _string_attribute(s, "gen_ai.operation.name") == "invoke_agent"
        )
        assert _string_attribute(invoke_agent, "gen_ai.conversation.id") == session_id
        assert _is_root_span(invoke_agent)
        invoke_agent_span_id = invoke_agent.get("spanId")
        assert invoke_agent_span_id
        invoke_agent_trace_id = invoke_agent.get("traceId")
        assert invoke_agent_trace_id

        chat_spans = [s for s in spans if _string_attribute(s, "gen_ai.operation.name") == "chat"]
        assert chat_spans
        for chat in chat_spans:
            assert chat.get("parentSpanId") == invoke_agent_span_id
            assert chat.get("traceId") == invoke_agent_trace_id
        assert any(
            prompt in (_string_attribute(c, "gen_ai.input.messages") or "") for c in chat_spans
        )
        assert any(
            "TELEMETRY_E2E_DONE" in (_string_attribute(c, "gen_ai.output.messages") or "")
            for c in chat_spans
        )

        tool_span = next(
            s for s in spans if _string_attribute(s, "gen_ai.operation.name") == "execute_tool"
        )
        assert tool_span.get("parentSpanId") == invoke_agent_span_id
        assert tool_span.get("traceId") == invoke_agent_trace_id
        assert _string_attribute(tool_span, "gen_ai.tool.name") == tool_name
        assert (_string_attribute(tool_span, "gen_ai.tool.call.id") or "").strip()
        assert (
            _string_attribute(tool_span, "gen_ai.tool.call.arguments") == f'{{"value":"{marker}"}}'
        )
        assert _string_attribute(tool_span, "gen_ai.tool.call.result") == marker

    async def test_should_export_per_request_subagent_chat_spans(self, ctx: E2ETestContext):
        telemetry_path = Path(ctx.work_dir) / f"telemetry-{uuid.uuid4().hex}.jsonl"
        source_name = "python-sdk-subagent-telemetry-e2e"
        prompt = (
            "Use the task tool in sync mode to ask a task agent to read subagent-otel.txt "
            "with the view tool. Then reply with SUBAGENT_OTEL_DONE."
        )
        (Path(ctx.work_dir) / "subagent-otel.txt").write_text(
            "SUBAGENT_OTEL_FILE_CONTENT", encoding="utf-8"
        )
        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
            working_directory=ctx.work_dir,
            env=ctx.get_env(),
            github_token="fake-token-for-e2e-tests"
            if os.environ.get("GITHUB_ACTIONS") == "true"
            else None,
            telemetry=TelemetryConfig(
                file_path=str(telemetry_path),
                exporter_type="file",
                source_name=source_name,
                capture_content=True,
            ),
        )
        try:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all
            )
            session_id = session.session_id
            try:
                answer = await session.send_and_wait(prompt, timeout=60.0)
                assert answer is not None
                assert "SUBAGENT_OTEL_DONE" in (answer.data.content or "")
            finally:
                await session.disconnect()
        finally:
            await client.stop()

        spans = [
            entry
            for entry in _read_telemetry_entries(telemetry_path)
            if entry.get("type") == "span"
        ]
        assert all(
            (span.get("instrumentationScope") or {}).get("name") == source_name for span in spans
        )
        assert all((span.get("status") or {}).get("code", 0) != 2 for span in spans)

        invocations = [
            s for s in spans if _string_attribute(s, "gen_ai.operation.name") == "invoke_agent"
        ]
        assert len(invocations) == 2, invocations
        roots = [s for s in invocations if _is_root_span(s)]
        assert len(roots) == 1, roots
        root = roots[0]
        assert _string_attribute(root, "gen_ai.conversation.id") == session_id
        assert root.get("spanId") and root.get("traceId")
        tasks = [
            s
            for s in spans
            if _string_attribute(s, "gen_ai.operation.name") == "execute_tool"
            and _string_attribute(s, "gen_ai.tool.name") == "task"
        ]
        assert len(tasks) == 1, tasks
        task = tasks[0]
        assert task.get("parentSpanId") == root["spanId"]
        children = [s for s in invocations if s.get("parentSpanId") == task.get("spanId")]
        assert len(children) == 1, children
        child = children[0]
        assert task.get("traceId") == child.get("traceId") == root["traceId"]

        chats = [s for s in spans if _string_attribute(s, "gen_ai.operation.name") == "chat"]
        assert len(chats) == 4, chats
        parent_chats = [s for s in chats if s.get("parentSpanId") == root["spanId"]]
        assert len(parent_chats) == 2
        assert all(s.get("traceId") == root["traceId"] for s in parent_chats)
        child_chats = [s for s in chats if s.get("parentSpanId") == child["spanId"]]
        assert len(child_chats) == 2, child_chats
        assert all(s.get("traceId") == root["traceId"] for s in child_chats)
        assert all(
            _string_attribute(s, "github.copilot.initiator") == "sub-agent" for s in child_chats
        )
        requesting_chats = [
            s
            for s in child_chats
            if '"view"' in (_string_attribute(s, "gen_ai.output.messages") or "")
        ]
        assert len(requesting_chats) == 1
        assert "SUBAGENT_OTEL_FILE_CONTENT" not in (
            _string_attribute(requesting_chats[0], "gen_ai.input.messages") or ""
        )
        finals = [
            s
            for s in child_chats
            if "SUBAGENT_OTEL_CHILD_DONE" in (_string_attribute(s, "gen_ai.output.messages") or "")
        ]
        assert len(finals) == 1
        assert "SUBAGENT_OTEL_FILE_CONTENT" in (
            _string_attribute(finals[0], "gen_ai.input.messages") or ""
        )


# ---------------------------------------------------------------------------
# Unit-style tests mirroring dotnet/test/TelemetryTests.cs
# ---------------------------------------------------------------------------


class TestTelemetryConfig:
    """Mirrors TelemetryConfig_DefaultValues_AreNull / TelemetryConfig_CanSetAllProperties."""

    async def test_default_values_are_unset(self):
        # Python's TelemetryConfig is a TypedDict with total=False, so an empty
        # constructor leaves every field unset (equivalent to C#'s null defaults).
        cfg: TelemetryConfig = TelemetryConfig()
        assert cfg.get("otlp_endpoint") is None
        assert cfg.get("otlp_protocol") is None
        assert cfg.get("file_path") is None
        assert cfg.get("exporter_type") is None
        assert cfg.get("source_name") is None
        assert cfg.get("capture_content") is None

    async def test_can_set_all_properties(self):
        cfg: TelemetryConfig = TelemetryConfig(
            otlp_endpoint="http://localhost:4318",
            otlp_protocol="http/protobuf",
            file_path="/tmp/traces.json",
            exporter_type="otlp-http",
            source_name="my-app",
            capture_content=True,
        )
        assert cfg["otlp_endpoint"] == "http://localhost:4318"
        assert cfg["otlp_protocol"] == "http/protobuf"
        assert cfg["file_path"] == "/tmp/traces.json"
        assert cfg["exporter_type"] == "otlp-http"
        assert cfg["source_name"] == "my-app"
        assert cfg["capture_content"] is True


class TestTelemetryHelpers:
    """Mirrors TelemetryHelpers_Restores_W3C_Trace_Context."""

    async def test_restores_w3c_trace_context(self):
        # The helpers are a no-op if the OpenTelemetry API is not installed;
        # skip the test in that case to keep CI portable.
        opentelemetry = pytest.importorskip("opentelemetry")
        from opentelemetry import propagate, trace
        from opentelemetry.sdk.trace import TracerProvider
        from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator

        # Configure a real tracer provider + W3C propagator so the helpers
        # actually have something to inject/extract.
        previous_provider = trace.get_tracer_provider()
        previous_propagator = propagate.get_global_textmap()
        trace.set_tracer_provider(TracerProvider())
        propagate.set_global_textmap(TraceContextTextMapPropagator())
        try:
            tracer = trace.get_tracer("copilot-sdk-test")
            with tracer.start_as_current_span("parent") as parent:
                ctx = get_trace_context()
                assert ctx.get("traceparent"), "expected non-empty traceparent under active span"
                expected_trace_id = format(parent.get_span_context().trace_id, "032x")
                assert expected_trace_id in ctx["traceparent"]

            # Now outside any active span, restore the captured headers and
            # verify the propagated trace id round-trips.
            captured_traceparent = ctx["traceparent"]
            captured_tracestate = ctx.get("tracestate")
            with trace_context(captured_traceparent, captured_tracestate):
                restored = get_trace_context()
                assert restored.get("traceparent")
                assert expected_trace_id in restored["traceparent"]

            # Invalid traceparents should not raise; they simply produce no
            # propagated context (matching the C# helper's null return).
            with trace_context("not-a-traceparent", None):
                bad = get_trace_context()
                assert "traceparent" not in bad
        finally:
            propagate.set_global_textmap(previous_propagator)
            trace.set_tracer_provider(previous_provider)
        _ = opentelemetry  # keep importorskip reference
