//! User-input callback — answer the agent's `ask_user` prompts and log
//! every question.

use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::{PermissionResult, SessionHandler, UserInputResponse};
use github_copilot_sdk::hooks::{HookContext, PreToolUseInput, PreToolUseOutput, SessionHooks};
use github_copilot_sdk::types::{PermissionRequestData, RequestId, SessionConfig, SessionId};
use github_copilot_sdk::{Client, ClientOptions};
use tokio::sync::Mutex;

struct InputResponder {
    log: Arc<Mutex<Vec<String>>>,
}

#[async_trait]
impl SessionHandler for InputResponder {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::Approved
    }

    async fn on_user_input(
        &self,
        _session_id: SessionId,
        question: String,
        _choices: Option<Vec<String>>,
        _allow_freeform: Option<bool>,
    ) -> Option<UserInputResponse> {
        self.log
            .lock()
            .await
            .push(format!("question: {question}"));
        Some(UserInputResponse {
            answer: "Paris".to_string(),
            was_freeform: true,
        })
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
        let mut out = PreToolUseOutput::default();
        out.permission_decision = Some("allow".to_string());
        Some(out)
    }
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let input_log = Arc::new(Mutex::new(Vec::<String>::new()));
    let handler = Arc::new(InputResponder {
        log: input_log.clone(),
    });

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.request_user_input = Some(true);
    let config = config
        .with_handler(handler)
        .with_hooks(Arc::new(AllowAllHooks));

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait(
            "I want to learn about a city. Use the ask_user tool to ask me \
             which city I'm interested in. Then tell me about that city.",
        )
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    println!("\n--- User input log ---");
    let log = input_log.lock().await;
    for entry in log.iter() {
        println!("  {entry}");
    }
    println!("\nTotal user input requests: {}", log.len());

    session.destroy().await?;
    Ok(())
}
