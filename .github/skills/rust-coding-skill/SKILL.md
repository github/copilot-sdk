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

Plain functions on a type beat traits for navigability — IDE "Go to definition"
on an inherent method jumps directly to the implementation, while a trait method
hops to the trait declaration first. Use that as the default.

There are four intentional exceptions where the SDK exposes a trait because it
*is* an extension point — code paths consumers must be able to plug behaviour
into:

- **`SessionHandler`** (`rust/src/handler.rs`) — single `on_event()` dispatches
  CLI events. Notification-triggered events (`permission.requested`,
  `external_tool.requested`, `elicitation.requested`) are dispatched on spawned
  tasks, so implementations must be safe for concurrent invocation. Use
  `ApproveAllHandler` in tests and examples.
- **`SessionHooks`** (`rust/src/hooks.rs`) — optional lifecycle callbacks. The
  SDK auto-enables hooks (`config.hooks = Some(true)`) when an impl is supplied
  to `create_session` / `resume_session`.
- **`SystemMessageTransform`** (`rust/src/system_message.rs`) — declare
  `section_ids()` and return content from `transform_section()`.
- **`ToolHandler`** (`rust/src/tool.rs`) — client-side tool implementations.
  Dispatch by name via `ToolHandlerRouter`.

Don't add new traits without a clear extension story. In particular, don't
implement `From`/`Into` for SDK-internal conversions: they can't take extra
parameters, can't return `Result`, and hide which conversion is happening at
call sites. Prefer named methods like `to_info(&self)` or
`MyType::from_record(record, ctx)`.

Trivial field re-shaping ("flatten this struct into that one") is best inlined
at the call site. A free-standing `map_x_to_y(x) -> Y` adds a hop without
adding clarity.

Closures should stay short — under ~10 lines is a good rule. Long anonymous
closures show up as opaque frames in stack traces. Extract them to named
functions when they grow. Visitor patterns are a closure-fest in disguise;
expose an `iter()` method instead and let the consumer drive the traversal.

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

When porting code from the Node, Python, Go, .NET, or any other SDK,
four idioms reliably translate poorly into idiomatic Rust. Each has
specific guidance:

### Event subscription: channels (and `Stream`), not callbacks

Other SDKs expose callback registration:

- Node / Python: `client.on('event', handler)` / `add_listener`
- C#: `event` declarations and `+= handler`, or `IObservable<T>`
- Go: `for ev := range ch { ... }` (closer to Rust already)

Rust's async ecosystem prefers explicit channels over callback closures
because closures fight `Send + Sync + 'static` and don't compose with
`select!`/`StreamExt`. Pick the channel type by semantics:

| Use case | Primitive |
|---|---|
| One producer → one consumer with backpressure | `tokio::sync::mpsc` (cap 1) or `tokio::sync::oneshot` for single value |
| Many producers → one consumer (work queue, command bus) | `tokio::sync::mpsc` |
| One producer → many consumers, every event delivered (pub/sub) | `tokio::sync::broadcast` |
| One producer → many consumers, only the **latest** value matters (current state) | `tokio::sync::watch` |

For the **public** API, prefer returning `impl Stream<Item = Event>`
(typically by wrapping a `broadcast::Receiver` in
`tokio_stream::wrappers::BroadcastStream`). `Stream` is the canonical
"observable" shape in Rust — it composes with `select!`, `take`, `map`,
`filter`, `timeout`, etc. Internally use a channel; externally consider
exposing a `Stream`. This is what `tonic`, `reqwest::Response::bytes_stream`,
and `sqlx::query::fetch` expose. See `EventSubscription` and
`LifecycleSubscription` for the canonical examples in this crate.

`Fn`-callback registration (`on_event(handler)`) is not an outright
anti-pattern — `notify` (the FS watcher) and `bevy` use it idiomatically
for non-async / domain-specific contexts — but for an async SDK exposing
events to user code, channels + `Stream` is the canonical shape.

### Cancellation: drop is the primitive; `CancellationToken` for SDK-internal coordination

Cancellation does NOT plumb through every call site like Go's
`context.Context` or .NET's `CancellationToken`. Two distinct cases,
both idiomatic:

**1. Caller-owned futures (`send_message`, `send_and_wait`, subscription streams).**
Drop the future / `select!` it out / wrap in `tokio::time::timeout`.
The caller already has full lifecycle control via the value's lifetime;
adding a token parameter just duplicates what `select!`/`timeout`/drop
already provide. This is what `reqwest`, `sqlx`, the `aws-sdk-*` crates,
and `tonic`'s client side do. **Don't accept a token here.**

Document cancel-safety on every `.await` in the SDK's hot path the way
[`tokio` itself does](https://docs.rs/tokio/latest/tokio/macro.select.html#cancellation-safety):
state explicitly which operations are safe to cancel mid-flight and
which are not.

**2. SDK-internal task coordination (event loops, subprocess readers,
spawned background tasks).** Use [`tokio_util::sync::CancellationToken`].
This is the canonical Rust analog to Go's `ctx.Done()` and .NET's
`CancellationToken`, but scoped to where it actually belongs: tasks the
caller doesn't own. `tonic` uses it to propagate client-disconnect into
spawned server handlers; `tokio-graceful-shutdown` builds a whole
hierarchical-shutdown framework on it. The token's parent/child tree
maps cleanly onto session/request scoping.

In this SDK, `Session.shutdown: CancellationToken` ties the event loop
and any spawned helpers to the session's lifetime. `Drop for Session`
calls `cancel()`. Power users can call
`Session::cancellation_token() -> CancellationToken` to get a child
token and bind their own work to the session lifetime via `select!`.
Cancelling the child does NOT cancel the parent — child cancellation is
isolated by design.

**Citations**: [`tokio_util::sync::CancellationToken` docs][ctoken]
([`tonic` cancellation example][tonic-cancel]),
[withoutboats: "Asynchronous clean-up"][wb-cleanup],
[Cybernetist: "Rust tokio task cancellation patterns"][cybernetist].

[ctoken]: https://docs.rs/tokio-util/latest/tokio_util/sync/struct.CancellationToken.html
[tonic-cancel]: https://github.com/hyperium/tonic/blob/master/examples/src/cancellation/server.rs
[wb-cleanup]: https://without.boats/blog/asynchronous-clean-up/
[cybernetist]: https://cybernetist.com/2024/04/19/rust-tokio-task-cancellation-patterns/

### Optional fields: `Option<T>`, not nullable pointers or zero values

`Option<T>`, not nullable references or "empty string means missing"
sentinels. Defaults come from `Default` impls, not from constructors
that accept zero values. Pair with `#[non_exhaustive]` on public config
structs and a builder so adding fields stays non-breaking — this is the
AWS SDK convention. If the SDK has *required* builder fields and you
want compile-time enforcement of `.build()` validity, prefer
`build() -> Result<Self, BuildError>` over typestate unless the
required-field count is tiny (1-2). Typestate is overkill for plain
optional fields.

### serde JSON: container `rename_all` plus per-field overrides

JSON tag attributes become `#[serde(rename_all = "camelCase")]` at the
type level, with per-field `#[serde(rename = "…")]` overrides for
outliers. For optional output fields use
`#[serde(skip_serializing_if = "Option::is_none")]` to omit unset
values from the wire (the JSON-RPC convention this SDK follows
matches LSP's). Use `#[serde(default)]` for forward/backward-compatible
input. `serde_with` is the right escape hatch for non-trivial transforms
(durations, base64, numeric-as-string keys); reach for it as needed,
not by default.

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
