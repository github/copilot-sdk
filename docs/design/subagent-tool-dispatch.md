# Subagent Tool Dispatch: Cross-SDK Design

> Protocol-level specification for resolving child session IDs created by subagents
> and dispatching tool calls, permission requests, hooks, and user-input requests
> back to the parent session that owns the handlers.

## Problem

When a user configures **custom agents** (subagents) on a session, the Copilot CLI
creates a *child session* for each agent invocation. The child session has its own
session ID that is **not** in the SDK's session registry — only parent sessions are
registered by `createSession()`.

All four request types (`tool.call`, `permission.request`, `hooks.invoke`,
`userInput.request`) may arrive with a child session ID. Without resolution logic
the SDK returns "unknown session", breaking the entire subagent feature.

## Request Flow

```
┌──────────┐                  ┌──────────┐                   ┌──────────┐
│  Parent   │  createSession   │   SDK    │   JSON-RPC init   │ Copilot  │
│  App      │ ───────────────▶ │  Client  │ ────────────────▶ │  CLI     │
│           │  (tools, agents) │          │                   │          │
└──────────┘                   └────┬─────┘                   └────┬─────┘
                                    │                              │
           1. session.event ◀───────┼──────────────────────────────┤
              type: subagent.started │                              │
              data.remoteSessionId   │                              │
              data.toolCallId        │                              │
              data.agentName         │                              │
                                    │                              │
           2. SDK maps              │                              │
              child → parent        │                              │
              child → agentName     │                              │
                                    │                              │
           3. tool.call  ◀─────────┼──────────────────────────────┤
              sessionId = CHILD_ID  │                              │
              toolName, arguments   │                              │
                                    │                              │
           4. resolveSession(CHILD) │                              │
              → parent session      │                              │
              + allowlist check     │                              │
                                    │                              │
           5. invoke tool handler   │                              │
              return result ────────┼─────────────────────────────▶│
                                    │                              │
           6. session.event ◀───────┼──────────────────────────────┤
              type: subagent.completed                             │
              data.toolCallId       │                              │
                                    ▼                              ▼
```

Steps 3–5 repeat for `permission.request`, `hooks.invoke`, and `userInput.request`.

## Protocol Contract

### Session Events (parent session's event stream)

All subagent events arrive as `session.event` notifications keyed by the
**parent** session's `sessionEventRequest.sessionId`.

| Event type            | Key `data` fields                                                      | Purpose                        |
|-----------------------|------------------------------------------------------------------------|--------------------------------|
| `subagent.started`    | `remoteSessionId` (child ID), `toolCallId`, `agentName`, `agentDisplayName` | Register child → parent mapping |
| `subagent.completed`  | `toolCallId`, `agentName`                                              | Cleanup instance tracking      |
| `subagent.failed`     | `toolCallId`, `agentName`, `error`                                     | Cleanup instance tracking      |

> **Ordering guarantee:** The CLI emits `subagent.started` before the first
> request that uses the child session ID.

### Request Types Requiring Resolution

| JSON-RPC method       | Params include `sessionId` | Child sessions possible? |
|-----------------------|----------------------------|--------------------------|
| `tool.call`           | ✅                          | ✅                        |
| `permission.request`  | ✅                          | ✅                        |
| `hooks.invoke`        | ✅                          | ✅                        |
| `userInput.request`   | ✅                          | ✅                        |

## Data Model

All SDKs must maintain three maps on the client instance. Names below are
language-agnostic; adapt casing to each language's conventions.

```
childToParent:      Map<childSessionId, parentSessionId>
childToAgent:       Map<childSessionId, agentName>
subagentInstances:  Map<parentSessionId, Map<toolCallId, SubagentInstance>>
```

**SubagentInstance** fields:

| Field            | Type     | Description                               |
|------------------|----------|-------------------------------------------|
| `agentName`      | string   | Custom agent name from `subagent.started`  |
| `toolCallId`     | string   | Unique tool call ID for this launch        |
| `childSessionId` | string   | Child session ID (from `remoteSessionId`)  |
| `startedAt`      | datetime | Timestamp of the `subagent.started` event  |

