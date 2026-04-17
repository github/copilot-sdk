#![cfg_attr(test, allow(clippy::unwrap_used))]

pub mod embeddedcli;
pub mod generated;
pub mod handler;
pub mod hooks;
mod jsonrpc;
pub mod resolve;
mod router;
pub mod session;
pub mod tool;
pub mod transforms;
pub mod types;

#[doc(hidden)]
pub mod duration_serde;

use std::ffi::OsString;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::{Arc, OnceLock};

pub use jsonrpc::{
    error_codes, JsonRpcClient, JsonRpcError, JsonRpcMessage, JsonRpcNotification, JsonRpcRequest,
    JsonRpcResponse,
};
use tokio::io::{AsyncBufReadExt, AsyncRead, AsyncWrite, BufReader};
use tokio::net::TcpStream;
use tokio::process::{Child, Command};
use tokio::sync::{broadcast, mpsc, oneshot, Mutex as AsyncMutex};
use tracing::{debug, error, info, warn, Instrument};
pub use types::{
    ensure_attachment_display_names, Attachment, AttachmentLineRange, AttachmentSelectionPosition,
    AttachmentSelectionRange, AzureProviderOptions, CommandDefinition, CreateSessionResult,
    CustomAgentConfig, ElicitationMode, ElicitationRequest, ElicitationResult, GetMessagesResponse,
    GitHubReferenceType, InfiniteSessionConfig, InputFormat, InputOptions, ListModelsHandler,
    ListSessionsResponse, LogLevel, LogOptions, MessageOptions, ModelBilling, ModelCapabilities,
    ModelCapabilitiesOverride, ModelCapabilitiesOverrideLimits,
    ModelCapabilitiesOverrideLimitsVision, ModelCapabilitiesOverrideSupports, ModelInfo,
    ModelLimits, ModelPolicy, ModelSupports, ModelVisionLimits, ModelsListResponse, ProviderConfig,
    RequestId, ResumeSessionConfig, SectionOverride, SendAndWaitResult, SessionCapabilities,
    SessionConfig, SessionEvent, SessionEventData, SessionEventNotification, SessionEventType,
    SessionFsAppendFileRequest, SessionFsConfig, SessionFsConventions, SessionFsExistsRequest,
    SessionFsExistsResult, SessionFsHandler, SessionFsMkdirRequest, SessionFsReadFileRequest,
    SessionFsReadFileResult, SessionFsReaddirEntry, SessionFsReaddirEntryType,
    SessionFsReaddirRequest, SessionFsReaddirResult, SessionFsReaddirWithTypesRequest,
    SessionFsReaddirWithTypesResult, SessionFsRenameRequest, SessionFsRmRequest,
    SessionFsStatRequest, SessionFsStatResult, SessionFsWriteFileRequest, SessionId,
    SessionMetadata, SetModelOptions, SystemMessageConfig, Tool, ToolInvocation, ToolResult,
    ToolResultExpanded, ToolResultResponse, UiCapabilities,
};

/// Protocol version this SDK speaks. Must match the version expected by
/// the copilot-agent-runtime server.
pub const SDK_PROTOCOL_VERSION: u32 = generated::SDK_PROTOCOL_VERSION;

/// Minimum protocol version this SDK can communicate with.
const MIN_PROTOCOL_VERSION: u32 = 2;

/// Errors returned by the SDK.
#[non_exhaustive]
#[derive(Debug, thiserror::Error)]
pub enum Error {
    /// JSON-RPC transport or protocol violation.
    #[error("protocol error: {0}")]
    Protocol(ProtocolError),

    /// The CLI returned a JSON-RPC error response.
    #[error("RPC error {code}: {message}")]
    Rpc { code: i32, message: String },

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
        name: &'static str,
        hint: &'static str,
    },

    /// Caller-provided SDK configuration is invalid.
    #[error("invalid configuration: {0}")]
    InvalidConfig(String),
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
#[non_exhaustive]
#[derive(Debug, thiserror::Error)]
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
    VersionMismatch { server: u32, min: u32, max: u32 },

    /// The CLI server's protocol version changed between calls.
    #[error("version changed: was {previous}, now {current}")]
    VersionChanged { previous: u32, current: u32 },
}

/// Session-scoped errors.
#[non_exhaustive]
#[derive(Debug, thiserror::Error)]
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
#[non_exhaustive]
#[derive(Debug, Default)]
pub enum Transport {
    /// Communicate over stdin/stdout pipes (default).
    #[default]
    Stdio,
    /// Spawn the CLI with `--port` and connect via TCP.
    Tcp { port: u16 },
    /// Connect to an already-running CLI server (no process spawning).
    External { host: String, port: u16 },
}

