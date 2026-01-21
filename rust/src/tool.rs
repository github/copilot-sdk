//! Tool definition helpers for the Copilot SDK.

use crate::error::Result;
use schemars::JsonSchema;
use serde::{de::DeserializeOwned, Deserialize, Serialize};
use serde_json::Value;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;

/// Information about a tool invocation.
#[derive(Debug, Clone)]
pub struct ToolInvocation {
    /// Session ID.
    pub session_id: String,
    /// Unique ID for this tool call.
    pub tool_call_id: String,
    /// Name of the tool being called.
    pub tool_name: String,
    /// Raw arguments as JSON value.
    pub arguments: Value,
}

/// Result of a tool execution.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolResult {
    /// Text result for the LLM.
    #[serde(rename = "textResultForLlm")]
    pub text_result_for_llm: String,

    /// Binary results for the LLM.
    #[serde(rename = "binaryResultsForLlm", skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ToolBinaryResult>>,

    /// Result type: "success" or "failure".
    #[serde(rename = "resultType")]
    pub result_type: String,

    /// Error message (for failures).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,

    /// Session log (optional).
    #[serde(rename = "sessionLog", skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,

    /// Tool telemetry data.
    #[serde(rename = "toolTelemetry", skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<Value>,
}

impl ToolResult {
    /// Create a successful result with text.
    pub fn success(text: impl Into<String>) -> Self {
        Self {
            text_result_for_llm: text.into(),
            binary_results_for_llm: None,
            result_type: "success".to_string(),
            error: None,
            session_log: None,
            tool_telemetry: None,
        }
    }

    /// Create a failure result.
    pub fn failure(error: impl Into<String>) -> Self {
        Self {
            text_result_for_llm: "Invoking this tool produced an error. Detailed information is not available.".to_string(),
            binary_results_for_llm: None,
            result_type: "failure".to_string(),
            error: Some(error.into()),
            session_log: None,
            tool_telemetry: None,
        }
    }

    /// Create a result for an unsupported tool.
    pub fn unsupported(tool_name: &str) -> Self {
        Self {
            text_result_for_llm: format!("Tool '{}' is not supported by this client instance.", tool_name),
            binary_results_for_llm: None,
            result_type: "failure".to_string(),
            error: Some(format!("tool '{}' not supported", tool_name)),
            session_log: None,
            tool_telemetry: None,
        }
    }
}

/// Binary result for tools.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolBinaryResult {
    /// Base64-encoded data.
    pub data: String,
    /// MIME type.
    #[serde(rename = "mimeType")]
    pub mime_type: String,
    /// Result type.
    #[serde(rename = "type")]
    pub result_type: String,
    /// Optional description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// Type alias for async tool handlers.
pub type ToolHandler = Arc<
    dyn Fn(ToolInvocation) -> Pin<Box<dyn Future<Output = Result<ToolResult>> + Send>>
        + Send
        + Sync,
>;

/// A tool definition that can be exposed to Copilot.
#[derive(Clone)]
pub struct Tool {
    /// Tool name.
    pub name: String,
    /// Tool description.
    pub description: String,
    /// JSON Schema for parameters.
    pub parameters: Option<Value>,
    /// Tool handler function.
    pub handler: ToolHandler,
}

impl std::fmt::Debug for Tool {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Tool")
            .field("name", &self.name)
            .field("description", &self.description)
            .field("parameters", &self.parameters)
            .finish()
    }
}

/// Define a tool with automatic JSON schema generation from a typed handler.
///
/// # Example
///
/// ```ignore
/// use schemars::JsonSchema;
/// use serde::Deserialize;
///
/// #[derive(Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     city: String,
/// }
///
/// let tool = define_tool::<GetWeatherParams, _, _>(
///     "get_weather",
///     "Get weather for a city",
///     |params, _inv| async move {
///         Ok(format!("Weather in {}: 22 degrees", params.city))
///     },
/// );
/// ```
pub fn define_tool<P, F, Fut, R>(name: &str, description: &str, handler: F) -> Tool
where
    P: DeserializeOwned + JsonSchema + Send + 'static,
    F: Fn(P, ToolInvocation) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<R>> + Send + 'static,
    R: IntoToolResult + 'static,
{
    // Generate JSON schema for the parameters
    let schema = schemars::schema_for!(P);
    let parameters = serde_json::to_value(schema).ok();

    let handler = Arc::new(handler);

    let wrapped_handler: ToolHandler = Arc::new(move |inv: ToolInvocation| {
        let handler = handler.clone();
        Box::pin(async move {
            // Parse arguments into typed struct
            let params: P = serde_json::from_value(inv.arguments.clone())
                .map_err(|e| crate::error::CopilotError::ToolExecution(
                    format!("Failed to parse arguments: {}", e)
                ))?;

            let result = handler(params, inv).await?;
            result.into_tool_result()
        })
    });

    Tool {
        name: name.to_string(),
        description: description.to_string(),
        parameters,
        handler: wrapped_handler,
    }
}

