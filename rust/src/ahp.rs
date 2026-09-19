//! Transport-neutral native Agent Host Protocol endpoints.
//!
//! Applications own the public listener and forward opaque AHP 0.9 messages.
//! The runtime owns the agent mapping. An output callback must finish only after
//! transport delivery: its return value is the runtime's delivery acknowledgement.
//! This experimental API requires a runtime implementing the native `ahp.*` RPCs.

use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Weak};

use futures_util::FutureExt;
use futures_util::future::{BoxFuture, Shared};
use parking_lot::Mutex;
use serde_json::Value;
use tokio::sync::{Mutex as AsyncMutex, OwnedSemaphorePermit, Semaphore, oneshot};
use tokio_util::sync::CancellationToken;
use wire::rpc_methods;

use crate::generated::api_types as wire;
use crate::jsonrpc::JsonRpcMessage;
use crate::{
    Client, ClientInner, Error, ErrorKind, JsonRpcError, JsonRpcRequest, JsonRpcResponse, Result,
};

tokio::task_local! {
    static IN_CALLBACK: ();
}

/// Cancellation of an application policy callback.
#[derive(Clone)]
pub struct AhpCallbackContext {
    /// Cancelled when the request, logical connection, endpoint, or client closes.
    pub cancellation: CancellationToken,
}

/// Identity returned by application session policy.
#[derive(Clone, Debug)]
pub struct AhpSessionIdentity {
    /// ID of the ordinary SDK session to expose.
    pub session_id: String,
}

/// Native request to create an application-owned session.
#[derive(Clone, Debug)]
pub struct AhpCreateSessionRequest {
    /// Logical AHP connection requesting creation.
    pub connection_id: String,
    /// Runtime-validated ID to use when creating the SDK session.
    pub requested_session_id: String,
    /// Requested workspace, subject to application policy.
    pub working_directory: Option<String>,
    /// Requested model, subject to application policy.
    pub model: Option<String>,
    /// Opaque AHP configuration.
    pub config: Option<Value>,
}

/// Native request to resume an application-owned session.
#[derive(Clone, Debug)]
pub struct AhpResumeSessionRequest {
    /// Logical AHP connection requesting resume.
    pub connection_id: String,
    /// ID of the session to resume.
    pub session_id: String,
}

/// Application-owned session control request.
#[derive(Clone, Debug)]
pub struct AhpSessionControlRequest {
    /// Target SDK session.
    pub session_id: String,
    /// Native control operation.
    pub kind: String,
    /// Opaque operation arguments.
    pub payload: Value,
}

/// Outcome of an application-owned control operation.
#[derive(Clone, Debug, Default)]
pub struct AhpSessionControlResult {
    /// Whether the requested change was applied.
    pub applied: bool,
    /// Explanation of a refusal.
    pub reason: Option<String>,
    /// Explicit operation result, including outcomes that refuse a change.
    pub result: Option<Value>,
}

/// Asynchronous application policy callback. Ordinary SDK calls are reentrant;
/// recursive AHP endpoint operations from a policy callback are rejected.
pub type AhpCallback<T, R> =
    Arc<dyn Fn(T, AhpCallbackContext) -> BoxFuture<'static, Result<R>> + Send + Sync>;

/// Per-direction bounds, including messages waiting for and undergoing delivery.
#[derive(Clone, Copy, Debug)]
pub struct AhpLimits {
    /// Maximum UTF-8 message size; at most 1 MiB.
    pub max_message_bytes: usize,
    /// Maximum queued and in-flight messages; at most 128.
    pub max_queued_messages: usize,
    /// Maximum queued and in-flight UTF-8 bytes; at most 8 MiB.
    pub max_buffered_bytes: usize,
}

impl Default for AhpLimits {
    fn default() -> Self {
        Self {
            max_message_bytes: 1024 * 1024,
            max_queued_messages: 128,
            max_buffered_bytes: 8 * 1024 * 1024,
        }
    }
}

impl AhpLimits {
    fn validate(self) -> Result<()> {
        let max = Self::default();
        if self.max_message_bytes == 0
            || self.max_message_bytes > max.max_message_bytes
            || self.max_queued_messages == 0
            || self.max_queued_messages > max.max_queued_messages
            || self.max_buffered_bytes == 0
            || self.max_buffered_bytes > max.max_buffered_bytes
        {
            return Err(invalid(
                "AHP limits must be positive and cannot exceed native limits",
            ));
        }
        Ok(())
    }
}

