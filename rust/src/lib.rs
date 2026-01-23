//! # GitHub Copilot SDK for Rust
//!
//! Embed Copilot's agentic workflows in your Rust application. The GitHub Copilot SDK
//! exposes the same engine behind Copilot CLI: a production-tested agent runtime you
//! can invoke programmatically.
//!
//! ## Quick Start
//!
//! ```no_run
//! use github_copilot_sdk::{Client, ClientOptions, SessionConfig};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     // Create and start client
//!     let client = Client::new(ClientOptions::default()).await?;
//!     
//!     // Create session
//!     let mut session = client.create_session(SessionConfig {
//!         model: Some("gpt-4o".to_string()),
//!         ..Default::default()
//!     }).await?;
//!     
//!     // Send message and wait for response
//!     let response = session.send_and_wait("Hello, Copilot!").await?;
//!     println!("Response: {}", response);
//!     
//!     Ok(())
//! }
//! ```

pub mod client;
pub mod error;
pub mod generated;
pub mod jsonrpc;
pub mod sdk_protocol_version;
pub mod session;
pub mod tools;
pub mod types;

pub use client::Client;
pub use error::{Error, Result};
pub use generated::SessionEvent;
pub use session::{Session, SessionConfig};
pub use tools::{Tool, ToolHandler, ToolInvocation, ToolResult};
pub use types::*;
