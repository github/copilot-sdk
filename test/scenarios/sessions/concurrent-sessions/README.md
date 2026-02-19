# Config Sample: Concurrent Sessions

Demonstrates creating **multiple sessions on the same client** with different configurations and verifying that each session maintains its own isolated state.

## What This Tests

1. **Session isolation** — Two sessions created on the same client receive different system prompts and respond according to their own persona, not the other's.
2. **Concurrent operation** — Both sessions can be used in parallel without interference.

## What Each Sample Does

1. Creates a client, then opens two sessions concurrently:
   - **Session 1** — system prompt: _"You are a pirate. Always say Arrr!"_
   - **Session 2** — system prompt: _"You are a robot. Always say BEEP BOOP!"_
2. Sends the same question (_"What is the capital of France?"_) to both sessions
3. Prints both responses with labels (`Session 1 (pirate):` and `Session 2 (robot):`)
4. Destroys both sessions

## Configuration

| Option | Session 1 | Session 2 |
|--------|-----------|-----------|
| `systemMessage.mode` | `"replace"` | `"replace"` |
| `systemMessage.content` | Pirate persona | Robot persona |
| `availableTools` | `[]` | `[]` |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
