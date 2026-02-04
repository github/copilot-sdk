# Error Handling Hook

The `onErrorOccurred` hook is called when errors occur during session execution. Use it to:

- Implement custom error logging
- Track error patterns
- Provide user-friendly error messages
- Trigger alerts for critical errors

## Hook Signature

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
type ErrorOccurredHandler = (
  input: ErrorOccurredHookInput,
  invocation: HookInvocation
) => Promise<ErrorOccurredHookOutput | null | undefined>;
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
ErrorOccurredHandler = Callable[
    [ErrorOccurredHookInput, HookInvocation],
    Awaitable[ErrorOccurredHookOutput | None]
]
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
type ErrorOccurredHandler func(
    input ErrorOccurredHookInput,
    invocation HookInvocation,
) (*ErrorOccurredHookOutput, error)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
public delegate Task<ErrorOccurredHookOutput?> ErrorOccurredHandler(
    ErrorOccurredHookInput input,
    HookInvocation invocation);
```

</details>

## Input

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp when the error occurred |
| `cwd` | string | Current working directory |
| `error` | string | Error message |
| `errorContext` | string | Where the error occurred: `"model_call"`, `"tool_execution"`, `"system"`, or `"user_input"` |
| `recoverable` | boolean | Whether the error can potentially be recovered from |

## Output

Return `null` or `undefined` to use default error handling. Otherwise, return an object with:

| Field | Type | Description |
|-------|------|-------------|
| `suppressOutput` | boolean | If true, don't show error output to user |
| `errorHandling` | string | How to handle: `"retry"`, `"skip"`, or `"abort"` |
| `retryCount` | number | Number of times to retry (if errorHandling is `"retry"`) |
| `userNotification` | string | Custom message to show the user |

## Examples

### Basic Error Logging

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      console.error(`[${invocation.sessionId}] Error: ${input.error}`);
      console.error(`  Context: ${input.errorContext}`);
      console.error(`  Recoverable: ${input.recoverable}`);
      return null;
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
async def on_error_occurred(input_data, invocation):
    print(f"[{invocation['session_id']}] Error: {input_data['error']}")
    print(f"  Context: {input_data['error_context']}")
    print(f"  Recoverable: {input_data['recoverable']}")
    return None

session = await client.create_session({
    "hooks": {"on_error_occurred": on_error_occurred}
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
    Hooks: &copilot.SessionHooks{
        OnErrorOccurred: func(input copilot.ErrorOccurredHookInput, inv copilot.HookInvocation) (*copilot.ErrorOccurredHookOutput, error) {
            fmt.Printf("[%s] Error: %s\n", inv.SessionID, input.Error)
            fmt.Printf("  Context: %s\n", input.ErrorContext)
            fmt.Printf("  Recoverable: %v\n", input.Recoverable)
            return nil, nil
        },
    },
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnErrorOccurred = (input, invocation) =>
        {
            Console.Error.WriteLine($"[{invocation.SessionId}] Error: {input.Error}");
            Console.Error.WriteLine($"  Type: {input.ErrorType}");
            if (!string.IsNullOrEmpty(input.Stack))
            {
                Console.Error.WriteLine($"  Stack: {input.Stack}");
            }
            return Task.FromResult<ErrorOccurredHookOutput?>(null);
        },
    },
});
```

</details>

### Send Errors to Monitoring Service

```typescript
import { captureException } from "@sentry/node"; // or your monitoring service

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      captureException(new Error(input.error), {
        tags: {
          sessionId: invocation.sessionId,
          errorType: input.errorType,
        },
        extra: {
          stack: input.stack,
          context: input.context,
          cwd: input.cwd,
        },
      });
      
      return null;
    },
  },
});
```

### User-Friendly Error Messages

```typescript
const ERROR_MESSAGES: Record<string, string> = {
  "rate_limit": "Too many requests. Please wait a moment and try again.",
  "auth_failed": "Authentication failed. Please check your credentials.",
  "network_error": "Network connection issue. Please check your internet connection.",
  "timeout": "Request timed out. Try breaking your request into smaller parts.",
};

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input) => {
      const friendlyMessage = ERROR_MESSAGES[input.errorType];
      
      if (friendlyMessage) {
        return {
          modifiedMessage: friendlyMessage,
        };
      }
      
      return null;
    },
  },
});
```

