# Config Sample: System Message

Demonstrates configuring the Copilot SDK's **system message** using `replace` mode. This validates that a custom system prompt fully replaces the default system prompt, changing the agent's personality and response style.

## Append vs Replace Modes

| Mode | Behavior |
|------|----------|
| `"append"` | Adds your content **after** the default system prompt. The agent retains its base personality plus your additions. |
| `"replace"` | **Replaces** the entire default system prompt with your content. The agent's personality is fully defined by your prompt. |

## What Each Sample Does

1. Creates a session with `systemMessage` in `replace` mode using a pirate personality prompt
2. Sends: _"What is the capital of France?"_
3. Prints the response — which should be in pirate speak (containing "Arrr!", nautical terms, etc.)

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `systemMessage.mode` | `"replace"` | Replaces the default system prompt entirely |
| `systemMessage.content` | Pirate personality prompt | Instructs the agent to always respond in pirate speak |
| `availableTools` | `[]` (empty array) | No tools — focuses the test on system message behavior |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
