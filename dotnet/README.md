# Copilot SDK

SDK for programmatic control of GitHub Copilot CLI.

## Installation

```bash
dotnet add package GitHub.Copilot.SDK
```

## Run the Samples

Try the interactive chat sample (from the repo root):

```bash
dotnet run --file dotnet/samples/Chat.cs
```

The manual permission/tool-result resume sample can be run the same way:

```bash
dotnet run --file dotnet/samples/ManualToolResume.cs
```

## Quick Start

```csharp
using GitHub.Copilot;

// Create and start client
await using var client = new CopilotClient();
await client.StartAsync();

// Create a session (OnPermissionRequest is optional; ApproveAll allows every tool)
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnPermissionRequest = PermissionHandler.ApproveAll,
});

// Wait for the response using the session.idle event
var done = new TaskCompletionSource();

session.On<SessionEvent>(evt =>
{
    if (evt is AssistantMessageEvent msg)
    {
        Console.WriteLine(msg.Data.Content);
    }
    else if (evt is SessionIdleEvent)
    {
        done.SetResult();
    }
});

// Send a message and wait for completion
await session.SendAsync(new MessageOptions { Prompt = "What is 2+2?" });
await done.Task;
```

## API Reference

### CopilotClient

#### Constructor

```csharp
new CopilotClient(CopilotClientOptions? options = null)
```

**Options:**

