// Copyright (c) Microsoft Corporation. All rights reserved.

//! Network callbacks through which the SDK host takes over parts of the
//! runtime's networking.
//!
//! **Experimental.** May change or be removed in future releases.
//!
//! Configure them with [`NetworkConfig`](crate::network::NetworkConfig) on
//! [`ClientOptions::with_network`](crate::ClientOptions::with_network), or for a
//! client created with [`Client::from_streams`](crate::Client::from_streams), with
//! [`Client::register_network`](crate::Client::register_network), for
//! example when the runtime runs in WSL or a container and should use VS Code's
//! proxy settings, the host's Kerberos tickets or its proxy credentials. Each
//! registered callback replaces the runtime's own handling for the traffic
//! selected by [`NetworkConfig::scope`](crate::network::NetworkConfig::scope);
//! without it, the runtime handles that part itself. The registration ends when
//! the connection closes.

use std::future::Future;
use std::sync::Arc;

use async_trait::async_trait;

use crate::Result;
pub use crate::rpc::{
    NetworkAuthenticateBasicRequest, NetworkAuthenticateBasicResult,
    NetworkAuthenticateNegotiateRequest, NetworkResolveProxyRequest, NetworkScope,
};

pub(crate) const RESOLVE_PROXY_METHOD: &str = "network.resolveProxy";
pub(crate) const AUTHENTICATE_NEGOTIATE_METHOD: &str = "network.authenticateNegotiate";
pub(crate) const AUTHENTICATE_BASIC_METHOD: &str = "network.authenticateBasic";

/// Decides the proxies for the runtime's requests from the SDK host, for example from
/// VS Code's proxy settings when the runtime runs in WSL or a container.
///
/// Configure an implementation with [`NetworkConfig::with_resolve_proxy`]. Async
/// functions and closures taking a [`NetworkResolveProxyRequest`] implement this trait.
///
/// **Experimental.** May change or be removed in future releases.
#[async_trait]
pub trait ResolveProxyProvider: Send + Sync + 'static {
    /// Return the proxies for the request's `url` as a PAC-style list such as
    /// `PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT` (the format of
    /// Electron's `session.resolveProxy`); return `DIRECT` for a direct connection, or
    /// `None` to let the runtime resolve the proxy itself, as if no provider were
    /// configured. Entry types the runtime doesn't support, such as SOCKS and QUIC, are
    /// skipped.
    ///
    /// The `url` is the request target as a PAC script sees it: without credentials or
    /// fragment, and for `https` and `wss` also without path and query. The provider
    /// decides every request that would otherwise follow ambient proxy settings: proxy
    /// environment variables, `NO_PROXY` and operating-system proxy settings are not
    /// consulted unless it returns `None`. An error is returned to the runtime as a
    /// JSON-RPC error and fails the request rather than connecting directly. The runtime
    /// caches answers, including `None`, for 5 seconds.
    ///
    /// When the runtime stops waiting for this request (it cancels the request, for
    /// example after waiting 10 seconds, or the connection closes), the SDK drops the
    /// returned future, so any separately spawned work must stop on its own.
    async fn resolve_proxy(&self, request: NetworkResolveProxyRequest) -> Result<Option<String>>;
}

#[async_trait]
impl<F, Fut> ResolveProxyProvider for F
where
    F: Fn(NetworkResolveProxyRequest) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Option<String>>> + Send,
{
    async fn resolve_proxy(&self, request: NetworkResolveProxyRequest) -> Result<Option<String>> {
        (self)(request).await
    }
}

