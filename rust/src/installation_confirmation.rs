//! Connection-global human confirmation for experimental installation operations.

use std::collections::HashMap;
use std::collections::hash_map::Entry;
use std::panic::AssertUnwindSafe;
use std::sync::{Arc, OnceLock, Weak};

use async_trait::async_trait;
use futures_util::FutureExt;
use parking_lot::{Mutex, RwLock};
use tokio_util::sync::CancellationToken;
use tracing::warn;

pub use crate::rpc::{
    InstallationConfirmationRequest, InstallationConfirmationResponse, InstallationDecision,
    InstallationReview, McpInstallationReview,
};
use crate::{
    Client, ClientInner, JsonRpcError, JsonRpcRequest, JsonRpcResponse, Result, error_codes,
};

pub(crate) const CONFIRM_METHOD: &str = "installations.confirm";
const REQUEST_CANCELLED: i32 = -32800;

/// The independent request and connection lifetimes of one confirmation.
///
/// These signals retire a human review. They do not cancel outbound installation
/// or OAuth RPCs. The runtime remains authoritative for the operation's expiry.
#[derive(Clone)]
pub struct InstallationConfirmationContext {
    request_cancelled: CancellationToken,
    connection_closed: CancellationToken,
}

impl InstallationConfirmationContext {
    /// Cancelled when the runtime retires this request, including at its deadline.
    ///
    /// Cancelling the returned child token cannot cancel another review.
    pub fn request_cancelled(&self) -> CancellationToken {
        self.request_cancelled.child_token()
    }

    /// Cancelled when the original connection closes, independently of request cancellation.
    pub fn connection_closed(&self) -> CancellationToken {
        self.connection_closed.child_token()
    }
}

/// Collects a fresh human decision for the complete review on its original connection.
///
/// Match the request's `operation_id` and `policy_session_id` against the exact
/// action registered by the host before displaying it. Never infer the originating
/// action from a current session, server name or global pending slot. Refuse
/// unknown actions or incomplete reviews.
///
/// The SDK echoes the original challenge and fingerprint; the handler returns
/// only the decision. Cancellation or closure drops the handler future, so any
/// separately spawned UI work must also observe the supplied context.
///
/// This experimental callback does not imply that the connected runtime supports
/// installation, removal or activation.
#[async_trait]
pub trait InstallationConfirmationHandler: Send + Sync + 'static {
    /// Review this operation and return an explicit decision, or an error to refuse it.
    async fn confirm(
        &self,
        request: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ) -> Result<InstallationDecision>;
}

/// Registered synchronously by the transport before forwarding each confirmation.
/// This preserves request/cancellation ordering across the router's separate queues.
#[derive(Default)]
pub(crate) struct ConfirmationRequests {
    pending: Mutex<HashMap<u64, Arc<CancellationToken>>>,
}

impl ConfirmationRequests {
    pub(crate) fn register(&self, id: u64) -> bool {
        let mut pending = self.pending.lock();
        match pending.entry(id) {
            Entry::Vacant(entry) => {
                entry.insert(Arc::new(CancellationToken::new()));
                true
            }
            Entry::Occupied(_) => false,
        }
    }

    pub(crate) fn cancel(&self, id: u64) {
        if let Some(token) = self.pending.lock().get(&id) {
            token.cancel();
        }
    }

    pub(crate) fn clear(&self) {
        self.pending.lock().clear();
    }

    fn claim(self: &Arc<Self>, id: u64) -> Option<PendingConfirmation> {
        let cancellation = self.pending.lock().get(&id)?.clone();
        Some(PendingConfirmation {
            requests: self.clone(),
            id,
            cancellation,
        })
    }
}

struct PendingConfirmation {
    requests: Arc<ConfirmationRequests>,
    id: u64,
    cancellation: Arc<CancellationToken>,
}

impl Drop for PendingConfirmation {
    fn drop(&mut self) {
        let mut pending = self.requests.pending.lock();
        if pending
            .get(&self.id)
            .is_some_and(|token| Arc::ptr_eq(token, &self.cancellation))
        {
            pending.remove(&self.id);
        }
    }
}

pub(crate) struct InstallationConfirmationDispatcher {
    handler: RwLock<Option<Arc<dyn InstallationConfirmationHandler>>>,
    client: OnceLock<Weak<ClientInner>>,
}

impl InstallationConfirmationDispatcher {
    pub(crate) fn new() -> Self {
        Self {
            handler: RwLock::new(None),
            client: OnceLock::new(),
        }
    }

