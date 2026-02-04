# Session Lifecycle Hooks

Session lifecycle hooks let you respond to session start and end events. Use them to:

- Initialize context when sessions begin
- Clean up resources when sessions end
- Track session metrics and analytics
- Configure session behavior dynamically

## Session Start Hook {#session-start}

The `onSessionStart` hook is called when a session begins (new or resumed).

### Hook Signature

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
type SessionStartHandler = (
  input: SessionStartHookInput,
  invocation: HookInvocation
) => Promise<SessionStartHookOutput | null | undefined>;
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
SessionStartHandler = Callable[
    [SessionStartHookInput, HookInvocation],
    Awaitable[SessionStartHookOutput | None]
]
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
type SessionStartHandler func(
    input SessionStartHookInput,
    invocation HookInvocation,
) (*SessionStartHookOutput, error)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
public delegate Task<SessionStartHookOutput?> SessionStartHandler(
    SessionStartHookInput input,
    HookInvocation invocation);
```

</details>

### Input

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp when the hook was triggered |
| `cwd` | string | Current working directory |
| `source` | `"startup"` \| `"resume"` \| `"new"` | How the session was started |
| `initialPrompt` | string \| undefined | The initial prompt if provided |

### Output

| Field | Type | Description |
|-------|------|-------------|
| `additionalContext` | string | Context to add at session start |
| `modifiedConfig` | object | Override session configuration |

### Examples

#### Add Project Context at Start

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      console.log(`Session ${invocation.sessionId} started (${input.source})`);
      
      const projectInfo = await detectProjectType(input.cwd);
      
      return {
        additionalContext: `
This is a ${projectInfo.type} project.
Main language: ${projectInfo.language}
Package manager: ${projectInfo.packageManager}
        `.trim(),
      };
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
async def on_session_start(input_data, invocation):
    print(f"Session {invocation['session_id']} started ({input_data['source']})")
    
    project_info = await detect_project_type(input_data["cwd"])
    
    return {
        "additionalContext": f"""
This is a {project_info['type']} project.
Main language: {project_info['language']}
Package manager: {project_info['packageManager']}
        """.strip()
    }

session = await client.create_session({
    "hooks": {"on_session_start": on_session_start}
})
```

</details>

#### Handle Session Resume

```typescript
const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      if (input.source === "resume") {
        // Load previous session state
        const previousState = await loadSessionState(invocation.sessionId);
        
        return {
          additionalContext: `
Session resumed. Previous context:
- Last topic: ${previousState.lastTopic}
- Open files: ${previousState.openFiles.join(", ")}
          `.trim(),
        };
      }
      return null;
    },
  },
});
```

#### Load User Preferences

```typescript
const session = await client.createSession({
  hooks: {
    onSessionStart: async () => {
      const preferences = await loadUserPreferences();
      
      const contextParts = [];
      
      if (preferences.language) {
        contextParts.push(`Preferred language: ${preferences.language}`);
      }
      if (preferences.codeStyle) {
        contextParts.push(`Code style: ${preferences.codeStyle}`);
      }
      if (preferences.verbosity === "concise") {
        contextParts.push("Keep responses brief and to the point.");
      }
      
      return {
        additionalContext: contextParts.join("\n"),
      };
    },
  },
});
```

---

## Session End Hook {#session-end}

The `onSessionEnd` hook is called when a session ends.

### Hook Signature

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
type SessionEndHandler = (
  input: SessionEndHookInput,
  invocation: HookInvocation
) => Promise<SessionEndHookOutput | null | undefined>;
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
SessionEndHandler = Callable[
    [SessionEndHookInput, HookInvocation],
    Awaitable[SessionEndHookOutput | None]
]
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
type SessionEndHandler func(
    input SessionEndHookInput,
    invocation HookInvocation,
) (*SessionEndHookOutput, error)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
public delegate Task<SessionEndHookOutput?> SessionEndHandler(
    SessionEndHookInput input,
    HookInvocation invocation);
