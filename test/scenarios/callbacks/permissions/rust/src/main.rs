//! Permission callback — log every `permission.request` from the CLI and
//! approve all of them.

use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::{PermissionResult, SessionHandler};
use copilot::hooks::{HookContext, PreToolUseInput, PreToolUseOutput, SessionHooks};
use copilot::types::{PermissionRequestData, RequestId, SessionConfig, SessionId};
use copilot::{Client, ClientOptions};
use tokio::sync::Mutex;

struct PermissionLogger {
    log: Arc<Mutex<Vec<String>>>,
}

#[async_trait]
impl SessionHandler for PermissionLogger {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        let tool_name = data
            .extra
            .get("tool")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string();
        self.log.lock().await.push(format!("approved:{tool_name}"));
        PermissionResult::Approved
    }
}

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

    let permission_log = Arc::new(Mutex::new(Vec::<String>::new()));
    let handler = Arc::new(PermissionLogger {
        log: permission_log.clone(),
    });

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        ..Default::default()
    }
    .with_handler(handler)
    .with_hooks(Arc::new(AllowAllHooks));

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait(
            "List the files in the current directory using glob with pattern '*.md'.",
        )
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    println!("\n--- Permission request log ---");
    let log = permission_log.lock().await;
    for entry in log.iter() {
        println!("  {entry}");
    }
    println!("\nTotal permission requests: {}", log.len());

    session.destroy().await?;
    Ok(())
}
