//! LLM inference callback — connection-level interception of model-layer
//! HTTP and WebSocket traffic.
//!
//! When [`ClientOptions::llm_inference`](crate::ClientOptions::llm_inference)
//! is set, the SDK registers itself as the runtime's LLM inference provider on
//! [`Client::start`](crate::Client::start). From then on, whenever the runtime
//! would issue a model-layer request (inference, `/models`, `/policy`, …) — for
//! both CAPI and BYOK sessions — it asks the registered
//! [`LlmInferenceProvider`] to service it instead of making the call itself.
//!
//! Two levels of API are available:
//!
//! * [`LlmInferenceProvider`] is the low-level seam: a single
//!   [`on_llm_request`](LlmInferenceProvider::on_llm_request) method receives the
//!   request verbatim (URL / method / headers, a body-frame stream, a
//!   cancellation token) and writes the response through an
//!   [`LlmResponseSink`].
//! * [`LlmRequestHandler`](crate::llm_request_handler::LlmRequestHandler) builds
//!   on top of it with idiomatic [`reqwest`] / WebSocket forwarding seams; most
//!   consumers should start there.
//!
//! # Cancellation
//!
//! [`LlmInferenceRequest::cancel`] is triggered when the runtime cancels the
//! in-flight request (for example because the agent turn was aborted). Forward
//! it to the upstream call so it is torn down too, and stop writing to the sink.

use std::collections::HashMap;
use std::sync::{Arc, Weak};

use async_trait::async_trait;
use http::HeaderMap;
use http::header::{HeaderName, HeaderValue};
use parking_lot::Mutex;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;

use crate::generated::api_types::{
    LlmInferenceHttpRequestStartTransport, LlmInferenceHttpResponseChunkError,
    LlmInferenceHttpResponseChunkRequest, LlmInferenceHttpResponseStartRequest,
};
use crate::{Client, ClientInner, RequestId};

/// Transport the runtime would otherwise use for an intercepted request.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LlmTransport {
    /// Plain HTTP or SSE. Each response body frame is an opaque byte range.
    Http,
    /// Full-duplex WebSocket. Each request/response body frame maps to exactly
    /// one WebSocket message.
    Websocket,
}

impl LlmTransport {
    pub(crate) fn from_wire(value: Option<LlmInferenceHttpRequestStartTransport>) -> Self {
        match value {
            Some(LlmInferenceHttpRequestStartTransport::Websocket) => Self::Websocket,
            _ => Self::Http,
        }
    }
}

/// An outbound model-layer request the runtime is asking the consumer to
/// service on its behalf.
///
/// Low-level by design: URL / method / headers verbatim, the request body
/// delivered as a stream of frames via [`body`](Self::body), and the response
/// written through [`response`](Self::response). The runtime does not classify
/// the request; consumers that need provider/endpoint information derive it
/// from the URL and headers.
#[non_exhaustive]
pub struct LlmInferenceRequest {
    /// Opaque runtime-minted id, stable across the request lifecycle.
    pub request_id: String,
    /// Id of the runtime session that triggered this request, or `None` when it
    /// was issued outside any session (for example the startup model catalog).
    pub session_id: Option<String>,
    /// HTTP method (`GET`, `POST`, …).
    pub method: String,
    /// Absolute request URL.
    pub url: String,
    /// Request headers, multi-valued.
    pub headers: HeaderMap,
    /// Transport the runtime would otherwise use.
    pub transport: LlmTransport,
    /// Request body frames, in order. For [`LlmTransport::Http`] this is the
    /// (possibly streamed) request body; for [`LlmTransport::Websocket`] each
    /// frame is one inbound WebSocket message.
    pub body: LlmRequestBody,
    /// Triggered when the runtime cancels this in-flight request.
    pub cancel: CancellationToken,
    /// Sink the consumer writes the upstream response into.
    pub response: LlmResponseSink,
}

