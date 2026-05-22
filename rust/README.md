# GitHub Copilot CLI SDK for Rust

A Rust SDK for programmatic access to the GitHub Copilot CLI.

> **Note:** This SDK is in technical preview and may change in breaking ways.

See [github/copilot-sdk](https://github.com/github/copilot-sdk) for the equivalent SDKs in TypeScript, Python, Go, and .NET. The Rust SDK seeks parity with those SDKs; see [Differences From Other SDKs](#differences-from-other-sdks) below for the small set of intentional divergences.

**Releases:** [github.com/github/copilot-sdk/releases?q=rust%2F](https://github.com/github/copilot-sdk/releases?q=rust%2F) — per-version release notes for the Rust crate.

## Quick Start

```rust,no_run
use std::sync::Arc;
use github_copilot_sdk::{Client, ClientOptions, SessionConfig};
use github_copilot_sdk::handler::ApproveAllHandler;

# async fn example() -> Result<(), github_copilot_sdk::Error> {
let client = Client::start(ClientOptions::default()).await?;
let session = client.create_session(
    SessionConfig::default().with_permission_handler(Arc::new(ApproveAllHandler)),
).await?;
let _message_id = session.send("Hello!").await?;
session.disconnect().await?;
client.stop().await.ok();
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
let session = client.create_session(config.with_permission_handler(handler)).await?;

// Resume an existing session
let session = client.resume_session(config.with_permission_handler(handler)).await?;

// Low-level RPC
let result = client.call("method.name", Some(params)).await?;
let response = client.send_request("method.name", Some(params)).await?;

// Health check (echoes message back, returns typed PingResponse)
let pong = client.ping("hello").await?;

// Shutdown
client.stop().await?;
```

**`ClientOptions`:**

| Field         | Type                        | Description                                                     |
| ------------- | --------------------------- | --------------------------------------------------------------- |
| `program`     | `CliProgram`                | `Resolve` (default: auto-detect) or `Path(PathBuf)` (explicit)  |
| `prefix_args` | `Vec<OsString>`             | Args before `--server` (e.g. script path for node)              |
| `cwd`         | `PathBuf`                   | Working directory for CLI process                               |
| `env`         | `Vec<(OsString, OsString)>` | Environment variables for CLI process                           |
| `env_remove`  | `Vec<OsString>`             | Environment variables to remove                                 |
| `extra_args`  | `Vec<String>`               | Extra CLI flags                                                 |
| `transport`   | `Transport`                 | `Stdio` (default), `Tcp { port }`, or `External { host, port }` |

With the default `CliProgram::Resolve`, `Client::start()` automatically resolves the binary via `github_copilot_sdk::resolve::copilot_binary()` — checking `COPILOT_CLI_PATH`, the [embedded CLI](#embedded-cli), and then the system PATH. Use `CliProgram::Path(path)` to skip resolution.

### Session

Created via `Client::create_session` or `Client::resume_session`. Owns an internal event loop that dispatches CLI callbacks to the focused handler traits you install on `SessionConfig`, and broadcasts session events through `subscribe()`.

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
let messages = session.get_events().await?;

// Abort the current agent turn
session.abort().await?;

// Model management
session.set_model("claude-sonnet-4.5", None).await?;

// Generated typed RPCs cover lower-level session operations.
let model = session.rpc().model().get_current().await?;
let mode = session.rpc().mode().get().await?;

// Workspace files
let files = session.rpc().workspaces().list_files().await?;
let content = session
    .rpc()
    .workspaces()
    .read_file(github_copilot_sdk::generated::api_types::WorkspacesReadFileRequest {
        path: "plan.md".to_string(),
    })
    .await?;

// Plan management
let plan = session.rpc().plan().read().await?;
session
    .rpc()
    .plan()
    .update(github_copilot_sdk::generated::api_types::PlanUpdateRequest {
        content: "Updated plan content".to_string(),
    })
    .await?;

// Fleet (sub-agents)
session
    .rpc()
    .fleet()
    .start(github_copilot_sdk::generated::api_types::FleetStartRequest {
        prompt: Some("Implement the auth module".to_string()),
    })
    .await?;

// Cleanup (preserves on-disk session state for later resume)
session.disconnect().await?;
```

#### Typed RPC namespace

High-level helpers are convenience wrappers over a fully-typed
JSON-RPC namespace generated from the GitHub Copilot CLI schema. `Client::rpc()`
and `Session::rpc()` give direct access to every method on the wire,
including ones with no helper today, with strongly-typed request and
response structs.

```rust,ignore
// Common generated RPCs.
let files = session.rpc().workspaces().list_files().await?.files;
let models = client.rpc().models().list().await?.models;

// Methods with no helper — full schema-typed access.
let agents = session.rpc().agent().list().await?.agents;
let tasks = session.rpc().tasks().list().await?.tasks;
let forked = client
    .rpc()
    .sessions()
    .fork(github_copilot_sdk::generated::api_types::SessionsForkRequest {
        session_id: "session-id".into(),
        to_event_id: None,
    })
    .await?;
```

New RPCs land in the namespace immediately as the schema regenerates;
helpers are added on top only when an ergonomic story is worth the
maintenance.

### Handler Traits

The SDK exposes five focused handler traits, one per CLI callback type. Implement only the traits you need and install each with the matching `SessionConfig` setter. Each trait has a single `async fn handle(...)` method:

| Trait                   | Setter                            | Purpose                                       |
| ----------------------- | --------------------------------- | --------------------------------------------- |
| `PermissionHandler`     | `with_permission_handler(...)`    | Approve/deny tool-use permission requests     |
| `ElicitationHandler`    | `with_elicitation_handler(...)`   | Respond to structured elicitation prompts     |
| `UserInputHandler`      | `with_user_input_handler(...)`    | Answer free-form / choice user-input prompts  |
| `ExitPlanModeHandler`   | `with_exit_plan_mode_handler(...)`| Respond when the agent exits plan mode        |
| `AutoModeSwitchHandler` | `with_auto_mode_switch_handler(...)`| Respond to automatic mode-switch proposals  |

The CLI's `requestPermission` / `requestElicitation` / `requestUserInput` / etc. wire flags are derived automatically from which traits you've installed — clients that don't install a handler are silently skipped, letting another connected client handle the request.

```rust,ignore
use std::sync::Arc;
use async_trait::async_trait;
use github_copilot_sdk::handler::{PermissionHandler, PermissionResult};
use github_copilot_sdk::types::{PermissionRequestData, RequestId, SessionId};

struct MyPermissions;

#[async_trait]
impl PermissionHandler for MyPermissions {
    async fn handle(
        &self,
        _sid: SessionId,
        _rid: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        if data.extra.get("tool").and_then(|v| v.as_str()) == Some("view") {
            PermissionResult::approve_once()
        } else {
            PermissionResult::reject(None)
        }
    }
}

let config = SessionConfig::default().with_permission_handler(Arc::new(MyPermissions));
```

A single type can implement multiple handler traits — share one `Arc<Self>` across the setters by cloning:

```rust,ignore
let h = Arc::new(MyHandler);
let config = SessionConfig::default()
    .with_permission_handler(h.clone())
    .with_user_input_handler(h);
```

The built-in `ApproveAllHandler` and `DenyAllHandler` implement `PermissionHandler` for the common cases. To observe streamed session events (assistant messages, tool calls, etc.), call `session.subscribe()` — see [Streaming](#streaming) below.

### SessionConfig

```rust,ignore
let config = SessionConfig {
    model: Some("gpt-5".into()),
    system_message: Some(SystemMessageConfig {
        content: Some("Always explain your reasoning.".into()),
        ..Default::default()
    }),
    ..Default::default()
}
.with_elicitation_handler(Arc::new(my_elicitation_handler))
.with_permission_handler(handler);
let session = client.create_session(config).await?;
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
            .with_permission_handler(handler)
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
            .with_permission_handler(handler)
            .with_system_message_transform(Arc::new(MyTransform)),
    )
    .await?;
```

### Tool Registration

Define client-side tools as named types implementing `ToolHandler` and attach
them to `Tool` declarations via `Tool::with_handler`, then install via
`SessionConfig::with_tools`. Enable the `derive` feature for `schema_for::<T>()`
— it generates JSON Schema from Rust types via `schemars`.

```rust,ignore
use std::sync::Arc;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{schema_for, JsonSchema, ToolHandler};
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
    async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
        let params: GetWeatherParams = serde_json::from_value(inv.arguments)?;
        Ok(ToolResult::Text(format!("Weather in {}: sunny", params.city)))
    }
}

