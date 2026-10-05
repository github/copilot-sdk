//! Typed tool definition framework.
//!
//! Provides the [`ToolHandler`](crate::tool::ToolHandler) trait for
//! implementing tools as named types. Attach a handler to a
//! [`Tool`](crate::types::Tool) via
//! [`Tool::with_handler`](crate::types::Tool::with_handler), then install
//! the resulting tools on a session via
//! [`SessionConfig::with_tools`](crate::types::SessionConfig::with_tools).
//! The SDK builds an internal name-keyed registry from the handlers and
//! dispatches to the matching handler when the CLI broadcasts
//! `external_tool.requested`.
//!
//! Enable the `derive` feature for `schema_for`, which generates JSON
//! Schema from Rust types via `schemars`.

use async_trait::async_trait;
use indexmap::IndexMap;
/// Re-export of [`schemars::JsonSchema`] for deriving tool parameter schemas.
#[cfg(feature = "derive")]
pub use schemars::JsonSchema;

use crate::Error;
#[cfg(any(feature = "derive", test))]
use crate::types::Tool;
use crate::types::{ToolBinaryResult, ToolInvocation, ToolResult, ToolResultExpanded};

/// Generate a JSON Schema [`Value`](serde_json::Value) from a Rust type.
///
/// Strips `$schema` and `title` root-level metadata so the output is ready
/// to use as [`Tool::parameters`].
///
/// # Example
///
/// ```rust
/// use github_copilot_sdk::tool::{schema_for, JsonSchema};
///
/// #[derive(JsonSchema)]
/// struct Params {
///     /// City name
///     city: String,
/// }
///
/// let schema = schema_for::<Params>();
/// assert_eq!(schema["type"], "object");
/// assert!(schema["properties"]["city"].is_object());
/// ```
#[cfg(feature = "derive")]
pub fn schema_for<T: schemars::JsonSchema>() -> serde_json::Value {
    let schema = schemars::schema_for!(T);
    let mut value = serde_json::to_value(schema).expect("JSON Schema serialization cannot fail");
    if let Some(obj) = value.as_object_mut() {
        obj.remove("$schema");
        obj.remove("title");
    }
    value
}

/// Convert a JSON Schema [`Value`](serde_json::Value) into the
/// [`Tool::parameters`](crate::types::Tool::parameters) map shape
/// expected by the protocol.
///
/// Panics if the input is not a JSON object — tool parameter schemas
/// are always top-level objects (`{"type": "object", ...}`). Pair with
/// `schema_for` (available with the `derive` feature) or a
/// `serde_json::json!(...)` literal.
///
/// Use [`try_tool_parameters`] when the schema comes from dynamic input and
/// should return a recoverable error instead of panicking.
///
/// # Example
///
/// ```rust
/// use github_copilot_sdk::tool::tool_parameters;
/// use github_copilot_sdk::Tool;
///
/// let mut tool = Tool::default();
/// tool.name = "ping".to_string();
/// tool.description = "ping the server".to_string();
/// tool.parameters = tool_parameters(serde_json::json!({"type": "object"}));
/// # let _ = tool;
/// ```
pub fn tool_parameters(schema: serde_json::Value) -> IndexMap<String, serde_json::Value> {
    try_tool_parameters(schema).expect("tool parameter schema must be a JSON object")
}

/// Fallible variant of [`tool_parameters`] for callers handling dynamic schema input.
pub fn try_tool_parameters(
    schema: serde_json::Value,
) -> Result<IndexMap<String, serde_json::Value>, serde_json::Error> {
    serde_json::from_value(schema)
}

