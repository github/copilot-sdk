# Config Sample: Tool Overrides

Demonstrates how registering a custom tool with the same name as a built-in tool automatically overrides the built-in. The SDK's `mergeExcludedTools` logic adds custom tool names to `excludedTools`, so the CLI uses your implementation instead.

## What Each Sample Does

1. Creates a session with a custom `grep` tool that returns `"CUSTOM_GREP_RESULT: <query>"`
2. Sends: _"Use grep to search for the word 'hello'"_
3. Prints the response — which should contain `CUSTOM_GREP_RESULT` (proving the custom tool ran, not the built-in)

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `tools` | Custom `grep` tool | Overrides the built-in `grep` with a custom implementation |

Behind the scenes, the SDK automatically adds `"grep"` to `excludedTools` so the CLI's built-in grep is disabled.

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.

## Verification

The verify script checks that:
- The response contains `CUSTOM_GREP_RESULT` (custom tool was invoked)
- The response does **not** contain typical built-in grep output patterns
