# Copilot SDK for Rust

Rust SDK for programmatic access to the GitHub Copilot CLI.

## Installation

Add to your `Cargo.toml`:

```toml
[dependencies]
copilot-sdk = "0.1"
tokio = { version = "1", features = ["full"] }
```

Or use cargo add:

```bash
cargo add copilot-sdk
cargo add tokio --features full
```

## Requirements

- Rust 1.75 or later (2021 edition)
- [Copilot CLI](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli) installed and available in PATH

## Quick Start

```rust
use copilot_sdk::{CopilotClient, SessionConfig, MessageOptions, SessionEventType};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create a client (automatically spawns CLI server)
    let client = CopilotClient::new(None);
    client.start().await?;

    // Create a session
    let session = client.create_session(Some(SessionConfig {
        model: Some("gpt-4".to_string()),
        ..Default::default()
    })).await?;

    // Subscribe to events
    let _unsubscribe = session.on(Arc::new(|event| {
        if event.event_type == SessionEventType::AssistantMessage {
            if let Some(content) = &event.data.content {
                println!("Assistant: {}", content);
            }
        }
    }));

    // Send a message
    session.send(MessageOptions {
        prompt: "Hello, Copilot!".to_string(),
        ..Default::default()
    }).await?;

    // Or send and wait for response
    let response = session.send_and_wait(
        MessageOptions {
            prompt: "What is 2 + 2?".to_string(),
            ..Default::default()
        },
        None, // Use default timeout
    ).await?;

    // Clean up
    client.stop().await;
    Ok(())
}
```

## Custom Tools

Define custom tools that the assistant can invoke:

```rust
use copilot_sdk::{define_tool, SessionConfig, ToolResult};
use schemars::JsonSchema;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
struct GetWeatherParams {
    city: String,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = CopilotClient::new(None);
    client.start().await?;

    // Define a tool with typed parameters
    let weather_tool = define_tool::<GetWeatherParams, _, _, _>(
        "get_weather",
        "Get the current weather for a city",
        |params, _inv| async move {
            // Your implementation here
            Ok(format!("Weather in {}: 22°C, sunny", params.city))
        },
    );

    // Create a session with the tool
    let session = client.create_session(Some(SessionConfig {
        tools: vec![weather_tool],
        ..Default::default()
    })).await?;

    // The assistant can now use the get_weather tool
    session.send(MessageOptions {
        prompt: "What's the weather in Seattle?".to_string(),
        ..Default::default()
    }).await?;

    Ok(())
}
```

## Client Options

```rust
use copilot_sdk::{CopilotClient, ClientOptions};

// Connect to an external CLI server
let client = CopilotClient::new(Some(ClientOptions {
    cli_url: Some("localhost:3000".to_string()),
    ..Default::default()
}));

// Use TCP instead of stdio
let client = CopilotClient::new(Some(ClientOptions {
    use_stdio: Some(false),
    port: Some(3000),
    ..Default::default()
}));

// Custom CLI path
let client = CopilotClient::new(Some(ClientOptions {
    cli_path: Some("/path/to/copilot".to_string()),
    ..Default::default()
}));

// Disable auto-start
let client = CopilotClient::new(Some(ClientOptions {
    auto_start: Some(false),
    ..Default::default()
}));
```

## Session Configuration

```rust
use copilot_sdk::{SessionConfig, SystemMessageConfig};

let session = client.create_session(Some(SessionConfig {
    // Specify model
    model: Some("gpt-4".to_string()),

    // Custom session ID
    session_id: Some("my-session".to_string()),

    // Custom tools
    tools: vec![my_tool],

    // System message customization
    system_message: Some(SystemMessageConfig {
        mode: Some("append".to_string()),
        content: Some("Additional context...".to_string()),
    }),

    // Tool filtering
    available_tools: Some(vec!["Read".to_string(), "Write".to_string()]),
    excluded_tools: Some(vec!["Bash".to_string()]),

    // Enable streaming
    streaming: Some(true),

    ..Default::default()
})).await?;
```

## API Reference

### CopilotClient

| Method | Description |
|--------|-------------|
| `new(options)` | Create a new client |
| `start()` | Start and connect to the CLI server |
| `stop()` | Stop the server and close all sessions |
| `force_stop()` | Force stop without graceful cleanup |
| `create_session(config)` | Create a new session |
| `resume_session(id, config)` | Resume an existing session |
| `delete_session(id)` | Delete a session |
| `list_sessions()` | List all sessions |
| `ping(message)` | Ping the server |
| `get_state()` | Get connection state |

### CopilotSession

| Method | Description |
|--------|-------------|
| `session_id()` | Get the session ID |
| `send(options)` | Send a message |
| `send_and_wait(options, timeout)` | Send and wait for idle |
| `on(handler)` | Subscribe to events |
| `get_messages()` | Get session history |
| `abort()` | Abort current processing |
| `destroy()` | Destroy the session |

### Session Events

| Event Type | Description |
|------------|-------------|
| `AssistantMessage` | Complete assistant message |
| `AssistantMessageDelta` | Streaming message chunk |
| `SessionIdle` | Session is idle |
| `SessionError` | Error occurred |
| `ToolExecutionStart` | Tool execution started |
| `ToolExecutionComplete` | Tool execution completed |

## Examples

Run the basic example:

```bash
cargo run --example basic
```

## Development

```bash
# Format code
cargo fmt

# Run lints
cargo clippy -- -D warnings

# Run tests
cargo test

# Run E2E tests (requires CLI)
COPILOT_CLI_PATH=/path/to/copilot cargo test --test '*'
```

## License

MIT
