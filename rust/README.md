# Copilot CLI SDK for Rust

A Rust SDK for programmatic access to the GitHub Copilot CLI.

> **Note:** This SDK is in public preview and may change in breaking ways.

## Installation

Add the SDK as a path or git dependency in your `Cargo.toml`:

```toml
[dependencies]
copilot-sdk = { git = "https://github.com/github/copilot-sdk", package = "copilot-sdk" }
tokio = { version = "1", features = ["full"] }
async-trait = "0.1"
```

> The crate is not yet published to crates.io. Use a git dependency or a local path dependency pointing at the `rust/` directory.

## Quick Start

```rust
use std::sync::Arc;
use copilot::handler::ApproveAllHandler;
use copilot::types::{MessageOptions, SessionConfig};
use copilot::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("gpt-5".into()),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None, // hooks
            None, // transforms
        )
        .await?;

    let response = session
        .send_and_wait(MessageOptions::new("What is 2+2?"), None)
        .await?;
    println!("message id: {}", response.message_id);

    session.disconnect().await?;
    Ok(())
}
```

## Distributing your application with an embedded GitHub Copilot CLI

The Rust SDK supports embedding the Copilot CLI binary at build time via `build.rs`. When the `COPILOT_CLI_VERSION` environment variable is set during build, the binary is downloaded, compressed, and embedded into the compiled binary using `include_bytes!()`.

At runtime, the embedded CLI is automatically extracted to a cache directory on first use (when no explicit CLI path is provided).

To embed the CLI:

```bash
COPILOT_CLI_VERSION=1.0.0 cargo build --release
```

This feature requires the `embedded-cli` feature (enabled by default).

## Features

- **`embedded-cli`** (default): Enables build-time CLI embedding with `sha2` and `zstd` dependencies.
- **`derive`**: Enables `schemars` support for automatic JSON Schema generation of tool parameter types.

## API Reference

### Client

- `Client::start(options: ClientOptions) -> Result<Client, Error>` — Start a CLI server and connect.
- `Client::from_streams(reader, writer, cwd) -> Result<Client, Error>` — Connect using custom streams.
- `client.create_session(config, handler, hooks, transforms) -> Result<Session, Error>` — Create a new session with a handler, optional hooks, and optional transforms.
- `client.create_session_with_session_fs(config, handler, hooks, transforms, session_fs) -> Result<Session, Error>` — Create a session with a per-session filesystem handler.
- `client.resume_session(config, handler, hooks, transforms) -> Result<Session, Error>` — Resume an existing session.
- `client.resume_session_with_session_fs(config, handler, hooks, transforms, session_fs) -> Result<Session, Error>` — Resume a session with a per-session filesystem handler.
- `client.stop() -> Result<(), Error>` — Gracefully stop the CLI server.
- `client.force_stop()` — Forcefully terminate the CLI process.
- `client.ping() -> Result<Value, Error>` — Ping the server.
- `client.list_models() -> Result<Vec<ModelInfo>, Error>` — List models, optionally using `ClientOptions::on_list_models`.

**ClientOptions:**

- `program` (`CliProgram`): How to locate the CLI binary (`Resolve` auto-detects, `Path` uses an explicit path).
- `prefix_args` (`Vec<OsString>`): Arguments inserted before `--server` when spawning the CLI.
- `cwd` (`PathBuf`): Working directory for the CLI process.
- `transport` (`Transport`): `Stdio` (default), `Tcp { port }`, or `External { host, port }`.
- `env` (`Vec<(OsString, OsString)>`): Environment variables for the child process.
- `env_remove` (`Vec<OsString>`): Environment variable names removed from the child process.
- `extra_args` (`Vec<String>`): Extra CLI flags.
- `on_list_models` (`Option<Arc<dyn ListModelsHandler>>`): Optional model-list override with client-side caching.
- `session_fs` (`Option<SessionFsConfig>`): Optional custom session filesystem provider registration.

**SessionConfig:**

