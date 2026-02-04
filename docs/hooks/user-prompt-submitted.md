# User Prompt Submitted Hook

The `onUserPromptSubmitted` hook is called when a user submits a message. Use it to:

- Modify or enhance user prompts
- Add context before processing
- Filter or validate user input
- Implement prompt templates

## Hook Signature

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
type UserPromptSubmittedHandler = (
  input: UserPromptSubmittedHookInput,
  invocation: HookInvocation
) => Promise<UserPromptSubmittedHookOutput | null | undefined>;
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
UserPromptSubmittedHandler = Callable[
    [UserPromptSubmittedHookInput, HookInvocation],
    Awaitable[UserPromptSubmittedHookOutput | None]
]
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
type UserPromptSubmittedHandler func(
    input UserPromptSubmittedHookInput,
    invocation HookInvocation,
) (*UserPromptSubmittedHookOutput, error)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
public delegate Task<UserPromptSubmittedHookOutput?> UserPromptSubmittedHandler(
    UserPromptSubmittedHookInput input,
    HookInvocation invocation);
```

</details>

## Input

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp when the hook was triggered |
| `cwd` | string | Current working directory |
| `prompt` | string | The user's submitted prompt |

## Output

Return `null` or `undefined` to use the prompt unchanged. Otherwise, return an object with any of these fields:

| Field | Type | Description |
|-------|------|-------------|
| `modifiedPrompt` | string | Modified prompt to use instead of original |
| `additionalContext` | string | Extra context added to the conversation |
| `suppressOutput` | boolean | If true, suppress the assistant's response output |

## Examples

### Log All User Prompts

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input, invocation) => {
      console.log(`[${invocation.sessionId}] User: ${input.prompt}`);
      return null; // Pass through unchanged
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
async def on_user_prompt_submitted(input_data, invocation):
    print(f"[{invocation['session_id']}] User: {input_data['prompt']}")
    return None

session = await client.create_session({
    "hooks": {"on_user_prompt_submitted": on_user_prompt_submitted}
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
    Hooks: &copilot.SessionHooks{
        OnUserPromptSubmitted: func(input copilot.UserPromptSubmittedHookInput, inv copilot.HookInvocation) (*copilot.UserPromptSubmittedHookOutput, error) {
            fmt.Printf("[%s] User: %s\n", inv.SessionID, input.Prompt)
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
        OnUserPromptSubmitted = (input, invocation) =>
        {
            Console.WriteLine($"[{invocation.SessionId}] User: {input.Prompt}");
            return Task.FromResult<UserPromptSubmittedHookOutput?>(null);
        },
    },
});
```

</details>

### Add Project Context

```typescript
const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      const projectInfo = await getProjectInfo();
      
      return {
        additionalContext: `
Project: ${projectInfo.name}
Language: ${projectInfo.language}
Framework: ${projectInfo.framework}
        `.trim(),
      };
    },
  },
});
```

### Expand Shorthand Commands

```typescript
const SHORTCUTS: Record<string, string> = {
  "/fix": "Please fix the errors in the code",
  "/explain": "Please explain this code in detail",
  "/test": "Please write unit tests for this code",
  "/refactor": "Please refactor this code to improve readability and maintainability",
};

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      for (const [shortcut, expansion] of Object.entries(SHORTCUTS)) {
        if (input.prompt.startsWith(shortcut)) {
          const rest = input.prompt.slice(shortcut.length).trim();
          return {
            modifiedPrompt: `${expansion}${rest ? `: ${rest}` : ""}`,
          };
        }
      }
      return null;
    },
  },
});
```

### Content Filtering

```typescript
const BLOCKED_PATTERNS = [
  /password\s*[:=]/i,
  /api[_-]?key\s*[:=]/i,
  /secret\s*[:=]/i,
];

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      for (const pattern of BLOCKED_PATTERNS) {
        if (pattern.test(input.prompt)) {
          return {
            reject: true,
            rejectReason: "Please don't include sensitive credentials in your prompts. Use environment variables instead.",
          };
        }
      }
      return null;
    },
  },
});
```

### Enforce Prompt Length Limits

```typescript
const MAX_PROMPT_LENGTH = 10000;

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      if (input.prompt.length > MAX_PROMPT_LENGTH) {
        return {
          reject: true,
          rejectReason: `Prompt too long (${input.prompt.length} chars). Maximum allowed: ${MAX_PROMPT_LENGTH} chars. Please shorten your request.`,
        };
      }
      return null;
    },
  },
});
```

### Add User Preferences

```typescript
interface UserPreferences {
  codeStyle: "concise" | "verbose";
  preferredLanguage: string;
  experienceLevel: "beginner" | "intermediate" | "expert";
}

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      const prefs: UserPreferences = await loadUserPreferences();
      
      const contextParts = [];
      
      if (prefs.codeStyle === "concise") {
        contextParts.push("User prefers concise code with minimal comments.");
      } else {
        contextParts.push("User prefers verbose code with detailed comments.");
      }
      
      if (prefs.experienceLevel === "beginner") {
        contextParts.push("Explain concepts in simple terms.");
      }
      
      return {
        additionalContext: contextParts.join(" "),
      };
    },
  },
});
```

### Rate Limiting

```typescript
const promptTimestamps: number[] = [];
const RATE_LIMIT = 10; // prompts
const RATE_WINDOW = 60000; // 1 minute

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      const now = Date.now();
      
      // Remove timestamps outside the window
      while (promptTimestamps.length > 0 && promptTimestamps[0] < now - RATE_WINDOW) {
        promptTimestamps.shift();
      }
      
      if (promptTimestamps.length >= RATE_LIMIT) {
        return {
          reject: true,
          rejectReason: `Rate limit exceeded. Please wait before sending more prompts.`,
        };
      }
      
      promptTimestamps.push(now);
      return null;
    },
  },
});
```

### Prompt Templates

```typescript
const TEMPLATES: Record<string, (args: string) => string> = {
  "bug:": (desc) => `I found a bug: ${desc}

Please help me:
1. Understand why this is happening
2. Suggest a fix
3. Explain how to prevent similar bugs`,

  "feature:": (desc) => `I want to implement this feature: ${desc}

Please:
1. Outline the implementation approach
2. Identify potential challenges
3. Provide sample code`,
};

const session = await client.createSession({
  hooks: {
    onUserPromptSubmitted: async (input) => {
      for (const [prefix, template] of Object.entries(TEMPLATES)) {
        if (input.prompt.toLowerCase().startsWith(prefix)) {
          const args = input.prompt.slice(prefix.length).trim();
          return {
            modifiedPrompt: template(args),
          };
        }
      }
      return null;
    },
  },
});
```

## Best Practices

1. **Preserve user intent** - When modifying prompts, ensure the core intent remains clear.

2. **Be transparent about modifications** - If you significantly change a prompt, consider logging or notifying the user.

3. **Use `additionalContext` over `modifiedPrompt`** - Adding context is less intrusive than rewriting the prompt.

4. **Provide clear rejection reasons** - When rejecting prompts, explain why and how to fix it.

5. **Keep processing fast** - This hook runs on every user message. Avoid slow operations.

## See Also

- [Hooks Overview](./overview.md)
- [Session Lifecycle Hooks](./session-lifecycle.md)
- [Pre-Tool Use Hook](./pre-tool-use.md)
