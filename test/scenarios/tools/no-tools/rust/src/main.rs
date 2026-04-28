//! No-tools session — replace the system prompt and empty the available tools
//! list so the agent cannot execute code, read files, or call any built-ins.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

const SYSTEM_PROMPT: &str = "You are a minimal assistant with no tools available.
You cannot execute code, read files, edit files, search, or perform any actions.
You can only respond with text based on your training data.
If asked about your capabilities or tools, clearly state that you have no tools available.";

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
        available_tools: Some(Vec::new()),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));
    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait("Use the bash tool to run 'echo hello'.")
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
