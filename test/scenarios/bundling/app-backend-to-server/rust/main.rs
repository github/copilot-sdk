use std::env;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, SessionConfig, Transport, MessageOptions};

fn parse_cli_url() -> (String, u16) {
    let url = env::var("COPILOT_CLI_URL")
        .or_else(|_| env::var("CLI_URL"))
        .unwrap_or_else(|_| "localhost:3000".into());
    let url = url
        .trim_start_matches("http://")
        .trim_start_matches("https://");
    match url.rsplit_once(':') {
        Some((host, port)) => (host.into(), port.parse().expect("invalid port")),
        None => (url.into(), 3000),
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let (host, port) = parse_cli_url();

    let client = Client::start(ClientOptions {
        transport: Transport::External { host, port },
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
