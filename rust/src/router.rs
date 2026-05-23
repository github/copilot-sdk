use std::collections::{HashMap, VecDeque};
use std::sync::Arc;

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

use crate::jsonrpc::{
    JsonRpcError, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse, WriterHandle, error_codes,
};
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
    items: VecDeque<PendingItem>,
}

enum PendingItem {
    Notification(SessionEventNotification),
    Request(JsonRpcRequest),
}

#[derive(Default)]
struct SessionRouterState {
    sessions: HashMap<SessionId, SessionSenders>,
    pending: HashMap<SessionId, PendingSessionMessages>,
    pending_registration_count: usize,
    /// Outbound writer used to synthesize JSON-RPC error responses when
    /// the pending buffer overflows. `None` in tests that exercise the
    /// router in isolation; production construction goes through
    /// [`SessionRouter::new`] which threads a real handle in.
    writer: Option<WriterHandle>,
}

impl SessionRouterState {
    fn register(&mut self, session_id: &SessionId, senders: SessionSenders) {
        if let Some(pending) = self.pending.remove(session_id.as_str()) {
            for item in pending.items {
                match item {
                    PendingItem::Notification(n) => {
                        let _ = senders.notifications.send(n);
                    }
                    PendingItem::Request(r) => {
                        let _ = senders.requests.send(r);
                    }
                }
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
        push_pending(
            self.pending.entry(session_id.clone()).or_default(),
            &session_id,
            PendingItem::Notification(notification),
            self.writer.as_ref(),
        );
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
        push_pending(
            self.pending.entry(session_id.clone()).or_default(),
            &session_id,
            PendingItem::Request(request),
            self.writer.as_ref(),
        );
    }
}

/// Push an item into a session's pending buffer, evicting the oldest entry
/// (regardless of type) when the per-session limit is reached. A single
/// FIFO across notifications and requests preserves cross-type arrival
/// order, which matters because some session.event notifications must be
/// observed by the consumer before later inbound requests (e.g. an
/// "approval requested" event arriving before the matching tool call).
///
/// When the evicted entry is a request, we synthesize a JSON-RPC error
/// response back to the runtime so it doesn't block waiting for a reply
/// that will never arrive. Notifications are fire-and-forget, so dropping
/// one only emits a warning.
fn push_pending(
    buf: &mut PendingSessionMessages,
    session_id: &SessionId,
    item: PendingItem,
    writer: Option<&WriterHandle>,
) {
    if buf.items.len() >= PENDING_SESSION_BUFFER_LIMIT {
        match buf.items.pop_front() {
            Some(PendingItem::Request(dropped)) => {
                warn!(
                    session_id = %session_id,
                    method = %dropped.method,
                    request_id = dropped.id,
                    limit = PENDING_SESSION_BUFFER_LIMIT,
                    "pending session buffer full; dropping oldest request and responding with error"
                );
                if let Some(writer) = writer {
                    writer.send_fire_and_forget(&pending_overflow_response(dropped.id));
                }
            }
            Some(PendingItem::Notification(_)) => {
                warn!(
                    session_id = %session_id,
                    limit = PENDING_SESSION_BUFFER_LIMIT,
                    "pending session buffer full; dropping oldest notification"
                );
            }
            None => {}
        }
    }
    buf.items.push_back(item);
}

/// Build a JSON-RPC error response for a request the SDK had to discard
/// because the pending-session buffer overflowed before the runtime
/// returned `session.create`.
fn pending_overflow_response(id: u64) -> JsonRpcResponse {
    JsonRpcResponse {
        jsonrpc: "2.0".to_string(),
        id,
        result: None,
        error: Some(JsonRpcError {
            code: error_codes::INTERNAL_ERROR,
            message: "request dropped: pending session buffer overflow before session.create \
                      response"
                .to_string(),
            data: None,
        }),
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
    /// Test-only constructor. Production callers must use
    /// [`SessionRouter::with_writer`] so dropped requests get error
    /// responses. Tests that don't exercise the writer can use this.
    #[cfg(test)]
    pub(crate) fn new() -> Self {
        Self {
            state: Arc::new(Mutex::new(SessionRouterState::default())),
        }
    }

    /// Construct a router with a handle onto the JSON-RPC outbound writer,
    /// used to synthesize error responses when pending-buffer overflow
    /// forces us to discard an inbound request.
    pub(crate) fn with_writer(writer: WriterHandle) -> Self {
        Self {
            state: Arc::new(Mutex::new(SessionRouterState {
                writer: Some(writer),
                ..SessionRouterState::default()
            })),
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
    use serde_json::json;

    use super::*;
    use crate::jsonrpc::JsonRpcRequest;

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
        assert_eq!(pending.items.len(), PENDING_SESSION_BUFFER_LIMIT);
    }

    #[test]
    fn pending_buffer_preserves_cross_type_arrival_order() {
        // Interleave a request between notifications and make sure the
        // request lands in its arrival slot relative to surrounding events
        // on the consumer side, not batched after every notification.
        let router = SessionRouter::new();
        let guard = router.begin_pending_session_routing();

        {
            let mut state = router.state.lock();
            state.route_notification("remote", make_notification("remote", "evt-0"));
            state.route_request(make_request(1, "remote", "userInput.request"));
            state.route_notification("remote", make_notification("remote", "evt-1"));
        }

        let sid = SessionId::from("remote");
        let mut channels = router.register(&sid);
        drop(guard);

        // First notification flushes first, then the request lands in the
        // request channel before the trailing notification is delivered.
        let n0 = channels.notifications.try_recv().expect("first notif");
        assert_eq!(n0.event.event_type, "evt-0");
        let r = channels.requests.try_recv().expect("request");
        assert_eq!(r.id, 1);
        let n1 = channels.notifications.try_recv().expect("trailing notif");
        assert_eq!(n1.event.event_type, "evt-1");
    }

    #[tokio::test]
    async fn pending_request_overflow_emits_jsonrpc_error_response() {
        use tokio::io::AsyncReadExt;
        use tokio::sync::{broadcast, mpsc};

        use crate::jsonrpc::{JsonRpcClient, error_codes};

        // Stand up a real JsonRpcClient over a duplex pair so we can read
        // back the bytes the WriterHandle pushes onto the wire.
        let (server_read, client_write) = tokio::io::duplex(64 * 1024);
        let (_client_read, _server_write) = tokio::io::duplex(64);
        let (notif_tx, _) = broadcast::channel(16);
        let (req_tx, _req_rx) = mpsc::unbounded_channel();
        let rpc = JsonRpcClient::new(client_write, _client_read, notif_tx, req_tx);
        let router = SessionRouter::with_writer(rpc.writer_handle());
        let _guard = router.begin_pending_session_routing();

        // First buffered request is the one we expect to evict.
        let evicted_id = 7777;
        router
            .state
            .lock()
            .route_request(make_request(evicted_id, "remote", "userInput.request"));
        for i in 0..PENDING_SESSION_BUFFER_LIMIT {
            router.state.lock().route_request(make_request(
                i as u64,
                "remote",
                "userInput.request",
            ));
        }

        // Drain the wire and find an error response with the evicted id.
        let mut buf = Vec::with_capacity(4096);
        let mut reader = server_read;
        tokio::time::timeout(std::time::Duration::from_secs(1), async {
            while !buf
                .windows(2)
                .any(|w| w == b"\r\n" && buf.windows(4).any(|w4| w4 == b"\r\n\r\n"))
            {
                let mut chunk = [0u8; 256];
                let n = reader.read(&mut chunk).await.expect("read frame");
                if n == 0 {
                    break;
                }
                buf.extend_from_slice(&chunk[..n]);
            }
        })
        .await
        .expect("frame within timeout");

        let body_start = buf
            .windows(4)
            .position(|w| w == b"\r\n\r\n")
            .expect("header terminator")
            + 4;
        let body = std::str::from_utf8(&buf[body_start..]).expect("utf-8 body");
        let response: JsonRpcResponse =
            serde_json::from_str(body.trim_end()).expect("parse response");
        assert_eq!(response.id, evicted_id);
        let err = response.error.expect("error payload");
        assert_eq!(err.code, error_codes::INTERNAL_ERROR);
        assert!(err.message.contains("pending session buffer overflow"));
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
