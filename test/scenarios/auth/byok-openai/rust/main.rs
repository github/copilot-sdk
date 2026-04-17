use std::env;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, ProviderConfig, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let api_key = env::var("OPENAI_API_KEY").expect("Missing OPENAI_API_KEY");

    let base_url = env::var("OPENAI_BASE_URL")
        .unwrap_or_else(|_| "https://api.openai.com/v1".into());

    let model = env::var("OPENAI_MODEL")
        .unwrap_or_else(|_| "claude-haiku-4.5".into());

    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some(model),
                provider: Some(ProviderConfig {
                    provider_type: Some("openai".into()),
                    base_url: Some(base_url),
                    api_key: Some(api_key),
                    bearer_token: None,
                    wire_api: None,
                    azure: None,
                    headers: None,
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
