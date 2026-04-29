use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;

use serde_json::Value;
use tokio::sync::{Mutex, oneshot};
use tokio::task::JoinHandle;
use tracing::{Instrument, warn};

use crate::generated::api_types::{
    PermissionDecision, PermissionDecisionApproveOnce, PermissionDecisionApproveOnceKind,
    PermissionDecisionReject, PermissionDecisionRejectKind,
};
use crate::generated::session_events::{
    ElicitationRequestedData, ExternalToolRequestedData, SessionErrorData, SessionEventType,
    UserInputRequestedData,
};
use crate::handler::{
    ExitPlanModeResult, HandlerEvent, HandlerResponse, PermissionResult, SessionHandler,
    UserInputResponse,
};
use crate::hooks::SessionHooks;
use crate::transforms::SystemMessageTransform;
use crate::types::{
    CreateSessionResult, ElicitationRequest, ElicitationResult, ExitPlanModeData,
    GetMessagesResponse, InputOptions, MessageOptions, PermissionRequestData, RequestId,
    ResumeSessionConfig, SectionOverride, SessionCapabilities, SessionConfig, SessionEvent,
    SessionId, SessionTelemetryEvent, SetModelOptions, SystemMessageConfig, ToolInvocation,
    ToolResult, ToolResultResponse, ensure_attachment_display_names,
};
use crate::{Client, Error, JsonRpcResponse, SessionError, SessionEventNotification, error_codes};

/// Shared state between a [`Session`] and its event loop, used by [`Session::send_and_wait`].
struct IdleWaiter {
    tx: oneshot::Sender<Result<Option<SessionEvent>, Error>>,
    last_assistant_message: Option<SessionEvent>,
}

/// A session on a Copilot CLI server.
///
/// Created via [`Client::create_session`] or [`Client::resume_session`].
/// Owns an internal event loop that dispatches events to the [`SessionHandler`].
///
/// Protocol methods (`send`, `get_messages`, `abort`, etc.) automatically
/// inject the session ID into RPC params.
///
/// Call [`destroy`](Self::destroy) for graceful cleanup (RPC + local). If dropped
/// without calling `destroy`, the `Drop` impl aborts the event loop and
/// unregisters from the router as a best-effort safety net.
pub struct Session {
    id: SessionId,
    cwd: PathBuf,
    workspace_path: Option<PathBuf>,
    remote_url: Option<String>,
    client: Client,
    event_loop: Mutex<Option<JoinHandle<()>>>,
    /// Only populated while a `send_and_wait` call is in flight.
    idle_waiter: Arc<Mutex<Option<IdleWaiter>>>,
    /// Capabilities negotiated with the CLI, updated on `capabilities.changed` events.
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
    /// Broadcast channel for runtime event subscribers — see [`Session::subscribe`].
    event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
}

impl Session {
    /// Session ID assigned by the CLI.
    pub fn id(&self) -> &SessionId {
        &self.id
    }

    /// Working directory of the CLI process.
    pub fn cwd(&self) -> &PathBuf {
        &self.cwd
    }

    /// Workspace directory for the session (if using infinite sessions).
    pub fn workspace_path(&self) -> Option<&Path> {
        self.workspace_path.as_deref()
    }

    /// Remote session URL, if the session is running remotely.
    pub fn remote_url(&self) -> Option<&str> {
        self.remote_url.as_deref()
    }

    /// Session capabilities negotiated with the CLI.
    ///
    /// Capabilities are set during session creation and updated at runtime
    /// via `capabilities.changed` events.
    pub fn capabilities(&self) -> SessionCapabilities {
        self.capabilities.read().clone()
    }

    /// Subscribe to events for this session.
    ///
    /// Returns an [`EventSubscription`](crate::subscription::EventSubscription)
    /// that yields every [`SessionEvent`] dispatched on this session's
    /// event loop. Drop the value to unsubscribe; there is no separate
    /// cancel handle.
    ///
    /// **Observe-only.** Subscribers receive a clone of every
    /// [`SessionEvent`] but cannot influence permission decisions, tool
    /// results, or anything else that requires returning a
    /// [`HandlerResponse`]. Those remain
    /// the responsibility of the [`SessionHandler`] passed via
    /// [`SessionConfig::handler`](crate::types::SessionConfig::handler).
    ///
    /// The returned handle implements both an inherent
    /// [`recv`](crate::subscription::EventSubscription::recv) method and
    /// [`Stream`](tokio_stream::Stream), so callers can use a `while let`
    /// loop or any combinator from `tokio_stream::StreamExt` /
    /// `futures::StreamExt`.
    ///
    /// Each subscriber maintains its own queue. If a consumer cannot keep
    /// up, the oldest events are dropped and `recv` returns
    /// [`RecvError::Lagged`](crate::subscription::RecvError::Lagged)
    /// reporting the count of skipped events. Slow consumers do not block
    /// the session's event loop.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(session: copilot::session::Session) {
    /// let mut events = session.subscribe();
    /// tokio::spawn(async move {
    ///     while let Ok(event) = events.recv().await {
    ///         println!("[{}] event {}", event.id, event.event_type);
    ///     }
    /// });
    /// # }
    /// ```
    pub fn subscribe(&self) -> crate::subscription::EventSubscription {
        crate::subscription::EventSubscription::new(self.event_tx.subscribe())
    }

    /// The underlying Client (for advanced use cases).
    pub fn client(&self) -> &Client {
        &self.client
    }

    /// Stop the internal event loop. Called automatically on [`destroy`](Self::destroy).
    pub async fn stop_event_loop(&self) {
        let handle = self.event_loop.lock().await.take();
        if let Some(handle) = handle {
            handle.abort();
            let _ = handle.await;
        }
        // Fail any pending send_and_wait so it returns immediately.
        if let Some(waiter) = self.idle_waiter.lock().await.take() {
            let _ = waiter
                .tx
                .send(Err(Error::Session(SessionError::EventLoopClosed)));
        }
    }

    /// Send a user message to the agent.
    ///
    /// Accepts anything convertible to [`MessageOptions`] — pass a `&str` for the
    /// trivial case, or build a `MessageOptions` for mode/attachments. The
    /// `wait_timeout` field on `MessageOptions` is ignored here (use
    /// [`send_and_wait`](Self::send_and_wait) if you need to wait).
    ///
    /// Returns the assigned message ID, which can be used to correlate the
    /// send with later [`SessionEvent`]s emitted in
    /// response (assistant messages, tool requests, etc.).
    ///
    /// Returns an error if a [`send_and_wait`](Self::send_and_wait) call is
    /// currently in flight, since the plain send would race with the waiter.
    pub async fn send(&self, opts: impl Into<MessageOptions>) -> Result<String, Error> {
        if self.idle_waiter.lock().await.is_some() {
            return Err(Error::Session(SessionError::SendWhileWaiting));
        }
        self.send_inner(opts.into()).await
    }

