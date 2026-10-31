//! Experimental connection-owned Microsoft Entra acquisition.

use std::future::Future;
use std::panic::AssertUnwindSafe;
use std::sync::{Arc, OnceLock, Weak};

use async_trait::async_trait;
use futures_util::FutureExt;
use tokio_util::sync::CancellationToken;
use tracing::warn;

/// **Experimental.** Request for host-owned identity or resource-token acquisition.
pub use crate::rpc::EntraTokenAcquireRequest as EntraTokenProviderArgs;
use crate::rpc::EntraTokenGetTokenRequest;
use crate::{
    Client, ClientInner, JsonRpcError, JsonRpcRequest, JsonRpcResponse, Result, error_codes,
};

pub(crate) const ACQUIRE_METHOD: &str = "entraToken.getToken";

/// Result of host-owned identity selection or resource-token acquisition.
///
/// **Experimental.** May change or be removed in future releases.
#[derive(Clone, serde::Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub enum EntraTokenProviderResult {
    /// An identity was selected or revalidated without acquiring a resource token.
    Identity {
        /// Opaque host account identifier, preserved for subsequent acquisitions.
        #[serde(rename = "accountId")]
        account_id: String,
        /// Optional display name for the selected identity.
        #[serde(skip_serializing_if = "Option::is_none")]
        username: Option<String>,
    },
    /// A resource token was acquired for exactly the requested identity.
    Token {
        /// The opaque host account identifier from the request.
        #[serde(rename = "accountId")]
        account_id: String,
        /// Resource access token. The host owns its caching and expiration.
        #[serde(rename = "accessToken")]
        access_token: String,
    },
    /// The requested identity or resource is unavailable.
    Unavailable,
    /// The host cancelled the acquisition.
    Cancelled,
}

impl std::fmt::Debug for EntraTokenProviderResult {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Identity { .. } => formatter.write_str("Identity { .. }"),
            Self::Token { .. } => formatter.write_str("Token { .. }"),
            Self::Unavailable => formatter.write_str("Unavailable"),
            Self::Cancelled => formatter.write_str("Cancelled"),
        }
    }
}

/// Cancellation lifetime of one acquisition on its original connection.
///
/// **Experimental.** May change or be removed in future releases.
#[derive(Clone)]
pub struct EntraTokenProviderContext {
    cancellation: CancellationToken,
}

impl EntraTokenProviderContext {
    /// Cancelled when the runtime retires the request or the connection closes.
    ///
    /// Cancelling the returned child token cannot cancel the connection.
    ///
    /// **Experimental.** May change or be removed in future releases.
    pub fn cancellation(&self) -> CancellationToken {
        self.cancellation.child_token()
    }
}

/// Host-owned identity selection and resource-token acquisition.
///
/// Renewal must remain silent and honor the exact requested account. Cancellation
/// drops the callback future; independently spawned work must observe the context.
///
/// **Experimental.** May change or be removed in future releases.
#[async_trait]
pub trait EntraTokenProvider: Send + Sync + 'static {
    /// Return the matching identity, token, unavailable, or cancelled outcome.
    ///
    /// **Experimental.** May change or be removed in future releases.
    async fn get_token(
        &self,
        request: EntraTokenProviderArgs,
        context: EntraTokenProviderContext,
    ) -> Result<EntraTokenProviderResult>;
}

#[async_trait]
impl<F, Fut> EntraTokenProvider for F
where
    F: Fn(EntraTokenProviderArgs, EntraTokenProviderContext) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<EntraTokenProviderResult>> + Send,
{
    async fn get_token(
        &self,
        request: EntraTokenProviderArgs,
        context: EntraTokenProviderContext,
    ) -> Result<EntraTokenProviderResult> {
        (self)(request, context).await
    }
}

pub(crate) struct EntraTokenDispatcher {
    provider: Option<Arc<dyn EntraTokenProvider>>,
    client: OnceLock<Weak<ClientInner>>,
}

impl EntraTokenDispatcher {
    pub(crate) fn new(provider: Option<Arc<dyn EntraTokenProvider>>) -> Self {
        Self {
            provider,
            client: OnceLock::new(),
        }
    }

    pub(crate) fn set_client(&self, client: Weak<ClientInner>) {
        let _ = self.client.set(client);
    }

    pub(crate) fn dispatch(self: &Arc<Self>, request: JsonRpcRequest) {
        let Some(client) = self.client.get().and_then(Weak::upgrade) else {
            return;
        };
        let Some(pending) = client.rpc.cancellable_requests.claim(request.id) else {
            return;
        };
        let request_cancelled = pending.cancellation().clone();
        let connection_closed = client.rpc.connection_closed_token();
        let context = EntraTokenProviderContext {
            cancellation: connection_closed.child_token(),
        };
        drop(client);
        let dispatcher = self.clone();
        tokio::spawn(async move {
            let outcome = tokio::select! {
                biased;
                _ = connection_closed.cancelled() => return,
                _ = request_cancelled.cancelled() => {
                    context.cancellation.cancel();
                    Err((error_codes::REQUEST_CANCELLED, "Entra acquisition cancelled"))
                }
                result = dispatcher.acquire(request.params, context.clone()) => result,
            };
            if connection_closed.is_cancelled() {
                return;
            }
            let outcome = if request_cancelled.is_cancelled() {
                context.cancellation.cancel();
                Err((
                    error_codes::REQUEST_CANCELLED,
                    "Entra acquisition cancelled",
                ))
            } else {
                outcome
            };
            let (result, error) = match outcome {
                Ok(result) => (Some(result), None),
                Err((code, message)) => (
                    None,
                    Some(JsonRpcError {
                        code: code.into(),
                        message: message.to_owned(),
                        data: None,
                    }),
                ),
            };
            let Some(client) = dispatcher.client.get().and_then(Weak::upgrade) else {
                return;
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
                warn!("failed to send Entra acquisition response");
            }
            drop(pending);
        });
    }

    async fn acquire(
        &self,
        params: Option<serde_json::Value>,
        context: EntraTokenProviderContext,
    ) -> std::result::Result<serde_json::Value, (i32, &'static str)> {
        let provider = self.provider.as_ref().ok_or((
            error_codes::METHOD_NOT_FOUND,
            "No host-owned Entra token provider configured",
        ))?;
        let request: EntraTokenGetTokenRequest =
            serde_json::from_value(params.unwrap_or(serde_json::Value::Null)).map_err(|_| {
                (
                    error_codes::INVALID_PARAMS,
                    "Invalid Entra acquisition request",
                )
            })?;
        let result = match AssertUnwindSafe(provider.get_token(request.request, context))
            .catch_unwind()
            .await
        {
            Ok(Ok(result)) => result,
            Ok(Err(_)) => return Err((error_codes::INTERNAL_ERROR, "Entra token provider failed")),
            Err(_) => return Err((error_codes::INTERNAL_ERROR, "Entra token provider panicked")),
        };
        serde_json::to_value(result).map_err(|_| {
            (
                error_codes::INTERNAL_ERROR,
                "Invalid Entra token provider result",
            )
        })
    }
}

#[cfg(test)]
mod tests;
