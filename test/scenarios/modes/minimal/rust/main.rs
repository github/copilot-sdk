use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SessionEventData, SystemMessageConfig};
use copilot::{Client, ClientOptions, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        available_tools: Some(vec![]),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".into()),
            content: Some("You are a helpful assistant. Answer questions concisely.".into()),
            ..Default::default()
        }),
        ..Default::default()
    };

    let session = client
        .create_session(config, Arc::new(ApproveAllHandler), None, None)
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("Use the grep tool to search for 'SDK' in README.md."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        if let SessionEventData::AssistantMessage(d) = &event.data {
            println!("Response: {}", d.content);
        }
    }

    session.disconnect().await?;
    println!("Minimal mode test complete");
    Ok(())
}
