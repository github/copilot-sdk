//! Skills — point the CLI at a directory of user-defined skills via
//! `SessionConfig::skill_directories`.

use std::path::PathBuf;
use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::ApproveAllHandler;
use copilot::hooks::{HookContext, PreToolUseInput, PreToolUseOutput, SessionHooks};
use copilot::types::SessionConfig;
use copilot::{Client, ClientOptions};

struct AllowAllHooks;

#[async_trait]
impl SessionHooks for AllowAllHooks {
    async fn on_pre_tool_use(
        &self,
        _input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        Some(PreToolUseOutput {
            permission_decision: Some("allow".to_string()),
            ..Default::default()
        })
    }
}

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    // CARGO_MANIFEST_DIR resolves to .../tools/skills/rust at compile time.
    let skills_dir: PathBuf = [env!("CARGO_MANIFEST_DIR"), "..", "sample-skills"]
        .iter()
        .collect();

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        skill_directories: Some(vec![skills_dir]),
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler))
    .with_hooks(Arc::new(AllowAllHooks));

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait("Use the greeting skill to greet someone named Alice.")
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    println!("\nSkill directories configured successfully");

    session.destroy().await?;
    Ok(())
}
