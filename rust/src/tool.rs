//! Typed tool definition framework.
//!
//! Provides the [`ToolHandler`](crate::tool::ToolHandler) trait for implementing tools as named types,
//! and [`ToolHandlerRouter`](crate::tool::ToolHandlerRouter) for automatic dispatch of tool calls within a
//! [`SessionHandler`](crate::handler::SessionHandler).
//!
//! Enable the `derive` feature for `schema_for`, which generates JSON
//! Schema from Rust types via `schemars`.

use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
/// Re-export of [`schemars::JsonSchema`] for deriving tool parameter schemas.
#[cfg(feature = "derive")]
pub use schemars::JsonSchema;

use crate::Error;
use crate::handler::{HandlerEvent, HandlerResponse, SessionHandler};
use crate::types::{Tool, ToolInvocation, ToolResult, ToolResultExpanded};

/// Generate a JSON Schema [`Value`](serde_json::Value) from a Rust type.
///
/// Strips `$schema` and `title` root-level metadata so the output is ready
/// to use as [`Tool::parameters`].
///
/// # Example
///
/// ```rust
/// use copilot::tool::{schema_for, JsonSchema};
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
/// [`Tool::parameters`] map shape expected by the protocol.
///
/// Panics if the input is not a JSON object — tool parameter schemas
/// are always top-level objects (`{"type": "object", ...}`). Pair with
/// [`schema_for`] or a `serde_json::json!(...)` literal.
///
/// Use [`try_tool_parameters`] when the schema comes from dynamic input and
/// should return a recoverable error instead of panicking.
///
/// # Example
///
/// ```rust
/// use copilot::tool::tool_parameters;
/// use copilot::Tool;
///
/// let tool = Tool {
///     name: "ping".to_string(),
///     namespaced_name: None,
///     description: "ping the server".to_string(),
///     parameters: tool_parameters(serde_json::json!({"type": "object"})),
///     instructions: None,
///     ..Default::default()
/// };
/// # let _ = tool;
/// ```
pub fn tool_parameters(schema: serde_json::Value) -> HashMap<String, serde_json::Value> {
    try_tool_parameters(schema).expect("tool parameter schema must be a JSON object")
}

/// Fallible variant of [`tool_parameters`] for callers handling dynamic schema input.
pub fn try_tool_parameters(
    schema: serde_json::Value,
) -> Result<HashMap<String, serde_json::Value>, serde_json::Error> {
    serde_json::from_value(schema)
}

/// A client-defined tool with its handler logic.
///
/// Implement this trait for each tool you expose to the Copilot agent.
/// The struct is a named type — visible in stack traces and navigable
/// via "go to definition" — unlike closure-based alternatives.
///
/// # Example
///
/// ```rust,ignore
/// use copilot::tool::{schema_for, tool_parameters, JsonSchema, ToolHandler};
/// use copilot::{Error, Tool, ToolInvocation, ToolResult};
/// use serde::Deserialize;
/// use async_trait::async_trait;
///
/// #[derive(Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     /// City name
///     city: String,
///     /// Temperature unit
///     unit: Option<String>,
/// }
///
/// struct GetWeatherTool;
///
/// #[async_trait]
/// impl ToolHandler for GetWeatherTool {
///     fn tool(&self) -> Tool {
///         Tool {
///             name: "get_weather".to_string(),
///             namespaced_name: None,
///             description: "Get weather for a city".to_string(),
///             parameters: tool_parameters(schema_for::<GetWeatherParams>()),
///             instructions: None,
///             ..Default::default()
///         }
///     }
///
///     async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
///         let params: GetWeatherParams = serde_json::from_value(inv.arguments)?;
///         Ok(ToolResult::Text(format!("Weather in {}: sunny", params.city)))
///     }
/// }
/// ```
#[async_trait]
pub trait ToolHandler: Send + Sync {
    /// The tool definition sent to the CLI during session creation.
    fn tool(&self) -> Tool;

    /// Handle a tool invocation from the agent.
    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error>;
}