- `Connection` - How to connect to the Copilot runtime. Defaults to `null` (equivalent to `RuntimeConnection.ForStdio()` with the bundled runtime). See "RuntimeConnection" below.
- `LogLevel` - Runtime log level. Accepts well-known values `CopilotLogLevel.None`, `Error`, `Warning`, `Info`, `Debug`, `All`. Defaults to null (the runtime's own default).
- `WorkingDirectory` - Working directory for the runtime process.
- `BaseDirectory` - Base directory for Copilot data (session state, config, etc.). Sets `COPILOT_HOME` on the spawned runtime process. When not set, the runtime defaults to `~/.copilot`. Useful in restricted environments where only specific directories are writable. Ignored when connecting via `RuntimeConnection.ForUri(...)`.
- `EnableRemoteSessions` - Enables remote-session features.
- `Environment` - Environment variables to pass to the runtime process.
- `Logger` - `ILogger` instance for SDK logging.
- `GitHubToken` - GitHub token for authentication. When provided, takes priority over other auth methods.
- `UseLoggedInUser` - Whether to use logged-in user for authentication (default: true, but false when `GitHubToken` is provided). Cannot be used with `RuntimeConnection.ForUri(...)`.
- `Telemetry` - OpenTelemetry configuration for the runtime process. Providing this enables telemetry — no separate flag needed. See [Telemetry](#telemetry) below.

#### RuntimeConnection

`CopilotClientOptions.Connection` describes how the SDK reaches a Copilot runtime. There are three flavors, all constructed via static factories:

- `RuntimeConnection.ForStdio(path?, args?)` — spawns the runtime as a child process and communicates over stdio. This is the default when `Connection` is null.
- `RuntimeConnection.ForTcp(port = 0, connectionToken?, path?, args?)` — spawns the runtime as a child process listening on a TCP port. `port = 0` auto-allocates; if a non-zero port is already in use, startup fails (no fallback). Use `CopilotClient.RuntimePort` after `StartAsync` to read the assigned port. `connectionToken` is required if other clients will connect via `RuntimeConnection.ForUri(...)`.
- `RuntimeConnection.ForUri(url, connectionToken?)` — connects to an already-running runtime at `url` (e.g., `"localhost:8080"`). Does not spawn a process.

#### Methods

##### `StartAsync(): Task`

Start the CLI server and establish connection.

##### `StopAsync(): Task`

Stop the server and close all sessions. Throws if errors are encountered during cleanup.

##### `ForceStopAsync(): Task`

Force stop the CLI server without graceful cleanup. Use when `StopAsync()` takes too long.

##### `CreateSessionAsync(SessionConfig? config = null): Task<CopilotSession>`

Create a new conversation session.

**Config:**

- `SessionId` - Custom session ID
- `Model` - Model to use ("gpt-5", "claude-sonnet-4.5", etc.)
- `ReasoningEffort` - Reasoning effort level for models that support it ("low", "medium", "high", "xhigh"). Use `ListModelsAsync()` to check which models support this option.
- `Tools` - Custom tool declarations exposed to the CLI. Declarations without an invocable `AIFunction` are left pending for manual resolution.
- `SystemMessage` - System message customization
- `AvailableTools` - List of tool names to allow
- `ExcludedTools` - List of tool names to disable
- `Provider` - Custom API provider configuration (BYOK)
- `Streaming` - Enable streaming of response chunks (default: false)
- `InfiniteSessions` - Configure automatic context compaction (see below)
- `OnPermissionRequest` - Optional handler called before each tool execution to approve or deny it. When omitted, permission requests are emitted as events and left pending for manual resolution. Use `PermissionHandler.ApproveAll` to allow everything, or provide a custom function for fine-grained control. See [Permission Handling](#permission-handling) section.
- `OnUserInputRequest` - Handler for user input requests from the agent (enables ask_user tool). See [User Input Requests](#user-input-requests) section.
- `Hooks` - Hook handlers for session lifecycle events. See [Session Hooks](#session-hooks) section.

##### `ResumeSessionAsync(string sessionId, ResumeSessionConfig? config = null): Task<CopilotSession>`

Resume an existing session. Returns the session with `WorkspacePath` populated if infinite sessions were enabled.

**ResumeSessionConfig:**

- `OnPermissionRequest` - Optional handler called before each tool execution to approve or deny it. See [Permission Handling](#permission-handling) section.

##### `PingAsync(string? message = null): Task<PingResponse>`

Ping the server to check connectivity.

##### `State: ConnectionState`

Get current connection state.

##### `ListSessionsAsync(): Task<IList<SessionMetadata>>`

List all available sessions.

##### `DeleteSessionAsync(string sessionId): Task`

Delete a session and its data from disk.

##### `GetForegroundSessionIdAsync(): Task<string?>`

Get the ID of the session currently displayed in the TUI. Only available when connecting to a server running in TUI+server mode (`--ui-server`).

##### `SetForegroundSessionIdAsync(string sessionId): Task`

Request the TUI to switch to displaying the specified session. Only available in TUI+server mode.

##### `OnLifecycle<T>(Action<T> handler): IDisposable where T : SessionLifecycleEvent`

Subscribe to session lifecycle events. Pass a derived type to filter by kind, or `SessionLifecycleEvent` to receive every lifecycle event. Returns an `IDisposable` that unsubscribes when disposed.

```csharp
// Receive every lifecycle event:
using var subscription = client.OnLifecycle<SessionLifecycleEvent>(evt =>
{
    Console.WriteLine($"Session {evt.SessionId}: {evt.Type}");
});

// Only receive foreground events:
using var foreground = client.OnLifecycle<SessionForegroundEvent>(evt =>
{
    Console.WriteLine($"Session {evt.SessionId} is now in foreground");
});
```

**Lifecycle Event Types:**

- `SessionCreatedEvent` — A new session was created
- `SessionDeletedEvent` — A session was deleted
- `SessionUpdatedEvent` — A session was updated
- `SessionForegroundEvent` — A session became the foreground session in TUI
- `SessionBackgroundEvent` — A session is no longer the foreground session

---

### CopilotSession

Represents a single conversation session.

#### Properties

- `SessionId` - The unique identifier for this session
- `WorkspacePath` - Path to the session workspace directory when infinite sessions are enabled. Contains `checkpoints/`, `plan.md`, and `files/` subdirectories. Null if infinite sessions are disabled.

#### Methods

##### `SendAsync(MessageOptions options): Task<string>`

Send a message to the session.

**Options:**

- `Prompt` - The message/prompt to send
- `Attachments` - File attachments
- `Mode` - Delivery mode ("enqueue" or "immediate")

Returns the message ID.

##### `On(Action<SessionEvent> handler): IDisposable`

Subscribe to session events. Returns a disposable to unsubscribe.

```csharp
var subscription = session.On<SessionEvent>(evt =>
{
    Console.WriteLine($"Event: {evt.Type}");
});

// Later...
subscription.Dispose();
```

##### `AbortAsync(): Task`

Abort the currently processing message in this session.

##### `GetEventsAsync(): Task<IReadOnlyList<SessionEvent>>`

Get all events/messages from this session.

##### `DisposeAsync(): ValueTask`

Close the session and release in-memory resources. Session data on disk is preserved — the conversation can be resumed later via `ResumeSessionAsync()`. To permanently delete session data, use `client.DeleteSessionAsync()`.

```csharp
// Preferred: automatic cleanup via await using
await using var session = await client.CreateSessionAsync(config);
// session is automatically disposed when leaving scope

// Alternative: explicit dispose
var session2 = await client.CreateSessionAsync(config);
await session2.DisposeAsync();
```

---

## Event Types

Sessions emit various events during processing. Each event type is a class that inherits from `SessionEvent`:

- `UserMessageEvent` - User message added
- `AssistantMessageEvent` - Assistant response
- `ToolExecutionStartEvent` - Tool execution started
- `ToolExecutionCompleteEvent` - Tool execution completed
- `SessionStartEvent` - Session started
- `SessionIdleEvent` - Session is idle
- `SessionErrorEvent` - Session error occurred
- And more...

Use pattern matching to handle specific event types:

```csharp
session.On<SessionEvent>(evt =>
{
    switch (evt)
    {
        case AssistantMessageEvent msg:
            Console.WriteLine(msg.Data.Content);
            break;
        case SessionErrorEvent err:
            Console.WriteLine($"Error: {err.Data.Message}");
            break;
    }
});
```

## Image Support

The SDK supports image attachments via the `Attachments` parameter. You can attach images by providing their file path, or by passing base64-encoded data directly using a blob attachment:

```csharp
// File attachment — runtime reads from disk
await session.SendAsync(new MessageOptions
{
    Prompt = "What's in this image?",
    Attachments = new List<UserMessageDataAttachmentsItem>
    {
        new UserMessageDataAttachmentsItemFile
        {
            Path = "/path/to/image.jpg",
            DisplayName = "image.jpg",
        }
    }
});

// Blob attachment — provide base64 data directly
await session.SendAsync(new MessageOptions
{
    Prompt = "What's in this image?",
    Attachments = new List<UserMessageDataAttachmentsItem>
    {
        new UserMessageDataAttachmentsItemBlob
        {
            Data = base64ImageData,
            MimeType = "image/png",
        }
    }
});
```

Supported image formats include JPG, PNG, GIF, and other common image types. The agent's `view` tool can also read images directly from the filesystem, so you can also ask questions like:

```csharp
await session.SendAsync(new MessageOptions { Prompt = "What does the most recent jpg in this directory portray?" });
```

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    Streaming = true
});

// Use TaskCompletionSource to wait for completion
var done = new TaskCompletionSource();

session.On<SessionEvent>(evt =>
{
    switch (evt)
    {
        case AssistantMessageDeltaEvent delta:
            // Streaming message chunk - print incrementally
            Console.Write(delta.Data.DeltaContent);
            break;
        case AssistantReasoningDeltaEvent reasoningDelta:
            // Streaming reasoning chunk (if model supports reasoning)
            Console.Write(reasoningDelta.Data.DeltaContent);
            break;
        case AssistantMessageEvent msg:
            // Final message - complete content
            Console.WriteLine("\n--- Final message ---");
            Console.WriteLine(msg.Data.Content);
            break;
        case AssistantReasoningEvent reasoningEvt:
            // Final reasoning content (if model supports reasoning)
            Console.WriteLine("--- Reasoning ---");
            Console.WriteLine(reasoningEvt.Data.Content);
            break;
        case SessionIdleEvent:
            // Session finished processing
            done.SetResult();
            break;
    }
});

await session.SendAsync(new MessageOptions { Prompt = "Tell me a short story" });
await done.Task; // Wait for streaming to complete
```

When `Streaming = true`:

- `AssistantMessageDeltaEvent` events are sent with `DeltaContent` containing incremental text
- `AssistantReasoningDeltaEvent` events are sent with `DeltaContent` for reasoning/chain-of-thought (model-dependent)
- Accumulate `DeltaContent` values to build the full response progressively
- The final `AssistantMessageEvent` and `AssistantReasoningEvent` events contain the complete content

Note: `AssistantMessageEvent` and `AssistantReasoningEvent` (final events) are always sent regardless of streaming setting.

## Infinite Sessions

By default, sessions use **infinite sessions** which automatically manage context window limits through background compaction and persist state to a workspace directory.

```csharp
// Default: infinite sessions enabled with default thresholds
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5"
});

// Access the workspace path for checkpoints and files
Console.WriteLine(session.WorkspacePath);
// => ~/.copilot/session-state/{sessionId}/

// Custom thresholds
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    InfiniteSessions = new InfiniteSessionConfig
    {
        Enabled = true,
        BackgroundCompactionThreshold = 0.80, // Start compacting at 80% context usage
        BufferExhaustionThreshold = 0.95      // Block at 95% until compaction completes
    }
});

