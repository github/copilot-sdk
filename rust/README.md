# Copilot CLI SDK for Rust

A Rust SDK for programmatic access to the GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

See [github/copilot-sdk](https://github.com/github/copilot-sdk) for the equivalent SDKs in TypeScript, Python, Go, and .NET.

## Quick Start

```rust,no_run
use std::sync::Arc;
use github_copilot_sdk::{Client, ClientOptions, SessionConfig};
use github_copilot_sdk::handler::ApproveAllHandler;

# async fn example() -> Result<(), github_copilot_sdk::Error> {
let client = Client::start(ClientOptions::default()).await?;
let session = client.create_session(
    SessionConfig::default().with_handler(Arc::new(ApproveAllHandler)),
).await?;
let _message_id = session.send("Hello!").await?;
session.disconnect().await?;
client.stop().await?;
# Ok(())
# }
```

## Architecture

```text
Your Application
       ↓
  github_copilot_sdk::Client  (manages CLI process lifecycle)
       ↓
  github_copilot_sdk::Session (per-session event loop + handler dispatch)
       ↓ JSON-RPC over stdio or TCP
  copilot --server --stdio
```

The SDK manages the CLI process lifecycle: spawning, health-checking, and graceful shutdown. Communication uses [JSON-RPC 2.0](https://www.jsonrpc.org/specification) over stdin/stdout with `Content-Length` framing (the same protocol used by LSP). TCP transport is also supported.

## API Reference

### Client

```rust,ignore
// Start a client (spawns CLI process)
let client = Client::start(options).await?;

// Create a new session
let session = client.create_session(config.with_handler(handler)).await?;

// Resume an existing session
let session = client.resume_session(config.with_handler(handler)).await?;

// Low-level RPC
let result = client.call("method.name", Some(params)).await?;
let response = client.send_request("method.name", Some(params)).await?;

// Health check (echoes message back, returns typed PingResponse)
let pong = client.ping("hello").await?;

// Shutdown
client.stop().await?;
```

**`ClientOptions`:**

| Field | Type | Description |
|---|---|---|
| `program` | `CliProgram` | `Resolve` (default: auto-detect) or `Path(PathBuf)` (explicit) |
| `prefix_args` | `Vec<OsString>` | Args before `--server` (e.g. script path for node) |
| `cwd` | `PathBuf` | Working directory for CLI process |
| `env` | `Vec<(OsString, OsString)>` | Environment variables for CLI process |
| `env_remove` | `Vec<OsString>` | Environment variables to remove |
| `extra_args` | `Vec<String>` | Extra CLI flags |
| `transport` | `Transport` | `Stdio` (default), `Tcp { port }`, or `External { host, port }` |

With the default `CliProgram::Resolve`, `Client::start()` automatically resolves the binary via `github_copilot_sdk::resolve::copilot_binary()` — checking `COPILOT_CLI_PATH`, the [embedded CLI](#embedded-cli), and then the system PATH. Use `CliProgram::Path(path)` to skip resolution.

### Session

Created via `Client::create_session` or `Client::resume_session`. Owns an internal event loop that dispatches events to the `SessionHandler`.

```rust,ignore
use github_copilot_sdk::MessageOptions;

// Simple send — &str / String convert into MessageOptions automatically.
// Returns the assigned message ID for correlation with later events.
let _id = session.send("Fix the bug in auth.rs").await?;

// Send with mode and attachments
let _id = session
    .send(
        MessageOptions::new("What's in this image?")
            .with_mode("autopilot")
            .with_attachments(attachments),
    )
    .await?;

// Message history
let messages = session.get_messages().await?;

// Abort the current agent turn
session.abort().await?;

// Model management
let model = session.get_model().await?;
session.set_model("claude-sonnet-4.5", None).await?;

// Mode management (interactive, plan, autopilot)
let mode = session.get_mode().await?;
session.set_mode("autopilot").await?;

// Workspace files
let files = session.list_workspace_files().await?;
let content = session.read_workspace_file("plan.md").await?;

// Plan management
let (exists, content) = session.read_plan().await?;
session.update_plan("Updated plan content").await?;

// Fleet (sub-agents)
session.start_fleet(Some("Implement the auth module")).await?;

// Cleanup (preserves on-disk session state for later resume)
session.disconnect().await?;
```

#### Typed RPC namespace

The ergonomic helpers above are convenience wrappers over a fully-typed
JSON-RPC namespace generated from the Copilot CLI schema. `Client::rpc()`
and `Session::rpc()` give direct access to every method on the wire,
including ones with no helper today, with strongly-typed request and
response structs.

```rust,ignore
// Methods with helpers — wire strings live in one generated place.
let files = session.rpc().workspaces().list_files().await?.files;
let models = client.rpc().models().list().await?.models;

// Methods with no helper — full schema-typed access.
let agents = session.rpc().agent().list().await?.agents;
let tasks = session.rpc().tasks().list().await?.tasks;
let forked = client
    .rpc()
    .sessions()
    .fork(github_copilot_sdk::generated::api_types::SessionsForkRequest {
        session_id: "session-id".to_string(),
        from_message_id: None,
    })
    .await?;
```

New RPCs land in the namespace immediately as the schema regenerates;
helpers are added on top only when an ergonomic story is worth the
maintenance.

### SessionHandler

Implement this trait to control how a session responds to CLI events. Two styles are supported:

**1. Per-event methods (recommended).** Override only the callbacks you care about; every method has a safe default (permission → deny, user input → none, external tool → "no handler", elicitation → cancel, exit plan → default). This is the `serenity::EventHandler` pattern.

```rust,ignore
use async_trait::async_trait;
use github_copilot_sdk::handler::{PermissionResult, SessionHandler};
use github_copilot_sdk::types::{PermissionRequestData, RequestId, SessionId};

struct MyHandler;

#[async_trait]
impl SessionHandler for MyHandler {
    async fn on_permission_request(
        &self,
        _sid: SessionId,
        _rid: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        if data.extra.get("tool").and_then(|v| v.as_str()) == Some("view") {
            PermissionResult::Approved
        } else {
            PermissionResult::Denied
        }
    }

    async fn on_session_event(&self, sid: SessionId, event: github_copilot_sdk::types::SessionEvent) {
        println!("[{sid}] {}", event.event_type);
    }
}
```

**2. Single `on_event` method.** Override `on_event` directly and `match` on `HandlerEvent` — useful for logging middleware, custom routing, or when you want one exhaustive dispatch point.

```rust,ignore
use github_copilot_sdk::handler::*;
use async_trait::async_trait;

#[async_trait]
impl SessionHandler for MyRouter {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::SessionEvent { session_id, event } => {
                println!("[{session_id}] {}", event.event_type);
                HandlerResponse::Ok
            }
            HandlerEvent::PermissionRequest { .. } => {
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            HandlerEvent::UserInput { question, .. } => {
                HandlerResponse::UserInput(Some(UserInputResponse {
                    answer: prompt_user(&question),
                    was_freeform: true,
                }))
            }
            _ => HandlerResponse::Ok,
        }
    }
}
```

The default `on_event` dispatches to the per-event methods, so overriding `on_event` short-circuits them entirely — pick one style per handler.

Events are processed serially per session — blocking in a handler method pauses that session's event loop (which is correct, since the CLI is also waiting for the response). Other sessions are unaffected.

> **Note:** Notification-triggered events (`PermissionRequest` via `permission.requested`, `ExternalTool` via `external_tool.requested`) are dispatched on spawned tasks and may run concurrently with the serial event loop. See the trait-level docs on `SessionHandler` for details.

### SessionConfig

```rust,ignore
let config = SessionConfig {
    model: Some("gpt-5".into()),
    system_message: Some(SystemMessageConfig {
        content: Some("Always explain your reasoning.".into()),
        ..Default::default()
    }),
    request_elicitation: Some(true),    // enable elicitation provider
    ..Default::default()
};
let session = client.create_session(config.with_handler(handler)).await?;
```

### Session Hooks

Hooks intercept CLI behavior at lifecycle points — tool use, prompt submission, session start/end, and errors. Install a `SessionHooks` impl with [`SessionConfig::with_hooks`] — the SDK auto-enables `hooks` in `SessionConfig` when one is set.

```rust,ignore
use std::sync::Arc;
use github_copilot_sdk::hooks::*;
use async_trait::async_trait;

struct MyHooks;

#[async_trait]
impl SessionHooks for MyHooks {
    async fn on_hook(&self, event: HookEvent) -> HookOutput {
        match event {
            HookEvent::PreToolUse { input, ctx } => {
                if input.tool_name == "dangerous_tool" {
                    HookOutput::PreToolUse(PreToolUseOutput {
                        permission_decision: Some("deny".to_string()),
                        permission_decision_reason: Some("blocked by policy".to_string()),
                        ..Default::default()
                    })
                } else {
                    HookOutput::None // pass through
                }
            }
            HookEvent::SessionStart { input, .. } => {
                HookOutput::SessionStart(SessionStartOutput {
                    additional_context: Some("Extra system context".to_string()),
                    ..Default::default()
                })
            }
            _ => HookOutput::None,
        }
    }
}

let session = client
    .create_session(
        config
            .with_handler(handler)
            .with_hooks(Arc::new(MyHooks)),
    )
    .await?;
```

**Hook events:** `PreToolUse`, `PostToolUse`, `UserPromptSubmitted`, `SessionStart`, `SessionEnd`, `ErrorOccurred`. Each carries typed input/output structs. Return `HookOutput::None` for events you don't handle.

### System Message Transforms

Transforms customize system message sections during session creation. The SDK injects `action: "transform"` entries for each section ID your transform handles.

```rust,ignore
use github_copilot_sdk::transforms::*;
use async_trait::async_trait;

struct MyTransform;

#[async_trait]
impl SystemMessageTransform for MyTransform {
    fn section_ids(&self) -> Vec<String> {
        vec!["instructions".to_string()]
    }

    async fn transform_section(
        &self,
        _section_id: &str,
        content: &str,
        _ctx: TransformContext,
    ) -> Option<String> {
        Some(format!("{content}\n\nAlways be concise."))
    }
}

let session = client
    .create_session(
        config
            .with_handler(handler)
            .with_transform(Arc::new(MyTransform)),
    )
    .await?;
```

### Tool Registration

Define client-side tools as named types with `ToolHandler`, then route them with `ToolHandlerRouter`. Enable the `derive` feature for `schema_for::<T>()` — it generates JSON Schema from Rust types via `schemars`.

```rust,ignore
use std::sync::Arc;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{
    schema_for, tool_parameters, JsonSchema, ToolHandler, ToolHandlerRouter,
};
use github_copilot_sdk::{Error, SessionConfig, Tool, ToolInvocation, ToolResult};
use serde::Deserialize;
use async_trait::async_trait;

#[derive(Deserialize, JsonSchema)]
struct GetWeatherParams {
    /// City name
    city: String,
    /// Temperature unit
    unit: Option<String>,
}

struct GetWeatherTool;

#[async_trait]
impl ToolHandler for GetWeatherTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "get_weather".to_string(),
            namespaced_name: None,
            description: "Get weather for a city".to_string(),
            parameters: tool_parameters(schema_for::<GetWeatherParams>()),
            instructions: None,
        }
    }

    async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
        let params: GetWeatherParams = serde_json::from_value(inv.arguments)?;
        Ok(ToolResult::Text(format!("Weather in {}: sunny", params.city)))
    }
}

// Build a router that dispatches tool calls by name
let router = ToolHandlerRouter::new(
    vec![Box::new(GetWeatherTool)],
    Arc::new(ApproveAllHandler),
);

let config = SessionConfig {
    tools: Some(router.tools()),
    ..Default::default()
}
.with_handler(Arc::new(router));
let session = client.create_session(config).await?;
```

Tools are named types (not closures) — visible in stack traces and navigable via "go to definition". The router implements `SessionHandler`, forwarding unrecognized tools and non-tool events to the inner handler.

For trivial tools that don't need a named type, [`define_tool`](crate::tool::define_tool) collapses the definition to a single expression:

```rust,ignore
use github_copilot_sdk::tool::{define_tool, JsonSchema, ToolHandlerRouter};
use github_copilot_sdk::ToolResult;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
struct GetWeatherParams { city: String }

let router = ToolHandlerRouter::new(
    vec![define_tool(
        "get_weather",
        "Get weather for a city",
        |_inv, params: GetWeatherParams| async move {
            Ok(ToolResult::Text(format!("Sunny in {}", params.city)))
        },
    )],
    Arc::new(ApproveAllHandler),
);
```

The closure receives the full [`ToolInvocation`](crate::types::ToolInvocation) alongside the deserialized parameters, so handlers that need `inv.session_id` or `inv.tool_call_id` for telemetry, streaming updates, or scoped lookups can use them directly. Use `_inv` when you don't need the metadata.

Reach for the `ToolHandler` trait directly when you need shared state across multiple methods or want a named type that shows up by name in stack traces.

### Permission Policies

Set a permission policy directly on `SessionConfig` with the chainable builders. They wrap whatever handler you've installed (defaulting to `DenyAllHandler` if none) so only permission requests are intercepted; every other event flows through unchanged.

```rust,ignore
let session = client
    .create_session(
        SessionConfig::default()
            .with_handler(Arc::new(my_handler))
            .approve_all_permissions(),
        // or .deny_all_permissions()
        // or .approve_permissions_if(|data| {
        //     data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
        // })
    )
    .await?;
```

> Call the policy method **after** `with_handler` — `with_handler` overwrites the handler field, so `approve_all_permissions().with_handler(...)` discards the wrap.

For composing a policy onto a handler outside the builder chain (e.g. when wrapping a `ToolHandlerRouter` you've built elsewhere), the `permission` module exposes the same primitives as free functions:

```rust,ignore
use github_copilot_sdk::permission;

let router = ToolHandlerRouter::new(tools, Arc::new(MyHandler));
let handler = permission::approve_all(Arc::new(router));
// or permission::deny_all(...) / permission::approve_if(..., predicate)

let session = client.create_session(config.with_handler(handler)).await?;
```

### Capabilities & Elicitation

The SDK negotiates capabilities with the CLI after session creation. Enable elicitation to let the agent present structured UI dialogs (forms, URL prompts) to the user.

```rust,ignore
let config = SessionConfig {
    request_elicitation: Some(true),
    ..Default::default()
};
```

The handler receives `HandlerEvent::ElicitationRequest` with a message, optional JSON Schema for form fields, and an optional mode. Known modes include `Form` and `Url`, but the mode may be absent or an unknown future value. Return `HandlerResponse::Elicitation(result)`.

### Progress Reporting (`send_and_wait`)

For fire-and-forget messaging where you need to block until the agent finishes:

```rust,ignore
use std::time::Duration;
use github_copilot_sdk::MessageOptions;

// Sends a message and blocks until session.idle or session.error
session
    .send_and_wait(
        MessageOptions::new("Fix the bug").with_wait_timeout(Duration::from_secs(120)),
    )
    .await?;
```

Default timeout is 60 seconds. Only one `send_and_wait` can be active per session — concurrent calls return an error.

### Newtypes

**`SessionId`** — a newtype wrapper around `String` that prevents accidentally passing workspace IDs or request IDs where session IDs are expected. Transparent serialization (`#[serde(transparent)]`), zero-cost `Deref<Target=str>`, and ergonomic comparisons with `&str` and `String`.

```rust,ignore
use github_copilot_sdk::SessionId;

let id = SessionId::new("sess-abc123");
assert_eq!(id, "sess-abc123");           // compare with &str
let raw: String = id.into_inner();       // unwrap when needed
```

### Error Handling

The SDK uses a typed error enum:

```rust,ignore
pub enum Error {
    Protocol(ProtocolError),       // JSON-RPC framing, CLI startup, version mismatch
    Rpc { code: i32, message: String }, // CLI returned an error response
    Session(SessionError),         // Session not found, agent error, timeout, conflicts
    Io(std::io::Error),            // Transport I/O error
    Json(serde_json::Error),       // Serialization error
    BinaryNotFound { name, hint }, // CLI binary not found
}

// Check if the transport is broken (caller should discard the client)
if err.is_transport_failure() {
    client = Client::start(options).await?;
}
```

## Layout

| File | Description |
|---|---|
| `lib.rs` | `Client`, `ClientOptions`, `CliProgram`, `Transport`, `Error` |
| `session.rs` | `Session` struct, event loop, `send`/`send_and_wait`, `Client::create_session`/`resume_session` |
| `subscription.rs` | `EventSubscription` / `LifecycleSubscription` (`Stream`-able observer handles for `subscribe()` / `subscribe_lifecycle()`) |
| `handler.rs` | `SessionHandler` trait, `HandlerEvent`/`HandlerResponse` enums, `ApproveAllHandler` |
| `hooks.rs` | `SessionHooks` trait, `HookEvent`/`HookOutput` enums, typed hook inputs/outputs |
| `transforms.rs` | `SystemMessageTransform` trait, section-level system message customization |
| `tool.rs` | `ToolHandler` trait, `ToolHandlerRouter`, `schema_for::<T>()` (with `derive` feature) |
| `types.rs` | CLI protocol types (`SessionId`, `SessionEvent`, `SessionConfig`, `Tool`, etc.) |
| `resolve.rs` | Binary resolution (`copilot_binary`, `node_binary`, `extended_path`) |
| `embeddedcli.rs` | Embedded CLI extraction (`embedded-cli` feature) |
| `router.rs` | Internal per-session event demux |
| `jsonrpc.rs` | Internal Content-Length framed JSON-RPC transport |

## Embedded CLI

By default, `copilot_binary()` searches `COPILOT_CLI_PATH`, the system PATH, and common install locations. To **ship with a specific CLI version** embedded in the binary, set `COPILOT_CLI_VERSION` at build time:

```bash
COPILOT_CLI_VERSION=1.0.15 cargo build
```

### How it works

1. **Build time:** The SDK's `build.rs` detects `COPILOT_CLI_VERSION`, downloads the platform-appropriate archive from the [`github/copilot-cli` GitHub Releases](https://github.com/github/copilot-cli/releases) (`copilot-{platform}.tar.gz` on macOS/Linux, `.zip` on Windows), verifies the archive's SHA-256 against the release's `SHA256SUMS.txt`, extracts the `copilot` binary, compresses it with zstd, and embeds via `include_bytes!()`. No extra steps or tools needed — just the env var.

2. **Runtime:** On the first call to `github_copilot_sdk::resolve::copilot_binary()`, the embedded binary is lazily extracted to `~/.cache/github-copilot-sdk-{version}/copilot` (or `copilot.exe` on Windows), SHA-256 verified, and cached. Subsequent calls return the cached path.

3. **Dev builds:** Without the env var, `build.rs` does nothing. The binary is resolved from PATH as usual — zero friction.

### Resolution priority

`copilot_binary()` checks these sources in order:

1. `COPILOT_CLI_PATH` environment variable
2. Embedded CLI (build-time, via `COPILOT_CLI_VERSION`)
3. System PATH + common install locations

### Platforms

Supported: `darwin-arm64`, `darwin-x64`, `linux-x64`, `linux-arm64`, `win32-x64`, `win32-arm64`. The target platform is auto-detected from `CARGO_CFG_TARGET_OS` and `CARGO_CFG_TARGET_ARCH` (cross-compilation works).

## Features

No features are enabled by default — the bare SDK resolves the CLI from `COPILOT_CLI_PATH` or the system PATH without pulling in additional feature-gated dependencies.

| Feature | Default | Description |
|---|---|---|
| `embedded-cli` | — | Build-time CLI embedding via `COPILOT_CLI_VERSION` (adds `sha2`, `zstd`). Enable when you need to ship a self-contained binary with a pinned CLI version. |
| `derive` | — | `schema_for::<T>()` for generating JSON Schema from Rust types (adds `schemars`). Enable when defining [tool parameters](#tool-registration). |

```toml
# These examples use registry syntax for illustration; until the crate is
# published, use a path or git dependency instead.

# Minimal — resolve CLI from PATH
github-copilot-sdk = "0.1"

# Ship a pinned CLI version in your binary
github-copilot-sdk = { version = "0.1", features = ["embedded-cli"] }

# Derive JSON Schema for tool parameters
github-copilot-sdk = { version = "0.1", features = ["derive"] }

# Both
github-copilot-sdk = { version = "0.1", features = ["embedded-cli", "derive"] }
```
