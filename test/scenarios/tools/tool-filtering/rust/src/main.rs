//! Tool filtering — restrict the agent to a subset of built-in tools via
//! `SessionConfig::available_tools`.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{SessionConfig, SystemMessageConfig};
use github_copilot_sdk::{Client, ClientOptions};

const SYSTEM_PROMPT: &str = "You are a helpful assistant. You have access to a limited set \
of tools. When asked about your tools, list exactly which tools you have available.";

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut sysmsg = SystemMessageConfig::default();
    sysmsg.mode = Some("replace".to_string());
    sysmsg.content = Some(SYSTEM_PROMPT.to_string());

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.system_message = Some(sysmsg);
    config.available_tools = Some(vec![
        "grep".to_string(),
        "glob".to_string(),
        "view".to_string(),
    ]);
    let config = config.with_handler(Arc::new(ApproveAllHandler));

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