/// Endpoint-local session policy. Omitted callbacks use runtime defaults.
#[derive(Default)]
pub struct AhpEndpointOptions {
    /// Whether clients may create sessions.
    pub allow_session_creation: Option<bool>,
    /// Opaque root capabilities.
    pub capabilities: Option<Value>,
    /// Create an ordinary SDK session and return its identity.
    pub on_create_session: Option<AhpCallback<AhpCreateSessionRequest, AhpSessionIdentity>>,
    /// Resume an ordinary SDK session and return its identity.
    pub on_resume_session: Option<AhpCallback<AhpResumeSessionRequest, AhpSessionIdentity>>,
    /// Authorize the endpoint's session set for all AHP operations.
    pub on_list_sessions: Option<AhpCallback<(), Vec<AhpSessionIdentity>>>,
    /// Handle application-owned controls such as disposal.
    pub on_session_control: Option<AhpCallback<AhpSessionControlRequest, AhpSessionControlResult>>,
    /// Optional tighter bounds than the runtime defaults.
    pub limits: AhpLimits,
}

/// Application transport callbacks for one independent logical connection.
pub struct AhpConnectionOptions {
    /// Deliver one opaque message; resolve after physical delivery, not enqueueing.
    pub on_message: Arc<dyn Fn(String) -> BoxFuture<'static, Result<()>> + Send + Sync>,
    /// Terminal notification, with an error message on abnormal closure.
    pub on_close: Option<Arc<dyn Fn(Option<String>) + Send + Sync>>,
}

/// Cloneable native endpoint handle. Explicitly dispose it when its listener closes.
#[derive(Clone)]
pub struct AhpEndpoint(Arc<Endpoint>);

/// Cloneable logical connection. Closing it does not close its SDK session.
#[derive(Clone)]
pub struct AhpConnection(Arc<Connection>);

type Cleanup = Shared<BoxFuture<'static, std::result::Result<(), String>>>;

struct Endpoint {
    id: String,
    client: Weak<ClientInner>,
    options: AhpEndpointOptions,
    closed: CancellationToken,
    finished: AtomicBool,
    connections: Mutex<HashMap<String, Arc<Connection>>>,
    cleanup: Mutex<Option<Cleanup>>,
}

struct Connection {
    id: String,
    endpoint: Weak<Endpoint>,
    options: AhpConnectionOptions,
    closed: CancellationToken,
    finished: AtomicBool,
    input: Budget,
    output: Budget,
    sending: AsyncMutex<()>,
    delivery_tail: Mutex<Option<oneshot::Receiver<()>>>,
    cleanup: Mutex<Option<Cleanup>>,
}

struct Budget {
    messages: Arc<Semaphore>,
    bytes: Arc<Semaphore>,
    max_message_bytes: usize,
}

impl Budget {
    fn new(limits: AhpLimits) -> Self {
        Self {
            messages: Arc::new(Semaphore::new(limits.max_queued_messages)),
            bytes: Arc::new(Semaphore::new(limits.max_buffered_bytes)),
            max_message_bytes: limits.max_message_bytes,
        }
    }

    fn reserve(&self, message: &str) -> Result<(OwnedSemaphorePermit, OwnedSemaphorePermit)> {
        if message.len() > self.max_message_bytes {
            return Err(invalid("AHP message size limit exceeded"));
        }
        let count = self
            .messages
            .clone()
            .try_acquire_owned()
            .map_err(|_| invalid("AHP connection buffer limit exceeded"))?;
        let bytes = self
            .bytes
            .clone()
            .try_acquire_many_owned(message.len() as u32)
            .map_err(|_| invalid("AHP connection buffer limit exceeded"))?;
        Ok((count, bytes))
    }
}

fn invalid(message: impl Into<String>) -> Error {
    Error::with_message(ErrorKind::InvalidConfig, message.into())
}

fn closed() -> Error {
    invalid("AHP endpoint or connection is closed")
}

fn assert_not_callback() -> Result<()> {
    if IN_CALLBACK.try_with(|_| ()).is_ok() {
        return Err(invalid(
            "Recursive AHP endpoint operations from an AHP callback are not supported",
        ));
    }
    Ok(())
}