    async fn send_inner(&self, opts: MessageOptions) -> Result<String, Error> {
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "prompt": opts.prompt,
        });
        if let Some(m) = opts.mode {
            params["mode"] = Value::String(m);
        }
        if let Some(mut a) = opts.attachments {
            ensure_attachment_display_names(&mut a);
            params["attachments"] = serde_json::to_value(a)?;
        }
        let result = self.client.call("session.send", Some(params)).await?;
        let message_id = result
            .get("messageId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string())
            .unwrap_or_default();
        Ok(message_id)
    }

    /// Enable or disable session-wide auto-approval for tool permission requests.
    pub async fn set_approve_all_permissions(&self, enabled: bool) -> Result<(), Error> {
        self.client
            .call(
                "session.permissions.setApproveAll",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "enabled": enabled,
                })),
            )
            .await?;
        Ok(())
    }

    /// Send a user message and wait for the agent to finish processing.
    ///
    /// Accepts anything convertible to [`MessageOptions`] — pass a `&str` for the
    /// trivial case, or build a `MessageOptions` for mode/attachments/timeout.
    /// Blocks until `session.idle` (success) or `session.error` (failure),
    /// returning the last `assistant.message` event captured during streaming.
    /// Times out after `MessageOptions::wait_timeout` (default 60 seconds).
    ///
    /// Only one `send_and_wait` call may be active per session at a time.
    /// Calling [`send`](Self::send) while a `send_and_wait`
    /// is in flight will also return an error.
    pub async fn send_and_wait(
        &self,
        opts: impl Into<MessageOptions>,
    ) -> Result<Option<SessionEvent>, Error> {
        let opts = opts.into();
        let timeout_duration = opts.wait_timeout.unwrap_or(Duration::from_secs(60));
        let (tx, rx) = oneshot::channel();

        {
            let mut guard = self.idle_waiter.lock().await;
            if guard.is_some() {
                return Err(Error::Session(SessionError::SendWhileWaiting));
            }
            *guard = Some(IdleWaiter {
                tx,
                last_assistant_message: None,
            });
        }

        let result = tokio::time::timeout(timeout_duration, async {
            if let Err(e) = self.send_inner(opts).await {
                self.idle_waiter.lock().await.take();
                return Err(e);
            }

            match rx.await {
                Ok(result) => result,
                Err(_) => {
                    self.idle_waiter.lock().await.take();
                    Err(Error::Session(SessionError::EventLoopClosed))
                }
            }
        })
        .await;

        match result {
            Ok(inner) => inner,
            Err(_) => {
                self.idle_waiter.lock().await.take();
                Err(Error::Session(SessionError::Timeout(timeout_duration)))
            }
        }
    }

    /// Retrieve the session's message history.
    pub async fn get_messages(&self) -> Result<Vec<SessionEvent>, Error> {
        let result = self
            .client
            .call(
                "session.getMessages",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        let response: GetMessagesResponse = serde_json::from_value(result)?;
        Ok(response.events)
    }

    /// Abort the current agent turn.
    pub async fn abort(&self) -> Result<(), Error> {
        self.client
            .call(
                "session.abort",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        Ok(())
    }

    /// Switch to a different model.
    ///
    /// Pass `None` for `opts` if no extra configuration is needed. Mirrors
    /// Go's `Session.SetModel(ctx, model, *SetModelOptions)`.
    pub async fn set_model(
        &self,
        model: &str,
        opts: Option<SetModelOptions>,
    ) -> Result<Option<String>, Error> {
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "modelId": model,
        });
        if let Some(opts) = opts {
            if let Some(effort) = opts.reasoning_effort {
                params["reasoningEffort"] = Value::String(effort);
            }
            if let Some(caps) = opts.model_capabilities {
                params["modelCapabilities"] = serde_json::to_value(caps)?;
            }
        }
        let result = self
            .client
            .call("session.model.switchTo", Some(params))
            .await?;
        Ok(result
            .get("modelId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string()))
    }

    /// Get the current model.
    pub async fn get_model(&self) -> Result<Option<String>, Error> {
        let result = self
            .client
            .call(
                "session.model.getCurrent",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        Ok(result
            .get("modelId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string()))
    }

    /// Set the session mode (e.g. "interactive", "plan", "autopilot").
    pub async fn set_mode(&self, mode: &str) -> Result<String, Error> {
        let result = self
            .client
            .call(
                "session.mode.set",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "mode": mode,
                })),
            )
            .await?;
        Ok(result
            .get("mode")
            .and_then(|v| v.as_str())
            .unwrap_or(mode)
            .to_string())
    }

    /// Get the current session mode.
    pub async fn get_mode(&self) -> Result<String, Error> {
        let result = self
            .client
            .call(
                "session.mode.get",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        Ok(result
            .get("mode")
            .and_then(|v| v.as_str())
            .unwrap_or("interactive")
            .to_string())
    }

    /// Get the current session name.
    pub async fn get_name(&self) -> Result<Option<String>, Error> {
        let result = self
            .client
            .call(
                "session.name.get",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        Ok(result
            .get("name")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string()))
    }

    /// Set the current session name.
    pub async fn set_name(&self, name: &str) -> Result<(), Error> {
        self.client
            .call(
                "session.name.set",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "name": name,
                })),
            )
            .await?;
        Ok(())
    }

    /// Disconnect this session from the CLI.
    ///
    /// Sends the `session.destroy` RPC, stops the event loop, and unregisters
    /// the session from the client. **Session state on disk** (conversation
    /// history, planning state, artifacts) is **preserved**, so the
    /// conversation can be resumed later via [`Client::resume_session`]
    /// using this session's ID. To permanently remove all on-disk session
    /// data, use [`Client::delete_session`] instead.
    ///
    /// The caller should ensure the session is idle (e.g. [`send_and_wait`]
    /// has returned) before disconnecting; in-flight tool or event handlers
    /// may otherwise observe failures.
    ///
    /// [`Client::resume_session`]: crate::Client::resume_session
    /// [`Client::delete_session`]: crate::Client::delete_session
    /// [`send_and_wait`]: Self::send_and_wait
    pub async fn disconnect(&self) -> Result<(), Error> {
        self.client
            .call(
                "session.destroy",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        self.stop_event_loop().await;
        self.client.unregister_session(&self.id);
        Ok(())
    }

    /// Alias for [`disconnect`](Self::disconnect).
    ///
    /// Named after the `session.destroy` wire RPC. Prefer `disconnect` in
    /// new code — the wire-level "destroy" is misleading because on-disk
    /// state is preserved.
    pub async fn destroy(&self) -> Result<(), Error> {
        self.disconnect().await
    }

    /// List files in the session workspace.
    pub async fn list_workspace_files(&self) -> Result<Vec<String>, Error> {
        let result = self
            .client
            .call(
                "session.workspace.listFiles",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        let files = result
            .get("files")
            .and_then(|v| v.as_array())
            .map(|arr| {
                arr.iter()
                    .filter_map(|v| v.as_str().map(|s| s.to_string()))
                    .collect()
            })
            .unwrap_or_default();
        Ok(files)
    }

    /// Read a file from the session workspace.
    pub async fn read_workspace_file(&self, path: &Path) -> Result<String, Error> {
        let result = self
            .client
            .call(
                "session.workspace.readFile",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "path": path.to_string_lossy(),
                })),
            )
            .await?;
        Ok(result
            .get("content")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string())
    }

    /// Create a file in the session workspace.
    pub async fn create_workspace_file(&self, path: &Path, content: &str) -> Result<(), Error> {
        self.client
            .call(
                "session.workspace.createFile",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "path": path.to_string_lossy(),
                    "content": content,
                })),
            )
            .await?;
        Ok(())
    }

    /// Read the session plan.
    pub async fn read_plan(&self) -> Result<(bool, Option<String>), Error> {
        let result = self
            .client
            .call(
                "session.plan.read",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        let exists = result
            .get("exists")
            .and_then(|v| v.as_bool())
            .unwrap_or(false);
        let content = result
            .get("content")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string());
        Ok((exists, content))
    }

    /// Update the session plan.
    pub async fn update_plan(&self, content: &str) -> Result<(), Error> {
        self.client
            .call(
                "session.plan.update",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "content": content,
                })),
            )
            .await?;
        Ok(())
    }

    /// Delete the session plan.
    pub async fn delete_plan(&self) -> Result<(), Error> {
        self.client
            .call(
                "session.plan.delete",
                Some(serde_json::json!({ "sessionId": self.id })),
            )
            .await?;
        Ok(())
    }

    /// Write a log message to the session.
    ///
    /// Pass `None` for `opts` to use defaults (info level, persisted).
    /// Mirrors Go's `Session.Log(ctx, message, *LogOptions)`.
    pub async fn log(
        &self,
        message: &str,
        opts: Option<crate::types::LogOptions>,
    ) -> Result<(), Error> {
        let opts = opts.unwrap_or_default();
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "message": message,
        });
        if let Some(level) = opts.level {
            params["level"] = serde_json::to_value(level)?;
        }
        if let Some(ephemeral) = opts.ephemeral {
            params["ephemeral"] = Value::Bool(ephemeral);
        }
        self.client.call("session.log", Some(params)).await?;
        Ok(())
    }

    /// Send a telemetry event through the session's internal shared API.
    pub async fn send_telemetry(&self, event: SessionTelemetryEvent) -> Result<(), Error> {
        let mut params = serde_json::to_value(event)?;
        let params_object = params
            .as_object_mut()
            .expect("SessionTelemetryEvent always serializes to an object");
        params_object.insert("sessionId".to_string(), serde_json::to_value(&self.id)?);

        self.client
            .call("session.sendTelemetry", Some(params))
            .await?;
        Ok(())
    }

    /// Request user input via an interactive UI form (elicitation).
    ///
    /// Sends a JSON Schema describing form fields to the CLI host. The host
    /// renders a form dialog and returns the user's response.
    ///
    /// Prefer the typed convenience methods [`confirm`](Self::confirm),
    /// [`select`](Self::select), and [`input`](Self::input) for common cases.
    pub async fn elicitation(
        &self,
        message: &str,
        schema: Value,
    ) -> Result<ElicitationResult, Error> {
        self.assert_elicitation()?;
        let result = self
            .client
            .call(
                "session.ui.elicitation",
                Some(serde_json::json!({
                    "sessionId": self.id,
                    "message": message,
                    "schema": schema,
                })),
            )
            .await?;
        let elicitation: ElicitationResult = serde_json::from_value(result)?;
        Ok(elicitation)
    }

    /// Ask the user a yes/no confirmation question.
    ///
    /// Returns `true` if the user accepted and confirmed, `false` otherwise.
    pub async fn confirm(&self, message: &str) -> Result<bool, Error> {
        self.assert_elicitation()?;
        let schema = serde_json::json!({
            "type": "object",
            "properties": {
                "confirmed": {
                    "type": "boolean",
                    "default": true,
                }
            },
            "required": ["confirmed"]
        });
        let result = self.elicitation(message, schema).await?;
        Ok(result.action == "accept"
            && result
                .content
                .and_then(|c| c.get("confirmed").and_then(|v| v.as_bool()))
                == Some(true))
    }

    /// Ask the user to select from a list of options.
    ///
    /// Returns the selected option string on accept, or `None` on decline/cancel.
    pub async fn select(&self, message: &str, options: &[&str]) -> Result<Option<String>, Error> {
        self.assert_elicitation()?;
        let schema = serde_json::json!({
            "type": "object",
            "properties": {
                "selection": {
                    "type": "string",
                    "enum": options,
                }
            },
            "required": ["selection"]
        });
        let result = self.elicitation(message, schema).await?;
        if result.action != "accept" {
            return Ok(None);
        }
        let selection = result.content.and_then(|c| {
            c.get("selection")
                .and_then(|v| v.as_str())
                .map(String::from)
        });
        Ok(selection)
    }

    /// Ask the user for free-form text input.
    ///
    /// Returns the input string on accept, or `None` on decline/cancel.
    /// Use [`InputOptions`] to set validation constraints and field metadata.
    pub async fn input(
        &self,
        message: &str,
        options: Option<&InputOptions<'_>>,
    ) -> Result<Option<String>, Error> {
        self.assert_elicitation()?;
        let mut field = serde_json::json!({ "type": "string" });
        if let Some(opts) = options {
            if let Some(title) = opts.title {
                field["title"] = Value::String(title.to_string());
            }
            if let Some(desc) = opts.description {
                field["description"] = Value::String(desc.to_string());
            }
            if let Some(min) = opts.min_length {
                field["minLength"] = Value::Number(min.into());
            }
            if let Some(max) = opts.max_length {
                field["maxLength"] = Value::Number(max.into());
            }
            if let Some(fmt) = &opts.format {
                field["format"] = Value::String(fmt.as_str().to_string());
            }
            if let Some(default) = opts.default {
                field["default"] = Value::String(default.to_string());
            }
        }
        let schema = serde_json::json!({
            "type": "object",
            "properties": { "value": field },
            "required": ["value"]
        });
        let result = self.elicitation(message, schema).await?;
        if result.action != "accept" {
            return Ok(None);
        }
        let value = result
            .content
            .and_then(|c| c.get("value").and_then(|v| v.as_str()).map(String::from));
        Ok(value)
    }

    /// Returns an error if the host doesn't support elicitation.
    fn assert_elicitation(&self) -> Result<(), Error> {
        if self
            .capabilities
            .read()
            .ui
            .as_ref()
            .and_then(|u| u.elicitation)
            != Some(true)
        {
            return Err(Error::Session(SessionError::ElicitationNotSupported));
        }
        Ok(())
    }

    /// Start a fleet of sub-agents.
    pub async fn start_fleet(&self, prompt: Option<&str>) -> Result<bool, Error> {
        let mut params = serde_json::json!({ "sessionId": self.id });
        if let Some(p) = prompt {
            params["prompt"] = Value::String(p.to_string());
        }
        let result = self
            .client
            .call("session.fleet.start", Some(params))
            .await?;
        Ok(result
            .get("started")
            .and_then(|v| v.as_bool())
            .unwrap_or(false))
    }

    /// Generic RPC forwarder — auto-injects sessionId into params.
    pub async fn call_rpc(
        &self,
        method: &str,
        extra_params: Option<Value>,
    ) -> Result<Value, Error> {
        let mut params = serde_json::json!({ "sessionId": self.id });
        let extra_obj = extra_params.as_ref().and_then(Value::as_object);
        if let (Some(base), Some(extra_obj)) = (params.as_object_mut(), extra_obj) {
            for (k, v) in extra_obj {
                base.insert(k.clone(), v.clone());
            }
        }
        self.client.call(method, Some(params)).await
    }
}

