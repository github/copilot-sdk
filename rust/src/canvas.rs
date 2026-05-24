//! Canvas declarations, provider callbacks, and host-side canvas RPC types.

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use thiserror::Error;

use crate::generated::api_types::CanvasAction;
use crate::types::SessionId;

/// JSON Schema object used for canvas inputs and canvas-scoped tools.
pub type CanvasJsonSchema = serde_json::Map<String, Value>;

/// Declarative metadata for a single canvas, sent over the wire on
/// `session.create` / `session.resume`.
#[derive(Debug, Clone, Serialize, Deserialize)]
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

/// Response returned from [`CanvasHandler::on_open`].
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenResponse {
    /// URL the host should render. Optional for canvases with no visual surface.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Provider-supplied title shown in host chrome.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Provider-supplied status text shown in host chrome.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
}

/// Host capabilities passed to canvas provider callbacks.
#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasHostContext {
    /// Host capability details.
    #[serde(default)]
    pub capabilities: CanvasHostCapabilities,
}

/// Host capability details passed to canvas provider callbacks.
#[derive(Debug, Clone, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasHostCapabilities {
    /// Whether the host supports canvas rendering.
    #[serde(default)]
    pub canvases: bool,
}

/// Context handed to [`CanvasHandler::on_open`].
#[derive(Debug, Clone)]
pub struct CanvasOpenContext {
    /// Session that requested the canvas.
    pub session_id: SessionId,
    /// Owning provider identifier.
    pub extension_id: String,
    /// Canvas id from the declaring [`CanvasDeclaration`].
    pub canvas_id: String,
    /// Stable instance id supplied by the runtime.
    pub instance_id: String,
    /// Validated input payload.
    pub input: Value,
    /// Host capabilities supplied by the runtime.
    pub host: Option<CanvasHostContext>,
}

/// Context handed to [`CanvasHandler::on_action`].
#[derive(Debug, Clone)]
pub struct CanvasActionContext {
    /// Session that invoked the action.
    pub session_id: SessionId,
    /// Owning provider identifier.
    pub extension_id: String,
    /// Canvas id targeted by the action.
    pub canvas_id: String,
    /// Instance id targeted by the action.
    pub instance_id: String,
    /// Action name from [`CanvasAgentActionDeclaration::name`].
    pub action_name: String,
    /// Validated input payload.
    pub input: Value,
    /// Host capabilities supplied by the runtime.
    pub host: Option<CanvasHostContext>,
}

/// Context handed to a canvas's close lifecycle hook.
#[derive(Debug, Clone)]
pub struct CanvasLifecycleContext {
    /// Session owning the canvas instance.
    pub session_id: SessionId,
    /// Owning provider identifier.
    pub extension_id: String,
    /// Canvas id from the declaring [`CanvasDeclaration`].
    pub canvas_id: String,
    /// Instance id this lifecycle event applies to.
    pub instance_id: String,
    /// Host capabilities supplied by the runtime.
    pub host: Option<CanvasHostContext>,
}

/// Structured error returned from canvas handlers.
#[derive(Debug, Clone, Error, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
#[error("{code}: {message}")]
pub struct CanvasError {
    /// Machine-readable error code.
    pub code: String,
    /// Human-readable message.
    pub message: String,
}

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
pub type CanvasResult<T> = Result<T, CanvasError>;

