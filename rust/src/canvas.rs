//! Extension-owned canvases declared via `joinSession({ canvases: [...] })`.
//!
//! This module is the Rust mirror of the TypeScript wire shape.
//!
//! The wire RPC method is `hostExtension.invoke`; inside, the inner
//! `method == "canvas.action.invoke"` identifies canvas dispatches. The SDK
//! routes purely on `params.canvasId` + `params.actionName`.

use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use thiserror::Error;

use crate::types::SessionId;

/// Declarative metadata for a single canvas, sent over the wire on
/// `session.create` / `session.resume`.
///
/// Mirrors the TypeScript `CanvasDeclaration` interface verbatim. The
/// `handler` that backs this declaration is held in-process (see [`Canvas`])
/// and never serialized.
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasDeclaration {
    /// Canvas identifier, unique within the declaring connection. Stable across
    /// resumes — re-declaring with the same `id` replaces the prior entry.
    pub id: String,
    /// Human-readable name shown in host UI / canvas pickers.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Long-form description; surfaced in the agent's discovery prompt.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// JSON Schema for the `input` payload accepted by `canvas.open`.
    /// Runtime validates incoming `open_canvas` calls against this; handlers
    /// never see malformed input.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<Value>,
    /// Agent-callable actions this canvas exposes. Names MUST NOT start with
    /// `canvas.` (reserved for lifecycle verbs `canvas.{open,focus,close,reload}`).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub agent_actions: Option<Vec<CanvasAgentActionDeclaration>>,
    /// User-facing toolbar buttons rendered by the host canvas chrome.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub toolbar: Option<Vec<CanvasToolbarItemDeclaration>>,
}

/// A single agent-callable action contributed by a canvas.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasAgentActionDeclaration {
    /// Action identifier, unique within the canvas. MUST NOT start with
    /// `canvas.` — that prefix is reserved for lifecycle verbs.
    pub name: String,
    /// Description shown to the model when picking an action.
    pub description: String,
    /// Optional JSON Schema for the action's `input` payload.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<Value>,
}

/// A single toolbar button contributed by a canvas. The host canvas chrome
/// renders these and dispatches `actionName` with optional `input` when
/// clicked.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasToolbarItemDeclaration {
    /// Stable id used by the host to key the button.
    pub id: String,
    /// User-visible label.
    pub label: String,
    /// Optional icon identifier; semantics are host-defined.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub icon: Option<String>,
    /// Optional tooltip shown on hover.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tooltip: Option<String>,
    /// The `agentActions[].name` to dispatch when clicked. May also be a
    /// reserved `canvas.*` verb (e.g. `canvas.reload`) — runtime routes
    /// reserved names to the matching lifecycle method.
    pub action_name: String,
    /// Optional fixed input payload passed verbatim to the action handler.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input: Option<Value>,
}

/// Response returned from [`CanvasHandler::on_open`]. The extension's URL is
/// embedded by the host in its webview surface when the host advertises
/// the `canvas.webview` capability.
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenResponse {
    /// URL the host should embed (typically a loopback HTTP server owned by
    /// the extension). Optional for canvases that have no visual surface.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Stable per-instance identifier the extension can correlate with its
    /// own state. The host echoes this back on subsequent lifecycle calls.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub instance_id: Option<String>,
}

/// Per-instance resume hint sent on `session.resume` to rebuild the runtime's
/// canvas-instance registry. The host persists open canvases across CLI
/// process restarts and hands them back here so subsequent
/// `invoke_canvas_action` dispatches find the existing instance instead of
/// erroring with `canvas_instance_not_found`.
///
/// The handler's `on_open` is **not** re-invoked on rehydrate — the extension
/// keeps whatever state it had in its own process. Entries the runtime cannot
/// bind to a currently-declared canvas trigger a `session.canvas.closed`
/// event with `reason: "rehydrate_failed"`.
#[derive(Debug, Clone, Default, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct CanvasInstanceRehydrate {
    /// Canonical extension id that owns the canvas.
    pub extension_id: String,
    /// Canvas declaration id within that extension.
    pub canvas_id: String,
    /// Stable instance id the host originally opened the canvas under.
    pub instance_id: String,
    /// Optional URL recorded at the original open. Populated as-is into the
    /// rebuilt instance record; not re-validated by the runtime.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Context handed to [`CanvasHandler::on_open`].
