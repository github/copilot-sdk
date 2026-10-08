//! Experimental runtime-owned Agent Host Protocol (AHP) listeners.

use std::collections::HashMap;
use std::future::Future;
use std::sync::{Arc, Weak};

use async_trait::async_trait;
use futures_util::FutureExt;
use parking_lot::Mutex;
use serde_json::Value;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;
use tracing::warn;

/// Experimental AHP host exit reason, including local owner disconnection.
pub use crate::generated::api_types::HostExitReason as AhpHostExitReason;
/// Experimental listener-task exit payload or local owner-disconnection report.
/// `Exited` means hosting-task failure, not runtime process death; `exit_code` is `None`.
pub use crate::generated::api_types::HostExitedNotification as AhpHostExit;
use crate::generated::api_types::{
    HostDisposeRequest, HostListSessionsRequest, HostListSessionsResult, HostPublishSessionRequest,
    HostPublishSessionResult, HostSessionCreateCallback, HostSessionReleasedNotification,
    HostStartOptions, HostStartResult,
};
use crate::session::Session;
use crate::{
    Client, ClientInner, Error, ErrorKind, ProtocolErrorKind, ResumeSessionConfig, SessionConfig,
};

/// Experimental local callback for a host's exit or owner disconnection.
pub type AhpHostExitCallback = Arc<dyn Fn(AhpHostExit) + Send + Sync>;

/// One request to create an application-owned session for an AHP participant.
///
/// **Experimental.** Preserve the supplied configuration, especially the session
/// ID and working directory. Add application tools, hooks and handlers before
/// calling [`Client::create_session`] on the supplied request-scoped client.
#[non_exhaustive]
pub struct AhpSessionRequest {
    /// Host-selected settings, with no permission handler installed.
    pub config: SessionConfig,
    /// Cancelled when the handoff ends, including timeout or owner loss.
    ///
    /// This is a child token: cancelling it does not cancel the host or session.
    /// Cancellation is cooperative. A session returned after cancellation is
    /// still passed to the release callback exactly once.
    pub cancellation_token: CancellationToken,
}

/// Host-selected settings for resuming a durable application-owned AHP session.
#[non_exhaustive]
pub struct AhpSessionResumeRequest {
    /// Preserve these settings and add application tools, hooks and handlers.
    pub config: ResumeSessionConfig,
    /// Cooperative cancellation; late returned sessions are still released once.
    pub cancellation_token: CancellationToken,
}

/// Resumes application-owned AHP sessions. Published resident sessions bypass this callback.
#[async_trait]
pub trait AhpSessionResumeFactory: Send + Sync + 'static {
    /// Return the original resumed allocation, or a retained attached original.
    async fn resume_session(
        &self,
        request: AhpSessionResumeRequest,
        client: Client,
    ) -> Result<Arc<Session>, Error>;
}

#[async_trait]
impl<F, Fut> AhpSessionResumeFactory for F
where
    F: Fn(AhpSessionResumeRequest, Client) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Arc<Session>, Error>> + Send,
{
    async fn resume_session(
        &self,
        request: AhpSessionResumeRequest,
        client: Client,
    ) -> Result<Arc<Session>, Error> {
        self(request, client).await
    }
}

/// Creates ordinary SDK sessions for a runtime-supervised AHP host.
///
/// **Experimental.** Return the original session in an [`Arc`], not a resumed
/// wrapper. The SDK retains that exact allocation until the participation ends.
/// The runtime checks that it is a fresh resident session on this connection.
/// Implementations must not capture the owning client; use the request-scoped
/// client argument to avoid a reference cycle. Async closures are supported.
#[async_trait]
pub trait AhpSessionFactory: Send + Sync + 'static {
    /// Materialize the host-selected session, preserving its configuration.
    async fn create_session(
        &self,
        request: AhpSessionRequest,
        client: Client,
    ) -> Result<Arc<Session>, Error>;
}

#[async_trait]
impl<F, Fut> AhpSessionFactory for F
where
    F: Fn(AhpSessionRequest, Client) -> Fut + Send + Sync + 'static,
    Fut: Future<Output = Result<Arc<Session>, Error>> + Send,
{
    async fn create_session(
        &self,
        request: AhpSessionRequest,
        client: Client,
    ) -> Result<Arc<Session>, Error> {
        self(request, client).await
    }
}

