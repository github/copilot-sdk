use std::env;
use std::ffi::OsString;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let token = env::var("GITHUB_TOKEN").expect("Missing GITHUB_TOKEN");

    let client = Client::start(ClientOptions {
        env: vec![(
            OsString::from("GITHUB_TOKEN"),
            OsString::from(&token),
        )],
        ..Default::default()
    })
    .await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let response = session.send_and_wait(MessageOptions::new("What is the capital of France?"), None).await?;
    println!("{:?}", response);

    session.disconnect().await?;
    Ok(())
}