```

</details>

### Input

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp when the hook was triggered |
| `cwd` | string | Current working directory |
| `reason` | string | Why the session ended (see below) |
| `finalMessage` | string \| undefined | The last message from the session |
| `error` | string \| undefined | Error message if session ended due to error |

#### End Reasons

| Reason | Description |
|--------|-------------|
| `"complete"` | Session completed normally |
| `"error"` | Session ended due to an error |
| `"abort"` | Session was aborted by user or code |
| `"timeout"` | Session timed out |
| `"user_exit"` | User explicitly ended the session |

### Output

| Field | Type | Description |
|-------|------|-------------|
| `suppressOutput` | boolean | Suppress the final session output |
| `cleanupActions` | string[] | List of cleanup actions to perform |
| `sessionSummary` | string | Summary of the session for logging/analytics |

### Examples

#### Track Session Metrics

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const sessionStartTimes = new Map<string, number>();

const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      sessionStartTimes.set(invocation.sessionId, input.timestamp);
      return null;
    },
    onSessionEnd: async (input, invocation) => {
      const startTime = sessionStartTimes.get(invocation.sessionId);
      const duration = startTime ? input.timestamp - startTime : 0;
      
      await recordMetrics({
        sessionId: invocation.sessionId,
        duration,
        endReason: input.reason,
      });
      
      sessionStartTimes.delete(invocation.sessionId);
      return null;
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
session_start_times = {}

async def on_session_start(input_data, invocation):
    session_start_times[invocation["session_id"]] = input_data["timestamp"]
    return None

async def on_session_end(input_data, invocation):
    start_time = session_start_times.get(invocation["session_id"])
    duration = input_data["timestamp"] - start_time if start_time else 0
    
    await record_metrics({
        "session_id": invocation["session_id"],
        "duration": duration,
        "end_reason": input_data["reason"],
    })
    
    session_start_times.pop(invocation["session_id"], None)
    return None

session = await client.create_session({
    "hooks": {
        "on_session_start": on_session_start,
        "on_session_end": on_session_end,
    }
})
```

</details>

#### Clean Up Resources

```typescript
const sessionResources = new Map<string, { tempFiles: string[] }>();

const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      sessionResources.set(invocation.sessionId, { tempFiles: [] });
      return null;
    },
    onSessionEnd: async (input, invocation) => {
      const resources = sessionResources.get(invocation.sessionId);
      
      if (resources) {
        // Clean up temp files
        for (const file of resources.tempFiles) {
          await fs.unlink(file).catch(() => {});
        }
        sessionResources.delete(invocation.sessionId);
      }
      
      console.log(`Session ${invocation.sessionId} ended: ${input.reason}`);
      return null;
    },
  },
});
```

#### Save Session State for Resume

```typescript
const session = await client.createSession({
  hooks: {
    onSessionEnd: async (input, invocation) => {
      if (input.reason !== "error") {
        // Save state for potential resume
        await saveSessionState(invocation.sessionId, {
          endTime: input.timestamp,
          cwd: input.cwd,
          reason: input.reason,
        });
      }
      return null;
    },
  },
});
```

#### Log Session Summary

```typescript
const sessionData: Record<string, { prompts: number; tools: number; startTime: number }> = {};

const session = await client.createSession({
  hooks: {
    onSessionStart: async (input, invocation) => {
      sessionData[invocation.sessionId] = { 
        prompts: 0, 
        tools: 0, 
        startTime: input.timestamp 
      };
      return null;
    },
    onUserPromptSubmitted: async (_, invocation) => {
      sessionData[invocation.sessionId].prompts++;
      return null;
    },
    onPreToolUse: async (_, invocation) => {
      sessionData[invocation.sessionId].tools++;
      return { permissionDecision: "allow" };
    },
    onSessionEnd: async (input, invocation) => {
      const data = sessionData[invocation.sessionId];
      console.log(`
Session Summary:
  ID: ${invocation.sessionId}
  Duration: ${(input.timestamp - data.startTime) / 1000}s
  Prompts: ${data.prompts}
  Tool calls: ${data.tools}
  End reason: ${input.reason}
      `.trim());
      
      delete sessionData[invocation.sessionId];
      return null;
    },
  },
});
```

## Best Practices

1. **Keep `onSessionStart` fast** - Users are waiting for the session to be ready.

2. **Handle all end reasons** - Don't assume sessions end cleanly; handle errors and aborts.

3. **Clean up resources** - Use `onSessionEnd` to free any resources allocated during the session.

4. **Store minimal state** - If tracking session data, keep it lightweight.

5. **Make cleanup idempotent** - `onSessionEnd` might not be called if the process crashes.

## See Also

- [Hooks Overview](./overview.md)
- [Error Handling Hook](./error-handling.md)
- [Debugging Guide](../debugging.md)
