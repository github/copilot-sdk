//! Canvas declarations, provider callbacks, and host-side canvas RPC types.

use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use thiserror::Error;

use crate::types::SessionId;

/// JSON Schema object used for canvas inputs and canvas-scoped tools.
pub type CanvasJsonSchema = serde_json::Map<String, Value>;

/// Runtime-controlled routing state for an open canvas instance.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum CanvasInstanceAvailability {
    /// The owning provider is currently connected and routing calls will be dispatched normally.
    Ready,
    /// The owning provider is not currently connected; routing calls fail with
    /// `canvas_provider_unavailable` until the agent re-issues `open_canvas` or
    /// the provider reconnects.
    Stale,
}

/// Declarative metadata for a single canvas, sent over the wire on
/// `session.create` / `session.resume`.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
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
    pub actions: Option<Vec<CanvasAgentActionDeclaration>>,
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

/// A single agent-callable action contributed by a canvas.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasAgentActionDeclaration {
    /// Action identifier, unique within the canvas.
    pub name: String,
    /// Description shown to the model when picking an action.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional JSON Schema for the action's `input` payload.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<Value>,
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

/// Open canvas instance returned by `session.canvas.open`,
/// `session.canvas.listOpen`, and `session.resume`.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
#[non_exhaustive]
pub struct OpenCanvasInstance {
    /// Stable caller-supplied canvas instance identifier.
    pub instance_id: String,
    /// Owning provider identifier.
    pub extension_id: String,
    /// Owning extension display name, when available.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Provider-local canvas identifier.
    pub canvas_id: String,
    /// Rendered title.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Provider-supplied status text.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// URL for web-rendered canvases.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Input supplied when the instance was opened.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input: Option<Value>,
    /// Whether this snapshot came from an idempotent reopen.
    pub reopen: bool,
    /// Runtime-controlled routing state for this instance.
    pub availability: CanvasInstanceAvailability,
}

impl OpenCanvasInstance {
    /// Construct an open canvas instance snapshot with the required fields set.
    pub fn new(
        instance_id: impl Into<String>,
        extension_id: impl Into<String>,
        canvas_id: impl Into<String>,
    ) -> Self {
        Self {
            instance_id: instance_id.into(),
            extension_id: extension_id.into(),
            extension_name: None,
            canvas_id: canvas_id.into(),
            title: None,
            status: None,
            url: None,
            input: None,
            reopen: false,
            availability: CanvasInstanceAvailability::Stale,
        }
    }

    /// Set the owning extension display name.
    pub fn with_extension_name(mut self, extension_name: impl Into<String>) -> Self {
        self.extension_name = Some(extension_name.into());
        self
    }

    /// Set the rendered title.
    pub fn with_title(mut self, title: impl Into<String>) -> Self {
        self.title = Some(title.into());
        self
    }

    /// Set the provider-supplied status text.
    pub fn with_status(mut self, status: impl Into<String>) -> Self {
        self.status = Some(status.into());
        self
    }

    /// Set the URL for web-rendered canvases.
    pub fn with_url(mut self, url: impl Into<String>) -> Self {
        self.url = Some(url.into());
        self
    }

    /// Set the input supplied when the instance was opened.
    pub fn with_input(mut self, input: Value) -> Self {
        self.input = Some(input);
        self
    }

    /// Set whether this snapshot came from an idempotent reopen.
    pub fn with_reopen(mut self, reopen: bool) -> Self {
        self.reopen = reopen;
        self
    }

    /// Set the runtime-controlled routing availability.
    pub fn with_availability(mut self, availability: CanvasInstanceAvailability) -> Self {
        self.availability = availability;
        self
    }
}

/// Result returned by [`SessionCanvas::discover`](crate::session::SessionCanvas::discover).
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasDiscoverResult {
    /// Declared canvases available in this session.
    pub canvases: Vec<DiscoveredCanvas>,
}

/// Canvas available in the current session.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredCanvas {
    /// Owning provider identifier.
    pub extension_id: String,
    /// Owning extension display name, when available.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Provider-local canvas identifier.
    pub canvas_id: String,
    /// Human-readable canvas name.
    pub display_name: String,
    /// Short, single-sentence description shown to the agent in canvas catalogs.
    pub description: String,
    /// JSON Schema for canvas open input.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<Value>,
    /// Actions the agent or host may invoke on an open instance.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub actions: Option<Vec<CanvasAgentActionDeclaration>>,
}

/// Result returned by [`SessionCanvas::list_open`](crate::session::SessionCanvas::list_open).
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasListOpenResult {
    /// Currently open canvas instances.
    pub open_canvases: Vec<OpenCanvasInstance>,
}

