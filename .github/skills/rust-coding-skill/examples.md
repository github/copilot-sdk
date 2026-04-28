# Rust Coding Skill — Examples

Patterns specific to the Rust SDK in this repo (`rust/`) that aren't obvious
from general Rust knowledge.

## Defining a tool

### Anti-pattern — building the wire payload by hand

```rust
let raw = serde_json::json!({
    "name": "get_weather",
    "description": "...",
    "parameters": { "type": "object", ... },
});
config.tools = Some(vec![serde_json::from_value(raw)?]);
```

### Preferred — implement `ToolHandler`, route via `ToolHandlerRouter`

```rust
use copilot::tool::{Tool, ToolHandler, ToolHandlerRouter, ToolInvocation, ToolResult};
use copilot::Error;

struct GetWeatherTool;

#[async_trait::async_trait]
impl ToolHandler for GetWeatherTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "get_weather".to_string(),
            description: "Get the current weather for a city.".to_string(),
            // ..Default::default() — leaves namespaced_name, instructions,
            // overrides_built_in_tool, skip_permission at their defaults.
            ..Default::default()
        }
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        // ...
        Ok(ToolResult::Text("...".into()))
    }
}

use copilot::handler::ApproveAllHandler;
use std::sync::Arc;

let router = ToolHandlerRouter::new(
    vec![Box::new(GetWeatherTool)],
    Arc::new(ApproveAllHandler),
);
```

## Spans for spawned event loops

The session event loop is spawned per session. Always attach a span so events
emitted inside it correlate.

### Anti-pattern — losing parent context

```rust
tokio::spawn(async move {
    while let Some(event) = rx.recv().await {
        info!("event {:?}", event);  // No span — can't filter by session
    }
});
```

### Preferred — `error_span!` + `.instrument()`

```rust
use tracing::Instrument;

let span = tracing::error_span!("session_event_loop", session_id = %id);
tokio::spawn(async move {
    while let Some(event) = rx.recv().await {
        info!(event_type = ?event.kind, "session event");
    }
}.instrument(span));
```

## Concurrent permission handlers

`HandlerEvent::PermissionRequest` and `HandlerEvent::ExternalTool` are dispatched
on spawned tasks (see `rust/src/session.rs:973` and `:1022`). Implementations
must be safe for concurrent invocation.

### Anti-pattern — non-`Send` mutable state in the handler

```rust
struct MyHandler {
    last_request: std::cell::RefCell<Option<String>>,  // not thread-safe
}
```

### Preferred — `parking_lot::Mutex` or atomics

```rust
struct MyHandler {
    last_request: parking_lot::Mutex<Option<String>>,
}
```

## Adding a field to a public struct

Adding a field to a public, non-exhaustive struct is a breaking change because
existing callers' struct literals stop compiling. Two patterns soften this:

### Pattern 1 — `Default` + `..Default::default()` in docs

```rust
#[derive(Default)]
pub struct Tool {
    pub name: String,
    pub description: String,
    // new field
    pub overrides_built_in_tool: bool,
}

// In docs and examples:
let t = Tool {
    name: "x".into(),
    description: "y".into(),
    ..Default::default()
};
```

### Pattern 2 — `#[non_exhaustive]` for types callers shouldn't construct

Use sparingly — only for types that are *only* meant to be received from the
SDK, never built by users.

```rust
#[non_exhaustive]
pub struct CreateSessionResult {
    pub session_id: SessionId,
    // ...
}
```

## Test handler for non-permission scenarios

When a test doesn't exercise the permission flow, use the SDK's built-in
`ApproveAllHandler` instead of writing a custom one:

```rust
use copilot::handler::ApproveAllHandler;
use copilot::types::SessionConfig;
use std::sync::Arc;

let session = client
    .create_session(SessionConfig::default().with_handler(Arc::new(ApproveAllHandler)))
    .await?;
```

## Regenerating types after a schema bump

```bash
# 1. Update schema (usually arrives with @github/copilot package update)
cd nodejs && npm install @github/copilot@latest && cd ..

# 2. Regenerate Rust types
cd scripts/codegen && npm run generate:rust

# 3. Verify
cd ../../rust && cargo check --all-features
```

If a generated type changes shape, hand-fix any user-facing wrappers in
`rust/src/types.rs` rather than monkey-patching the generated file.
