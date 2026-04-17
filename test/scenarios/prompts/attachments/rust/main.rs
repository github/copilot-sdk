use std::path::PathBuf;
use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::types::{Attachment, SessionConfig, SessionEventData, SystemMessageConfig};
use copilot::{Client, ClientOptions, MessageOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        available_tools: Some(vec![]),
        system_message: Some(SystemMessageConfig {
            mode: Some("replace".into()),
            content: Some(
                "You are a helpful assistant. Answer questions about attached files concisely."
                    .into(),
            ),
            ..Default::default()
        }),
        ..Default::default()
    };

    let session = client
        .create_session(config, Arc::new(ApproveAllHandler), None, None)
        .await?;

    let sample_file = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .join("sample-data.txt")
        .canonicalize()?;

    let response = session
        .send_and_wait(
            MessageOptions::new("What languages are listed in the attached file?").with_attachments(vec![Attachment::File {
                path: sample_file,
                display_name: Some("sample-data.txt".into()),
                line_range: None,
            }]),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        if let SessionEventData::AssistantMessage(d) = &event.data {
            println!("{}", d.content);
        }
    }

    session.disconnect().await?;
    Ok(())
}
