//! Default agent mode — the agent has access to built-in tools (grep, view, etc.)
//! and can use them to complete a task.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::SessionConfig;
use copilot::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));
    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait(
            "Use the grep tool to search for the word 'SDK' in README.md and show the matching lines.",
        )
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("Response: {content}");
        }
    }

    println!("Default mode test complete");
    session.destroy().await?;
    Ok(())
}
