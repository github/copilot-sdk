//! Streaming session — count `assistant.message_delta` events while waiting
//! for the final response.

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::SessionConfig;
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let client = Client::start(ClientOptions::default()).await?;

    let chunks = Arc::new(AtomicUsize::new(0));

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.streaming = Some(true);
    let config = config.with_permission_handler(Arc::new(ApproveAllHandler));
    let session = client.create_session(config).await?;

    let mut events = session.subscribe();
    let chunks_clone = chunks.clone();
    let counter = tokio::spawn(async move {
        while let Ok(event) = events.recv().await {
            if event.event_type == "assistant.message_delta" {
                chunks_clone.fetch_add(1, Ordering::Relaxed);
            }
        }
    });

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

    session.disconnect().await?;
    drop(counter);
    Ok(())
}