let tool = Tool::new("get_weather")
    .with_description("Get weather for a city")
    .with_parameters(schema_for::<GetWeatherParams>())
    .with_handler(Arc::new(GetWeatherTool));

let config = SessionConfig::default()
    .with_permission_handler(Arc::new(ApproveAllHandler))
    .with_tools(vec![tool]);
let session = client.create_session(config).await?;
```

Tools are named types (not closures) — visible in stack traces and navigable via "go to definition". The SDK registers each tool's handler under its `Tool::name` and surfaces the same `Tool` definitions to the CLI automatically.

Tools without an attached handler (`Tool::with_handler` never called) are declaration-only: the SDK advertises them on the wire but doesn't dispatch invocations to anything. Useful when another connected client services the tool.

For trivial tools that don't need a named type, the `define_tool` helper function (available with the `derive` feature) collapses the definition to a single expression and returns a fully-formed `Tool` with handler attached:

```rust,ignore
use github_copilot_sdk::tool::{define_tool, JsonSchema};
use github_copilot_sdk::ToolResult;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
struct GetWeatherParams { city: String }

let tool = define_tool(
    "get_weather",
    "Get weather for a city",
    |_inv, params: GetWeatherParams| async move {
        Ok(ToolResult::Text(format!("Sunny in {}", params.city)))
    },
);

