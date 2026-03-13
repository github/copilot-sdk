# Copilot SDK for Node.js/TypeScript

TypeScript SDK for programmatic control of GitHub Copilot CLI via JSON-RPC.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
npm install @github/copilot-sdk
```

## Run the Sample

Try the interactive chat sample (from the repo root):

```bash
cd nodejs
npm ci
npm run build
cd samples
npm install
npm start
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

// Wait for response using typed event handlers
const done = new Promise<void>((resolve) => {
    session.on("assistant.message", (event) => {
        console.log(event.data.content);
    });
    session.on("session.idle", () => {
        resolve();
    });
});

// Send a message and wait for completion
await session.send({ prompt: "What is 2+2?" });
await done;

// Clean up
await session.disconnect();
await client.stop();
```

Sessions also support `Symbol.asyncDispose` for use with [`await using`](https://github.com/tc39/proposal-explicit-resource-management) (TypeScript 5.2+/Node.js 18.0+):

```typescript
await using session = await client.createSession({ model: "gpt-5" });
// session is automatically disconnected when leaving scope
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
- `githubToken?: string` - GitHub token for authentication. When provided, takes priority over other auth methods.
- `useLoggedInUser?: boolean` - Whether to use logged-in user for authentication (default: true, but false when `githubToken` is provided). Cannot be used with `cliUrl`.
- `telemetry?: TelemetryConfig` - OpenTelemetry configuration for the CLI process. Providing this object enables telemetry — no separate flag needed. See [Telemetry](#telemetry) below.
- `onGetTraceContext?: TraceContextProvider` - Advanced: callback for linking your application's own OpenTelemetry spans into the same distributed trace as the CLI's spans. Not needed for normal telemetry collection. See [Telemetry](#telemetry) below.

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

- `sessionId?: string` - Custom session ID.
- `model?: string` - Model to use ("gpt-5", "claude-sonnet-4.5", etc.). **Required when using custom provider.**
- `reasoningEffort?: "low" | "medium" | "high" | "xhigh"` - Reasoning effort level for models that support it. Use `listModels()` to check which models support this option.
- `tools?: Tool[]` - Custom tools exposed to the CLI
- `systemMessage?: SystemMessageConfig` - System message customization (see below)
- `infiniteSessions?: InfiniteSessionConfig` - Configure automatic context compaction (see below)
- `provider?: ProviderConfig` - Custom API provider configuration (BYOK - Bring Your Own Key). See [Custom Providers](#custom-providers) section.
- `onUserInputRequest?: UserInputHandler` - Handler for user input requests from the agent. Enables the `ask_user` tool. See [User Input Requests](#user-input-requests) section.
- `hooks?: SessionHooks` - Hook handlers for session lifecycle events. See [Session Hooks](#session-hooks) section.

##### `resumeSession(sessionId: string, config?: ResumeSessionConfig): Promise<CopilotSession>`

Resume an existing session. Returns the session with `workspacePath` populated if infinite sessions were enabled.

##### `ping(message?: string): Promise<{ message: string; timestamp: number }>`

Ping the server to check connectivity.

##### `getState(): ConnectionState`

Get current connection state.

##### `listSessions(filter?: SessionListFilter): Promise<SessionMetadata[]>`

List all available sessions. Optionally filter by working directory context.

**SessionMetadata:**

- `sessionId: string` - Unique session identifier
- `startTime: Date` - When the session was created
- `modifiedTime: Date` - When the session was last modified
- `summary?: string` - Optional session summary
- `isRemote: boolean` - Whether the session is remote
- `context?: SessionContext` - Working directory context from session creation

**SessionContext:**

- `cwd: string` - Working directory where the session was created
- `gitRoot?: string` - Git repository root (if in a git repo)
- `repository?: string` - GitHub repository in "owner/repo" format
- `branch?: string` - Current git branch

##### `deleteSession(sessionId: string): Promise<void>`

Delete a session and its data from disk.

##### `getForegroundSessionId(): Promise<string | undefined>`

Get the ID of the session currently displayed in the TUI. Only available when connecting to a server running in TUI+server mode (`--ui-server`).

##### `setForegroundSessionId(sessionId: string): Promise<void>`

Request the TUI to switch to displaying the specified session. Only available in TUI+server mode.

##### `on(eventType: SessionLifecycleEventType, handler): () => void`

Subscribe to a specific session lifecycle event type. Returns an unsubscribe function.

```typescript
const unsubscribe = client.on("session.foreground", (event) => {
    console.log(`Session ${event.sessionId} is now in foreground`);
});
```

##### `on(handler: SessionLifecycleHandler): () => void`

Subscribe to all session lifecycle events. Returns an unsubscribe function.

```typescript
const unsubscribe = client.on((event) => {
    console.log(`${event.type}: ${event.sessionId}`);
});
```

**Lifecycle Event Types:**
- `session.created` - A new session was created
- `session.deleted` - A session was deleted
- `session.updated` - A session was updated (e.g., new messages)
- `session.foreground` - A session became the foreground session in TUI
- `session.background` - A session is no longer the foreground session

---

### CopilotSession

Represents a single conversation session.

#### Properties

##### `sessionId: string`

The unique identifier for this session.

##### `workspacePath?: string`

Path to the session workspace directory when infinite sessions are enabled. Contains `checkpoints/`, `plan.md`, and `files/` subdirectories. Undefined if infinite sessions are disabled.

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

##### `on(eventType: string, handler: TypedSessionEventHandler): () => void`

Subscribe to a specific event type. The handler receives properly typed events.

```typescript
// Listen for specific event types with full type inference
session.on("assistant.message", (event) => {
    console.log(event.data.content); // TypeScript knows about event.data.content
});

session.on("session.idle", () => {
    console.log("Session is idle");
});

// Listen to streaming events
session.on("assistant.message_delta", (event) => {
    process.stdout.write(event.data.deltaContent);
});
```

##### `on(handler: SessionEventHandler): () => void`

Subscribe to all session events. Returns an unsubscribe function.

```typescript
const unsubscribe = session.on((event) => {
    // Handle any event type
    console.log(event.type, event);
});

// Later...
unsubscribe();
```

##### `abort(): Promise<void>`

Abort the currently processing message in this session.

##### `getMessages(): Promise<SessionEvent[]>`

Get all events/messages from this session.

##### `disconnect(): Promise<void>`

Disconnect the session and free resources. Session data on disk is preserved for later resumption.

##### `destroy(): Promise<void>` *(deprecated)*

Deprecated — use `disconnect()` instead.

---

## Event Types

Sessions emit various events during processing:

- `user.message` - User message added
- `assistant.message` - Assistant response
- `assistant.message_delta` - Streaming response chunk
- `tool.execution_start` - Tool execution started
- `tool.execution_complete` - Tool execution completed
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

// Wait for completion using typed event handlers
const done = new Promise<void>((resolve) => {
    session.on("assistant.message_delta", (event) => {
        // Streaming message chunk - print incrementally
        process.stdout.write(event.data.deltaContent);
    });

    session.on("assistant.reasoning_delta", (event) => {
        // Streaming reasoning chunk (if model supports reasoning)
        process.stdout.write(event.data.deltaContent);
    });

    session.on("assistant.message", (event) => {
        // Final message - complete content
        console.log("\n--- Final message ---");
        console.log(event.data.content);
    });

    session.on("assistant.reasoning", (event) => {
        // Final reasoning content (if model supports reasoning)
        console.log("--- Reasoning ---");
        console.log(event.data.content);
    });

    session.on("session.idle", () => {
        // Session finished processing
        resolve();
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

#### Overriding Built-in Tools

If you register a tool with the same name as a built-in CLI tool (e.g. `edit_file`, `read_file`), the SDK will throw an error unless you explicitly opt in by setting `overridesBuiltInTool: true`. This flag signals that you intend to replace the built-in tool with your custom implementation.

```ts
defineTool("edit_file", {
    description: "Custom file editor with project-specific validation",
    parameters: z.object({ path: z.string(), content: z.string() }),
    overridesBuiltInTool: true,
    handler: async ({ path, content }) => { /* your logic */ },
})
```

#### Skipping Permission Prompts

Set `skipPermission: true` on a tool definition to allow it to execute without triggering a permission prompt:

```ts
defineTool("safe_lookup", {
    description: "A read-only lookup that needs no confirmation",
    parameters: z.object({ id: z.string() }),
    skipPermission: true,
    handler: async ({ id }) => { /* your logic */ },
})
```

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

### Infinite Sessions

By default, sessions use **infinite sessions** which automatically manage context window limits through background compaction and persist state to a workspace directory.

```typescript
// Default: infinite sessions enabled with default thresholds
const session = await client.createSession({ model: "gpt-5" });

// Access the workspace path for checkpoints and files
console.log(session.workspacePath);
// => ~/.copilot/session-state/{sessionId}/

// Custom thresholds
const session = await client.createSession({
    model: "gpt-5",
    infiniteSessions: {
        enabled: true,
        backgroundCompactionThreshold: 0.80, // Start compacting at 80% context usage
        bufferExhaustionThreshold: 0.95, // Block at 95% until compaction completes
    },
});

// Disable infinite sessions
const session = await client.createSession({
    model: "gpt-5",
    infiniteSessions: { enabled: false },
});
```

When enabled, sessions emit compaction events:

- `session.compaction_start` - Background compaction started
- `session.compaction_complete` - Compaction finished (includes token counts)

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

### Custom Providers

The SDK supports custom OpenAI-compatible API providers (BYOK - Bring Your Own Key), including local providers like Ollama. When using a custom provider, you must specify the `model` explicitly.

**ProviderConfig:**

- `type?: "openai" | "azure" | "anthropic"` - Provider type (default: "openai")
- `baseUrl: string` - API endpoint URL (required)
- `apiKey?: string` - API key (optional for local providers like Ollama)
- `bearerToken?: string` - Bearer token for authentication (takes precedence over apiKey)
- `wireApi?: "completions" | "responses"` - API format for OpenAI/Azure (default: "completions")
- `azure?.apiVersion?: string` - Azure API version (default: "2024-10-21")

**Example with Ollama:**

```typescript
const session = await client.createSession({
    model: "deepseek-coder-v2:16b", // Required when using custom provider
    provider: {
        type: "openai",
        baseUrl: "http://localhost:11434/v1", // Ollama endpoint
        // apiKey not required for Ollama
    },
});

await session.sendAndWait({ prompt: "Hello!" });
```

**Example with custom OpenAI-compatible API:**

```typescript
const session = await client.createSession({
    model: "gpt-4",
    provider: {
        type: "openai",
        baseUrl: "https://my-api.example.com/v1",
        apiKey: process.env.MY_API_KEY,
    },
});
```

**Example with Azure OpenAI:**

```typescript
const session = await client.createSession({
    model: "gpt-4",
    provider: {
        type: "azure",  // Must be "azure" for Azure endpoints, NOT "openai"
        baseUrl: "https://my-resource.openai.azure.com",  // Just the host, no path
        apiKey: process.env.AZURE_OPENAI_KEY,
        azure: {
            apiVersion: "2024-10-21",
        },
    },
});
```

> **Important notes:**
> - When using a custom provider, the `model` parameter is **required**. The SDK will throw an error if no model is specified.
> - For Azure OpenAI endpoints (`*.openai.azure.com`), you **must** use `type: "azure"`, not `type: "openai"`.
> - The `baseUrl` should be just the host (e.g., `https://my-resource.openai.azure.com`). Do **not** include `/openai/v1` in the URL - the SDK handles path construction automatically.

## Telemetry

The SDK supports OpenTelemetry for distributed tracing. Provide a `telemetry` config to enable trace export from the CLI process — this is all most users need:

```typescript
const client = new CopilotClient({
  telemetry: {
    otlpEndpoint: "http://localhost:4318",
  },
});
```

With just this configuration, the CLI emits spans for every session, message, and tool call to your collector. No additional dependencies or setup required.

**TelemetryConfig options:**

- `otlpEndpoint?: string` - OTLP HTTP endpoint URL
- `filePath?: string` - File path for JSON-lines trace output
- `exporterType?: string` - `"otlp-http"` or `"file"`
- `sourceName?: string` - Instrumentation scope name
- `captureContent?: boolean` - Whether to capture message content

### Advanced: Trace Context Propagation

> **You don't need this for normal telemetry collection.** The `telemetry` config above is sufficient to get full traces from the CLI.

`onGetTraceContext` is only needed if your application creates its own OpenTelemetry spans and you want them to appear in the **same distributed trace** as the CLI's spans — for example, to nest a "handle tool call" span inside the CLI's "execute tool" span, or to show the SDK call as a child of your application's request-handling span.

If you're already using `@opentelemetry/api` in your app and want this linkage, provide a callback:

```typescript
import { propagation, context } from "@opentelemetry/api";

const client = new CopilotClient({
  telemetry: { otlpEndpoint: "http://localhost:4318" },
  onGetTraceContext: () => {
    const carrier: Record<string, string> = {};
    propagation.inject(context.active(), carrier);
    return carrier;
  },
});
```

Inbound trace context from the CLI is available on the `ToolInvocation` object passed to tool handlers as `traceparent` and `tracestate` fields. See the [OpenTelemetry guide](../docs/observability/opentelemetry.md) for a full wire-up example.

## User Input Requests

Enable the agent to ask questions to the user using the `ask_user` tool by providing an `onUserInputRequest` handler:

```typescript
const session = await client.createSession({
    model: "gpt-5",
    onUserInputRequest: async (request, invocation) => {
        // request.question - The question to ask
        // request.choices - Optional array of choices for multiple choice
        // request.allowFreeform - Whether freeform input is allowed (default: true)

        console.log(`Agent asks: ${request.question}`);
        if (request.choices) {
            console.log(`Choices: ${request.choices.join(", ")}`);
        }

        // Return the user's response
        return {
            answer: "User's answer here",
            wasFreeform: true, // Whether the answer was freeform (not from choices)
        };
    },
});
```

## Session Hooks

Hook into session lifecycle events by providing handlers in the `hooks` configuration:

```typescript
const session = await client.createSession({
    model: "gpt-5",
    hooks: {
        // Called before each tool execution
        onPreToolUse: async (input, invocation) => {
            console.log(`About to run tool: ${input.toolName}`);
            // Return permission decision and optionally modify args
            return {
                permissionDecision: "allow", // "allow", "deny", or "ask"
                modifiedArgs: input.toolArgs, // Optionally modify tool arguments
                additionalContext: "Extra context for the model",
            };
        },

        // Called after each tool execution
        onPostToolUse: async (input, invocation) => {
            console.log(`Tool ${input.toolName} completed`);
            // Optionally modify the result or add context
            return {
                additionalContext: "Post-execution notes",
            };
        },

        // Called when user submits a prompt
        onUserPromptSubmitted: async (input, invocation) => {
            console.log(`User prompt: ${input.prompt}`);
            return {
                modifiedPrompt: input.prompt, // Optionally modify the prompt
            };
        },

        // Called when session starts
        onSessionStart: async (input, invocation) => {
            console.log(`Session started from: ${input.source}`); // "startup", "resume", "new"
            return {
                additionalContext: "Session initialization context",
            };
        },

        // Called when session ends
        onSessionEnd: async (input, invocation) => {
            console.log(`Session ended: ${input.reason}`);
        },

        // Called when an error occurs
        onErrorOccurred: async (input, invocation) => {
            console.error(`Error in ${input.errorContext}: ${input.error}`);
            return {
                errorHandling: "retry", // "retry", "skip", or "abort"
            };
        },
    },
});
```

**Available hooks:**

- `onPreToolUse` - Intercept tool calls before execution. Can allow/deny or modify arguments.
- `onPostToolUse` - Process tool results after execution. Can modify results or add context.
- `onUserPromptSubmitted` - Intercept user prompts. Can modify the prompt before processing.
- `onSessionStart` - Run logic when a session starts or resumes.
- `onSessionEnd` - Cleanup or logging when session ends.
- `onErrorOccurred` - Handle errors with retry/skip/abort strategies.

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
