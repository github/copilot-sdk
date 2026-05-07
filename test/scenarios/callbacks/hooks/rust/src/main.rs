//! Session hooks — intercept lifecycle events (session start/end, pre/post
//! tool use, user prompt, errors) and log every firing.

use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::hooks::{
    ErrorOccurredInput, ErrorOccurredOutput, HookContext, PostToolUseInput, PostToolUseOutput,
    PreToolUseInput, PreToolUseOutput, SessionEndInput, SessionEndOutput, SessionHooks,
    SessionStartInput, SessionStartOutput, UserPromptSubmittedInput, UserPromptSubmittedOutput,
};
use github_copilot_sdk::types::SessionConfig;
use github_copilot_sdk::{Client, ClientOptions};
use tokio::sync::Mutex;

struct HookLogger {
    log: Arc<Mutex<Vec<String>>>,
}

impl HookLogger {
    async fn append(&self, entry: String) {
        self.log.lock().await.push(entry);
    }
}

#[async_trait]
impl SessionHooks for HookLogger {
    async fn on_session_start(
        &self,
        _input: SessionStartInput,
        _ctx: HookContext,
    ) -> Option<SessionStartOutput> {
        self.append("onSessionStart".to_string()).await;
        None
    }

    async fn on_session_end(
        &self,
        _input: SessionEndInput,
        _ctx: HookContext,
    ) -> Option<SessionEndOutput> {
        self.append("onSessionEnd".to_string()).await;
        None
    }

    async fn on_pre_tool_use(
        &self,
        input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        self.append(format!("onPreToolUse:{}", input.tool_name))
            .await;
        let mut out = PreToolUseOutput::default();
        out.permission_decision = Some("allow".to_string());
        Some(out)
    }

    async fn on_post_tool_use(
        &self,
        input: PostToolUseInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseOutput> {
        self.append(format!("onPostToolUse:{}", input.tool_name))
            .await;
        None
    }

    async fn on_user_prompt_submitted(
        &self,
        input: UserPromptSubmittedInput,
        _ctx: HookContext,
    ) -> Option<UserPromptSubmittedOutput> {
        self.append("onUserPromptSubmitted".to_string()).await;
        let mut out = UserPromptSubmittedOutput::default();
        out.modified_prompt = Some(input.prompt);
        Some(out)
    }

    async fn on_error_occurred(
        &self,
        input: ErrorOccurredInput,
        _ctx: HookContext,
    ) -> Option<ErrorOccurredOutput> {
        self.append(format!("onErrorOccurred:{}", input.error))
            .await;
        None
    }
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let hook_log = Arc::new(Mutex::new(Vec::<String>::new()));
    let hooks = Arc::new(HookLogger {
        log: hook_log.clone(),
    });

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    let config = config
        .with_handler(Arc::new(ApproveAllHandler))
        .with_hooks(hooks);

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait(
            "List the files in the current directory using the glob tool with pattern '*.md'.",
        )
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    println!("\n--- Hook execution log ---");
    let log = hook_log.lock().await;
    for entry in log.iter() {
        println!("  {entry}");
    }
    println!("\nTotal hooks fired: {}", log.len());

    session.destroy().await?;
    Ok(())
}