/// Define a tool from an async closure that takes a typed, `JsonSchema`-derived
/// parameter struct.
///
/// The returned `Box<dyn ToolHandler>` plugs directly into
/// [`ToolHandlerRouter::new`]. JSON Schema for the parameter type is generated
/// via [`schema_for`] at construction time.
///
/// For tools that need access to the raw [`ToolInvocation`] (invocation id,
/// correlation metadata) or that build their schema dynamically, implement
/// [`ToolHandler`] by hand instead.
///
/// # Example
///
/// ```rust,no_run
/// use copilot::tool::{define_tool, JsonSchema};
/// use copilot::ToolResult;
/// use serde::Deserialize;
///
/// #[derive(Deserialize, JsonSchema)]
/// struct GetWeatherParams {
///     /// City name
///     city: String,
/// }
///
/// let tool = define_tool(
///     "get_weather",
///     "Get weather for a city",
///     |params: GetWeatherParams| async move {
///         Ok(ToolResult::Text(format!("Sunny in {}", params.city)))
///     },
/// );
/// # let _ = tool;
/// ```
#[cfg(feature = "derive")]
pub fn define_tool<P, F, Fut>(
    name: impl Into<String>,
    description: impl Into<String>,
    handler: F,
) -> Box<dyn ToolHandler>
where
    P: schemars::JsonSchema + serde::de::DeserializeOwned + Send + 'static,
    F: Fn(P) -> Fut + Send + Sync + 'static,
    Fut: std::future::Future<Output = Result<ToolResult, Error>> + Send + 'static,
{
    struct FnTool<P, F> {
        name: String,
        description: String,
        parameters: HashMap<String, serde_json::Value>,
        handler: F,
        _marker: std::marker::PhantomData<fn(P)>,
    }

    #[async_trait]
    impl<P, F, Fut> ToolHandler for FnTool<P, F>
    where
        P: schemars::JsonSchema + serde::de::DeserializeOwned + Send + 'static,
        F: Fn(P) -> Fut + Send + Sync + 'static,
        Fut: std::future::Future<Output = Result<ToolResult, Error>> + Send + 'static,
    {
        fn tool(&self) -> Tool {
            Tool {
                name: self.name.clone(),
                description: self.description.clone(),
                parameters: self.parameters.clone(),
                ..Default::default()
            }
        }

        async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
            let params: P = serde_json::from_value(invocation.arguments)?;
            (self.handler)(params).await
        }
    }

    Box::new(FnTool {
        name: name.into(),
        description: description.into(),
        parameters: tool_parameters(schema_for::<P>()),
        handler,
        _marker: std::marker::PhantomData,
    })
}

/// A [`SessionHandler`] that dispatches tool calls to registered
/// [`ToolHandler`] implementations by name.
///
/// For tool calls matching a registered handler, the handler is invoked
/// directly. All other events (permissions, user input, unrecognized tools)
/// are forwarded to the inner handler.
///
/// # Example
///
/// ```rust,no_run
/// use std::sync::Arc;
/// use copilot::handler::ApproveAllHandler;
/// use copilot::tool::ToolHandlerRouter;
///
/// let router = ToolHandlerRouter::new(
///     vec![/* Box::new(MyTool), ... */],
///     Arc::new(ApproveAllHandler),
/// );
///
/// // Use router.tools() in SessionConfig
/// // Use Arc::new(router) as the session handler
/// ```
pub struct ToolHandlerRouter {
    handlers: HashMap<String, Box<dyn ToolHandler>>,
    inner: Arc<dyn SessionHandler>,
}

impl std::fmt::Debug for ToolHandlerRouter {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let mut tools: Vec<_> = self.handlers.keys().collect();
        tools.sort();
        f.debug_struct("ToolHandlerRouter")
            .field("tool_count", &self.handlers.len())
            .field("tools", &tools)
            .finish()
    }
}

impl ToolHandlerRouter {
    /// Create a router from tool handler impls and a fallback handler.
    ///
    /// Call [`tools()`](Self::tools) to get the tool definitions for
    /// [`SessionConfig::tools`](crate::SessionConfig::tools).
    pub fn new(tools: Vec<Box<dyn ToolHandler>>, inner: Arc<dyn SessionHandler>) -> Self {
        let mut handlers = HashMap::new();
        for tool in tools {
            handlers.insert(tool.tool().name.clone(), tool);
        }
        Self { handlers, inner }
    }

