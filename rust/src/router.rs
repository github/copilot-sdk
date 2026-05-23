use std::collections::{HashMap, VecDeque};
use std::sync::Arc;

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

use crate::jsonrpc::{JsonRpcNotification, JsonRpcRequest};
use crate::types::{SessionEventNotification, SessionId};

/// Upper bound on buffered notifications/requests per pending session id.
///
/// Holds traffic that arrives between `session.create` being sent and the
/// SDK learning the runtime-assigned session id from the response (cloud
/// path). Drop-oldest behavior is acceptable: cloud handshakes are short,
/// and 128 entries is well above realistic init/replay bursts.
const PENDING_SESSION_BUFFER_LIMIT: usize = 128;

/// Per-session channels created by the router during session registration.
pub(crate) struct SessionChannels {
    /// Filtered `session.event` notifications for this session.
    pub(crate) notifications: mpsc::UnboundedReceiver<SessionEventNotification>,
    /// Filtered JSON-RPC requests (tool.call, userInput.request, etc.) for this session.
    pub(crate) requests: mpsc::UnboundedReceiver<JsonRpcRequest>,
}

struct SessionSenders {
    notifications: mpsc::UnboundedSender<SessionEventNotification>,
    requests: mpsc::UnboundedSender<JsonRpcRequest>,
}

#[derive(Default)]
struct PendingSessionMessages {
    notifications: VecDeque<SessionEventNotification>,
    requests: VecDeque<JsonRpcRequest>,
}

#[derive(Default)]
struct SessionRouterState {
    sessions: HashMap<SessionId, SessionSenders>,
    pending: HashMap<SessionId, PendingSessionMessages>,
    pending_registration_count: usize,
}

impl SessionRouterState {
    fn register(&mut self, session_id: &SessionId, senders: SessionSenders) {
        if let Some(pending) = self.pending.remove(session_id.as_str()) {
            for notification in pending.notifications {
                let _ = senders.notifications.send(notification);
            }
            for request in pending.requests {
                let _ = senders.requests.send(request);
            }
        }
        self.sessions.insert(session_id.clone(), senders);
    }

    fn route_notification(&mut self, session_id: &str, notification: SessionEventNotification) {
        if let Some(sender) = self.sessions.get(session_id) {
            let _ = sender.notifications.send(notification);
            return;
        }
        if self.pending_registration_count == 0 {
            return;
        }

        let session_id = SessionId::from(session_id);
        let pending = self.pending.entry(session_id.clone()).or_default();
        if pending.notifications.len() >= PENDING_SESSION_BUFFER_LIMIT {
            pending.notifications.pop_front();
            warn!(
                session_id = %session_id,
                limit = PENDING_SESSION_BUFFER_LIMIT,
                "pending session notification buffer full; dropping oldest notification"
            );
        }
        pending.notifications.push_back(notification);
    }

    fn route_request(&mut self, request: JsonRpcRequest) {
        let Some(session_id) = request
            .params
            .as_ref()
            .and_then(|p| p.get("sessionId"))
            .and_then(|v| v.as_str())
        else {
            warn!(method = %request.method, "request missing sessionId");
            return;
        };
        if let Some(sender) = self.sessions.get(session_id) {
            let _ = sender.requests.send(request);
            return;
        }
        if self.pending_registration_count == 0 {
            warn!(
                session_id = session_id,
                method = %request.method,
                "request for unregistered session"
            );
            return;
        }

        let session_id = SessionId::from(session_id);
        let pending = self.pending.entry(session_id.clone()).or_default();
        if pending.requests.len() >= PENDING_SESSION_BUFFER_LIMIT {
            pending.requests.pop_front();
            warn!(
                session_id = %session_id,
                limit = PENDING_SESSION_BUFFER_LIMIT,
                "pending session request buffer full; dropping oldest request"
            );
        }
        pending.requests.push_back(request);
    }
}

/// Guard that keeps the router in "pending routing" mode for cloud
/// `session.create`: while any guard is alive, notifications/requests with
/// unknown session ids are buffered (up to [`PENDING_SESSION_BUFFER_LIMIT`])
/// instead of dropped. On `register`, buffered messages flush in arrival
/// order into the freshly-created per-session channels.
///
/// When the last guard drops, any still-pending buffers are cleared.
pub(crate) struct PendingSessionRouting {
    state: Arc<Mutex<SessionRouterState>>,
}

