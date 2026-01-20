# Copilot SDK for Java

Java SDK for programmatic control of GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Requirements

- Java 21 or later
- GitHub Copilot CLI installed and in PATH (or provide custom `cliPath`)

## Installation

### Maven

Run `mvn install` locally, then configure the dependency in your project.

```xml
<dependency>
    <groupId>com.github.copilot</groupId>
    <artifactId>copilot-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.copilot:copilot-sdk:0.1.0'
```

## Quick Start

```java
import com.github.copilot.sdk.*;
import com.github.copilot.sdk.events.*;
import com.github.copilot.sdk.json.*;

import java.util.concurrent.CompletableFuture;

public class Example {
    public static void main(String[] args) throws Exception {
        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session
            var session = client.createSession(
                new SessionConfig().setModel(CopilotModel.CLAUDE_SONNET_4_5.toString())
            ).get();

            // Wait for response using session.idle event
            var done = new CompletableFuture<Void>();

            session.on(evt -> {
                if (evt instanceof AssistantMessageEvent msg) {
                    System.out.println(msg.getData().getContent());
                } else if (evt instanceof SessionIdleEvent) {
                    done.complete(null);
                }
            });

            // Send a message and wait for completion
            session.send(new MessageOptions().setPrompt("What is 2+2?")).get();
            done.get();
        }
    }
}
```

## Try it with JBang

You can quickly try the SDK without setting up a full project using [JBang](https://www.jbang.dev/):

```bash
# Assuming you are in the `java/` directory of this repository
# Install the SDK locally first (not yet on Maven Central)
mvn install

# Install JBang (if not already installed)
# macOS: brew install jbang
# Linux/Windows: curl -Ls https://sh.jbang.dev | bash -s - app setup

# Run the example
jbang jbang-example.java
```

The `jbang-example.java` file includes the dependency declaration and can be run directly:

```java
//DEPS com.github.copilot:copilot-sdk:0.1.0
```

## API Reference

### CopilotClient

#### Constructor

```java
new CopilotClient()
new CopilotClient(CopilotClientOptions options)
```

**Options:**

- `cliPath` - Path to CLI executable (default: "copilot" from PATH)
- `cliArgs` - Extra arguments prepended before SDK-managed flags
- `cliUrl` - URL of existing CLI server to connect to (e.g., `"localhost:8080"`). When provided, the client will not spawn a CLI process.
- `port` - Server port (default: 0 for random)
- `useStdio` - Use stdio transport instead of TCP (default: true)
- `logLevel` - Log level (default: "info")
- `autoStart` - Auto-start server (default: true)
- `autoRestart` - Auto-restart on crash (default: true)
- `cwd` - Working directory for the CLI process
- `environment` - Environment variables to pass to the CLI process

#### Methods

##### `start(): CompletableFuture<Void>`

Start the CLI server and establish connection.

##### `stop(): CompletableFuture<Void>`

Stop the server and close all sessions.

##### `forceStop(): CompletableFuture<Void>`

Force stop the CLI server without graceful cleanup.

##### `createSession(SessionConfig config): CompletableFuture<CopilotSession>`

Create a new conversation session.

**Config:**

- `sessionId` - Custom session ID
- `model` - Model to use ("gpt-5", "claude-sonnet-4.5", etc.)
- `tools` - Custom tools exposed to the CLI
- `systemMessage` - System message customization
- `availableTools` - List of tool names to allow
- `excludedTools` - List of tool names to disable
- `provider` - Custom API provider configuration (BYOK)
- `streaming` - Enable streaming of response chunks (default: false)
- `mcpServers` - MCP server configurations
- `customAgents` - Custom agent configurations
- `onPermissionRequest` - Handler for permission requests

##### `resumeSession(String sessionId, ResumeSessionConfig config): CompletableFuture<CopilotSession>`

Resume an existing session.

##### `ping(String message): CompletableFuture<PingResponse>`

Ping the server to check connectivity.

##### `getState(): ConnectionState`

Get current connection state. Returns one of: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `ERROR`.

##### `listSessions(): CompletableFuture<List<SessionMetadata>>`

List all available sessions.

##### `deleteSession(String sessionId): CompletableFuture<Void>`

Delete a session and its data from disk.

##### `getLastSessionId(): CompletableFuture<String>`

Get the ID of the most recently used session.

---

### CopilotSession

Represents a single conversation session.

#### Properties

- `getSessionId()` - The unique identifier for this session

#### Methods

##### `send(MessageOptions options): CompletableFuture<String>`

Send a message to the session.

**Options:**

- `prompt` - The message/prompt to send
- `attachments` - File attachments
- `mode` - Delivery mode ("enqueue" or "immediate")

Returns the message ID.

##### `sendAndWait(MessageOptions options, long timeoutMs): CompletableFuture<AssistantMessageEvent>`

Send a message and wait for the session to become idle. Default timeout is 60 seconds.

##### `on(Consumer<AbstractSessionEvent> handler): Closeable`

Subscribe to session events. Returns a `Closeable` to unsubscribe.

```java
var subscription = session.on(evt -> {
    System.out.println("Event: " + evt.getType());
});

// Later...
subscription.close();
```

##### `abort(): CompletableFuture<Void>`

Abort the currently processing message in this session.

##### `getMessages(): CompletableFuture<List<AbstractSessionEvent>>`

Get all events/messages from this session.

##### `close()`

Dispose the session and free resources.

---

## Event Types

Sessions emit various events during processing. Each event type extends `AbstractSessionEvent`:

- `UserMessageEvent` - User message added
- `AssistantMessageEvent` - Assistant response
- `AssistantMessageDeltaEvent` - Streaming response chunk
- `ToolExecutionStartEvent` - Tool execution started
- `ToolExecutionCompleteEvent` - Tool execution completed
- `SessionStartEvent` - Session started
- `SessionIdleEvent` - Session is idle
- `SessionErrorEvent` - Session error occurred
- `SessionResumeEvent` - Session was resumed
- And more...

Use pattern matching (Java 21+) to handle specific event types:

```java
session.on(evt -> {
    if (evt instanceof AssistantMessageEvent msg) {
        System.out.println(msg.getData().getContent());
    } else if (evt instanceof SessionErrorEvent err) {
        System.out.println("Error: " + err.getData().getMessage());
    }
});
```

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setStreaming(true)
).get();