impl Drop for Session {
    fn drop(&mut self) {
        let handle = self
            .event_loop
            .try_lock()
            .ok()
            .and_then(|mut guard| guard.take());
        if let Some(handle) = handle {
            handle.abort();
        }
        self.client.unregister_session(&self.id);
    }
}

impl Client {
    /// Create a new session on the CLI.
    ///
    /// Sends `session.create`, registers the session on the router,
    /// and spawns an internal event loop that dispatches to the handler.
    ///
    /// All callbacks (event handler, hooks, transform) are configured
    /// via [`SessionConfig`] using [`with_handler`](SessionConfig::with_handler),
    /// [`with_hooks`](SessionConfig::with_hooks), and
    /// [`with_transform`](SessionConfig::with_transform).
    ///
    /// If [`hooks_handler`](SessionConfig::hooks_handler) is set, the
    /// wire-level `hooks` flag is automatically enabled.
    ///
    /// If [`transform`](SessionConfig::transform) is set, the SDK injects
    /// `action: "transform"` sections into the [`SystemMessageConfig`] wire
    /// format and handles `systemMessage.transform` RPC callbacks during
    /// the session.
    ///
    /// If [`handler`](SessionConfig::handler) is `None`, the session uses
    /// [`DenyAllHandler`](crate::handler::DenyAllHandler) — permission
    /// requests are denied; other events are no-ops.
    pub async fn create_session(&self, mut config: SessionConfig) -> Result<Session, Error> {
        let handler = config
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        let hooks = config.hooks_handler.take();
        let transforms = config.transform.take();

        if hooks.is_some() && config.hooks.is_none() {
            config.hooks = Some(true);
        }
        if let Some(ref transforms) = transforms {
            inject_transform_sections(&mut config, transforms.as_ref());
        }
        let params = serde_json::to_value(&config)?;
        let result = self.call("session.create", Some(params)).await?;
        let create_result: CreateSessionResult = serde_json::from_value(result)?;

        let session_id = create_result.session_id;
        let capabilities = Arc::new(parking_lot::RwLock::new(
            create_result.capabilities.unwrap_or_default(),
        ));
        let channels = self.register_session(&session_id);

        let idle_waiter = Arc::new(Mutex::new(None));
        let (event_tx, _) = tokio::sync::broadcast::channel(512);
        let event_loop = spawn_event_loop(
            session_id.clone(),
            self.clone(),
            handler,
            hooks,
            transforms,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
            event_tx.clone(),
        );

        Ok(Session {
            id: session_id,
            cwd: self.cwd().clone(),
            workspace_path: create_result.workspace_path,
            remote_url: create_result.remote_url,
            client: self.clone(),
            event_loop: Mutex::new(Some(event_loop)),
            idle_waiter,
            capabilities,
            event_tx,
        })
    }