fn client(inner: &Weak<ClientInner>) -> Result<Client> {
    inner.upgrade().map(Client::from_inner).ok_or_else(closed)
}

fn check_response(response: JsonRpcResponse) -> Result<Value> {
    if let Some(error) = response.error {
        return Err(Error::with_message(
            ErrorKind::Rpc { code: error.code },
            if error.code == -32601 {
                "This runtime does not support native AHP endpoints. Use a matching runtime with ahp.* support.".into()
            } else {
                error.message
            },
        ));
    }
    Ok(response.result.unwrap_or(Value::Null))
}

impl Client {
    /// Register a transport-neutral AHP endpoint on this client's runtime.
    ///
    /// This does not start a listener or another runtime. The application owns
    /// transport authentication and endpoint-local session policy.
    pub async fn create_ahp_endpoint(&self, options: AhpEndpointOptions) -> Result<AhpEndpoint> {
        assert_not_callback()?;
        options.limits.validate()?;
        self.inner.router.ensure_started(
            &self.inner.notification_tx,
            &self.inner.request_rx,
            self.inner.extension_launch_provider.clone(),
            self.inner.llm_inference.get().cloned(),
            self.inner.on_github_telemetry.clone(),
            self.inner.github_token_registry.clone(),
        );
        let registry = self.inner.router.ahp.clone();
        *registry.client.lock() = Arc::downgrade(&self.inner);
        let weak_registry = Arc::downgrade(&registry);
        self.inner
            .rpc
            .set_server_message_handler(Box::new(move |message| {
                let Some(registry) = weak_registry.upgrade() else {
                    return false;
                };
                match message {
                    JsonRpcMessage::Request(request) if request.method.starts_with("ahp.") => {
                        registry.dispatch(request.clone());
                        true
                    }
                    JsonRpcMessage::Notification(notification)
                        if notification.method.starts_with("ahp.")
                            || notification.method == "$/cancelRequest" =>
                    {
                        registry.notification(
                            &notification.method,
                            notification.params.as_ref().unwrap_or(&Value::Null),
                        );
                        false
                    }
                    _ => false,
                }
            }));
        let params = serde_json::to_value(wire::AhpRegisterEndpointRequest {
            allow_session_creation: options.allow_session_creation,
            capabilities: options.capabilities.clone(),
            callbacks: wire::AhpEndpointCallbacks {
                create_session: options.on_create_session.is_some(),
                resume_session: options.on_resume_session.is_some(),
                list_sessions: options.on_list_sessions.is_some(),
                session_control: options.on_session_control.is_some(),
            },
            ..Default::default()
        })?;
        let registered = Arc::new(Mutex::new(None));
        let registered_inline = registered.clone();
        let weak_client = Arc::downgrade(&self.inner);
        let closed_token = self.inner.rpc.connection_closed_token();
        let client = self.clone();
        let (result_tx, result_rx) = oneshot::channel();
        tokio::spawn(async move {
            let result = async {
                let response = client
                    .inner
                    .rpc
                    .send_request_with_inline_callback(
                        rpc_methods::AHP_REGISTERENDPOINT,
                        Some(params),
                        Some(Box::new(move |response| {
                            if response.error.is_some() {
                                return Ok(());
                            }
                            let response: wire::AhpEndpointRef = serde_json::from_value(
                                response.result.clone().unwrap_or(Value::Null),
                            )?;
                            let id = response.endpoint_id;
                            let endpoint = Arc::new(Endpoint {
                                id: id.clone(),
                                client: weak_client,
                                options,
                                closed: closed_token,
                                finished: AtomicBool::new(false),
                                connections: Mutex::new(HashMap::new()),
                                cleanup: Mutex::new(None),
                            });
                            registry.endpoints.lock().insert(id, endpoint.clone());
                            let weak = Arc::downgrade(&endpoint);
                            let token = endpoint.closed.clone();
                            tokio::spawn(async move {
                                token.cancelled().await;
                                if let Some(endpoint) = weak.upgrade() {
                                    endpoint.finish(Some("AHP endpoint closed".into()));
                                }
                            });
                            *registered_inline.lock() = Some(AhpEndpoint(endpoint));
                            Ok(())
                        })),
                    )
                    .await?;
                check_response(response)?;
                registered.lock().take().ok_or_else(closed)
            }
            .await;
            if let Err(Ok(endpoint)) = result_tx.send(result)
                && let Err(error) = endpoint.dispose().await
            {
                tracing::warn!(%error, "Failed to dispose abandoned AHP endpoint");
            }
        });
        result_rx.await.map_err(|_| closed())?
    }
}

