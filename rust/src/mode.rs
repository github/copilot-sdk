/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

//! Client-level "empty" mode for minimal/safe defaults.
//!
//! See the plan in <https://github.com/github/copilot-agent-runtime/issues/7155>:
//! [`ClientMode::Empty`] disables ambient CLI-style behavior by default so an
//! app must explicitly opt back into features. This module exposes the public
//! enum, the [`ToolSet`] builder for source-qualified tool filter patterns,
//! and the [`BUILTIN_TOOLS_ISOLATED`] curated allowlist.

use std::collections::HashMap;

use crate::types::{SectionOverride, SystemMessageConfig};

/// Controls SDK defaults for ambient CLI-style behavior.
///
/// - [`ClientMode::CopilotCli`] (default): defaults equivalent to Copilot CLI.
///   Useful when building a coding agent that shares sessions with Copilot CLI.
///   **Do not use this mode for server-based multi-user applications** — the
///   default coding agent has tools and capabilities that operate across
///   sessions and can access the host OS environment.
/// - [`ClientMode::Empty`]: disables optional features by default. The app
///   must explicitly opt into anything it needs. Required for any scenario
///   where CLI-like ambient behavior is unsafe (e.g. multi-user servers).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ClientMode {
    /// Defaults equivalent to Copilot CLI (the default).
    #[default]
    CopilotCli,
    /// Disables optional features by default; app must opt in explicitly.
    Empty,
}

/// Tool name character set enforced by the runtime at every registration
/// boundary. Mirrors the runtime's `VALID_TOOL_NAME_REGEX`.
fn is_valid_tool_name(name: &str) -> bool {
    !name.is_empty()
        && name
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-')
}

fn validate_name(kind: &str, name: &str) -> Result<(), crate::Error> {
    if name == "*" {
        return Ok(());
    }
    if !is_valid_tool_name(name) {
        return Err(crate::Error::with_message(
            crate::ErrorKind::InvalidConfig,
            format!(
                "Invalid {kind} tool name '{name}': tool names must match \
             /^[a-zA-Z0-9_-]+$/ or be the wildcard '*'."
            ),
        ));
    }
    Ok(())
}

/// Builder that produces source-qualified tool filter strings (e.g.
/// `"builtin:bash"`, `"mcp:*"`, `"custom:foo"`) for the session's
/// `available_tools` list.
///
/// Tools are classified by the runtime at registration time, not from name
/// parsing — so `add_builtin("foo")` matches only tools registered as
/// built-in, even if an MCP server happens to register a tool with the same
/// wire name.
///
/// # Example
///
/// ```
/// # use github_copilot_sdk::mode::{ToolSet, BUILTIN_TOOLS_ISOLATED};
/// let tools = ToolSet::new()
///     .add_builtin_many(BUILTIN_TOOLS_ISOLATED)?
///     .add_mcp("*")?
///     .add_custom("*")?
///     .to_vec();
/// # Ok::<(), github_copilot_sdk::Error>(())
/// ```
#[derive(Debug, Clone, Default)]
pub struct ToolSet {
    items: Vec<String>,
}

impl ToolSet {
    /// Construct an empty tool set.
    pub fn new() -> Self {
        Self::default()
    }

    /// Add a single built-in tool pattern. Pass a specific name (e.g.
    /// `"bash"`) or `"*"` to match all built-in tools.
    pub fn add_builtin(mut self, name: &str) -> Result<Self, crate::Error> {
        validate_name("builtin", name)?;
        self.items.push(format!("builtin:{name}"));
        Ok(self)
    }

    /// Add a list of built-in tool patterns (e.g. [`BUILTIN_TOOLS_ISOLATED`]).
    pub fn add_builtin_many<I, S>(mut self, names: I) -> Result<Self, crate::Error>
    where
        I: IntoIterator<Item = S>,
        S: AsRef<str>,
    {
        for name in names {
            let name = name.as_ref();
            validate_name("builtin", name)?;
            self.items.push(format!("builtin:{name}"));
        }
        Ok(self)
    }

    /// Add a custom tool pattern. Matches tools registered via the SDK's
    /// `tools` option or via custom agents.
    pub fn add_custom(mut self, name: &str) -> Result<Self, crate::Error> {
        validate_name("custom", name)?;
        self.items.push(format!("custom:{name}"));
        Ok(self)
    }

    /// Add an MCP tool pattern. Pass the runtime's canonical wire name
    /// (e.g. `"github-list_issues"`) or `"*"` to match all MCP tools.
    pub fn add_mcp(mut self, tool_name: &str) -> Result<Self, crate::Error> {
        validate_name("mcp", tool_name)?;
        self.items.push(format!("mcp:{tool_name}"));
        Ok(self)
    }