/// Convert an MCP `CallToolResult` JSON value into a Copilot tool result.
///
/// Returns `None` when the value is not shaped like a `CallToolResult`.
pub fn convert_mcp_call_tool_result(value: &serde_json::Value) -> Option<ToolResult> {
    let content = value.get("content")?.as_array()?;
    let mut text_parts = Vec::new();
    let mut binary_results = Vec::new();

    for block in content {
        match block.get("type").and_then(serde_json::Value::as_str) {
            Some("text") => {
                if let Some(text) = block.get("text").and_then(serde_json::Value::as_str) {
                    text_parts.push(text.to_string());
                }
            }
            Some("image") => {
                let data = block
                    .get("data")
                    .and_then(serde_json::Value::as_str)
                    .filter(|s| !s.is_empty());
                let mime_type = block
                    .get("mimeType")
                    .and_then(serde_json::Value::as_str)
                    .filter(|s| !s.is_empty());
                if let (Some(data), Some(mime_type)) = (data, mime_type) {
                    binary_results.push(ToolBinaryResult {
                        data: data.to_string(),
                        mime_type: mime_type.to_string(),
                        r#type: "image".to_string(),
                        description: None,
                    });
                }
            }
            Some("resource") => {
                let Some(resource) = block.get("resource").and_then(serde_json::Value::as_object)
                else {
                    continue;
                };
                if let Some(text) = resource
                    .get("text")
                    .and_then(serde_json::Value::as_str)
                    .filter(|s| !s.is_empty())
                {
                    text_parts.push(text.to_string());
                }
                if let Some(blob) = resource
                    .get("blob")
                    .and_then(serde_json::Value::as_str)
                    .filter(|s| !s.is_empty())
                {
                    let mime_type = resource
                        .get("mimeType")
                        .and_then(serde_json::Value::as_str)
                        .filter(|s| !s.is_empty())
                        .unwrap_or("application/octet-stream");
                    let description = resource
                        .get("uri")
                        .and_then(serde_json::Value::as_str)
                        .filter(|s| !s.is_empty())
                        .map(ToString::to_string);
                    binary_results.push(ToolBinaryResult {
                        data: blob.to_string(),
                        mime_type: mime_type.to_string(),
                        r#type: "resource".to_string(),
                        description,
                    });
                }
            }
            _ => {}
        }
    }

    Some(ToolResult::Expanded(ToolResultExpanded {
        text_result_for_llm: text_parts.join("\n"),
        result_type: if value.get("isError").and_then(serde_json::Value::as_bool) == Some(true) {
            "failure".to_string()
        } else {
            "success".to_string()
        },
        binary_results_for_llm: (!binary_results.is_empty()).then_some(binary_results),
        session_log: None,
        error: None,
        tool_telemetry: None,
        tool_references: None,
    }))
}

/// A client-defined tool's runtime implementation.
///
/// Implement this trait when you want to bind a Rust function to a tool
/// name and have the SDK dispatch matching `external_tool.requested`
/// broadcasts to it. Attach the impl to a [`Tool`](crate::types::Tool)
/// via [`Tool::with_handler`](crate::types::Tool::with_handler).
///
/// Named handler types (e.g. `struct MyTool;`) are visible in stack
/// traces and navigable via "go to definition", which is preferable to
/// closure-based alternatives for non-trivial tools. For trivial tools,
/// the `define_tool` helper function (available with the `derive`
/// feature) wraps a free `async fn` or closure into a [`Tool`](crate::types::Tool) with
/// the handler already attached.
///
/// # Example
///
/// ```rust,ignore
/// use github_copilot_sdk::tool::{schema_for, JsonSchema, ToolHandler};
/// use github_copilot_sdk::types::{Tool, ToolInvocation};
/// use github_copilot_sdk::{Error, ToolResult};
/// use serde::Deserialize;
/// use async_trait::async_trait;
/// use std::sync::Arc;
///
/// #[derive(Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     /// City name
///     city: String,
/// }
///
/// struct GetWeather;
///
/// #[async_trait]
/// impl ToolHandler for GetWeather {
///     async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
///         let params: GetWeatherParams = serde_json::from_value(inv.arguments)?;
///         Ok(ToolResult::Text(format!("Weather in {}: sunny", params.city)))
///     }
/// }
///
/// // Build the Tool declaration with the handler attached:
/// let tool = Tool::new("get_weather")
///     .with_description("Get weather for a city")
///     .with_parameters(schema_for::<GetWeatherParams>())
///     .with_handler(Arc::new(GetWeather));
/// ```
#[async_trait]
pub trait ToolHandler: Send + Sync + 'static {
    /// Handle a tool invocation from the agent.
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error>;
}

