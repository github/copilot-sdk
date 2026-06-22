//! Idiomatic forwarding layer on top of [`LlmInferenceProvider`].
//!
//! [`LlmRequestHandler`] is the high-level seam most consumers want: it exposes
//! one HTTP send method and one WebSocket factory, each defaulting to
//! transparent pass-through to the real upstream. Override
//! [`send_http`](LlmRequestHandler::send_http) to mutate / replace HTTP
//! requests, or [`open_websocket`](LlmRequestHandler::open_websocket) to mutate
//! the handshake or return a custom [`CopilotWebSocketHandler`].
//!
//! Any `T: LlmRequestHandler` is automatically an [`LlmInferenceProvider`] via a
//! blanket impl, so a handler can be handed straight to
//! [`LlmInferenceConfig::new`](crate::LlmInferenceConfig::new).

use std::pin::Pin;
use std::sync::{Arc, LazyLock};

use async_trait::async_trait;
use bytes::Bytes;
use futures_util::{SinkExt, Stream, StreamExt};
use http::HeaderMap;
use http::header::HeaderName;
use tokio::net::TcpStream;
use tokio::sync::Mutex;
use tokio_tungstenite::tungstenite::Message;
use tokio_tungstenite::tungstenite::client::IntoClientRequest;
use tokio_tungstenite::{MaybeTlsStream, WebSocketStream, connect_async};
use tokio_util::sync::CancellationToken;

use crate::llm_inference::{
    LlmInferenceError, LlmInferenceProvider, LlmInferenceRequest, LlmRequestBody, LlmResponseInit,
    LlmResponseSink, LlmTransport,
};

/// Hop-by-hop and connection-management headers that must not be forwarded to a
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

/// Streaming response body: a sequence of byte chunks or a terminal error.
pub type LlmHttpResponseBody = Pin<Box<dyn Stream<Item = Result<Bytes, LlmInferenceError>> + Send>>;

/// A buffered HTTP request handed to [`LlmRequestHandler::send_http`].
#[non_exhaustive]
pub struct LlmHttpRequest {
    /// HTTP method.
    pub method: String,
    /// Absolute request URL.
    pub url: String,
    /// Request headers.
    pub headers: HeaderMap,
    /// Fully-buffered request body.
    pub body: Vec<u8>,
    /// Triggered when the runtime cancels the request.
    pub cancel: CancellationToken,
}

/// A streaming HTTP response returned by [`LlmRequestHandler::send_http`].
#[non_exhaustive]
pub struct LlmHttpResponse {
    /// HTTP status code.
    pub status: u16,
    /// Optional status reason phrase.
    pub status_text: Option<String>,
    /// Response headers.
    pub headers: HeaderMap,
    /// Streaming response body.
    pub body: LlmHttpResponseBody,
}

impl LlmHttpResponse {
    /// Build a response with the given parts.
    pub fn new(
        status: u16,
        status_text: Option<String>,
        headers: HeaderMap,
        body: LlmHttpResponseBody,
    ) -> Self {
        Self {
            status,
            status_text,
            headers,
            body,
        }
    }
}

/// Context describing an intercepted request, shared by the HTTP and WebSocket
/// seams.
#[derive(Clone)]
#[non_exhaustive]
pub struct LlmRequestContext {
    /// Opaque runtime-minted request id.
    pub request_id: String,
    /// Originating session id, if any.
    pub session_id: Option<String>,
    /// Transport the runtime would otherwise use.
    pub transport: LlmTransport,
    /// Request URL.
    pub url: String,
    /// Request headers.
    pub headers: HeaderMap,
    /// Triggered when the runtime cancels the request.
    pub cancel: CancellationToken,
}

/// A single WebSocket message flowing through a [`CopilotWebSocketHandler`].
#[derive(Clone)]
pub struct LlmWebSocketMessage {
    /// Message payload.
    pub data: Vec<u8>,
    /// Whether the payload is a binary frame (`true`) or a text frame (`false`).
    pub binary: bool,
}

impl LlmWebSocketMessage {
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
pub struct LlmWebSocketResponse {
    sink: LlmResponseSink,
}

impl LlmWebSocketResponse {
    fn new(sink: LlmResponseSink) -> Self {
        Self { sink }
    }

    /// Forward one upstream message to the runtime.
    pub async fn send_message(
        &self,
        message: LlmWebSocketMessage,
    ) -> Result<(), LlmInferenceError> {
        if message.binary {
            self.sink.write_binary(&message.data).await
        } else {
            let text = String::from_utf8_lossy(&message.data);
            self.sink.write_text(&text).await
        }
    }