    /// Resume an existing session on the CLI.
    ///
    /// Sends `session.resume` and `session.skills.reload`, registers the
    /// session on the router, and spawns the event loop.
    ///
    /// All callbacks (event handler, hooks, transform) are configured
    /// via [`ResumeSessionConfig`] using its `with_*` builder methods.
    ///
    /// See [`Self::create_session`] for the defaults applied when callback
    /// fields are unset.
    pub async fn resume_session(&self, mut config: ResumeSessionConfig) -> Result<Session, Error> {
        let handler = config
            .handler
            .take()
            .unwrap_or_else(|| Arc::new(crate::handler::DenyAllHandler));
        let hooks = config.hooks_handler.take();
        let transforms = config.transform.take();

        if hooks.is_some() && config.hooks.is_none() {
            config.hooks = Some(true);
        }
        if let Some(ref transforms) = transforms {
            inject_transform_sections_resume(&mut config, transforms.as_ref());
        }
        let session_id = config.session_id.clone();
        let params = serde_json::to_value(&config)?;
        let result = self.call("session.resume", Some(params)).await?;

        // The CLI may reassign the session ID on resume.
        let cli_session_id: SessionId = result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .unwrap_or(&session_id)
            .into();

        let resume_capabilities: Option<SessionCapabilities> = result
            .get("capabilities")
            .and_then(|v| {
                serde_json::from_value(v.clone())
                    .map_err(|e| warn!(error = %e, "failed to deserialize capabilities from resume response"))
                    .ok()
            });
        let remote_url = result
            .get("remoteUrl")
            .or_else(|| result.get("remote_url"))
            .and_then(|value| value.as_str())
            .map(ToString::to_string);

        // Reload skills after resume (best-effort).
        if let Err(e) = self
            .call(
                "session.skills.reload",
                Some(serde_json::json!({ "sessionId": cli_session_id })),
            )
            .await
        {
            warn!(error = %e, "failed to reload skills after resume");
        }

        let capabilities = Arc::new(parking_lot::RwLock::new(
            resume_capabilities.unwrap_or_default(),
        ));
        let channels = self.register_session(&cli_session_id);

        let idle_waiter = Arc::new(Mutex::new(None));
        let (event_tx, _) = tokio::sync::broadcast::channel(512);
        let event_loop = spawn_event_loop(
            cli_session_id.clone(),
            self.clone(),
            handler,
            hooks,
            transforms,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
            event_tx.clone(),
        );

        Ok(Session {
            id: cli_session_id,
            cwd: self.cwd().clone(),
            workspace_path: None,
            remote_url,
            client: self.clone(),
            event_loop: Mutex::new(Some(event_loop)),
            idle_waiter,
            capabilities,
            event_tx,
        })
    }
}

