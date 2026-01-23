//! Example showing MCP server integration

use github_copilot_sdk::{
    Client, ClientOptions, MCPLocalServerConfig, MCPServerConfig, SessionConfig, SystemMessage,
};
use std::collections::HashMap;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("MCP Server Example");

    // Create client
    let client = Client::new(ClientOptions::default()).await?;

    // Configure MCP server
    let mut mcp_servers = HashMap::new();
    mcp_servers.insert(
        "my-mcp-server".to_string(),
        MCPServerConfig::Local(MCPLocalServerConfig {
            tools: vec!["search".to_string(), "fetch".to_string()],
            server_type: "stdio".to_string(),
            timeout: Some(30),
            command: "node".to_string(),
            args: vec!["mcp-server.js".to_string()],
            env: None,
        }),
    );

    // Create session with MCP server
    let session = client
        .create_session(SessionConfig {
            model: Some("gpt-4o".to_string()),
            mcp_servers: Some(mcp_servers),
            system_message: Some(SystemMessage::Append {
                content: Some(
                    "You have access to an MCP server with search and fetch tools.".to_string(),
                ),
            }),
            ..Default::default()
        })
        .await?;

    println!("Session created with MCP server!");

    // Send a message
    let response = session
        .send_and_wait("Search for information about Rust async programming")
        .await?;

    println!("Response: {}", response);

    // Clean up
    client.stop().await?;

    Ok(())
}