    /// Tool definitions for [`SessionConfig::tools`](crate::SessionConfig::tools).
    pub fn tools(&self) -> Vec<Tool> {
        self.handlers.values().map(|h| h.tool()).collect()
    }
}

#[async_trait]
impl SessionHandler for ToolHandlerRouter {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::ExternalTool { invocation } => {
                if let Some(handler) = self.handlers.get(&invocation.tool_name) {
                    match handler.call(invocation).await {
                        Ok(result) => HandlerResponse::ToolResult(result),
                        Err(e) => {
                            let msg = e.to_string();
                            HandlerResponse::ToolResult(ToolResult::Expanded(ToolResultExpanded {
                                text_result_for_llm: msg.clone(),
                                result_type: "failure".to_string(),
                                session_log: None,
                                error: Some(msg),
                            }))
                        }
                    }
                } else {
                    self.inner
                        .on_event(HandlerEvent::ExternalTool { invocation })
                        .await
                }
            }
            other => self.inner.on_event(other).await,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::types::{PermissionRequestData, RequestId, SessionId};

    struct EchoTool;

    #[async_trait]
    impl ToolHandler for EchoTool {
        fn tool(&self) -> Tool {
            Tool {
                name: "echo".to_string(),
                namespaced_name: None,
                description: "Echo the input".to_string(),
                parameters: tool_parameters(serde_json::json!({"type": "object"})),
                instructions: None,
                ..Default::default()
            }
        }

