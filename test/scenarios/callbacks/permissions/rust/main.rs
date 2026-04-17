use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::{HandlerEvent, HandlerResponse, PermissionResult, SessionHandler};
use copilot::types::{SessionConfig, SessionEventData};
use copilot::{Client, ClientOptions, MessageOptions};

struct PermissionHandler {
    count: AtomicUsize,
}

#[async_trait]
impl SessionHandler for PermissionHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::PermissionRequest { ref data, .. } => {
                let tool = data
                    .get("toolName")
                    .and_then(|v| v.as_str())
                    .unwrap_or("unknown");
                let n = self.count.fetch_add(1, Ordering::SeqCst) + 1;
                println!("Permission #{n}: approved tool '{tool}'");
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            _ => HandlerResponse::Ok,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let handler = Arc::new(PermissionHandler {
        count: AtomicUsize::new(0),
    });

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        request_permission: Some(true),
        ..Default::default()
    };

    let session = client
        .create_session(config, handler.clone(), None, None)
        .await?;

    let response = session
        .send_and_wait(MessageOptions::new("List the files in the current directory using glob with pattern '*.md'."), None)
        .await?;

    if let Some(event) = response.event {
        if let SessionEventData::AssistantMessage(d) = &event.data {
            println!("Response: {}", d.content);
        }
    }

    println!(
        "\nTotal permission requests: {}",
        handler.count.load(Ordering::SeqCst)
    );
    session.disconnect().await?;
    Ok(())
}
