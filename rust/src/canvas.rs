//! Canvas declarations, provider callbacks, and host-side canvas RPC types.
//!
//! <div class="warning">
//!
//! **Experimental.** Canvas types are part of an experimental wire-protocol surface
//! and may change or be removed in future SDK or CLI releases.
//!
//! </div>

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::generated::api_types::CanvasAction;

/// JSON Schema object used for canvas inputs and canvas-scoped tools.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type CanvasJsonSchema = serde_json::Map<String, Value>;

/// Declarative metadata for a single canvas, sent over the wire on
/// `session.create` / `session.resume`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct CanvasDeclaration {
    /// Canvas identifier, unique within the declaring connection.
    pub id: String,
    /// Human-readable name shown in host UI and canvas pickers.
    pub display_name: String,
    /// Short, single-sentence description shown to the agent in canvas catalogs.
    pub description: String,
    /// JSON Schema for the `input` payload accepted by `canvas.open`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<Value>,
    /// Agent-callable actions this canvas exposes.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub actions: Option<Vec<CanvasAction>>,
}

impl CanvasDeclaration {
    /// Construct a canvas declaration with the required fields set.
    pub fn new(
        id: impl Into<String>,
        display_name: impl Into<String>,
        description: impl Into<String>,
    ) -> Self {
        Self {
            id: id.into(),
            display_name: display_name.into(),
            description: description.into(),
            input_schema: None,
            actions: None,
        }
    }

    /// Set the description surfaced in discovery and agent context.
    pub fn with_description(mut self, description: impl Into<String>) -> Self {
        self.description = description.into();
        self
    }
}

/// Structured error returned from canvas handlers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasError {
    /// Machine-readable error code.
    pub code: String,
    /// Human-readable message.
    pub message: String,
}

impl std::fmt::Display for CanvasError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}: {}", self.code, self.message)
    }
}

impl std::error::Error for CanvasError {}

impl CanvasError {
    /// Construct a new error envelope with the given code and message.
    pub fn new(code: impl Into<String>, message: impl Into<String>) -> Self {
        Self {
            code: code.into(),
            message: message.into(),
        }
    }

    /// Default error returned when a custom action has no handler.
    pub fn no_handler() -> Self {
        Self::new(
            "canvas_action_no_handler",
            "No handler implemented for this canvas action",
        )
    }
}

/// Result alias for canvas handler methods.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type CanvasResult<T> = Result<T, CanvasError>;

/// Provider-side canvas lifecycle handler.
///
/// <div class="warning">
///
/// **Experimental.** This trait is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
///
/// A session installs a single [`CanvasHandler`] (via
/// [`SessionConfig::with_canvas_handler`](crate::types::SessionConfig::with_canvas_handler)).
/// The handler receives every inbound `canvas.open` / `canvas.close` /
/// `canvas.action.invoke` JSON-RPC request the runtime issues for this
/// session and decides — typically by inspecting
/// [`CanvasProviderOpenRequest::canvas_id`](crate::generated::api_types::CanvasProviderOpenRequest::canvas_id)
/// — which application-side canvas should handle the call.
///
/// The SDK does not maintain a per-canvas registry; multiplexing across
/// declared canvases is the implementor's responsibility.
#[async_trait]
pub trait CanvasHandler: Send + Sync {
    /// Open a new canvas instance.
    async fn on_open(
        &self,
        ctx: crate::generated::api_types::CanvasProviderOpenRequest,
    ) -> CanvasResult<crate::generated::api_types::CanvasProviderOpenResult>;

    /// Handle a non-lifecycle action declared by the canvas.
    async fn on_action(
        &self,
        _ctx: crate::generated::api_types::CanvasProviderInvokeActionRequest,
    ) -> CanvasResult<Value> {
        Err(CanvasError::no_handler())
    }

