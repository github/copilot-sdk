use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, InfiniteSessionConfig, SessionConfig, SystemMessageConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                system_message: Some(SystemMessageConfig {
                    mode: Some("replace".into()),
                    content: Some(
                        "You are a helpful assistant. Answer concisely in one sentence.".into(),
                    ),
                    ..Default::default()
                }),
                available_tools: Some(vec![]),
                infinite_sessions: Some(InfiniteSessionConfig {
                    enabled: Some(true),
                    background_compaction_threshold: Some(0.80),
                    buffer_exhaustion_threshold: Some(0.95),
                }),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let prompts = [
        "What is the capital of France?",
        "What is the capital of Japan?",
        "What is the capital of Brazil?",
    ];

    for prompt in prompts {
        let response = session.send_and_wait(MessageOptions::new(prompt), None).await?;
        match response.event {
            Some(event) => {
                println!("Q: {prompt}");
                println!("A: {}\n", event.data);
            }
            None => println!("Q: {prompt}\nA: (no response)\n"),
        }
    }

    println!("Infinite sessions test complete — all messages processed successfully");
    Ok(())
}
