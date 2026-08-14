use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

use crate::jsonrpc::{JsonRpcNotification, JsonRpcRequest};
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

    /// Drop all registered session channels.
    ///
    /// Used by [`Client::force_stop`](crate::Client::force_stop) to release
    /// per-session state without waiting for graceful unregistration.
    pub(crate) fn clear(&self) {
        self.sessions.lock().clear();
    }

    /// Start the router tasks if not already running.
    ///
    /// Takes the notification broadcast and request channel from the Client.
    /// If `request_rx` is `None` (already taken by `take_request_rx()`),
    /// only notification routing is available.
    pub(crate) fn ensure_started(
        &self,
        notification_tx: &broadcast::Sender<JsonRpcNotification>,
        request_rx: &Mutex<Option<mpsc::UnboundedReceiver<JsonRpcRequest>>>,
        llm_inference: Option<Arc<crate::copilot_request_handler::CopilotRequestDispatcher>>,
        github_telemetry: Option<crate::github_telemetry::GitHubTelemetryCallback>,
    ) {
        let mut started = self.started.lock();
        if *started {
            return;
        }
        *started = true;

        // Notification routing task
        let sessions = self.sessions.clone();
        let mut notif_rx = notification_tx.subscribe();
        tokio::spawn(async move {
            loop {
                match notif_rx.recv().await {
                    Ok(notification) => {
                        // Client-global `gitHubTelemetry.event` notifications carry
                        // no routable session and are surfaced to the consumer
                        // callback (if any) registered at client construction.
                        if notification.method == "gitHubTelemetry.event" {
                            if let Some(ref callback) = github_telemetry {
                                let Some(ref params) = notification.params else {
                                    continue;
                                };
                                match serde_json::from_value::<
                                    crate::github_telemetry::GitHubTelemetryNotification,
                                >(params.clone())
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
                        let Some(ref params) = notification.params else {
                            continue;
                        };
                        let Some(session_id) = params.get("sessionId").and_then(|v| v.as_str())
                        else {
                            continue;
                        };

                        let sender = {
                            let guard = sessions.lock();
                            guard.get(session_id).map(|s| s.notifications.clone())
                        };
                        if let Some(sender) = sender {
                            match serde_json::from_value::<SessionEventNotification>(params.clone())
                            {
                                Ok(event_notification) => {
                                    let _ = sender.send(event_notification);
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
        if let Some(mut rx) = request_rx.lock().take() {
            let sessions = self.sessions.clone();
            tokio::spawn(async move {
                while let Some(request) = rx.recv().await {
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

                    if let Some(sid) = session_id {
                        let sender = {
                            let guard = sessions.lock();
                            guard.get(sid).map(|s| s.requests.clone())
                        };
                        if let Some(sender) = sender {
                            let _ = sender.send(request);
                        } else {
                            warn!(
                                session_id = sid,
                                method = %request.method,
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
}