#[allow(clippy::too_many_arguments)]
fn spawn_event_loop(
    session_id: SessionId,
    client: Client,
    handler: Arc<dyn SessionHandler>,
    hooks: Option<Arc<dyn SessionHooks>>,
    transforms: Option<Arc<dyn SystemMessageTransform>>,
    channels: crate::router::SessionChannels,
    idle_waiter: Arc<Mutex<Option<IdleWaiter>>>,
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
    event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
) -> JoinHandle<()> {
    let crate::router::SessionChannels {
        mut notifications,
        mut requests,
    } = channels;

    let span = tracing::error_span!("session_event_loop", session_id = %session_id);
    tokio::spawn(
        async move {
            loop {
                tokio::select! {
                    Some(notification) = notifications.recv() => {
                        handle_notification(
                            &session_id, &client, &handler, notification, &idle_waiter, &capabilities, &event_tx,
                        ).await;
                    }
                    Some(request) = requests.recv() => {
                        handle_request(
                            &session_id, &client, &handler, hooks.as_deref(), transforms.as_deref(), request,
                        ).await;
                    }
                    else => break,
                }
            }
            // Channels closed — fail any pending send_and_wait.
            if let Some(waiter) = idle_waiter.lock().await.take() {
                let _ = waiter
                    .tx
                    .send(Err(Error::Session(SessionError::EventLoopClosed)));
            }
        }
        .instrument(span),
    )
}

fn extract_request_id(data: &Value) -> Option<RequestId> {
    data.get("requestId")
        .and_then(|v| v.as_str())
        .filter(|s| !s.is_empty())
        .map(RequestId::new)
}

fn pending_permission_result_kind(response: &HandlerResponse) -> &'static str {
    match response {
        HandlerResponse::Permission(PermissionResult::Approved) => "approve-once",
        HandlerResponse::Permission(PermissionResult::Denied) => "reject",
        _ => "user-not-available",
    }
}

fn permission_request_response(response: &HandlerResponse) -> PermissionDecision {
    match response {
        HandlerResponse::Permission(PermissionResult::Approved) => {
            PermissionDecision::ApproveOnce(PermissionDecisionApproveOnce {
                kind: PermissionDecisionApproveOnceKind::ApproveOnce,
            })
        }
        _ => PermissionDecision::Reject(PermissionDecisionReject {
            kind: PermissionDecisionRejectKind::Reject,
            feedback: None,
        }),
    }
}

/// Map a handler response into the `result` payload for the notification
/// path (`session.permissions.handlePendingPermissionRequest`).
///
/// Returns `None` when the SDK must not respond — currently only the
/// [`PermissionResult::Deferred`] case, where the handler takes over
/// responsibility for the round-trip itself.
fn notification_permission_payload(response: &HandlerResponse) -> Option<Value> {
    match response {
        HandlerResponse::Permission(PermissionResult::Deferred) => None,
        HandlerResponse::Permission(PermissionResult::Custom(value)) => Some(value.clone()),
        _ => Some(serde_json::json!({
            "kind": pending_permission_result_kind(response),
        })),
    }
}

/// Map a handler response into the JSON-RPC `result` payload for the
/// direct-RPC path (`permission.request`).
///
/// Always returns a value. [`PermissionResult::Deferred`] is treated as
/// [`PermissionResult::Approved`] here because the JSON-RPC contract
/// requires a reply — see the variant's doc comment.
fn direct_permission_payload(response: &HandlerResponse) -> Value {
    match response {
        HandlerResponse::Permission(PermissionResult::Custom(value)) => value.clone(),
        HandlerResponse::Permission(PermissionResult::Deferred) => serde_json::to_value(
            permission_request_response(&HandlerResponse::Permission(PermissionResult::Approved)),
        )
        .expect("serializing direct permission response should succeed"),
        _ => serde_json::to_value(permission_request_response(response))
            .expect("serializing direct permission response should succeed"),
    }
}

