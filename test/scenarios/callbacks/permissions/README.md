# Config Sample: Permissions

Demonstrates the **permission request flow** — the runtime asks the SDK for permission before executing tools, and the SDK can approve or deny each request. This sample approves all requests while logging which tools were invoked.

This pattern is the foundation for:
- **Enterprise policy enforcement** where certain tools are restricted
- **Audit logging** where all tool invocations must be recorded
- **Interactive approval UIs** where a human confirms sensitive operations
- **Fine-grained access control** based on tool name, arguments, or context

## How It Works

1. **Enable `onPermissionRequest` handler** on the session config
2. **Track which tools requested permission** in a log array
3. **Approve all permission requests** (return `kind: "approved"`)
4. **Send a prompt that triggers tool use** (e.g., listing files via glob)
5. **Print the permission log** showing which tools were approved

## What Each Sample Does

1. Creates a session with an `onPermissionRequest` callback that logs and approves
2. Sends: _"List the files in the current directory using glob with pattern '*'."_
3. The runtime calls `onPermissionRequest` before each tool execution
4. The callback records `approved:<toolName>` and returns approval
5. Prints the agent's response
6. Dumps the permission log showing all approved tool invocations

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `onPermissionRequest` | Log + approve | Records tool name, returns `approved` |
| `hooks.onPreToolUse` | Auto-allow | No tool confirmation prompts |

## Key Insight

The `onPermissionRequest` handler gives the integrator full control over which tools the agent can execute. By inspecting the request (tool name, arguments), you can implement allow/deny lists, require human approval for dangerous operations, or log every action for compliance. Returning `{ kind: "denied" }` blocks the tool from running.

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
