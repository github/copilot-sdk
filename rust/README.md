# Copilot CLI SDK for Rust

A Rust SDK for programmatic access to the GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

Add to your `Cargo.toml`:

```toml
[dependencies]
copilot-sdk = "0.1"
tokio = { version = "1", features = ["full"] }
```

## Quick Start

```rust
use copilot_sdk::{Client, ClientOptions, SessionConfig, MessageOptions, SessionEventType};

#[tokio::main]
async fn main() -> copilot_sdk::Result<()> {
    // Create client
    let mut client = Client::new(ClientOptions::new().log_level("error"));

    // Start the client
    client.start().await?;

    // Create a session
    let session = client.create_session(SessionConfig::new().model("gpt-5")).await?;

    // Set up event handler
    let mut rx = session.subscribe();
    let handler = tokio::spawn(async move {
        while let Ok(event) = rx.recv().await {
            if event.r#type == SessionEventType::AssistantMessage {
                if let Some(content) = &event.data.content {
                    println!("{}", content);
                }
            }
            if event.r#type == SessionEventType::SessionIdle {
                break;
            }
        }
    });

    // Send a message
    session.send(MessageOptions::new("What is 2+2?")).await?;

    // Wait for completion
    let _ = handler.await;

    // Clean up
    session.destroy().await?;
    client.stop().await;

    Ok(())
}
```

## API Reference

### Client

- `Client::new(options: ClientOptions)` - Create a new client
- `client.start().await` - Start the CLI server
- `client.stop().await` - Stop the CLI server (returns Vec of errors)
- `client.force_stop().await` - Forcefully stop without graceful cleanup
- `client.create_session(config).await` - Create a new session
- `client.resume_session(session_id).await` - Resume an existing session
- `client.resume_session_with_options(session_id, config).await` - Resume with configuration
- `client.state()` - Get connection state
- `client.ping(message).await` - Ping the server
- `client.get_status().await` - Get CLI status
- `client.get_auth_status().await` - Get authentication status
- `client.list_models().await` - List available models
- `client.list_sessions().await` - List active sessions
- `client.delete_session(session_id).await` - Delete a session

**ClientOptions (builder pattern):**

```rust
ClientOptions::new()
    .cli_path("/usr/local/bin/copilot")  // Path to CLI executable
    .cli_url("localhost:8080")           // Connect to existing server
    .cwd("/path/to/workdir")             // Working directory
    .port(8080)                          // Port for TCP mode
    .use_stdio(true)                     // Use stdio transport (default)
    .log_level("error")                  // Log level
    .auto_start(true)                    // Auto-start on first use
    .auto_restart(true)                  // Auto-restart on crash
    .env(vec![("KEY".into(), "VALUE".into())])  // Environment variables
```

### Session

- `session.send(options).await` - Send a message
- `session.send_and_wait(options, timeout).await` - Send and wait for idle
- `session.subscribe()` - Get a broadcast receiver for events
- `session.on(handler)` - Register an event handler (returns unsubscribe fn)
- `session.abort().await` - Abort current processing
- `session.get_messages().await` - Get message history
- `session.destroy().await` - Destroy the session
- `session.session_id()` - Get session ID

### Tools

Expose your own functionality to Copilot by attaching tools to a session.

#### Using define_tool (Recommended)

Use `define_tool` for type-safe tools with automatic JSON schema generation:

```rust
use copilot_sdk::{define_tool, ToolInvocation};
use serde::Deserialize;
use schemars::JsonSchema;

#[derive(Debug, Deserialize, JsonSchema)]
struct LookupIssueParams {
    /// Issue identifier
    id: String,
}

let lookup_issue = define_tool(
    "lookup_issue",
    "Fetch issue details from our tracker",
    |params: LookupIssueParams, _inv: ToolInvocation| async move {
        // params is automatically deserialized from the LLM's arguments
        let issue = fetch_issue(&params.id).await?;
        Ok(issue.summary)
    },
);

let session = client.create_session(
    SessionConfig::new()
        .model("gpt-5")
        .tool(lookup_issue)
).await?;
```

