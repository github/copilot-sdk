/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[test]
fn custom_agents_local_only_respects_mode_and_caller_value() {
    assert_eq!(
        resolve_custom_agents_local_only(ClientMode::Empty, None),
        Some(true)
    );
    assert_eq!(
        resolve_custom_agents_local_only(ClientMode::Empty, Some(false)),
        Some(false)
    );
    assert_eq!(
        resolve_custom_agents_local_only(ClientMode::CopilotCli, None),
        None
    );
}

#[test]
fn tool_set_emits_source_qualified_patterns() {
    let v = ToolSet::new()
        .add_builtin("bash")
        .unwrap()
        .add_builtin("*")
        .unwrap()
        .add_custom("A_b-9")
        .unwrap()
        .add_custom("*")
        .unwrap()
        .add_mcp("github-list_issues")
        .unwrap()
        .add_mcp("*")
        .unwrap()
        .to_vec();
    assert_eq!(
        v,
        vec![
            "builtin:bash",
            "builtin:*",
            "custom:A_b-9",
            "custom:*",
            "mcp:github-list_issues",
            "mcp:*",
        ]
    );
}

#[test]
fn tool_set_rejects_invalid_names() {
    for bad in ["bash!", "with space", "colon:name", "", "wild*card"] {
        assert!(
            ToolSet::new().add_builtin(bad).is_err(),
            "expected '{bad}' to be rejected"
        );
        assert!(ToolSet::new().add_custom(bad).is_err());
        assert!(ToolSet::new().add_mcp(bad).is_err());
    }
}

#[test]
fn validate_tool_filter_list_accepts_only_qualified_wildcards() {
    assert!(validate_tool_filter_list("availableTools", None).is_ok());
    for (list, valid) in [
        (vec!["*".to_string()], false),
        (vec!["builtin:*".to_string(), "mcp:*".to_string()], true),
    ] {
        assert_eq!(
            validate_tool_filter_list("availableTools", Some(&list)).is_ok(),
            valid,
            "{list:?}"
        );
    }
}

#[test]
fn system_message_copilot_cli_passes_through_unchanged() {
    let cfg = SystemMessageConfig {
        mode: Some("append".to_string()),
        content: Some("hello".to_string()),
        sections: None,
    };
    let out = system_message_for_mode(ClientMode::CopilotCli, Some(cfg.clone()));
    let out = out.unwrap();
    assert_eq!(out.mode.as_deref(), Some("append"));
    assert_eq!(out.content.as_deref(), Some("hello"));
}

#[test]
fn system_message_empty_none_injects_strip() {
    let out = system_message_for_mode(ClientMode::Empty, None).unwrap();
    assert_eq!(out.mode.as_deref(), Some("customize"));
    let sections = out.sections.unwrap();
    let env = sections.get("environment_context").unwrap();
    assert_eq!(env.action.as_deref(), Some("remove"));
}

#[test]
fn system_message_empty_append_promoted_to_customize() {
    let cfg = SystemMessageConfig {
        mode: Some("append".to_string()),
        content: Some("hi".to_string()),
        sections: None,
    };
    let out = system_message_for_mode(ClientMode::Empty, Some(cfg)).unwrap();
    assert_eq!(out.mode.as_deref(), Some("customize"));
    assert_eq!(out.content.as_deref(), Some("hi"));
    let sections = out.sections.unwrap();
    assert!(sections.contains_key("environment_context"));
}

#[test]
fn system_message_empty_replace_passes_through() {
    let cfg = SystemMessageConfig {
        mode: Some("replace".to_string()),
        content: Some("verbatim".to_string()),
        sections: None,
    };
    let out = system_message_for_mode(ClientMode::Empty, Some(cfg.clone())).unwrap();
    assert_eq!(out.mode.as_deref(), Some("replace"));
    assert_eq!(out.content.as_deref(), Some("verbatim"));
    assert!(out.sections.is_none());
}

#[test]
fn system_message_empty_customize_with_env_context_preserved() {
    let mut sections = HashMap::new();
    sections.insert(
        "environment_context".to_string(),
        SectionOverride {
            action: Some("replace".to_string()),
            content: Some("custom env".to_string()),
        },
    );
    let cfg = SystemMessageConfig {
        mode: Some("customize".to_string()),
        content: None,
        sections: Some(sections),
    };
    let out = system_message_for_mode(ClientMode::Empty, Some(cfg)).unwrap();
    let env = out.sections.unwrap().remove("environment_context").unwrap();
    assert_eq!(env.action.as_deref(), Some("replace"));
    assert_eq!(env.content.as_deref(), Some("custom env"));
}

#[test]
fn system_message_empty_customize_without_env_context_gets_strip() {
    let mut sections = HashMap::new();
    sections.insert(
        "other_section".to_string(),
        SectionOverride {
            action: Some("replace".to_string()),
            content: Some("body".to_string()),
        },
    );
    let cfg = SystemMessageConfig {
        mode: Some("customize".to_string()),
        content: None,
        sections: Some(sections),
    };
    let out = system_message_for_mode(ClientMode::Empty, Some(cfg)).unwrap();
    let secs = out.sections.unwrap();
    assert!(secs.contains_key("other_section"));
    let env = secs.get("environment_context").unwrap();
    assert_eq!(env.action.as_deref(), Some("remove"));
}

#[test]
fn memory_defaults_follow_mode_and_preserve_explicit_values() {
    for mode in [ClientMode::CopilotCli, ClientMode::Empty] {
        assert_eq!(
            memory_for_mode(mode, None),
            (mode == ClientMode::Empty).then(MemoryConfiguration::disabled),
            "{mode:?}"
        );
        for supplied in [
            MemoryConfiguration::enabled(),
            MemoryConfiguration::disabled(),
        ] {
            assert_eq!(
                memory_for_mode(mode, Some(supplied.clone())),
                Some(supplied),
                "{mode:?}"
            );
        }
    }
}

#[test]
fn experimental_mode_defaults_false_in_empty_mode() {
    assert_eq!(
        experimental_mode_for_mode(ClientMode::Empty, None),
        Some(false)
    );
    assert_eq!(
        experimental_mode_for_mode(ClientMode::Empty, Some(true)),
        Some(true)
    );
    assert_eq!(
        experimental_mode_for_mode(ClientMode::Empty, Some(false)),
        Some(false)
    );
}

#[test]
fn experimental_mode_remains_runtime_controlled_in_copilot_cli_mode() {
    assert_eq!(
        experimental_mode_for_mode(ClientMode::CopilotCli, None),
        None
    );
}
