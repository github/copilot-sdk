//! Streaming session — count `assistant.message_delta` events while waiting
//! for the final response.

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use copilot::handler::{HandlerEvent, HandlerResponse, PermissionResult, SessionHandler};
use copilot::types::SessionConfig;
use copilot::{Client, ClientOptions};

struct StreamCounter {
    chunks: Arc<AtomicUsize>,
}

#[async_trait]
impl SessionHandler for StreamCounter {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::SessionEvent { event, .. } => {
                if event.event_type == "assistant.message_delta" {
                    self.chunks.fetch_add(1, Ordering::Relaxed);
                }
                HandlerResponse::Ok
            }
            HandlerEvent::PermissionRequest { .. } => {
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            _ => HandlerResponse::Ok,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let chunks = Arc::new(AtomicUsize::new(0));
    let handler = Arc::new(StreamCounter {
        chunks: chunks.clone(),
    });

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        streaming: Some(true),
        ..Default::default()
    }
    .with_handler(handler);
    let session = client.create_session(config).await?;

    let response = session.send_and_wait("What is the capital of France?").await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    println!(
        "\nStreaming chunks received: {}",
        chunks.load(Ordering::Relaxed)
    );

    session.destroy().await?;
    Ok(())
}
