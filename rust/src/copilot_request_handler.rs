//! Connection-level interception of the model-layer HTTP and WebSocket traffic
//! the runtime issues — for both CAPI and BYOK sessions.
//!
//! When [`ClientOptions::request_handler`](crate::ClientOptions::request_handler)
//! is set, the SDK registers itself as the runtime's request handler on
//! [`Client::start`](crate::Client::start). From then on, whenever the runtime
//! would issue a model-layer request (inference, `/models`, `/policy`, …) it
//! asks the registered [`CopilotRequestHandler`] to service it instead of making
//! the call itself.
//!
//! [`CopilotRequestHandler`] is the single seam consumers implement: one HTTP
//! send method and one WebSocket factory, each defaulting to transparent
//! pass-through to the real upstream. Override
//! [`send_http`](CopilotRequestHandler::send_http) to mutate / replace HTTP
//! requests, or [`open_websocket`](CopilotRequestHandler::open_websocket) to
//! mutate the handshake or return a custom [`CopilotWebSocketHandler`].
//!
//! # Cancellation
//!
//! [`CopilotRequestContext::cancel`] fires when the runtime cancels the
//! in-flight request (for example because the agent turn was aborted). Forward
//! it to the upstream call so it is torn down too, and stop writing the response.

use std::collections::HashMap;
use std::pin::Pin;
use std::sync::{Arc, LazyLock, OnceLock, Weak};

use async_trait::async_trait;
use base64::Engine;
use bytes::Bytes;
use futures_util::{SinkExt, Stream, StreamExt};
use http::HeaderMap;
use http::header::{HeaderName, HeaderValue};
use parking_lot::Mutex;
use tokio::net::TcpStream;
use tokio::sync::Mutex as AsyncMutex;
use tokio::sync::mpsc;
use tokio_tungstenite::tungstenite::Message;
use tokio_tungstenite::tungstenite::client::IntoClientRequest;
use tokio_tungstenite::{MaybeTlsStream, WebSocketStream, connect_async};
use tokio_util::sync::CancellationToken;
use tracing::warn;

use crate::generated::api_types::{
    LlmInferenceHttpRequestChunkRequest, LlmInferenceHttpRequestStartRequest,
    LlmInferenceHttpRequestStartTransport, LlmInferenceHttpResponseChunkError,
    LlmInferenceHttpResponseChunkRequest, LlmInferenceHttpResponseStartRequest,
};
use crate::{
    Client, ClientInner, JsonRpcRequest, JsonRpcResponse, RequestId, SessionId, error_codes,
};

const METHOD_HTTP_REQUEST_START: &str = "llmInference.httpRequestStart";
const METHOD_HTTP_REQUEST_CHUNK: &str = "llmInference.httpRequestChunk";

/// Transport the runtime would otherwise use for an intercepted request.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CopilotRequestTransport {
    /// Plain HTTP or SSE. Each response body frame is an opaque byte range.
    Http,
    /// Full-duplex WebSocket. Each request/response body frame maps to exactly
    /// one WebSocket message.
    Websocket,
}

impl CopilotRequestTransport {
    fn from_wire(value: Option<LlmInferenceHttpRequestStartTransport>) -> Self {
        match value {
            Some(LlmInferenceHttpRequestStartTransport::Websocket) => Self::Websocket,
            _ => Self::Http,
        }
    }
}

/// Error returned by a [`CopilotRequestHandler`] hook or the response stream.
#[derive(Debug)]
#[non_exhaustive]
pub enum CopilotRequestError {
    /// The response was used after the RPC connection to the runtime closed.
    ConnectionClosed,

    /// The response state machine was violated (for example `start` called
    /// twice, or a write before `start`).
    InvalidState(String),

    /// An upstream transport failure while forwarding the request.
    Upstream(String),

    /// A failure surfaced by the consumer's own handler.
    Handler(String),

    /// An RPC error talking to the runtime.
    Rpc(crate::Error),
}

impl CopilotRequestError {
    /// Construct a handler-level error from a message — the idiomatic way for a
    /// consumer to fail an intercepted request.
    pub fn message(message: impl Into<String>) -> Self {
        Self::Handler(message.into())
    }
}

impl std::fmt::Display for CopilotRequestError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::ConnectionClosed => {
                f.write_str("Copilot request response used after RPC connection closed")
            }
            Self::InvalidState(message) | Self::Upstream(message) | Self::Handler(message) => {
                f.write_str(message)
            }
            Self::Rpc(err) => write!(f, "{err}"),
        }
    }
}