#[derive(Debug, Clone)]
pub struct CanvasOpenContext {
    /// Session that requested the canvas.
    pub session_id: SessionId,
    /// Canvas id (matches the declaring [`CanvasDeclaration::id`]).
    pub canvas_id: String,
    /// Agent-supplied stable instance id. Required by the runtime on every
    /// `canvas.open` invocation; handlers should key their per-instance state
    /// off this value.
    pub instance_id: String,
    /// Validated `input` payload, shaped by [`CanvasDeclaration::input_schema`].
    pub input: Value,
    /// Toolbar items declared on the canvas, passed through for handler
    /// convenience (e.g. if the extension wants to mirror them in its own UI).
    pub toolbar: Option<Vec<CanvasToolbarItemDeclaration>>,
}

/// Context handed to [`CanvasHandler::on_action`].
#[derive(Debug, Clone)]
pub struct CanvasActionContext {
    /// Session that invoked the action.
    pub session_id: SessionId,
    /// Canvas id targeted by the action.
    pub canvas_id: String,
    /// Instance id targeted by the action.
    pub instance_id: String,
    /// Action name from [`CanvasAgentActionDeclaration::name`].
    pub action_name: String,
    /// Validated `input` payload, shaped by the action's `input_schema`.
    pub input: Value,
}

/// Context handed to lifecycle hooks ([`CanvasHandler::on_focus`],
/// [`CanvasHandler::on_close`], [`CanvasHandler::on_reload`]).
#[derive(Debug, Clone)]
pub struct CanvasLifecycleContext {
    /// Session owning the canvas instance.
    pub session_id: SessionId,
    /// Canvas id (matches the declaring [`CanvasDeclaration::id`]).
    pub canvas_id: String,
    /// Instance id this lifecycle event applies to.
    pub instance_id: String,
}

/// Structured error returned from canvas handlers. Serialized into the
/// `canvas.action.invoke` error envelope.
#[derive(Debug, Clone, Error, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
#[error("{code}: {message}")]
pub struct CanvasError {
    /// Machine-readable error code. Reserved codes:
    /// - `canvas_action_no_handler` — action declared but no handler implemented
    /// - `canvas_input_invalid` — input failed schema validation (runtime emits)
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

    /// Default error returned by [`CanvasHandler::on_action`] when the
    /// handler did not override it — i.e. the canvas declared no
    /// `agentActions[]` or forgot to wire one.
    pub fn no_handler() -> Self {
        Self::new(
            "canvas_action_no_handler",
            "No handler implemented for this canvas action",
        )
    }
}

/// Result alias for canvas handler methods.
pub type CanvasResult<T> = Result<T, CanvasError>;