impl AhpEndpoint {
    /// Runtime-assigned endpoint ID.
    pub fn id(&self) -> &str {
        &self.0.id
    }

    /// Open a logical AHP connection with independent ordering and backpressure.
    pub async fn open_connection(&self, options: AhpConnectionOptions) -> Result<AhpConnection> {
        assert_not_callback()?;
        let endpoint = &self.0;
        if endpoint.closed.is_cancelled() {
            return Err(closed());
        }
        let registered = Arc::new(Mutex::new(None));
        let registered_inline = registered.clone();
        let endpoint = endpoint.clone();
        let (result_tx, result_rx) = oneshot::channel();
        tokio::spawn(async move {
            let result = async {
                let response = client(&endpoint.client)?
                    .inner
                    .rpc
                    .send_request_with_inline_callback(
                        rpc_methods::AHP_OPENCONNECTION,
                        Some(serde_json::to_value(wire::AhpEndpointRef {
                            endpoint_id: endpoint.id.clone(),
                        })?),
                        Some(Box::new(move |response| {
                            if response.error.is_some() {
                                return Ok(());
                            }
                            let response: wire::AhpOpenConnectionResult = serde_json::from_value(
                                response.result.clone().unwrap_or(Value::Null),
                            )?;
                            let id = response.connection_id;
                            let connection = Arc::new(Connection {
                                id: id.clone(),
                                endpoint: Arc::downgrade(&endpoint),
                                closed: endpoint.closed.child_token(),
                                finished: AtomicBool::new(false),
                                options,
                                input: Budget::new(endpoint.options.limits),
                                output: Budget::new(endpoint.options.limits),
                                sending: AsyncMutex::new(()),
                                delivery_tail: Mutex::new(None),
                                cleanup: Mutex::new(None),
                            });
                            endpoint.connections.lock().insert(id, connection.clone());
                            if endpoint.closed.is_cancelled() {
                                connection.finish(Some("AHP endpoint closed".into()));
                                return Err(closed());
                            }
                            *registered_inline.lock() = Some(AhpConnection(connection));
                            Ok(())
                        })),
                    )
                    .await?;
                check_response(response)?;
                registered.lock().take().ok_or_else(closed)
            }
            .await;
            if let Err(Ok(connection)) = result_tx.send(result)
                && let Err(error) = connection.close().await
            {
                tracing::warn!(%error, "Failed to close abandoned AHP connection");
            }
        });
        result_rx.await.map_err(|_| closed())?
    }

    /// Recompute and enforce the application-authorized session set.
    pub async fn refresh_exposure(&self) -> Result<()> {
        assert_not_callback()?;
        self.0
            .run(async {
                client(&self.0.client)?
                    .rpc()
                    .ahp()
                    .refresh_exposure(wire::AhpEndpointRef {
                        endpoint_id: self.0.id.clone(),
                    })
                    .await
            })
            .await?;
        Ok(())
    }

    /// Replace the opaque root capability catalogue.
    pub async fn set_capabilities(&self, capabilities: Value) -> Result<()> {
        assert_not_callback()?;
        self.0
            .run(async {
                client(&self.0.client)?
                    .rpc()
                    .ahp()
                    .set_capabilities(wire::AhpSetCapabilitiesRequest {
                        endpoint_id: self.0.id.clone(),
                        capabilities,
                    })
                    .await
            })
            .await?;
        Ok(())
    }

    /// Idempotently dispose the endpoint and cancel callbacks and deliveries.
    /// Application-owned SDK sessions remain alive.
    pub async fn dispose(&self) -> Result<()> {
        assert_not_callback()?;
        let cleanup = {
            let mut cleanup = self.0.cleanup.lock();
            if let Some(future) = &*cleanup {
                future.clone()
            } else {
                if self.0.closed.is_cancelled() {
                    return Ok(());
                }
                let client = client(&self.0.client)?;
                let id = self.0.id.clone();
                let future = async move {
                    client
                        .rpc()
                        .ahp()
                        .dispose_endpoint(wire::AhpEndpointRef { endpoint_id: id })
                        .await
                        .map(|_| ())
                        .map_err(|e| e.to_string())
                }
                .boxed()
                .shared();
                *cleanup = Some(future.clone());
                future
            }
        };
        self.0.finish(None);
        cleanup.await.map_err(invalid)
    }
}