impl std::error::Error for CopilotRequestError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Rpc(err) => Some(err),
            _ => None,
        }
    }
}

impl From<crate::Error> for CopilotRequestError {
    fn from(err: crate::Error) -> Self {
        Self::Rpc(err)
    }
}

/// Context describing an intercepted request, shared by the HTTP and WebSocket
/// seams.
#[derive(Clone)]
#[non_exhaustive]
pub struct CopilotRequestContext {
    /// Opaque runtime-minted request id, stable across the request lifecycle.
    pub request_id: String,
    /// Id of the runtime session that triggered this request, or `None` when it
    /// was issued outside any session (for example the startup model catalog).
    pub session_id: Option<String>,
    /// Transport the runtime would otherwise use.
    pub transport: CopilotRequestTransport,
    /// Absolute request URL.
    pub url: String,
    /// Request headers, multi-valued.
    pub headers: HeaderMap,
    /// Fires when the runtime cancels this in-flight request.
    pub cancel: CancellationToken,
}

/// Streaming response body: a sequence of byte chunks or a terminal error.
pub type CopilotHttpResponseBody =
    Pin<Box<dyn Stream<Item = Result<Bytes, CopilotRequestError>> + Send>>;

/// A buffered HTTP request handed to [`CopilotRequestHandler::send_http`].
#[non_exhaustive]
pub struct CopilotHttpRequest {
    /// HTTP method (`GET`, `POST`, …).
    pub method: String,
    /// Absolute request URL.
    pub url: String,
    /// Request headers.
    pub headers: HeaderMap,
    /// Fully-buffered request body.
    pub body: Vec<u8>,
    /// Fires when the runtime cancels the request.
    pub cancel: CancellationToken,
}

/// A streaming HTTP response returned by [`CopilotRequestHandler::send_http`].
#[non_exhaustive]
pub struct CopilotHttpResponse {
    /// HTTP status code.
    pub status: u16,
    /// Optional status reason phrase.
    pub status_text: Option<String>,
    /// Response headers.
    pub headers: HeaderMap,
    /// Streaming response body.
    pub body: CopilotHttpResponseBody,
}

impl CopilotHttpResponse {
    /// Build a response with the given parts.
    pub fn new(
        status: u16,
        status_text: Option<String>,
        headers: HeaderMap,
        body: CopilotHttpResponseBody,
    ) -> Self {
        Self {
            status,
            status_text,
            headers,
            body,
        }
    }
}

/// A single WebSocket message flowing through a [`CopilotWebSocketHandler`].
#[derive(Clone)]
pub struct CopilotWebSocketMessage {
    /// Message payload.
    pub data: Vec<u8>,
    /// Whether the payload is a binary frame (`true`) or a text frame (`false`).
    pub binary: bool,
}

impl CopilotWebSocketMessage {
    /// A UTF-8 text message.
    pub fn text(data: impl Into<String>) -> Self {
        Self {
            data: data.into().into_bytes(),
            binary: false,
        }
    }

    /// A binary message.
    pub fn binary(data: Vec<u8>) -> Self {
        Self { data, binary: true }
    }
}

/// The runtime-facing side of a WebSocket: a [`CopilotWebSocketHandler`] writes
/// upstream→runtime messages here.
#[derive(Clone)]
pub struct CopilotWebSocketResponse {
    exchange: Arc<CopilotRequestExchange>,
}

impl CopilotWebSocketResponse {
    fn new(exchange: Arc<CopilotRequestExchange>) -> Self {
        Self { exchange }
    }

    /// Forward one upstream message to the runtime.
    pub async fn send_message(
        &self,
        message: CopilotWebSocketMessage,
    ) -> Result<(), CopilotRequestError> {
        self.exchange.ensure_ws_started().await?;
        if message.binary {
            self.exchange.write_binary(&message.data).await
        } else {
            let text = String::from_utf8_lossy(&message.data);
            self.exchange.write_text(&text).await
        }
    }

    /// End the runtime response stream (the upstream connection closed).
    pub async fn close(&self) -> Result<(), CopilotRequestError> {
        self.exchange.end_response().await
    }

    async fn fail(
        &self,
        message: impl Into<String>,
        code: Option<String>,
    ) -> Result<(), CopilotRequestError> {
        self.exchange.error_response(message, code).await
    }
}

/// A per-connection WebSocket handler. The default implementation
/// ([`ForwardingCopilotWebSocketHandler`]) bridges to the real upstream;
/// override [`CopilotRequestHandler::open_websocket`] to supply a custom one.
#[async_trait]
pub trait CopilotWebSocketHandler: Send + Sync {
    /// Forward one runtime→upstream message.
    async fn send_request_message(
        &self,
        message: CopilotWebSocketMessage,
    ) -> Result<(), CopilotRequestError>;

