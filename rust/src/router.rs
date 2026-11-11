use std::collections::{HashMap, HashSet};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::{Arc, Weak};

use indexmap::{IndexMap, IndexSet};
use parking_lot::Mutex;
use serde::Deserialize;
use serde_json::Value;
use tokio::sync::mpsc;
use tracing::{debug, warn};

use crate::jsonrpc::{JsonRpcError, JsonRpcRequest, JsonRpcResponse, error_codes};
use crate::types::{SessionEventNotification, SessionId};

/// Identity of one specific registration of a session ID.
///
/// Session IDs are not unique over time: a caller can retry a cancelled
/// startup with the same pinned ID, and the retry replaces the previous
/// registration. Removal is therefore compare-and-remove against this
/// token, so a stale owner (an aborted startup future or a superseded
/// [`Session`](crate::session::Session)) can never unregister the live
/// registration that replaced it.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub(crate) struct RegistrationToken(u64);

/// Per-session channels plus the identity of the registration that owns
/// them. Returned by [`SessionRouter::register`].
pub(crate) struct SessionRegistration {
    pub(crate) channels: SessionChannels,
    pub(crate) token: RegistrationToken,
    pub(crate) state: Arc<RegistrationState>,
}

/// Open channels alone do not prove startup completed or its owner is still
/// active. Displaced registrations share this state with their startup guard.
#[derive(Default)]
pub(crate) struct RegistrationState {
    ready: AtomicBool,
    cancelled: AtomicBool,
}

impl RegistrationState {
    pub(crate) fn mark_ready(&self) {
        self.ready.store(true, Ordering::Release);
    }

    pub(crate) fn cancel(&self) {
        self.cancelled.store(true, Ordering::Release);
    }

    fn is_active(&self) -> bool {
        !self.cancelled.load(Ordering::Acquire)
    }

    fn is_resident(&self) -> bool {
        self.ready.load(Ordering::Acquire) && self.is_active()
    }
}

/// What the client's notification dispatcher receives, in one FIFO.
pub(crate) enum DispatcherInput {
    /// A notification, in the order the read loop read it.
    Notification(crate::jsonrpc::JsonRpcNotification),
    /// A session registered after every notification queued ahead of this.
    /// Its events wait until the dispatcher reaches this point, so the
    /// held-request reconciliation sees every event read before registration.
    /// The token keeps a superseded registration's claim from releasing the
    /// current one early.
    Claim(SessionId, RegistrationToken),
}

/// One `session.event` on its way to its session's event loop.
pub(crate) struct RoutedEvent {
    pub(crate) notification: SessionEventNotification,
    /// The request was held before the session registered, together with
    /// the event that settled it. Observers still see it, but no handler is
    /// asked to answer a request another client already answered.
    pub(crate) settled: bool,
}

/// Session events that ask this client's handlers for an answer, each with
/// the event that settles it. Both carry the same `requestId`.
const SETTLED_BY: [(&str, &str); 5] = [
    ("permission.requested", "permission.completed"),
    ("external_tool.requested", "external_tool.completed"),
    ("elicitation.requested", "elicitation.completed"),
    ("mcp.oauth_required", "mcp.oauth_completed"),
    ("command.execute", "command.completed"),
];

fn request_id(notification: &SessionEventNotification) -> Option<&str> {
    notification.event.data.get("requestId")?.as_str()
}

/// The event that would settle `notification` and the request ID it would
/// carry, when `notification` asks a handler for an answer.
fn settling_event(notification: &SessionEventNotification) -> Option<(&'static str, &str)> {
    let event_type = notification.event.event_type.as_str();
    let (_, settled_by) = SETTLED_BY
        .iter()
        .find(|(requested, _)| *requested == event_type)?;
    Some((settled_by, request_id(notification)?))
}

/// Per-session channels created by the router during session registration.
pub(crate) struct SessionChannels {
    /// Filtered `session.event` notifications for this session.
    pub(crate) notifications: mpsc::UnboundedReceiver<RoutedEvent>,
    /// Filtered JSON-RPC requests (tool.call, userInput.request, etc.) for this session.
    pub(crate) requests: mpsc::UnboundedReceiver<JsonRpcRequest>,
}

