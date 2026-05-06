---
name: rust-coding-skill
description: "Use this skill whenever editing `*.rs` files in the `rust/` SDK in order to write idiomatic, efficient, well-structured Rust code"
---

# Rust Coding Skill

Opinionated Rust rules for the Copilot Rust SDK (`rust/`). Priority order:

1. **Readable code** — every line should earn its place
2. **Correct code** — especially in concurrent/async contexts
3. **Performant code** — think about allocations, data structures, hot paths

## Error handling

The SDK's public error type is `crate::Error` (`rust/src/error.rs`). Add new
variants there rather than introducing parallel error enums per module — every
public failure mode is part of the API contract and should be expressible in one
type. Internal modules can use `thiserror` enums when a richer local taxonomy
helps; convert at the boundary.

`anyhow` is reserved for binaries and example code. Library code never returns
`anyhow::Result` — callers can't pattern-match on `anyhow::Error`, so it would
prevent them from handling specific failures.

In production code, prefer `?`, `let-else`, and `if let`. Reach for `expect("…")`
when an invariant cannot fail and the message would help debug a future
regression. `unwrap()` belongs in tests only — Clippy enforces this in the SDK
via `#![cfg_attr(test, allow(clippy::unwrap_used))]` in `lib.rs`.

When you need to log on the way through, prefer
`.inspect_err(|e| warn!(error = ?e, "context"))?` over a `match` that logs and
re-wraps. It reads top-to-bottom and keeps the happy path uncluttered.

## Async and concurrency

The default for request-scoped I/O is `async fn` plus `.await` — futures
inherit cancellation from their parent task and can borrow local references.
Reach for `tokio::spawn` only when you genuinely need background work (an event
loop, a long-lived watcher) and track the `JoinHandle` so you can cancel or join
it on shutdown. Fire-and-forget spawns silently swallow panics and outlive the
session; don't.

Blocking calls (filesystem, subprocess wait) belong in
`tokio::task::spawn_blocking`, *not* on the async runtime. The blocking pool is
bounded, so for genuinely long-lived workers (think: file watchers that run for
the lifetime of a session) prefer `std::thread::spawn` with a channel back into
async land.

Lock choice matters. `tokio::sync::Mutex` is correct when you must hold the
guard across `.await`; `parking_lot::Mutex` (or `RwLock`) is faster on hot
synchronous paths and is what `session.rs` uses for capability state.
`std::sync::Mutex` is rarely the right answer in this crate — its poisoning
semantics buy us nothing and it's slower than `parking_lot`. Never hold a
`std::sync::Mutex` guard across an `.await`; Clippy will catch this, but the
fix is to move the await out, not silence the lint.

For lazy statics use `std::sync::LazyLock`. The `once_cell` crate is no longer
needed.

## Traits and conversions

Plain functions on a type beat traits for navigability. Use them as the default.

**Trait-based extension points are different.** When a consumer must plug behaviour into the SDK, prefer one trait with one default-impl method per event over per-event `Box<dyn Fn>` callback fields. This is what `tower_lsp::LanguageServer`, `rmcp::ServerHandler`, and `notify::EventHandler` do — the dominant idiom in async Rust for "wire-protocol handler" traits. Callback fields fight `Send + Sync + 'static`, fragment consumer state across closures, and skip exhaustiveness checks.

The four extension-point traits in this crate:

- **`SessionHandler`** (`rust/src/handler.rs`) — per-event methods (`on_permission_request`, `on_user_input`, `on_external_tool`, `on_elicitation`, `on_exit_plan_mode`, `on_auto_mode_switch`, `on_session_event`) each with a default impl. The dispatcher `on_event(HandlerEvent)` is itself a default method that fans out to them; override per-event methods in normal use, override `on_event` only when you want a single exhaustive match. Concurrent invocations are possible (notification-triggered events run on spawned tasks), so `Send + Sync + 'static` is required on the trait.
- **`SessionHooks`** (`rust/src/hooks.rs`) — optional lifecycle callbacks. The SDK auto-enables hooks when an impl is supplied to `create_session` / `resume_session`.
- **`SystemMessageTransform`** (`rust/src/system_message.rs`) — declare `section_ids()` and return content from `transform_section()`.
- **`ToolHandler`** (`rust/src/tool.rs`) — client-side tool implementations, dispatched by name via `ToolHandlerRouter`.

`ApproveAllHandler` is the standard test handler for `SessionHandler`.

**Don't add traits without a clear extension story.** Don't implement `From`/`Into` for SDK-internal conversions: they can't take extra parameters, can't return `Result`, and hide which conversion is happening at call sites. Prefer named methods like `to_info(&self)` or `MyType::from_record(record, ctx)`.

