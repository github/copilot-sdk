use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::{
    HandlerEvent, HandlerResponse, PermissionResult, SessionHandler, UserInputResponse,
};
use copilot::types::{SessionConfig, SessionEventData};
use copilot::{Client, ClientOptions, MessageOptions};

struct UserInputHandler {
    input_count: AtomicUsize,
}

#[async_trait]
impl SessionHandler for UserInputHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::UserInput { ref question, .. } => {
                let n = self.input_count.fetch_add(1, Ordering::SeqCst) + 1;
                println!("User input #{n}: {question}");
                HandlerResponse::UserInput(Some(UserInputResponse::new("Paris", true)))
            }
            HandlerEvent::PermissionRequest { .. } => {
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            _ => HandlerResponse::Ok,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let handler = Arc::new(UserInputHandler {
        input_count: AtomicUsize::new(0),
    });

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        request_user_input: Some(true),
        request_permission: Some(true),
        ..Default::default()
    };

    let session = client
        .create_session(config, handler.clone(), None, None)
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("I want to learn about a city. Use the ask_user tool to ask me which city I'm interested in. Then tell me about that city."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        if let SessionEventData::AssistantMessage(d) = &event.data {
            println!("Response: {}", d.content);
        }
    }

    println!(
        "\nTotal user input requests: {}",
        handler.input_count.load(Ordering::SeqCst)
    );
    session.disconnect().await?;
    Ok(())
}