    pub(crate) fn set_client(&self, client: Weak<ClientInner>) {
        let _ = self.client.set(client);
    }

    pub(crate) fn set_handler(&self, handler: Option<Arc<dyn InstallationConfirmationHandler>>) {
        *self.handler.write() = handler;
    }

    pub(crate) fn clear(&self) {
        self.handler.write().take();
    }

    pub(crate) fn dispatch(self: &Arc<Self>, request: JsonRpcRequest) {
        let Some(client) = self.client.get().and_then(Weak::upgrade) else {
            return;
        };
        let Some(pending) = client.rpc.confirmation_requests.claim(request.id) else {
            warn!("confirmation request retired before dispatch");
            return;
        };
        let context = InstallationConfirmationContext {
            request_cancelled: pending.cancellation.as_ref().clone(),
            connection_closed: client.rpc.connection_closed_token(),
        };
        let handler = self.handler.read().clone();
        let dispatcher = self.clone();
        tokio::spawn(async move {
            let outcome = tokio::select! {
                biased;
                _ = context.connection_closed.cancelled() => return,
                _ = context.request_cancelled.cancelled() => {
                    Err((REQUEST_CANCELLED, "Installation confirmation request cancelled"))
                }
                outcome = Self::handle(handler, request.params, context.clone()) => outcome,
            };
            if context.connection_closed.is_cancelled() {
                return;
            }
            let outcome = if context.request_cancelled.is_cancelled() {
                Err((
                    REQUEST_CANCELLED,
                    "Installation confirmation request cancelled",
                ))
            } else {
                outcome
            };
            dispatcher.respond(request.id, outcome).await;
            drop(pending);
        });
    }

    async fn handle(
        handler: Option<Arc<dyn InstallationConfirmationHandler>>,
        params: Option<serde_json::Value>,
        context: InstallationConfirmationContext,
    ) -> std::result::Result<InstallationConfirmationResponse, (i32, &'static str)> {
        let Some(handler) = handler else {
            return Err((
                error_codes::METHOD_NOT_FOUND,
                "No installations client-global handler registered",
            ));
        };
        let request: InstallationConfirmationRequest =
            serde_json::from_value(params.unwrap_or(serde_json::Value::Null)).map_err(|_| {
                (
                    error_codes::INVALID_PARAMS,
                    "Invalid installation confirmation review",
                )
            })?;
        let confirmation_id = request.confirmation_id.clone();
        let review_fingerprint = request.review_fingerprint.clone();
        let outcome = AssertUnwindSafe(handler.confirm(request, context))
            .catch_unwind()
            .await;
        let decision = match outcome {
            Ok(Ok(
                decision @ (InstallationDecision::Confirm
                | InstallationDecision::Decline
                | InstallationDecision::Cancel),
            )) => decision,
            Ok(Ok(InstallationDecision::Unknown)) => {
                return Err((
                    error_codes::INTERNAL_ERROR,
                    "Invalid installation confirmation decision",
                ));
            }
            Ok(Err(_)) => {
                return Err((
                    error_codes::INTERNAL_ERROR,
                    "Installation confirmation handler failed",
                ));
            }
            Err(_) => {
                return Err((
                    error_codes::INTERNAL_ERROR,
                    "Installation confirmation handler panicked",
                ));
            }
        };
        Ok(InstallationConfirmationResponse {
            confirmation_id,
            review_fingerprint,
            decision,
        })
    }

    async fn respond(
        &self,
        id: u64,
        outcome: std::result::Result<InstallationConfirmationResponse, (i32, &'static str)>,
    ) {
        let Some(client) = self.client.get().and_then(Weak::upgrade) else {
            return;
        };
        let (result, error) = match outcome {
            Ok(response) => match serde_json::to_value(response) {
                Ok(value) => (Some(value), None),
                Err(_) => {
                    warn!("failed to serialise installation confirmation response");
                    (
                        None,
                        Some(JsonRpcError {
                            code: error_codes::INTERNAL_ERROR,
                            message: "Installation confirmation serialisation failed".to_string(),
                            data: None,
                        }),
                    )
                }
            },
            Err((code, message)) => (
                None,
                Some(JsonRpcError {
                    code,
                    message: message.to_string(),
                    data: None,
                }),
            ),
        };
        if Client::from_inner(client)
            .send_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id,
                result,
                error,
            })
            .await
            .is_err()
        {
            warn!("failed to send installation confirmation response");
        }
    }
}