impl Endpoint {
    async fn run<T>(&self, work: impl std::future::Future<Output = Result<T>>) -> Result<T> {
        if self.closed.is_cancelled() {
            return Err(closed());
        }
        tokio::select! {
            biased;
            _ = self.closed.cancelled() => Err(closed()),
            result = work => result,
        }
    }

    fn finish(&self, error: Option<String>) {
        if self.finished.swap(true, Ordering::SeqCst) {
            return;
        }
        self.closed.cancel();
        let connections = std::mem::take(&mut *self.connections.lock());
        for connection in connections.into_values() {
            connection.finish(error.clone());
        }
        if let Ok(client) = client(&self.client) {
            client.inner.router.ahp.endpoints.lock().remove(&self.id);
        }
    }
}

impl AhpConnection {
    /// Runtime-assigned logical connection ID.
    pub fn id(&self) -> &str {
        &self.0.id
    }

    /// Send an opaque UTF-8 message. Resolves on runtime admission, not an AHP response.
    pub async fn send(&self, message: impl Into<String>) -> Result<()> {
        assert_not_callback()?;
        let message = message.into();
        let connection = &self.0;
        if connection.closed.is_cancelled() {
            return Err(closed());
        }
        let permits = match connection.input.reserve(&message) {
            Ok(permits) => permits,
            Err(error) => {
                connection.fail(error.to_string());
                return Err(error);
            }
        };
        let result = async {
            let _permits = permits;
            let _guard = connection.sending.lock().await;
            let endpoint = connection.endpoint.upgrade().ok_or_else(closed)?;
            endpoint
                .run(async {
                    client(&endpoint.client)?
                        .rpc()
                        .ahp()
                        .send(wire::AhpMessage {
                            endpoint_id: endpoint.id.clone(),
                            connection_id: connection.id.clone(),
                            message,
                        })
                        .await
                })
                .await?;
            Ok(())
        };
        let result: Result<()> = tokio::select! {
            biased;
            _ = connection.closed.cancelled() => Err(closed()),
            result = result => result,
        };
        if let Err(error) = &result {
            connection.fail(error.to_string());
        }
        result
    }

    /// Idempotently close this connection without closing its SDK sessions.
    pub async fn close(&self) -> Result<()> {
        assert_not_callback()?;
        let cleanup = self.0.begin_close(None);
        match cleanup {
            Some(cleanup) => cleanup.await.map_err(invalid),
            None => Ok(()),
        }
    }
}

impl Connection {
    fn finish(&self, error: Option<String>) {
        if self.finished.swap(true, Ordering::SeqCst) {
            return;
        }
        self.endpoint
            .upgrade()
            .and_then(|endpoint| endpoint.connections.lock().remove(&self.id));
        self.closed.cancel();
        if let Some(callback) = &self.options.on_close
            && std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| callback(error))).is_err()
        {
            tracing::warn!("AHP on_close callback panicked");
        }
    }

    fn begin_close(&self, error: Option<String>) -> Option<Cleanup> {
        let mut cleanup = self.cleanup.lock();
        if let Some(future) = &*cleanup {
            return Some(future.clone());
        }
        if self.closed.is_cancelled() {
            return None;
        }
        let endpoint = self.endpoint.upgrade()?;
        let client = match client(&endpoint.client) {
            Ok(client) => client,
            Err(_) => {
                self.finish(error);
                return None;
            }
        };
        let params = wire::AhpConnectionRef {
            endpoint_id: endpoint.id.clone(),
            connection_id: self.id.clone(),
        };
        let future = async move {
            client
                .rpc()
                .ahp()
                .close_connection(params)
                .await
                .map(|_| ())
                .map_err(|e| e.to_string())
        }
        .boxed()
        .shared();
        *cleanup = Some(future.clone());
        drop(cleanup);
        self.finish(error);
        Some(future)
    }

    fn fail(&self, error: String) {
        if let Some(cleanup) = self.begin_close(Some(error)) {
            tokio::spawn(async move {
                if let Err(error) = cleanup.await {
                    tracing::warn!(%error, "Failed to close native AHP connection");
                }
            });
        }
    }
}

