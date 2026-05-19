//! Stdio transport — spawn the CLI as a child and exchange JSON-RPC over its stdio.

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

    let response = session.send_and_wait("What is the capital of France?").await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