/// Receives the exact original session once per ended handoff.
///
/// Runs on a blocking worker, or synchronously if that worker is cancelled during
/// runtime shutdown, outside SDK locks; panics are caught and logged. The SDK never
/// calls `destroy` or `disconnect` on release. Retain an application [`Arc`] if
/// the session should outlive the handoff, or perform cleanup here explicitly.
/// To choose asynchronous cleanup, spawn a task that owns the released `Arc`:
///
/// ```no_run
/// use github_copilot_sdk::AhpHostOptions;
///
/// let options = AhpHostOptions::new().with_on_session_released(|original| {
///     tokio::spawn(async move {
///         if let Err(error) = original.disconnect().await {
///             eprintln!("application session cleanup failed: {error}");
///         }
///     });
/// });
/// ```
///
/// The closure is synchronous. Spawned cleanup is application-owned; host
/// disposal does not await it. Cleanup that needs a runtime must account for
/// the originating runtime already being shut down.
pub type AhpSessionReleasedCallback = Arc<dyn Fn(Arc<Session>) + Send + Sync>;

/// Options for [`Client::start_ahp_host`].
///
/// **Experimental.** May change or be removed in future releases.
/// At least one transport must be explicitly enabled; both may be used.
/// Transport defaults and validation belong to the runtime.
#[derive(Clone, Default)]
#[non_exhaustive]
pub struct AhpHostOptions {
    /// Stable durable catalog identity; defaults are runtime-owned.
    /// Must agree with `github_environment.compute_id` when both are supplied.
    pub compute_id: Option<String>,
    /// Experimental local WebSocket listener configuration.
    pub local_server: Option<crate::rpc::HostLocalServerOptions>,
    /// Experimental Mission Control registration; name and compute ID are required.
    pub github_environment: Option<crate::rpc::HostGitHubEnvironmentOptions>,
    /// Local callback, never serialized. Called at most once.
    ///
    /// Disconnect reports `OwnerDisconnected` without claiming the host was
    /// cleaned up. Already-received runtime exit notifications take precedence.
    /// `Exited` reports hosting-task failure, not process death; `exit_code` is `None`.
    /// Panics are caught and logged, as for other SDK notification callbacks.
    /// Do not capture the owning `Client` (or a session holding its connection):
    /// that creates a reference cycle. Forward the exit to application code
    /// through a channel instead.
    pub on_exit: Option<AhpHostExitCallback>,
    /// Optional application session factory, never serialized.
    pub create_session: Option<Arc<dyn AhpSessionFactory>>,
    /// Optional application resume callback, never serialized.
    pub resume_session: Option<Arc<dyn AhpSessionResumeFactory>>,
    /// Optional release notification for the original application session.
    pub on_session_released: Option<AhpSessionReleasedCallback>,
}

impl std::fmt::Debug for AhpHostOptions {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("AhpHostOptions")
            .field("compute_id", &self.compute_id)
            .field(
                "local_server",
                &self.local_server.as_ref().map(|_| "[configured]"),
            )
            .field("github_environment", &self.github_environment)
            .field("on_exit", &self.on_exit.is_some())
            .field("create_session", &self.create_session.is_some())
            .field("resume_session", &self.resume_session.is_some())
            .field("on_session_released", &self.on_session_released.is_some())
            .finish()
    }
}

impl AhpHostOptions {
    /// Create options with no transports enabled. Select at least one before starting.
    pub fn new() -> Self {
        Self::default()
    }

    /// Select the same durable catalog for local and Mission Control hosting.
    pub fn with_compute_id(mut self, compute_id: impl Into<String>) -> Self {
        self.compute_id = Some(compute_id.into());
        self
    }

    /// Enable the local WebSocket listener. Experimental; may change or be removed.
    pub fn with_local_server(mut self, options: crate::rpc::HostLocalServerOptions) -> Self {
        self.local_server = Some(options);
        self
    }

    /// Enable Mission Control registration. Experimental; may change or be removed.
    pub fn with_github_environment(
        mut self,
        options: crate::rpc::HostGitHubEnvironmentOptions,
    ) -> Self {
        self.github_environment = Some(options);
        self
    }

    /// Set the local exit callback.
    ///
    /// Do not capture the owning client or its sessions; use a channel to notify
    /// application code without keeping the connection alive.
    pub fn with_on_exit(mut self, callback: impl Fn(AhpHostExit) + Send + Sync + 'static) -> Self {
        self.on_exit = Some(Arc::new(callback));
        self
    }

