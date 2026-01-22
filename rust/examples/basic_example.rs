//! Basic example demonstrating the Copilot SDK.
//!
//! This example shows how to:
//! - Create a client and connect to the Copilot CLI
//! - Create a session with a specific model
//! - Send messages and receive responses
//! - Handle events using the broadcast channel
//!
//! # Running
//!
//! Make sure you have the Copilot CLI installed and authenticated:
//! ```bash
//! copilot auth login
//! cargo run --example basic_example
//! ```

use copilot_sdk::{
    Client, ClientOptions, MessageOptions, SessionConfig, SessionEventType,
};
use std::time::Duration;

#[tokio::main]
async fn main() -> copilot_sdk::Result<()> {
    println!("Starting Copilot SDK example...\n");

    // Create a client with error-level logging
    let mut client = Client::new(ClientOptions::new().log_level("error"));

    // Start the client (spawns the Copilot CLI server)
    println!("Starting Copilot CLI server...");
    client.start().await?;
    println!("Connected!\n");

    // Check authentication status
    let auth_status = client.get_auth_status().await?;
    if !auth_status.is_authenticated {
        eprintln!("Warning: Not authenticated. Run 'copilot auth login' first.");
        client.stop().await;
        return Ok(());
    }
    println!(
        "Authenticated as: {}\n",
        auth_status.login.unwrap_or_else(|| "unknown".to_string())
    );

    // Create a session
    println!("Creating session...");
    let session = client
        .create_session(SessionConfig::new().model("gpt-4o"))
        .await?;
    println!("Session created: {}\n", session.session_id());

    // Subscribe to events
    let mut rx = session.subscribe();

    // Spawn a task to handle events
    let event_handler = tokio::spawn(async move {
        while let Ok(event) = rx.recv().await {
            match event.r#type {
                SessionEventType::AssistantMessage => {
                    if let Some(content) = &event.data.content {
                        println!("\n📝 Assistant: {}\n", content);
                    }
                }
                SessionEventType::SessionError => {
                    if let Some(message) = &event.data.message {
                        eprintln!("❌ Error: {}", message);
                    }
                }
                SessionEventType::SessionIdle => {
                    println!("✅ Session idle");
                    break;
                }
                _ => {}
            }
        }
    });

    // Send a message
    println!("Sending message...");
    let _message_id = session
        .send(MessageOptions::new("What is 2+2? Please respond briefly."))
        .await?;

    // Wait for the event handler to finish
    let _ = tokio::time::timeout(Duration::from_secs(30), event_handler).await;

    // Clean up
    println!("\nCleaning up...");
    session.destroy().await?;
    client.stop().await;
    println!("Done!");

    Ok(())
}
