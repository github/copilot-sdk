//! Infinite sessions — explicit `InfiniteSessionConfig` thresholds and a
//! sequence of three turns to exercise the persistent workspace.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{InfiniteSessionConfig, SessionConfig, SystemMessageConfig};
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut sysmsg = SystemMessageConfig::default();
    sysmsg.mode = Some("replace".to_string());
    sysmsg.content =
        Some("You are a helpful assistant. Answer concisely in one sentence.".to_string());

    let mut infinite = InfiniteSessionConfig::default();
    infinite.enabled = Some(true);
    infinite.background_compaction_threshold = Some(0.80);
    infinite.buffer_exhaustion_threshold = Some(0.95);

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.available_tools = Some(Vec::new());
    config.system_message = Some(sysmsg);
    config.infinite_sessions = Some(infinite);
    let config = config.with_handler(Arc::new(ApproveAllHandler));

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
