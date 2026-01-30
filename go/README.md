# Copilot CLI SDK for Go

A Go SDK for programmatic access to the GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
go get github.com/github/copilot-sdk/go
```

## Quick Start

```go
package main

import (
    "fmt"
    "log"

    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    // Create client
    client := copilot.NewClient(&copilot.ClientOptions{
        LogLevel: "error",
    })

    // Start the client
    if err := client.Start(); err != nil {
        log.Fatal(err)
    }
    defer client.Stop()

    // Create a session
    session, err := client.CreateSession(&copilot.SessionConfig{
        Model: "gpt-5",
    })
    if err != nil {
        log.Fatal(err)
    }
    defer session.Destroy()

    // Set up event handler
    done := make(chan bool)
    session.On(func(event copilot.SessionEvent) {
        if event.Type == "assistant.message" {
            if event.Data.Content != nil {
                fmt.Println(*event.Data.Content)
            }
        }
        if event.Type == "session.idle" {
            close(done)
        }
    })

    // Send a message
    _, err = session.Send(copilot.MessageOptions{
        Prompt: "What is 2+2?",
    })
    if err != nil {
        log.Fatal(err)
    }

    // Wait for completion
    <-done
}
```

## API Reference

### Client

- `NewClient(options *ClientOptions) *Client` - Create a new client
- `Start() error` - Start the CLI server
- `Stop() []error` - Stop the CLI server (returns array of errors, empty if all succeeded)
- `ForceStop()` - Forcefully stop without graceful cleanup
- `CreateSession(config *SessionConfig) (*Session, error)` - Create a new session
- `ResumeSession(sessionID string) (*Session, error)` - Resume an existing session
- `ResumeSessionWithOptions(sessionID string, config *ResumeSessionConfig) (*Session, error)` - Resume with additional configuration
- `ListSessions() ([]SessionMetadata, error)` - List all sessions known to the server
- `DeleteSession(sessionID string) error` - Delete a session permanently
- `GetState() ConnectionState` - Get connection state
- `Ping(message string) (*PingResponse, error)` - Ping the server

**ClientOptions:**

- `CLIPath` (string): Path to CLI executable (default: "copilot" or `COPILOT_CLI_PATH` env var)
- `CLIUrl` (string): URL of existing CLI server (e.g., `"localhost:8080"`, `"http://127.0.0.1:9000"`, or just `"8080"`). When provided, the client will not spawn a CLI process.
- `Cwd` (string): Working directory for CLI process
- `Port` (int): Server port for TCP mode (default: 0 for random)
- `UseStdio` (bool): Use stdio transport instead of TCP (default: true)
- `LogLevel` (string): Log level (default: "info")
- `AutoStart` (\*bool): Auto-start server on first use (default: true). Use `Bool(false)` to disable.
- `AutoRestart` (\*bool): Auto-restart on crash (default: true). Use `Bool(false)` to disable.
- `Env` ([]string): Environment variables for CLI process (default: inherits from current process)
- `GithubToken` (string): GitHub token for authentication. When provided, takes priority over other auth methods.
- `UseLoggedInUser` (\*bool): Whether to use logged-in user for authentication (default: true, but false when `GithubToken` is provided). Cannot be used with `CLIUrl`.

**SessionConfig:**

- `Model` (string): Model to use ("gpt-5", "claude-sonnet-4.5", etc.). **Required when using custom provider.**
- `ReasoningEffort` (string): Reasoning effort level for models that support it ("low", "medium", "high", "xhigh"). Use `ListModels()` to check which models support this option.
- `SessionID` (string): Custom session ID
- `Tools` ([]Tool): Custom tools exposed to the CLI
- `SystemMessage` (\*SystemMessageConfig): System message configuration
- `Provider` (\*ProviderConfig): Custom API provider configuration (BYOK). See [Custom Providers](#custom-providers) section.
- `Streaming` (bool): Enable streaming delta events
- `InfiniteSessions` (\*InfiniteSessionConfig): Automatic context compaction configuration
- `OnUserInputRequest` (UserInputHandler): Handler for user input requests from the agent (enables ask_user tool). See [User Input Requests](#user-input-requests) section.
- `Hooks` (\*SessionHooks): Hook handlers for session lifecycle events. See [Session Hooks](#session-hooks) section.

**ResumeSessionConfig:**

- `Tools` ([]Tool): Tools to expose when resuming
- `ReasoningEffort` (string): Reasoning effort level for models that support it
- `Provider` (\*ProviderConfig): Custom API provider configuration (BYOK). See [Custom Providers](#custom-providers) section.
- `Streaming` (bool): Enable streaming delta events

### Session

- `Send(options MessageOptions) (string, error)` - Send a message
- `On(handler SessionEventHandler) func()` - Subscribe to events (returns unsubscribe function)
- `Abort() error` - Abort the currently processing message
- `GetMessages() ([]SessionEvent, error)` - Get message history
- `Destroy() error` - Destroy the session

### Helper Functions

- `Bool(v bool) *bool` - Helper to create bool pointers for `AutoStart`/`AutoRestart` options

## Image Support

The SDK supports image attachments via the `Attachments` field in `MessageOptions`. You can attach images by providing their file path:

```go
_, err = session.Send(copilot.MessageOptions{
    Prompt: "What's in this image?",
    Attachments: []copilot.Attachment{
        {
            Type: "file",
            Path: "/path/to/image.jpg",
        },
    },
})
```

Supported image formats include JPG, PNG, GIF, and other common image types. The agent's `view` tool can also read images directly from the filesystem, so you can also ask questions like:

```go
_, err = session.Send(copilot.MessageOptions{
    Prompt: "What does the most recent jpg in this directory portray?",
})
```

### Tools

Expose your own functionality to Copilot by attaching tools to a session.

#### Using DefineTool (Recommended)

Use `DefineTool` for type-safe tools with automatic JSON schema generation:

```go
type LookupIssueParams struct {
    ID string `json:"id" jsonschema:"Issue identifier"`
}

