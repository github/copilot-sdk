// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use super::SessionConfig;

#[test]
fn refresh_custom_instructions_builder_default_and_debug() {
    let config = SessionConfig::default();
    assert_eq!(config.refresh_custom_instructions, None);
    assert!(format!("{config:?}").contains("refresh_custom_instructions: None"));

    let config = config.with_refresh_custom_instructions(true);
    assert_eq!(config.refresh_custom_instructions, Some(true));
    assert!(format!("{config:?}").contains("refresh_custom_instructions: Some(true)"));

    let config = config.with_refresh_custom_instructions(false);
    assert_eq!(config.refresh_custom_instructions, Some(false));
    assert!(format!("{config:?}").contains("refresh_custom_instructions: Some(false)"));
}