/// Request parameters for `session.canvas.open`.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenRequest {
    /// Owning provider identifier.
    pub extension_id: String,
    /// Provider-local canvas identifier.
    pub canvas_id: String,
    /// Caller-supplied stable instance identifier.
    pub instance_id: String,
    /// Optional opaque payload forwarded to the canvas provider.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input: Option<Value>,
}

/// Request parameters for `session.canvas.close`.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasCloseRequest {
    /// Open canvas instance identifier.
    pub instance_id: String,
}

/// Request parameters for `session.canvas.invokeAction`.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasInvokeActionRequest {
    /// Open canvas instance identifier.
    pub instance_id: String,
    /// Action name to invoke.
    pub action_name: String,
    /// Optional input forwarded to the extension's action handler.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input: Option<Value>,
}

/// Result returned from `session.canvas.invokeAction`.
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasInvokeActionResult {
    /// Provider-supplied action result.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
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

/// Per-canvas handler implementing provider-side canvas lifecycle callbacks.
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

/// A registered canvas: declarative metadata plus an in-process handler.
#[derive(Clone)]
pub struct Canvas {
    declaration: CanvasDeclaration,
    handler: Arc<dyn CanvasHandler>,
}

impl Canvas {
    /// Begin building a canvas from its declarative metadata.
    pub fn builder(declaration: CanvasDeclaration) -> CanvasBuilder {
        CanvasBuilder {
            declaration,
            handler: None,
        }
    }

    /// Borrow the declarative metadata serialized onto the wire.
    pub fn declaration(&self) -> &CanvasDeclaration {
        &self.declaration
    }

    /// Clone the in-process handler for dispatch.
    pub fn handler(&self) -> Arc<dyn CanvasHandler> {
        self.handler.clone()
    }
}

impl Serialize for Canvas {
    fn serialize<S: serde::Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        self.declaration.serialize(serializer)
    }
}

impl std::fmt::Debug for Canvas {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Canvas")
            .field("declaration", &self.declaration)
            .field("handler", &"<dyn CanvasHandler>")
            .finish()
    }
}

/// Builder for [`Canvas`].
pub struct CanvasBuilder {
    declaration: CanvasDeclaration,
    handler: Option<Arc<dyn CanvasHandler>>,
}

impl CanvasBuilder {
    /// Attach the per-canvas handler.
    pub fn handler(mut self, handler: Arc<dyn CanvasHandler>) -> Self {
        self.handler = Some(handler);
        self
    }

    /// Finalize into a [`Canvas`].
    ///
    /// Returns an error if no handler was attached.
    pub fn build(self) -> CanvasResult<Canvas> {
        let Some(handler) = self.handler else {
            return Err(CanvasError::new(
                "canvas_builder_missing_handler",
                "Canvas::builder().handler(...) must be called before build()",
            ));
        };

        Ok(Canvas {
            declaration: self.declaration,
            handler,
        })
    }
}

/// Per-session canvas registry, keyed by canvas id.
pub type CanvasRegistry = HashMap<String, Arc<dyn CanvasHandler>>;

/// Build a [`CanvasRegistry`] from a session's declared canvases.
pub fn build_registry(canvases: &[Canvas]) -> CanvasRegistry {
    let mut map = CanvasRegistry::new();
    for canvas in canvases {
        map.insert(canvas.declaration.id.clone(), canvas.handler.clone());
    }
    map
}

/// Common fields sent by direct `canvas.*` provider callbacks.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderRequestParams {
    /// Session that requested the canvas operation.
    pub session_id: SessionId,
    /// Owning provider identifier.
    pub extension_id: String,
    /// Provider-local canvas identifier.
    pub canvas_id: String,
    /// Open canvas instance identifier.
    pub instance_id: String,
    /// Optional provider input payload.
    #[serde(default)]
    pub input: Value,
    /// Host capabilities supplied by the runtime.
    #[serde(default)]
    pub host: Option<CanvasHostContext>,
}

/// Wire-level params for `canvas.action.invoke`.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasInvokeParams {
    /// Session that requested the canvas operation.
    pub session_id: SessionId,
    /// Owning provider identifier.
    pub extension_id: String,
    /// Provider-local canvas identifier.
    pub canvas_id: String,
    /// Open canvas instance identifier.
    pub instance_id: String,
    /// Custom action name.
    pub action_name: String,
    /// Optional provider input payload.
    #[serde(default)]
    pub input: Value,
    /// Host capabilities supplied by the runtime.
    #[serde(default)]
    pub host: Option<CanvasHostContext>,
}