#[derive(Default)]
pub(crate) struct Registry {
    client: Mutex<Weak<ClientInner>>,
    endpoints: Mutex<HashMap<String, Arc<Endpoint>>>,
    requests: Mutex<HashMap<u64, CancellationToken>>,
}

impl Registry {
    pub(crate) fn clear(&self) {
        let endpoints = std::mem::take(&mut *self.endpoints.lock());
        for endpoint in endpoints.into_values() {
            endpoint.finish(None);
        }
    }

    pub(crate) fn notification(&self, method: &str, params: &Value) {
        if method == "$/cancelRequest" {
            if let Some(id) = params["id"].as_u64()
                && let Some(token) = self.requests.lock().get(&id)
            {
                token.cancel();
            }
            return;
        }
        let result = match method {
            "ahp.endpointClosed" => serde_json::from_value::<wire::AhpEndpointClosedNotification>(
                params.clone(),
            )
            .map(|notification| {
                let endpoint = self
                    .endpoints
                    .lock()
                    .get(&notification.endpoint_id)
                    .cloned();
                if let Some(endpoint) = endpoint {
                    endpoint.finish(notification.error);
                }
            }),
            "ahp.connectionClosed" => {
                serde_json::from_value::<wire::AhpConnectionClosedNotification>(params.clone()).map(
                    |notification| {
                        let endpoint = self
                            .endpoints
                            .lock()
                            .get(&notification.endpoint_id)
                            .cloned();
                        if let Some(endpoint) = endpoint {
                            let connection = endpoint
                                .connections
                                .lock()
                                .get(&notification.connection_id)
                                .cloned();
                            if let Some(connection) = connection {
                                connection.finish(notification.error);
                            }
                        }
                    },
                )
            }
            _ => return,
        };
        if let Err(error) = result {
            tracing::warn!(%error, %method, "Invalid AHP lifecycle notification");
        }
    }

