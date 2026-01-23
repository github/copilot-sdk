# Copilot SDK for Node.js/TypeScript

TypeScript SDK for programmatic control of GitHub Copilot CLI via JSON-RPC.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
npm install @github/copilot-sdk
```

## Quick Start

```typescript
import { CopilotClient } from "@github/copilot-sdk";

// Create and start client
const client = new CopilotClient();
await client.start();

// Create a session
const session = await client.createSession({
    model: "gpt-5",
});

// Wait for response using session.idle event
const done = new Promise<void>((resolve) => {
    session.on((event) => {
        if (event.type === "assistant.message") {
            console.log(event.data.content);
        } else if (event.type === "session.idle") {
            resolve();
        }
    });
});

// Send a message and wait for completion
await session.send({ prompt: "What is 2+2?" });
await done;

// Clean up
await session.destroy();
await client.stop();
```

## API Reference

### CopilotClient

#### Constructor

```typescript
new CopilotClient(options?: CopilotClientOptions)
```

**Options:**

- `cliPath?: string` - Path to CLI executable (default: "copilot" from PATH)
- `cliArgs?: string[]` - Extra arguments prepended before SDK-managed flags (e.g. `["./dist-cli/index.js"]` when using `node`)
- `cliUrl?: string` - URL of existing CLI server to connect to (e.g., `"localhost:8080"`, `"http://127.0.0.1:9000"`, or just `"8080"`). When provided, the client will not spawn a CLI process.
- `port?: number` - Server port (default: 0 for random)
- `useStdio?: boolean` - Use stdio transport instead of TCP (default: true)
- `logLevel?: string` - Log level (default: "info")
- `autoStart?: boolean` - Auto-start server (default: true)
- `autoRestart?: boolean` - Auto-restart on crash (default: true)

#### Methods

##### `start(): Promise<void>`

Start the CLI server and establish connection.

##### `stop(): Promise<Error[]>`

Stop the server and close all sessions. Returns a list of any errors encountered during cleanup.

##### `forceStop(): Promise<void>`

Force stop the CLI server without graceful cleanup. Use when `stop()` takes too long.

##### `createSession(config?: SessionConfig): Promise<CopilotSession>`

Create a new conversation session.

**Config:**

- `sessionId?: string` - Custom session ID
- `model?: string` - Model to use ("gpt-5", "claude-sonnet-4.5", etc.)
- `tools?: Tool[]` - Custom tools exposed to the CLI
- `systemMessage?: SystemMessageConfig` - System message customization (see below)

##### `resumeSession(sessionId: string, config?: ResumeSessionConfig): Promise<CopilotSession>`

Resume an existing session.

##### `ping(message?: string): Promise<{ message: string; timestamp: number }>`

Ping the server to check connectivity.

##### `getState(): ConnectionState`

Get current connection state.

##### `listSessions(): Promise<SessionMetadata[]>`

List all available sessions.

##### `deleteSession(sessionId: string): Promise<void>`

Delete a session and its data from disk.

---

### CopilotSession

Represents a single conversation session.

#### Methods

##### `send(options: MessageOptions): Promise<string>`

Send a message to the session. Returns immediately after the message is queued; use event handlers or `sendAndWait()` to wait for completion.

**Options:**

- `prompt: string` - The message/prompt to send
- `attachments?: Array<{type, path, displayName}>` - File attachments
- `mode?: "enqueue" | "immediate"` - Delivery mode

Returns the message ID.

##### `sendAndWait(options: MessageOptions, timeout?: number): Promise<AssistantMessageEvent | undefined>`

Send a message and wait until the session becomes idle.

**Options:**

- `prompt: string` - The message/prompt to send
- `attachments?: Array<{type, path, displayName}>` - File attachments
- `mode?: "enqueue" | "immediate"` - Delivery mode
- `timeout?: number` - Optional timeout in milliseconds

Returns the final assistant message event, or undefined if none was received.

##### `on(handler: SessionEventHandler): () => void`

Subscribe to session events. Returns an unsubscribe function.

```typescript
const unsubscribe = session.on((event) => {
    console.log(event);
});

// Later...
unsubscribe();
```

##### `abort(): Promise<void>`

Abort the currently processing message in this session.

##### `getMessages(): Promise<SessionEvent[]>`

Get all events/messages from this session.

##### `destroy(): Promise<void>`

Destroy the session and free resources.

---

## Event Types

Sessions emit various events during processing:

- `user.message` - User message added
- `assistant.message` - Assistant response
- `assistant.message_delta` - Streaming response chunk
- `tool.execution_start` - Tool execution started
- `tool.execution_end` - Tool execution completed
- And more...

See `SessionEvent` type in the source for full details.

## Image Support

The SDK supports image attachments via the `attachments` parameter. You can attach images by providing their file path:

```typescript
await session.send({
    prompt: "What's in this image?",
    attachments: [
        {
            type: "file",
            path: "/path/to/image.jpg",
        },
    ],
});
```

Supported image formats include JPG, PNG, GIF, and other common image types. The agent's `view` tool can also read images directly from the filesystem, so you can also ask questions like:

```typescript
await session.send({ prompt: "What does the most recent jpg in this directory portray?" });
```

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```typescript
const session = await client.createSession({
    model: "gpt-5",
    streaming: true,
});

