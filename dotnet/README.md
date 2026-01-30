# Copilot SDK

SDK for programmatic control of GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
dotnet add package GitHub.Copilot.SDK
```

## Quick Start

```csharp
using GitHub.Copilot.SDK;

// Create and start client
await using var client = new CopilotClient();
await client.StartAsync();

// Create a session
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5"
});

// Wait for response using session.idle event
var done = new TaskCompletionSource();

session.On(evt =>
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

- `CliPath` - Path to CLI executable (default: "copilot" from PATH)
- `CliArgs` - Extra arguments prepended before SDK-managed flags
- `CliUrl` - URL of existing CLI server to connect to (e.g., `"localhost:8080"`). When provided, the client will not spawn a CLI process.
- `Port` - Server port (default: 0 for random)
- `UseStdio` - Use stdio transport instead of TCP (default: true)
- `LogLevel` - Log level (default: "info")
- `AutoStart` - Auto-start server (default: true)
- `AutoRestart` - Auto-restart on crash (default: true)
- `Cwd` - Working directory for the CLI process
- `Environment` - Environment variables to pass to the CLI process
- `Logger` - `ILogger` instance for SDK logging
- `GithubToken` - GitHub token for authentication. When provided, takes priority over other auth methods.
- `UseLoggedInUser` - Whether to use logged-in user for authentication (default: true, but false when `GithubToken` is provided). Cannot be used with `CliUrl`.

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
- `Tools` - Custom tools exposed to the CLI
- `SystemMessage` - System message customization
- `AvailableTools` - List of tool names to allow
- `ExcludedTools` - List of tool names to disable
- `Provider` - Custom API provider configuration (BYOK)
- `Streaming` - Enable streaming of response chunks (default: false)
- `InfiniteSessions` - Configure automatic context compaction (see below)
- `OnUserInputRequest` - Handler for user input requests from the agent (enables ask_user tool). See [User Input Requests](#user-input-requests) section.
- `Hooks` - Hook handlers for session lifecycle events. See [Session Hooks](#session-hooks) section.

##### `ResumeSessionAsync(string sessionId, ResumeSessionConfig? config = null): Task<CopilotSession>`

Resume an existing session. Returns the session with `WorkspacePath` populated if infinite sessions were enabled.

##### `PingAsync(string? message = null): Task<PingResponse>`

Ping the server to check connectivity.

##### `State: ConnectionState`

Get current connection state.

##### `ListSessionsAsync(): Task<List<SessionMetadata>>`

List all available sessions.

##### `DeleteSessionAsync(string sessionId): Task`

Delete a session and its data from disk.

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

##### `On(SessionEventHandler handler): IDisposable`

Subscribe to session events. Returns a disposable to unsubscribe.

```csharp
var subscription = session.On(evt =>
{
    Console.WriteLine($"Event: {evt.Type}");
});

// Later...
subscription.Dispose();
```

##### `AbortAsync(): Task`

Abort the currently processing message in this session.

##### `GetMessagesAsync(): Task<IReadOnlyList<SessionEvent>>`

Get all events/messages from this session.

##### `DisposeAsync(): ValueTask`

Dispose the session and free resources.

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
session.On(evt =>
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

The SDK supports image attachments via the `Attachments` parameter. You can attach images by providing their file path:

```csharp
await session.SendAsync(new MessageOptions
{
    Prompt = "What's in this image?",
    Attachments = new List<UserMessageDataAttachmentsItem>
    {
        new UserMessageDataAttachmentsItem
        {
            Type = UserMessageDataAttachmentsItemType.File,
            Path = "/path/to/image.jpg"
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

session.On(evt =>
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

You can let the CLI call back into your process when the model needs capabilities you own. Use `AIFunctionFactory.Create` from Microsoft.Extensions.AI for type-safe tool definitions:

```csharp
using Microsoft.Extensions.AI;
using System.ComponentModel;

var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    Tools = [
        AIFunctionFactory.Create(
            async ([Description("Issue identifier")] string id) => {
                var issue = await FetchIssueAsync(id);
                return issue;
            },
            "lookup_issue",
            "Fetch issue details from our tracker"),
    ]
});
```

When Copilot invokes `lookup_issue`, the client automatically runs your handler and responds to the CLI. Handlers can return any JSON-serializable value (automatically wrapped), or a `ToolResultAIContent` wrapping a `ToolResultObject` for full control over result metadata.

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
- `OnPostToolUse` - Process tool results after execution. Can modify results or add context.
- `OnUserPromptSubmitted` - Intercept user prompts. Can modify the prompt before processing.
- `OnSessionStart` - Run logic when a session starts or resumes.
- `OnSessionEnd` - Cleanup or logging when session ends.
- `OnErrorOccurred` - Handle errors with retry/skip/abort strategies.

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
- GitHub Copilot CLI installed and in PATH (or provide custom `CliPath`)

## License

MIT
