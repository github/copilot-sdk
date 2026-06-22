#![doc = include_str!("../README.md")]
#![warn(missing_docs)]
#![deny(rustdoc::broken_intra_doc_links)]
#![cfg_attr(test, allow(clippy::unwrap_used))]

/// Canvas declarations, provider callbacks, and host-side canvas RPC types.
pub mod canvas;
mod canvas_dispatch;
/// Bundled CLI binary extraction and caching.
#[cfg(feature = "bundled-cli")]
pub(crate) mod embeddedcli;
mod errors;
pub use errors::*;
/// Connection-level Copilot request handler — intercept and replace the
/// model-layer HTTP and WebSocket traffic the runtime issues for both CAPI and
/// BYOK sessions.
pub mod copilot_request_handler;
/// Event handler traits for session lifecycle.
pub mod handler;
/// Lifecycle hook callbacks (pre/post tool use, prompt submission, session start/end).
pub mod hooks;
mod jsonrpc;
/// Permission-policy helpers that produce a [`handler::PermissionHandler`].
pub mod permission;
/// GitHub Copilot CLI binary resolution (env var, embedded, dev cache).
pub(crate) mod resolve;
mod router;
/// Session management — create, resume, send messages, and interact with the agent.
pub mod session;
/// Custom session filesystem provider (virtualizable filesystem layer).
pub mod session_fs;
mod session_fs_dispatch;
/// Event subscription handles returned by `subscribe()` methods.
pub mod subscription;
/// Typed tool definition framework and dispatch router.
pub mod tool;
/// W3C Trace Context propagation for distributed tracing.
pub mod trace_context;
/// System message transform callbacks for customizing agent prompts.
pub mod transforms;
/// Protocol types shared between the SDK and the GitHub Copilot CLI.
pub mod types;
mod wire;

/// Session event payload types — auto-generated from the protocol schema.
pub mod session_events;

/// JSON-RPC request/response types and typed namespace builders for
/// [`Client::rpc`] and [`session::Session::rpc`](crate::session::Session::rpc).
pub mod rpc;

// Auto-generated protocol-type modules. Crate-private so the only public
// access path is via the `session_events` and `rpc` facade modules above —
// callers can never depend on the implementation-detail layout under
// `generated::*`.
pub(crate) mod generated;

/// Client-level mode ([`ClientMode`]) and the [`ToolSet`] builder for
/// source-qualified tool filter patterns.
pub mod mode;

use std::ffi::OsString;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::{Arc, OnceLock};
use std::time::{Duration, Instant};

use async_trait::async_trait;
// JSON-RPC wire types are internal transport details (like Go SDK's internal/jsonrpc2/).
// External callers interact via Client/Session methods, not raw RPC.
pub(crate) use jsonrpc::{
    JsonRpcClient, JsonRpcError, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse, error_codes,
};
pub use mode::{BUILTIN_TOOLS_ISOLATED, ClientMode, ToolSet};

/// Re-exported JSON-RPC internals for integration tests (requires `test-support` feature).
#[cfg(feature = "test-support")]
pub mod test_support {
    pub use crate::jsonrpc::{
        JsonRpcClient, JsonRpcMessage, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse,
        error_codes,
    };
}
use serde::{Deserialize, Serialize};
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncWrite, BufReader};
use tokio::net::TcpStream;
use tokio::process::{Child, Command};
use tokio::sync::{broadcast, mpsc, oneshot};
use tracing::{Instrument, debug, error, info, warn};
pub use types::*;

mod sdk_protocol_version;
pub use sdk_protocol_version::{SDK_PROTOCOL_VERSION, get_sdk_protocol_version};
pub use subscription::{EventSubscription, LifecycleSubscription};

/// Minimum protocol version this SDK can communicate with.
const MIN_PROTOCOL_VERSION: u32 = 3;
const RUNTIME_SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(10);

/// How the SDK communicates with the CLI server.
#[derive(Debug, Default)]
#[non_exhaustive]
pub enum Transport {
    /// Communicate over stdin/stdout pipes (default).
    #[default]
    Stdio,
    /// Spawn the CLI with `--port` and connect via TCP.
    Tcp {
        /// Port to listen on (0 for OS-assigned).
        port: u16,
        /// Optional connection token. When `None` and the SDK is spawning
        /// the CLI, the SDK auto-generates a 128-bit hex token so the
        /// loopback listener is safe by default.
        connection_token: Option<String>,
    },
    /// Connect to an already-running CLI server (no process spawning).
    External {
        /// Hostname or IP of the running server.
        host: String,
        /// Port of the running server.
        port: u16,
        /// Optional connection token. Required when the external server
        /// was started with a token, ignored otherwise.
        connection_token: Option<String>,
    },
}

/// How the SDK locates the GitHub Copilot CLI binary.
#[derive(Debug, Clone, Default)]
pub enum CliProgram {
    /// Auto-resolve: `COPILOT_CLI_PATH` → embedded CLI → dev cache.
    /// This is the default.
    #[default]
    Resolve,
    /// Use an explicit binary path (skips resolution).
    Path(PathBuf),
}

impl From<PathBuf> for CliProgram {
    fn from(path: PathBuf) -> Self {
        Self::Path(path)
    }
}

/// `true` when this build of the SDK has the Copilot CLI embedded in
/// its binary — i.e. the `bundled-cli` cargo feature is on **and** the
/// target platform is one for which `build.rs` shipped an archive.
///
/// Useful for branching on bundling presence without forcing the lazy
/// extraction triggered by [`install_bundled_cli`].
pub const HAS_BUNDLED_CLI: bool = cfg!(has_bundled_cli);

/// Returns the path to the bundled Copilot CLI, extracting it from the
/// embedded archive on first call.
///
/// This is the same path [`Client::start`] resolves to when
/// [`ClientOptions::program`] is [`CliProgram::Resolve`], no
/// `COPILOT_CLI_PATH` override is set, and no
/// [`ClientOptions::bundled_cli_extract_dir`] is configured — exposing
/// it directly so callers (health checks, diagnostics, version probes)
/// can reach the bundled binary without spinning up a full [`Client`].
///
/// Subsequent calls return the cached result. Extraction is skipped
/// when the target file already exists.
///
/// Returns `None` when the `bundled-cli` feature is off, the target
/// platform isn't supported by `build.rs`, or extraction failed (the
/// failure is logged via `tracing::warn!`). When `None` is returned for
/// the "feature off" reason, [`HAS_BUNDLED_CLI`] is also `false`.
///
/// This deliberately does not fall back to the build-time-extracted
/// dev-cache path used when `bundled-cli` is off — callers that want
/// that resolution should continue to use [`CliProgram::Resolve`].
pub fn install_bundled_cli() -> Option<PathBuf> {
    #[cfg(feature = "bundled-cli")]
    {
        embeddedcli::path()
    }
    #[cfg(not(feature = "bundled-cli"))]
    {
        None
    }
}

/// Options for starting a [`Client`].
///
/// When `program` is [`CliProgram::Resolve`] (the default), [`Client::start`]
/// uses `COPILOT_CLI_PATH` when set to a real file. Otherwise it uses the
/// bundled Copilot CLI when the default `bundled-cli` cargo feature is enabled,
/// or the build-time extracted dev-cache CLI when that feature is disabled.
///
/// Set `program` to [`CliProgram::Path`] to use an explicit binary instead.
/// This skips auto-resolution entirely.
#[non_exhaustive]
pub struct ClientOptions {
    /// How to locate the CLI binary.
    pub program: CliProgram,
    /// Arguments prepended before `--server` (e.g. the script path for node).
    pub prefix_args: Vec<OsString>,
    /// Working directory for the CLI process.
    pub working_directory: PathBuf,
    /// Environment variables set on the child process.
    pub env: Vec<(OsString, OsString)>,
    /// Environment variable names to remove from the child process.
    pub env_remove: Vec<OsString>,
    /// Extra CLI flags appended after the transport-specific arguments.
    pub extra_args: Vec<String>,
    /// Transport mode used to communicate with the CLI server.
    pub transport: Transport,
    /// GitHub token for authentication. When set, the SDK passes the token
    /// to the CLI via `--auth-token-env COPILOT_SDK_AUTH_TOKEN` and exports
    /// the token in that env var. When set, the CLI defaults to *not*
    /// using the logged-in user (override with [`Self::use_logged_in_user`]).
    pub github_token: Option<String>,
    /// Whether the CLI should fall back to the logged-in `gh` user when no
    /// token is provided. `None` means use the runtime default (true unless
    /// [`Self::github_token`] is set, in which case false).
    pub use_logged_in_user: Option<bool>,
    /// Log level passed to the CLI server via `--log-level`. When `None`,
    /// the SDK does not pass `--log-level` to the runtime at all and the
    /// CLI uses its built-in default.
    pub log_level: Option<LogLevel>,
    /// Server-wide idle timeout for sessions, in seconds. When set to a
    /// positive value, the SDK passes `--session-idle-timeout <secs>` to
    /// the CLI; sessions without activity for this duration are
    /// automatically cleaned up. `None` or `Some(0)` leaves sessions
    /// running indefinitely (the CLI default).
    pub session_idle_timeout_seconds: Option<u64>,
    /// Optional override for [`Client::list_models`].
    ///
    /// When set, [`Client::list_models`] returns the handler's result
    /// without making a `models.list` RPC. This is the BYOK escape hatch
    /// for environments where the model catalog is provisioned separately
    /// from the GitHub Copilot CLI (e.g. external inference servers selected via
    /// [`Transport::External`]).
    pub on_list_models: Option<Arc<dyn ListModelsHandler>>,
    /// Custom session filesystem provider configuration.
    ///
    /// When set, the SDK calls `sessionFs.setProvider` during
    /// [`Client::start`] to register a virtualizable filesystem layer with
    /// the CLI. Each session created on this client must supply its own
    /// [`SessionFsProvider`] via
    /// [`SessionConfig::with_session_fs_provider`](crate::SessionConfig::with_session_fs_provider).
    pub session_fs: Option<SessionFsConfig>,
    /// Connection-level Copilot request handler configuration.
    ///
    /// When set, the SDK registers itself as the runtime's request handler
    /// during [`Client::start`], so the runtime routes its model-layer HTTP and
    /// WebSocket traffic — for both CAPI and BYOK sessions — through the
    /// configured
    /// [`CopilotRequestHandler`]
    /// instead of issuing the calls itself.
    pub request_handler: Option<Arc<dyn crate::copilot_request_handler::CopilotRequestHandler>>,
    /// Optional [`TraceContextProvider`] used to inject W3C Trace Context
    /// headers (`traceparent` / `tracestate`) on outbound `session.create`,
    /// `session.resume`, and `session.send` requests.
    ///
    /// When [`MessageOptions`] carries a per-turn override (set via
    /// [`MessageOptions::with_trace_context`](crate::types::MessageOptions::with_trace_context)
    /// or the underlying fields), it takes precedence over this provider.
    ///
    /// [`MessageOptions`]: crate::types::MessageOptions
    pub on_get_trace_context: Option<Arc<dyn TraceContextProvider>>,
    /// OpenTelemetry config forwarded to the spawned CLI process. See
    /// [`TelemetryConfig`] for the env-var mapping. The SDK takes no
    /// OpenTelemetry dependency — this is pure spawn-time env injection.
    pub telemetry: Option<TelemetryConfig>,
    /// Override the directory where the CLI persists its state (sessions,
    /// auth, telemetry buffers). When set, exported as `COPILOT_HOME` to
    /// the spawned CLI process. Useful for sandboxing test runs or
    /// running multiple isolated SDK instances side-by-side.
    pub base_directory: Option<PathBuf>,
    /// Enable remote session support (Mission Control integration).
    /// When `true`, the SDK passes `--remote` to the spawned CLI process so
    /// sessions in a GitHub repository working directory are accessible from
    /// GitHub web and mobile. Ignored when connecting to an external server
    /// via [`Transport::External`].
    pub enable_remote_sessions: bool,
    /// Override the directory where the bundled CLI binary is extracted on
    /// first use.
    ///
    /// When `None` (the default), the SDK extracts the embedded CLI to
    /// `<platform cache dir>/github-copilot-sdk/cli/<version>/copilot[.exe]`,
    /// where the cache dir is [`dirs::cache_dir()`] —
    /// `%LOCALAPPDATA%` on Windows, `~/Library/Caches/` on macOS,
    /// `$XDG_CACHE_HOME` (or `~/.cache/`) on Linux. Use this knob to
    /// redirect the extraction (e.g. to a session-scoped temp directory in
    /// CI runners) without changing the global cache layout.
    ///
    /// Only applies when the `bundled-cli` cargo feature is on (the
    /// default). With `bundled-cli` disabled (`default-features = false`)
    /// there is no archive to re-extract at runtime — the binary lives
    /// at a build-time-known conventional path. To relocate that
    /// extraction, set `COPILOT_CLI_EXTRACT_DIR` (honored symmetrically
    /// at build and runtime); to point the runtime at a different
    /// binary altogether, use [`CliProgram::Path`] or `COPILOT_CLI_PATH`.
    pub bundled_cli_extract_dir: Option<PathBuf>,
    /// SDK-level mode controlling whether sessions get CLI-style defaults
    /// (the default) or are stripped to a minimal/safe baseline. See
    /// [`ClientMode`] for the contract and trade-offs.
    pub mode: ClientMode,
}