// Wait for completion using session.idle event
const done = new Promise<void>((resolve) => {
    session.on((event) => {
        if (event.type === "assistant.message_delta") {
            // Streaming message chunk - print incrementally
            process.stdout.write(event.data.deltaContent);
        } else if (event.type === "assistant.reasoning_delta") {
            // Streaming reasoning chunk (if model supports reasoning)
            process.stdout.write(event.data.deltaContent);
        } else if (event.type === "assistant.message") {
            // Final message - complete content
            console.log("\n--- Final message ---");
            console.log(event.data.content);
        } else if (event.type === "assistant.reasoning") {
            // Final reasoning content (if model supports reasoning)
            console.log("--- Reasoning ---");
            console.log(event.data.content);
        } else if (event.type === "session.idle") {
            // Session finished processing
            resolve();
        }
    });
});

await session.send({ prompt: "Tell me a short story" });
await done; // Wait for streaming to complete
```

When `streaming: true`:

- `assistant.message_delta` events are sent with `deltaContent` containing incremental text
- `assistant.reasoning_delta` events are sent with `deltaContent` for reasoning/chain-of-thought (model-dependent)
- Accumulate `deltaContent` values to build the full response progressively
- The final `assistant.message` and `assistant.reasoning` events contain the complete content

Note: `assistant.message` and `assistant.reasoning` (final events) are always sent regardless of streaming setting.

## Advanced Usage

### Manual Server Control

```typescript
const client = new CopilotClient({ autoStart: false });

// Start manually
await client.start();

// Use client...

// Stop manually
await client.stop();
```

### Tools

You can let the CLI call back into your process when the model needs capabilities you own. Use `defineTool` with Zod schemas for type-safe tool definitions:

```ts
import { z } from "zod";
import { CopilotClient, defineTool } from "@github/copilot-sdk";

const session = await client.createSession({
    model: "gpt-5",
    tools: [
        defineTool("lookup_issue", {
            description: "Fetch issue details from our tracker",
            parameters: z.object({
                id: z.string().describe("Issue identifier"),
            }),
            handler: async ({ id }) => {
                const issue = await fetchIssue(id);
                return issue;
            },
        }),
    ],
});
```

When Copilot invokes `lookup_issue`, the client automatically runs your handler and responds to the CLI. Handlers can return any JSON-serializable value (automatically wrapped), a simple string, or a `ToolResultObject` for full control over result metadata. Raw JSON schemas are also supported if Zod isn't desired.

### System Message Customization

Control the system prompt using `systemMessage` in session config:

```typescript
const session = await client.createSession({
    model: "gpt-5",
    systemMessage: {
        content: `
<workflow_rules>
- Always check for security vulnerabilities
- Suggest performance improvements when applicable
</workflow_rules>
`,
    },
});
```

The SDK auto-injects environment context, tool instructions, and security guardrails. The default CLI persona is preserved, and your `content` is appended after SDK-managed sections. To change the persona or fully redefine the prompt, use `mode: "replace"`.

For full control (removes all guardrails), use `mode: "replace"`:

```typescript
const session = await client.createSession({
    model: "gpt-5",
    systemMessage: {
        mode: "replace",
        content: "You are a helpful assistant.",
    },
});
```

### Multiple Sessions

```typescript
const session1 = await client.createSession({ model: "gpt-5" });
const session2 = await client.createSession({ model: "claude-sonnet-4.5" });

// Both sessions are independent
await session1.sendAndWait({ prompt: "Hello from session 1" });
await session2.sendAndWait({ prompt: "Hello from session 2" });
```

### Custom Session IDs

```typescript
const session = await client.createSession({
    sessionId: "my-custom-session-id",
    model: "gpt-5",
});
```

### File Attachments

```typescript
await session.send({
    prompt: "Analyze this file",
    attachments: [
        {
            type: "file",
            path: "/path/to/file.js",
            displayName: "My File",
        },
    ],
});
```

## Error Handling

```typescript
try {
    const session = await client.createSession();
    await session.send({ prompt: "Hello" });
} catch (error) {
    console.error("Error:", error.message);
}
```

## Requirements

- Node.js >= 18.0.0
- GitHub Copilot CLI installed and in PATH (or provide custom `cliPath`)

## License

MIT