    /// Tear down the upstream connection.
    async fn close(&self) -> Result<(), CopilotRequestError>;
}

/// The connection-level Copilot request seam.
///
/// One implementor services both transports. Defaults forward transparently to
/// the real upstream, so overriding nothing yields a pass-through; override a
/// method to mutate or replace traffic.
#[async_trait]
pub trait CopilotRequestHandler: Send + Sync + 'static {
    /// Service one intercepted HTTP request. Default: forward to the real
    /// upstream via [`forward_http`]. Override to mutate the request before
    /// forwarding, mutate the response after, or replace the call entirely.
    async fn send_http(
        &self,
        request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        forward_http(request).await
    }

    /// Open a per-connection WebSocket handler. Default: a
    /// [`ForwardingCopilotWebSocketHandler`] wired to the real upstream.
    /// Override to mutate the handshake (URL / headers via `ctx`) or return a
    /// custom handler. `response` is the runtime-facing sink for upstream
    /// messages.
    async fn open_websocket(
        &self,
        ctx: &CopilotRequestContext,
        response: CopilotWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, CopilotRequestError> {
        let handler =
            ForwardingCopilotWebSocketHandler::builder(ctx.url.clone(), ctx.headers.clone())
                .connect(response)
                .await?;
        Ok(Box::new(handler))
    }
}

/// Forward through a shared handler, so an `Arc<H>` can be registered while the
/// consumer retains a handle (for example to read state the handler records).
#[async_trait]
impl<H: CopilotRequestHandler> CopilotRequestHandler for Arc<H> {
    async fn send_http(
        &self,
        request: CopilotHttpRequest,
        ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        (**self).send_http(request, ctx).await
    }

    async fn open_websocket(
        &self,
        ctx: &CopilotRequestContext,
        response: CopilotWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, CopilotRequestError> {
        (**self).open_websocket(ctx, response).await
    }
}
/// fresh upstream connection.
const FORBIDDEN_HEADERS: &[&str] = &[
    "host",
    "connection",
    "content-length",
    "transfer-encoding",
    "keep-alive",
    "upgrade",
    "proxy-connection",
    "te",
    "trailer",
];

fn is_forbidden_header(name: &HeaderName) -> bool {
    let name = name.as_str();
    FORBIDDEN_HEADERS.contains(&name) || name.starts_with("sec-websocket")
}

/// Drop headers that belong to the inbound connection rather than the request.
fn strip_forbidden_headers(headers: &mut HeaderMap) {
    let forbidden: Vec<HeaderName> = headers
        .keys()
        .filter(|name| is_forbidden_header(name))
        .cloned()
        .collect();
    for name in forbidden {
        headers.remove(&name);
    }
}

static SHARED_HTTP_CLIENT: LazyLock<reqwest::Client> = LazyLock::new(|| {
    reqwest::Client::builder()
        .redirect(reqwest::redirect::Policy::none())
        .build()
        .expect("default reqwest client must build")
});

/// Forward an HTTP request to its real upstream and stream the response back.
///
/// This is the default behaviour of [`CopilotRequestHandler::send_http`];
/// consumers that mutate a request can call it to forward the mutated request.
pub async fn forward_http(
    request: CopilotHttpRequest,
) -> Result<CopilotHttpResponse, CopilotRequestError> {
    let method = reqwest::Method::from_bytes(request.method.as_bytes())
        .map_err(|e| CopilotRequestError::InvalidState(format!("invalid HTTP method: {e}")))?;

    let mut headers = request.headers;
    strip_forbidden_headers(&mut headers);

    let mut builder = SHARED_HTTP_CLIENT
        .request(method, &request.url)
        .headers(headers);
    if !request.body.is_empty() {
        builder = builder.body(request.body);
    }

    let response = tokio::select! {
        _ = request.cancel.cancelled() => {
            return Err(CopilotRequestError::message("Request cancelled by runtime"));
        }
        result = builder.send() => result.map_err(|e| CopilotRequestError::Upstream(e.to_string()))?,
    };

    let status = response.status().as_u16();
    let status_text = response.status().canonical_reason().map(str::to_string);
    let headers = response.headers().clone();
    let body = response
        .bytes_stream()
        .map(|item| item.map_err(|e| CopilotRequestError::Upstream(e.to_string())));

    Ok(CopilotHttpResponse {
        status,
        status_text,
        headers,
        body: Box::pin(body),
    })
}

type UpstreamWrite =
    futures_util::stream::SplitSink<WebSocketStream<MaybeTlsStream<TcpStream>>, Message>;

/// Transform applied to a WebSocket message; return `None` to drop it.
pub type WebSocketTransform =
    Arc<dyn Fn(CopilotWebSocketMessage) -> Option<CopilotWebSocketMessage> + Send + Sync>;

/// Builder for a [`ForwardingCopilotWebSocketHandler`].
pub struct ForwardingCopilotWebSocketHandlerBuilder {
    url: String,
    headers: HeaderMap,
    on_send_request_message: Option<WebSocketTransform>,
    on_send_response_message: Option<WebSocketTransform>,
}

impl ForwardingCopilotWebSocketHandlerBuilder {
    /// Hook runtime→upstream messages (mutate or drop before forwarding).
    pub fn on_send_request_message(mut self, transform: WebSocketTransform) -> Self {
        self.on_send_request_message = Some(transform);
        self
    }

