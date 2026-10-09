// Copyright (c) Microsoft Corporation. All rights reserved.

//! Experimental provider-process desktop notification host callbacks.

use std::panic::AssertUnwindSafe;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, OnceLock, Weak};
use std::time::Duration;

use async_trait::async_trait;
use futures_util::FutureExt;
use parking_lot::RwLock;
use serde_json::Value;
use tokio_util::sync::CancellationToken;
use tracing::warn;

pub use crate::rpc::{
    NotificationActivateResult, NotificationCapabilitiesResult, NotificationClickAction,
    NotificationClickKind, NotificationExtensionOrigin, NotificationFocusTarget,
    NotificationHostClickAction, NotificationHostContext, NotificationHostRegistrationResult,
    NotificationHostRequest, NotificationHostShowParams, NotificationHostShowResult,
    NotificationHostUnregistrationResult, NotificationOsPermissionState, NotificationPermission,
    NotificationPermissionResult, NotificationPermissionState, NotificationPlatform,
    NotificationShowParams, NotificationShowResult, NotificationSound, NotificationSounds,
};
use crate::{
    Client, ClientInner, Error, ErrorKind, JsonRpcError, JsonRpcRequest, JsonRpcResponse, Result,
    SessionId, error_codes,
};

pub(crate) const GET_CAPABILITIES_METHOD: &str = "notifications.getCapabilities";
pub(crate) const REQUEST_PERMISSION_METHOD: &str = "notifications.requestPermission";
pub(crate) const SHOW_METHOD: &str = "notifications.show";

pub(crate) fn is_callback(method: &str) -> bool {
    matches!(
        method,
        GET_CAPABILITIES_METHOD | REQUEST_PERMISSION_METHOD | SHOW_METHOD
    )
}

/// Lifetime and activation access for one host callback.
///
/// Retaining this context does not keep its client alive. Host work spawned
/// outside the handler must observe [`Self::cancellation`] and check it before
/// OS handoff. Notification content must not be logged.
#[derive(Clone)]
pub struct NotificationContext {
    cancellation: CancellationToken,
    client: Weak<ClientInner>,
    session_id: SessionId,
}

impl NotificationContext {
    /// Cancels pending work when the request, connection, or host registration is retired.
    ///
    /// Cancelling this child token does not cancel the originating request.
    /// Successful completion does not cancel it. It is not proof that a saved
    /// focus receipt is still valid; [`Self::activate_canvas`] checks that with
    /// the runtime. Combine it with application-owned generation cancellation
    /// when spawning work outside the handler.
    pub fn cancellation(&self) -> CancellationToken {
        self.cancellation.child_token()
    }

    /// Consume a live, one-shot canvas-focus activation on the original session.
    ///
    /// URLs do not use this method: supporting hosts authenticate URL activation
    /// metadata before handing it to the OS. Canvas activation remains subject
    /// to the runtime's attachment, ownership, and liveness checks.
    pub async fn activate_canvas(
        &self,
        activation_id: impl Into<String>,
    ) -> Result<NotificationActivateResult> {
        let inner = self.client.upgrade().ok_or_else(|| {
            Error::with_message(
                ErrorKind::InvalidConfig,
                "Notification host connection is closed",
            )
        })?;
        if inner.rpc.connection_closed_token().is_cancelled() {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "Notification host connection is closed",
            ));
        }
        let value = Client::from_inner(inner)
            .call(
                "session.notifications.activate",
                Some(serde_json::json!({
                    "sessionId": self.session_id,
                    "activationId": activation_id.into(),
                })),
            )
            .await?;
        serde_json::from_value(value).map_err(|_| {
            Error::with_message(ErrorKind::Json, "Invalid notification activation response")
        })
    }
}

/// Native notification services supplied by a trusted application host.
///
/// Configure through [`crate::ClientOptions::with_notification_handler`].
/// Registration does not grant extension or OS permission. Resolve the
/// runtime-authenticated attachment against a trusted installation mapping;
/// caller-provided names or module paths alone are not consent identity.
#[async_trait]
pub trait NotificationHandler: Send + Sync + 'static {
    /// Report support and current permission without prompting or delivering.
    async fn get_capabilities(
        &self,
        request: NotificationHostRequest,
        context: NotificationContext,
    ) -> Result<NotificationCapabilitiesResult>;

    /// Request extension and OS permission following an explicit user action.
    async fn request_permission(
        &self,
        request: NotificationHostRequest,
        context: NotificationContext,
    ) -> Result<NotificationPermissionResult>;

    /// Hand one notification to the OS, checking cancellation before handoff.
    ///
    /// Accepted means OS handoff, not confirmed display. Do not retry delivery
    /// after an ambiguous outcome or silently substitute unsupported sounds.
    async fn show(
        &self,
        request: NotificationHostShowParams,
        context: NotificationContext,
    ) -> Result<NotificationHostShowResult>;
}

