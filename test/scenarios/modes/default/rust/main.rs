use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SessionEventData};
use copilot::{Client, ClientOptions, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        ..Default::default()
    };

    let session = client
        .create_session(config, Arc::new(ApproveAllHandler), None, None)
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("Use the grep tool to search for the word 'SDK' in README.md and show the matching lines."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        if let SessionEventData::AssistantMessage(d) = &event.data {
            println!("Response: {}", d.content);
        }
    }

    session.disconnect().await?;
    println!("Default mode test complete");
    Ok(())
}
