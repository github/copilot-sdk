//! Inbound `llmInference.*` JSON-RPC request dispatch.
//!
//! Internal — the public-facing trait lives in [`crate::llm_inference`]. Unlike
//! `sessionFs.*`, these requests are client-global (not routed per session) and
//! carry a streaming body: an `httpRequestStart` opens a request, subsequent
//! `httpRequestChunk`s feed its body, and the registered
//! [`LlmInferenceProvider`] writes the response back through an
//! [`LlmResponseSink`].

use std::collections::HashMap;
use std::sync::{Arc, OnceLock, Weak};

use base64::Engine;
use parking_lot::Mutex;
use tokio::sync::mpsc;
use tokio_util::sync::CancellationToken;
use tracing::warn;

use crate::generated::api_types::{
    LlmInferenceHttpRequestChunkRequest, LlmInferenceHttpRequestStartRequest,
};
use crate::llm_inference::{
    LlmInferenceError, LlmInferenceProvider, LlmInferenceRequest, LlmRequestBody, LlmResponseInit,
    LlmResponseSink, LlmShared, LlmTransport, SinkFlags, headers_from_wire,
};
use crate::{Client, ClientInner, JsonRpcRequest, JsonRpcResponse, error_codes};

const METHOD_HTTP_REQUEST_START: &str = "llmInference.httpRequestStart";
const METHOD_HTTP_REQUEST_CHUNK: &str = "llmInference.httpRequestChunk";

struct PendingEntry {
    shared: Arc<LlmShared>,
    /// Sender feeding the request body stream. Dropped (set to `None`) on
    /// `end` or `cancel` to close the stream.
    body_tx: Option<mpsc::UnboundedSender<Vec<u8>>>,
}

/// Routes inbound `llmInference.*` requests to the registered provider,
/// reassembling each request's streaming body and acking every frame.
pub(crate) struct LlmInferenceDispatcher {
    provider: Arc<dyn LlmInferenceProvider>,
    client: OnceLock<Weak<ClientInner>>,
    pending: Mutex<HashMap<String, PendingEntry>>,
    /// Chunks that arrived before their `httpRequestStart` (defensive — the
    /// runtime orders them, but ordering across the napi hop is not contractual).
    staged: Mutex<HashMap<String, Vec<LlmInferenceHttpRequestChunkRequest>>>,
}

impl LlmInferenceDispatcher {
    pub(crate) fn new(provider: Arc<dyn LlmInferenceProvider>) -> Self {
        Self {
            provider,
            client: OnceLock::new(),
            pending: Mutex::new(HashMap::new()),
            staged: Mutex::new(HashMap::new()),
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

        let request_id = params.request_id.into_inner();
        let (body_tx, body_rx) = mpsc::unbounded_channel();
        let shared = Arc::new(LlmShared {
            request_id: request_id.clone(),
            flags: Mutex::new(SinkFlags::default()),
            cancel: CancellationToken::new(),
            client: self.client_weak(),
        });
        let sink = LlmResponseSink::new(shared.clone());

        self.pending.lock().insert(
            request_id.clone(),
            PendingEntry {
                shared: shared.clone(),
                body_tx: Some(body_tx),
            },
        );

        let inference_request = LlmInferenceRequest {
            request_id: request_id.clone(),
            session_id: params.session_id.map(|s| s.into_inner()),
            method: params.method,
            url: params.url,
            headers: headers_from_wire(&params.headers),
            transport: LlmTransport::from_wire(params.transport),
            body: LlmRequestBody::new(body_rx),
            cancel: shared.cancel.clone(),
            response: sink.clone(),
        };

        let provider = self.provider.clone();
        let dispatcher = Arc::clone(self);
        tokio::spawn(async move {
            let result = provider.on_llm_request(inference_request).await;
            finalize(&sink, result).await;
            dispatcher.remove_pending(&request_id);
        });

        // Replay any chunks that beat the start over the wire.
        let staged = self.staged.lock().remove(shared.request_id.as_str());
        if let Some(chunks) = staged {
            let mut pending = self.pending.lock();
            if let Some(entry) = pending.get_mut(shared.request_id.as_str()) {
                for chunk in &chunks {
                    apply_chunk(entry, chunk);
                }
            }
        }

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
        {
            let mut pending = self.pending.lock();
            if let Some(entry) = pending.get_mut(&request_id) {
                apply_chunk(entry, &params);
            } else {
                drop(pending);
                self.staged
                    .lock()
                    .entry(request_id)
                    .or_default()
                    .push(params);
            }
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
fn apply_chunk(entry: &mut PendingEntry, params: &LlmInferenceHttpRequestChunkRequest) {
    if params.cancel == Some(true) {
        entry.shared.flags.lock().cancelled = true;
        entry.shared.cancel.cancel();
        entry.body_tx = None;
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
        if let Some(tx) = &entry.body_tx {
            let _ = tx.send(decoded);
        }
    }

    if params.end == Some(true) {
        entry.body_tx = None;
    }
}

/// Drive the response sink to a terminal state once the provider returns,
/// covering providers that error, get cancelled, or forget to finalize.
async fn finalize(sink: &LlmResponseSink, result: Result<(), LlmInferenceError>) {
    match result {
        Ok(()) => {
            if !sink.is_finished() {
                fail_via_sink(
                    sink,
                    "LLM inference provider returned without finalising the response".to_string(),
                )
                .await;
            }
        }
        Err(err) => {
            if sink.is_finished() {
                return;
            }
            if sink.is_cancelled() {
                if !sink.is_started() {
                    let _ = sink.start(LlmResponseInit::new(499)).await;
                }
                let _ = sink
                    .error(
                        "Request cancelled by runtime",
                        Some("cancelled".to_string()),
                    )
                    .await;
            } else {
                fail_via_sink(sink, err.to_string()).await;
            }
        }
    }
}

async fn fail_via_sink(sink: &LlmResponseSink, message: String) {
    if !sink.is_started() {
        let _ = sink.start(LlmResponseInit::new(502)).await;
    }
    let _ = sink.error(message, None).await;
}

fn parse_params<T: serde::de::DeserializeOwned>(request: &JsonRpcRequest) -> Option<T> {
    request
        .params
        .as_ref()
        .and_then(|p| serde_json::from_value(p.clone()).ok())
}