pub(crate) struct NotificationDispatcher {
    handler: RwLock<Option<Arc<dyn NotificationHandler>>>,
    client: OnceLock<Weak<ClientInner>>,
    shutdown: CancellationToken,
    registered: AtomicBool,
    registration: RwLock<Option<NotificationHostRegistrationResult>>,
}

impl NotificationDispatcher {
    pub(crate) fn new() -> Self {
        Self {
            handler: RwLock::new(None),
            client: OnceLock::new(),
            shutdown: CancellationToken::new(),
            registered: AtomicBool::new(false),
            registration: RwLock::new(None),
        }
    }

    pub(crate) fn set_client(&self, client: Weak<ClientInner>) {
        let _ = self.client.set(client);
    }

    #[cfg(any(feature = "runtime", test))]
    pub(crate) fn set_handler(&self, handler: Option<Arc<dyn NotificationHandler>>) {
        *self.handler.write() = handler;
    }

    pub(crate) fn clear(&self) -> bool {
        self.shutdown.cancel();
        self.handler.write().take();
        self.registered.swap(false, Ordering::AcqRel)
    }

    pub(crate) fn dispatch(self: &Arc<Self>, request: JsonRpcRequest) {
        let Some(client) = self.client.get().and_then(Weak::upgrade) else {
            return;
        };
        let Some(pending) = client.rpc.cancellable_requests.claim(request.id) else {
            warn!("notification request retired before dispatch");
            return;
        };
        let request_cancelled = pending.cancellation().clone();
        let closed = client.rpc.connection_closed_token();
        let cancellation = closed.child_token();
        let handler = if self.registered.load(Ordering::Acquire) {
            self.handler.read().clone()
        } else {
            None
        };
        let dispatcher = self.clone();
        let owner = Arc::downgrade(&client);
        tokio::spawn(async move {
            let outcome = tokio::select! {
                biased;
                _ = closed.cancelled() => {
                    cancellation.cancel();
                    return;
                }
                _ = dispatcher.shutdown.cancelled() => {
                    cancellation.cancel();
                    Err((error_codes::REQUEST_CANCELLED, "Notification host stopped"))
                }
                _ = request_cancelled.cancelled() => {
                    cancellation.cancel();
                    Err((error_codes::REQUEST_CANCELLED, "Notification request cancelled"))
                }
                outcome = AssertUnwindSafe(Self::handle(
                    handler,
                    &request.method,
                    request.params.unwrap_or(Value::Null),
                    owner,
                    cancellation.clone(),
                )).catch_unwind() => {
                    outcome.unwrap_or(Err((error_codes::INTERNAL_ERROR, "Notification handler failed")))
                }
            };
            if closed.is_cancelled() {
                cancellation.cancel();
                return;
            }
            let outcome = if request_cancelled.is_cancelled() || dispatcher.shutdown.is_cancelled()
            {
                cancellation.cancel();
                Err((
                    error_codes::REQUEST_CANCELLED,
                    "Notification request cancelled",
                ))
            } else {
                outcome
            };
            let (result, error) = match outcome {
                Ok(value) => (Some(value), None),
                Err((code, message)) => (
                    None,
                    Some(JsonRpcError {
                        code,
                        message: message.to_owned(),
                        data: None,
                    }),
                ),
            };
            if Client::from_inner(client)
                .send_response(&JsonRpcResponse {
                    jsonrpc: "2.0".to_owned(),
                    id: request.id,
                    result,
                    error,
                })
                .await
                .is_err()
            {
                warn!("failed to send notification response");
            }
            drop(pending);
        });
    }

