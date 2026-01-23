# GitHub Copilot SDK for Rust

[![Crates.io](https://img.shields.io/crates/v/github-copilot-sdk.svg)](https://crates.io/crates/github-copilot-sdk)
[![Documentation](https://docs.rs/github-copilot-sdk/badge.svg)](https://docs.rs/github-copilot-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Embed Copilot's agentic workflows in your Rust application. The GitHub Copilot SDK exposes the same engine behind Copilot CLI: a production-tested agent runtime you can invoke programmatically.

## Features

- 🤖 **Agent Runtime** - Full access to Copilot's planning, tool invocation, and file editing capabilities
- 🔧 **Custom Tools** - Define and register your own tools with type-safe handlers
- 🔒 **Permission Control** - Fine-grained control over what the agent can access
- 🔌 **MCP Support** - Integration with Model Context Protocol servers
- ⚡ **Async/Await** - Built on Tokio for high-performance async operations
- 📡 **Multiple Transports** - Stdio (default) or TCP connections

## Prerequisites

You need the GitHub Copilot CLI installed and available in your PATH:

```bash
# Install Copilot CLI
# Follow instructions at: https://docs.github.com/en/copilot/using-github-copilot/using-github-copilot-in-the-command-line
```

## Installation

Add this to your `Cargo.toml`:

```toml
[dependencies]
github-copilot-sdk = "0.1"
tokio = { version = "1", features = ["full"] }
```

## Quick Start

```rust
use github_copilot_sdk::{Client, ClientOptions, SessionConfig, SessionEvent};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create and start client
    let client = Client::new(ClientOptions::default()).await?;
    
    // Create session
    let session = client.create_session(SessionConfig {
        model: Some("gpt-4o".to_string()),
        ..Default::default()
    }).await?;
    
    // Register event handler
    session.on_event(std::sync::Arc::new(|event| {
        if let SessionEvent::AssistantMessage { content, .. } = event {
            println!("Assistant: {}", content);
        }
    })).await;
    
    // Send message and wait for response
    let response = session.send_and_wait("Hello, Copilot!").await?;
    println!("Response: {}", response);
    
    // Clean shutdown
    client.stop().await?;
    Ok(())
}
```

## Custom Tools

Define and register custom tools with type-safe handlers:

```rust
use github_copilot_sdk::{Tool, ToolHandler, ToolInvocation, ToolResult};
use async_trait::async_trait;
use std::collections::HashMap;

struct GetTimeHandler;

#[async_trait]
impl ToolHandler for GetTimeHandler {
    async fn handle(
        &self,
        _arguments: HashMap<String, serde_json::Value>,
        _invocation: ToolInvocation,
    ) -> github_copilot_sdk::Result<ToolResult> {
        let now = chrono::Local::now();
        Ok(ToolResult::text(format!("Current time: {}", now)))
    }
}

// Register the tool
let tool = Tool::new(
    "get_current_time",
    "Get the current date and time",
    serde_json::json!({
        "type": "object",
        "properties": {},
        "required": []
    }),
);

session.register_tool(tool, std::sync::Arc::new(GetTimeHandler)).await?;
```

## Permission Handling

Control what the agent can access:

```rust
use github_copilot_sdk::{PermissionRequest, PermissionRequestResult};

session.set_permission_handler(std::sync::Arc::new(
    |request: PermissionRequest, _invocation| {
        match request.kind.as_str() {
            "file.read" => Ok(PermissionRequestResult {
                kind: "allow".to_string(),
                rules: None,
            }),
            "file.write" => Ok(PermissionRequestResult {
                kind: "deny".to_string(),
                rules: None,
            }),
            _ => Ok(PermissionRequestResult {
                kind: "allow".to_string(),
                rules: None,
            }),
        }
    },
)).await;
```

## Configuration

### Client Options

```rust
use github_copilot_sdk::ClientOptions;

let options = ClientOptions {
    cli_path: "copilot".to_string(),      // Path to CLI executable
    cwd: Some("/path/to/dir".to_string()), // Working directory
    use_stdio: true,                       // Use stdio transport (default)
    log_level: "info".to_string(),         // CLI log level
    auto_start: true,                      // Auto-start CLI
    auto_restart: true,                    // Auto-restart on crash
    ..Default::default()
};

let client = Client::new(options).await?;
```

### Session Configuration

```rust
use github_copilot_sdk::{SessionConfig, SystemMessage};

let config = SessionConfig {
    model: Some("gpt-4o".to_string()),
    system_message: Some(SystemMessage::Append {
        content: Some("You are a helpful assistant.".to_string()),
    }),
    cwd: Some("/project/path".to_string()),
    ..Default::default()
};

let session = client.create_session(config).await?;
```

## Transport Modes

### Stdio (Default)

```rust
let client = Client::new(ClientOptions {
    use_stdio: true,
    ..Default::default()
}).await?;
```

### TCP

```rust
let client = Client::new(ClientOptions {
    use_stdio: false,
    port: 3000,
    ..Default::default()
}).await?;
```

### Connect to External Server

```rust
let client = Client::new(ClientOptions {
    cli_url: Some("localhost:3000".to_string()),
    ..Default::default()
}).await?;
```

## Examples

The repository includes several examples:

- **hello** - Basic usage
- **tools** - Custom tool registration
- **permissions** - Permission handling
- **mcp** - MCP server integration

Run an example:

```bash
cargo run --example hello
```

## API Documentation

Full API documentation is available at [docs.rs/github-copilot-sdk](https://docs.rs/github-copilot-sdk).

## Session Events

The SDK emits various events during operation:

- `AssistantMessage` - Final response from the assistant
- `MessagePending` - Progress indicator
- `ToolCallRequested` - Tool invocation request
- `PermissionRequested` - Permission request
- `SessionStateChanged` - Session state changes
- `Error` - Error events

## Models

All models available via Copilot CLI are supported. Popular choices:

- `gpt-4o` - GPT-4 Optimized
- `gpt-4o-mini` - Smaller, faster GPT-4
- `claude-sonnet-4` - Claude Sonnet
- `o1` - OpenAI o1
- `o1-mini` - Smaller o1

## Error Handling

The SDK uses `Result<T, Error>` for error handling:

```rust
use github_copilot_sdk::Error;

match session.send_and_wait("Hello").await {
    Ok(response) => println!("{}", response),
    Err(Error::NotConnected) => eprintln!("Not connected"),
    Err(Error::Timeout) => eprintln!("Request timed out"),
    Err(e) => eprintln!("Error: {}", e),
}
```

## Requirements

- **Rust**: 1.70 or higher
- **Copilot CLI**: Latest version
- **GitHub Copilot**: Active subscription

## Billing

Usage is billed according to the GitHub Copilot CLI billing model. See [GitHub Copilot Pricing](https://github.com/features/copilot#pricing).

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

MIT License - see [LICENSE](../LICENSE) for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/github/copilot-sdk/issues)
- **Documentation**: [SDK Documentation](https://docs.rs/github-copilot-sdk)
- **Examples**: See the `examples/` directory

## Additional Resources

- [Getting Started Guide](../docs/getting-started.md)
- [Cookbook](../cookbook/README.md)
- [Samples](../samples/README.md)
