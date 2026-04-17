use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, ResumeSessionConfig, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;
    let handler = || Arc::new(ApproveAllHandler) as Arc<dyn copilot::handler::SessionHandler>;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                available_tools: Some(vec![]),
                ..Default::default()
            },
            handler(),
            None,
            None,
        )
        .await?;

    session
        .send_and_wait(
            MessageOptions::new("Remember this: the secret word is PINEAPPLE."),
            None,
        )
        .await?;

    let session_id = session.id().clone();

    let resumed = client
        .resume_session(
            ResumeSessionConfig {
                session_id,
                ..Default::default()
            },
            handler(),
            None,
            None,
        )
        .await?;
    println!("Session resumed");

    let response = resumed
        .send_and_wait(MessageOptions::new("What was the secret word I told you?"), None)
        .await?;

    match response.event {
        Some(event) => println!("{}", event.data),
        None => println!("(no response)"),
    }

    Ok(())
}
