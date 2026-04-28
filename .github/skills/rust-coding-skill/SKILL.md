---
name: rust-coding-skill
description: "Use this skill whenever editing `*.rs` files in the `rust/` SDK in order to write idiomatic, efficient, well-structured Rust code"
---

# Rust Coding Skill

Opinionated Rust rules for the Copilot Rust SDK (`rust/`). Priority order:

1. **Readable code** — every line should earn its place
2. **Correct code** — especially in concurrent/async contexts
3. **Performant code** — think about allocations, data structures, hot paths

## Error Handling

| Rule | Do | Don't |
|------|----|-------|
| Module boundaries | `thiserror` enum | `Box<dyn Error>` |
| Binary boundary only | `anyhow::Result` | `anyhow` in library code |
| Production code | `?`, `let-else`, `if let`, `expect("reason")` | `unwrap()` (tests only) |
| Log-and-propagate | `inspect_err` + `warn!`, then `?` | `match` that logs and re-wraps |

The SDK's public error type is `crate::Error` (see `rust/src/error.rs`). Add new error
variants there rather than introducing parallel error enums per module.

## Async & Concurrency

| Rule | Do | Don't |
|------|----|-------|
| Request-scoped I/O | `async fn` + `.await` (futures) | `tokio::spawn` per request |
| Background work | `tokio::spawn` + track `JoinHandle` | Fire-and-forget spawn |
| Blocking I/O (fs, subprocess) | `tokio::task::spawn_blocking` | Blocking the async runtime |
| Long-lived workers | `std::thread::spawn` | `spawn_blocking` (pool is bounded) |
| Locks in async | `tokio::sync::Mutex` | `std::sync::Mutex` across `.await` |
| Hot-path sync locks | `parking_lot::Mutex` | `std::sync::Mutex` |
| Lazy statics | `std::sync::LazyLock` | `once_cell::Lazy` |

The SDK already uses `parking_lot::RwLock` for hot-path session capability state and
`tokio::sync::Mutex` for the idle-waiter rendezvous in `session.rs`. Match those patterns.

## Traits & Conversions

