//! Reasoning effort — set the model's reasoning depth via
//! `SessionConfig::reasoning_effort`.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{SessionConfig, SystemMessageConfig};
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut sysmsg = SystemMessageConfig::default();
    sysmsg.mode = Some("replace".to_string());
    sysmsg.content = Some("You are a helpful assistant. Answer concisely.".to_string());

    let mut config = SessionConfig::default();
    config.model = Some("claude-opus-4.6".to_string());
    config.reasoning_effort = Some("low".to_string());
    config.available_tools = Some(Vec::new());
    config.system_message = Some(sysmsg);
    let config = config.with_handler(Arc::new(ApproveAllHandler));

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
