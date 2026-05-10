#![cfg(feature = "test-support")]
#![allow(clippy::unwrap_used)]

#[path = "e2e/abort.rs"]
mod abort;
#[path = "e2e/ask_user.rs"]
mod ask_user;
#[path = "e2e/builtin_tools.rs"]
mod builtin_tools;
#[path = "e2e/client.rs"]
mod client;
#[path = "e2e/client_api.rs"]
mod client_api;
#[path = "e2e/client_lifecycle.rs"]
mod client_lifecycle;
#[path = "e2e/client_options.rs"]
mod client_options;
#[path = "e2e/commands.rs"]
mod commands;
#[path = "e2e/compaction.rs"]
mod compaction;
#[path = "e2e/elicitation.rs"]
mod elicitation;
#[path = "e2e/error_resilience.rs"]
mod error_resilience;
#[path = "e2e/event_fidelity.rs"]
mod event_fidelity;
#[path = "e2e/hooks.rs"]
mod hooks;
#[path = "e2e/hooks_extended.rs"]
mod hooks_extended;
#[path = "e2e/mcp_and_agents.rs"]
mod mcp_and_agents;
#[path = "e2e/mode_handlers.rs"]
mod mode_handlers;
#[path = "e2e/multi_client.rs"]
mod multi_client;
#[path = "e2e/multi_client_commands_elicitation.rs"]
mod multi_client_commands_elicitation;
#[path = "e2e/multi_turn.rs"]
mod multi_turn;
#[path = "e2e/pending_work_resume.rs"]
mod pending_work_resume;
#[path = "e2e/per_session_auth.rs"]
mod per_session_auth;
#[path = "e2e/permissions.rs"]
mod permissions;
#[path = "e2e/rpc_additional_edge_cases.rs"]
mod rpc_additional_edge_cases;
#[path = "e2e/rpc_agent.rs"]
mod rpc_agent;
#[path = "e2e/rpc_event_side_effects.rs"]
mod rpc_event_side_effects;
#[path = "e2e/rpc_mcp_and_skills.rs"]
mod rpc_mcp_and_skills;
#[path = "e2e/rpc_mcp_config.rs"]
mod rpc_mcp_config;
#[path = "e2e/rpc_server.rs"]
mod rpc_server;
#[path = "e2e/rpc_session_state.rs"]
mod rpc_session_state;
#[path = "e2e/rpc_shell_and_fleet.rs"]
mod rpc_shell_and_fleet;
#[path = "e2e/rpc_shell_edge_cases.rs"]
mod rpc_shell_edge_cases;
#[path = "e2e/rpc_tasks_and_handlers.rs"]
mod rpc_tasks_and_handlers;
#[path = "e2e/session.rs"]
mod session;
#[path = "e2e/session_config.rs"]
mod session_config;
#[path = "e2e/session_fs.rs"]
mod session_fs;
#[path = "e2e/session_lifecycle.rs"]
mod session_lifecycle;
#[path = "e2e/skills.rs"]
mod skills;
#[path = "e2e/streaming_fidelity.rs"]
mod streaming_fidelity;
#[path = "e2e/support.rs"]
mod support;
#[path = "e2e/suspend.rs"]
mod suspend;
#[path = "e2e/system_message_transform.rs"]
mod system_message_transform;
#[path = "e2e/telemetry.rs"]
mod telemetry;
#[path = "e2e/tool_results.rs"]
mod tool_results;
#[path = "e2e/tools.rs"]
mod tools;
