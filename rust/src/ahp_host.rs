//! Experimental runtime-owned Agent Host Protocol (AHP) listeners.

use std::collections::HashMap;
use std::sync::{Arc, Weak};

use parking_lot::Mutex;
use tokio::sync::broadcast;
use tracing::warn;

use crate::generated::api_types::{HostDisposeRequest, HostStartRequest};
use crate::{Client, ClientInner, Error, ErrorKind, ProtocolErrorKind};

/// Experimental AHP host exit reason, as reported by the runtime.
pub use crate::generated::api_types::HostExitReason as AhpHostExitReason;
/// Experimental AHP host exit payload, as reported by the runtime.
pub use crate::generated::api_types::HostExitedNotification as AhpHostExit;

/// Experimental local callback for a host's first `host.exited` notification.
pub type AhpHostExitCallback = Arc<dyn Fn(AhpHostExit) + Send + Sync>;

/// Options for [`Client::start_ahp_host`].
///
/// **Experimental.** May change or be removed in future releases.
/// Listener defaults and validation belong to the runtime, not the SDK.
#[derive(Clone, Default)]
#[non_exhaustive]
pub struct AhpHostOptions {
    /// Listener hostname; defaults to loopback in the runtime.
    pub hostname: Option<String>,
    /// Listener port; zero asks the runtime for an available port.
    pub port: Option<i32>,
    /// Optional explicit connection token.
    pub token: Option<String>,
    /// Whether connections require a token. Defaults to true in the runtime.
    pub require_connection_token: Option<bool>,
    /// Local callback, never serialized. Called at most once.
    ///
    /// Disconnect releases this callback without synthesizing an exit: a
    /// disconnected transport cannot establish whether the host was reaped.
    /// Panics are caught and logged, as for other SDK notification callbacks.
    pub on_exit: Option<AhpHostExitCallback>,
}

impl std::fmt::Debug for AhpHostOptions {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("AhpHostOptions")
            .field("hostname", &self.hostname)
            .field("port", &self.port)
            .field("token", &self.token.as_ref().map(|_| "[redacted]"))
            .field("require_connection_token", &self.require_connection_token)
            .field("on_exit", &self.on_exit.is_some())
            .finish()
    }
}

impl AhpHostOptions {
    /// Create options using runtime defaults.
    pub fn new() -> Self {
        Self::default()
    }

    /// Set the listener hostname.
    pub fn with_hostname(mut self, hostname: impl Into<String>) -> Self {
        self.hostname = Some(hostname.into());
        self
    }

    /// Set the listener port.
    pub fn with_port(mut self, port: i32) -> Self {
        self.port = Some(port);
        self
    }

    /// Set an explicit connection token.
    pub fn with_token(mut self, token: impl Into<String>) -> Self {
        self.token = Some(token.into());
        self
    }

    /// Set whether connections require a token.
    pub fn with_require_connection_token(mut self, required: bool) -> Self {
        self.require_connection_token = Some(required);
        self
    }

    /// Set the local exit callback.
    pub fn with_on_exit(mut self, callback: impl Fn(AhpHostExit) + Send + Sync + 'static) -> Self {
        self.on_exit = Some(Arc::new(callback));
        self
    }
}

/// A small handle to a runtime-owned AHP listener.
///
/// **Experimental.** May change or be removed in future releases.
/// The owning [`Client`] connection controls the listener's lifetime. This
/// handle does not keep the client alive. Dropping it performs no RPC or
/// background cleanup; call [`dispose`](Self::dispose) explicitly instead.
#[derive(Clone)]
#[non_exhaustive]
pub struct AhpHost {
    /// Runtime host identifier.
    pub host_id: String,
    /// Listener URL returned by the runtime.
    pub url: String,
    /// Host process identifier returned by the runtime.
    pub pid: i64,
    /// Connection token, absent when token authentication is disabled.
    pub token: Option<String>,
    client: Weak<ClientInner>,
}

impl std::fmt::Debug for AhpHost {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("AhpHost")
            .field("host_id", &self.host_id)
            .field("url", &self.url)
            .field("pid", &self.pid)
            .field("token", &self.token.as_ref().map(|_| "[redacted]"))
            .finish_non_exhaustive()
    }
}