/// Supplies the Kerberos tokens for proxies' Negotiate challenges from the SDK host,
/// for example from Windows when the runtime runs in WSL.
///
/// Configure an implementation with [`NetworkConfig::with_authenticate_negotiate`].
/// Async functions and closures taking a [`NetworkAuthenticateNegotiateRequest`]
/// implement this trait.
///
/// **Experimental.** May change or be removed in future releases.
#[async_trait]
pub trait AuthenticateNegotiateProvider: Send + Sync + 'static {
    /// Return a fresh, single-use base64 SPNEGO token without the `Negotiate ` prefix, or
    /// `None` when credentials are unavailable.
    ///
    /// The runtime then does not look up Kerberos credentials itself, so `None` or an
    /// error fails the request, unless the proxy also offers Basic and an
    /// [`AuthenticateBasicProvider`] supplies credentials; an error is returned to the
    /// runtime as a JSON-RPC error. The request's `spn` uses the platform-independent
    /// `service/host[@REALM]` form; validate it before issuing credentials. Never log or
    /// persist the token. Only the initial token is requested; exchanges requiring
    /// continuation tokens are not supported.
    ///
    /// When the runtime stops waiting for this request (it cancels the request, for
    /// example after waiting 30 seconds, or the connection closes), the SDK drops the
    /// returned future, so any separately spawned work must stop on its own.
    async fn authenticate_negotiate(
        &self,
        request: NetworkAuthenticateNegotiateRequest,
    ) -> Result<Option<String>>;
}

#[async_trait]
impl<F, Fut> AuthenticateNegotiateProvider for F
where
    F: Fn(NetworkAuthenticateNegotiateRequest) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Option<String>>> + Send,
{
    async fn authenticate_negotiate(
        &self,
        request: NetworkAuthenticateNegotiateRequest,
    ) -> Result<Option<String>> {
        (self)(request).await
    }
}

/// Supplies the credentials for proxies' Basic challenges from the SDK host, for
/// example credentials the host's user entered.
///
/// Configure an implementation with [`NetworkConfig::with_authenticate_basic`].
/// Async functions and closures taking a [`NetworkAuthenticateBasicRequest`]
/// implement this trait.
///
/// **Experimental.** May change or be removed in future releases.
#[async_trait]
pub trait AuthenticateBasicProvider: Send + Sync + 'static {
    /// Return credentials for a proxy's Basic challenge, or `None` when unavailable,
    /// which fails the request with the proxy's challenge.
    ///
    /// Applies to requests the runtime tunnels through the proxy (`https` and `wss`).
    /// The runtime remembers the answer for that proxy and uses it for later
    /// connections, until the proxy rejects it; then it calls again with
    /// [`previous_rejected`](NetworkAuthenticateBasicRequest::previous_rejected) set, so
    /// answer with different credentials, for example by asking the user, or with `None`.
    /// A later connection sends the answer without waiting for a challenge, unless the
    /// runtime may try Negotiate with that proxy first.
    /// A connection submits at most three sets of credentials. While a call is pending, other requests through
    /// that proxy wait for its answer. When a proxy offers both Negotiate and Basic, the
    /// runtime prefers Negotiate and answers with Basic when Negotiate is unavailable or
    /// the proxy rejects it. The request's
    /// [`realm`](NetworkAuthenticateBasicRequest::realm) comes from the proxy, so treat
    /// it as untrusted text. An empty user name also fails the request, the user name
    /// must not contain a colon, and neither the user name nor the password may contain
    /// control characters; an omitted password is empty. An error is returned to the
    /// runtime as a JSON-RPC error and fails the request. Never log or persist
    /// credentials.
    ///
    /// The runtime does not time this request out: a request whose own deadline passes
    /// stops waiting, but the answer is still remembered for later requests. When the
    /// connection closes, the SDK drops the returned future, so any separately spawned
    /// work must stop on its own.
    async fn authenticate_basic(
        &self,
        request: NetworkAuthenticateBasicRequest,
    ) -> Result<Option<NetworkAuthenticateBasicResult>>;
}

#[async_trait]
impl<F, Fut> AuthenticateBasicProvider for F
where
    F: Fn(NetworkAuthenticateBasicRequest) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Option<NetworkAuthenticateBasicResult>>> + Send,
{
    async fn authenticate_basic(
        &self,
        request: NetworkAuthenticateBasicRequest,
    ) -> Result<Option<NetworkAuthenticateBasicResult>> {
        (self)(request).await
    }
}

