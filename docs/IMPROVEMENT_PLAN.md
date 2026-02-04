# Documentation Improvement Plan

Generated: 2026-02-03
Audited by: docs-maintenance agent

## Summary

- **Coverage**: ~85% of SDK features documented
- **Sample Accuracy**: 8 issues found (mostly in Go examples missing context parameter)
- **Link Health**: 1 broken link
- **Multi-language**: All docs have 4-language examples ✓

---

## Critical Issues (Fix Immediately)

### 1. Go Examples Missing Context Parameter

- **Files**: Multiple docs
- **Problem**: Go SDK's `CreateSession` requires `context.Context` as first parameter, but many docs show it without context
- **Impact**: Examples won't compile

**Files affected:**
- `docs/hooks/overview.md` (lines 97, 98)
- `docs/hooks/pre-tool-use.md` (line 129)
- `docs/hooks/post-tool-use.md` (line 122)
- `docs/hooks/user-prompt-submitted.md` (line 116)
- `docs/hooks/session-lifecycle.md` (multiple)
- `docs/hooks/error-handling.md` (line 123)
- `docs/debugging.md` (lines 50-52, 170-173, 225)
- `docs/auth/index.md` (lines 125-129)
- `docs/auth/byok.md` (lines 111-121)

**Example fix for `docs/hooks/overview.md` line 97-98:**

```go
// Current (wrong):
client, _ := copilot.NewClient(copilot.ClientOptions{})
session, _ := client.CreateSession(ctx, copilot.SessionConfig{

// Should be:
client := copilot.NewClient(nil)
session, err := client.CreateSession(context.Background(), &copilot.SessionConfig{
```

### 2. Go NewClient Takes Pointer

- **Files**: `docs/hooks/overview.md`, `docs/debugging.md`
- **Problem**: `NewClient` takes `*ClientOptions` not `ClientOptions`
- **Impact**: Examples won't compile

**Example fix:**

```go
// Current (wrong):
client, _ := copilot.NewClient(copilot.ClientOptions{})

// Should be:
client := copilot.NewClient(nil)  // or
client := copilot.NewClient(&copilot.ClientOptions{LogLevel: "debug"})
```

### 3. Go NewClient Returns Only Client (Not Error)

- **Files**: `docs/debugging.md` (lines 50-52, 170-173, 225)
- **Problem**: Docs show `client, err := copilot.NewClient(...)` but `NewClient` returns only `*Client`
- **Impact**: Examples won't compile

**Fix:**

```go
// Current (wrong):
client, err := copilot.NewClient(copilot.ClientOptions{...})

// Should be:
client := copilot.NewClient(&copilot.ClientOptions{...})
```

---

## High Priority (Should Fix Soon)

### 1. Session Persistence Guide Has Wrong Method Names

- **File**: `docs/guides/session-persistence.md`
- **Lines**: Multiple
- **Problem**: Uses non-existent methods like `sendPrompt`, `CreateSessionOptions`, `PromptOptions`
- **Fix**: Update to use correct API methods

**TypeScript fixes needed:**

```typescript
// Current (wrong):
await session.sendPrompt({ content: "..." });

// Should be:
await session.send({ prompt: "..." });
// or
await session.sendAndWait({ prompt: "..." });
```

**Python fixes needed:**

```python
# Current (wrong):
await session.send_prompt(content="...")

# Should be:
await session.send({"prompt": "..."})
# or
await session.send_and_wait({"prompt": "..."})
```

**Go fixes needed:**

```go
// Current (wrong):
session.SendPrompt(copilot.PromptOptions{Content: "..."})

// Should be:
session.SendAndWait(copilot.MessageOptions{Prompt: "..."}, 0)
```

**C# fixes needed:**

```csharp
// Current (wrong):
await session.SendPromptAsync(new PromptOptions { Content = "..." });

// Should be:
await session.SendAndWaitAsync(new MessageOptions { Prompt = "..." });
```

### 2. Error Handling Doc Has Non-Existent Fields

- **File**: `docs/hooks/error-handling.md`
- **Lines**: ~148-158, 174-185, 200-210
- **Problem**: References `input.errorType` and `input.stack` but actual type uses `input.errorContext` and `input.error` (no stack)
- **Fix**: Update to match actual `ErrorOccurredHookInput` type

```typescript
// Current (wrong):
input.errorType
input.stack

// Should be:
input.errorContext  // "model_call" | "tool_execution" | "system" | "user_input"
input.error         // string error message
```

### 3. User Prompt Submitted Hook Uses Non-Existent Output Fields

- **File**: `docs/hooks/user-prompt-submitted.md`
- **Lines**: ~207-218, 227-238
- **Problem**: Examples use `reject` and `rejectReason` which don't exist in `UserPromptSubmittedHookOutput`
- **Fix**: Update to use actual output fields

```typescript
// Current (wrong):
return {
  reject: true,
  rejectReason: "..."
};

// The actual UserPromptSubmittedHookOutput has:
// - modifiedPrompt?: string
// - additionalContext?: string  
// - suppressOutput?: boolean

// Alternative: throw an error or use a different approach
```

---

## Medium Priority (Nice to Have)

### 1. Broken Link in Getting Started

- **File**: `docs/getting-started.md`
- **Line**: ~1023
- **Problem**: Link to `./mcp.md` should be `./mcp/overview.md`

```markdown
<!-- Current (broken): -->
📖 **[Full MCP documentation →](./mcp.md)**

<!-- Should be: -->
📖 **[Full MCP documentation →](./mcp/overview.md)**
```