    /// Hook upstream→runtime messages (mutate or drop before forwarding).
    pub fn on_send_response_message(mut self, transform: WebSocketTransform) -> Self {
        self.on_send_response_message = Some(transform);
        self
    }

    /// Dial the upstream WebSocket and begin pumping upstream→runtime messages
    /// into `response`.
    pub async fn connect(
        self,
        response: CopilotWebSocketResponse,
    ) -> Result<ForwardingCopilotWebSocketHandler, CopilotRequestError> {
        let mut request =
            self.url.as_str().into_client_request().map_err(|e| {
                CopilotRequestError::Upstream(format!("invalid websocket url: {e}"))
            })?;
        for (name, value) in &self.headers {
            if is_forbidden_header(name) {
                continue;
            }
            request.headers_mut().append(name.clone(), value.clone());
        }

        let (stream, _) = connect_async(request)
            .await
            .map_err(|e| CopilotRequestError::Upstream(format!("websocket connect failed: {e}")))?;
        let (write, mut read) = stream.split();

        let cancel = CancellationToken::new();
        let loop_cancel = cancel.clone();
        let on_response = self.on_send_response_message.clone();
        tokio::spawn(async move {
            loop {
                tokio::select! {
                    _ = loop_cancel.cancelled() => break,
                    msg = read.next() => match msg {
                        Some(Ok(Message::Text(text))) => {
                            let message = CopilotWebSocketMessage::text(text);
                            if let Some(out) = apply_transform(&on_response, message) {
                                let _ = response.send_message(out).await;
                            }
                        }
                        Some(Ok(Message::Binary(data))) => {
                            let message = CopilotWebSocketMessage::binary(data);
                            if let Some(out) = apply_transform(&on_response, message) {
                                let _ = response.send_message(out).await;
                            }
                        }
                        Some(Ok(Message::Close(_))) | None => break,
                        Some(Ok(_)) => continue,
                        Some(Err(e)) => {
                            let _ = response.fail(e.to_string(), None).await;
                            return;
                        }
                    }
                }
            }
            let _ = response.close().await;
        });

        Ok(ForwardingCopilotWebSocketHandler {
            write: AsyncMutex::new(Some(write)),
            on_send_request_message: self.on_send_request_message,
            cancel,
        })
    }
}

/// The default WebSocket handler: forwards each runtime message to the real
/// upstream and each upstream message back to the runtime. Mutate by supplying
/// transforms on the [builder](ForwardingCopilotWebSocketHandler::builder).
pub struct ForwardingCopilotWebSocketHandler {
    write: AsyncMutex<Option<UpstreamWrite>>,
    on_send_request_message: Option<WebSocketTransform>,
    cancel: CancellationToken,
}

impl ForwardingCopilotWebSocketHandler {
    /// Start building a forwarding handler for `url` with the given upstream
    /// handshake headers.
    pub fn builder(url: String, headers: HeaderMap) -> ForwardingCopilotWebSocketHandlerBuilder {
        ForwardingCopilotWebSocketHandlerBuilder {
            url,
            headers,
            on_send_request_message: None,
            on_send_response_message: None,
        }
    }
}

#[async_trait]
impl CopilotWebSocketHandler for ForwardingCopilotWebSocketHandler {
    async fn send_request_message(
        &self,
        message: CopilotWebSocketMessage,
    ) -> Result<(), CopilotRequestError> {
        let Some(message) = apply_transform(&self.on_send_request_message, message) else {
            return Ok(());
        };
        let ws_message = if message.binary {
            Message::Binary(message.data)
        } else {
            Message::Text(String::from_utf8_lossy(&message.data).into_owned())
        };
        let mut guard = self.write.lock().await;
        if let Some(write) = guard.as_mut() {
            write
                .send(ws_message)
                .await
                .map_err(|e| CopilotRequestError::Upstream(e.to_string()))?;
        }
        Ok(())
    }