impl std::fmt::Debug for ClientOptions {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ClientOptions")
            .field("program", &self.program)
            .field("prefix_args", &self.prefix_args)
            .field("working_directory", &self.working_directory)
            .field("env", &self.env)
            .field("env_remove", &self.env_remove)
            .field("extra_args", &self.extra_args)
            .field("transport", &self.transport)
            .field(
                "github_token",
                &self.github_token.as_ref().map(|_| "<redacted>"),
            )
            .field("use_logged_in_user", &self.use_logged_in_user)
            .field("log_level", &self.log_level)
            .field(
                "session_idle_timeout_seconds",
                &self.session_idle_timeout_seconds,
            )
            .field(
                "on_list_models",
                &self.on_list_models.as_ref().map(|_| "<set>"),
            )
            .field("session_fs", &self.session_fs)
            .field(
                "request_handler",
                &self.request_handler.as_ref().map(|_| "<set>"),
            )
            .field(
                "on_get_trace_context",
                &self.on_get_trace_context.as_ref().map(|_| "<set>"),
            )
            .field("telemetry", &self.telemetry)
            .field("base_directory", &self.base_directory)
            .field("enable_remote_sessions", &self.enable_remote_sessions)
            .field("bundled_cli_extract_dir", &self.bundled_cli_extract_dir)
            .finish()
    }
}

/// Custom handler for [`Client::list_models`].
///
/// Implementations override the default `models.list` RPC, returning a
/// caller-supplied catalog of models. Set via [`ClientOptions::on_list_models`].
///
/// Implementations must be `Send + Sync` because [`Client`] is shared across
/// tasks. Errors returned by [`list_models`](Self::list_models) are propagated
/// from [`Client::list_models`] unchanged.
#[async_trait]
pub trait ListModelsHandler: Send + Sync + 'static {
    /// Return the list of available models.
    async fn list_models(&self) -> Result<Vec<Model>>;
}

/// Log verbosity for the CLI server (passed via `--log-level`).
#[derive(Debug, Clone, Copy, Eq, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum LogLevel {
    /// Suppress all CLI logs.
    None,
    /// Errors only.
    Error,
    /// Warnings and errors.
    Warning,
    /// Info and above.
    Info,
    /// Debug, info, warnings, errors.
    Debug,
    /// Everything, including trace output.
    All,
}

impl LogLevel {
    /// CLI argument value (e.g. `"info"`, `"debug"`).
    pub fn as_str(self) -> &'static str {
        match self {
            Self::None => "none",
            Self::Error => "error",
            Self::Warning => "warning",
            Self::Info => "info",
            Self::Debug => "debug",
            Self::All => "all",
        }
    }
}

impl std::fmt::Display for LogLevel {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.as_str())
    }
}

/// Backend exporter for the CLI's OpenTelemetry pipeline.
///
/// Maps to the `COPILOT_OTEL_EXPORTER_TYPE` environment variable on the
/// spawned CLI process. Wire values are `"otlp-http"` and `"file"`.
#[derive(Debug, Clone, Copy, Eq, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
#[non_exhaustive]
pub enum OtelExporterType {
    /// Export via OTLP HTTP to the endpoint configured by
    /// [`TelemetryConfig::otlp_endpoint`].
    OtlpHttp,
    /// Export to a JSON-lines file at the path configured by
    /// [`TelemetryConfig::file_path`].
    File,
}

impl OtelExporterType {
    /// Environment-variable value (`"otlp-http"` or `"file"`).
    pub fn as_str(self) -> &'static str {
        match self {
            Self::OtlpHttp => "otlp-http",
            Self::File => "file",
        }
    }
}

/// OTLP HTTP protocol used by the CLI's OpenTelemetry OTLP exporter.
///
/// Maps to the standard `OTEL_EXPORTER_OTLP_PROTOCOL` environment variable on
/// the spawned CLI process. Wire values are `"http/json"` and
/// `"http/protobuf"`.
#[derive(Debug, Clone, Copy, Eq, PartialEq, Serialize, Deserialize)]
#[non_exhaustive]
pub enum OtlpHttpProtocol {
    /// Export using OTLP/HTTP JSON.
    #[serde(rename = "http/json")]
    HttpJson,
    /// Export using OTLP/HTTP protobuf.
    #[serde(rename = "http/protobuf")]
    HttpProtobuf,
}

impl OtlpHttpProtocol {
    /// Environment-variable value (`"http/json"` or `"http/protobuf"`).
    pub fn as_str(self) -> &'static str {
        match self {
            Self::HttpJson => "http/json",
            Self::HttpProtobuf => "http/protobuf",
        }
    }
}

/// OpenTelemetry configuration forwarded to the spawned GitHub Copilot CLI
/// process.
///
/// When [`ClientOptions::telemetry`] is `Some(...)`, the SDK sets
/// `COPILOT_OTEL_ENABLED=true` plus any populated fields below as the
/// corresponding `OTEL_*` / `COPILOT_OTEL_*` environment variables. The
/// CLI's built-in OpenTelemetry exporter consumes these at startup. The
/// SDK itself takes no OpenTelemetry dependency.
///
/// Environment-variable mapping:
///
/// | Field                | Variable                                              |
/// |----------------------|-------------------------------------------------------|
/// | (any field set)      | `COPILOT_OTEL_ENABLED=true`                           |
/// | [`otlp_endpoint`]    | `OTEL_EXPORTER_OTLP_ENDPOINT`                         |
/// | [`otlp_protocol`]    | `OTEL_EXPORTER_OTLP_PROTOCOL`                         |
/// | [`file_path`]        | `COPILOT_OTEL_FILE_EXPORTER_PATH`                     |
/// | [`exporter_type`]    | `COPILOT_OTEL_EXPORTER_TYPE`                          |
/// | [`source_name`]      | `COPILOT_OTEL_SOURCE_NAME`                            |
/// | [`capture_content`]  | `OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT`  |
///
/// Caller-supplied entries in [`ClientOptions::env`] override these, so a
/// developer can pin any individual variable to a different value while
/// keeping the rest of the config managed by [`TelemetryConfig`].
///
/// Marked `#[non_exhaustive]` so future CLI-side telemetry knobs can be
/// added without breaking callers.
///
/// [`otlp_endpoint`]: Self::otlp_endpoint
/// [`otlp_protocol`]: Self::otlp_protocol
/// [`file_path`]: Self::file_path
/// [`exporter_type`]: Self::exporter_type
/// [`source_name`]: Self::source_name
/// [`capture_content`]: Self::capture_content
#[derive(Debug, Clone, Default)]
#[non_exhaustive]
pub struct TelemetryConfig {
    /// OTLP HTTP endpoint URL for trace/metric export.
    pub otlp_endpoint: Option<String>,
    /// OTLP HTTP protocol for all signals.
    pub otlp_protocol: Option<OtlpHttpProtocol>,
    /// File path for JSON-lines trace output.
    pub file_path: Option<PathBuf>,
    /// Exporter backend type. Typically [`OtelExporterType::OtlpHttp`] or
    /// [`OtelExporterType::File`].
    pub exporter_type: Option<OtelExporterType>,
    /// Instrumentation scope name. Useful for distinguishing this
    /// embedder's traces from other Copilot-CLI consumers exporting to the
    /// same backend.
    pub source_name: Option<String>,
    /// Whether the CLI captures GenAI message content (prompts and
    /// responses) on emitted spans. `Some(true)` opts in; `Some(false)`
    /// opts out; `None` leaves the CLI default (typically off).
    pub capture_content: Option<bool>,
}

impl TelemetryConfig {
    /// Construct an empty [`TelemetryConfig`]; all fields default to
    /// unset (`is_empty()` returns `true`).
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the OTLP HTTP endpoint URL for trace/metric export.
    pub fn with_otlp_endpoint(mut self, endpoint: impl Into<String>) -> Self {
        self.otlp_endpoint = Some(endpoint.into());
        self
    }

    /// Set the OTLP HTTP protocol for all signals.
    pub fn with_otlp_protocol(mut self, protocol: OtlpHttpProtocol) -> Self {
        self.otlp_protocol = Some(protocol);
        self
    }

    /// Set the file path for JSON-lines trace output.
    pub fn with_file_path(mut self, path: impl Into<PathBuf>) -> Self {
        self.file_path = Some(path.into());
        self
    }

    /// Set the exporter backend type.
    pub fn with_exporter_type(mut self, exporter_type: OtelExporterType) -> Self {
        self.exporter_type = Some(exporter_type);
        self
    }

    /// Set the instrumentation scope name. Useful for distinguishing
    /// this embedder's traces from other Copilot-CLI consumers
    /// exporting to the same backend.
    pub fn with_source_name(mut self, source_name: impl Into<String>) -> Self {
        self.source_name = Some(source_name.into());
        self
    }

    /// Opt in or out of GenAI message content capture on emitted spans.
    /// `true` opts in; `false` opts out. Leaving this unset preserves
    /// the CLI default (typically off).
    pub fn with_capture_content(mut self, capture: bool) -> Self {
        self.capture_content = Some(capture);
        self
    }

    /// Returns `true` if all fields are unset. Used by [`Client::start`]
    /// to decide whether to set `COPILOT_OTEL_ENABLED`.
    pub fn is_empty(&self) -> bool {
        self.otlp_endpoint.is_none()
            && self.otlp_protocol.is_none()
            && self.file_path.is_none()
            && self.exporter_type.is_none()
            && self.source_name.is_none()
            && self.capture_content.is_none()
    }
}

impl Default for ClientOptions {
    fn default() -> Self {
        Self {
            program: CliProgram::Resolve,
            prefix_args: Vec::new(),
            working_directory: std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")),
            env: Vec::new(),
            env_remove: Vec::new(),
            extra_args: Vec::new(),
            transport: Transport::default(),
            github_token: None,
            use_logged_in_user: None,
            log_level: None,
            session_idle_timeout_seconds: None,
            on_list_models: None,
            session_fs: None,
            request_handler: None,
            on_get_trace_context: None,
            telemetry: None,
            base_directory: None,
            enable_remote_sessions: false,
            bundled_cli_extract_dir: None,
            mode: ClientMode::default(),
        }
    }
}

impl ClientOptions {
    /// Construct a new [`ClientOptions`] with default values.
    ///
    /// Equivalent to [`ClientOptions::default`]; provided as a documented
    /// construction entry point for the builder chain. The struct is
    /// `#[non_exhaustive]`, so external callers cannot use struct-literal
    /// syntax — use this builder or [`Default::default`] plus mut-let.
    ///
    /// # Example
    ///
    /// ```
    /// # use github_copilot_sdk::{ClientOptions, LogLevel};
    /// let opts = ClientOptions::new()
    ///     .with_log_level(LogLevel::Debug)
    ///     .with_github_token("ghp_…");
    /// ```
    pub fn new() -> Self {
        Self::default()
    }

    /// How to locate the CLI binary. See [`CliProgram`].
    pub fn with_program(mut self, program: impl Into<CliProgram>) -> Self {
        self.program = program.into();
        self
    }

