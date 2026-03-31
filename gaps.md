# SDK Feature Gaps: Node.js → Python, Go, .NET

These features were added to the **Node.js SDK** in commits `4088739` and `4d26e30` but have **no SDK-level wrapper** in the other languages yet. All three target SDKs already have the **generated RPC and event types** — what's missing is the developer-facing API that wires those up.

---

## Gap 1: Commands

**Source commit:** `4088739` — *[Node] Add Commands and UI Elicitation Support to SDK (#906)*

Lets apps register slash-commands (e.g. `/deploy`) that users can invoke from the CLI TUI. The SDK dispatches `command.execute` events to a user-provided handler and auto-responds via the RPC layer.

### What Node.js ships

| Layer | What | Location |
|-------|------|----------|
| **Types** | `CommandDefinition` (name, description, handler) | `nodejs/src/types.ts` |
| | `CommandContext` (sessionId, command, commandName, args) | `nodejs/src/types.ts` |
| | `CommandHandler` callback type | `nodejs/src/types.ts` |
| **Config** | `SessionConfig.commands?: CommandDefinition[]` | `nodejs/src/types.ts` |
| **Session** | `registerCommands()` — stores handlers on session | `nodejs/src/session.ts` |
| | `_executeCommandAndRespond()` — dispatches to handler, calls `commands.handlePendingCommand` RPC | `nodejs/src/session.ts` |
| **Client** | Serializes `commands` (name + description only) in create/resume wire payload | `nodejs/src/client.ts` |
| **Events** | Routes `command.execute` events → `_executeCommandAndRespond()` | `nodejs/src/session.ts` |

### What each target SDK needs

- **Types/config**: Equivalent of `CommandDefinition`, `CommandContext`, `CommandHandler` and a config option to pass commands at session creation.
- **Session method**: Register command handlers, listen for `command.execute` events, invoke the handler, then call the existing generated `commands.handlePendingCommand` RPC method (already generated in all three SDKs).
- **Client wiring**: Serialize `commands` array (name + description) into create/resume payloads.
- **Tests**: Unit test for handler dispatch + E2E test using the test harness (snapshot `test/snapshots/` may need a new YAML).
- **README**: Document the feature with an example.

### Existing generated infrastructure (ready to use)

| SDK | Generated RPC method | Generated event types |
|-----|---------------------|-----------------------|
| Python | `CommandsApi.handle_pending_command()` | `COMMAND_EXECUTE`, `COMMAND_QUEUED`, etc. |
| Go | `CommandsApi.HandlePendingCommand()` | `SessionEventTypeCommandExecute`, etc. |
| .NET | `CommandsApi.HandlePendingCommandAsync()` | `CommandExecuteEvent`, `CommandExecuteData` |

---

## Gap 2: UI Elicitation (client → server)

**Source commit:** `4088739` — *[Node] Add Commands and UI Elicitation Support to SDK (#906)*

Provides a `session.ui` object with convenience methods that let SDK code **ask the user questions** (confirm, select, text input, or a full custom form). Gated by `session.capabilities.ui.elicitation`.

### What Node.js ships

| Layer | What | Location |
|-------|------|----------|
| **Types** | `ElicitationSchema`, `ElicitationSchemaField` (union of field variants) | `nodejs/src/types.ts` |
| | `ElicitationParams` (message + requestedSchema) | `nodejs/src/types.ts` |
| | `ElicitationResult` (action: accept/decline/cancel, content) | `nodejs/src/types.ts` |
| | `ElicitationFieldValue` (string \| number \| boolean \| string[]) | `nodejs/src/types.ts` |
| | `InputOptions` (title, description, minLength, maxLength, format, default) | `nodejs/src/types.ts` |
| | `SessionUiApi` interface | `nodejs/src/types.ts` |
| **Session** | `get ui()` → `SessionUiApi` with `elicitation()`, `confirm()`, `select()`, `input()` | `nodejs/src/session.ts` |
| | `assertElicitation()` — throws if capability absent | `nodejs/src/session.ts` |
| **Capabilities** | `session.capabilities.ui?.elicitation` boolean | `nodejs/src/session.ts`, `nodejs/src/client.ts` |

### Convenience method behavior

| Method | Sends to server | Returns |
|--------|----------------|---------|
| `confirm(message)` | Boolean schema field | `true` / `false` |
| `select(message, options)` | Enum string field | Selected string or `null` |
| `input(message, options?)` | String field with optional constraints | String value or `null` |
| `elicitation(params)` | Full custom schema | `ElicitationResult` |

### What each target SDK needs

- **Types**: All the schema/param/result types above (language-idiomatic naming).
- **Session property/methods**: A `ui` accessor (or equivalent) with `confirm`, `select`, `input`, `elicitation` methods that call the existing generated `ui.elicitation` RPC method.
- **Capability gating**: Check `session.capabilities.ui.elicitation` before calling; throw/error if unsupported.
- **Tests & docs**: Unit tests for each convenience method + README examples.

### Existing generated infrastructure (ready to use)

| SDK | Generated RPC method |
|-----|---------------------|
| Python | `UiApi.elicitation()` |
| Go | `UiApi.Elicitation()` |
| .NET | `UiApi.ElicitationAsync()` |

---

## Gap 3: onElicitationRequest (server → client callback)

**Source commit:** `4d26e30` — *[Node] Add onElicitationRequest Callback for Elicitation Provider Support (#908)*

The inverse of Gap 2. When the **server** (or an MCP tool) needs to ask the end-user a question, it sends an `elicitation.requested` event to the SDK client. The SDK dispatches it to a user-provided handler and responds via `ui.handlePendingElicitation`.

### What Node.js ships

| Layer | What | Location |
|-------|------|----------|
| **Types** | `ElicitationRequest` (message, requestedSchema?, mode?, elicitationSource?, url?) | `nodejs/src/types.ts` |
| | `ElicitationHandler` callback type (request, invocation) → ElicitationResult | `nodejs/src/types.ts` |
| **Config** | `SessionConfig.onElicitationRequest?: ElicitationHandler` | `nodejs/src/types.ts` |
| **Session** | `registerElicitationHandler(handler)` — stores handler | `nodejs/src/session.ts` |
| | `_handleElicitationRequest()` — dispatches to handler, calls `ui.handlePendingElicitation` RPC, auto-cancels on error | `nodejs/src/session.ts` |
| **Client** | Sends `requestElicitation: true` in create/resume payload when handler is provided | `nodejs/src/client.ts` |
| **Events** | Routes `elicitation.requested` events → `_handleElicitationRequest()` | `nodejs/src/session.ts` |

### Error handling contract

If the user-provided handler throws, the SDK automatically responds with `{ action: "cancel" }` so the server doesn't hang.

### What each target SDK needs

- **Types/config**: `ElicitationRequest`, `ElicitationHandler`, and a config option (`on_elicitation_request` / `OnElicitationRequest`).
- **Session method**: Register the handler, listen for `elicitation.requested` events, dispatch to handler, respond via the existing generated `ui.handlePendingElicitation` RPC.
- **Client wiring**: Send `requestElicitation: true` in create/resume payloads when handler is provided.
- **Error handling**: Catch handler errors and auto-cancel.
- **Tests & docs**: Unit + E2E tests, README section.

### Existing generated infrastructure (ready to use)

| SDK | Generated RPC method | Generated event type |
|-----|---------------------|---------------------|
| Python | `UiApi.handle_pending_elicitation()` | `ELICITATION_REQUESTED` |
| Go | `UiApi.HandlePendingElicitation()` | `SessionEventTypeElicitationRequested` |
| .NET | `UiApi.HandlePendingElicitationAsync()` | `ElicitationRequestedEvent` |

---

## Summary Matrix

| Feature | Node.js | Python | Go | .NET |
|---------|---------|--------|----|------|
| **Commands** | ✅ Full | ❌ Generated types only | ❌ Generated types only | ❌ Generated types only |
| **UI Elicitation** (client→server) | ✅ Full | ❌ Generated types only | ❌ Generated types only | ❌ Generated types only |
| **onElicitationRequest** (server→client) | ✅ Full | ❌ Generated types only | ❌ Generated types only | ❌ Generated types only |

All three gaps follow the same pattern: the **wire-level plumbing already exists** (generated RPC methods + event types). What's missing is the **SDK-level developer API** — types, config options, session methods, event routing, error handling, tests, and docs.