    async fn close(&self) -> Result<(), CopilotRequestError> {
        self.cancel.cancel();
        let mut guard = self.write.lock().await;
        if let Some(mut write) = guard.take() {
            let _ = write.send(Message::Close(None)).await;
            let _ = write.close().await;
        }
        Ok(())
    }
}

fn apply_transform(
    transform: &Option<WebSocketTransform>,
    message: CopilotWebSocketMessage,
) -> Option<CopilotWebSocketMessage> {
    match transform {
        Some(f) => f(message),
        None => Some(message),
    }
}

/// Mutable response state machine for a single exchange.
#[derive(Default)]
struct ResponseState {
    started: bool,
    finished: bool,
}

/// One intercepted request in flight.
///
/// Carries the request metadata plus the body byte stream the runtime feeds in
/// via `httpRequestChunk` frames, and emits the handler's response straight back
/// to the runtime through the generated `llmInference` server API — a single
/// object the dispatcher owns and the handler drives.
struct CopilotRequestExchange {
    request_id: String,
    session_id: Option<String>,
    method: String,
    url: String,
    headers: HeaderMap,
    transport: CopilotRequestTransport,
    cancel: CancellationToken,
    client: Weak<ClientInner>,
    /// Sender feeding the request body stream. Dropped (set to `None`) on `end`
    /// or `cancel` to close the stream.
    body_tx: Mutex<Option<mpsc::UnboundedSender<Vec<u8>>>>,
    body_rx: AsyncMutex<mpsc::UnboundedReceiver<Vec<u8>>>,
    state: Mutex<ResponseState>,
}

impl CopilotRequestExchange {
    fn new(params: LlmInferenceHttpRequestStartRequest, client: Weak<ClientInner>) -> Self {
        let (body_tx, body_rx) = mpsc::unbounded_channel();
        Self {
            request_id: params.request_id.into_inner(),
            session_id: params.session_id.map(SessionId::into_inner),
            method: params.method,
            url: params.url,
            headers: headers_from_wire(&params.headers),
            transport: CopilotRequestTransport::from_wire(params.transport),
            cancel: CancellationToken::new(),
            client,
            body_tx: Mutex::new(Some(body_tx)),
            body_rx: AsyncMutex::new(body_rx),
            state: Mutex::new(ResponseState::default()),
        }
    }

    fn context(&self) -> CopilotRequestContext {
        CopilotRequestContext {
            request_id: self.request_id.clone(),
            session_id: self.session_id.clone(),
            transport: self.transport,
            url: self.url.clone(),
            headers: self.headers.clone(),
            cancel: self.cancel.clone(),
        }
    }

    fn client(&self) -> Result<Client, CopilotRequestError> {
        self.client
            .upgrade()
            .map(Client::from_inner)
            .ok_or(CopilotRequestError::ConnectionClosed)
    }

    fn request_id(&self) -> RequestId {
        RequestId::new(self.request_id.clone())
    }

    // --- Request body feed (driven by the dispatcher as frames arrive) ---

    fn push_chunk(&self, data: Vec<u8>) {
        if let Some(tx) = self.body_tx.lock().as_ref() {
            let _ = tx.send(data);
        }
    }

    fn push_end(&self) {
        *self.body_tx.lock() = None;
    }

    fn push_cancel(&self) {
        self.cancel.cancel();
        *self.body_tx.lock() = None;
    }

    async fn recv_body(&self) -> Option<Vec<u8>> {
        self.body_rx.lock().await.recv().await
    }

    async fn drain_body(&self) -> Vec<u8> {
        let mut buf = Vec::new();
        let mut rx = self.body_rx.lock().await;
        while let Some(frame) = rx.recv().await {
            buf.extend_from_slice(&frame);
        }
        buf
    }

    // --- Response emit (driven by the handler). Strict state machine: ---
    // start_response once -> 0..N write -> exactly one of
    // end_response / error_response.

    fn started(&self) -> bool {
        self.state.lock().started
    }

    fn finished(&self) -> bool {
        self.state.lock().finished
    }

