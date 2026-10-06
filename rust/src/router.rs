use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Weak};

use parking_lot::Mutex;
use tokio::sync::{broadcast, mpsc};
use tracing::warn;

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

/// A registration displaced by [`SessionRouter::replace`].
///
/// Holding it keeps the displaced session's channels open, so a failed
/// resume can hand routing back with [`SessionRouter::restore_owned`].
/// Dropping it closes them, which ends that session's event loop.
pub(crate) struct ReplacedRegistration(SessionSenders);

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
        let replaced = self.sessions.lock().insert(
            session_id.clone(),
            SessionSenders {
                notifications: notif_tx,
                requests: req_tx,
                token,
            },
        );
        let registration = SessionRegistration {
            channels: SessionChannels {
                notifications: notif_rx,
                requests: req_rx,
            },
            token,
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
        self.sessions.lock().remove(session_id.as_str());
    }

    pub(crate) fn is_registered_owner(
        &self,
        session_id: &SessionId,
        token: RegistrationToken,
    ) -> bool {
        self.sessions
            .lock()
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
        let mut sessions = self.sessions.lock();
        if !sessions
            .get(session_id.as_str())
            .is_some_and(|senders| senders.token == token)
        {
            return false;
        }
        match replaced.filter(|replaced| !replaced.0.requests.is_closed()) {
            Some(ReplacedRegistration(senders)) => {
                sessions.insert(session_id.clone(), senders);
            }
            None => {
                sessions.remove(session_id.as_str());
            }
        }
        true
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
    pub(crate) fn ensure_started(&self, client: &Arc<crate::ClientInner>) {
        let mut started = self.started.lock();
        if *started {
            return;
        }
        *started = true;
        let weak_client = Arc::downgrade(client);
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
                        let Some(session_id) = params.get("sessionId").and_then(|v| v.as_str())
                        else {
                            continue;
                        };

                        let sender = {
                            let guard = sessions.lock();
                            guard
                                .get_key_value(session_id)
                                .map(|(id, s)| (id.clone(), s.notifications.clone()))
                        };
                        if let Some((session_id, sender)) = sender {
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
                                    let _ = sender.send(event_notification);
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
                        .and_then(|v| v.as_str())
                        .map(str::to_owned);
                    let Some(session_id) = session_id else {
                        warn!(method = %request.method, "request missing sessionId");
                        send_error(
                            &weak_client,
                            request.id,
                            error_codes::INVALID_PARAMS,
                            "missing required field: sessionId".to_string(),
                        )
                        .await;
                        continue;
                    };

                    let sender = sessions
                        .lock()
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
                        send_error(
                            &weak_client,
                            request.id,
                            error_codes::INTERNAL_ERROR,
                            format!("Session not found: {session_id}"),
                        )
                        .await;
                    }
                }
            });
        }
    }
}

async fn send_error(
    client: &Weak<crate::ClientInner>,
    request_id: u64,
    code: i32,
    message: String,
) {
    let Some(inner) = client.upgrade() else {
        return;
    };
    // Retire any cancellation registered for a request that never reaches a handler.
    drop(inner.rpc.cancellable_requests.claim(request_id));
    let _ = crate::Client::from_inner(inner)
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: None,
            error: Some(JsonRpcError {
                code,
                message,
                data: None,
            }),
        })
        .await;
}

#[cfg(test)]
mod tests;
