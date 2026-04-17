use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::{HandlerEvent, HandlerResponse, SessionHandler};
use copilot::{Client, ClientOptions, MessageOptions, SessionConfig, SessionEventType};

struct DeltaCountingHandler {
    delta_count: Arc<AtomicUsize>,
}

#[async_trait]
impl SessionHandler for DeltaCountingHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        if let HandlerEvent::SessionEvent { event, .. } = &event {
            if event.event_type == SessionEventType::AssistantMessageDelta {
                self.delta_count.fetch_add(1, Ordering::Relaxed);
            }
        }
        HandlerResponse::Ok
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let delta_count = Arc::new(AtomicUsize::new(0));
    let handler = Arc::new(DeltaCountingHandler {
        delta_count: delta_count.clone(),
    });

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                streaming: Some(true),
                ..Default::default()
            },
            handler,
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(MessageOptions::new("What is the capital of France?"), None)
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    println!(
        "\nStreaming chunks received: {}",
        delta_count.load(Ordering::Relaxed)
    );

    Ok(())
}
