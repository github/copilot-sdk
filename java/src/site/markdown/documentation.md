# GitHub Copilot SDK for Java - Documentation

This guide covers common use cases for the GitHub Copilot SDK for Java. For complete API details, see the [Javadoc](apidocs/index.html).

## Table of Contents

- [Basic Usage](#Basic_Usage)
- [Handling Responses](#Handling_Responses)
- [Troubleshooting Event Handling](#Troubleshooting_Event_Handling)
- [Event Types Reference](#Event_Types_Reference)
- [Streaming Responses](#Streaming_Responses)
- [Session Operations](#Session_Operations)
- [Choosing a Model](#Choosing_a_Model)
- [Reasoning Effort](#Reasoning_Effort)
- [Tool Filtering](#Tool_Filtering)
- [Working Directory](#Working_Directory)
- [Connection State & Diagnostics](#Connection_State__Diagnostics)
- [Message Delivery Mode](#Message_Delivery_Mode)
- [Session Management](#Session_Management)
- [SessionConfig Reference](#SessionConfig_Reference)
    - [Cloning SessionConfig](#Cloning_SessionConfig)

---

## Basic Usage

Create a client, start a session, and send a message:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

try (var client = new CopilotClient()) {
    client.start().get();

    var session = client.createSession(
        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-4.1")
    ).get();

    var response = session.sendAndWait("Explain Java records in one sentence").get();
    System.out.println(response.getData().content());

    session.close();
}
```

The client manages the connection to the Copilot CLI. Sessions are independent conversations that can run concurrently.

---

## Handling Responses

### Simple Request-Response

For straightforward interactions, use `sendAndWait()`:

```java
var response = session.sendAndWait("What is the capital of France?").get();
System.out.println(response.getData().content());
```

### Event-Based Processing

For more control, subscribe to events and use `send()`:

> **Exception isolation:** If a handler throws an exception, the SDK logs the
> error. By default, dispatch stops after the first handler error
> (`PROPAGATE_AND_LOG_ERRORS`). To continue dispatching to remaining handlers,
> set `EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS`. You can customize error
> handling with `session.setEventErrorHandler()` — see the
> [Advanced Usage](advanced.html#Custom_Event_Error_Handler) guide.

## Troubleshooting Event Handling

### Symptoms of policy misconfiguration

- You registered multiple `session.on(...)` handlers, but only the first one runs
- A handler throws once and later handlers stop receiving events
- You expected "best effort" fan-out, but dispatch halts on errors

### Fix

Set the event error policy to suppress-and-continue:

```java
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
```

Optionally add a custom error handler for observability:

```java
session.setEventErrorHandler((event, exception) -> {
    System.err.println("Handler failed for event " + event.getType() + ": " + exception.getMessage());
});
```

Use `PROPAGATE_AND_LOG_ERRORS` when you want fail-fast behavior.

```java
var done = new CompletableFuture<Void>();

// Type-safe event handlers (recommended)
session.on(AssistantMessageEvent.class, msg -> {
    System.out.println("Response: " + msg.getData().content());
});

session.on(SessionErrorEvent.class, err -> {
    System.err.println("Error: " + err.getData().message());
});

session.on(SessionIdleEvent.class, idle -> {
    done.complete(null);
});

session.send("Tell me a joke").get();
done.get(); // Wait for completion
```

You can also use a single handler for all events:

```java
session.on(event -> {
    switch (event) {
        case AssistantMessageEvent msg -> 
            System.out.println("Response: " + msg.getData().content());
        case SessionErrorEvent err -> 
            System.err.println("Error: " + err.getData().message());
        case SessionIdleEvent idle -> 
            done.complete(null);
        default -> { }
    }
});
```

### Key Event Types

| Event | Description |
|-------|-------------|
| `AssistantMessageEvent` | Complete assistant response |
| `AssistantMessageDeltaEvent` | Streaming chunk (when streaming enabled) |
| `SessionIdleEvent` | Session finished processing |
| `SessionErrorEvent` | An error occurred |

For the complete list of all event types, see [Event Types Reference](#Event_Types_Reference) below.

---

## Event Types Reference

The SDK supports event types organized by category. All events extend `SessionEvent`.

### Session Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `SessionStartEvent` | `session.start` | Session has started |
| `SessionResumeEvent` | `session.resume` | Session was resumed |
| `SessionIdleEvent` | `session.idle` | Session finished processing, ready for input |
| `SessionErrorEvent` | `session.error` | An error occurred in the session |
| `SessionInfoEvent` | `session.info` | Informational message from the session |
| `SessionShutdownEvent` | `session.shutdown` | Session is shutting down (includes reason and exit code) |
| `SessionModelChangeEvent` | `session.model_change` | The model was changed mid-session |
| `SessionModeChangedEvent` | `session.mode_changed` | Session mode changed (e.g., plan mode) |
| `SessionPlanChangedEvent` | `session.plan_changed` | Session plan was updated |
| `SessionWorkspaceFileChangedEvent` | `session.workspace_file_changed` | A file in the workspace was modified |
| `SessionHandoffEvent` | `session.handoff` | Session handed off to another agent |
| `SessionTruncationEvent` | `session.truncation` | Context was truncated due to limits |
| `SessionSnapshotRewindEvent` | `session.snapshot_rewind` | Session rewound to a previous snapshot |
| `SessionUsageInfoEvent` | `session.usage_info` | Token usage information |
| `SessionCompactionStartEvent` | `session.compaction_start` | Context compaction started (infinite sessions) |
| `SessionCompactionCompleteEvent` | `session.compaction_complete` | Context compaction completed |
| `SessionContextChangedEvent` | `session.context_changed` | Working directory context changed |
| `SessionTaskCompleteEvent` | `session.task_complete` | Task completed with summary |

### Assistant Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `AssistantTurnStartEvent` | `assistant.turn_start` | Assistant began processing |
| `AssistantIntentEvent` | `assistant.intent` | Assistant's detected intent |
| `AssistantReasoningEvent` | `assistant.reasoning` | Full reasoning content (reasoning models) |
| `AssistantReasoningDeltaEvent` | `assistant.reasoning_delta` | Streaming reasoning chunk |
| `AssistantMessageEvent` | `assistant.message` | Complete assistant message |
| `AssistantMessageDeltaEvent` | `assistant.message_delta` | Streaming message chunk |
| `AssistantStreamingDeltaEvent` | `assistant.streaming_delta` | Streaming progress with size metrics |
| `AssistantTurnEndEvent` | `assistant.turn_end` | Assistant finished processing |
| `AssistantUsageEvent` | `assistant.usage` | Token usage for this turn |

### Tool Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `ToolUserRequestedEvent` | `tool.user_requested` | User requested a tool invocation |
| `ToolExecutionStartEvent` | `tool.execution_start` | Tool execution started |
| `ToolExecutionProgressEvent` | `tool.execution_progress` | Tool execution progress update |
| `ToolExecutionPartialResultEvent` | `tool.execution_partial_result` | Partial result from tool |
| `ToolExecutionCompleteEvent` | `tool.execution_complete` | Tool execution completed |

### User Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `UserMessageEvent` | `user.message` | User sent a message |
| `PendingMessagesModifiedEvent` | `pending_messages.modified` | Pending message queue changed |

### Subagent Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `SubagentStartedEvent` | `subagent.started` | Subagent was spawned |
| `SubagentSelectedEvent` | `subagent.selected` | Subagent was selected for task |
| `SubagentDeselectedEvent` | `subagent.deselected` | Subagent was deselected |
| `SubagentCompletedEvent` | `subagent.completed` | Subagent completed its task |
| `SubagentFailedEvent` | `subagent.failed` | Subagent failed |

### Other Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `AbortEvent` | `abort` | Operation was aborted |
| `HookStartEvent` | `hook.start` | Hook execution started |
| `HookEndEvent` | `hook.end` | Hook execution completed |
| `SystemMessageEvent` | `system.message` | System-level message |
| `SystemNotificationEvent` | `system.notification` | System notification (informational) |
| `SkillInvokedEvent` | `skill.invoked` | A skill was invoked |

### External Tool Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `ExternalToolRequestedEvent` | `external_tool.requested` | An external tool invocation was requested |
| `ExternalToolCompletedEvent` | `external_tool.completed` | An external tool invocation completed |

### Permission Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `PermissionRequestedEvent` | `permission.requested` | A permission request was issued |
| `PermissionCompletedEvent` | `permission.completed` | A permission request was resolved |

### Command Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `CommandQueuedEvent` | `command.queued` | A command was queued for execution |
| `CommandCompletedEvent` | `command.completed` | A queued command completed |
| `CommandExecuteEvent` | `command.execute` | A registered slash command was dispatched for execution |

### Elicitation Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `ElicitationRequestedEvent` | `elicitation.requested` | An elicitation (UI dialog) request was received |

### Capability Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `CapabilitiesChangedEvent` | `capabilities.changed` | Session capabilities were updated |

### Plan Mode Events

| Event | Type String | Description |
|-------|-------------|-------------|
| `ExitPlanModeRequestedEvent` | `exit_plan_mode.requested` | Exit from plan mode was requested |
| `ExitPlanModeCompletedEvent` | `exit_plan_mode.completed` | Exit from plan mode completed |

See the [generated package Javadoc](apidocs/com/github/copilot/sdk/generated/package-summary.html) for detailed event data structures.

---

## Streaming Responses

Enable streaming to receive response chunks as they're generated:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setModel("gpt-4.1")
        .setStreaming(true)
).get();

var done = new CompletableFuture<Void>();

session.on(AssistantMessageDeltaEvent.class, delta -> {
    // Print each chunk as it arrives
    System.out.print(delta.getData().deltaContent());
});

session.on(SessionIdleEvent.class, idle -> {
    System.out.println(); // Newline at end
    done.complete(null);
});

session.send("Write a haiku about Java").get();
done.get();
```

---

## Session Operations

### Get Conversation History

Retrieve all messages and events from a session using `getMessages()`:

```java
var history = session.getMessages().get();

for (var event : history) {
    switch (event) {
        case UserMessageEvent user -> 
            System.out.println("User: " + user.getData().content());
        case AssistantMessageEvent assistant -> 
            System.out.println("Assistant: " + assistant.getData().content());
        case ToolExecutionCompleteEvent tool -> 
            System.out.println("Tool: " + tool.getData().toolCallId());
        default -> { }
    }
}
```

This is useful for:
- Displaying conversation history in a UI
- Persisting conversations for later review
- Debugging and logging session state

### Abort Current Operation

Cancel a long-running operation using `abort()`:

```java
// Start a potentially long operation
var messageFuture = session.send("Analyze this large codebase...");

// User clicks cancel button
session.abort().get();

// The session will emit an AbortEvent
session.on(AbortEvent.class, evt -> {
    System.out.println("Operation was cancelled");
});
```

Use cases:
- User-initiated cancellation in interactive applications
- Timeout handling in automated pipelines
- Graceful shutdown when application is closing

### Custom Timeout

Use `sendAndWait` with a custom timeout for CI/CD pipelines:

```java
try {
    // 30-second timeout
    var response = session.sendAndWait(
        new MessageOptions().setPrompt("Quick question"),
        30000  // timeout in milliseconds
    ).get();
} catch (ExecutionException e) {
    if (e.getCause() instanceof TimeoutException) {
        System.err.println("Request timed out");
        session.abort().get();
    }
}
```

---

## Choosing a Model

### List Available Models

Query available models before creating a session:

```java
var models = client.listModels().get();

for (var model : models) {
    System.out.printf("%s (%s)%n", model.getId(), model.getName());
}
```

### Use a Specific Model

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("claude-sonnet-4")
).get();
```

---

## Reasoning Effort

For models that support it, control how much effort the model spends reasoning before responding:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setModel("o3-mini")
        .setReasoningEffort("high")
).get();
```

| Level | Description |
|-------|-------------|
| `"low"` | Fastest responses, less detailed reasoning |
| `"medium"` | Balanced speed and reasoning depth |
| `"high"` | Thorough reasoning, slower responses |
| `"xhigh"` | Maximum reasoning effort |

> **Note:** Only applies to reasoning models (e.g., `o3-mini`). Non-reasoning models ignore this setting.

---

## Tool Filtering

Control which built-in tools the assistant can use with allowlists and blocklists.

### Allowlist (Available Tools)

Restrict the session to only specific tools:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setAvailableTools(List.of("read_file", "search_code", "list_dir"))
).get();
```

When `availableTools` is set, the assistant can **only** use tools in this list.

### Blocklist (Excluded Tools)

Allow all tools except specific ones:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setExcludedTools(List.of("execute_command", "write_file"))
).get();
```

Tools in the `excludedTools` list will not be available to the assistant.

### Combining with Custom Tools

Tool filtering applies to built-in tools. Your custom tools (registered via `setTools()`) are always available:

```java
var lookupTool = ToolDefinition.create("lookup_issue", "Fetch issue", schema, handler);

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setTools(List.of(lookupTool))           // Custom tool always available
        .setAvailableTools(List.of("read_file")) // Only allow read_file from built-ins
).get();
```

---

## Working Directory

Set the working directory for file operations in the session:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setWorkingDirectory("/path/to/project")
).get();
```

This affects how the assistant resolves relative file paths when using tools like `read_file`, `write_file`, and `search_code`.

---

## Connection State & Diagnostics

### Checking Connection State

Query the client's connection state at any time without making a server call:

```java
ConnectionState state = client.getState();
switch (state) {
    case CONNECTED -> System.out.println("Ready");
    case CONNECTING -> System.out.println("Starting up...");
    case DISCONNECTED -> System.out.println("Not connected");
    case ERROR -> System.out.println("Connection failed");
}
```

The four states are:

| State | Description |
|-------|-------------|
| `DISCONNECTED` | Client has not been started yet |
| `CONNECTING` | `start()` was called but hasn't completed |
| `CONNECTED` | Client is connected and ready |
| `ERROR` | Connection failed (check logs for details) |

### State Transitions

```
                 start()
  DISCONNECTED ──────────► CONNECTING
                               │
                    ┌──────────┼──────────┐
                    │ success  │          │ failure
                    ▼          │          ▼
              CONNECTED        │        ERROR
                    │          │
        stop() /    │          │
        forceStop() │          │
                    ▼          │
              DISCONNECTED ◄───┘
                    │
                    │ start()  (retry)
                    ▼
               CONNECTING
```

- **DISCONNECTED → CONNECTING:** `start()` was called
- **CONNECTING → CONNECTED:** Server connection and protocol handshake succeeded
- **CONNECTING → ERROR:** Connection or protocol verification failed
- **CONNECTED → DISCONNECTED:** `stop()` or `forceStop()` was called, or `close()` via try-with-resources
- **DISCONNECTED → CONNECTING:** `start()` can be called again to retry

### Server Status

Get CLI version and protocol information:

```java
var status = client.getStatus().get();
System.out.println("CLI version: " + status.getVersion());
System.out.println("Protocol version: " + status.getProtocolVersion());
```

### Authentication Status

Check whether the current connection is authenticated and how:

```java
var auth = client.getAuthStatus().get();
if (auth.isAuthenticated()) {
    System.out.println("Logged in as: " + auth.getLogin());
    System.out.println("Auth type: " + auth.getAuthType());
    System.out.println("Host: " + auth.getHost());
} else {
    System.out.println("Not authenticated: " + auth.getStatusMessage());
}
```

### Ping

Verify server connectivity:

```java
var pong = client.ping("hello").get();
System.out.println("Server responded, protocol version: " + pong.protocolVersion());
```

See [ConnectionState](apidocs/com/github/copilot/sdk/ConnectionState.html), [GetStatusResponse](apidocs/com/github/copilot/sdk/json/GetStatusResponse.html), and [GetAuthStatusResponse](apidocs/com/github/copilot/sdk/json/GetAuthStatusResponse.html) Javadoc for details.

---

## Message Delivery Mode

Control how messages are delivered to the session:

```java
// Default: message is enqueued for processing
session.send(new MessageOptions()
    .setPrompt("Analyze this codebase")
).get();

// Immediate: process the message right away
session.send(new MessageOptions()
    .setPrompt("Quick question")
    .setMode("immediate")
).get();
```

| Mode | Description |
|------|-------------|
| `"enqueue"` | Queue the message for processing (default) |
| `"immediate"` | Process the message immediately |

---

## Session Management

### Multiple Concurrent Sessions

```java
var session1 = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-4.1")
).get();

var session2 = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("claude-sonnet-4")
).get();

// Send messages concurrently
var future1 = session1.sendAndWait("Summarize REST APIs");
var future2 = session2.sendAndWait("Summarize GraphQL");

System.out.println("GPT: " + future1.get().getData().content());
System.out.println("Claude: " + future2.get().getData().content());
```

### Resume a Previous Session

```java
// Get the last session ID
var lastSessionId = client.getLastSessionId().get();

// Or list all sessions
var sessions = client.listSessions().get();

// Resume a session
var session = client.resumeSession(lastSessionId, new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
```

### Resume Options

When resuming a session, you can optionally reconfigure many settings. This is useful when you need to change the model, update tool configurations, or modify behavior.

| Option | Description |
|--------|-------------|
| `model` | Change the model for the resumed session |
| `systemMessage` | Override or extend the system prompt |
| `availableTools` | Restrict which tools are available |
| `excludedTools` | Disable specific tools |
| `provider` | Re-provide BYOK credentials (required for BYOK sessions) |
| `reasoningEffort` | Adjust reasoning effort level |
| `streaming` | Enable/disable streaming responses |
| `workingDirectory` | Change the working directory |
| `configDir` | Override configuration directory |
| `mcpServers` | Configure MCP servers |
| `customAgents` | Configure custom agents |
| `agent` | Pre-select a custom agent at session start |
| `skillDirectories` | Directories to load skills from |
| `disabledSkills` | Skills to disable |
| `infiniteSessions` | Configure infinite session behavior |
| `commands` | Slash command definitions for the resumed session |
| `onElicitationRequest` | Handler for incoming elicitation requests |
| `disableResume` | When `true`, resumes without emitting a `session.resume` event |
| `onEvent` | Event handler registered before session resumption |

**Example: Changing Model on Resume**

```java
// Resume with a different model
var config = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setModel("claude-sonnet-4")    // Switch to a different model
    .setReasoningEffort("high");    // Increase reasoning effort

var session = client.resumeSession("user-123-task-456", config).get();
```

See [ResumeSessionConfig](apidocs/com/github/copilot/sdk/json/ResumeSessionConfig.html) Javadoc for complete options.

### Clean Up Sessions

Closing a session releases its in-memory resources but **preserves session data on disk**, so
it can be resumed later. Use `deleteSession()` to permanently remove session data from disk:

```java
// Close a session (releases in-memory resources; session can be resumed later)
session.close();

// Permanently delete a session and all its data from disk (cannot be resumed)
client.deleteSession(sessionId).get();
```

---

## SessionConfig Reference

Complete list of all `SessionConfig` options for `createSession()`:

| Option | Type | Description | Guide |
|--------|------|-------------|-------|
| `sessionId` | String | Custom session ID (auto-generated if omitted) | — |
| `clientName` | String | Client name for User-Agent identification | — |
| `model` | String | AI model to use | [Choosing a Model](#Choosing_a_Model) |
| `reasoningEffort` | String | Reasoning depth: `"low"`, `"medium"`, `"high"`, `"xhigh"` | [Reasoning Effort](#Reasoning_Effort) |
| `tools` | List&lt;ToolDefinition&gt; | Custom tools the assistant can invoke | [Custom Tools](advanced.html#Custom_Tools) |
| `systemMessage` | SystemMessageConfig | Customize assistant behavior | [System Messages](advanced.html#System_Messages) |
| `availableTools` | List&lt;String&gt; | Allowlist of built-in tool names | [Tool Filtering](#Tool_Filtering) |
| `excludedTools` | List&lt;String&gt; | Blocklist of built-in tool names | [Tool Filtering](#Tool_Filtering) |
| `provider` | ProviderConfig | BYOK API provider configuration | [BYOK](advanced.html#Bring_Your_Own_Key_BYOK) |
| `onPermissionRequest` | PermissionHandler | Handler for permission requests | [Permission Handling](advanced.html#Permission_Handling) |
| `onUserInputRequest` | UserInputHandler | Handler for user input requests | [User Input Handling](advanced.html#User_Input_Handling) |
| `hooks` | SessionHooks | Lifecycle and tool execution hooks | [Session Hooks](hooks.html) |
| `workingDirectory` | String | Working directory for file operations | [Working Directory](#Working_Directory) |
| `streaming` | boolean | Enable streaming response chunks | [Streaming Responses](#Streaming_Responses) |
| `mcpServers` | Map&lt;String, Object&gt; | MCP server configurations | [MCP Servers](mcp.html) |
| `customAgents` | List&lt;CustomAgentConfig&gt; | Custom agent definitions | [Custom Agents](advanced.html#Custom_Agents) |
| `agent` | String | Pre-select a custom agent at session start | [Custom Agents](advanced.html#Custom_Agents) |
| `infiniteSessions` | InfiniteSessionConfig | Auto-compaction for long conversations | [Infinite Sessions](advanced.html#Infinite_Sessions) |
| `skillDirectories` | List&lt;String&gt; | Directories to load skills from | [Skills](advanced.html#Skills_Configuration) |
| `disabledSkills` | List&lt;String&gt; | Skills to disable by name | [Skills](advanced.html#Skills_Configuration) |
| `configDir` | String | Custom configuration directory | [Config Dir](advanced.html#Custom_Configuration_Directory) |
| `commands` | List&lt;CommandDefinition&gt; | Slash command definitions | [Slash Commands](advanced.html#Slash_Commands) |
| `onElicitationRequest` | ElicitationHandler | Handler for incoming elicitation requests | [Elicitation](advanced.html#Elicitation_UI_Dialogs) |
| `onEvent` | Consumer&lt;SessionEvent&gt; | Event handler registered before session creation | [Early Event Registration](advanced.html#Early_Event_Registration) |

### Cloning SessionConfig

Use `clone()` to copy a base configuration before making per-session changes:

```java
var base = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setModel("gpt-4.1")
    .setStreaming(true);

var derived = base.clone()
    .setWorkingDirectory("/repo-a");
```

`clone()` creates a shallow copy. Collection fields are copied into new collection instances, while nested objects/handlers are shared references.

See [SessionConfig](apidocs/com/github/copilot/sdk/json/SessionConfig.html) Javadoc for full details.

---

## Next Steps

- 📖 **[Advanced Usage](advanced.html)** - Tools, BYOK, MCP Servers, System Messages, Infinite Sessions, Skills
- 📖 **[Session Hooks](hooks.html)** - Intercept tool execution and session lifecycle events
- 📖 **[MCP Servers](mcp.html)** - Integrate external tools via Model Context Protocol
- 📖 **[API Javadoc](apidocs/index.html)** - Complete API reference
