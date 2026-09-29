use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

use crate::jsonrpc::JsonRpcRequest;
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
}

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
    token: RegistrationToken,
}

/// Routes notifications and requests by sessionId to per-session channels.
///
/// Internal to the SDK — consumers interact via `Client::register_session()`.
pub(crate) struct SessionRouter {
    sessions: Arc<Mutex<HashMap<SessionId, SessionSenders>>>,
    next_token: AtomicU64,
    started: Mutex<bool>,
}

impl SessionRouter {
    pub(crate) fn new() -> Self {
        Self {
            sessions: Arc::new(Mutex::new(HashMap::new())),
            next_token: AtomicU64::new(0),
            started: Mutex::new(false),
        }
    }

    /// Register a session to receive filtered events and requests.
    ///
    /// Replaces any existing registration for the same ID and returns a
    /// fresh [`RegistrationToken`] identifying this registration.
    pub(crate) fn register(&self, session_id: &SessionId) -> SessionRegistration {
        let (notif_tx, notif_rx) = mpsc::unbounded_channel();
        let (req_tx, req_rx) = mpsc::unbounded_channel();
        let token = RegistrationToken(self.next_token.fetch_add(1, Ordering::Relaxed));
        self.sessions.lock().insert(
            session_id.clone(),
            SessionSenders {
                notifications: notif_tx,
                requests: req_tx,
                token,
            },
        );
        SessionRegistration {
            channels: SessionChannels {
                notifications: notif_rx,
                requests: req_rx,
            },
            token,
        }
    }

    /// Unregister a session, dropping its channels.
    ///
    /// Unconditional: removes whichever registration currently holds the
    /// ID. Only for client-wide teardown, where every session is going away
    /// regardless of owner. Owners of a specific registration must use
    /// [`unregister_owned`](Self::unregister_owned).
    pub(crate) fn unregister(&self, session_id: &SessionId) {
        self.sessions.lock().remove(session_id.as_str());
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
        let mut sessions = self.sessions.lock();
        if sessions
            .get(session_id.as_str())
            .is_some_and(|senders| senders.token == token)
        {
            sessions.remove(session_id.as_str());
            true
        } else {
            false
        }
    }

    /// Snapshot every currently-registered session ID.
    ///
    /// Used by [`Client::stop`](crate::Client::stop) to iterate active
    /// sessions for cooperative shutdown without holding the router lock
    /// across `.await`.
    pub(crate) fn session_ids(&self) -> Vec<SessionId> {
        self.sessions.lock().keys().cloned().collect()
    }

    /// Count the currently-registered sessions without exposing their IDs.
    #[cfg(any(test, feature = "test-support"))]
    pub(crate) fn session_count(&self) -> usize {
        self.sessions.lock().len()
    }

    /// Drop all registered session channels.
    ///
    /// Used by [`Client::force_stop`](crate::Client::force_stop) to release
    /// per-session state without waiting for graceful unregistration.
    pub(crate) fn clear(&self) {
        self.sessions.lock().clear();
    }