### Suppress Non-Critical Errors

```typescript
const SUPPRESSED_ERRORS = [
  "tool_not_found",
  "file_not_found",
];

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input) => {
      if (SUPPRESSED_ERRORS.includes(input.errorType)) {
        console.log(`Suppressed error: ${input.errorType}`);
        return { suppressError: true };
      }
      return null;
    },
  },
});
```

### Add Recovery Context

```typescript
const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input) => {
      if (input.errorType === "tool_execution_failed") {
        return {
          additionalContext: `
The tool failed. Here are some recovery suggestions:
- Check if required dependencies are installed
- Verify file paths are correct
- Try a simpler approach
          `.trim(),
        };
      }
      
      if (input.errorType === "rate_limit") {
        return {
          additionalContext: "Rate limit hit. Waiting before retry.",
        };
      }
      
      return null;
    },
  },
});
```

### Track Error Patterns

```typescript
interface ErrorStats {
  count: number;
  lastOccurred: number;
  contexts: string[];
}

const errorStats = new Map<string, ErrorStats>();

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      const key = `${input.errorType}:${input.error.substring(0, 50)}`;
      
      const existing = errorStats.get(key) || {
        count: 0,
        lastOccurred: 0,
        contexts: [],
      };
      
      existing.count++;
      existing.lastOccurred = input.timestamp;
      existing.contexts.push(invocation.sessionId);
      
      errorStats.set(key, existing);
      
      // Alert if error is recurring
      if (existing.count >= 5) {
        console.warn(`Recurring error detected: ${key} (${existing.count} times)`);
      }
      
      return null;
    },
  },
});
```

### Alert on Critical Errors

```typescript
const CRITICAL_ERRORS = ["auth_failed", "api_error", "system_error"];

const session = await client.createSession({
  hooks: {
    onErrorOccurred: async (input, invocation) => {
      if (CRITICAL_ERRORS.includes(input.errorType)) {
        await sendAlert({
          level: "critical",
          message: `Critical error in session ${invocation.sessionId}`,
          error: input.error,
          type: input.errorType,
          timestamp: new Date(input.timestamp).toISOString(),
        });
      }
      
      return null;
    },
  },
});
```

### Combine with Other Hooks for Context

```typescript
const sessionContext = new Map<string, { lastTool?: string; lastPrompt?: string }>();

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input, invocation) => {
      const ctx = sessionContext.get(invocation.sessionId) || {};
      ctx.lastTool = input.toolName;
      sessionContext.set(invocation.sessionId, ctx);
      return { permissionDecision: "allow" };
    },
    
    onUserPromptSubmitted: async (input, invocation) => {
      const ctx = sessionContext.get(invocation.sessionId) || {};
      ctx.lastPrompt = input.prompt.substring(0, 100);
      sessionContext.set(invocation.sessionId, ctx);
      return null;
    },
    
    onErrorOccurred: async (input, invocation) => {
      const ctx = sessionContext.get(invocation.sessionId);
      
      console.error(`Error in session ${invocation.sessionId}:`);
      console.error(`  Error: ${input.error}`);
      console.error(`  Type: ${input.errorType}`);
      if (ctx?.lastTool) {
        console.error(`  Last tool: ${ctx.lastTool}`);
      }
      if (ctx?.lastPrompt) {
        console.error(`  Last prompt: ${ctx.lastPrompt}...`);
      }
      
      return null;
    },
  },
});
```

## Best Practices

1. **Always log errors** - Even if you suppress them from users, keep logs for debugging.

2. **Categorize errors** - Use `errorType` to handle different errors appropriately.

3. **Don't swallow critical errors** - Only suppress errors you're certain are non-critical.

4. **Keep hooks fast** - Error handling shouldn't slow down recovery.

5. **Provide helpful context** - When errors occur, `additionalContext` can help the model recover.

6. **Monitor error patterns** - Track recurring errors to identify systemic issues.

## See Also

- [Hooks Overview](./overview.md)
- [Session Lifecycle Hooks](./session-lifecycle.md)
- [Debugging Guide](../debugging.md)