struct SessionSenders {
    notifications: mpsc::UnboundedSender<RoutedEvent>,
    requests: mpsc::UnboundedSender<JsonRpcRequest>,
    token: RegistrationToken,
    state: Arc<RegistrationState>,
}

/// A registration displaced by [`SessionRouter::replace`].
///
/// Holding it keeps the displaced session's channels open, so a failed
/// resume can hand routing back with [`SessionRouter::restore_owned`].
/// Dropping it closes them, which ends that session's event loop.
pub(crate) struct ReplacedRegistration(SessionSenders);

impl ReplacedRegistration {
    pub(crate) fn is_resident(&self) -> bool {
        self.0.state.is_resident() && !self.0.requests.is_closed()
    }
}

pub(crate) struct SessionRpcCallbackGuard {
    callbacks: Arc<Mutex<HashMap<SessionId, usize>>>,
    session_id: SessionId,
}

impl Drop for SessionRpcCallbackGuard {
    fn drop(&mut self) {
        let mut callbacks = self.callbacks.lock();
        if let Some(count) = callbacks.get_mut(&self.session_id) {
            *count -= 1;
            if *count == 0 {
                callbacks.remove(&self.session_id);
            }
        }
    }
}

/// Most events held for session IDs that are not registered yet.
///
/// Every session this client creates or resumes registers before its RPC is
/// sent, or on the read loop as its response arrives. Events for an ID that
/// never registers (a session joined with the raw `sessions.connect` RPC, or
/// a misbehaving peer) are held only up to this bound; past it, every event
/// held for the ID seen first is dropped with a warning naming the requests
/// among them. A request dropped this way is never answered by this client,
/// even if its session registers later. Registered sessions never wait on it.
const MAX_UNCLAIMED_EVENTS: usize = 8192;

/// Most ended registrations remembered so their trailing events are dropped.
/// Past it, the oldest are forgotten and their late events count against
/// [`MAX_UNCLAIMED_EVENTS`] instead.
const MAX_RETIRED_SESSIONS: usize = 4096;

/// Routing table shared by the router and the client's notification dispatcher.
#[derive(Default)]
struct RouterState {
    sessions: HashMap<SessionId, SessionSenders>,
    /// Events for IDs not registered yet, in arrival order, keyed in the
    /// order the IDs were first seen. Registering the ID delivers them ahead
    /// of any later event.
    unclaimed: IndexMap<SessionId, Vec<SessionEventNotification>>,
    unclaimed_events: usize,
    /// IDs whose registration ended, oldest first. Their trailing events are
    /// dropped rather than held for a registration that will not come.
    retired: IndexSet<SessionId>,
    /// Registered IDs whose [`DispatcherInput::Claim`] the dispatcher has not
    /// reached yet, with their events so far in arrival order.
    claiming: HashMap<SessionId, Vec<SessionEventNotification>>,
}

impl RouterState {
    /// Install `senders` for `session_id`. Its held events, and any routed
    /// before `dispatcher` reaches the claim this queues, are delivered when
    /// it does. Without a running dispatcher they are delivered now.
    fn insert(
        &mut self,
        session_id: &SessionId,
        senders: SessionSenders,
        dispatcher: Option<&mpsc::UnboundedSender<DispatcherInput>>,
    ) -> Option<SessionSenders> {
        self.retired.shift_remove(session_id.as_str());
        let held = self
            .unclaimed
            .shift_remove(session_id.as_str())
            .unwrap_or_default();
        self.unclaimed_events -= held.len();
        self.claiming
            .entry(session_id.clone())
            .or_default()
            .splice(0..0, held);
        let token = senders.token;
        let replaced = self.sessions.insert(session_id.clone(), senders);
        let claimed = dispatcher.is_some_and(|tx| {
            tx.send(DispatcherInput::Claim(session_id.clone(), token))
                .is_ok()
        });
        if !claimed {
            self.claim(session_id, token);
        }
        replaced
    }

