//! Concurrent sessions — two sessions on a single client running in
//! parallel with different system prompts.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

const PIRATE_PROMPT: &str = "You are a pirate. Always say Arrr!";
const ROBOT_PROMPT: &str = "You are a robot. Always say BEEP BOOP!";

fn make_config(system: &str) -> SessionConfig {
    SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".to_string()),
            content: Some(system.to_string()),
            ..Default::default()
        }),
        available_tools: Some(Vec::new()),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler))
}

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

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