        async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
            Ok(ToolResult::Text(inv.arguments.to_string()))
        }
    }

    #[test]
    fn tool_handler_returns_tool_definition() {
        let tool = EchoTool;
        let def = tool.tool();
        assert_eq!(def.name, "echo");
        assert_eq!(def.description, "Echo the input");
        assert!(def.parameters.contains_key("type"));
    }

    #[test]
    fn try_tool_parameters_rejects_non_object_schema() {
        let err = try_tool_parameters(serde_json::json!(["not", "an", "object"]))
            .expect_err("non-object schemas should be rejected");

        assert!(err.is_data());
    }

    #[tokio::test]
    async fn tool_handler_call_returns_result() {
        let tool = EchoTool;
        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "echo".to_string(),
            arguments: serde_json::json!({"msg": "hello"}),
        };

        let result = tool.call(inv).await.unwrap();
        match result {
            ToolResult::Text(s) => assert!(s.contains("hello")),
            _ => panic!("expected Text result"),
        }
    }

    #[cfg(feature = "derive")]
    #[tokio::test]
    async fn define_tool_builds_schema_and_dispatches() {
        use serde::Deserialize;

        #[derive(Deserialize, schemars::JsonSchema)]
        struct Params {
            city: String,
        }

        let tool = define_tool(
            "weather",
            "Get the weather for a city",
            |params: Params| async move { Ok(ToolResult::Text(format!("sunny in {}", params.city))) },
        );

        let def = tool.tool();
        assert_eq!(def.name, "weather");
        assert_eq!(def.description, "Get the weather for a city");
        assert_eq!(def.parameters["type"], "object");
        assert!(def.parameters["properties"]["city"].is_object());

        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "weather".to_string(),
            arguments: serde_json::json!({"city": "Seattle"}),
        };
        match tool.call(inv).await.unwrap() {
            ToolResult::Text(s) => assert_eq!(s, "sunny in Seattle"),
            _ => panic!("expected Text result"),
        }
    }

    #[tokio::test]
    async fn router_dispatches_to_correct_handler() {
        struct ToolA;
        #[async_trait]
        impl ToolHandler for ToolA {
            fn tool(&self) -> Tool {
                Tool {
                    name: "tool_a".to_string(),
                    namespaced_name: None,
                    description: "A".to_string(),
                    parameters: HashMap::new(),
                    instructions: None,
                    ..Default::default()
                }
            }

            async fn call(&self, _inv: ToolInvocation) -> Result<ToolResult, Error> {
                Ok(ToolResult::Text("a_result".to_string()))
            }
        }

        struct ToolB;
        #[async_trait]
        impl ToolHandler for ToolB {
            fn tool(&self) -> Tool {
                Tool {
                    name: "tool_b".to_string(),
                    namespaced_name: None,
                    description: "B".to_string(),
                    parameters: HashMap::new(),
                    instructions: None,
                    ..Default::default()
                }
            }

            async fn call(&self, _inv: ToolInvocation) -> Result<ToolResult, Error> {
                Ok(ToolResult::Text("b_result".to_string()))
            }
        }

        let router = ToolHandlerRouter::new(
            vec![Box::new(ToolA), Box::new(ToolB)],
            Arc::new(crate::handler::ApproveAllHandler),
        );

        let tools = router.tools();
        assert_eq!(tools.len(), 2);

        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "tool_b".to_string(),
            arguments: serde_json::json!({}),
        };

        let response = router
            .on_event(HandlerEvent::ExternalTool { invocation: inv })
            .await;
        match response {
            HandlerResponse::ToolResult(ToolResult::Text(s)) => assert_eq!(s, "b_result"),
            _ => panic!("expected ToolResult::Text"),
        }
    }

    #[tokio::test]
    async fn router_falls_through_for_unknown_tool() {
        use std::sync::atomic::{AtomicBool, Ordering};

        struct FallbackHandler {
            called: AtomicBool,
        }
        #[async_trait]
        impl SessionHandler for FallbackHandler {
            async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
                if let HandlerEvent::ExternalTool { .. } = event {
                    self.called.store(true, Ordering::Relaxed);
                }
                HandlerResponse::ToolResult(ToolResult::Text("fallback".to_string()))
            }
        }

        let fallback = Arc::new(FallbackHandler {
            called: AtomicBool::new(false),
        });
        let router = ToolHandlerRouter::new(vec![], fallback.clone());

        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "unknown".to_string(),
            arguments: serde_json::json!({}),
        };

        let response = router
            .on_event(HandlerEvent::ExternalTool { invocation: inv })
            .await;
        assert!(fallback.called.load(Ordering::Relaxed));
        match response {
            HandlerResponse::ToolResult(ToolResult::Text(s)) => assert_eq!(s, "fallback"),
            _ => panic!("expected fallback result"),
        }
    }

    #[tokio::test]
    async fn router_returns_failure_on_handler_error() {
        struct FailTool;
        #[async_trait]
        impl ToolHandler for FailTool {
            fn tool(&self) -> Tool {
                Tool {
                    name: "bad_tool".to_string(),
                    namespaced_name: None,
                    description: "Always fails".to_string(),
                    parameters: HashMap::new(),
                    instructions: None,
                    ..Default::default()
                }
            }

            async fn call(&self, _inv: ToolInvocation) -> Result<ToolResult, Error> {
                Err(Error::Rpc {
                    code: -1,
                    message: "intentional failure".to_string(),
                })
            }
        }

        let router = ToolHandlerRouter::new(
            vec![Box::new(FailTool)],
            Arc::new(crate::handler::ApproveAllHandler),
        );

        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "bad_tool".to_string(),
            arguments: serde_json::json!({}),
        };

        let response = router
            .on_event(HandlerEvent::ExternalTool { invocation: inv })
            .await;
        match response {
            HandlerResponse::ToolResult(ToolResult::Expanded(exp)) => {
                assert_eq!(exp.result_type, "failure");
                assert!(exp.error.unwrap().contains("intentional failure"));
            }
            _ => panic!("expected expanded failure result"),
        }
    }

    #[tokio::test]
    async fn router_forwards_non_tool_events() {
        use crate::handler::PermissionResult;

        struct PermHandler;
        #[async_trait]
        impl SessionHandler for PermHandler {
            async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
                match event {
                    HandlerEvent::PermissionRequest { .. } => {
                        HandlerResponse::Permission(PermissionResult::Denied)
                    }
                    _ => HandlerResponse::Ok,
                }
            }
        }

        let router = ToolHandlerRouter::new(vec![], Arc::new(PermHandler));

        let response = router
            .on_event(HandlerEvent::PermissionRequest {
                session_id: SessionId::from("s1"),
                request_id: RequestId::new("r1"),
                data: PermissionRequestData {
                    extra: serde_json::json!({}),
                },
            })
            .await;
        assert!(matches!(
            response,
            HandlerResponse::Permission(PermissionResult::Denied)
        ));
    }

    // Tests requiring `schemars` (the `derive` feature).
    #[cfg(feature = "derive")]
    mod derive_tests {
        use serde::Deserialize;

        use super::super::*;
        use crate::SessionId;

        #[derive(Deserialize, schemars::JsonSchema)]
        struct GetWeatherParams {
            /// City name to get weather for.
            city: String,
            /// Temperature unit (celsius or fahrenheit).
            unit: Option<String>,
        }

        #[test]
        fn schema_for_generates_clean_schema() {
            let schema = schema_for::<GetWeatherParams>();
            assert_eq!(schema["type"], "object");
            assert!(schema["properties"]["city"].is_object());
            assert!(schema["properties"]["unit"].is_object());
            // city is required (non-Option), unit is not
            let required = schema["required"].as_array().unwrap();
            assert!(required.contains(&serde_json::json!("city")));
            assert!(!required.contains(&serde_json::json!("unit")));
            // Root-level metadata stripped
            assert!(schema.get("$schema").is_none());
            assert!(schema.get("title").is_none());
        }

        struct GetWeatherTool;

        #[async_trait]
        impl ToolHandler for GetWeatherTool {
            fn tool(&self) -> Tool {
                Tool {
                    name: "get_weather".to_string(),
                    namespaced_name: None,
                    description: "Get weather for a city".to_string(),
                    parameters: tool_parameters(schema_for::<GetWeatherParams>()),
                    instructions: None,
                    ..Default::default()
                }
            }

            async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
                let params: GetWeatherParams = serde_json::from_value(inv.arguments)?;
                Ok(ToolResult::Text(format!(
                    "{} {}",
                    params.city,
                    params.unit.unwrap_or_default()
                )))
            }
        }

        #[test]
        fn tool_handler_with_schema_for() {
            let tool = GetWeatherTool;
            let def = tool.tool();
            assert_eq!(def.name, "get_weather");
            let schema = serde_json::to_value(&def.parameters).expect("serialize tool parameters");
            assert_eq!(schema["type"], "object");
            assert!(schema["properties"]["city"].is_object());
        }

        #[tokio::test]
        async fn tool_handler_deserializes_typed_params() {
            let tool = GetWeatherTool;
            let inv = ToolInvocation {
                session_id: SessionId::from("s1"),
                tool_call_id: "tc1".to_string(),
                tool_name: "get_weather".to_string(),
                arguments: serde_json::json!({"city": "Seattle", "unit": "celsius"}),
            };

            let result = tool.call(inv).await.unwrap();
            match result {
                ToolResult::Text(s) => assert_eq!(s, "Seattle celsius"),
                _ => panic!("expected Text result"),
            }
        }

        #[tokio::test]
        async fn tool_handler_returns_error_on_bad_params() {
            let tool = GetWeatherTool;
            let inv = ToolInvocation {
                session_id: SessionId::from("s1"),
                tool_call_id: "tc1".to_string(),
                tool_name: "get_weather".to_string(),
                arguments: serde_json::json!({"wrong_field": 42}),
            };

            let err = tool.call(inv).await.unwrap_err();
            assert!(matches!(err, Error::Json(_)));
        }

        #[tokio::test]
        async fn router_with_schema_for_tools() {
            let router = ToolHandlerRouter::new(
                vec![Box::new(GetWeatherTool)],
                Arc::new(crate::handler::ApproveAllHandler),
            );

            let tools = router.tools();
            assert_eq!(tools.len(), 1);
            assert_eq!(tools[0].name, "get_weather");

            let inv = ToolInvocation {
                session_id: SessionId::from("s1"),
                tool_call_id: "tc1".to_string(),
                tool_name: "get_weather".to_string(),
                arguments: serde_json::json!({"city": "Portland"}),
            };

            let response = router
                .on_event(HandlerEvent::ExternalTool { invocation: inv })
                .await;
            match response {
                HandlerResponse::ToolResult(ToolResult::Text(s)) => {
                    assert!(s.contains("Portland"));
                }
                _ => panic!("expected ToolResult::Text"),
            }
        }
    }
}
