use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::{ApproveAllHandler, HandlerEvent, HandlerResponse, SessionHandler};
use copilot::hooks::{HookEvent, HookOutput, PreToolUseOutput, SessionHooks};
use copilot::types::{SessionConfig, SessionEventData};
use copilot::{Client, ClientOptions, MessageOptions};

struct LoggingHooks;

#[async_trait]
impl SessionHooks for LoggingHooks {
    async fn on_hook(&self, event: HookEvent) -> HookOutput {
        match event {
            HookEvent::SessionStart { .. } => {
                println!("hook: sessionStart");
                HookOutput::None
            }
            HookEvent::PreToolUse { ref input, .. } => {
                println!("hook: preToolUse ({})", input.tool_name);
                HookOutput::PreToolUse(PreToolUseOutput {
                    permission_decision: Some("allow".into()),
                    ..Default::default()
                })
            }
            HookEvent::PostToolUse { ref input, .. } => {
                println!("hook: postToolUse ({})", input.tool_name);
                HookOutput::None
            }
            HookEvent::UserPromptSubmitted { .. } => {
                println!("hook: userPromptSubmitted");
                HookOutput::None
            }
            HookEvent::SessionEnd { .. } => {
                println!("hook: sessionEnd");
                HookOutput::None
            }
            _ => HookOutput::None,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".into()),
        hooks: Some(true),
        ..Default::default()
    };

    let session = client
        .create_session(config, Arc::new(ApproveAllHandler), Some(Arc::new(LoggingHooks)), None)
        .await?;

    let response = session
        .send_and_wait(MessageOptions::new("List the files in the current directory using the glob tool with pattern '*.md'."), None)
        .await?;

    if let Some(event) = response.event {
        println!("Response type: {}", event.event_type);
        if let SessionEventData::AssistantMessage(d) = &event.data {
            println!("Content: {}", d.content);
        }
    }

    session.disconnect().await?;
    println!("Hooks scenario complete");
    Ok(())
}