    async fn start_response(
        &self,
        status: u16,
        status_text: Option<String>,
        headers: HeaderMap,
    ) -> Result<(), CopilotRequestError> {
        {
            let mut state = self.state.lock();
            if state.started {
                return Err(CopilotRequestError::InvalidState(
                    "response start() called twice".to_string(),
                ));
            }
            if state.finished {
                return Err(CopilotRequestError::InvalidState(
                    "response already finished".to_string(),
                ));
            }
            state.started = true;
        }
        let request = LlmInferenceHttpResponseStartRequest {
            headers: headers_to_wire(&headers),
            request_id: self.request_id(),
            status: i64::from(status),
            status_text,
        };
        self.client()?
            .rpc()
            .llm_inference()
            .http_response_start(request)
            .await?;
        Ok(())
    }

    /// Start the WebSocket upgrade head (status 101) once, ignoring repeat
    /// calls. The dispatcher emits it eagerly before pumping; later writes call
    /// this as a harmless no-op backstop.
    async fn ensure_ws_started(&self) -> Result<(), CopilotRequestError> {
        if self.started() {
            return Ok(());
        }
        self.start_response(101, None, HeaderMap::new()).await
    }

    async fn write_text(&self, text: &str) -> Result<(), CopilotRequestError> {
        self.write(text.to_string(), false).await
    }

    async fn write_binary(&self, data: &[u8]) -> Result<(), CopilotRequestError> {
        let encoded = base64::engine::general_purpose::STANDARD.encode(data);
        self.write(encoded, true).await
    }

    async fn write(&self, data: String, binary: bool) -> Result<(), CopilotRequestError> {
        {
            let state = self.state.lock();
            if !state.started {
                return Err(CopilotRequestError::InvalidState(
                    "response write called before start()".to_string(),
                ));
            }
            if state.finished {
                return Err(CopilotRequestError::InvalidState(
                    "response write called after end()/error()".to_string(),
                ));
            }
        }
        let request = LlmInferenceHttpResponseChunkRequest {
            binary: binary.then_some(true),
            data,
            end: Some(false),
            error: None,
            request_id: self.request_id(),
        };
        self.client()?
            .rpc()
            .llm_inference()
            .http_response_chunk(request)
            .await?;
        Ok(())
    }

    async fn end_response(&self) -> Result<(), CopilotRequestError> {
        {
            let mut state = self.state.lock();
            if state.finished {
                return Ok(());
            }
            state.finished = true;
        }
        let request = LlmInferenceHttpResponseChunkRequest {
            binary: None,
            data: String::new(),
            end: Some(true),
            error: None,
            request_id: self.request_id(),
        };
        self.client()?
            .rpc()
            .llm_inference()
            .http_response_chunk(request)
            .await?;
        Ok(())
    }