var done = new CompletableFuture<Void>();

session.on(evt -> {
    if (evt instanceof AssistantMessageDeltaEvent delta) {
        // Streaming message chunk - print incrementally
        System.out.print(delta.getData().getDeltaContent());
    } else if (evt instanceof AssistantMessageEvent msg) {
        // Final message - complete content
        System.out.println("\n--- Final message ---");
        System.out.println(msg.getData().getContent());
    } else if (evt instanceof SessionIdleEvent) {
        done.complete(null);
    }
});

session.send(new MessageOptions().setPrompt("Tell me a short story")).get();
done.get();
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

// Stop manually
client.stop().get();
```

### Tools

You can let the CLI call back into your process when the model needs capabilities you own:

```java
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
        String id = ((Map<String, Object>) invocation.getArguments()).get("id").toString();
        return CompletableFuture.completedFuture(fetchIssue(id));
    }
);

var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setTools(List.of(lookupTool))
).get();
```

### System Message Customization

Control the system prompt using `SystemMessageConfig` in session config:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
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

For full control (removes all guardrails), use `REPLACE` mode:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.REPLACE)
            .setContent("You are a helpful assistant."))
).get();
```

### Multiple Sessions

```java
var session1 = client.createSession(
    new SessionConfig().setModel("gpt-5")
).get();

var session2 = client.createSession(
    new SessionConfig().setModel("claude-sonnet-4.5")
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
        new Attachment()
            .setType("file")
            .setPath("/path/to/file.java")
            .setDisplayName("My File")
    ))
).get();
```

### Bring Your Own Key (BYOK)

Use a custom API provider:

```java
var session = client.createSession(
    new SessionConfig()
        .setProvider(new ProviderConfig()
            .setType("openai")
            .setBaseUrl("https://api.openai.com/v1")
            .setApiKey("your-api-key"))
).get();
```

### Permission Handling

Handle permission requests from the CLI:

```java
var session = client.createSession(
    new SessionConfig()
        .setModel("gpt-5")
        .setOnPermissionRequest((request, invocation) -> {
            // Approve or deny the permission request
            var result = new PermissionRequestResult();
            result.setKind("user-approved");
            return CompletableFuture.completedFuture(result);
        })
).get();
```

## Error Handling

```java
try {
    var session = client.createSession().get();
    session.send(new MessageOptions().setPrompt("Hello")).get();
} catch (ExecutionException ex) {
    Throwable cause = ex.getCause();
    System.err.println("Error: " + cause.getMessage());
}
```

## License

MIT
