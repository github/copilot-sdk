//! Wire-format structs for the `session.create` and `session.resume`
//! JSON-RPC payloads.
//!
//! Built explicitly from [`SessionConfig`](crate::types::SessionConfig) and
//! [`ResumeSessionConfig`](crate::types::ResumeSessionConfig) at
//! `Client::create_session` / `Client::resume_session` time via
//! [`SessionConfig::into_wire`](crate::types::SessionConfig::into_wire) and
//! [`ResumeSessionConfig::into_wire`](crate::types::ResumeSessionConfig::into_wire),
//! respectively.
//!
//! Keeping the wire shape separate from the user-facing config avoids
//! having callback fields on a serializable struct: the user-facing
//! configs hold trait-object handlers, the wire structs hold only the
//! plain data the runtime needs.

use std::collections::HashMap;
use std::path::PathBuf;

use serde::Serialize;

use crate::canvas::CanvasDeclaration;
use crate::generated::api_types::{
    ModelCapabilitiesOverride, OpenCanvasInstance, RemoteSessionMode,
};
use crate::generated::session_events::ReasoningSummary;
use crate::types::{
    CloudSessionOptions, ContextTier, CustomAgentConfig, DefaultAgentConfig, ExtensionInfo,
    InfiniteSessionConfig, LargeToolOutputConfig, McpServerConfig, ProviderConfig, SessionId,
    SystemMessageConfig, Tool,
};

/// Wire representation of a slash command (name + description only). The
/// runtime executes the command; the SDK's `CommandHandler` callback is
/// invoked from a separate dispatch path and never crosses the wire.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CommandWireDefinition {
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
}

/// The exact JSON shape sent on the `session.create` JSON-RPC request.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionCreateWire {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub streaming: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<SystemMessageConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<Tool>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvases: Option<Vec<CanvasDeclaration>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_canvas_renderer: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_extensions: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_sdk_path: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_info: Option<ExtensionInfo>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// SDK always sends `"excluded"` so include + exclude lists compose
    /// naturally (everything matching X except Y).
    pub tool_filter_precedence: &'static str,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_oauth_token_storage: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub embedding_cache_storage: Option<String>,
    pub env_value_mode: &'static str,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_config_discovery: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_embedding_retrieval: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub organization_custom_instructions: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_on_demand_instruction_discovery: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_file_hooks: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_host_git_operations: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_session_store: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_skills: Option<bool>,
    pub request_user_input: bool,
    pub request_permission: bool,
    pub request_exit_plan_mode: bool,
    pub request_auto_mode_switch: bool,
    pub request_elicitation: bool,
    pub request_mcp_apps: bool,
    pub hooks: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<PathBuf>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instruction_directories: Option<Vec<PathBuf>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_directories: Option<Vec<PathBuf>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub large_output: Option<LargeToolOutputConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_agent: Option<DefaultAgentConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_session_telemetry: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<PathBuf>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<PathBuf>,
    #[serde(rename = "gitHubToken", skip_serializing_if = "Option::is_none")]
    pub github_token: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session: Option<RemoteSessionMode>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cloud: Option<CloudSessionOptions>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_sub_agent_streaming_events: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commands: Option<Vec<CommandWireDefinition>>,
}

/// The exact JSON shape sent on the `session.resume` JSON-RPC request.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionResumeWire {
    pub session_id: SessionId,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub streaming: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<SystemMessageConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<Tool>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvases: Option<Vec<CanvasDeclaration>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub open_canvases: Option<Vec<OpenCanvasInstance>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_canvas_renderer: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_extensions: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_sdk_path: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_info: Option<ExtensionInfo>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// SDK always sends `"excluded"`. See create-wire docs.
    pub tool_filter_precedence: &'static str,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, McpServerConfig>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_oauth_token_storage: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub embedding_cache_storage: Option<String>,
    pub env_value_mode: &'static str,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_config_discovery: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_embedding_retrieval: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub organization_custom_instructions: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_on_demand_instruction_discovery: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_file_hooks: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_host_git_operations: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_session_store: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_skills: Option<bool>,
    pub request_user_input: bool,
    pub request_permission: bool,
    pub request_exit_plan_mode: bool,
    pub request_auto_mode_switch: bool,
    pub request_elicitation: bool,
    pub request_mcp_apps: bool,
    pub hooks: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<PathBuf>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instruction_directories: Option<Vec<PathBuf>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_directories: Option<Vec<PathBuf>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub large_output: Option<LargeToolOutputConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents: Option<Vec<CustomAgentConfig>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_agent: Option<DefaultAgentConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub infinite_sessions: Option<InfiniteSessionConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_session_telemetry: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<PathBuf>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<PathBuf>,
    #[serde(rename = "gitHubToken", skip_serializing_if = "Option::is_none")]
    pub github_token: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session: Option<RemoteSessionMode>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_sub_agent_streaming_events: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commands: Option<Vec<CommandWireDefinition>>,
    /// Maps to wire field `disableResume`.
    #[serde(rename = "disableResume", skip_serializing_if = "Option::is_none")]
    pub suppress_resume_event: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continue_pending_work: Option<bool>,
}
