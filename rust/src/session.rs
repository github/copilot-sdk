use std::collections::HashMap;
use std::panic::AssertUnwindSafe;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::{Duration, Instant};

use futures_util::FutureExt;
use parking_lot::Mutex as ParkingLotMutex;
use serde_json::Value;
use tokio::sync::oneshot;
use tokio::task::JoinHandle;
use tokio_util::sync::CancellationToken;
use tracing::{Instrument, error, warn};

use crate::canvas::CanvasHandler;
use crate::generated::api_types::{
    LogRequest, ModelSwitchAutoTierRequest, ModelSwitchAutoTierResult, ModelSwitchToRequest,
    OpenCanvasInstance, PermissionDecisionRequest, RegisterEventInterestParams,
    ToolsGetCurrentMetadataResult, rpc_methods,
};
use crate::generated::session_events::{
    CommandExecuteData, ElicitationRequestedData, ExternalToolRequestedData, McpOauthRequiredData,
    SessionCanvasClosedData, SessionErrorData, SessionEventType, SessionIdleData, SessionMode,
};
use crate::handler::{
    AutoModeSwitchHandler, AutoModeSwitchResponse, ElicitationHandler, ExitPlanModeHandler,
    McpAuthHandler, McpAuthRequest, McpAuthResult, PermissionHandler, PermissionResult,
    UserInputHandler, UserInputResponse,
};
use crate::hooks::SessionHooks;
use crate::provider_token::BearerTokenProvider;
use crate::session_fs::SessionFsProvider;
use crate::trace_context::inject_trace_context;
use crate::transforms::SystemMessageTransform;
use crate::types::{
    AutoTier, AutoTierPreference, CommandContext, CommandDefinition, CommandHandler,
    CreateSessionResult, ElicitationRequest, ElicitationResult, ExitPlanModeData,
    GetMessagesResponse, MessageOptions, PermissionRequestData, RequestId, ResumeSessionConfig,
    ResumeSessionResult, SectionOverride, SessionCapabilities, SessionConfig, SessionEvent,
    SessionId, SetModelOptions, SystemMessageConfig, ToolInvocation, ToolResult,
    ToolResultExpanded, TraceContext, UiInputOptions, ensure_attachment_display_names,
};
use crate::{
    Client, Error, ErrorKind, JsonRpcResponse, SessionErrorKind, SessionEventNotification,
    error_codes,
};

/// Fixed name of the runtime's built-in tool-search tool. A client can replace
/// its behavior by registering a tool with this exact name and
/// `overrides_built_in_tool` set to `true`.
const TOOL_SEARCH_TOOL_NAME: &str = "tool_search_tool";

/// Default capacity of the per-session event broadcast buffer backing
/// [`Session::subscribe`] and [`PreparedSession::subscribe`].
///
/// Override per session with
/// [`SessionConfig::event_buffer_capacity`](crate::types::SessionConfig::event_buffer_capacity)
/// or
/// [`ResumeSessionConfig::event_buffer_capacity`](crate::types::ResumeSessionConfig::event_buffer_capacity).
pub const DEFAULT_EVENT_BUFFER_CAPACITY: usize = 512;

/// Validate a caller-supplied event buffer capacity and resolve the default.
///
/// Zero is rejected rather than clamped: a zero-capacity broadcast channel
/// cannot exist, and silently substituting a different capacity would hide a
/// caller bug.
fn resolve_event_buffer_capacity(capacity: Option<usize>) -> Result<usize, Error> {
    match capacity {
        Some(0) => Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "event_buffer_capacity must be greater than zero",
        )),
        Some(capacity) => Ok(capacity),
        None => Ok(DEFAULT_EVENT_BUFFER_CAPACITY),
    }
}

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
    pub managed_settings_enabled: bool,
    pub elicitation: Option<Arc<dyn ElicitationHandler>>,
    pub mcp_auth: Option<Arc<dyn McpAuthHandler>>,
    pub user_input: Option<Arc<dyn UserInputHandler>>,
    pub exit_plan_mode: Option<Arc<dyn ExitPlanModeHandler>>,
    pub auto_mode_switch: Option<Arc<dyn AutoModeSwitchHandler>>,
    pub tools: Arc<HashMap<String, Arc<dyn crate::tool::ToolHandler>>>,
}

type PendingExternalTools = Arc<ParkingLotMutex<HashMap<RequestId, Arc<CancellationToken>>>>;

struct PendingExternalToolGuard {
    request_id: RequestId,
    token: Arc<CancellationToken>,
    pending: PendingExternalTools,
}

impl Drop for PendingExternalToolGuard {
    fn drop(&mut self) {
        let mut pending = self.pending.lock();
        if pending
            .get(&self.request_id)
            .is_some_and(|token| Arc::ptr_eq(token, &self.token))
        {
            pending.remove(&self.request_id);
        }
    }
}

impl PendingExternalToolGuard {
    fn claim(&self) -> bool {
        let mut pending = self.pending.lock();
        if pending
            .get(&self.request_id)
            .is_some_and(|token| Arc::ptr_eq(token, &self.token))
        {
            pending.remove(&self.request_id);
            true
        } else {
            false
        }
    }
}

fn has_managed_settings(
    enable_managed_settings: Option<bool>,
    managed_settings: Option<&crate::types::ManagedSettings>,
) -> bool {
    enable_managed_settings == Some(true) || managed_settings.is_some()
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
    session_id: PendingSessionId,
    shutdown: CancellationToken,
    external_tools_shutdown: CancellationToken,
    disarmed: bool,
}

/// Which session ID a [`PendingSessionRegistration`] should unregister on
/// cleanup.
///
/// `session.create` for cloud sessions without a caller-pinned ID does not
/// know the ID until the response arrives, at which point the inline
/// response callback registers it and stashes it. The guard therefore reads
/// the stash at cleanup time instead of capturing an ID up front.
enum PendingSessionId {
    /// The ID was known before the RPC was issued (resume, and create with a
    /// client- or caller-supplied ID).
    Known(SessionId, crate::router::RegistrationToken),
    /// Server-assigned ID, populated by the `session.create` inline response
    /// callback. `None` in the stash means nothing was ever registered.
    Deferred(Arc<ParkingLotMutex<Option<(SessionId, crate::router::SessionRegistration)>>>),
}

impl PendingSessionRegistration {
    fn new(
        client: Client,
        session_id: SessionId,
        token: crate::router::RegistrationToken,
        shutdown: CancellationToken,
        external_tools_shutdown: CancellationToken,
    ) -> Self {
        Self {
            client,
            session_id: PendingSessionId::Known(session_id, token),
            shutdown,
            external_tools_shutdown,
            disarmed: false,
        }
    }

    /// Guard for a registration whose session ID is assigned by the server.
    fn deferred(
        client: Client,
        stash: Arc<ParkingLotMutex<Option<(SessionId, crate::router::SessionRegistration)>>>,
        shutdown: CancellationToken,
        external_tools_shutdown: CancellationToken,
    ) -> Self {
        Self {
            client,
            session_id: PendingSessionId::Deferred(stash),
            shutdown,
            external_tools_shutdown,
            disarmed: false,
        }
    }

    fn registered_id(&self) -> Option<SessionId> {
        match &self.session_id {
            PendingSessionId::Known(id, _) => Some(id.clone()),
            PendingSessionId::Deferred(stash) => stash.lock().as_ref().map(|(id, _)| id.clone()),
        }
    }

    /// Re-target the guard at a now-known session ID. Used by
    /// `session.create` once the response has been parsed and the stash has
    /// been drained into the event loop.
    fn resolve_to(&mut self, session_id: SessionId, token: crate::router::RegistrationToken) {
        self.session_id = PendingSessionId::Known(session_id, token);
    }

    async fn cleanup(mut self, event_loop: JoinHandle<()>) {
        self.external_tools_shutdown.cancel();
        self.shutdown.cancel();
        let _ = event_loop.await;
        if let Some(id) = self.registered_id() {
            if let PendingSessionId::Known(_, token) = self.session_id {
                self.client.unregister_session_owned(&id, token);
            } else if let PendingSessionId::Deferred(stash) = &self.session_id
                && let Some((id, registration)) = stash.lock().as_ref()
            {
                self.client.unregister_session_owned(id, registration.token);
            }
        }
        self.disarmed = true;
    }

