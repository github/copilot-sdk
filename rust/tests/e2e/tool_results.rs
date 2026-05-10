use std::collections::HashMap;
use std::sync::Arc;

use github_copilot_sdk::generated::session_events::{SessionEventType, ToolExecutionCompleteData};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::{
    Error, SessionConfig, Tool, ToolInvocation, ToolResult, ToolResultExpanded,
};
use serde_json::json;
use tokio::sync::mpsc;

use super::support::{assistant_message_content, collect_until_idle, with_e2e_context};

#[tokio::test]
async fn should_handle_structured_toolresultobject_from_custom_tool() {
    with_e2e_context(
        "tool_results",
        "should_handle_structured_toolresultobject_from_custom_tool",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = create_tool_session(ctx, &client, WeatherTool).await;

                let answer = session
                    .send_and_wait("What's the weather in Paris?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer).to_lowercase();
                assert!(content.contains("sunny") || content.contains("72"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_tool_result_with_failure_resulttype() {
    with_e2e_context(
        "tool_results",
        "should_handle_tool_result_with_failure_resulttype",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = create_tool_session(ctx, &client, CheckStatusTool).await;

                let answer = session
                    .send_and_wait("Check the status of the service using check_status. If it fails, say 'service is down'.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer)
                    .to_lowercase()
                    .contains("service is down"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_preserve_tooltelemetry_and_not_stringify_structured_results_for_llm() {
    with_e2e_context(
        "tool_results",
        "should_preserve_tooltelemetry_and_not_stringify_structured_results_for_llm",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = create_tool_session(ctx, &client, AnalyzeCodeTool).await;

                let answer = session
                    .send_and_wait("Analyze the file main.ts for issues.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(
                    assistant_message_content(&answer)
                        .to_lowercase()
                        .contains("no issues")
                );

                let exchanges = ctx.exchanges();
                let tool_results: Vec<_> = exchanges
                    .last()
                    .and_then(|exchange| exchange.get("request"))
                    .and_then(|request| request.get("messages"))
                    .and_then(serde_json::Value::as_array)
                    .expect("messages")
                    .iter()
                    .filter(|message| {
                        message.get("role").and_then(serde_json::Value::as_str) == Some("tool")
                    })
                    .collect();
                assert_eq!(tool_results.len(), 1);
                let content = tool_results[0].to_string();
                assert!(!content.contains("toolTelemetry"));
                assert!(!content.contains("resultType"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_tool_result_with_rejected_resulttype() {
    with_e2e_context(
        "tool_results",
        "should_handle_tool_result_with_rejected_resulttype",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (call_tx, mut call_rx) = mpsc::unbounded_channel();
                let session = create_tool_session(ctx, &client, DeployTool { call_tx }).await;
                let events = session.subscribe();

                session
                    .send("Deploy the service using deploy_service. If it's rejected, tell me it was 'rejected by policy'.")
                    .await
                    .expect("send");
                recv_called(&mut call_rx, "deploy tool").await;
                let observed = collect_until_idle(events).await;
                let complete = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::ToolExecutionComplete)
                    .and_then(|event| event.typed_data::<ToolExecutionCompleteData>())
                    .expect("tool.execution_complete");
                assert!(!complete.success);
                let error = complete.error.expect("tool error");
                assert_eq!(error.code.as_deref(), Some("rejected"));
                assert!(error.message.contains("Deployment rejected"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_tool_result_with_denied_resulttype() {
    with_e2e_context(
        "tool_results",
        "should_handle_tool_result_with_denied_resulttype",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let (call_tx, mut call_rx) = mpsc::unbounded_channel();
                let session = create_tool_session(ctx, &client, AccessSecretTool { call_tx }).await;
                let events = session.subscribe();

                session
                    .send("Use access_secret to get the API key. If access is denied, tell me it was 'access denied'.")
                    .await
                    .expect("send");
                recv_called(&mut call_rx, "access secret tool").await;
                let observed = collect_until_idle(events).await;
                let complete = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::ToolExecutionComplete)
                    .and_then(|event| event.typed_data::<ToolExecutionCompleteData>())
                    .expect("tool.execution_complete");
                assert!(!complete.success);
                let error = complete.error.expect("tool error");
                assert_eq!(error.code.as_deref(), Some("denied"));
                assert!(error.message.contains("Access denied"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

async fn create_tool_session<T>(
    _ctx: &super::support::E2eContext,
    client: &github_copilot_sdk::Client,
    tool: T,
) -> github_copilot_sdk::session::Session
where
    T: ToolHandler + 'static,
{
    let router = ToolHandlerRouter::new(vec![Box::new(tool)], Arc::new(ApproveAllHandler));
    let tools = router.tools();
    client
        .create_session(
            SessionConfig::default()
                .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                .with_handler(Arc::new(router))
                .with_tools(tools),
        )
        .await
        .expect("create session")
}

async fn recv_called(receiver: &mut mpsc::UnboundedReceiver<()>, description: &'static str) {
    tokio::time::timeout(std::time::Duration::from_secs(10), receiver.recv())
        .await
        .unwrap_or_else(|_| panic!("timed out waiting for {description}"))
        .unwrap_or_else(|| panic!("{description} channel closed"));
}

fn expanded(text: impl Into<String>, result_type: impl Into<String>) -> ToolResult {
    ToolResult::Expanded(ToolResultExpanded {
        text_result_for_llm: text.into(),
        result_type: result_type.into(),
        binary_results_for_llm: None,
        session_log: None,
        error: None,
        tool_telemetry: None,
    })
}

struct WeatherTool;

#[async_trait::async_trait]
impl ToolHandler for WeatherTool {
    fn tool(&self) -> Tool {
        string_tool(
            "get_weather",
            "Gets weather for a city",
            "city",
            "City name",
        )
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let city = invocation
            .arguments
            .get("city")
            .and_then(serde_json::Value::as_str)
            .unwrap_or("Paris");
        Ok(expanded(
            format!("The weather in {city} is sunny and 72\u{b0}F"),
            "success",
        ))
    }
}

struct CheckStatusTool;

#[async_trait::async_trait]
impl ToolHandler for CheckStatusTool {
    fn tool(&self) -> Tool {
        Tool::new("check_status").with_description("Checks the status of a service")
    }

    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let mut result = match expanded("Service unavailable", "failure") {
            ToolResult::Expanded(result) => result,
            _ => unreachable!(),
        };
        result.error = Some("API timeout".to_string());
        Ok(ToolResult::Expanded(result))
    }
}

struct AnalyzeCodeTool;

#[async_trait::async_trait]
impl ToolHandler for AnalyzeCodeTool {
    fn tool(&self) -> Tool {
        string_tool(
            "analyze_code",
            "Analyzes code for issues",
            "file",
            "File to analyze",
        )
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let file = invocation
            .arguments
            .get("file")
            .and_then(serde_json::Value::as_str)
            .unwrap_or("main.ts");
        let mut result = match expanded(format!("Analysis of {file}: no issues found"), "success") {
            ToolResult::Expanded(result) => result,
            _ => unreachable!(),
        };
        result.tool_telemetry = Some(HashMap::from([(
            "metrics".to_string(),
            json!({ "analysisTimeMs": 150 }),
        )]));
        Ok(ToolResult::Expanded(result))
    }
}

struct DeployTool {
    call_tx: mpsc::UnboundedSender<()>,
}

#[async_trait::async_trait]
impl ToolHandler for DeployTool {
    fn tool(&self) -> Tool {
        Tool::new("deploy_service").with_description("Deploys a service")
    }

    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let _ = self.call_tx.send(());
        Ok(expanded(
            "Deployment rejected: policy violation - production deployments require approval",
            "rejected",
        ))
    }
}

struct AccessSecretTool {
    call_tx: mpsc::UnboundedSender<()>,
}

#[async_trait::async_trait]
impl ToolHandler for AccessSecretTool {
    fn tool(&self) -> Tool {
        Tool::new("access_secret").with_description("Accesses a secret")
    }

    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let _ = self.call_tx.send(());
        Ok(expanded(
            "Access denied: insufficient permissions to read secrets",
            "denied",
        ))
    }
}

fn string_tool(
    name: &str,
    description: &str,
    parameter: &str,
    parameter_description: &str,
) -> Tool {
    Tool::new(name)
        .with_description(description)
        .with_parameters(json!({
            "type": "object",
            "properties": {
                parameter: {
                    "type": "string",
                    "description": parameter_description,
                }
            },
            "required": [parameter],
        }))
}
