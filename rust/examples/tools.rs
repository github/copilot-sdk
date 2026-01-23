//! Example showing custom tool registration

use async_trait::async_trait;
use github_copilot_sdk::{
    Client, ClientOptions, SessionConfig, SessionEvent, Tool, ToolHandler, ToolInvocation,
    ToolResult,
};
use std::collections::HashMap;
use std::sync::Arc;

/// A custom tool that gets the current time
struct GetTimeHandler;

#[async_trait]
impl ToolHandler for GetTimeHandler {
    async fn handle(
        &self,
        _arguments: HashMap<String, serde_json::Value>,
        _invocation: ToolInvocation,
    ) -> github_copilot_sdk::Result<ToolResult> {
        let now = chrono::Local::now();
        Ok(ToolResult::text(format!(
            "Current time is: {}",
            now.format("%Y-%m-%d %H:%M:%S")
        )))
    }
}

/// A custom tool that calculates something
struct CalculatorHandler;

#[async_trait]
impl ToolHandler for CalculatorHandler {
    async fn handle(
        &self,
        arguments: HashMap<String, serde_json::Value>,
        _invocation: ToolInvocation,
    ) -> github_copilot_sdk::Result<ToolResult> {
        let a = arguments.get("a").and_then(|v| v.as_f64()).unwrap_or(0.0);

        let b = arguments.get("b").and_then(|v| v.as_f64()).unwrap_or(0.0);

        let operation = arguments
            .get("operation")
            .and_then(|v| v.as_str())
            .unwrap_or("add");

        let result = match operation {
            "add" => a + b,
            "subtract" => a - b,
            "multiply" => a * b,
            "divide" => {
                if b != 0.0 {
                    a / b
                } else {
                    return Ok(ToolResult::error("Division by zero"));
                }
            }
            _ => return Ok(ToolResult::error("Unknown operation")),
        };

        Ok(ToolResult::text(format!(
            "{} {} {} = {}",
            a, operation, b, result
        )))
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Custom Tools Example");

    // Create client
    let client = Client::new(ClientOptions::default()).await?;

    // Create session
    let session = client
        .create_session(SessionConfig {
            model: Some("gpt-4o".to_string()),
            ..Default::default()
        })
        .await?;

    // Register get_time tool
    let get_time_tool = Tool::new(
        "get_current_time",
        "Get the current date and time",
        serde_json::json!({
            "type": "object",
            "properties": {},
            "required": []
        }),
    );
    session
        .register_tool(get_time_tool, Arc::new(GetTimeHandler))
        .await?;

    // Register calculator tool
    let calculator_tool = Tool::new(
        "calculator",
        "Perform basic arithmetic operations",
        serde_json::json!({
            "type": "object",
            "properties": {
                "a": {
                    "type": "number",
                    "description": "First number"
                },
                "b": {
                    "type": "number",
                    "description": "Second number"
                },
                "operation": {
                    "type": "string",
                    "enum": ["add", "subtract", "multiply", "divide"],
                    "description": "Operation to perform"
                }
            },
            "required": ["a", "b", "operation"]
        }),
    );
    session
        .register_tool(calculator_tool, Arc::new(CalculatorHandler))
        .await?;

    println!("Tools registered!");

    // Set up event handler
    session
        .on_event(Arc::new(|event| {
            if let SessionEvent::AssistantMessage { content, .. } = event {
                println!("Assistant: {}", content);
            }
        }))
        .await;

    // Test the tools
    println!("\n--- Test 1: Get current time ---");
    session.send_and_wait("What time is it?").await?;

    println!("\n--- Test 2: Calculator ---");
    session.send_and_wait("Calculate 42 * 17 for me").await?;

    // Clean up
    client.stop().await?;

    Ok(())
}