    /// Start the router tasks if not already running.
    ///
    /// Takes the notification broadcast and request channel from the client.
    /// If its request receiver was already taken by `take_request_rx()`,
    /// only notification routing is available.
    pub(crate) fn ensure_started(&self, client: &crate::ClientInner) {
        let mut started = self.started.lock();
        if *started {
            return;
        }
        *started = true;
        let extension_launch_provider = client.extension_launch_provider.clone();
        let llm_inference = client.llm_inference.get().cloned();
        let github_telemetry = client.on_github_telemetry.clone();
        let github_token_registry = client.github_token_registry.clone();
        let installation_confirmation = client.installation_confirmation.clone();

        // Notification routing task
        let sessions = self.sessions.clone();
        let mut notif_rx = client.notification_tx.subscribe();
        tokio::spawn(async move {
            loop {
                match notif_rx.recv().await {
                    Ok(notification) => {
                        // Client-global `gitHubTelemetry.event` notifications carry
                        // no routable session and are surfaced to the consumer
                        // callback (if any) registered at client construction.
                        if notification.method == "gitHubTelemetry.event" {
                            if let Some(ref callback) = github_telemetry {
                                let Some(params) = notification.params else {
                                    continue;
                                };
                                match serde_json::from_value::<
                                    crate::github_telemetry::GitHubTelemetryNotification,
                                >(params)
                                {
                                    Ok(telemetry) => {
                                        if std::panic::catch_unwind(std::panic::AssertUnwindSafe(
                                            || callback(telemetry),
                                        ))
                                        .is_err()
                                        {
                                            warn!(
                                                "gitHubTelemetry.event callback panicked; \
                                             continuing notification routing"
                                            );
                                        }
                                    }
                                    Err(e) => {
                                        warn!(
                                            error = %e,
                                            "failed to deserialize gitHubTelemetry.event notification"
                                        );
                                    }
                                }
                            }
                            continue;
                        }
                        if notification.method != "session.event" {
                            continue;
                        }
                        let Some(mut params) = notification.params else {
                            continue;
                        };
                        let Some(session_id) = params
                            .get("sessionId")
                            .and_then(|v| v.as_str())
                            .map(str::to_owned)
                        else {
                            continue;
                        };

                        if sessions.lock().contains_key(session_id.as_str()) {
                            // Leave null in the existing slot so serde still rejects
                            // missing data, without rebuilding the owned payload.
                            let data = params
                                .get_mut("event")
                                .and_then(|event| event.get_mut("data"))
                                .map(serde_json::Value::take);
                            match serde_json::from_value::<SessionEventNotification>(params) {
                                Ok(mut event_notification) => {
                                    if let Some(data) = data {
                                        event_notification.event.data = data;
                                    }
                                    route_notification(&sessions, &session_id, event_notification);
                                }
                                Err(e) => {
                                    warn!(
                                        error = %e,
                                        session_id = %session_id,
                                        "failed to deserialize session event notification"
                                    );
                                }
                            }
                        }
                        // Unknown session IDs are silently dropped — the session
                        // may have been unregistered between dispatch and delivery.
                    }
                    Err(broadcast::error::RecvError::Lagged(n)) => {
                        warn!(missed = n, "notification router lagged");
                    }
                    Err(broadcast::error::RecvError::Closed) => break,
                }
            }
        });

        // Request routing task (if request_rx is available)
        if let Some(mut rx) = client.request_rx.lock().take() {
            let sessions = self.sessions.clone();
            tokio::spawn(async move {
                while let Some(request) = rx.recv().await {
                    if request.method == crate::installation_confirmation::CONFIRM_METHOD {
                        installation_confirmation.dispatch(request);
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
                        if let Some(dispatcher) = &llm_inference {
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
                        .and_then(|v| v.as_str());

                    if let Some(sid) = session_id.map(str::to_owned) {
                        let method = request.method.clone();
                        if !route_request(&sessions, &sid, request) {
                            warn!(
                                session_id = %sid,
                                method = %method,
                                "request for unregistered session"
                            );
                        }
                    } else {
                        warn!(
                            method = %request.method,
                            "request missing sessionId"
                        );
                    }
                }
            });
        }
    }
}

// Keep lookup and enqueue in one critical section so a replacement registration
// cannot receive a message through a sender captured from its predecessor.
fn route_notification(
    sessions: &Mutex<HashMap<SessionId, SessionSenders>>,
    session_id: &str,
    notification: SessionEventNotification,
) {
    let guard = sessions.lock();
    if let Some(senders) = guard.get(session_id) {
        let _ = senders.notifications.send(notification);
    }
}

fn route_request(
    sessions: &Mutex<HashMap<SessionId, SessionSenders>>,
    session_id: &str,
    request: JsonRpcRequest,
) -> bool {
    let guard = sessions.lock();
    let Some(senders) = guard.get(session_id) else {
        return false;
    };
    let _ = senders.requests.send(request);
    true
}

#[cfg(test)]
mod tests {
    use super::*;

    fn session_id() -> SessionId {
        SessionId::new("router-ownership")
    }

    #[test]
    fn each_registration_gets_a_distinct_token() {
        let router = SessionRouter::new();
        let first = router.register(&session_id());
        let second = router.register(&session_id());
        assert_ne!(first.token, second.token);
    }

    #[test]
    fn unregister_owned_removes_only_the_matching_registration() {
        let router = SessionRouter::new();
        let stale = router.register(&session_id());
        let live = router.register(&session_id());

        // The stale owner must not evict the registration that replaced it.
        assert!(!router.unregister_owned(&session_id(), stale.token));
        assert_eq!(router.session_ids(), vec![session_id()]);

        assert!(router.unregister_owned(&session_id(), live.token));
        assert!(router.session_ids().is_empty());

        // Removing twice is a no-op rather than evicting a future tenant.
        assert!(!router.unregister_owned(&session_id(), live.token));
    }

    #[test]
    fn routed_requests_and_notifications_reach_the_current_registration() {
        let router = SessionRouter::new();
        let mut stale = router.register(&session_id());
        let mut current = router.register(&session_id());

        assert!(route_request(
            &router.sessions,
            session_id().as_str(),
            JsonRpcRequest::new(1, "tool.call", None),
        ));
        assert_eq!(
            current.channels.requests.try_recv().unwrap().method,
            "tool.call"
        );

        route_notification(
            &router.sessions,
            session_id().as_str(),
            SessionEventNotification {
                session_id: session_id(),
                event: crate::types::SessionEvent {
                    id: "event-1".to_string(),
                    timestamp: "2026-01-01T00:00:00Z".to_string(),
                    parent_id: None,
                    ephemeral: None,
                    agent_id: None,
                    debug_cli_received_at_ms: None,
                    debug_ws_forwarded_at_ms: None,
                    event_type: "session.idle".to_string(),
                    data: serde_json::json!({}),
                },
            },
        );
        assert_eq!(
            current.channels.notifications.try_recv().unwrap().event.id,
            "event-1"
        );

        assert!(stale.channels.requests.try_recv().is_err());
        assert!(stale.channels.notifications.try_recv().is_err());
    }
}
