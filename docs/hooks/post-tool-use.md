# Post-Tool Use Hook

The `onPostToolUse` hook is called **after** a tool executes. Use it to:

- Transform or filter tool results
- Log tool execution for auditing
- Add context based on results
- Suppress results from the conversation

## Hook Signature

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
type PostToolUseHandler = (
  input: PostToolUseHookInput,
  invocation: HookInvocation
) => Promise<PostToolUseHookOutput | null | undefined>;
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
PostToolUseHandler = Callable[
    [PostToolUseHookInput, HookInvocation],
    Awaitable[PostToolUseHookOutput | None]
]
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
type PostToolUseHandler func(
    input PostToolUseHookInput,
    invocation HookInvocation,
) (*PostToolUseHookOutput, error)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
public delegate Task<PostToolUseHookOutput?> PostToolUseHandler(
    PostToolUseHookInput input,
    HookInvocation invocation);
```

</details>

## Input

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp when the hook was triggered |
| `cwd` | string | Current working directory |
| `toolName` | string | Name of the tool that was called |
| `toolArgs` | object | Arguments that were passed to the tool |
| `toolResult` | object | Result returned by the tool |

## Output

Return `null` or `undefined` to pass through the result unchanged. Otherwise, return an object with any of these fields:

| Field | Type | Description |
|-------|------|-------------|
| `modifiedResult` | object | Modified result to use instead of original |
| `additionalContext` | string | Extra context injected into the conversation |
| `suppressOutput` | boolean | If true, result won't appear in conversation |

## Examples

### Log All Tool Results

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input, invocation) => {
      console.log(`[${invocation.sessionId}] Tool: ${input.toolName}`);
      console.log(`  Args: ${JSON.stringify(input.toolArgs)}`);
      console.log(`  Result: ${JSON.stringify(input.toolResult)}`);
      return null; // Pass through unchanged
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
async def on_post_tool_use(input_data, invocation):
    print(f"[{invocation['session_id']}] Tool: {input_data['toolName']}")
    print(f"  Args: {input_data['toolArgs']}")
    print(f"  Result: {input_data['toolResult']}")
    return None  # Pass through unchanged

session = await client.create_session({
    "hooks": {"on_post_tool_use": on_post_tool_use}
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
    Hooks: &copilot.SessionHooks{
        OnPostToolUse: func(input copilot.PostToolUseHookInput, inv copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
            fmt.Printf("[%s] Tool: %s\n", inv.SessionID, input.ToolName)
            fmt.Printf("  Args: %v\n", input.ToolArgs)
            fmt.Printf("  Result: %v\n", input.ToolResult)
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
        OnPostToolUse = (input, invocation) =>
        {
            Console.WriteLine($"[{invocation.SessionId}] Tool: {input.ToolName}");
            Console.WriteLine($"  Args: {input.ToolArgs}");
            Console.WriteLine($"  Result: {input.ToolResult}");
            return Task.FromResult<PostToolUseHookOutput?>(null);
        },
    },
});
```

</details>

### Redact Sensitive Data

```typescript
const SENSITIVE_PATTERNS = [
  /api[_-]?key["\s:=]+["']?[\w-]+["']?/gi,
  /password["\s:=]+["']?[\w-]+["']?/gi,
  /secret["\s:=]+["']?[\w-]+["']?/gi,
];

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      if (typeof input.toolResult === "string") {
        let redacted = input.toolResult;
        for (const pattern of SENSITIVE_PATTERNS) {
          redacted = redacted.replace(pattern, "[REDACTED]");
        }
        
        if (redacted !== input.toolResult) {
          return { modifiedResult: redacted };
        }
      }
      return null;
    },
  },
});
```

### Truncate Large Results

```typescript
const MAX_RESULT_LENGTH = 10000;

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      const resultStr = JSON.stringify(input.toolResult);
      
      if (resultStr.length > MAX_RESULT_LENGTH) {
        return {
          modifiedResult: {
            truncated: true,
            originalLength: resultStr.length,
            content: resultStr.substring(0, MAX_RESULT_LENGTH) + "...",
          },
          additionalContext: `Note: Result was truncated from ${resultStr.length} to ${MAX_RESULT_LENGTH} characters.`,
        };
      }
      return null;
    },
  },
});
```

### Add Context Based on Results

```typescript
const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      // If a file read returned an error, add helpful context
      if (input.toolName === "read_file" && input.toolResult?.error) {
        return {
          additionalContext: "Tip: If the file doesn't exist, consider creating it or checking the path.",
        };
      }
      
      // If shell command failed, add debugging hint
      if (input.toolName === "shell" && input.toolResult?.exitCode !== 0) {
        return {
          additionalContext: "The command failed. Check if required dependencies are installed.",
        };
      }
      
      return null;
    },
  },
});
```

### Filter Error Stack Traces

```typescript
const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      if (input.toolResult?.error && input.toolResult?.stack) {
        // Remove internal stack trace details
        return {
          modifiedResult: {
            error: input.toolResult.error,
            // Keep only first 3 lines of stack
            stack: input.toolResult.stack.split("\n").slice(0, 3).join("\n"),
          },
        };
      }
      return null;
    },
  },
});
```

### Audit Trail for Compliance

```typescript
interface AuditEntry {
  timestamp: number;
  sessionId: string;
  toolName: string;
  args: unknown;
  result: unknown;
  success: boolean;
}

const auditLog: AuditEntry[] = [];

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input, invocation) => {
      auditLog.push({
        timestamp: input.timestamp,
        sessionId: invocation.sessionId,
        toolName: input.toolName,
        args: input.toolArgs,
        result: input.toolResult,
        success: !input.toolResult?.error,
      });
      
      // Optionally persist to database/file
      await saveAuditLog(auditLog);
      
      return null;
    },
  },
});
```

### Suppress Noisy Results

```typescript
const NOISY_TOOLS = ["list_directory", "search_codebase"];

const session = await client.createSession({
  hooks: {
    onPostToolUse: async (input) => {
      if (NOISY_TOOLS.includes(input.toolName)) {
        // Summarize instead of showing full result
        const items = Array.isArray(input.toolResult) 
          ? input.toolResult 
          : input.toolResult?.items || [];
        
        return {
          modifiedResult: {
            summary: `Found ${items.length} items`,
            firstFew: items.slice(0, 5),
          },
        };
      }
      return null;
    },
  },
});
```

## Best Practices

1. **Return `null` when no changes needed** - This is more efficient than returning an empty object or the same result.

2. **Be careful with result modification** - Changing results can affect how the model interprets tool output. Only modify when necessary.

3. **Use `additionalContext` for hints** - Instead of modifying results, add context to help the model interpret them.

4. **Consider privacy when logging** - Tool results may contain sensitive data. Apply redaction before logging.

5. **Keep hooks fast** - Post-tool hooks run synchronously. Heavy processing should be done asynchronously or batched.

## See Also

- [Hooks Overview](./overview.md)
- [Pre-Tool Use Hook](./pre-tool-use.md)
- [Error Handling Hook](./error-handling.md)
