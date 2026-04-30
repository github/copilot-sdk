//! TCP transport — connect to an externally-running CLI server. Reads
//! `COPILOT_CLI_URL` (default `localhost:3000`) for `host:port`.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::SessionConfig;
use github_copilot_sdk::{Client, ClientOptions, Transport};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let cli_url =
        std::env::var("COPILOT_CLI_URL").unwrap_or_else(|_| "localhost:3000".to_string());
    let (host, port_str) = cli_url
        .split_once(':')
        .expect("COPILOT_CLI_URL must be 'host:port'");
    let port: u16 = port_str.parse().expect("COPILOT_CLI_URL port must be u16");

    let mut opts = ClientOptions::default();
    opts.transport = Transport::External {
        host: host.to_string(),
        port,
    };
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    let config = config.with_handler(Arc::new(ApproveAllHandler));

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
