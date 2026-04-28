//! Custom agents — define a sub-agent ("researcher") with its own prompt
//! and tool allowlist, alongside a client-defined `analyze-codebase` tool.

use std::sync::Arc;

use copilot::handler::ApproveAllHandler;
use copilot::tool::{ToolHandlerRouter, define_tool};
use copilot::types::{CustomAgentConfig, DefaultAgentConfig, SessionConfig, ToolResult};
use copilot::{Client, ClientOptions};
use schemars::JsonSchema;
use serde::Deserialize;

#[derive(Deserialize, JsonSchema)]
#[schemars(description = "Parameters for analyze-codebase")]
struct AnalyzeParams {
    /// the analysis query
    query: String,
}

#[tokio::main]
async fn main() -> Result<(), copilot::Error> {
    let client = Client::start(ClientOptions {
        github_token: std::env::var("GITHUB_TOKEN").ok(),
        ..Default::default()
    })
    .await?;

    let analyze_codebase = define_tool(
        "analyze-codebase",
        "Performs deep analysis of the codebase",
        |params: AnalyzeParams| async move {
            Ok(ToolResult::Text(format!(
                "Analysis result for: {}",
                params.query
            )))
        },
    );

    let router = ToolHandlerRouter::new(vec![analyze_codebase], Arc::new(ApproveAllHandler));
    let tools = router.tools();

    let config = SessionConfig {
        model: Some("claude-haiku-4.5".to_string()),
        tools: Some(tools),
        default_agent: Some(DefaultAgentConfig {
            excluded_tools: Some(vec!["analyze-codebase".to_string()]),
        }),
        custom_agents: Some(vec![CustomAgentConfig {
            name: "researcher".to_string(),
            display_name: Some("Research Agent".to_string()),
            description: Some(
                "A research agent that can only read and search files, not modify them"
                    .to_string(),
            ),
            tools: Some(vec![
                "grep".to_string(),
                "glob".to_string(),
                "view".to_string(),
                "analyze-codebase".to_string(),
            ]),
            prompt: "You are a research assistant. You can search and read files but cannot modify \
                     anything. When asked about your capabilities, list the tools you have access to."
                .to_string(),
            ..Default::default()
        }]),
        ..Default::default()
    }
    .with_handler(Arc::new(router));

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