/// Define a [`Tool`] from an async function (or closure) that takes a typed,
/// `JsonSchema`-derived parameter struct.
///
/// The returned [`Tool`] carries an attached handler ready to install on a
/// session via [`SessionConfig::with_tools`](crate::types::SessionConfig::with_tools).
/// JSON Schema for the parameter type is generated via [`schema_for`] at
/// construction time.
///
/// The handler bound (`Fn(ToolInvocation, P) -> Fut + Send + Sync + 'static`)
/// accepts both bare `async fn` items and closures — the same shape as
/// [`tower::service_fn`][tower-service-fn] and
/// [`hyper::service::service_fn`][hyper-service-fn]. Prefer a free `async fn`
/// for non-trivial tools so it shows up in stack traces by name.
///
/// The closure receives the full [`ToolInvocation`] alongside the deserialized
/// parameters so handlers can use `inv.session_id`, `inv.tool_call_id`, or
/// other invocation metadata. Handlers that don't need that metadata can
/// destructure with `|_inv, params|`.
///
/// # Example
///
/// ```rust,no_run
/// use github_copilot_sdk::tool::{define_tool, JsonSchema};
/// use github_copilot_sdk::types::ToolInvocation;
/// use github_copilot_sdk::{Error, ToolResult};
/// use serde::Deserialize;
///
/// #[derive(Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     /// City name
///     city: String,
/// }
///
/// async fn get_weather(
///     inv: ToolInvocation,
///     params: GetWeatherParams,
/// ) -> Result<ToolResult, Error> {
///     let _ = inv.session_id;
///     Ok(ToolResult::Text(format!("Sunny in {}", params.city)))
/// }
///
/// // Pass a free async fn — preferred for non-trivial tools.
/// let tool = define_tool("get_weather", "Get weather for a city", get_weather);
///
/// // ...or an inline closure when the body is trivial.
/// let tool = define_tool(
///     "echo",
///     "Echo the input",
///     |_inv, params: GetWeatherParams| async move {
///         Ok(ToolResult::Text(params.city))
///     },
/// );
/// # let _ = tool;
/// ```
///
/// [tower-service-fn]: https://docs.rs/tower/latest/tower/fn.service_fn.html
/// [hyper-service-fn]: https://docs.rs/hyper/latest/hyper/service/fn.service_fn.html
#[cfg(feature = "derive")]
pub fn define_tool<P, F, Fut>(
    name: impl Into<String>,
    description: impl Into<String>,
    handler: F,
) -> Tool
where
    P: schemars::JsonSchema + serde::de::DeserializeOwned + Send + 'static,
    F: Fn(ToolInvocation, P) -> Fut + Send + Sync + 'static,
    Fut: std::future::Future<Output = Result<ToolResult, Error>> + Send + 'static,
{
    struct FnHandler<P, F> {
        handler: F,
        _marker: std::marker::PhantomData<fn(P)>,
    }

    #[async_trait]
    impl<P, F, Fut> ToolHandler for FnHandler<P, F>
    where
        P: schemars::JsonSchema + serde::de::DeserializeOwned + Send + 'static,
        F: Fn(ToolInvocation, P) -> Fut + Send + Sync + 'static,
        Fut: std::future::Future<Output = Result<ToolResult, Error>> + Send + 'static,
    {
        async fn call(&self, mut invocation: ToolInvocation) -> Result<ToolResult, Error> {
            let arguments = std::mem::take(&mut invocation.arguments);
            let params: P = serde_json::from_value(arguments)?;
            (self.handler)(invocation, params).await
        }
    }

    Tool {
        name: name.into(),
        description: description.into(),
        parameters: tool_parameters(schema_for::<P>()),
        ..Default::default()
    }
    .with_handler(std::sync::Arc::new(FnHandler {
        handler,
        _marker: std::marker::PhantomData,
    }))
}

/// Define a declaration-only [`Tool`] with a JSON Schema derived from `P`.
///
/// Equivalent to [`define_tool`] but produces a [`Tool`] with no attached
/// handler — useful when another connected client services this tool, or
/// when you only need to advertise the schema for capability negotiation.
///
/// # Example
///
/// ```rust,no_run
/// use github_copilot_sdk::tool::{define_tool_declaration, JsonSchema};
/// use serde::Deserialize;
///
/// #[derive(Deserialize, JsonSchema)]
/// struct Params { query: String }
///
/// let declared = define_tool_declaration::<Params>(
///     "legacy_thing",
///     "Handled by another connected client",
/// );
/// # let _ = declared;
/// ```
#[cfg(feature = "derive")]
pub fn define_tool_declaration<P>(name: impl Into<String>, description: impl Into<String>) -> Tool
where
    P: schemars::JsonSchema,
{
    Tool {
        name: name.into(),
        description: description.into(),
        parameters: tool_parameters(schema_for::<P>()),
        ..Default::default()
    }
}

#[cfg(test)]
mod tests;