/// Provider-side canvas lifecycle handler.
///
/// A session installs a single [`CanvasHandler`] (via
/// [`SessionConfig::with_canvas_handler`](crate::types::SessionConfig::with_canvas_handler)).
/// The handler receives every inbound `canvas.open` / `canvas.close` /
/// `canvas.action.invoke` JSON-RPC request the runtime issues for this
/// session and decides — typically by inspecting [`CanvasOpenContext::canvas_id`]
/// — which application-side canvas should handle the call.
///
/// The SDK does not maintain a per-canvas registry; multiplexing across
/// declared canvases is the implementor's responsibility.
#[async_trait]
pub trait CanvasHandler: Send + Sync {
    /// Open a new canvas instance.
    async fn on_open(&self, ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse>;

    /// Handle a non-lifecycle action declared by the canvas.
    async fn on_action(&self, _ctx: CanvasActionContext) -> CanvasResult<Value> {
        Err(CanvasError::no_handler())
    }

    /// Canvas was closed by the user or agent.
    async fn on_close(&self, _ctx: CanvasLifecycleContext) -> CanvasResult<()> {
        Ok(())
    }
}

/// Common fields sent by direct `canvas.*` provider callbacks.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CanvasProviderRequestParams {
    pub session_id: SessionId,
    pub extension_id: String,
    pub canvas_id: String,
    pub instance_id: String,
    #[serde(default)]
    pub input: Value,
    #[serde(default)]
    pub host: Option<CanvasHostContext>,
}

/// Wire-level params for `canvas.action.invoke`.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CanvasInvokeParams {
    pub session_id: SessionId,
    pub extension_id: String,
    pub canvas_id: String,
    pub instance_id: String,
    pub action_name: String,
    #[serde(default)]
    pub input: Value,
    #[serde(default)]
    pub host: Option<CanvasHostContext>,
}

impl CanvasProviderRequestParams {
    pub(crate) fn into_open_context(self) -> CanvasOpenContext {
        CanvasOpenContext {
            session_id: self.session_id,
            extension_id: self.extension_id,
            canvas_id: self.canvas_id,
            instance_id: self.instance_id,
            input: self.input,
            host: self.host,
        }
    }

    pub(crate) fn into_lifecycle_context(self) -> CanvasLifecycleContext {
        CanvasLifecycleContext {
            session_id: self.session_id,
            extension_id: self.extension_id,
            canvas_id: self.canvas_id,
            instance_id: self.instance_id,
            host: self.host,
        }
    }
}

impl CanvasInvokeParams {
    pub(crate) fn into_action_context(self) -> CanvasActionContext {
        CanvasActionContext {
            session_id: self.session_id,
            extension_id: self.extension_id,
            canvas_id: self.canvas_id,
            instance_id: self.instance_id,
            action_name: self.action_name,
            input: self.input,
            host: self.host,
        }
    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    struct EchoHandler;

    #[async_trait]
    impl CanvasHandler for EchoHandler {
        async fn on_open(&self, ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
            Ok(CanvasOpenResponse {
                url: Some(format!("https://example.test/{}", ctx.canvas_id)),
                title: Some("Echo".to_string()),
                status: Some("ready".to_string()),
            })
        }

        async fn on_action(&self, ctx: CanvasActionContext) -> CanvasResult<Value> {
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
            .on_open(CanvasOpenContext {
                session_id: SessionId::from("s1"),
                extension_id: "project:echo".to_string(),
                canvas_id: "echo".to_string(),
                instance_id: "echo-1".to_string(),
                input: json!({ "x": 1 }),
                host: None,
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
            .on_action(CanvasActionContext {
                session_id: SessionId::from("s1"),
                extension_id: "project:echo".to_string(),
                canvas_id: "echo".to_string(),
                instance_id: "inst-1".to_string(),
                action_name: "shout".to_string(),
                input: json!("hi"),
                host: None,
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
            async fn on_open(&self, _ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
                Ok(CanvasOpenResponse {
                    url: None,
                    title: None,
                    status: None,
                })
            }
        }

        let err = OpenOnly
            .on_action(CanvasActionContext {
                session_id: SessionId::from("s1"),
                extension_id: "project:open-only".to_string(),
                canvas_id: "x".to_string(),
                instance_id: "x-1".to_string(),
                action_name: "anything".to_string(),
                input: Value::Null,
                host: None,
            })
            .await
            .unwrap_err();

        assert_eq!(err.code, "canvas_action_no_handler");
    }
}
