use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;

use serde::de::DeserializeOwned;
use serde_json::Value;
use tokio::sync::oneshot;
use tokio::task::JoinHandle;
use tracing::warn;

use crate::generated::session_events::ElicitationRequestedDataMode;
use crate::handler::{
    ExitPlanModeResult, HandlerEvent, HandlerResponse, PermissionResult, SessionHandler,
    UserInputResponse,
};
use crate::hooks::SessionHooks;
use crate::transforms::SystemMessageTransform;
use crate::types::{
    ensure_attachment_display_names, CreateSessionResult, ElicitationMode, ElicitationRequest,
    ElicitationResult, GetMessagesResponse, InputOptions, LogOptions, MessageOptions, RequestId,
    ResumeSessionConfig, SectionOverride, SendAndWaitResult, SessionCapabilities, SessionConfig,
    SessionEvent, SessionEventData, SessionEventType, SessionFsAppendFileRequest,
    SessionFsExistsRequest, SessionFsHandler, SessionFsMkdirRequest, SessionFsReadFileRequest,
    SessionFsReaddirRequest, SessionFsReaddirWithTypesRequest, SessionFsRenameRequest,
    SessionFsRmRequest, SessionFsStatRequest, SessionFsWriteFileRequest, SessionId,
    SetModelOptions, SystemMessageConfig, ToolInvocation, ToolResult, ToolResultResponse,
    UiCapabilities,
};
use crate::{error_codes, Client, Error, JsonRpcResponse, SessionError, SessionEventNotification};

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
/// Protocol methods (`send_message`, `get_messages`, `abort`, etc.) automatically
/// inject the session ID into RPC params.
///
/// Call [`disconnect`](Self::disconnect) for graceful cleanup (RPC + local). If dropped
/// without calling `disconnect`, the `Drop` impl aborts the event loop and
/// unregisters from the router as a best-effort safety net.
#[must_use]
pub struct Session {
    id: SessionId,
    cwd: PathBuf,
    workspace_path: Option<PathBuf>,
    remote_url: Option<String>,
    client: Client,
    event_loop: parking_lot::Mutex<Option<JoinHandle<()>>>,
    /// Only populated while a `send_and_wait` call is in flight.
    idle_waiter: Arc<parking_lot::Mutex<Option<IdleWaiter>>>,
    /// Capabilities negotiated with the CLI, updated on `capabilities.changed` events.
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
}

impl Session {
    /// Session ID assigned by the CLI.
    pub fn id(&self) -> &SessionId {
        &self.id
    }

