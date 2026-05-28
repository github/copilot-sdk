use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::{Duration, Instant};

use parking_lot::Mutex as ParkingLotMutex;
use serde_json::Value;
use tokio::sync::oneshot;
use tokio::task::JoinHandle;
use tokio_util::sync::CancellationToken;
use tracing::{Instrument, warn};

use crate::canvas::CanvasHandler;
use crate::generated::api_types::{LogRequest, ModelSwitchToRequest, OpenCanvasInstance};
use crate::generated::session_events::{
    CommandExecuteData, ElicitationRequestedData, ExternalToolRequestedData, SessionErrorData,
    SessionEventType,
};
use crate::handler::{
    AutoModeSwitchHandler, AutoModeSwitchResponse, ElicitationHandler, ExitPlanModeHandler,
    PermissionHandler, PermissionResult, UserInputHandler, UserInputResponse,
};
use crate::hooks::SessionHooks;
use crate::session_fs::SessionFsProvider;
use crate::trace_context::inject_trace_context;
use crate::transforms::SystemMessageTransform;
use crate::types::{
    CommandContext, CommandDefinition, CommandHandler, CreateSessionResult, ElicitationRequest,
    ElicitationResult, ExitPlanModeData, GetMessagesResponse, MessageOptions,
    PermissionRequestData, RequestId, ResumeSessionConfig, ResumeSessionResult, SectionOverride,
    SessionCapabilities, SessionConfig, SessionEvent, SessionId, SetModelOptions,
    SystemMessageConfig, ToolInvocation, ToolResult, ToolResultExpanded, TraceContext,
    UiInputOptions, ensure_attachment_display_names,
};
use crate::{
    Client, Error, ErrorKind, JsonRpcResponse, SessionErrorKind, SessionEventNotification,
    error_codes,
};

/// Bundle of the per-session callbacks the SDK dispatches to. Built from a
/// [`SessionConfig`] / [`ResumeSessionConfig`] at
/// [`Client::create_session`] / [`Client::resume_session`] time. Each
/// field is `None` (or an empty map for tools) when the caller didn't
/// install a handler -- in that case the SDK skips dispatch for that
/// event type. The wire flags on `session.create` / `session.resume`
/// are derived from these fields.
#[derive(Clone)]
pub(crate) struct SessionHandlers {
    pub permission: Option<Arc<dyn PermissionHandler>>,
    pub elicitation: Option<Arc<dyn ElicitationHandler>>,
    pub user_input: Option<Arc<dyn UserInputHandler>>,
    pub exit_plan_mode: Option<Arc<dyn ExitPlanModeHandler>>,
    pub auto_mode_switch: Option<Arc<dyn AutoModeSwitchHandler>>,
    pub tools: Arc<HashMap<String, Arc<dyn crate::tool::ToolHandler>>>,
}

/// Shared state between a [`Session`] and its event loop, used by [`Session::send_and_wait`].
struct IdleWaiter {
    tx: oneshot::Sender<Result<Option<SessionEvent>, Error>>,
    last_assistant_message: Option<SessionEvent>,
    started_at: Instant,
    first_assistant_message_seen: bool,
}

/// RAII guard that clears the [`Session::idle_waiter`] slot on drop. Used
/// by [`Session::send_and_wait`] to ensure the slot doesn't leak if the
/// caller's future is cancelled (outer `tokio::time::timeout` / `select!`
/// / dropped JoinHandle). Synchronous clear via `parking_lot::Mutex` —
/// no async drop needed.
///
/// Without this, an outer cancellation between "install waiter" and
/// "drain channel" would leave the slot occupied, causing all subsequent
/// `send` and `send_and_wait` calls on the session to return
/// [`SendWhileWaiting`](SessionErrorKind::SendWhileWaiting). Closes RFD-400
/// review finding #2.
struct WaiterGuard {
    slot: Arc<ParkingLotMutex<Option<IdleWaiter>>>,
}

impl Drop for WaiterGuard {
    fn drop(&mut self) {
        self.slot.lock().take();
    }
}

struct PendingSessionRegistration {
    client: Client,
    session_id: SessionId,
    shutdown: CancellationToken,
    disarmed: bool,
}

impl PendingSessionRegistration {
    fn new(client: Client, session_id: SessionId, shutdown: CancellationToken) -> Self {
        Self {
            client,
            session_id,
            shutdown,
            disarmed: false,
        }
    }

    async fn cleanup(mut self, event_loop: JoinHandle<()>) {
        self.shutdown.cancel();
        let _ = event_loop.await;
        self.client.unregister_session(&self.session_id);
        self.disarmed = true;
    }

    fn disarm(&mut self) {
        self.disarmed = true;
    }
}

impl Drop for PendingSessionRegistration {
    fn drop(&mut self) {
        if !self.disarmed {
            self.shutdown.cancel();
            self.client.unregister_session(&self.session_id);
        }
    }
}

