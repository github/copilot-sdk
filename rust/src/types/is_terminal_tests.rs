/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::Tool;

#[test]
fn is_terminal_serializes_as_camel_case_when_set() {
    let tool = Tool {
        name: "clear_context".to_owned(),
        is_terminal: true,
        ..Default::default()
    };
    let value = serde_json::to_value(&tool).expect("tool serializes");
    assert_eq!(
        value.get("isTerminal"),
        Some(&serde_json::Value::Bool(true))
    );
}

#[test]
fn is_terminal_is_omitted_when_false() {
    let tool = Tool {
        name: "plain".to_owned(),
        ..Default::default()
    };
    let value = serde_json::to_value(&tool).expect("tool serializes");
    assert!(value.get("isTerminal").is_none());
}

/// `Tool` has a hand-written `Debug` impl, so a new field is only reported
/// if it is added there by hand. Guard against that drift.
#[test]
fn is_terminal_appears_in_debug_output() {
    let terminal = Tool {
        name: "clear_context".to_owned(),
        is_terminal: true,
        ..Default::default()
    };
    assert!(format!("{terminal:?}").contains("is_terminal: true"));

    let plain = Tool {
        name: "plain".to_owned(),
        ..Default::default()
    };
    assert!(format!("{plain:?}").contains("is_terminal: false"));
}