    /// Deliver the events held for `session_id` to its registration, if
    /// `token` still identifies it. A later registration's own claim follows.
    fn claim(&mut self, session_id: &SessionId, token: RegistrationToken) {
        let Some(senders) = self
            .sessions
            .get(session_id.as_str())
            .filter(|senders| senders.token == token)
        else {
            return;
        };
        let Some(events) = self.claiming.remove(session_id.as_str()) else {
            return;
        };
        // A request whose settling event was read before the session
        // registered was answered while this client was not listening; only
        // observers should see it.
        let read: HashSet<(String, String)> = events
            .iter()
            .filter_map(|event| {
                Some((
                    event.event.event_type.clone(),
                    request_id(event)?.to_owned(),
                ))
            })
            .collect();
        for notification in events {
            let settled = settling_event(&notification).is_some_and(|(event_type, id)| {
                read.contains(&(event_type.to_owned(), id.to_owned()))
            });
            if let Err(error) = senders.notifications.send(RoutedEvent {
                notification,
                settled,
            }) {
                log_dropped(&error.0.notification, "session event loop has stopped");
            }
        }
    }

    fn remove(&mut self, session_id: &SessionId) {
        for event in self
            .claiming
            .remove(session_id.as_str())
            .unwrap_or_default()
        {
            log_dropped(&event, "session is no longer registered");
        }
        if self.sessions.remove(session_id.as_str()).is_some() {
            self.retired.insert(session_id.clone());
            if self.retired.len() > MAX_RETIRED_SESSIONS {
                self.retired.shift_remove_index(0);
            }
        }
    }

    fn route(&mut self, notification: SessionEventNotification) {
        if let Some(events) = self.claiming.get_mut(notification.session_id.as_str()) {
            events.push(notification);
        } else if let Some(senders) = self.sessions.get(notification.session_id.as_str()) {
            if let Err(error) = senders.notifications.send(RoutedEvent {
                notification,
                settled: false,
            }) {
                log_dropped(&error.0.notification, "session event loop has stopped");
            }
        } else if self.retired.contains(notification.session_id.as_str()) {
            log_dropped(&notification, "session is no longer registered");
        } else {
            while self.unclaimed_events >= MAX_UNCLAIMED_EVENTS
                && let Some((session_id, events)) = self.unclaimed.shift_remove_index(0)
            {
                self.unclaimed_events -= events.len();
                warn!(
                    %session_id,
                    dropped = events.len(),
                    request_ids = ?events.iter().filter(|event| settling_event(event).is_some()).filter_map(request_id).collect::<Vec<_>>(),
                    "dropped events held for an unregistered session: too many events for unregistered sessions"
                );
            }
            self.unclaimed_events += 1;
            self.unclaimed
                .entry(notification.session_id.clone())
                .or_default()
                .push(notification);
        }
    }
}

/// Session events dropped here belong to no live session of this client.
/// Requests among them are not answered: on a shared session another
/// connected client may still own them.
fn log_dropped(notification: &SessionEventNotification, reason: &'static str) {
    let event_type = notification.event.event_type.as_str();
    if settling_event(notification).is_some() {
        warn!(
            session_id = %notification.session_id,
            event_type,
            request_id = ?request_id(notification),
            reason,
            "dropped session request event"
        );
    } else {
        debug!(
            session_id = %notification.session_id,
            event_type,
            reason,
            "dropped session event"
        );
    }
}

/// Delivers `session.event` notifications into the router's per-session
/// channels. Held by the client's notification dispatcher.
pub(crate) struct SessionEventSink(Arc<Mutex<RouterState>>);

impl SessionEventSink {
    /// Drop every route once the connection has closed and the dispatcher
    /// has delivered everything read before it, which ends each session's
    /// event loop.
    pub(crate) fn close(&self) {
        *self.0.lock() = RouterState::default();
    }

    /// The dispatcher reached a [`DispatcherInput::Claim`]: every event read
    /// before that session registered has been routed.
    pub(crate) fn claim(&self, session_id: &SessionId, token: RegistrationToken) {
        self.0.lock().claim(session_id, token);
    }

