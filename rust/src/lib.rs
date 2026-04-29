#![doc = include_str!("../README.md")]
#![warn(missing_docs)]
#![deny(rustdoc::broken_intra_doc_links)]
#![cfg_attr(test, allow(clippy::unwrap_used))]

/// Bundled CLI binary extraction and caching.
pub mod embeddedcli;
/// Event handler traits for session lifecycle.
pub mod handler;
/// Lifecycle hook callbacks (pre/post tool use, prompt submission, session start/end).
pub mod hooks;
mod jsonrpc;
/// Permission-policy helpers that wrap an existing [`handler::SessionHandler`].
pub mod permission;
/// Copilot CLI binary resolution (env var, embedded, PATH search).
pub mod resolve;
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
/// Protocol types shared between the SDK and the Copilot CLI.
pub mod types;

/// Auto-generated protocol types from Copilot JSON Schemas.
pub mod generated;

use std::ffi::OsString;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::{Arc, OnceLock};

use async_trait::async_trait;
// JSON-RPC wire types are internal transport details (like Go SDK's internal/jsonrpc2/).
// External callers interact via Client/Session methods, not raw RPC.
pub(crate) use jsonrpc::{
    JsonRpcClient, JsonRpcError, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse, error_codes,
};

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
pub use subscription::{EventSubscription, Lagged, LifecycleSubscription, RecvError};

/// Minimum protocol version this SDK can communicate with.
const MIN_PROTOCOL_VERSION: u32 = 2;

/// Errors returned by the SDK.
#[derive(Debug, thiserror::Error)]
#[non_exhaustive]
pub enum Error {
    /// JSON-RPC transport or protocol violation.
    #[error("protocol error: {0}")]
    Protocol(ProtocolError),

    /// The CLI returned a JSON-RPC error response.
    #[error("RPC error {code}: {message}")]
    Rpc {
        /// JSON-RPC error code.
        code: i32,
        /// Human-readable error message.
        message: String,
    },

    /// Session-scoped error (not found, agent error, timeout, etc.).
    #[error("session error: {0}")]
    Session(SessionError),

    /// I/O error on the stdio transport or during process spawn.
    #[error(transparent)]
    Io(#[from] std::io::Error),

    /// Failed to serialize or deserialize a JSON-RPC message.
    #[error(transparent)]
    Json(#[from] serde_json::Error),

    /// A required binary was not found on the system.
    #[error("binary not found: {name} ({hint})")]
    BinaryNotFound {
        /// Binary name that was searched for.
        name: &'static str,
        /// Guidance on how to install or configure the binary.
        hint: &'static str,
    },
}

impl Error {
    /// Returns true if this error indicates the transport is broken — the CLI
    /// process exited, the connection was lost, or an I/O failure occurred.
    /// Callers should discard the client and create a fresh one.
    pub fn is_transport_failure(&self) -> bool {
        matches!(
            self,
            Error::Protocol(ProtocolError::RequestCancelled) | Error::Io(_)
        )
    }
}

/// Aggregate of errors collected during [`Client::stop`].
///
/// `Client::stop` performs cooperative shutdown across every active
/// session before killing the CLI child process. Errors from any
/// per-session `session.destroy` RPC and from the terminal child-kill
/// step are collected here rather than short-circuiting on the first
/// failure, so callers see the full picture of what went wrong during
/// teardown.
///
/// Implements [`std::error::Error`] and forwards to `Display` for the
/// first error, with a count suffix when there are more.
#[derive(Debug)]
pub struct StopErrors(Vec<Error>);

impl StopErrors {
    /// Borrow the collected errors as a slice, in the order they
    /// occurred (per-session destroys first, then child-kill last).
    pub fn errors(&self) -> &[Error] {
        &self.0
    }

    /// Consume the aggregate and return the underlying error vector.
    pub fn into_errors(self) -> Vec<Error> {
        self.0
    }
}

impl std::fmt::Display for StopErrors {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self.0.as_slice() {
            [] => write!(f, "stop completed with no errors"),
            [only] => write!(f, "stop failed: {only}"),
            [first, rest @ ..] => write!(
                f,
                "stop failed with {n} errors; first: {first}",
                n = 1 + rest.len(),
            ),
        }
    }
}

impl std::error::Error for StopErrors {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        self.0
            .first()
            .map(|e| e as &(dyn std::error::Error + 'static))
    }
}

/// Specific protocol-level errors in the JSON-RPC transport or CLI lifecycle.
#[derive(Debug, thiserror::Error)]
#[non_exhaustive]
pub enum ProtocolError {
    /// Missing `Content-Length` header in a JSON-RPC message.
    #[error("missing Content-Length header")]
    MissingContentLength,

