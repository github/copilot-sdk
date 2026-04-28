//! Stdio transport — spawn the CLI as a child and exchange JSON-RPC over its stdio.

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

    let response = session.send_and_wait("What is the capital of France?").await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
