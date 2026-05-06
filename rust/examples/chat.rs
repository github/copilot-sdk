//! Interactive chat with GitHub Copilot.
//!
//! Starts a GitHub Copilot CLI server, creates a session, and enters a read-eval-print
//! loop where each line you type is sent to the agent. Streaming is enabled so
//! response tokens print to stdout incrementally as they arrive.
//!
//! ```sh
//! cargo run -p github-copilot-sdk --example chat
//! ```

use std::io::{self, BufRead, Write};
use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::handler::{
    HandlerEvent, HandlerResponse, PermissionResult, SessionHandler, UserInputResponse,
};
use github_copilot_sdk::types::{MessageOptions, SessionConfig, SessionEvent};
use github_copilot_sdk::{Client, ClientOptions};

/// Handler that prints assistant message deltas as they stream in
/// and auto-approves permissions.
struct ChatHandler;

#[async_trait]
impl SessionHandler for ChatHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::SessionEvent { event, .. } => {
                print_event(&event);
                HandlerResponse::Ok
            }
            HandlerEvent::PermissionRequest { .. } => {
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            HandlerEvent::UserInput { question, .. } => {
                // Prompt the user on behalf of the agent.
                print!("\n[agent asks] {question}\n> ");
                io::stdout().flush().ok();
                let answer = read_line().unwrap_or_default();
                HandlerResponse::UserInput(Some(UserInputResponse {
                    answer,
                    was_freeform: true,
                }))
            }
            _ => HandlerResponse::Ok,
        }
    }
}

fn print_event(event: &SessionEvent) {
    match event.event_type.as_str() {
        "assistant.message_delta" => {
            let text = event
                .data
                .get("deltaContent")
                .and_then(|c| c.as_str())
                .unwrap_or("");
            print!("{text}");
            io::stdout().flush().ok();
        }
        "assistant.message" => {
            // Final message — print a newline to terminate the streamed output.
            println!();
        }
        "session.error" => {
            let msg = event
                .data
                .get("message")
                .and_then(|m| m.as_str())
                .unwrap_or("unknown error");
            eprintln!("\n[error] {msg}");
        }
        _ => {}
    }
}

fn read_line() -> Option<String> {
    let stdin = io::stdin();
    let mut line = String::new();
    stdin.lock().read_line(&mut line).ok()?;
    if line.is_empty() {
        return None; // EOF
    }
    Some(line.trim_end_matches(&['\n', '\r'][..]).to_string())
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let client = Client::start(ClientOptions::default()).await?;

    let config = {
        let mut cfg = SessionConfig::default();
        cfg.streaming = Some(true);
        cfg.with_handler(Arc::new(ChatHandler))
    };
    let session = client.create_session(config).await?;

    println!(
        "Session {} started. Type a message (Ctrl-D to quit).\n",
        session.id()
    );

    loop {
        print!("> ");
        io::stdout().flush().ok();

        let Some(line) = read_line() else { break };
        if line.is_empty() {
            continue;
        }

        session
            .send_and_wait(MessageOptions::new(line).with_wait_timeout(Duration::from_secs(120)))
            .await?;
    }

    println!("\nGoodbye.");
    session.destroy().await?;
    Ok(())
}