    /// Create application-owned sessions using a factory or async closure.
    ///
    /// Use the supplied client rather than capturing this host's owner.
    ///
    /// ```no_run
    /// use std::sync::Arc;
    /// use github_copilot_sdk::{AhpHostOptions, AhpSessionRequest, Client};
    ///
    /// # async fn example(owner: &Client) -> Result<(), github_copilot_sdk::Error> {
    /// let host = owner.start_ahp_host(
    ///     AhpHostOptions::new()
    ///         .with_local_server(Default::default())
    ///         .with_create_session(|request: AhpSessionRequest, client: Client| async move {
    ///             // Add application tools and hooks without changing host settings.
    ///             Ok(Arc::new(client.create_session(request.config).await?))
    ///         })
    ///         .with_on_session_released(|session| println!("released {}", session.id())),
    /// ).await?;
    /// host.dispose().await?;
    /// # Ok(())
    /// # }
    /// ```
    pub fn with_create_session(mut self, factory: impl AhpSessionFactory) -> Self {
        self.create_session = Some(Arc::new(factory));
        self
    }

    /// Resume durable app-owned AHP sessions with a factory or async closure.
    ///
    /// Use the request-scoped client and return the original `Arc<Session>`,
    /// or return an existing attached original without reconfiguring it.
    /// Published resident sessions attach directly, without calling this factory.
    pub fn with_resume_session(mut self, factory: impl AhpSessionResumeFactory) -> Self {
        self.resume_session = Some(Arc::new(factory));
        self
    }

    /// Receive the original session when its AHP participation ends.
    ///
    /// This does not end the application's own participation.
    pub fn with_on_session_released(
        mut self,
        callback: impl Fn(Arc<Session>) + Send + Sync + 'static,
    ) -> Self {
        self.on_session_released = Some(Arc::new(callback));
        self
    }
}

/// A small handle to a runtime-owned AHP listener.
///
/// **Experimental.** May change or be removed in future releases.
/// The owning [`Client`] connection controls the listener's lifetime. This
/// handle does not keep the client alive. Dropping it performs no RPC or
/// background cleanup; call [`dispose`](Self::dispose) explicitly instead.
/// Dropping the last owning client handle disposes callback-backed hosts;
/// application-retained sessions still own their ordinary SDK connection.
#[derive(Clone)]
#[non_exhaustive]
pub struct AhpHost {
    /// Runtime host identifier.
    pub host_id: String,
    /// Local listener URL, absent for Mission Control-only hosts.
    pub url: Option<String>,
    /// Experimental Mission Control environment identifier, when registered.
    pub environment_id: Option<String>,
    /// Legacy separate host process ID. Absent for in-process listeners; use `dispose()` to stop.
    pub pid: Option<i64>,
    /// Connection token, absent when token authentication is disabled.
    pub token: Option<String>,
    client: Weak<ClientInner>,
}

impl std::fmt::Debug for AhpHost {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("AhpHost")
            .field("host_id", &self.host_id)
            .field("url", &self.url)
            .field("environment_id", &self.environment_id)
            .field("pid", &self.pid)
            .field("token", &self.token.as_ref().map(|_| "[redacted]"))
            .finish_non_exhaustive()
    }
}

impl AhpHost {
    /// Durably advertise an attached application session in the compute-scoped catalog.
    pub async fn publish_session(
        &self,
        session_id: impl Into<crate::SessionId>,
    ) -> Result<HostPublishSessionResult, Error> {
        let client = Client::from_inner(self.client.upgrade().ok_or_else(|| {
            Error::from(ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled))
        })?);
        client
            .rpc()
            .host()
            .publish_session(HostPublishSessionRequest {
                host_id: self.host_id.clone(),
                session_id: session_id.into(),
            })
            .await
    }

    /// List all live and dormant catalog sessions advertised by this host.
    pub async fn list_sessions(&self) -> Result<HostListSessionsResult, Error> {
        let client = Client::from_inner(self.client.upgrade().ok_or_else(|| {
            Error::from(ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled))
        })?);
        client
            .rpc()
            .host()
            .list_sessions(HostListSessionsRequest {
                host_id: Some(self.host_id.clone()),
            })
            .await
    }

    /// Forward `host.dispose` to the runtime.
    ///
    /// Every call makes its own RPC, including repeated and concurrent calls.
    /// Results and errors come from the runtime; no result is cached and no
    /// exit notification is synthesized.
    pub async fn dispose(&self) -> Result<(), Error> {
        let client = Client::from_inner(self.client.upgrade().ok_or_else(|| {
            Error::from(ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled))
        })?);
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

