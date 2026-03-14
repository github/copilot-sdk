# OpenTelemetry Instrumentation for Copilot SDK

This guide shows how to add OpenTelemetry tracing to your Copilot SDK applications.

## Built-in Telemetry Support

The SDK has built-in support for configuring OpenTelemetry on the CLI process and propagating W3C Trace Context between the SDK and CLI. Provide a `TelemetryConfig` when creating the client to opt in:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: skip -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
  telemetry: {
    otlpEndpoint: "http://localhost:4318",
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: skip -->
```python
from copilot import CopilotClient, SubprocessConfig

client = CopilotClient(SubprocessConfig(
    telemetry={
        "otlp_endpoint": "http://localhost:4318",
    },
))
```

</details>

<details>
<summary><strong>Go</strong></summary>

<!-- docs-validate: skip -->
```go
client, err := copilot.NewClient(copilot.ClientOptions{
    Telemetry: &copilot.TelemetryConfig{
        OTLPEndpoint: "http://localhost:4318",
    },
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
var client = new CopilotClient(new CopilotClientOptions
{
    Telemetry = new TelemetryConfig
    {
        OtlpEndpoint = "http://localhost:4318",
    },
});
```

</details>

### TelemetryConfig Options

| Option | Node.js | Python | Go | .NET | Description |
|---|---|---|---|---|---|
| OTLP endpoint | `otlpEndpoint` | `otlp_endpoint` | `OTLPEndpoint` | `OtlpEndpoint` | OTLP HTTP endpoint URL |
| File path | `filePath` | `file_path` | `FilePath` | `FilePath` | File path for JSON-lines trace output |
| Exporter type | `exporterType` | `exporter_type` | `ExporterType` | `ExporterType` | `"otlp-http"` or `"file"` |
| Source name | `sourceName` | `source_name` | `SourceName` | `SourceName` | Instrumentation scope name |
| Capture content | `captureContent` | `capture_content` | `CaptureContent` | `CaptureContent` | Whether to capture message content |

### Trace Context Propagation

> **Most users don't need this.** The `TelemetryConfig` above is all you need to collect traces from the CLI. The trace context propagation described in this section is an **advanced feature** for applications that create their own OpenTelemetry spans and want them to appear in the **same distributed trace** as the CLI's spans.

The SDK can propagate W3C Trace Context (`traceparent`/`tracestate`) on JSON-RPC payloads so that your application's spans and the CLI's spans are linked in one distributed trace. This is useful when, for example, you want to see a "handle tool call" span in your app nested inside the CLI's "execute tool" span, or show the SDK call as a child of your request-handling span.

#### SDK → CLI (outbound)

For **Node.js**, provide an `onGetTraceContext` callback on the client options. This is only needed if your application already uses `@opentelemetry/api` and you want to link your spans with the CLI's spans. The SDK calls this callback before `session.create`, `session.resume`, and `session.send` RPCs:

<!-- docs-validate: skip -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";
import { propagation, context } from "@opentelemetry/api";

const client = new CopilotClient({
  telemetry: { otlpEndpoint: "http://localhost:4318" },
  onGetTraceContext: () => {
    const carrier: Record<string, string> = {};
    propagation.inject(context.active(), carrier);
    return carrier; // { traceparent: "00-...", tracestate: "..." }
  },
});
```

For **Python**, **Go**, and **.NET**, trace context injection is automatic when the respective OpenTelemetry/Activity API is configured — no callback is needed.

#### CLI → SDK (inbound)

When the CLI invokes a tool handler, the `traceparent` and `tracestate` from the CLI's span are available in all languages:

- **Go**: The `ToolInvocation.TraceContext` field is a `context.Context` with the trace already restored — use it directly as the parent for your spans.
- **Python**: Trace context is automatically restored around the handler via `trace_context()` — child spans are parented to the CLI's span automatically.
- **.NET**: Trace context is automatically restored via `RestoreTraceContext()` — child `Activity` instances are parented to the CLI's span automatically.
- **Node.js**: Since the SDK has no OpenTelemetry dependency, `traceparent` and `tracestate` are passed as raw strings on the `ToolInvocation` object. Restore the context manually if needed:

<!-- docs-validate: skip -->
```typescript
import { propagation, context, trace } from "@opentelemetry/api";

session.registerTool(myTool, async (args, invocation) => {
  // Restore the CLI's trace context as the active context
  const carrier = {
    traceparent: invocation.traceparent,
    tracestate: invocation.tracestate,
  };
  const parentCtx = propagation.extract(context.active(), carrier);

  // Create a child span under the CLI's span
  const tracer = trace.getTracer("my-app");
  return context.with(parentCtx, () =>
    tracer.startActiveSpan("my-tool", async (span) => {
      try {
        const result = await doWork(args);
        return result;
      } finally {
        span.end();
      }
    })
  );
});
```

### Per-Language Dependencies

| Language | Dependency | Notes |
|---|---|---|
| Node.js | — | No dependency; provide `onGetTraceContext` callback for outbound propagation |
| Python | `opentelemetry-api` | Install with `pip install copilot-sdk[telemetry]` |
| Go | `go.opentelemetry.io/otel` | Required dependency |
| .NET | — | Uses built-in `System.Diagnostics.Activity` |

## References

- [OpenTelemetry GenAI Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/)
- [OpenTelemetry MCP Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/mcp/)
- [OpenTelemetry Python SDK](https://opentelemetry.io/docs/instrumentation/python/)
- [Copilot SDK Documentation](https://github.com/github/copilot-sdk)