Trivial field re-shaping is best inlined. Closures should stay short (under ~10 lines); extract to named functions when they grow. Visitor patterns are a closure-fest — expose `iter()` and let the consumer drive.

## Concurrency primitives

**Channels, not callback closures, for event flow.** Closures fight `Send + Sync + 'static` and don't compose with `select!`. Channel choice by semantics:

| Use case | Primitive |
|---|---|
| One producer → one consumer with backpressure | `tokio::sync::mpsc` (cap 1) or `tokio::sync::oneshot` for single value |
| Many producers → one consumer | `tokio::sync::mpsc` |
| One producer → many consumers, every event delivered (pub/sub) | `tokio::sync::broadcast` |
| One producer → many consumers, only the latest value matters | `tokio::sync::watch` |

For the **public** API, prefer returning `impl Stream<Item = Event>` (wrap a `broadcast::Receiver` in `tokio_stream::wrappers::BroadcastStream`). `Stream` composes with `select!`, `take`, `map`, `filter`, `timeout`. See `EventSubscription` and `LifecycleSubscription`.

**Cancellation: drop is the primitive; `tokio_util::sync::CancellationToken` for SDK-internal task coordination.**

- **Caller-owned futures** (`send_message`, subscription streams): drop / `select!` / `tokio::time::timeout`. Don't accept a token parameter — it duplicates what `select!` already provides. Document cancel-safety on every `.await` in the hot path.
- **SDK-internal tasks** (event loops, subprocess readers, anything `tokio::spawn`ed by the SDK): use `CancellationToken` stored on the long-lived handle. `Drop` calls `cancel()`. `Session::cancellation_token()` exposes a child token so callers can bind external work to the session lifetime.

Refs: [`CancellationToken`][ctoken] · [`tonic` example][tonic-cancel] · [withoutboats: async clean-up][wb-cleanup] · [Cybernetist: cancellation patterns][cybernetist].

[ctoken]: https://docs.rs/tokio-util/latest/tokio_util/sync/struct.CancellationToken.html
[tonic-cancel]: https://github.com/hyperium/tonic/blob/master/examples/src/cancellation/server.rs
[wb-cleanup]: https://without.boats/blog/asynchronous-clean-up/
[cybernetist]: https://cybernetist.com/2024/04/19/rust-tokio-task-cancellation-patterns/

## Optional fields and serde

Use `Option<T>` for optional fields, not nullable references or sentinel values. Defaults come from `Default` impls. Pair with `#[non_exhaustive]` on public config structs and a builder so adding fields stays non-breaking.

For required builder fields: prefer `build() -> Result<Self, BuildError>` over typestate unless required-field count is tiny (1-2).

JSON: `#[serde(rename_all = "camelCase")]` at the type level, per-field `#[serde(rename = "…")]` for outliers, `#[serde(skip_serializing_if = "Option::is_none")]` for output, `#[serde(default)]` for input tolerance. Reach for `serde_with` only for non-trivial transforms (durations, base64, numeric-as-string keys).

## Tracing — `#[tracing::instrument]` is banned

Banned via `clippy.toml`. Use manual spans with `error_span!`:

- **Almost always use `error_span!`**, not `info_span!`. Span level controls
  the *minimum* filter at which the span appears. An `info_span` disappears when
  the filter is `warn` or `error` — taking all child events with it, even
  errors. `error_span!` ensures the span is always present.
- **Spawned tasks lose parent context.** Attach a span with `.instrument()` or
  events inside won't correlate.
- **Never hold `span.enter()` guards across `.await`** — use `.instrument(span)`
  instead (also enforced by Clippy).

```rust
use tracing::Instrument;

async fn send_message(&self, session_id: &str, prompt: &str) -> Result<(), Error> {
    let span = tracing::error_span!("send_message", session_id = %session_id);
    async { /* body */ }.instrument(span).await
}

let span = tracing::error_span!("event_loop", session_id = %id);
tokio::spawn(async move { run_loop().await }.instrument(span));
```

Log with structured fields: `info!(session_id = %id, "Session created")`.
Static messages stay greppable; dynamic data goes in named fields, not
interpolated into the message string.

## Idioms that don't port from other languages

When porting from Node, Python, Go, or .NET: see the **Concurrency primitives** and **Traits and conversions** sections above. The two patterns that most reliably translate poorly are (1) per-event `Box<dyn Fn>` callback fields — use a trait with default-impl methods (the `tower_lsp::LanguageServer` / `rmcp::ServerHandler` / `notify::EventHandler` shape) — and (2) plumbing `context.Context` / `CancellationToken` through every call site — drop-cancel for caller-owned futures, `tokio_util::sync::CancellationToken` for SDK-internal tasks.