impl Drop for PendingSessionRouting {
    fn drop(&mut self) {
        let mut state = self.state.lock();
        state.pending_registration_count = state.pending_registration_count.saturating_sub(1);
        if state.pending_registration_count == 0 {
            state.pending.clear();
        }
    }
}

/// Routes notifications and requests by sessionId to per-session channels.
///
/// Internal to the SDK — consumers interact via `Client::register_session()`.
pub(crate) struct SessionRouter {
    state: Arc<Mutex<SessionRouterState>>,
}

impl SessionRouter {
    pub(crate) fn new() -> Self {
        Self {
            state: Arc::new(Mutex::new(SessionRouterState::default())),
        }
    }

    /// Register a session to receive filtered events and requests.
    pub(crate) fn register(&self, session_id: &SessionId) -> SessionChannels {
        let (notif_tx, notif_rx) = mpsc::unbounded_channel();
        let (req_tx, req_rx) = mpsc::unbounded_channel();
        self.state.lock().register(
            session_id,
            SessionSenders {
                notifications: notif_tx,
                requests: req_tx,
            },
        );
        SessionChannels {
            notifications: notif_rx,
            requests: req_rx,
        }
    }

    /// Enter pending-routing mode. While the returned guard is alive,
    /// notifications and requests addressed to session ids that are not
    /// yet registered are buffered instead of being dropped.
    pub(crate) fn begin_pending_session_routing(&self) -> PendingSessionRouting {
        self.state.lock().pending_registration_count += 1;
        PendingSessionRouting {
            state: self.state.clone(),
        }
    }

    /// Unregister a session, dropping its channels and any pending buffer.
    pub(crate) fn unregister(&self, session_id: &SessionId) {
        let mut state = self.state.lock();
        state.sessions.remove(session_id.as_str());
        state.pending.remove(session_id.as_str());
    }

    /// Snapshot every currently-registered session ID.
    ///
    /// Used by [`Client::stop`](crate::Client::stop) to iterate active
    /// sessions for cooperative shutdown without holding the router lock
    /// across `.await`.
    pub(crate) fn session_ids(&self) -> Vec<SessionId> {
        self.state.lock().sessions.keys().cloned().collect()
    }

    /// Drop all registered session channels and pending buffers.
    ///
    /// Used by [`Client::force_stop`](crate::Client::force_stop) to release
    /// per-session state without waiting for graceful unregistration.
    pub(crate) fn clear(&self) {
        let mut state = self.state.lock();
        state.sessions.clear();
        state.pending.clear();
    }

