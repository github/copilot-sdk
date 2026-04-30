# Changelog

All notable changes to the `github-copilot-sdk` crate will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

After 0.1.0 ships, [release-plz](https://release-plz.dev/) will prepend new
entries from conventional-commit history. The Unreleased entry below is
hand-curated so that crates.io readers get a usable summary of the public
surface on first publish, not a flat list of merge commits — release-plz
will rename `[Unreleased]` to `[0.1.0] - <date>` and add a fresh empty
`[Unreleased]` above it when it cuts the first release PR.

## [Unreleased]

Initial public release. Programmatic Rust access to the GitHub Copilot CLI
over JSON-RPC 2.0 (stdio or TCP), with handler-based event dispatch, typed
tool/permission/elicitation helpers, and runtime session management.

This is a **technical preview**. The crate is pre-1.0 and the public API may
change in breaking ways before 1.0. The rendered docs on
[docs.rs](https://docs.rs/github-copilot-sdk) are the canonical reference for the
public surface.

### Added

#### Client lifecycle
- `Client::start` — spawn and manage a GitHub Copilot CLI child process.
- `Client::from_streams` — connect to a CLI server over caller-supplied
  `AsyncRead`/`AsyncWrite` (testing, custom transports).
- `Client::stop` / `Client::force_stop` — graceful and immediate shutdown.
- `Client::state` returning `ConnectionState` (`Connecting`, `Connected`,
  `Disconnecting`, `Disconnected`).
- `Client::subscribe_lifecycle` returning a `LifecycleSubscription` for
  runtime observation of created / destroyed / foreground / background
  events. Implements `tokio_stream::Stream` and offers an inherent
  `recv()`; drop the value to unsubscribe.
- `Client::ping(message)` returning typed `PingResponse` and
  `Client::verify_protocol_version` for handshake validation.
- `Client::list_sessions`, `get_session_metadata`, `delete_session`,
  `get_last_session_id`, `get_foreground_session_id`,
  `set_foreground_session_id`.
- `Client::list_models`, `get_status` (typed `GetStatusResponse`),
  `get_auth_status` (typed `GetAuthStatusResponse`), `get_quota`,
  `send_telemetry`.

#### Sessions
- `Client::create_session` and `Client::resume_session` accepting
  `SessionConfig` with handler, capabilities, system message, mode, model,
  permission policy, working directory, and resume parameters.
- `Session::send` returning the assigned message ID for
  correlation with later events.
- `Session::send_and_wait` for synchronous prompt → final-event flows.
- `Session::subscribe` returning an `EventSubscription` for observe-only
  access to the session's event stream. Implements `tokio_stream::Stream`
  and offers an inherent `recv()`; drop the value to unsubscribe.
- Mode + model controls: `get_mode` / `set_mode`, `get_model` /
  `set_model(model, SetModelOptions)` with `reasoning_effort` and
  `model_capabilities` overrides.
- Plan helpers: `read_plan`, `delete_plan`.
- Workspace helpers: `list_workspace_files`, `read_workspace_file`,
  `create_workspace_file`, `cwd`, `remote_url`.
- UI primitives: `session.ui().elicitation()`, `confirm()`, `select()`,
  `input()` — grouped under a `SessionUi` sub-API to mirror .NET / Python /
  Go.
- `Session::log(message, LogOptions)` with optional severity and
  ephemeral flag.
- `Session::send_telemetry`, `start_fleet`, `abort`,
  `set_approve_all_permissions`, `set_name`.
- `Session::disconnect` (canonical) and `Session::destroy` (alias)
  preserve on-disk session state for later resume.
- `Session::stop_event_loop` for shutting down the per-session loop.

#### Handlers + helpers
- `SessionHandler` trait with default fallback impls for each event
  (permissions, external tools, elicitation, plan-mode prompts).
- `ApproveAllHandler` / `DenyAllHandler` reference handlers.
- Permission policy helpers: `permission::approve_all`,
  `permission::deny_all`, `permission::approve_if`, plus chainable
  builders on `SessionConfig` (`approve_all_permissions`,
  `deny_all_permissions`, `approve_if`).
- `PermissionResult` is `#[non_exhaustive]` and supports `Approved`,
  `Denied`, `Deferred` (handler will resolve via
  `handlePendingPermissionRequest` itself — notification path only;
  direct RPC falls back to `Approved`), and
  `Custom(serde_json::Value)` for response shapes beyond
  `{ "kind": "approve-once" | "reject" }` (e.g. allowlist payloads).
- All extension-point and protocol-evolving public enums are
  `#[non_exhaustive]` so future variants are additive (non-breaking):
  `Error`, `ProtocolError`, `SessionError`, `Transport`, `Attachment`,
  `ToolResult`, `ElicitationMode`, `InputFormat`, `GitHubReferenceType`,
  `SessionLifecycleEventType`, plus the handler/hook event/response enums.
  Closed taxonomies (`LogLevel`, `ConnectionState`, `CliProgram`) remain
  exhaustive so callers benefit from compile-time exhaustiveness checks.
- Tool helpers: `tool::DefineTool`, `tool::tool_schema_for<T>`,
  `tool::ToolHandlerRouter`, derive support via `derive` feature.
  `ToolHandlerRouter` overrides each `SessionHandler` per-event method
  directly, so callers can use the narrow-typed entry points (e.g.
  `router.on_external_tool(invocation).await -> ToolResult`) instead of
  unwrapping a `HandlerResponse` from `on_event`. The default `on_event`
  still routes correctly through the per-event methods, so legacy
  callers are unaffected.
- Hooks API for instrumenting send/receive flows (`github_copilot_sdk::hooks`).
- `SessionHandler::on_auto_mode_switch` — typed handler for the CLI's
  rate-limit-recovery prompt (`autoModeSwitch.request` JSON-RPC
  callback, added in copilot-agent-runtime PR #7024). Returns a typed
  [`AutoModeSwitchResponse`] enum with `Yes`, `YesAlways`, `No`
  variants (`#[serde(rename_all = "snake_case")]`, wire values byte-
  identical to the runtime's `"yes" | "yes_always" | "no"` schema).
  Default impl declines (`No`); override only if your application
  surfaces a UX for the prompt. `SessionConfig::request_auto_mode_switch`
  and `ResumeSessionConfig::request_auto_mode_switch` default to
  `Some(true)` so the CLI advertises the callback to the SDK out of the
  box. **Cross-SDK divergence:** typed handler is Rust-only as of 0.1.0.
  Node, Python, Go, and .NET observe the request as a raw JSON-RPC
  callback today; parity ports for those SDKs are post-release follow-up
  work.
- New session-event fields surfaced by the `@github/copilot ^1.0.39`
  schema bump:
  - `SessionErrorData.eligible_for_auto_switch: Option<bool>` — set on
    `errorType: "rate_limit"` to signal the runtime will follow with an
    `auto_mode_switch.requested` event. UI clients can suppress
    duplicate rendering of the rate-limit error when they show their
    own auto-mode-switch prompt.
  - `SessionErrorData.error_code: Option<String>` — fine-grained
    upstream provider error code (e.g.
    `"user_weekly_rate_limited"`, `"integration_rate_limited"`).
  - `SessionModelChangeData.cause: Option<String>` —
    `"rate_limit_auto_switch"` for changes triggered by the
    auto-mode-switch recovery path. Lets UI render contextual copy.
  - `AutoModeSwitchRequestedData.retry_after_seconds: Option<f64>` —
    seconds until the rate limit resets, when known. Clients can
    render a humanized reset time alongside the prompt. (The request-
    callback path's `retry_after_seconds` parameter on
    [`SessionHandler::on_auto_mode_switch`](crate::handler::SessionHandler::on_auto_mode_switch)
    uses `Option<u64>` for HTTP `Retry-After` `delta-seconds`
    semantics.)

#### Types
- Newtype `SessionId`, plus generated RPC types under `github_copilot_sdk::generated`.
- `LogLevel`, `LogOptions`, `SetModelOptions`, `PingResponse`,
  `SessionLifecycleEvent`, `SessionLifecycleEventType`, `ConnectionState`,
  `SessionTelemetryEvent`, `ServerTelemetryEvent`, `SystemMessageConfig`,
  `MessageOptions`, `SectionOverride`, `Attachment`,
  `InputFormat`, `InputOptions`.
- Strongly-typed `Error` and `ProtocolError` with `is_transport_failure`
  classifier and `error_codes` constants.

#### Typed RPC namespace
- `Client::rpc()` and `Session::rpc()` accessors exposing a generated, typed
  view over the full GitHub Copilot CLI JSON-RPC API. Sub-namespaces mirror the
  schema (e.g. `client.rpc().models().list()`, `session.rpc().workspaces()
  .list_files()`, `session.rpc().agent().list()`,
  `session.rpc().tasks().list()`).
- All hand-authored helpers (`list_workspace_files`, `read_plan`, `set_mode`,
  `list_models`, `get_quota`, etc.) are now thin one-line delegations over
  this namespace. Wire-method strings exist in exactly one place
  (`generated/rpc.rs`), making typo bugs like the `session.workspace.*`
  → `session.workspaces.*` regression structurally impossible. Public
  helper signatures are unchanged.

#### Configuration parity
- All remaining public configuration types are now `#[non_exhaustive]`
  for forward-compatibility — adding fields post-1.0 is non-breaking on
  consumers that construct via `Default::default()` plus field
  assignment or the `with_*` builders. Affected: `SessionConfig`,
  `ResumeSessionConfig`, `ClientOptions`, `ProviderConfig`,
  `McpServerConfig`, `Tool`, `CustomAgentConfig`,
  `InfiniteSessionConfig`, `SystemMessageConfig`, `ConnectionState`.
  (`HookEvent`, `HookOutput`, `MessageOptions`, `TelemetryConfig`,
  `SessionFsConfig`, `FsError`, `FileInfo`, `DirEntry`, `ToolInvocation`,
  `Error`, `Transport`, `DeliveryMode` were already marked.) Callers
  using exhaustive struct literals must switch to
  `let mut x = Type::default(); x.field = ...;` or the available `with_*`
  builders; `..Default::default()` no longer compiles for these types
  outside the defining crate.
- `MessageOptions::mode` is now typed `Option<DeliveryMode>` (was
  `Option<String>`). `DeliveryMode` is `#[non_exhaustive]` and serializes
  to the wire strings `"enqueue"` (default) and `"immediate"`. The prior
  rustdoc incorrectly described this field as a permission mode; the
  field controls how the prompt is delivered relative to in-flight work.
  `MessageOptions::with_mode` now takes `DeliveryMode` directly. Callers
  that previously passed `"agent"` or `"autopilot"` were already silently
  no-ops at the CLI level — switch to a `DeliveryMode` variant or omit
  the field entirely.
- `SessionConfig::default()` and `ResumeSessionConfig::new()` now set the
  four permission-flow flags (`request_user_input`, `request_permission`,
  `request_exit_plan_mode`, `request_elicitation`) to `Some(true)` instead
  of `None`. Mirrors Node's `client.ts` behavior of always advertising the
  permission surface and deriving handler presence from the
  `SessionHandler` impl. The default `DenyAllHandler` refuses all
  permission requests so the wire surface is safe out-of-the-box; callers
  that want the wire surface fully disabled set the flags explicitly to
  `Some(false)`.
- `SessionListFilter` — typed filter for `Client::list_sessions` covering
  `cwd`, `git_root`, `repository`, and `branch`. Replaces the prior
  `Option<serde_json::Value>` parameter.
- `McpServerConfig` tagged enum (`Stdio` / `Http` / `Sse`) with
  `McpStdioServerConfig` and `McpHttpServerConfig` payload structs.
  `SessionConfig::mcp_servers`, `ResumeSessionConfig::mcp_servers`, and
  `CustomAgentConfig::mcp_servers` are now `Option<HashMap<String,
  McpServerConfig>>` instead of typeless `Value` maps. Stdio configurations
  serialized by older callers (no explicit `type`, or `type: "local"`) are
  accepted on the deserialize path.
- `PermissionRequestData` gains typed `kind: Option<PermissionRequestKind>`
  and `tool_call_id: Option<String>` fields covering the eight CLI
  permission categories (`shell`, `write`, `read`, `url`, `mcp`,
  `custom-tool`, `memory`, `hook`); unknown values fall through to
  `PermissionRequestKind::Unknown` for forward compatibility. The original
  params object is still available via the existing `extra: Value` flatten.
- `PermissionResult` gains `UserNotAvailable` (sent as
  `{ "kind": "user-not-available" }`) and `NoResult` (sent as
  `{ "kind": "no-result" }`) variants for headless agents and explicit
  fall-through-to-CLI-default responses.
- `Client::stop` cooperatively shuts down active sessions before killing
  the CLI child: walks every session still registered with the client,
  sends `session.destroy` for each, then kills the child. Errors from
  per-session destroys and the terminal child-kill are collected into a
  new `StopErrors` aggregate (`Result<(), StopErrors>`) instead of
  short-circuiting on the first failure, mirroring the Node SDK's
  `Error[]` return shape. `StopErrors` implements `std::error::Error`
  and exposes `errors()` / `into_errors()` for inspection. Callers that
  previously used `client.stop().await?` should switch to
  `client.stop().await.ok();` (best-effort) or match on the aggregate.
- `ResumeSessionConfig::disable_resume: Option<bool>` — force-fail resume
  if the session does not exist on disk, instead of silently starting a
  new session.
- `SessionConfig` and `ResumeSessionConfig` gain six configuration knobs
  matching the Node SDK shape (Bucket B.1):
  - `session_id: Option<SessionId>` (SessionConfig only — required on
    resume, where it remains `SessionId`) — supply a custom session ID
    instead of letting the CLI generate one.
  - `working_directory: Option<PathBuf>` — per-session cwd override,
    independent of [`ClientOptions::cwd`](crate::ClientOptions::cwd).
  - `config_dir: Option<PathBuf>` — override the default configuration
    directory location for this session.
  - `model_capabilities: Option<ModelCapabilitiesOverride>` — per-property
    overrides for model capabilities, deep-merged over runtime defaults.
    The same type was previously available only on
    `SetModelOptions::model_capabilities`.
  - `github_token: Option<String>` — per-session GitHub token. Distinct
    from [`ClientOptions::github_token`], which authenticates the CLI
    process; this token determines the GitHub identity used for content
    exclusion, model routing, and quota checks for this session. The
    field is redacted from the `Debug` output.
  - `include_sub_agent_streaming_events: Option<bool>` — forward streaming
    delta events from sub-agents to this connection (Node default: true).
- `ClientOptions` gains the simple subset of Node's
  `CopilotClientOptions` knobs (Bucket B.2):
  - `log_level: Option<LogLevel>` — typed enum (`None`, `Error`, `Warning`,
    `Info`, `Debug`, `All`) replacing the previously hard-coded
    `--log-level info` argument. When unset, the SDK still passes
    `--log-level info` for parity with prior behavior.
  - `session_idle_timeout_seconds: Option<u64>` — server-wide idle
    timeout for sessions in seconds. When `Some(n)` with `n > 0`, the
    SDK passes `--session-idle-timeout <n>`. `None` or `Some(0)` leaves
    sessions running indefinitely (the CLI default).
  - The Node knob `isChildProcess` (sub-CLI parent-stdio mode) and
    `autoStart` (lazy-init pattern) are intentionally **not** ported —
    `isChildProcess` requires a transport variant the Rust SDK does not
    yet support; `autoStart` does not apply because [`Client::start`] is
    a single explicit constructor rather than a deferred-init pattern.
  - `on_list_models: Option<Arc<dyn ListModelsHandler>>` — BYOK escape
    hatch matching Node's `onListModels`. When set, [`Client::list_models`]
    returns the handler's result without making a `models.list` RPC.
    `ListModelsHandler` is a new public `async_trait` (mirrors the shape
    of `SessionHandler` / `SessionHooks`) with a single
    `async fn list_models(&self) -> Result<Vec<Model>, Error>` method.
    `ClientOptions` switched from `#[derive(Debug)]` to a manual `Debug`
    impl that prints the handler as `<set>` / `None` (same precedent as
    `SessionConfig::handler` and `github_token`).
- `MessageOptions` gains `request_headers: Option<HashMap<String, String>>`
  with a corresponding [`MessageOptions::with_request_headers`] builder
  method, matching Node's `MessageOptions.requestHeaders` and Go's
  `MessageOptions.RequestHeaders`. Custom HTTP headers are forwarded to
  the CLI via the `requestHeaders` field on `session.send`. The field is
  omitted from the wire when `None` or empty (matches Node's
  `omitempty` semantics).
- Slash command registration: new [`CommandHandler`] async trait,
  [`CommandDefinition`] (with `new`/`with_description` builders), and
  [`CommandContext`] (`session_id`, `command`, `command_name`, `args`)
  hand-authored in `crate::types`. `SessionConfig::commands` and
  `ResumeSessionConfig::commands` accept a `Vec<CommandDefinition>` via
  the new `with_commands` builder, matching Node's
  `SessionConfig.commands`, Python's `SessionConfig.commands`, and Go's
  `SessionConfig.Commands`. The SDK serializes only `{name, description?}`
  on the wire (handlers stay client-side), and dispatches incoming
  `command.execute` events to the registered handler — acking with no
  error on success, `error: <message>` on `Err`, and
  `error: "Unknown command: <name>"` when the name is unregistered.
  `CommandContext` and `CommandDefinition` are `#[non_exhaustive]` so
  forward-compatible fields (e.g. aliases, completion providers) can land
  without breaking callers.
- Custom session filesystem: new [`SessionFsProvider`] async trait,
  [`SessionFsConfig`], [`FsError`], [`FileInfo`], [`DirEntry`],
  [`DirEntryKind`], and [`SessionFsConventions`] in `crate::session_fs`
  (also re-exported from `crate::types`). When [`ClientOptions::session_fs`]
  is set, [`Client::start`] calls `sessionFs.setProvider` on the CLI to
  delegate per-session filesystem operations to a provider supplied via
  [`SessionConfig::with_session_fs_provider`] /
  [`ResumeSessionConfig::with_session_fs_provider`]. Inbound `sessionFs.*`
  requests dispatch to the provider; `FsError::NotFound` maps to the wire
  `ENOENT` code and other `FsError` values map to `UNKNOWN`.
  `From<std::io::Error>` is provided so handlers backed by `std::fs` /
  `tokio::fs` can propagate errors with `?`. All trait methods have
  default implementations returning `Err(FsError::Other("not supported"))`,
  so providers only override the methods they need and forward-compatible
  schema additions land without breaking existing implementations.
  Diverges from Node/Python/Go's factory-closure pattern in favor of
  direct `Arc<dyn SessionFsProvider>` registration.
- W3C Trace Context propagation: new [`TraceContext`] struct and
  [`TraceContextProvider`] async trait in `crate::trace_context` (also
  re-exported from `crate::types`). Hybrid shape combines Node's
  callback-based `onGetTraceContext` and Go's per-turn
  `MessageOptions.Traceparent` / `Tracestate`:
  [`ClientOptions::on_get_trace_context`] supplies an ambient provider that
  injects `traceparent` / `tracestate` on `session.create`,
  `session.resume`, and `session.send`, while
  [`MessageOptions::with_traceparent`], [`MessageOptions::with_tracestate`],
  and [`MessageOptions::with_trace_context`] override per-turn (override
  wins; provider is not invoked when MessageOptions carries trace headers).
  [`ToolInvocation`] is now `#[non_exhaustive]` and exposes inbound
  `traceparent` / `tracestate` populated from `external_tool.requested`
  events, plus a [`ToolInvocation::trace_context`] helper. Wire fields are
  omitted when unset (matches Node/Go `omitempty` semantics).
- `ToolInvocation` and `SessionId` now derive `Default`. Production code
  never constructs `ToolInvocation` literals (it's a CLI-emitted read-only
  type), but downstream test scaffolding can now use
  `ToolInvocation { tool_name: "...".into(), ..Default::default() }` and
  absorb future `#[non_exhaustive]` field additions automatically.
- OpenTelemetry env-var passthrough: new [`TelemetryConfig`] struct and
  [`OtelExporterType`] enum (both `#[non_exhaustive]`), wired on
  [`ClientOptions::telemetry`]. When `Some(...)`, the SDK injects
  `COPILOT_OTEL_ENABLED=true` plus `OTEL_EXPORTER_OTLP_ENDPOINT`,
  `COPILOT_OTEL_FILE_EXPORTER_PATH`, `COPILOT_OTEL_EXPORTER_TYPE`,
  `COPILOT_OTEL_SOURCE_NAME`, and
  `OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT` into the spawned CLI
  process — verbatim env-var names matching Node/Python/Go. Pure
  passthrough: no `opentelemetry-rust` dependency; the CLI itself owns the
  exporter. `exporter_type` is a typed enum (`OtlpHttp` / `File`) following
  the [`LogLevel`](LogLevel) precedent for finite, enumerated CLI knobs;
  serialized verbatim as `"otlp-http"` / `"file"`. User-supplied
  `ClientOptions::env` continues to win over telemetry-injected values.

### Documentation
- `README.md` with quickstart, architecture diagram, and feature matrix.
- Examples under `examples/`: `chat`, `hooks`, `tool_server`,
  `lifecycle_observer`.
- `RELEASING.md` operational runbook for maintainers.

#### Builder ergonomics
- `ClientOptions::new()` plus a chainable `with_*` builder per public
  field (`with_program`, `with_prefix_args`, `with_cwd`, `with_env`,
  `with_env_remove`, `with_extra_args`, `with_transport`,
  `with_github_token`, `with_use_logged_in_user`, `with_log_level`,
  `with_session_idle_timeout_seconds`, `with_list_models_handler`,
  `with_session_fs`, `with_trace_context_provider`, `with_telemetry`).
  Mirrors the existing [`MessageOptions::new`] / `with_*` shape and
  closes the cross-crate ergonomics gap on `#[non_exhaustive]` —
  external callers no longer need to write
  `let mut opts = ClientOptions::default(); opts.field = ...;` for
  every field they touch. Existing `ClientOptions::default()` and
  mut-let-and-assign continue to work unchanged.
- `Tool::new(name)` plus `with_namespaced_name`, `with_description`,
  `with_instructions`, `with_parameters`, `with_overrides_built_in_tool`,
  `with_skip_permission` for tool definitions. Same rationale —
  `Tool` is the most-instantiated `#[non_exhaustive]` type at consumer
  call sites in real-world consumer code, where the
  builder shape replaces the per-consumer `make_tool(name, desc,
  params)` helper that consumers were writing to smooth over the
  mut-let pattern.
- Per-field `with_*` builder methods on `SessionConfig` and
  `ResumeSessionConfig` covering every public scalar, vector, and
  optional-struct field (~30 new methods on each). Mirrors the
  `ClientOptions` / `Tool` shape; existing closure-installing
  chains (`with_handler`, `with_hooks`, `with_transform`,
  `with_commands`, `with_session_fs_provider`,
  `approve_all_permissions`, etc.) continue to work unchanged. The
  primary win: external session-construction sites collapse from
  `let mut cfg = ResumeSessionConfig::new(id); cfg.client_name =
  Some("...".into()); cfg.streaming = Some(true); ...` (10-15
  lines per site) to a single fluent chain.
- Round out builder coverage on the remaining consumer-facing
  config structs: `CustomAgentConfig::new(name, prompt)` plus
  `with_display_name`, `with_description`, `with_tools`,
  `with_mcp_servers`, `with_infer`, `with_skills`;
  `InfiniteSessionConfig::new()` plus `with_enabled`,
  `with_background_compaction_threshold`,
  `with_buffer_exhaustion_threshold`;
  `ProviderConfig::new(base_url)` plus `with_provider_type`,
  `with_wire_api`, `with_api_key`, `with_bearer_token`,
  `with_azure`, `with_headers`; `SystemMessageConfig::new()` plus
  `with_mode`, `with_content`, `with_sections`;
  `TelemetryConfig::new()` plus `with_otlp_endpoint`,
  `with_file_path`, `with_exporter_type`, `with_source_name`,
  `with_capture_content`. `TraceContext` also gains a symmetric
  `new()` + `with_traceparent` pair alongside the existing
  `from_traceparent` shorthand.
- Documented the direct-field-assignment escape hatch on
  `SessionConfig` and `ResumeSessionConfig` for callers forwarding
  `Option<T>` values from upstream code (matches the
  `http::request::Parts` / `hyper::Body::Builder` convention; per-
  field `with_*_opt` setters intentionally omitted to keep the
  primary API surface small).

### Fixed
- `SessionUi::elicitation` (and the `confirm` / `select` / `input`
  convenience helpers that delegate through it) now sends the user-supplied
  JSON Schema as `requestedSchema` on the wire, matching the
  `session.ui.elicitation` request shape that all other SDKs ship and that
  this crate's own generated `UIElicitationRequest` type expects. The
  hand-authored convenience layer was sending it as `schema`, so every UI
  helper call was effectively dead — the CLI saw a missing required
  `requestedSchema` field. The mock-server test for elicitation
  round-tripped through the same misnamed field, so the bug slipped past
  unit tests; the test now asserts on `requestedSchema` and explicitly
  rejects a stray `schema` key.
- `Client::list_sessions` now wraps the optional filter under `params.filter`
  on the wire, matching the `session.list` request shape that Node, Python,
  Go, and .NET ship. The hand-authored implementation was flattening the
  filter fields directly onto `params`, which the runtime silently ignored
  — so `list_sessions(Some(filter))` was functionally equivalent to
  `list_sessions(None)` in 0.0.x. Same class of bug as the elicitation
  wire fix above: the existing mock-server test asserted on the flat shape
  it observed rather than the schema's wrapped shape, so the bug
  round-tripped through both ends. The test now asserts the wrapped path
  (`params.filter.repository`) and explicitly rejects the flattened
  fallback (`params.repository`).
- `Client::get_status` and `Client::get_auth_status` now use the
  correct wire method names (`status.get` and `auth.getStatus`)
  matching Node, Go, Python, and .NET. The hand-authored
  implementation was sending `getStatus` and `getAuthStatus` — names
  that aren't registered on the CLI runtime — so both calls would
  have returned a "method not found" error (or a misleading no-such-
  method log) instead of the expected status payload. Same class of
  bug as the elicitation `requestedSchema` and `list_sessions`
  filter-wrapping fixes above: the mock-server test for these
  methods asserted on the wrong-name strings the implementation
  used, so the bugs round-tripped through both ends. The test now
  asserts on the canonical wire names AND explicitly rejects the
  hand-authored aliases (`assert_ne!(request["method"], "getStatus")`
  / `"getAuthStatus"`).

### Notes
- Minimum supported Rust version (MSRV): 1.94.0 (pinned via
  `rust-toolchain.toml`).
- No `Client::actual_port` accessor — this SDK is strictly stream-based,
  so the concept doesn't apply. See `Client::from_streams` rustdoc.
- `cargo semver-checks` runs in `continue-on-error` mode for 0.1.0; will
  flip to blocking once 0.1.0 is published and serves as the baseline.
- `infinite_sessions: Option<InfiniteSessionConfig>` is wired on both
  `SessionConfig` and `ResumeSessionConfig` and follows the same
  default-omit-on-the-wire semantics as Node/Go: when `None`, the field
  is skipped and the CLI applies its own default. No behavioral
  divergence from the other SDKs.
- `Client::stop` returns `Result<(), StopErrors>` and now cooperatively
  shuts down each active session via `session.destroy` before killing
  the CLI child, aggregating all per-session and child-kill errors into
  the returned `StopErrors`. See the entry under "Configuration parity"
  above for the migration note.
