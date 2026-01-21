# Copilot CLI SDK for Rust

A Rust SDK for programmatic access to the GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
cargo add copilot-sdk
cargo add tokio --features full
```

Or add to your `Cargo.toml`:

```toml
[dependencies]
copilot-sdk = "0.1"
tokio = { version = "1", features = ["full"] }
```

## Quick Start

```rust
use copilot_sdk::{CopilotClient, ClientOptions, SessionConfig, SessionEvent, MessageOptions, SessionEventType};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create client (returns Result)
    let client = CopilotClient::new(Some(ClientOptions {
        log_level: Some("error".to_string()),
        ..Default::default()
    }))?;

    // Start the client
    client.start().await?;

    // Create a session
    let session = client.create_session(Some(SessionConfig {
        model: Some("gpt-5".to_string()),
        ..Default::default()
    })).await?;

    // Set up event handler (receives Arc<SessionEvent>)
    let (tx, mut rx) = tokio::sync::mpsc::channel(1);
    let tx_clone = tx.clone();

    session.on(Arc::new(move |event: Arc<SessionEvent>| {
        if event.event_type == SessionEventType::AssistantMessage {
            if let Some(content) = &event.data.content {
                println!("{}", content);
            }
        }
        if event.event_type == SessionEventType::SessionIdle {
            let _ = tx_clone.try_send(());
        }
    }));

    // Send a message
    session.send(MessageOptions {
        prompt: "What is 2+2?".to_string(),
        ..Default::default()
    }).await?;

    // Wait for completion
    rx.recv().await;

    // Clean up
    session.destroy().await?;
    client.stop().await;

    Ok(())
}
```

## API Reference

### CopilotClient

The main client for interacting with the Copilot CLI server.

#### Constructor

```rust
CopilotClient::new(options: Option<ClientOptions>) -> Result<Self, CopilotError>
```

Creates a new client. Returns `Result` to handle invalid configuration errors.

**ClientOptions:**

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `cli_path` | `Option<String>` | `"copilot"` | Path to CLI executable (or `COPILOT_CLI_PATH` env var) |
| `cli_url` | `Option<String>` | `None` | URL of existing CLI server (e.g., `"localhost:8080"`, `"http://127.0.0.1:9000"`, or `"8080"`). When provided, the client will not spawn a CLI process. |
| `cwd` | `Option<String>` | `None` | Working directory for CLI process |
| `port` | `Option<u16>` | `0` | Server port for TCP mode (0 = random) |
| `use_stdio` | `Option<bool>` | `true` | Use stdio transport instead of TCP |
| `log_level` | `Option<String>` | `"info"` | Log level for CLI server |
| `auto_start` | `Option<bool>` | `true` | Auto-start server on first use |
| `auto_restart` | `Option<bool>` | `true` | Auto-restart on crash |
| `env` | `Option<HashMap<String, String>>` | `None` | Environment variables for CLI process |

#### Methods

##### `start() -> Result<()>`

Start the CLI server and establish connection.

##### `stop() -> Vec<CopilotError>`

Stop the CLI server and close all sessions. Returns a list of any errors encountered during cleanup.

##### `force_stop()`

Forcefully stop without graceful cleanup. Use when `stop()` takes too long.

##### `create_session(config: Option<SessionConfig>) -> Result<Arc<CopilotSession>>`

Create a new conversation session.

**SessionConfig:**

| Field | Type | Description |
|-------|------|-------------|
| `session_id` | `Option<String>` | Custom session ID |
| `model` | `Option<String>` | Model to use (`"gpt-5"`, `"claude-sonnet-4.5"`, etc.) |
| `tools` | `Vec<Tool>` | Custom tools exposed to the CLI |
| `streaming` | `Option<bool>` | Enable streaming responses |
| `system_message` | `Option<SystemMessageConfig>` | System message customization |
| `provider` | `Option<ProviderConfig>` | Custom model provider |
| `mcp_servers` | `Option<Vec<McpServerConfig>>` | MCP server configurations |
| `available_tools` | `Option<Vec<String>>` | Allowlist of available tools |
| `excluded_tools` | `Option<Vec<String>>` | Tools to exclude |

##### `resume_session(session_id: &str, config: Option<ResumeSessionConfig>) -> Result<Arc<CopilotSession>>`

Resume an existing session.

##### `get_state() -> ConnectionState`

Get current connection state (`Disconnected`, `Connecting`, `Connected`, `Error`).

##### `ping(message: Option<&str>) -> Result<PingResponse>`

Ping the server to verify connectivity.

##### `list_sessions() -> Result<Vec<SessionMetadata>>`

List all available sessions.

##### `delete_session(session_id: &str) -> Result<()>`

Delete a session and its data from disk.

---

### CopilotSession

Represents a single conversation session.

#### Methods

##### `send(options: MessageOptions) -> Result<String>`

Send a message to the session. Returns immediately after the message is queued; use event handlers or `send_and_wait()` to wait for completion.

**MessageOptions:**

| Field | Type | Description |
|-------|------|-------------|
| `prompt` | `String` | The message/prompt to send |
| `attachments` | `Option<Vec<Attachment>>` | File attachments |
| `mode` | `Option<String>` | Delivery mode (`"enqueue"` or `"immediate"`) |

Returns the message ID.

##### `send_and_wait(options: MessageOptions, timeout: Option<Duration>) -> Result<Option<SessionEvent>>`

Send a message and wait until the session becomes idle. Returns the final assistant message event, or `None` if none was received.

##### `on(handler: SessionEventHandler) -> impl FnOnce()`

Subscribe to session events. Returns an unsubscribe function.

**Important:** The handler receives `Arc<SessionEvent>` (not `SessionEvent`) to avoid expensive clones when dispatching to multiple handlers.

```rust
let unsubscribe = session.on(Arc::new(|event: Arc<SessionEvent>| {
    println!("Event: {:?}", event.event_type);
}));