    /// Arguments prepended before `--server` (e.g. the script path for node).
    pub fn with_prefix_args<I, S>(mut self, args: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<OsString>,
    {
        self.prefix_args = args.into_iter().map(Into::into).collect();
        self
    }

    /// Working directory for the CLI process.
    pub fn with_cwd(mut self, cwd: impl Into<PathBuf>) -> Self {
        self.working_directory = cwd.into();
        self
    }

    /// Environment variables to set on the child process.
    pub fn with_env<I, K, V>(mut self, env: I) -> Self
    where
        I: IntoIterator<Item = (K, V)>,
        K: Into<OsString>,
        V: Into<OsString>,
    {
        self.env = env.into_iter().map(|(k, v)| (k.into(), v.into())).collect();
        self
    }

    /// Environment variable names to remove from the child process.
    pub fn with_env_remove<I, S>(mut self, names: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<OsString>,
    {
        self.env_remove = names.into_iter().map(Into::into).collect();
        self
    }

    /// Extra CLI flags appended after the transport-specific arguments.
    pub fn with_extra_args<I, S>(mut self, args: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        self.extra_args = args.into_iter().map(Into::into).collect();
        self
    }

    /// Transport mode used to communicate with the CLI server. See [`Transport`].
    pub fn with_transport(mut self, transport: Transport) -> Self {
        self.transport = transport;
        self
    }

    /// GitHub token for authentication. The SDK passes the token to the
    /// CLI via `--auth-token-env COPILOT_SDK_AUTH_TOKEN`.
    pub fn with_github_token(mut self, token: impl Into<String>) -> Self {
        self.github_token = Some(token.into());
        self
    }

    /// Whether the CLI should fall back to the logged-in `gh` user when
    /// no token is provided. See the field docs for default semantics.
    pub fn with_use_logged_in_user(mut self, use_logged_in: bool) -> Self {
        self.use_logged_in_user = Some(use_logged_in);
        self
    }

    /// Log level passed to the CLI server via `--log-level`.
    pub fn with_log_level(mut self, level: LogLevel) -> Self {
        self.log_level = Some(level);
        self
    }

    /// Server-wide idle timeout for sessions (seconds). Pass `0` to leave
    /// sessions running indefinitely (the CLI default).
    pub fn with_session_idle_timeout_seconds(mut self, seconds: u64) -> Self {
        self.session_idle_timeout_seconds = Some(seconds);
        self
    }

    /// Override [`Client::list_models`] with a caller-supplied handler.
    /// The handler is wrapped in `Arc` internally.
    pub fn with_list_models_handler<H>(mut self, handler: H) -> Self
    where
        H: ListModelsHandler + 'static,
    {
        self.on_list_models = Some(Arc::new(handler));
        self
    }

    /// Custom session filesystem provider configuration.
    pub fn with_session_fs(mut self, config: SessionFsConfig) -> Self {
        self.session_fs = Some(config);
        self
    }

    /// Register a connection-level Copilot request handler. The runtime will
    /// route its model-layer HTTP and WebSocket traffic through the handler
    /// configured here instead of issuing the calls itself. The handler is
    /// wrapped in `Arc` internally.
    pub fn with_request_handler<H>(mut self, handler: H) -> Self
    where
        H: crate::copilot_request_handler::CopilotRequestHandler,
    {
        self.request_handler = Some(Arc::new(handler));
        self
    }

    /// Set the [`TraceContextProvider`] used to inject W3C Trace Context
    /// headers on outbound `session.create` / `session.resume` /
    /// `session.send` requests. The provider is wrapped in `Arc` internally.
    pub fn with_trace_context_provider<P>(mut self, provider: P) -> Self
    where
        P: TraceContextProvider + 'static,
    {
        self.on_get_trace_context = Some(Arc::new(provider));
        self
    }

    /// OpenTelemetry config forwarded to the spawned CLI process.
    pub fn with_telemetry(mut self, config: TelemetryConfig) -> Self {
        self.telemetry = Some(config);
        self
    }

    /// Override the directory where the CLI persists its state. Set as
    /// `COPILOT_HOME` on the spawned CLI process.
    pub fn with_base_directory(mut self, dir: impl Into<PathBuf>) -> Self {
        self.base_directory = Some(dir.into());
        self
    }

    /// Enable remote session support (Mission Control). Passes `--remote`
    /// to the spawned CLI process.
    pub fn with_enable_remote_sessions(mut self, enabled: bool) -> Self {
        self.enable_remote_sessions = enabled;
        self
    }

    /// Override the directory where the bundled CLI binary is extracted on
    /// first use. See [`Self::bundled_cli_extract_dir`].
    ///
    /// Only applies when the `bundled-cli` cargo feature is on. With
    /// `bundled-cli` disabled (`default-features = false`), set
    /// `COPILOT_CLI_EXTRACT_DIR` to relocate the build-time extraction
    /// (honored symmetrically at build and runtime), or use
    /// [`CliProgram::Path`] / `COPILOT_CLI_PATH` to point at a different
    /// binary at runtime.
    pub fn with_bundled_cli_extract_dir(mut self, dir: impl Into<PathBuf>) -> Self {
        self.bundled_cli_extract_dir = Some(dir.into());
        self
    }

    /// Set the SDK [`ClientMode`]. Use [`ClientMode::Empty`] for any
    /// scenario where CLI-like ambient behavior is unsafe (e.g. multi-user
    /// servers). Empty mode additionally requires [`Self::base_directory`]
    /// or [`Self::session_fs`] to be set, validated at [`Client::start`].
    pub fn with_mode(mut self, mode: ClientMode) -> Self {
        self.mode = mode;
        self
    }
}

/// Validate a [`SessionFsConfig`] before sending `sessionFs.setProvider`.
fn validate_session_fs_config(cfg: &SessionFsConfig) -> Result<()> {
    if cfg.initial_cwd.trim().is_empty() {
        return Err(Error::with_message(
            ErrorKind::Session(SessionErrorKind::InvalidSessionFsConfig),
            "invalid SessionFsConfig: initial_cwd must not be empty",
        ));
    }
    if cfg.session_state_path.trim().is_empty() {
        return Err(Error::with_message(
            ErrorKind::Session(SessionErrorKind::InvalidSessionFsConfig),
            "invalid SessionFsConfig: session_state_path must not be empty",
        ));
    }
    Ok(())
}

/// Generate a fresh CSPRNG-backed token for authenticating an SDK-spawned
/// loopback CLI server. 128 bits of entropy, lowercase-hex encoded — not
/// a UUID (the schema-shaped IDs in this crate stay `String` per the
/// pre-1.0 review consensus, so adopting a `Uuid` type just for SDK-
/// generated secrets would be inconsistent and semantically misleading;
/// this is opaque random data, not an identifier).
fn generate_connection_token() -> String {
    let mut bytes = [0u8; 16];
    getrandom::getrandom(&mut bytes)
        .expect("OS CSPRNG (getrandom) is unavailable; cannot generate connection token");
    let mut hex = String::with_capacity(32);
    for byte in bytes {
        use std::fmt::Write;
        let _ = write!(hex, "{byte:02x}");
    }
    hex
}

/// Connection to a GitHub Copilot CLI server (stdio, TCP, or external).
///
/// Cheaply cloneable — cloning shares the underlying connection.
/// The child process (if any) is killed when the last clone drops.
#[derive(Clone)]
pub struct Client {
    inner: Arc<ClientInner>,
}

impl std::fmt::Debug for Client {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Client")
            .field("working_directory", &self.inner.cwd)
            .field("pid", &self.pid())
            .finish()
    }
}

struct ClientInner {
    child: parking_lot::Mutex<Option<Child>>,
    rpc: JsonRpcClient,
    cwd: PathBuf,
    request_rx: parking_lot::Mutex<Option<mpsc::UnboundedReceiver<JsonRpcRequest>>>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    router: router::SessionRouter,
    negotiated_protocol_version: OnceLock<u32>,
    state: parking_lot::Mutex<ConnectionState>,
    lifecycle_tx: broadcast::Sender<SessionLifecycleEvent>,
    on_list_models: Option<Arc<dyn ListModelsHandler>>,
    models_cache: parking_lot::Mutex<Arc<tokio::sync::OnceCell<Vec<Model>>>>,
    session_fs_configured: bool,
    session_fs_sqlite_declared: bool,
    /// Inbound `llmInference.*` dispatcher, installed when
    /// [`ClientOptions::request_handler`] is set.
    llm_inference: OnceLock<Arc<copilot_request_handler::CopilotRequestDispatcher>>,
    on_get_trace_context: Option<Arc<dyn TraceContextProvider>>,
    /// Token sent in the `connect` handshake. Auto-generated when the
    /// SDK spawns its own CLI in TCP mode and no explicit token is set;
    /// `None` for stdio and for external-server transport without an
    /// explicit token.
    effective_connection_token: Option<String>,
    /// SDK [`ClientMode`] captured at start time. Drives empty-mode safe
    /// defaults inside `create_session` / `resume_session`.
    pub(crate) mode: ClientMode,
}

impl Client {
    /// Start a CLI server process with the given options.
    ///
    /// For [`Transport::Stdio`], spawns the CLI with `--stdio` and communicates
    /// over stdin/stdout pipes. For [`Transport::Tcp`], spawns with `--port`
    /// and connects via TCP once the server reports it is listening. For
    /// [`Transport::External`], connects to an already-running server.
    ///
    /// After establishing the connection, calls [`verify_protocol_version`](Self::verify_protocol_version)
    /// to ensure the CLI server speaks a compatible protocol version.
    /// When [`ClientOptions::session_fs`] is set, also calls
    /// `sessionFs.setProvider` to register the SDK as the filesystem
    /// backend.
    pub async fn start(options: ClientOptions) -> Result<Self> {
        let start_time = Instant::now();
        if options.mode == ClientMode::Empty
            && options.base_directory.is_none()
            && options.session_fs.is_none()
        {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "ClientMode::Empty requires either `base_directory` or \
                 `session_fs` to be set (no implicit ~/.copilot fallback).",
            ));
        }
        if let Some(cfg) = &options.session_fs {
            validate_session_fs_config(cfg)?;
        }
        // Auth options only make sense when the SDK spawns the CLI; with an
        // external server, the server manages its own auth.
        if matches!(options.transport, Transport::External { .. }) {
            if options.github_token.is_some() {
                return Err(Error::with_message(
                    ErrorKind::InvalidConfig,
                    "invalid client configuration: github_token cannot be used with \
                     Transport::External (external server manages its own auth)",
                ));
            }
            if options.use_logged_in_user == Some(true) {
                return Err(Error::with_message(
                    ErrorKind::InvalidConfig,
                    "invalid client configuration: use_logged_in_user cannot be used with \
                     Transport::External (external server manages its own auth)",
                ));
            }
        }
        // Validate token shape. Stdio variants no longer carry a token
        // (enforced by the type). For Tcp/External, empty-string is
        // rejected eagerly.
        match &options.transport {
            Transport::Tcp {
                connection_token: Some(t),
                ..
            }
            | Transport::External {
                connection_token: Some(t),
                ..
            } if t.is_empty() => {
                return Err(Error::with_message(
                    ErrorKind::InvalidConfig,
                    "invalid client configuration: connection_token must be a non-empty string",
                ));
            }
            _ => {}
        }
        // Capture (and where needed, auto-generate) the token actually sent
        // to the server. For Tcp, the SDK auto-generates one when the
        // caller leaves it unset so the loopback listener is safe by
        // default.
        let mut options = options;
        let effective_connection_token: Option<String> = match &mut options.transport {
            Transport::Stdio => None,
            Transport::Tcp {
                connection_token, ..
            } => Some(
                connection_token
                    .get_or_insert_with(generate_connection_token)
                    .clone(),
            ),
            Transport::External {
                connection_token, ..
            } => connection_token.clone(),
        };
        let session_fs_config = options.session_fs.clone();
        let request_handler = options.request_handler.clone();
        let session_fs_sqlite_declared = session_fs_config
            .as_ref()
            .and_then(|c| c.capabilities.as_ref())
            .is_some_and(|caps| caps.sqlite);
        let program = match &options.program {
            CliProgram::Path(path) => {
                info!(path = %path.display(), "using explicit copilot CLI path");
                path.clone()
            }
            CliProgram::Resolve => {
                let resolved = resolve::copilot_binary_with_extract_dir(
                    options.bundled_cli_extract_dir.as_deref(),
                )?;
                info!(path = %resolved.display(), "resolved copilot CLI");
                #[cfg(windows)]
                {
                    if let Some(ext) = resolved.extension().and_then(|e| e.to_str()).filter(|ext| {
                        ext.eq_ignore_ascii_case("cmd") || ext.eq_ignore_ascii_case("bat")
                    }) {
                        warn!(
                            path = %resolved.display(),
                            ext = %ext,
                            "resolved copilot CLI is a .cmd/.bat wrapper; \
                             this may cause console window flashes on Windows"
                        );
                    }
                }
                resolved
            }
        };