lookupIssue := copilot.DefineTool("lookup_issue", "Fetch issue details from our tracker",
    func(params LookupIssueParams, inv copilot.ToolInvocation) (any, error) {
        // params is automatically unmarshaled from the LLM's arguments
        issue, err := fetchIssue(params.ID)
        if err != nil {
            return nil, err
        }
        return issue.Summary, nil
    })

session, _ := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
    Tools: []copilot.Tool{lookupIssue},
})
```

#### Using Tool struct directly

For more control over the JSON schema, use the `Tool` struct directly:

```go
lookupIssue := copilot.Tool{
    Name:        "lookup_issue",
    Description: "Fetch issue details from our tracker",
    Parameters: map[string]interface{}{
        "type": "object",
        "properties": map[string]interface{}{
            "id": map[string]interface{}{
                "type":        "string",
                "description": "Issue identifier",
            },
        },
        "required": []string{"id"},
    },
    Handler: func(invocation copilot.ToolInvocation) (copilot.ToolResult, error) {
        args := invocation.Arguments.(map[string]interface{})
        issue, err := fetchIssue(args["id"].(string))
        if err != nil {
            return copilot.ToolResult{}, err
        }
        return copilot.ToolResult{
            TextResultForLLM: issue.Summary,
            ResultType:       "success",
            SessionLog:       fmt.Sprintf("Fetched issue %s", issue.ID),
        }, nil
    },
}

session, _ := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
    Tools: []copilot.Tool{lookupIssue},
})
```

When the model selects a tool, the SDK automatically runs your handler (in parallel with other calls) and responds to the CLI's `tool.call` with the handler's result.

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```go
package main

