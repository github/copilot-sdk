/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;
#[cfg(feature = "derive")]
use crate::types::SessionId;

#[test]
fn try_tool_parameters_rejects_non_object_schema() {
    let err = try_tool_parameters(serde_json::json!(["not", "an", "object"]))
        .expect_err("non-object schemas should be rejected");

    assert!(err.is_data());
}

#[test]
fn tool_parameters_sort_keys_of_schemas_built_from_hash_maps() {
    // A schema converted from a `HashMap` carries that map's per-process
    // iteration order into an insertion-ordered `Value`. The tool definition
    // must not depend on it, at any depth.
    let properties: std::collections::HashMap<String, serde_json::Value> = [
        "zeta", "alpha", "mu", "beta", "omega", "gamma", "kappa", "delta",
    ]
    .into_iter()
    .map(|name| {
        let field: std::collections::HashMap<&str, &str> =
            [("type", "string"), ("description", name)].into();
        (name.to_string(), serde_json::to_value(field).unwrap())
    })
    .collect();
    let mut schema = serde_json::Map::new();
    schema.insert("type".into(), "object".into());
    schema.insert(
        "properties".into(),
        serde_json::to_value(properties).unwrap(),
    );

    let parameters = tool_parameters(serde_json::Value::Object(schema));

    assert_eq!(
        parameters.keys().map(String::as_str).collect::<Vec<_>>(),
        ["properties", "type"]
    );
    let properties = parameters["properties"].as_object().unwrap();
    assert_eq!(
        properties.keys().map(String::as_str).collect::<Vec<_>>(),
        [
            "alpha", "beta", "delta", "gamma", "kappa", "mu", "omega", "zeta"
        ]
    );
    for field in properties.values() {
        assert_eq!(
            field
                .as_object()
                .unwrap()
                .keys()
                .map(String::as_str)
                .collect::<Vec<_>>(),
            ["description", "type"]
        );
    }
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
    assert!(!tool.parameters.contains_key("$schema"));
    assert!(!tool.parameters.contains_key("title"));
    let handler = tool.handler.as_ref().expect("define_tool attaches handler");

    let mut inv = ToolInvocation {
        session_id: SessionId::from("s1"),
        tool_call_id: "tc1".to_string(),
        tool_name: "weather".to_string(),
        arguments: serde_json::json!({"city": "Seattle"}),
        available_tools: None,
        traceparent: None,
        tracestate: None,
        agent_id: None,
        request_id: None,
    };
    match handler.call(inv.clone()).await.unwrap() {
        ToolResult::Text(s) => assert_eq!(s, "sunny in Seattle"),
        _ => panic!("expected Text result"),
    }

    inv.arguments = serde_json::json!({"wrong_field": 42});
    let error = handler.call(inv).await.unwrap_err();
    assert!(matches!(error.kind(), crate::ErrorKind::Json));
}
