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
/// Typed tool definition framework and dispatch router.
pub mod tool;
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
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncWrite, BufReader};
use tokio::net::TcpStream;
use tokio::process::{Child, Command};
use tokio::sync::{broadcast, mpsc, oneshot};
use tracing::{Instrument, debug, error, info, warn};
pub use types::*;

mod sdk_protocol_version;
pub use sdk_protocol_version::{SDK_PROTOCOL_VERSION, get_sdk_protocol_version};

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

    /// `send_message` was called while a `send_and_wait` is in flight.
    #[error("cannot send_message while send_and_wait is in flight")]
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
#[derive(Debug)]
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
        }
    }
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
    pub async fn start(options: ClientOptions) -> Result<Self, Error> {
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
                Self::from_transport(reader, writer, None, options.cwd)?
            }
            Transport::Tcp { port } => {
                let (mut child, actual_port) = Self::spawn_tcp(&program, &options, port).await?;
                let stream = TcpStream::connect(("127.0.0.1", actual_port)).await?;
                let (reader, writer) = tokio::io::split(stream);
                Self::drain_stderr(&mut child);
                Self::from_transport(reader, writer, Some(child), options.cwd)?
            }
            Transport::Stdio => {
                let mut child = Self::spawn_stdio(&program, &options)?;
                let stdin = child.stdin.take().expect("stdin is piped");
                let stdout = child.stdout.take().expect("stdout is piped");
                Self::drain_stderr(&mut child);
                Self::from_transport(stdout, stdin, Some(child), options.cwd)?
            }
        };

        client.verify_protocol_version().await?;
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
        Self::from_transport(reader, writer, None, cwd)
    }

    fn from_transport(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        child: Option<Child>,
        cwd: PathBuf,
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

    fn spawn_stdio(program: &Path, options: &ClientOptions) -> Result<Child, Error> {
        info!(cwd = ?options.cwd, program = %program.display(), "spawning copilot CLI (stdio)");
        let mut command = Self::build_command(program, options);
        command
            .args([
                "--server",
                "--stdio",
                "--no-auto-update",
                "--log-level",
                "info",
            ])
            .args(Self::auth_args(options))
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
        command
            .args([
                "--server",
                "--port",
                &port.to_string(),
                "--no-auto-update",
                "--log-level",
                "info",
            ])
            .args(Self::auth_args(options))
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
        let response = self.ping("").await?;
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
    /// The `message` is echoed back by the server. Mirrors Go's
    /// `Client.Ping(ctx, message)`.
    ///
    /// [`PingResponse`]: crate::types::PingResponse
    pub async fn ping(&self, message: &str) -> Result<crate::types::PingResponse, Error> {
        let value = self
            .call("ping", Some(serde_json::json!({ "message": message })))
            .await?;
        Ok(serde_json::from_value(value)?)
    }

    /// List persisted sessions.
    pub async fn list_sessions(
        &self,
        filter: Option<serde_json::Value>,
    ) -> Result<Vec<SessionMetadata>, Error> {
        let params = filter.unwrap_or(serde_json::json!({}));
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
    /// # async fn example(client: &copilot::Client) -> Result<(), copilot::Error> {
    /// use copilot::types::SessionId;
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
    /// # async fn example(client: &copilot::Client) -> Result<(), copilot::Error> {
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
    pub async fn get_status(&self) -> Result<serde_json::Value, Error> {
        self.call("getStatus", Some(serde_json::json!({}))).await
    }

    /// Get authentication status.
    pub async fn get_auth_status(&self) -> Result<serde_json::Value, Error> {
        self.call("getAuthStatus", Some(serde_json::json!({})))
            .await
    }

    /// List available models.
    pub async fn list_models(&self) -> Result<Vec<Model>, Error> {
        let result = self
            .call("models.list", Some(serde_json::json!({})))
            .await?;
        let response: ModelList = serde_json::from_value(result)?;
        Ok(response.models)
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
    pub async fn get_quota(&self) -> Result<generated::api_types::AccountGetQuotaResult, Error> {
        let result = self
            .call(
                generated::api_types::rpc_methods::ACCOUNT_GETQUOTA,
                Some(serde_json::json!({})),
            )
            .await?;
        Ok(serde_json::from_value(result)?)
    }

    /// Return the OS process ID of the CLI child process, if one was spawned.
    pub fn pid(&self) -> Option<u32> {
        self.inner.child.lock().as_ref().and_then(|c| c.id())
    }

    /// Stop the CLI process.
    pub async fn stop(&self) -> Result<(), Error> {
        let pid = self.pid();
        info!(pid = ?pid, "stopping CLI process");
        let Some(mut child) = self.inner.child.lock().take() else {
            *self.inner.state.lock() = ConnectionState::Disconnected;
            return Ok(());
        };
        child.kill().await?;
        *self.inner.state.lock() = ConnectionState::Disconnected;
        info!(pid = ?pid, "CLI process stopped");
        Ok(())
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
    /// # async fn example(client: copilot::Client) {
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

    /// Subscribe to session lifecycle events.
    ///
    /// Returns a [`tokio::sync::broadcast::Receiver`] that
    /// yields every [`SessionLifecycleEvent`] sent by the CLI. Drop the
    /// receiver to unsubscribe.
    ///
    /// Each receiver maintains its own queue. If a consumer cannot keep up,
    /// the oldest events are dropped and `recv` returns
    /// [`RecvError::Lagged`](tokio::sync::broadcast::error::RecvError::Lagged)
    /// with the count of skipped events; consumers should match on it and
    /// continue. Slow consumers do not block the producer.
    ///
    /// To filter by event type, match on `event.event_type` in the consumer
    /// task. There is no built-in typed filter — `match` is more flexible and
    /// keeps the API surface small.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(client: copilot::Client) {
    /// let mut events = client.subscribe_lifecycle();
    /// tokio::spawn(async move {
    ///     while let Ok(event) = events.recv().await {
    ///         println!("session {} -> {:?}", event.session_id, event.event_type);
    ///     }
    /// });
    /// # }
    /// ```
    pub fn subscribe_lifecycle(&self) -> broadcast::Receiver<SessionLifecycleEvent> {
        self.inner.lifecycle_tx.subscribe()
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
}
