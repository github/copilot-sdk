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
                available_tools: Some(vec![
                    "grep".into(),
                    "glob".into(),
                    "view".into(),
                ]),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("What tools do you have available? List each one by name."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    Ok(())
}