    async fn error_response(
        &self,
        message: impl Into<String>,
        code: Option<String>,
    ) -> Result<(), CopilotRequestError> {
        {
            let mut state = self.state.lock();
            if state.finished {
                return Ok(());
            }
            state.finished = true;
        }
        let request = LlmInferenceHttpResponseChunkRequest {
            binary: None,
            data: String::new(),
            end: Some(true),
            error: Some(LlmInferenceHttpResponseChunkError {
                code,
                message: message.into(),
            }),
            request_id: self.request_id(),
        };
        self.client()?
            .rpc()
            .llm_inference()
            .http_response_chunk(request)
            .await?;
        Ok(())
    }
}

/// Drive one exchange through the registered handler, dispatching by transport.
async fn drive_exchange(
    exchange: &Arc<CopilotRequestExchange>,
    handler: &Arc<dyn CopilotRequestHandler>,
) -> Result<(), CopilotRequestError> {
    let ctx = exchange.context();
    match exchange.transport {
        CopilotRequestTransport::Http => {
            let body = exchange.drain_body().await;
            let request = CopilotHttpRequest {
                method: exchange.method.clone(),
                url: exchange.url.clone(),
                headers: exchange.headers.clone(),
                body,
                cancel: ctx.cancel.clone(),
            };
            let response = handler.send_http(request, &ctx).await?;
            stream_http_response(response, exchange, &ctx.cancel).await
        }
        CopilotRequestTransport::Websocket => {
            // The runtime blocks the WebSocket connect until it receives the 101
            // response head (the upgrade acknowledgement) and only then forwards
            // inbound messages as request-body chunks. Emit it eagerly here —
            // waiting for the first upstream message would deadlock, since the
            // upstream stays silent until it receives a request message the
            // runtime won't send before the upgrade completes.
            exchange.ensure_ws_started().await?;
            let response = CopilotWebSocketResponse::new(exchange.clone());
            let ws = handler.open_websocket(&ctx, response).await?;
            let result = pump_websocket_requests(ws.as_ref(), exchange, &ctx.cancel).await;
            let _ = ws.close().await;
            match result {
                Ok(()) => exchange.end_response().await,
                Err(err) if ctx.cancel.is_cancelled() => {
                    exchange
                        .error_response(
                            "Request cancelled by runtime",
                            Some("cancelled".to_string()),
                        )
                        .await?;
                    let _ = err;
                    Ok(())
                }
                Err(err) => Err(err),
            }
        }
    }
}

/// Stream an HTTP response into the runtime, honouring cancellation.
async fn stream_http_response(
    response: CopilotHttpResponse,
    exchange: &CopilotRequestExchange,
    cancel: &CancellationToken,
) -> Result<(), CopilotRequestError> {
    exchange
        .start_response(response.status, response.status_text, response.headers)
        .await?;

    let mut body = response.body;
    loop {
        tokio::select! {
            _ = cancel.cancelled() => {
                return exchange
                    .error_response("Request cancelled by runtime", Some("cancelled".to_string()))
                    .await;
            }
            next = body.next() => match next {
                Some(Ok(chunk)) => {
                    for piece in chunk.chunks(32 * 1024) {
                        exchange.write_binary(piece).await?;
                    }
                }
                Some(Err(e)) => {
                    return exchange.error_response(e.to_string(), None).await;
                }
                None => break,
            }
        }
    }
    exchange.end_response().await
}

/// Forward runtime→upstream WebSocket messages until the runtime closes its side
/// or cancels.
async fn pump_websocket_requests(
    handler: &dyn CopilotWebSocketHandler,
    exchange: &CopilotRequestExchange,
    cancel: &CancellationToken,
) -> Result<(), CopilotRequestError> {
    loop {
        tokio::select! {
            _ = cancel.cancelled() => {
                return Err(CopilotRequestError::message("Request cancelled by runtime"));
            }
            frame = exchange.recv_body() => match frame {
                Some(data) => {
                    handler
                        .send_request_message(CopilotWebSocketMessage { data, binary: false })
                        .await?;
                }
                None => return Ok(()),
            }
        }
    }
}

/// Drive the exchange's response to a terminal state once the handler returns,
/// covering handlers that error, get cancelled, or forget to finalize.
async fn finalize_exchange(
    exchange: &CopilotRequestExchange,
    result: Result<(), CopilotRequestError>,
) {
    match result {
        Ok(()) => {
            if !exchange.finished() {
                fail_via_response(
                    exchange,
                    502,
                    "Copilot request handler returned without finalising the response".to_string(),
                )
                .await;
            }
        }
        Err(err) => {
            if exchange.finished() {
                return;
            }
            if exchange.cancel.is_cancelled() {
                if !exchange.started() {
                    let _ = exchange.start_response(499, None, HeaderMap::new()).await;
                }
                let _ = exchange
                    .error_response(
                        "Request cancelled by runtime",
                        Some("cancelled".to_string()),
                    )
                    .await;
            } else {
                fail_via_response(exchange, 502, err.to_string()).await;
            }
        }
    }
}

async fn fail_via_response(exchange: &CopilotRequestExchange, status: u16, message: String) {
    if !exchange.started() {
        let _ = exchange
            .start_response(status, None, HeaderMap::new())
            .await;
    }
    let _ = exchange.error_response(message, None).await;
}

/// Routes inbound `llmInference.*` requests to the registered handler,
/// reassembling each request's streaming body and acking every frame.
pub(crate) struct CopilotRequestDispatcher {
    handler: Arc<dyn CopilotRequestHandler>,
    client: OnceLock<Weak<ClientInner>>,
    pending: Mutex<HashMap<String, Arc<CopilotRequestExchange>>>,
}

impl CopilotRequestDispatcher {
    pub(crate) fn new(handler: Arc<dyn CopilotRequestHandler>) -> Self {
        Self {
            handler,
            client: OnceLock::new(),
            pending: Mutex::new(HashMap::new()),
        }
    }

    pub(crate) fn set_client(&self, client: Weak<ClientInner>) {
        let _ = self.client.set(client);
    }

    fn client(&self) -> Option<Client> {
        self.client
            .get()
            .and_then(Weak::upgrade)
            .map(Client::from_inner)
    }

    fn client_weak(&self) -> Weak<ClientInner> {
        self.client.get().cloned().unwrap_or_else(Weak::new)
    }