fn invoke_exit_callback(callback: AhpHostExitCallback, exit: AhpHostExit) {
    tokio::task::spawn_blocking(move || {
        if std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| callback(exit))).is_err() {
            warn!("host.exited callback panicked; continuing notification routing");
        }
    });
}

// Keep the committed start response observable after caller cancellation, so a
// successful late start can be disposed rather than becoming an orphan.
struct PendingStart {
    callbacks: Arc<ExitCallbacks>,
    sessions: Arc<HostSessions>,
    host_id: Option<String>,
    start: Option<tokio::task::JoinHandle<Result<HostStartResult, Error>>>,
    client: Weak<ClientInner>,
}

impl Drop for PendingStart {
    fn drop(&mut self) {
        if let Some(host_id) = &self.host_id {
            let callback = self.callbacks.lock().remove(host_id);
            drop(callback);
            self.sessions.release_host(host_id);
        }
        if let Some(start) = self.start.take() {
            let client = self.client.clone();
            self.sessions.2.spawn(async move {
                if let Ok(Ok(result)) = start.await
                    && let Some(inner) = client.upgrade()
                {
                    let client = Client::from_inner(inner);
                    if let Err(error) = client
                        .rpc()
                        .host()
                        .dispose(HostDisposeRequest {
                            host_id: result.host_id,
                        })
                        .await
                    {
                        warn!(%error, "cancelled AHP start cleanup failed");
                    }
                }
            });
        }
    }
}

impl Client {
    pub(crate) fn host_sessions(&self) -> Result<Arc<HostSessions>, Error> {
        self.ahp_host_sessions
            .clone()
            .or_else(|| self.inner.ahp_host_sessions.upgrade())
            .ok_or_else(|| handoff_error("AHP owning client has been dropped"))
    }

    /// Start a runtime-owned AHP listener through the generated `host.start` RPC.
    ///
    /// **Experimental.** May change or be removed in future releases.
    /// The runtime supplies listener defaults and validates all listener options.
    /// A local exit callback is registered before sending the request, so it can
    /// receive an exit that arrives before the start response.
    /// Cancelling this future removes local callbacks and disposes a successfully
    /// started listener once the pending start response arrives.
    pub async fn start_ahp_host(&self, options: AhpHostOptions) -> Result<AhpHost, Error> {
        if options.local_server.is_none() && options.github_environment.is_none() {
            return Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "AHP hosting requires localServer or githubEnvironment",
            ));
        }
        let host_id = uuid::Uuid::new_v4().to_string();
        let mut pending = PendingStart {
            callbacks: self.inner.ahp_host_callbacks.clone(),
            sessions: self.host_sessions()?,
            host_id: Some(host_id.clone()),
            start: None,
            client: Arc::downgrade(&self.inner),
        };
        if let Some(callback) = options.on_exit {
            pending.callbacks.lock().insert(host_id.clone(), callback);
        }
        let session_factory = options.create_session.as_ref().map(|_| true);
        let resume_factory = options.resume_session.as_ref().map(|_| true);
        if session_factory.is_some() || resume_factory.is_some() {
            pending.sessions.0.lock().factories.insert(
                host_id.clone(),
                FactoryRegistration {
                    create: options.create_session,
                    resume: options.resume_session,
                    on_released: options.on_session_released,
                },
            );
        }
        let mut start_options = HostStartOptions::new(host_id);
        if let Some(compute_id) = options.compute_id {
            start_options = start_options.compute_id(compute_id);
        }
        if let Some(local_server) = options.local_server {
            start_options = start_options.local_server(local_server);
        }
        if let Some(github_environment) = options.github_environment {
            start_options = start_options.github_environment(github_environment);
        }
        if let Some(session_factory) = session_factory {
            start_options = start_options.session_factory(session_factory);
        }
        if let Some(resume_factory) = resume_factory {
            start_options = start_options.resume_factory(resume_factory);
        }
        let request = self.inner.rpc.send_request_with_inline_callback(
            "host.start",
            Some(serde_json::to_value(start_options)?),
            None,
        );
        pending.start = Some(tokio::spawn(async move {
            let response = request.await?;
            if let Some(error) = response.error {
                return Err(Error::from_rpc(error.code, error.message, error.data));
            }
            Ok(serde_json::from_value(
                response.result.unwrap_or(Value::Null),
            )?)
        }));
        let result = pending
            .start
            .as_mut()
            .expect("start task was just registered")
            .await;
        pending.start = None;
        let result = result.map_err(|_| handoff_error("AHP host start task failed"))??;
        pending.host_id = None;
        Ok(AhpHost {
            host_id: result.host_id,
            url: result.url,
            environment_id: result.environment_id,
            pid: result.pid,
            token: result.token,
            client: Arc::downgrade(&self.inner),
        })
    }

    pub(crate) fn spawn_ahp_host_dispatcher(
        &self,
        mut notifications: mpsc::UnboundedReceiver<crate::jsonrpc::JsonRpcNotification>,
    ) {
        let callbacks = self.inner.ahp_host_callbacks.clone();
        let sessions = self.inner.ahp_host_sessions.clone();
        let closed = self.inner.rpc.connection_closed_token();
        tokio::spawn(async move {
            loop {
                let notification = tokio::select! {
                    // Drain received exits before reporting transport loss.
                    biased;
                    notification = notifications.recv() => notification,
                    _ = closed.cancelled() => break,
                };
                match notification {
                    Some(notification) if notification.method == "host.exited" => {
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
                        if let Some(sessions) = sessions.upgrade() {
                            sessions.release_host(&exit.host_id);
                        }
                        let callback = callbacks.lock().remove(&exit.host_id);
                        if let Some(callback) = callback {
                            invoke_exit_callback(callback, exit);
                        }
                    }
                    Some(notification) if notification.method == "host.sessionReleased" => {
                        if let Some(params) = notification.params
                            && let Ok(released) =
                                serde_json::from_value::<HostSessionReleasedNotification>(params)
                            && let Some(sessions) = sessions.upgrade()
                        {
                            sessions.release(&released.host_id, &released.handoff_id);
                        }
                    }
                    Some(_) => {}
                    None => break,
                }
            }
            if let Some(sessions) = sessions.upgrade() {
                sessions.release_all();
            }
            let callbacks = std::mem::take(&mut *callbacks.lock());
            for (host_id, callback) in callbacks {
                invoke_exit_callback(
                    callback,
                    AhpHostExit {
                        host_id,
                        reason: AhpHostExitReason::OwnerDisconnected,
                        exit_code: None,
                        error: Some(
                            "Owner connection closed; runtime cleanup cannot be acknowledged on this connection."
                                .to_owned(),
                        ),
                    },
                );
            }
        });
    }
}

