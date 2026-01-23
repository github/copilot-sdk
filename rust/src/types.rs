//! Core types for the Copilot SDK

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Connection state of the client
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Error,
}

/// Options for configuring the Copilot CLI client
#[derive(Debug, Clone)]
pub struct ClientOptions {
    /// Path to the Copilot CLI executable (default: "copilot")
    pub cli_path: String,
    /// Working directory for the CLI process (default: current directory)
    pub cwd: Option<String>,
    /// Port for TCP transport (default: 0 = random port)
    pub port: u16,
    /// Use stdio transport instead of TCP (default: true)
    pub use_stdio: bool,
    /// URL of an existing Copilot CLI server to connect to over TCP
    /// Format: "host:port", "http://host:port", or just "port"
    /// Mutually exclusive with cli_path when use_stdio is true
    pub cli_url: Option<String>,
    /// Log level for the CLI server (default: "info")
    pub log_level: String,
    /// Automatically start the CLI server on first use (default: true)
    pub auto_start: bool,
    /// Automatically restart the CLI server if it crashes (default: true)
    pub auto_restart: bool,
    /// Environment variables for the CLI process
    pub env: Option<HashMap<String, String>>,
}

impl Default for ClientOptions {
    fn default() -> Self {
        Self {
            cli_path: "copilot".to_string(),
            cwd: None,
            port: 0,
            use_stdio: true,
            cli_url: None,
            log_level: "info".to_string(),
            auto_start: true,
            auto_restart: true,
            env: None,
        }
    }
}

/// System message configuration mode
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "mode", rename_all = "lowercase")]
pub enum SystemMessage {
    /// Append content to the default system message
    Append {
        #[serde(skip_serializing_if = "Option::is_none")]
        content: Option<String>,
    },
    /// Replace the entire system message (removes SDK guardrails)
    Replace { content: String },
}

/// Permission request from the server
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionRequest {
    pub kind: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    #[serde(flatten)]
    pub extra: HashMap<String, serde_json::Value>,
}

/// Result of a permission request
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionRequestResult {
    pub kind: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<Vec<serde_json::Value>>,
}

/// Context for permission invocation
#[derive(Debug, Clone)]
pub struct PermissionInvocation {
    pub session_id: String,
}

/// MCP (Model Context Protocol) server configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum MCPServerConfig {
    Local(MCPLocalServerConfig),
    Remote(MCPRemoteServerConfig),
}

/// Local/stdio MCP server configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPLocalServerConfig {
    pub tools: Vec<String>,
    #[serde(rename = "type")]
    pub server_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<u32>,
    pub command: String,
    pub args: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,
}

/// Remote MCP server configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPRemoteServerConfig {
    pub tools: Vec<String>,
    #[serde(rename = "type")]
    pub server_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<u32>,
    pub url: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
}
