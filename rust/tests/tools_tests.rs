//! Tests for tool system

use github_copilot_sdk::{Tool, ToolResult};

#[test]
fn test_tool_creation() {
    let tool = Tool::new(
        "test_tool",
        "A test tool",
        serde_json::json!({
            "type": "object",
            "properties": {
                "param1": {"type": "string"}
            }
        }),
    );

    assert_eq!(tool.name, "test_tool");
    assert_eq!(tool.description, "A test tool");
}

#[test]
fn test_simple_tool() {
    let tool = Tool::simple("simple", "Simple tool");

    assert_eq!(tool.name, "simple");
    let schema = tool.parameters;
    assert_eq!(schema["type"], "object");
}

#[test]
fn test_tool_result_text() {
    let result = ToolResult::text("Hello");

    assert_eq!(result.content, Some("Hello".to_string()));
    assert_eq!(result.success, Some(true));
    assert!(result.error.is_none());
}

#[test]
fn test_tool_result_error() {
    let result = ToolResult::error("Something went wrong");

    assert_eq!(result.error, Some("Something went wrong".to_string()));
    assert_eq!(result.success, Some(false));
    assert!(result.content.is_none());
}

#[test]
fn test_tool_result_binary() {
    let data = vec![1, 2, 3, 4];
    let result = ToolResult::binary(data, "application/octet-stream");

    assert!(result.data.is_some());
    assert_eq!(
        result.mime_type,
        Some("application/octet-stream".to_string())
    );
    assert_eq!(result.success, Some(true));
}

#[test]
fn test_tool_result_with_telemetry() {
    let mut telemetry = std::collections::HashMap::new();
    telemetry.insert("duration_ms".to_string(), serde_json::json!(42));

    let result = ToolResult::text("Done").with_telemetry(telemetry.clone());

    assert_eq!(result.content, Some("Done".to_string()));
    assert!(result.telemetry.is_some());
    assert_eq!(
        result.telemetry.unwrap().get("duration_ms"),
        Some(&serde_json::json!(42))
    );
}
