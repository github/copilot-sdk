//! Define custom tools and expose them to the Copilot agent.
//!
//! Registers two tools — `get_weather` (typed params via schemars) and
//! `roll_dice` (manual schema) — then asks the agent a question that
//! triggers tool use.
//!
//! Requires the `derive` feature for typed parameter schemas:
//!
//! ```sh
//! cargo run -p github-copilot-sdk --example tool_server --features derive
//! ```

// Gate the entire example behind the `derive` feature so it compiles
// (as a stub that prints the required feature flag) when clippy/check
// runs without the feature.
#[cfg(not(feature = "derive"))]
fn main() {
    eprintln!("This example requires the `derive` feature:");
    eprintln!("  cargo run -p github-copilot-sdk --example tool_server --features derive");
    std::process::exit(1);
}

#[cfg(feature = "derive")]
use std::sync::Arc;
#[cfg(feature = "derive")]
use std::time::Duration;

#[cfg(feature = "derive")]
use async_trait::async_trait;
#[cfg(feature = "derive")]
use github_copilot_sdk::handler::ApproveAllHandler;
#[cfg(feature = "derive")]
use github_copilot_sdk::tool::{
    JsonSchema, ToolHandler, ToolHandlerRouter, schema_for, tool_parameters,
};
#[cfg(feature = "derive")]
use github_copilot_sdk::types::{MessageOptions, SessionConfig, Tool, ToolInvocation, ToolResult};
#[cfg(feature = "derive")]
use github_copilot_sdk::{Client, ClientOptions, Error};
#[cfg(feature = "derive")]
use serde::Deserialize;

// ---------------------------------------------------------------------------
// Tool 1: get_weather — typed parameters derived from a Rust struct
// ---------------------------------------------------------------------------

#[cfg(feature = "derive")]
#[derive(Deserialize, JsonSchema)]
struct GetWeatherParams {
    /// City name (e.g. "Seattle").
    city: String,
    /// Temperature unit: "celsius" or "fahrenheit".
    unit: Option<String>,
}

#[cfg(feature = "derive")]
struct GetWeatherTool;

#[cfg(feature = "derive")]
#[async_trait]
impl ToolHandler for GetWeatherTool {
    fn tool(&self) -> Tool {
        let mut tool = Tool::default();
        tool.name = "get_weather".to_string();
        tool.description = "Get the current weather for a city.".to_string();
        tool.parameters = tool_parameters(schema_for::<GetWeatherParams>());
        tool
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let params: GetWeatherParams = serde_json::from_value(invocation.arguments)?;
        let unit = params.unit.as_deref().unwrap_or("celsius");
        // Stub response — a real implementation would call a weather API.
        let reply = format!(
            "Weather in {}: 18°{}, partly cloudy",
            params.city,
            if unit == "fahrenheit" { "F" } else { "C" },
        );
        Ok(ToolResult::Text(reply))
    }
}

// ---------------------------------------------------------------------------
// Tool 2: roll_dice — manual JSON Schema
// ---------------------------------------------------------------------------

#[cfg(feature = "derive")]
struct RollDiceTool;

#[cfg(feature = "derive")]
#[async_trait]
impl ToolHandler for RollDiceTool {
    fn tool(&self) -> Tool {
        let mut tool = Tool::default();
        tool.name = "roll_dice".to_string();
        tool.description = "Roll one or more dice and return the total.".to_string();
        tool.parameters = tool_parameters(serde_json::json!({
            "type": "object",
            "properties": {
                "sides": { "type": "integer", "description": "Number of sides per die (default 6, max 1000)." },
                "count": { "type": "integer", "description": "Number of dice to roll (default 1, max 100)." }
            }
        }));
        tool
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let sides = invocation
            .arguments
            .get("sides")
            .and_then(|v| v.as_u64())
            .unwrap_or(6)
            .clamp(1, 1000) as u32;
        let count = invocation
            .arguments
            .get("count")
            .and_then(|v| v.as_u64())
            .unwrap_or(1)
            .clamp(1, 100) as u32;

        let mut total = 0u32;
        let mut rolls = Vec::with_capacity(count as usize);
        for _ in 0..count {
            // Simple deterministic "random" for the example.
            let roll = (std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .subsec_nanos()
                % sides)
                + 1;
            rolls.push(roll);
            total += roll;
        }

        Ok(ToolResult::Text(format!(
            "Rolled {count}d{sides}: {rolls:?} = {total}"
        )))
    }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

#[cfg(feature = "derive")]
#[tokio::main]
async fn main() -> Result<(), github_copilot_sdk::Error> {
    let router = ToolHandlerRouter::new(
        vec![Box::new(GetWeatherTool), Box::new(RollDiceTool)],
        Arc::new(ApproveAllHandler),
    );
    let tools = router.tools();
    let handler = Arc::new(router);

    let client = Client::start(ClientOptions::default()).await?;

    let config = {
        let mut cfg = SessionConfig::default();
        cfg.tools = Some(tools);
        cfg.with_handler(handler)
    };
    let session = client.create_session(config).await?;

    println!(
        "Session {} — asking about weather + dice...\n",
        session.id()
    );

    let response = session
        .send_and_wait(
            MessageOptions::new("What's the weather in Seattle? Also roll 3d20 for me.")
                .with_wait_timeout(Duration::from_secs(60)),
        )
        .await?;

    if let Some(event) = response {
        let text = event
            .data
            .get("content")
            .and_then(|c| c.as_str())
            .unwrap_or("<no response>");
        println!("{text}");
    }

    session.destroy().await?;
    Ok(())
}
