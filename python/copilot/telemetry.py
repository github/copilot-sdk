"""OpenTelemetry trace context helpers for Copilot SDK."""

from __future__ import annotations

from collections.abc import Generator
from contextlib import contextmanager


def get_trace_context() -> dict[str, str]:
    """Get the current W3C Trace Context (traceparent/tracestate) if OpenTelemetry is available."""
    try:
        from opentelemetry import context, propagate
    except ImportError:
        return {}

    carrier: dict[str, str] = {}
    propagate.inject(carrier, context=context.get_current())
    result: dict[str, str] = {}
    if "traceparent" in carrier:
        result["traceparent"] = carrier["traceparent"]
    if "tracestate" in carrier:
        result["tracestate"] = carrier["tracestate"]
    return result


@contextmanager
def trace_context(traceparent: str | None, tracestate: str | None) -> Generator[None, None, None]:
    """Context manager that sets the trace context from W3C headers for the block's duration."""
    try:
        from opentelemetry import context, propagate
    except ImportError:
        yield
        return

    if not traceparent:
        yield
        return

    carrier: dict[str, str] = {"traceparent": traceparent}
    if tracestate:
        carrier["tracestate"] = tracestate

    ctx = propagate.extract(carrier, context=context.get_current())
    token = context.attach(ctx)
    try:
        yield
    finally:
        context.detach(token)
