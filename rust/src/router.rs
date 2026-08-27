use std::collections::HashMap;
use std::sync::Arc;

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

use crate::jsonrpc::{JsonRpcNotification, JsonRpcRequest};
use crate::types::{
    SessionEventNotification, SessionId, SessionLifecycleEvent, SessionLifecycleEventType,
};

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
    is_watch: bool,
}

/// Routes notifications and requests by sessionId to per-session channels.
///
/// Internal to the SDK — consumers interact via `Client::register_session()`.
#[derive(Clone)]
pub(crate) struct SessionRouter {
    sessions: Arc<Mutex<HashMap<SessionId, SessionSenders>>>,
    started: Arc<Mutex<bool>>,
}

impl SessionRouter {
    pub(crate) fn new() -> Self {
        Self {
            sessions: Arc::new(Mutex::new(HashMap::new())),
            started: Arc::new(Mutex::new(false)),
        }
    }

    /// Register a session to receive filtered events and requests.
    pub(crate) fn register(&self, session_id: &SessionId) -> SessionChannels {
        self.register_with_kind(session_id, false)
    }

    /// Register a passive shared-session watch.
    pub(crate) fn register_watch(&self, session_id: &SessionId) -> SessionChannels {
        self.register_with_kind(session_id, true)
    }

    fn register_with_kind(&self, session_id: &SessionId, is_watch: bool) -> SessionChannels {
        let (notif_tx, notif_rx) = mpsc::unbounded_channel();
        let (req_tx, req_rx) = mpsc::unbounded_channel();
        self.sessions.lock().insert(
            session_id.clone(),
            SessionSenders {
                notifications: notif_tx,
                requests: req_tx,
                is_watch,
            },
        );
        SessionChannels {
            notifications: notif_rx,
            requests: req_rx,
        }
    }

    /// Unregister a session, dropping its channels.
    pub(crate) fn unregister(&self, session_id: &SessionId) {
        self.sessions.lock().remove(session_id.as_str());
    }

    /// Snapshot registered session IDs with their cleanup classification.
    pub(crate) fn session_entries(&self) -> Vec<(SessionId, bool)> {
        self.sessions
            .lock()
            .iter()
            .map(|(session_id, senders)| (session_id.clone(), senders.is_watch))
            .collect()
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
        github_token_registry: Arc<crate::github_token::GitHubTokenRegistry>,
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
                        if notification.method == "session.lifecycle" {
                            let Some(ref params) = notification.params else {
                                continue;
                            };
                            match serde_json::from_value::<SessionLifecycleEvent>(params.clone()) {
                                Ok(event)
                                    if event.event_type
                                        == SessionLifecycleEventType::Disconnected =>
                                {
                                    let mut guard = sessions.lock();
                                    if guard
                                        .get(&event.session_id)
                                        .is_some_and(|senders| senders.is_watch)
                                    {
                                        guard.remove(&event.session_id);
                                    }
                                }
                                Ok(_) => {}
                                Err(e) => {
                                    warn!(
                                        error = %e,
                                        "failed to deserialize session.lifecycle notification"
                                    );
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
                    if request.method == "gitHubToken.getToken" {
                        github_token_registry.dispatch(request).await;
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