/// Trait for converting values into ToolResult.
///
/// This trait enables flexible return types from tool handlers. Instead of
/// always returning `ToolResult`, handlers can return simpler types like
/// `String`, `&str`, `()`, or `serde_json::Value`, and they will be
/// automatically converted to successful `ToolResult` values.
///
/// # Built-in Implementations
///
/// | Type | Result |
/// |------|--------|
/// | `ToolResult` | Passed through unchanged |
/// | `String` | Success with the string as content |
/// | `&str` | Success with the string as content |
/// | `()` | Success with empty content |
/// | `serde_json::Value` | Success with JSON serialized as string |
///
/// # Example
///
/// ```ignore
/// // These tool handlers are all valid:
///
/// // Return a String
/// |params, _inv| async move { Ok("Done!".to_string()) }
///
/// // Return a ToolResult for more control
/// |params, _inv| async move { Ok(ToolResult::success("Done!")) }
///
/// // Return nothing (empty success)
/// |params, _inv| async move { Ok(()) }
///
/// // Return JSON
/// |params, _inv| async move { Ok(serde_json::json!({"status": "ok"})) }
/// ```
pub trait IntoToolResult {
    /// Convert this value into a [`ToolResult`].
    ///
    /// # Returns
    ///
    /// A `Result` containing the converted `ToolResult`, or an error if
    /// conversion fails (e.g., JSON serialization error for `Value` types).
    fn into_tool_result(self) -> Result<ToolResult>;
}

impl IntoToolResult for ToolResult {
    fn into_tool_result(self) -> Result<ToolResult> {
        Ok(self)
    }
}

impl IntoToolResult for String {
    fn into_tool_result(self) -> Result<ToolResult> {
        Ok(ToolResult::success(self))
    }
}

impl IntoToolResult for &str {
    fn into_tool_result(self) -> Result<ToolResult> {
        Ok(ToolResult::success(self))
    }
}

impl IntoToolResult for () {
    fn into_tool_result(self) -> Result<ToolResult> {
        Ok(ToolResult::success(""))
    }
}

impl IntoToolResult for Value {
    fn into_tool_result(self) -> Result<ToolResult> {
        let json = serde_json::to_string(&self)?;
        Ok(ToolResult::success(json))
    }
}

/// Builder for creating tools manually without automatic schema generation.
pub struct ToolBuilder {
    name: String,
    description: String,
    parameters: Option<Value>,
}

impl ToolBuilder {
    /// Create a new tool builder.
    pub fn new(name: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            description: String::new(),
            parameters: None,
        }
    }

    /// Set the tool description.
    pub fn description(mut self, description: impl Into<String>) -> Self {
        self.description = description.into();
        self
    }

    /// Set the JSON schema for parameters.
    pub fn parameters(mut self, parameters: Value) -> Self {
        self.parameters = Some(parameters);
        self
    }

    /// Build the tool with an async handler.
    pub fn handler<F, Fut>(self, handler: F) -> Tool
    where
        F: Fn(ToolInvocation) -> Fut + Send + Sync + 'static,
        Fut: Future<Output = Result<ToolResult>> + Send + 'static,
    {
        let handler = Arc::new(handler);
        Tool {
            name: self.name,
            description: self.description,
            parameters: self.parameters,
            handler: Arc::new(move |inv| {
                let handler = handler.clone();
                Box::pin(async move { handler(inv).await })
            }),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tool_result_success() {
        let result = ToolResult::success("Hello");
        assert_eq!(result.result_type, "success");
        assert_eq!(result.text_result_for_llm, "Hello");
        assert!(result.error.is_none());
    }

    #[test]
    fn test_tool_result_failure() {
        let result = ToolResult::failure("Something went wrong");
        assert_eq!(result.result_type, "failure");
        assert!(result.error.is_some());
    }

    #[test]
    fn test_tool_result_unsupported() {
        let result = ToolResult::unsupported("unknown_tool");
        assert_eq!(result.result_type, "failure");
        assert!(result.text_result_for_llm.contains("unknown_tool"));
    }
}