// Disable infinite sessions
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    InfiniteSessions = new InfiniteSessionConfig { Enabled = false }
});
```

When enabled, sessions emit compaction events:

- `SessionCompactionStartEvent` - Background compaction started
- `SessionCompactionCompleteEvent` - Compaction finished (includes token counts)

## Advanced Usage

### Manual Server Control

```csharp
var client = new CopilotClient(new CopilotClientOptions { AutoStart = false });

// Start manually
await client.StartAsync();

// Use client...

// Stop manually
await client.StopAsync();
```

### Tools

You can let the CLI call back into your process when the model needs capabilities you own. Use `CopilotTool.DefineTool` for type-safe tool definitions:

```csharp
using Microsoft.Extensions.AI;
using System.ComponentModel;

var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    Tools = [
        CopilotTool.DefineTool(
            async ([Description("Issue identifier")] string id) => {
                var issue = await FetchIssueAsync(id);
                return issue;
            },
            factoryOptions: new AIFunctionFactoryOptions
            {
                Name = "lookup_issue",
                Description = "Fetch issue details from our tracker",
            }),
    ]
});
```

When Copilot invokes `lookup_issue`, the client automatically runs your handler and responds to the CLI. Handlers can return any JSON-serializable value (automatically wrapped), or a `ToolResultAIContent` wrapping a `ToolResultObject` for full control over result metadata. Include a `ToolInvocation` parameter in your handler if you need the session ID, tool call ID, tool name, or raw arguments.

#### Overriding Built-in Tools

If you register a tool with the same name as a built-in CLI tool (e.g. `edit_file`, `read_file`), the runtime will return an error unless you explicitly opt in with `CopilotToolOptions.OverridesBuiltInTool`. This flag signals that you intend to replace the built-in tool with your custom implementation.

```csharp
var editFile = CopilotTool.DefineTool(
    async ([Description("File path")] string path, [Description("New content")] string content) => {
        // your logic
    },
    toolOptions: new CopilotToolOptions
    {
        OverridesBuiltInTool = true
    },
    factoryOptions: new AIFunctionFactoryOptions
    {
        Name = "edit_file",
        Description = "Custom file editor with project-specific validation",
    });