let config = SessionConfig::default()
    .with_permission_handler(Arc::new(ApproveAllHandler))
    .with_tools(vec![tool]);
```

The closure receives the full [`ToolInvocation`](crate::types::ToolInvocation) alongside the deserialized parameters, so handlers that need `inv.session_id` or `inv.tool_call_id` for telemetry, streaming updates, or scoped lookups can use them directly. Use `_inv` when you don't need the metadata.

Reach for the `ToolHandler` trait directly when you need shared state across multiple methods or want a named type that shows up by name in stack traces.

### Permission Policies

Set a permission policy directly on `SessionConfig` with the chainable builders. They install a synthesized `PermissionHandler` so only permission requests are intercepted; every other event flows through unchanged.

```rust,ignore
let session = client
    .create_session(
        SessionConfig::default()
            .approve_all_permissions(),
        // or .deny_all_permissions()
        // or .approve_permissions_if(|data| {
        //     data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
        // })
    )
    .await?;
```

> The policy builders set the permission handler slot directly; they're equivalent to calling `with_permission_handler(...)` with the corresponding built-in (`ApproveAllHandler`, `DenyAllHandler`, or `permission::approve_if(...)`).

The `permission` module also exposes the policy primitives as standalone helpers for the rare case where you want to construct the handler value separately and install it via `with_permission_handler`:

```rust,ignore
use github_copilot_sdk::permission;

let handler = permission::approve_if(|data| {
    data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
});
// or permission::approve_all() / permission::deny_all()

let session = client
    .create_session(config.with_permission_handler(handler))
    .await?;
```

### Elicitation

To opt your client into receiving `elicitation.requested` broadcasts, install an `ElicitationHandler` on the session config. The wire flag `requestElicitation` is derived from the presence of the handler; clients without one are silently skipped, allowing other connected clients on the same CLI to handle the request.

```rust,ignore
use async_trait::async_trait;
use github_copilot_sdk::handler::{ElicitationHandler, ElicitationResult};
use github_copilot_sdk::types::{ElicitationRequest, RequestId, SessionId};

struct MyElicitation;

#[async_trait]
impl ElicitationHandler for MyElicitation {
    async fn handle(
        &self,
        _sid: SessionId,
        _rid: RequestId,
        _request: ElicitationRequest,
    ) -> ElicitationResult {
        ElicitationResult::cancel()
    }
}

let config = SessionConfig::default()
    .with_permission_handler(Arc::new(ApproveAllHandler))
    .with_elicitation_handler(Arc::new(MyElicitation));
```

The handler receives a message, optional JSON Schema for form fields, and an optional mode. Known modes include `Form` and `Url`, but the mode may be absent or an unknown future value.

### User Input Requests

Some sessions ask the user free-form questions (or multiple-choice prompts) outside the elicitation flow. Install a `UserInputHandler` and the SDK will forward `userInput.request` callbacks:

```rust,ignore
use async_trait::async_trait;
use github_copilot_sdk::handler::{UserInputHandler, UserInputResponse};
use github_copilot_sdk::types::SessionId;

struct MyUserInput;

