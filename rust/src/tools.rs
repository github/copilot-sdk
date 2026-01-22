//! Tool definition utilities for creating type-safe tools.

use schemars::JsonSchema;
use serde::de::DeserializeOwned;
use serde::Serialize;

use crate::error::Result;
use crate::types::{Tool, ToolInvocation, ToolResult};

/// Create a tool with automatic JSON schema generation from typed parameters.
///
/// The handler receives typed arguments (automatically deserialized from JSON)
/// and returns a result that can be a string, ToolResult, or any serializable type.
///
/// # Example
///
/// ```no_run
/// use copilot_sdk::{define_tool, ToolInvocation, ToolResult};
/// use serde::{Deserialize, Serialize};
/// use schemars::JsonSchema;
///
/// #[derive(Debug, Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     /// The city to get weather for.
///     city: String,
///     /// Temperature unit (celsius or fahrenheit).
///     unit: Option<String>,
/// }
///
/// let tool = define_tool(
///     "get_weather",
///     "Get weather for a city",
///     |params: GetWeatherParams, inv: ToolInvocation| async move {
///         Ok(format!("Weather in {}: 22°{}", params.city, params.unit.unwrap_or("C".to_string())))
///     },
/// );
/// ```
pub fn define_tool<T, F, Fut, R>(name: impl Into<String>, description: impl Into<String>, handler: F) -> Tool
where
    T: DeserializeOwned + JsonSchema + Send + 'static,
    F: Fn(T, ToolInvocation) -> Fut + Send + Sync + 'static,
    Fut: std::future::Future<Output = Result<R>> + Send + 'static,
    R: IntoToolResult + Send + 'static,
{
    use std::sync::Arc;

    let schema = generate_schema::<T>();
    let handler = Arc::new(handler);

    let handler_box: crate::types::ToolHandlerFn = Box::new(move |invocation| {
        let handler = Arc::clone(&handler);
        // Parse arguments into typed struct
        let args_result: std::result::Result<T, _> = serde_json::from_value(invocation.arguments.clone());

        Box::pin(async move {
            let params = args_result.map_err(|e| {
                crate::error::CopilotError::tool_execution(format!("failed to parse arguments: {}", e))
            })?;

            let result = handler(params, invocation).await?;
            Ok(result.into_tool_result())
        })
    });

    Tool {
        name: name.into(),
        description: description.into(),
        parameters: Some(schema),
        handler: Some(handler_box),
    }
}

/// Trait for converting values into ToolResult.
pub trait IntoToolResult {
    /// Convert this value into a ToolResult.
    fn into_tool_result(self) -> ToolResult;
}

impl IntoToolResult for ToolResult {
    fn into_tool_result(self) -> ToolResult {
        self
    }
}

impl IntoToolResult for String {
    fn into_tool_result(self) -> ToolResult {
        ToolResult::success(self)
    }
}

impl IntoToolResult for &str {
    fn into_tool_result(self) -> ToolResult {
        ToolResult::success(self)
    }
}

/// Wrapper for serializable types to convert to ToolResult.
pub struct JsonResult<T>(pub T);

impl<T: Serialize> IntoToolResult for JsonResult<T> {
    fn into_tool_result(self) -> ToolResult {
        match serde_json::to_string(&self.0) {
            Ok(s) => ToolResult::success(s),
            Err(e) => ToolResult::failure(format!("failed to serialize result: {}", e)),
        }
    }
}

/// Generate a JSON schema for a type.
fn generate_schema<T: JsonSchema>() -> serde_json::Value {
    let schema = schemars::schema_for!(T);
    serde_json::to_value(schema).unwrap_or(serde_json::Value::Null)
}

/// Helper macro for defining tools with less boilerplate.
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::tool;
///
/// #[derive(Debug, Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     city: String,
/// }
///
/// let weather_tool = tool!(
///     "get_weather",
///     "Get weather for a city",
///     |params: GetWeatherParams, _inv| async move {
///         Ok(format!("Weather in {}: sunny", params.city))
///     }
/// );
/// ```
#[macro_export]
macro_rules! tool {
    ($name:expr, $desc:expr, $handler:expr) => {
        $crate::define_tool($name, $desc, $handler)
    };
}

#[cfg(test)]
mod tests {
    use super::*;
    use schemars::JsonSchema;
    use serde::Deserialize;

    #[derive(Debug, Deserialize, JsonSchema)]
    struct TestParams {
        value: String,
    }

    #[tokio::test]
    async fn test_define_tool_creates_schema() {
        let tool = define_tool(
            "test_tool",
            "A test tool",
            |_params: TestParams, _inv: ToolInvocation| async move {
                Ok("result".to_string())
            },
        );

        assert_eq!(tool.name, "test_tool");
        assert_eq!(tool.description, "A test tool");
        assert!(tool.parameters.is_some());
        assert!(tool.handler.is_some());
    }
}