var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    Tools = [editFile],
});
```

#### Skipping Permission Prompts

Set `CopilotToolOptions.SkipPermission` to allow a tool to execute without triggering a permission prompt:

```csharp
var safeLookup = CopilotTool.DefineTool(
    async ([Description("Lookup ID")] string id) => {
        // your logic
    },
    toolOptions: new CopilotToolOptions
    {
        SkipPermission = true
    },
    factoryOptions: new AIFunctionFactoryOptions
    {
        Name = "safe_lookup",
        Description = "A read-only lookup that needs no confirmation",
    });
```

`DefineTool` delegates to `AIFunctionFactory.Create`, so advanced `AIFunctionFactoryOptions` remain available through the overload that accepts both `AIFunctionFactoryOptions` and `CopilotToolOptions`.

If you want to use `AIFunctionFactory.Create` directly, you can set `skip_permission` in the tool's `AdditionalProperties`.

#### Deferring Tools

Set `CopilotToolOptions.Defer` to control whether a tool may be loaded lazily via tool search rather than always pre-loaded. Use `CopilotToolDefer.Auto` to allow the tool to be deferred and surfaced through tool search, or `CopilotToolDefer.Never` to force it to always be pre-loaded. Defaults to `CopilotToolDefer.Auto`.

```csharp
var lookupIssue = CopilotTool.DefineTool(
    async ([Description("Issue ID")] string id) => {
        // your logic
    },
    toolOptions: new CopilotToolOptions
    {
        Defer = CopilotToolDefer.Auto
    },
    factoryOptions: new AIFunctionFactoryOptions
    {
        Name = "lookup_issue",
        Description = "Fetch issue details",
    });
