use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::tool::{ToolHandler, ToolHandlerRouter};
use github_copilot_sdk::{
    Client, Error, OtelExporterType, SessionConfig, TelemetryConfig, Tool, ToolInvocation,
    ToolResult,
};
use serde_json::json;

use super::support::{assistant_message_content, wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_export_file_telemetry_for_sdk_interactions() {
    with_e2e_context(
        "telemetry",
        "should_export_file_telemetry_for_sdk_interactions",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let telemetry_path = ctx.work_dir().join("rust-telemetry-e2e.jsonl");
                let source_name = "rust-sdk-telemetry-e2e";
                let tool_name = "echo_telemetry_marker";
                let marker = "copilot-sdk-telemetry-e2e";
                let prompt = format!(
                    "Use the {tool_name} tool with value '{marker}', then respond with TELEMETRY_E2E_DONE."
                );

                let client = Client::start(ctx.client_options().with_telemetry(
                    TelemetryConfig::new()
                        .with_file_path(&telemetry_path)
                        .with_exporter_type(OtelExporterType::File)
                        .with_source_name(source_name)
                        .with_capture_content(true),
                ))
                .await
                .expect("start client");
                let router = ToolHandlerRouter::new(
                    vec![Box::new(EchoTelemetryTool {
                        name: tool_name.to_string(),
                    })],
                    Arc::new(ApproveAllHandler),
                );
                let tools = router.tools();
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(router))
                            .with_tools(tools),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(prompt.as_str())
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("TELEMETRY_E2E_DONE"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");

                let entries = read_telemetry_entries(&telemetry_path).await;
                let spans: Vec<_> = entries
                    .iter()
                    .filter(|entry| string_property(entry, "type") == Some("span"))
                    .collect();
                assert!(!spans.is_empty(), "expected telemetry spans in {entries:?}");
                assert!(spans.iter().all(|span| {
                    span.get("instrumentationScope")
                        .and_then(|scope| string_property(scope, "name"))
                        == Some(source_name)
                }));

                let trace_ids: std::collections::HashSet<_> = spans
                    .iter()
                    .filter_map(|span| string_property(span, "traceId"))
                    .collect();
                assert_eq!(trace_ids.len(), 1);
                assert!(spans.iter().all(|span| status_code(span) != Some(2)));

                let invoke_agent = find_span(&spans, "invoke_agent");
                assert_eq!(
                    string_attribute(invoke_agent, "gen_ai.conversation.id").as_deref(),
                    Some(session.id().as_str())
                );
                let invoke_agent_span_id =
                    string_property(invoke_agent, "spanId").expect("invoke_agent span id");
                assert!(is_root_span(invoke_agent));

                let chat_spans: Vec<_> = spans
                    .iter()
                    .copied()
                    .filter(|span| {
                        string_attribute(span, "gen_ai.operation.name").as_deref() == Some("chat")
                    })
                    .collect();
                assert!(!chat_spans.is_empty());
                assert!(chat_spans.iter().all(|span| {
                    string_property(span, "parentSpanId") == Some(invoke_agent_span_id)
                }));
                assert!(chat_spans.iter().any(|span| string_attribute(
                    span,
                    "gen_ai.input.messages"
                )
                .is_some_and(|messages| messages.contains(&prompt))));
                assert!(chat_spans.iter().any(|span| string_attribute(
                    span,
                    "gen_ai.output.messages"
                )
                .is_some_and(|messages| messages.contains("TELEMETRY_E2E_DONE"))));

                let tool_span = find_span(&spans, "execute_tool");
                assert_eq!(
                    string_property(tool_span, "parentSpanId"),
                    Some(invoke_agent_span_id)
                );
                assert_eq!(
                    string_attribute(tool_span, "gen_ai.tool.name").as_deref(),
                    Some(tool_name)
                );
                assert_eq!(
                    string_attribute(tool_span, "gen_ai.tool.call.arguments").as_deref(),
                    Some(format!("{{\"value\":\"{marker}\"}}").as_str())
                );
                assert_eq!(
                    string_attribute(tool_span, "gen_ai.tool.call.result").as_deref(),
                    Some(marker)
                );
            })
        },
    )
    .await;
}

struct EchoTelemetryTool {
    name: String,
}

#[async_trait]
impl ToolHandler for EchoTelemetryTool {
    fn tool(&self) -> Tool {
        Tool::new(&self.name)
            .with_description("Echoes a marker string for telemetry validation.")
            .with_parameters(json!({
                "type": "object",
                "properties": {
                    "value": { "type": "string" }
                },
                "required": ["value"]
            }))
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        Ok(ToolResult::Text(
            invocation
                .arguments
                .get("value")
                .and_then(serde_json::Value::as_str)
                .unwrap_or_default()
                .to_string(),
        ))
    }
}

async fn read_telemetry_entries(path: &std::path::Path) -> Vec<serde_json::Value> {
    wait_for_condition("telemetry file to contain spans", || {
        let path = path.to_path_buf();
        async move {
            read_telemetry_entries_once(&path).is_ok_and(|entries| {
                entries.iter().any(|entry| {
                    string_property(entry, "type") == Some("span")
                        && string_attribute(entry, "gen_ai.operation.name").as_deref()
                            == Some("invoke_agent")
                })
            })
        }
    })
    .await;
    read_telemetry_entries_once(path).expect("read telemetry entries")
}

fn read_telemetry_entries_once(path: &std::path::Path) -> std::io::Result<Vec<serde_json::Value>> {
    if !path.exists() || path.metadata()?.len() == 0 {
        return Ok(Vec::new());
    }
    std::fs::read_to_string(path).map(|content| {
        content
            .lines()
            .filter(|line| !line.trim().is_empty())
            .map(|line| serde_json::from_str(line).expect("telemetry JSON line"))
            .collect()
    })
}

fn find_span<'a>(spans: &'a [&'a serde_json::Value], operation: &str) -> &'a serde_json::Value {
    spans
        .iter()
        .copied()
        .find(|span| string_attribute(span, "gen_ai.operation.name").as_deref() == Some(operation))
        .unwrap_or_else(|| panic!("span {operation} not found in {spans:?}"))
}

fn string_property<'a>(value: &'a serde_json::Value, name: &str) -> Option<&'a str> {
    value.get(name).and_then(serde_json::Value::as_str)
}

fn string_attribute(value: &serde_json::Value, name: &str) -> Option<String> {
    value
        .get("attributes")
        .and_then(|attributes| attributes.get(name))
        .map(|value| match value {
            serde_json::Value::String(value) => value.clone(),
            serde_json::Value::Number(_) | serde_json::Value::Bool(_) => value.to_string(),
            serde_json::Value::Array(_) | serde_json::Value::Object(_) => value.to_string(),
            serde_json::Value::Null => String::new(),
        })
}

fn status_code(value: &serde_json::Value) -> Option<i64> {
    value
        .get("status")
        .and_then(|status| status.get("code"))
        .and_then(serde_json::Value::as_i64)
}

fn is_root_span(value: &serde_json::Value) -> bool {
    string_property(value, "parentSpanId")
        .is_none_or(|parent| parent.is_empty() || parent == "0000000000000000")
}
