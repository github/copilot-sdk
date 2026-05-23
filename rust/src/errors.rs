//! Crate errors.

use std::{
    borrow::{Borrow, Cow},
    fmt,
    time::Duration,
};
use crate::types::SessionId;

/// Crate-specific [`Result`](std::result::Result).
pub type Result<T> = std::result::Result<T, Error>;

// ── Repr / Custom ─────────────────────────────────────────────────────────────

/// Internal representation shared by all SDK error structs.
///
/// `T` is the `*Kind` enum specific to each error struct. Shared across
/// [`Error`], [`ProtocolError`], [`SessionError`], [`FsError`],
/// [`RecvError`], and the crate-internal `EmbeddedCliError`.
#[derive(Debug)]
pub(crate) enum Repr<T: fmt::Debug> {
    Simple(T),
    SimpleMessage(T, Cow<'static, str>),
    Custom(Custom<T>),
    // CustomMessage(Custom<T>, Cow<'static, str>),
}

/// Custom error representation: a kind tag plus a boxed source error.
#[derive(Debug)]
pub(crate) struct Custom<T: fmt::Debug> {
    pub(crate) kind: T,
    pub(crate) error: Box<dyn std::error::Error + Send + Sync>,
}

// ── ProtocolErrorKind / ProtocolError ─────────────────────────────────────────

/// Specific protocol-level error kind in the JSON-RPC transport or CLI lifecycle.
#[derive(Clone, Debug, PartialEq, Eq)]
#[non_exhaustive]
pub enum ProtocolErrorKind {
    /// Missing `Content-Length` header in a JSON-RPC message.
    MissingContentLength,

    /// Invalid `Content-Length` header value.
    InvalidContentLength(String),

    /// A pending JSON-RPC request was cancelled (e.g. the response channel was dropped).
    RequestCancelled,

    /// The CLI process did not report a listening port within the timeout.
    CliStartupTimeout,

    /// The CLI process exited before reporting a listening port.
    CliStartupFailed,

    /// The CLI server's protocol version is outside the SDK's supported range.
    VersionMismatch {
        /// Version reported by the server.
        server: u32,
        /// Minimum version supported by this SDK.
        min: u32,
        /// Maximum version supported by this SDK.
        max: u32,
    },

    /// The CLI server's protocol version changed between calls.
    VersionChanged {
        /// Previously negotiated version.
        previous: u32,
        /// Newly reported version.
        current: u32,
    },
}

impl fmt::Display for ProtocolErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProtocolErrorKind::MissingContentLength => {
                write!(f, "missing Content-Length header")
            }
            ProtocolErrorKind::InvalidContentLength(v) => {
                write!(f, "invalid Content-Length value: \"{v}\"")
            }
            ProtocolErrorKind::RequestCancelled => write!(f, "request cancelled"),
            ProtocolErrorKind::CliStartupTimeout => {
                write!(f, "timed out waiting for CLI to report listening port")
            }
            ProtocolErrorKind::CliStartupFailed => {
                write!(f, "CLI exited before reporting listening port")
            }
            ProtocolErrorKind::VersionMismatch { server, min, max } => {
                write!(
                    f,
                    "version mismatch: server={server}, supported={min}\u{2013}{max}"
                )
            }
            ProtocolErrorKind::VersionChanged { previous, current } => {
                write!(f, "version changed: was {previous}, now {current}")
            }
        }
    }
}

/// Errors in the JSON-RPC transport or CLI lifecycle.
///
/// Accessible via [`Error::kind`] when the kind is
/// [`ErrorKind::Protocol`].
#[derive(Debug)]
pub struct ProtocolError {
    repr: Repr<ProtocolErrorKind>,
}

impl ProtocolError {
    /// The [`ProtocolErrorKind`] of this error.
    pub fn kind(&self) -> &ProtocolErrorKind {
        match &self.repr {
            Repr::Simple(k)
            | Repr::SimpleMessage(k, ..)
            | Repr::Custom(Custom { kind: k, .. }) => k,
        }
    }

    /// The message provided when this error was constructed, or `None`.
    pub fn message(&self) -> Option<&str> {
        match &self.repr {
            Repr::SimpleMessage(_, m) => Some(m.borrow()),
            _ => None,
        }
    }
}

impl fmt::Display for ProtocolError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match &self.repr {
            Repr::Simple(k) => write!(f, "{k}"),
            Repr::SimpleMessage(_, m) => write!(f, "{m}"),
            Repr::Custom(Custom { error, .. }) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for ProtocolError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            Repr::Custom(Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}

impl From<ProtocolErrorKind> for ProtocolError {
    fn from(kind: ProtocolErrorKind) -> Self {
        Self { repr: Repr::Simple(kind) }
    }
}

// ── SessionErrorKind / SessionError ───────────────────────────────────────────

/// Session-scoped error kind.
#[derive(Clone, Debug, PartialEq, Eq)]
#[non_exhaustive]
pub enum SessionErrorKind {
    /// The CLI could not find the requested session.
    NotFound(SessionId),

