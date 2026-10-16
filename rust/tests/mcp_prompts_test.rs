// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use std::path::Path;

use github_copilot_sdk::rpc::{
    McpPromptsGetRequest, McpPromptsGetResult, McpPromptsListRequest, McpPromptsListResult,
};
use serde_json::{Value, json};

// Serialization coverage complements Node/.NET's real MCP boundary tests.
#[test]
fn prompt_results_preserve_opaque_content_and_optional_fields() {
    let fixture_path =
        Path::new(env!("CARGO_MANIFEST_DIR")).join("../test/harness/mcp-prompt-fixtures.json");
    let fixtures: Value =
        serde_json::from_str(&std::fs::read_to_string(fixture_path).unwrap()).unwrap();
    for page in ["firstPage", "secondPage"] {
        let result: McpPromptsListResult = serde_json::from_value(fixtures[page].clone()).unwrap();
        assert_eq!(serde_json::to_value(result).unwrap(), fixtures[page]);
    }
    let result: McpPromptsGetResult =
        serde_json::from_value(fixtures["richPrompt"].clone()).unwrap();
    assert_eq!(
        serde_json::to_value(result).unwrap(),
        fixtures["richPrompt"]
    );
}

#[test]
fn prompt_requests_preserve_omitted_empty_and_string_arguments() {
    for arguments in [
        None,
        Some(json!({})),
        Some(json!({"topic": "日本語", "style": ""})),
    ] {
        let mut wire = json!({"serverName": "fixture", "promptName": "rich"});
        if let Some(arguments) = arguments {
            wire["arguments"] = arguments;
        }
        let request: McpPromptsGetRequest = serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(serde_json::to_value(request).unwrap(), wire);
    }
    for wire in [
        json!({"serverName": "fixture"}),
        json!({"serverName": "fixture", "cursor": "page:2/opaque+cursor="}),
    ] {
        let request: McpPromptsListRequest = serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(serde_json::to_value(request).unwrap(), wire);
    }
    assert!(
        serde_json::from_value::<McpPromptsGetRequest>(
            json!({"serverName": "fixture", "promptName": "rich", "arguments": {"topic": 42}})
        )
        .is_err()
    );
}