/// Resolve a direct `canvas.open` request against a registry.
pub async fn dispatch_canvas_open(
    registry: &CanvasRegistry,
    params: CanvasProviderRequestParams,
) -> CanvasResult<Value> {
    let handler = canvas_handler(registry, &params.canvas_id)?;
    let response = handler
        .on_open(CanvasOpenContext {
            session_id: params.session_id,
            extension_id: params.extension_id,
            canvas_id: params.canvas_id,
            instance_id: params.instance_id,
            input: params.input,
            host: params.host,
        })
        .await?;
    serde_json::to_value(response).map_err(|error| {
        CanvasError::new(
            "canvas_open_response_serialization_failed",
            format!("failed to serialize canvas.open response: {error}"),
        )
    })
}

/// Resolve a direct `canvas.close` request.
pub async fn dispatch_canvas_close(
    registry: &CanvasRegistry,
    params: CanvasProviderRequestParams,
) -> CanvasResult<Value> {
    let handler = canvas_handler(registry, &params.canvas_id)?;
    let ctx = CanvasLifecycleContext {
        session_id: params.session_id,
        extension_id: params.extension_id,
        canvas_id: params.canvas_id,
        instance_id: params.instance_id,
        host: params.host,
    };
    handler.on_close(ctx).await?;
    Ok(Value::Null)
}

/// Resolve a direct `canvas.action.invoke` request against a registry.
pub async fn dispatch_canvas_action(
    registry: &CanvasRegistry,
    params: CanvasInvokeParams,
) -> CanvasResult<Value> {
    let handler = canvas_handler(registry, &params.canvas_id)?;
    handler
        .on_action(CanvasActionContext {
            session_id: params.session_id,
            extension_id: params.extension_id,
            canvas_id: params.canvas_id,
            instance_id: params.instance_id,
            action_name: params.action_name,
            input: params.input,
            host: params.host,
        })
        .await
}

fn canvas_handler(
    registry: &CanvasRegistry,
    canvas_id: &str,
) -> CanvasResult<Arc<dyn CanvasHandler>> {
    registry.get(canvas_id).cloned().ok_or_else(|| {
        CanvasError::new(
            "canvas_not_found",
            format!("No canvas registered with id '{canvas_id}'"),
        )
    })
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
            actions: Some(vec![CanvasAgentActionDeclaration {
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
    async fn dispatch_routes_canvas_open() {
        let canvas = Canvas::builder(CanvasDeclaration::new("echo", "Echo", "Echo values"))
            .handler(Arc::new(EchoHandler))
            .build()
            .unwrap();
        let registry = build_registry(&[canvas]);
        let params = CanvasProviderRequestParams {
            session_id: SessionId::from("s1"),
            extension_id: "project:echo".to_string(),
            canvas_id: "echo".to_string(),
            instance_id: "echo-1".to_string(),
            input: json!({ "x": 1 }),
            host: None,
        };

        let result = dispatch_canvas_open(&registry, params).await.unwrap();

        assert_eq!(result["url"], "https://example.test/echo");
        assert_eq!(result["title"], "Echo");
        assert_eq!(result["status"], "ready");
    }

    #[tokio::test]
    async fn dispatch_routes_custom_action() {
        let canvas = Canvas::builder(CanvasDeclaration::new("echo", "Echo", "Echo values"))
            .handler(Arc::new(EchoHandler))
            .build()
            .unwrap();
        let registry = build_registry(&[canvas]);

        let result = dispatch_canvas_action(
            &registry,
            CanvasInvokeParams {
                session_id: SessionId::from("s1"),
                extension_id: "project:echo".to_string(),
                canvas_id: "echo".to_string(),
                instance_id: "inst-1".to_string(),
                action_name: "shout".to_string(),
                input: json!("hi"),
                host: None,
            },
        )
        .await
        .unwrap();

        assert_eq!(result["echoed"], "shout");
        assert_eq!(result["input"], "hi");
    }

    #[tokio::test]
    async fn dispatch_unknown_canvas_errors() {
        let err = dispatch_canvas_open(
            &CanvasRegistry::new(),
            CanvasProviderRequestParams {
                session_id: SessionId::from("s1"),
                extension_id: "project:missing".to_string(),
                canvas_id: "missing".to_string(),
                instance_id: "missing-1".to_string(),
                input: Value::Null,
                host: None,
            },
        )
        .await
        .unwrap_err();

        assert_eq!(err.code, "canvas_not_found");
    }

    #[test]
    fn builder_requires_handler() {
        let err = Canvas::builder(CanvasDeclaration::new("echo", "Echo", "Echo values"))
            .build()
            .unwrap_err();

        assert_eq!(err.code, "canvas_builder_missing_handler");
    }
}
