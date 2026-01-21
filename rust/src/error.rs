//! Error types for the Copilot SDK.
//!
//! This module provides error types used throughout the SDK for handling various
//! failure scenarios including JSON-RPC errors, connection issues, process failures,
//! and configuration problems.
//!
//! # Error Types
//!
//! The main error type is [`CopilotError`], which encompasses all possible errors
//! that can occur when using the SDK:
//!
//! - **Protocol Errors**: JSON-RPC errors, version mismatches
//! - **Connection Errors**: Network issues, connection failures
//! - **Process Errors**: CLI server startup/crash failures
//! - **Session Errors**: Session creation/management failures
//! - **Configuration Errors**: Invalid client configuration
//!
//! # Example
//!
//! ```ignore
//! use copilot_sdk::{CopilotClient, CopilotError};
//!
//! match CopilotClient::new(None) {
//!     Ok(client) => {
//!         // Use client...
//!     }
//!     Err(CopilotError::InvalidConfig(msg)) => {
//!         eprintln!("Configuration error: {}", msg);
//!     }
//!     Err(e) => {
//!         eprintln!("Error: {}", e);
//!     }
//! }
//! ```

use thiserror::Error;

/// Main error type for the Copilot SDK.
///
/// This enum represents all possible errors that can occur when using the SDK.
/// Each variant includes contextual information to help diagnose the issue.
///
/// # Variants
///
/// - [`JsonRpc`](Self::JsonRpc) - Error received from the JSON-RPC server
/// - [`Connection`](Self::Connection) - Network or connection failure
/// - [`Process`](Self::Process) - CLI server process failure
/// - [`Session`](Self::Session) - Session-related error
/// - [`NotConnected`](Self::NotConnected) - Client not connected
/// - [`ProtocolVersionMismatch`](Self::ProtocolVersionMismatch) - SDK/server version incompatibility
/// - [`Timeout`](Self::Timeout) - Operation timed out
/// - [`InvalidResponse`](Self::InvalidResponse) - Malformed server response
/// - [`ToolExecution`](Self::ToolExecution) - Tool handler failure
/// - [`Serialization`](Self::Serialization) - JSON serialization failure
/// - [`Io`](Self::Io) - I/O operation failure
/// - [`ClientStopped`](Self::ClientStopped) - Client has been stopped
/// - [`InvalidConfig`](Self::InvalidConfig) - Invalid configuration provided
#[derive(Error, Debug)]
pub enum CopilotError {
    /// JSON-RPC error received from the server.
    ///
    /// This error is returned when the JSON-RPC server responds with an error.
    /// The error code and message are provided by the server.
    ///
    /// # Fields
    ///
    /// - `code` - Standard JSON-RPC error code (see [`JsonRpcError`] constants)
    /// - `message` - Human-readable error description from the server
    /// - `data` - Optional additional error data provided by the server
    #[error("JSON-RPC error {code}: {message}")]
    JsonRpc {
        /// The JSON-RPC error code. Standard codes are defined in [`JsonRpcError`].
        code: i32,
        /// Human-readable error message from the server.
        message: String,
        /// Optional additional error data provided by the server.
        data: Option<serde_json::Value>,
    },

    /// Connection error.
    ///
    /// Returned when there is a network or transport-level failure
    /// communicating with the CLI server.
    #[error("Connection error: {0}")]
    Connection(String),

    /// Process error (CLI server failed to start or crashed).
    ///
    /// Returned when the CLI server process fails to start, crashes,
    /// or exits unexpectedly.
    #[error("Process error: {0}")]
    Process(String),

    /// Session error.
    ///
    /// Returned when a session-related operation fails, such as
    /// creating, resuming, or destroying a session.
    #[error("Session error: {0}")]
    Session(String),

    /// Client not connected.
    ///
    /// Returned when attempting to use a client that hasn't been
    /// connected yet. Call [`CopilotClient::start()`](crate::CopilotClient::start)
    /// before using other methods.
    #[error("Client not connected. Call start() first")]
    NotConnected,

    /// Protocol version mismatch.
    ///
    /// Returned when the SDK's protocol version doesn't match the server's
    /// version. This usually means either the SDK or CLI needs to be updated.
    ///
    /// # Fields
    ///
    /// - `expected` - The protocol version the SDK expects
    /// - `actual` - The protocol version reported by the server
    #[error("SDK protocol version mismatch: SDK expects version {expected}, but server reports version {actual}. Please update your SDK or server to ensure compatibility")]
    ProtocolVersionMismatch {
        /// The protocol version expected by this SDK.
        expected: i32,
        /// The protocol version reported by the server.
        actual: i32,
    },

    /// Protocol version not reported by server.
    ///
    /// Returned when the server doesn't report a protocol version.
    /// This usually indicates an older server that needs to be updated.
    ///
    /// # Fields
    ///
    /// - `expected` - The protocol version the SDK expects
    #[error("SDK protocol version mismatch: SDK expects version {expected}, but server does not report a protocol version. Please update your server to ensure compatibility")]
    ProtocolVersionNotReported {
        /// The protocol version expected by this SDK.
        expected: i32,
    },

    /// Timeout waiting for response.
    ///
    /// Returned when an operation takes longer than the configured
    /// timeout duration.
    #[error("Timeout waiting for response")]
    Timeout,

