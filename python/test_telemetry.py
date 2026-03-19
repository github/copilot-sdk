"""Tests for OpenTelemetry telemetry helpers."""

from __future__ import annotations

from unittest.mock import patch

from copilot._telemetry import get_trace_context, trace_context
from copilot.types import SubprocessConfig, TelemetryConfig


class TestGetTraceContext:
    def test_returns_empty_dict_when_otel_not_installed(self):
        """get_trace_context() returns {} when opentelemetry is not importable."""
        real_import = __import__

        def _block_otel(name: str, *args, **kwargs):
            if name.startswith("opentelemetry"):
                raise ImportError("mocked")
            return real_import(name, *args, **kwargs)

        with patch("builtins.__import__", side_effect=_block_otel):
            result = get_trace_context()

        assert result == {}

    def test_returns_dict_type(self):
        """get_trace_context() always returns a dict."""
        result = get_trace_context()
        assert isinstance(result, dict)


class TestTraceContext:
    def test_yields_without_error_when_no_traceparent(self):
        """trace_context() with no traceparent should yield without error."""
        with trace_context(None, None):
            pass  # should not raise

    def test_yields_without_error_when_otel_not_installed(self):
        """trace_context() should gracefully yield even if opentelemetry is missing."""
        real_import = __import__

        def _block_otel(name: str, *args, **kwargs):
            if name.startswith("opentelemetry"):
                raise ImportError("mocked")
            return real_import(name, *args, **kwargs)

        with patch("builtins.__import__", side_effect=_block_otel):
            with trace_context("00-abc-def-01", None):
                pass  # should not raise

    def test_yields_without_error_with_traceparent(self):
        """trace_context() with a traceparent value should yield without error."""
        tp = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
        with trace_context(tp, None):
            pass  # should not raise

    def test_yields_without_error_with_tracestate(self):
        """trace_context() with both traceparent and tracestate should yield without error."""
        tp = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
        with trace_context(tp, "congo=t61rcWkgMzE"):
            pass  # should not raise


class TestTelemetryConfig:
    def test_telemetry_config_type(self):
        """TelemetryConfig can be constructed as a TypedDict."""
        config: TelemetryConfig = {
            "otlp_endpoint": "http://localhost:4318",
            "exporter_type": "otlp-http",
            "source_name": "my-app",
            "capture_content": True,
        }
        assert config["otlp_endpoint"] == "http://localhost:4318"
        assert config["capture_content"] is True

    def test_telemetry_config_in_subprocess_config(self):
        """TelemetryConfig can be used in SubprocessConfig."""
        config = SubprocessConfig(
            telemetry={
                "otlp_endpoint": "http://localhost:4318",
                "exporter_type": "otlp-http",
            }
        )
        assert config.telemetry is not None
        assert config.telemetry["otlp_endpoint"] == "http://localhost:4318"

    def test_telemetry_env_var_mapping(self):
        """TelemetryConfig fields map to expected environment variable names."""
        config: TelemetryConfig = {
            "otlp_endpoint": "http://localhost:4318",
            "file_path": "/tmp/traces.jsonl",
            "exporter_type": "file",
            "source_name": "test-app",
            "capture_content": True,
        }

        env: dict[str, str] = {}
        env["COPILOT_OTEL_ENABLED"] = "true"
        if "otlp_endpoint" in config:
            env["OTEL_EXPORTER_OTLP_ENDPOINT"] = config["otlp_endpoint"]
        if "file_path" in config:
            env["COPILOT_OTEL_FILE_EXPORTER_PATH"] = config["file_path"]
        if "exporter_type" in config:
            env["COPILOT_OTEL_EXPORTER_TYPE"] = config["exporter_type"]
        if "source_name" in config:
            env["COPILOT_OTEL_SOURCE_NAME"] = config["source_name"]
        if "capture_content" in config:
            env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = str(
                config["capture_content"]
            ).lower()

        assert env["COPILOT_OTEL_ENABLED"] == "true"
        assert env["OTEL_EXPORTER_OTLP_ENDPOINT"] == "http://localhost:4318"
        assert env["COPILOT_OTEL_FILE_EXPORTER_PATH"] == "/tmp/traces.jsonl"
        assert env["COPILOT_OTEL_EXPORTER_TYPE"] == "file"
        assert env["COPILOT_OTEL_SOURCE_NAME"] == "test-app"
        assert env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] == "true"

    def test_capture_content_false_maps_to_lowercase(self):
        """capture_content=False should map to 'false' string."""
        config: TelemetryConfig = {"capture_content": False}
        value = str(config["capture_content"]).lower()
        assert value == "false"

    def test_empty_telemetry_config(self):
        """An empty TelemetryConfig is valid since total=False."""
        config: TelemetryConfig = {}
        assert len(config) == 0
