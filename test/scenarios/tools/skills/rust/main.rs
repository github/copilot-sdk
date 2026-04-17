use std::path::PathBuf;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                skill_directories: Some(vec![PathBuf::from("./skills")]),
                hooks: Some(true),
                request_permission: Some(true),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("Use the greeting skill to greet someone named Alice."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    println!("\nSkill directories configured successfully");

    Ok(())
}