    /// The CLI reported an error during agent execution (via `session.error` event).
    AgentError,

    /// A `send_and_wait` call exceeded its timeout.
    Timeout(Duration),

    /// `send` was called while a `send_and_wait` is in flight.
    SendWhileWaiting,

    /// The session event loop exited before a pending `send_and_wait` completed.
    EventLoopClosed,

    /// Elicitation is not supported by the host.
    /// Check `session.capabilities().ui.elicitation` before calling UI methods.
    ElicitationNotSupported,

    /// The client was started with [`crate::ClientOptions::session_fs`] but this
    /// session was created without a [`crate::session_fs::SessionFsProvider`]. Set one via
    /// [`crate::SessionConfig::with_session_fs_provider`] (or
    /// [`crate::ResumeSessionConfig::with_session_fs_provider`]).
    SessionFsProviderRequired,

    /// [`crate::ClientOptions::session_fs`] was provided with empty or invalid
    /// fields. All of `initial_cwd` and `session_state_path` must be non-empty.
    InvalidSessionFsConfig,

    /// The CLI returned a different session ID than the one the SDK registered.
    SessionIdMismatch {
        /// Session ID registered by the SDK before the RPC was sent.
        requested: SessionId,
        /// Session ID returned by the CLI.
        returned: SessionId,
    },
}

impl fmt::Display for SessionErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            SessionErrorKind::NotFound(id) => write!(f, "session not found: {id}"),
            SessionErrorKind::AgentError => write!(f, "agent error"),
            SessionErrorKind::Timeout(d) => write!(f, "timed out after {d:?}"),
            SessionErrorKind::SendWhileWaiting => {
                write!(f, "cannot send while send_and_wait is in flight")
            }
            SessionErrorKind::EventLoopClosed => {
                write!(f, "event loop closed before session reached idle")
            }
            SessionErrorKind::ElicitationNotSupported => write!(
                f,
                "elicitation not supported by host \
                 \u{2014} check session.capabilities().ui.elicitation first"
            ),
            SessionErrorKind::SessionFsProviderRequired => write!(
                f,
                "session was created on a client with session_fs configured \
                 but no SessionFsProvider was supplied"
            ),
            SessionErrorKind::InvalidSessionFsConfig => {
                write!(f, "invalid SessionFsConfig")
            }
            SessionErrorKind::SessionIdMismatch { requested, returned } => write!(
                f,
                "CLI returned session ID {returned} after SDK registered {requested}"
            ),
        }
    }
}

/// Session-scoped errors.
///
/// Accessible via [`Error::kind`] when the kind is [`ErrorKind::Session`].
#[derive(Debug)]
pub struct SessionError {
    repr: Repr<SessionErrorKind>,
}

impl SessionError {
    /// The [`SessionErrorKind`] of this error.
    pub fn kind(&self) -> &SessionErrorKind {
        match &self.repr {
            Repr::Simple(k)
            | Repr::SimpleMessage(k, ..)
            | Repr::Custom(Custom { kind: k, .. }) => k,
        }
    }

    /// The message provided when this error was constructed, or `None`.
    pub fn message(&self) -> Option<&str> {
        match &self.repr {
            Repr::SimpleMessage(_, m) => Some(m.borrow()),
            _ => None,
        }
    }
}