#### Using Tool struct directly

For more control over the JSON schema, use the `Tool` struct directly:

```rust
use copilot_sdk::{Tool, ToolInvocation, ToolResult};
use serde_json::json;

let lookup_issue = Tool::new("lookup_issue", "Fetch issue details from our tracker")
    .parameters(json!({
        "type": "object",
        "properties": {
            "id": {
                "type": "string",
                "description": "Issue identifier"
            }
        },
        "required": ["id"]
    }))
    .handler(|invocation: ToolInvocation| {
        Box::pin(async move {
            let args = invocation.arguments;
            let id = args.get("id").and_then(|v| v.as_str()).unwrap_or("");
            Ok(ToolResult::success(format!("Issue {}: Example", id)))
        })
    });
```

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```rust
use copilot_sdk::{Client, ClientOptions, SessionConfig, MessageOptions, SessionEventType};

#[tokio::main]
async fn main() -> copilot_sdk::Result<()> {
    let mut client = Client::new(ClientOptions::new());
    client.start().await?;

    let session = client.create_session(
        SessionConfig::new()
            .model("gpt-5")
            .streaming(true)
    ).await?;

    let mut rx = session.subscribe();
    tokio::spawn(async move {
        while let Ok(event) = rx.recv().await {
            match event.r#type {
                SessionEventType::AssistantMessageDelta => {
                    // Streaming message chunk - print incrementally
                    if let Some(delta) = &event.data.delta_content {
                        print!("{}", delta);
                    }
                }
                SessionEventType::AssistantReasoningDelta => {
                    // Streaming reasoning chunk (if model supports reasoning)
                    if let Some(delta) = &event.data.delta_content {
                        print!("{}", delta);
                    }
                }
                SessionEventType::AssistantMessage => {
                    // Final message - complete content
                    println!("\n--- Final message ---");
                    if let Some(content) = &event.data.content {
                        println!("{}", content);
                    }
                }
                SessionEventType::AssistantReasoning => {
                    // Final reasoning content (if model supports reasoning)
                    println!("--- Reasoning ---");
                    if let Some(content) = &event.data.content {
                        println!("{}", content);
                    }
                }
                _ => {}
            }
        }
    });

    session.send(MessageOptions::new("Tell me a short story")).await?;

    Ok(())
}
```

When `streaming: true`:

- `assistant.message_delta` events are sent with `delta_content` containing incremental text
- `assistant.reasoning_delta` events are sent with `delta_content` for reasoning/chain-of-thought (model-dependent)
- Accumulate `delta_content` values to build the full response progressively
- The final `assistant.message` and `assistant.reasoning` events contain the complete content

Note: `assistant.message` and `assistant.reasoning` (final events) are always sent regardless of streaming setting.

## Transport Modes

### stdio (Default)

Communicates with CLI via stdin/stdout pipes. Recommended for most use cases.

```rust
let client = Client::new(ClientOptions::new()); // Uses stdio by default
```

### TCP

Communicates with CLI via TCP socket. Useful for distributed scenarios.

```rust
let client = Client::new(ClientOptions::new().use_stdio(false).port(8080));
```

### External Server

Connect to an existing CLI server:

```rust
let client = Client::new(ClientOptions::new().cli_url("localhost:8080"));
```

## Environment Variables

- `COPILOT_CLI_PATH` - Path to the Copilot CLI executable

## Error Handling

The SDK uses a custom `Result` type with `CopilotError`:

```rust
use copilot_sdk::{Result, CopilotError};

async fn example() -> Result<()> {
    // Errors are automatically converted
    match client.start().await {
        Ok(()) => println!("Connected!"),
        Err(CopilotError::Connection(msg)) => eprintln!("Connection failed: {}", msg),
        Err(CopilotError::ProtocolMismatch { expected, actual }) => {
            eprintln!("Protocol mismatch: expected {}, got {}", expected, actual);
        }
        Err(e) => eprintln!("Error: {}", e),
    }
    Ok(())
}
```

## License

MIT
