# GitHub Copilot SDK for Java

Java SDK for programmatic control of GitHub Copilot CLI via JSON-RPC.

> **📦 The Java SDK is maintained in a separate repository: [`github/copilot-sdk-java`](https://github.com/github/copilot-sdk-java)**
>
> **Note:** This SDK is in public preview and may change in breaking ways.

[![Build](https://github.com/github/copilot-sdk-java/actions/workflows/build-test.yml/badge.svg)](https://github.com/github/copilot-sdk-java/actions/workflows/build-test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github/copilot-sdk-java)](https://central.sonatype.com/artifact/com.github/copilot-sdk-java)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Documentation](https://img.shields.io/badge/docs-online-brightgreen)](https://github.github.io/copilot-sdk-java/)
[![Javadoc](https://javadoc.io/badge2/com.github/copilot-sdk-java/javadoc.svg)](https://javadoc.io/doc/com.github/copilot-sdk-java/latest/index.html)

## Installation

### Maven

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>LATEST</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github:copilot-sdk-java:LATEST'
```

### Try it with JBang

Run the SDK without setting up a full project using [JBang](https://www.jbang.dev/):

```bash
jbang https://github.com/github/copilot-sdk-java/blob/main/jbang-example.java
```

## Quick Start

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session (onPermissionRequest is required)
            var session = client.createSession(
                new SessionConfig()
                    .setModel("gpt-5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
            ).get();

            var done = new java.util.concurrent.CompletableFuture<Void>();

            // Handle events
            session.on(AssistantMessageEvent.class, msg ->
                System.out.println(msg.getData().content()));
            session.on(SessionIdleEvent.class, idle ->
                done.complete(null));

            // Send a message and wait for completion
            session.send(new MessageOptions().setPrompt("What is 2+2?"));
            done.get();
        }
    }
}
```

`CopilotClient` implements `AutoCloseable` — using try-with-resources ensures graceful shutdown. You can also call `stop()` or `forceStop()` manually.

## API Reference

### CopilotClient

#### Constructor

```java
new CopilotClient()
new CopilotClient(CopilotClientOptions options)
```

**Options (`CopilotClientOptions`):**

- `cliPath` - Path to CLI executable (default: `COPILOT_CLI_PATH` env var)
- `cliArgs` - Extra arguments prepended before SDK-managed flags
- `cliUrl` - URL of existing CLI server to connect to (e.g., `"localhost:8080"`). When provided, the client will not spawn a CLI process.
- `port` - Server port (default: `0` for random)
- `useStdio` - Use stdio transport instead of TCP (default: `true`)
- `logLevel` - Log level (default: `"info"`)
- `autoStart` - Auto-start server (default: `true`)
- `cwd` - Working directory for the CLI process
- `environment` - Environment variables to pass to the CLI process
- `executor` - Custom `ExecutorService` for async operations
- `gitHubToken` - GitHub token for authentication. When provided, takes priority over other auth methods.
- `useLoggedInUser` - Whether to use logged-in user for authentication (default: `true`, but `false` when `gitHubToken` is provided). Cannot be used with `cliUrl`.
- `telemetry` - OpenTelemetry configuration for the CLI process. Providing this enables telemetry — no separate flag needed. See [Telemetry](#telemetry) below.

#### Methods

##### `start(): CompletableFuture<Void>`

Start the CLI server and establish connection.

##### `stop(): CompletableFuture<Void>`

Stop the server and close all sessions gracefully.

##### `forceStop(): CompletableFuture<Void>`

Force stop the CLI server without graceful cleanup. Use when `stop()` takes too long.

##### `createSession(SessionConfig config): CompletableFuture<CopilotSession>`

Create a new conversation session.

**SessionConfig options:**

- `model` - Model to use (e.g., `"gpt-5"`, `"claude-sonnet-4.5"`)
- `reasoningEffort` - Reasoning effort level: `"low"`, `"medium"`, `"high"`, `"xhigh"`
- `onPermissionRequest` - **Required.** Handler for permission requests (see [Permission Handling](#permission-handling))
- `onUserInputRequest` - Handler for `ask_user` tool requests (see [User Input Requests](#user-input-requests))
- `tools` - List of `ToolDefinition` for custom tools
- `systemMessage` - System message configuration
- `streaming` - Enable streaming responses (default: `false`)
- `provider` - Custom provider for BYOK (see [BYOK](#bring-your-own-key-byok))
- `hooks` - Session lifecycle hooks
- `infiniteSessions` - Infinite session configuration
- `mcpServers` - MCP server configuration
- `customAgents` - Custom agent definitions
- `agent` - Name of agent to use
- `workingDirectory` - Override the working directory for the session
- `sessionId` - Custom session ID
- `availableTools` - Allowlist of built-in tools
- `excludedTools` - Denylist of built-in tools
- `skillDirectories` - Directories to load skills from
- `disabledSkills` - Skills to disable by name
- `configDir` - Custom configuration directory
- `onEvent` - Early event handler registered before the session.create RPC (see [Early Event Registration](#early-event-registration))

##### `resumeSession(String sessionId, ResumeSessionConfig config): CompletableFuture<CopilotSession>`

Resume an existing session by ID.

##### `listSessions(): CompletableFuture<List<SessionMetadata>>`

List all persisted sessions.

##### `listSessions(SessionListFilter filter): CompletableFuture<List<SessionMetadata>>`

List sessions with context filtering (by repository, branch, cwd, or git root).

##### `deleteSession(String sessionId): CompletableFuture<Void>`

Delete a persisted session.

##### `getLastSessionId(): CompletableFuture<String>`

Get the last active session ID.

##### `listModels(): CompletableFuture<List<ModelInfo>>`

List available models.

##### `ping(String message): CompletableFuture<PingResponse>`

Ping the CLI server to check connectivity.

##### `getStatus(): CompletableFuture<GetStatusResponse>`

Get server status information.

##### `getAuthStatus(): CompletableFuture<GetAuthStatusResponse>`

Get authentication status.

##### `getState(): ConnectionState`

Get the current connection state.

##### `onLifecycle(SessionLifecycleHandler handler): AutoCloseable`

Subscribe to session lifecycle events (created, deleted, updated, foreground, background).

##### `close()`

Close the client (delegates to `stop()`, waits up to 10 seconds).

### CopilotSession

#### Methods

##### `send(String prompt): CompletableFuture<String>`

Send a message and return the request ID.

##### `send(MessageOptions options): CompletableFuture<String>`

Send a message with options (prompt, attachments, mode).

##### `sendAndWait(String prompt): CompletableFuture<AssistantMessageEvent>`

Send a message and wait for the final assistant response (default timeout: 60 seconds).

##### `sendAndWait(MessageOptions options): CompletableFuture<AssistantMessageEvent>`

Send a message with options and wait for the final assistant response.

##### `sendAndWait(MessageOptions options, long timeoutMs): CompletableFuture<AssistantMessageEvent>`

Send a message and wait with a custom timeout. Use `timeoutMs <= 0` for no timeout.

##### `on(Consumer<AbstractSessionEvent> handler): Closeable`

Register a handler for all session events. Returns a `Closeable` to unsubscribe.

##### `on(Class<T> eventType, Consumer<T> handler): Closeable`

Register a typed handler for a specific event type. Returns a `Closeable` to unsubscribe.

##### `getMessages(): CompletableFuture<List<AbstractSessionEvent>>`

Get the message history for the session.

##### `abort(): CompletableFuture<Void>`

Abort the current operation.

##### `setModel(String model): CompletableFuture<Void>`

Switch to a different model mid-session.

##### `setModel(String model, String reasoningEffort): CompletableFuture<Void>`

Switch model with a specific reasoning effort level.

##### `log(String message): CompletableFuture<Void>`

Send a log message to the session.

##### `listAgents(): CompletableFuture<List<AgentInfo>>`

List available agents.

##### `selectAgent(String agentName): CompletableFuture<AgentInfo>`

Programmatically select an agent.

##### `deselectAgent(): CompletableFuture<Void>`

Deselect the current agent, returning to the default.

##### `compact(): CompletableFuture<Void>`

Trigger manual context compaction.

##### `getSessionId(): String`

Get the session ID.

##### `getWorkspacePath(): String`

Get the workspace path for persisted session state.

##### `close()`

Close the session and release resources.

## Event Types

Sessions emit various events during processing:

- `UserMessageEvent` - User message added
- `AssistantMessageEvent` - Assistant response (final)
- `AssistantMessageDeltaEvent` - Streaming response chunk
- `AssistantReasoningEvent` - Reasoning content (final)
- `AssistantReasoningDeltaEvent` - Streaming reasoning chunk
- `AssistantTurnStartEvent` / `AssistantTurnEndEvent` - Turn boundaries
- `ToolExecutionStartEvent` - Tool execution started
- `ToolExecutionCompleteEvent` - Tool execution completed
- `ToolExecutionProgressEvent` - Tool execution progress update
- `SessionStartEvent` - Session created
- `SessionResumeEvent` - Session resumed
- `SessionIdleEvent` - Session finished processing
- `SessionErrorEvent` - Error occurred
- `SessionModelChangeEvent` - Model switched
- `SessionCompactionStartEvent` / `SessionCompactionCompleteEvent` - Compaction lifecycle
- `SessionShutdownEvent` - Session shutdown with usage stats
- `PermissionRequestedEvent` / `PermissionCompletedEvent` - Permission lifecycle
- `SubagentStartedEvent` / `SubagentCompletedEvent` / `SubagentFailedEvent` - Subagent lifecycle
- `HookStartEvent` / `HookEndEvent` - Hook invocation lifecycle
- `SkillInvokedEvent` - Skill loaded and invoked
- `AbortEvent` - Operation aborted
- And more...

All events extend `AbstractSessionEvent` with common fields: `id`, `timestamp`, `parentId`, `ephemeral`.

## Image Support

The SDK supports image attachments via the `attachments` parameter. You can attach images by providing their file path, or by passing base64-encoded data directly using a blob attachment:

```java
// File attachment — runtime reads from disk
session.send(new MessageOptions()
    .setPrompt("What's in this image?")
    .setAttachments(List.of(
        new Attachment("file", "/path/to/image.jpg", "image.jpg")
    ))
).get();

// Blob attachment — provide base64 data directly
byte[] imageBytes = Files.readAllBytes(Path.of("/path/to/screenshot.png"));
String base64Data = Base64.getEncoder().encodeToString(imageBytes);

session.send(new MessageOptions()
    .setPrompt("What's in this image?")
    .setAttachments(List.of(
        new BlobAttachment()
            .setData(base64Data)
            .setMimeType("image/png")
            .setDisplayName("screenshot.png")
    ))
).get();
```

Supported image formats include JPG, PNG, GIF, and other common image types. The agent's `view` tool can also read images directly from the filesystem, so you can also ask questions like:

```java
session.send(new MessageOptions()
    .setPrompt("What does the most recent jpg in this directory portray?")
).get();
```

Both `Attachment` and `BlobAttachment` implement the sealed `MessageAttachment` interface. For a mixed list with both types, use an explicit type hint:

```java
session.send(new MessageOptions()
    .setPrompt("Analyze these")
    .setAttachments(List.<MessageAttachment>of(
        new Attachment("file", "/path/to/file.java", "Source"),
        new BlobAttachment()
            .setData(base64Data)
            .setMimeType("image/png")
            .setDisplayName("screenshot.png")
    ))
).get();
```

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setStreaming(true)
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();

var done = new CompletableFuture<Void>();

session.on(AssistantMessageDeltaEvent.class, delta -> {
    // Streaming message chunk - print incrementally
    System.out.print(delta.getData().deltaContent());
});

session.on(AssistantReasoningDeltaEvent.class, delta -> {
    // Streaming reasoning chunk (if model supports reasoning)
    System.out.print(delta.getData().deltaContent());
});

session.on(AssistantMessageEvent.class, msg -> {
    // Final message - complete content
    System.out.println("\n--- Final message ---");
    System.out.println(msg.getData().content());
});

session.on(AssistantReasoningEvent.class, reasoning -> {
    // Final reasoning content (if model supports reasoning)
    System.out.println("--- Reasoning ---");
    System.out.println(reasoning.getData().content());
});

session.on(SessionIdleEvent.class, idle -> {
    // Session finished processing
    done.complete(null);
});

session.send(new MessageOptions().setPrompt("Tell me a short story")).get();
done.get(); // Wait for streaming to complete
```

When `streaming` is enabled:

- `AssistantMessageDeltaEvent` events are sent with `deltaContent()` containing incremental text
- `AssistantReasoningDeltaEvent` events are sent with `deltaContent()` for reasoning/chain-of-thought (model-dependent)
- Accumulate `deltaContent()` values to build the full response progressively
- The final `AssistantMessageEvent` and `AssistantReasoningEvent` events contain the complete content

Note: `AssistantMessageEvent` and `AssistantReasoningEvent` (final events) are always sent regardless of streaming setting.

## Infinite Sessions

By default, sessions use **infinite sessions** which automatically manage context window limits through background compaction and persist state to a workspace directory.

```java
// Default: infinite sessions enabled with default thresholds
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();

// Access the workspace path for checkpoints and files
System.out.println(session.getWorkspacePath());
// => ~/.copilot/session-state/{sessionId}/

// Custom thresholds
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setInfiniteSessions(new InfiniteSessionConfig()
            .setEnabled(true)
            .setBackgroundCompactionThreshold(0.80)  // Start compacting at 80% context usage
            .setBufferExhaustionThreshold(0.95))     // Block at 95% until compaction completes
).get();

// Disable infinite sessions
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setInfiniteSessions(new InfiniteSessionConfig().setEnabled(false))
).get();
```

When enabled, sessions emit compaction events:

- `SessionCompactionStartEvent` - Background compaction started
- `SessionCompactionCompleteEvent` - Compaction finished (includes token counts, success status)

You can also trigger compaction manually:

```java
session.compact().get();
```

## Advanced Usage

### Manual Server Control

```java
var client = new CopilotClient(
    new CopilotClientOptions().setAutoStart(false)
);

// Start manually
client.start().get();

// Use client...

// Stop manually — graceful shutdown, closes all sessions
client.stop().get();

// Or force stop — immediate shutdown, no cleanup
client.forceStop().get();
```

### Tools

You can let the CLI call back into your process when the model needs capabilities you own. Use `ToolDefinition.create()` for type-safe tool definitions:

```java
import com.github.copilot.sdk.json.ToolDefinition;

var lookupTool = ToolDefinition.create(
    "lookup_issue",
    "Fetch issue details from our tracker",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "id", Map.of("type", "string", "description", "Issue identifier")
        ),
        "required", List.of("id")
    ),
    invocation -> {
        var args = invocation.getArgumentsAs(IssueArgs.class);
        var issue = fetchIssue(args.id());
        return CompletableFuture.completedFuture(issue);
    }
);

var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setTools(List.of(lookupTool))
).get();
```

When Copilot invokes `lookup_issue`, the client automatically runs your handler and responds to the CLI. Handlers return `CompletableFuture<Object>` — the value is JSON-serialized and sent back. You can use `invocation.getArguments()` for raw `Map<String, Object>` access, or `invocation.getArgumentsAs(Class)` for typed deserialization.

#### Overriding Built-in Tools

If you register a tool with the same name as a built-in CLI tool (e.g. `edit_file`, `read_file`), the SDK will throw an error unless you use `ToolDefinition.createOverride()`. This signals that you intend to replace the built-in tool with your custom implementation.

```java
var customGrep = ToolDefinition.createOverride(
    "grep",
    "Project-aware search with custom filtering",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "query", Map.of("type", "string", "description", "Search query")
        ),
        "required", List.of("query")
    ),
    invocation -> {
        String query = (String) invocation.getArguments().get("query");
        return CompletableFuture.completedFuture("Results for: " + query);
    }
);
```

#### Skipping Permission Prompts

Set `ToolDefinition.createSkipPermission()` to allow a tool to execute without triggering a permission prompt:

```java
var safeLookup = ToolDefinition.createSkipPermission(
    "safe_lookup",
    "A read-only lookup that needs no confirmation",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "id", Map.of("type", "string")
        ),
        "required", List.of("id")
    ),
    invocation -> {
        String id = (String) invocation.getArguments().get("id");
        return CompletableFuture.completedFuture(lookupRecord(id));
    }
);
```

### System Message Customization

Control the system prompt using `systemMessage` in session config:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.APPEND)
            .setContent("""
                <workflow_rules>
                - Always check for security vulnerabilities
                - Suggest performance improvements when applicable
                </workflow_rules>
            """))
).get();
```

The SDK auto-injects environment context, tool instructions, and security guardrails. The default CLI persona is preserved, and your `content` is appended after SDK-managed sections.

#### Customize Mode

Use `SystemMessageMode.CUSTOMIZE` to selectively override individual sections of the prompt while preserving the rest:

```java
import com.github.copilot.sdk.json.SystemPromptSections;
import com.github.copilot.sdk.json.SectionOverride;
import com.github.copilot.sdk.json.SectionOverrideAction;

var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.CUSTOMIZE)
            .setSections(Map.of(
                // Replace the tone section
                SystemPromptSections.TONE,
                    new SectionOverride()
                        .setAction(SectionOverrideAction.REPLACE)
                        .setContent("Respond in a warm, professional tone."),
                // Remove coding-specific rules
                SystemPromptSections.CODE_CHANGE_RULES,
                    new SectionOverride()
                        .setAction(SectionOverrideAction.REMOVE),
                // Append to existing guidelines
                SystemPromptSections.GUIDELINES,
                    new SectionOverride()
                        .setAction(SectionOverrideAction.APPEND)
                        .setContent("\n* Always cite data sources")
            ))
            // Additional instructions appended after all sections
            .setContent("Focus on financial analysis and reporting."))
).get();
```

Available section IDs are constants on `SystemPromptSections`: `IDENTITY`, `TONE`, `TOOL_EFFICIENCY`, `ENVIRONMENT_CONTEXT`, `CODE_CHANGE_RULES`, `GUIDELINES`, `SAFETY`, `TOOL_INSTRUCTIONS`, `CUSTOM_INSTRUCTIONS`, `LAST_INSTRUCTIONS`.

Each section override supports four actions: `REPLACE`, `REMOVE`, `APPEND`, and `PREPEND`. Unknown section IDs are handled gracefully: content is appended to additional instructions, and `REMOVE` overrides are silently ignored.

#### Replace Mode

For full control (removes all guardrails), use `SystemMessageMode.REPLACE`:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.REPLACE)
            .setContent("You are a helpful assistant."))
).get();
```

### Multiple Sessions

```java
var session1 = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();

var session2 = client.createSession(
    new SessionConfig()
        .setModel("claude-sonnet-4.5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();

// Both sessions are independent
session1.send(new MessageOptions().setPrompt("Hello from session 1")).get();
session2.send(new MessageOptions().setPrompt("Hello from session 2")).get();
```

### File Attachments

```java
session.send(new MessageOptions()
    .setPrompt("Analyze this file")
    .setAttachments(List.of(
        new Attachment("file", "/path/to/file.java", "MyService.java")
    ))
).get();
```

### Custom Agents

Extend the base Copilot assistant with specialized agents:

```java
var reviewer = new CustomAgentConfig()
    .setName("code-reviewer")
    .setDisplayName("Code Reviewer")
    .setDescription("Reviews code for best practices and security")
    .setPrompt("You are a code review expert.")
    .setTools(List.of("read_file", "search_code"));

var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setCustomAgents(List.of(reviewer))
).get();

// The user can now mention @code-reviewer in messages
session.send("@code-reviewer Review src/Main.java").get();
```

### MCP Servers

Extend the AI with external tools via the Model Context Protocol:

```java
Map<String, Object> server = Map.of(
    "type", "local",
    "command", "npx",
    "args", List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
    "tools", List.of("*")
);

var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setMcpServers(Map.of("filesystem", server))
).get();
```

### Bring Your Own Key (BYOK)

Use a custom API provider:

```java
var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setProvider(new ProviderConfig()
            .setType("openai")
            .setBaseUrl("https://api.openai.com/v1")
            .setApiKey("your-api-key"))
).get();
```

Supported providers: `"openai"` (OpenAI, Ollama, Foundry Local, vLLM, LiteLLM), `"azure"` (Azure OpenAI / Azure AI Foundry), `"anthropic"` (Claude models).

### Early Event Registration

Register an event handler *before* the `session.create` RPC is issued, ensuring no early events (like `SessionStartEvent`) are missed:

```java
var events = new CopyOnWriteArrayList<AbstractSessionEvent>();

var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setOnEvent(events::add)  // Registered before session.create RPC
).get();
```

## Telemetry

The SDK supports OpenTelemetry for distributed tracing. Provide a `telemetry` config to enable trace export from the CLI process:

```java
var client = new CopilotClient(
    new CopilotClientOptions()
        .setTelemetry(new TelemetryConfig()
            .setOtlpEndpoint("http://localhost:4318"))
);
```

With just this configuration, the CLI emits spans for every session, message, and tool call to your collector. No additional dependencies or setup required.

**TelemetryConfig options:**

- `otlpEndpoint` - OTLP HTTP endpoint URL
- `filePath` - File path for JSON-lines trace output
- `exporterType` - `"otlp-http"` or `"file"`
- `sourceName` - Instrumentation scope name
- `captureContent` - Whether to capture message content

To export to a local file instead:

```java
var client = new CopilotClient(
    new CopilotClientOptions()
        .setTelemetry(new TelemetryConfig()
            .setExporterType("file")
            .setFilePath("/tmp/copilot-traces.json")
            .setCaptureContent(true))
);
```

## Permission Handling

An `onPermissionRequest` handler is **required** whenever you create or resume a session. The handler is called before the agent executes each tool (file writes, shell commands, custom tools, etc.) and must return a decision.

### Approve All (simplest)

Use the built-in `PermissionHandler.APPROVE_ALL` to allow every tool call without any checks:

```java
import com.github.copilot.sdk.json.PermissionHandler;

