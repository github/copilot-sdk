use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, CustomAgentConfig, SessionConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                custom_agents: Some(vec![CustomAgentConfig {
                    name: "researcher".into(),
                    display_name: Some("Research Agent".into()),
                    description: Some(
                        "A research agent that can only read and search files".into(),
                    ),
                    tools: Some(vec!["grep".into(), "glob".into(), "view".into()]),
                    prompt: Some("You are a research assistant.".into()),
                    mcp_servers: None,
                    infer: None,
                    skills: None,
                }]),
                ..Default::default()
            },
            Arc::new(ApproveAllHandler),
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("What custom agents are available? Describe the researcher agent."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    Ok(())
}