// Later...
unsubscribe();
```

##### `abort() -> Result<()>`

Abort the currently processing message.

##### `get_messages() -> Result<Vec<SessionEvent>>`

Get all events/messages from this session's history.

##### `destroy() -> Result<()>`

Destroy the session and free resources.

---

## Tools

Expose your own functionality to Copilot by attaching tools to a session.

### Using `define_tool` (Recommended)

Use `define_tool` for type-safe tools with automatic JSON schema generation:

```rust
use copilot_sdk::{define_tool, SessionConfig, ToolInvocation};
use schemars::JsonSchema;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
struct LookupIssueParams {
    /// Issue identifier
    id: String,
}

let lookup_issue = define_tool::<LookupIssueParams, _, _, _>(
    "lookup_issue",
    "Fetch issue details from our tracker",
    |params, _inv| async move {
        let issue = fetch_issue(&params.id).await?;
        Ok(issue.summary)
    },
);

let session = client.create_session(Some(SessionConfig {
    model: Some("gpt-5".to_string()),
    tools: vec![lookup_issue],
    ..Default::default()
})).await?;
```

### Using `ToolBuilder`

For more control over the JSON schema:

```rust
use copilot_sdk::{ToolBuilder, ToolResult};
use serde_json::json;

let lookup_issue = ToolBuilder::new("lookup_issue")
    .description("Fetch issue details from our tracker")
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
    .handler(|inv| async move {
        let id = inv.arguments.get("id")
            .and_then(|v| v.as_str())
            .unwrap_or("");
        let issue = fetch_issue(id).await?;
        Ok(ToolResult::success(issue.summary))
    });

let session = client.create_session(Some(SessionConfig {
    model: Some("gpt-5".to_string()),
    tools: vec![lookup_issue],
    ..Default::default()
})).await?;
```

When the model selects a tool, the SDK automatically runs your handler and responds to the CLI's `tool.call` with the result.

---

## Event Types

Sessions emit various events during processing:

| Event Type | Description |
|------------|-------------|
| `UserMessage` | User message added |
| `AssistantMessage` | Complete assistant response |
| `AssistantMessageDelta` | Streaming response chunk |
| `AssistantReasoning` | Complete reasoning content |
| `AssistantReasoningDelta` | Streaming reasoning chunk |
| `ToolExecutionStart` | Tool execution started |
| `ToolExecutionComplete` | Tool execution completed |
| `SessionIdle` | Session finished processing |
| `SessionError` | Error occurred |
| `SessionStart` | Session started |

See [`SessionEventType`](src/generated/session_events.rs) for the full list.

---

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```rust
use copilot_sdk::{CopilotClient, SessionConfig, SessionEvent, MessageOptions, SessionEventType};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = CopilotClient::new(None)?;
    client.start().await?;

    let session = client.create_session(Some(SessionConfig {
        model: Some("gpt-5".to_string()),
        streaming: Some(true),
        ..Default::default()
    })).await?;

    let (tx, mut rx) = tokio::sync::mpsc::channel(1);
    let tx_clone = tx.clone();

    session.on(Arc::new(move |event: Arc<SessionEvent>| {
        match event.event_type {
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
            SessionEventType::SessionIdle => {
                let _ = tx_clone.try_send(());
            }
            _ => {}
        }
    }));

    session.send(MessageOptions {
        prompt: "Tell me a short story".to_string(),
        ..Default::default()
    }).await?;

    rx.recv().await;

    session.destroy().await?;
    client.stop().await;

    Ok(())
}
```

When `streaming: Some(true)`:

- `AssistantMessageDelta` events contain `delta_content` with incremental text
- `AssistantReasoningDelta` events contain reasoning chunks (model-dependent)
- Accumulate `delta_content` values to build the response progressively
- Final `AssistantMessage` and `AssistantReasoning` events contain complete content

Note: Final events are always sent regardless of streaming setting.

---

## Transport Modes

### stdio (Default)

Communicates with CLI via stdin/stdout pipes. Recommended for most use cases.

```rust
let client = CopilotClient::new(None)?; // Uses stdio by default
```

### TCP

Communicates with CLI via TCP socket. Useful for distributed scenarios.

```rust
let client = CopilotClient::new(Some(ClientOptions {
    use_stdio: Some(false),
    port: Some(3000),
    ..Default::default()
}))?;
```

### External Server

Connect to an already-running CLI server:

```rust
let client = CopilotClient::new(Some(ClientOptions {
    cli_url: Some("localhost:8080".to_string()),
    ..Default::default()
}))?;
```

---

## Error Handling

The SDK uses `CopilotError` for all error types:

```rust
use copilot_sdk::{CopilotClient, CopilotError};

match CopilotClient::new(None) {
    Ok(client) => {
        // Use client...
    }
    Err(CopilotError::InvalidConfig(msg)) => {
        eprintln!("Configuration error: {}", msg);
    }
    Err(e) => {
        eprintln!("Error: {}", e);
    }
}
```

Common error types:

| Error | Description |
|-------|-------------|
| `InvalidConfig` | Invalid client configuration |
| `NotConnected` | Client not connected |
| `Connection` | Connection error |
| `Process` | CLI process error |
| `Timeout` | Operation timed out |
| `JsonRpc` | JSON-RPC error from server |
| `Session` | Session-related error |

---

## Environment Variables

- `COPILOT_CLI_PATH` - Path to the Copilot CLI executable

---

## Requirements

- Rust 1.75+ (2021 edition)
- Tokio async runtime
- GitHub Copilot CLI installed and accessible

---

## License

MIT