/// Process a notification from the CLI's broadcast channel.
#[allow(clippy::too_many_arguments)]
async fn handle_notification(
    session_id: &SessionId,
    client: &Client,
    handler: &Arc<dyn SessionHandler>,
    notification: SessionEventNotification,
    idle_waiter: &Arc<Mutex<Option<IdleWaiter>>>,
    capabilities: &Arc<parking_lot::RwLock<SessionCapabilities>>,
    event_tx: &tokio::sync::broadcast::Sender<SessionEvent>,
) {
    let event = notification.event.clone();
    let event_type = event.parsed_type();

    // Signal send_and_wait if active. The lock is only contended when
    // a send_and_wait call is in flight (idle_waiter is Some).
    match event_type {
        SessionEventType::AssistantMessage
        | SessionEventType::SessionIdle
        | SessionEventType::SessionError => {
            let mut guard = idle_waiter.lock().await;
            if let Some(waiter) = guard.as_mut() {
                match event_type {
                    SessionEventType::AssistantMessage => {
                        waiter.last_assistant_message = Some(event.clone());
                    }
                    SessionEventType::SessionIdle | SessionEventType::SessionError => {
                        if let Some(waiter) = guard.take() {
                            if event_type == SessionEventType::SessionIdle {
                                let _ = waiter.tx.send(Ok(waiter.last_assistant_message));
                            } else {
                                let error_msg = event
                                    .typed_data::<SessionErrorData>()
                                    .map(|d| d.message)
                                    .or_else(|| {
                                        event
                                            .data
                                            .get("message")
                                            .and_then(|v| v.as_str())
                                            .map(|s| s.to_string())
                                    })
                                    .unwrap_or_else(|| "session error".to_string());
                                let _ = waiter
                                    .tx
                                    .send(Err(Error::Session(SessionError::AgentError(error_msg))));
                            }
                        }
                    }
                    _ => {}
                }
            }
        }
        _ => {}
    }

    // Fan out the event to runtime subscribers (`Session::subscribe`). `send`
    // only errors when there are no receivers, which is the normal case
    // before any consumer subscribes.
    let _ = event_tx.send(event.clone());

    // Fire-and-forget dispatch for the general event.
    handler
        .on_event(HandlerEvent::SessionEvent {
            session_id: session_id.clone(),
            event,
        })
        .await;

    // Update capabilities when the CLI reports changes. The CLI sends
    // the full updated capabilities object — replace wholesale so removals
    // and new subfields are handled correctly.
    if event_type == SessionEventType::CapabilitiesChanged {
        match serde_json::from_value::<SessionCapabilities>(notification.event.data.clone()) {
            Ok(changed) => *capabilities.write() = changed,
            Err(e) => warn!(error = %e, "failed to deserialize capabilities.changed payload"),
        }
    }

    // Notification-based permission/tool/elicitation requests require a
    // separate RPC callback. Spawn concurrently since the CLI doesn't block.
    match event_type {
        SessionEventType::PermissionRequested => {
            let Some(request_id) = extract_request_id(&notification.event.data) else {
                return;
            };
            let client = client.clone();
            let handler = handler.clone();
            let sid = session_id.clone();
            let data = PermissionRequestData {
                extra: notification.event.data.clone(),
            };
            tokio::spawn(async move {
                let response = handler
                    .on_event(HandlerEvent::PermissionRequest {
                        session_id: sid.clone(),
                        request_id: request_id.clone(),
                        data,
                    })
                    .await;
                let Some(result_value) = notification_permission_payload(&response) else {
                    // Handler returned Deferred — it will call
                    // handlePendingPermissionRequest itself.
                    return;
                };
                let _ = client
                    .call(
                        "session.permissions.handlePendingPermissionRequest",
                        Some(serde_json::json!({
                            "sessionId": sid,
                            "requestId": request_id,
                            "result": result_value,
                        })),
                    )
                    .await;
            });
        }
        SessionEventType::ExternalToolRequested => {
            let Some(request_id) = extract_request_id(&notification.event.data) else {
                return;
            };
            let data: ExternalToolRequestedData =
                match serde_json::from_value(notification.event.data.clone()) {
                    Ok(d) => d,
                    Err(e) => {
                        warn!(error = %e, "failed to deserialize external_tool.requested");
                        let client = client.clone();
                        let sid = session_id.clone();
                        tokio::spawn(async move {
                            let _ = client
                                .call(
                                    "session.tools.handlePendingToolCall",
                                    Some(serde_json::json!({
                                        "sessionId": sid,
                                        "requestId": request_id,
                                        "error": format!("Failed to deserialize tool request: {e}"),
                                    })),
                                )
                                .await;
                        });
                        return;
                    }
                };
            let client = client.clone();
            let handler = handler.clone();
            let sid = session_id.clone();
            tokio::spawn(async move {
                if data.tool_call_id.is_empty() || data.tool_name.is_empty() {
                    let error_msg = if data.tool_call_id.is_empty() {
                        "Missing toolCallId"
                    } else {
                        "Missing toolName"
                    };
                    let _ = client
                        .call(
                            "session.tools.handlePendingToolCall",
                            Some(serde_json::json!({
                                "sessionId": sid,
                                "requestId": request_id,
                                "error": error_msg,
                            })),
                        )
                        .await;
                    return;
                }
                let invocation = ToolInvocation {
                    session_id: sid.clone(),
                    tool_call_id: data.tool_call_id,
                    tool_name: data.tool_name,
                    arguments: data
                        .arguments
                        .unwrap_or(Value::Object(serde_json::Map::new())),
                };
                let response = handler
                    .on_event(HandlerEvent::ExternalTool { invocation })
                    .await;
                let tool_result = match response {
                    HandlerResponse::ToolResult(r) => r,
                    _ => ToolResult::Text("Unexpected handler response".to_string()),
                };
                let result_value = serde_json::to_value(&tool_result).unwrap_or(Value::Null);
                let _ = client
                    .call(
                        "session.tools.handlePendingToolCall",
                        Some(serde_json::json!({
                            "sessionId": sid,
                            "requestId": request_id,
                            "result": result_value,
                        })),
                    )
                    .await;
            });
        }
        SessionEventType::UserInputRequested => {
            let user_input_data: UserInputRequestedData =
                match serde_json::from_value(notification.event.data.clone()) {
                    Ok(d) => d,
                    Err(e) => {
                        warn!(error = %e, "failed to deserialize user_input.requested");
                        return;
                    }
                };
            let client = client.clone();
            let handler = handler.clone();
            let sid = session_id.clone();
            tokio::spawn(async move {
                let response = handler
                    .on_event(HandlerEvent::UserInput {
                        session_id: sid.clone(),
                        question: user_input_data.question,
                        choices: (!user_input_data.choices.is_empty())
                            .then_some(user_input_data.choices),
                        allow_freeform: user_input_data.allow_freeform,
                    })
                    .await;
                let result = match response {
                    HandlerResponse::UserInput(Some(UserInputResponse {
                        answer,
                        was_freeform,
                    })) => serde_json::json!({
                        "sessionId": sid,
                        "requestId": user_input_data.request_id,
                        "answer": answer,
                        "wasFreeform": was_freeform,
                    }),
                    _ => serde_json::json!({
                        "sessionId": sid,
                        "requestId": user_input_data.request_id,
                        "noResponse": true,
                    }),
                };
                let _ = client
                    .call("session.respondToUserInput", Some(result))
                    .await;
            });
        }
        SessionEventType::ElicitationRequested => {
            let Some(request_id) = extract_request_id(&notification.event.data) else {
                return;
            };
            let elicitation_data: ElicitationRequestedData =
                match serde_json::from_value(notification.event.data.clone()) {
                    Ok(d) => d,
                    Err(e) => {
                        warn!(error = %e, "failed to deserialize elicitation request");
                        return;
                    }
                };
            let request = ElicitationRequest {
                message: elicitation_data.message,
                requested_schema: elicitation_data
                    .requested_schema
                    .map(|s| serde_json::to_value(s).unwrap_or(Value::Null)),
                mode: elicitation_data.mode.map(|m| match m {
                    crate::generated::session_events::ElicitationRequestedMode::Form => {
                        crate::types::ElicitationMode::Form
                    }
                    crate::generated::session_events::ElicitationRequestedMode::Url => {
                        crate::types::ElicitationMode::Url
                    }
                    _ => crate::types::ElicitationMode::Unknown,
                }),
                elicitation_source: elicitation_data.elicitation_source,
                url: elicitation_data.url,
            };
            let client = client.clone();
            let handler = handler.clone();
            let sid = session_id.clone();
            tokio::spawn(async move {
                let cancel = ElicitationResult {
                    action: "cancel".to_string(),
                    content: None,
                };
                // Dispatch to handler inside a nested task so panics are
                // caught as JoinErrors (matches Node SDK's try/catch pattern).
                let handler_task = tokio::spawn({
                    let sid = sid.clone();
                    let request_id = request_id.clone();
                    async move {
                        handler
                            .on_event(HandlerEvent::ElicitationRequest {
                                session_id: sid,
                                request_id,
                                request,
                            })
                            .await
                    }
                });
                let result = match handler_task.await {
                    Ok(HandlerResponse::Elicitation(r)) => r,
                    _ => cancel.clone(),
                };
                if let Err(e) = client
                    .call(
                        "session.ui.handlePendingElicitation",
                        Some(serde_json::json!({
                            "sessionId": sid,
                            "requestId": request_id,
                            "result": result,
                        })),
                    )
                    .await
                {
                    // RPC failed — attempt cancel as last resort
                    warn!(error = %e, "handlePendingElicitation failed, sending cancel");
                    let _ = client
                        .call(
                            "session.ui.handlePendingElicitation",
                            Some(serde_json::json!({
                                "sessionId": sid,
                                "requestId": request_id,
                                "result": cancel,
                            })),
                        )
                        .await;
                }
            });
        }
        _ => {}
    }
}

