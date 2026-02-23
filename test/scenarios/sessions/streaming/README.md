# Config Sample: Streaming

Demonstrates configuring the Copilot SDK with **`streaming: true`** to receive incremental response chunks. This validates that the server sends multiple `assistant.message_delta` events before the final `assistant.message` event.

## What Each Sample Does

1. Creates a session with `streaming: true`
2. Registers an event listener to count `assistant.message_delta` events
3. Sends: _"What is the capital of France?"_
4. Prints the final response and the number of streaming chunks received

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `streaming` | `true` | Enables incremental streaming — the server emits `assistant.message_delta` events as tokens are generated |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
