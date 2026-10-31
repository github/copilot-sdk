// Copyright (c) Microsoft Corporation. All rights reserved.

//! Kerberos credentials supplied by the SDK host.
//!
//! By default the provider authenticates only requests made for sessions the
//! client creates or resumes; see
//! [`KerberosAuthScope`](crate::kerberos_auth::KerberosAuthScope). The
//! registration ends when the connection closes.

use std::future::Future;
use std::sync::Arc;

use async_trait::async_trait;

use crate::Result;
pub use crate::rpc::{KerberosAuthAcquireRequest, KerberosAuthMode, KerberosAuthScope};

pub(crate) const ACQUIRE_METHOD: &str = "kerberosAuth.acquire";

/// Supplies initial SPNEGO tokens for Kerberos service principals requested by the runtime.
///
/// Configure an implementation with
/// [`ClientOptions::with_kerberos_auth`](crate::ClientOptions::with_kerberos_auth).
/// Async functions and closures taking a [`KerberosAuthAcquireRequest`] implement this trait.
#[async_trait]
pub trait KerberosAuthProvider: Send + Sync + 'static {
    /// Return a fresh, single-use base64 SPNEGO token without the `Negotiate ` prefix, or
    /// `None` when credentials are unavailable.
    ///
    /// The request's `spn` uses the platform-independent `service/host[@REALM]` form;
    /// validate it before issuing credentials. An error is returned to the runtime as a
    /// JSON-RPC error. Never log or persist the token.
    ///
    /// When the runtime stops waiting for this request, or the connection closes, the
    /// SDK drops the returned future, so any separately spawned work must stop on its own.
    async fn acquire(&self, request: KerberosAuthAcquireRequest) -> Result<Option<String>>;
}

#[async_trait]
impl<F, Fut> KerberosAuthProvider for F
where
    F: Fn(KerberosAuthAcquireRequest) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Option<String>>> + Send,
{
    async fn acquire(&self, request: KerberosAuthAcquireRequest) -> Result<Option<String>> {
        (self)(request).await
    }
}

/// Kerberos authentication callback configuration.
///
/// When set on [`ClientOptions::kerberos_auth`](crate::ClientOptions::kerberos_auth),
/// [`Client::start`](crate::Client::start) registers the provider with the runtime
/// before sessions can be created. The registration ends when the connection closes.
#[non_exhaustive]
#[derive(Clone)]
pub struct KerberosAuthConfig {
    /// [`KerberosAuthMode::Fallback`] uses the provider only when native lookup fails;
    /// [`KerberosAuthMode::Always`] never attempts native lookup.
    pub mode: KerberosAuthMode,
    /// Traffic the provider authenticates; `None` lets the runtime default to
    /// [`KerberosAuthScope::Connection`].
    ///
    /// [`KerberosAuthScope::Connection`] authenticates only requests made for
    /// sessions this client creates or resumes. [`KerberosAuthScope::Runtime`]
    /// also authenticates the runtime's shared background traffic; only one
    /// connection at a time can hold it, so registration fails if another
    /// client holds it.
    pub scope: Option<KerberosAuthScope>,
    /// Supplies tokens for `kerberosAuth.acquire` requests.
    pub provider: Arc<dyn KerberosAuthProvider>,
}

impl KerberosAuthConfig {
    /// Build a configuration for `provider` in the given `mode`, with the
    /// runtime's default scope.
    pub fn new<P>(mode: KerberosAuthMode, provider: P) -> Self
    where
        P: KerberosAuthProvider,
    {
        Self {
            mode,
            scope: None,
            provider: Arc::new(provider),
        }
    }

    /// Set the traffic the provider authenticates; see [`KerberosAuthConfig::scope`].
    pub fn with_scope(mut self, scope: KerberosAuthScope) -> Self {
        self.scope = Some(scope);
        self
    }
}

impl std::fmt::Debug for KerberosAuthConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("KerberosAuthConfig")
            .field("mode", &self.mode)
            .field("scope", &self.scope)
            .field("provider", &"<set>")
            .finish()
    }
}

/// Handle `kerberosAuth.acquire` with the configured provider, then register it with the runtime.
///
/// These requests honor `$/cancelRequest`, which drops the provider future.
#[cfg(feature = "runtime")]
pub(crate) async fn register(client: &crate::Client, config: KerberosAuthConfig) -> Result<()> {
    let KerberosAuthConfig {
        mode,
        scope,
        provider,
    } = config;
    client.register_request_handler(ACQUIRE_METHOD, move |params, _client| {
        let provider = provider.clone();
        async move {
            let token = provider.acquire(serde_json::from_value(params)?).await?;
            Ok(serde_json::to_value(
                crate::rpc::KerberosAuthAcquireResult { token },
            )?)
        }
    })?;
    client
        .rpc()
        .kerberos_auth()
        .set_provider(crate::rpc::KerberosAuthSetProviderRequest { mode, scope })
        .await
}
