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
use copilot_sdk::{CopilotClient, ClientOptions, SessionConfig, MessageOptions, SessionEventType};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create client
    let client = CopilotClient::new(Some(ClientOptions {
        log_level: Some("error".to_string()),
        ..Default::default()
    }));

    // Start the client
    client.start().await?;

    // Create a session
    let session = client.create_session(Some(SessionConfig {
        model: Some("gpt-5".to_string()),
        ..Default::default()
    })).await?;

    // Set up event handler
    let (tx, mut rx) = tokio::sync::mpsc::channel(1);
    let tx_clone = tx.clone();

    session.on(Arc::new(move |event| {
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

### Client

- `CopilotClient::new(options: Option<ClientOptions>) -> Self` - Create a new client
- `start() -> Result<()>` - Start the CLI server
- `stop() -> Vec<CopilotError>` - Stop the CLI server (returns array of errors, empty if all succeeded)
- `force_stop()` - Forcefully stop without graceful cleanup
- `create_session(config: Option<SessionConfig>) -> Result<Arc<CopilotSession>>` - Create a new session
- `resume_session(session_id: &str, config: Option<ResumeSessionConfig>) -> Result<Arc<CopilotSession>>` - Resume an existing session
- `get_state() -> ConnectionState` - Get connection state
- `ping(message: Option<&str>) -> Result<PingResponse>` - Ping the server

**ClientOptions:**

- `cli_path` (Option\<String\>): Path to CLI executable (default: "copilot" or `COPILOT_CLI_PATH` env var)
- `cli_url` (Option\<String\>): URL of existing CLI server (e.g., `"localhost:8080"`, `"http://127.0.0.1:9000"`, or just `"8080"`). When provided, the client will not spawn a CLI process.
- `cwd` (Option\<String\>): Working directory for CLI process
- `port` (Option\<u16\>): Server port for TCP mode (default: 0 for random)
- `use_stdio` (Option\<bool\>): Use stdio transport instead of TCP (default: true)
- `log_level` (Option\<String\>): Log level (default: "info")
- `auto_start` (Option\<bool\>): Auto-start server on first use (default: true)
- `auto_restart` (Option\<bool\>): Auto-restart on crash (default: true)
- `env` (Option\<Vec\<(String, String)\>\>): Environment variables for CLI process (default: inherits from current process)

**ResumeSessionConfig:**

- `tools` (Vec\<Tool\>): Tools to expose when resuming
- `provider` (Option\<ProviderConfig\>): Custom model provider configuration

### Session

- `send(options: MessageOptions) -> Result<String>` - Send a message
- `send_and_wait(options: MessageOptions, timeout: Option<Duration>) -> Result<Option<SessionEvent>>` - Send and wait for idle
- `on(handler: SessionEventHandler) -> impl FnOnce()` - Subscribe to events (returns unsubscribe function)
- `abort() -> Result<()>` - Abort the currently processing message
- `get_messages() -> Result<Vec<SessionEvent>>` - Get message history
- `destroy() -> Result<()>` - Destroy the session

### Tools

Expose your own functionality to Copilot by attaching tools to a session.

#### Using define_tool (Recommended)

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

#### Using Tool struct directly

For more control over the JSON schema, use the `ToolBuilder`:

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

When the model selects a tool, the SDK automatically runs your handler (in parallel with other calls) and responds to the CLI's `tool.call` with the handler's result.

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```rust
use copilot_sdk::{CopilotClient, SessionConfig, MessageOptions, SessionEventType};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = CopilotClient::new(None);
    client.start().await?;

    let session = client.create_session(Some(SessionConfig {
        model: Some("gpt-5".to_string()),
        streaming: Some(true),
        ..Default::default()
    })).await?;

    let (tx, mut rx) = tokio::sync::mpsc::channel(1);
    let tx_clone = tx.clone();

    session.on(Arc::new(move |event| {
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

- `AssistantMessageDelta` events are sent with `delta_content` containing incremental text
- `AssistantReasoningDelta` events are sent with `delta_content` for reasoning/chain-of-thought (model-dependent)
- Accumulate `delta_content` values to build the full response progressively
- The final `AssistantMessage` and `AssistantReasoning` events contain the complete content

Note: `AssistantMessage` and `AssistantReasoning` (final events) are always sent regardless of streaming setting.

## Transport Modes

### stdio (Default)

Communicates with CLI via stdin/stdout pipes. Recommended for most use cases.

```rust
let client = CopilotClient::new(None); // Uses stdio by default
```

### TCP

Communicates with CLI via TCP socket. Useful for distributed scenarios.

```rust
let client = CopilotClient::new(Some(ClientOptions {
    use_stdio: Some(false),
    port: Some(3000),
    ..Default::default()
}));
```

## Environment Variables

- `COPILOT_CLI_PATH` - Path to the Copilot CLI executable

## License

MIT
