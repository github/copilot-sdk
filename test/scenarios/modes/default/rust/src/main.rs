//! Default agent mode — the agent has access to built-in tools (grep, view, etc.)
//! and can use them to complete a task.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::SessionConfig;
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    let config = config.with_handler(Arc::new(ApproveAllHandler));
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
