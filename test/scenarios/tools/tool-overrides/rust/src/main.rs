//! Tool overrides — replace the built-in `grep` tool with a custom
//! implementation that returns a distinct marker.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::define_tool;
use github_copilot_sdk::types::{SessionConfig, ToolResult};
use github_copilot_sdk::{Client, ClientOptions};
use schemars::JsonSchema;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
#[schemars(description = "Parameters for custom grep")]
struct GrepParams {
    /// Search query
    query: String,
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let client = Client::start(ClientOptions::default()).await?;

    let mut grep_tool = define_tool(
        "grep",
        "A custom grep implementation that overrides the built-in",
        |_inv, params: GrepParams| async move {
            Ok(ToolResult::Text(format!("CUSTOM_GREP_RESULT: {}", params.query)))
        },
    );
    grep_tool.overrides_built_in_tool = true;

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    let config = config
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_tools(vec![grep_tool]);

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait("Use grep to search for the word 'hello'")
        .await?;

    if let Some(event) = response {
        if let Some(content) = event.data.get("content").and_then(|c| c.as_str()) {
            println!("{content}");
        }
    }

    session.disconnect().await?;
    Ok(())
}