import (
    "fmt"
    "log"

    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    client := copilot.NewClient(nil)

    if err := client.Start(); err != nil {
        log.Fatal(err)
    }
    defer client.Stop()

    session, err := client.CreateSession(&copilot.SessionConfig{
        Model:     "gpt-5",
        Streaming: true,
    })
    if err != nil {
        log.Fatal(err)
    }
    defer session.Destroy()

    done := make(chan bool)

    session.On(func(event copilot.SessionEvent) {
        if event.Type == "assistant.message_delta" {
            // Streaming message chunk - print incrementally
            if event.Data.DeltaContent != nil {
                fmt.Print(*event.Data.DeltaContent)
            }
        } else if event.Type == "assistant.reasoning_delta" {
            // Streaming reasoning chunk (if model supports reasoning)
            if event.Data.DeltaContent != nil {
                fmt.Print(*event.Data.DeltaContent)
            }
        } else if event.Type == "assistant.message" {
            // Final message - complete content
            fmt.Println("\n--- Final message ---")
            if event.Data.Content != nil {
                fmt.Println(*event.Data.Content)
            }
        } else if event.Type == "assistant.reasoning" {
            // Final reasoning content (if model supports reasoning)
            fmt.Println("--- Reasoning ---")
            if event.Data.Content != nil {
                fmt.Println(*event.Data.Content)
            }
        }
        if event.Type == "session.idle" {
            close(done)
        }
    })

    _, err = session.Send(copilot.MessageOptions{
        Prompt: "Tell me a short story",
    })
    if err != nil {
        log.Fatal(err)
    }

    <-done
}
```

When `Streaming: true`:

- `assistant.message_delta` events are sent with `DeltaContent` containing incremental text
- `assistant.reasoning_delta` events are sent with `DeltaContent` for reasoning/chain-of-thought (model-dependent)
- Accumulate `DeltaContent` values to build the full response progressively
- The final `assistant.message` and `assistant.reasoning` events contain the complete content

Note: `assistant.message` and `assistant.reasoning` (final events) are always sent regardless of streaming setting.

## Infinite Sessions

By default, sessions use **infinite sessions** which automatically manage context window limits through background compaction and persist state to a workspace directory.

```go
// Default: infinite sessions enabled with default thresholds
session, _ := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
})

// Access the workspace path for checkpoints and files
fmt.Println(session.WorkspacePath())
// => ~/.copilot/session-state/{sessionId}/

// Custom thresholds
session, _ := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
    InfiniteSessions: &copilot.InfiniteSessionConfig{
        Enabled:                       copilot.Bool(true),
        BackgroundCompactionThreshold: copilot.Float64(0.80), // Start compacting at 80% context usage
        BufferExhaustionThreshold:     copilot.Float64(0.95), // Block at 95% until compaction completes
    },
})

// Disable infinite sessions
session, _ := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
    InfiniteSessions: &copilot.InfiniteSessionConfig{
        Enabled: copilot.Bool(false),
    },
})
```

When enabled, sessions emit compaction events:

- `session.compaction_start` - Background compaction started
- `session.compaction_complete` - Compaction finished (includes token counts)

## Custom Providers

The SDK supports custom OpenAI-compatible API providers (BYOK - Bring Your Own Key), including local providers like Ollama. When using a custom provider, you must specify the `Model` explicitly.

**ProviderConfig:**

- `Type` (string): Provider type - "openai", "azure", or "anthropic" (default: "openai")
- `BaseURL` (string): API endpoint URL (required)
- `APIKey` (string): API key (optional for local providers like Ollama)
- `BearerToken` (string): Bearer token for authentication (takes precedence over APIKey)
- `WireApi` (string): API format for OpenAI/Azure - "completions" or "responses" (default: "completions")
- `Azure.APIVersion` (string): Azure API version (default: "2024-10-21")

**Example with Ollama:**

```go
session, err := client.CreateSession(&copilot.SessionConfig{
    Model: "deepseek-coder-v2:16b", // Required when using custom provider
    Provider: &copilot.ProviderConfig{
        Type:    "openai",
        BaseURL: "http://localhost:11434/v1", // Ollama endpoint
        // APIKey not required for Ollama
    },
})
```

**Example with custom OpenAI-compatible API:**

```go
session, err := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-4",
    Provider: &copilot.ProviderConfig{
        Type:    "openai",
        BaseURL: "https://my-api.example.com/v1",
        APIKey:  os.Getenv("MY_API_KEY"),
    },
})
```

**Example with Azure OpenAI:**

```go
session, err := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-4",
    Provider: &copilot.ProviderConfig{
        Type:    "azure",  // Must be "azure" for Azure endpoints, NOT "openai"
        BaseURL: "https://my-resource.openai.azure.com",  // Just the host, no path
        APIKey:  os.Getenv("AZURE_OPENAI_KEY"),
        Azure: &copilot.AzureProviderOptions{
            APIVersion: "2024-10-21",
        },
    },
})
```
> **Important notes:**
> - When using a custom provider, the `Model` parameter is **required**. The SDK will return an error if no model is specified.
> - For Azure OpenAI endpoints (`*.openai.azure.com`), you **must** use `Type: "azure"`, not `Type: "openai"`.
> - The `BaseURL` should be just the host (e.g., `https://my-resource.openai.azure.com`). Do **not** include `/openai/v1` in the URL - the SDK handles path construction automatically.

## User Input Requests

Enable the agent to ask questions to the user using the `ask_user` tool by providing an `OnUserInputRequest` handler:

