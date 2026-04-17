use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, SessionConfig, Transport, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli_url = std::env::var("COPILOT_CLI_URL").unwrap_or_else(|_| "localhost:3000".into());
    let (host, port) = parse_host_port(&cli_url);

    let client = Client::start(ClientOptions {
        transport: Transport::External {
            host: host.into(),
            port,
        },
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

fn parse_host_port(url: &str) -> (&str, u16) {
    let url = url
        .strip_prefix("http://")
        .or_else(|| url.strip_prefix("https://"))
        .unwrap_or(url);
    match url.rsplit_once(':') {
        Some((host, port)) => (host, port.parse().unwrap_or(3000)),
        None => (url, 3000),
    }
}
