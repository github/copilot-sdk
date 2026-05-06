//! MCP servers — configure an MCP server from env and pass it through to
//! the CLI via `SessionConfig::mcp_servers`. Build-only when
//! `MCP_SERVER_CMD` is unset.

use std::collections::HashMap;
use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{
    McpServerConfig, McpStdioServerConfig, SessionConfig, SystemMessageConfig,
};
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mcp_cmd = std::env::var("MCP_SERVER_CMD").ok();
    let mcp_args_env = std::env::var("MCP_SERVER_ARGS").ok();
    let mcp_servers = mcp_cmd.as_ref().map(|cmd| {
        let args: Vec<String> = mcp_args_env
            .as_deref()
            .map(|s| s.split(' ').map(str::to_string).collect())
            .unwrap_or_default();
        let stdio = McpStdioServerConfig {
            tools: vec!["*".to_string()],
            command: cmd.clone(),
            args,
            ..Default::default()
        };
        let mut map = HashMap::new();
        map.insert("example".to_string(), McpServerConfig::Stdio(stdio));
        map
    });

    let mut sysmsg = SystemMessageConfig::default();
    sysmsg.mode = Some("replace".to_string());
    sysmsg.content =
        Some("You are a helpful assistant. Answer questions concisely.".to_string());

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.system_message = Some(sysmsg);
    config.available_tools = Some(Vec::new());
    config.mcp_servers = mcp_servers;
    let config = config.with_handler(Arc::new(ApproveAllHandler));

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
