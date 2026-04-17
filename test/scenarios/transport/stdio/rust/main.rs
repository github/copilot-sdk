use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{CliProgram, Client, ClientOptions, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let program = std::env::var("COPILOT_CLI_PATH")
        .ok()
        .map(|p| CliProgram::Path(p.into()))
        .unwrap_or(CliProgram::Resolve);

    let client = Client::start(ClientOptions {
        program,
        ..Default::default()
    })
    .await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                available_tools: Some(vec![]),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(MessageOptions::new("What is the capital of France?"), None)
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    Ok(())
}