    async fn handle(
        handler: Option<Arc<dyn NotificationHandler>>,
        method: &str,
        params: Value,
        client: Weak<ClientInner>,
        cancellation: CancellationToken,
    ) -> std::result::Result<Value, (i32, &'static str)> {
        let handler = handler.ok_or((
            error_codes::METHOD_NOT_FOUND,
            "No notification host handler registered",
        ))?;
        let invalid = || {
            (
                error_codes::INVALID_PARAMS,
                "Invalid notification host request",
            )
        };
        let failed = || (error_codes::INTERNAL_ERROR, "Notification handler failed");
        let context = |request: &NotificationHostContext| NotificationContext {
            cancellation,
            client,
            session_id: request.session_id.clone(),
        };
        match method {
            GET_CAPABILITIES_METHOD => {
                let request: NotificationHostRequest =
                    serde_json::from_value(params).map_err(|_| invalid())?;
                let context = context(&request.context);
                let response = handler
                    .get_capabilities(request, context)
                    .await
                    .map_err(|_| failed())?;
                serde_json::to_value(response).map_err(|_| failed())
            }
            REQUEST_PERMISSION_METHOD => {
                let request: NotificationHostRequest =
                    serde_json::from_value(params).map_err(|_| invalid())?;
                let context = context(&request.context);
                let response = handler
                    .request_permission(request, context)
                    .await
                    .map_err(|_| failed())?;
                serde_json::to_value(response).map_err(|_| failed())
            }
            SHOW_METHOD => {
                let request: NotificationHostShowParams =
                    serde_json::from_value(params).map_err(|_| invalid())?;
                let context = context(&request.context);
                let response = handler.show(request, context).await.map_err(|_| failed())?;
                serde_json::to_value(response).map_err(|_| failed())
            }
            _ => Err((
                error_codes::METHOD_NOT_FOUND,
                "Unknown notification host method",
            )),
        }
    }
}

impl Client {
    /// The initial notification-host registration outcome, or `None` if not configured.
    ///
    /// Only `registered` acknowledges registration. This is the handshake
    /// outcome, not a promise that a stopped or disconnected client can deliver.
    pub fn notification_host_registration(&self) -> Option<NotificationHostRegistrationResult> {
        self.inner.notifications.registration.read().clone()
    }

    #[cfg(any(feature = "runtime", test))]
    pub(crate) async fn register_notification_host(&self) -> Result<()> {
        let notifications = self.inner.notifications.clone();
        match self
            .call_with_inline_callback(
                "notifications.registerHost",
                Some(serde_json::json!({})),
                Some(Box::new(move |response| {
                    let outcome: NotificationHostRegistrationResult =
                        serde_json::from_value(response.result.clone().unwrap_or(Value::Null))
                            .map_err(|_| {
                                Error::with_message(
                                    ErrorKind::Json,
                                    "Invalid notification host registration response",
                                )
                            })?;
                    notifications.registered.store(
                        matches!(outcome, NotificationHostRegistrationResult::Registered(_)),
                        Ordering::Release,
                    );
                    *notifications.registration.write() = Some(outcome);
                    Ok(())
                })),
            )
            .await
        {
            Ok(_) => Ok(()),
            Err(error) if matches!(error.kind(), ErrorKind::Rpc { code: -32601 }) => {
                let unsupported =
                    serde_json::from_value(serde_json::json!({ "status": "unsupported" }))
                        .map_err(|_| {
                            Error::with_message(
                                ErrorKind::Json,
                                "Invalid notification host registration response",
                            )
                        })?;
                self.inner
                    .notifications
                    .registered
                    .store(false, Ordering::Release);
                *self.inner.notifications.registration.write() = Some(unsupported);
                Ok(())
            }
            Err(_) => Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "Notification host registration failed",
            )),
        }
    }

    pub(crate) async fn unregister_notification_host(&self) -> Result<()> {
        if !self.inner.notifications.clear() {
            return Ok(());
        }
        let response = tokio::time::timeout(
            Duration::from_secs(5),
            self.call("notifications.unregisterHost", Some(serde_json::json!({}))),
        )
        .await
        .map_err(|_| {
            Error::with_message(
                ErrorKind::InvalidConfig,
                "Notification host unregistration timed out",
            )
        })?
        .map_err(|_| {
            Error::with_message(
                ErrorKind::InvalidConfig,
                "Notification host unregistration failed",
            )
        })?;
        let response: NotificationHostUnregistrationResult = serde_json::from_value(response)
            .map_err(|_| {
                Error::with_message(
                    ErrorKind::Json,
                    "Invalid notification host unregistration response",
                )
            })?;
        if !matches!(
            response,
            NotificationHostUnregistrationResult::Unregistered(_)
        ) {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "Notification host unregistration failed",
            ));
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests;
