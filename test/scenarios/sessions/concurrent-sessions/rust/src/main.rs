//! Concurrent sessions — two sessions on a single client running in
//! parallel with different system prompts.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{SessionConfig, SystemMessageConfig};
use github_copilot_sdk::{Client, ClientOptions};

const PIRATE_PROMPT: &str = "You are a pirate. Always say Arrr!";
const ROBOT_PROMPT: &str = "You are a robot. Always say BEEP BOOP!";

fn make_config(system: &str) -> SessionConfig {
    let mut sysmsg = SystemMessageConfig::default();
    sysmsg.mode = Some("replace".to_string());
    sysmsg.content = Some(system.to_string());

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.system_message = Some(sysmsg);
    config.available_tools = Some(Vec::new());
    config.with_handler(Arc::new(ApproveAllHandler))
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let session1 = client.create_session(make_config(PIRATE_PROMPT)).await?;
    let session2 = client.create_session(make_config(ROBOT_PROMPT)).await?;

    let (r1, r2) = tokio::join!(
        session1.send_and_wait("What is the capital of France?"),
        session2.send_and_wait("What is the capital of France?"),
    );

    if let Some(event) = r1? {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("Session 1 (pirate): {content}");
        }
    }
    if let Some(event) = r2? {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("Session 2 (robot): {content}");
        }
    }

    session1.destroy().await?;
    session2.destroy().await?;
    Ok(())
}
