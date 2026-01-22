//! Error types for the Copilot SDK.

use thiserror::Error;

/// Result type alias for Copilot SDK operations.
pub type Result<T> = std::result::Result<T, CopilotError>;

/// Errors that can occur when using the Copilot SDK.
#[derive(Error, Debug)]
pub enum CopilotError {
    /// Connection to the CLI server failed.
    #[error("connection error: {0}")]
    Connection(String),

    /// JSON-RPC error received from the server.
    #[error("JSON-RPC error (code {code}): {message}")]
    JsonRpc {
        /// The error code.
        code: i32,
        /// The error message.
        message: String,
        /// Optional additional error data.
        data: Option<serde_json::Value>,
    },

    /// Protocol version mismatch between SDK and server.
    #[error("protocol mismatch: SDK expects version {expected}, server reports version {actual}")]
    ProtocolMismatch {
        /// Expected protocol version.
        expected: i32,
        /// Actual protocol version reported by the server.
        actual: i32,
    },

    /// Session-related error.
    #[error("session error: {0}")]
    Session(String),

    /// Tool execution error.
    #[error("tool execution error: {0}")]
    ToolExecution(String),

    /// JSON serialization or deserialization error.
    #[error("serialization error: {0}")]
    Serialization(#[from] serde_json::Error),

    /// I/O error.
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),

    /// Client is not connected.
    #[error("client not connected")]
    NotConnected,

    /// Operation timed out.
    #[error("operation timed out after {0:?}")]
    Timeout(std::time::Duration),

    /// Invalid configuration.
    #[error("invalid configuration: {0}")]
    InvalidConfig(String),

    /// Client was stopped.
    #[error("client stopped")]
    ClientStopped,

    /// Request was cancelled.
    #[error("request cancelled")]
    Cancelled,
}

impl CopilotError {
    /// Create a new JSON-RPC error.
    pub fn json_rpc(code: i32, message: impl Into<String>, data: Option<serde_json::Value>) -> Self {
        Self::JsonRpc {
            code,
            message: message.into(),
            data,
        }
    }

    /// Create a new connection error.
    pub fn connection(message: impl Into<String>) -> Self {
        Self::Connection(message.into())
    }

    /// Create a new session error.
    pub fn session(message: impl Into<String>) -> Self {
        Self::Session(message.into())
    }

    /// Create a new tool execution error.
    pub fn tool_execution(message: impl Into<String>) -> Self {
        Self::ToolExecution(message.into())
    }

    /// Create a new invalid configuration error.
    pub fn invalid_config(message: impl Into<String>) -> Self {
        Self::InvalidConfig(message.into())
    }
}

/// JSON-RPC error structure for protocol-level errors.
#[derive(Debug, Clone)]
pub struct JsonRpcError {
    /// Error code.
    pub code: i32,
    /// Error message.
    pub message: String,
    /// Optional additional data.
    pub data: Option<serde_json::Value>,
}

impl JsonRpcError {
    /// Create a new JSON-RPC error.
    pub fn new(code: i32, message: impl Into<String>) -> Self {
        Self {
            code,
            message: message.into(),
            data: None,
        }
    }

    /// Create a new JSON-RPC error with additional data.
    pub fn with_data(code: i32, message: impl Into<String>, data: serde_json::Value) -> Self {
        Self {
            code,
            message: message.into(),
            data: Some(data),
        }
    }

    /// Standard error codes.
    pub const PARSE_ERROR: i32 = -32700;
    pub const INVALID_REQUEST: i32 = -32600;
    pub const METHOD_NOT_FOUND: i32 = -32601;
    pub const INVALID_PARAMS: i32 = -32602;
    pub const INTERNAL_ERROR: i32 = -32603;
}

impl std::fmt::Display for JsonRpcError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "JSON-RPC Error {}: {}", self.code, self.message)
    }
}

impl std::error::Error for JsonRpcError {}

impl From<JsonRpcError> for CopilotError {
    fn from(err: JsonRpcError) -> Self {
        Self::JsonRpc {
            code: err.code,
            message: err.message,
            data: err.data,
        }
    }
}