impl fmt::Display for SessionError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if let &SessionErrorKind::InvalidSessionFsConfig = self.kind() {
            write!(f, "{}: ", SessionErrorKind::InvalidSessionFsConfig)?;
        }
        match &self.repr {
            Repr::Simple(k) => write!(f, "{k}"),
            Repr::SimpleMessage(_, m) => write!(f, "{m}"),
            Repr::Custom(Custom { error, .. }) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for SessionError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            Repr::Custom(Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}

impl From<SessionErrorKind> for SessionError {
    fn from(kind: SessionErrorKind) -> Self {
        Self { repr: Repr::Simple(kind) }
    }
}

// ── ErrorKind ─────────────────────────────────────────────────────────────────

/// The kind of [`Error`].
#[derive(Clone, Debug, PartialEq, Eq)]
#[non_exhaustive]
pub enum ErrorKind {
    /// JSON-RPC transport or protocol violation.
    Protocol(ProtocolErrorKind),
    /// The CLI returned a JSON-RPC error response.
    Rpc {
        /// JSON-RPC error code.
        code: i32,
    },
    /// Session-scoped error (not found, agent error, timeout, etc.).
    Session(SessionErrorKind),
    /// I/O error on the stdio transport or during process spawn.
    Io,
    /// Failed to serialize or deserialize a JSON-RPC message.
    Json,
    /// A required binary was not found on the system.
    BinaryNotFound {
        /// Name of the binary.
        name: String,
        /// Optional hint for how to resolve the issue.
        hint: Option<String>,
    },
    /// Invalid combination of options or configuration.
    InvalidConfig,
}

impl fmt::Display for ErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ErrorKind::Protocol(k) => write!(f, "{k}"),
            ErrorKind::Rpc { code } => write!(f, "RPC error {code}"),
            ErrorKind::Session(k) => write!(f, "{k}"),
            ErrorKind::Io => write!(f, "I/O error"),
            ErrorKind::Json => write!(f, "JSON error"),
            ErrorKind::BinaryNotFound { name, hint: Some(h) } => {
                write!(f, "binary not found: {name} ({h})")
            }
            ErrorKind::BinaryNotFound { name, hint: None } => {
                write!(f, "binary not found: {name}")
            }
            ErrorKind::InvalidConfig => write!(f, "invalid configuration"),
        }
    }
}

/// Errors returned by the SDK.
#[derive(Debug)]
pub struct Error {
    repr: Repr<ErrorKind>,
}

impl Error {
    /// Constructs a new `Error` boxing another [`std::error::Error`].
    pub(crate) fn new<E>(kind: ErrorKind, error: E) -> Self
    where
        E: Into<Box<dyn std::error::Error + Send + Sync>>,
    {
        Self {
            repr: Repr::Custom(Custom {
                kind,
                error: error.into(),
            }),
        }
    }

    /// The [`ErrorKind`] of this `Error`.
    pub fn kind(&self) -> &ErrorKind {
        match &self.repr {
            Repr::Simple(kind)
            | Repr::SimpleMessage(kind, ..)
            | Repr::Custom(Custom { kind, .. }) => kind,
        }
    }

    /// The message provided when this `Error` was constructed, or `None`.
    pub fn message(&self) -> Option<&str> {
        match &self.repr {
            Repr::SimpleMessage(_, message) => Some(message.borrow()),
            _ => None,
        }
    }

    /// Create an `Error` with a message.
    #[must_use]
    pub fn with_message<C>(kind: ErrorKind, message: C) -> Self
    where
        C: Into<Cow<'static, str>>,
    {
        Self {
            repr: Repr::SimpleMessage(kind, message.into()),
        }
    }

    /// Returns `true` if this error indicates the transport is broken — the CLI
    /// process exited, the connection was lost, or an I/O failure occurred.
    /// Callers should discard the client and create a fresh one.
    pub fn is_transport_failure(&self) -> bool {
        matches!(self.kind(), ErrorKind::Io)
            || matches!(
                self.kind(),
                ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled)
            )
    }

    /// Returns the JSON-RPC error code if this is an [`ErrorKind::Rpc`] error.
    pub fn rpc_code(&self) -> Option<i32> {
        match self.kind() {
            ErrorKind::Rpc { code } => Some(*code),
            _ => None,
        }
    }
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if let &ErrorKind::Rpc { code } = self.kind() {
            write!(f, "{}: ", ErrorKind::Rpc { code })?;
        }
        match &self.repr {
            Repr::Simple(kind) => write!(f, "{kind}"),
            Repr::SimpleMessage(_, message) => write!(f, "{message}"),
            Repr::Custom(Custom { error, .. }) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match &self.repr {
            Repr::Custom(Custom { error, .. }) => Some(&**error),
            _ => None,
        }
    }
}

impl From<ErrorKind> for Error {
    fn from(kind: ErrorKind) -> Self {
        Self {
            repr: Repr::Simple(kind),
        }
    }
}

impl From<ProtocolErrorKind> for Error {
    fn from(kind: ProtocolErrorKind) -> Self {
        Self::from(ErrorKind::Protocol(kind))
    }
}

impl From<SessionErrorKind> for Error {
    fn from(kind: SessionErrorKind) -> Self {
        Self::from(ErrorKind::Session(kind))
    }
}

impl From<std::io::Error> for Error {
    fn from(error: std::io::Error) -> Self {
        Self::new(ErrorKind::Io, error)
    }
}

impl From<serde_json::Error> for Error {
    fn from(error: serde_json::Error) -> Self {
        Self::new(ErrorKind::Json, error)
    }
}

/// Aggregate of errors collected during [`crate::Client::stop`].
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
pub struct StopErrors(pub(crate) Vec<Error>);

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

impl fmt::Display for StopErrors {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
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