    /// End the runtime response stream (the upstream connection closed).
    pub async fn close(&self) -> Result<(), LlmInferenceError> {
        self.sink.end().await
    }
}

/// A per-connection WebSocket handler. The default implementation
/// ([`ForwardingWebSocketHandler`]) bridges to the real upstream; override
/// [`LlmRequestHandler::open_websocket`] to supply a custom one.
#[async_trait]
pub trait CopilotWebSocketHandler: Send + Sync {
    /// Forward one runtime→upstream message.
    async fn send_request_message(
        &self,
        message: LlmWebSocketMessage,
    ) -> Result<(), LlmInferenceError>;

    /// Tear down the upstream connection.
    async fn close(&self) -> Result<(), LlmInferenceError>;
}

/// The idiomatic, high-level LLM inference seam.
///
/// One subclass services both transports. Defaults forward transparently to the
/// real upstream, so overriding nothing yields a pass-through; override a method
/// to mutate or replace traffic.
#[async_trait]
pub trait LlmRequestHandler: Send + Sync + 'static {
    /// Service one intercepted HTTP request. Default: forward to the real
    /// upstream via [`forward_http`]. Override to mutate the request before
    /// forwarding, mutate the response after, or replace the call entirely.
    async fn send_http(
        &self,
        request: LlmHttpRequest,
        _ctx: &LlmRequestContext,
    ) -> Result<LlmHttpResponse, LlmInferenceError> {
        forward_http(request).await
    }