/// The request body of an [`LlmInferenceRequest`], delivered as a stream of
/// frames.
pub struct LlmRequestBody {
    rx: mpsc::UnboundedReceiver<Vec<u8>>,
}

impl LlmRequestBody {
    pub(crate) fn new(rx: mpsc::UnboundedReceiver<Vec<u8>>) -> Self {
        Self { rx }
    }

    /// Receive the next body frame, or `None` once the body has ended (cleanly
    /// or via cancellation — check [`LlmInferenceRequest::cancel`] to tell them
    /// apart).
    pub async fn recv(&mut self) -> Option<Vec<u8>> {
        self.rx.recv().await
    }

    /// Drain the body to completion, concatenating every remaining frame.
    pub async fn drain(&mut self) -> Vec<u8> {
        let mut buf = Vec::new();
        while let Some(frame) = self.rx.recv().await {
            buf.extend_from_slice(&frame);
        }
        buf
    }
}

/// The response head passed to [`LlmResponseSink::start`].
#[non_exhaustive]
pub struct LlmResponseInit {
    /// HTTP status code.
    pub status: u16,
    /// Optional HTTP status reason phrase.
    pub status_text: Option<String>,
    /// Response headers.
    pub headers: HeaderMap,
}

impl LlmResponseInit {
    /// Construct a response head with the given status and no headers.
    pub fn new(status: u16) -> Self {
        Self {
            status,
            status_text: None,
            headers: HeaderMap::new(),
        }
    }

    /// Set the status reason phrase.
    pub fn with_status_text(mut self, status_text: impl Into<String>) -> Self {
        self.status_text = Some(status_text.into());
        self
    }

    /// Set the response headers.
    pub fn with_headers(mut self, headers: HeaderMap) -> Self {
        self.headers = headers;
        self
    }
}

/// Error returned by an [`LlmInferenceProvider`] or [`LlmResponseSink`].
#[derive(Debug)]
#[non_exhaustive]
pub enum LlmInferenceError {
    /// The runtime dropped the request (it acknowledged a response frame with
    /// `accepted: false`), so the consumer should abort its upstream work.
    RejectedByRuntime,

    /// The sink was used after the RPC connection to the runtime closed.
    ConnectionClosed,

    /// The sink's state machine was violated (for example `start` called twice,
    /// or a write before `start`).
    InvalidState(String),

    /// An upstream transport failure while forwarding the request.
    Upstream(String),

    /// A failure surfaced by the consumer's own handler.
    Handler(String),

    /// An RPC error talking to the runtime.
    Rpc(crate::Error),
}

impl LlmInferenceError {
    /// Construct a handler-level error from a message — the idiomatic way for a
    /// consumer to fail an inference request.
    pub fn message(message: impl Into<String>) -> Self {
        Self::Handler(message.into())
    }
}

impl std::fmt::Display for LlmInferenceError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::RejectedByRuntime => f.write_str(
                "LLM inference response was rejected by the runtime (request no longer active)",
            ),
            Self::ConnectionClosed => {
                f.write_str("LLM inference response sink used after RPC connection closed")
            }
            Self::InvalidState(message) | Self::Upstream(message) | Self::Handler(message) => {
                f.write_str(message)
            }
            Self::Rpc(err) => write!(f, "{err}"),
        }
    }
}

impl std::error::Error for LlmInferenceError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Rpc(err) => Some(err),
            _ => None,
        }
    }
}

impl From<crate::Error> for LlmInferenceError {
    fn from(err: crate::Error) -> Self {
        Self::Rpc(err)
    }
}

