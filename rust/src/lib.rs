//! # Copilot CLI SDK for Rust
//!
//! A Rust SDK for programmatic access to the GitHub Copilot CLI.
//!
//! > **Note:** This SDK is in technical preview and may change in breaking ways.
//!
//! ## Quick Start
//!
//! ```no_run
//! use copilot_sdk::{Client, ClientOptions, SessionConfig, MessageOptions, SessionEventType};
//!
//! #[tokio::main]
//! async fn main() -> copilot_sdk::Result<()> {
//!     // Create client
//!     let mut client = Client::new(ClientOptions::new().log_level("error"));
//!
//!     // Start the client
//!     client.start().await?;
//!
//!     // Create a session
//!     let session = client.create_session(SessionConfig::new().model("gpt-5")).await?;
//!
//!     // Set up event handler
//!     let mut rx = session.subscribe();
//!     tokio::spawn(async move {
//!         while let Ok(event) = rx.recv().await {
//!             if event.r#type == SessionEventType::AssistantMessage {
//!                 if let Some(content) = &event.data.content {
//!                     println!("{}", content);
//!                 }
//!             }
//!         }
//!     });
//!
//!     // Send a message and wait for completion
//!     let response = session.send_and_wait(MessageOptions::new("What is 2+2?"), None).await?;
//!
//!     // Clean up
//!     session.destroy().await?;
//!     client.stop().await;
//!
//!     Ok(())
//! }
//! ```
//!
//! ## Tools
//!
//! Expose your own functionality to Copilot by attaching tools to a session:
//!
//! ```no_run
//! use copilot_sdk::{define_tool, Client, ClientOptions, SessionConfig, ToolInvocation};
//! use serde::Deserialize;
//! use schemars::JsonSchema;
//!
//! #[derive(Debug, Deserialize, JsonSchema)]
//! struct LookupIssueParams {
//!     /// Issue identifier
//!     id: String,
//! }
//!
//! #[tokio::main]
//! async fn main() -> copilot_sdk::Result<()> {
//!     let lookup_issue = define_tool(
//!         "lookup_issue",
//!         "Fetch issue details from our tracker",
//!         |params: LookupIssueParams, _inv: ToolInvocation| async move {
//!             Ok(format!("Issue {}: Example issue", params.id))
//!         },
//!     );
//!
//!     let mut client = Client::new(ClientOptions::new());
//!     client.start().await?;
//!
//!     let session = client.create_session(
//!         SessionConfig::new()
//!             .model("gpt-5")
//!             .tool(lookup_issue)
//!     ).await?;
//!
//!     Ok(())
//! }
//! ```
//!
//! ## Streaming
//!
//! Enable streaming to receive assistant response chunks as they're generated:
//!
//! ```no_run
//! use copilot_sdk::{Client, ClientOptions, SessionConfig, MessageOptions, SessionEventType};
//!
//! #[tokio::main]
//! async fn main() -> copilot_sdk::Result<()> {
//!     let mut client = Client::new(ClientOptions::new());
//!     client.start().await?;
//!
//!     let session = client.create_session(
//!         SessionConfig::new()
//!             .model("gpt-5")
//!             .streaming(true)
//!     ).await?;
//!
//!     let mut rx = session.subscribe();
//!     tokio::spawn(async move {
//!         while let Ok(event) = rx.recv().await {
//!             match event.r#type {
//!                 SessionEventType::AssistantMessageDelta => {
//!                     if let Some(delta) = &event.data.delta_content {
//!                         print!("{}", delta);
//!                     }
//!                 }
//!                 SessionEventType::AssistantMessage => {
//!                     println!("\n--- Final message ---");
//!                     if let Some(content) = &event.data.content {
//!                         println!("{}", content);
//!                     }
//!                 }
//!                 _ => {}
//!             }
//!         }
//!     });
//!
//!     session.send(MessageOptions::new("Tell me a short story")).await?;
//!
//!     Ok(())
//! }
//! ```

pub mod client;
pub mod error;
pub mod generated;
pub mod jsonrpc;
pub mod session;
pub mod tools;
pub mod types;

// Re-export main types at the crate root
pub use client::{Client, SDK_PROTOCOL_VERSION, get_sdk_protocol_version};
pub use error::{CopilotError, JsonRpcError, Result};
pub use generated::{SessionEvent, SessionEventType, EventData};
pub use session::Session;
pub use tools::{define_tool, IntoToolResult, JsonResult};
pub use types::*;
