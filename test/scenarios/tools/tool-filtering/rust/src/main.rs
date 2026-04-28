//! Tool filtering — restrict the agent to a subset of built-in tools via
//! `SessionConfig::available_tools`.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

const SYSTEM_PROMPT: &str = "You are a helpful assistant. You have access to a limited set \
of tools. When asked about your tools, list exactly which tools you have available.";

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
            content: Some(SYSTEM_PROMPT.to_string()),
            ..Default::default()
        }),
        available_tools: Some(vec![
            "grep".to_string(),
            "glob".to_string(),
            "view".to_string(),
        ]),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait("What tools do you have available? List each one by name.")
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
