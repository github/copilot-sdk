//! Custom agents — define a sub-agent ("researcher") with its own prompt
//! and tool allowlist, alongside a client-defined `analyze-codebase` tool.

use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandlerRouter, define_tool};
use github_copilot_sdk::types::{CustomAgentConfig, DefaultAgentConfig, SessionConfig, ToolResult};
use github_copilot_sdk::{Client, ClientOptions};
use schemars::JsonSchema;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
#[schemars(description = "Parameters for analyze-codebase")]
struct AnalyzeParams {
    /// the analysis query
    query: String,
}

#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let mut opts = ClientOptions::default();
    opts.github_token = std::env::var("GITHUB_TOKEN").ok();
    let client = Client::start(opts).await?;

    let analyze_codebase = define_tool(
        "analyze-codebase",
        "Performs deep analysis of the codebase",
        |_inv, params: AnalyzeParams| async move {
            Ok(ToolResult::Text(format!(
                "Analysis result for: {}",
                params.query
            )))
        },
    );

    let router = ToolHandlerRouter::new(vec![analyze_codebase], Arc::new(ApproveAllHandler));
    let tools = router.tools();

    let mut researcher = CustomAgentConfig::default();
    researcher.name = "researcher".to_string();
    researcher.display_name = Some("Research Agent".to_string());
    researcher.description = Some(
        "A research agent that can only read and search files, not modify them".to_string(),
    );
    researcher.tools = Some(vec![
        "grep".to_string(),
        "glob".to_string(),
        "view".to_string(),
        "analyze-codebase".to_string(),
    ]);
    researcher.prompt =
        "You are a research assistant. You can search and read files but cannot modify \
                  anything. When asked about your capabilities, list the tools you have access to."
            .to_string();

    let mut config = SessionConfig::default();
    config.model = Some("claude-haiku-4.5".to_string());
    config.tools = Some(tools);
    config.default_agent = Some(DefaultAgentConfig {
        excluded_tools: Some(vec!["analyze-codebase".to_string()]),
    });
    config.custom_agents = Some(vec![researcher]);
    let config = config.with_handler(Arc::new(router));

    let session = client.create_session(config).await?;

    let response = session
        .send_and_wait(
            "What custom agents are available? Describe the researcher agent and its capabilities.",
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
