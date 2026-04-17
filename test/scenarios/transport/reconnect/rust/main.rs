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

    let handler = || Arc::new(ApproveAllHandler) as Arc<dyn copilot::handler::SessionHandler>;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        available_tools: Some(vec![]),
        ..Default::default()
    };

    // Session 1
    println!("--- Session 1 ---");
    let session1 = client
        .create_session(config.clone(), handler(), None, None)
        .await?;

    let r1 = session1
        .send_and_wait(MessageOptions::new("What is the capital of France?"), None)
        .await?;

    if let Some(event) = r1.event {
        println!("{}", event.data);
    }

    session1.stop_event_loop().await;
    println!("Session 1 disconnected\n");

    // Session 2 — tests that the server accepts new sessions
    println!("--- Session 2 ---");
    let session2 = client
        .create_session(config, handler(), None, None)
        .await?;

    let r2 = session2
        .send_and_wait(MessageOptions::new("What is the capital of Germany?"), None)
        .await?;

    if let Some(event) = r2.event {
        println!("{}", event.data);
    }

    session2.stop_event_loop().await;
    println!("Session 2 disconnected");

    println!("\nReconnect test passed — both sessions completed successfully");

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