/// Network callback configuration.
///
/// When set on [`ClientOptions::network`](crate::ClientOptions::network) with at
/// least one callback, [`Client::start`](crate::Client::start) registers the
/// callbacks with the runtime before sessions can be created; a failed registration
/// fails startup. Only the configured callbacks are registered. The registration ends
/// when the connection closes.
///
/// **Experimental.** May change or be removed in future releases.
#[non_exhaustive]
#[derive(Clone, Default)]
pub struct NetworkConfig {
    /// Traffic the callbacks serve; `None` lets the runtime default to
    /// [`NetworkScope::Connection`].
    ///
    /// [`NetworkScope::Connection`] serves only requests made for sessions this
    /// client creates or resumes. [`NetworkScope::Runtime`] also serves the
    /// runtime's shared background traffic, such as catalog refreshes and
    /// telemetry; only one connection at a time can hold it, so startup fails if
    /// another client holds it.
    pub scope: Option<NetworkScope>,
    /// Answers `network.resolveProxy` requests; see [`ResolveProxyProvider`].
    ///
    /// Requests to loopback targets, and requests with an explicitly configured
    /// proxy, never consult it.
    pub resolve_proxy: Option<Arc<dyn ResolveProxyProvider>>,
    /// Answers `network.authenticateNegotiate` requests; see
    /// [`AuthenticateNegotiateProvider`].
    ///
    /// Proxies chosen by [`NetworkConfig::resolve_proxy`] are authenticated the
    /// same way.
    pub authenticate_negotiate: Option<Arc<dyn AuthenticateNegotiateProvider>>,
    /// Answers `network.authenticateBasic` requests; see
    /// [`AuthenticateBasicProvider`].
    ///
    /// Proxies chosen by [`NetworkConfig::resolve_proxy`] are authenticated the
    /// same way.
    pub authenticate_basic: Option<Arc<dyn AuthenticateBasicProvider>>,
}

impl NetworkConfig {
    /// Build a configuration without callbacks, with the runtime's default scope.
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the traffic the callbacks serve; see [`NetworkConfig::scope`].
    pub fn with_scope(mut self, scope: NetworkScope) -> Self {
        self.scope = Some(scope);
        self
    }

    /// Decide the runtime's proxies with `provider`; see [`ResolveProxyProvider`].
    pub fn with_resolve_proxy<P>(mut self, provider: P) -> Self
    where
        P: ResolveProxyProvider,
    {
        self.resolve_proxy = Some(Arc::new(provider));
        self
    }

    /// Supply Negotiate authentication tokens with `provider`; see
    /// [`AuthenticateNegotiateProvider`].
    pub fn with_authenticate_negotiate<P>(mut self, provider: P) -> Self
    where
        P: AuthenticateNegotiateProvider,
    {
        self.authenticate_negotiate = Some(Arc::new(provider));
        self
    }

    /// Supply Basic authentication credentials with `provider`; see
    /// [`AuthenticateBasicProvider`].
    pub fn with_authenticate_basic<P>(mut self, provider: P) -> Self
    where
        P: AuthenticateBasicProvider,
    {
        self.authenticate_basic = Some(Arc::new(provider));
        self
    }
}

impl std::fmt::Debug for NetworkConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("NetworkConfig")
            .field("scope", &self.scope)
            .field(
                "resolve_proxy",
                &self.resolve_proxy.as_ref().map(|_| "<set>"),
            )
            .field(
                "authenticate_negotiate",
                &self.authenticate_negotiate.as_ref().map(|_| "<set>"),
            )
            .field(
                "authenticate_basic",
                &self.authenticate_basic.as_ref().map(|_| "<set>"),
            )
            .finish()
    }
}