    fn disarm(&mut self) {
        self.disarmed = true;
    }
}

impl Drop for PendingSessionRegistration {
    fn drop(&mut self) {
        if !self.disarmed {
            self.external_tools_shutdown.cancel();
            self.shutdown.cancel();
            if let Some(id) = self.registered_id() {
                if let PendingSessionId::Known(_, token) = self.session_id {
                    self.client.unregister_session_owned(&id, token);
                } else if let PendingSessionId::Deferred(stash) = &self.session_id
                    && let Some((id, registration)) = stash.lock().as_ref()
                {
                    self.client.unregister_session_owned(id, registration.token);
                }
            }
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
    /// Cancels only host-owned external tool callbacks. Disconnect signals this
    /// before the destroy RPC without stopping unrelated event delivery.
    external_tools_shutdown: CancellationToken,
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
    github_token_registration:
        ParkingLotMutex<Option<crate::github_token::GitHubTokenRegistration>>,
    /// Identity of this session's router registration.
    registration_token: crate::router::RegistrationToken,
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
    /// and awaits the loop's natural exit rather than aborting the task, so
    /// the loop always stops between iterations instead of at an arbitrary
    /// await point. See RFD-400 review finding #3.
    ///
    /// Inbound requests are dispatched to their own spawned tasks, which this
    /// call does not await. A handler (permission callback, tool call,
    /// elicitation response) still running at teardown may therefore outlive
    /// the loop, and its response can be lost if the connection closes first.
    /// Await your own handler work before calling this if it must complete.
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
        let auto_tier = opts.auto_tier.clone();
        let request = ModelSwitchToRequest {
            auto_tier: match &auto_tier {
                Some(AutoTierPreference::Tier(tier)) => Some(tier.clone()),
                _ => None,
            },
            compaction_decision: None,
            context_tier: opts.context_tier,
            defer_if_model_change_queued: None,
            model_capabilities: opts.model_capabilities,
            model_change_scope: None,
            model_id: model.to_string(),
            picker_persistence: None,
            reasoning_effort: opts.reasoning_effort,
            reasoning_summary: opts.reasoning_summary,
            repo_scope: None,
            require_available: None,
            run_compaction_preflight: None,
            source: None,
            verbosity: None,
        };

        if matches!(auto_tier, Some(AutoTierPreference::Reset)) {
            // The generated request skips a `None` tier, which the runtime reads
            // as "leave the preference alone" rather than "use provider-default
            // routing", so send an explicit null instead.
            let mut wire_params = serde_json::to_value(request)?;
            wire_params["sessionId"] = serde_json::Value::String(self.id.to_string());
            wire_params["autoTier"] = serde_json::Value::Null;
            self.client
                .call("session.model.switchTo", Some(wire_params))
                .await?;
            return Ok(());
        }

        self.rpc().model().switch_to(request).await?;
        Ok(())
    }

    /// Change the Auto routing preference without changing the selected model.
    ///
    /// The runtime does not apply the preference immediately. It records the
    /// request and commits it only when a later user turn using the `auto`
    /// model successfully obtains a usable model from the provider. A
    /// [`ModelSwitchAutoTierStatus::Pending`] status therefore confirms that the
    /// request was accepted, not that it took effect.
    ///
    /// Watch for the outcome through the `session.model_change` event on
    /// success, or the ephemeral `session.auto_tier_switch_failed` event on
    /// failure. You can also read the current committed and in-flight state at
    /// any time through `session.rpc().model().get_current()`.
    ///
    /// Only the most recent request survives: issuing a new request replaces any
    /// earlier one that has not yet been claimed by a turn.
    ///
    /// Pass `None` to return to the provider's default Auto routing.
    ///
    /// **Experimental.** Part of an experimental Auto routing surface and may
    /// change or be removed in a future release.
    ///
    /// # Cancel safety
    ///
    /// **Cancel-safe.** Single `session.model.switchAutoTier` RPC; the
    /// underlying [`Client::call`](crate::Client::call) is cancel-safe via the
    /// writer-actor.
    ///
    /// [`ModelSwitchAutoTierStatus::Pending`]: crate::generated::api_types::ModelSwitchAutoTierStatus::Pending
    pub async fn set_auto_tier(
        &self,
        auto_tier: Option<AutoTier>,
    ) -> Result<ModelSwitchAutoTierResult, Error> {
        self.rpc()
            .model()
            .switch_auto_tier(ModelSwitchAutoTierRequest {
                auto_tier,
                source: None,
            })
            .await
    }

    /// Disconnect this session from the CLI.
    ///
    /// Sends the `session.detach` RPC, stops the event loop, and unregisters
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
        self.client.detach_session(&self.id).await?;
        self.external_tools_shutdown.cancel();
        self.stop_event_loop().await;
        self.github_token_registration.lock().take();
        self.client
            .unregister_session_owned(&self.id, self.registration_token);
        Ok(())
    }

    /// Deprecated alias for [`disconnect`](Self::disconnect).
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
        // its next select poll and break cleanly. We do NOT abort the
        // JoinHandle — that would land at any await point in the loop body,
        // potentially leaving the CLI with an unanswered request id.
        // RFD-400 review finding #3. Requests already dispatched to their
        // own tasks are not tracked here and may outlive the session.
        //
        // The handle itself is left in `event_loop` to be reaped by the
        // tokio runtime when it next polls; we intentionally don't await
        // it here because Drop is sync.
        self.shutdown.cancel();
        self.external_tools_shutdown.cancel();
        self.github_token_registration.lock().take();
        self.client
            .unregister_session_owned(&self.id, self.registration_token);
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
    /// Prepare a new session without touching the transport.
    ///
    /// Returns a [`PreparedSession`] that owns the session's event broadcast
    /// channel, so callers can install an
    /// [`EventSubscription`](crate::subscription::EventSubscription) via
    /// [`PreparedSession::subscribe`] *before* any protocol activity starts.
    /// Call [`PreparedSession::start`] to actually create the session.
    ///
    /// This is the loss-free entry point for consumers that must observe
    /// every *routed* event a session emits, including events the CLI emits
    /// while `session.create` is still in flight and ephemeral events (such
    /// as `session.idle`) that cannot be recovered from
    /// [`Session::get_messages`]. [`create_session`](Self::create_session)
    /// is a thin wrapper over `prepare_session(...)?.start()` and cannot
    /// offer the same guarantee, because the subscription can only be
    /// installed after the returned `Session` exists.
    ///
    /// Routing requires a known session ID. When the server assigns the ID,
    /// the SDK cannot register the session on its notification router until
    /// the `session.create` response arrives, so notifications emitted
    /// before that point are not routable and stay unobservable. Pin
    /// [`SessionConfig::session_id`](crate::types::SessionConfig::session_id)
    /// for complete pre-response coverage — see the "Server-assigned session
    /// IDs" section on [`PreparedSession`].
    ///
    /// # Inertness
    ///
    /// `prepare_session` performs no router registration, spawns no task,
    /// and writes nothing to the wire. It only validates
    /// [`event_buffer_capacity`](SessionConfig::event_buffer_capacity),
    /// allocates a local broadcast channel and cancellation token, and
    /// stores the config. Dropping the returned handle without starting it
    /// leaves no client-side or server-side state behind and closes every
    /// subscription taken from it.
    ///
    /// # Errors
    ///
    /// Returns [`ErrorKind::InvalidConfig`] if
    /// [`event_buffer_capacity`](SessionConfig::event_buffer_capacity) is
    /// `Some(0)`. All other configuration and protocol errors surface from
    /// [`PreparedSession::start`], with the same
    /// [`ErrorKind`]s [`create_session`](Self::create_session) has always
    /// returned.
    ///
    /// # Example
    ///
    /// ```no_run
    /// # use github_copilot_sdk::{Client, SessionConfig};
    /// # async fn example(client: Client) -> Result<(), github_copilot_sdk::Error> {
    /// let prepared = client.prepare_session(SessionConfig::default())?;
    /// let mut events = prepared.subscribe();
    /// let drain = tokio::spawn(async move {
    ///     while let Ok(event) = events.recv().await {
    ///         println!("{}", event.event_type);
    ///     }
    /// });
    /// let session = prepared.start().await?;
    /// # let _ = (session, drain);
    /// # Ok(())
    /// # }
    /// ```
    pub fn prepare_session(&self, config: SessionConfig) -> Result<PreparedSession, Error> {
        let capacity = resolve_event_buffer_capacity(config.event_buffer_capacity)?;
        Ok(PreparedSession::new(
            self.clone(),
            PreparedKind::Create(Box::new(config)),
            capacity,
        ))
    }

