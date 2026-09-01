//! Connection-level extension launch profile resolution.

use std::sync::{Arc, OnceLock, Weak};

use async_trait::async_trait;
use parking_lot::RwLock;
use serde::Serialize;
use serde_json::Value;
use tracing::warn;

pub use crate::rpc::{
    ExtensionLaunchProfile, ExtensionLaunchProviderResolveRequest,
    ExtensionLaunchProviderResolveResult,
};
use crate::{
    Client, ClientInner, JsonRpcError, JsonRpcRequest, JsonRpcResponse, Result, error_codes,
};

pub(crate) const RESOLVE_METHOD: &str = "extensionLaunchProvider.resolve";
const MISSING_HANDLER_MESSAGE: &str = "No extensionLaunchProvider client-global handler registered";

/// Resolves process launch profiles for extension entrypoints discovered by the runtime.
///
/// Configure an implementation with
/// [`ClientOptions::with_extension_launch_provider`](crate::ClientOptions::with_extension_launch_provider).
/// The SDK registers the provider before [`Client::start`](crate::Client::start)
/// returns, so extension resolution cannot race session creation.
///
/// The returned executable, arguments, and environment are forwarded unchanged.
/// The runtime remains authoritative for its reserved `COPILOT_SDK_PATH`,
/// `SESSION_ID`, and `COPILOT_EXTENSION_PARENT_PID` environment variables.
#[async_trait]
pub trait ExtensionLaunchProvider: Send + Sync + 'static {
    /// Resolve a launch profile for one discovered extension entrypoint.
    ///
    /// Return a result with `launch: None` when the provider does not support
    /// the entrypoint.
    async fn resolve(
        &self,
        request: ExtensionLaunchProviderResolveRequest,
    ) -> Result<ExtensionLaunchProviderResolveResult>;
}

pub(crate) struct ExtensionLaunchProviderDispatcher {
    handler: RwLock<Option<Arc<dyn ExtensionLaunchProvider>>>,
    client: OnceLock<Weak<ClientInner>>,
}

impl ExtensionLaunchProviderDispatcher {
    pub(crate) fn new(handler: Option<Arc<dyn ExtensionLaunchProvider>>) -> Self {
        Self {
            handler: RwLock::new(handler),
            client: OnceLock::new(),
        }
    }

    pub(crate) fn set_client(&self, client: Weak<ClientInner>) {
        let _ = self.client.set(client);
    }

    pub(crate) fn is_configured(&self) -> bool {
        self.handler.read().is_some()
    }

    pub(crate) fn clear(&self) {
        self.handler.write().take();
    }

    pub(crate) async fn dispatch(&self, request: JsonRpcRequest) {
        let request_id = request.id;
        let Some(handler) = self.handler.read().clone() else {
            self.send_error(
                request_id,
                error_codes::INTERNAL_ERROR,
                MISSING_HANDLER_MESSAGE,
            )
            .await;
            return;
        };

        let params = request
            .params
            .unwrap_or_else(|| Value::Object(serde_json::Map::new()));
        let request = match serde_json::from_value(params) {
            Ok(request) => request,
            Err(error) => {
                self.send_error(
                    request_id,
                    error_codes::INVALID_PARAMS,
                    &format!("invalid params: {error}"),
                )
                .await;
                return;
            }
        };

        match handler.resolve(request).await {
            Ok(result) => self.respond(request_id, result).await,
            Err(error) => {
                self.send_error(request_id, error_codes::INTERNAL_ERROR, &error.to_string())
                    .await;
            }
        }
    }

    fn client(&self) -> Option<Client> {
        self.client
            .get()
            .and_then(Weak::upgrade)
            .map(Client::from_inner)
    }

    async fn respond<T: Serialize>(&self, request_id: u64, result: T) {
        let value = match serde_json::to_value(result) {
            Ok(value) => value,
            Err(error) => {
                warn!(error = %error, "failed to serialize extension launch provider response");
                self.send_error(
                    request_id,
                    error_codes::INTERNAL_ERROR,
                    "serialization failure",
                )
                .await;
                return;
            }
        };

        let Some(client) = self.client() else {
            return;
        };
        let _ = client
            .send_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request_id,
                result: Some(value),
                error: None,
            })
            .await;
    }

    async fn send_error(&self, request_id: u64, code: i32, message: &str) {
        let Some(client) = self.client() else {
            return;
        };
        let _ = client
            .send_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id: request_id,
                result: None,
                error: Some(JsonRpcError {
                    code,
                    message: message.to_string(),
                    data: None,
                }),
            })
            .await;
    }
}
