use std::collections::HashMap;
use std::sync::Arc;

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

use crate::jsonrpc::{JsonRpcNotification, JsonRpcRequest};
use crate::types::SessionEventNotification;

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

/// Routes notifications and requests by sessionId to per-session channels.
///
/// Internal to the SDK — consumers interact via `Client::register_session()`.
pub(crate) struct SessionRouter {
    sessions: Arc<Mutex<HashMap<String, SessionSenders>>>,
    started: Mutex<bool>,
}

impl SessionRouter {
    pub(crate) fn new() -> Self {
        Self {
            sessions: Arc::new(Mutex::new(HashMap::new())),
            started: Mutex::new(false),
        }
    }

    /// Register a session to receive filtered events and requests.
    pub(crate) fn register(&self, session_id: &str) -> SessionChannels {
        let (notif_tx, notif_rx) = mpsc::unbounded_channel();
        let (req_tx, req_rx) = mpsc::unbounded_channel();
        self.sessions.lock().insert(
            session_id.to_string(),
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

    /// Unregister a session, dropping its channels.
    pub(crate) fn unregister(&self, session_id: &str) {
        self.sessions.lock().remove(session_id);
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
