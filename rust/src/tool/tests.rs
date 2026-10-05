/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;
use crate::types::SessionId;

struct EchoTool;

fn echo_tool() -> Tool {
    Tool {
        name: "echo".to_string(),
        description: "Echo the input".to_string(),
        parameters: tool_parameters(serde_json::json!({"type": "object"})),
        ..Default::default()
    }
    .with_handler(std::sync::Arc::new(EchoTool))
}

#[async_trait]
impl ToolHandler for EchoTool {
    async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
        Ok(ToolResult::Text(inv.arguments.to_string()))
    }
}

#[test]
fn tool_handler_returns_tool_definition() {
    let def = echo_tool();
    assert_eq!(def.name, "echo");
    assert_eq!(def.description, "Echo the input");
    assert!(def.parameters.contains_key("type"));
    assert!(def.handler.is_some());
}

#[test]
fn try_tool_parameters_rejects_non_object_schema() {
    let err = try_tool_parameters(serde_json::json!(["not", "an", "object"]))
        .expect_err("non-object schemas should be rejected");

    assert!(err.is_data());
}

#[test]
fn tool_parameters_serialize_in_deterministic_order() {
    // Regression: `Tool.parameters` was a `HashMap`, whose per-instance
    // random iteration order made the serialized top-level schema keys
    // differ between constructions (and between sessions), busting the
    // model provider's prompt cache. `IndexMap` keeps the order stable.
    let schema = serde_json::json!({
        "type": "object",
        "properties": {
            "url": { "type": "string" },
            "count": { "type": "integer" }
        },
        "required": ["url"],
        "additionalProperties": false
    });

    let build = || Tool {
        name: "fetch".to_string(),
        parameters: tool_parameters(schema.clone()),
        ..Default::default()
    };

    let expected = serde_json::to_string(&build()).expect("serialize tool");
    for _ in 0..64 {
        let actual = serde_json::to_string(&build()).expect("serialize tool");
        assert_eq!(actual, expected);
    }

    // Pin the exact top-level key order so a regression to any
    // order-randomizing container is caught, not just internal drift.
    let tool = build();
    let keys: Vec<&str> = tool.parameters.keys().map(String::as_str).collect();
    assert_eq!(
        keys,
        ["additionalProperties", "properties", "required", "type"]
    );
}

