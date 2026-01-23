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

**ResumeSessionConfig:**

- `Tools` ([]Tool): Tools to expose when resuming
- `Provider` (\*ProviderConfig): Custom model provider configuration

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
