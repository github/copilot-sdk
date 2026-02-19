# SDK E2E Scenario Tests

End-to-end scenario tests for the Copilot SDK. Each scenario demonstrates a specific SDK capability with implementations in TypeScript, Python, and Go.

## Structure

```
scenarios/
├── auth/           # Authentication flows (OAuth, BYOK, token sources)
├── bundling/       # Deployment architectures (stdio, TCP, containers)
├── callbacks/      # Lifecycle hooks, permissions, user input
├── modes/          # Preset modes (CLI, filesystem, minimal)
├── prompts/        # Prompt configuration (attachments, system messages, reasoning)
├── sessions/       # Session management (streaming, resume, concurrent, infinite)
├── tools/          # Tool capabilities (custom agents, MCP, skills, filtering)
├── transport/      # Wire protocols (stdio, TCP, WASM, reconnect)
└── verify.sh       # Run all scenarios
```

## Running

Run all scenarios:

```bash
COPILOT_CLI_PATH=/path/to/copilot GITHUB_TOKEN=$(gh auth token) bash verify.sh
```

Run a single scenario:

```bash
COPILOT_CLI_PATH=/path/to/copilot GITHUB_TOKEN=$(gh auth token) bash <category>/<scenario>/verify.sh
```

## Prerequisites

- **Copilot CLI** — set `COPILOT_CLI_PATH`
- **GitHub token** — set `GITHUB_TOKEN` or use `gh auth login`
- **Node.js 20+**, **Python 3.10+**, **Go 1.24+** (per language)