var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();
```

### Custom Permission Handler

Provide your own function to inspect each request and apply custom logic:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest((request, invocation) -> {
            // request.getKind() — what type of operation is being requested:
            //   "shell"       — executing a shell command
            //   "write"       — writing or editing a file
            //   "read"        — reading a file
            //   "mcp"         — calling an MCP tool
            //   "custom-tool" — calling one of your registered tools
            //   "url"         — fetching a URL
            //   "memory"      — storing or retrieving persistent session memory
            //   "hook"        — invoking a registered hook
            // request.getToolCallId()      — the tool call that triggered this request
            // request.getToolName()        — name of the tool (for custom-tool / mcp)
            // request.getFileName()        — file being written (for write)
            // request.getFullCommandText() — full shell command (for shell)

            if ("shell".equals(request.getKind())) {
                // Deny shell commands
                var result = new PermissionRequestResult();
                result.setKind(PermissionRequestResultKind.DENIED_INTERACTIVELY_BY_USER);
                return CompletableFuture.completedFuture(result);
            }

            var result = new PermissionRequestResult();
            result.setKind(PermissionRequestResultKind.APPROVED);
            return CompletableFuture.completedFuture(result);
        })
).get();
```

### Permission Result Kinds

| Constant                                                        | Meaning                                                      |
| --------------------------------------------------------------- | ------------------------------------------------------------ |
| `PermissionRequestResultKind.APPROVED`                          | Allow the tool to run                                        |
| `PermissionRequestResultKind.DENIED_INTERACTIVELY_BY_USER`      | User explicitly denied the request                           |
| `PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER`| No approval rule matched and user could not be asked         |
| `PermissionRequestResultKind.DENIED_BY_RULES`                   | Denied by a policy rule                                      |