    /// Returns a defensive copy of the accumulated filter strings.
    pub fn to_vec(&self) -> Vec<String> {
        self.items.clone()
    }

    /// Returns the accumulated filter strings, consuming the builder.
    pub fn into_vec(self) -> Vec<String> {
        self.items
    }

    /// Number of accumulated filter strings.
    pub fn len(&self) -> usize {
        self.items.len()
    }

    /// Returns `true` if no filter strings have been added.
    pub fn is_empty(&self) -> bool {
        self.items.is_empty()
    }
}

impl From<ToolSet> for Vec<String> {
    fn from(value: ToolSet) -> Self {
        value.into_vec()
    }
}

/// Built-in tools that operate only within the bounds of a single session —
/// no host filesystem access outside the session, no cross-session state,
/// no host environment access, no network.
///
/// Safe to enable in [`ClientMode::Empty`] scenarios (e.g. multi-tenant
/// servers) without leaking host capabilities.
///
/// **Contract:** tools in this set MUST NOT be extended (even behind options
/// or args) to read or write state outside the session boundary. Adding
/// cross-session or host-state behavior to one of these tools is a breaking
/// change that requires removing it from this set.
pub const BUILTIN_TOOLS_ISOLATED: &[&str] = &[
    "ask_user",
    "task_complete",
    "exit_plan_mode",
    "task",
    "read_agent",
    "write_agent",
    "list_agents",
    "send_inbox",
    "context_board",
    "skill",
];

/// Validate a tool filter list (`available_tools` or `excluded_tools`).
/// Rejects the bare `"*"` shorthand with a clear error pointing the developer
/// at the source-qualified forms.
pub(crate) fn validate_tool_filter_list(
    field: &str,
    list: Option<&[String]>,
) -> Result<(), crate::Error> {
    let Some(list) = list else { return Ok(()) };
    for item in list {
        if item == "*" {
            return Err(crate::Error::with_message(
                crate::ErrorKind::InvalidConfig,
                format!(
                    "{field} contains a bare '*' which matches no tool. Use \
                 source-qualified wildcards instead: \
                 ToolSet::new().add_builtin(\"*\").add_mcp(\"*\").add_custom(\"*\")."
                ),
            ));
        }
    }
    Ok(())
}

/// Returns the system message config to use, adjusted for the current mode.
/// In empty mode we ensure the `environment_context` section is removed
/// unless the app has already taken control of it.
pub(crate) fn system_message_for_mode(
    mode: ClientMode,
    supplied: Option<SystemMessageConfig>,
) -> Option<SystemMessageConfig> {
    if mode != ClientMode::Empty {
        return supplied;
    }
    let strip_env = || {
        let mut sections = HashMap::new();
        sections.insert(
            "environment_context".to_string(),
            SectionOverride {
                action: Some("remove".to_string()),
                content: None,
            },
        );
        sections
    };
    let Some(supplied) = supplied else {
        return Some(SystemMessageConfig {
            mode: Some("customize".to_string()),
            content: None,
            sections: Some(strip_env()),
        });
    };
    let mode_str = supplied.mode.as_deref().unwrap_or("append");
    match mode_str {
        "replace" => Some(supplied),
        "customize" => {
            if supplied
                .sections
                .as_ref()
                .is_some_and(|s| s.contains_key("environment_context"))
            {
                Some(supplied)
            } else {
                let mut sections = supplied.sections.unwrap_or_default();
                sections.insert(
                    "environment_context".to_string(),
                    SectionOverride {
                        action: Some("remove".to_string()),
                        content: None,
                    },
                );
                Some(SystemMessageConfig {
                    mode: Some("customize".to_string()),
                    content: supplied.content,
                    sections: Some(sections),
                })
            }
        }
        // "append" or any unrecognized value: promote to customize so we
        // can also strip environment_context; the runtime appends `content`
        // to additional instructions either way.
        _ => Some(SystemMessageConfig {
            mode: Some("customize".to_string()),
            content: supplied.content,
            sections: Some(strip_env()),
        }),
    }
}

/// Returns the `enable_experimental_mode` value to send for the given mode.
pub(crate) fn experimental_mode_for_mode(
    mode: ClientMode,
    supplied: Option<bool>,
) -> Option<bool> {
    if mode == ClientMode::Empty {
        Some(supplied.unwrap_or(false))
    } else {
        supplied
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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
    fn experimental_mode_defaults_false_in_empty_mode() {
        assert_eq!(experimental_mode_for_mode(ClientMode::Empty, None), Some(false));
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
}
