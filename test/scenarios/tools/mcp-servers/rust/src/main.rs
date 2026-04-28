//! MCP servers — configure an MCP server from env and pass it through to
//! the CLI via `SessionConfig::mcp_servers`. Build-only when
//! `MCP_SERVER_CMD` is unset.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let mcp_cmd = std::env::var("MCP_SERVER_CMD").ok();
    let mcp_args_env = std::env::var("MCP_SERVER_ARGS").ok();
    let mcp_servers = mcp_cmd.as_ref().map(|cmd| {
        let args: Vec<&str> = mcp_args_env
            .as_deref()
            .map(|s| s.split(' ').collect())
            .unwrap_or_default();
        serde_json::json!({
            "example": {
                "type": "stdio",
                "command": cmd,
                "args": args,
                "tools": ["*"],
            }
        })
    });

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".to_string()),
            content: Some("You are a helpful assistant. Answer questions concisely.".to_string()),
            ..Default::default()
        }),
        available_tools: Some(Vec::new()),
        mcp_servers,
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

    if mcp_cmd.is_some() {
        println!("\nMCP servers configured: example");
    } else {
        println!("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)");
    }

    session.destroy().await?;
    Ok(())
}