    /// Spawn the notification and request routing tasks.
    ///
    /// Called exactly once during [`Client::from_streams`]. Takes the
    /// notification broadcast and request channel from the Client. If
    /// `request_rx` is `None` (already taken by `take_request_rx()`), only
    /// notification routing is available.
    pub(crate) fn start(
        &self,
        notification_tx: &broadcast::Sender<JsonRpcNotification>,
        request_rx: &Mutex<Option<mpsc::UnboundedReceiver<JsonRpcRequest>>>,
    ) {
        // Notification routing task
        let state = self.state.clone();
        let mut notif_rx = notification_tx.subscribe();
        tokio::spawn(async move {
            loop {
                match notif_rx.recv().await {
                    Ok(notification) => {
                        if notification.method != "session.event" {
                            continue;
                        }
                        let Some(ref params) = notification.params else {
                            continue;
                        };
                        let Some(session_id) = params.get("sessionId").and_then(|v| v.as_str())
                        else {
                            continue;
                        };

                        match serde_json::from_value::<SessionEventNotification>(params.clone()) {
                            Ok(event_notification) => {
                                state
                                    .lock()
                                    .route_notification(session_id, event_notification);
                            }
                            Err(e) => {
                                warn!(
                                    error = %e,
                                    session_id = session_id,
                                    "failed to deserialize session event notification"
                                );
                            }
                        }
                    }
                    Err(broadcast::error::RecvError::Lagged(n)) => {
                        warn!(missed = n, "notification router lagged");
                    }
                    Err(broadcast::error::RecvError::Closed) => break,
                }
            }
        });

        // Request routing task (if request_rx is available)
        if let Some(mut rx) = request_rx.lock().take() {
            let state = self.state.clone();
            tokio::spawn(async move {
                while let Some(request) = rx.recv().await {
                    state.lock().route_request(request);
                }
            });
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jsonrpc::JsonRpcRequest;
    use serde_json::json;

    fn make_notification(session_id: &str, kind: &str) -> SessionEventNotification {
        let value = json!({
            "sessionId": session_id,
            "event": {
                "id": "evt-id",
                "timestamp": "1970-01-01T00:00:00Z",
                "parentId": null,
                "type": kind,
                "data": {},
            },
        });
        serde_json::from_value(value).expect("valid session event notification")
    }

    fn make_request(id: u64, session_id: &str, method: &str) -> JsonRpcRequest {
        JsonRpcRequest {
            jsonrpc: "2.0".to_string(),
            id,
            method: method.to_string(),
            params: Some(json!({ "sessionId": session_id })),
        }
    }

    #[test]
    fn pending_buffer_off_drops_unknown_session() {
        let router = SessionRouter::new();
        router
            .state
            .lock()
            .route_notification("ghost", make_notification("ghost", "session.start"));
        assert!(router.state.lock().pending.is_empty());
    }

    #[test]
    fn pending_buffer_on_buffers_and_flushes_in_order() {
        let router = SessionRouter::new();
        let guard = router.begin_pending_session_routing();

        for i in 0..3 {
            router
                .state
                .lock()
                .route_notification("remote", make_notification("remote", &format!("evt-{i}")));
        }
        for i in 0..2 {
            router
                .state
                .lock()
                .route_request(make_request(100 + i, "remote", "userInput.request"));
        }

        let sid = SessionId::from("remote");
        let mut channels = router.register(&sid);
        drop(guard);

        let mut got_notifications = 0;
        while channels.notifications.try_recv().is_ok() {
            got_notifications += 1;
        }
        assert_eq!(got_notifications, 3, "all buffered notifications flushed");

        let mut got_requests = 0;
        while channels.requests.try_recv().is_ok() {
            got_requests += 1;
        }
        assert_eq!(got_requests, 2, "all buffered requests flushed");
    }

    #[test]
    fn pending_buffer_drops_oldest_at_limit() {
        let router = SessionRouter::new();
        let _guard = router.begin_pending_session_routing();

        for i in 0..(PENDING_SESSION_BUFFER_LIMIT + 5) {
            router
                .state
                .lock()
                .route_notification("remote", make_notification("remote", &format!("evt-{i}")));
        }

        let state = router.state.lock();
        let pending = state.pending.get("remote").expect("pending bucket exists");
        assert_eq!(pending.notifications.len(), PENDING_SESSION_BUFFER_LIMIT);
    }

    #[test]
    fn last_guard_drop_clears_pending_buffers() {
        let router = SessionRouter::new();
        let g1 = router.begin_pending_session_routing();
        let g2 = router.begin_pending_session_routing();

        router
            .state
            .lock()
            .route_notification("a", make_notification("a", "evt"));
        router
            .state
            .lock()
            .route_notification("b", make_notification("b", "evt"));

        drop(g1);
        assert_eq!(router.state.lock().pending.len(), 2, "still buffering");
        drop(g2);
        assert!(
            router.state.lock().pending.is_empty(),
            "last guard drop clears pending"
        );
    }

    #[test]
    fn unregister_clears_pending_for_session() {
        let router = SessionRouter::new();
        let _guard = router.begin_pending_session_routing();
        router
            .state
            .lock()
            .route_notification("doomed", make_notification("doomed", "evt"));
        router
            .state
            .lock()
            .route_notification("kept", make_notification("kept", "evt"));

        router.unregister(&SessionId::from("doomed"));

        let state = router.state.lock();
        assert!(!state.pending.contains_key("doomed"));
        assert!(state.pending.contains_key("kept"));
    }
}