/// Per-canvas handler implementing the lifecycle the runtime dispatches.
///
/// Each [`Canvas`] owns one `Arc<dyn CanvasHandler>`. The SDK routes incoming
/// `canvas.action.invoke` requests by `(canvas_id, action_name)`:
///
/// - `canvas.open`   → [`Self::on_open`]   (required)
/// - `canvas.focus`  → [`Self::on_focus`]  (default no-op)
/// - `canvas.close`  → [`Self::on_close`]  (default no-op)
/// - `canvas.reload` → [`Self::on_reload`] (default no-op)
/// - anything else   → [`Self::on_action`] (default returns `canvas_action_no_handler`)
///
/// Implementations may be invoked concurrently — keep them `Send + Sync`.
#[async_trait]
pub trait CanvasHandler: Send + Sync {
    /// Required. Open a new canvas instance. Return its URL (if any) and an
    /// extension-owned instance id (if any).
    async fn on_open(&self, ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse>;

    /// Optional. Handle a non-lifecycle action declared in
    /// [`CanvasDeclaration::agent_actions`]. Default returns
    /// [`CanvasError::no_handler`] so canvases with no agent actions don't
    /// need to think about it.
    async fn on_action(&self, _ctx: CanvasActionContext) -> CanvasResult<Value> {
        Err(CanvasError::no_handler())
    }

    /// Optional. Canvas was brought to the foreground.
    async fn on_focus(&self, _ctx: CanvasLifecycleContext) -> CanvasResult<()> {
        Ok(())
    }

    /// Optional. Canvas was closed by the user or agent.
    async fn on_close(&self, _ctx: CanvasLifecycleContext) -> CanvasResult<()> {
        Ok(())
    }

    /// Optional. Host requested a reload (e.g. user hit refresh).
    async fn on_reload(&self, _ctx: CanvasLifecycleContext) -> CanvasResult<()> {
        Ok(())
    }
}

/// A registered canvas: declarative metadata + in-process handler.
///
/// Construct via [`Canvas::builder`]. The declaration is serialized onto the
/// wire (handlers are dropped — they're not transferable); the handler is
/// retained in the SDK's per-session registry and invoked by
/// `canvas.action.invoke` dispatch keyed by `(canvas_id, action_name)`.
#[derive(Clone)]
pub struct Canvas {
    declaration: CanvasDeclaration,
    handler: Arc<dyn CanvasHandler>,
}

impl Canvas {
    /// Begin building a canvas from its declarative metadata. Call
    /// [`CanvasBuilder::handler`] then [`CanvasBuilder::build`].
    pub fn builder(declaration: CanvasDeclaration) -> CanvasBuilder {
        CanvasBuilder {
            declaration,
            handler: None,
        }
    }

    /// Borrow the declarative metadata (serialized onto the wire).
    pub fn declaration(&self) -> &CanvasDeclaration {
        &self.declaration
    }

    /// Clone the in-process handler arc for dispatch.
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

/// Builder for [`Canvas`]. The handler is required; [`Self::build`] panics
/// if called without one (mirrors the Node `createCanvas` requirement that
/// `onOpen` be provided).
pub struct CanvasBuilder {
    declaration: CanvasDeclaration,
    handler: Option<Arc<dyn CanvasHandler>>,
}

impl CanvasBuilder {
    /// Attach the per-canvas handler. Required.
    pub fn handler(mut self, handler: Arc<dyn CanvasHandler>) -> Self {
        self.handler = Some(handler);
        self
    }

