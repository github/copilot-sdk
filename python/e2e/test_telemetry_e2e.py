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

import asyncio
import json
import os
import uuid
from pathlib import Path
from typing import Any

import pytest

from copilot import CopilotClient
from copilot._telemetry import get_trace_context, trace_context
from copilot.client import SubprocessConfig, TelemetryConfig
from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext, get_final_assistant_message

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


async def _read_telemetry_entries(
    path: Path, complete: Any, *, timeout: float = 30.0
) -> list[dict[str, Any]]:
    deadline = asyncio.get_event_loop().time() + timeout
    while asyncio.get_event_loop().time() < deadline:
        if path.exists() and path.stat().st_size > 0:
            entries: list[dict[str, Any]] = []
            for line in path.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if not line:
                    continue
                entries.append(json.loads(line))
            if entries and complete(entries):
                return entries
        await asyncio.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for telemetry records in '{path}'.")


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
            SubprocessConfig(
                cli_path=ctx.cli_path,
                cwd=ctx.work_dir,
                env=ctx.get_env(),
                github_token=github_token,
                telemetry=TelemetryConfig(
                    file_path=str(telemetry_path),
                    exporter_type="file",
                    source_name=source_name,
                    capture_content=True,
                ),
            )
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

            await session.send(prompt)
            answer = await get_final_assistant_message(session, timeout=60.0)
            assert "TELEMETRY_E2E_DONE" in (answer.data.content or "")

            await session.disconnect()
        finally:
            await client.stop()

        entries = await _read_telemetry_entries(
            telemetry_path,
            lambda items: any(
                item.get("type") == "span"
                and _string_attribute(item, "gen_ai.operation.name") == "invoke_agent"
                for item in items
            ),
        )
        spans = [item for item in entries if item.get("type") == "span"]
        assert spans

        for span in spans:
            scope = span.get("instrumentationScope") or {}
            assert scope.get("name") == source_name

        trace_ids = {s.get("traceId") for s in spans if s.get("traceId")}
        assert len(trace_ids) == 1

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

        chat_spans = [s for s in spans if _string_attribute(s, "gen_ai.operation.name") == "chat"]
        assert chat_spans
        for chat in chat_spans:
            assert chat.get("parentSpanId") == invoke_agent_span_id
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
        assert _string_attribute(tool_span, "gen_ai.tool.name") == tool_name
        assert (_string_attribute(tool_span, "gen_ai.tool.call.id") or "").strip()
        assert (
            _string_attribute(tool_span, "gen_ai.tool.call.arguments") == f'{{"value":"{marker}"}}'
        )
        assert _string_attribute(tool_span, "gen_ai.tool.call.result") == marker


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
        assert cfg.get("file_path") is None
        assert cfg.get("exporter_type") is None
        assert cfg.get("source_name") is None
        assert cfg.get("capture_content") is None

    async def test_can_set_all_properties(self):
        cfg: TelemetryConfig = TelemetryConfig(
            otlp_endpoint="http://localhost:4318",
            file_path="/tmp/traces.json",
            exporter_type="otlp-http",
            source_name="my-app",
            capture_content=True,
        )
        assert cfg["otlp_endpoint"] == "http://localhost:4318"
        assert cfg["file_path"] == "/tmp/traces.json"
        assert cfg["exporter_type"] == "otlp-http"
        assert cfg["source_name"] == "my-app"
        assert cfg["capture_content"] is True


class TestSubprocessConfigTelemetry:
    """Mirrors CopilotClientOptions_Telemetry_DefaultsToNull."""

    async def test_telemetry_defaults_to_none(self):
        config = SubprocessConfig()
        assert config.telemetry is None

    # NOTE: CopilotClientOptions_Clone_CopiesTelemetry from the C# baseline has
    # no Python equivalent: SubprocessConfig is a plain dataclass with no
    # Clone() method, so there is nothing meaningful to test.


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
