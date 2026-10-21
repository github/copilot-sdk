/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::{Tool, ToolNamespace};

#[test]
fn namespace_serializes_with_optional_description() {
    let described = Tool::new("word_create").with_namespace(
        ToolNamespace::new("word").with_description("Create and edit Word documents"),
    );
    assert_eq!(
        serde_json::to_value(&described).expect("tool serializes")["namespace"],
        serde_json::json!({"name": "word", "description": "Create and edit Word documents"})
    );

    let undescribed = Tool::new("word_create").with_namespace(ToolNamespace::new("word"));
    assert_eq!(
        serde_json::to_value(&undescribed).expect("tool serializes")["namespace"],
        serde_json::json!({"name": "word"})
    );
}

#[test]
fn namespace_is_omitted_when_unset() {
    let value = serde_json::to_value(Tool::new("plain")).expect("tool serializes");
    assert!(value.get("namespace").is_none());
}

/// `Tool` has a hand-written `Debug` impl, so a new field is only reported
/// if it is added there by hand. Guard against that drift.
#[test]
fn namespace_appears_in_debug_output() {
    let tool = Tool::new("word_create").with_namespace(ToolNamespace::new("word"));
    assert!(format!("{tool:?}").contains("namespace: Some(ToolNamespace"));
}