    /// Route one `session.event` notification's params to its session.
    ///
    /// Never waits: per-session channels are unbounded, so one slow session
    /// cannot hold up the others or the dispatcher.
    pub(crate) fn route(&self, mut params: Value) {
        // Leave null in the existing slot so serde still rejects missing
        // data, without rebuilding the owned payload.
        let data = params
            .get_mut("event")
            .and_then(|event| event.get_mut("data"))
            .map(Value::take);
        match SessionEventNotification::deserialize(&params) {
            Ok(mut notification) => {
                if let Some(data) = data {
                    notification.event.data = data;
                }
                self.0.lock().route(notification);
            }
            Err(error) => {
                warn!(
                    %error,
                    session_id = ?params.get("sessionId"),
                    event_type = ?params.get("event").and_then(|event| event.get("type")),
                    "failed to deserialize session event notification"
                );
            }
        }
    }
}

/// Routes notifications and requests by sessionId to per-session channels.
///
/// Internal to the SDK — consumers interact via `Client::register_session()`.
pub(crate) struct SessionRouter {
    state: Arc<Mutex<RouterState>>,
    /// Weak so it does not keep the dispatcher's channel open after the read
    /// loop ends.
    dispatcher: std::sync::OnceLock<mpsc::WeakUnboundedSender<DispatcherInput>>,
    /// Recovery and rejected-enrollment cleanup require exclusive initialization
    /// ownership; ordinary initializations share it until all setup settles.
    /// A true value fences the ID after failed cleanup on this connection.
    initializations: Mutex<HashMap<SessionId, Arc<tokio::sync::RwLock<bool>>>>,
    active_rpc_callbacks: Arc<Mutex<HashMap<SessionId, usize>>>,
    next_token: AtomicU64,
    started: Mutex<bool>,
}

impl SessionRouter {
    pub(crate) fn new() -> Self {
        Self {
            state: Arc::default(),
            dispatcher: std::sync::OnceLock::new(),
            initializations: Mutex::new(HashMap::new()),
            active_rpc_callbacks: Arc::default(),
            next_token: AtomicU64::new(0),
            started: Mutex::new(false),
        }
    }

    pub(crate) fn initialization(&self, session_id: &SessionId) -> Arc<tokio::sync::RwLock<bool>> {
        self.initializations
            .lock()
            .entry(session_id.clone())
            .or_default()
            .clone()
    }

    pub(crate) fn track_rpc_callback(&self, session_id: &SessionId) -> SessionRpcCallbackGuard {
        *self
            .active_rpc_callbacks
            .lock()
            .entry(session_id.clone())
            .or_default() += 1;
        SessionRpcCallbackGuard {
            callbacks: self.active_rpc_callbacks.clone(),
            session_id: session_id.clone(),
        }
    }

    pub(crate) fn has_active_rpc_callback(&self, session_id: &SessionId) -> bool {
        self.active_rpc_callbacks.lock().contains_key(session_id)
    }

    /// Queue registration claims behind the notifications already read.
    pub(crate) fn connect_dispatcher(&self, dispatcher: &mpsc::UnboundedSender<DispatcherInput>) {
        let _ = self.dispatcher.set(dispatcher.downgrade());
    }

    fn dispatcher(&self) -> Option<mpsc::UnboundedSender<DispatcherInput>> {
        self.dispatcher.get()?.upgrade()
    }

    pub(crate) fn event_sink(&self) -> SessionEventSink {
        SessionEventSink(self.state.clone())
    }

    /// Register a session to receive filtered events and requests.
    ///
    /// Replaces any existing registration for the same ID and returns a
    /// fresh [`RegistrationToken`] identifying this registration.
    pub(crate) fn register(&self, session_id: &SessionId) -> SessionRegistration {
        self.replace(session_id).0
    }

