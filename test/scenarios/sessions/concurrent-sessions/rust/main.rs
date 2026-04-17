use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::{Client, ClientOptions, SessionConfig, SystemMessageConfig, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let handler = || Arc::new(ApproveAllHandler) as Arc<dyn copilot::handler::SessionHandler>;

    let session1 = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                system_message: Some(SystemMessageConfig {
                    mode: Some("replace".into()),
                    content: Some("You are a pirate. Always say Arrr!".into()),
                    ..Default::default()
                }),
                available_tools: Some(vec![]),
                ..Default::default()
            },
            handler(),
            None,
            None,
        )
        .await?;

    let session2 = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                system_message: Some(SystemMessageConfig {
                    mode: Some("replace".into()),
                    content: Some("You are a robot. Always say BEEP BOOP!".into()),
                    ..Default::default()
                }),
                available_tools: Some(vec![]),
                ..Default::default()
            },
            handler(),
            None,
            None,
        )
        .await?;

    let session3 = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                system_message: Some(SystemMessageConfig {
                    mode: Some("replace".into()),
                    content: Some("You are a wizard. Always say Abracadabra!".into()),
                    ..Default::default()
                }),
                available_tools: Some(vec![]),
                ..Default::default()
            },
            handler(),
            None,
            None,
        )
        .await?;

    let (r1, r2, r3) = tokio::join!(
        session1.send_and_wait(MessageOptions::new("What is the capital of France?"), None),
        session2.send_and_wait(MessageOptions::new("What is the capital of France?"), None),
        session3.send_and_wait(MessageOptions::new("What is the capital of France?"), None),
    );

    for (label, result) in [("Pirate", r1), ("Robot", r2), ("Wizard", r3)] {
        match result {
            Ok(r) => match r.event {
                Some(event) => println!("{label}: {}", event.data),
                None => println!("{label}: (no response)"),
            },
            Err(e) => eprintln!("{label} error: {e}"),
        }
    }

    Ok(())
}
