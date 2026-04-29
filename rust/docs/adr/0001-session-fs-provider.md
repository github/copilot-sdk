# ADR 0001: SessionFsProvider trait and plumbing

- **Status:** Proposed
- **Date:** 2026-04-29
- **Deciders:** @tclem and Rust SDK working group
- **Phase:** Public release § 4.2 (last-mile parity gap before 1.0)
- **Cross-SDK reference:** Node `nodejs/src/sessionFsProvider.ts`,
  Python `python/copilot/session_fs_provider.py`, Go `go/session_fs_*.go`

## Context

The Copilot CLI exposes a virtualizable filesystem layer ("SessionFs") over
JSON-RPC. When a host application opts in, the CLI delegates all per-session
file I/O — `readFile`, `writeFile`, `appendFile`, `exists`, `stat`, `mkdir`,
`readdir`, `readdirWithTypes`, `rm`, `rename` — to the SDK consumer instead
of touching the real filesystem. This lets hosts (e.g. desktop apps, IDE
plugins, browser-based environments) sandbox sessions, project files into
in-memory or remote storage, and apply permission policies before bytes
move.

Node, Python, and Go SDKs all expose this surface. The Rust SDK ships
without it. § 4.2 of the public release plan calls this out as a 1.0
blocker.

The schema-side wire types are already generated and live in
`rust/src/generated/api_types.rs` (`SessionFsReadFileRequest`,
`SessionFsStatResult`, `SessionFsErrorCode`, ...) and
`rust/src/generated/rpc.rs` (`ClientRpcSessionFs::set_provider`,
`SESSIONFS_*` method-name constants). No codegen work is required — the
gap is the consumer-facing trait, the registration plumbing, and the
inbound-request dispatch arm.

The two distinct touch points:

1. **Outbound handshake.** Once at startup, the SDK calls
   `sessionFs.setProvider(initialCwd, sessionStatePath, conventions)` to
   tell the CLI it should route filesystem requests to the SDK instead
   of using the real filesystem. This is a client-level concern.

2. **Inbound per-session dispatch.** After `setProvider`, every CLI
   filesystem call lands as a JSON-RPC request *to* the SDK, scoped by
   `sessionId`. The SDK must look up the session's registered
   `SessionFsProvider`, dispatch the call, and respond with the
   schema-shaped result.

```
              +-----------+                       +-----------+
              |           |  sessionFs.setProvider  |           |
              |    SDK    | ---------------------> |    CLI    |
              |  (Rust)   |                        |           |
              |           |  sessionFs.readFile     |           |
              |           | <--------------------- |           |
              |           |     (per session)       |           |
              +-----------+                        +-----------+
                    ^                                    |
                    |  routed by sessionId               |
                    |                                    |
              +-----------+                              |
              | session A |  --- handler dispatch  <-----+
              | provider  |
              +-----------+
              | session B |
              | provider  |
              +-----------+
```

### Methodology: verify-before-drafting

Phase 4 has hit the verify-first pattern consistently:

- A.2 — `infinite_sessions` already wired
- A.6 — router already had the registry
- 4.5 — wrong struct identified by cross-checking Node + Go before drafting
- 4.1 — `CommandExecuteData` and `handle_pending_command` already in
  `rust/src/generated/`, no codegen work needed

This ADR is grounded in an explicit cross-SDK audit before any code:

| Source                                | Verified                                        |
| ------------------------------------- | ----------------------------------------------- |
| `nodejs/src/sessionFsProvider.ts`     | Trait shape, error adapter, factory pattern    |
| `nodejs/src/types.ts:1571`            | `SessionFsConfig` fields                        |
| `nodejs/src/client.ts:303-321,430-445`| Validation + handshake flow                     |
| `nodejs/src/client.ts:714-723`        | Per-session registration in `create_session`    |
| `python/copilot/session_fs_provider.py`| Async trait shape, adapter, `SessionFsFileInfo`|
| `python/copilot/client.py:1056`       | `_set_session_fs_provider` flow                 |
| `go/client.go:336-345`                | `SetProvider` outbound RPC                      |
| `go/client.go:677-687,837-847`        | `CreateSessionFsHandler` factory closure        |
| `rust/src/generated/api_types.rs`     | All request/response types present              |
| `rust/src/generated/rpc.rs:43-45`     | `client.session_fs().set_provider(...)` typed   |