    /// Invalid response from server.
    ///
    /// Returned when the server sends a response that cannot be
    /// parsed or is malformed.
    #[error("Invalid response: {0}")]
    InvalidResponse(String),

    /// Tool execution error.
    ///
    /// Returned when a tool handler fails during execution.
    /// The error message contains details about what went wrong.
    #[error("Tool execution error: {0}")]
    ToolExecution(String),

    /// Serialization error.
    ///
    /// Returned when JSON serialization or deserialization fails.
    #[error("Serialization error: {0}")]
    Serialization(#[from] serde_json::Error),

    /// IO error.
    ///
    /// Returned when an I/O operation fails, such as reading from
    /// or writing to the CLI server process.
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    /// Client stopped.
    ///
    /// Returned when attempting to use a client that has been stopped.
    #[error("Client stopped")]
    ClientStopped,

    /// Invalid configuration.
    ///
    /// Returned when the client configuration is invalid, such as
    /// providing mutually exclusive options.
    ///
    /// # Example
    ///
    /// ```ignore
    /// // This will return InvalidConfig because cli_url and use_stdio are
    /// // mutually exclusive
    /// let client = CopilotClient::new(Some(ClientOptions {
    ///     cli_url: Some("localhost:8080".to_string()),
    ///     use_stdio: Some(true),
    ///     ..Default::default()
    /// }));
    /// ```
    #[error("Invalid configuration: {0}")]
    InvalidConfig(String),
}

/// JSON-RPC error representation.
///
/// This struct represents a JSON-RPC 2.0 error object, containing an error code,
/// message, and optional additional data. It can be converted into a [`CopilotError`].
///
/// # Standard Error Codes
///
/// The JSON-RPC 2.0 specification defines several standard error codes:
///
/// | Code | Constant | Description |
/// |------|----------|-------------|
/// | -32700 | [`PARSE_ERROR`](Self::PARSE_ERROR) | Invalid JSON was received |
/// | -32600 | [`INVALID_REQUEST`](Self::INVALID_REQUEST) | Invalid JSON-RPC request |
/// | -32601 | [`METHOD_NOT_FOUND`](Self::METHOD_NOT_FOUND) | Method does not exist |
/// | -32602 | [`INVALID_PARAMS`](Self::INVALID_PARAMS) | Invalid method parameters |
/// | -32603 | [`INTERNAL_ERROR`](Self::INTERNAL_ERROR) | Internal JSON-RPC error |
///
/// # Example
///
/// ```
/// use copilot_sdk::JsonRpcError;
///
/// let error = JsonRpcError::new(JsonRpcError::METHOD_NOT_FOUND, "Method not found")
///     .with_data(serde_json::json!({"method": "unknown.method"}));
///
/// assert_eq!(error.code, -32601);
/// assert_eq!(error.message, "Method not found");
/// ```
#[derive(Debug, Clone)]
pub struct JsonRpcError {
    /// The JSON-RPC error code.
    ///
    /// Standard error codes are available as constants on this type
    /// (e.g., [`Self::PARSE_ERROR`], [`Self::METHOD_NOT_FOUND`]).
    /// Server-specific error codes may also be used.
    pub code: i32,

    /// Human-readable error message describing what went wrong.
    pub message: String,

    /// Optional additional error data.
    ///
    /// This can contain any additional information about the error
    /// that might be useful for debugging or error handling.
    pub data: Option<serde_json::Value>,
}

impl JsonRpcError {
    /// Create a new JSON-RPC error with the given code and message.
    ///
    /// # Arguments
    ///
    /// * `code` - The JSON-RPC error code
    /// * `message` - Human-readable error message
    ///
    /// # Example
    ///
    /// ```
    /// use copilot_sdk::JsonRpcError;
    ///
    /// let error = JsonRpcError::new(-32600, "Invalid Request");
    /// assert_eq!(error.code, JsonRpcError::INVALID_REQUEST);
    /// ```
    pub fn new(code: i32, message: impl Into<String>) -> Self {
        Self {
            code,
            message: message.into(),
            data: None,
        }
    }

    /// Add additional data to the error.
    ///
    /// Returns `self` for method chaining.
    ///
    /// # Arguments
    ///
    /// * `data` - Additional error data as a JSON value
    ///
    /// # Example
    ///
    /// ```
    /// use copilot_sdk::JsonRpcError;
    /// use serde_json::json;
    ///
    /// let error = JsonRpcError::new(-32602, "Invalid params")
    ///     .with_data(json!({"expected": "string", "got": "number"}));
    /// ```
    pub fn with_data(mut self, data: serde_json::Value) -> Self {
        self.data = Some(data);
        self
    }

    /// Parse error: Invalid JSON was received by the server.
    ///
    /// An error occurred on the server while parsing the JSON text.
    pub const PARSE_ERROR: i32 = -32700;

    /// Invalid Request: The JSON sent is not a valid JSON-RPC Request object.
    pub const INVALID_REQUEST: i32 = -32600;

    /// Method not found: The method does not exist or is not available.
    pub const METHOD_NOT_FOUND: i32 = -32601;

    /// Invalid params: Invalid method parameter(s).
    pub const INVALID_PARAMS: i32 = -32602;

    /// Internal error: Internal JSON-RPC error.
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
///
/// This is a convenience alias for `std::result::Result<T, CopilotError>`.
pub type Result<T> = std::result::Result<T, CopilotError>;