        let client = match options.transport {
            Transport::External {
                ref host,
                port,
                connection_token: _,
            } => {
                info!(host = %host, port = %port, "connecting to external CLI server");
                let connect_start = Instant::now();
                let stream = TcpStream::connect((host.as_str(), port)).await?;
                debug!(
                    elapsed_ms = connect_start.elapsed().as_millis(),
                    host = %host,
                    port,
                    "Client::start TCP connect complete"
                );
                let (reader, writer) = tokio::io::split(stream);
                Self::from_transport(
                    reader,
                    writer,
                    None,
                    options.working_directory,
                    options.on_list_models,
                    session_fs_config.is_some(),
                    session_fs_sqlite_declared,
                    options.on_get_trace_context,
                    effective_connection_token.clone(),
                    options.mode,
                )?
            }
            Transport::Tcp {
                port,
                connection_token: _,
            } => {
                let (mut child, actual_port) = Self::spawn_tcp(&program, &options, port).await?;
                let connect_start = Instant::now();
                let stream = TcpStream::connect(("127.0.0.1", actual_port)).await?;
                debug!(
                    elapsed_ms = connect_start.elapsed().as_millis(),
                    port = actual_port,
                    "Client::start TCP connect complete"
                );
                let (reader, writer) = tokio::io::split(stream);
                Self::drain_stderr(&mut child);
                Self::from_transport(
                    reader,
                    writer,
                    Some(child),
                    options.working_directory,
                    options.on_list_models,
                    session_fs_config.is_some(),
                    session_fs_sqlite_declared,
                    options.on_get_trace_context,
                    effective_connection_token.clone(),
                    options.mode,
                )?
            }
            Transport::Stdio => {
                let mut child = Self::spawn_stdio(&program, &options)?;
                let stdin = child.stdin.take().expect("stdin is piped");
                let stdout = child.stdout.take().expect("stdout is piped");
                Self::drain_stderr(&mut child);
                Self::from_transport(
                    stdout,
                    stdin,
                    Some(child),
                    options.working_directory,
                    options.on_list_models,
                    session_fs_config.is_some(),
                    session_fs_sqlite_declared,
                    options.on_get_trace_context,
                    effective_connection_token.clone(),
                    options.mode,
                )?
            }
        };

