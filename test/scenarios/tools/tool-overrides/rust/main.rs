use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::ApproveAllHandler;
use copilot::tool::{ToolHandler, ToolHandlerRouter};
use copilot::{Client, ClientOptions, Error, SessionConfig, Tool, ToolInvocation, ToolResult, MessageOptions};

struct MyGrepTool;

#[async_trait]
impl ToolHandler for MyGrepTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "grep".into(),
            description: "A custom grep that overrides the built-in".into(),
            parameters: Some(serde_json::json!({
                "type": "object",
                "properties": {
                    "query": { "type": "string", "description": "Search query" }
                },
                "required": ["query"]
            })),
            overrides_built_in_tool: Some(true),
        }
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let query = invocation
            .arguments
            .get("query")
            .and_then(|v| v.as_str())
            .unwrap_or("");
        Ok(ToolResult::Text(format!(
            "CUSTOM_GREP_RESULT: {query}"
        )))
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let router = ToolHandlerRouter::new(
        vec![Box::new(MyGrepTool)],
        Arc::new(ApproveAllHandler),
    );
    let tools = router.tools();

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                tools: Some(tools),
                ..Default::default()
            },
            Arc::new(router),
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("Use grep to search for the word 'hello'"),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    Ok(())
}