## Code organization

- **Public API:** every `pub` item in the crate is part of the SDK's contract.
  Adding a field to a `pub struct` is a breaking change unless the struct is
  `#[non_exhaustive]` or constructors hide field-by-field literals. Prefer
  `Default + ..Default::default()` patterns and document new fields with
  rustdoc.
- **Generated code lives in `rust/src/generated/`** and must not be
  hand-edited. Regenerate with `cd scripts/codegen && npm run generate:rust`.
  When a generated type lacks a field the schema doesn't yet describe (e.g.
  `Tool::overrides_built_in_tool`), hand-author the user-facing type in
  `rust/src/types.rs` and stop re-exporting the generated one.
- **`#[expect(dead_code)]`** instead of `#[allow(dead_code)]` on individual
  fields — it forces a cleanup once the field gets used.
- **`..Default::default()`** — avoid in production code (be explicit about
  which fields you're setting); prefer it in tests and doc examples to keep
  the focus on the values that matter for the test.
- **Import grouping** — three blocks separated by blank lines:
  (1) `std`/`core`/`alloc`, (2) external crates, (3)
  `crate::`/`super::`/`self::`. Enforced by nightly `cargo fmt` via
  `rust/.rustfmt.nightly.toml`.
- **`pub(crate)` vs `pub`** — most modules in `lib.rs` are private (`mod`), so
  `pub` items inside them are already crate-private. Use `pub(crate)` only when
  you want to be explicit that an item must not become part of the public API.

## Testing

- **No mock testing.** Depend on real implementations, spin up lightweight
  versions (e.g. `MockServer` in tests), or restructure code so the logic
  under test takes its dependency's output as input.
- `assert_eq!(actual, expected)` — actual first, for readable diffs.
- Tests at end of file: `#[cfg(test)] mod tests`. Never place production code
  after the test module.
- Keep tests concurrent-safe — unique temp dirs (`tempfile::tempdir()`),
  unique data, no global state.
- `ApproveAllHandler` is the standard test handler for sessions that don't
  exercise permission logic — see `rust/src/handler.rs:174`.

## Cross-platform

The SDK ships on macOS, Windows, and Linux; CI exercises all three. Construct
paths with `Path::join` rather than string concatenation — `/` and `\` are not
interchangeable, and string equality breaks on Windows UNC paths. Log paths
with `path.display()`; serialize with `to_string_lossy()` only when you need a
`String`.

Process spawning needs care. The SDK applies `CREATE_NO_WINDOW` on Windows
when launching the CLI (see `Client::build_command`); preserve that if you
touch process spawning. Subprocess stdout often contains `\r` on Windows — strip
or split on `\r?\n` rather than assuming `\n`.

Tests must use `tempfile::tempdir()`, never hardcoded `/tmp/`, and any test
that asserts on a path string needs to normalize separators or use
`std::path::MAIN_SEPARATOR`.

## Build speed

Specify Tokio features explicitly — never `features = ["full"]`. Iterate with
`cargo check`; reach for `cargo build` only when you need the binary. Audit
new dependency feature flags with `cargo tree` before committing.

## Comments

Explain **why**, never **what**. No comments that restate code. No decorative
banners (`// ── Section ────────`).

**Never compare to other SDKs in code comments or rustdoc.** Don't write
"Mirrors Node's `Foo`", "Like Go's `Bar`", "Unlike Python's `Baz`", or include
file/line citations into other SDKs (`nodejs/src/types.ts:1592`, `go/types.go:14`).
The Rust SDK seeks parity with the Node, Python, Go, and .NET SDKs, and that
fact is stated once at the top of `rust/README.md`. Intentional divergences
live in the README's "Differences From Other SDKs" section. Repeating the
relationship per-symbol is unscalable, drifts as the other SDKs evolve, and
adds noise to consumer-facing rustdoc — Rust users care about the Rust API,
not its lineage. Self-references within the Rust crate (e.g. "Mirrors
[`from_streams`] but adds…") are fine.

## Toolchain

The SDK is pinned to `rust 1.94.0` via `rust/rust-toolchain.toml`. Formatting
uses nightly (`nightly-2026-04-14`) so unstable rustfmt options like grouped
imports work — see `rust/.rustfmt.nightly.toml`. CI runs:

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
field the schema lacks, hand-author the user-facing type in `rust/src/types.rs`
and stop re-exporting the generated one.