        debug!(
            elapsed_ms = start_time.elapsed().as_millis(),
            "Client::start transport setup complete"
        );
        client.verify_protocol_version().await?;
        debug!(
            elapsed_ms = start_time.elapsed().as_millis(),
            "Client::start protocol verification complete"
        );
        if let Some(cfg) = session_fs_config {
            let session_fs_start = Instant::now();
            let capabilities = cfg.capabilities.as_ref().map(|c| {
                crate::generated::api_types::SessionFsSetProviderCapabilities {
                    sqlite: Some(c.sqlite),
                }
            });
            let request = crate::generated::api_types::SessionFsSetProviderRequest {
                capabilities,
                conventions: cfg.conventions.into_wire(),
                initial_cwd: cfg.initial_cwd,
                session_state_path: cfg.session_state_path,
            };
            client.rpc().session_fs().set_provider(request).await?;
            debug!(
                elapsed_ms = session_fs_start.elapsed().as_millis(),
                "Client::start session filesystem setup complete"
            );
        }
        if let Some(handler) = request_handler {
            let llm_inference_start = Instant::now();
            let dispatcher = Arc::new(copilot_request_handler::CopilotRequestDispatcher::new(
                handler,
            ));
            dispatcher.set_client(Arc::downgrade(&client.inner));
            let _ = client.inner.llm_inference.set(dispatcher.clone());
            // Start the router early (before any session is registered) so the
            // startup model catalog request is dispatched to the handler.
            client.inner.router.ensure_started(
                &client.inner.notification_tx,
                &client.inner.request_rx,
                Some(dispatcher.clone()),
            );
            client.rpc().llm_inference().set_provider().await?;
            debug!(
                elapsed_ms = llm_inference_start.elapsed().as_millis(),
                "Client::start Copilot request handler registration complete"
            );
        }
        debug!(
            elapsed_ms = start_time.elapsed().as_millis(),
            "Client::start complete"
        );
        Ok(client)
    }

    /// Create a Client from raw async streams (no child process).
    ///
    /// Useful for testing or connecting to a server over a custom transport.
    pub fn from_streams(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        cwd: PathBuf,
    ) -> Result<Self> {
        Self::from_transport(
            reader,
            writer,
            None,
            cwd,
            None,
            false,
            false,
            None,
            None,
            ClientMode::default(),
        )
    }

    /// Construct a [`Client`] from raw streams with a
    /// [`TraceContextProvider`] preset, for integration testing.
    ///
    /// Mirrors [`from_streams`](Self::from_streams) but exposes the
    /// `on_get_trace_context` plumbing so tests can verify outbound
    /// `traceparent` / `tracestate` injection on `session.create`,
    /// `session.resume`, and `session.send`.
    #[cfg(any(test, feature = "test-support"))]
    pub fn from_streams_with_trace_provider(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        cwd: PathBuf,
        provider: Arc<dyn TraceContextProvider>,
    ) -> Result<Self> {
        Self::from_transport(
            reader,
            writer,
            None,
            cwd,
            None,
            false,
            false,
            Some(provider),
            None,
            ClientMode::default(),
        )
    }

    /// Construct a [`Client`] from raw streams with a preset
    /// `effective_connection_token`, for integration testing the
    /// `connect` handshake's token-forwarding path.
    #[cfg(any(test, feature = "test-support"))]
    pub fn from_streams_with_connection_token(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        cwd: PathBuf,
        token: Option<String>,
    ) -> Result<Self> {
        Self::from_transport(
            reader,
            writer,
            None,
            cwd,
            None,
            false,
            false,
            None,
            token,
            ClientMode::default(),
        )
    }

    /// Public test-only wrapper around the random connection-token
    /// generator used by [`Client::start`] when the SDK spawns a TCP
    /// server without an explicit token. Lets integration tests
    /// validate the token shape (32-char lowercase hex, 128 bits of
    /// entropy) without re-implementing the helper.
    #[cfg(any(test, feature = "test-support"))]
    pub fn generate_connection_token_for_test() -> String {
        generate_connection_token()
    }

    #[allow(clippy::too_many_arguments)]
    fn from_transport(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        child: Option<Child>,
        cwd: PathBuf,
        on_list_models: Option<Arc<dyn ListModelsHandler>>,
        session_fs_configured: bool,
        session_fs_sqlite_declared: bool,
        on_get_trace_context: Option<Arc<dyn TraceContextProvider>>,
        effective_connection_token: Option<String>,
        mode: ClientMode,
    ) -> Result<Self> {
        let setup_start = Instant::now();
        let (request_tx, request_rx) = mpsc::unbounded_channel::<JsonRpcRequest>();
        let (notification_broadcast_tx, _) = broadcast::channel::<JsonRpcNotification>(1024);
        let rpc = JsonRpcClient::new(
            writer,
            reader,
            notification_broadcast_tx.clone(),
            request_tx,
        );

        let pid = child.as_ref().and_then(|c| c.id());
        info!(pid = ?pid, "copilot CLI client ready");

        let client = Self {
            inner: Arc::new(ClientInner {
                child: parking_lot::Mutex::new(child),
                rpc,
                cwd,
                request_rx: parking_lot::Mutex::new(Some(request_rx)),
                notification_tx: notification_broadcast_tx,
                router: router::SessionRouter::new(),
                negotiated_protocol_version: OnceLock::new(),
                state: parking_lot::Mutex::new(ConnectionState::Connected),
                lifecycle_tx: broadcast::channel(256).0,
                on_list_models,
                models_cache: parking_lot::Mutex::new(Arc::new(tokio::sync::OnceCell::new())),
                session_fs_configured,
                session_fs_sqlite_declared,
                llm_inference: OnceLock::new(),
                on_get_trace_context,
                effective_connection_token,
                mode,
            }),
        };
        client.spawn_lifecycle_dispatcher();
        debug!(
            elapsed_ms = setup_start.elapsed().as_millis(),
            pid = ?pid,
            "Client::from_transport setup complete"
        );
        Ok(client)
    }

    /// Spawn the background task that re-broadcasts `session.lifecycle`
    /// notifications via [`ClientInner::lifecycle_tx`] to subscribers
    /// returned by [`Self::subscribe_lifecycle`].
    fn spawn_lifecycle_dispatcher(&self) {
        let inner = Arc::clone(&self.inner);
        let mut notif_rx = inner.notification_tx.subscribe();
        tokio::spawn(async move {
            loop {
                match notif_rx.recv().await {
                    Ok(notification) => {
                        if notification.method != "session.lifecycle" {
                            continue;
                        }
                        let Some(params) = notification.params.as_ref() else {
                            continue;
                        };
                        let event: SessionLifecycleEvent =
                            match serde_json::from_value(params.clone()) {
                                Ok(e) => e,
                                Err(e) => {
                                    warn!(
                                        error = %e,
                                        "failed to deserialize session.lifecycle notification"
                                    );
                                    continue;
                                }
                            };
                        // `send` only errors when there are no subscribers — that's
                        // the normal case before any consumer calls subscribe_lifecycle.
                        let _ = inner.lifecycle_tx.send(event);
                    }
                    Err(tokio::sync::broadcast::error::RecvError::Lagged(n)) => {
                        warn!(missed = n, "lifecycle dispatcher lagged");
                    }
                    Err(tokio::sync::broadcast::error::RecvError::Closed) => break,
                }
            }
        });
    }

    fn build_command(program: &Path, options: &ClientOptions) -> Command {
        let mut command = Command::new(program);
        for arg in &options.prefix_args {
            command.arg(arg);
        }
        // Inject the SDK auth token first so explicit `env` / `env_remove`
        // entries can override or strip it.
        if let Some(token) = &options.github_token {
            command.env("COPILOT_SDK_AUTH_TOKEN", token);
        }
        // Inject telemetry env vars before user env so callers can still
        // override individual variables via `options.env`.
        if let Some(telemetry) = &options.telemetry {
            command.env("COPILOT_OTEL_ENABLED", "true");
            if let Some(endpoint) = &telemetry.otlp_endpoint {
                command.env("OTEL_EXPORTER_OTLP_ENDPOINT", endpoint);
            }
            if let Some(protocol) = telemetry.otlp_protocol {
                command.env("OTEL_EXPORTER_OTLP_PROTOCOL", protocol.as_str());
            }
            if let Some(path) = &telemetry.file_path {
                command.env("COPILOT_OTEL_FILE_EXPORTER_PATH", path);
            }
            if let Some(exporter) = telemetry.exporter_type {
                command.env("COPILOT_OTEL_EXPORTER_TYPE", exporter.as_str());
            }
            if let Some(source) = &telemetry.source_name {
                command.env("COPILOT_OTEL_SOURCE_NAME", source);
            }
            if let Some(capture) = telemetry.capture_content {
                command.env(
                    "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT",
                    if capture { "true" } else { "false" },
                );
            }
        }
        if let Some(dir) = &options.base_directory {
            command.env("COPILOT_HOME", dir);
        }
        // Empty mode disables the process-wide system keychain so the CLI
        // falls back to file-based credentials scoped to COPILOT_HOME.
        if options.mode == ClientMode::Empty {
            command.env("COPILOT_DISABLE_KEYTAR", "1");
        }
        if let Transport::Tcp {
            connection_token: Some(token),
            ..
        } = &options.transport
        {
            command.env("COPILOT_CONNECTION_TOKEN", token);
        }
        for (key, value) in &options.env {
            command.env(key, value);
        }
        for key in &options.env_remove {
            command.env_remove(key);
        }
        command
            .current_dir(&options.working_directory)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped());

        #[cfg(windows)]
        {
            use std::os::windows::process::CommandExt;
            const CREATE_NO_WINDOW: u32 = 0x08000000;
            command.as_std_mut().creation_flags(CREATE_NO_WINDOW);
        }

        command
    }

    /// Returns the CLI auth flags derived from [`ClientOptions::github_token`]
    /// and [`ClientOptions::use_logged_in_user`].
    ///
    /// When a token is set, adds `--auth-token-env COPILOT_SDK_AUTH_TOKEN`.
    /// When the effective `use_logged_in_user` is `false` (either explicitly
    /// or because a token was provided without an override), adds
    /// `--no-auto-login`.
    fn auth_args(options: &ClientOptions) -> Vec<&'static str> {
        let mut args: Vec<&'static str> = Vec::new();
        if options.github_token.is_some() {
            args.push("--auth-token-env");
            args.push("COPILOT_SDK_AUTH_TOKEN");
        }
        let use_logged_in = options
            .use_logged_in_user
            .unwrap_or(options.github_token.is_none());
        if !use_logged_in {
            args.push("--no-auto-login");
        }
        args
    }

    /// Returns `--session-idle-timeout <secs>` when
    /// [`ClientOptions::session_idle_timeout_seconds`] is `Some(n)` with
    /// `n > 0`. Otherwise returns an empty vector.
    fn session_idle_timeout_args(options: &ClientOptions) -> Vec<String> {
        match options.session_idle_timeout_seconds {
            Some(secs) if secs > 0 => {
                vec!["--session-idle-timeout".to_string(), secs.to_string()]
            }
            _ => Vec::new(),
        }
    }

    fn remote_args(options: &ClientOptions) -> Vec<String> {
        if options.enable_remote_sessions {
            vec!["--remote".to_string()]
        } else {
            Vec::new()
        }
    }

    fn log_level_args(options: &ClientOptions) -> Vec<&'static str> {
        match options.log_level {
            Some(level) => vec!["--log-level", level.as_str()],
            None => Vec::new(),
        }
    }

    fn spawn_stdio(program: &Path, options: &ClientOptions) -> Result<Child> {
        info!(cwd = ?options.working_directory, program = %program.display(), "spawning copilot CLI (stdio)");
        let mut command = Self::build_command(program, options);
        command
            .args(["--server", "--stdio", "--no-auto-update"])
            .args(Self::log_level_args(options))
            .args(Self::auth_args(options))
            .args(Self::session_idle_timeout_args(options))
            .args(Self::remote_args(options))
            .args(&options.extra_args)
            .stdin(Stdio::piped());
        let spawn_start = Instant::now();
        let child = command.spawn()?;
        debug!(
            elapsed_ms = spawn_start.elapsed().as_millis(),
            "Client::spawn_stdio subprocess spawned"
        );
        Ok(child)
    }

    async fn spawn_tcp(program: &Path, options: &ClientOptions, port: u16) -> Result<(Child, u16)> {
        info!(cwd = ?options.working_directory, program = %program.display(), port = %port, "spawning copilot CLI (tcp)");
        let mut command = Self::build_command(program, options);
        command
            .args(["--server", "--port", &port.to_string(), "--no-auto-update"])
            .args(Self::log_level_args(options))
            .args(Self::auth_args(options))
            .args(Self::session_idle_timeout_args(options))
            .args(Self::remote_args(options))
            .args(&options.extra_args)
            .stdin(Stdio::null());
        let spawn_start = Instant::now();
        let mut child = command.spawn()?;
        debug!(
            elapsed_ms = spawn_start.elapsed().as_millis(),
            "Client::spawn_tcp subprocess spawned"
        );
        let stdout = child.stdout.take().expect("stdout is piped");

        let (port_tx, port_rx) = oneshot::channel::<u16>();
        let span = tracing::error_span!("copilot_cli_port_scan");
        tokio::spawn(
            async move {
                // Scan stdout for the port announcement.
                let port_re = regex::Regex::new(r"listening on port (\d+)").expect("valid regex");
                let mut lines = BufReader::new(stdout).lines();
                let mut port_tx = Some(port_tx);
                while let Ok(Some(line)) = lines.next_line().await {
                    debug!(line = %line, "CLI stdout");
                    if let Some(tx) = port_tx.take() {
                        if let Some(caps) = port_re.captures(&line)
                            && let Some(p) =
                                caps.get(1).and_then(|m| m.as_str().parse::<u16>().ok())
                        {
                            let _ = tx.send(p);
                            continue;
                        }
                        // Not the port line — put tx back
                        port_tx = Some(tx);
                    }
                }
            }
            .instrument(span),
        );

        let port_wait_start = Instant::now();
        let actual_port = tokio::time::timeout(std::time::Duration::from_secs(10), port_rx)
            .await
            .map_err(|_| Error::from(ErrorKind::Protocol(ProtocolErrorKind::CliStartupTimeout)))?
            .map_err(|_| Error::from(ErrorKind::Protocol(ProtocolErrorKind::CliStartupFailed)))?;

        debug!(
            elapsed_ms = port_wait_start.elapsed().as_millis(),
            port = actual_port,
            "Client::spawn_tcp TCP port wait complete"
        );
        info!(port = %actual_port, "CLI server listening");
        Ok((child, actual_port))
    }

    fn drain_stderr(child: &mut Child) {
        if let Some(stderr) = child.stderr.take() {
            let span = tracing::error_span!("copilot_cli");
            tokio::spawn(
                async move {
                    let mut reader = BufReader::new(stderr).lines();
                    while let Ok(Some(line)) = reader.next_line().await {
                        warn!(line = %line, "CLI stderr");
                    }
                }
                .instrument(span),
            );
        }
    }

    /// Returns the working directory of the CLI process.
    pub fn cwd(&self) -> &PathBuf {
        &self.inner.cwd
    }

    /// Returns the SDK [`ClientMode`] this client was started with.
    pub fn mode(&self) -> ClientMode {
        self.inner.mode
    }

    /// Typed RPC namespace for server-level methods.
    ///
    /// Every protocol method lives here under its schema-aligned path —
    /// e.g. `client.rpc().models().list()`. Wire method names and request/
    /// response types are generated from the protocol schema, so the typed
    /// namespace can't drift from the wire contract.
    ///
    /// The hand-authored helpers on [`Client`] delegate to this namespace
    /// and remain the recommended entry point for everyday use; reach for
    /// `rpc()` when you want a method without a hand-written wrapper.
    pub fn rpc(&self) -> crate::generated::rpc::ClientRpc<'_> {
        crate::generated::rpc::ClientRpc { client: self }
    }

    /// Send a JSON-RPC request and wait for the response.
    #[allow(dead_code, reason = "convenience for future internal use")]
    pub(crate) async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse> {
        self.inner.rpc.send_request(method, params).await
    }

    /// Send a JSON-RPC request, check for errors, and return the result value.
    ///
    /// This is the primary method for session-level RPC calls. It wraps
    /// the internal send/receive cycle with error checking so callers
    /// don't need to inspect the response manually.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** The frame is committed to the wire via the
    /// writer-actor task before the future yields; cancelling the await
    /// (via `tokio::time::timeout`, `select!`, or dropped JoinHandle)
    /// drops the response oneshot but does not desync the transport.
    /// The pending-requests entry is cleaned up by an RAII guard.
    /// However, the call's *side effect* on the CLI may still occur —
    /// the CLI receives the request and processes it; the caller just
    /// won't see the response. For idempotent methods this is fine; for
    /// non-idempotent methods (e.g. `session.create`) the caller should
    /// avoid wrapping the call in a timeout shorter than the expected
    /// CLI processing window.
    pub async fn call(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<serde_json::Value> {
        self.call_with_inline_callback(method, params, None).await
    }

    /// Same as [`call`](Self::call), but installs an `inline_callback`
    /// that runs synchronously on the JSON-RPC read task the instant the
    /// successful response is parsed, before it is delivered to this
    /// awaiter and before the read loop dispatches the next message.
    ///
    /// This is the only way to perform client-side bookkeeping (for
    /// example, registering a server-assigned session id with the
    /// router) that must be visible to any notification or request the
    /// server may emit on the same connection immediately after the
    /// response.
    ///
    /// If the callback returns an error, that error is propagated to
    /// this awaiter in place of the response. The callback never causes
    /// the read loop to crash.
    pub(crate) async fn call_with_inline_callback(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
        inline_callback: Option<crate::jsonrpc::InlineResponseCallback>,
    ) -> Result<serde_json::Value> {
        let session_id: Option<SessionId> = params
            .as_ref()
            .and_then(|p| p.get("sessionId"))
            .and_then(|v| v.as_str())
            .map(SessionId::from);
        let response = self
            .inner
            .rpc
            .send_request_with_inline_callback(method, params, inline_callback)
            .await?;
        if let Some(err) = response.error {
            if err.message.contains("Session not found") {
                return Err(ErrorKind::Session(SessionErrorKind::NotFound(
                    session_id.unwrap_or_else(|| "unknown".into()),
                ))
                .into());
            }
            return Err(Error::with_message(
                ErrorKind::Rpc { code: err.code },
                err.message,
            ));
        }
        Ok(response.result.unwrap_or(serde_json::Value::Null))
    }

    /// Send a JSON-RPC response back to the CLI (e.g. for permission or tool call requests).
    pub(crate) async fn send_response(&self, response: &JsonRpcResponse) -> Result<()> {
        self.inner.rpc.write(response).await
    }

    /// Reconstruct a [`Client`] handle from a shared inner pointer.
    pub(crate) fn from_inner(inner: Arc<ClientInner>) -> Self {
        Self { inner }
    }

    /// Take the receiver for incoming JSON-RPC requests from the CLI.
    ///
    /// Can only be called once — subsequent calls return `None`.
    #[expect(dead_code, reason = "reserved for future pub(crate) use")]
    pub(crate) fn take_request_rx(&self) -> Option<mpsc::UnboundedReceiver<JsonRpcRequest>> {
        self.inner.request_rx.lock().take()
    }

    /// Register a session to receive filtered events and requests.
    ///
    /// Returns per-session channels for notifications and requests, routed
    /// by `sessionId`. Starts the internal router on first call.
    ///
    /// When done, call [`unregister_session`](Self::unregister_session) to
    /// clean up (typically on session destroy).
    pub(crate) fn register_session(
        &self,
        session_id: &SessionId,
    ) -> crate::router::SessionChannels {
        self.inner.router.ensure_started(
            &self.inner.notification_tx,
            &self.inner.request_rx,
            self.inner.llm_inference.get().cloned(),
        );
        self.inner.router.register(session_id)
    }

    /// Unregister a session, dropping its per-session channels.
    pub(crate) fn unregister_session(&self, session_id: &SessionId) {
        self.inner.router.unregister(session_id);
    }

    /// Returns the protocol version negotiated with the CLI server, if any.
    ///
    /// Set during [`start`](Self::start). Returns `None` if the server didn't
    /// report a version, or if the client was created via
    /// [`from_streams`](Self::from_streams) without calling
    /// [`verify_protocol_version`](Self::verify_protocol_version).
    pub fn protocol_version(&self) -> Option<u32> {
        self.inner.negotiated_protocol_version.get().copied()
    }

    /// Verify the CLI server's protocol version is within the supported range.
    ///
    /// Called automatically by [`start`](Self::start). Call manually after
    /// [`from_streams`](Self::from_streams) if you need version verification
    /// on a custom transport.
    ///
    /// # Handshake sequence
    ///
    /// 1. Sends the `connect` JSON-RPC method, forwarding the
    ///    [`Transport`]'s `connection_token` (or the auto-generated
    ///    token for SDK-spawned TCP servers) as the `token` param. This
    ///    is the canonical handshake used by all SDK languages and is
    ///    what the CLI uses to enforce loopback authentication when
    ///    started with `COPILOT_CONNECTION_TOKEN`.
    /// 2. If the server returns `-32601` (`MethodNotFound`), falls back
    ///    to the legacy `ping` RPC. This preserves compatibility with
    ///    older CLI versions that predate `connect`.
    ///
    /// # Result
    ///
    /// Returns an error if the negotiated `protocolVersion` is outside
    /// `MIN_PROTOCOL_VERSION`..=[`SDK_PROTOCOL_VERSION`]. If the server
    /// doesn't report a version, logs a warning and succeeds.
    pub async fn verify_protocol_version(&self) -> Result<()> {
        let handshake_start = Instant::now();
        let mut used_fallback_ping = false;
        // Try the new `connect` handshake first (sends the connection
        // token, if any). Fall back to `ping` for legacy CLI servers
        // that don't expose `connect` (-32601 MethodNotFound).
        let server_version = match self.connect_handshake().await {
            Ok(v) => v,
            Err(ref e) if e.rpc_code() == Some(error_codes::METHOD_NOT_FOUND) => {
                used_fallback_ping = true;
                self.ping(None).await?.protocol_version
            }
            Err(e) => return Err(e),
        };

        match server_version {
            None => {
                warn!("CLI server did not report protocolVersion; skipping version check");
            }
            Some(v) if !(MIN_PROTOCOL_VERSION..=SDK_PROTOCOL_VERSION).contains(&v) => {
                return Err(ErrorKind::Protocol(ProtocolErrorKind::VersionMismatch {
                    server: v,
                    min: MIN_PROTOCOL_VERSION,
                    max: SDK_PROTOCOL_VERSION,
                })
                .into());
            }
            Some(v) => {
                if let Some(&existing) = self.inner.negotiated_protocol_version.get() {
                    if existing != v {
                        return Err(ErrorKind::Protocol(ProtocolErrorKind::VersionChanged {
                            previous: existing,
                            current: v,
                        })
                        .into());
                    }
                } else {
                    let _ = self.inner.negotiated_protocol_version.set(v);
                }
            }
        }

        debug!(
            elapsed_ms = handshake_start.elapsed().as_millis(),
            protocol_version = ?server_version,
            used_fallback_ping,
            "Client::verify_protocol_version protocol handshake complete"
        );
        Ok(())
    }

    /// Send the `connect` JSON-RPC handshake. Returns the server's
    /// reported protocol version, or `None` if the server omits it.
    /// Forwards the [`Transport`]'s `connection_token` (or the
    /// auto-generated token for SDK-spawned TCP servers) as the `token`
    /// param. Server-side, the token is required when the server was
    /// started with `COPILOT_CONNECTION_TOKEN`.
    async fn connect_handshake(&self) -> Result<Option<u32>> {
        let result = self
            .rpc()
            .connect(crate::generated::api_types::ConnectRequest {
                token: self.inner.effective_connection_token.clone(),
            })
            .await?;
        Ok(u32::try_from(result.protocol_version).ok())
    }

    /// Send a `ping` RPC and return the typed [`PingResponse`].
    ///
    /// Pass `Some(message)` to have the server echo it back; pass `None` for
    /// a bare health check. The response includes a `protocolVersion` when
    /// the CLI reports one.
    ///
    /// [`PingResponse`]: crate::types::PingResponse
    pub async fn ping(&self, message: Option<&str>) -> Result<crate::types::PingResponse> {
        let params = match message {
            Some(m) => serde_json::json!({ "message": m }),
            None => serde_json::json!({}),
        };
        let value = self
            .call(generated::api_types::rpc_methods::PING, Some(params))
            .await?;
        Ok(serde_json::from_value(value)?)
    }

    /// List persisted sessions, optionally filtered by working directory,
    /// repository, or git context.
    pub async fn list_sessions(
        &self,
        filter: Option<SessionListFilter>,
    ) -> Result<Vec<SessionMetadata>> {
        let params = match filter {
            Some(f) => serde_json::json!({ "filter": f }),
            None => serde_json::json!({}),
        };
        let result = self.call("session.list", Some(params)).await?;
        let response: ListSessionsResponse = serde_json::from_value(result)?;
        Ok(response.sessions)
    }

    /// Fetch metadata for a specific persisted session by ID.
    ///
    /// Returns `Ok(None)` if no session with the given ID exists. More
    /// efficient than calling [`list_sessions`](Self::list_sessions) and
    /// filtering when you only need data for a single session.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(client: &github_copilot_sdk::Client) -> Result<(), github_copilot_sdk::Error> {
    /// use github_copilot_sdk::types::SessionId;
    /// if let Some(metadata) = client.get_session_metadata(&SessionId::new("session-123")).await? {
    ///     println!("Session started at: {}", metadata.start_time);
    /// }
    /// # Ok(())
    /// # }
    /// ```
    pub async fn get_session_metadata(
        &self,
        session_id: &SessionId,
    ) -> Result<Option<SessionMetadata>> {
        let result = self
            .call(
                "session.getMetadata",
                Some(serde_json::json!({ "sessionId": session_id })),
            )
            .await?;
        let response: GetSessionMetadataResponse = serde_json::from_value(result)?;
        Ok(response.session)
    }

    /// Delete a persisted session by ID.
    pub async fn delete_session(&self, session_id: &SessionId) -> Result<()> {
        self.call(
            "session.delete",
            Some(serde_json::json!({ "sessionId": session_id })),
        )
        .await?;
        Ok(())
    }

    /// Return the ID of the most recently updated session, if any.
    ///
    /// Useful for resuming the last conversation when the session ID was
    /// not stored. Returns `Ok(None)` if no sessions exist.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(client: &github_copilot_sdk::Client) -> Result<(), github_copilot_sdk::Error> {
    /// if let Some(last_id) = client.get_last_session_id().await? {
    ///     println!("Last session: {last_id}");
    /// }
    /// # Ok(())
    /// # }
    /// ```
    pub async fn get_last_session_id(&self) -> Result<Option<SessionId>> {
        let result = self
            .call("session.getLastId", Some(serde_json::json!({})))
            .await?;
        let response: GetLastSessionIdResponse = serde_json::from_value(result)?;
        Ok(response.session_id)
    }

    /// Return the ID of the session currently displayed in the TUI, if any.
    ///
    /// Only meaningful when connected to a server running in TUI+server mode
    /// (`--ui-server`). Returns `Ok(None)` if no foreground session is set.
    pub async fn get_foreground_session_id(&self) -> Result<Option<SessionId>> {
        let result = self
            .call("session.getForeground", Some(serde_json::json!({})))
            .await?;
        let response: GetForegroundSessionResponse = serde_json::from_value(result)?;
        Ok(response.session_id)
    }

    /// Request that the TUI switch to displaying the specified session.
    ///
    /// Only meaningful when connected to a server running in TUI+server mode
    /// (`--ui-server`).
    pub async fn set_foreground_session_id(&self, session_id: &SessionId) -> Result<()> {
        self.call(
            "session.setForeground",
            Some(serde_json::json!({ "sessionId": session_id })),
        )
        .await?;
        Ok(())
    }

    /// Get the CLI server status.
    pub async fn get_status(&self) -> Result<GetStatusResponse> {
        let result = self.call("status.get", Some(serde_json::json!({}))).await?;
        Ok(serde_json::from_value(result)?)
    }

    /// Get authentication status.
    pub async fn get_auth_status(&self) -> Result<GetAuthStatusResponse> {
        let result = self
            .call("auth.getStatus", Some(serde_json::json!({})))
            .await?;
        Ok(serde_json::from_value(result)?)
    }

    /// List available models.
    ///
    /// When [`ClientOptions::on_list_models`] is set, returns the handler's
    /// result without making a `models.list` RPC. Otherwise queries the CLI.
    pub async fn list_models(&self) -> Result<Vec<Model>> {
        let cache = self.inner.models_cache.lock().clone();
        let models = cache
            .get_or_try_init(|| async {
                if let Some(handler) = &self.inner.on_list_models {
                    handler.list_models().await
                } else {
                    Ok(self.rpc().models().list().await?.models)
                }
            })
            .await?;
        Ok(models.clone())
    }

    /// Invoke [`ClientOptions::on_get_trace_context`] when configured,
    /// otherwise return [`TraceContext::default()`].
    pub(crate) async fn resolve_trace_context(&self) -> TraceContext {
        if let Some(provider) = &self.inner.on_get_trace_context {
            provider.get_trace_context().await
        } else {
            TraceContext::default()
        }
    }

    /// Return the OS process ID of the CLI child process, if one was spawned.
    pub fn pid(&self) -> Option<u32> {
        self.inner.child.lock().as_ref().and_then(|c| c.id())
    }

    /// Cooperatively shut down the client and the CLI child process.
    ///
    /// Walks every still-registered session and sends `session.destroy`
    /// for each one, asks SDK-owned runtimes to shut down, then kills the
    /// CLI child. Errors from per-session destroys, runtime shutdown, and
    /// the final child-kill are collected into
    /// [`StopErrors`] rather than short-circuiting on the first failure
    /// — so callers see the full picture of teardown.
    ///
    /// If you have already called [`Session::disconnect`] on every
    /// session this client created, the per-session destroy step is a
    /// no-op (the router map is empty); only the child-kill remains.
    ///
    /// [`Session::disconnect`]: crate::session::Session::disconnect
    ///
    /// # Cancel safety
    ///
    /// **Cancel-unsafe but recoverable.** The body sequentially destroys
    /// every registered session (each via [`Client::call`](Self::call),
    /// individually cancel-safe) before killing the child. Cancelling
    /// `stop()` mid-loop leaves some sessions still in the router map
    /// and the child still running. Recovery: call [`force_stop`](Self::force_stop)
    /// (sync, kills the child unconditionally and clears router state)
    /// or call `stop()` again with a fresh future. The documented
    /// `tokio::time::timeout(..., client.stop())` pattern in the example
    /// below uses `force_stop` as the fallback for exactly this case.
    pub async fn stop(&self) -> std::result::Result<(), StopErrors> {
        let pid = self.pid();
        info!(pid = ?pid, "stopping CLI process");
        let mut errors: Vec<Error> = Vec::new();

        // Snapshot the registered session IDs without holding the router
        // lock across the destroy RPCs.
        for session_id in self.inner.router.session_ids() {
            match self
                .call(
                    "session.destroy",
                    Some(serde_json::json!({ "sessionId": session_id })),
                )
                .await
            {
                Ok(_) => {}
                Err(e) => {
                    warn!(
                        session_id = %session_id,
                        error = %e,
                        "session.destroy failed during Client::stop",
                    );
                    errors.push(e);
                }
            }
            self.inner.router.unregister(&session_id);
        }

        let should_shutdown_runtime = self.inner.child.lock().is_some();
        if should_shutdown_runtime {
            let runtime_shutdown_start = Instant::now();
            match tokio::time::timeout(RUNTIME_SHUTDOWN_TIMEOUT, self.rpc().runtime().shutdown())
                .await
            {
                Ok(Ok(())) => {
                    debug!(
                        elapsed_ms = runtime_shutdown_start.elapsed().as_millis(),
                        "Client::stop runtime shutdown complete"
                    );
                }
                Ok(Err(e)) => {
                    warn!(
                        elapsed_ms = runtime_shutdown_start.elapsed().as_millis(),
                        error = %e,
                        "runtime.shutdown failed during Client::stop",
                    );
                    errors.push(e);
                }
                Err(_) => {
                    let e = std::io::Error::new(
                        std::io::ErrorKind::TimedOut,
                        "runtime.shutdown timed out during Client::stop",
                    );
                    warn!(
                        elapsed_ms = runtime_shutdown_start.elapsed().as_millis(),
                        timeout = ?RUNTIME_SHUTDOWN_TIMEOUT,
                        error = %e,
                        "runtime.shutdown timed out during Client::stop",
                    );
                    errors.push(e.into());
                }
            }
        }

        let child = self.inner.child.lock().take();
        *self.inner.state.lock() = ConnectionState::Disconnected;
        *self.inner.models_cache.lock() = Arc::new(tokio::sync::OnceCell::new());
        if let Some(mut child) = child {
            match child.try_wait() {
                Ok(Some(_status)) => {}
                Ok(None) => {
                    // The runtime completes all cleanup before responding to
                    // runtime.shutdown and then leaves termination to us; it
                    // deliberately keeps its JSON-RPC server alive to send the
                    // response and never self-exits. Waiting for a self-exit
                    // that will never come just wastes time, so terminate the
                    // child immediately.
                    if let Err(e) = child.kill().await {
                        errors.push(e.into());
                    }
                }
                Err(e) => errors.push(e.into()),
            }
        }

        info!(pid = ?pid, errors = errors.len(), "CLI process stopped");
        if errors.is_empty() {
            Ok(())
        } else {
            Err(StopErrors(errors))
        }
    }

    /// Forcibly stop the CLI process without waiting for it to exit.
    ///
    /// Synchronous fallback when [`stop`](Self::stop) is unsuitable — for
    /// example when the awaiting tokio runtime is shutting down or the
    /// process is wedged on I/O. Sends a kill signal without awaiting
    /// reaper completion and immediately drops all per-session router
    /// state so dependent tasks observe a closed channel rather than a
    /// hang.
    ///
    /// # Cancel safety
    ///
    /// **Synchronous and infallible by construction.** Not async; cannot
    /// be cancelled. Designed as the recovery path when [`stop`](Self::stop)
    /// is wrapped in a timeout that elapses.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(client: github_copilot_sdk::Client) {
    /// // Try graceful shutdown first; fall back to force_stop if hung.
    /// match tokio::time::timeout(
    ///     std::time::Duration::from_secs(5),
    ///     client.stop(),
    /// ).await {
    ///     Ok(_) => {}
    ///     Err(_) => client.force_stop(),
    /// }
    /// # }
    /// ```
    pub fn force_stop(&self) {
        let pid = self.pid();
        info!(pid = ?pid, "force-stopping CLI process");
        if let Some(mut child) = self.inner.child.lock().take()
            && let Err(e) = child.start_kill()
        {
            error!(pid = ?pid, error = %e, "failed to send kill signal");
        }
        self.inner.rpc.force_close();
        // Drop all session channels so any awaiters see a closed channel
        // instead of waiting for responses that will never arrive.
        self.inner.router.clear();
        *self.inner.state.lock() = ConnectionState::Disconnected;
        *self.inner.models_cache.lock() = Arc::new(tokio::sync::OnceCell::new());
    }

    /// Subscribe to lifecycle events.
    ///
    /// Returns a [`LifecycleSubscription`] that yields every
    /// [`SessionLifecycleEvent`] sent by the CLI. Drop the value to
    /// unsubscribe; there is no separate cancel handle.
    ///
    /// The returned handle implements both an inherent
    /// [`recv`](LifecycleSubscription::recv) method and [`Stream`](tokio_stream::Stream),
    /// so callers can use a `while let` loop or any combinator from
    /// `tokio_stream::StreamExt` / `futures::StreamExt`.
    ///
    /// Each subscriber maintains its own queue. If a consumer cannot keep
    /// up, the oldest events are dropped and `recv` returns
    /// [`RecvErrorKind::Lagged`](crate::subscription::RecvErrorKind::Lagged)
    /// with the count of skipped events; consumers
    /// should match on it and continue. Slow consumers do not block the
    /// producer.
    ///
    /// To filter by event type, match on `event.event_type` in the
    /// consumer task. There is no built-in typed filter — `match` is more
    /// flexible and keeps the API surface small.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(client: github_copilot_sdk::Client) {
    /// let mut events = client.subscribe_lifecycle();
    /// tokio::spawn(async move {
    ///     while let Ok(event) = events.recv().await {
    ///         println!("session {} -> {:?}", event.session_id, event.event_type);
    ///     }
    /// });
    /// # }
    /// ```
    pub fn subscribe_lifecycle(&self) -> LifecycleSubscription {
        LifecycleSubscription::new(self.inner.lifecycle_tx.subscribe())
    }
}