```

## Commands

Register slash commands so that users of the CLI's TUI can invoke custom actions via `/commandName`. Each command has a `Name`, optional `Description`, and a `Handler` called when the user executes it.

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnPermissionRequest = PermissionHandler.ApproveAll,
    Commands =
    [
        new CommandDefinition
        {
            Name = "deploy",
            Description = "Deploy the app to production",
            Handler = async (context) =>
            {
                Console.WriteLine($"Deploying with args: {context.Args}");
                // Do work here — any thrown error is reported back to the CLI
            },
        },
    ],
});
```

When the user types `/deploy staging` in the CLI, the SDK receives a `command.execute` event, routes it to your handler, and automatically responds to the CLI. If the handler throws, the error message is forwarded.

Commands are sent to the CLI on both `CreateSessionAsync` and `ResumeSessionAsync`, so you can update the command set when resuming.

## UI Elicitation

When the session has elicitation support — either from the CLI's TUI or from another client that registered an `OnElicitationRequest` handler (see [Elicitation Requests](#elicitation-requests)) — the SDK can request interactive form dialogs from the user. The `session.Ui` object provides convenience methods built on a single generic elicitation RPC.

> **Capability check:** Elicitation is only available when at least one connected participant advertises support. Always check `session.Capabilities.Ui?.Elicitation` before calling UI methods — this property updates automatically as participants join and leave.

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnPermissionRequest = PermissionHandler.ApproveAll,
});

if (session.Capabilities.Ui?.Elicitation == true)
{
    // Confirm dialog — returns boolean
    bool ok = await session.Ui.ConfirmAsync("Deploy to production?");

    // Selection dialog — returns selected value or null
    string? env = await session.Ui.SelectAsync("Pick environment",
        ["production", "staging", "dev"]);

    // Text input — returns string or null
    string? name = await session.Ui.InputAsync("Project name:", new UiInputOptions
    {
        Title = "Name",
        MinLength = 1,
        MaxLength = 50,
    });

    // Generic elicitation with full schema control
    ElicitationResult result = await session.Ui.ElicitAsync(new ElicitationParams
    {
        Message = "Configure deployment",
        RequestedSchema = new ElicitationSchema
        {
            Type = "object",
            Properties = new Dictionary<string, object>
            {
                ["region"] = new Dictionary<string, object>
                {
                    ["type"] = "string",
                    ["enum"] = new[] { "us-east", "eu-west" },
                },
                ["dryRun"] = new Dictionary<string, object>
                {
                    ["type"] = "boolean",
                    ["default"] = true,
                },
            },
            Required = ["region"],
        },
    });
    // result.Action: Accept, Decline, or Cancel
    // result.Content: { "region": "us-east", "dryRun": true } (when accepted)
}
```

All UI methods throw if elicitation is not supported by the host.

### System Message Customization

Control the system prompt using `SystemMessage` in session config:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    SystemMessage = new SystemMessageConfig
    {
        Mode = SystemMessageMode.Append,
        Content = @"
<workflow_rules>
- Always check for security vulnerabilities
- Suggest performance improvements when applicable
</workflow_rules>
"
    }
});
```

#### Customize Mode

