# Config Sample: Tool Filtering

Demonstrates advanced tool filtering using the `availableTools` whitelist. This restricts the agent to only the specified read-only tools, removing all others (bash, edit, create_file, etc.).

The Copilot SDK supports two complementary filtering mechanisms:

- **`availableTools`** (whitelist) — Only the listed tools are available. All others are removed.
- **`excludedTools`** (blacklist) — All tools are available *except* the listed ones.

This sample tests the **whitelist** approach with `["grep", "glob", "view"]`.

## What Each Sample Does

1. Creates a session with `availableTools: ["grep", "glob", "view"]` and a `systemMessage` in `replace` mode
2. Sends: _"What tools do you have available? List each one by name."_
3. Prints the response — which should list only grep, glob, and view

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `availableTools` | `["grep", "glob", "view"]` | Whitelists only read-only tools |
| `systemMessage.mode` | `"replace"` | Replaces the default system prompt entirely |
| `systemMessage.content` | Custom prompt | Instructs the agent to list its available tools |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.

## Verification

The verify script checks that:
- The response mentions at least one whitelisted tool (grep, glob, or view)
- The response does **not** mention excluded tools (bash, edit, or create_file)