impl Drop for ClientInner {
    fn drop(&mut self) {
        if let Some(ref mut child) = *self.child.lock() {
            let pid = child.id();
            if let Err(e) = child.start_kill() {
                error!(pid = ?pid, error = %e, "failed to kill CLI process on drop");
            } else {
                info!(pid = ?pid, "kill signal sent for CLI process on drop");
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn is_transport_failure_matches_request_cancelled() {
        let err = Error::from(ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled));
        assert!(err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_matches_io_error() {
        let err = Error::from(std::io::Error::new(std::io::ErrorKind::BrokenPipe, "gone"));
        assert!(err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_rejects_rpc_error() {
        let err = Error::with_message(ErrorKind::Rpc { code: -1 }, "bad");
        assert!(!err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_rejects_session_error() {
        let err = Error::from(ErrorKind::Session(SessionErrorKind::NotFound("s1".into())));
        assert!(!err.is_transport_failure());
    }

    #[test]
    fn client_options_builder_composes() {
        let opts = ClientOptions::new()
            .with_program(CliProgram::Path(PathBuf::from("/usr/local/bin/copilot")))
            .with_prefix_args(["node"])
            .with_cwd(PathBuf::from("/tmp"))
            .with_env([("KEY", "value")])
            .with_env_remove(["UNWANTED"])
            .with_extra_args(["--quiet"])
            .with_github_token("ghp_test")
            .with_use_logged_in_user(false)
            .with_log_level(LogLevel::Debug)
            .with_session_idle_timeout_seconds(120)
            .with_enable_remote_sessions(true);
        assert!(matches!(opts.program, CliProgram::Path(_)));
        assert_eq!(opts.prefix_args, vec![std::ffi::OsString::from("node")]);
        assert_eq!(opts.working_directory, PathBuf::from("/tmp"));
        assert_eq!(
            opts.env,
            vec![(
                std::ffi::OsString::from("KEY"),
                std::ffi::OsString::from("value")
            )]
        );
        assert_eq!(opts.env_remove, vec![std::ffi::OsString::from("UNWANTED")]);
        assert_eq!(opts.extra_args, vec!["--quiet".to_string()]);
        assert_eq!(opts.github_token.as_deref(), Some("ghp_test"));
        assert_eq!(opts.use_logged_in_user, Some(false));
        assert!(matches!(opts.log_level, Some(LogLevel::Debug)));
        assert_eq!(opts.session_idle_timeout_seconds, Some(120));
        assert!(opts.enable_remote_sessions);
    }

    #[test]
    fn is_transport_failure_rejects_other_protocol_errors() {
        let err = Error::from(ErrorKind::Protocol(ProtocolErrorKind::CliStartupTimeout));
        assert!(!err.is_transport_failure());
    }

    #[test]
    fn build_command_lets_env_remove_strip_injected_token() {
        let opts = ClientOptions {
            github_token: Some("secret".to_string()),
            env_remove: vec![std::ffi::OsString::from("COPILOT_SDK_AUTH_TOKEN")],
            ..Default::default()
        };
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        // get_envs() iter yields the latest action per key — None means removed.
        let action = cmd
            .as_std()
            .get_envs()
            .find(|(k, _)| *k == std::ffi::OsStr::new("COPILOT_SDK_AUTH_TOKEN"))
            .map(|(_, v)| v);
        assert_eq!(
            action,
            Some(None),
            "env_remove should win over github_token"
        );
    }

    #[test]
    fn build_command_lets_env_override_injected_token() {
        let opts = ClientOptions {
            github_token: Some("from-options".to_string()),
            env: vec![(
                std::ffi::OsString::from("COPILOT_SDK_AUTH_TOKEN"),
                std::ffi::OsString::from("from-env"),
            )],
            ..Default::default()
        };
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        let value = cmd
            .as_std()
            .get_envs()
            .find(|(k, _)| *k == std::ffi::OsStr::new("COPILOT_SDK_AUTH_TOKEN"))
            .and_then(|(_, v)| v);
        assert_eq!(value, Some(std::ffi::OsStr::new("from-env")));
    }

    #[test]
    fn build_command_injects_github_token_by_default() {
        let opts = ClientOptions {
            github_token: Some("just-the-token".to_string()),
            ..Default::default()
        };
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        let value = cmd
            .as_std()
            .get_envs()
            .find(|(k, _)| *k == std::ffi::OsStr::new("COPILOT_SDK_AUTH_TOKEN"))
            .and_then(|(_, v)| v);
        assert_eq!(value, Some(std::ffi::OsStr::new("just-the-token")));
    }

    fn env_value<'a>(cmd: &'a tokio::process::Command, key: &str) -> Option<&'a std::ffi::OsStr> {
        cmd.as_std()
            .get_envs()
            .find(|(k, _)| *k == std::ffi::OsStr::new(key))
            .and_then(|(_, v)| v)
    }

    #[test]
    fn telemetry_config_builder_composes() {
        let cfg = TelemetryConfig::new()
            .with_otlp_endpoint("http://collector:4318")
            .with_otlp_protocol(OtlpHttpProtocol::HttpProtobuf)
            .with_file_path(PathBuf::from("/var/log/copilot.jsonl"))
            .with_exporter_type(OtelExporterType::OtlpHttp)
            .with_source_name("my-app")
            .with_capture_content(true);

        assert_eq!(cfg.otlp_endpoint.as_deref(), Some("http://collector:4318"));
        assert_eq!(cfg.otlp_protocol, Some(OtlpHttpProtocol::HttpProtobuf));
        assert_eq!(
            cfg.file_path.as_deref(),
            Some(Path::new("/var/log/copilot.jsonl")),
        );
        assert_eq!(cfg.exporter_type, Some(OtelExporterType::OtlpHttp));
        assert_eq!(cfg.source_name.as_deref(), Some("my-app"));
        assert_eq!(cfg.capture_content, Some(true));
        assert!(!cfg.is_empty());
        assert!(TelemetryConfig::new().is_empty());
    }

    #[test]
    fn otlp_http_protocol_serde_matches_env_value() {
        for (protocol, wire) in [
            (OtlpHttpProtocol::HttpJson, "http/json"),
            (OtlpHttpProtocol::HttpProtobuf, "http/protobuf"),
        ] {
            assert_eq!(protocol.as_str(), wire);

            let serialized = serde_json::to_string(&protocol).unwrap();
            assert_eq!(serialized, format!("\"{wire}\""));

            let deserialized: OtlpHttpProtocol = serde_json::from_str(&serialized).unwrap();
            assert_eq!(deserialized, protocol);
        }
    }

    #[test]
    fn build_command_sets_otel_env_when_telemetry_enabled() {
        let opts = ClientOptions {
            telemetry: Some(TelemetryConfig {
                otlp_endpoint: Some("http://collector:4318".to_string()),
                otlp_protocol: Some(OtlpHttpProtocol::HttpProtobuf),
                file_path: Some(PathBuf::from("/var/log/copilot.jsonl")),
                exporter_type: Some(OtelExporterType::OtlpHttp),
                source_name: Some("my-app".to_string()),
                capture_content: Some(true),
            }),
            ..Default::default()
        };
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_ENABLED"),
            Some(std::ffi::OsStr::new("true")),
        );
        assert_eq!(
            env_value(&cmd, "OTEL_EXPORTER_OTLP_ENDPOINT"),
            Some(std::ffi::OsStr::new("http://collector:4318")),
        );
        assert_eq!(
            env_value(&cmd, "OTEL_EXPORTER_OTLP_PROTOCOL"),
            Some(std::ffi::OsStr::new("http/protobuf")),
        );
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_FILE_EXPORTER_PATH"),
            Some(std::ffi::OsStr::new("/var/log/copilot.jsonl")),
        );
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_EXPORTER_TYPE"),
            Some(std::ffi::OsStr::new("otlp-http")),
        );
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_SOURCE_NAME"),
            Some(std::ffi::OsStr::new("my-app")),
        );
        assert_eq!(
            env_value(&cmd, "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"),
            Some(std::ffi::OsStr::new("true")),
        );
    }

    #[test]
    fn build_command_omits_otel_env_when_telemetry_none() {
        let opts = ClientOptions::default();
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        for key in [
            "COPILOT_OTEL_ENABLED",
            "OTEL_EXPORTER_OTLP_ENDPOINT",
            "OTEL_EXPORTER_OTLP_PROTOCOL",
            "COPILOT_OTEL_FILE_EXPORTER_PATH",
            "COPILOT_OTEL_EXPORTER_TYPE",
            "COPILOT_OTEL_SOURCE_NAME",
            "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT",
        ] {
            assert!(
                env_value(&cmd, key).is_none(),
                "expected {key} to be unset when telemetry is None",
            );
        }
    }

    #[test]
    fn build_command_omits_unset_telemetry_fields() {
        let opts = ClientOptions {
            telemetry: Some(TelemetryConfig {
                otlp_endpoint: Some("http://collector:4318".to_string()),
                ..Default::default()
            }),
            ..Default::default()
        };
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        // The one set field plus the implicit enabled flag should propagate.
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_ENABLED"),
            Some(std::ffi::OsStr::new("true")),
        );
        assert_eq!(
            env_value(&cmd, "OTEL_EXPORTER_OTLP_ENDPOINT"),
            Some(std::ffi::OsStr::new("http://collector:4318")),
        );
        // None of the other fields should leak as env vars.
        for key in [
            "OTEL_EXPORTER_OTLP_PROTOCOL",
            "COPILOT_OTEL_FILE_EXPORTER_PATH",
            "COPILOT_OTEL_EXPORTER_TYPE",
            "COPILOT_OTEL_SOURCE_NAME",
            "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT",
        ] {
            assert!(env_value(&cmd, key).is_none(), "{key} should be unset");
        }
    }

    #[test]
    fn build_command_lets_user_env_override_telemetry() {
        let opts = ClientOptions {
            telemetry: Some(TelemetryConfig {
                otlp_endpoint: Some("http://from-config:4318".to_string()),
                ..Default::default()
            }),
            env: vec![(
                std::ffi::OsString::from("OTEL_EXPORTER_OTLP_ENDPOINT"),
                std::ffi::OsString::from("http://from-user-env:4318"),
            )],
            ..Default::default()
        };
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        assert_eq!(
            env_value(&cmd, "OTEL_EXPORTER_OTLP_ENDPOINT"),
            Some(std::ffi::OsStr::new("http://from-user-env:4318")),
            "user-supplied options.env should override telemetry config",
        );
    }

    #[test]
    fn build_command_sets_copilot_home_env_when_configured() {
        let opts = ClientOptions::new().with_base_directory(PathBuf::from("/custom/copilot"));
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        assert_eq!(
            env_value(&cmd, "COPILOT_HOME"),
            Some(std::ffi::OsStr::new("/custom/copilot")),
        );

        let opts = ClientOptions::default();
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        assert!(env_value(&cmd, "COPILOT_HOME").is_none());
    }

    #[test]
    fn build_command_sets_connection_token_env_when_configured() {
        let opts = ClientOptions::new().with_transport(Transport::Tcp {
            port: 0,
            connection_token: Some("secret-token".to_string()),
        });
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        assert_eq!(
            env_value(&cmd, "COPILOT_CONNECTION_TOKEN"),
            Some(std::ffi::OsStr::new("secret-token")),
        );

        let opts = ClientOptions::default();
        let cmd = Client::build_command(Path::new("/bin/echo"), &opts);
        assert!(env_value(&cmd, "COPILOT_CONNECTION_TOKEN").is_none());
    }

    #[tokio::test]
    async fn start_rejects_empty_connection_token() {
        let opts = ClientOptions::new()
            .with_transport(Transport::Tcp {
                port: 0,
                connection_token: Some(String::new()),
            })
            .with_program(CliProgram::Path(PathBuf::from("/bin/echo")));
        let err = Client::start(opts).await.unwrap_err();
        assert!(
            matches!(err.kind(), ErrorKind::InvalidConfig),
            "got {err:?}"
        );
    }

    #[tokio::test]
    async fn start_rejects_empty_external_connection_token() {
        let opts = ClientOptions::new()
            .with_transport(Transport::External {
                host: "127.0.0.1".to_string(),
                port: 1,
                connection_token: Some(String::new()),
            })
            .with_program(CliProgram::Path(PathBuf::from("/bin/echo")));
        let err = Client::start(opts).await.unwrap_err();
        assert!(
            matches!(err.kind(), ErrorKind::InvalidConfig),
            "got {err:?}"
        );
    }

    #[test]
    fn telemetry_config_capture_content_serializes_as_lowercase_bool() {
        let opts_true = ClientOptions {
            telemetry: Some(TelemetryConfig {
                capture_content: Some(true),
                ..Default::default()
            }),
            ..Default::default()
        };
        let opts_false = ClientOptions {
            telemetry: Some(TelemetryConfig {
                capture_content: Some(false),
                ..Default::default()
            }),
            ..Default::default()
        };
        let cmd_true = Client::build_command(Path::new("/bin/echo"), &opts_true);
        let cmd_false = Client::build_command(Path::new("/bin/echo"), &opts_false);
        assert_eq!(
            env_value(
                &cmd_true,
                "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"
            ),
            Some(std::ffi::OsStr::new("true")),
        );
        assert_eq!(
            env_value(
                &cmd_false,
                "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"
            ),
            Some(std::ffi::OsStr::new("false")),
        );
    }

    #[test]
    fn session_idle_timeout_args_are_omitted_by_default() {
        let opts = ClientOptions::default();
        assert!(Client::session_idle_timeout_args(&opts).is_empty());
    }

    #[test]
    fn session_idle_timeout_args_omitted_for_zero() {
        let opts = ClientOptions {
            session_idle_timeout_seconds: Some(0),
            ..Default::default()
        };
        assert!(Client::session_idle_timeout_args(&opts).is_empty());
    }

    #[test]
    fn session_idle_timeout_args_emit_flag_for_positive_value() {
        let opts = ClientOptions {
            session_idle_timeout_seconds: Some(300),
            ..Default::default()
        };
        assert_eq!(
            Client::session_idle_timeout_args(&opts),
            vec!["--session-idle-timeout".to_string(), "300".to_string()]
        );
    }

    #[test]
    fn remote_args_omitted_by_default() {
        let opts = ClientOptions::default();
        assert!(Client::remote_args(&opts).is_empty());
    }

    #[test]
    fn remote_args_emit_flag_when_enabled() {
        let opts = ClientOptions {
            enable_remote_sessions: true,
            ..Default::default()
        };
        assert_eq!(Client::remote_args(&opts), vec!["--remote".to_string()]);
    }

    #[test]
    fn log_level_args_omitted_when_unset() {
        let opts = ClientOptions::default();
        assert!(opts.log_level.is_none());
        assert!(
            Client::log_level_args(&opts).is_empty(),
            "with no caller-supplied log_level the SDK must not pass --log-level"
        );
    }

    #[test]
    fn log_level_args_emit_flag_when_set() {
        let opts = ClientOptions::default().with_log_level(LogLevel::Debug);
        assert_eq!(Client::log_level_args(&opts), vec!["--log-level", "debug"]);
    }

    #[test]
    fn log_level_str_round_trips() {
        for level in [
            LogLevel::None,
            LogLevel::Error,
            LogLevel::Warning,
            LogLevel::Info,
            LogLevel::Debug,
            LogLevel::All,
        ] {
            let s = level.as_str();
            let json = serde_json::to_string(&level).unwrap();
            assert_eq!(json, format!("\"{s}\""));
            let parsed: LogLevel = serde_json::from_str(&json).unwrap();
            assert_eq!(parsed, level);
        }
    }

    #[test]
    fn client_options_debug_redacts_handler() {
        struct StubHandler;
        #[async_trait]
        impl ListModelsHandler for StubHandler {
            async fn list_models(&self) -> Result<Vec<Model>> {
                Ok(vec![])
            }
        }
        let opts = ClientOptions {
            on_list_models: Some(Arc::new(StubHandler)),
            github_token: Some("secret-token".into()),
            ..Default::default()
        };
        let debug = format!("{opts:?}");
        assert!(debug.contains("on_list_models: Some(\"<set>\")"));
        assert!(debug.contains("github_token: Some(\"<redacted>\")"));
        assert!(!debug.contains("secret-token"));
    }

    #[tokio::test]
    async fn list_models_uses_on_list_models_handler_when_set() {
        use std::sync::atomic::{AtomicUsize, Ordering};

        struct CountingHandler {
            calls: Arc<AtomicUsize>,
            models: Vec<Model>,
        }
        #[async_trait]
        impl ListModelsHandler for CountingHandler {
            async fn list_models(&self) -> Result<Vec<Model>> {
                self.calls.fetch_add(1, Ordering::SeqCst);
                Ok(self.models.clone())
            }
        }

        let calls = Arc::new(AtomicUsize::new(0));
        let model = Model {
            id: "byok-gpt-4".into(),
            name: "BYOK GPT-4".into(),
            ..Default::default()
        };
        let handler: Arc<dyn ListModelsHandler> = Arc::new(CountingHandler {
            calls: Arc::clone(&calls),
            models: vec![model.clone()],
        });

        let client = client_with_list_models_handler(handler);

        let result = client.list_models().await.unwrap();
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "byok-gpt-4");
        assert_eq!(calls.load(Ordering::SeqCst), 1);
    }