#[test]
fn convert_mcp_call_tool_result_collects_text_and_binary_content() {
    let result = convert_mcp_call_tool_result(&serde_json::json!({
        "isError": true,
        "content": [
            { "type": "text", "text": "hello" },
            { "type": "image", "data": "aW1n", "mimeType": "image/png" },
            {
                "type": "resource",
                "resource": {
                    "uri": "file:///tmp/data.bin",
                    "blob": "Ymlu",
                    "mimeType": "application/octet-stream",
                    "text": "resource text"
                }
            }
        ]
    }))
    .expect("valid CallToolResult should convert");

    let ToolResult::Expanded(expanded) = result else {
        panic!("expected expanded tool result");
    };

    assert_eq!(expanded.text_result_for_llm, "hello\nresource text");
    assert_eq!(expanded.result_type, "failure");
    let binary_results = expanded
        .binary_results_for_llm
        .expect("binary results should be captured");
    assert_eq!(binary_results.len(), 2);
    assert_eq!(binary_results[0].r#type, "image");
    assert_eq!(binary_results[0].data, "aW1n");
    assert_eq!(binary_results[0].mime_type, "image/png");
    assert_eq!(
        binary_results[1].description.as_deref(),
        Some("file:///tmp/data.bin")
    );
}

#[test]
fn convert_mcp_call_tool_result_converts_image_content() {
    let result = convert_mcp_call_tool_result(&serde_json::json!({
        "content": [
            { "type": "image", "data": "aW1hZ2U=", "mimeType": "image/jpeg" }
        ]
    }))
    .expect("valid CallToolResult should convert");

    let ToolResult::Expanded(expanded) = result else {
        panic!("expected expanded tool result");
    };

    assert_eq!(expanded.text_result_for_llm, "");
    assert_eq!(expanded.result_type, "success");
    let binary_results = expanded
        .binary_results_for_llm
        .expect("image result should be captured");
    assert_eq!(binary_results.len(), 1);
    assert_eq!(binary_results[0].data, "aW1hZ2U=");
    assert_eq!(binary_results[0].mime_type, "image/jpeg");
    assert_eq!(binary_results[0].r#type, "image");
    assert!(binary_results[0].description.is_none());
}

#[test]
fn convert_mcp_call_tool_result_converts_resource_blob_content() {
    let result = convert_mcp_call_tool_result(&serde_json::json!({
        "content": [
            {
                "type": "resource",
                "resource": {
                    "uri": "file:///tmp/report.pdf",
                    "blob": "cGRm",
                    "mimeType": "application/pdf"
                }
            }
        ]
    }))
    .expect("valid CallToolResult should convert");

    let ToolResult::Expanded(expanded) = result else {
        panic!("expected expanded tool result");
    };

    let binary_results = expanded
        .binary_results_for_llm
        .expect("resource result should be captured");
    assert_eq!(binary_results.len(), 1);
    assert_eq!(binary_results[0].data, "cGRm");
    assert_eq!(binary_results[0].mime_type, "application/pdf");
    assert_eq!(binary_results[0].r#type, "resource");
    assert_eq!(
        binary_results[0].description.as_deref(),
        Some("file:///tmp/report.pdf")
    );
}

#[test]
fn convert_mcp_call_tool_result_defaults_resource_blob_mime_type() {
    let result = convert_mcp_call_tool_result(&serde_json::json!({
        "content": [
            {
                "type": "resource",
                "resource": {
                    "uri": "file:///tmp/data.bin",
                    "blob": "Ymlu"
                }
            },
            {
                "type": "resource",
                "resource": {
                    "blob": "YmluMg==",
                    "mimeType": ""
                }
            }
        ]
    }))
    .expect("valid CallToolResult should convert");

    let ToolResult::Expanded(expanded) = result else {
        panic!("expected expanded tool result");
    };

    let binary_results = expanded
        .binary_results_for_llm
        .expect("resource blobs should be captured");
    assert_eq!(binary_results.len(), 2);
    assert_eq!(binary_results[0].mime_type, "application/octet-stream");
    assert_eq!(binary_results[1].mime_type, "application/octet-stream");
}

#[test]
fn convert_mcp_call_tool_result_omits_binary_results_without_binary_content() {
    let result = convert_mcp_call_tool_result(&serde_json::json!({
        "content": [
            { "type": "text", "text": "hello" },
            {
                "type": "resource",
                "resource": {
                    "uri": "file:///tmp/readme.md",
                    "text": "resource text"
                }
            }
        ]
    }))
    .expect("valid CallToolResult should convert");

    let ToolResult::Expanded(expanded) = result else {
        panic!("expected expanded tool result");
    };

    assert_eq!(expanded.text_result_for_llm, "hello\nresource text");
    assert!(expanded.binary_results_for_llm.is_none());
}

#[tokio::test]
async fn tool_handler_call_returns_result() {
    let tool = EchoTool;
    let inv = ToolInvocation {
        session_id: SessionId::from("s1"),
        tool_call_id: "tc1".to_string(),
        tool_name: "echo".to_string(),
        arguments: serde_json::json!({"msg": "hello"}),
        available_tools: None,
        traceparent: None,
        tracestate: None,
    };

    let result = tool.call(inv).await.unwrap();
    match result {
        ToolResult::Text(s) => assert!(s.contains("hello")),
        _ => panic!("expected Text result"),
    }
}

#[cfg(feature = "derive")]
#[tokio::test]
async fn define_tool_builds_schema_and_dispatches() {
    use serde::Deserialize;

    #[derive(Deserialize, schemars::JsonSchema)]
    struct Params {
        city: String,
    }

    let tool = define_tool(
        "weather",
        "Get the weather for a city",
        |_inv, params: Params| async move { Ok(ToolResult::Text(format!("sunny in {}", params.city))) },
    );

    assert_eq!(tool.name, "weather");
    assert_eq!(tool.description, "Get the weather for a city");
    assert_eq!(tool.parameters["type"], "object");
    assert!(tool.parameters["properties"]["city"].is_object());
    let handler = tool.handler.as_ref().expect("define_tool attaches handler");

    let inv = ToolInvocation {
        session_id: SessionId::from("s1"),
        tool_call_id: "tc1".to_string(),
        tool_name: "weather".to_string(),
        arguments: serde_json::json!({"city": "Seattle"}),
        available_tools: None,
        traceparent: None,
        tracestate: None,
    };
    match handler.call(inv).await.unwrap() {
        ToolResult::Text(s) => assert_eq!(s, "sunny in Seattle"),
        _ => panic!("expected Text result"),
    }
}

// Tests requiring `schemars` (the `derive` feature).
#[cfg(feature = "derive")]
mod derive_tests {
    use serde::Deserialize;

    use super::super::*;
    use crate::{ErrorKind, SessionId};

    #[derive(Deserialize, schemars::JsonSchema)]
    struct GetWeatherParams {
        /// City name to get weather for.
        city: String,
        /// Temperature unit (celsius or fahrenheit).
        unit: Option<String>,
    }

    #[test]
    fn schema_for_generates_clean_schema() {
        let schema = schema_for::<GetWeatherParams>();
        assert_eq!(schema["type"], "object");
        assert!(schema["properties"]["city"].is_object());
        assert!(schema["properties"]["unit"].is_object());
        // city is required (non-Option), unit is not
        let required = schema["required"].as_array().unwrap();
        assert!(required.contains(&serde_json::json!("city")));
        assert!(!required.contains(&serde_json::json!("unit")));
        // Root-level metadata stripped
        assert!(schema.get("$schema").is_none());
        assert!(schema.get("title").is_none());
    }

    struct GetWeatherTool;

    fn get_weather_tool() -> Tool {
        Tool {
            name: "get_weather".to_string(),
            description: "Get weather for a city".to_string(),
            parameters: tool_parameters(schema_for::<GetWeatherParams>()),
            ..Default::default()
        }
        .with_handler(std::sync::Arc::new(GetWeatherTool))
    }

    #[async_trait]
    impl ToolHandler for GetWeatherTool {
        async fn call(&self, inv: ToolInvocation) -> Result<ToolResult, Error> {
            let params: GetWeatherParams = serde_json::from_value(inv.arguments)?;
            Ok(ToolResult::Text(format!(
                "{} {}",
                params.city,
                params.unit.unwrap_or_default()
            )))
        }
    }

    #[test]
    fn tool_handler_with_schema_for() {
        let def = get_weather_tool();
        assert_eq!(def.name, "get_weather");
        let schema = serde_json::to_value(&def.parameters).expect("serialize tool parameters");
        assert_eq!(schema["type"], "object");
        assert!(schema["properties"]["city"].is_object());
        assert!(def.handler.is_some());
    }

    #[tokio::test]
    async fn tool_handler_deserializes_typed_params() {
        let tool = GetWeatherTool;
        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "get_weather".to_string(),
            arguments: serde_json::json!({"city": "Seattle", "unit": "celsius"}),
            available_tools: None,
            traceparent: None,
            tracestate: None,
        };

        let result = tool.call(inv).await.unwrap();
        match result {
            ToolResult::Text(s) => assert_eq!(s, "Seattle celsius"),
            _ => panic!("expected Text result"),
        }
    }

    #[tokio::test]
    async fn tool_handler_returns_error_on_bad_params() {
        let tool = GetWeatherTool;
        let inv = ToolInvocation {
            session_id: SessionId::from("s1"),
            tool_call_id: "tc1".to_string(),
            tool_name: "get_weather".to_string(),
            arguments: serde_json::json!({"wrong_field": 42}),
            available_tools: None,
            traceparent: None,
            tracestate: None,
        };

        let err = tool.call(inv).await.unwrap_err();
        assert!(matches!(err.kind(), ErrorKind::Json));
    }

    #[tokio::test]
    async fn schema_for_derived_tool_round_trips_through_call() {
        let tool = GetWeatherTool;

        // Calling the tool with matching arguments returns the
        // expected typed result. (Per-name dispatch is the SDK's
        // concern; here we exercise just the handler contract.)
        let result = tool
            .call(ToolInvocation {
                session_id: SessionId::from("s1"),
                tool_call_id: "tc1".to_string(),
                tool_name: "get_weather".to_string(),
                arguments: serde_json::json!({"city": "Portland"}),
                available_tools: None,
                traceparent: None,
                tracestate: None,
            })
            .await
            .expect("ToolHandler::call should succeed for matching args");
        match result {
            ToolResult::Text(s) => assert!(s.contains("Portland")),
            _ => panic!("expected ToolResult::Text"),
        }
    }
}