#[async_trait]
impl UserInputHandler for MyUserInput {
    async fn handle(
        &self,
        _sid: SessionId,
        question: String,
        _choices: Option<Vec<String>>,
        _allow_freeform: Option<bool>,
    ) -> Option<UserInputResponse> {
        // Render `question` + `choices` to your UI, then:
        Some(UserInputResponse {
            answer: "Yes".to_string(),
            was_freeform: false,
        })
    }
}

let config = SessionConfig::default()
    .with_user_input_handler(Arc::new(MyUserInput));
```

Return `None` to signal "no answer available" (the CLI falls back to its own prompt).

### Slash Commands

Register named commands so users can invoke them as `/name args` from the TUI:

```rust,ignore
use github_copilot_sdk::types::{CommandContext, CommandDefinition, CommandHandler};
use async_trait::async_trait;

struct DeployCommand;

#[async_trait]
impl CommandHandler for DeployCommand {
    async fn on_command(&self, ctx: CommandContext) -> Result<(), github_copilot_sdk::Error> {
        println!("deploy {}", ctx.args);
        Ok(())
    }
}

let mut config = SessionConfig::default();
config.commands = Some(vec![
    CommandDefinition::new("deploy", Arc::new(DeployCommand))
        .with_description("Deploy the application"),
]);
```

Only `name` and `description` are sent over the wire; the handler stays in your process. Returning `Err(_)` surfaces the message back through the TUI.

### Streaming

Set `streaming: true` to receive incremental delta events alongside finalized messages:

```rust,ignore
let mut config = SessionConfig::default();
config.streaming = Some(true);

let mut events = session.subscribe();
while let Ok(event) = events.recv().await {
    match event.event_type.as_str() {
        "assistant.message_delta" | "assistant.reasoning_delta" => {
            if let Some(d) = event.data.get("delta").and_then(|v| v.as_str()) {
                print!("{d}");
            }
        }
        "assistant.message" => println!(),  // final
        _ => {}
    }
}
```

When streaming is off (the default), only the final `assistant.message` and `assistant.reasoning` events fire. Delta events arrive in order; concatenating their `delta` text payloads reproduces the final message.

### Infinite Sessions

Enable the SDK's session-store integration so conversations persist across CLI restarts and grow beyond the model's context window via automatic compaction:

```rust,ignore
use github_copilot_sdk::types::InfiniteSessionConfig;

let mut infinite = InfiniteSessionConfig::default();
infinite.workspace_path = Some("/path/to/workspace".into());

let mut config = SessionConfig::default();
config.infinite_sessions = Some(infinite);
```

The CLI emits `session.compaction_start` / `session.compaction_complete` events around each compaction. The session id remains stable across compactions; resume with `Client::resume_session` to pick up a prior conversation. Workspace state lives under `~/.copilot/session-state/{sessionId}` by default — override with `workspace_path` to relocate.

### Custom Providers (BYOK)

Route model traffic through your own inference endpoint instead of GitHub's hosted models:

```rust,ignore
use github_copilot_sdk::types::ProviderConfig;

let mut provider = ProviderConfig::default();
provider.provider_type = Some("openai".to_string());
provider.base_url = "https://my-proxy.example.com/v1".to_string();
provider.bearer_token = Some(std::env::var("OPENAI_API_KEY")?);

let mut config = SessionConfig::default();
config.provider = Some(provider);
```

Provider types include `"openai"`, `"azure"`, and `"anthropic"`. Set `wire_api` to `"completions"` or `"responses"` (OpenAI/Azure only). Custom headers go in `provider.headers`. The SDK forwards the configuration to the CLI verbatim — the CLI handles the upstream call, including authentication.

### Telemetry

Forward OpenTelemetry signals from the spawned CLI process to your collector:

```rust,ignore
use github_copilot_sdk::{ClientOptions, OtelExporterType, TelemetryConfig};

let mut telem = TelemetryConfig::default();
telem.exporter_type = Some(OtelExporterType::OtlpHttp);
telem.otlp_endpoint = Some("http://localhost:4318".to_string());
telem.source_name = Some("my-app".to_string());

