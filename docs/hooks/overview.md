# Session Hooks

Hooks allow you to intercept and customize the behavior of Copilot sessions at key points in the conversation lifecycle. Use hooks to:

- **Control tool execution** - approve, deny, or modify tool calls
- **Transform results** - modify tool outputs before they're processed
- **Add context** - inject additional information at session start
- **Handle errors** - implement custom error handling
- **Audit and log** - track all interactions for compliance

## Available Hooks

| Hook | Trigger | Use Case |
|------|---------|----------|
| [`onPreToolUse`](./pre-tool-use.md) | Before a tool executes | Permission control, argument validation |
| [`onPostToolUse`](./post-tool-use.md) | After a tool executes | Result transformation, logging |
| [`onUserPromptSubmitted`](./user-prompt-submitted.md) | When user sends a message | Prompt modification, filtering |
| [`onSessionStart`](./session-lifecycle.md#session-start) | Session begins | Add context, configure session |
| [`onSessionEnd`](./session-lifecycle.md#session-end) | Session ends | Cleanup, analytics |
| [`onErrorOccurred`](./error-handling.md) | Error happens | Custom error handling |

## Quick Start

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      console.log(`Tool called: ${input.toolName}`);
      // Allow all tools
      return { permissionDecision: "allow" };
    },
    onPostToolUse: async (input) => {
      console.log(`Tool result: ${JSON.stringify(input.toolResult)}`);
      return null; // No modifications
    },
    onSessionStart: async (input) => {
      return { additionalContext: "User prefers concise answers." };
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient

async def main():
    client = CopilotClient()

    async def on_pre_tool_use(input_data, invocation):
        print(f"Tool called: {input_data['toolName']}")
        return {"permissionDecision": "allow"}

    async def on_post_tool_use(input_data, invocation):
        print(f"Tool result: {input_data['toolResult']}")
        return None

    async def on_session_start(input_data, invocation):
        return {"additionalContext": "User prefers concise answers."}

    session = await client.create_session({
        "hooks": {
            "on_pre_tool_use": on_pre_tool_use,
            "on_post_tool_use": on_post_tool_use,
            "on_session_start": on_session_start,
        }
    })
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
    "context"
    "fmt"
    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    client := copilot.NewClient(nil)

    session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
        Hooks: &copilot.SessionHooks{
            OnPreToolUse: func(input copilot.PreToolUseHookInput, inv copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
                fmt.Printf("Tool called: %s\n", input.ToolName)
                return &copilot.PreToolUseHookOutput{
                    PermissionDecision: "allow",
                }, nil
            },
            OnPostToolUse: func(input copilot.PostToolUseHookInput, inv copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
                fmt.Printf("Tool result: %v\n", input.ToolResult)
                return nil, nil
            },
            OnSessionStart: func(input copilot.SessionStartHookInput, inv copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
                return &copilot.SessionStartHookOutput{
                    AdditionalContext: "User prefers concise answers.",
                }, nil
            },
        },
    })
    _ = session
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

var client = new CopilotClient();

var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnPreToolUse = (input, invocation) =>
        {
            Console.WriteLine($"Tool called: {input.ToolName}");
            return Task.FromResult<PreToolUseHookOutput?>(
                new PreToolUseHookOutput { PermissionDecision = "allow" }
            );
        },
        OnPostToolUse = (input, invocation) =>
        {
            Console.WriteLine($"Tool result: {input.ToolResult}");
            return Task.FromResult<PostToolUseHookOutput?>(null);
        },
        OnSessionStart = (input, invocation) =>
        {
            return Task.FromResult<SessionStartHookOutput?>(
                new SessionStartHookOutput { AdditionalContext = "User prefers concise answers." }
            );
        },
    },
});
```

</details>

## Hook Invocation Context

Every hook receives an `invocation` parameter with context about the current session:

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | string | The ID of the current session |

This allows hooks to maintain state or perform session-specific logic.

## Common Patterns

### Logging All Tool Calls

```typescript
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      console.log(`[${new Date().toISOString()}] Tool: ${input.toolName}, Args: ${JSON.stringify(input.toolArgs)}`);
      return { permissionDecision: "allow" };
    },
    onPostToolUse: async (input) => {
      console.log(`[${new Date().toISOString()}] Result: ${JSON.stringify(input.toolResult)}`);
      return null;
    },
  },
});
```

### Blocking Dangerous Tools

```typescript
const BLOCKED_TOOLS = ["shell", "bash", "exec"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (BLOCKED_TOOLS.includes(input.toolName)) {
        return {
          permissionDecision: "deny",
          permissionDecisionReason: "Shell access is not permitted",
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});
```

### Adding User Context

```typescript
const session = await client.createSession({
  hooks: {
    onSessionStart: async () => {
      const userPrefs = await loadUserPreferences();
      return {
        additionalContext: `User preferences: ${JSON.stringify(userPrefs)}`,
      };
    },
  },
});
```

## Hook Guides

- **[Pre-Tool Use Hook](./pre-tool-use.md)** - Control tool execution permissions
- **[Post-Tool Use Hook](./post-tool-use.md)** - Transform tool results
- **[User Prompt Submitted Hook](./user-prompt-submitted.md)** - Modify user prompts
- **[Session Lifecycle Hooks](./session-lifecycle.md)** - Session start and end
- **[Error Handling Hook](./error-handling.md)** - Custom error handling

## See Also

- [Getting Started Guide](../getting-started.md)
- [Custom Tools](../getting-started.md#step-4-add-custom-tools)
- [Debugging Guide](../debugging.md)
