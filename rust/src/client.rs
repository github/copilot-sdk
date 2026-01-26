use crate::jsonrpc::JsonRpcTransport;
use crate::types::{AgentConfig, ProtocolEvent};
use anyhow::{Context, Result};
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use tokio::sync::{Mutex, mpsc, oneshot};

#[derive(Debug, Deserialize)]
struct JsonRpcResponse {
    id: Option<u64>,
    result: Option<Value>,
    error: Option<Value>,
    method: Option<String>,
    params: Option<Value>,
}

pub struct Client {
    transport: JsonRpcTransport,
    next_id: AtomicU64,
    pending_requests: Arc<Mutex<HashMap<u64, oneshot::Sender<Result<Value>>>>>,
    events_tx: mpsc::Sender<ProtocolEvent>,
    events_rx: Option<mpsc::Receiver<ProtocolEvent>>,
}

impl Client {
    pub fn new() -> Self {
        let (events_tx, events_rx) = mpsc::channel(100);
        let pending_requests: Arc<Mutex<HashMap<u64, oneshot::Sender<Result<Value>>>>> =
            Arc::new(Mutex::new(HashMap::new()));
        let (transport_in_tx, mut transport_in_rx) = mpsc::channel::<String>(100);

        let transport = JsonRpcTransport::new(transport_in_tx);
        let pending_requests_clone = pending_requests.clone();
        let events_tx_clone = events_tx.clone();

        tokio::spawn(async move {
            while let Some(raw_msg) = transport_in_rx.recv().await {
                if let Ok(msg) = serde_json::from_str::<JsonRpcResponse>(&raw_msg) {
                    if let Some(id) = msg.id {
                        let mut map = pending_requests_clone.lock().await;
                        if let Some(tx) = map.remove(&id) {
                            let result = if let Some(err) = msg.error {
                                Err(anyhow::anyhow!("RPC Error: {:?}", err))
                            } else {
                                Ok(msg.result.unwrap_or(Value::Null))
                            };
                            let _ = tx.send(result);
                            continue;
                        }
                    }

                    if let Some(method) = msg.method {
                        let event = ProtocolEvent {
                            event: method,
                            payload: msg.params.unwrap_or(Value::Null),
                        };
                        let _ = events_tx_clone.send(event).await;
                    }
                }
            }
        });

        Self {
            transport,
            next_id: AtomicU64::new(1),
            pending_requests,
            events_tx,
            events_rx: Some(events_rx),
        }
    }

    pub async fn send_request<P: Serialize, R: DeserializeOwned>(
        &self,
        method: &str,
        params: P,
    ) -> Result<R> {
        let id = self.next_id.fetch_add(1, Ordering::SeqCst);
        let (tx, rx) = oneshot::channel();

        {
            let mut map = self.pending_requests.lock().await;
            map.insert(id, tx);
        }

        let req = serde_json::json!({
            "jsonrpc": "2.0",
            "id": id,
            "method": method,
            "params": params
        });

        if let Err(e) = self.transport.send(&req).await {
            let mut map = self.pending_requests.lock().await;
            map.remove(&id);
            return Err(e.into());
        }

        let response_value = rx.await.context("Client dropped or connection closed")??;
        serde_json::from_value(response_value).map_err(Into::into)
    }

    pub async fn initialize(&self, config: AgentConfig) -> Result<()> {
        let _resp: Value = self
            .send_request(
                "initialize",
                serde_json::json!({
                    "agentInfo": {
                        "id": config.agent_id,
                        "name": config.agent_name,
                        "version": config.version,
                        "capabilities": config.capabilities
                    },
                    "sdkVersion": env!("CARGO_PKG_VERSION"),
                    "protocolVersion": "1.0.0"
                }),
            )
            .await?;

        self.transport
            .send(&serde_json::json!({
                "jsonrpc": "2.0",
                "method": "initialized",
                "params": {}
            }))
            .await?;

        Ok(())
    }

    pub fn take_event_receiver(&mut self) -> Option<mpsc::Receiver<ProtocolEvent>> {
        self.events_rx.take()
    }
}
