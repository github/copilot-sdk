# Config Sample: Reasoning Effort

Demonstrates configuring the Copilot SDK with different **reasoning effort** levels. The `reasoningEffort` session config controls how much compute the model spends thinking before responding.

## Reasoning Effort Levels

| Level | Effect |
|-------|--------|
| `low` | Fastest responses, minimal reasoning |
| `medium` | Balanced speed and depth |
| `high` | Deeper reasoning, slower responses |
| `xhigh` | Maximum reasoning effort |

## What This Sample Does

1. Creates a session with `reasoningEffort: "low"` and `availableTools: []`
2. Sends: _"What is the capital of France?"_
3. Prints the response — confirming the model responds correctly at low effort

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `reasoningEffort` | `"low"` | Sets minimal reasoning effort |
| `availableTools` | `[]` (empty array) | Removes all built-in tools |
| `systemMessage.mode` | `"replace"` | Replaces the default system prompt |
| `systemMessage.content` | Custom concise prompt | Instructs the agent to answer concisely |

## Languages

| Directory | SDK / Approach | Language |
|-----------|---------------|----------|
| `typescript/` | `@github/copilot-sdk` | TypeScript (Node.js) |
| `python/` | `github-copilot-sdk` | Python |
| `go/` | `github.com/github/copilot-sdk/go` | Go |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
