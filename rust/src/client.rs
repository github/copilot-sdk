use crate::jsonrpc::JsonRpcTransport;
use crate::types::{AgentConfig, LogLevel, LogMessage, ProtocolEvent};
use anyhow::Result;
use serde_json::json;
use tokio::sync::mpsc;

// prototype, must be improved
const PROTOCOL_VERSION: &str = "1.0.0";

pub struct Client {
    transport: JsonRpcTransport,
    incoming_events: Option<mpsc::Receiver<ProtocolEvent>>,
}

impl Client {
    pub fn new() -> Self {
        let (tx, rx) = mpsc::channel(100);

        let transport = JsonRpcTransport::new(tx);

        Self {
            transport,
            incoming_events: Some(rx),
        }
    }

    pub async fn initialize(&self, config: AgentConfig) -> Result<()> {
        let init_params = json!({
            "agentInfo": {
                "id": config.agent_id,
                "name": config.agent_name,
                "version": config.version,
                "capabilities": config.capabilities
            },
            "sdkVersion": env!("CARGO_PKG_VERSION"),
            "protocolVersion": PROTOCOL_VERSION
        });


        let message = json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": init_params
        });

        self.transport.send(&message).await?;

        Ok(())
    }

    pub async fn log(&self, level: LogLevel, message: &str) -> Result<()> {
        let log_payload = LogMessage {
            level,
            message: message.to_string(),
            metadata: None,
        };

        let notification = json!({
            "jsonrpc": "2.0",
            "method": "logMessage",
            "params": log_payload
        });

        self.transport.send(&notification).await
    }

    pub fn take_event_receiver(&mut self) -> Option<mpsc::Receiver<ProtocolEvent>> {
        self.incoming_events.take()
    }
}