    #[tokio::test]
    async fn list_models_serializes_concurrent_cache_misses() {
        use std::sync::atomic::{AtomicUsize, Ordering};

        struct SlowCountingHandler {
            calls: Arc<AtomicUsize>,
            models: Vec<Model>,
        }
        #[async_trait]
        impl ListModelsHandler for SlowCountingHandler {
            async fn list_models(&self) -> Result<Vec<Model>> {
                self.calls.fetch_add(1, Ordering::SeqCst);
                tokio::time::sleep(std::time::Duration::from_millis(25)).await;
                Ok(self.models.clone())
            }
        }

        let calls = Arc::new(AtomicUsize::new(0));
        let model = Model {
            id: "single-flight-model".into(),
            name: "Single Flight Model".into(),
            ..Default::default()
        };
        let handler: Arc<dyn ListModelsHandler> = Arc::new(SlowCountingHandler {
            calls: Arc::clone(&calls),
            models: vec![model],
        });
        let client = client_with_list_models_handler(handler);

        let (first, second) = tokio::join!(client.list_models(), client.list_models());
        assert_eq!(first.unwrap()[0].id, "single-flight-model");
        assert_eq!(second.unwrap()[0].id, "single-flight-model");
        assert_eq!(calls.load(Ordering::SeqCst), 1);
    }

