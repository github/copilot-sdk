# Config Sample: Tool Overrides

Demonstrates how to override a built-in tool with a custom implementation using the `overridesBuiltInTool` flag. When this flag is set on a custom tool, the SDK knows to disable the corresponding built-in tool so your implementation is used instead.

## What Each Sample Does

1. Creates a session with a custom `grep` tool (with `overridesBuiltInTool` enabled) that returns `"CUSTOM_GREP_RESULT: <query>"`
2. Sends: _"Use grep to search for the word 'hello'"_
3. Prints the response — which should contain `CUSTOM_GREP_RESULT` (proving the custom tool ran, not the built-in)

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `tools` | Custom `grep` tool | Provides a custom grep implementation |
| `overridesBuiltInTool` | `true` | Tells the SDK to disable the built-in `grep` in favor of the custom one |

The flag is set per-tool in TypeScript (`overridesBuiltInTool: true`), Python (`overrides_built_in_tool=True`), and Go (`OverridesBuiltInTool: true`). In C#, set `is_override` in the tool's `AdditionalProperties` via `AIFunctionFactoryOptions`.

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.

## Verification

The verify script checks that:
- The response contains `CUSTOM_GREP_RESULT` (custom tool was invoked)
- The response does **not** contain typical built-in grep output patterns