#[derive(Clone)]
struct FactoryRegistration {
    create: Option<Arc<dyn AhpSessionFactory>>,
    resume: Option<Arc<dyn AhpSessionResumeFactory>>,
    on_released: Option<AhpSessionReleasedCallback>,
}

#[derive(Default)]
struct HostSessionState {
    factories: HashMap<String, FactoryRegistration>,
    handoffs: HashMap<String, Arc<Handoff>>,
}

impl Drop for HostSessionState {
    fn drop(&mut self) {
        // Also runs when owner-drop cleanup is cancelled before its first poll.
        for handoff in self.handoffs.values() {
            handoff.release();
        }
    }
}

pub(crate) struct HostSessions(
    Mutex<HostSessionState>,
    Mutex<Weak<ClientInner>>,
    tokio::runtime::Handle,
);

impl Drop for HostSessions {
    fn drop(&mut self) {
        let state = std::mem::take(self.0.get_mut());
        for handoff in state.handoffs.values() {
            handoff.cancellation.cancel();
        }
        // Only the public Client handles own this retention root. Session clients
        // own the connection without pointing back here. A finite teardown task
        // keeps originals alive until their hosts detach, without a reference cycle.
        if !state.factories.is_empty()
            && let Some(inner) = self.1.get_mut().upgrade()
        {
            self.2.spawn(async move {
                let client = Client::from_inner(inner);
                for host_id in state.factories.keys() {
                    let disposal = async {
                        client
                            .rpc()
                            .host()
                            .dispose(HostDisposeRequest {
                                host_id: host_id.clone(),
                            })
                            .await
                    };
                    if !matches!(
                        tokio::time::timeout(std::time::Duration::from_secs(10), disposal).await,
                        Ok(Ok(_))
                    ) {
                        warn!("AHP owner-drop host cleanup failed");
                    }
                }
                drop(state);
            });
        }
    }
}

