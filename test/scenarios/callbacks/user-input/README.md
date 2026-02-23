# Config Sample: User Input Request

Demonstrates the **user input request flow** — the runtime's `ask_user` tool triggers a callback to the SDK, allowing the host application to programmatically respond to agent questions without human interaction.

This pattern is useful for:
- **Automated pipelines** where answers are predetermined or fetched from config
- **Custom UIs** that intercept user input requests and present their own dialogs
- **Testing** agent flows that require user interaction

## How It Works

1. **Enable `onUserInputRequest` callback** on the session
2. The callback auto-responds with `"Paris"` whenever the agent asks a question via `ask_user`
3. **Send a prompt** that instructs the agent to use `ask_user` to ask which city the user is interested in
4. The agent receives `"Paris"` as the answer and tells us about it
5. Print the response and confirm the user input flow worked via a log

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `onUserInputRequest` | Returns `{ answer: "Paris", wasFreeform: true }` | Auto-responds to `ask_user` tool calls |
| `onPermissionRequest` | Auto-approve | No permission dialogs |
| `hooks.onPreToolUse` | Auto-allow | No tool confirmation prompts |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