All three reference SDKs ship the same surface; the Rust shape can mirror
it without inventing new abstractions.

### Tauri-side audit

Per phase-04 §4.2 of the public release plan, `src-tauri/` does not call
`SessionFs` / `session_fs` / register a virtual filesystem provider today.
Adding `ClientOptions::session_fs` and `SessionConfig::session_fs_provider`
is purely additive for the Tauri consumer — `None` preserves current
behavior.

## Decision

Add a hand-authored `SessionFsProvider` async trait under
`rust/src/session_fs.rs`, mirror the Node/Python surface, and wire it
through `ClientOptions` + `SessionConfig` + `ResumeSessionConfig`. The
SDK takes responsibility for dispatching inbound `sessionFs.*` requests to
the registered provider and translating Rust `Result<T, FsError>` into the
schema's `{ ..., error: Option<SessionFsError> }` payload.

### 1. Trait shape: `async_trait`

```rust
#[async_trait::async_trait]
pub trait SessionFsProvider: Send + Sync {
    async fn read_file(&self, path: &str) -> Result<String, FsError>;
    async fn write_file(&self, path: &str, content: &str, mode: Option<i64>) -> Result<(), FsError>;
    async fn append_file(&self, path: &str, content: &str, mode: Option<i64>) -> Result<(), FsError>;
    async fn exists(&self, path: &str) -> Result<bool, FsError>;
    async fn stat(&self, path: &str) -> Result<FileInfo, FsError>;
    async fn mkdir(&self, path: &str, recursive: bool, mode: Option<i64>) -> Result<(), FsError>;
    async fn readdir(&self, path: &str) -> Result<Vec<String>, FsError>;
    async fn readdir_with_types(&self, path: &str) -> Result<Vec<DirEntry>, FsError>;
    async fn rm(&self, path: &str, recursive: bool, force: bool) -> Result<(), FsError>;
    async fn rename(&self, src: &str, dest: &str) -> Result<(), FsError>;
}
```

Rationale: matches the precedent set by `SessionHandler`, `ToolHandler`,
`CommandHandler`, and `ListModelsHandler` — all consumer extension points
in the Rust SDK are async traits. Trait registration uses
`Arc<dyn SessionFsProvider>` so the same provider can be cloned across
spawned dispatch tasks.

#### Rejected: sync trait

A sync trait (`fn read_file(&self, path: &str) -> Result<String, FsError>`)
would force handler implementations to either block the runtime or
pre-cache, both anti-patterns. Filesystem operations are inherently I/O
shaped — even an in-memory implementation might want to enforce mutex
ordering across tasks. Sync rejected.

#### Rejected: trait-erased boxed-closure type alias

```rust
pub type ReadFileHandler = Box<dyn Fn(String) -> BoxFuture<'static, Result<String, FsError>> + Send + Sync>;
```

Violates the global Copilot rule "avoid lambdas/functors as function
arguments". Anonymous in stack traces, harder to navigate via
go-to-definition, harder to re-use. Trait wins.

### 2. Method signatures: `Result<T, FsError>` not `Result<T, schema::SessionFsError>`

The provider returns Rust-idiomatic `Result<T, FsError>` where `FsError`
is hand-authored:

```rust
#[non_exhaustive]
#[derive(Debug, Clone, thiserror::Error)]
pub enum FsError {
    #[error("not found: {0}")]
    NotFound(String),
    #[error("{0}")]
    Other(String),
}
```

The SDK's internal adapter converts `FsError` into the schema's
`SessionFsError { code, message }` payload:

| Rust variant     | Schema code |
| ---------------- | ----------- |
| `NotFound(_)`    | `ENOENT`    |
| `Other(_)`       | `UNKNOWN`   |

`From<std::io::Error> for FsError` is provided so handlers backed by
`tokio::fs` can use `?`:

```rust
impl From<std::io::Error> for FsError {
    fn from(err: std::io::Error) -> Self {
        match err.kind() {
            std::io::ErrorKind::NotFound => FsError::NotFound(err.to_string()),
            _ => FsError::Other(err.to_string()),
        }
    }
}
```

