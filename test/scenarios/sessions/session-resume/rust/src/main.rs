//! Session resume — create a session, plant a memory, then resume by ID
//! and verify the agent recalls it.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{ResumeSessionConfig, SessionConfig};
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
        available_tools: Some(Vec::new()),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));
    let session = client.create_session(config).await?;

    session
        .send_and_wait("Remember this: the secret word is PINEAPPLE.")
        .await?;

    let session_id = session.id().clone();
    // Note: do NOT destroy — `resume_session` needs the session to persist.

    let resume_config =
        ResumeSessionConfig::new(session_id).with_handler(Arc::new(ApproveAllHandler));
    let resumed = client.resume_session(resume_config).await?;
    println!("Session resumed");

    let response = resumed
        .send_and_wait("What was the secret word I told you?")
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    resumed.destroy().await?;
    Ok(())
}
