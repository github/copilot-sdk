# Features

These guides cover the capabilities you can add to your Copilot SDK application. Each guide includes examples in all supported languages (TypeScript, Python, Go, and .NET).

> **New to the SDK?** Start with the [Getting Started tutorial](../getting-started.md) first, then come back here to add more capabilities.

## Guides

| Feature | Description |
|---|---|
| [Hooks](./hooks.md) | Intercept and customize session behavior — control tool execution, transform results, handle errors |
| [Custom Agents](./custom-agents.md) | Define specialized sub-agents with scoped tools and instructions |
| [MCP Servers](./mcp.md) | Integrate Model Context Protocol servers for external tool access |
| [Skills](./skills.md) | Load reusable prompt modules from directories |
| [Image Input](./image-input.md) | Send images to sessions as attachments |
| [Streaming Events](./streaming-events.md) | Subscribe to real-time session events (40+ event types) |
| [Steering & Queueing](./steering-and-queueing.md) | Control message delivery — immediate steering vs. sequential queueing |
| [Session Persistence](./session-persistence.md) | Resume sessions across restarts, manage session storage |

## Related

- [Hooks Reference](../hooks/index.md) — detailed API reference for each hook type
- [Integrations](../integrations/microsoft-agent-framework.md) — use the SDK with other platforms (MAF, etc.)
- [Troubleshooting](../troubleshooting/debugging.md) — when things don't work as expected
- [Compatibility](../troubleshooting/compatibility.md) — SDK vs CLI feature matrix