Use `Mode = SystemMessageMode.Customize` to selectively override individual sections of the prompt while preserving the rest:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    SystemMessage = new SystemMessageConfig
    {
        Mode = SystemMessageMode.Customize,
        Sections = new Dictionary<SystemMessageSection, SectionOverride>
        {
            [SystemMessageSection.Tone] = new() { Action = SectionOverrideAction.Replace, Content = "Respond in a warm, professional tone. Be thorough in explanations." },
            [SystemMessageSection.CodeChangeRules] = new() { Action = SectionOverrideAction.Remove },
            [SystemMessageSection.Guidelines] = new() { Action = SectionOverrideAction.Append, Content = "\n* Always cite data sources" },
        },
        Content = "Focus on financial analysis and reporting."
    }
});
```

Available section IDs are defined as static properties on the `SystemMessageSection` struct: `Identity`, `Tone`, `ToolEfficiency`, `EnvironmentContext`, `CodeChangeRules`, `Guidelines`, `Safety`, `ToolInstructions`, `CustomInstructions`, `RuntimeInstructions`, `LastInstructions`.

Each section override supports four actions: `Replace`, `Remove`, `Append`, and `Prepend`. Unknown section IDs are handled gracefully: content is appended to additional instructions, and `Remove` overrides are silently ignored.

#### Replace Mode

For full control (removes all guardrails), use `Mode = SystemMessageMode.Replace`:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    SystemMessage = new SystemMessageConfig
    {
        Mode = SystemMessageMode.Replace,
        Content = "You are a helpful assistant."
    }
});
```

### Multiple Sessions

```csharp
var session1 = await client.CreateSessionAsync(new SessionConfig { Model = "gpt-5" });
var session2 = await client.CreateSessionAsync(new SessionConfig { Model = "claude-sonnet-4.5" });

// Both sessions are independent
await session1.SendAsync(new MessageOptions { Prompt = "Hello from session 1" });
await session2.SendAsync(new MessageOptions { Prompt = "Hello from session 2" });
```

### File Attachments

```csharp
await session.SendAsync(new MessageOptions
{
    Prompt = "Analyze this file",
    Attachments = new List<UserMessageDataAttachmentsItem>
    {
        new UserMessageDataAttachmentsItem
        {
            Type = UserMessageDataAttachmentsItemType.File,
            Path = "/path/to/file.cs",
            DisplayName = "My File"
        }
    }
});
```

### Bring Your Own Key (BYOK)

Use a custom API provider:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Provider = new ProviderConfig
    {
        Type = "openai",
        BaseUrl = "https://api.openai.com/v1",
        ApiKey = "your-api-key"
    }
});
```

## Telemetry

The SDK supports OpenTelemetry for distributed tracing. Provide a `Telemetry` config to enable trace export and automatic W3C Trace Context propagation.

```csharp
var client = new CopilotClient(new CopilotClientOptions
{
    Telemetry = new TelemetryConfig
    {
        OtlpEndpoint = "http://localhost:4318",
    },
});
```

**TelemetryConfig properties:**

- `OtlpEndpoint` - OTLP HTTP endpoint URL
- `OtlpProtocol` - OTLP HTTP protocol for all signals (`"http/json"` or `"http/protobuf"`)
- `FilePath` - File path for JSON-lines trace output
- `ExporterType` - `"otlp-http"` or `"file"`
- `SourceName` - Instrumentation scope name
- `CaptureContent` - Whether to capture message content

Trace context (`traceparent`/`tracestate`) is automatically propagated between the SDK and CLI on `CreateSessionAsync`, `ResumeSessionAsync`, and `SendAsync` calls, and inbound when the CLI invokes tool handlers.

No extra dependencies — uses built-in `System.Diagnostics.Activity`.

## Permission Handling

An `OnPermissionRequest` handler is optional when you create or resume a session. When provided, it is called before the agent executes each tool (file writes, shell commands, custom tools, etc.) and returns a decision. When omitted, permission requests are emitted as events and left pending for the consumer to resolve with the pending permission RPC.

### Approve All (simplest)

Use the built-in `PermissionHandler.ApproveAll` helper to allow every tool call without any checks:

```csharp
using GitHub.Copilot;

