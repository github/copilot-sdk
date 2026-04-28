//! Infinite sessions — explicit `InfiniteSessionConfig` thresholds and a
//! sequence of three turns to exercise the persistent workspace.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{InfiniteSessionConfig, SessionConfig, SystemMessageConfig};
use copilot::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        available_tools: Some(Vec::new()),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".to_string()),
            content: Some(
                "You are a helpful assistant. Answer concisely in one sentence.".to_string(),
            ),
            ..Default::default()
        }),
        infinite_sessions: Some(InfiniteSessionConfig {
            enabled: Some(true),
            background_compaction_threshold: Some(0.80),
            buffer_exhaustion_threshold: Some(0.95),
        }),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler));

    let session = client.create_session(config).await?;

    let prompts = [
        "What is the capital of France?",
        "What is the capital of Japan?",
        "What is the capital of Brazil?",
    ];

    for prompt in prompts {
        let response = session.send_and_wait(prompt).await?;
        if let Some(event) = response {
            if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
                println!("Q: {prompt}");
                println!("A: {content}\n");
            }
        }
    }

    println!("Infinite sessions test complete — all messages processed successfully");

    session.destroy().await?;
    Ok(())
}