    /// Finalize into a [`Canvas`]. Panics if [`Self::handler`] was not called.
    pub fn build(self) -> Canvas {
        let handler = self
            .handler
            .expect("Canvas::builder().handler(...) must be called before build()");
        Canvas {
            declaration: self.declaration,
            handler,
        }
    }
}

/// Per-session canvas registry, keyed by `canvas_id`.
///
/// Built from a session's `canvases: Vec<Canvas>` at create/resume time and
/// consulted by the JSON-RPC dispatch path when an incoming
/// `canvas.action.invoke` arrives.
pub type CanvasRegistry = HashMap<String, Arc<dyn CanvasHandler>>;

/// Build a [`CanvasRegistry`] from a session's declared canvases.
///
/// Duplicate ids: later entries replace earlier ones (matches the runtime's
/// re-declare-replace semantics on `session.resume`).
pub fn build_registry(canvases: &[Canvas]) -> CanvasRegistry {
    let mut map = CanvasRegistry::new();
    for canvas in canvases {
        map.insert(canvas.declaration.id.clone(), canvas.handler.clone());
    }
    map
}

/// Wire-level params for `canvas.action.invoke` (the inner `method` field of
/// a `hostExtension.invoke` JSON-RPC request).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasInvokeParams {
    /// Canvas id from the declaring [`CanvasDeclaration::id`].
    pub canvas_id: String,
    /// Present for every action except `canvas.open` (runtime allocates
    /// the instance id after open returns).
    #[serde(default)]
    pub instance_id: Option<String>,
    /// `canvas.{open,focus,close,reload}` for lifecycle verbs; otherwise a
    /// custom action name declared in [`CanvasDeclaration::agent_actions`].
    pub action_name: String,
    /// Validated `input` payload. Runtime has already checked it against the
    /// canvas's `input_schema` / action's `input_schema`.
    #[serde(default)]
    pub input: Value,
    /// Toolbar items declared on the canvas — runtime passes them through on
    /// `canvas.open` so handlers don't need to re-derive their own copy.
    #[serde(default)]
    pub toolbar: Option<Vec<CanvasToolbarItemDeclaration>>,
}

/// Resolve a `canvas.action.invoke` request against a registry and run the
/// matching handler method. Returns `Ok(result_value)` on success or
/// `Err(canvas_error)` on failure.
///
/// Reserved verbs (`canvas.{open,focus,close,reload}`) route to the matching
/// lifecycle method; any other `action_name` routes to
/// [`CanvasHandler::on_action`].
pub async fn dispatch_canvas_invoke(
    registry: &CanvasRegistry,
    session_id: SessionId,
    params: CanvasInvokeParams,
) -> CanvasResult<Value> {
    let handler = registry.get(&params.canvas_id).cloned().ok_or_else(|| {
        CanvasError::new(
            "canvas_not_registered",
            format!(
                "No canvas handler registered for id '{}' in this session",
                params.canvas_id
            ),
        )
    })?;

    match params.action_name.as_str() {
        "canvas.open" => {
            let instance_id = params.instance_id.ok_or_else(|| {
                CanvasError::new(
                    "canvas_missing_instance_id",
                    "canvas.open requires an instanceId",
                )
            })?;
            let ctx = CanvasOpenContext {
                session_id,
                canvas_id: params.canvas_id,
                instance_id,
                input: params.input,
                toolbar: params.toolbar,
            };
            let response = handler.on_open(ctx).await?;
            Ok(serde_json::to_value(response).unwrap_or(Value::Null))
        }
        verb @ ("canvas.focus" | "canvas.close" | "canvas.reload") => {
            let instance_id = params.instance_id.ok_or_else(|| {
                CanvasError::new(
                    "canvas_missing_instance_id",
                    format!("Lifecycle verb '{verb}' requires an instanceId"),
                )
            })?;
            let ctx = CanvasLifecycleContext {
                session_id,
                canvas_id: params.canvas_id,
                instance_id,
            };
            match verb {
                "canvas.focus" => handler.on_focus(ctx).await?,
                "canvas.close" => handler.on_close(ctx).await?,
                "canvas.reload" => handler.on_reload(ctx).await?,
                _ => unreachable!(),
            }
            Ok(Value::Null)
        }
        other => {
            let instance_id = params.instance_id.ok_or_else(|| {
                CanvasError::new(
                    "canvas_missing_instance_id",
                    format!("Action '{other}' requires an instanceId"),
                )
            })?;
            let ctx = CanvasActionContext {
                session_id,
                canvas_id: params.canvas_id,
                instance_id,
                action_name: other.to_string(),
                input: params.input,
            };
            handler.on_action(ctx).await
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn declaration_serializes_camel_case_and_skips_none() {
        let decl = CanvasDeclaration {
            id: "counter".into(),
            display_name: Some("Counter".into()),
            description: None,
            input_schema: None,
            agent_actions: Some(vec![CanvasAgentActionDeclaration {
                name: "increment".into(),
                description: "bump".into(),
                input_schema: None,
            }]),
            toolbar: None,
        };
        let v = serde_json::to_value(&decl).unwrap();
        assert_eq!(v["id"], "counter");
        assert_eq!(v["displayName"], "Counter");
        assert!(v.get("description").is_none());
        assert!(v.get("inputSchema").is_none());
        assert_eq!(v["agentActions"][0]["name"], "increment");
        assert!(v.get("toolbar").is_none());
    }

    #[test]
    fn toolbar_item_round_trip() {
        let item = CanvasToolbarItemDeclaration {
            id: "reload".into(),
            label: "Reload".into(),
            icon: Some("refresh".into()),
            tooltip: None,
            action_name: "canvas.reload".into(),
            input: Some(json!({ "force": true })),
        };
        let v = serde_json::to_value(&item).unwrap();
        assert_eq!(v["actionName"], "canvas.reload");
        assert_eq!(v["input"]["force"], true);
        let back: CanvasToolbarItemDeclaration = serde_json::from_value(v).unwrap();
        assert_eq!(back, item);
    }

    struct EchoHandler;

    #[async_trait]
    impl CanvasHandler for EchoHandler {
        async fn on_open(&self, ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
            Ok(CanvasOpenResponse {
                url: Some(format!("https://example.test/{}", ctx.canvas_id)),
                instance_id: Some(format!("instance-of-{}", ctx.canvas_id)),
            })
        }

        async fn on_action(&self, ctx: CanvasActionContext) -> CanvasResult<Value> {
            Ok(json!({ "echoed": ctx.action_name, "input": ctx.input }))
        }
    }

    #[tokio::test]
    async fn canvas_serializes_as_declaration() {
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "echo".into(),
            display_name: Some("Echo".into()),
            ..Default::default()
        })
        .handler(Arc::new(EchoHandler))
        .build();
        let v = serde_json::to_value(&canvas).unwrap();
        assert_eq!(v["id"], "echo");
        assert_eq!(v["displayName"], "Echo");
        assert!(v.get("handler").is_none());
    }

    #[tokio::test]
    async fn default_on_action_returns_no_handler() {
        // EchoHandler overrides on_action; use a bare handler here to hit the default.
        struct OpenOnly;
        #[async_trait]
        impl CanvasHandler for OpenOnly {
            async fn on_open(&self, _ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
                Ok(CanvasOpenResponse::default())
            }
        }
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "bare".into(),
            ..Default::default()
        })
        .handler(Arc::new(OpenOnly))
        .build();

