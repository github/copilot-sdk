//! Basic hello world example

use github_copilot_sdk::{Client, ClientOptions, SessionConfig, SessionEvent};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Starting Copilot SDK example...");

    // Create client with default options
    let client = Client::new(ClientOptions::default()).await?;
    println!("Client started successfully");

    // Create a session
    let session = client
        .create_session(SessionConfig {
            model: Some("gpt-4o".to_string()),
            ..Default::default()
        })
        .await?;

    println!("Session created: {}", session.id());

    // Register event handler
    session
        .on_event(std::sync::Arc::new(|event| match event {
            SessionEvent::AssistantMessage { content, .. } => {
                println!("Assistant: {}", content);
            }
            SessionEvent::MessagePending { .. } => {
                print!(".");
                use std::io::Write;
                std::io::stdout().flush().unwrap();
            }
            _ => {}
        }))
        .await;

    // Send a message and wait for response
    println!("\nSending message...");
    let response = session
        .send_and_wait("Hello, Copilot! What can you do?")
        .await?;
    println!("\nFinal response: {}", response);

    // Clean shutdown
    client.stop().await?;
    println!("\nClient stopped");

    Ok(())
}