struct Handoff {
    host_id: String,
    requested_session_id: String,
    cancellation: CancellationToken,
    on_released: Option<AhpSessionReleasedCallback>,
    session: Mutex<Option<Arc<Session>>>,
    runtime: tokio::runtime::Handle,
}

// Dropping a cancelled blocking task must still deliver its release notification.
struct ReleaseNotification {
    callback: AhpSessionReleasedCallback,
    session: Option<Arc<Session>>,
}

impl Drop for ReleaseNotification {
    fn drop(&mut self) {
        if let Some(session) = self.session.take()
            && std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| (self.callback)(session)))
                .is_err()
        {
            warn!("AHP session release callback panicked");
        }
    }
}

impl Handoff {
    fn notify_released(&self, session: Arc<Session>) {
        if let Some(callback) = self.on_released.clone() {
            let notification = ReleaseNotification {
                callback,
                session: Some(session),
            };
            self.runtime.spawn_blocking(move || drop(notification));
        }
    }

    fn release(&self) {
        let session = {
            let mut session = self.session.lock();
            self.cancellation.cancel();
            session.take()
        };
        if let Some(session) = session {
            self.notify_released(session);
        }
    }

    fn retain(&self, session: Arc<Session>) -> Result<(), Error> {
        {
            let mut retained = self.session.lock();
            if !self.cancellation.is_cancelled() {
                *retained = Some(session);
                return Ok(());
            }
        }
        self.notify_released(session);
        Err(handoff_error("AHP session handoff ended"))
    }
}

impl HostSessions {
    pub(crate) fn new() -> Self {
        Self(
            Mutex::default(),
            Mutex::default(),
            tokio::runtime::Handle::current(),
        )
    }

    pub(crate) fn set_owner(&self, owner: Weak<ClientInner>) {
        *self.1.lock() = owner;
    }

    fn release(&self, host_id: &str, handoff_id: &str) {
        let handoff = {
            let mut state = self.0.lock();
            if !state.factories.contains_key(host_id)
                || state
                    .handoffs
                    .get(handoff_id)
                    .is_some_and(|h| h.host_id != host_id)
            {
                return;
            }
            state.handoffs.remove(handoff_id)
        };
        if let Some(handoff) = handoff {
            handoff.release();
        }
    }

    fn release_host(&self, host_id: &str) {
        let (factory, handoffs) = {
            let mut state = self.0.lock();
            let factory = state.factories.remove(host_id);
            let ids: Vec<_> = state
                .handoffs
                .iter()
                .filter(|(_, h)| h.host_id == host_id)
                .map(|(id, _)| id.clone())
                .collect();
            let handoffs: Vec<_> = ids
                .iter()
                .filter_map(|id| state.handoffs.remove(id))
                .collect();
            (factory, handoffs)
        };
        drop(factory);
        for handoff in handoffs {
            handoff.release();
        }
    }

    fn release_all(&self) {
        let state = std::mem::take(&mut *self.0.lock());
        drop(state);
    }

    pub(crate) fn expects_session(&self, id: Option<&crate::SessionId>) -> bool {
        id.is_some_and(|id| {
            self.0
                .lock()
                .handoffs
                .values()
                .any(|h| h.requested_session_id == id.as_str())
        })
    }
}

fn handoff_error(message: &'static str) -> Error {
    Error::with_message(ErrorKind::InvalidConfig, message)
}

impl Client {
    pub(crate) fn register_ahp_session_factory(&self) -> Result<(), Error> {
        let client = Arc::downgrade(&self.inner);
        self.inner.rpc.register_request_handler(
            "host.materializeSession",
            Arc::new(move |params| {
                // Register before the reader advances to a release notification,
                // not when the spawned application task gets its first poll.
                let prepared = (|| {
                    let inner = client
                        .upgrade()
                        .ok_or_else(|| handoff_error("AHP owner connection is closed"))?;
                    let client = Client::from_inner(inner);
                    let params = serde_json::from_value(params)?;
                    let (factory, handoff) = client.prepare_ahp_session(&params)?;
                    Ok::<_, Error>((client, params, factory, handoff))
                })();
                Box::pin(async move {
                    let (client, params, factory, handoff) = prepared?;
                    client
                        .materialize_ahp_session(params, factory, handoff)
                        .await
                })
            }),
        )
    }

