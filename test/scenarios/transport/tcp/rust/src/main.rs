//! TCP transport — connect to an externally-running CLI server. Reads
//! `COPILOT_CLI_URL` (default `localhost:3000`) for `host:port`.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::SessionConfig;
use copilot::{Client, ClientOptions, Transport};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let cli_url =
        std::env::var("COPILOT_CLI_URL").unwrap_or_else(|_| "localhost:3000".to_string());
    let (host, port_str) = cli_url
        .split_once(':')
        .expect("COPILOT_CLI_URL must be 'host:port'");
    let port: u16 = port_str.parse().expect("COPILOT_CLI_URL port must be u16");

    let client = Client::start(ClientOptions {
        transport: Transport::External {
            host: host.to_string(),
            port,
        },
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));

    let session = client.create_session(config).await?;

    let response = session.send_and_wait("What is the capital of France?").await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