    #[tokio::test]
    async fn cancelled_resume_session_unregisters_pending_session() {
        let (client_write, _server_read) = tokio::io::duplex(8192);
        let (_server_write, client_read) = tokio::io::duplex(8192);
        let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
        let session_id = SessionId::new("resume-cancel-test");
        let handle = tokio::spawn({
            let client = client.clone();
            async move {
                client
                    .resume_session(ResumeSessionConfig::new(session_id))
                    .await
            }
        });

        wait_for_pending_session_registration(&client).await;
        handle.abort();
        let _ = handle.await;

        assert!(client.inner.router.session_ids().is_empty());
        client.force_stop();
    }

    fn client_with_list_models_handler(handler: Arc<dyn ListModelsHandler>) -> Client {
        Client {
            inner: Arc::new(ClientInner {
                child: parking_lot::Mutex::new(None),
                rpc: {
                    let (req_tx, _req_rx) = mpsc::unbounded_channel();
                    let (notif_tx, _notif_rx) = broadcast::channel(16);
                    let (read_pipe, _write_pipe) = tokio::io::duplex(64);
                    let (_unused_read, write_pipe) = tokio::io::duplex(64);
                    JsonRpcClient::new(write_pipe, read_pipe, notif_tx, req_tx)
                },
                cwd: PathBuf::from("."),
                request_rx: parking_lot::Mutex::new(None),
                notification_tx: broadcast::channel(16).0,
                router: router::SessionRouter::new(),
                negotiated_protocol_version: OnceLock::new(),
                state: parking_lot::Mutex::new(ConnectionState::Connected),
                lifecycle_tx: broadcast::channel(16).0,
                on_list_models: Some(handler),
                models_cache: parking_lot::Mutex::new(Arc::new(tokio::sync::OnceCell::new())),
                session_fs_configured: false,
                session_fs_sqlite_declared: false,
                llm_inference: OnceLock::new(),
                on_get_trace_context: None,
                effective_connection_token: None,
                mode: ClientMode::default(),
            }),
        }
    }

    async fn wait_for_pending_session_registration(client: &Client) {
        let deadline = tokio::time::Instant::now() + std::time::Duration::from_secs(1);
        while client.inner.router.session_ids().is_empty() {
            assert!(
                tokio::time::Instant::now() < deadline,
                "session was not registered"
            );
            tokio::time::sleep(std::time::Duration::from_millis(10)).await;
        }
    }
}
