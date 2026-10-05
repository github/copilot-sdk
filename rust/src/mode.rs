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

use crate::types::{MemoryConfiguration, SectionOverride, SystemMessageConfig};

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

/// Resolve the effective custom-agents locality setting for a client mode.
pub(crate) fn resolve_custom_agents_local_only(
    mode: ClientMode,
    custom_agents_local_only: Option<bool>,
) -> Option<bool> {
    custom_agents_local_only.or_else(|| (mode == ClientMode::Empty).then_some(true))
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

/// Returns the memory configuration to use, adjusted for the current mode.
///
/// In [`ClientMode::Empty`] the memory feature defaults to disabled so an app
/// must opt in explicitly. In [`ClientMode::CopilotCli`] no SDK default is
/// applied: the configuration is left unset so the runtime applies its own
/// default for the memory feature. A value supplied by the app always wins.
pub(crate) fn memory_for_mode(
    mode: ClientMode,
    supplied: Option<MemoryConfiguration>,
) -> Option<MemoryConfiguration> {
    match supplied {
        Some(config) => Some(config),
        None if mode == ClientMode::Empty => Some(MemoryConfiguration::disabled()),
        None => None,
    }
}

/// Returns the `enable_experimental_mode` value to send for the given mode.
pub(crate) fn experimental_mode_for_mode(mode: ClientMode, supplied: Option<bool>) -> Option<bool> {
    if mode == ClientMode::Empty {
        Some(supplied.unwrap_or(false))
    } else {
        supplied
    }
}

#[cfg(test)]
mod tests;