`#[non_exhaustive]` so the CLI schema can grow new error codes (e.g.
`EACCES`, `EEXIST`) without breaking existing handlers — the SDK always
falls back to `Other`/`UNKNOWN` for unknown variants.

`FileInfo` and `DirEntry` are also hand-authored newtype structs (not
re-exports of `SessionFsStatResult` / `SessionFsReaddirWithTypesEntry`)
to keep handler-facing types decoupled from the wire schema and future-
proof against generated-type churn. Both are `#[non_exhaustive]`.

### 3. Concurrency model: concurrent dispatch, providers must be `Send + Sync`

Each inbound `sessionFs.*` request is dispatched on a fresh
`tokio::spawn` task, matching Node's behavior. Providers must be
`Send + Sync` (already required by `Arc<dyn SessionFsProvider>`) and must
be safe for concurrent invocation across distinct paths. Implementations
that need ordering or mutual exclusion must enforce it internally
(e.g. `tokio::sync::Mutex` keyed by path).

#### Rejected: per-session sequential dispatch

Sequencing every request through one queue per session would give weaker
parallelism than the CLI assumes. The CLI may issue many concurrent reads
during planning and tool calls — serializing them in the SDK would make
the Rust integration measurably slower than the Node/Python paths.

The trade-off is documented in the rustdoc:

```rust
/// Dispatched concurrently. Implementations MUST be safe for concurrent
/// invocation across distinct paths. Use internal synchronization
/// (e.g. `tokio::sync::Mutex`) if your backing store needs ordering.
```

### 4. Plumbing: direct Arc registration, not factory closure

Node, Python, and Go all use a factory closure
(`createSessionFsHandler: (session) => SessionFsProvider`) so the
provider can take a back-reference to the `Session` it will serve. The
Rust SDK takes a different approach — register `Arc<dyn SessionFsProvider>`
directly, no factory:

```rust
let config = SessionConfig::default()
    .with_handler(handler)
    .with_session_fs_provider(my_provider.clone());
```

Rationale:

- The factory pattern's only motivation is back-reference to the session,
  which the caller can carry themselves — they construct one provider
  per session anyway, so closing over a session-id at construction time
  is straightforward.
- Sidesteps the "lambdas as function arguments" rule. A trait-bounded
  factory closure (`Fn(Session) -> Arc<dyn SessionFsProvider>`) would be
  invisible in stack traces.
- The SDK has no `Session` value to pass at the call site that's
  ergonomically reachable from the factory closure: `Session` is
  constructed *during* `create_session`, after the provider must already
  be registered (so the inbound `sessionFs.*` request handler can find
  it). Node sidesteps this with an explicit two-step: build the session
  object then mutate `session.clientSessionApis.sessionFs` before
  sending `session.create`. Rust doesn't expose mutable session state
  that way.
- For consumers that genuinely need the session ID inside the provider,
  the recommended pattern is to construct one provider per
  `create_session` call and capture the intended `session_id` (which the
  caller already chose if they set `SessionConfig::session_id`, or
  generated themselves) at construction.

If a future requirement makes the factory shape necessary, we can add
`with_session_fs_provider_factory(F)` as an additional, narrower API
without breaking the direct-Arc form.

#### Wire-up

```rust
// ClientOptions: client-level handshake config (additive, defaults to None).
pub struct ClientOptions {
    // ... existing fields ...
    pub session_fs: Option<SessionFsConfig>,
}

#[non_exhaustive]
pub struct SessionFsConfig {
    pub initial_cwd: String,
    pub session_state_path: String,
    pub conventions: SessionFsConventions, // Posix | Windows
}

// SessionConfig + ResumeSessionConfig (mirrored).
pub struct SessionConfig {
    // ... existing fields ...
    pub session_fs_provider: Option<Arc<dyn SessionFsProvider>>,
}
```

Validation lives in `Client::start` (matching Node/Go): when
`options.session_fs` is `Some`, all three subfields must be non-empty
and `conventions` must be `Posix | Windows`. Failure returns a typed
error, not a `panic`. After validation, the SDK calls
`client.session_fs().set_provider(...)` immediately after the
`session.create`-handshake-equivalent client-bringup step.

