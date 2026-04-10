# GitHub Copilot SDK Documentation

Welcome to the GitHub Copilot SDK docs. Whether you're building your first Copilot-powered app or deploying to production, you'll find what you need here.

## Where to Start

| I want to... | Go to |
|---|---|
| **Build my first app** | [Getting Started](./getting-started.md) — end-to-end tutorial with streaming & custom tools |
| **Set up for production** | [Setup Guides](./setup/index.md) — architecture, deployment patterns, scaling |
| **Configure authentication** | [Authentication](./auth/index.md) — GitHub OAuth, environment variables, BYOK |
| **Add features to my app** | [Features](./features/index.md) — hooks, custom agents, MCP, skills, and more |
| **Debug an issue** | [Troubleshooting](./troubleshooting/debugging.md) — common problems and solutions |

## Documentation Map

### [Getting Started](./getting-started.md)

Step-by-step tutorial that takes you from zero to a working Copilot app with streaming responses and custom tools.

### [Setup](./setup/index.md)

How to configure and deploy the SDK for your use case.

- [Default Setup (Bundled CLI)](./setup/bundled-cli.md) — the SDK includes the CLI automatically
- [Local CLI](./setup/local-cli.md) — use your own CLI binary or running instance
- [Backend Services](./setup/backend-services.md) — server-side with headless CLI over TCP
- [GitHub OAuth](./setup/github-oauth.md) — implement the OAuth flow
- [Azure Managed Identity](./setup/azure-managed-identity.md) — BYOK with Azure AI Foundry
- [Scaling & Multi-Tenancy](./setup/scaling.md) — horizontal scaling, isolation patterns

### [Authentication](./auth/index.md)

Configuring how users and services authenticate with Copilot.

- [Authentication Overview](./auth/index.md) — methods, priority order, and examples
- [Bring Your Own Key (BYOK)](./auth/byok.md) — use your own API keys from OpenAI, Azure, Anthropic, and more

### [Features](./features/index.md)

Guides for building with the SDK's capabilities.

- [Hooks](./features/hooks.md) — intercept and customize session behavior
- [Custom Agents](./features/custom-agents.md) — define specialized sub-agents
- [MCP Servers](./features/mcp.md) — integrate Model Context Protocol servers
- [Skills](./features/skills.md) — load reusable prompt modules
- [Image Input](./features/image-input.md) — send images as attachments
- [Streaming Events](./features/streaming-events.md) — real-time event reference
- [Steering & Queueing](./features/steering-and-queueing.md) — message delivery modes
- [Session Persistence](./features/session-persistence.md) — resume sessions across restarts

### [Hooks Reference](./hooks/index.md)

Detailed API reference for each session hook.

- [Pre-Tool Use](./hooks/pre-tool-use.md) — approve, deny, or modify tool calls
- [Post-Tool Use](./hooks/post-tool-use.md) — transform tool results
- [User Prompt Submitted](./hooks/user-prompt-submitted.md) — modify or filter user messages
- [Session Lifecycle](./hooks/session-lifecycle.md) — session start and end
- [Error Handling](./hooks/error-handling.md) — custom error handling

### [Troubleshooting](./troubleshooting/debugging.md)

- [Debugging Guide](./troubleshooting/debugging.md) — common issues and solutions
- [MCP Debugging](./troubleshooting/mcp-debugging.md) — MCP-specific troubleshooting
- [Compatibility](./troubleshooting/compatibility.md) — SDK vs CLI feature matrix

### [Observability](./observability/opentelemetry.md)

- [OpenTelemetry Instrumentation](./observability/opentelemetry.md) — built-in TelemetryConfig and trace context propagation

### [Integrations](./integrations/microsoft-agent-framework.md)

Guides for using the SDK with other platforms and frameworks.

- [Microsoft Agent Framework](./integrations/microsoft-agent-framework.md) — MAF multi-agent workflows