    /// Working directory of the CLI process.
    pub fn cwd(&self) -> &Path {
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

    /// The underlying Client (for advanced use cases).
    pub fn client(&self) -> &Client {
        &self.client
    }

    /// Stop the internal event loop. Called automatically on [`disconnect`](Self::disconnect).
    pub async fn stop_event_loop(&self) {
        let handle = self.event_loop.lock().take();
        if let Some(handle) = handle {
            handle.abort();
            let _ = handle.await;
        }
        // Fail any pending send_and_wait so it returns immediately.
        if let Some(waiter) = self.idle_waiter.lock().take() {
            let _ = waiter
                .tx
                .send(Err(Error::Session(SessionError::EventLoopClosed)));
        }
    }

    /// Send a user message to the agent.
    ///
    /// Returns the message ID assigned by the CLI.
    ///
    /// Returns an error if a [`send_and_wait`](Self::send_and_wait) call is
    /// currently in flight, since the plain send would race with the waiter.
    pub async fn send_message(&self, options: MessageOptions) -> Result<String, Error> {
        if self.idle_waiter.lock().is_some() {
            return Err(Error::Session(SessionError::SendWhileWaiting));
        }
        self.send_message_inner(options).await
    }

    async fn send_message_inner(&self, options: MessageOptions) -> Result<String, Error> {
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "prompt": options.prompt,
        });
        if let Some(m) = options.mode {
            params["mode"] = Value::String(m);
        }
        if let Some(mut a) = options.attachments {
            ensure_attachment_display_names(&mut a);
            params["attachments"] = serde_json::to_value(a)?;
        }
        if let Some(headers) = options.request_headers {
            params["requestHeaders"] = serde_json::to_value(headers)?;
        }
        let result = self.client.call("session.send", Some(params)).await?;
        let message_id = result
            .get("messageId")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string();
        Ok(message_id)
    }

    /// Send a user message and wait for the agent to finish processing.
    ///
    /// Blocks until `session.idle` (success) or `session.error` (failure),
    /// returning the message ID and the last `assistant.message` event
    /// captured during streaming.
    /// Times out after `timeout` (default 60 seconds).
    ///
    /// Only one `send_and_wait` call may be active per session at a time.
    /// Calling [`send_message`](Self::send_message) while a `send_and_wait`
    /// is in flight will also return an error.
    pub async fn send_and_wait(
        &self,
        options: MessageOptions,
        timeout: Option<Duration>,
    ) -> Result<SendAndWaitResult, Error> {
        let (tx, rx) = oneshot::channel();

        {
            let mut guard = self.idle_waiter.lock();
            if guard.is_some() {
                return Err(Error::Session(SessionError::SendWhileWaiting));
            }
            *guard = Some(IdleWaiter {
                tx,
                last_assistant_message: None,
            });
        }

        let timeout_duration = timeout.unwrap_or(Duration::from_secs(60));
        let result = tokio::time::timeout(timeout_duration, async {
            let message_id = match self.send_message_inner(options).await {
                Ok(id) => id,
                Err(e) => {
                    self.idle_waiter.lock().take();
                    return Err(e);
                }
            };

            match rx.await {
                Ok(Ok(event)) => Ok(SendAndWaitResult { message_id, event }),
                Ok(Err(e)) => Err(e),
                Err(_) => {
                    self.idle_waiter.lock().take();
                    Err(Error::Session(SessionError::EventLoopClosed))
                }
            }
        })
        .await;

        match result {
            Ok(inner) => inner,
            Err(_) => {
                self.idle_waiter.lock().take();
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
    pub async fn set_model(
        &self,
        model: &str,
        reasoning_effort: Option<&str>,
    ) -> Result<Option<String>, Error> {
        let options = reasoning_effort.map_or_else(SetModelOptions::new, |effort| {
            SetModelOptions::new().with_reasoning_effort(effort)
        });
        self.set_model_with_options(model, options).await
    }

    /// Switch to a different model with additional override options.
    pub async fn set_model_with_options(
        &self,
        model: &str,
        options: SetModelOptions,
    ) -> Result<Option<String>, Error> {
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "modelId": model,
        });
        if let Some(effort) = options.reasoning_effort {
            params["reasoningEffort"] = Value::String(effort);
        }
        if let Some(model_capabilities) = options.model_capabilities {
            params["modelCapabilities"] = serde_json::to_value(model_capabilities)?;
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

    /// Disconnect from the session on the CLI.
    ///
    /// Sends `session.destroy` to the CLI (which preserves on-disk state for
    /// later resumption via [`Client::resume_session`]), stops the event loop,
    /// and unregisters from the router.
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

    /// Write a log message to the session.
    pub async fn log(&self, message: &str, options: Option<&LogOptions>) -> Result<(), Error> {
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "message": message,
        });
        if let Some(opts) = options {
            if let Some(level) = &opts.level {
                params["level"] = Value::String(level.as_str().to_string());
            }
            if let Some(ephemeral) = opts.ephemeral {
                params["ephemeral"] = Value::Bool(ephemeral);
            }
        }
        self.client.call("session.log", Some(params)).await?;
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

    /// Generic RPC forwarder — auto-injects sessionId into params.
    ///
    /// Useful as an escape hatch for RPC methods not yet covered by
    /// first-class Session methods.
    pub async fn call_rpc(
        &self,
        method: &str,
        extra_params: Option<Value>,
    ) -> Result<Value, Error> {
        let mut params = serde_json::json!({ "sessionId": self.id });
        if let Some(extra) = extra_params {
            if let (Some(base), Some(extra_obj)) = (params.as_object_mut(), extra.as_object()) {
                for (k, v) in extra_obj {
                    base.insert(k.clone(), v.clone());
                }
            }
        }
        self.client.call(method, Some(params)).await
    }
}

impl Drop for Session {
    fn drop(&mut self) {
        if let Some(handle) = self.event_loop.lock().take() {
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
    /// If `hooks` is provided, the `hooks` flag in the config is automatically
    /// set to `true` so the CLI sends `hooks.invoke` RPC requests.
    ///
    /// If `transforms` is provided, the SDK injects `action: "transform"`
    /// sections into the [`SystemMessageConfig`] wire format and handles
    /// `systemMessage.transform` RPC callbacks during the session.
    pub async fn create_session(
        &self,
        config: SessionConfig,
        handler: Arc<dyn SessionHandler>,
        hooks: Option<Arc<dyn SessionHooks>>,
        transforms: Option<Arc<dyn SystemMessageTransform>>,
    ) -> Result<Session, Error> {
        self.create_session_with_session_fs(config, handler, hooks, transforms, None)
            .await
    }

    /// Create a new session with a per-session filesystem handler.
    pub async fn create_session_with_session_fs(
        &self,
        mut config: SessionConfig,
        handler: Arc<dyn SessionHandler>,
        hooks: Option<Arc<dyn SessionHooks>>,
        transforms: Option<Arc<dyn SystemMessageTransform>>,
        session_fs: Option<Arc<dyn SessionFsHandler>>,
    ) -> Result<Session, Error> {
        if self.inner.session_fs.is_some() && session_fs.is_none() {
            return Err(Error::InvalidConfig(
                "session_fs requires create_session_with_session_fs(..., Some(handler))".into(),
            ));
        }
        if self.inner.session_fs.is_none() && session_fs.is_some() {
            return Err(Error::InvalidConfig(
                "session filesystem handler requires ClientOptions::session_fs".into(),
            ));
        }

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

        let idle_waiter = Arc::new(parking_lot::Mutex::new(None));
        let event_loop = spawn_event_loop(
            session_id.clone(),
            self.clone(),
            handler,
            hooks,
            transforms,
            session_fs,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
        );

        Ok(Session {
            id: session_id,
            cwd: self.cwd().to_path_buf(),
            workspace_path: create_result.workspace_path,
            remote_url: create_result.remote_url,
            client: self.clone(),
            event_loop: parking_lot::Mutex::new(Some(event_loop)),
            idle_waiter,
            capabilities,
        })
    }

    /// Resume an existing session on the CLI.
    ///
    /// Sends `session.resume` and `session.skills.reload`, registers the
    /// session on the router, and spawns the event loop.
    ///
    /// If `hooks` is provided, the `hooks` flag in the config is automatically
    /// set to `true` so the CLI sends `hooks.invoke` RPC requests.
    ///
    /// If `transforms` is provided, the SDK injects `action: "transform"`
    /// sections into the [`SystemMessageConfig`] wire format and handles
    /// `systemMessage.transform` RPC callbacks during the session.
    pub async fn resume_session(
        &self,
        config: ResumeSessionConfig,
        handler: Arc<dyn SessionHandler>,
        hooks: Option<Arc<dyn SessionHooks>>,
        transforms: Option<Arc<dyn SystemMessageTransform>>,
    ) -> Result<Session, Error> {
        self.resume_session_with_session_fs(config, handler, hooks, transforms, None)
            .await
    }

    /// Resume a session with a per-session filesystem handler.
    pub async fn resume_session_with_session_fs(
        &self,
        mut config: ResumeSessionConfig,
        handler: Arc<dyn SessionHandler>,
        hooks: Option<Arc<dyn SessionHooks>>,
        transforms: Option<Arc<dyn SystemMessageTransform>>,
        session_fs: Option<Arc<dyn SessionFsHandler>>,
    ) -> Result<Session, Error> {
        if self.inner.session_fs.is_some() && session_fs.is_none() {
            return Err(Error::InvalidConfig(
                "session_fs requires resume_session_with_session_fs(..., Some(handler))".into(),
            ));
        }
        if self.inner.session_fs.is_none() && session_fs.is_some() {
            return Err(Error::InvalidConfig(
                "session filesystem handler requires ClientOptions::session_fs".into(),
            ));
        }

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

        let idle_waiter = Arc::new(parking_lot::Mutex::new(None));
        let event_loop = spawn_event_loop(
            cli_session_id.clone(),
            self.clone(),
            handler,
            hooks,
            transforms,
            session_fs,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
        );

        Ok(Session {
            id: cli_session_id,
            cwd: self.cwd().to_path_buf(),
            workspace_path: None,
            remote_url,
            client: self.clone(),
            event_loop: parking_lot::Mutex::new(Some(event_loop)),
            idle_waiter,
            capabilities,
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
    session_fs: Option<Arc<dyn SessionFsHandler>>,
    channels: crate::router::SessionChannels,
    idle_waiter: Arc<parking_lot::Mutex<Option<IdleWaiter>>>,
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
) -> JoinHandle<()> {
    let crate::router::SessionChannels {
        mut notifications,
        mut requests,
    } = channels;

    tokio::spawn(async move {
        loop {
            tokio::select! {
                Some(notification) = notifications.recv() => {
                    handle_notification(
                        &session_id, &client, &handler, notification, &idle_waiter, &capabilities,
                    ).await;
                }
                Some(request) = requests.recv() => {
                    handle_request(
                        &session_id,
                        &client,
                        &handler,
                        hooks.as_deref(),
                        transforms.as_deref(),
                        session_fs.as_deref(),
                        request,
                    ).await;
                }
                else => break,
            }
        }
        // Channels closed — fail any pending send_and_wait.
        if let Some(waiter) = idle_waiter.lock().take() {
            let _ = waiter
                .tx
                .send(Err(Error::Session(SessionError::EventLoopClosed)));
        }
    })
}

/// Process a notification from the CLI's broadcast channel.
async fn handle_notification(
    session_id: &SessionId,
    client: &Client,
    handler: &Arc<dyn SessionHandler>,
    notification: SessionEventNotification,
    idle_waiter: &Arc<parking_lot::Mutex<Option<IdleWaiter>>>,
    capabilities: &Arc<parking_lot::RwLock<SessionCapabilities>>,
) {
    // Signal send_and_wait if active. The lock is only contended when
    // a send_and_wait call is in flight (idle_waiter is Some).
    match &notification.event.event_type {
        SessionEventType::AssistantMessage => {
            let mut guard = idle_waiter.lock();
            if let Some(waiter) = guard.as_mut() {
                waiter.last_assistant_message = Some(notification.event.clone());
            }
        }
        SessionEventType::SessionIdle => {
            if let Some(waiter) = idle_waiter.lock().take() {
                let _ = waiter.tx.send(Ok(waiter.last_assistant_message));
            }
        }
        SessionEventType::SessionError => {
            if let Some(waiter) = idle_waiter.lock().take() {
                let error_msg = match &notification.event.data {
                    SessionEventData::SessionError(d) => d.message.clone(),
                    _ => "session error".to_string(),
                };
                let _ = waiter
                    .tx
                    .send(Err(Error::Session(SessionError::AgentError(error_msg))));
            }
        }
        _ => {}
    }

    // Update capabilities when the CLI reports changes. The CLI sends
    // the full updated capabilities object — replace wholesale so removals
    // and new subfields are handled correctly.
    if let SessionEventData::CapabilitiesChanged(d) = &notification.event.data {
        *capabilities.write() = SessionCapabilities {
            ui: d.ui.as_ref().map(|u| UiCapabilities {
                elicitation: u.elicitation,
            }),
        };
    }

    // Fire-and-forget dispatch for the general event.
    handler
        .on_event(HandlerEvent::SessionEvent {
            session_id: session_id.clone(),
            event: Box::new(notification.event.clone()),
        })
        .await;

    // Notification-based permission/tool/elicitation requests require a
    // separate RPC callback. Spawn concurrently since the CLI doesn't block.
    match &notification.event.data {
        SessionEventData::PermissionRequested(d) => {
            let request_id = RequestId::new(&d.request_id);
            let client = client.clone();
            let handler = handler.clone();
            let sid = session_id.clone();
            let data = serde_json::to_value(&notification.event.data)
                .unwrap_or(Value::Object(Default::default()));
            tokio::spawn(async move {
                let response = handler
                    .on_event(HandlerEvent::PermissionRequest {
                        session_id: sid.clone(),
                        request_id: request_id.clone(),
                        data,
                    })
                    .await;
                // NoResult means "leave the pending request unanswered" —
                // skip the callback entirely so the CLI can ask again later.
                let result_kind = match response {
                    HandlerResponse::Permission(PermissionResult::Approved) => "approved",
                    HandlerResponse::Permission(PermissionResult::DeniedByRules) => {
                        "denied-by-rules"
                    }
                    HandlerResponse::Permission(PermissionResult::DeniedByUser) => {
                        "denied-interactively-by-user"
                    }
                    HandlerResponse::Permission(PermissionResult::DeniedNoApprovalRule) => {
                        "denied-no-approval-rule-and-could-not-request-from-user"
                    }
                    HandlerResponse::Permission(PermissionResult::NoResult) => return,
                    _ => "denied-no-approval-rule-and-could-not-request-from-user",
                };
                let _ = client
                    .call(
                        "session.permissions.handlePendingPermissionRequest",
                        Some(serde_json::json!({
                            "sessionId": sid,
                            "requestId": request_id,
                            "result": { "kind": result_kind },
                        })),
                    )
                    .await;
            });
        }
        SessionEventData::ExternalToolRequested(d) => {
            let request_id = RequestId::new(&d.request_id);
            let tool_call_id = d.tool_call_id.clone();
            let tool_name = d.tool_name.clone();
            let client = client.clone();
            let handler = handler.clone();
            let sid = session_id.clone();
            let arguments = d
                .arguments
                .clone()
                .unwrap_or(Value::Object(serde_json::Map::new()));
            tokio::spawn(async move {
                if tool_call_id.is_empty() || tool_name.is_empty() {
                    let error_msg = if tool_call_id.is_empty() {
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
                    tool_call_id,
                    tool_name,
                    arguments,
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
        SessionEventData::ElicitationRequested(d) => {
            let request_id = RequestId::new(&d.request_id);
            let request = ElicitationRequest {
                message: d.message.clone(),
                mode: d.mode.as_ref().map(|m| match m {
                    ElicitationRequestedDataMode::Form => ElicitationMode::Form,
                    ElicitationRequestedDataMode::Url => ElicitationMode::Url,
                    ElicitationRequestedDataMode::Unknown => ElicitationMode::Unknown,
                }),
                requested_schema: d
                    .requested_schema
                    .as_ref()
                    .and_then(|s| serde_json::to_value(s).ok()),
                elicitation_source: d.elicitation_source.clone(),
                url: d.url.clone(),
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
    session_fs: Option<&dyn SessionFsHandler>,
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
            let data = request
                .params
                .as_ref()
                .cloned()
                .unwrap_or(Value::Object(serde_json::Map::new()));

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
            let data = request
                .params
                .as_ref()
                .cloned()
                .unwrap_or(Value::Object(serde_json::Map::new()));

            let response = handler
                .on_event(HandlerEvent::PermissionRequest {
                    session_id: sid,
                    request_id,
                    data,
                })
                .await;

            let result_kind = match response {
                HandlerResponse::Permission(PermissionResult::Approved) => "approved",
                HandlerResponse::Permission(PermissionResult::DeniedByRules) => "denied-by-rules",
                HandlerResponse::Permission(PermissionResult::DeniedByUser) => {
                    "denied-interactively-by-user"
                }
                HandlerResponse::Permission(PermissionResult::DeniedNoApprovalRule) => {
                    "denied-no-approval-rule-and-could-not-request-from-user"
                }
                HandlerResponse::Permission(PermissionResult::NoResult) => {
                    // NoResult is only valid for notification-based flows.
                    // For request-based permission.request, the CLI is blocked
                    // waiting for a response — we must send one.
                    warn!("PermissionResult::NoResult is invalid for permission.request; denying");
                    "denied-no-approval-rule-and-could-not-request-from-user"
                }
                _ => "denied-no-approval-rule-and-could-not-request-from-user",
            };
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(serde_json::json!({
                    "result": { "kind": result_kind },
                })),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "sessionFs.readFile" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsReadFileRequest>(client, request.id, &request)
                    .await
            else {
                return;
            };

            match session_fs.read_file(&params).await {
                Ok(result) => {
                    let _ = send_success_response(client, request.id, result).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.writeFile" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsWriteFileRequest>(client, request.id, &request)
                    .await
            else {
                return;
            };

            match session_fs.write_file(&params).await {
                Ok(()) => {
                    let _ = send_success_response(client, request.id, Value::Null).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.appendFile" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsAppendFileRequest>(client, request.id, &request)
                    .await
            else {
                return;
            };

            match session_fs.append_file(&params).await {
                Ok(()) => {
                    let _ = send_success_response(client, request.id, Value::Null).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.exists" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsExistsRequest>(client, request.id, &request).await
            else {
                return;
            };

            match session_fs.exists(&params).await {
                Ok(result) => {
                    let _ = send_success_response(client, request.id, result).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.stat" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsStatRequest>(client, request.id, &request).await
            else {
                return;
            };

            match session_fs.stat(&params).await {
                Ok(result) => {
                    let _ = send_success_response(client, request.id, result).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.mkdir" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsMkdirRequest>(client, request.id, &request).await
            else {
                return;
            };

            match session_fs.mkdir(&params).await {
                Ok(()) => {
                    let _ = send_success_response(client, request.id, Value::Null).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.readdir" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsReaddirRequest>(client, request.id, &request).await
            else {
                return;
            };

            match session_fs.readdir(&params).await {
                Ok(result) => {
                    let _ = send_success_response(client, request.id, result).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.readdirWithTypes" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) = parse_request_params::<SessionFsReaddirWithTypesRequest>(
                client, request.id, &request,
            )
            .await
            else {
                return;
            };

            match session_fs.readdir_with_types(&params).await {
                Ok(result) => {
                    let _ = send_success_response(client, request.id, result).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.rm" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsRmRequest>(client, request.id, &request).await
            else {
                return;
            };

            match session_fs.rm(&params).await {
                Ok(()) => {
                    let _ = send_success_response(client, request.id, Value::Null).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
        }

        "sessionFs.rename" => {
            let Some(session_fs) = session_fs else {
                let _ = send_missing_session_fs_handler(client, request.id, &sid).await;
                return;
            };
            let Some(params) =
                parse_request_params::<SessionFsRenameRequest>(client, request.id, &request).await
            else {
                return;
            };

            match session_fs.rename(&params).await {
                Ok(()) => {
                    let _ = send_success_response(client, request.id, Value::Null).await;
                }
                Err(err) => {
                    let _ = send_handler_error_response(client, request.id, &err).await;
                }
            }
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

async fn send_success_response<T: serde::Serialize>(
    client: &Client,
    id: u64,
    result: T,
) -> Result<(), Error> {
    let response = JsonRpcResponse {
        jsonrpc: "2.0".to_string(),
        id,
        result: Some(serde_json::to_value(result)?),
        error: None,
    };
    client.send_response(&response).await
}

async fn send_handler_error_response(client: &Client, id: u64, error: &Error) -> Result<(), Error> {
    send_error_response(client, id, error_codes::INTERNAL_ERROR, &error.to_string()).await
}

async fn send_missing_session_fs_handler(
    client: &Client,
    id: u64,
    session_id: &SessionId,
) -> Result<(), Error> {
    send_error_response(
        client,
        id,
        error_codes::INTERNAL_ERROR,
        &format!("No sessionFs handler registered for session: {session_id}"),
    )
    .await
}

async fn parse_request_params<T: DeserializeOwned>(
    client: &Client,
    id: u64,
    request: &crate::JsonRpcRequest,
) -> Option<T> {
    match request
        .params
        .as_ref()
        .and_then(|params| serde_json::from_value::<T>(params.clone()).ok())
    {
        Some(params) => Some(params),
        None => {
            let _ = send_error_response(
                client,
                id,
                error_codes::INVALID_PARAMS,
                &format!("invalid {} params", request.method),
            )
            .await;
            None
        }
    }
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
    use super::*;
    use std::path::PathBuf;
    use std::sync::Arc;

    use async_trait::async_trait;
    use tokio::io::{duplex, AsyncRead, AsyncReadExt};

    struct NoopHandler;

    #[async_trait]
    impl SessionHandler for NoopHandler {
        async fn on_event(&self, _event: HandlerEvent) -> HandlerResponse {
            HandlerResponse::Ok
        }
    }

    struct TestSessionFsHandler;

    #[async_trait]
    impl SessionFsHandler for TestSessionFsHandler {
        async fn read_file(
            &self,
            request: &SessionFsReadFileRequest,
        ) -> Result<crate::SessionFsReadFileResult, Error> {
            assert_eq!(request.session_id, "session-1");
            assert_eq!(request.path, "notes.txt");
            Ok(crate::SessionFsReadFileResult {
                content: "hello".to_string(),
            })
        }

        async fn write_file(&self, _request: &SessionFsWriteFileRequest) -> Result<(), Error> {
            Ok(())
        }

        async fn append_file(&self, _request: &SessionFsAppendFileRequest) -> Result<(), Error> {
            Ok(())
        }

        async fn exists(
            &self,
            _request: &SessionFsExistsRequest,
        ) -> Result<crate::SessionFsExistsResult, Error> {
            Ok(crate::SessionFsExistsResult { exists: false })
        }

        async fn stat(
            &self,
            _request: &SessionFsStatRequest,
        ) -> Result<crate::SessionFsStatResult, Error> {
            Ok(crate::SessionFsStatResult::default())
        }

        async fn mkdir(&self, _request: &SessionFsMkdirRequest) -> Result<(), Error> {
            Ok(())
        }

        async fn readdir(
            &self,
            _request: &SessionFsReaddirRequest,
        ) -> Result<crate::SessionFsReaddirResult, Error> {
            Ok(crate::SessionFsReaddirResult::default())
        }

        async fn readdir_with_types(
            &self,
            _request: &SessionFsReaddirWithTypesRequest,
        ) -> Result<crate::SessionFsReaddirWithTypesResult, Error> {
            Ok(crate::SessionFsReaddirWithTypesResult::default())
        }

        async fn rm(&self, _request: &SessionFsRmRequest) -> Result<(), Error> {
            Ok(())
        }

        async fn rename(&self, _request: &SessionFsRenameRequest) -> Result<(), Error> {
            Ok(())
        }
    }

    async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> serde_json::Value {
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            reader.read_exact(&mut byte).await.unwrap();
            header.push(byte[0] as char);
            if header.ends_with("\r\n\r\n") {
                break;
            }
        }

        let length: usize = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut buf = vec![0u8; length];
        reader.read_exact(&mut buf).await.unwrap();
        serde_json::from_slice(&buf).unwrap()
    }

    #[tokio::test]
    async fn handle_request_dispatches_session_fs_read_file() {
        let (client_write, mut server_read) = duplex(4096);
        let (_server_write, client_read) = duplex(4096);
        let handler: Arc<dyn SessionHandler> = Arc::new(NoopHandler);
        let client = Client::from_transport(
            client_read,
            client_write,
            None,
            std::env::temp_dir(),
            None,
            None,
        )
        .unwrap();

        handle_request(
            &"session-1".into(),
            &client,
            &handler,
            None,
            None,
            Some(&TestSessionFsHandler),
            crate::JsonRpcRequest {
                jsonrpc: "2.0".to_string(),
                id: 7,
                method: "sessionFs.readFile".to_string(),
                params: Some(serde_json::json!({
                    "sessionId": "session-1",
                    "path": "notes.txt",
                })),
            },
        )
        .await;

        let response = read_framed(&mut server_read).await;
        assert_eq!(response["id"], 7);
        assert_eq!(response["result"]["content"], "hello");
    }

    #[tokio::test]
    async fn create_session_requires_session_fs_handler_when_enabled() {
        let (client_write, _server_read) = duplex(4096);
        let (_server_write, client_read) = duplex(4096);
        let client = Client::from_transport(
            client_read,
            client_write,
            None,
            std::env::temp_dir(),
            None,
            Some(crate::SessionFsConfig {
                initial_cwd: PathBuf::from("/repo"),
                session_state_path: PathBuf::from("/repo/.session-state"),
                conventions: crate::SessionFsConventions::Posix,
            }),
        )
        .unwrap();

        let error = match client
            .create_session(SessionConfig::default(), Arc::new(NoopHandler), None, None)
            .await
        {
            Ok(_) => panic!("expected create_session to fail without a session_fs handler"),
            Err(error) => error,
        };

        assert!(matches!(error, Error::InvalidConfig(_)));
    }
}