/// Process a JSON-RPC request from the CLI.
async fn handle_request(
    session_id: &SessionId,
    client: &Client,
    handler: &Arc<dyn SessionHandler>,
    hooks: Option<&dyn SessionHooks>,
    transforms: Option<&dyn SystemMessageTransform>,
    request: crate::JsonRpcRequest,
) {
    let sid = session_id.clone();

    match request.method.as_str() {
        "hooks.invoke" => {
            let params = request.params.as_ref();
            let hook_type = params
                .and_then(|p| p.get("hookType"))
                .and_then(|v| v.as_str())
                .unwrap_or("");
            let input = params
                .and_then(|p| p.get("input"))
                .cloned()
                .unwrap_or(Value::Object(Default::default()));

            let rpc_result = if let Some(hooks) = hooks {
                match crate::hooks::dispatch_hook(hooks, &sid, hook_type, input).await {
                    Ok(output) => output,
                    Err(e) => {
                        warn!(error = %e, hook_type = hook_type, "hook dispatch failed");
                        serde_json::json!({ "output": {} })
                    }
                }
            } else {
                serde_json::json!({ "output": {} })
            };

            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(rpc_result),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "tool.call" => {
            let invocation: ToolInvocation = match request
                .params
                .as_ref()
                .and_then(|p| serde_json::from_value::<ToolInvocation>(p.clone()).ok())
            {
                Some(inv) => inv,
                None => {
                    let _ = send_error_response(
                        client,
                        request.id,
                        error_codes::INVALID_PARAMS,
                        "invalid tool.call params",
                    )
                    .await;
                    return;
                }
            };
            let response = handler
                .on_event(HandlerEvent::ExternalTool { invocation })
                .await;
            let tool_result = match response {
                HandlerResponse::ToolResult(r) => r,
                _ => ToolResult::Text("Unexpected handler response".to_string()),
            };
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(serde_json::json!(ToolResultResponse {
                    result: tool_result
                })),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "userInput.request" => {
            let params = request.params.as_ref();
            let Some(question) = params
                .and_then(|p| p.get("question"))
                .and_then(|v| v.as_str())
            else {
                warn!("userInput.request missing 'question' field");
                let rpc_response = JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request.id,
                    result: None,
                    error: Some(crate::JsonRpcError {
                        code: error_codes::INVALID_PARAMS,
                        message: "missing required field: question".to_string(),
                        data: None,
                    }),
                };
                let _ = client.send_response(&rpc_response).await;
                return;
            };
            let question = question.to_string();
            let choices = params
                .and_then(|p| p.get("choices"))
                .and_then(|v| v.as_array())
                .map(|arr| {
                    arr.iter()
                        .filter_map(|v| v.as_str().map(|s| s.to_string()))
                        .collect()
                });
            let allow_freeform = params
                .and_then(|p| p.get("allowFreeform"))
                .and_then(|v| v.as_bool());

            let response = handler
                .on_event(HandlerEvent::UserInput {
                    session_id: sid,
                    question,
                    choices,
                    allow_freeform,
                })
                .await;

            let rpc_result = match response {
                HandlerResponse::UserInput(Some(UserInputResponse {
                    answer,
                    was_freeform,
                })) => serde_json::json!({
                    "answer": answer,
                    "wasFreeform": was_freeform,
                }),
                _ => serde_json::json!({ "noResponse": true }),
            };
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(rpc_result),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "exitPlanMode.request" => {
            let params = request
                .params
                .as_ref()
                .cloned()
                .unwrap_or(Value::Object(serde_json::Map::new()));
            let data: ExitPlanModeData = match serde_json::from_value(params) {
                Ok(d) => d,
                Err(e) => {
                    warn!(error = %e, "failed to deserialize exitPlanMode.request params, using defaults");
                    ExitPlanModeData::default()
                }
            };

            let response = handler
                .on_event(HandlerEvent::ExitPlanMode {
                    session_id: sid,
                    data,
                })
                .await;

            let rpc_result = match response {
                HandlerResponse::ExitPlanMode(ExitPlanModeResult {
                    approved,
                    selected_action,
                    feedback,
                }) => serde_json::json!({
                    "approved": approved,
                    "selectedAction": selected_action,
                    "feedback": feedback,
                }),
                _ => serde_json::json!({ "approved": true }),
            };
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(rpc_result),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "permission.request" => {
            let Some(request_id) = request
                .params
                .as_ref()
                .and_then(|p| p.get("requestId"))
                .and_then(|v| v.as_str())
                .filter(|s| !s.is_empty())
            else {
                warn!("permission.request missing 'requestId' field");
                let rpc_response = JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request.id,
                    result: None,
                    error: Some(crate::JsonRpcError {
                        code: error_codes::INVALID_PARAMS,
                        message: "missing required field: requestId".to_string(),
                        data: None,
                    }),
                };
                let _ = client.send_response(&rpc_response).await;
                return;
            };
            let request_id = RequestId::new(request_id);
            let data = PermissionRequestData {
                extra: request
                    .params
                    .as_ref()
                    .cloned()
                    .unwrap_or(Value::Object(serde_json::Map::new())),
            };

            let response = handler
                .on_event(HandlerEvent::PermissionRequest {
                    session_id: sid,
                    request_id,
                    data,
                })
                .await;
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(direct_permission_payload(&response)),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "systemMessage.transform" => {
            let params = request.params.as_ref();
            let sections: HashMap<String, crate::transforms::TransformSection> =
                match params.and_then(|p| p.get("sections")) {
                    Some(v) => match serde_json::from_value(v.clone()) {
                        Ok(s) => s,
                        Err(e) => {
                            let _ = send_error_response(
                                client,
                                request.id,
                                error_codes::INVALID_PARAMS,
                                &format!("invalid sections: {e}"),
                            )
                            .await;
                            return;
                        }
                    },
                    None => {
                        let _ = send_error_response(
                            client,
                            request.id,
                            error_codes::INVALID_PARAMS,
                            "missing sections parameter",
                        )
                        .await;
                        return;
                    }
                };

            let rpc_result = if let Some(transforms) = transforms {
                let response =
                    crate::transforms::dispatch_transform(transforms, &sid, sections).await;
                match serde_json::to_value(response) {
                    Ok(v) => v,
                    Err(e) => {
                        warn!(error = %e, "failed to serialize transform response");
                        serde_json::json!({ "sections": {} })
                    }
                }
            } else {
                // No transforms registered — pass through all sections unchanged.
                let passthrough: HashMap<String, crate::transforms::TransformSection> = sections;
                serde_json::json!({ "sections": passthrough })
            };

            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(rpc_result),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        method => {
            warn!(
                method = method,
                "unhandled request method in session event loop"
            );
            let _ = send_error_response(
                client,
                request.id,
                error_codes::METHOD_NOT_FOUND,
                &format!("unknown method: {method}"),
            )
            .await;
        }
    }
}

async fn send_error_response(
    client: &Client,
    id: u64,
    code: i32,
    message: &str,
) -> Result<(), Error> {
    let response = JsonRpcResponse {
        jsonrpc: "2.0".to_string(),
        id,
        result: None,
        error: Some(crate::JsonRpcError {
            code,
            message: message.to_string(),
            data: None,
        }),
    };
    client.send_response(&response).await
}

/// Inject `action: "transform"` sections into a `SystemMessageConfig`,
/// forcing `mode: "customize"` (required by the CLI for transforms to fire).
/// Preserves any existing caller-provided section overrides.
fn apply_transform_sections(
    sys_msg: &mut SystemMessageConfig,
    transforms: &dyn SystemMessageTransform,
) {
    sys_msg.mode = Some("customize".to_string());
    let sections = sys_msg.sections.get_or_insert_with(HashMap::new);
    for id in transforms.section_ids() {
        sections.entry(id).or_insert_with(|| SectionOverride {
            action: Some("transform".to_string()),
            content: None,
        });
    }
}

fn inject_transform_sections(config: &mut SessionConfig, transforms: &dyn SystemMessageTransform) {
    let sys_msg = config.system_message.get_or_insert_with(Default::default);
    apply_transform_sections(sys_msg, transforms);
}

fn inject_transform_sections_resume(
    config: &mut ResumeSessionConfig,
    transforms: &dyn SystemMessageTransform,
) {
    let sys_msg = config.system_message.get_or_insert_with(Default::default);
    apply_transform_sections(sys_msg, transforms);
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::{
        direct_permission_payload, notification_permission_payload, pending_permission_result_kind,
        permission_request_response,
    };
    use crate::handler::{HandlerResponse, PermissionResult};

    #[test]
    fn pending_permission_requests_use_decision_kinds() {
        assert_eq!(
            pending_permission_result_kind(&HandlerResponse::Permission(
                PermissionResult::Approved,
            )),
            "approve-once"
        );
        assert_eq!(
            pending_permission_result_kind(&HandlerResponse::Permission(PermissionResult::Denied)),
            "reject"
        );
        assert_eq!(
            pending_permission_result_kind(&HandlerResponse::Ok),
            "user-not-available"
        );
    }

    #[test]
    fn direct_permission_requests_use_decision_response_kinds() {
        assert_eq!(
            serde_json::to_value(permission_request_response(&HandlerResponse::Permission(
                PermissionResult::Approved
            ),))
            .expect("serializing approved permission response should succeed"),
            json!({ "kind": "approve-once" })
        );
        assert_eq!(
            serde_json::to_value(permission_request_response(&HandlerResponse::Permission(
                PermissionResult::Denied
            ),))
            .expect("serializing denied permission response should succeed"),
            json!({ "kind": "reject" })
        );
        assert_eq!(
            serde_json::to_value(permission_request_response(&HandlerResponse::Ok))
                .expect("serializing fallback permission response should succeed"),
            json!({ "kind": "reject" })
        );
    }

    #[test]
    fn notification_payload_handles_deferred_and_custom() {
        // Deferred → no payload, SDK must not respond.
        assert!(
            notification_permission_payload(&HandlerResponse::Permission(
                PermissionResult::Deferred,
            ))
            .is_none()
        );

        // Custom → handler-supplied value passed through verbatim.
        let custom = json!({
            "kind": "approve-and-remember",
            "allowlist": ["ls", "grep"],
        });
        assert_eq!(
            notification_permission_payload(&HandlerResponse::Permission(
                PermissionResult::Custom(custom.clone()),
            )),
            Some(custom)
        );

        // Approved/Denied → existing kind-only shape.
        assert_eq!(
            notification_permission_payload(&HandlerResponse::Permission(
                PermissionResult::Approved,
            )),
            Some(json!({ "kind": "approve-once" }))
        );
        assert_eq!(
            notification_permission_payload(
                &HandlerResponse::Permission(PermissionResult::Denied,)
            ),
            Some(json!({ "kind": "reject" }))
        );
    }

    #[test]
    fn direct_payload_handles_deferred_and_custom() {
        // Custom → handler-supplied value passed through verbatim.
        let custom = json!({
            "kind": "approve-and-remember",
            "allowlist": ["ls", "grep"],
        });
        assert_eq!(
            direct_permission_payload(&HandlerResponse::Permission(PermissionResult::Custom(
                custom.clone(),
            ))),
            custom
        );

        // Deferred → falls back to Approved because the direct RPC must reply.
        assert_eq!(
            direct_permission_payload(&HandlerResponse::Permission(PermissionResult::Deferred)),
            json!({ "kind": "approve-once" })
        );

        // Approved/Denied → existing kind-only shape.
        assert_eq!(
            direct_permission_payload(&HandlerResponse::Permission(PermissionResult::Approved)),
            json!({ "kind": "approve-once" })
        );
        assert_eq!(
            direct_permission_payload(&HandlerResponse::Permission(PermissionResult::Denied)),
            json!({ "kind": "reject" })
        );
    }
}
