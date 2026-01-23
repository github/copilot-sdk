//! Tool system for defining and handling custom tools

use crate::error::Result;
use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;

/// A tool definition with JSON schema for parameters
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Tool {
    /// Tool name
    pub name: String,

    /// Tool description
    pub description: String,

    /// JSON schema for parameters
    pub parameters: Value,
}

impl Tool {
    /// Create a new tool with the given name, description, and parameter schema
    pub fn new(name: impl Into<String>, description: impl Into<String>, parameters: Value) -> Self {
        Self {
            name: name.into(),
            description: description.into(),
            parameters,
        }
    }

    /// Create a simple tool with no parameters
    pub fn simple(name: impl Into<String>, description: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            description: description.into(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {},
                "required": []
            }),
        }
    }
}

/// Context for tool invocation
#[derive(Debug, Clone)]
pub struct ToolInvocation {
    /// Session ID where the tool was called
    pub session_id: String,

    /// Unique ID for this tool call
    pub tool_call_id: String,
}

/// Result of a tool execution
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolResult {
    /// Result content (text or data)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,

    /// Binary data (base64 encoded)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<String>,

    /// MIME type for binary data
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,

    /// Telemetry data
    #[serde(skip_serializing_if = "Option::is_none")]
    pub telemetry: Option<HashMap<String, Value>>,

    /// Whether the tool execution was successful
    #[serde(skip_serializing_if = "Option::is_none")]
    pub success: Option<bool>,

    /// Error message if execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

impl ToolResult {
    /// Create a text result
    pub fn text(content: impl Into<String>) -> Self {
        Self {
            content: Some(content.into()),
            data: None,
            mime_type: None,
            telemetry: None,
            success: Some(true),
            error: None,
        }
    }

    /// Create a binary result
    pub fn binary(data: Vec<u8>, mime_type: impl Into<String>) -> Self {
        Self {
            content: None,
            data: Some(base64::encode(&data)),
            mime_type: Some(mime_type.into()),
            telemetry: None,
            success: Some(true),
            error: None,
        }
    }

    /// Create an error result
    pub fn error(message: impl Into<String>) -> Self {
        Self {
            content: None,
            data: None,
            mime_type: None,
            telemetry: None,
            success: Some(false),
            error: Some(message.into()),
        }
    }

    /// Add telemetry data to the result
    pub fn with_telemetry(mut self, telemetry: HashMap<String, Value>) -> Self {
        self.telemetry = Some(telemetry);
        self
    }
}

/// Handler trait for tool execution
#[async_trait]
pub trait ToolHandler: Send + Sync {
    /// Execute the tool with the given arguments
    async fn handle(
        &self,
        arguments: HashMap<String, Value>,
        invocation: ToolInvocation,
    ) -> Result<ToolResult>;
}

/// Helper to create a tool handler from a closure
pub struct FunctionToolHandler<F>
where
    F: Fn(
            HashMap<String, Value>,
            ToolInvocation,
        )
            -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<ToolResult>> + Send>>
        + Send
        + Sync,
{
    handler: F,
}

impl<F> FunctionToolHandler<F>
where
    F: Fn(
            HashMap<String, Value>,
            ToolInvocation,
        )
            -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<ToolResult>> + Send>>
        + Send
        + Sync,
{
    pub fn new(handler: F) -> Self {
        Self { handler }
    }
}

#[async_trait]
impl<F> ToolHandler for FunctionToolHandler<F>
where
    F: Fn(
            HashMap<String, Value>,
            ToolInvocation,
        )
            -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<ToolResult>> + Send>>
        + Send
        + Sync,
{
    async fn handle(
        &self,
        arguments: HashMap<String, Value>,
        invocation: ToolInvocation,
    ) -> Result<ToolResult> {
        (self.handler)(arguments, invocation).await
    }
}

// Helper module for base64 encoding (simple implementation)
mod base64 {
    const CHARSET: &[u8] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    pub fn encode(data: &[u8]) -> String {
        let mut result = String::new();
        let mut i = 0;

        while i < data.len() {
            let b0 = data[i];
            let b1 = if i + 1 < data.len() { data[i + 1] } else { 0 };
            let b2 = if i + 2 < data.len() { data[i + 2] } else { 0 };

            result.push(CHARSET[(b0 >> 2) as usize] as char);
            result.push(CHARSET[(((b0 & 0x03) << 4) | (b1 >> 4)) as usize] as char);

            if i + 1 < data.len() {
                result.push(CHARSET[(((b1 & 0x0f) << 2) | (b2 >> 6)) as usize] as char);
            } else {
                result.push('=');
            }

            if i + 2 < data.len() {
                result.push(CHARSET[(b2 & 0x3f) as usize] as char);
            } else {
                result.push('=');
            }

            i += 3;
        }

        result
    }
}
