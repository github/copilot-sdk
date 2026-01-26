use crate::client::Client;
use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionConfig {
    pub system_message: Option<String>,
}

pub struct Session {
    client: Arc<Client>,
    pub id: String,
}

impl Session {
    pub async fn create(client: Arc<Client>, config: SessionConfig) -> Result<Self> {
        #[derive(Deserialize)]
        struct CreateSessionResponse {
            id: String,
        }

        let resp: CreateSessionResponse = client.send_request("session/create", config).await?;

        Ok(Self {
            client,
            id: resp.id,
        })
    }

    pub async fn send_message(&self, content: &str) -> Result<()> {
        let _resp: serde_json::Value = self
            .client
            .send_request(
                "session/send",
                serde_json::json!({
                    "sessionId": self.id,
                    "message": content
                }),
            )
            .await?;
        Ok(())
    }
}
