//! Session hooks for logging and auditing.
//!
//! Demonstrates `SessionHooks` to intercept lifecycle events — logging every
//! tool invocation, summarizing prompts, and recording session start/end
//! for audit purposes.
//!
//! ```sh
//! cargo run -p github-copilot-sdk --example hooks
//! ```

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::hooks::{
    HookEvent, HookOutput, PostToolUseOutput, PreToolUseOutput, SessionEndOutput, SessionHooks,
    SessionStartOutput,
};
use github_copilot_sdk::types::{MessageOptions, SessionConfig};
use github_copilot_sdk::{Client, ClientOptions};

/// Hooks implementation that logs lifecycle events to stdout.
struct AuditHooks;

#[async_trait]
impl SessionHooks for AuditHooks {
    async fn on_hook(&self, event: HookEvent) -> HookOutput {
        match event {
            HookEvent::SessionStart { input, ctx } => {
                println!(
                    "[audit] session {} started (source={}, cwd={})",
                    ctx.session_id,
                    input.source,
                    input.cwd.display(),
                );
                HookOutput::SessionStart(SessionStartOutput {
                    additional_context: Some("You are being audited. Be concise.".to_string()),
                    ..Default::default()
                })
            }

            HookEvent::PreToolUse { input, ctx } => {
                println!(
                    "[audit] session {} — pre tool use: {} (args: {})",
                    ctx.session_id, input.tool_name, input.tool_args,
                );
                // Example: deny a specific tool by name.
                if input.tool_name == "dangerous_tool" {
                    return HookOutput::PreToolUse(PreToolUseOutput {
                        permission_decision: Some("deny".to_string()),
                        permission_decision_reason: Some("blocked by audit policy".to_string()),
                        ..Default::default()
                    });
                }
                HookOutput::None
            }

            HookEvent::PostToolUse { input, ctx } => {
                println!(
                    "[audit] session {} — post tool use: {} (result: {})",
                    ctx.session_id, input.tool_name, input.tool_result,
                );
                HookOutput::PostToolUse(PostToolUseOutput::default())
            }

            HookEvent::UserPromptSubmitted { input, ctx } => {
                println!(
                    "[audit] session {} — user prompt ({} chars)",
                    ctx.session_id,
                    input.prompt.len(),
                );
                HookOutput::None
            }

            HookEvent::SessionEnd { input, ctx } => {
                println!(
                    "[audit] session {} ended (reason={})",
                    ctx.session_id, input.reason,
                );
                HookOutput::SessionEnd(SessionEndOutput {
                    session_summary: Some("Audited session complete.".to_string()),
                    ..Default::default()
                })
            }

            HookEvent::ErrorOccurred { input, ctx } => {
                eprintln!(
                    "[audit] session {} — error in {}: {} (recoverable={})",
                    ctx.session_id, input.error_context, input.error, input.recoverable,
                );
                HookOutput::None
            }

            _ => HookOutput::None,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let client = Client::start(ClientOptions::default()).await?;

    let config = SessionConfig {
        // hooks: true is set automatically when a hooks handler is provided.
        ..Default::default()
    }
    .with_handler(Arc::new(ApproveAllHandler))
    .with_hooks(Arc::new(AuditHooks));
    let session = client.create_session(config).await?;

    println!(
        "Session {} with audit hooks. Sending a message...\n",
        session.id()
    );

    let response = session
        .send_and_wait(
            MessageOptions::new("Say hello in three languages.")
                .with_wait_timeout(Duration::from_secs(60)),
        )
        .await?;

    if let Some(event) = response {
        let text = event
            .data
            .get("content")
            .and_then(|c| c.as_str())
            .unwrap_or("<no response>");
        println!("\n{text}");
    }

    session.destroy().await?;
    Ok(())
}
