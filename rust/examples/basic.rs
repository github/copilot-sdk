//! Basic example demonstrating the Copilot SDK.
//!
//! This example shows how to:
//! - Create a client and connect to the CLI server
//! - Create a session
//! - Subscribe to events
//! - Send a message and wait for a response
//!
//! # Running
//!
//! ```bash
//! cargo run --example basic
//! ```

use copilot_sdk::{
    CopilotClient, MessageOptions, SessionConfig, SessionEvent, SessionEventType,
};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Starting Copilot SDK basic example...\n");

    // Create a client with default options
    // This will spawn the CLI server automatically when needed
    let client = CopilotClient::new(None)?;

    // Start the client (connects to CLI server)
    println!("Connecting to Copilot CLI server...");
    client.start().await?;
    println!("Connected!\n");

    // Ping the server to verify connectivity
    let pong = client.ping(Some("hello")).await?;
    println!("Ping response: {}", pong.message);
    println!("Protocol version: {:?}\n", pong.protocol_version);

    // Create a session
    println!("Creating session...");
    let session = client
        .create_session(Some(SessionConfig {
            model: Some("gpt-4".to_string()),
            ..Default::default()
        }))
        .await?;
    println!("Session created: {}\n", session.session_id());

    // Subscribe to events (handler receives Arc<SessionEvent>)
    let _unsubscribe = session.on(Arc::new(|event: Arc<SessionEvent>| {
        match event.event_type {
            SessionEventType::AssistantMessage => {
                if let Some(content) = &event.data.content {
                    println!("Assistant: {}", content);
                }
            }
            SessionEventType::SessionError => {
                if let Some(message) = &event.data.message {
                    eprintln!("Error: {}", message);
                }
            }
            SessionEventType::SessionIdle => {
                println!("\n[Session idle]");
            }
            _ => {
                // Log other event types
                println!("[Event: {}]", event.event_type);
            }
        }
    }));

    // Send a message and wait for response
    println!("Sending message...\n");
    let response = session
        .send_and_wait(
            MessageOptions {
                prompt: "What is 2 + 2? Answer briefly.".to_string(),
                ..Default::default()
            },
            None, // Use default timeout
        )
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.content {
            println!("\nFinal response: {}", content);
        }
    }

    // Clean up
    println!("\nStopping client...");
    let errors = client.stop().await;
    if !errors.is_empty() {
        for err in errors {
            eprintln!("Cleanup error: {}", err);
        }
    }

    println!("Done!");
    Ok(())
}
