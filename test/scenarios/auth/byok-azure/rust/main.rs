use std::env;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{
    AzureProviderOptions, Client, ClientOptions, MessageOptions, ProviderConfig, SessionConfig,
    SystemMessageConfig,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let endpoint = env::var("AZURE_OPENAI_ENDPOINT").expect("Missing AZURE_OPENAI_ENDPOINT");
    let api_key = env::var("AZURE_OPENAI_API_KEY").expect("Missing AZURE_OPENAI_API_KEY");

    let model = env::var("AZURE_OPENAI_MODEL")
        .unwrap_or_else(|_| "claude-haiku-4.5".into());

    let api_version = env::var("AZURE_API_VERSION")
        .unwrap_or_else(|_| "2024-10-21".into());

    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some(model),
                provider: Some(ProviderConfig {
                    provider_type: Some("azure".into()),
                    base_url: Some(endpoint),
                    api_key: Some(api_key),
                    bearer_token: None,
                    wire_api: None,
                    azure: Some(AzureProviderOptions {
                        api_version: Some(api_version),
                    }),
                    headers: None,
                }),
                available_tools: Some(vec![]),
                system_message: Some(SystemMessageConfig {
                    mode: Some("replace".into()),
                    content: Some("You are a helpful assistant. Answer concisely.".into()),
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