    pub(crate) async fn dispatch(self: &Arc<Self>, request: JsonRpcRequest) {
        match request.method.as_str() {
            METHOD_HTTP_REQUEST_START => self.handle_start(request).await,
            METHOD_HTTP_REQUEST_CHUNK => self.handle_chunk(request).await,
            other => {
                warn!(method = other, "unknown llmInference request method");
                self.send_error(request.id, "unknown llmInference method")
                    .await;
            }
        }
    }

    async fn handle_start(self: &Arc<Self>, request: JsonRpcRequest) {
        let id = request.id;
        let Some(params) = parse_params::<LlmInferenceHttpRequestStartRequest>(&request) else {
            self.send_error(id, "invalid llmInference.httpRequestStart params")
                .await;
            return;
        };

        let exchange = Arc::new(CopilotRequestExchange::new(params, self.client_weak()));
        let request_id = exchange.request_id.clone();
        self.pending
            .lock()
            .insert(request_id.clone(), exchange.clone());

        let handler = self.handler.clone();
        let dispatcher = Arc::clone(self);
        tokio::spawn(async move {
            let result = drive_exchange(&exchange, &handler).await;
            finalize_exchange(&exchange, result).await;
            dispatcher.remove_pending(&request_id);
        });

        self.ack(id).await;
    }

    async fn handle_chunk(&self, request: JsonRpcRequest) {
        let id = request.id;
        let Some(params) = parse_params::<LlmInferenceHttpRequestChunkRequest>(&request) else {
            self.send_error(id, "invalid llmInference.httpRequestChunk params")
                .await;
            return;
        };

        let request_id = params.request_id.to_string();
        let exchange = self.pending.lock().get(&request_id).cloned();
        if let Some(exchange) = exchange {
            apply_chunk(&exchange, &params);
        }

        self.ack(id).await;
    }

    fn remove_pending(&self, request_id: &str) {
        self.pending.lock().remove(request_id);
    }

    async fn ack(&self, id: u64) {
        let Some(client) = self.client() else {
            return;
        };
        let _ = client
            .send_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id,
                result: Some(serde_json::json!({})),
                error: None,
            })
            .await;
    }

    async fn send_error(&self, id: u64, message: &str) {
        let Some(client) = self.client() else {
            return;
        };
        let _ = client
            .send_response(&JsonRpcResponse {
                jsonrpc: "2.0".to_string(),
                id,
                result: None,
                error: Some(crate::JsonRpcError {
                    code: error_codes::INTERNAL_ERROR,
                    message: message.to_string(),
                    data: None,
                }),
            })
            .await;
    }
}

/// Apply one body chunk to a pending request: route data into the body stream,
/// or terminate it on `end` / `cancel`.
fn apply_chunk(exchange: &CopilotRequestExchange, params: &LlmInferenceHttpRequestChunkRequest) {
    if params.cancel == Some(true) {
        exchange.push_cancel();
        return;
    }

    if !params.data.is_empty() {
        let decoded = if params.binary == Some(true) {
            match base64::engine::general_purpose::STANDARD.decode(params.data.as_bytes()) {
                Ok(bytes) => bytes,
                Err(e) => {
                    warn!(error = %e, "failed to decode base64 llmInference body chunk");
                    return;
                }
            }
        } else {
            params.data.clone().into_bytes()
        };
        exchange.push_chunk(decoded);
    }

    if params.end == Some(true) {
        exchange.push_end();
    }
}

fn parse_params<T: serde::de::DeserializeOwned>(request: &JsonRpcRequest) -> Option<T> {
    request
        .params
        .as_ref()
        .and_then(|p| serde_json::from_value(p.clone()).ok())
}

/// Convert a wire header map into an [`http::HeaderMap`], skipping any entry the
/// `http` crate rejects.
fn headers_from_wire(wire: &HashMap<String, Vec<String>>) -> HeaderMap {
    let mut headers = HeaderMap::new();
    for (name, values) in wire {
        let Ok(header_name) = HeaderName::from_bytes(name.as_bytes()) else {
            continue;
        };
        for value in values {
            let Ok(header_value) = HeaderValue::from_str(value) else {
                continue;
            };
            headers.append(header_name.clone(), header_value);
        }
    }
    headers
}

/// Convert an [`http::HeaderMap`] into the wire header map, dropping values that
/// are not valid UTF-8.
fn headers_to_wire(headers: &HeaderMap) -> HashMap<String, Vec<String>> {
    let mut wire: HashMap<String, Vec<String>> = HashMap::new();
    for (name, value) in headers {
        let Ok(value) = value.to_str() else {
            continue;
        };
        wire.entry(name.as_str().to_string())
            .or_default()
            .push(value.to_string());
    }
    wire
}
