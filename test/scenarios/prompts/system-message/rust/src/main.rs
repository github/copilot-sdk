//! Custom system message — replace the built-in prompt entirely.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

const PIRATE_PROMPT: &str = "You are a pirate. Always respond in pirate speak. Say 'Arrr!' \
in every response. Use nautical terms and pirate slang throughout.";

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".to_string()),
            content: Some(PIRATE_PROMPT.to_string()),
            ..Default::default()
        }),
        available_tools: Some(Vec::new()),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));

    let session = client.create_session(config).await?;

    let response = session.send_and_wait("What is the capital of France?").await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
