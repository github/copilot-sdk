# Config Sample: Infinite Sessions

Demonstrates configuring the Copilot SDK with **infinite sessions** enabled, which uses context compaction to allow sessions to continue beyond the model's context window limit.

## What This Tests

1. **Config acceptance** — The `infiniteSessions` configuration with compaction thresholds is accepted by the server without errors.
2. **Session continuity** — Multiple messages are sent and responses received successfully with infinite sessions enabled.

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `infiniteSessions.enabled` | `true` | Enables context compaction for the session |
| `infiniteSessions.backgroundCompactionThreshold` | `0.80` | Triggers background compaction at 80% context usage |
| `infiniteSessions.bufferExhaustionThreshold` | `0.95` | Forces compaction at 95% context usage |
| `availableTools` | `[]` | No tools — keeps context small for testing |
| `systemMessage.mode` | `"replace"` | Replaces the default system prompt |

## How It Works

When `infiniteSessions` is enabled, the server monitors context window usage. As the conversation grows:

- At `backgroundCompactionThreshold` (80%), the server begins compacting older messages in the background.
- At `bufferExhaustionThreshold` (95%), compaction is forced before the next message is processed.

This allows sessions to run indefinitely without hitting context limits.

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