/// The low-level LLM inference registration seam.
///
/// Implementors service intercepted model-layer requests. The same callback
/// handles both buffered and streaming responses by calling
/// [`LlmResponseSink::write_text`] / [`LlmResponseSink::write_binary`] zero or
/// more times before [`LlmResponseSink::end`]. Returning an `Err` surfaces a
/// transport-level failure to the runtime (equivalent to
/// [`LlmResponseSink::error`] when `start` has not yet been called).
///
/// Most consumers should use
/// [`LlmRequestHandler`](crate::llm_request_handler::LlmRequestHandler), which
/// implements this trait with idiomatic HTTP/WebSocket forwarding.
#[async_trait]
pub trait LlmInferenceProvider: Send + Sync + 'static {
    /// Service one intercepted model-layer request. The implementor must
    /// eventually finalize the response via [`LlmResponseSink::end`] or
    /// [`LlmResponseSink::error`]; returning `Err` is treated as a transport
    /// failure.
    async fn on_llm_request(&self, request: LlmInferenceRequest) -> Result<(), LlmInferenceError>;
}

/// Configuration for a connection-level LLM inference callback.
///
/// When set on [`ClientOptions::llm_inference`](crate::ClientOptions::llm_inference),
/// the SDK registers as the inference provider on connect, and the runtime
/// routes its model-layer HTTP and WebSocket traffic through the provider
/// instead of issuing the calls itself.
#[derive(Clone)]
#[non_exhaustive]
pub struct LlmInferenceConfig {
    /// Services intercepted requests.
    pub provider: Arc<dyn LlmInferenceProvider>,
}

impl LlmInferenceConfig {
    /// Build a config from a provider.
    pub fn new(provider: Arc<dyn LlmInferenceProvider>) -> Self {
        Self { provider }
    }
}

impl std::fmt::Debug for LlmInferenceConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("LlmInferenceConfig")
            .field("provider", &"<set>")
            .finish()
    }
}

/// Mutable flags tracking the response sink's state machine. Shared between the
/// dispatcher (which may flip `cancelled`) and the [`LlmResponseSink`].
#[derive(Default)]
pub(crate) struct SinkFlags {
    pub(crate) started: bool,
    pub(crate) finished: bool,
    pub(crate) cancelled: bool,
}

/// State shared between the dispatcher and a request's [`LlmResponseSink`].
pub(crate) struct LlmShared {
    pub(crate) request_id: String,
    pub(crate) flags: Mutex<SinkFlags>,
    pub(crate) cancel: CancellationToken,
    pub(crate) client: Weak<ClientInner>,
}

/// The sink a consumer writes an upstream response into.
///
/// The state machine is strict: [`start`](Self::start) once, then zero or more
/// [`write_text`](Self::write_text) / [`write_binary`](Self::write_binary)
/// calls, then exactly one of [`end`](Self::end) or [`error`](Self::error).
#[derive(Clone)]
pub struct LlmResponseSink {
    shared: Arc<LlmShared>,
}

impl LlmResponseSink {
    pub(crate) fn new(shared: Arc<LlmShared>) -> Self {
        Self { shared }
    }

    fn client(&self) -> Result<Client, LlmInferenceError> {
        self.shared
            .client
            .upgrade()
            .map(Client::from_inner)
            .ok_or(LlmInferenceError::ConnectionClosed)
    }

    fn request_id(&self) -> RequestId {
        RequestId::new(self.shared.request_id.clone())
    }

    /// Send the response head (status + headers) back to the runtime. Must be
    /// called exactly once, before any body frames.
    pub async fn start(&self, init: LlmResponseInit) -> Result<(), LlmInferenceError> {
        {
            let mut flags = self.shared.flags.lock();
            if flags.started {
                return Err(LlmInferenceError::InvalidState(
                    "response sink start() called twice".to_string(),
                ));
            }
            if flags.finished {
                return Err(LlmInferenceError::InvalidState(
                    "response sink already finished".to_string(),
                ));
            }
            flags.started = true;
        }
        let client = self.client()?;
        let request = LlmInferenceHttpResponseStartRequest {
            headers: headers_to_wire(&init.headers),
            request_id: self.request_id(),
            status: i64::from(init.status),
            status_text: init.status_text,
        };
        let result = client
            .rpc()
            .llm_inference()
            .http_response_start(request)
            .await?;
        if !result.accepted {
            return Err(self.rejected_by_runtime());
        }
        Ok(())
    }

