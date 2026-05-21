//! Tool overrides — replace the built-in `grep` tool with a custom
//! implementation that returns a distinct marker.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandler, define_tool};
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
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let grep_tool: Arc<dyn ToolHandler> = Arc::from(define_tool(
        "grep",
        "A custom grep implementation that overrides the built-in",
        |_inv, params: GrepParams| async move {
            Ok(ToolResult::Text(format!("CUSTOM_GREP_RESULT: {}", params.query)))
        },
    ));

    let mut tools = vec![grep_tool.tool()];
    for t in tools.iter_mut() {
        if t.name == "grep" {
            t.overrides_built_in_tool = true;
        }
    }

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.tools = Some(tools);
    let config = config
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .with_tool_handlers(vec![grep_tool]);

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