/// A session on a GitHub Copilot CLI server.
///
/// Created via [`Client::create_session`] or [`Client::resume_session`].
/// Owns an internal event loop that dispatches events to the per-callback
/// handlers installed on the session config.
///
/// Protocol methods (`send`, `get_events`, `abort`, etc.) automatically
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
    /// Handle to the spawned event-loop task. Sync `parking_lot::Mutex`
    /// because the lock is never held across an `.await` and the `Drop`
    /// impl needs to take the handle synchronously without `try_lock`
    /// fallibility.
    event_loop: ParkingLotMutex<Option<JoinHandle<()>>>,
    /// Cooperative shutdown signal for the event loop. The loop selects
    /// on [`shutdown.cancelled()`](CancellationToken::cancelled) alongside
    /// its inbound channels; [`Session::stop_event_loop`] and [`Drop`]
    /// both call [`cancel()`](CancellationToken::cancel) to ask the loop
    /// to exit between iterations rather than aborting the task (which
    /// can land at any await point and leave the session mid-protocol).
    /// See RFD-400 review finding #3.
    ///
    /// `CancellationToken` is the canonical signalling primitive in
    /// `tokio_util`; it is what `tonic` uses for the equivalent task-
    /// coordination case. Advanced consumers can obtain a child token
    /// via [`Session::cancellation_token`] to bind their own work to
    /// the session lifetime.
    shutdown: CancellationToken,
    /// Only populated while a `send_and_wait` call is in flight.
    ///
    /// Sync `parking_lot::Mutex` because the lock is never held across an
    /// `.await`, and synchronous access lets the `WaiterGuard` RAII helper
    /// in `send_and_wait` clear the slot from a `Drop` impl on caller-side
    /// cancellation. See RFD-400 review (cancel-safety hardening).
    idle_waiter: Arc<ParkingLotMutex<Option<IdleWaiter>>>,
    /// Capabilities negotiated with the CLI, updated on `capabilities.changed` events.
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
    /// Canvas instances currently known to be open for this session.
    open_canvases: Arc<parking_lot::RwLock<Vec<OpenCanvasInstance>>>,
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

    /// Open canvas instances reported by the most recent `session.resume`
    /// response or surfaced by inbound `canvas.opened` events.
    pub fn open_canvases(&self) -> Vec<OpenCanvasInstance> {
        self.open_canvases.read().clone()
    }

    /// Returns a [`CancellationToken`] that fires when this session shuts
    /// down (via [`Session::stop_event_loop`], [`Session::destroy`], or
    /// [`Drop`]).
    ///
    /// Use this to bind an external task's lifetime to the session — when
    /// the session shuts down, awaiting [`cancelled()`](CancellationToken::cancelled)
    /// resolves so cooperative consumers can stop cleanly.
    ///
    /// The returned handle is a *child* token: calling
    /// [`cancel()`](CancellationToken::cancel) on it cancels only the
    /// caller's child, not the session itself. To cancel the session, call
    /// [`Session::stop_event_loop`].
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(session: github_copilot_sdk::session::Session) {
    /// let token = session.cancellation_token();
    /// tokio::select! {
    ///     _ = token.cancelled() => println!("session shut down"),
    ///     _ = tokio::time::sleep(std::time::Duration::from_secs(60)) => {
    ///         println!("60s elapsed, session still alive");
    ///     }
    /// }
    /// # }
    /// ```
    pub fn cancellation_token(&self) -> CancellationToken {
        self.shutdown.child_token()
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
    /// results, or anything else that requires returning a value. Those
    /// remain the responsibility of the per-callback handlers passed via
    /// [`SessionConfig`]'s `with_*_handler`
    /// builder methods.
    ///
    /// The returned handle implements both an inherent
    /// [`recv`](crate::subscription::EventSubscription::recv) method and
    /// [`Stream`](tokio_stream::Stream), so callers can use a `while let`
    /// loop or any combinator from `tokio_stream::StreamExt` /
    /// `futures::StreamExt`.
    ///
    /// Each subscriber maintains its own queue. If a consumer cannot keep
    /// up, the oldest events are dropped and `recv` returns
    /// [`RecvErrorKind::Lagged`](crate::subscription::RecvErrorKind::Lagged)
    /// reporting the count of skipped events. Slow consumers do not block
    /// the session's event loop.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # async fn example(session: github_copilot_sdk::session::Session) {
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

    /// Typed RPC namespace for this session.
    ///
    /// Every protocol method lives here under its schema-aligned path —
    /// e.g. `session.rpc().workspaces().list_files()`. Wire method names
    /// and request/response types are generated from the protocol schema,
    /// so the typed namespace can't drift from the wire contract.
    ///
    /// The hand-authored helpers on [`Session`] delegate to this namespace
    /// and remain the recommended entry point for everyday use; reach for
    /// `rpc()` when you want a method without a hand-written wrapper.
    pub fn rpc(&self) -> crate::generated::rpc::SessionRpc<'_> {
        crate::generated::rpc::SessionRpc { session: self }
    }

    /// Stop the internal event loop. Called automatically on [`destroy`](Self::destroy).
    ///
    /// Cooperative: signals shutdown via the session's [`CancellationToken`]
    /// and awaits the loop's natural exit rather than aborting the task.
    /// Any in-flight handler (permission callback, tool call, elicitation
    /// response) completes before the loop exits, so the CLI never sees a
    /// half-handled request. See RFD-400 review finding #3.
    pub async fn stop_event_loop(&self) {
        self.shutdown.cancel();
        let handle = self.event_loop.lock().take();
        if let Some(handle) = handle {
            let _ = handle.await;
        }
        // Fail any pending send_and_wait so it returns immediately.
        if let Some(waiter) = self.idle_waiter.lock().take() {
            let _ = waiter.tx.send(Err(
                ErrorKind::Session(SessionErrorKind::EventLoopClosed).into()
            ));
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
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** The underlying `session.send` RPC is dispatched
    /// through the writer-actor (see [`Client::call`](crate::Client::call)),
    /// so dropping this future after the actor has committed to writing
    /// will not produce a partial frame on the wire. If the caller's
    /// future is dropped between "frame enqueued" and "response received",
    /// the message has already landed on the wire — the agent will process
    /// it and emit events normally; the caller just won't see the returned
    /// message ID.
    pub async fn send(&self, opts: impl Into<MessageOptions>) -> Result<String, Error> {
        if self.idle_waiter.lock().is_some() {
            return Err(ErrorKind::Session(SessionErrorKind::SendWhileWaiting).into());
        }
        self.send_inner(opts.into()).await
    }

    async fn send_inner(&self, opts: MessageOptions) -> Result<String, Error> {
        let mut params = serde_json::json!({
            "sessionId": self.id,
            "prompt": opts.prompt,
        });
        if let Some(m) = opts.mode {
            params["mode"] = serde_json::to_value(m)?;
        }
        if let Some(am) = opts.agent_mode {
            params["agentMode"] = serde_json::to_value(am)?;
        }
        if let Some(mut a) = opts.attachments {
            ensure_attachment_display_names(&mut a);
            params["attachments"] = serde_json::to_value(a)?;
        }
        if let Some(headers) = opts.request_headers
            && !headers.is_empty()
        {
            params["requestHeaders"] = serde_json::to_value(headers)?;
        }
        if let Some(display_prompt) = opts.display_prompt {
            params["displayPrompt"] = serde_json::to_value(display_prompt)?;
        }
        let trace_ctx = if opts.traceparent.is_some() || opts.tracestate.is_some() {
            TraceContext {
                traceparent: opts.traceparent,
                tracestate: opts.tracestate,
            }
        } else {
            self.client.resolve_trace_context().await
        };
        inject_trace_context(&mut params, &trace_ctx);
        let rpc_start = Instant::now();
        let result = self.client.call("session.send", Some(params)).await?;
        let message_id = result
            .get("messageId")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string())
            .unwrap_or_default();
        tracing::debug!(
            elapsed_ms = rpc_start.elapsed().as_millis(),
            session_id = %self.id,
            message_id = %message_id,
            "Session::send completed successfully"
        );
        Ok(message_id)
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
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** A `WaiterGuard` clears the in-flight slot on every
    /// exit path (success, internal failure, internal timeout, *and*
    /// external cancellation via `tokio::time::timeout` / `select!` /
    /// dropped JoinHandle). Subsequent `send` and `send_and_wait` calls on
    /// this session will succeed normally — the slot is never leaked.
    pub async fn send_and_wait(
        &self,
        opts: impl Into<MessageOptions>,
    ) -> Result<Option<SessionEvent>, Error> {
        let total_start = Instant::now();
        let opts = opts.into();
        let timeout_duration = opts.wait_timeout.unwrap_or(Duration::from_secs(60));
        let (tx, rx) = oneshot::channel();

        {
            let mut guard = self.idle_waiter.lock();
            if guard.is_some() {
                return Err(ErrorKind::Session(SessionErrorKind::SendWhileWaiting).into());
            }
            *guard = Some(IdleWaiter {
                tx,
                last_assistant_message: None,
                started_at: total_start,
                first_assistant_message_seen: false,
            });
        }

        // RAII: clears the idle_waiter slot on every exit path, including
        // external cancellation (caller's outer `select!` / `timeout` /
        // dropped future). Without this, an outer cancellation would leak
        // the slot and brick subsequent `send`/`send_and_wait` calls.
        let _waiter_guard = WaiterGuard {
            slot: self.idle_waiter.clone(),
        };

        let result = tokio::time::timeout(timeout_duration, async {
            self.send_inner(opts).await?;
            match rx.await {
                Ok(result) => result,
                Err(_) => Err(ErrorKind::Session(SessionErrorKind::EventLoopClosed).into()),
            }
        })
        .await;

        match result {
            Ok(inner) => {
                tracing::debug!(
                    elapsed_ms = total_start.elapsed().as_millis(),
                    session_id = %self.id,
                    completed_by = if inner.is_ok() { "idle" } else { "error" },
                    "Session::send_and_wait complete"
                );
                inner
            }
            Err(_) => {
                tracing::warn!(
                    elapsed_ms = total_start.elapsed().as_millis(),
                    session_id = %self.id,
                    completed_by = "timeout",
                    "Session::send_and_wait failed"
                );
                Err(ErrorKind::Session(SessionErrorKind::Timeout(timeout_duration)).into())
            }
        }
    }

    /// Retrieve the session's timeline events.
    pub async fn get_events(&self) -> Result<Vec<SessionEvent>, Error> {
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

    /// Deprecated alias for [`get_events`](Self::get_events).
    #[deprecated(since = "0.1.0", note = "Use `get_events()` instead")]
    pub async fn get_messages(&self) -> Result<Vec<SessionEvent>, Error> {
        self.get_events().await
    }

    /// Abort the current agent turn.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** Single `session.abort` RPC; the underlying
    /// [`Client::call`](crate::Client::call) is cancel-safe via the
    /// writer-actor.
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
    /// Pass `None` for `opts` if no extra configuration is needed.
    pub async fn set_model(&self, model: &str, opts: Option<SetModelOptions>) -> Result<(), Error> {
        let opts = opts.unwrap_or_default();
        let request = ModelSwitchToRequest {
            model_id: model.to_string(),
            reasoning_effort: opts.reasoning_effort,
            reasoning_summary: None,
            model_capabilities: opts.model_capabilities,
        };
        self.rpc().model().switch_to(request).await?;
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

    /// Deprecated alias for [`disconnect`](Self::disconnect). The
    /// underlying wire RPC happens to be named `session.destroy`, but it
    /// only severs the connection — on-disk session state is preserved.
    /// Prefer `disconnect` in new code.
    #[deprecated(since = "0.1.0", note = "Use `disconnect()` instead")]
    pub async fn destroy(&self) -> Result<(), Error> {
        self.disconnect().await
    }

    /// Write a log message to the session.
    ///
    /// Pass `None` for `opts` to use defaults (info level, persisted).
    pub async fn log(
        &self,
        message: &str,
        opts: Option<crate::types::LogOptions>,
    ) -> Result<(), Error> {
        let opts = opts.unwrap_or_default();
        let level = match opts.level {
            Some(level) => Some(serde_json::from_value(serde_json::to_value(level)?)?),
            None => None,
        };
        let request = LogRequest {
            message: message.to_string(),
            level,
            ephemeral: opts.ephemeral,
            r#type: None,
            tip: None,
            url: None,
        };
        self.rpc().log(request).await?;
        Ok(())
    }

    /// Returns the UI sub-API for elicitation, confirmation, selection, and
    /// free-form input.
    ///
    /// All UI methods route through `session.ui.*` RPCs and require host
    /// support — check `session.capabilities().ui.elicitation` before use.
    pub fn ui(&self) -> SessionUi<'_> {
        SessionUi { session: self }
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
            return Err(ErrorKind::Session(SessionErrorKind::ElicitationNotSupported).into());
        }
        Ok(())
    }
}

impl Drop for Session {
    fn drop(&mut self) {
        // Cooperative shutdown: cancel the event loop's token to signal
        // exit between iterations. The loop will see the cancellation on
        // its next select poll and break cleanly without interrupting an
        // in-flight handler. We do NOT abort the JoinHandle — that would
        // land at any await point in the loop body, potentially leaving
        // the CLI with an unanswered request id. RFD-400 review finding
        // #3.
        //
        // The handle itself is left in `event_loop` to be reaped by the
        // tokio runtime when it next polls; we intentionally don't await
        // it here because Drop is sync.
        self.shutdown.cancel();
        self.client.unregister_session(&self.id);
    }
}

/// UI sub-API for a [`Session`] — elicitation, confirmation, selection,
/// and free-form input.
///
/// Acquired via [`Session::ui`]. Methods route to `session.ui.*` RPCs and
/// require host elicitation support — check
/// `session.capabilities().ui.elicitation` before use.
pub struct SessionUi<'a> {
    session: &'a Session,
}

impl<'a> SessionUi<'a> {
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
        self.session.assert_elicitation()?;
        let result = self
            .session
            .client
            .call(
                "session.ui.elicitation",
                Some(serde_json::json!({
                    "sessionId": self.session.id,
                    "message": message,
                    "requestedSchema": schema,
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
        self.session.assert_elicitation()?;
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
        self.session.assert_elicitation()?;
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
    /// Use [`UiInputOptions`] to set validation constraints and field metadata.
    pub async fn input(
        &self,
        message: &str,
        options: Option<&UiInputOptions<'_>>,
    ) -> Result<Option<String>, Error> {
        self.session.assert_elicitation()?;
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
}

impl Client {
    /// Create a new session on the CLI.
    ///
    /// Sends `session.create`, registers the session on the router,
    /// and spawns an internal event loop that dispatches to the handler.
    ///
    /// All callbacks (per-event handlers, tool handlers, hooks, transform)
    /// are configured via [`SessionConfig`] using its `with_*_handler` /
    /// `with_tools` / `with_hooks` / `with_system_message_transform` builder
    /// methods.
    ///
    /// If [`hooks_handler`](SessionConfig::hooks_handler) is set, the
    /// wire-level `hooks` flag is automatically enabled.
    ///
    /// If [`system_message_transform`](SessionConfig::system_message_transform) is set, the SDK injects
    /// `action: "transform"` sections into the [`SystemMessageConfig`] wire
    /// format and handles `systemMessage.transform` RPC callbacks during
    /// the session.
    ///
    /// Each per-event handler is independently optional. If a handler is
    /// not installed, the SDK signals the runtime not to emit the matching
    /// broadcast (and silently skips dispatch if one arrives anyway).
    pub async fn create_session(&self, mut config: SessionConfig) -> Result<Session, Error> {
        let total_start = Instant::now();
        // For cloud sessions, let the CLI/server assign the session id and
        // register the session lazily once the response arrives. For non-cloud
        // sessions we generate the id client-side (when the caller didn't
        // supply one) so the session can be registered BEFORE the RPC — the
        // CLI may issue session-scoped requests (e.g. sessionFs.writeFile for
        // workspace metadata) during session.create processing, before it has
        // sent the response.
        let caller_session_id = config.session_id.clone();
        let use_server_generated_id = config.cloud.is_some() && caller_session_id.is_none();
        let local_session_id: Option<SessionId> = if use_server_generated_id {
            None
        } else {
            Some(
                caller_session_id
                    .clone()
                    .unwrap_or_else(|| SessionId::new(uuid::Uuid::new_v4().to_string())),
            )
        };
        if config.hooks_handler.is_some() && config.hooks.is_none() {
            config.hooks = Some(true);
        }
        if let Some(transforms) = config.system_message_transform.clone() {
            inject_transform_sections(&mut config, transforms.as_ref());
        }
        let mode = self.inner.mode;
        if mode == crate::ClientMode::Empty && config.available_tools.is_none() {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "ClientMode::Empty requires available_tools to be set on the session config. \
                 Use ToolSet to specify which tools the session may use (e.g. \
                 ToolSet::new().add_builtin_many(BUILTIN_TOOLS_ISOLATED)).",
            ));
        }
        crate::mode::validate_tool_filter_list(
            "available_tools",
            config.available_tools.as_deref(),
        )?;
        crate::mode::validate_tool_filter_list("excluded_tools", config.excluded_tools.as_deref())?;
        config.system_message =
            crate::mode::system_message_for_mode(mode, config.system_message.take());
        if mode == crate::ClientMode::Empty && config.enable_session_telemetry.is_none() {
            config.enable_session_telemetry = Some(false);
        }
        let opt_skip_custom_instructions = config.skip_custom_instructions;
        let opt_custom_agents_local_only = config.custom_agents_local_only;
        let opt_coauthor_enabled = config.coauthor_enabled;
        let opt_manage_schedule_enabled = config.manage_schedule_enabled;
        let (wire, mut runtime) = config.into_wire(local_session_id.clone())?;

        let permission_handler = crate::permission::resolve_handler(
            runtime.permission_handler.take(),
            runtime.permission_policy.take(),
        );
        let handlers = SessionHandlers {
            permission: permission_handler,
            elicitation: runtime.elicitation_handler.take(),
            user_input: runtime.user_input_handler.take(),
            exit_plan_mode: runtime.exit_plan_mode_handler.take(),
            auto_mode_switch: runtime.auto_mode_switch_handler.take(),
            tools: Arc::new(std::mem::take(&mut runtime.tool_handlers)),
        };
        let hooks = runtime.hooks_handler.take();
        let transforms = runtime.system_message_transform.take();
        let tools_count = wire.tools.as_ref().map_or(0, Vec::len);
        let commands_count = runtime.commands.as_ref().map_or(0, Vec::len);
        let has_hooks = hooks.is_some();
        let command_handlers = build_command_handler_map(runtime.commands.as_deref());
        let canvas_handler = runtime.canvas_handler.take();
        let session_fs_provider = runtime.session_fs_provider.take();
        if self.inner.session_fs_configured && session_fs_provider.is_none() {
            return Err(ErrorKind::Session(SessionErrorKind::SessionFsProviderRequired).into());
        }
        if self.inner.session_fs_sqlite_declared
            && let Some(ref provider) = session_fs_provider
            && provider.sqlite().is_none()
        {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "SessionFs capabilities declare SQLite support but the provider \
                 does not implement SessionFsSqliteProvider",
            ));
        }

        let mut params = serde_json::to_value(&wire)?;
        let trace_ctx = self.resolve_trace_context().await;
        inject_trace_context(&mut params, &trace_ctx);

        let setup_start = Instant::now();
        let capabilities = Arc::new(parking_lot::RwLock::new(SessionCapabilities::default()));
        let idle_waiter = Arc::new(ParkingLotMutex::new(None));
        let open_canvases = Arc::new(parking_lot::RwLock::new(Vec::new()));
        let shutdown = CancellationToken::new();
        let (event_tx, _) = tokio::sync::broadcast::channel(512);

        // For cloud sessions (use_server_generated_id), defer session
        // registration to the inline callback so the read task registers
        // the session synchronously the instant the response arrives.
        // For non-cloud sessions, register up-front so the CLI can issue
        // session-scoped requests during session.create processing.
        let inline_stash: Arc<
            ParkingLotMutex<Option<(SessionId, crate::router::SessionChannels)>>,
        > = Arc::new(ParkingLotMutex::new(None));

        let inline_callback: Option<crate::jsonrpc::InlineResponseCallback> = if let Some(ref sid) =
            local_session_id
        {
            let channels = self.register_session(sid);
            *inline_stash.lock() = Some((sid.clone(), channels));
            None
        } else {
            let client = self.clone();
            let stash = inline_stash.clone();
            let expected = caller_session_id.clone();
            Some(Box::new(move |response| {
                let result = response.result.as_ref().ok_or_else(|| {
                    Error::with_message(ErrorKind::Json, "session.create response had no result")
                })?;
                let parsed: CreateSessionResult =
                    serde_json::from_value(result.clone()).map_err(Error::from)?;
                if let Some(requested) = expected.as_ref()
                    && parsed.session_id != *requested
                {
                    return Err(ErrorKind::Session(SessionErrorKind::SessionIdMismatch {
                        requested: requested.clone(),
                        returned: parsed.session_id,
                    })
                    .into());
                }
                let channels = client.register_session(&parsed.session_id);
                *stash.lock() = Some((parsed.session_id, channels));
                Ok(())
            }))
        };

        let rpc_start = Instant::now();
        let result = match self
            .call_with_inline_callback("session.create", Some(params), inline_callback)
            .await
        {
            Ok(result) => result,
            Err(error) => {
                if let Some((id, _channels)) = inline_stash.lock().take() {
                    self.unregister_session(&id);
                }
                return Err(error);
            }
        };
        tracing::debug!(
            elapsed_ms = rpc_start.elapsed().as_millis(),
            "Client::create_session session creation request completed successfully"
        );
        let create_result: CreateSessionResult = match serde_json::from_value(result) {
            Ok(result) => result,
            Err(error) => {
                if let Some((id, _channels)) = inline_stash.lock().take() {
                    self.unregister_session(&id);
                }
                return Err(error.into());
            }
        };

        if let Some(ref requested) = local_session_id
            && create_result.session_id != *requested
        {
            if let Some((id, _channels)) = inline_stash.lock().take() {
                self.unregister_session(&id);
            }
            return Err(ErrorKind::Session(SessionErrorKind::SessionIdMismatch {
                requested: requested.clone(),
                returned: create_result.session_id.clone(),
            })
            .into());
        }

        let (session_id, channels) = inline_stash
            .lock()
            .take()
            .expect("session registration must have populated stash on success");
        let event_loop = spawn_event_loop(
            session_id.clone(),
            self.clone(),
            handlers,
            hooks,
            transforms,
            command_handlers,
            canvas_handler,
            session_fs_provider,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
            open_canvases.clone(),
            event_tx.clone(),
            shutdown.clone(),
        );
        tracing::debug!(
            elapsed_ms = setup_start.elapsed().as_millis(),
            session_id = %session_id,
            tools_count,
            commands_count,
            has_hooks,
            "Client::create_session local setup complete"
        );
        *capabilities.write() = create_result.capabilities.unwrap_or_default();

        tracing::debug!(
            elapsed_ms = total_start.elapsed().as_millis(),
            session_id = %session_id,
            "Client::create_session complete"
        );
        let session = Session {
            id: session_id,
            cwd: self.cwd().clone(),
            workspace_path: create_result.workspace_path,
            remote_url: create_result.remote_url,
            client: self.clone(),
            event_loop: ParkingLotMutex::new(Some(event_loop)),
            shutdown,
            idle_waiter,
            capabilities,
            open_canvases,
            event_tx,
        };
        apply_mode_post_create_patch(
            &session,
            mode,
            opt_skip_custom_instructions,
            opt_custom_agents_local_only,
            opt_coauthor_enabled,
            opt_manage_schedule_enabled,
        )
        .await?;
        Ok(session)
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
        let total_start = Instant::now();
        let session_id = config.session_id.clone();
        if config.hooks_handler.is_some() && config.hooks.is_none() {
            config.hooks = Some(true);
        }
        if let Some(transforms) = config.system_message_transform.clone() {
            inject_transform_sections_resume(&mut config, transforms.as_ref());
        }
        let mode = self.inner.mode;
        if mode == crate::ClientMode::Empty && config.available_tools.is_none() {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "ClientMode::Empty requires available_tools to be set on the session config. \
                 Use ToolSet to specify which tools the session may use (e.g. \
                 ToolSet::new().add_builtin_many(BUILTIN_TOOLS_ISOLATED)).",
            ));
        }
        crate::mode::validate_tool_filter_list(
            "available_tools",
            config.available_tools.as_deref(),
        )?;
        crate::mode::validate_tool_filter_list("excluded_tools", config.excluded_tools.as_deref())?;
        config.system_message =
            crate::mode::system_message_for_mode(mode, config.system_message.take());
        if mode == crate::ClientMode::Empty && config.enable_session_telemetry.is_none() {
            config.enable_session_telemetry = Some(false);
        }
        let opt_skip_custom_instructions = config.skip_custom_instructions;
        let opt_custom_agents_local_only = config.custom_agents_local_only;
        let opt_coauthor_enabled = config.coauthor_enabled;
        let opt_manage_schedule_enabled = config.manage_schedule_enabled;
        let (wire, mut runtime) = config.into_wire()?;

        let permission_handler = crate::permission::resolve_handler(
            runtime.permission_handler.take(),
            runtime.permission_policy.take(),
        );
        let handlers = SessionHandlers {
            permission: permission_handler,
            elicitation: runtime.elicitation_handler.take(),
            user_input: runtime.user_input_handler.take(),
            exit_plan_mode: runtime.exit_plan_mode_handler.take(),
            auto_mode_switch: runtime.auto_mode_switch_handler.take(),
            tools: Arc::new(std::mem::take(&mut runtime.tool_handlers)),
        };
        let hooks = runtime.hooks_handler.take();
        let transforms = runtime.system_message_transform.take();
        let tools_count = wire.tools.as_ref().map_or(0, Vec::len);
        let commands_count = runtime.commands.as_ref().map_or(0, Vec::len);
        let has_hooks = hooks.is_some();
        let command_handlers = build_command_handler_map(runtime.commands.as_deref());
        let canvas_handler = runtime.canvas_handler.take();
        let session_fs_provider = runtime.session_fs_provider.take();
        if self.inner.session_fs_configured && session_fs_provider.is_none() {
            return Err(ErrorKind::Session(SessionErrorKind::SessionFsProviderRequired).into());
        }
        if self.inner.session_fs_sqlite_declared
            && let Some(ref provider) = session_fs_provider
            && provider.sqlite().is_none()
        {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "SessionFs capabilities declare SQLite support but the provider \
                 does not implement SessionFsSqliteProvider",
            ));
        }

        let mut params = serde_json::to_value(&wire)?;
        let trace_ctx = self.resolve_trace_context().await;
        inject_trace_context(&mut params, &trace_ctx);

        let capabilities = Arc::new(parking_lot::RwLock::new(SessionCapabilities::default()));
        let setup_start = Instant::now();
        let channels = self.register_session(&session_id);
        let idle_waiter = Arc::new(ParkingLotMutex::new(None));
        let open_canvases = Arc::new(parking_lot::RwLock::new(Vec::new()));
        let shutdown = CancellationToken::new();
        let (event_tx, _) = tokio::sync::broadcast::channel(512);
        let event_loop = spawn_event_loop(
            session_id.clone(),
            self.clone(),
            handlers,
            hooks,
            transforms,
            command_handlers,
            canvas_handler,
            session_fs_provider,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
            open_canvases.clone(),
            event_tx.clone(),
            shutdown.clone(),
        );
        let mut registration =
            PendingSessionRegistration::new(self.clone(), session_id.clone(), shutdown.clone());
        tracing::debug!(
            elapsed_ms = setup_start.elapsed().as_millis(),
            session_id = %session_id,
            tools_count,
            commands_count,
            has_hooks,
            "Client::resume_session local setup complete"
        );

        let rpc_start = Instant::now();
        let result = match self.call("session.resume", Some(params)).await {
            Ok(result) => result,
            Err(error) => {
                registration.cleanup(event_loop).await;
                return Err(error);
            }
        };
        tracing::debug!(
            elapsed_ms = rpc_start.elapsed().as_millis(),
            session_id = %session_id,
            "Client::resume_session session resume request completed successfully"
        );

        let resume_result: ResumeSessionResult = match serde_json::from_value(result) {
            Ok(result) => result,
            Err(error) => {
                registration.cleanup(event_loop).await;
                return Err(error.into());
            }
        };
        let cli_session_id = resume_result
            .session_id
            .clone()
            .unwrap_or_else(|| session_id.clone());
        if cli_session_id != session_id {
            registration.cleanup(event_loop).await;
            return Err(ErrorKind::Session(SessionErrorKind::SessionIdMismatch {
                requested: session_id,
                returned: cli_session_id,
            })
            .into());
        }

        // Reload skills after resume (best-effort).
        let skills_reload_start = Instant::now();
        if let Err(e) = self
            .call(
                "session.skills.reload",
                Some(serde_json::json!({ "sessionId": session_id })),
            )
            .await
        {
            warn!(
                elapsed_ms = skills_reload_start.elapsed().as_millis(),
                session_id = %session_id,
                error = %e,
                "Client::resume_session skills reload request failed"
            );
        } else {
            tracing::debug!(
                elapsed_ms = skills_reload_start.elapsed().as_millis(),
                session_id = %session_id,
                "Client::resume_session skills reload request completed successfully"
            );
        }

        *capabilities.write() = resume_result.capabilities.unwrap_or_default();
        *open_canvases.write() = resume_result.open_canvases.unwrap_or_default();

        tracing::debug!(
            elapsed_ms = total_start.elapsed().as_millis(),
            session_id = %session_id,
            "Client::resume_session complete"
        );
        registration.disarm();
        let session = Session {
            id: session_id,
            cwd: self.cwd().clone(),
            workspace_path: resume_result.workspace_path,
            remote_url: resume_result.remote_url,
            client: self.clone(),
            event_loop: ParkingLotMutex::new(Some(event_loop)),
            shutdown,
            idle_waiter,
            capabilities,
            open_canvases,
            event_tx,
        };
        apply_mode_post_create_patch(
            &session,
            mode,
            opt_skip_custom_instructions,
            opt_custom_agents_local_only,
            opt_coauthor_enabled,
            opt_manage_schedule_enabled,
        )
        .await?;
        Ok(session)
    }
}

