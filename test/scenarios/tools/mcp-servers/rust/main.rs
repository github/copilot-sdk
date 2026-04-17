use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let mut config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        available_tools: Some(vec![]),
        ..Default::default()
    };

    if let Ok(cmd) = std::env::var("MCP_SERVER_CMD") {
        let args: Vec<String> = std::env::var("MCP_SERVER_ARGS")
            .unwrap_or_default()
            .split_whitespace()
            .map(String::from)
            .collect();

        config.mcp_servers = Some(serde_json::json!({
            "my-server": {
                "type": "stdio",
                "command": cmd,
                "args": args,
                "tools": { "include": ["*"] }
            }
        }));
    }

    let session = client
        .create_session(config, Arc::new(ApproveAllHandler), None, None)
        .await?;

    let response = session
        .send_and_wait(MessageOptions::new("What is the capital of France?"), None)
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    if std::env::var("MCP_SERVER_CMD").is_ok() {
        println!("\nMCP server configured successfully");
    } else {
        println!("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)");
    }

    Ok(())
}