    /// Open a per-connection WebSocket handler. Default: a
    /// [`ForwardingWebSocketHandler`] wired to the real upstream. Override to
    /// mutate the handshake (URL / headers via `ctx`) or return a custom
    /// handler. `response` is the runtime-facing sink for upstream messages.
    async fn open_websocket(
        &self,
        ctx: &LlmRequestContext,
        response: LlmWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, LlmInferenceError> {
        let handler = ForwardingWebSocketHandler::builder(ctx.url.clone(), ctx.headers.clone())
            .connect(response)
            .await?;
        Ok(Box::new(handler))
    }
}

#[async_trait]
impl<T: LlmRequestHandler> LlmInferenceProvider for T {
    async fn on_llm_request(&self, request: LlmInferenceRequest) -> Result<(), LlmInferenceError> {
        let LlmInferenceRequest {
            request_id,
            session_id,
            method,
            url,
            headers,
            transport,
            mut body,
            cancel,
            response,
        } = request;

        let ctx = LlmRequestContext {
            request_id,
            session_id,
            transport,
            url: url.clone(),
            headers: headers.clone(),
            cancel: cancel.clone(),
        };

        match transport {
            LlmTransport::Http => {
                let body_bytes = body.drain().await;
                let http_request = LlmHttpRequest {
                    method,
                    url,
                    headers,
                    body: body_bytes,
                    cancel: cancel.clone(),
                };
                let http_response = self.send_http(http_request, &ctx).await?;
                stream_http_response(http_response, &response, &cancel).await
            }
            LlmTransport::Websocket => {
                response.start(LlmResponseInit::new(101)).await?;
                let writer = LlmWebSocketResponse::new(response.clone());
                let ws = self.open_websocket(&ctx, writer).await?;
                let result = pump_websocket_requests(ws.as_ref(), &mut body, &cancel).await;
                let _ = ws.close().await;
                match result {
                    Ok(()) => response.end().await,
                    Err(err) if cancel.is_cancelled() => {
                        response
                            .error(
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
}

/// Stream an HTTP response into the runtime sink, honouring cancellation.
async fn stream_http_response(
    response: LlmHttpResponse,
    sink: &LlmResponseSink,
    cancel: &CancellationToken,
) -> Result<(), LlmInferenceError> {
    let mut init = LlmResponseInit::new(response.status).with_headers(response.headers);
    init.status_text = response.status_text;
    sink.start(init).await?;

    let mut body = response.body;
    loop {
        tokio::select! {
            _ = cancel.cancelled() => {
                return sink
                    .error("Request cancelled by runtime", Some("cancelled".to_string()))
                    .await;
            }
            next = body.next() => match next {
                Some(Ok(chunk)) => {
                    for piece in chunk.chunks(32 * 1024) {
                        sink.write_binary(piece).await?;
                    }
                }
                Some(Err(e)) => {
                    return sink.error(e.to_string(), None).await;
                }
                None => break,
            }
        }
    }
    sink.end().await
}

/// Forward runtime→upstream WebSocket messages until the runtime closes its side
/// or cancels.
async fn pump_websocket_requests(
    handler: &dyn CopilotWebSocketHandler,
    body: &mut LlmRequestBody,
    cancel: &CancellationToken,
) -> Result<(), LlmInferenceError> {
    loop {
        tokio::select! {
            _ = cancel.cancelled() => {
                return Err(LlmInferenceError::message("Request cancelled by runtime"));
            }
            frame = body.recv() => match frame {
                Some(data) => {
                    handler
                        .send_request_message(LlmWebSocketMessage { data, binary: false })
                        .await?;
                }
                None => return Ok(()),
            }
        }
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
/// This is the default behaviour of [`LlmRequestHandler::send_http`]; consumers
/// that mutate a request can call it to forward the mutated request.
pub async fn forward_http(request: LlmHttpRequest) -> Result<LlmHttpResponse, LlmInferenceError> {
    let method = reqwest::Method::from_bytes(request.method.as_bytes())
        .map_err(|e| LlmInferenceError::InvalidState(format!("invalid HTTP method: {e}")))?;

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
            return Err(LlmInferenceError::message("Request cancelled by runtime"));
        }
        result = builder.send() => result.map_err(|e| LlmInferenceError::Upstream(e.to_string()))?,
    };

    let status = response.status().as_u16();
    let status_text = response.status().canonical_reason().map(str::to_string);
    let headers = response.headers().clone();
    let body = response
        .bytes_stream()
        .map(|item| item.map_err(|e| LlmInferenceError::Upstream(e.to_string())));

    Ok(LlmHttpResponse {
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
    Arc<dyn Fn(LlmWebSocketMessage) -> Option<LlmWebSocketMessage> + Send + Sync>;

/// Builder for a [`ForwardingWebSocketHandler`].
pub struct ForwardingWebSocketHandlerBuilder {
    url: String,
    headers: HeaderMap,
    on_send_request_message: Option<WebSocketTransform>,
    on_send_response_message: Option<WebSocketTransform>,
}

impl ForwardingWebSocketHandlerBuilder {
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
        response: LlmWebSocketResponse,
    ) -> Result<ForwardingWebSocketHandler, LlmInferenceError> {
        let mut request = self
            .url
            .as_str()
            .into_client_request()
            .map_err(|e| LlmInferenceError::Upstream(format!("invalid websocket url: {e}")))?;
        for (name, value) in &self.headers {
            if is_forbidden_header(name) {
                continue;
            }
            request.headers_mut().append(name.clone(), value.clone());
        }

        let (stream, _) = connect_async(request)
            .await
            .map_err(|e| LlmInferenceError::Upstream(format!("websocket connect failed: {e}")))?;
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
                            let message = LlmWebSocketMessage::text(text);
                            if let Some(out) = apply_transform(&on_response, message) {
                                let _ = response.send_message(out).await;
                            }
                        }
                        Some(Ok(Message::Binary(data))) => {
                            let message = LlmWebSocketMessage::binary(data);
                            if let Some(out) = apply_transform(&on_response, message) {
                                let _ = response.send_message(out).await;
                            }
                        }
                        Some(Ok(Message::Close(_))) | None => break,
                        Some(Ok(_)) => continue,
                        Some(Err(e)) => {
                            let _ = response.sink.error(e.to_string(), None).await;
                            return;
                        }
                    }
                }
            }
            let _ = response.close().await;
        });

        Ok(ForwardingWebSocketHandler {
            write: Mutex::new(Some(write)),
            on_send_request_message: self.on_send_request_message,
            cancel,
        })
    }
}

/// The default WebSocket handler: forwards each runtime message to the real
/// upstream and each upstream message back to the runtime. Mutate by supplying
/// transforms on the [builder](ForwardingWebSocketHandler::builder).
pub struct ForwardingWebSocketHandler {
    write: Mutex<Option<UpstreamWrite>>,
    on_send_request_message: Option<WebSocketTransform>,
    cancel: CancellationToken,
}

impl ForwardingWebSocketHandler {
    /// Start building a forwarding handler for `url` with the given upstream
    /// handshake headers.
    pub fn builder(url: String, headers: HeaderMap) -> ForwardingWebSocketHandlerBuilder {
        ForwardingWebSocketHandlerBuilder {
            url,
            headers,
            on_send_request_message: None,
            on_send_response_message: None,
        }
    }
}

#[async_trait]
impl CopilotWebSocketHandler for ForwardingWebSocketHandler {
    async fn send_request_message(
        &self,
        message: LlmWebSocketMessage,
    ) -> Result<(), LlmInferenceError> {
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
                .map_err(|e| LlmInferenceError::Upstream(e.to_string()))?;
        }
        Ok(())
    }

    async fn close(&self) -> Result<(), LlmInferenceError> {
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
    message: LlmWebSocketMessage,
) -> Option<LlmWebSocketMessage> {
    match transform {
        Some(f) => f(message),
        None => Some(message),
    }
}
