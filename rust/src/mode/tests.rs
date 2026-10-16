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
        .add_custom("foo")
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
            "custom:foo",
            "custom:*",
            "mcp:github-list_issues",
            "mcp:*",
        ]
    );
}

#[test]
fn tool_set_add_builtin_many() {
    let v = ToolSet::new()
        .add_builtin_many(BUILTIN_TOOLS_ISOLATED)
        .unwrap()
        .into_vec();
    assert_eq!(v.len(), BUILTIN_TOOLS_ISOLATED.len());
    assert_eq!(v[0], format!("builtin:{}", BUILTIN_TOOLS_ISOLATED[0]));
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
fn tool_set_accepts_wildcard_and_underscores_and_dashes() {
    assert!(ToolSet::new().add_builtin("*").is_ok());
    assert!(ToolSet::new().add_mcp("github-list_issues").is_ok());
    assert!(ToolSet::new().add_custom("A_b-9").is_ok());
}

#[test]
fn into_vec_is_idempotent_with_to_vec() {
    let ts = ToolSet::new().add_builtin("bash").unwrap();
    assert_eq!(ts.to_vec(), vec!["builtin:bash"]);
    assert_eq!(ts.into_vec(), vec!["builtin:bash"]);
}

#[test]
fn into_vec_string_conversion() {
    let v: Vec<String> = ToolSet::new().add_mcp("*").unwrap().into();
    assert_eq!(v, vec!["mcp:*"]);
}

#[test]
fn validate_tool_filter_list_rejects_bare_star() {
    let bad = vec!["*".to_string()];
    assert!(validate_tool_filter_list("availableTools", Some(&bad)).is_err());
}

#[test]
fn validate_tool_filter_list_allows_qualified_star() {
    let ok = vec!["builtin:*".to_string(), "mcp:*".to_string()];
    assert!(validate_tool_filter_list("availableTools", Some(&ok)).is_ok());
}

#[test]
fn validate_tool_filter_list_none_is_ok() {
    assert!(validate_tool_filter_list("availableTools", None).is_ok());
}

#[test]
fn builtin_tools_isolated_contents() {
    assert!(BUILTIN_TOOLS_ISOLATED.contains(&"ask_user"));
    assert!(BUILTIN_TOOLS_ISOLATED.contains(&"task_complete"));
    assert!(BUILTIN_TOOLS_ISOLATED.contains(&"skill"));
    assert!(!BUILTIN_TOOLS_ISOLATED.contains(&"bash"));
    assert!(!BUILTIN_TOOLS_ISOLATED.contains(&"edit"));
    assert!(!BUILTIN_TOOLS_ISOLATED.contains(&"web_fetch"));
}

#[test]
fn client_mode_default_is_copilot_cli() {
    assert_eq!(ClientMode::default(), ClientMode::CopilotCli);
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
fn memory_copilot_cli_leaves_unset_when_not_supplied() {
    assert_eq!(memory_for_mode(ClientMode::CopilotCli, None), None);
}

#[test]
fn memory_copilot_cli_preserves_supplied() {
    assert_eq!(
        memory_for_mode(ClientMode::CopilotCli, Some(MemoryConfiguration::enabled())),
        Some(MemoryConfiguration::enabled())
    );
}

#[test]
fn memory_empty_defaults_to_disabled() {
    assert_eq!(
        memory_for_mode(ClientMode::Empty, None),
        Some(MemoryConfiguration::disabled())
    );
}

#[test]
fn memory_empty_preserves_supplied() {
    assert_eq!(
        memory_for_mode(ClientMode::Empty, Some(MemoryConfiguration::enabled())),
        Some(MemoryConfiguration::enabled())
    );
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