var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnPermissionRequest = PermissionHandler.ApproveAll,
});
```

### Custom Permission Handler

Provide your own permission handler (`Func<PermissionRequest, PermissionInvocation, Task<PermissionDecision>>`) to inspect each request and apply custom logic:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnPermissionRequest = async (request, invocation) =>
    {
        // Pattern-match on the discriminated PermissionRequest union to access
        // per-kind fields (FullCommandText, Path, ToolName, …).
        return request switch
        {
            PermissionRequestShell s => PermissionDecision.Reject($"Refusing shell: {s.FullCommandText}"),
            _ => PermissionDecision.ApproveOnce(),
        };
    }
});
```

### Permission Decisions

The handler returns a `PermissionDecision`. Use the static factories for common cases (returned types are the strongly-typed variant classes — full IntelliSense via `PermissionDecision.<dot>`):

| Factory                                | Meaning                                                                                      |
| -------------------------------------- | -------------------------------------------------------------------------------------------- |
| `PermissionDecision.ApproveOnce()`     | Allow this single request                                                                    |
| `PermissionDecision.Reject(feedback)`  | Deny the request, optionally forwarding feedback to the LLM                                  |
| `PermissionDecision.UserNotAvailable()`| Deny the request because no user is available to confirm it                                  |
| `PermissionDecision.NoResult()`        | Decline to respond, allowing another connected client to answer instead                      |

For richer decisions that need an `Approval` payload — `PermissionDecisionApproveForSession`, `PermissionDecisionApproveForLocation`, `PermissionDecisionApprovePermanently` — instantiate the variant class directly.

### Resuming Sessions

You may pass `OnPermissionRequest` when resuming a session too:

```csharp
var session = await client.ResumeSessionAsync("session-id", new ResumeSessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll,
});
```

### Per-Tool Skip Permission

To let a specific custom tool bypass the permission prompt entirely, set `SkipPermission = true` in `CopilotToolOptions`. See [Skipping Permission Prompts](#skipping-permission-prompts) under Tools.

## User Input Requests

Enable the agent to ask questions to the user using the `ask_user` tool by providing an `OnUserInputRequest` handler:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnUserInputRequest = async (request, invocation) =>
    {
        // request.Question - The question to ask
        // request.Choices - Optional list of choices for multiple choice
        // request.AllowFreeform - Whether freeform input is allowed (default: true)

        Console.WriteLine($"Agent asks: {request.Question}");
        if (request.Choices?.Count > 0)
        {
            Console.WriteLine($"Choices: {string.Join(", ", request.Choices)}");
        }

        // Return the user's response
        return new UserInputResponse
        {
            Answer = "User's answer here",
            WasFreeform = true // Whether the answer was freeform (not from choices)
        };
    }
});
```

## Session Hooks

Hook into session lifecycle events by providing handlers in the `Hooks` configuration:

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    Hooks = new SessionHooks
    {
        // Called before each tool execution
        OnPreToolUse = async (input, invocation) =>
        {
            Console.WriteLine($"About to run tool: {input.ToolName}");
            // Return permission decision and optionally modify args
            return new PreToolUseHookOutput
            {
                PermissionDecision = "allow", // "allow", "deny", or "ask"
                ModifiedArgs = input.ToolArgs, // Optionally modify tool arguments
                AdditionalContext = "Extra context for the model"
            };
        },

        // Called after each tool execution
        OnPostToolUse = async (input, invocation) =>
        {
            Console.WriteLine($"Tool {input.ToolName} completed");
            return new PostToolUseHookOutput
            {
                AdditionalContext = "Post-execution notes"
            };
        },

        // Called when a tool execution result was a failure. OnPostToolUse only
        // fires on success, so register OnPostToolUseFailure to observe failed
        // tool calls. The CLI extracts the failure message and passes it as
        // input.Error.
        OnPostToolUseFailure = async (input, invocation) =>
        {
            Console.WriteLine($"Tool {input.ToolName} failed: {input.Error}");
            return new PostToolUseFailureHookOutput
            {
                AdditionalContext = $"Retry guidance for {input.ToolName}"
            };
        },

        // Called when user submits a prompt
        OnUserPromptSubmitted = async (input, invocation) =>
        {
            Console.WriteLine($"User prompt: {input.Prompt}");
            return new UserPromptSubmittedHookOutput
            {
                ModifiedPrompt = input.Prompt // Optionally modify the prompt
            };
        },

        // Called when session starts
        OnSessionStart = async (input, invocation) =>
        {
            Console.WriteLine($"Session started from: {input.Source}"); // "startup", "resume", "new"
            return new SessionStartHookOutput
            {
                AdditionalContext = "Session initialization context"
            };
        },

        // Called when session ends
        OnSessionEnd = async (input, invocation) =>
        {
            Console.WriteLine($"Session ended: {input.Reason}");
            return null;
        },

        // Called when an error occurs
        OnErrorOccurred = async (input, invocation) =>
        {
            Console.WriteLine($"Error in {input.ErrorContext}: {input.Error}");
            return new ErrorOccurredHookOutput
            {
                ErrorHandling = "retry" // "retry", "skip", or "abort"
            };
        }
    }
});
```