- `model` (`Option<String>`): Model to use (e.g., `"gpt-5"`, `"claude-sonnet-4.5"`).
- `session_id` (`Option<SessionId>`): Custom session ID.
- `client_name` (`Option<String>`): Application name surfaced to the CLI.
- `reasoning_effort` (`Option<String>`): Reasoning effort level.
- `model_capabilities` (`Option<ModelCapabilitiesOverride>`): Deep-partial capability overrides.
- `config_dir` (`Option<PathBuf>`): Override the CLI config directory.
- `working_directory` (`Option<PathBuf>`): Working directory used for tool execution.
- `tools` (`Vec<Tool>`): Custom tools exposed to the CLI.
- `available_tools` (`Vec<String>`): Allowlist of built-in tools.
- `system_message` (`Option<SystemMessageConfig>`): System message configuration.
- `provider` (`Option<ProviderConfig>`): Custom API provider (BYOK).
- `streaming` (`bool`): Enable streaming delta events.
- `infinite_sessions` (`Option<InfiniteSessionConfig>`): Context compaction config.
- `custom_agents` (`Vec<CustomAgentConfig>`): Custom sub-agents.
- `agent` (`Option<String>`): Custom agent to activate for the session.
- `commands` (`Vec<CommandDefinition>`): Slash-command definitions for the session.

### Session

- `session.send_message(options) -> Result<String, Error>` — Send a message (non-blocking).
- `session.send_and_wait(options, timeout) -> Result<SendAndWaitResult, Error>` — Send and wait for idle.
- `session.get_messages() -> Result<Vec<SessionEvent>, Error>` — Get message history.
- `session.abort() -> Result<(), Error>` — Abort the currently processing message.
- `session.set_model(model, reasoning_effort) -> Result<Option<String>, Error>` — Change the model.
- `session.set_model_with_options(model, options) -> Result<Option<String>, Error>` — Change the model with capability overrides.
- `session.disconnect() -> Result<(), Error>` — Disconnect while preserving resumable state.
- `session.stop_event_loop()` — Stop listening for events.

### Tools

Implement the `ToolHandler` trait to create custom tools:

```rust
use copilot::tool::ToolHandler;
use copilot::{Error, Tool, ToolInvocation, ToolResult};
use async_trait::async_trait;

struct MyTool;

#[async_trait]
impl ToolHandler for MyTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "my_tool".into(),
            description: "Does something useful".into(),
            parameters: None,
            overrides_built_in_tool: None,
        }
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        Ok(ToolResult::Text(format!("Hello from my_tool!")))
    }
}
```

Use `ToolHandlerRouter` to register multiple tools and dispatch invocations automatically:

```rust
use std::sync::Arc;
use copilot::handler::ApproveAllHandler;
use copilot::tool::ToolHandlerRouter;
use copilot::types::SessionConfig;

let router = ToolHandlerRouter::new(
    vec![Box::new(MyTool)],
    Arc::new(ApproveAllHandler),
);

let session = client
    .create_session(
        SessionConfig {
            tools: router.tools(),
            ..Default::default()
        },
        Arc::new(router),
        None,
        None,
    )
    .await?;
```

### Session Handlers

Implement `SessionHandler` to process session events:

```rust
use copilot::handler::{SessionHandler, HandlerEvent, HandlerResponse, PermissionResult};
use async_trait::async_trait;

struct MyHandler;

#[async_trait]
impl SessionHandler for MyHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::PermissionRequest { .. } => {
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            _ => HandlerResponse::Ignore,
        }
    }
}
```

Use `ApproveAllHandler` to automatically approve all permission requests.

### Error Handling

The SDK uses a unified `Error` enum:

- `Error::Protocol(ProtocolError)` — Transport/protocol issues.
- `Error::Rpc { code, message }` — CLI returned a JSON-RPC error.
- `Error::Session(SessionError)` — Session-scoped errors.
- `Error::Io(io::Error)` — I/O errors.
- `Error::Json(serde_json::Error)` — Serialization errors.
- `Error::BinaryNotFound { name, hint }` — CLI binary not found.

Use `error.is_transport_failure()` to check if the error indicates a broken connection.

## Custom Providers (BYOK)

Configure a custom API provider for Bring Your Own Key usage:

```rust
use copilot::types::{ProviderConfig, SessionConfig};

let session = client
    .create_session(
        SessionConfig {
            model: Some("gpt-4o".into()),
            provider: Some(ProviderConfig {
                provider_type: Some("openai".into()),
                base_url: None,
                api_key: Some(std::env::var("OPENAI_API_KEY").unwrap()),
                bearer_token: None,
                wire_api: None,
                azure: None,
                headers: None,
            }),
            ..Default::default()
        },
        Arc::new(ApproveAllHandler),
        None,
        None,
    )
    .await?;
```

## Contributing

See the repository [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.