type CommandHandlerMap = HashMap<String, Arc<dyn CommandHandler>>;

async fn apply_mode_post_create_patch(
    session: &Session,
    mode: crate::ClientMode,
    opt_skip_custom_instructions: Option<bool>,
    opt_custom_agents_local_only: Option<bool>,
    opt_coauthor_enabled: Option<bool>,
    opt_manage_schedule_enabled: Option<bool>,
) -> Result<(), Error> {
    use crate::generated::api_types::SessionUpdateOptionsParams;
    let mut patch = SessionUpdateOptionsParams::default();
    let should_send = if mode == crate::ClientMode::Empty {
        patch.skip_custom_instructions = Some(opt_skip_custom_instructions.unwrap_or(true));
        patch.custom_agents_local_only = Some(opt_custom_agents_local_only.unwrap_or(true));
        patch.coauthor_enabled = Some(opt_coauthor_enabled.unwrap_or(false));
        patch.manage_schedule_enabled = Some(opt_manage_schedule_enabled.unwrap_or(false));
        patch.installed_plugins = Some(Vec::new());
        true
    } else {
        let mut any = false;
        if let Some(v) = opt_skip_custom_instructions {
            patch.skip_custom_instructions = Some(v);
            any = true;
        }
        if let Some(v) = opt_custom_agents_local_only {
            patch.custom_agents_local_only = Some(v);
            any = true;
        }
        if let Some(v) = opt_coauthor_enabled {
            patch.coauthor_enabled = Some(v);
            any = true;
        }
        if let Some(v) = opt_manage_schedule_enabled {
            patch.manage_schedule_enabled = Some(v);
            any = true;
        }
        any
    };
    if !should_send {
        return Ok(());
    }
    if let Err(error) = session.rpc().options().update(patch).await {
        let _ = session.disconnect().await;
        return Err(error);
    }
    Ok(())
}

