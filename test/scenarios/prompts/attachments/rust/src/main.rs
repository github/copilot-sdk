//! File attachments — send a prompt alongside a file attachment so the
//! model can reference the file's content in its response.

use std::path::PathBuf;
use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::types::{Attachment, MessageOptions, SessionConfig, SystemMessageConfig};
use github_copilot_sdk::{Client, ClientOptions};

const SYSTEM_PROMPT: &str =
    "You are a helpful assistant. Answer questions about attached files concisely.";

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let mut sysmsg = SystemMessageConfig::default();
    sysmsg.mode = Some("replace".to_string());
    sysmsg.content = Some(SYSTEM_PROMPT.to_string());

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.system_message = Some(sysmsg);
    config.available_tools = Some(Vec::new());
    let config = config.with_handler(Arc::new(ApproveAllHandler));

    let session = client.create_session(config).await?;

    // CARGO_MANIFEST_DIR resolves to .../prompts/attachments/rust at compile time.
    let sample_file: PathBuf = [env!("CARGO_MANIFEST_DIR"), "..", "sample-data.txt"]
        .iter()
        .collect();
    let sample_file = sample_file.canonicalize().unwrap_or(sample_file);

    let response = session
        .send_and_wait(
            MessageOptions::new("What languages are listed in the attached file?").with_attachments(
                vec![Attachment::File {
                    path: sample_file,
                    display_name: None,
                    line_range: None,
                }],
            ),
        )
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.destroy().await?;
    Ok(())
}