    fn prepare_ahp_session(
        &self,
        params: &HostSessionCreateCallback,
    ) -> Result<(FactoryRegistration, Arc<Handoff>), Error> {
        let resume = params.resume.unwrap_or(false);
        let requested_session_id = params
            .config
            .get("sessionId")
            .and_then(Value::as_str)
            .ok_or_else(|| handoff_error("AHP session configuration requires sessionId"))?
            .to_owned();
        let (factory, handoff) = {
            let sessions = self.host_sessions()?;
            let mut state = sessions.0.lock();
            let factory = state
                .factories
                .get(&params.host_id)
                .cloned()
                .ok_or_else(|| handoff_error("AHP session factory is unavailable"))?;
            if (resume && factory.resume.is_none()) || (!resume && factory.create.is_none()) {
                return Err(handoff_error("AHP session factory is unavailable"));
            }
            if state.handoffs.contains_key(&params.handoff_id) {
                return Err(handoff_error("AHP session handoff already exists"));
            }
            let handoff = Arc::new(Handoff {
                host_id: params.host_id.clone(),
                requested_session_id,
                cancellation: CancellationToken::new(),
                on_released: factory.on_released.clone(),
                session: Mutex::new(None),
                runtime: sessions.2.clone(),
            });
            state
                .handoffs
                .insert(params.handoff_id.clone(), handoff.clone());
            (factory, handoff)
        };
        Ok((factory, handoff))
    }

    async fn materialize_ahp_session(
        &self,
        params: HostSessionCreateCallback,
        factory: FactoryRegistration,
        handoff: Arc<Handoff>,
    ) -> Result<Value, Error> {
        if handoff.cancellation.is_cancelled() {
            return Err(handoff_error("AHP session handoff ended"));
        }
        let resume = params.resume.unwrap_or(false);
        let cancellation_token = handoff.cancellation.child_token();
        let client = self.clone();
        let entry = handoff.clone();
        // Do not drop the application future when cancellation wins: its eventual
        // original session must still be delivered to on_session_released.
        let work = tokio::spawn(async move {
            let result = std::panic::AssertUnwindSafe(async {
                if resume {
                    let request = AhpSessionResumeRequest {
                        config: resume_config_from_host(&params.config)?,
                        cancellation_token,
                    };
                    factory
                        .resume
                        .as_ref()
                        .ok_or_else(|| handoff_error("AHP resume factory is unavailable"))?
                        .resume_session(request, client.clone())
                        .await
                } else {
                    let request = AhpSessionRequest {
                        config: config_from_host(&params.config)?,
                        cancellation_token,
                    };
                    factory
                        .create
                        .as_ref()
                        .ok_or_else(|| handoff_error("AHP create factory is unavailable"))?
                        .create_session(request, client.clone())
                        .await
                }
            })
            .catch_unwind()
            .await
            .map_err(|_| handoff_error("AHP session factory panicked"))?;
            let session = result?;
            entry.retain(session.clone())?;
            session.validate_ahp_handoff(&client, &params.config, resume)?;
            Ok(serde_json::json!({"sessionId": session.id()}))
        });
        let result = tokio::select! {
            biased;
            _ = handoff.cancellation.cancelled() => Err(handoff_error("AHP session handoff ended")),
            result = work => result.unwrap_or_else(|_| Err(handoff_error("AHP session factory task failed"))),
        };
        if result.is_err()
            && let Some(sessions) = self.inner.ahp_host_sessions.upgrade()
        {
            sessions.release(&params.host_id, &params.handoff_id);
        }
        result
    }
}

// These are SDK config names, not session.create wire names (notably
// enableMcpApps and the options.update settings). Reject new unknown settings
// rather than silently losing a host-selected constraint.
macro_rules! host_settings {
    ($apply:ident) => {
        $apply! {
            "workingDirectory" => working_directory,
            "configDir" => config_directory,
            "enableExperimentalMode" => enable_experimental_mode,
            "infiniteSessions" => infinite_sessions,
            "additionalDirectories" => additional_directories,
            "streaming" => streaming,
            "gitHubToken" => github_token,
            "mcpOAuthTokenStorage" => mcp_oauth_token_storage,
            "enableConfigDiscovery" => enable_config_discovery,
            "featureFlags" => feature_flags,
            "enableManagedSettings" => enable_managed_settings,
            "enableSessionStore" => enable_session_store,
            "availableTools" => available_tools,
            "excludedTools" => excluded_tools,
            "allowedModels" => allowed_models,
            "systemMessage" => system_message,
            "skipCustomInstructions" => skip_custom_instructions,
            "customAgentsLocalOnly" => custom_agents_local_only,
            "coauthorEnabled" => coauthor_enabled,
            "manageScheduleEnabled" => manage_schedule_enabled,
            "memory" => memory,
            "pluginDirectories" => plugin_directories,
            "skillDirectories" => skill_directories,
            "instructionDirectories" => instruction_directories,
            "enableMcpApps" => enable_mcp_apps,
            "model" => model,
            "reasoningEffort" => reasoning_effort,
            "contextTier" => context_tier,
        }
    };
}