When `options.session_fs.is_some()` and `config.session_fs_provider.is_none()`,
`create_session` / `resume_session` returns
`Err(SessionError::SessionFsProviderRequired)` rather than letting CLI
requests later fail with an opaque "no provider" error.

### 5. Inbound dispatch in the event loop

`sessionFs.*` lands as a JSON-RPC *request* (not notification) routed by
`sessionId` through the existing `SessionRouter::register` channels and
into `handle_request`. A new arm covers all 10 methods:

```rust
match request.method.as_str() {
    "sessionFs.readFile" => session_fs_dispatch::read_file(...).await,
    "sessionFs.writeFile" => session_fs_dispatch::write_file(...).await,
    // ...
    "sessionFs.rename" => session_fs_dispatch::rename(...).await,
    // ... existing arms ...
}
```

`session_fs_dispatch::read_file` (and friends) deserialize the request
params with the generated `SessionFsReadFileRequest` type, look up the
session's provider in an `Arc<HashMap<SessionId, Arc<dyn SessionFsProvider>>>`,
spawn a task that calls the handler method, and respond with the
schema-shaped result. The dispatch helper module lives at
`rust/src/session_fs_dispatch.rs` and is `pub(crate)`.

The fs-provider map is registered at session-create time alongside
`command_handlers` and threaded through `spawn_event_loop` →
`handle_request`. Same shape as the existing `command_handlers` map
introduced in §4.1.

When the CLI sends `sessionFs.*` for an unknown sessionId or a session
without a registered provider, the dispatch arm responds with an RPC
error using the schema's "method not found" / "no handler" semantics —
matching Node's runtime behavior.

### 6. Naming and module organization

| Concept                | Name                                          |
| ---------------------- | --------------------------------------------- |
| Module                 | `rust/src/session_fs.rs` (public re-export)   |
| Dispatch internals     | `rust/src/session_fs_dispatch.rs` (`pub(crate)`)|
| Trait                  | `SessionFsProvider`                           |
| Client config struct   | `SessionFsConfig`                             |
| Conventions enum       | `SessionFsConventions { Posix, Windows }`     |
| Error type             | `FsError`                                     |
| File metadata          | `FileInfo`                                    |
| Directory entry        | `DirEntry`                                    |
| Directory entry kind   | `DirEntryKind { File, Directory, Other }`     |
| Builder on client opts | `ClientOptions::with_session_fs(...)`         |
| Builder on session cfg | `SessionConfig::with_session_fs_provider(...)` |
| Mirror on resume cfg   | `ResumeSessionConfig::with_session_fs_provider(...)` |

`SessionFsProvider` is re-exported from `crate::types` and the crate
root, matching how `SessionHandler`, `ToolHandler`, and `CommandHandler`
are surfaced.

`SessionFsConventions` is hand-authored rather than reusing the generated
`SessionFsSetProviderConventions` because the generated enum has a
catch-all `Unknown` variant for forward-compat that doesn't make sense
on the consumer-facing input side. The conversion is mechanical inside
the handshake helper.

### 7. Forward compatibility

- `SessionFsConfig`, `FsError`, `FileInfo`, `DirEntry`, and
  `DirEntryKind` are all `#[non_exhaustive]`. Forward-compat consistent
  with `MessageOptions`, `CommandDefinition`, and `CommandContext` from
  prior phases.
- The trait itself does NOT use `#[non_exhaustive]` semantics (Rust has
  no equivalent). Adding new methods to the trait is a breaking change.
  If the CLI schema later adds new `sessionFs.*` methods (e.g. `chmod`,
  `symlink`), the SDK provides a default implementation that returns
  `Err(FsError::Other("operation not supported".into()))` so existing
  implementations continue to compile. New methods land with default
  impls; consumers opt in by overriding.
- The `set_provider` payload includes `conventions` which the schema
  declares as a closed enum. If the CLI grows new convention values, the
  generated enum's `Unknown` variant absorbs them, and the SDK rejects
  unknown conventions at validation time with a typed error.

## Consequences

### Positive

- Closes the last 1.0 parity gap that consumers care about for sandbox /
  IDE integrations.
- Aligns Rust's filesystem-virtualization story with Node, Python, Go.
- Public surface is small (~12 hand-authored items), all `#[non_exhaustive]`
  where forward-compat matters.
