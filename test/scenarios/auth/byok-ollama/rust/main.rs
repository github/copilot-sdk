use std::env;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, ProviderConfig, SessionConfig, SystemMessageConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let base_url = env::var("OLLAMA_BASE_URL")
        .unwrap_or_else(|_| "http://localhost:11434/v1".into());

    let model = env::var("OLLAMA_MODEL")
        .unwrap_or_else(|_| "llama3.2:3b".into());

    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some(model),
                provider: Some(ProviderConfig {
                    provider_type: Some("openai".into()),
                    base_url: Some(base_url),
                    api_key: None,
                    bearer_token: None,
                    wire_api: None,
                    azure: None,
                    headers: None,
                }),
                available_tools: Some(vec![]),
                system_message: Some(SystemMessageConfig {
                    mode: Some("replace".into()),
                    content: Some(
                        "You are a compact local assistant. Keep answers short, concrete, and under 80 words.".into(),
                    ),
                    sections: None,
                }),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let response = session.send_and_wait(MessageOptions::new("What is the capital of France?"), None).await?;
    println!("{:?}", response);

    session.disconnect().await?;
    Ok(())
}