```go
session, err := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
    OnUserInputRequest: func(request copilot.UserInputRequest, invocation copilot.UserInputInvocation) (copilot.UserInputResponse, error) {
        // request.Question - The question to ask
        // request.Choices - Optional slice of choices for multiple choice
        // request.AllowFreeform - Whether freeform input is allowed (default: true)
        
        fmt.Printf("Agent asks: %s\n", request.Question)
        if len(request.Choices) > 0 {
            fmt.Printf("Choices: %v\n", request.Choices)
        }
        
        // Return the user's response
        return copilot.UserInputResponse{
            Answer:      "User's answer here",
            WasFreeform: true, // Whether the answer was freeform (not from choices)
        }, nil
    },
})
```

## Session Hooks

Hook into session lifecycle events by providing handlers in the `Hooks` configuration:

```go
session, err := client.CreateSession(&copilot.SessionConfig{
    Model: "gpt-5",
    Hooks: &copilot.SessionHooks{
        // Called before each tool execution
        OnPreToolUse: func(input copilot.PreToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PreToolUseHookOutput, error) {
            fmt.Printf("About to run tool: %s\n", input.ToolName)
            // Return permission decision and optionally modify args
            return &copilot.PreToolUseHookOutput{
                PermissionDecision: "allow", // "allow", "deny", or "ask"
                ModifiedArgs:       input.ToolArgs, // Optionally modify tool arguments
                AdditionalContext:  "Extra context for the model",
            }, nil
        },
        
        // Called after each tool execution
        OnPostToolUse: func(input copilot.PostToolUseHookInput, invocation copilot.HookInvocation) (*copilot.PostToolUseHookOutput, error) {
            fmt.Printf("Tool %s completed\n", input.ToolName)
            return &copilot.PostToolUseHookOutput{
                AdditionalContext: "Post-execution notes",
            }, nil
        },
        
        // Called when user submits a prompt
        OnUserPromptSubmitted: func(input copilot.UserPromptSubmittedHookInput, invocation copilot.HookInvocation) (*copilot.UserPromptSubmittedHookOutput, error) {
            fmt.Printf("User prompt: %s\n", input.Prompt)
            return &copilot.UserPromptSubmittedHookOutput{
                ModifiedPrompt: input.Prompt, // Optionally modify the prompt
            }, nil
        },
        
        // Called when session starts
        OnSessionStart: func(input copilot.SessionStartHookInput, invocation copilot.HookInvocation) (*copilot.SessionStartHookOutput, error) {
            fmt.Printf("Session started from: %s\n", input.Source) // "startup", "resume", "new"
            return &copilot.SessionStartHookOutput{
                AdditionalContext: "Session initialization context",
            }, nil
        },
        
        // Called when session ends
        OnSessionEnd: func(input copilot.SessionEndHookInput, invocation copilot.HookInvocation) (*copilot.SessionEndHookOutput, error) {
            fmt.Printf("Session ended: %s\n", input.Reason)
            return nil, nil
        },
        
        // Called when an error occurs
        OnErrorOccurred: func(input copilot.ErrorOccurredHookInput, invocation copilot.HookInvocation) (*copilot.ErrorOccurredHookOutput, error) {
            fmt.Printf("Error in %s: %s\n", input.ErrorContext, input.Error)
            return &copilot.ErrorOccurredHookOutput{
                ErrorHandling: "retry", // "retry", "skip", or "abort"
            }, nil
        },
    },
})
```

**Available hooks:**

- `OnPreToolUse` - Intercept tool calls before execution. Can allow/deny or modify arguments.
- `OnPostToolUse` - Process tool results after execution. Can modify results or add context.
- `OnUserPromptSubmitted` - Intercept user prompts. Can modify the prompt before processing.
- `OnSessionStart` - Run logic when a session starts or resumes.
- `OnSessionEnd` - Cleanup or logging when session ends.
- `OnErrorOccurred` - Handle errors with retry/skip/abort strategies.

## Transport Modes

### stdio (Default)

Communicates with CLI via stdin/stdout pipes. Recommended for most use cases.

```go
client := copilot.NewClient(nil) // Uses stdio by default
```

### TCP

Communicates with CLI via TCP socket. Useful for distributed scenarios.

## Environment Variables

- `COPILOT_CLI_PATH` - Path to the Copilot CLI executable

## License

MIT