- Zero codegen changes — pure consumer wiring on top of generated types.
- Direct-Arc registration is more idiomatic than factory closure and
  composable with future builder ergonomics.

### Negative

- Adds a new trait to the public API surface that's hard to remove
  post-1.0. Mitigated by the cross-SDK precedent — Node, Python, and Go
  have shipped this exact shape and we've not seen breaking-change
  pressure.
- The default-impl-per-method strategy for forward compat means the trait
  body grows over time. Acceptable: the trait is still smaller than
  `SessionHandler` after a year.
- Concurrent dispatch shifts the burden of mutual exclusion onto handler
  implementors. Documented in the rustdoc and in the `examples/`
  directory's session-fs example, but a foot-gun that didn't exist
  before.
- The Rust API diverges from Node/Python/Go on the factory-closure point.
  Documented in the public README's "Differences from other SDKs"
  section.

### Neutral

- `tokio::fs`-backed example provider lands in `examples/session_fs/` to
  show the `?` ergonomics of `From<io::Error> for FsError`.
- Test coverage in `rust/tests/session_fs_test.rs`: validate
  `setProvider` outbound RPC, dispatch happy-path for each of the 10
  methods, error-mapping (`NotFound` → `ENOENT`, `Other` → `UNKNOWN`),
  validation rejection (missing fields, wrong conventions string),
  missing-provider diagnostic.

## Implementation order

Once approved:

1. Hand-author `rust/src/session_fs.rs` with trait, `FsError`,
   `SessionFsConfig`, `FileInfo`, `DirEntry`, `DirEntryKind`,
   `SessionFsConventions`, plus `From<io::Error>`.
2. Add `ClientOptions::session_fs` field + `with_session_fs` builder +
   validation in `Client::start`.
3. Add the `setProvider` outbound RPC in `Client::start` after CLI
   bringup, gated on `options.session_fs.is_some()`.
4. Add `SessionConfig::session_fs_provider` and
   `ResumeSessionConfig::session_fs_provider` fields with builders.
5. Add the per-session provider map (`Arc<HashMap<SessionId, Arc<dyn SessionFsProvider>>>`)
   to the dispatch infrastructure, mirroring the §4.1 `command_handlers`
   shape. Thread through `spawn_event_loop` → `handle_request`.
6. Hand-author `rust/src/session_fs_dispatch.rs` with one helper per
   method that deserializes params, calls the provider, and serializes
   the schema response with `FsError` → `SessionFsError` mapping.
7. Wire 10 new arms in `handle_request`'s match.
8. Mock-server tests in `rust/tests/session_fs_test.rs`.
9. Example: `rust/examples/session_fs.rs` showing a `tokio::fs`-backed
   provider.
10. CHANGELOG entry under "Configuration parity".
11. README addition: brief "Differences from other SDKs" call-out for
    the direct-Arc choice.

## Cross-repo impact

Additive only:

- github-app's `ClientOptions { ... }` literal at `cli.rs:337` will need
  `session_fs: None,` added. Same mechanical pattern as Bucket B.2.
- `src-tauri/` does not register a virtual filesystem today — no
  behavior change.
- A sync-time Sync session message will flag the new field for the next
  pull, alongside the §4.1 `commands: None,` already queued.

## References

- `nodejs/src/sessionFsProvider.ts` — Node trait + adapter
- `nodejs/src/types.ts:1571-1587` — `SessionFsConfig`
- `nodejs/src/client.ts:303-321` — validation
- `nodejs/src/client.ts:430-445` — handshake
- `nodejs/src/client.ts:714-723,856-865` — per-session registration
- `python/copilot/session_fs_provider.py` — Python trait + adapter
- `go/client.go:336-345,677-687` — Go SetProvider + factory
- `rust/src/generated/api_types.rs:1132-1318` — generated request/result types
- `rust/src/generated/rpc.rs:43-45,211-300` — `ClientRpcSessionFs`
- `rust/src/generated/rpc.rs:172` — schema-side handler trait pattern
  (reference; not used by Rust SDK)
- Phase 4 plan §4.2: `docs/copilot/2026-04-14-sdk-release/phase-04-parity-for-1.0.md#42--sessionfsprovider-virtual-fs`