    /// Like [`register`](Self::register), but also returns the registration
    /// it displaced so the caller can [`restore_owned`](Self::restore_owned)
    /// it if the new one is abandoned.
    pub(crate) fn replace(
        &self,
        session_id: &SessionId,
    ) -> (SessionRegistration, Option<ReplacedRegistration>) {
        let (notif_tx, notif_rx) = mpsc::unbounded_channel();
        let (req_tx, req_rx) = mpsc::unbounded_channel();
        let token = RegistrationToken(self.next_token.fetch_add(1, Ordering::Relaxed));
        let registration_state = Arc::new(RegistrationState::default());
        let dispatcher = self.dispatcher();
        let replaced = self.state.lock().insert(
            session_id,
            SessionSenders {
                notifications: notif_tx,
                requests: req_tx,
                token,
                state: registration_state.clone(),
            },
            dispatcher.as_ref(),
        );
        let registration = SessionRegistration {
            channels: SessionChannels {
                notifications: notif_rx,
                requests: req_rx,
            },
            token,
            state: registration_state,
        };
        (registration, replaced.map(ReplacedRegistration))
    }

    /// Unregister a session, dropping its channels.
    ///
    /// Unconditional: removes whichever registration currently holds the
    /// ID. Only for client-wide teardown, where every session is going away
    /// regardless of owner. Owners of a specific registration must use
    /// [`unregister_owned`](Self::unregister_owned).
    pub(crate) fn unregister(&self, session_id: &SessionId) {
        self.state.lock().remove(session_id);
    }

    pub(crate) fn is_registered_owner(
        &self,
        session_id: &SessionId,
        token: RegistrationToken,
    ) -> bool {
        self.state
            .lock()
            .sessions
            .get(session_id.as_str())
            .is_some_and(|senders| senders.token == token)
    }

    /// Unregister a session only if it is still the registration identified
    /// by `token`.
    ///
    /// Returns `true` when the entry was removed. A `false` result means
    /// the registration had already been replaced by a newer one, which the
    /// caller does not own and must leave alone.
    pub(crate) fn unregister_owned(
        &self,
        session_id: &SessionId,
        token: RegistrationToken,
    ) -> bool {
        self.restore_owned(session_id, token, None)
    }

    /// Unregister the registration identified by `token`, handing the ID
    /// back to `replaced` when that session is still running.
    ///
    /// Returns `false`, leaving the router unchanged, when `token` no longer
    /// identifies the live registration.
    pub(crate) fn restore_owned(
        &self,
        session_id: &SessionId,
        token: RegistrationToken,
        replaced: Option<ReplacedRegistration>,
    ) -> bool {
        let mut state = self.state.lock();
        if !state
            .sessions
            .get(session_id.as_str())
            .is_some_and(|senders| senders.token == token)
        {
            return false;
        }
        match replaced
            .filter(|replaced| replaced.0.state.is_active() && !replaced.0.requests.is_closed())
        {
            Some(ReplacedRegistration(senders)) => {
                state.insert(session_id, senders, self.dispatcher().as_ref());
            }
            None => state.remove(session_id),
        }
        true
    }

    /// Snapshot every currently-registered session ID.
    ///
    /// Used by [`Client::stop`](crate::Client::stop) to iterate active
    /// sessions for cooperative shutdown without holding the router lock
    /// across `.await`.
    pub(crate) fn session_ids(&self) -> Vec<SessionId> {
        self.state.lock().sessions.keys().cloned().collect()
    }

    /// Count the currently-registered sessions without exposing their IDs.
    #[cfg(any(test, feature = "test-support"))]
    pub(crate) fn session_count(&self) -> usize {
        self.state.lock().sessions.len()
    }

    /// Drop all registered session channels.
    ///
    /// Used by [`Client::force_stop`](crate::Client::force_stop) to release
    /// per-session state without waiting for graceful unregistration.
    pub(crate) fn clear(&self) {
        *self.state.lock() = RouterState::default();
    }