    /// Invalid `Content-Length` header value.
    #[error("invalid Content-Length value: \"{0}\"")]
    InvalidContentLength(String),

    /// A pending JSON-RPC request was cancelled (e.g. the response channel was dropped).
    #[error("request cancelled")]
    RequestCancelled,

    /// The CLI process did not report a listening port within the timeout.
    #[error("timed out waiting for CLI to report listening port")]
    CliStartupTimeout,

    /// The CLI process exited before reporting a listening port.
    #[error("CLI exited before reporting listening port")]
    CliStartupFailed,

    /// The CLI server's protocol version is outside the SDK's supported range.
    #[error("version mismatch: server={server}, supported={min}–{max}")]
    VersionMismatch {
        /// Version reported by the server.
        server: u32,
        /// Minimum version supported by this SDK.
        min: u32,
        /// Maximum version supported by this SDK.
        max: u32,
    },

    /// The CLI server's protocol version changed between calls.
    #[error("version changed: was {previous}, now {current}")]
    VersionChanged {
        /// Previously negotiated version.
        previous: u32,
        /// Newly reported version.
        current: u32,
    },
}

/// Session-scoped errors.
#[derive(Debug, thiserror::Error)]
#[non_exhaustive]
pub enum SessionError {
    /// The CLI could not find the requested session.
    #[error("session not found: {0}")]
    NotFound(SessionId),

    /// The CLI reported an error during agent execution (via `session.error` event).
    #[error("{0}")]
    AgentError(String),

    /// A `send_and_wait` call exceeded its timeout.
    #[error("timed out after {0:?}")]
    Timeout(std::time::Duration),

    /// `send` was called while a `send_and_wait` is in flight.
    #[error("cannot send while send_and_wait is in flight")]
    SendWhileWaiting,

    /// The session event loop exited before a pending `send_and_wait` completed.
    #[error("event loop closed before session reached idle")]
    EventLoopClosed,

    /// Elicitation is not supported by the host.
    /// Check `session.capabilities().ui.elicitation` before calling UI methods.
    #[error(
        "elicitation not supported by host — check session.capabilities().ui.elicitation first"
    )]
    ElicitationNotSupported,

    /// The client was started with [`ClientOptions::session_fs`] but this
    /// session was created without a [`SessionFsProvider`]. Set one via
    /// [`SessionConfig::with_session_fs_provider`] (or
    /// [`ResumeSessionConfig::with_session_fs_provider`]).
    #[error(
        "session was created on a client with session_fs configured but no SessionFsProvider was supplied"
    )]
    SessionFsProviderRequired,

    /// [`ClientOptions::session_fs`] was provided with empty or invalid
    /// fields. All of `initial_cwd` and `session_state_path` must be
    /// non-empty.
    #[error("invalid SessionFsConfig: {0}")]
    InvalidSessionFsConfig(String),
}

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
    },
    /// Connect to an already-running CLI server (no process spawning).
    External {
        /// Hostname or IP of the running server.
        host: String,
        /// Port of the running server.
        port: u16,
    },
}