**Available hooks:**

- `OnPreToolUse` - Intercept tool calls before execution. Can allow/deny or modify arguments.
- `OnPostToolUse` - Process tool results after successful execution. Can modify results or add context.
- `OnPostToolUseFailure` - Observe failed tool executions and inject extra context to guide the model's next step.
- `OnUserPromptSubmitted` - Intercept user prompts. Can modify the prompt before processing.
- `OnSessionStart` - Run logic when a session starts or resumes.
- `OnSessionEnd` - Cleanup or logging when session ends.
- `OnErrorOccurred` - Handle errors with retry/skip/abort strategies.

## Elicitation Requests

Register an `OnElicitationRequest` handler to let your client act as an elicitation provider — presenting form-based UI dialogs on behalf of the agent. When provided, the server notifies your client whenever a tool or MCP server needs structured user input.

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    OnPermissionRequest = PermissionHandler.ApproveAll,
    OnElicitationRequest = async (context) =>
    {
        // context.SessionId - Session that triggered the request
        // context.Message - Description of what information is needed
        // context.RequestedSchema - JSON Schema describing the form fields
        // context.Mode - "form" (structured input) or "url" (browser redirect)
        // context.ElicitationSource - Origin of the request (e.g. MCP server name)

        Console.WriteLine($"Elicitation from {context.ElicitationSource}: {context.Message}");

        // Present UI to the user and collect their response...
        return new ElicitationResult
        {
            Action = SessionUiElicitationResultAction.Accept,
            Content = new Dictionary<string, object>
            {
                ["region"] = "us-east",
                ["dryRun"] = true,
            },
        };
    },
});

// The session now reports elicitation capability
Console.WriteLine(session.Capabilities.Ui?.Elicitation); // True
```

When `OnElicitationRequest` is provided, the SDK sends `RequestElicitation = true` during session create/resume, which enables `session.Capabilities.Ui.Elicitation` on the session.

In multi-client scenarios:

- If no connected client was previously providing an elicitation capability, but a new client joins that can, all clients will receive a `capabilities.changed` event to notify them that elicitation is now possible. The SDK automatically updates `session.Capabilities` when these events arrive.
- Similarly, if the last elicitation provider disconnects, all clients receive a `capabilities.changed` event indicating elicitation is no longer available.
- The server fans out elicitation requests to **all** connected clients that registered a handler — the first response wins.

## Error Handling

```csharp
try
{
    var session = await client.CreateSessionAsync();
    await session.SendAsync(new MessageOptions { Prompt = "Hello" });
}
catch (IOException ex)
{
    Console.Error.WriteLine($"Communication Error: {ex.Message}");
}
catch (Exception ex)
{
    Console.Error.WriteLine($"Error: {ex.Message}");
}
```

## Requirements

- .NET 8.0 or later
- GitHub Copilot CLI installed and in PATH (or provide custom `Connection = RuntimeConnection.ForStdio(path: ...)`)

## License

MIT