    /// Prepare a session resume without touching the transport.
    ///
    /// The resume counterpart of [`prepare_session`](Self::prepare_session);
    /// see that method for the inertness guarantee, error semantics, and
    /// rationale. Particularly relevant on resume with
    /// [`continue_pending_work`](ResumeSessionConfig::continue_pending_work),
    /// where the runtime can start emitting events (and reach
    /// `session.idle`) while `session.resume` is still in flight.
    ///
    /// # Errors
    ///
    /// Returns [`ErrorKind::InvalidConfig`] if
    /// [`event_buffer_capacity`](ResumeSessionConfig::event_buffer_capacity)
    /// is `Some(0)`.
    pub fn prepare_resume_session(
        &self,
        config: ResumeSessionConfig,
    ) -> Result<PreparedSession, Error> {
        let capacity = resolve_event_buffer_capacity(config.event_buffer_capacity)?;
        Ok(PreparedSession::new(
            self.clone(),
            PreparedKind::Resume(Box::new(config)),
            capacity,
        ))
    }

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
    ///
    /// # Event delivery
    ///
    /// Equivalent to `prepare_session(config)?.start().await`. Because the
    /// first subscription can only be taken from the returned [`Session`],
    /// events the runtime emits before this call returns are broadcast with
    /// no receiver installed and are therefore not delivered to
    /// [`Session::subscribe`]. Use
    /// [`prepare_session`](Self::prepare_session) when startup events
    /// matter.
    pub async fn create_session(&self, config: SessionConfig) -> Result<Session, Error> {
        self.prepare_session(config)?.start().await
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
    ///
    /// # Event delivery
    ///
    /// Equivalent to `prepare_resume_session(config)?.start().await`, and
    /// carries the same startup-event caveat documented on
    /// [`create_session`](Self::create_session). Use
    /// [`prepare_resume_session`](Self::prepare_resume_session) when
    /// startup events matter.
    pub async fn resume_session(&self, config: ResumeSessionConfig) -> Result<Session, Error> {
        self.prepare_resume_session(config)?.start().await
    }

    async fn start_prepared_create(
        &self,
        mut config: SessionConfig,
        event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
        shutdown: CancellationToken,
    ) -> Result<Session, Error> {
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
        config.memory = crate::mode::memory_for_mode(mode, config.memory.take());
        config.enable_experimental_mode =
            crate::mode::experimental_mode_for_mode(mode, config.enable_experimental_mode);
        if mode == crate::ClientMode::Empty {
            if config.enable_session_telemetry.is_none() {
                config.enable_session_telemetry = Some(false);
            }
            if config.skip_embedding_retrieval.is_none() {
                config.skip_embedding_retrieval = Some(true);
            }
            if config.enable_on_demand_instruction_discovery.is_none() {
                config.enable_on_demand_instruction_discovery = Some(false);
            }
            if config.enable_file_hooks.is_none() {
                config.enable_file_hooks = Some(false);
            }
            if config.enable_host_git_operations.is_none() {
                config.enable_host_git_operations = Some(false);
            }
            if config.enable_session_store.is_none() {
                config.enable_session_store = Some(false);
            }
            if config.enable_skills.is_none() {
                config.enable_skills = Some(false);
            }
        }
        if mode == crate::ClientMode::Empty && config.mcp_oauth_token_storage.is_none() {
            config.mcp_oauth_token_storage = Some("in-memory".into());
        }
        if mode == crate::ClientMode::Empty && config.embedding_cache_storage.is_none() {
            config.embedding_cache_storage = Some("in-memory".into());
        }
        config.custom_agents_local_only =
            crate::mode::resolve_custom_agents_local_only(mode, config.custom_agents_local_only);
        let opt_skip_custom_instructions = config.skip_custom_instructions;
        let opt_custom_agents_local_only = config.custom_agents_local_only;
        let opt_coauthor_enabled = config.coauthor_enabled;
        let opt_manage_schedule_enabled = config.manage_schedule_enabled;
        let opt_included_builtin_skills = config.included_builtin_skills.take();
        let (mut wire, mut runtime) = config.into_wire(local_session_id.clone())?;
        wire.enable_github_telemetry_forwarding =
            self.inner.on_github_telemetry.is_some().then_some(true);

        let permission_handler = crate::permission::resolve_handler(
            runtime.permission_handler.take(),
            runtime.permission_policy.take(),
        );
        let handlers = SessionHandlers {
            permission: permission_handler,
            managed_settings_enabled: has_managed_settings(
                wire.enable_managed_settings,
                wire.managed_settings.as_ref(),
            ),
            elicitation: runtime.elicitation_handler.take(),
            mcp_auth: runtime.mcp_auth_handler.take(),
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
        let bearer_token_providers = std::mem::take(&mut runtime.bearer_token_providers);
        let github_token_registration = runtime
            .github_token_provider
            .take()
            .map(|provider| self.register_github_token_provider(provider));
        wire.github_token_provider_registration_id = github_token_registration
            .as_ref()
            .map(|registration| registration.id().to_string());
        let has_mcp_auth_handler = handlers.mcp_auth.is_some();
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
        let external_tools_shutdown = self.inner.rpc.connection_closed_token();

        // For cloud sessions (use_server_generated_id), defer session
        // registration to the inline callback so the read task registers
        // the session synchronously the instant the response arrives.
        // For non-cloud sessions, register up-front so the CLI can issue
        // session-scoped requests during session.create processing.
        let inline_stash: Arc<
            ParkingLotMutex<Option<(SessionId, crate::router::SessionRegistration)>>,
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
                // Register and stash under a single stash-lock hold. The
                // cancellation guard identifies the session to unregister by
                // peeking this stash, so registering outside the lock would
                // leave a window where a concurrent guard drop (caller
                // cancellation) sees `None` and leaks the registration.
                // `register_session` takes the router lock, never the stash
                // lock, so there is no lock-order inversion here.
                let mut stashed = stash.lock();
                let registration = client.register_session(&parsed.session_id);
                *stashed = Some((parsed.session_id, registration));
                Ok(())
            }))
        };

        // Armed for the whole startup sequence: any early return, and any
        // drop of this future (caller cancellation), cancels the session
        // token and unregisters whatever was registered on the router. For
        // the cloud path the ID is only known once the inline callback has
        // run, so the guard reads the stash at cleanup time.
        let mut pending_registration = match local_session_id {
            Some(ref sid) => {
                let token = inline_stash
                    .lock()
                    .as_ref()
                    .expect("session registration must exist")
                    .1
                    .token;
                PendingSessionRegistration::new(
                    self.clone(),
                    sid.clone(),
                    token,
                    shutdown.clone(),
                    external_tools_shutdown.clone(),
                )
            }
            None => PendingSessionRegistration::deferred(
                self.clone(),
                inline_stash.clone(),
                shutdown.clone(),
                external_tools_shutdown.clone(),
            ),
        };

        let rpc_start = Instant::now();
        let result = self
            .call_with_inline_callback("session.create", Some(params), inline_callback)
            .await?;
        tracing::debug!(
            elapsed_ms = rpc_start.elapsed().as_millis(),
            "Client::create_session session creation request completed successfully"
        );
        let create_result: CreateSessionResult = serde_json::from_value(result)?;

        if let Some(ref requested) = local_session_id
            && create_result.session_id != *requested
        {
            return Err(ErrorKind::Session(SessionErrorKind::SessionIdMismatch {
                requested: requested.clone(),
                returned: create_result.session_id.clone(),
            })
            .into());
        }

        let (session_id, registration) = inline_stash
            .lock()
            .take()
            .expect("session registration must have populated stash on success");
        let channels = registration.channels;
        let registration_token = registration.token;
        pending_registration.resolve_to(session_id.clone(), registration_token);
        let event_loop = spawn_event_loop(
            session_id.clone(),
            self.clone(),
            handlers,
            hooks,
            transforms,
            command_handlers,
            canvas_handler,
            session_fs_provider,
            bearer_token_providers,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
            open_canvases.clone(),
            event_tx.clone(),
            shutdown.clone(),
            external_tools_shutdown.clone(),
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
        if has_mcp_auth_handler
            && let Err(error) = register_mcp_auth_interest(self, &session_id).await
        {
            pending_registration.cleanup(event_loop).await;
            return Err(error);
        }

        tracing::debug!(
            elapsed_ms = total_start.elapsed().as_millis(),
            session_id = %session_id,
            "Client::create_session complete"
        );
        pending_registration.disarm();
        let session = Session {
            id: session_id,
            cwd: self.cwd().clone(),
            workspace_path: create_result.workspace_path,
            remote_url: create_result.remote_url,
            client: self.clone(),
            event_loop: ParkingLotMutex::new(Some(event_loop)),
            shutdown,
            external_tools_shutdown,
            idle_waiter,
            capabilities,
            open_canvases,
            event_tx,
            github_token_registration: ParkingLotMutex::new(github_token_registration),
            registration_token,
        };
        apply_mode_post_create_patch(
            &session,
            mode,
            opt_skip_custom_instructions,
            opt_custom_agents_local_only,
            opt_coauthor_enabled,
            opt_manage_schedule_enabled,
            opt_included_builtin_skills,
        )
        .await?;
        if let Some(registration) = session.github_token_registration.lock().as_ref() {
            registration.claim(session.id.clone());
        } else {
            self.retire_github_token_provider(&session.id);
        }
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
    async fn start_prepared_resume(
        &self,
        mut config: ResumeSessionConfig,
        event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
        shutdown: CancellationToken,
    ) -> Result<Session, Error> {
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
        config.memory = crate::mode::memory_for_mode(mode, config.memory.take());
        config.enable_experimental_mode =
            crate::mode::experimental_mode_for_mode(mode, config.enable_experimental_mode);
        if mode == crate::ClientMode::Empty {
            if config.enable_session_telemetry.is_none() {
                config.enable_session_telemetry = Some(false);
            }
            if config.skip_embedding_retrieval.is_none() {
                config.skip_embedding_retrieval = Some(true);
            }
            if config.enable_on_demand_instruction_discovery.is_none() {
                config.enable_on_demand_instruction_discovery = Some(false);
            }
            if config.enable_file_hooks.is_none() {
                config.enable_file_hooks = Some(false);
            }
            if config.enable_host_git_operations.is_none() {
                config.enable_host_git_operations = Some(false);
            }
            if config.enable_session_store.is_none() {
                config.enable_session_store = Some(false);
            }
            if config.enable_skills.is_none() {
                config.enable_skills = Some(false);
            }
        }
        if mode == crate::ClientMode::Empty && config.mcp_oauth_token_storage.is_none() {
            config.mcp_oauth_token_storage = Some("in-memory".into());
        }
        if mode == crate::ClientMode::Empty && config.embedding_cache_storage.is_none() {
            config.embedding_cache_storage = Some("in-memory".into());
        }
        config.custom_agents_local_only =
            crate::mode::resolve_custom_agents_local_only(mode, config.custom_agents_local_only);
        let opt_skip_custom_instructions = config.skip_custom_instructions;
        let opt_custom_agents_local_only = config.custom_agents_local_only;
        let opt_coauthor_enabled = config.coauthor_enabled;
        let opt_manage_schedule_enabled = config.manage_schedule_enabled;
        let opt_included_builtin_skills = config.included_builtin_skills.take();
        let (mut wire, mut runtime) = config.into_wire()?;
        wire.enable_github_telemetry_forwarding =
            self.inner.on_github_telemetry.is_some().then_some(true);

        let permission_handler = crate::permission::resolve_handler(
            runtime.permission_handler.take(),
            runtime.permission_policy.take(),
        );
        let handlers = SessionHandlers {
            permission: permission_handler,
            managed_settings_enabled: has_managed_settings(
                wire.enable_managed_settings,
                wire.managed_settings.as_ref(),
            ),
            elicitation: runtime.elicitation_handler.take(),
            mcp_auth: runtime.mcp_auth_handler.take(),
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
        let bearer_token_providers = std::mem::take(&mut runtime.bearer_token_providers);
        let github_token_registration = runtime
            .github_token_provider
            .take()
            .map(|provider| self.register_github_token_provider(provider));
        wire.github_token_provider_registration_id = github_token_registration
            .as_ref()
            .map(|registration| registration.id().to_string());
        let has_mcp_auth_handler = handlers.mcp_auth.is_some();
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
        let registration = self.register_session(&session_id);
        let registration_token = registration.token;
        let channels = registration.channels;
        let idle_waiter = Arc::new(ParkingLotMutex::new(None));
        let open_canvases = Arc::new(parking_lot::RwLock::new(Vec::new()));
        let external_tools_shutdown = self.inner.rpc.connection_closed_token();
        let event_loop = spawn_event_loop(
            session_id.clone(),
            self.clone(),
            handlers,
            hooks,
            transforms,
            command_handlers,
            canvas_handler,
            session_fs_provider,
            bearer_token_providers,
            channels,
            idle_waiter.clone(),
            capabilities.clone(),
            open_canvases.clone(),
            event_tx.clone(),
            shutdown.clone(),
            external_tools_shutdown.clone(),
        );
        let mut registration = PendingSessionRegistration::new(
            self.clone(),
            session_id.clone(),
            registration_token,
            shutdown.clone(),
            external_tools_shutdown.clone(),
        );
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
        if has_mcp_auth_handler
            && let Err(error) = register_mcp_auth_interest(self, &session_id).await
        {
            registration.cleanup(event_loop).await;
            return Err(error);
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
        // Upsert resume snapshots rather than replacing wholesale. Live
        // `session.canvas.opened` notifications can arrive on the event loop
        // while `session.resume` is in flight; a wholesale replace would
        // discard those updates.
        {
            let mut snapshots = open_canvases.write();
            for snapshot in resume_result.open_canvases.unwrap_or_default() {
                upsert_open_canvas_snapshot(&mut snapshots, snapshot);
            }
        }

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
            external_tools_shutdown,
            idle_waiter,
            capabilities,
            open_canvases,
            event_tx,
            github_token_registration: ParkingLotMutex::new(github_token_registration),
            registration_token,
        };
        apply_mode_post_create_patch(
            &session,
            mode,
            opt_skip_custom_instructions,
            opt_custom_agents_local_only,
            opt_coauthor_enabled,
            opt_manage_schedule_enabled,
            opt_included_builtin_skills,
        )
        .await?;
        if let Some(registration) = session.github_token_registration.lock().as_ref() {
            registration.claim(session.id.clone());
        } else {
            self.retire_github_token_provider(&session.id);
        }
        Ok(session)
    }
}

/// A session that has been configured but not yet created on the CLI.
///
/// Returned by [`Client::prepare_session`] and
/// [`Client::prepare_resume_session`]. Its purpose is to make the session's
/// event stream observable *before* any protocol activity starts:
/// [`subscribe`](Self::subscribe) installs a receiver on the same broadcast
/// channel the eventual [`Session`] uses, so events the runtime emits while
/// `session.create` / `session.resume` is still in flight are delivered
/// rather than dropped for lack of a receiver.
///
/// # Lifecycle
///
/// A prepared handle is inert. It holds only a broadcast sender, a
/// cancellation token, the client handle, and the config — it performs no
/// router registration, spawns no task, and writes nothing to the wire
/// until [`start`](Self::start) is first polled.
///
/// * Dropping it without starting leaves no client-side or server-side
///   state, and closes every subscription taken from it.
/// * Dropping the [`start`](Self::start) future mid-flight cancels the
///   session token, unregisters the session from the router if it was
///   registered, and closes early subscriptions. A retry with the same
///   session ID succeeds. Cleanup of already-spawned tasks is signalled,
///   not awaited: `Drop` is synchronous and cannot await, so the event loop
///   terminates promptly but not synchronously.
/// * A startup error from [`start`](Self::start) performs the same cleanup
///   and preserves the [`ErrorKind`] the equivalent
///   [`Client::create_session`] / [`Client::resume_session`] call has always
///   returned.
///
/// [`start`](Self::start) consumes `self` and the type is deliberately not
/// [`Clone`], so a prepared session can be started at most once and can
/// never produce two event loops.
///
/// # Buffering
///
/// The broadcast buffer is finite —
/// [`DEFAULT_EVENT_BUFFER_CAPACITY`] unless
/// [`SessionConfig::event_buffer_capacity`] /
/// [`ResumeSessionConfig::event_buffer_capacity`] overrides it. Subscribers
/// that fall behind observe
/// [`Lagged`](crate::subscription::Lagged) instead of applying backpressure
/// to the event loop. Consumers that need a lossless view of a large
/// startup burst must either configure a capacity that covers it or drain
/// the subscription concurrently with [`start`](Self::start).
///
/// # Server-assigned session IDs
///
/// For cloud sessions without a caller-supplied session ID, the CLI assigns
/// the ID and the SDK can only register the session on its notification
/// router once the `session.create` response arrives. Notifications the
/// server emits before that point are not routable to any session and are
/// therefore not observable. The guarantee this type provides is narrower
/// and precise: **routed** events are never dropped for lack of an
/// installed receiver. Pin
/// [`SessionConfig::session_id`](crate::types::SessionConfig::session_id)
/// to get registration before the RPC and full pre-response coverage.
#[must_use = "a PreparedSession does nothing until started"]
pub struct PreparedSession {
    client: Client,
    kind: PreparedKind,
    event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
    shutdown: CancellationToken,
}

/// Which startup path a [`PreparedSession`] runs when started. Boxed
/// because the two config types are large and differently sized.
enum PreparedKind {
    Create(Box<SessionConfig>),
    Resume(Box<ResumeSessionConfig>),
}

impl PreparedSession {
    fn new(client: Client, kind: PreparedKind, event_buffer_capacity: usize) -> Self {
        let (event_tx, _) = tokio::sync::broadcast::channel(event_buffer_capacity);
        Self {
            client,
            kind,
            event_tx,
            shutdown: CancellationToken::new(),
        }
    }

    /// Subscribe to this session's events before it starts.
    ///
    /// The returned [`EventSubscription`](crate::subscription::EventSubscription)
    /// is backed by the same broadcast channel
    /// [`Session::subscribe`] returns after [`start`](Self::start)
    /// succeeds, so a subscription taken here observes the full event
    /// stream from the session's first routed event onward — including
    /// ephemeral events such as `session.idle` that
    /// [`Session::get_messages`] cannot recover.
    ///
    /// May be called any number of times, and each subscriber receives its
    /// own copy of the stream — subject to the buffering contract above. A
    /// subscriber that falls further behind than the configured capacity
    /// observes [`Lagged`](crate::subscription::Lagged) and skips the
    /// events it missed, rather than stalling the session's event loop.
    /// Subscriptions taken here close if the prepared session is dropped
    /// without starting, or if startup fails.
    pub fn subscribe(&self) -> crate::subscription::EventSubscription {
        crate::subscription::EventSubscription::new(self.event_tx.subscribe())
    }

    /// Create or resume the session on the CLI.
    ///
    /// This is where all protocol activity happens: config validation,
    /// router registration, the `session.create` / `session.resume` RPC,
    /// and the event loop spawn. Nothing observable occurs until this
    /// future is first polled.
    ///
    /// # Errors
    ///
    /// Returns the same errors as [`Client::create_session`] /
    /// [`Client::resume_session`] — including
    /// [`ErrorKind::InvalidConfig`] for invalid configs, transport and RPC
    /// failures, and
    /// [`SessionIdMismatch`](crate::SessionErrorKind::SessionIdMismatch)
    /// when the CLI returns a different session ID than the one requested.
    /// Every error path unregisters the session and closes subscriptions
    /// taken from this handle.
    pub async fn start(self) -> Result<Session, Error> {
        let Self {
            client,
            kind,
            event_tx,
            shutdown,
        } = self;
        match kind {
            PreparedKind::Create(config) => {
                client
                    .start_prepared_create(*config, event_tx, shutdown)
                    .await
            }
            PreparedKind::Resume(config) => {
                client
                    .start_prepared_resume(*config, event_tx, shutdown)
                    .await
            }
        }
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
    opt_included_builtin_skills: Option<Vec<String>>,
) -> Result<(), Error> {
    let Some(patch) = build_mode_post_create_patch(
        mode,
        opt_skip_custom_instructions,
        opt_custom_agents_local_only,
        opt_coauthor_enabled,
        opt_manage_schedule_enabled,
        opt_included_builtin_skills,
    ) else {
        return Ok(());
    };
    if let Err(error) = session.rpc().options().update(patch).await {
        let _ = session.disconnect().await;
        return Err(error);
    }
    Ok(())
}

/// Builds the `session.options.update` patch applied immediately after a session
/// is created or resumed, or returns `None` when no patch should be sent.
///
/// Under [`ClientMode::Empty`](crate::ClientMode::Empty) the overridable feature
/// flags fall back to safe defaults (caller values win), while
/// `installed_plugins` is unconditionally empty. `included_builtin_skills`
/// defaults to an empty list, but callers can explicitly allow selected
/// runtime-bundled skills. Under other modes only explicitly-set fields are
/// forwarded.
fn build_mode_post_create_patch(
    mode: crate::ClientMode,
    opt_skip_custom_instructions: Option<bool>,
    opt_custom_agents_local_only: Option<bool>,
    opt_coauthor_enabled: Option<bool>,
    opt_manage_schedule_enabled: Option<bool>,
    opt_included_builtin_skills: Option<Vec<String>>,
) -> Option<crate::generated::api_types::SessionUpdateOptionsParams> {
    use crate::generated::api_types::SessionUpdateOptionsParams;
    let mut patch = SessionUpdateOptionsParams::default();
    let should_send = if mode == crate::ClientMode::Empty {
        patch.skip_custom_instructions = Some(opt_skip_custom_instructions.unwrap_or(true));
        patch.custom_agents_local_only = Some(opt_custom_agents_local_only.unwrap_or(true));
        patch.coauthor_enabled = Some(opt_coauthor_enabled.unwrap_or(false));
        patch.manage_schedule_enabled = Some(opt_manage_schedule_enabled.unwrap_or(false));
        patch.installed_plugins = Some(Vec::new());
        patch.included_builtin_skills = Some(opt_included_builtin_skills.unwrap_or_default());
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
        if let Some(v) = opt_included_builtin_skills {
            patch.included_builtin_skills = Some(v);
            any = true;
        }
        any
    };
    if !should_send {
        return None;
    }
    Some(patch)
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

fn remove_open_canvas_snapshot(snapshots: &mut Vec<OpenCanvasInstance>, instance_id: &str) {
    snapshots.retain(|open| open.instance_id != instance_id);
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
    bearer_token_providers: HashMap<String, Arc<dyn BearerTokenProvider>>,
    channels: crate::router::SessionChannels,
    idle_waiter: Arc<ParkingLotMutex<Option<IdleWaiter>>>,
    capabilities: Arc<parking_lot::RwLock<SessionCapabilities>>,
    open_canvases: Arc<parking_lot::RwLock<Vec<OpenCanvasInstance>>>,
    event_tx: tokio::sync::broadcast::Sender<SessionEvent>,
    shutdown: CancellationToken,
    external_tools_shutdown: CancellationToken,
) -> JoinHandle<()> {
    let crate::router::SessionChannels {
        mut notifications,
        mut requests,
    } = channels;
    let pending_external_tools: PendingExternalTools =
        Arc::new(ParkingLotMutex::new(HashMap::new()));

    let span = tracing::error_span!("session_event_loop", session_id = %session_id);
    tokio::spawn(
        async move {
            loop {
                // `mpsc::UnboundedReceiver::recv` and
                // `CancellationToken::cancelled` are both cancel-safe per
                // RFD 400.
                //
                // Inbound JSON-RPC *requests* are dispatched fire-and-forget:
                // each `handle_request` runs in its own spawned task that
                // awaits the handler and sends that request's response. This
                // mirrors the other Copilot SDKs and moves concurrency to the
                // request-dispatch boundary, so any slow handler — not just
                // `userInput.request` (which can stay pending for the full
                // input backstop of several minutes), but also `exitPlanMode`,
                // `autoModeSwitch`, hooks, transforms, or canvas/session-FS
                // providers — cannot park the reader loop and starve sibling
                // requests or co-emitted notifications. JSON-RPC permits
                // concurrent requests and out-of-order responses, so the SDK
                // does not serialize them.
                //
                // `handle_notification` is awaited inline because it only
                // performs fast dispatch work; its slow interactive callbacks
                // (permission/tool/elicitation) are themselves spawned as child
                // tasks. All of these spawned tasks intentionally outlive the
                // parent loop and own their own cleanup — RFD 400's "spawn
                // background tasks to perform cancel-unsafe operations" pattern.
                tokio::select! {
                    _ = shutdown.cancelled() => break,
                    Some(notification) = notifications.recv() => {
                        handle_notification(
                            &session_id, &client, &handlers, &command_handlers, notification, &idle_waiter, &capabilities, &open_canvases, &event_tx, &shutdown, &external_tools_shutdown, &pending_external_tools,
                        ).await;
                    }
                    Some(request) = requests.recv() => {
                        // Clone the Arc-backed dispatch context into the task so
                        // the spawned `handle_request` future is `'static`. All
                        // clones are cheap (Arc refcount bumps / small maps).
                        let span = tracing::error_span!("session_request_handler", session_id = %session_id);
                        let session_id = session_id.clone();
                        let client = client.clone();
                        let handlers = handlers.clone();
                        let hooks = hooks.clone();
                        let transforms = transforms.clone();
                        let canvas_handler = canvas_handler.clone();
                        let session_fs_provider = session_fs_provider.clone();
                        let bearer_token_providers = bearer_token_providers.clone();
                        let request_id = request.id;
                        let method = request.method.clone();
                        tokio::spawn(
                            async move {
                                let ctx = RequestDispatchContext {
                                    client: &client,
                                    handlers: &handlers,
                                    hooks: hooks.as_deref(),
                                    transforms: transforms.as_deref(),
                                    canvas_handler: canvas_handler.as_ref(),
                                    session_fs_provider: session_fs_provider.as_ref(),
                                    bearer_token_providers: &bearer_token_providers,
                                };
                                let dispatch = handle_request(&session_id, ctx, request);
                                if AssertUnwindSafe(dispatch).catch_unwind().await.is_err() {
                                    // Tokio isolates the panic to this task, so without a
                                    // reply the CLI waits out its own timeout on this id.
                                    error!(method = %method, "request handler panicked");
                                    let _ = send_error_response(
                                        &client,
                                        request_id,
                                        error_codes::INTERNAL_ERROR,
                                        "request handler panicked",
                                    )
                                    .await;
                                }
                            }
                            .instrument(span),
                        );
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

fn permission_request_data(
    event_data: &Value,
    managed_settings_enabled: bool,
) -> PermissionRequestData {
    let request_data = event_data
        .get("permissionRequest")
        .cloned()
        .unwrap_or_else(|| event_data.clone());
    let managed_approval_required = match request_data.get("managedApprovalRequired") {
        None => None,
        Some(Value::Bool(value)) => Some(*value),
        Some(_) => Some(true),
    };
    match serde_json::from_value::<PermissionRequestData>(request_data) {
        Ok(mut data) => {
            data.extra = event_data.clone();
            data.managed_settings_enabled = managed_settings_enabled;
            data
        }
        Err(_) => PermissionRequestData {
            kind: None,
            tool_call_id: None,
            managed_approval_required,
            managed_settings_enabled,
            extra: event_data.clone(),
        },
    }
}

/// Build the full `session.permissions.handlePendingPermissionRequest`
/// params for a permission result.
///
/// `decisionContext` is a sibling of `result` and is only present when the
/// handler attributed the decision — omitting it preserves legacy behavior.
///
/// Returns `None` when the SDK must not send a response.
fn permission_response_params(
    session_id: &SessionId,
    request_id: &RequestId,
    result: &PermissionResult,
) -> Option<Value> {
    let (decision, decision_context) = match result {
        PermissionResult::Decision { decision, context } => (decision, context.clone()),
        PermissionResult::NoResult => return None,
    };
    let mut params = serde_json::to_value(PermissionDecisionRequest {
        decision_context,
        request_id: request_id.clone(),
        result: decision.clone(),
    })
    .expect("serializing permission response should succeed");
    params["sessionId"] =
        serde_json::to_value(session_id).expect("serializing session ID should succeed");
    Some(params)
}

async fn register_mcp_auth_interest(client: &Client, session_id: &SessionId) -> Result<(), Error> {
    let mut params = serde_json::to_value(RegisterEventInterestParams {
        event_type: "mcp.oauth_required".to_string(),
    })?;
    params["sessionId"] = Value::String(session_id.to_string());
    client
        .call(rpc_methods::SESSION_EVENTLOG_REGISTERINTEREST, Some(params))
        .await?;
    Ok(())
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
        tool_references: None,
    })
}

fn is_autopilot_continuation_idle(event: &SessionEvent) -> bool {
    event
        .typed_data::<SessionIdleData>()
        .is_some_and(|data| data.mode == Some(SessionMode::Autopilot))
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
    shutdown: &CancellationToken,
    external_tools_shutdown: &CancellationToken,
    pending_external_tools: &PendingExternalTools,
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
                    SessionEventType::SessionIdle if is_autopilot_continuation_idle(&event) => {}
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

    // Update the snapshot caches BEFORE broadcasting so subscribers that
    // call `Session::capabilities()` / `Session::open_canvases()` in
    // response to the event observe the new state.
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
    if event_type == SessionEventType::SessionCanvasClosed {
        match serde_json::from_value::<SessionCanvasClosedData>(notification.event.data.clone()) {
            Ok(closed) => {
                if closed.instance_id.is_empty() {
                    warn!("failed to deserialize session.canvas.closed payload");
                } else {
                    remove_open_canvas_snapshot(&mut open_canvases.write(), &closed.instance_id);
                }
            }
            Err(e) => warn!(error = %e, "failed to deserialize session.canvas.closed payload"),
        }
    }

    // Fan out the event to runtime subscribers (`Session::subscribe`). `send`
    // only errors when there are no receivers, which is the normal case
    // before any consumer subscribes.
    let _ = event_tx.send(event.clone());

    tracing::debug!(
        elapsed_ms = dispatch_start.elapsed().as_millis(),
        session_id = %session_id,
        event_type = %notification.event.event_type,
        "Session::handle_notification dispatch"
    );

    // Notification-based permission/tool/elicitation requests require a
    // separate RPC callback. Spawn concurrently since the CLI doesn't block.
    match event_type {
        SessionEventType::ExternalToolCompleted => {
            if let Some(request_id) = extract_request_id(&notification.event.data)
                && let Some(token) = pending_external_tools.lock().remove(&request_id)
            {
                token.cancel();
            }
        }
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
            let shutdown = shutdown.clone();
            let data = permission_request_data(
                &notification.event.data,
                handlers.managed_settings_enabled,
            );
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
                    let Some(params) = permission_response_params(&sid, &request_id, &result)
                    else {
                        // Handler returned Deferred / NoResult — it will
                        // call handlePendingPermissionRequest itself (or
                        // leave the request unanswered).
                        return;
                    };
                    let rpc_start = Instant::now();
                    let method =
                        rpc_methods::SESSION_PERMISSIONS_HANDLEPENDINGPERMISSIONREQUEST;
                    tokio::select! {
                        biased;
                        response = client.call(method, Some(params)) => {
                            match response {
                                Ok(_) => tracing::debug!(
                                    elapsed_ms = rpc_start.elapsed().as_millis(),
                                    session_id = %sid,
                                    request_id = %request_id,
                                    method,
                                    "Session::handle_notification response sent successfully"
                                ),
                                Err(error) => warn!(
                                    error = %error,
                                    session_id = %sid,
                                    request_id = %request_id,
                                    method,
                                    "failed to deliver permission decision back to the runtime"
                                ),
                            }
                        }
                        _ = shutdown.cancelled() => {
                            warn!(
                                elapsed_ms = rpc_start.elapsed().as_millis(),
                                session_id = %sid,
                                request_id = %request_id,
                                method,
                                delivery_outcome = "unknown",
                                "permission confirmation acknowledgement wait cancelled during session shutdown"
                            );
                        }
                    }
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
            let cancellation = Arc::new(external_tools_shutdown.child_token());
            {
                let mut pending = pending_external_tools.lock();
                if external_tools_shutdown.is_cancelled() || pending.contains_key(&request_id) {
                    return;
                }
                pending.insert(request_id.clone(), cancellation.clone());
            }
            let client = client.clone();
            let sid = session_id.clone();
            let pending_external_tools = pending_external_tools.clone();
            let guard_request_id = request_id.clone();
            let guard_cancellation = cancellation.clone();
            let span = tracing::error_span!(
                "external_tool_handler",
                session_id = %sid,
                request_id = %request_id
            );
            tokio::spawn(
                async move {
                    let guard = PendingExternalToolGuard {
                        request_id: guard_request_id,
                        token: guard_cancellation,
                        pending: pending_external_tools,
                    };
                    if cancellation.is_cancelled() {
                        return;
                    }
                    // `tool_name.is_empty()` would have produced a `None`
                    // lookup in `handlers.tools` and short-circuited at the
                    // outer guard above, so only the tool_call_id check is
                    // reachable here.
                    if data.tool_call_id.is_empty() {
                        if !guard.claim() {
                            return;
                        }
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
                    // The built-in tool-search tool receives a snapshot of the
                    // session's currently initialized tools so an override can
                    // filter the live catalog without issuing its own RPC. Fetch
                    // it only for that tool to avoid a round-trip on every tool
                    // call; a failed fetch leaves the snapshot `None` rather than
                    // failing the tool.
                    let available_tools = if tool_name == TOOL_SEARCH_TOOL_NAME {
                        let metadata_result = tokio::select! {
                            biased;
                            _ = cancellation.cancelled() => return,
                            result = client.call(
                                rpc_methods::SESSION_TOOLS_GETCURRENTMETADATA,
                                Some(serde_json::json!({ "sessionId": sid })),
                            ) => result,
                        };
                        match metadata_result {
                            Ok(value) => {
                                serde_json::from_value::<ToolsGetCurrentMetadataResult>(value)
                                    .ok()
                                    .and_then(|result| result.tools)
                            }
                            Err(_) => None,
                        }
                    } else {
                        None
                    };
                    let invocation = ToolInvocation {
                        session_id: sid.clone(),
                        tool_call_id: data.tool_call_id,
                        tool_name: data.tool_name,
                        arguments: data
                            .arguments
                            .unwrap_or(Value::Object(serde_json::Map::new())),
                        available_tools,
                        traceparent: data.traceparent,
                        tracestate: data.tracestate,
                    };
                    let handler_start = Instant::now();
                    let tool_result = tokio::select! {
                        biased;
                        _ = cancellation.cancelled() => return,
                        result = tool_handler.call(invocation) => match result {
                            Ok(r) => r,
                            Err(e) => tool_failure_result(e.to_string()),
                        },
                    };
                    tracing::debug!(
                        elapsed_ms = handler_start.elapsed().as_millis(),
                        session_id = %sid,
                        request_id = %request_id,
                        tool_call_id = %tool_call_id,
                        tool_name = %tool_name,
                        "ToolHandler::call dispatch"
                    );
                    if !guard.claim() {
                        return;
                    }
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
        SessionEventType::McpOauthRequired => {
            let Some(request_id) = extract_request_id(&notification.event.data) else {
                return;
            };
            let Some(mcp_auth_handler) = handlers.mcp_auth.clone() else {
                warn!(
                    session_id = %session_id,
                    request_id = %request_id,
                    "received MCP OAuth request without a registered MCP auth handler"
                );
                return;
            };
            let data: McpOauthRequiredData =
                match serde_json::from_value(notification.event.data.clone()) {
                    Ok(d) => d,
                    Err(e) => {
                        warn!(error = %e, "failed to deserialize MCP OAuth request");
                        return;
                    }
                };
            let request = McpAuthRequest {
                request_id: request_id.clone(),
                server_name: data.server_name,
                server_url: data.server_url,
                reason: data.reason,
                www_authenticate_params: data.www_authenticate_params,
                resource_metadata: data.resource_metadata,
                static_client_config: data.static_client_config,
            };
            let client = client.clone();
            let sid = session_id.clone();
            let span = tracing::error_span!(
                "mcp_auth_request_handler",
                session_id = %sid,
                request_id = %request_id
            );
            tokio::spawn(
                async move {
                    let cancel = McpAuthResult::Cancelled;
                    let handler_task = tokio::spawn({
                        let sid = sid.clone();
                        let request_id = request_id.clone();
                        let span = tracing::error_span!(
                            "mcp_auth_callback",
                            session_id = %sid,
                            request_id = %request_id
                        );
                        async move {
                            let handler_start = Instant::now();
                            let response = mcp_auth_handler
                                .handle(sid.clone(), request_id.clone(), request)
                                .await;
                            tracing::debug!(
                                elapsed_ms = handler_start.elapsed().as_millis(),
                                session_id = %sid,
                                request_id = %request_id,
                                "McpAuthHandler::handle dispatch"
                            );
                            response
                        }
                        .instrument(span)
                    });
                    let result = match handler_task.await {
                        Ok(result) => result,
                        Err(_) => cancel,
                    };
                    let rpc_start = Instant::now();
                    let _ = client
                        .call(
                            "session.mcp.oauth.handlePendingRequest",
                            Some(serde_json::json!({
                                "sessionId": sid,
                                "requestId": request_id,
                                "result": result.into_wire(),
                            })),
                        )
                        .await;
                    tracing::debug!(
                        elapsed_ms = rpc_start.elapsed().as_millis(),
                        "Session::handle_notification MCP auth response sent"
                    );
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
    bearer_token_providers: &'a HashMap<String, Arc<dyn BearerTokenProvider>>,
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
    let bearer_token_providers = ctx.bearer_token_providers;

    if request.method.starts_with("sessionFs.") {
        crate::session_fs_dispatch::dispatch(client, session_fs_provider, request).await;
        return;
    }

    if request.method.starts_with("canvas.") {
        crate::canvas_dispatch::dispatch(client, canvas_handler, request).await;
        return;
    }

    if request.method == crate::generated::api_types::rpc_methods::PROVIDERTOKEN_GETTOKEN {
        crate::provider_token_dispatch::dispatch(client, bearer_token_providers, request).await;
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

    use super::{
        build_mode_post_create_patch, has_managed_settings, is_autopilot_continuation_idle,
        permission_request_data, permission_response_params,
    };
    use crate::handler::PermissionResult;
    use crate::types::{
        PermissionDecisionContext, PermissionDecisionOutcome, PermissionDecisionSource,
        PermissionDecisionSurface, RequestId, SessionEvent, SessionId,
    };

    #[test]
    fn identifies_only_autopilot_continuation_idles() {
        let mut event = SessionEvent {
            id: "event-1".to_string(),
            timestamp: "2026-01-01T00:00:00Z".to_string(),
            parent_id: None,
            ephemeral: None,
            agent_id: None,
            debug_cli_received_at_ms: None,
            debug_ws_forwarded_at_ms: None,
            event_type: "session.idle".to_string(),
            data: json!({ "mode": "autopilot" }),
        };

        assert!(is_autopilot_continuation_idle(&event));

        event.data = json!({ "mode": "interactive" });
        assert!(!is_autopilot_continuation_idle(&event));

        event.data = json!({});
        assert!(!is_autopilot_continuation_idle(&event));
    }

    #[test]
    fn empty_mode_post_patch_sets_empty_included_builtin_skills() {
        let patch =
            build_mode_post_create_patch(crate::ClientMode::Empty, None, None, None, None, None)
                .expect("empty mode always sends a patch");
        assert_eq!(
            patch.included_builtin_skills,
            Some(Vec::new()),
            "empty mode must fail closed with an empty includedBuiltinSkills list"
        );
        assert_eq!(patch.installed_plugins.as_ref().map(|p| p.len()), Some(0));
        // Serializes as an explicit empty array (not omitted).
        let value = serde_json::to_value(&patch).expect("serialize patch");
        assert_eq!(value["includedBuiltinSkills"], serde_json::json!([]));
    }

    #[test]
    fn empty_mode_post_patch_preserves_explicit_builtin_skill_allowlist() {
        let patch = build_mode_post_create_patch(
            crate::ClientMode::Empty,
            Some(false),
            Some(false),
            Some(true),
            Some(true),
            Some(vec!["code-review".to_string()]),
        )
        .expect("empty mode always sends a patch");
        assert_eq!(
            patch.included_builtin_skills,
            Some(vec!["code-review".to_string()])
        );
    }

    #[test]
    fn copilot_cli_mode_does_not_inject_included_builtin_skills() {
        // No fields set -> no patch at all.
        assert!(
            build_mode_post_create_patch(
                crate::ClientMode::CopilotCli,
                None,
                None,
                None,
                None,
                None
            )
            .is_none()
        );
        // A field set -> patch sent, but skills field stays absent.
        let patch = build_mode_post_create_patch(
            crate::ClientMode::CopilotCli,
            Some(true),
            None,
            None,
            None,
            None,
        )
        .expect("a set field triggers a patch");
        assert_eq!(patch.included_builtin_skills, None);
        assert!(patch.installed_plugins.is_none());
        let value = serde_json::to_value(&patch).expect("serialize patch");
        assert!(value.get("includedBuiltinSkills").is_none());

        let patch = build_mode_post_create_patch(
            crate::ClientMode::CopilotCli,
            None,
            None,
            None,
            None,
            Some(vec!["code-review".to_string()]),
        )
        .expect("an explicit allowlist triggers a patch");
        assert_eq!(
            patch.included_builtin_skills,
            Some(vec!["code-review".to_string()])
        );
    }

    #[test]
    fn direct_injection_enables_managed_safeguards() {
        let settings = crate::types::ManagedSettings::default();
        assert!(has_managed_settings(None, Some(&settings)));
        assert!(!has_managed_settings(None, None));
    }

    fn attribution_context() -> PermissionDecisionContext {
        PermissionDecisionContext {
            outcome: PermissionDecisionOutcome::AutoApproved,
            response_capability: None,
            source: PermissionDecisionSource::AssistedApproval,
            surface: PermissionDecisionSurface::CopilotApp,
        }
    }

    #[test]
    fn response_params_omit_decision_context_without_attribution() {
        for (result, expected) in [
            (
                PermissionResult::approve_once(),
                json!({ "kind": "approve-once" }),
            ),
            (PermissionResult::reject(None), json!({ "kind": "reject" })),
            (
                PermissionResult::reject(Some("bad".to_string())),
                json!({ "kind": "reject", "feedback": "bad" }),
            ),
            (
                PermissionResult::user_not_available(),
                json!({ "kind": "user-not-available" }),
            ),
        ] {
            let params = permission_response_params(
                &SessionId::from("session-1"),
                &RequestId::from("permission-1"),
                &result,
            )
            .unwrap();
            assert_eq!(
                params,
                json!({
                    "sessionId": "session-1",
                    "requestId": "permission-1",
                    "result": expected,
                })
            );
        }
    }

    #[test]
    fn response_params_forward_decision_context_alongside_result() {
        let params = permission_response_params(
            &SessionId::from("session-1"),
            &RequestId::from("permission-1"),
            &PermissionResult::approve_once().with_context(attribution_context()),
        )
        .unwrap();
        assert_eq!(
            params,
            json!({
                "sessionId": "session-1",
                "requestId": "permission-1",
                "result": { "kind": "approve-once" },
                "decisionContext": {
                    "outcome": "auto_approved",
                    "source": "assisted_approval",
                    "surface": "copilot_app",
                },
            })
        );
        // The context is a sibling of `result`, never nested inside it.
        assert!(params["result"].get("decisionContext").is_none());
    }

    #[test]
    fn response_params_suppressed_for_no_result() {
        assert!(
            permission_response_params(
                &SessionId::from("session-1"),
                &RequestId::from("permission-1"),
                &PermissionResult::NoResult,
            )
            .is_none()
        );
    }

    #[test]
    fn with_context_is_a_no_op_on_no_result() {
        let result = PermissionResult::no_result().with_context(attribution_context());
        assert!(matches!(result, PermissionResult::NoResult));
    }

    #[test]
    fn with_context_replaces_rather_than_nests() {
        let result = PermissionResult::approve_once()
            .with_context(attribution_context())
            .with_context(PermissionDecisionContext {
                outcome: PermissionDecisionOutcome::PromptedUser,
                response_capability: None,
                source: PermissionDecisionSource::HumanResponse,
                surface: PermissionDecisionSurface::Sdk,
            });
        let params = permission_response_params(
            &SessionId::from("session-1"),
            &RequestId::from("permission-1"),
            &result,
        )
        .unwrap();
        assert_eq!(
            params["decisionContext"],
            json!({
                "outcome": "prompted_user",
                "source": "human_response",
                "surface": "sdk",
            })
        );
    }

    #[test]
    fn permission_request_data_reads_nested_managed_approval_metadata() {
        let data = permission_request_data(
            &json!({
                "requestId": "permission-1",
                "permissionRequest": {
                    "kind": "read",
                    "managedApprovalRequired": true,
                    "path": "/workspace/file.txt"
                }
            }),
            false,
        );

        assert_eq!(data.managed_approval_required, Some(true));
        assert_eq!(
            data.extra["permissionRequest"]["path"],
            "/workspace/file.txt"
        );
    }

    #[test]
    fn permission_request_data_preserves_managed_flag_when_other_fields_are_malformed() {
        let data = permission_request_data(
            &json!({
                "requestId": "permission-1",
                "permissionRequest": {
                    "kind": "read",
                    "managedApprovalRequired": true,
                    "toolCallId": 42
                }
            }),
            false,
        );

        assert_eq!(data.managed_approval_required, Some(true));
        assert_eq!(data.extra["requestId"], "permission-1");
    }

    #[test]
    fn permission_request_data_fails_closed_for_malformed_managed_flag() {
        let data = permission_request_data(
            &json!({
                "requestId": "permission-1",
                "permissionRequest": {
                    "kind": "read",
                    "managedApprovalRequired": "yes",
                    "path": "/workspace/file.txt"
                }
            }),
            false,
        );

        assert_eq!(data.managed_approval_required, Some(true));
    }

    #[test]
    fn permission_request_data_preserves_valid_false_managed_flag() {
        let data = permission_request_data(
            &json!({
                "requestId": "permission-1",
                "permissionRequest": {
                    "kind": "read",
                    "managedApprovalRequired": false,
                    "path": "/workspace/file.txt"
                }
            }),
            false,
        );

        assert_eq!(data.managed_approval_required, Some(false));
    }
}