/// How the SDK locates the Copilot CLI binary.
#[non_exhaustive]
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
    /// Optional custom handler for listing models in BYOK mode.
    pub on_list_models: Option<Arc<dyn ListModelsHandler>>,
    /// Optional custom session filesystem provider configuration.
    pub session_fs: Option<SessionFsConfig>,
    /// Transport mode used to communicate with the CLI server.
    pub transport: Transport,
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
            on_list_models: None,
            session_fs: None,
            transport: Transport::default(),
        }
    }
}

/// Connection to a Copilot CLI server (stdio, TCP, or external).
///
/// Cheaply cloneable — cloning shares the underlying connection.
/// The child process (if any) is killed when the last clone drops.
#[derive(Clone)]
#[must_use]
pub struct Client {
    inner: Arc<ClientInner>,
}

struct ClientInner {
    child: parking_lot::Mutex<Option<Child>>,
    rpc: JsonRpcClient,
    cwd: PathBuf,
    request_rx: parking_lot::Mutex<Option<mpsc::UnboundedReceiver<JsonRpcRequest>>>,
    notification_tx: broadcast::Sender<JsonRpcNotification>,
    router: router::SessionRouter,
    negotiated_protocol_version: OnceLock<u32>,
    models_cache: parking_lot::Mutex<Option<Vec<ModelInfo>>>,
    models_cache_lock: AsyncMutex<()>,
    on_list_models: Option<Arc<dyn ListModelsHandler>>,
    session_fs: Option<SessionFsConfig>,
}