### Resuming Sessions

Pass `onPermissionRequest` when resuming a session too — it is required:

```java
var session = client.resumeSession("session-id",
    new ResumeSessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();
```

### Per-Tool Skip Permission

To let a specific custom tool bypass the permission prompt entirely, use `ToolDefinition.createSkipPermission()`. See [Skipping Permission Prompts](#skipping-permission-prompts) under Tools.

## User Input Requests

Enable the agent to ask questions to the user using the `ask_user` tool by providing an `onUserInputRequest` handler:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setOnUserInputRequest((request, invocation) -> {
            // request.getQuestion() - The question to ask
            // request.getChoices()  - Optional list of choices for multiple choice

            System.out.println("Agent asks: " + request.getQuestion());

            if (request.getChoices() != null && !request.getChoices().isEmpty()) {
                System.out.println("Options: " + request.getChoices());
                return CompletableFuture.completedFuture(
                    new UserInputResponse()
                        .setAnswer(request.getChoices().get(0))
                        .setWasFreeform(false)
                );
            }

            // Freeform input
            return CompletableFuture.completedFuture(
                new UserInputResponse()
                    .setAnswer("User's answer here")
                    .setWasFreeform(true)
            );
        })
).get();
```

## Session Hooks

Hook into session lifecycle events by providing handlers in the `hooks` configuration:

```java
var hooks = new SessionHooks()
    // Called before each tool execution
    .setOnPreToolUse((input, invocation) -> {
        System.out.println("About to run tool: " + input.getToolName());
        // Return permission decision and optionally modify args
        return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
    })

    // Called after each tool execution
    .setOnPostToolUse((input, invocation) -> {
        System.out.println("Tool " + input.getToolName() + " completed");
        return CompletableFuture.completedFuture(null);
    })

    // Called when user submits a prompt
    .setOnUserPromptSubmitted((input, invocation) -> {
        System.out.println("User prompt: " + input.getPrompt());
        return CompletableFuture.completedFuture(null);
    })

    // Called when session starts
    .setOnSessionStart((input, invocation) -> {
        System.out.println("Session started from: " + input.getSource());
        return CompletableFuture.completedFuture(null);
    })

    // Called when session ends
    .setOnSessionEnd((input, invocation) -> {
        System.out.println("Session ended: " + input.getReason());
        return CompletableFuture.completedFuture(null);
    });