/// How the SDK locates the Copilot CLI binary.
#[derive(Debug, Clone, Default)]
pub enum CliProgram {
    /// Auto-resolve: `COPILOT_CLI_PATH` → embedded CLI → PATH + common locations.
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

/// Options for starting a [`Client`].
///
/// When `program` is [`CliProgram::Resolve`] (the default),
/// [`Client::start`] automatically resolves the binary via
/// [`resolve::copilot_binary()`] — checking `COPILOT_CLI_PATH`, the
/// embedded CLI, and then the system PATH and common install locations.
///
/// Set `program` to [`CliProgram::Path`] to use an explicit binary.
#[non_exhaustive]
pub struct ClientOptions {
    /// How to locate the CLI binary.
    pub program: CliProgram,
    /// Arguments prepended before `--server` (e.g. the script path for node).
    pub prefix_args: Vec<OsString>,
    /// Working directory for the CLI process.
    pub cwd: PathBuf,
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
    /// the SDK uses [`LogLevel::Info`].
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
    /// from the Copilot CLI (e.g. external inference servers selected via
    /// [`Transport::External`]). Mirrors Node's `onListModels` option.
    pub on_list_models: Option<Arc<dyn ListModelsHandler>>,
    /// Custom session filesystem provider configuration.
    ///
    /// When set, the SDK calls `sessionFs.setProvider` during
    /// [`Client::start`] to register a virtualizable filesystem layer with
    /// the CLI. Each session created on this client must supply its own
    /// [`SessionFsProvider`] via
    /// [`SessionConfig::with_session_fs_provider`](crate::SessionConfig::with_session_fs_provider).
    /// Mirrors Node's `sessionFs` option. See `docs/adr/0001-session-fs-provider.md`.
    pub session_fs: Option<SessionFsConfig>,
    /// Optional [`TraceContextProvider`] used to inject W3C Trace Context
    /// headers (`traceparent` / `tracestate`) on outbound `session.create`,
    /// `session.resume`, and `session.send` requests.
    ///
    /// Mirrors Node's `onGetTraceContext` callback. When [`MessageOptions`]
    /// carries a per-turn override (set via
    /// [`MessageOptions::with_trace_context`](crate::types::MessageOptions::with_trace_context)
    /// or the underlying fields), it takes precedence over this provider.
    ///
    /// [`MessageOptions`]: crate::types::MessageOptions
    pub on_get_trace_context: Option<Arc<dyn TraceContextProvider>>,
    /// OpenTelemetry config forwarded to the spawned CLI process. See
    /// [`TelemetryConfig`] for the env-var mapping. The SDK takes no
    /// OpenTelemetry dependency — this is pure spawn-time env injection.
    pub telemetry: Option<TelemetryConfig>,
}

impl std::fmt::Debug for ClientOptions {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ClientOptions")
            .field("program", &self.program)
            .field("prefix_args", &self.prefix_args)
            .field("cwd", &self.cwd)
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
                "on_get_trace_context",
                &self.on_get_trace_context.as_ref().map(|_| "<set>"),
            )
            .field("telemetry", &self.telemetry)
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
    async fn list_models(&self) -> Result<Vec<Model>, Error>;
}

/// Log verbosity for the CLI server (passed via `--log-level`).
///
/// Mirrors Node's `CopilotClientOptions.logLevel` literal union and the
/// CLI's `--log-level` argument.
#[derive(Debug, Clone, Copy, Eq, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum LogLevel {
    /// Suppress all CLI logs.
    None,
    /// Errors only.
    Error,
    /// Warnings and errors.
    Warning,
    /// Default. Info and above.
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
/// spawned CLI process. Mirrors Node's `TelemetryConfig.exporterType`
/// literal union (`"otlp-http" | "file"`).
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

/// OpenTelemetry configuration forwarded to the spawned Copilot CLI
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
/// | [`file_path`]        | `COPILOT_OTEL_FILE_EXPORTER_PATH`                     |
/// | [`exporter_type`]    | `COPILOT_OTEL_EXPORTER_TYPE`                          |
/// | [`source_name`]      | `COPILOT_OTEL_SOURCE_NAME`                            |
/// | [`capture_content`]  | `OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT`  |
///
/// Caller-supplied entries in [`ClientOptions::env`] override these, so a
/// developer can pin any individual variable to a different value while
/// keeping the rest of the config managed by [`TelemetryConfig`].
///
/// Mirrors Node's `TelemetryConfig`, Python's `TelemetryConfig`, and Go's
/// `TelemetryConfig`. Marked `#[non_exhaustive]` so future CLI-side
/// telemetry knobs can be added without breaking callers.
///
/// [`otlp_endpoint`]: Self::otlp_endpoint
/// [`file_path`]: Self::file_path
/// [`exporter_type`]: Self::exporter_type
/// [`source_name`]: Self::source_name
/// [`capture_content`]: Self::capture_content
#[derive(Debug, Clone, Default)]
#[non_exhaustive]
pub struct TelemetryConfig {
    /// OTLP HTTP endpoint URL for trace/metric export.
    pub otlp_endpoint: Option<String>,
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
    /// Returns `true` if all fields are unset. Used by [`Client::start`]
    /// to decide whether to set `COPILOT_OTEL_ENABLED`.
    pub fn is_empty(&self) -> bool {
        self.otlp_endpoint.is_none()
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
            cwd: std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")),
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
            on_get_trace_context: None,
            telemetry: None,
        }
    }
}