    /// Canvas was closed by the user or agent.
    async fn on_close(
        &self,
        _ctx: crate::generated::api_types::CanvasProviderCloseRequest,
    ) -> CanvasResult<()> {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;
    use crate::generated::api_types::{
        CanvasProviderInvokeActionRequest, CanvasProviderOpenRequest, CanvasProviderOpenResult,
    };
    use crate::types::SessionId;

    struct EchoHandler;

    #[async_trait]
    impl CanvasHandler for EchoHandler {
        async fn on_open(
            &self,
            ctx: CanvasProviderOpenRequest,
        ) -> CanvasResult<CanvasProviderOpenResult> {
            Ok(CanvasProviderOpenResult {
                url: Some(format!("https://example.test/{}", ctx.canvas_id)),
                title: Some("Echo".to_string()),
                status: Some("ready".to_string()),
            })
        }

        async fn on_action(&self, ctx: CanvasProviderInvokeActionRequest) -> CanvasResult<Value> {
            Ok(json!({ "echoed": ctx.action_name, "input": ctx.input }))
        }
    }

    #[test]
    fn declaration_serializes_camel_case_and_skips_none() {
        let decl = CanvasDeclaration {
            id: "counter".to_string(),
            display_name: "Counter".to_string(),
            description: "Count things".to_string(),
            input_schema: None,
            actions: Some(vec![CanvasAction {
                name: "increment".to_string(),
                description: Some("bump".to_string()),
                input_schema: None,
            }]),
        };

        let value = serde_json::to_value(&decl).unwrap();

        assert_eq!(value["id"], "counter");
        assert_eq!(value["displayName"], "Counter");
        assert_eq!(value["description"], "Count things");
        assert_eq!(value["actions"][0]["name"], "increment");
    }

    #[tokio::test]
    async fn handler_on_open_returns_response() {
        let handler = EchoHandler;
        let response = handler
            .on_open(CanvasProviderOpenRequest {
                session_id: SessionId::from("s1"),
                extension_id: "project:echo".to_string(),
                canvas_id: "echo".to_string(),
                instance_id: "echo-1".to_string(),
                input: Some(json!({ "x": 1 })),
                host: None,
                session: None,
            })
            .await
            .unwrap();

        assert_eq!(response.url.as_deref(), Some("https://example.test/echo"));
        assert_eq!(response.title.as_deref(), Some("Echo"));
        assert_eq!(response.status.as_deref(), Some("ready"));
    }

    #[tokio::test]
    async fn handler_on_action_returns_value() {
        let handler = EchoHandler;
        let result = handler
            .on_action(CanvasProviderInvokeActionRequest {
                session_id: SessionId::from("s1"),
                extension_id: "project:echo".to_string(),
                canvas_id: "echo".to_string(),
                instance_id: "inst-1".to_string(),
                action_name: "shout".to_string(),
                input: Some(json!("hi")),
                host: None,
                session: None,
            })
            .await
            .unwrap();

        assert_eq!(result["echoed"], "shout");
        assert_eq!(result["input"], "hi");
    }

    #[tokio::test]
    async fn default_on_action_returns_no_handler_error() {
        struct OpenOnly;
        #[async_trait]
        impl CanvasHandler for OpenOnly {
            async fn on_open(
                &self,
                _ctx: CanvasProviderOpenRequest,
            ) -> CanvasResult<CanvasProviderOpenResult> {
                Ok(CanvasProviderOpenResult {
                    url: None,
                    title: None,
                    status: None,
                })
            }
        }

        let err = OpenOnly
            .on_action(CanvasProviderInvokeActionRequest {
                session_id: SessionId::from("s1"),
                extension_id: "project:open-only".to_string(),
                canvas_id: "x".to_string(),
                instance_id: "x-1".to_string(),
                action_name: "anything".to_string(),
                input: Some(Value::Null),
                host: None,
                session: None,
            })
            .await
            .unwrap_err();

        assert_eq!(err.code, "canvas_action_no_handler");
    }
}
