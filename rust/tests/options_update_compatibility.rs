#![cfg(test)]
#![allow(clippy::unwrap_used)]

use github_copilot_sdk::Error;
use github_copilot_sdk::rpc::{
    SessionSettingsPatch, SessionUpdateOptionsParams, SessionUpdateOptionsResult,
    SessionUpdateOptionsWithSettings,
};
use github_copilot_sdk::session::Session;
use serde_json::json;

async fn _legacy_update_signature(
    session: &Session,
    params: SessionUpdateOptionsParams,
) -> Result<SessionUpdateOptionsResult, Error> {
    session.rpc().options().update(params).await
}

async fn _settings_update_signature(
    session: &Session,
    params: SessionUpdateOptionsWithSettings,
) -> Result<SessionUpdateOptionsResult, Error> {
    session.rpc().options().update_with_options(params).await
}

#[test]
fn extended_options_forward_old_values_and_session_settings() {
    let params = SessionUpdateOptionsWithSettings::new()
        .model("legacy-model")
        .settings(SessionSettingsPatch {
            memory: Some(false),
            ..Default::default()
        });
    assert_eq!(
        serde_json::to_value(params).unwrap(),
        json!({ "model": "legacy-model", "settings": { "memory": false } }),
    );
}

#[test]
#[allow(deprecated)]
fn legacy_options_update_keeps_complete_literals_and_patterns() {
    macro_rules! legacy_fields {
        ($($field:ident),* $(,)?) => {{
            let mut params = SessionUpdateOptionsParams { $($field: None,)* };
            params.model = Some("legacy-model".to_owned());
            assert_eq!(serde_json::to_value(&params).unwrap(), json!({ "model": "legacy-model" }));
            let SessionUpdateOptionsParams { $($field: _,)* } = params;
        }};
    }

    legacy_fields!(
        additional_content_exclusion_policies,
        agent_context,
        agent_directories,
        allow_all_mcp_server_instructions,
        ask_user_disabled,
        autopilot,
        available_tools,
        capi,
        client_name,
        coauthor_enabled,
        context_tier,
        continue_on_auto_mode,
        copilot_url,
        custom_agents_local_only,
        disabled_instruction_sources,
        disabled_skills,
        enable_file_hooks,
        enable_host_git_operations,
        enable_on_demand_instruction_discovery,
        enable_reasoning_summaries,
        enable_script_safety,
        enable_session_store,
        enable_skills,
        enable_streaming,
        env_value_mode,
        events_log_directory,
        events_log_includes_subagents,
        excluded_builtin_agents,
        excluded_tools,
        feature_flags,
        hook_directories,
        ignored_skills_locations,
        image_generation,
        included_builtin_agents,
        included_builtin_skills,
        installed_plugins,
        instruction_directories,
        integration_id,
        is_experimental_mode,
        log_interactive_shells,
        lsp_client_name,
        manage_schedule_enabled,
        max_inline_binary_bytes,
        model,
        model_capabilities_overrides,
        organization_custom_instructions,
        provider,
        reasoning_effort,
        reasoning_summary,
        running_in_interactive_mode,
        sandbox_config,
        sandbox_config_source,
        session_capabilities,
        session_limits,
        shell,
        shell_init_profile,
        shell_process_flags,
        skill_directories,
        skip_custom_instructions,
        skip_embedding_retrieval,
        suppress_custom_agent_prompt,
        tool_filter_precedence,
        trajectory_file,
        verbosity,
        working_directory,
    );
}