    pub(crate) fn dispatch(self: &Arc<Self>, request: JsonRpcRequest) {
        let params = request.params.clone().unwrap_or(Value::Null);
        let endpoint = params["endpointId"]
            .as_str()
            .and_then(|id| self.endpoints.lock().get(id).cloned());
        let Some(endpoint) = endpoint else {
            let client = self.client.lock().upgrade().map(Client::from_inner);
            if let Some(client) = client {
                tokio::spawn(async move {
                    if let Err(error) = client
                        .send_response(&JsonRpcResponse {
                            jsonrpc: "2.0".into(),
                            id: request.id,
                            result: None,
                            error: Some(JsonRpcError {
                                code: -32603,
                                message: closed().to_string(),
                                data: None,
                            }),
                        })
                        .await
                    {
                        tracing::warn!(%error, "Failed to reject unknown AHP endpoint");
                    }
                });
            }
            return;
        };
        let connection = params["connectionId"]
            .as_str()
            .and_then(|id| endpoint.connections.lock().get(id).cloned());
        let token = connection
            .as_ref()
            .map_or_else(|| endpoint.closed.child_token(), |c| c.closed.child_token());
        self.requests.lock().insert(request.id, token.clone());
        let registry = self.clone();
        // Reserve before spawning: queued delivery tasks also count against the bound.
        let delivery = if request.method == "ahp.message" {
            Some(connection.as_ref().ok_or_else(closed).and_then(|c| {
                let message: wire::AhpMessage = serde_json::from_value(params.clone())?;
                let permits = c.output.reserve(&message.message)?;
                let (done, next) = oneshot::channel();
                let previous = c.delivery_tail.lock().replace(next);
                Ok((permits, previous, done, message.message))
            }))
        } else {
            None
        };
        tokio::spawn(async move {
            let mut delivery_guard = None;
            let work = async {
                if let Some(delivery) = delivery {
                    let (permits, previous, done, message) = delivery?;
                    delivery_guard = Some((permits, done));
                    if let Some(previous) = previous {
                        let _ = previous.await;
                    }
                    let connection = connection.as_ref().ok_or_else(closed)?;
                    (connection.options.on_message)(message).await?;
                    return Ok(Value::Null);
                }
                if matches!(
                    request.method.as_str(),
                    "ahp.createSession" | "ahp.resumeSession"
                ) && connection.is_none()
                {
                    return Err(closed());
                }
                let context = AhpCallbackContext {
                    cancellation: token.clone(),
                };
                IN_CALLBACK
                    .scope((), async {
                        match request.method.as_str() {
                            "ahp.createSession" => {
                                let handler = endpoint
                                    .options
                                    .on_create_session
                                    .as_ref()
                                    .ok_or_else(|| invalid("No AHP create callback"))?;
                                let request: wire::AhpCreateSessionRequest =
                                    serde_json::from_value(params)?;
                                let result = handler(
                                    AhpCreateSessionRequest {
                                        connection_id: request.connection_id,
                                        requested_session_id: request.requested_session_id,
                                        working_directory: request.working_directory,
                                        model: request.model,
                                        config: request.config,
                                    },
                                    context,
                                )
                                .await?;
                                Ok(serde_json::to_value(wire::AhpSessionIdentity {
                                    session_id: result.session_id.into(),
                                })?)
                            }
                            "ahp.resumeSession" => {
                                let handler = endpoint
                                    .options
                                    .on_resume_session
                                    .as_ref()
                                    .ok_or_else(|| invalid("No AHP resume callback"))?;
                                let request: wire::AhpResumeSessionRequest =
                                    serde_json::from_value(params)?;
                                let result = handler(
                                    AhpResumeSessionRequest {
                                        connection_id: request.connection_id,
                                        session_id: request.session_id.to_string(),
                                    },
                                    context,
                                )
                                .await?;
                                Ok(serde_json::to_value(wire::AhpSessionIdentity {
                                    session_id: result.session_id.into(),
                                })?)
                            }
                            "ahp.listSessions" => {
                                let handler = endpoint
                                    .options
                                    .on_list_sessions
                                    .as_ref()
                                    .ok_or_else(|| invalid("No AHP list callback"))?;
                                let sessions = handler((), context).await?;
                                Ok(serde_json::to_value(wire::AhpListSessionsResult {
                                    session_ids: sessions
                                        .into_iter()
                                        .map(|s| s.session_id)
                                        .collect(),
                                })?)
                            }
                            "ahp.sessionControl" => {
                                let handler = endpoint
                                    .options
                                    .on_session_control
                                    .as_ref()
                                    .ok_or_else(|| invalid("No AHP control callback"))?;
                                let request: wire::AhpSessionControlRequest =
                                    serde_json::from_value(params)?;
                                let result = handler(
                                    AhpSessionControlRequest {
                                        session_id: request.session_id.to_string(),
                                        kind: request.kind,
                                        payload: request.payload,
                                    },
                                    context,
                                )
                                .await?;
                                Ok(serde_json::to_value(wire::AhpSessionControlResult {
                                    applied: result.applied,
                                    reason: result.reason,
                                    result: result.result,
                                })?)
                            }
                            _ => Err(invalid("Unknown AHP callback")),
                        }
                    })
                    .await
            };
            let result: Result<Value> = tokio::select! {
                biased;
                _ = token.cancelled() => Err(Error::with_message(ErrorKind::Rpc { code: -32800 }, "AHP callback cancelled")),
                result = std::panic::AssertUnwindSafe(work).catch_unwind() =>
                    result.unwrap_or_else(|_| Err(invalid("AHP callback panicked"))),
            };
            registry.requests.lock().remove(&request.id);
            if request.method == "ahp.message"
                && let Err(error) = &result
                && let Some(connection) = connection
            {
                connection.fail(error.to_string());
            }
            if let Ok(client) = client(&endpoint.client) {
                let (result, error) = match result {
                    Ok(result) => (Some(result), None),
                    Err(error) => (
                        None,
                        Some(JsonRpcError {
                            code: match error.kind() {
                                ErrorKind::Rpc { code } => *code,
                                _ => -32603,
                            },
                            message: error.to_string(),
                            data: None,
                        }),
                    ),
                };
                if let Err(error) = client
                    .send_response(&JsonRpcResponse {
                        jsonrpc: "2.0".into(),
                        id: request.id,
                        result,
                        error,
                    })
                    .await
                {
                    tracing::warn!(%error, "Failed to send AHP callback response");
                }
            }
            drop(delivery_guard);
        });
    }
}