fn build_command_handler_map(commands: Option<&[CommandDefinition]>) -> Arc<CommandHandlerMap> {
    let map = match commands {
        Some(commands) => commands
            .iter()
            .filter(|cmd| !cmd.name.is_empty())
            .map(|cmd| (cmd.name.clone(), cmd.handler.clone()))
            .collect(),
        None => HashMap::new(),
    };
    Arc::new(map)
}

fn upsert_open_canvas_snapshot(
    snapshots: &mut Vec<OpenCanvasInstance>,
    snapshot: OpenCanvasInstance,
) {
    if let Some(existing) = snapshots
        .iter_mut()
        .find(|open| open.instance_id == snapshot.instance_id)
    {
        *existing = snapshot;
    } else {
        snapshots.push(snapshot);
    }
}

#[allow(clippy::too_many_arguments)]
fn spawn_event_loop(
    session_id: SessionId,
    client: Client,
    handlers: SessionHandlers,
    hooks: Option<Arc<dyn SessionHooks>>,
    transforms: Option<Arc<dyn SystemMessageTransform>>,
    command_handlers: Arc<CommandHandlerMap>,
    canvas_handler: Option<Arc<dyn CanvasHandler>>,
    session_fs_provider: Option<Arc<dyn SessionFsProvider>>,
    channels: crate::router::SessionChannels,
    idle_waiter: Arc<ParkingLotMutex<Option<IdleWaiter>>>,
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
    open_canvases: Arc<parking_lot::RwLock<Vec<OpenCanvasInstance>>>,
    event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
    shutdown: CancellationToken,
) -> JoinHandle<()> {
    let crate::router::SessionChannels {
        mut notifications,
        mut requests,
    } = channels;

    let span = tracing::error_span!("session_event_loop", session_id = %session_id);
    tokio::spawn(
        async move {
            loop {
                // `mpsc::UnboundedReceiver::recv` and
                // `CancellationToken::cancelled` are both cancel-safe per
                // RFD 400. The selected branch's `await`'d handler is
                // *not* mid-cancelled by the select — once a branch fires
                // it runs to completion within the loop's iteration.
                // Spawned child tasks inside `handle_notification`
                // (permission/tool/elicitation callbacks) intentionally
                // outlive the parent loop and own their own cleanup;
                // this is RFD 400's "spawn background tasks to perform
                // cancel-unsafe operations" pattern and is correct as-is.
                tokio::select! {
                    _ = shutdown.cancelled() => break,
                    Some(notification) = notifications.recv() => {
                        handle_notification(
                            &session_id, &client, &handlers, &command_handlers, notification, &idle_waiter, &capabilities, &open_canvases, &event_tx,
                        ).await;
                    }
                    Some(request) = requests.recv() => {
                        let ctx = RequestDispatchContext {
                            client: &client,
                            handlers: &handlers,
                            hooks: hooks.as_deref(),
                            transforms: transforms.as_deref(),
                            canvas_handler: canvas_handler.as_ref(),
                            session_fs_provider: session_fs_provider.as_ref(),
                        };
                        handle_request(&session_id, ctx, request).await;
                    }
                    else => break,
                }
            }
            // Channels closed or shutdown signaled — fail any pending
            // send_and_wait so the caller observes a clean error.
            if let Some(waiter) = idle_waiter.lock().take() {
                let _ = waiter
                    .tx
                    .send(Err(ErrorKind::Session(SessionErrorKind::EventLoopClosed).into()));
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

/// Map a [`PermissionResult`] to the `result` payload sent back to the
/// server via `session.permissions.handlePendingPermissionRequest`.
///
/// Returns `None` when the SDK must not send a response.
fn notification_permission_payload(result: &PermissionResult) -> Option<Value> {
    match result {
        PermissionResult::NoResult => None,
        PermissionResult::Decision(decision) => Some(
            serde_json::to_value(decision).expect("serializing permission decision should succeed"),
        ),
    }
}

fn tool_failure_result(message: impl Into<String>) -> ToolResult {
    let message = message.into();
    ToolResult::Expanded(ToolResultExpanded {
        text_result_for_llm: message.clone(),
        result_type: "failure".to_string(),
        binary_results_for_llm: None,
        session_log: None,
        error: Some(message),
        tool_telemetry: None,
    })
}

/// Process a notification from the CLI's broadcast channel.
#[allow(clippy::too_many_arguments)]
async fn handle_notification(
    session_id: &SessionId,
    client: &Client,
    handlers: &SessionHandlers,
    command_handlers: &Arc<CommandHandlerMap>,
    notification: SessionEventNotification,
    idle_waiter: &Arc<ParkingLotMutex<Option<IdleWaiter>>>,
    capabilities: &Arc<parking_lot::RwLock<SessionCapabilities>>,
    open_canvases: &Arc<parking_lot::RwLock<Vec<OpenCanvasInstance>>>,
    event_tx: &tokio::sync::broadcast::Sender<SessionEvent>,
) {
    let dispatch_start = Instant::now();
    let event = notification.event.clone();
    let event_type = event.parsed_type();
    if event_type == SessionEventType::PermissionRequested {
        tracing::debug!(
            session_id = %session_id,
            event_type = %event.event_type,
            "Session::handle_notification permission request received"
        );
    }

    // Signal send_and_wait if active. The lock is only contended when
    // a send_and_wait call is in flight (idle_waiter is Some).
    match event_type {
        SessionEventType::AssistantMessage
        | SessionEventType::SessionIdle
        | SessionEventType::SessionError => {
            let mut guard = idle_waiter.lock();
            if let Some(waiter) = guard.as_mut() {
                match event_type {
                    SessionEventType::AssistantMessage => {
                        if !waiter.first_assistant_message_seen {
                            waiter.first_assistant_message_seen = true;
                            tracing::debug!(
                                elapsed_ms = waiter.started_at.elapsed().as_millis(),
                                session_id = %session_id,
                                "Session::send_and_wait first assistant message"
                            );
                        }
                        waiter.last_assistant_message = Some(event.clone());
                    }
                    SessionEventType::SessionIdle | SessionEventType::SessionError => {
                        if let Some(waiter) = guard.take() {
                            if event_type == SessionEventType::SessionIdle {
                                tracing::debug!(
                                    elapsed_ms = waiter.started_at.elapsed().as_millis(),
                                    session_id = %session_id,
                                    "Session::send_and_wait idle received"
                                );
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
                                let _ = waiter.tx.send(Err(Error::with_message(
                                    ErrorKind::Session(SessionErrorKind::AgentError),
                                    error_msg,
                                )));
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

    // Update capabilities when the CLI reports changes. The CLI sends
    // the full updated capabilities object — replace wholesale so removals
    // and new subfields are handled correctly.
    if event_type == SessionEventType::CapabilitiesChanged {
        match serde_json::from_value::<SessionCapabilities>(notification.event.data.clone()) {
            Ok(changed) => *capabilities.write() = changed,
            Err(e) => warn!(error = %e, "failed to deserialize capabilities.changed payload"),
        }
    }
    if event_type == SessionEventType::SessionCanvasOpened {
        match serde_json::from_value::<OpenCanvasInstance>(notification.event.data.clone()) {
            Ok(open_canvas) => {
                upsert_open_canvas_snapshot(&mut open_canvases.write(), open_canvas);
            }
            Err(e) => warn!(error = %e, "failed to deserialize session.canvas.opened payload"),
        }
    }

    tracing::debug!(
        elapsed_ms = dispatch_start.elapsed().as_millis(),
        session_id = %session_id,
        event_type = %notification.event.event_type,
        "Session::handle_notification dispatch"
    );

    // Notification-based permission/tool/elicitation requests require a
    // separate RPC callback. Spawn concurrently since the CLI doesn't block.
    match event_type {
        SessionEventType::PermissionRequested => {
            let Some(request_id) = extract_request_id(&notification.event.data) else {
                return;
            };
            // Honor the runtime's `resolvedByHook` signal — when the
            // server has already resolved the permission via a hook,
            // clients must not send a second response.
            if notification
                .event
                .data
                .get("resolvedByHook")
                .and_then(|v| v.as_bool())
                .unwrap_or(false)
            {
                return;
            }
            // Multi-client safety: if this client has no permission
            // handler installed, don't respond — another client on the
            // same CLI may handle it.
            let Some(permission_handler) = handlers.permission.clone() else {
                return;
            };
            let client = client.clone();
            let sid = session_id.clone();
            let data: PermissionRequestData =
                serde_json::from_value(notification.event.data.clone()).unwrap_or_else(|_| {
                    PermissionRequestData {
                        kind: None,
                        tool_call_id: None,
                        extra: notification.event.data.clone(),
                    }
                });
            let span = tracing::error_span!(
                "permission_request_handler",
                session_id = %sid,
                request_id = %request_id
            );
            tokio::spawn(
                async move {
                    let handler_start = Instant::now();
                    let result = permission_handler
                        .handle(sid.clone(), request_id.clone(), data)
                        .await;
                    tracing::debug!(
                        elapsed_ms = handler_start.elapsed().as_millis(),
                        session_id = %sid,
                        request_id = %request_id,
                        "PermissionHandler::handle dispatch"
                    );
                    let Some(result_value) = notification_permission_payload(&result) else {
                        // Handler returned Deferred / NoResult — it will
                        // call handlePendingPermissionRequest itself (or
                        // leave the request unanswered).
                        return;
                    };
                    let rpc_start = Instant::now();
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
                    tracing::debug!(
                        elapsed_ms = rpc_start.elapsed().as_millis(),
                        session_id = %sid,
                        request_id = %request_id,
                        "Session::handle_notification response sent successfully"
                    );
                }
                .instrument(span),
            );
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
                        let span = tracing::error_span!(
                            "external_tool_deserialize_error",
                            session_id = %sid,
                            request_id = %request_id
                        );
                        tokio::spawn(
                            async move {
                                let rpc_start = Instant::now();
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
                                tracing::debug!(
                                    elapsed_ms = rpc_start.elapsed().as_millis(),
                                    session_id = %sid,
                                    request_id = %request_id,
                                    "Session::handle_notification response sent successfully"
                                );
                            }
                            .instrument(span),
                        );
                        return;
                    }
                };
            // Multi-client safety: look up a handler for the requested
            // tool name. If this client has no handler installed for that
            // tool, don't respond — another connected client may have one.
            let tool_handler = if data.tool_name.is_empty() {
                None
            } else {
                handlers.tools.get(&data.tool_name).cloned()
            };
            let Some(tool_handler) = tool_handler else {
                return;
            };
            let client = client.clone();
            let sid = session_id.clone();
            let span = tracing::error_span!(
                "external_tool_handler",
                session_id = %sid,
                request_id = %request_id
            );
            tokio::spawn(
                async move {
                    // `tool_name.is_empty()` would have produced a `None`
                    // lookup in `handlers.tools` and short-circuited at the
                    // outer guard above, so only the tool_call_id check is
                    // reachable here.
                    if data.tool_call_id.is_empty() {
                        let error_msg = "Missing toolCallId";
                        let rpc_start = Instant::now();
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
                        tracing::debug!(
                            elapsed_ms = rpc_start.elapsed().as_millis(),
                            session_id = %sid,
                            request_id = %request_id,
                            "Session::handle_notification response sent successfully"
                        );
                        return;
                    }
                    let tool_call_id = data.tool_call_id.clone();
                    let tool_name = data.tool_name.clone();
                    let invocation = ToolInvocation {
                        session_id: sid.clone(),
                        tool_call_id: data.tool_call_id,
                        tool_name: data.tool_name,
                        arguments: data
                            .arguments
                            .unwrap_or(Value::Object(serde_json::Map::new())),
                        traceparent: data.traceparent,
                        tracestate: data.tracestate,
                    };
                    let handler_start = Instant::now();
                    let tool_result = match tool_handler.call(invocation).await {
                        Ok(r) => r,
                        Err(e) => tool_failure_result(e.to_string()),
                    };
                    tracing::debug!(
                        elapsed_ms = handler_start.elapsed().as_millis(),
                        session_id = %sid,
                        request_id = %request_id,
                        tool_call_id = %tool_call_id,
                        tool_name = %tool_name,
                        "ToolHandler::call dispatch"
                    );
                    let result_value = serde_json::to_value(tool_result).unwrap_or(Value::Null);
                    let rpc_start = Instant::now();
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
                    tracing::debug!(
                        elapsed_ms = rpc_start.elapsed().as_millis(),
                        session_id = %sid,
                        request_id = %request_id,
                        tool_call_id = %tool_call_id,
                        tool_name = %tool_name,
                        "Session::handle_notification response sent successfully"
                    );
                }
                .instrument(span),
            );
        }
        SessionEventType::UserInputRequested => {
            // Notification-only signal for observers (UI, telemetry).
            // The CLI follows up with a `userInput.request` JSON-RPC call
            // that drives the `UserInputHandler` dispatch — handling
            // the notification here too would double-fire the handler
            // and produce duplicate prompts on the consumer side. See
            // github/github-app#4249.
        }
        SessionEventType::ElicitationRequested => {
            let Some(request_id) = extract_request_id(&notification.event.data) else {
                return;
            };
            // Multi-client safety: if this client has no elicitation
            // handler installed, don't respond — another client on the
            // same CLI may handle it.
            let Some(elicitation_handler) = handlers.elicitation.clone() else {
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
            let sid = session_id.clone();
            let span = tracing::error_span!(
                "elicitation_request_handler",
                session_id = %sid,
                request_id = %request_id
            );
            tokio::spawn(
                async move {
                    let cancel = ElicitationResult {
                        action: "cancel".to_string(),
                        content: None,
                    };
                    // Dispatch to a nested task so panics are caught as JoinErrors.
                    let handler_task = tokio::spawn({
                        let sid = sid.clone();
                        let request_id = request_id.clone();
                        let span = tracing::error_span!(
                            "elicitation_callback",
                            session_id = %sid,
                            request_id = %request_id
                        );
                        async move {
                            let handler_start = Instant::now();
                            let response = elicitation_handler
                                .handle(sid.clone(), request_id.clone(), request)
                                .await;
                            tracing::debug!(
                                elapsed_ms = handler_start.elapsed().as_millis(),
                                session_id = %sid,
                                request_id = %request_id,
                                "ElicitationHandler::handle dispatch"
                            );
                            response
                        }
                        .instrument(span)
                    });
                    let result = match handler_task.await {
                        Ok(r) => r,
                        Err(_) => cancel.clone(),
                    };
                    let rpc_start = Instant::now();
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
                    } else {
                        tracing::debug!(
                            elapsed_ms = rpc_start.elapsed().as_millis(),
                            session_id = %sid,
                            request_id = %request_id,
                            "Session::handle_notification response sent successfully"
                        );
                    }
                }
                .instrument(span),
            );
        }
        SessionEventType::CommandExecute => {
            let data: CommandExecuteData =
                match serde_json::from_value(notification.event.data.clone()) {
                    Ok(d) => d,
                    Err(e) => {
                        warn!(error = %e, "failed to deserialize command.execute");
                        return;
                    }
                };
            let client = client.clone();
            let command_handlers = command_handlers.clone();
            let sid = session_id.clone();
            let span = tracing::error_span!("command_handler", session_id = %sid);
            tokio::spawn(
                async move {
                    let request_id = data.request_id;
                    let ack_error = match command_handlers.get(&data.command_name).cloned() {
                        None => Some(format!("Unknown command: {}", data.command_name)),
                        Some(handler) => {
                            let command_name = data.command_name.clone();
                            let ctx = CommandContext {
                                session_id: sid.clone(),
                                command: data.command,
                                command_name: data.command_name,
                                args: data.args,
                            };
                            let handler_start = Instant::now();
                            let result = handler.on_command(ctx).await;
                            tracing::debug!(
                                elapsed_ms = handler_start.elapsed().as_millis(),
                                session_id = %sid,
                                request_id = %request_id,
                                command_name = %command_name,
                                "CommandHandler::call dispatch"
                            );
                            match result {
                                Ok(()) => None,
                                Err(e) => Some(e.to_string()),
                            }
                        }
                    };
                    let mut params = serde_json::json!({
                        "sessionId": sid,
                        "requestId": request_id,
                    });
                    if let Some(error_msg) = ack_error {
                        params["error"] = serde_json::Value::String(error_msg);
                    }
                    let rpc_start = Instant::now();
                    let _ = client
                        .call("session.commands.handlePendingCommand", Some(params))
                        .await;
                    tracing::debug!(
                        elapsed_ms = rpc_start.elapsed().as_millis(),
                        session_id = %sid,
                        request_id = %request_id,
                        "Session::handle_notification response sent successfully"
                    );
                }
                .instrument(span),
            );
        }
        _ => {}
    }
}

struct RequestDispatchContext<'a> {
    client: &'a Client,
    handlers: &'a SessionHandlers,
    hooks: Option<&'a dyn SessionHooks>,
    transforms: Option<&'a dyn SystemMessageTransform>,
    canvas_handler: Option<&'a Arc<dyn CanvasHandler>>,
    session_fs_provider: Option<&'a Arc<dyn SessionFsProvider>>,
}

/// Process a JSON-RPC request from the CLI.
async fn handle_request(
    session_id: &SessionId,
    ctx: RequestDispatchContext<'_>,
    request: crate::JsonRpcRequest,
) {
    let sid = session_id.clone();
    let client = ctx.client;
    let handlers = ctx.handlers;
    let hooks = ctx.hooks;
    let transforms = ctx.transforms;
    let canvas_handler = ctx.canvas_handler;
    let session_fs_provider = ctx.session_fs_provider;

    if request.method.starts_with("sessionFs.") {
        crate::session_fs_dispatch::dispatch(client, session_fs_provider, request).await;
        return;
    }

    if request.method.starts_with("canvas.") {
        crate::canvas_dispatch::dispatch(client, canvas_handler, request).await;
        return;
    }

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

            let handler_start = Instant::now();
            let response = if let Some(user_input_handler) = handlers.user_input.as_ref() {
                user_input_handler
                    .handle(sid.clone(), question, choices, allow_freeform)
                    .await
            } else {
                None
            };
            tracing::debug!(
                elapsed_ms = handler_start.elapsed().as_millis(),
                session_id = %sid,
                "UserInputHandler::handle dispatch"
            );

            let rpc_result = match response {
                Some(UserInputResponse {
                    answer,
                    was_freeform,
                }) => serde_json::json!({
                    "answer": answer,
                    "wasFreeform": was_freeform,
                }),
                None => serde_json::json!({ "noResponse": true }),
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

            let rpc_result = if let Some(exit_plan_handler) = handlers.exit_plan_mode.as_ref() {
                let result = exit_plan_handler.handle(sid, data).await;
                serde_json::to_value(result).expect("ExitPlanModeResult serialization cannot fail")
            } else {
                serde_json::json!({ "approved": true })
            };
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(rpc_result),
                error: None,
            };
            let _ = client.send_response(&rpc_response).await;
        }

        "autoModeSwitch.request" => {
            let error_code = request
                .params
                .as_ref()
                .and_then(|p| p.get("errorCode"))
                .and_then(|v| v.as_str())
                .map(|s| s.to_string());
            let retry_after_seconds = request
                .params
                .as_ref()
                .and_then(|p| p.get("retryAfterSeconds"))
                .and_then(|v| v.as_f64());

            let answer = if let Some(auto_mode_handler) = handlers.auto_mode_switch.as_ref() {
                auto_mode_handler
                    .handle(sid, error_code, retry_after_seconds)
                    .await
            } else {
                AutoModeSwitchResponse::No
            };
            let rpc_response = JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request.id,
                result: Some(serde_json::json!({ "response": answer })),
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
                let transform_start = Instant::now();
                let response =
                    crate::transforms::dispatch_transform(transforms, &sid, sections).await;
                tracing::debug!(
                    elapsed_ms = transform_start.elapsed().as_millis(),
                    session_id = %sid,
                    "SystemMessageTransform::transform_section dispatch"
                );
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

    use super::notification_permission_payload;
    use crate::handler::PermissionResult;

    #[test]
    fn notification_payload_suppresses_no_result() {
        assert!(notification_permission_payload(&PermissionResult::NoResult).is_none());
    }

    #[test]
    fn notification_payload_serializes_decisions() {
        assert_eq!(
            notification_permission_payload(&PermissionResult::approve_once()),
            Some(json!({ "kind": "approve-once" }))
        );
        assert_eq!(
            notification_permission_payload(&PermissionResult::reject(None)),
            Some(json!({ "kind": "reject" }))
        );
        assert_eq!(
            notification_permission_payload(&PermissionResult::reject(Some("bad".to_string()))),
            Some(json!({ "kind": "reject", "feedback": "bad" }))
        );
        assert_eq!(
            notification_permission_payload(&PermissionResult::user_not_available()),
            Some(json!({ "kind": "user-not-available" }))
        );
    }
}
