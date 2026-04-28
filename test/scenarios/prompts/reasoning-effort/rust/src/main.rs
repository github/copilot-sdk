//! Reasoning effort — set the model's reasoning depth via
//! `SessionConfig::reasoning_effort`.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let config = SessionConfig {
        model: Some("claude-opus-4.6".to_string()),
        reasoning_effort: Some("low".to_string()),
        available_tools: Some(Vec::new()),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".to_string()),
            content: Some("You are a helpful assistant. Answer concisely.".to_string()),
            ..Default::default()
        }),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));

    let session = client.create_session(config).await?;

    let response = session.send_and_wait("What is the capital of France?").await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("Reasoning effort: low");
            println!("Response: {content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