impl AhpHost {
    /// Forward `host.dispose` to the runtime.
    ///
    /// Every call makes its own RPC, including repeated and concurrent calls.
    /// Results and errors come from the runtime; no result is cached and no
    /// exit notification is synthesized.
    pub async fn dispose(&self) -> Result<(), Error> {
        let client = Client {
            inner: self.client.upgrade().ok_or_else(|| {
                Error::from(ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled))
            })?,
        };
        client
            .rpc()
            .host()
            .dispose(HostDisposeRequest {
                host_id: self.host_id.clone(),
            })
            .await?;
        Ok(())
    }
}

pub(crate) type ExitCallbacks = Mutex<HashMap<String, AhpHostExitCallback>>;

// Removes local registration even if the start future is cancelled. The runtime
// still owns any host whose start request has already reached the connection.
struct PendingStart {
    callbacks: Arc<ExitCallbacks>,
    host_id: Option<String>,
}

impl Drop for PendingStart {
    fn drop(&mut self) {
        if let Some(host_id) = &self.host_id {
            let callback = self.callbacks.lock().remove(host_id);
            drop(callback);
        }
    }
}

impl Client {
    /// Start a runtime-owned AHP listener through the generated `host.start` RPC.
    ///
    /// **Experimental.** May change or be removed in future releases.
    /// The runtime supplies listener defaults and validates all listener options.
    /// A local exit callback is registered before sending the request, so it can
    /// receive an exit that arrives before the start response.
    pub async fn start_ahp_host(&self, options: AhpHostOptions) -> Result<AhpHost, Error> {
        let host_id = uuid::Uuid::new_v4().to_string();
        let mut pending = PendingStart {
            callbacks: self.inner.ahp_host_callbacks.clone(),
            host_id: Some(host_id.clone()),
        };
        if let Some(callback) = options.on_exit {
            pending.callbacks.lock().insert(host_id.clone(), callback);
        }
        let result = self
            .rpc()
            .host()
            .start(HostStartRequest {
                host_id,
                hostname: options.hostname,
                port: options.port,
                token: options.token,
                require_connection_token: options.require_connection_token,
            })
            .await?;
        pending.host_id = None;
        Ok(AhpHost {
            host_id: result.host_id,
            url: result.url,
            pid: result.pid,
            token: result.token,
            client: Arc::downgrade(&self.inner),
        })
    }

    pub(crate) fn spawn_ahp_host_dispatcher(&self) {
        let callbacks = Arc::downgrade(&self.inner.ahp_host_callbacks);
        let mut notifications = self.inner.notification_tx.subscribe();
        let closed = self.inner.rpc.connection_closed_token();
        tokio::spawn(async move {
            loop {
                let notification = tokio::select! {
                    biased;
                    notification = notifications.recv() => notification,
                    _ = closed.cancelled() => break,
                };
                match notification {
                    Ok(notification) if notification.method == "host.exited" => {
                        let Some(params) = notification.params else {
                            continue;
                        };
                        let exit = match serde_json::from_value::<AhpHostExit>(params) {
                            Ok(exit) => exit,
                            Err(error) => {
                                warn!(%error, "failed to deserialize host.exited notification");
                                continue;
                            }
                        };
                        let Some(callbacks) = callbacks.upgrade() else {
                            return;
                        };
                        let callback = callbacks.lock().remove(&exit.host_id);
                        if let Some(callback) = callback
                            && std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                                callback(exit);
                            }))
                            .is_err()
                        {
                            warn!("host.exited callback panicked; continuing notification routing");
                        }
                    }
                    Ok(_) => {}
                    Err(broadcast::error::RecvError::Lagged(missed)) => {
                        warn!(missed, "AHP host dispatcher lagged");
                    }
                    Err(broadcast::error::RecvError::Closed) => break,
                }
            }
            if let Some(callbacks) = callbacks.upgrade() {
                let callbacks = std::mem::take(&mut *callbacks.lock());
                drop(callbacks);
            }
        });
    }
}

#[cfg(test)]
mod tests;