fn validate_session_fs_config(config: &SessionFsConfig) -> Result<(), Error> {
    if config.initial_cwd.as_os_str().is_empty() {
        return Err(Error::InvalidConfig(
            "session_fs.initial_cwd must not be empty".into(),
        ));
    }
    if config.session_state_path.as_os_str().is_empty() {
        return Err(Error::InvalidConfig(
            "session_fs.session_state_path must not be empty".into(),
        ));
    }
    if matches!(config.conventions, SessionFsConventions::Unknown) {
        return Err(Error::InvalidConfig(
            "session_fs.conventions must be either SessionFsConventions::Windows or SessionFsConventions::Posix".into(),
        ));
    }
    Ok(())
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
        if let Some(session_fs) = options.session_fs.as_ref() {
            validate_session_fs_config(session_fs)?;
        }

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

        let on_list_models = options.on_list_models.clone();
        let session_fs = options.session_fs.clone();
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
                    on_list_models,
                    session_fs,
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
                    on_list_models,
                    session_fs,
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
                    on_list_models,
                    session_fs,
                )?
            }
        };

        client.verify_protocol_version().await?;
        client.configure_connection_features().await?;
        Ok(client)
    }

    /// Create a Client from raw async streams (no child process).
    ///
    /// Useful for testing or connecting to a server over a custom transport.
    pub fn from_streams(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        cwd: PathBuf,
    ) -> Result<Self, Error> {
        Self::from_transport(reader, writer, None, cwd, None, None)
    }

    fn from_transport(
        reader: impl AsyncRead + Unpin + Send + 'static,
        writer: impl AsyncWrite + Unpin + Send + 'static,
        child: Option<Child>,
        cwd: PathBuf,
        on_list_models: Option<Arc<dyn ListModelsHandler>>,
        session_fs: Option<SessionFsConfig>,
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

        Ok(Self {
            inner: Arc::new(ClientInner {
                child: parking_lot::Mutex::new(child),
                rpc,
                cwd,
                request_rx: parking_lot::Mutex::new(Some(request_rx)),
                notification_tx: notification_broadcast_tx,
                router: router::SessionRouter::new(),
                negotiated_protocol_version: OnceLock::new(),
                models_cache: parking_lot::Mutex::new(None),
                models_cache_lock: AsyncMutex::new(()),
                on_list_models,
                session_fs,
            }),
        })
    }

    async fn configure_connection_features(&self) -> Result<(), Error> {
        if let Some(session_fs) = self.inner.session_fs.as_ref() {
            self.call(
                "sessionFs.setProvider",
                Some(serde_json::to_value(session_fs)?),
            )
            .await?;
        }
        Ok(())
    }

    fn build_command(program: &Path, options: &ClientOptions) -> Command {
        let mut command = Command::new(program);
        for arg in &options.prefix_args {
            command.arg(arg);
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
            .args(&options.extra_args)
            .stdin(Stdio::null());
        let mut child = command.spawn()?;
        let stdout = child.stdout.take().expect("stdout is piped");

        let (port_tx, port_rx) = oneshot::channel::<u16>();
        let span = tracing::error_span!("copilot_cli_port_scan");
        tokio::spawn(
            async move {
                static PORT_RE: OnceLock<regex::Regex> = OnceLock::new();
                let port_re =
                    PORT_RE.get_or_init(|| regex::Regex::new(r"listening on port (\d+)").unwrap());
                let mut lines = BufReader::new(stdout).lines();
                let mut port_tx = Some(port_tx);
                while let Ok(Some(line)) = lines.next_line().await {
                    debug!(line = %line, "CLI stdout");
                    if let Some(tx) = port_tx.take() {
                        if let Some(caps) = port_re.captures(&line) {
                            if let Some(p) =
                                caps.get(1).and_then(|m| m.as_str().parse::<u16>().ok())
                            {
                                let _ = tx.send(p);
                                continue;
                            }
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
    pub fn cwd(&self) -> &Path {
        &self.inner.cwd
    }

    /// Send a JSON-RPC request and wait for the response.
    pub async fn send_request(
        &self,
        method: &str,
        params: Option<serde_json::Value>,
    ) -> Result<JsonRpcResponse, Error> {
        self.inner.rpc.send_request(method, params).await
    }

    /// Send a JSON-RPC request, check for errors, and return the result value.
    ///
    /// This is the primary method for session-level RPC calls. It wraps
    /// [`send_request`](Self::send_request) with error checking so callers
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
    pub async fn send_response(&self, response: &JsonRpcResponse) -> Result<(), Error> {
        self.inner.rpc.write(response).await
    }

    /// Take the receiver for incoming JSON-RPC requests from the CLI.
    ///
    /// Can only be called once — subsequent calls return `None`.
    pub fn take_request_rx(&self) -> Option<mpsc::UnboundedReceiver<JsonRpcRequest>> {
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
    /// [`MIN_PROTOCOL_VERSION`]..=[`SDK_PROTOCOL_VERSION`]. If the server
    /// doesn't report a version, logs a warning and succeeds (backward
    /// compatibility with older CLI versions).
    pub async fn verify_protocol_version(&self) -> Result<(), Error> {
        let result = self.ping().await?;
        let server_version = result
            .get("protocolVersion")
            .and_then(|v| v.as_u64())
            .and_then(|v| u32::try_from(v).ok());

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

    /// Send a `ping` RPC and return the result payload.
    pub async fn ping(&self) -> Result<serde_json::Value, Error> {
        self.call("ping", Some(serde_json::json!({}))).await
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

    /// Delete a persisted session by ID.
    pub async fn delete_session(&self, session_id: &str) -> Result<(), Error> {
        self.call(
            "session.delete",
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
    pub async fn list_models(&self) -> Result<Vec<ModelInfo>, Error> {
        if let Some(models) = self.inner.models_cache.lock().clone() {
            return Ok(models);
        }

        let _cache_guard = self.inner.models_cache_lock.lock().await;
        if let Some(models) = self.inner.models_cache.lock().clone() {
            return Ok(models);
        }

        let models = if let Some(handler) = self.inner.on_list_models.as_ref() {
            handler.list_models().await?
        } else {
            let result = self
                .call("models.list", Some(serde_json::json!({})))
                .await?;
            let response: ModelsListResponse = serde_json::from_value(result)?;
            response.models
        };

        *self.inner.models_cache.lock() = Some(models.clone());
        Ok(models)
    }

    /// Get the ID of the most recently active session, if any.
    pub async fn get_last_session_id(&self) -> Result<Option<String>, Error> {
        let result = self
            .call("session.getLastId", Some(serde_json::json!({})))
            .await?;
        Ok(result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string()))
    }

    /// Get the ID of the current foreground session, if any.
    pub async fn get_foreground_session_id(&self) -> Result<Option<String>, Error> {
        let result = self
            .call("session.getForeground", Some(serde_json::json!({})))
            .await?;
        Ok(result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string()))
    }

    /// Set the foreground session.
    pub async fn set_foreground_session_id(&self, session_id: &str) -> Result<(), Error> {
        let result = self
            .call(
                "session.setForeground",
                Some(serde_json::json!({ "sessionId": session_id })),
            )
            .await?;
        // Treat a missing `success` field as success: only fail when the CLI
        // explicitly reports `success: false`.
        if result.get("success").and_then(|v| v.as_bool()) == Some(false) {
            let error_msg = result
                .get("error")
                .and_then(|v| v.as_str())
                .unwrap_or("unknown error")
                .to_string();
            return Err(Error::Rpc {
                code: -1,
                message: format!("setForeground failed: {error_msg}"),
            });
        }
        Ok(())
    }

    /// Get metadata for a specific session.
    pub async fn get_session_metadata(
        &self,
        session_id: &str,
    ) -> Result<Option<SessionMetadata>, Error> {
        let result = self
            .call(
                "session.getMetadata",
                Some(serde_json::json!({ "sessionId": session_id })),
            )
            .await?;
        if result.is_null() || result.as_object().is_some_and(|o| o.is_empty()) {
            return Ok(None);
        }
        let metadata: SessionMetadata = serde_json::from_value(result)?;
        Ok(Some(metadata))
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
            return Ok(());
        };
        child.kill().await?;
        info!(pid = ?pid, "CLI process stopped");
        Ok(())
    }

    /// Forcibly stop the CLI process without attempting graceful shutdown.
    ///
    /// Sends a kill signal to the child process and drops it. For external
    /// connections (no child process), this is a no-op.
    pub fn force_stop(&self) {
        let Some(mut child) = self.inner.child.lock().take() else {
            return;
        };
        let pid = child.id();
        if let Err(e) = child.start_kill() {
            error!(pid = ?pid, error = %e, "force_stop: failed to kill CLI process");
        } else {
            info!(pid = ?pid, "force_stop: kill signal sent for CLI process");
        }
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
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::sync::Arc;

    use tokio::io::{duplex, AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};

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

    async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
        let header = format!("Content-Length: {}\r\n\r\n", body.len());
        writer.write_all(header.as_bytes()).await.unwrap();
        writer.write_all(body).await.unwrap();
        writer.flush().await.unwrap();
    }

    async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> serde_json::Value {
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            reader.read_exact(&mut byte).await.unwrap();
            header.push(byte[0] as char);
            if header.ends_with("\r\n\r\n") {
                break;
            }
        }

        let length: usize = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut buf = vec![0u8; length];
        reader.read_exact(&mut buf).await.unwrap();
        serde_json::from_slice(&buf).unwrap()
    }

    #[tokio::test]
    async fn list_models_uses_handler_and_cache() {
        let (client_write, _server_read) = duplex(4096);
        let (_server_write, client_read) = duplex(4096);
        let calls = Arc::new(AtomicUsize::new(0));
        let handler_calls = calls.clone();
        let handler = Arc::new(move || {
            let handler_calls = handler_calls.clone();
            async move {
                handler_calls.fetch_add(1, Ordering::SeqCst);
                Ok(vec![ModelInfo {
                    id: "gpt-4.1".to_string(),
                    name: "GPT-4.1".to_string(),
                    capabilities: ModelCapabilities::default(),
                    policy: None,
                    billing: None,
                    supported_reasoning_efforts: None,
                    default_reasoning_effort: None,
                }])
            }
        });

        let client = Client::from_transport(
            client_read,
            client_write,
            None,
            std::env::temp_dir(),
            Some(handler),
            None,
        )
        .unwrap();

        let first = client.list_models().await.unwrap();
        let second = client.list_models().await.unwrap();

        assert_eq!(calls.load(Ordering::SeqCst), 1);
        assert_eq!(first[0].id, "gpt-4.1");
        assert_eq!(second[0].name, "GPT-4.1");
    }

    #[tokio::test]
    async fn configure_connection_features_registers_session_fs_provider() {
        let (client_write, mut server_read) = duplex(4096);
        let (mut server_write, client_read) = duplex(4096);
        let config = SessionFsConfig {
            initial_cwd: PathBuf::from("/repo"),
            session_state_path: PathBuf::from("/repo/.session-state"),
            conventions: SessionFsConventions::Posix,
        };

        let client = Client::from_transport(
            client_read,
            client_write,
            None,
            std::env::temp_dir(),
            None,
            Some(config),
        )
        .unwrap();

        let configure = tokio::spawn({
            let client = client.clone();
            async move { client.configure_connection_features().await.unwrap() }
        });

        let request = read_framed(&mut server_read).await;
        assert_eq!(request["method"], "sessionFs.setProvider");
        assert_eq!(request["params"]["initialCwd"], "/repo");
        assert_eq!(
            request["params"]["sessionStatePath"],
            "/repo/.session-state"
        );
        assert_eq!(request["params"]["conventions"], "posix");

        let response = serde_json::json!({
            "jsonrpc": "2.0",
            "id": request["id"],
            "result": {}
        });
        write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
        configure.await.unwrap();
    }

    #[test]
    fn validate_session_fs_config_rejects_unknown_conventions() {
        let error = validate_session_fs_config(&SessionFsConfig {
            initial_cwd: PathBuf::from("/repo"),
            session_state_path: PathBuf::from("/repo/.session-state"),
            conventions: SessionFsConventions::Unknown,
        })
        .unwrap_err();

        assert!(matches!(error, Error::InvalidConfig(_)));
    }
}