| Rule | Do | Don't |
|------|----|-------|
| Trait usage | Plain functions on the type | Traits (break code navigation) |
| Trivial field mapping | Construct struct inline at call site | Free-standing `map_x_to_y()` functions |
| Reusable conversion | Named method: `into_bar(self)`, `to_info(&self)`, `MyType::from_record(r)` | `From`/`Into` (can't express extra params or context) |
| Closures | Keep <10 lines; extract to named fn if larger | Long anonymous closures (invisible in stack traces) |
| Visitor pattern | Extract traversal into `iter()` method | Trait-based visitors |

**Intentional trait exceptions in this SDK** — these are consumer extension points
and stay as traits:

- **`SessionHandler`** (`rust/src/handler.rs`) — required handler. Single
  `on_event()` dispatches CLI events. Notification-triggered events
  (`permission.requested`, `external_tool.requested`, `elicitation.requested`) are
  dispatched on spawned tasks and may run concurrently — implementations must be
  safe for concurrent invocation. Use `ApproveAllHandler` in tests/examples.
- **`SessionHooks`** (`rust/src/hooks.rs`) — optional lifecycle callbacks. The SDK
  auto-enables hooks (`config.hooks = Some(true)`) when a `hooks` impl is provided
  to `create_session` / `resume_session`.
- **`SystemMessageTransform`** (`rust/src/system_message.rs`) — optional system
  message customization. Declare `section_ids()`, return content from
  `transform_section()`.
- **`ToolHandler`** (`rust/src/tool.rs`) — client-side tools. Use
  `ToolHandlerRouter` to dispatch by name.

## Tracing — `#[tracing::instrument]` is banned

Banned via `clippy.toml`. Use manual spans with `error_span!`:

- **Almost always use `error_span!`**, not `info_span!`. Span level controls the *minimum* filter at which the span appears. An `info_span` disappears when the filter is `warn` or `error` — taking all child events with it, even errors. `error_span!` ensures the span is always present.
- **Spawned tasks lose parent context.** Attach a span with `.instrument()` or events inside won't correlate.
- **Never hold `span.enter()` guards across `.await`** — use `.instrument(span)` instead (also enforced by clippy).

```rust
use tracing::Instrument;

async fn send_message(&self, session_id: &str, prompt: &str) -> Result<(), Error> {
    let span = tracing::error_span!("send_message", session_id = %session_id);
    async { /* body */ }.instrument(span).await
}

// Spawned tasks need explicit span attachment
let span = tracing::error_span!("event_loop", session_id = %id);
tokio::spawn(async move { run_loop().await }.instrument(span));
```

Log with structured fields: `info!(session_id = %id, "Session created")` — static
messages are greppable; dynamic data goes in named fields, not interpolated into
the message string.

## Code Organization

- **Public API:** every `pub` item in the crate is part of the SDK's contract.
  Adding fields to a `pub struct` is a breaking change unless the struct is
  `#[non_exhaustive]` or constructors hide field-by-field literals. Prefer
  `Default + ..Default::default()` patterns and document new fields with rustdoc.
- **Generated code lives in `rust/src/generated/`** and must not be hand-edited.
  Regenerate with `cd scripts/codegen && npm run generate:rust`. Hand-author
  user-facing types in `rust/src/types.rs` when you need fields the schema
  doesn't yet have (e.g. `Tool::overrides_built_in_tool`).
- **`#[expect(dead_code)]`** instead of `#[allow(dead_code)]` on individual fields.
- **`..Default::default()`** — avoid in production (be explicit), prefer in tests
  and doc examples to reduce boilerplate when adding fields.
- **Import grouping** — three blocks separated by blank lines: (1) `std`/`core`/`alloc`,
  (2) external crates, (3) `crate::`/`super::`/`self::`. Enforced by nightly
  `cargo fmt` (`rust/.rustfmt.nightly.toml`).
- **`pub(crate)` vs `pub`** — most modules in `lib.rs` are private (`mod`), so
  `pub` items inside them are already crate-private. Use `pub(crate)` only when
  you want to be explicit that an item must not become part of the public API.

## Testing

- **Avoid mock testing.** Depend on real implementations, spin up lightweight
  versions (e.g. `MockServer` in tests), or restructure code so logic takes
  dependency output as input.
- **`assert_eq!(actual, expected)`** — actual first for readable diffs.
- **`#[cfg(test)] mod tests` at end of file.** Never place production code after it.
- **Concurrent-safe tests** — unique temp dirs (`tempfile::tempdir()`), unique
  data. Avoid global state.
- **`ApproveAllHandler`** is the standard test handler for sessions that don't
  exercise permission logic — see `rust/src/handler.rs:174`.

## Cross-Platform

The SDK ships on macOS, Windows, and Linux (CI tests all three).

| Rule | Do | Don't |
|------|----|-------|
| Path construction | `Path::join()` | String concat with `/` or `\` |
| Path comparison | Compare `Path` values | String equality (breaks on Windows UNC) |
| Path logging | `path.display()` for logs, `to_string_lossy()` for serialization | Mixing the two |
| Platform code | Handle all 3 OSes or provide fallback | Missing platform branches |
| Process spawning | Use `tokio::process::Command`; watch for `\r` in stdout | Assuming `sh -c` everywhere |
| Test paths | `tempfile::tempdir()` | Hardcoded `/tmp/` |
| Test path assertions | Normalize separators or use `MAIN_SEPARATOR` | Direct string comparison |

The SDK applies `CREATE_NO_WINDOW` on Windows when spawning the CLI (see
`Client::build_command`). Preserve that if you touch process spawning.

## Build Speed

| Rule | Do | Don't |
|------|----|-------|
| Tokio features | Specify explicitly | `features = ["full"]` |
| Iteration | `cargo check` | `cargo build` |
| Dependencies | Minimize feature flags; audit with `cargo tree` | Kitchen-sink features |

## Comments

- Explain **why**, never **what**. No comments that restate code.
- No decorative banner/divider comments (e.g. `// ── Section ────────`).

## Toolchain

The SDK is pinned to `rust 1.94.0` via `rust/rust-toolchain.toml`. Formatting uses
nightly (`nightly-2026-04-14`) so unstable rustfmt options like grouped imports
work — see `rust/.rustfmt.nightly.toml`. CI runs:

```bash
cd rust
cargo +nightly-2026-04-14 fmt --check
cargo clippy --all-features --all-targets -- -D warnings
cargo test --all-features
```

Match those exact commands locally before pushing.

## Codegen

JSON-RPC and session-event types are generated from the Copilot CLI schema:

| Source | Output |
|---|---|
| `nodejs/node_modules/@github/copilot/schemas/api.schema.json` | `rust/src/generated/api_types.rs` |
| `nodejs/node_modules/@github/copilot/schemas/session-events.schema.json` | `rust/src/generated/session_events.rs` |

Regenerate with:

```bash
cd scripts/codegen && npm run generate:rust
```

Never hand-edit files under `rust/src/generated/`. If a generated type needs a
field that the schema lacks (e.g. tool runtime hints), hand-author the user-facing
type in `rust/src/types.rs` and stop re-exporting the generated one.