let mut opts = ClientOptions::default();
opts.telemetry = Some(telem);
let client = Client::start(opts).await?;
```

The SDK injects the appropriate environment variables (`COPILOT_OTEL_EXPORTER_TYPE`, `OTEL_EXPORTER_OTLP_ENDPOINT`, ...) into the spawned CLI process. The SDK takes no OpenTelemetry dependency; the CLI itself owns the exporter pipeline. Caller-supplied `ClientOptions::env` entries override telemetry-injected values.

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

## Differences From Other SDKs

The Rust SDK aligns closely with the Node, Python, Go, and .NET SDKs but diverges
in a few places where Rust idiom or the type system gives a clearly better
shape, and exposes a small additional surface where the language affords
ergonomics the dynamically-typed SDKs don't.

### Shape divergence

- **`SessionFsProvider` registration is direct, not factory-closure.** Where
  Node/Python/Go/.NET accept a closure that the runtime calls on each
  session-create to build a fresh provider, the Rust SDK takes
  `Arc<dyn SessionFsProvider>` directly via
  [`SessionConfig::with_session_fs_provider`]. The factory pattern doesn't
  cleanly express in Rust at the session-config call site — there is no
  `Session` value to thread in, and the SDK already prefers traits over
  boxed closures for handler-shaped APIs (`PermissionHandler`, `ToolHandler`,
  `SessionHooks`,
  `SystemMessageTransform`).

```rust,ignore
use std::sync::Arc;
use github_copilot_sdk::session_fs::{SessionFsConfig, SessionFsConventions};

let mut options = ClientOptions::default();
options.session_fs = Some(SessionFsConfig::new(
    "/workspace",
    "/workspace/.copilot",
    SessionFsConventions::Posix,
));
let client = Client::start(options).await?;

let session = client
    .create_session(
        SessionConfig::default()
            .with_permission_handler(Arc::new(ApproveAllHandler))
            .with_session_fs_provider(Arc::new(MyProvider::new())),
    )
    .await?;
```

See [`examples/session_fs.rs`](examples/session_fs.rs) for a complete
in-memory provider implementation.

### Rust-only API

A handful of conveniences exist only on the Rust SDK as of 0.1.0. These
are surface areas where Rust idiom (newtypes, enums, trait objects)
gives a clearly nicer shape than Node/Python/Go/.NET currently expose. Rust
gets to be Rust here — cross-SDK parity for these is a post-release
conversation, not a release blocker. None of these are deprecated and
none of them are scheduled for removal.

- **Typed newtypes** — `SessionId` and `RequestId` are `#[serde(transparent)]`
  newtypes around `String`, so the type system distinguishes a session
  identifier from an arbitrary `String` at compile time. Node/Python/Go
  use bare strings.
- **Permission policy builders** — `permission::approve_all`,
  `permission::deny_all`, and `permission::approve_if(predicate)`
  in `crate::permission` provide composable, no-handler-needed
  `PermissionHandler` shortcuts. Other SDKs require a
  full handler implementation for these patterns.
- **`Client::from_streams`** — connect to a CLI server over arbitrary
  caller-supplied `AsyncRead` / `AsyncWrite`. Useful for testing,
  in-process embedding, or custom transports. Other SDKs are spawn-only
  or fixed-stdio.
- **`enum Transport { Stdio, Tcp, External }`** — explicit, exhaustive
  transport selector on `ClientOptions::transport`. Node/Python/Go rely
  on conditional config field combinations instead.
- **Split `prefix_args` / `extra_args`** on `ClientOptions` — separate
  arg vectors for "prepend before subcommand" vs "append after the
  built-in flags", giving precise control over CLI invocation order
  without string-splicing.

## Layout

| File              | Description                                                                                                                |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `lib.rs`          | `Client`, `ClientOptions`, `CliProgram`, `Transport`, `Error`                                                              |
| `session.rs`      | `Session` struct, event loop, `send`/`send_and_wait`, `Client::create_session`/`resume_session`                            |
| `subscription.rs` | `EventSubscription` / `LifecycleSubscription` (`Stream`-able observer handles for `subscribe()` / `subscribe_lifecycle()`) |
| `handler.rs`      | `PermissionHandler`, `ElicitationHandler`, `UserInputHandler`, `ExitPlanModeHandler`, `AutoModeSwitchHandler` traits; `ApproveAllHandler`, `DenyAllHandler`           |
| `hooks.rs`        | `SessionHooks` trait, `HookEvent`/`HookOutput` enums, typed hook inputs/outputs                                            |
| `transforms.rs`   | `SystemMessageTransform` trait, section-level system message customization                                                 |
| `tool.rs`         | `ToolHandler` trait, `define_tool`, `schema_for::<T>()` (with `derive` feature)                                            |
| `types.rs`        | CLI protocol types (`SessionId`, `SessionEvent`, `SessionConfig`, `Tool`, etc.)                                            |
| `resolve.rs`      | Binary resolution (`copilot_binary`, `node_binary`, `extended_path`)                                                       |
| `embeddedcli.rs`  | Embedded CLI extraction (gated on the default `bundled-cli` feature)                                                       |
| `router.rs`       | Internal per-session event demux                                                                                           |
| `jsonrpc.rs`      | Internal Content-Length framed JSON-RPC transport                                                                          |

