//! Error types for the Copilot SDK

use thiserror::Error;

/// Result type alias for SDK operations
pub type Result<T> = std::result::Result<T, Error>;

/// SDK error types
#[derive(Error, Debug)]
pub enum Error {
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    #[error("JSON serialization error: {0}")]
    Json(#[from] serde_json::Error),

    #[error("JSON-RPC error: {0}")]
    JsonRpc(String),

    #[error("Client not connected")]
    NotConnected,

    #[error("Client already connected")]
    AlreadyConnected,

    #[error("Session not found: {0}")]
    SessionNotFound(String),

    #[error("Invalid configuration: {0}")]
    InvalidConfig(String),

    #[error("CLI process error: {0}")]
    ProcessError(String),

    #[error("Connection error: {0}")]
    ConnectionError(String),

    #[error("Timeout waiting for response")]
    Timeout,

    #[error("Permission denied: {0}")]
    PermissionDenied(String),

    #[error("Tool execution error: {0}")]
    ToolError(String),

    #[error("{0}")]
    Other(String),
}
