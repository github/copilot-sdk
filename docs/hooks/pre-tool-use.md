# Pre-Tool Use Hook

The `onPreToolUse` hook is called **before** a tool executes. Use it to:

- Approve or deny tool execution
- Modify tool arguments
- Add context for the tool
- Suppress tool output from the conversation

## Hook Signature

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
type PreToolUseHandler = (
  input: PreToolUseHookInput,
  invocation: HookInvocation
) => Promise<PreToolUseHookOutput | null | undefined>;
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
PreToolUseHandler = Callable[
    [PreToolUseHookInput, HookInvocation],
    Awaitable[PreToolUseHookOutput | None]
]
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
type PreToolUseHandler func(
    input PreToolUseHookInput,
    invocation HookInvocation,
) (*PreToolUseHookOutput, error)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
public delegate Task<PreToolUseHookOutput?> PreToolUseHandler(
    PreToolUseHookInput input,
    HookInvocation invocation);
```

</details>

## Input

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp when the hook was triggered |
| `cwd` | string | Current working directory |
| `toolName` | string | Name of the tool being called |
| `toolArgs` | object | Arguments passed to the tool |

## Output

Return `null` or `undefined` to allow the tool to execute with no changes. Otherwise, return an object with any of these fields:

| Field | Type | Description |
|-------|------|-------------|
| `permissionDecision` | `"allow"` \| `"deny"` \| `"ask"` | Whether to allow the tool call |
| `permissionDecisionReason` | string | Explanation shown to user (for deny/ask) |
| `modifiedArgs` | object | Modified arguments to pass to the tool |
| `additionalContext` | string | Extra context injected into the conversation |
| `suppressOutput` | boolean | If true, tool output won't appear in conversation |

### Permission Decisions

| Decision | Behavior |
|----------|----------|
| `"allow"` | Tool executes normally |
| `"deny"` | Tool is blocked, reason shown to user |
| `"ask"` | User is prompted to approve (interactive mode) |

## Examples

### Allow All Tools (Logging Only)

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input, invocation) => {
      console.log(`[${invocation.sessionId}] Calling ${input.toolName}`);
      console.log(`  Args: ${JSON.stringify(input.toolArgs)}`);
      return { permissionDecision: "allow" };
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
async def on_pre_tool_use(input_data, invocation):
    print(f"[{invocation['session_id']}] Calling {input_data['toolName']}")
    print(f"  Args: {input_data['toolArgs']}")
    return {"permissionDecision": "allow"}

session = await client.create_session({
    "hooks": {"on_pre_tool_use": on_pre_tool_use}
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
    Hooks: &copilot.SessionHooks{
        OnPreToolUse: func(input copilot.PreToolUseHookInput, inv copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
            fmt.Printf("[%s] Calling %s\n", inv.SessionID, input.ToolName)
            fmt.Printf("  Args: %v\n", input.ToolArgs)
            return &copilot.PreToolUseHookOutput{
                PermissionDecision: "allow",
            }, nil
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
        OnPreToolUse = (input, invocation) =>
        {
            Console.WriteLine($"[{invocation.SessionId}] Calling {input.ToolName}");
            Console.WriteLine($"  Args: {input.ToolArgs}");
            return Task.FromResult<PreToolUseHookOutput?>(
                new PreToolUseHookOutput { PermissionDecision = "allow" }
            );
        },
    },
});
```

</details>

### Block Specific Tools

```typescript
const BLOCKED_TOOLS = ["shell", "bash", "write_file", "delete_file"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (BLOCKED_TOOLS.includes(input.toolName)) {
        return {
          permissionDecision: "deny",
          permissionDecisionReason: `Tool '${input.toolName}' is not permitted in this environment`,
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});
```

### Modify Tool Arguments

```typescript
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      // Add a default timeout to all shell commands
      if (input.toolName === "shell" && input.toolArgs) {
        const args = input.toolArgs as { command: string; timeout?: number };
        return {
          permissionDecision: "allow",
          modifiedArgs: {
            ...args,
            timeout: args.timeout ?? 30000, // Default 30s timeout
          },
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});
```

### Restrict File Access to Specific Directories

```typescript
const ALLOWED_DIRECTORIES = ["/home/user/projects", "/tmp"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (input.toolName === "read_file" || input.toolName === "write_file") {
        const args = input.toolArgs as { path: string };
        const isAllowed = ALLOWED_DIRECTORIES.some(dir => 
          args.path.startsWith(dir)
        );
        
        if (!isAllowed) {
          return {
            permissionDecision: "deny",
            permissionDecisionReason: `Access to '${args.path}' is not permitted. Allowed directories: ${ALLOWED_DIRECTORIES.join(", ")}`,
          };
        }
      }
      return { permissionDecision: "allow" };
    },
  },
});
```

### Suppress Verbose Tool Output

```typescript
const VERBOSE_TOOLS = ["list_directory", "search_files"];

const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      return {
        permissionDecision: "allow",
        suppressOutput: VERBOSE_TOOLS.includes(input.toolName),
      };
    },
  },
});
```

### Add Context Based on Tool

```typescript
const session = await client.createSession({
  hooks: {
    onPreToolUse: async (input) => {
      if (input.toolName === "query_database") {
        return {
          permissionDecision: "allow",
          additionalContext: "Remember: This database uses PostgreSQL syntax. Always use parameterized queries.",
        };
      }
      return { permissionDecision: "allow" };
    },
  },
});
```

## Best Practices

1. **Always return a decision** - Returning `null` allows the tool, but being explicit with `{ permissionDecision: "allow" }` is clearer.

2. **Provide helpful denial reasons** - When denying, explain why so users understand:
   ```typescript
   return {
     permissionDecision: "deny",
     permissionDecisionReason: "Shell commands require approval. Please describe what you want to accomplish.",
   };
   ```

3. **Be careful with argument modification** - Ensure modified args maintain the expected schema for the tool.

4. **Consider performance** - Pre-tool hooks run synchronously before each tool call. Keep them fast.

5. **Use `suppressOutput` judiciously** - Suppressing output means the model won't see the result, which may affect conversation quality.

## See Also

- [Hooks Overview](./overview.md)
- [Post-Tool Use Hook](./post-tool-use.md)
- [Debugging Guide](../debugging.md)
