//! No-tools session — replace the system prompt and empty the available tools
//! list so the agent cannot execute code, read files, or call any built-ins.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{SessionConfig, SystemMessageConfig};
use github_copilot_sdk::{Client, ClientOptions};

const SYSTEM_PROMPT: &str = "You are a minimal assistant with no tools available.
You cannot execute code, read files, edit files, search, or perform any actions.
You can only respond with text based on your training data.
If asked about your capabilities or tools, clearly state that you have no tools available.";

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
    config.available_tools = Some(Vec::new());
    let config = config.with_handler(Arc::new(ApproveAllHandler));
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