## Embedded CLI

The SDK bundles the Copilot CLI binary inside the consumer's compiled crate by default. No env var setup, no separate install — just `cargo build` and you get a self-contained binary.

To opt out (e.g. for binary-size-sensitive consumers, or environments that provide the CLI via PATH), set `default-features = false`:

```toml
github-copilot-sdk = { version = "0.1", default-features = false }
```

### How it works

1. **Pinned at publish time.** When the rust crate is published, a workflow step writes `bundled_cli_version.txt` (CLI version + per-platform SHA-256 hashes) into the crate from the in-effect `nodejs/package-lock.json` and the matching GitHub Release's `SHA256SUMS.txt`. This file is gitignored locally; it only exists in the published crate tarball.

2. **Build time:** The SDK's `build.rs` resolves the version + per-platform SHA-256:
   - `COPILOT_CLI_VERSION` env var (advanced override; fetches live `SHA256SUMS.txt`).
   - Otherwise, `bundled_cli_version.txt` from the published crate.
   - Otherwise (mono-repo contributor build), live read from `../nodejs/package-lock.json` + live fetch of `SHA256SUMS.txt`.

   It then downloads the platform-appropriate archive from the [`github/copilot-cli` GitHub Releases](https://github.com/github/copilot-cli/releases) (`copilot-{platform}.tar.gz` on macOS/Linux, `.zip` on Windows), verifies the SHA-256, extracts the `copilot` binary, compresses it with zstd, and embeds via `include_bytes!()`.

3. **Runtime:** On the first call to `github_copilot_sdk::resolve::copilot_binary()`, the embedded binary is lazily extracted to `~/.cache/github-copilot-sdk-{version}/copilot` (or `copilot.exe` on Windows), SHA-256 verified, and cached. Subsequent calls return the cached path.

### Overriding the extraction location

Use [`ClientOptions::with_bundled_cli_extract_dir`] when you need to place the extracted binary somewhere other than `~/.cache/...` (CI runners with ephemeral homes, sandboxes that disallow XDG paths, etc.):

```rust,ignore
use std::path::PathBuf;
use github_copilot_sdk::{Client, ClientOptions};

let options = ClientOptions::new()
    .with_bundled_cli_extract_dir(PathBuf::from("/var/run/my-app/copilot"));
let client = Client::start(options).await?;
```

### Resolution priority

`copilot_binary()` checks these sources in order:

1. `COPILOT_CLI_PATH` environment variable
2. Embedded CLI (when the `bundled-cli` feature is enabled, which it is by default)
3. System PATH + common install locations

### Platforms

Supported: `darwin-arm64`, `darwin-x64`, `linux-x64`, `linux-arm64`, `win32-x64`, `win32-arm64`. The target platform is auto-detected from `CARGO_CFG_TARGET_OS` and `CARGO_CFG_TARGET_ARCH` (cross-compilation works).

## Features

| Feature        | Default | Description                                                                                                                                               |
| -------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `bundled-cli`  | ✓       | Build-time CLI embedding. Pulls in `sha2`, `zstd`. Disable via `default-features = false` to opt out (e.g. when shipping a smaller binary). |
| `derive`       | —       | `schema_for::<T>()` for generating JSON Schema from Rust types (adds `schemars`). Enable when defining [tool parameters](#tool-registration).             |

```toml
# These examples use registry syntax for illustration; until the crate is
# published, use a path or git dependency instead.

# Default — bundles the Copilot CLI in your binary.
github-copilot-sdk = "0.1"

# Opt out of bundling — resolve CLI from COPILOT_CLI_PATH or system PATH instead.
github-copilot-sdk = { version = "0.1", default-features = false }

# Derive JSON Schema for tool parameters (adds to default bundled-cli).
github-copilot-sdk = { version = "0.1", features = ["derive"] }
```