fn config_from_host(settings: &HashMap<String, Value>) -> Result<SessionConfig, Error> {
    let mut config = SessionConfig::default();
    macro_rules! decode {
                    ($($name:literal => $field:ident,)*) => {
                        for (key, value) in settings {
                            match key.as_str() {
                                "sessionId" => config.session_id = serde_json::from_value(value.clone())?,
                                "refreshCustomInstructions" => config.refresh_custom_instructions = serde_json::from_value(value.clone())?,
                                $($name => config.$field = serde_json::from_value(value.clone())?,)*
                                _ => return Err(handoff_error("Unsupported AHP session configuration setting")),
                            }
                        }
                    };
                }
    host_settings!(decode);
    Ok(config)
}

pub(crate) fn config_for_host(config: &SessionConfig) -> Result<Value, Error> {
    let mut settings = serde_json::Map::new();
    if let Some(id) = &config.session_id {
        settings.insert("sessionId".into(), serde_json::to_value(id)?);
    }
    if let Some(refresh) = config.refresh_custom_instructions {
        settings.insert("refreshCustomInstructions".into(), Value::Bool(refresh));
    }
    macro_rules! encode {
                    ($($name:literal => $field:ident,)*) => {
                        $(if let Some(value) = &config.$field {
                            settings.insert($name.to_owned(), serde_json::to_value(value)?);
                        })*
                    };
                }
    host_settings!(encode);
    Ok(Value::Object(settings))
}

fn resume_config_from_host(
    settings: &HashMap<String, Value>,
) -> Result<ResumeSessionConfig, Error> {
    let id = settings
        .get("sessionId")
        .and_then(Value::as_str)
        .ok_or_else(|| handoff_error("AHP resume requires sessionId"))?;
    let mut config = ResumeSessionConfig::new(id.into());
    macro_rules! decode {
        ($($name:literal => $field:ident,)*) => {
            for (key, value) in settings {
                match key.as_str() {
                    "sessionId" => {},
                    "continuePendingWork" => config.continue_pending_work = serde_json::from_value(value.clone())?,
                    "suppressResumeEvent" => config.suppress_resume_event = serde_json::from_value(value.clone())?,
                    "allowTranscriptRecovery" => config.allow_transcript_recovery = serde_json::from_value(value.clone())?,
                    $($name => config.$field = serde_json::from_value(value.clone())?,)*
                    _ => return Err(handoff_error("Unsupported AHP resume configuration setting")),
                }
            }
        };
    }
    host_settings!(decode);
    Ok(config)
}

pub(crate) fn resume_config_for_host(config: &ResumeSessionConfig) -> Result<Value, Error> {
    let mut settings = serde_json::Map::new();
    settings.insert(
        "sessionId".into(),
        serde_json::to_value(&config.session_id)?,
    );
    macro_rules! encode {
        ($($name:literal => $field:ident,)*) => {
            $(if let Some(value) = &config.$field {
                settings.insert($name.to_owned(), serde_json::to_value(value)?);
            })*
        };
    }
    host_settings!(encode);
    if let Some(value) = config.continue_pending_work {
        settings.insert("continuePendingWork".into(), Value::Bool(value));
    }
    if let Some(value) = config.suppress_resume_event {
        settings.insert("suppressResumeEvent".into(), Value::Bool(value));
    }
    if let Some(value) = config.allow_transcript_recovery {
        settings.insert("allowTranscriptRecovery".into(), Value::Bool(value));
    }
    Ok(Value::Object(settings))
}

pub(crate) fn contains_settings(actual: Option<&Value>, expected: &Value) -> bool {
    if let Some(expected) = expected.as_object() {
        actual.and_then(Value::as_object).is_some_and(|actual| {
            expected
                .iter()
                .all(|(key, value)| contains_settings(actual.get(key), value))
        })
    } else {
        actual == Some(expected)
    }
}
#[cfg(test)]
mod tests;