        let err = canvas
            .handler()
            .on_action(CanvasActionContext {
                session_id: SessionId::from("s1"),
                canvas_id: "bare".into(),
                instance_id: "i1".into(),
                action_name: "noop".into(),
                input: Value::Null,
            })
            .await
            .unwrap_err();
        assert_eq!(err.code, "canvas_action_no_handler");
    }

    #[tokio::test]
    async fn default_lifecycle_hooks_are_no_op() {
        struct OpenOnly;
        #[async_trait]
        impl CanvasHandler for OpenOnly {
            async fn on_open(&self, _ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
                Ok(CanvasOpenResponse::default())
            }
        }
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "bare".into(),
            ..Default::default()
        })
        .handler(Arc::new(OpenOnly))
        .build();

        let ctx = CanvasLifecycleContext {
            session_id: SessionId::from("s1"),
            canvas_id: "bare".into(),
            instance_id: "i1".into(),
        };
        canvas.handler().on_focus(ctx.clone()).await.unwrap();
        canvas.handler().on_close(ctx.clone()).await.unwrap();
        canvas.handler().on_reload(ctx).await.unwrap();
    }

    #[tokio::test]
    async fn dispatch_routes_canvas_open() {
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "echo".into(),
            ..Default::default()
        })
        .handler(Arc::new(EchoHandler))
        .build();
        let registry = build_registry(&[canvas]);

        let params = CanvasInvokeParams {
            canvas_id: "echo".into(),
            instance_id: Some("echo-1".into()),
            action_name: "canvas.open".into(),
            input: json!({ "x": 1 }),
            toolbar: None,
        };
        let result = dispatch_canvas_invoke(&registry, SessionId::from("s1"), params)
            .await
            .unwrap();
        assert_eq!(result["url"], "https://example.test/echo");
        assert_eq!(result["instanceId"], "instance-of-echo");
    }

    #[tokio::test]
    async fn dispatch_routes_lifecycle_verbs() {
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "echo".into(),
            ..Default::default()
        })
        .handler(Arc::new(EchoHandler))
        .build();
        let registry = build_registry(&[canvas]);

        for verb in ["canvas.focus", "canvas.close", "canvas.reload"] {
            let params = CanvasInvokeParams {
                canvas_id: "echo".into(),
                instance_id: Some("inst-1".into()),
                action_name: verb.into(),
                input: Value::Null,
                toolbar: None,
            };
            let result = dispatch_canvas_invoke(&registry, SessionId::from("s1"), params)
                .await
                .unwrap();
            assert!(result.is_null(), "verb {verb} should return null");
        }
    }

    #[tokio::test]
    async fn dispatch_routes_custom_action() {
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "echo".into(),
            ..Default::default()
        })
        .handler(Arc::new(EchoHandler))
        .build();
        let registry = build_registry(&[canvas]);

        let params = CanvasInvokeParams {
            canvas_id: "echo".into(),
            instance_id: Some("inst-1".into()),
            action_name: "shout".into(),
            input: json!("hi"),
            toolbar: None,
        };
        let result = dispatch_canvas_invoke(&registry, SessionId::from("s1"), params)
            .await
            .unwrap();
        assert_eq!(result["echoed"], "shout");
        assert_eq!(result["input"], "hi");
    }

    #[tokio::test]
    async fn dispatch_unknown_canvas_errors() {
        let registry = CanvasRegistry::new();
        let params = CanvasInvokeParams {
            canvas_id: "missing".into(),
            instance_id: None,
            action_name: "canvas.open".into(),
            input: Value::Null,
            toolbar: None,
        };
        let err = dispatch_canvas_invoke(&registry, SessionId::from("s1"), params)
            .await
            .unwrap_err();
        assert_eq!(err.code, "canvas_not_registered");
    }

    #[tokio::test]
    async fn dispatch_lifecycle_without_instance_id_errors() {
        let canvas = Canvas::builder(CanvasDeclaration {
            id: "echo".into(),
            ..Default::default()
        })
        .handler(Arc::new(EchoHandler))
        .build();
        let registry = build_registry(&[canvas]);

        let params = CanvasInvokeParams {
            canvas_id: "echo".into(),
            instance_id: None,
            action_name: "canvas.close".into(),
            input: Value::Null,
            toolbar: None,
        };
        let err = dispatch_canvas_invoke(&registry, SessionId::from("s1"), params)
            .await
            .unwrap_err();
        assert_eq!(err.code, "canvas_missing_instance_id");
    }

    #[tokio::test]
    async fn build_registry_replaces_duplicate_ids() {
        struct FirstHandler;
        #[async_trait]
        impl CanvasHandler for FirstHandler {
            async fn on_open(&self, _ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
                Ok(CanvasOpenResponse {
                    url: Some("first".into()),
                    instance_id: None,
                })
            }
        }
        struct SecondHandler;
        #[async_trait]
        impl CanvasHandler for SecondHandler {
            async fn on_open(&self, _ctx: CanvasOpenContext) -> CanvasResult<CanvasOpenResponse> {
                Ok(CanvasOpenResponse {
                    url: Some("second".into()),
                    instance_id: None,
                })
            }
        }
        let first = Canvas::builder(CanvasDeclaration {
            id: "dup".into(),
            ..Default::default()
        })
        .handler(Arc::new(FirstHandler))
        .build();
        let second = Canvas::builder(CanvasDeclaration {
            id: "dup".into(),
            ..Default::default()
        })
        .handler(Arc::new(SecondHandler))
        .build();
        let registry = build_registry(&[first, second]);
        let result = dispatch_canvas_invoke(
            &registry,
            SessionId::from("s1"),
            CanvasInvokeParams {
                canvas_id: "dup".into(),
                instance_id: Some("inst-1".into()),
                action_name: "canvas.open".into(),
                input: Value::Null,
                toolbar: None,
            },
        )
        .await
        .unwrap();
        assert_eq!(result["url"], "second");
    }
}