var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setHooks(hooks)
).get();
```

**Available hooks:**

- `onPreToolUse` - Intercept tool calls before execution. Can allow/deny or modify arguments.
- `onPostToolUse` - Process tool results after execution. Can modify results or add context.
- `onUserPromptSubmitted` - Intercept user prompts. Can modify the prompt before processing.
- `onSessionStart` - Run logic when a session starts or resumes.
- `onSessionEnd` - Cleanup or logging when session ends.

## Error Handling

All SDK methods return `CompletableFuture`. Errors surface via `ExecutionException`:

```java
try {
    var session = client.createSession(
        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    ).get();
    session.send(new MessageOptions().setPrompt("Hello")).get();
} catch (ExecutionException ex) {
    System.err.println("Error: " + ex.getCause().getMessage());
}
```

For reactive error handling, use `exceptionally()` or `handle()`:

```java
session.send(new MessageOptions().setPrompt("Hello"))
    .exceptionally(ex -> {
        System.err.println("Failed: " + ex.getMessage());
        return null;
    });
```

### Event Error Handling

By default, event handler exceptions stop dispatch. You can configure error behavior:

```java
// Continue dispatching despite handler errors
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);

// Custom error handler for metrics/alerting
session.setEventErrorHandler((event, exception) -> {
    logger.error("Handler failed on {}: {}",
        event.getType(), exception.getMessage());
});
```

| Policy                         | Behavior                                           |
| ------------------------------ | -------------------------------------------------- |
| `PROPAGATE_AND_LOG_ERRORS` (default) | Log the error; dispatch halts after first error |
| `SUPPRESS_AND_LOG_ERRORS`      | Log the error; all remaining handlers execute      |

## Documentation & Resources

| Resource                      | Link                                                                                                                                   |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **Full Documentation**        | [github.github.io/copilot-sdk-java](https://github.github.io/copilot-sdk-java/)                                                        |
| **Getting Started Guide**     | [Documentation](https://github.github.io/copilot-sdk-java/latest/documentation.html)                                                   |
| **API Reference (Javadoc)**   | [javadoc.io](https://javadoc.io/doc/com.github/copilot-sdk-java/latest/index.html)                                                     |
| **MCP Servers Integration**   | [MCP Guide](https://github.github.io/copilot-sdk-java/latest/mcp.html)                                                                 |
| **Cookbook**                   | [Recipes](https://github.com/github/copilot-sdk-java/tree/main/src/site/markdown/cookbook)                                              |
| **Source Code**               | [github/copilot-sdk-java](https://github.com/github/copilot-sdk-java)                                                                  |
| **Issues & Feature Requests** | [GitHub Issues](https://github.com/github/copilot-sdk-java/issues)                                                                     |
| **Releases**                  | [GitHub Releases](https://github.com/github/copilot-sdk-java/releases)                                                                 |

## Requirements

- Java 17 or later
- GitHub Copilot CLI installed and in PATH (or provide custom `cliPath`)

## Contributing

Contributions are welcome! Please see the [Contributing Guide](https://github.com/github/copilot-sdk-java/blob/main/CONTRIBUTING.md) in the GitHub Copilot SDK for Java repository.

## License

MIT — see [LICENSE](https://github.com/github/copilot-sdk-java/blob/main/LICENSE) for details.
