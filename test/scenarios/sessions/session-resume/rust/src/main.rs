//! Session resume — create a session, plant a memory, then resume by ID
//! and verify the agent recalls it.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{ResumeSessionConfig, SessionConfig};
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.available_tools = Some(Vec::new());
    let config = config.with_handler(Arc::new(ApproveAllHandler));
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