    /// Start the request routing task if not already running.
    ///
    /// Takes the request channel from the client, which buffers requests
    /// until then. Notifications are routed from client construction by the
    /// client's notification dispatcher through [`Self::event_sink`].
    pub(crate) fn ensure_started(&self, client: &Arc<crate::ClientInner>) {
        let mut started = self.started.lock();
        if *started {
            return;
        }
        *started = true;
        let weak_client = Arc::downgrade(client);
        let extension_launch_provider = client.extension_launch_provider.clone();
        let github_token_registry = client.github_token_registry.clone();
        let installation_confirmation = client.installation_confirmation.clone();
        let entra_token = client.entra_token.clone();

        // Request routing task (if request_rx is available)
        if let Some(mut rx) = client.request_rx.lock().take() {
            let state = self.state.clone();
            let connection_closed = client.rpc.connection_closed_token();
            tokio::spawn(async move {
                loop {
                    let request = tokio::select! {
                        biased;
                        _ = connection_closed.cancelled() => break,
                        request = rx.recv() => {
                            let Some(request) = request else { break };
                            request
                        }
                    };
                    if request.method == crate::installation_confirmation::CONFIRM_METHOD {
                        installation_confirmation.dispatch(request);
                        continue;
                    }
                    if request.method == crate::entra::ACQUIRE_METHOD {
                        entra_token.dispatch(request);
                        continue;
                    }
                    if request.method == crate::extension_launch_provider::RESOLVE_METHOD {
                        // The host's resolver may take arbitrarily long, so it must
                        // not hold up routing of later requests.
                        let provider = extension_launch_provider.clone();
                        tokio::spawn(async move { provider.dispatch(request).await });
                        continue;
                    }
                    if request.method == "gitHubToken.getToken" {
                        github_token_registry.dispatch(request);
                        continue;
                    }
                    // Client-global `llmInference.*` requests carry no routable
                    // session and are handled by the inference dispatcher.
                    if request.method.starts_with("llmInference.") {
                        let dispatcher = weak_client
                            .upgrade()
                            .and_then(|client| client.llm_inference.get().cloned());
                        if let Some(dispatcher) = dispatcher {
                            dispatcher.dispatch(request).await;
                        } else {
                            warn!(
                                method = %request.method,
                                "llmInference request with no provider registered"
                            );
                        }
                        continue;
                    }

                    let session_id = request
                        .params
                        .as_ref()
                        .and_then(|p| p.get("sessionId"))
                        .and_then(|v| v.as_str())
                        .map(str::to_owned);
                    let Some(session_id) = session_id else {
                        warn!(method = %request.method, "request missing sessionId");
                        if !send_error(
                            &weak_client,
                            request.id,
                            error_codes::INVALID_PARAMS,
                            "missing required field: sessionId".to_string(),
                        ) {
                            break;
                        }
                        continue;
                    };

                    let sender = state
                        .lock()
                        .sessions
                        .get(session_id.as_str())
                        .map(|s| s.requests.clone());
                    let unrouted = match sender {
                        Some(sender) => sender.send(request).err().map(|error| error.0),
                        None => Some(request),
                    };
                    // Every request owes the peer a response. Answer unroutable
                    // ones (for example, after a `Session` was dropped without
                    // detaching) so the runtime does not wait for its timeout.
                    if let Some(request) = unrouted {
                        warn!(
                            session_id = %session_id,
                            method = %request.method,
                            "request for unregistered session"
                        );
                        let message = if request.method
                            == crate::generated::api_types::rpc_methods::MEMORYSTORE_STORE
                        {
                            "Memory store owner is unavailable".to_string()
                        } else {
                            format!("Session not found: {session_id}")
                        };
                        if !send_error(
                            &weak_client,
                            request.id,
                            error_codes::INTERNAL_ERROR,
                            message,
                        ) {
                            break;
                        }
                    }
                }
            });
        }
    }
}

fn send_error(
    client: &Weak<crate::ClientInner>,
    request_id: u64,
    code: i32,
    message: String,
) -> bool {
    let Some(inner) = client.upgrade() else {
        return false;
    };
    // Retire any cancellation registered for a request that never reaches a handler.
    drop(inner.rpc.cancellable_requests.claim(request_id));
    if let Err(error) = inner.rpc.enqueue_response(&JsonRpcResponse {
        jsonrpc: "2.0".to_string(),
        id: request_id,
        result: None,
        error: Some(JsonRpcError {
            code: code.into(),
            message,
            data: None,
        }),
    }) {
        warn!(%error, "failed to enqueue router rejection; closing connection");
        crate::Client::from_inner(inner).force_stop();
        return false;
    }
    true
}

#[cfg(test)]
mod tests;