/// Handle the configured callbacks' requests, then register them with the runtime.
///
/// Does nothing when no callback is configured. These requests honor
/// `$/cancelRequest`, which drops the provider future. When the registration
/// fails, the handlers it installed are removed, so it can be retried on the
/// same connection. The runtime may accept the registration even if the
/// returned future is dropped, so the registration request and that cleanup
/// complete in the background either way, without keeping the client alive.
pub(crate) async fn register(client: &crate::Client, config: NetworkConfig) -> Result<()> {
    let NetworkConfig {
        scope,
        resolve_proxy,
        authenticate_negotiate,
        authenticate_basic,
    } = config;
    if resolve_proxy.is_none() && authenticate_negotiate.is_none() && authenticate_basic.is_none() {
        return Ok(());
    }
    let mut request = crate::rpc::NetworkSetProviderOptions::new();
    if let Some(scope) = scope {
        request = request.scope(scope);
    }
    if resolve_proxy.is_some() {
        request = request.resolve_proxy(Default::default());
    }
    if authenticate_negotiate.is_some() {
        request = request.authenticate_negotiate(Default::default());
    }
    if authenticate_basic.is_some() {
        request = request.authenticate_basic(Default::default());
    }
    let mut installed = Vec::new();
    let submitted = install(
        client,
        resolve_proxy,
        authenticate_negotiate,
        authenticate_basic,
        &mut installed,
    )
    .and_then(|()| {
        client.begin_call(
            crate::generated::api_types::rpc_methods::NETWORK_SETPROVIDER,
            Some(serde_json::to_value(request)?),
        )
    });
    let response = match submitted {
        Ok(response) => response,
        Err(error) => {
            uninstall(&client.inner, &installed);
            return Err(error);
        }
    };
    // Dropping the client, for example when `Client::start` is cancelled, closes the connection
    // and so fails the pending request, which ends this task.
    let owner = Arc::downgrade(&client.inner);
    tokio::spawn(async move {
        let result = response.await.map(drop);
        if result.is_err()
            && let Some(inner) = owner.upgrade()
        {
            uninstall(&inner, &installed);
        }
        result
    })
    .await
    .unwrap_or_else(|error| {
        Err(crate::Error::with_message(
            crate::ErrorKind::Protocol(crate::ProtocolErrorKind::RequestCancelled),
            format!("Network registration did not complete: {error}"),
        ))
    })
}

/// Install the handlers for the configured callbacks, recording each installed method.
fn install(
    client: &crate::Client,
    resolve_proxy: Option<Arc<dyn ResolveProxyProvider>>,
    authenticate_negotiate: Option<Arc<dyn AuthenticateNegotiateProvider>>,
    authenticate_basic: Option<Arc<dyn AuthenticateBasicProvider>>,
    installed: &mut Vec<&'static str>,
) -> Result<()> {
    if let Some(provider) = resolve_proxy {
        client.register_request_handler(RESOLVE_PROXY_METHOD, move |params, _client| {
            let provider = provider.clone();
            async move {
                let proxy = provider
                    .resolve_proxy(serde_json::from_value(params)?)
                    .await?;
                Ok(serde_json::to_value(
                    crate::rpc::NetworkResolveProxyResult { proxy },
                )?)
            }
        })?;
        installed.push(RESOLVE_PROXY_METHOD);
    }
    if let Some(provider) = authenticate_negotiate {
        client.register_request_handler(
            AUTHENTICATE_NEGOTIATE_METHOD,
            move |params, _client| {
                let provider = provider.clone();
                async move {
                    let token = provider
                        .authenticate_negotiate(serde_json::from_value(params)?)
                        .await?;
                    Ok(serde_json::to_value(
                        crate::rpc::NetworkAuthenticateNegotiateResult { token },
                    )?)
                }
            },
        )?;
        installed.push(AUTHENTICATE_NEGOTIATE_METHOD);
    }
    if let Some(provider) = authenticate_basic {
        client.register_request_handler(AUTHENTICATE_BASIC_METHOD, move |params, _client| {
            let provider = provider.clone();
            async move {
                let credentials = provider
                    .authenticate_basic(serde_json::from_value(params)?)
                    .await?;
                Ok(serde_json::to_value(credentials.unwrap_or_default())?)
            }
        })?;
        installed.push(AUTHENTICATE_BASIC_METHOD);
    }
    Ok(())
}

fn uninstall(inner: &crate::ClientInner, installed: &[&'static str]) {
    for method in installed {
        inner.rpc.unregister_request_handler(method);
    }
}
