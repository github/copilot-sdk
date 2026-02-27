# OpenTelemetry Instrumentation for Copilot SDK

This guide shows how to add OpenTelemetry tracing to your Copilot SDK applications using GenAI semantic conventions.

## Overview

The Copilot SDK emits session events as your agent processes requests. You can instrument your application to convert these events into OpenTelemetry spans and attributes following the [OpenTelemetry GenAI Semantic Conventions v1.34.0](https://opentelemetry.io/docs/specs/semconv/gen-ai/).

## Installation

```bash
pip install opentelemetry-sdk opentelemetry-api
```

For exporting to observability backends:

```bash
# Console output
pip install opentelemetry-sdk

# Azure Monitor
pip install azure-monitor-opentelemetry

# OTLP (Jaeger, Prometheus, etc.)
pip install opentelemetry-exporter-otlp
```

## Basic Setup

### 1. Initialize OpenTelemetry

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor, ConsoleSpanExporter

# Setup tracer provider
tracer_provider = TracerProvider()
trace.set_tracer_provider(tracer_provider)

# Add exporter (console example)
span_exporter = ConsoleSpanExporter()
tracer_provider.add_span_processor(SimpleSpanProcessor(span_exporter))

# Get a tracer
tracer = trace.get_tracer(__name__)
```

### 2. Create Spans Around Agent Operations

```python
from copilot import CopilotClient, PermissionHandler
from copilot.generated.session_events import SessionEventType
from opentelemetry import trace, context
from opentelemetry.trace import SpanKind

# Initialize client and start the CLI server
client = CopilotClient()
await client.start()

tracer = trace.get_tracer(__name__)

# Create a span for the agent invocation
span_attrs = {
    "gen_ai.operation.name": "invoke_agent",
    "gen_ai.provider.name": "github.copilot",
    "gen_ai.agent.name": "my-agent",
    "gen_ai.request.model": "gpt-5",
}

span = tracer.start_span(
    name="invoke_agent my-agent",
    kind=SpanKind.CLIENT,
    attributes=span_attrs
)
token = context.attach(trace.set_span_in_context(span))

try:
    # Create a session (model is set here, not on the client)
    session = await client.create_session({
        "model": "gpt-5",
        "on_permission_request": PermissionHandler.approve_all,
    })

    # Subscribe to events via callback
    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_USAGE:
            if event.data.model:
                span.set_attribute("gen_ai.response.model", event.data.model)

    unsubscribe = session.on(handle_event)

    # Send a message (returns a message ID)
    await session.send({"prompt": "Hello, world!"})

    # Or send and wait for the session to become idle
    response = await session.send_and_wait({"prompt": "Hello, world!"})
finally:
    context.detach(token)
    span.end()
    await client.stop()
```

## Copilot SDK Event to GenAI Attribute Mapping

The Copilot SDK emits `SessionEventType` events during agent execution. Subscribe to these events using `session.on(handler)`, which returns an unsubscribe function. Here's how to map these events to GenAI semantic convention attributes:

### Core Session Events

| SessionEventType | GenAI Attributes | Description |
|------------------|------------------|-------------|
| `SESSION_START` | - | Session initialization (mark span start) |
| `SESSION_IDLE` | - | Session completed (mark span end) |
| `SESSION_ERROR` | `error.type`, `error.message` | Error occurred |

### Assistant Events

| SessionEventType | GenAI Attributes | Description |
|------------------|------------------|-------------|
| `ASSISTANT_TURN_START` | - | Assistant begins processing |
| `ASSISTANT_TURN_END` | - | Assistant finished processing |
| `ASSISTANT_MESSAGE` | `gen_ai.output.messages` (event) | Final assistant message with complete content |
| `ASSISTANT_MESSAGE_DELTA` | - | Streaming message chunk (optional to trace) |
| `ASSISTANT_USAGE` | `gen_ai.usage.input_tokens`<br>`gen_ai.usage.output_tokens`<br>`gen_ai.response.model` | Token usage and model information |
| `ASSISTANT_REASONING` | - | Reasoning content (optional to trace) |
| `ASSISTANT_INTENT` | - | Assistant's understood intent |

### Tool Execution Events

| SessionEventType | GenAI Attributes / Span | Description |
|------------------|-------------------------|-------------|
| `TOOL_EXECUTION_START` | Create child span:<br>- `gen_ai.tool.name`<br>- `gen_ai.tool.call.id`<br>- `gen_ai.operation.name`: `execute_tool`<br>- `gen_ai.tool.call.arguments` (opt-in) | Tool execution begins |
| `TOOL_EXECUTION_COMPLETE` | On child span:<br>- `gen_ai.tool.call.result` (opt-in)<br>- `error.type` (if failed)<br>End child span | Tool execution finished |
| `TOOL_EXECUTION_PARTIAL_RESULT` | - | Streaming tool result |

### Model and Context Events

| SessionEventType | GenAI Attributes | Description |
|------------------|------------------|-------------|
| `SESSION_MODEL_CHANGE` | `gen_ai.request.model` | Model changed during session |
| `SESSION_CONTEXT_CHANGED` | - | Context window modified |
| `SESSION_TRUNCATION` | - | Context truncated |

## Detailed Event Mapping Examples

### ASSISTANT_USAGE Event

When you receive an `ASSISTANT_USAGE` event, extract token usage:

```python
from copilot.generated.session_events import SessionEventType

def handle_usage(event):
    if event.type == SessionEventType.ASSISTANT_USAGE:
        data = event.data
        if data.model:
            span.set_attribute("gen_ai.response.model", data.model)
        if data.input_tokens is not None:
            span.set_attribute("gen_ai.usage.input_tokens", int(data.input_tokens))
        if data.output_tokens is not None:
            span.set_attribute("gen_ai.usage.output_tokens", int(data.output_tokens))

unsubscribe = session.on(handle_usage)
await session.send({"prompt": "Hello"})
```

**Event Data Structure:**
<!-- docs-validate: skip -->
```python
@dataclass
class Usage:
    input_tokens: float
    output_tokens: float
    cache_read_tokens: float
    cache_write_tokens: float
```

**Maps to GenAI Attributes:**
- `input_tokens` → `gen_ai.usage.input_tokens`
- `output_tokens` → `gen_ai.usage.output_tokens`
- Response model → `gen_ai.response.model`

### TOOL_EXECUTION_START / COMPLETE Events

Create child spans for each tool execution:

```python
from opentelemetry.trace import SpanKind
import json

# Dictionary to track active tool spans
tool_spans = {}

def handle_tool_events(event):
    data = event.data

    if event.type == SessionEventType.TOOL_EXECUTION_START and data:
        call_id = data.tool_call_id or str(uuid.uuid4())
        tool_name = data.tool_name or "unknown"

        tool_attrs = {
            "gen_ai.tool.name": tool_name,
            "gen_ai.operation.name": "execute_tool",
        }

        if call_id:
            tool_attrs["gen_ai.tool.call.id"] = call_id

        # Optional: include tool arguments (may contain sensitive data)
        if data.arguments is not None:
            try:
                tool_attrs["gen_ai.tool.call.arguments"] = json.dumps(data.arguments)
            except Exception:
                tool_attrs["gen_ai.tool.call.arguments"] = str(data.arguments)

        tool_span = tracer.start_span(
            name=f"execute_tool {tool_name}",
            kind=SpanKind.CLIENT,
            attributes=tool_attrs
        )
        tool_token = context.attach(trace.set_span_in_context(tool_span))
        tool_spans[call_id] = (tool_span, tool_token)

    elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE and data:
        call_id = data.tool_call_id
        entry = tool_spans.pop(call_id, None) if call_id else None

        if entry:
            tool_span, tool_token = entry

            # Optional: include tool result (may contain sensitive data)
            if data.result is not None:
                try:
                    result_str = json.dumps(data.result)
                except Exception:
                    result_str = str(data.result)
                # Truncate to 512 chars to avoid huge spans
                tool_span.set_attribute("gen_ai.tool.call.result", result_str[:512])

            # Mark as error if tool failed
            if hasattr(data, "success") and data.success is False:
                tool_span.set_attribute("error.type", "tool_error")

            context.detach(tool_token)
            tool_span.end()

unsubscribe = session.on(handle_tool_events)
await session.send({"prompt": "What's the weather?"})
```

**Tool Event Data:**
- `tool_call_id` → `gen_ai.tool.call.id`
- `tool_name` → `gen_ai.tool.name`
- `arguments` → `gen_ai.tool.call.arguments` (opt-in)
- `result` → `gen_ai.tool.call.result` (opt-in)

### ASSISTANT_MESSAGE Event

Capture the final message as a span event:

```python
def handle_message(event):
    if event.type == SessionEventType.ASSISTANT_MESSAGE and event.data:
        if event.data.content:
            # Add as a span event (opt-in for content recording)
            span.add_event(
                "gen_ai.output.messages",
                attributes={
                    "gen_ai.event.content": json.dumps({
                        "role": "assistant",
                        "content": event.data.content
                    })
                }
            )

unsubscribe = session.on(handle_message)
await session.send({"prompt": "Tell me a joke"})
```

## Complete Example

```python
import asyncio
import json
import uuid
from copilot import CopilotClient, PermissionHandler
from copilot.generated.session_events import SessionEventType
from opentelemetry import trace, context
from opentelemetry.trace import SpanKind
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor, ConsoleSpanExporter

# Setup OpenTelemetry
tracer_provider = TracerProvider()
trace.set_tracer_provider(tracer_provider)
tracer_provider.add_span_processor(SimpleSpanProcessor(ConsoleSpanExporter()))
tracer = trace.get_tracer(__name__)

async def invoke_agent(prompt: str):
    """Invoke agent with full OpenTelemetry instrumentation."""

    # Create main span
    span_attrs = {
        "gen_ai.operation.name": "invoke_agent",
        "gen_ai.provider.name": "github.copilot",
        "gen_ai.agent.name": "example-agent",
        "gen_ai.request.model": "gpt-5",
    }

    span = tracer.start_span(
        name="invoke_agent example-agent",
        kind=SpanKind.CLIENT,
        attributes=span_attrs
    )
    token = context.attach(trace.set_span_in_context(span))
    tool_spans = {}

    try:
        client = CopilotClient()
        await client.start()

        session = await client.create_session({
            "model": "gpt-5",
            "on_permission_request": PermissionHandler.approve_all,
        })

        # Subscribe to events via callback
        def handle_event(event):
            data = event.data

            # Handle usage events
            if event.type == SessionEventType.ASSISTANT_USAGE and data:
                if data.model:
                    span.set_attribute("gen_ai.response.model", data.model)
                if data.input_tokens is not None:
                    span.set_attribute("gen_ai.usage.input_tokens", int(data.input_tokens))
                if data.output_tokens is not None:
                    span.set_attribute("gen_ai.usage.output_tokens", int(data.output_tokens))

            # Handle tool execution
            elif event.type == SessionEventType.TOOL_EXECUTION_START and data:
                call_id = data.tool_call_id or str(uuid.uuid4())
                tool_name = data.tool_name or "unknown"

                tool_attrs = {
                    "gen_ai.tool.name": tool_name,
                    "gen_ai.operation.name": "execute_tool",
                    "gen_ai.tool.call.id": call_id,
                }

                tool_span = tracer.start_span(
                    name=f"execute_tool {tool_name}",
                    kind=SpanKind.CLIENT,
                    attributes=tool_attrs
                )
                tool_token = context.attach(trace.set_span_in_context(tool_span))
                tool_spans[call_id] = (tool_span, tool_token)

            elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE and data:
                call_id = data.tool_call_id
                entry = tool_spans.pop(call_id, None) if call_id else None
                if entry:
                    tool_span, tool_token = entry
                    context.detach(tool_token)
                    tool_span.end()

            # Capture final message
            elif event.type == SessionEventType.ASSISTANT_MESSAGE and data:
                if data.content:
                    print(f"Assistant: {data.content}")

        unsubscribe = session.on(handle_event)

        # Send message and wait for completion
        response = await session.send_and_wait({"prompt": prompt})

        span.set_attribute("gen_ai.response.finish_reasons", ["stop"])
        unsubscribe()

    except Exception as e:
        span.set_attribute("error.type", type(e).__name__)
        raise
    finally:
        # Clean up any unclosed tool spans
        for call_id, (tool_span, tool_token) in tool_spans.items():
            tool_span.set_attribute("error.type", "stream_aborted")
            context.detach(tool_token)
            tool_span.end()

        context.detach(token)
        span.end()
        await client.stop()

# Run
asyncio.run(invoke_agent("What's 2+2?"))
```

## Required Span Attributes

According to OpenTelemetry GenAI semantic conventions, these attributes are **required** for agent invocation spans:

| Attribute | Description | Example |
|-----------|-------------|---------|
| `gen_ai.operation.name` | Operation type | `invoke_agent`, `chat`, `execute_tool` |
| `gen_ai.provider.name` | Provider identifier | `github.copilot` |
| `gen_ai.request.model` | Model used for request | `gpt-5`, `gpt-4.1` |

## Recommended Span Attributes

These attributes are **recommended** for better observability:

| Attribute | Description |
|-----------|-------------|
| `gen_ai.agent.id` | Unique agent identifier |
| `gen_ai.agent.name` | Human-readable agent name |
| `gen_ai.response.model` | Actual model used in response |
| `gen_ai.usage.input_tokens` | Input tokens consumed |
| `gen_ai.usage.output_tokens` | Output tokens generated |
| `gen_ai.response.finish_reasons` | Completion reasons (e.g., `["stop"]`) |

## Content Recording

Recording message content and tool arguments/results is **optional** and should be opt-in since it may contain sensitive data.

### Environment Variable Control

```bash
# Enable content recording
export OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT=true
```

### Checking at Runtime

<!-- docs-validate: skip -->
```python
import os

def should_record_content():
    return os.getenv("OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT", "false").lower() == "true"

# Only add content if enabled
if should_record_content() and event.data.content:
    span.add_event("gen_ai.output.messages", ...)
```

## MCP (Model Context Protocol) Tool Conventions

For MCP-based tools, add these additional attributes following the [OpenTelemetry MCP semantic conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/mcp/):

<!-- docs-validate: skip -->
```python
tool_attrs = {
    # Required
    "mcp.method.name": "tools/call",
    
    # Recommended
    "mcp.server.name": data.mcp_server_name,
    "mcp.session.id": session.session_id,
    
    # GenAI attributes
    "gen_ai.tool.name": data.mcp_tool_name,
    "gen_ai.operation.name": "execute_tool",
    "network.transport": "pipe",  # Copilot SDK uses stdio
}
```

## Span Naming Conventions

Follow these patterns for span names:

| Operation | Span Name Pattern | Example |
|-----------|-------------------|---------|
| Agent invocation | `invoke_agent {agent_name}` | `invoke_agent weather-bot` |
| Chat | `chat` | `chat` |
| Tool execution | `execute_tool {tool_name}` | `execute_tool fetch_weather` |
| MCP tool | `tools/call {tool_name}` | `tools/call read_file` |

## Metrics

You can also export metrics for token usage and operation duration:

```python
from opentelemetry import metrics
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.sdk.metrics.export import ConsoleMetricExporter, PeriodicExportingMetricReader

# Setup metrics
reader = PeriodicExportingMetricReader(ConsoleMetricExporter())
provider = MeterProvider(metric_readers=[reader])
metrics.set_meter_provider(provider)

meter = metrics.get_meter(__name__)

# Create metrics
operation_duration = meter.create_histogram(
    name="gen_ai.client.operation.duration",
    description="Duration of GenAI operations",
    unit="ms"
)

token_usage = meter.create_counter(
    name="gen_ai.client.token.usage",
    description="Token usage count"
)

# Record metrics
operation_duration.record(123.45, attributes={
    "gen_ai.operation.name": "invoke_agent",
    "gen_ai.request.model": "gpt-5",
})

token_usage.add(150, attributes={
    "gen_ai.token.type": "input",
    "gen_ai.operation.name": "invoke_agent",
})
```

## Azure Monitor Integration

For production observability with Azure Monitor:

```python
from azure.monitor.opentelemetry import configure_azure_monitor

# Enable Azure Monitor
connection_string = "InstrumentationKey=..."
configure_azure_monitor(connection_string=connection_string)

# Your instrumented code here
```

View traces in the Azure Portal under your Application Insights resource → Tracing.

## Best Practices

1. **Always close spans**: Use try/finally blocks to ensure spans are ended even on errors
2. **Set error attributes**: On exceptions, set `error.type` and optionally `error.message`
3. **Use child spans for tools**: Create separate spans for each tool execution
4. **Opt-in for content**: Only record message content and tool arguments when explicitly enabled
5. **Truncate large values**: Limit tool results and arguments to reasonable sizes (e.g., 512 chars)
6. **Set finish reasons**: Always set `gen_ai.response.finish_reasons` when the operation completes successfully
7. **Include model info**: Capture both request and response model names

## Troubleshooting

### No spans appearing

1. Verify tracer provider is set: `trace.set_tracer_provider(provider)`
2. Add a span processor: `provider.add_span_processor(SimpleSpanProcessor(exporter))`
3. Ensure spans are ended: Check for missing `span.end()` calls

### Tool spans not showing as children

Make sure to attach the tool span to the parent context:
<!-- docs-validate: skip -->
```python
tool_token = context.attach(trace.set_span_in_context(tool_span))
```

### Context warnings in async code

You may see "Failed to detach context" warnings in async streaming code. These are expected and don't affect tracing correctness.

## References

- [OpenTelemetry GenAI Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/)
- [OpenTelemetry MCP Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/mcp/)
- [OpenTelemetry Python SDK](https://opentelemetry.io/docs/instrumentation/python/)
- [GenAI Semantic Conventions v1.34.0](https://opentelemetry.io/schemas/1.34.0)
- [Copilot SDK Documentation](https://github.com/github/copilot-sdk)