### 2. Python Hook Invocation Uses Dict Instead of Object

- **Files**: `docs/hooks/pre-tool-use.md`, `docs/hooks/post-tool-use.md`, `docs/hooks/user-prompt-submitted.md`, `docs/hooks/session-lifecycle.md`, `docs/hooks/error-handling.md`
- **Problem**: Examples show `invocation['session_id']` but Python SDK uses `invocation.session_id` (it's an object, not a dict)
- **Note**: Need to verify actual Python SDK implementation - if it passes dict, this is correct; if object, needs update

### 3. Go SessionConfig Should Be Pointer

- **Files**: Multiple hook docs
- **Problem**: Some examples show `copilot.SessionConfig{}` directly, should be `&copilot.SessionConfig{}`
- **Fix**: Add `&` before struct literals

### 4. .NET Async Pattern Missing CancellationToken

- **Files**: Multiple docs
- **Problem**: .NET async examples don't show `CancellationToken` usage which is best practice
- **Note**: Low priority since methods work without it, but documentation could show the pattern

---

## Low Priority (Future Improvement)

### 1. Add API Reference Documentation

- **Location**: Consider adding `docs/api/` directory
- **Problem**: No auto-generated API reference docs
- **Suggestion**: Add TypeDoc/JSDoc extraction for Node.js, pdoc for Python, godoc for Go, and xmldoc for .NET

### 2. Add Events Reference

- **Location**: Consider `docs/events/` or `docs/api/events.md`
- **Problem**: No comprehensive list of all ~40+ event types with their data structures
- **Suggestion**: Generate from `SessionEvent` type definitions

### 3. Add Cookbook/Recipes Section

- **Location**: `docs/cookbook/` or `docs/recipes/`
- **Problem**: Advanced patterns not well documented
- **Suggestions**:
  - Building a code reviewer agent
  - Multi-turn conversation with context
  - Rate limiting and retry patterns
  - Production deployment checklist

### 4. Document ResumeSessionConfig Fully

- **File**: Consider adding to `docs/guides/session-persistence.md`
- **Problem**: `ResumeSessionConfig` type has many options not fully documented

---

## Missing Documentation

The following SDK features could use dedicated documentation:

- [ ] `onUserInputRequest` handler - add to hooks or guides
- [ ] `skillDirectories` and custom skills - no dedicated docs
- [ ] `availableTools` / `excludedTools` - mentioned but not explained in depth
- [ ] `configDir` option - undocumented
- [ ] Session export (`ExportSessionOptions`) - mentioned in compatibility but no guide
- [ ] `cliArgs` option - useful for advanced CLI flags

---

## Sample Code Fixes Needed

### File: `docs/hooks/overview.md`

**Line ~97-98 - Go example uses wrong NewClient signature:**
```go
// Current (wrong):
client, _ := copilot.NewClient(copilot.ClientOptions{})
session, _ := client.CreateSession(ctx, copilot.SessionConfig{

// Should be:
client := copilot.NewClient(nil)
session, err := client.CreateSession(context.Background(), &copilot.SessionConfig{
```

### File: `docs/debugging.md`

**Line ~50-52 - Go example returns error from NewClient:**
```go
// Current (wrong):
client, err := copilot.NewClient(copilot.ClientOptions{
    LogLevel: "debug",
})

// Should be:
client := copilot.NewClient(&copilot.ClientOptions{
    LogLevel: "debug",
})
```

### File: `docs/guides/session-persistence.md`

**Lines ~67-80 - Go example uses wrong struct and method names:**
```go
// Current (wrong):
client, _ := copilot.NewClient()
session, _ := client.CreateSession(copilot.CreateSessionOptions{
    SessionID: "user-123-task-456",
    Model:     "gpt-5.2-codex",
})
session.SendPrompt(copilot.PromptOptions{Content: "..."})

// Should be:
client := copilot.NewClient(nil)
if err := client.Start(); err != nil { ... }
session, err := client.CreateSession(context.Background(), &copilot.SessionConfig{
    SessionID: "user-123-task-456",
    Model:     "gpt-5.2-codex",
})
session.SendAndWait(copilot.MessageOptions{Prompt: "..."}, 0)
```

---

## Broken Links

| Source File | Line | Broken Link | Suggested Fix |
|-------------|------|-------------|---------------|
| `docs/getting-started.md` | ~1023 | `./mcp.md` | Change to `./mcp/overview.md` |

---

## Consistency Issues

- [ ] Go examples inconsistently use `ctx` vs `context.Background()` - standardize on showing full import
- [ ] Some Go examples show error handling, others use `_` - be consistent about error handling patterns
- [ ] Python examples inconsistently use dict vs TypedDict for config - clarify which is preferred

---

## Validation Notes

The following were verified correct:
- ✅ Node.js/TypeScript examples use correct `createSession`, `sendAndWait`, `defineTool` APIs
- ✅ Python examples correctly use snake_case (`create_session`, `send_and_wait`)
- ✅ .NET examples correctly use PascalCase (`CreateSessionAsync`, `SendAndWaitAsync`)
- ✅ Hook names match SDK types (`onPreToolUse`, `on_pre_tool_use`, `OnPreToolUse`)
- ✅ MCP server config structure is accurate across all languages
- ✅ Provider config for BYOK is accurate
- ✅ All languages show correct client initialization patterns (except Go errors noted above)