    /// Send a body frame as UTF-8 text (the common case for JSON / SSE).
    pub async fn write_text(&self, text: &str) -> Result<(), LlmInferenceError> {
        self.write(text.to_string(), false).await
    }

    /// Send a body frame as raw bytes (base64-encoded on the wire).
    pub async fn write_binary(&self, data: &[u8]) -> Result<(), LlmInferenceError> {
        use base64::Engine;
        let encoded = base64::engine::general_purpose::STANDARD.encode(data);
        self.write(encoded, true).await
    }

    async fn write(&self, data: String, binary: bool) -> Result<(), LlmInferenceError> {
        {
            let flags = self.shared.flags.lock();
            if flags.cancelled {
                return Err(LlmInferenceError::InvalidState(
                    "request was cancelled by the runtime".to_string(),
                ));
            }
            if !flags.started {
                return Err(LlmInferenceError::InvalidState(
                    "response sink write called before start()".to_string(),
                ));
            }
            if flags.finished {
                return Err(LlmInferenceError::InvalidState(
                    "response sink write called after end()/error()".to_string(),
                ));
            }
        }
        let client = self.client()?;
        let request = LlmInferenceHttpResponseChunkRequest {
            binary: binary.then_some(true),
            data,
            end: Some(false),
            error: None,
            request_id: self.request_id(),
        };
        let result = client
            .rpc()
            .llm_inference()
            .http_response_chunk(request)
            .await?;
        if !result.accepted {
            return Err(self.rejected_by_runtime());
        }
        Ok(())
    }

    /// Mark end-of-stream cleanly.
    pub async fn end(&self) -> Result<(), LlmInferenceError> {
        {
            let mut flags = self.shared.flags.lock();
            if flags.finished {
                return Ok(());
            }
            flags.finished = true;
        }
        let client = self.client()?;
        let request = LlmInferenceHttpResponseChunkRequest {
            binary: None,
            data: String::new(),
            end: Some(true),
            error: None,
            request_id: self.request_id(),
        };
        client
            .rpc()
            .llm_inference()
            .http_response_chunk(request)
            .await?;
        Ok(())
    }

    /// Mark end-of-stream with a transport-level failure. `code` is optional.
    pub async fn error(
        &self,
        message: impl Into<String>,
        code: Option<String>,
    ) -> Result<(), LlmInferenceError> {
        {
            let mut flags = self.shared.flags.lock();
            if flags.finished {
                return Ok(());
            }
            flags.finished = true;
        }
        let client = self.client()?;
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
        client
            .rpc()
            .llm_inference()
            .http_response_chunk(request)
            .await?;
        Ok(())
    }

    /// Invoked when the runtime acknowledges a frame with `accepted: false`:
    /// the request is no longer active, so cancel the consumer's upstream work.
    fn rejected_by_runtime(&self) -> LlmInferenceError {
        {
            let mut flags = self.shared.flags.lock();
            flags.cancelled = true;
            flags.finished = true;
        }
        self.shared.cancel.cancel();
        LlmInferenceError::RejectedByRuntime
    }

    pub(crate) fn is_finished(&self) -> bool {
        self.shared.flags.lock().finished
    }

    pub(crate) fn is_started(&self) -> bool {
        self.shared.flags.lock().started
    }

    pub(crate) fn is_cancelled(&self) -> bool {
        self.shared.flags.lock().cancelled
    }
}

/// Convert a wire header map into an [`http::HeaderMap`], skipping any entry
/// the `http` crate rejects.
pub(crate) fn headers_from_wire(wire: &HashMap<String, Vec<String>>) -> HeaderMap {
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
pub(crate) fn headers_to_wire(headers: &HeaderMap) -> HashMap<String, Vec<String>> {
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