All maps must be protected by appropriate synchronization primitives
(see [Concurrency Requirements](#concurrency-requirements)).

## Required Behavior

### Session Resolution

Every request handler must resolve the incoming `sessionId` through a single
shared function:

```
resolveSession(sessionId) → (session, isChild, error)
```

Algorithm:

1. **Direct lookup**: if `sessions[sessionId]` exists → return `(session, false, nil)`
2. **Child lookup**: if `childToParent[sessionId]` exists:
   - let `parentId = childToParent[sessionId]`
   - if `sessions[parentId]` exists → return `(parentSession, true, nil)`
   - else → error: `"parent session {parentId} for child {sessionId} not found"`
3. **Unknown** → error: `"unknown session {sessionId}"`

### Handler Dispatch

Each handler follows the same pattern:

```
(session, isChild, err) = resolveSession(params.sessionId)
if err → return error response

// For tool.call ONLY: enforce allowlist
if isChild AND handler == tool.call:
    if not isToolAllowedForChild(params.sessionId, params.toolName):
        return error: "Tool '{toolName}' is not supported by this client instance."

// Dispatch to the resolved session's handler
return session.handle(params)
```

## Allowlist Enforcement

The `CustomAgentConfig.tools` field controls which parent tools a subagent can
invoke.

| `tools` value          | Meaning                                  |
|------------------------|------------------------------------------|
| `null` / `nil` / `None` / not set | All parent tools accessible     |
| `[]` (empty list)      | No tools accessible                      |
| `["a", "b"]`           | Only tools `a` and `b` accessible        |

**Rules:**

- Allowlist check applies to both `tool.call` RPC requests (Protocol v2)
  and `external_tool.requested` broadcast events (Protocol v3). It does
  **not** apply to `permission.request`, `hooks.invoke`, or
  `userInput.request`.
- A denied tool returns `"Tool '{name}' is not supported by this client instance."`
  — never `"unknown session"`.
- The check algorithm:
  1. Look up `agentName = childToAgent[childSessionId]`
  2. Look up `CustomAgentConfig` for `agentName` on the parent session
  3. If `tools` is null/unset → **allow**
  4. If `toolName` is in `tools` list → **allow**
  5. Otherwise → **deny**

## Tool Advertisement

### Problem

Child sessions created by the CLI for subagents do not automatically inherit
parent custom tool definitions. The child session's LLM only sees built-in
tools — it has no knowledge of any custom tools the parent session registered
unless their definitions are explicitly forwarded.

### SDK Mechanism

The SDK auto-populates `toolDefinitions` on each `CustomAgentConfig` in the
`session.create` and `session.resume` requests. This field contains the full
tool definitions (name, description, parameters) for every tool listed in the
agent's `Tools` allowlist.

When `Tools` is `nil` / `null` / unset (meaning "all tools"), the SDK does
**not** populate `toolDefinitions` — enumerating all tools is unnecessary
because the CLI already has the full tool list from the session-level `Tools`
array.

### Wire Format

```json
{
  "customAgents": [{
    "name": "reviewer",
    "tools": ["save_result"],
    "toolDefinitions": [
      {
        "name": "save_result",
        "description": "Saves a result string",
        "parameters": {
          "type": "object",
          "properties": {
            "content": { "type": "string", "description": "The result to save" }
          },
          "required": ["content"]
        }
      }
    ]
  }]
}
```

### CLI Dependency

The CLI must read and propagate `toolDefinitions` to child sessions for custom
tools to be visible to subagent LLMs. If the CLI does not support this field,
custom tools will **not** be available to subagents — the child LLM will not
know they exist and will never attempt to call them. SDK-side allowlist
enforcement alone is not sufficient; tool advertisement is the complementary
mechanism that makes custom tools discoverable.

## Protocol v3 Allowlist Enforcement

In Protocol v3, tool calls from child sessions arrive as
`external_tool.requested` broadcast events on the **parent** session's event
stream, rather than as direct JSON-RPC requests to the client.

### Client-Level Interception

The client intercepts these events in `handleSessionEvent()` and enforces the
tool allowlist **before** dispatching to the session's tool handler:

1. Extract the child session ID and tool name from the broadcast event.
2. Resolve the child session to its parent using `childToParent`.
3. Look up the agent's `Tools` allowlist via `childToAgent` → agent config.
4. If the tool is **denied**, respond with a failure via
   `session.tools.handlePendingToolCall` RPC — the tool handler is never
   invoked.
5. If the tool is **allowed**, forward the event to the resolved parent
   session for normal tool dispatch.

### Why Client-Level?

This enforcement is done at the **client** level (not session level) because
the session object does not have access to the child-to-parent mapping or the
per-agent allowlist configuration. Only the client maintains the
`childToParent`, `childToAgent`, and agent config data structures needed to
make the allow/deny decision.

## Cleanup Contract

| Trigger                           | `childToParent` | `childToAgent` | `subagentInstances`  |
|-----------------------------------|-----------------|----------------|----------------------|
| Client `stop()` / shutdown        | Clear all       | Clear all      | Clear all            |
| Delete single session (parent)    | Remove children of that parent | Remove children of that parent | Remove parent entry |
| Destroy session (parent)          | Remove children of that parent | Remove children of that parent | Remove parent entry, fire cleanup callback |
| `subagent.completed` / `failed`   | **Preserve**    | **Preserve**   | Remove instance only |

> **Why preserve on subagent end?** Requests may still be in-flight after the
> `completed`/`failed` event. Keeping `childToParent` and `childToAgent` ensures
> those late-arriving requests resolve correctly.

## Error Types

| Error message                                                    | Condition                                                    |
|------------------------------------------------------------------|--------------------------------------------------------------|
| `unknown session {id}`                                           | `sessionId` not found as direct session or child mapping     |
| `parent session {parentId} for child {childId} not found`        | Child mapping exists but parent session was deleted/destroyed |
| `Tool '{name}' is not supported by this client instance.`        | Tool not in agent's allowlist, or tool not registered        |

## Concurrency Requirements

- All map access (`childToParent`, `childToAgent`, `subagentInstances`) must be
  synchronized.
- The lock must **not** be held during handler execution (tool handler calls,
  permission callbacks, hook invocations). This prevents deadlocks when a handler
  triggers further session operations.
- Lock is acquired only for map reads/writes, then released before callback
  dispatch.

```
lock()
(session, isChild, err) = read maps
unlock()

// handler runs WITHOUT lock
result = session.handle(params)
```

## Language-Specific Notes

### Node SDK (`nodejs/src/client.ts`)

**New client properties:**

```typescript
private childToParent: Map<string, string> = new Map();
private childToAgent: Map<string, string> = new Map();
private subagentInstances: Map<string, Map<string, SubagentInstance>> = new Map();
```

**Integration points:**

1. **Event interception**: In the session event handler, intercept
   `subagent.started`, `subagent.completed`, and `subagent.failed` events to
   populate/clean up the maps.
2. **`handleToolCallRequest`**: Replace direct `this.sessions.get(sessionId)`
   with `this.resolveSession(sessionId)`. Add allowlist check when `isChild`.
3. **`handlePermissionRequest`**, **`handleUserInputRequest`**,
   **`handleHooksInvoke`**: Replace direct session lookup with
   `this.resolveSession(sessionId)`.

**Concurrency**: Node.js is single-threaded (event loop), so no mutex is needed.
Standard `Map` operations are safe.

### Python SDK (`python/copilot/client.py`)

**New client properties:**

```python
self._child_to_parent: dict[str, str] = {}
self._child_to_agent: dict[str, str] = {}
self._subagent_instances: dict[str, dict[str, SubagentInstance]] = {}
```

**Integration points:**

1. **Event interception**: In the session event callback, intercept
   `subagent.started` / `completed` / `failed` to manage the maps.
2. **`_handle_tool_call_request`**: Replace direct `self._sessions.get()`
   with `self._resolve_session()`. Add allowlist check when `is_child`.
3. **`_handle_permission_request`**, **`_handle_user_input_request`**,
   **`_handle_hooks_invoke`**: Replace direct session lookup with
   `self._resolve_session()`.

**Concurrency**: Python asyncio is single-threaded within an event loop. If the
client is used from multiple threads, protect the maps with `self._sessions_lock`
(already exists on the Python client). Under pure asyncio, no extra lock is
needed.

### Go SDK (`go/client.go`)

The Go SDK already implements this feature. The Go implementation is the
**reference implementation** for this design. Key structures:

- `childToParent`, `childToAgent`, `subagentInstances` maps on `Client`
- `resolveSession()` method with direct → child fallback
- `isToolAllowedForChild()` for allowlist enforcement
- `sync.RWMutex` (`mu`) protects all map access

### .NET SDK (`dotnet/src/`)

Follow the same pattern. Use `ConcurrentDictionary<string, string>` for
`childToParent` and `childToAgent`, or protect with a `ReaderWriterLockSlim`.
The `SubagentInstance` can be a record or class.

## Checklist for SDK Implementers

- [ ] Add `childToParent`, `childToAgent`, `subagentInstances` maps to client
- [ ] Intercept `subagent.started` event → populate maps
- [ ] Intercept `subagent.completed` / `subagent.failed` → cleanup instances
- [ ] Implement `resolveSession()` with direct → child fallback
- [ ] Update `tool.call` handler to use `resolveSession()` + allowlist check
- [ ] Update `permission.request` handler to use `resolveSession()`
- [ ] Update `hooks.invoke` handler to use `resolveSession()`
- [ ] Update `userInput.request` handler to use `resolveSession()`
- [ ] Cleanup maps on client stop, session delete, and session destroy
- [ ] Verify concurrency safety for the language's execution model
- [ ] Add tests: child tool dispatch, allowlist deny, unknown session error
