//! Error types for the Copilot SDK.

use thiserror::Error;

/// Main error type for the Copilot SDK.
#[derive(Error, Debug)]
pub enum CopilotError {
    /// JSON-RPC error received from the server.
    #[error("JSON-RPC error {code}: {message}")]
    JsonRpc {
        code: i32,
        message: String,
        data: Option<serde_json::Value>,
    },

    /// Connection error.
    #[error("Connection error: {0}")]
    Connection(String),

    /// Process error (CLI server failed to start or crashed).
    #[error("Process error: {0}")]
    Process(String),

    /// Session error.
    #[error("Session error: {0}")]
    Session(String),

    /// Client not connected.
    #[error("Client not connected. Call start() first")]
    NotConnected,

    /// Protocol version mismatch.
    #[error("SDK protocol version mismatch: SDK expects version {expected}, but server reports version {actual}. Please update your SDK or server to ensure compatibility")]
    ProtocolVersionMismatch { expected: i32, actual: i32 },

    /// Protocol version not reported by server.
    #[error("SDK protocol version mismatch: SDK expects version {expected}, but server does not report a protocol version. Please update your server to ensure compatibility")]
    ProtocolVersionNotReported { expected: i32 },

    /// Timeout waiting for response.
    #[error("Timeout waiting for response")]
    Timeout,

    /// Invalid response from server.
    #[error("Invalid response: {0}")]
    InvalidResponse(String),

    /// Tool execution error.
    #[error("Tool execution error: {0}")]
    ToolExecution(String),

    /// Serialization error.
    #[error("Serialization error: {0}")]
    Serialization(#[from] serde_json::Error),

    /// IO error.
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    /// Client stopped.
    #[error("Client stopped")]
    ClientStopped,

    /// Invalid configuration.
    #[error("Invalid configuration: {0}")]
    InvalidConfig(String),
}

/// JSON-RPC error representation.
#[derive(Debug, Clone)]
pub struct JsonRpcError {
    pub code: i32,
    pub message: String,
    pub data: Option<serde_json::Value>,
}

impl JsonRpcError {
    pub fn new(code: i32, message: impl Into<String>) -> Self {
        Self {
            code,
            message: message.into(),
            data: None,
        }
    }

    pub fn with_data(mut self, data: serde_json::Value) -> Self {
        self.data = Some(data);
        self
    }

    /// Standard JSON-RPC error codes.
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
        CopilotError::JsonRpc {
            code: err.code,
            message: err.message,
            data: err.data,
        }
    }
}

/// Result type alias for Copilot operations.
pub type Result<T> = std::result::Result<T, CopilotError>;
