# Config Sample: No Tools

Demonstrates configuring the Copilot SDK with **zero tools** and a custom system prompt that reflects the tool-less state. This validates two things:

1. **Tool removal** — Setting `availableTools: []` removes all built-in tools (bash, view, edit, grep, glob, etc.) from the agent's capabilities.
2. **Agent awareness** — The replaced system prompt tells the agent it has no tools, and the agent's response confirms this.

## What Each Sample Does

1. Creates a session with `availableTools: []` and a `systemMessage` in `replace` mode
2. Sends: _"What tools do you have available? List them."_
3. Prints the response — which should confirm the agent has no tools

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `availableTools` | `[]` (empty array) | Whitelists zero tools — all built-in tools are removed |
| `systemMessage.mode` | `"replace"` | Replaces the default system prompt entirely |
| `systemMessage.content` | Custom minimal prompt | Tells the agent it has no tools and can only respond with text |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