/// Validate a [`SessionFsConfig`] before sending `sessionFs.setProvider`.
fn validate_session_fs_config(cfg: &SessionFsConfig) -> Result<(), Error> {
    if cfg.initial_cwd.trim().is_empty() {
        return Err(Error::Session(SessionError::InvalidSessionFsConfig(
            "initial_cwd must not be empty".to_string(),
        )));
    }
    if cfg.session_state_path.trim().is_empty() {
        return Err(Error::Session(SessionError::InvalidSessionFsConfig(
            "session_state_path must not be empty".to_string(),
        )));
    }
    Ok(())
}

/// Connection to a Copilot CLI server (stdio, TCP, or external).
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
            .field("cwd", &self.inner.cwd)
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
    server_telemetry_method: parking_lot::Mutex<Option<ServerTelemetryRpcMethod>>,
    state: parking_lot::Mutex<ConnectionState>,
    lifecycle_tx: broadcast::Sender<SessionLifecycleEvent>,
    on_list_models: Option<Arc<dyn ListModelsHandler>>,
    session_fs_configured: bool,
    on_get_trace_context: Option<Arc<dyn TraceContextProvider>>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum ServerTelemetryRpcMethod {
    SendTelemetry,
    NamespacedSendTelemetry,
}

impl ServerTelemetryRpcMethod {
    fn as_str(self) -> &'static str {
        match self {
            Self::SendTelemetry => "sendTelemetry",
            Self::NamespacedSendTelemetry => "server.sendTelemetry",
        }
    }
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
    pub async fn start(options: ClientOptions) -> Result<Self, Error> {
        if let Some(cfg) = &options.session_fs {
            validate_session_fs_config(cfg)?;
        }
        let session_fs_config = options.session_fs.clone();
        let program = match &options.program {
            CliProgram::Path(path) => {
                info!(path = %path.display(), "using explicit copilot CLI path");
                path.clone()
            }
            CliProgram::Resolve => {
                let resolved = resolve::copilot_binary()?;
                info!(path = %resolved.display(), "resolved copilot CLI");
                #[cfg(windows)]
                {
                    if let Some(ext) = resolved.extension().and_then(|e| e.to_str()) {
                        if ext.eq_ignore_ascii_case("cmd") || ext.eq_ignore_ascii_case("bat") {
                            warn!(
                                path = %resolved.display(),
                                ext = %ext,
                                "resolved copilot CLI is a .cmd/.bat wrapper; \
                                 this may cause console window flashes on Windows"
                            );
                        }
                    }
                }
                resolved
            }
        };

        let client = match options.transport {
            Transport::External { ref host, port } => {
                info!(host = %host, port = %port, "connecting to external CLI server");
                let stream = TcpStream::connect((host.as_str(), port)).await?;
                let (reader, writer) = tokio::io::split(stream);
                Self::from_transport(
                    reader,
                    writer,
                    None,
                    options.cwd,
                    options.on_list_models,
                    session_fs_config.is_some(),
                    options.on_get_trace_context,
                )?
            }
            Transport::Tcp { port } => {
                let (mut child, actual_port) = Self::spawn_tcp(&program, &options, port).await?;
                let stream = TcpStream::connect(("127.0.0.1", actual_port)).await?;
                let (reader, writer) = tokio::io::split(stream);
                Self::drain_stderr(&mut child);
                Self::from_transport(
                    reader,
                    writer,
                    Some(child),
                    options.cwd,
                    options.on_list_models,
                    session_fs_config.is_some(),
                    options.on_get_trace_context,
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
                    options.cwd,
                    options.on_list_models,
                    session_fs_config.is_some(),
                    options.on_get_trace_context,
                )?
            }
        };

        client.verify_protocol_version().await?;
        if let Some(cfg) = session_fs_config {
            let request = crate::generated::api_types::SessionFsSetProviderRequest {
                conventions: cfg.conventions.into_wire(),
                initial_cwd: cfg.initial_cwd,
                session_state_path: cfg.session_state_path,
            };
            client.rpc().session_fs().set_provider(request).await?;
        }
        Ok(client)
    }

    /// Create a Client from raw async streams (no child process).
    ///
    /// Useful for testing or connecting to a server over a custom transport.
    ///
    /// # No `actual_port` accessor
    ///
    /// Unlike Go's `Client.ActualPort`, this SDK does not expose a TCP port
    /// for the underlying transport. Go's CLI bootstrap spawns the binary,
    /// scrapes a port from its stderr, and then dials TCP. This SDK is
    /// strictly stream-based: callers either let [`Client::start`] manage a
    /// stdio child process, or hand in their own pre-connected
    /// `AsyncRead`/`AsyncWrite` pair via [`Client::from_streams`]. In either
    /// case the caller already has whatever transport-level state they
    /// need.
    pub fn from_streams(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        cwd: PathBuf,
    ) -> Result<Self, Error> {
        Self::from_transport(reader, writer, None, cwd, None, false, None)
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
    ) -> Result<Self, Error> {
        Self::from_transport(reader, writer, None, cwd, None, false, Some(provider))
    }

    fn from_transport(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        child: Option<Child>,
        cwd: PathBuf,
        on_list_models: Option<Arc<dyn ListModelsHandler>>,
        session_fs_configured: bool,
        on_get_trace_context: Option<Arc<dyn TraceContextProvider>>,
    ) -> Result<Self, Error> {
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
                server_telemetry_method: parking_lot::Mutex::new(None),
                state: parking_lot::Mutex::new(ConnectionState::Connected),
                lifecycle_tx: broadcast::channel(256).0,
                on_list_models,
                session_fs_configured,
                on_get_trace_context,
            }),
        };
        client.spawn_lifecycle_dispatcher();
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
        for (key, value) in &options.env {
            command.env(key, value);
        }
        for key in &options.env_remove {
            command.env_remove(key);
        }
        command
            .current_dir(&options.cwd)
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

    fn spawn_stdio(program: &Path, options: &ClientOptions) -> Result<Child, Error> {
        info!(cwd = ?options.cwd, program = %program.display(), "spawning copilot CLI (stdio)");
        let mut command = Self::build_command(program, options);
        let log_level = options.log_level.unwrap_or(LogLevel::Info);
        command
            .args([
                "--server",
                "--stdio",
                "--no-auto-update",
                "--log-level",
                log_level.as_str(),
            ])
            .args(Self::auth_args(options))
            .args(Self::session_idle_timeout_args(options))
            .args(&options.extra_args)
            .stdin(Stdio::piped());
        Ok(command.spawn()?)
    }

    async fn spawn_tcp(
        program: &Path,
        options: &ClientOptions,
        port: u16,
    ) -> Result<(Child, u16), Error> {
        info!(cwd = ?options.cwd, program = %program.display(), port = %port, "spawning copilot CLI (tcp)");
        let mut command = Self::build_command(program, options);
        let log_level = options.log_level.unwrap_or(LogLevel::Info);
        command
            .args([
                "--server",
                "--port",
                &port.to_string(),
                "--no-auto-update",
                "--log-level",
                log_level.as_str(),
            ])
            .args(Self::auth_args(options))
            .args(Self::session_idle_timeout_args(options))
            .args(&options.extra_args)
            .stdin(Stdio::null());
        let mut child = command.spawn()?;
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

        let actual_port = tokio::time::timeout(std::time::Duration::from_secs(10), port_rx)
            .await
            .map_err(|_| Error::Protocol(ProtocolError::CliStartupTimeout))?
            .map_err(|_| Error::Protocol(ProtocolError::CliStartupFailed))?;

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
    pub(crate) async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse, Error> {
        self.inner.rpc.send_request(method, params).await
    }

    /// Send a JSON-RPC request, check for errors, and return the result value.
    ///
    /// This is the primary method for session-level RPC calls. It wraps
    /// the internal send/receive cycle with error checking so callers
    /// don't need to inspect the response manually.
    pub async fn call(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<serde_json::Value, Error> {
        let session_id: Option<SessionId> = params
            .as_ref()
            .and_then(|p| p.get("sessionId"))
            .and_then(|v| v.as_str())
            .map(SessionId::from);
        let response = self.send_request(method, params).await?;
        if let Some(err) = response.error {
            if err.message.contains("Session not found") {
                return Err(Error::Session(SessionError::NotFound(
                    session_id.unwrap_or_else(|| "unknown".into()),
                )));
            }
            return Err(Error::Rpc {
                code: err.code,
                message: err.message,
            });
        }
        Ok(response.result.unwrap_or(serde_json::Value::Null))
    }

    /// Send a JSON-RPC response back to the CLI (e.g. for permission or tool call requests).
    pub(crate) async fn send_response(&self, response: &JsonRpcResponse) -> Result<(), Error> {
        self.inner.rpc.write(response).await
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
        self.inner
            .router
            .ensure_started(&self.inner.notification_tx, &self.inner.request_rx);
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
    /// Sends a `ping` RPC and checks the `protocolVersion` field in the
    /// response. Returns an error if the version is outside
    /// `MIN_PROTOCOL_VERSION`..=[`SDK_PROTOCOL_VERSION`]. If the server
    /// doesn't report a version, logs a warning and succeeds (backward
    /// compatibility with older CLI versions).
    pub async fn verify_protocol_version(&self) -> Result<(), Error> {
        let response = self.ping(None).await?;
        let server_version = response.protocol_version;

        match server_version {
            None => {
                warn!("CLI server did not report protocolVersion; skipping version check");
            }
            Some(v) if !(MIN_PROTOCOL_VERSION..=SDK_PROTOCOL_VERSION).contains(&v) => {
                return Err(Error::Protocol(ProtocolError::VersionMismatch {
                    server: v,
                    min: MIN_PROTOCOL_VERSION,
                    max: SDK_PROTOCOL_VERSION,
                }));
            }
            Some(v) => {
                if let Some(&existing) = self.inner.negotiated_protocol_version.get() {
                    if existing != v {
                        return Err(Error::Protocol(ProtocolError::VersionChanged {
                            previous: existing,
                            current: v,
                        }));
                    }
                } else {
                    let _ = self.inner.negotiated_protocol_version.set(v);
                }
            }
        }

        Ok(())
    }

    /// Send a `ping` RPC and return the typed [`PingResponse`].
    ///
    /// Pass `Some(message)` to have the server echo it back; pass `None` for
    /// a bare health check. The response includes a `protocolVersion` when
    /// the CLI reports one.
    ///
    /// [`PingResponse`]: crate::types::PingResponse
    pub async fn ping(&self, message: Option<&str>) -> Result<crate::types::PingResponse, Error> {
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
    ///
    /// Mirrors Node's `Client.listSessions` and Go's `Client.ListSessions`.
    pub async fn list_sessions(
        &self,
        filter: Option<SessionListFilter>,
    ) -> Result<Vec<SessionMetadata>, Error> {
        let params = match filter {
            Some(f) => serde_json::to_value(f)?,
            None => serde_json::json!({}),
        };
        let result = self.call("session.list", Some(params)).await?;
        let response: ListSessionsResponse = serde_json::from_value(result)?;
        Ok(response.sessions)
    }

    /// Fetch metadata for a specific persisted session by ID.
    ///
    /// Returns `Ok(None)` if no session with the given ID exists. This
    /// mirrors Go's `Client.GetSessionMetadata` and is more efficient than
    /// calling [`list_sessions`](Self::list_sessions) and filtering when
    /// you only need data for a single session.
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
    ) -> Result<Option<SessionMetadata>, Error> {
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
    pub async fn delete_session(&self, session_id: &SessionId) -> Result<(), Error> {
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
    /// Mirrors Go's `Client.GetLastSessionID`.
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
    pub async fn get_last_session_id(&self) -> Result<Option<SessionId>, Error> {
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
    ///
    /// Mirrors Go's `Client.GetForegroundSessionID`.
    pub async fn get_foreground_session_id(&self) -> Result<Option<SessionId>, Error> {
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
    ///
    /// Mirrors Go's `Client.SetForegroundSessionID`.
    pub async fn set_foreground_session_id(&self, session_id: &SessionId) -> Result<(), Error> {
        self.call(
            "session.setForeground",
            Some(serde_json::json!({ "sessionId": session_id })),
        )
        .await?;
        Ok(())
    }

    /// Get the CLI server status.
    pub async fn get_status(&self) -> Result<GetStatusResponse, Error> {
        let result = self.call("getStatus", Some(serde_json::json!({}))).await?;
        Ok(serde_json::from_value(result)?)
    }

    /// Get authentication status.
    pub async fn get_auth_status(&self) -> Result<GetAuthStatusResponse, Error> {
        let result = self
            .call("getAuthStatus", Some(serde_json::json!({})))
            .await?;
        Ok(serde_json::from_value(result)?)
    }

    /// List available models.
    ///
    /// When [`ClientOptions::on_list_models`] is set, returns the handler's
    /// result without making a `models.list` RPC. Otherwise queries the CLI.
    pub async fn list_models(&self) -> Result<Vec<Model>, Error> {
        if let Some(handler) = &self.inner.on_list_models {
            return handler.list_models().await;
        }
        Ok(self.rpc().models().list().await?.models)
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

    /// Send a top-level telemetry event via `sendTelemetry`.
    pub async fn send_telemetry(&self, event: ServerTelemetryEvent) -> Result<(), Error> {
        let params = serde_json::to_value(event)?;
        let cached_method = { *self.inner.server_telemetry_method.lock() };
        if let Some(method) = cached_method {
            match self.call(method.as_str(), Some(params.clone())).await {
                Ok(_) => return Ok(()),
                Err(Error::Rpc { code, .. })
                    if code == error_codes::METHOD_NOT_FOUND
                        && method == ServerTelemetryRpcMethod::SendTelemetry =>
                {
                    self.call(
                        ServerTelemetryRpcMethod::NamespacedSendTelemetry.as_str(),
                        Some(params),
                    )
                    .await?;
                    *self.inner.server_telemetry_method.lock() =
                        Some(ServerTelemetryRpcMethod::NamespacedSendTelemetry);
                    return Ok(());
                }
                Err(error) => return Err(error),
            }
        }

        match self
            .call(
                ServerTelemetryRpcMethod::SendTelemetry.as_str(),
                Some(params.clone()),
            )
            .await
        {
            Ok(_) => {
                *self.inner.server_telemetry_method.lock() =
                    Some(ServerTelemetryRpcMethod::SendTelemetry);
                Ok(())
            }
            Err(Error::Rpc { code, .. }) if code == error_codes::METHOD_NOT_FOUND => {
                self.call(
                    ServerTelemetryRpcMethod::NamespacedSendTelemetry.as_str(),
                    Some(params),
                )
                .await?;
                *self.inner.server_telemetry_method.lock() =
                    Some(ServerTelemetryRpcMethod::NamespacedSendTelemetry);
                Ok(())
            }
            Err(error) => Err(error),
        }
    }

    /// Fetch account-level quota snapshots (request-based usage).
    ///
    /// **Rust-only as of 0.1.0.** This method has no equivalent in the Node,
    /// Python, Go, or .NET SDKs at the time of this release; the underlying
    /// `account.getQuota` JSON-RPC endpoint is exposed only by the Rust SDK.
    /// See the "Rust-only API" section of the crate README for the full list
    /// of surface that diverges from the other SDKs.
    pub async fn get_quota(&self) -> Result<generated::api_types::AccountGetQuotaResult, Error> {
        self.rpc().account().get_quota().await
    }

    /// Return the OS process ID of the CLI child process, if one was spawned.
    pub fn pid(&self) -> Option<u32> {
        self.inner.child.lock().as_ref().and_then(|c| c.id())
    }

    /// Cooperatively shut down the client and the CLI child process.
    ///
    /// Walks every still-registered session and sends `session.destroy`
    /// for each one, then kills the CLI child. Errors from per-session
    /// destroys and the final child-kill are collected into
    /// [`StopErrors`] rather than short-circuiting on the first failure
    /// — so callers see the full picture of teardown.
    ///
    /// If you have already called [`Session::disconnect`] on every
    /// session this client created, the per-session destroy step is a
    /// no-op (the router map is empty); only the child-kill remains.
    ///
    /// [`Session::disconnect`]: crate::session::Session::disconnect
    pub async fn stop(&self) -> Result<(), StopErrors> {
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

        let child = self.inner.child.lock().take();
        *self.inner.state.lock() = ConnectionState::Disconnected;
        if let Some(mut child) = child
            && let Err(e) = child.kill().await
        {
            errors.push(Error::Io(e));
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
    /// Mirrors Go's `Client.ForceStop` (`go/client.go:453`).
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
        // Drop all session channels so any awaiters see a closed channel
        // instead of waiting for responses that will never arrive.
        self.inner.router.clear();
        *self.inner.state.lock() = ConnectionState::Disconnected;
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
    /// [`RecvError::Lagged`] with the count of skipped events; consumers
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

    /// Return the current [`ConnectionState`].
    ///
    /// Mirrors Go's `Client.State` (`go/client.go:1191`). The state advances
    /// to [`Connected`](ConnectionState::Connected) once
    /// [`Client::start`] / [`Client::from_streams`] returns successfully and
    /// drops to [`Disconnected`](ConnectionState::Disconnected) after
    /// [`stop`](Self::stop) or [`force_stop`](Self::force_stop).
    pub fn state(&self) -> ConnectionState {
        *self.inner.state.lock()
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
        let err = Error::Protocol(ProtocolError::RequestCancelled);
        assert!(err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_matches_io_error() {
        let err = Error::Io(std::io::Error::new(std::io::ErrorKind::BrokenPipe, "gone"));
        assert!(err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_rejects_rpc_error() {
        let err = Error::Rpc {
            code: -1,
            message: "bad".into(),
        };
        assert!(!err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_rejects_session_error() {
        let err = Error::Session(SessionError::NotFound("s1".into()));
        assert!(!err.is_transport_failure());
    }

    #[test]
    fn is_transport_failure_rejects_other_protocol_errors() {
        let err = Error::Protocol(ProtocolError::CliStartupTimeout);
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
    fn build_command_sets_otel_env_when_telemetry_enabled() {
        let opts = ClientOptions {
            telemetry: Some(TelemetryConfig {
                otlp_endpoint: Some("http://collector:4318".to_string()),
                file_path: Some(PathBuf::from("/var/log/copilot.jsonl")),
                exporter_type: Some(OtelExporterType::OtlpHttp),
                source_name: Some("github-app".to_string()),
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
            env_value(&cmd, "COPILOT_OTEL_FILE_EXPORTER_PATH"),
            Some(std::ffi::OsStr::new("/var/log/copilot.jsonl")),
        );
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_EXPORTER_TYPE"),
            Some(std::ffi::OsStr::new("otlp-http")),
        );
        assert_eq!(
            env_value(&cmd, "COPILOT_OTEL_SOURCE_NAME"),
            Some(std::ffi::OsStr::new("github-app")),
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
            async fn list_models(&self) -> Result<Vec<Model>, Error> {
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
            async fn list_models(&self) -> Result<Vec<Model>, Error> {
                self.calls.fetch_add(1, Ordering::SeqCst);
                Ok(self.models.clone())
            }
        }

        let calls = Arc::new(AtomicUsize::new(0));
        let model = Model {
            billing: None,
            capabilities: ModelCapabilities {
                limits: None,
                supports: None,
            },
            default_reasoning_effort: None,
            id: "byok-gpt-4".into(),
            name: "BYOK GPT-4".into(),
            policy: None,
            supported_reasoning_efforts: Vec::new(),
        };
        let handler = Arc::new(CountingHandler {
            calls: Arc::clone(&calls),
            models: vec![model.clone()],
        });

        // We can't call list_models() through Client::start without a CLI, but we
        // can exercise the override path by directly constructing a Client whose
        // inner has the handler set. This is the same dispatch path as the real
        // call; from_streams's None default is replaced via inner construction.
        let inner = ClientInner {
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
            server_telemetry_method: parking_lot::Mutex::new(None),
            state: parking_lot::Mutex::new(ConnectionState::Connected),
            lifecycle_tx: broadcast::channel(16).0,
            on_list_models: Some(handler),
            session_fs_configured: false,
            on_get_trace_context: None,
        };
        let client = Client {
            inner: Arc::new(inner),
        };

        let result = client.list_models().await.unwrap();
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "byok-gpt-4");
        assert_eq!(calls.load(Ordering::SeqCst), 1);
    }
}
