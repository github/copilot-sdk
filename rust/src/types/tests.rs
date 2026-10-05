/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::collections::HashMap;
use std::path::PathBuf;

use serde_json::json;

use super::{
    AgentMode, Attachment, AttachmentLineRange, AttachmentSelectionPosition,
    AttachmentSelectionRange, AutoTier, AzureProviderOptions, CapiSessionOptions, ConnectionState,
    CopilotExpAssignmentResponse, CustomAgentConfig, DeliveryMode, ExpConfigEntry, ExpFlagValue,
    ExtensionInfo, GitHubMcpToolConfig, GitHubReferenceType, InfiniteSessionConfig,
    LargeToolOutputConfig, McpServerConfig, McpStdioServerConfig, MemoryConfiguration,
    NamedProviderConfig, PermissionResponseCapability, ProviderConfig, ProviderModelConfig,
    ReasoningSummary, ResumeSessionConfig, SessionConfig, SessionEvent, SessionId,
    SystemMessageConfig, Tool, ToolBinaryResult, ToolResult, ToolResultExpanded,
    ToolResultResponse, ensure_attachment_display_names,
};
use crate::generated::session_events::TypedSessionEvent;

#[test]
fn permission_response_capability_is_publicly_exported() {
    assert_eq!(
        serde_json::to_value(PermissionResponseCapability::Interactive).unwrap(),
        json!("interactive")
    );
}

#[test]
fn tool_builder_composes() {
    let tool = Tool::new("greet")
        .with_description("Say hello")
        .with_namespaced_name("hello/greet")
        .with_instructions("Pass the user's name")
        .with_parameters(json!({
            "type": "object",
            "properties": { "name": { "type": "string" } },
            "required": ["name"]
        }))
        .with_overrides_built_in_tool(true)
        .with_skip_permission(true);
    assert_eq!(tool.name, "greet");
    assert_eq!(tool.description, "Say hello");
    assert_eq!(tool.namespaced_name.as_deref(), Some("hello/greet"));
    assert_eq!(tool.instructions.as_deref(), Some("Pass the user's name"));
    assert_eq!(tool.parameters.get("type").unwrap(), &json!("object"));
    assert!(tool.overrides_built_in_tool);
    assert!(tool.skip_permission);
}

#[test]
fn tool_defer_serialization() {
    let tool = Tool::new("lookup").with_defer(super::DeferMode::Auto);
    assert_eq!(tool.defer, Some(super::DeferMode::Auto));
    let value = serde_json::to_value(&tool).unwrap();
    assert_eq!(value.get("defer").unwrap(), &json!("auto"));

    let plain = Tool::new("plain");
    let value = serde_json::to_value(&plain).unwrap();
    assert!(value.get("defer").is_none());
}

#[test]
fn tool_metadata_serialization() {
    use indexmap::IndexMap;

    let mut metadata = IndexMap::new();
    metadata.insert(
        "github.com/copilot:safeForTelemetry".to_string(),
        json!({ "name": true, "inputsNames": false }),
    );
    let tool = Tool::new("lookup").with_metadata(metadata);
    let value = serde_json::to_value(&tool).unwrap();
    assert_eq!(
        value
            .get("metadata")
            .unwrap()
            .get("github.com/copilot:safeForTelemetry")
            .unwrap(),
        &json!({ "name": true, "inputsNames": false })
    );

    // Empty metadata is omitted on the wire.
    let plain = Tool::new("plain");
    let value = serde_json::to_value(&plain).unwrap();
    assert!(value.get("metadata").is_none());
}

#[test]
fn custom_agent_config_builder_with_model() {
    let agent = CustomAgentConfig::new("my-agent", "You are helpful.")
        .with_model("claude-haiku-4.5")
        .with_display_name("My Agent");
    assert_eq!(agent.name, "my-agent");
    assert_eq!(agent.model.as_deref(), Some("claude-haiku-4.5"));
    assert_eq!(agent.display_name.as_deref(), Some("My Agent"));
}

#[test]
fn custom_agent_config_serializes_model() {
    let agent = CustomAgentConfig::new("model-agent", "prompt").with_model("claude-haiku-4.5");
    let wire = serde_json::to_value(&agent).unwrap();
    assert_eq!(wire["model"], "claude-haiku-4.5");
    assert_eq!(wire["name"], "model-agent");
}

#[test]
fn custom_agent_config_omits_model_when_none() {
    let agent = CustomAgentConfig::new("no-model-agent", "prompt");
    let wire = serde_json::to_value(&agent).unwrap();
    assert!(wire.get("model").is_none());
}

#[test]
fn custom_agent_config_builder_with_reasoning_effort() {
    let agent = CustomAgentConfig::new("reasoning-agent", "prompt").with_reasoning_effort("high");
    assert_eq!(agent.reasoning_effort.as_deref(), Some("high"));
}

#[test]
fn custom_agent_config_serializes_reasoning_effort() {
    let agent = CustomAgentConfig::new("reasoning-agent", "prompt").with_reasoning_effort("high");
    let wire = serde_json::to_value(&agent).unwrap();
    assert_eq!(wire["reasoningEffort"], "high");
}

#[test]
fn custom_agent_config_omits_reasoning_effort_when_none() {
    let agent = CustomAgentConfig::new("default-agent", "prompt");
    let wire = serde_json::to_value(&agent).unwrap();
    assert!(wire.get("reasoningEffort").is_none());
}

#[test]
#[should_panic(expected = "tool parameter schema must be a JSON object")]
fn tool_with_parameters_panics_on_non_object_value() {
    let _ = Tool::new("noop").with_parameters(json!(null));
}

#[test]
fn tool_result_expanded_serializes_binary_results_for_llm() {
    let response = ToolResultResponse {
        result: ToolResult::Expanded(ToolResultExpanded {
            text_result_for_llm: "rendered chart".to_string(),
            result_type: "success".to_string(),
            binary_results_for_llm: Some(vec![ToolBinaryResult {
                data: "aW1n".to_string(),
                mime_type: "image/png".to_string(),
                r#type: "image".to_string(),
                description: Some("chart preview".to_string()),
            }]),
            session_log: None,
            error: None,
            tool_telemetry: None,
            tool_references: None,
        }),
    };

    let wire = serde_json::to_value(&response).unwrap();

    assert_eq!(
        wire,
        json!({
            "result": {
                "textResultForLlm": "rendered chart",
                "resultType": "success",
                "binaryResultsForLlm": [
                    {
                        "data": "aW1n",
                        "mimeType": "image/png",
                        "type": "image",
                        "description": "chart preview"
                    }
                ]
            }
        })
    );
}

#[test]
fn tool_result_expanded_omits_binary_results_for_llm_when_none() {
    let response = ToolResultResponse {
        result: ToolResult::Expanded(ToolResultExpanded {
            text_result_for_llm: "ok".to_string(),
            result_type: "success".to_string(),
            binary_results_for_llm: None,
            session_log: None,
            error: None,
            tool_telemetry: None,
            tool_references: None,
        }),
    };

    let wire = serde_json::to_value(&response).unwrap();

    assert_eq!(wire["result"]["textResultForLlm"], "ok");
    assert!(wire["result"].get("binaryResultsForLlm").is_none());
}

#[test]
fn tool_result_expanded_serializes_tool_references() {
    let response = ToolResultResponse {
        result: ToolResult::Expanded(
            ToolResultExpanded::new("found 2 tools", "success")
                .with_tool_references(["get_weather", "check_status"]),
        ),
    };

    let wire = serde_json::to_value(&response).unwrap();

    assert_eq!(
        wire,
        json!({
            "result": {
                "textResultForLlm": "found 2 tools",
                "resultType": "success",
                "toolReferences": ["get_weather", "check_status"]
            }
        })
    );
}

#[test]
fn tool_result_expanded_omits_tool_references_when_none() {
    let response = ToolResultResponse {
        result: ToolResult::Expanded(ToolResultExpanded::new("ok", "success")),
    };

    let wire = serde_json::to_value(&response).unwrap();

    assert_eq!(wire["result"]["textResultForLlm"], "ok");
    assert!(wire["result"].get("toolReferences").is_none());
}

#[test]
fn tool_result_expanded_with_tool_references_accepts_owned_strings() {
    // The builder is generic over `Into<String>`, so an owned `Vec<String>`
    // must compile and populate the field just like a `&str` array.
    let names: Vec<String> = vec!["alpha".to_string(), "beta".to_string()];
    let expanded = ToolResultExpanded::new("ok", "success").with_tool_references(names);

    assert_eq!(
        expanded.tool_references.as_deref(),
        Some(["alpha".to_string(), "beta".to_string()].as_slice())
    );
}

#[test]
fn tool_result_expanded_deserializes_tool_references() {
    let wire = json!({
        "textResultForLlm": "found tools",
        "resultType": "success",
        "toolReferences": ["alpha", "beta"]
    });

    let expanded: ToolResultExpanded = serde_json::from_value(wire).unwrap();

    assert_eq!(
        expanded.tool_references.as_deref(),
        Some(["alpha".to_string(), "beta".to_string()].as_slice())
    );
}

#[test]
fn session_config_default_wire_flags_off_without_handlers() {
    let cfg = SessionConfig::default();
    assert_eq!(cfg.mcp_oauth_token_storage, None);
    assert_eq!(cfg.allowed_models, None);
    // Wire flags are derived from handler presence at create_session
    // time, not stored on the config. With no handlers installed, every
    // request_* flag should serialize as false.
    let (wire, _runtime) = cfg
        .into_wire(Some(SessionId::from("default-flags")))
        .expect("default config has no duplicate handlers");
    assert!(!wire.request_user_input);
    assert!(!wire.request_permission);
    assert!(!wire.request_elicitation);
    assert!(!wire.request_exit_plan_mode);
    assert!(!wire.request_auto_mode_switch);
    assert!(!wire.hooks);
    assert!(!wire.request_mcp_apps);
    let json = serde_json::to_value(&wire).unwrap();
    assert!(json.get("askUserVariant").is_none());
    assert!(json.get("allowedModels").is_none());
}

#[test]
fn resume_session_config_new_wire_flags_off_without_handlers() {
    let cfg = ResumeSessionConfig::new(SessionId::from("resume-flags"));
    assert_eq!(cfg.mcp_oauth_token_storage, None);
    assert_eq!(cfg.allowed_models, None);
    let (wire, _runtime) = cfg
        .into_wire()
        .expect("default resume config has no duplicate handlers");
    assert!(!wire.request_user_input);
    assert!(!wire.request_permission);
    assert!(!wire.request_elicitation);
    assert!(!wire.request_exit_plan_mode);
    assert!(!wire.request_auto_mode_switch);
    assert!(!wire.hooks);
    assert!(!wire.request_mcp_apps);
    let json = serde_json::to_value(&wire).unwrap();
    assert!(json.get("askUserVariant").is_none());
    assert!(json.get("allowedModels").is_none());
}

#[test]
fn session_configs_build_debug_and_serialize_allowed_models() {
    let create = SessionConfig::default().with_allowed_models(["gpt-5.4", "claude-sonnet-4"]);
    assert_eq!(
        create.allowed_models.as_deref(),
        Some(&["gpt-5.4".to_string(), "claude-sonnet-4".to_string()][..])
    );
    assert!(format!("{create:?}").contains("allowed_models"));

    let (create_wire, _) = create
        .into_wire(Some(SessionId::from("create-allowed-models")))
        .expect("allowed model config has no duplicate handlers");
    let create_json = serde_json::to_value(&create_wire).unwrap();
    assert_eq!(
        create_json["allowedModels"],
        json!(["gpt-5.4", "claude-sonnet-4"])
    );

    let resume = ResumeSessionConfig::new(SessionId::from("resume-allowed-models"))
        .with_allowed_models(vec!["gpt-5.4".to_string(), "gpt-5-mini".to_string()]);
    assert_eq!(
        resume.allowed_models.as_deref(),
        Some(&["gpt-5.4".to_string(), "gpt-5-mini".to_string()][..])
    );
    assert!(format!("{resume:?}").contains("allowed_models"));

    let (resume_wire, _) = resume
        .into_wire()
        .expect("resume allowed model config has no duplicate handlers");
    let resume_json = serde_json::to_value(&resume_wire).unwrap();
    assert_eq!(
        resume_json["allowedModels"],
        json!(["gpt-5.4", "gpt-5-mini"])
    );
}

#[test]
fn custom_agents_local_only_serializes_on_create_and_resume() {
    let (create_wire, _) = SessionConfig::default()
        .with_custom_agents_local_only(false)
        .into_wire(Some(SessionId::from("create-locality")))
        .expect("create config has no duplicate handlers");
    let create_json = serde_json::to_value(&create_wire).unwrap();
    assert_eq!(create_json["customAgentsLocalOnly"], false);

    let (resume_wire, _) = ResumeSessionConfig::new(SessionId::from("resume-locality"))
        .with_custom_agents_local_only(false)
        .into_wire()
        .expect("resume config has no duplicate handlers");
    let resume_json = serde_json::to_value(&resume_wire).unwrap();
    assert_eq!(resume_json["customAgentsLocalOnly"], false);

    let (unset_create_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("create-unset")))
        .expect("create config has no duplicate handlers");
    let unset_create_json = serde_json::to_value(&unset_create_wire).unwrap();
    assert!(unset_create_json.get("customAgentsLocalOnly").is_none());

    let (unset_resume_wire, _) = ResumeSessionConfig::new(SessionId::from("resume-unset"))
        .into_wire()
        .expect("resume config has no duplicate handlers");
    let unset_resume_json = serde_json::to_value(&unset_resume_wire).unwrap();
    assert!(unset_resume_json.get("customAgentsLocalOnly").is_none());
}

#[test]
fn session_config_enable_mcp_apps_sets_wire_flag_and_serializes() {
    let cfg = SessionConfig::default().with_enable_mcp_apps(true);
    assert_eq!(cfg.enable_mcp_apps, Some(true));

    let (wire, _runtime) = cfg
        .into_wire(Some(SessionId::from("enable-mcp-apps")))
        .expect("enable_mcp_apps config has no duplicate handlers");
    assert!(wire.request_mcp_apps);

    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["requestMcpApps"], serde_json::Value::Bool(true));
}

#[test]
fn resume_session_config_enable_mcp_apps_sets_wire_flag_and_serializes() {
    let cfg = ResumeSessionConfig::new(SessionId::from("resume-enable-mcp-apps"))
        .with_enable_mcp_apps(true);
    assert_eq!(cfg.enable_mcp_apps, Some(true));

    let (wire, _runtime) = cfg
        .into_wire()
        .expect("resume enable_mcp_apps config has no duplicate handlers");
    assert!(wire.request_mcp_apps);

    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["requestMcpApps"], serde_json::Value::Bool(true));
}

#[test]
fn github_mcp_tool_config_serializes_for_create_and_resume() {
    let github_config = GitHubMcpToolConfig::new()
        .with_enable_all_tools(true)
        .with_additional_toolsets(["repos"])
        .with_additional_tools(["get_issue"])
        .with_enable_insiders_mode(true)
        .with_disable_form_deferral(true);

    let (create_wire, _) = SessionConfig::default()
        .with_github_mcp_tool_config(github_config.clone())
        .into_wire(Some(SessionId::from("github-mcp")))
        .expect("create config has no duplicate handlers");
    assert_eq!(
        serde_json::to_value(&create_wire).unwrap()["githubMcpToolConfig"],
        serde_json::json!({
            "enableAllTools": true,
            "additionalToolsets": ["repos"],
            "additionalTools": ["get_issue"],
            "enableInsidersMode": true,
            "disableFormDeferral": true,
        })
    );

    let (resume_wire, _) = ResumeSessionConfig::new(SessionId::from("github-mcp"))
        .with_github_mcp_tool_config(github_config)
        .into_wire()
        .expect("resume config has no duplicate handlers");
    assert!(resume_wire.github_mcp_tool_config.is_some());

    let (unset_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("github-mcp-unset")))
        .expect("default config has no duplicate handlers");
    assert!(
        serde_json::to_value(&unset_wire)
            .unwrap()
            .get("githubMcpToolConfig")
            .is_none()
    );
}

#[test]
fn memory_configuration_constructors_and_serde() {
    assert!(MemoryConfiguration::enabled().enabled);
    assert!(!MemoryConfiguration::disabled().enabled);
    assert!(MemoryConfiguration::disabled().with_enabled(true).enabled);

    let json = serde_json::to_value(MemoryConfiguration::enabled()).unwrap();
    assert_eq!(json, serde_json::json!({ "enabled": true }));
}

#[test]
fn session_config_with_memory_serializes() {
    let (wire, _runtime) = SessionConfig::default()
        .with_memory(MemoryConfiguration::enabled())
        .into_wire(Some(SessionId::from("memory-on")))
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["memory"], serde_json::json!({ "enabled": true }));

    let (wire_off, _) = SessionConfig::default()
        .with_memory(MemoryConfiguration::disabled())
        .into_wire(Some(SessionId::from("memory-off")))
        .expect("no duplicate handlers");
    let json_off = serde_json::to_value(&wire_off).unwrap();
    assert_eq!(json_off["memory"], serde_json::json!({ "enabled": false }));

    // Unset memory is omitted on the wire.
    let (empty_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("memory-unset")))
        .expect("no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("memory").is_none());
}

#[test]
fn resume_session_config_with_memory_serializes() {
    let (wire, _runtime) = ResumeSessionConfig::new(SessionId::from("resume-memory-on"))
        .with_memory(MemoryConfiguration::enabled())
        .into_wire()
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["memory"], serde_json::json!({ "enabled": true }));

    // Unset memory is omitted on the wire.
    let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("resume-memory-unset"))
        .into_wire()
        .expect("no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("memory").is_none());
}

#[test]
fn feature_flags_serialize_on_create_and_resume() {
    let feature_flags = HashMap::from([
        ("BACKGROUND_TASK_NOTIFICATION_PAYLOADS".to_string(), true),
        ("DISABLED_TEST_FLAG".to_string(), false),
    ]);
    let expected = serde_json::json!({
        "BACKGROUND_TASK_NOTIFICATION_PAYLOADS": true,
        "DISABLED_TEST_FLAG": false,
    });

    let create_config = SessionConfig::default().with_feature_flags(feature_flags.clone());
    assert_eq!(create_config.feature_flags.as_ref(), Some(&feature_flags));
    let (create_wire, _) = create_config
        .into_wire(Some(SessionId::from("feature-flags-create")))
        .expect("no duplicate handlers");
    let create_json = serde_json::to_value(&create_wire).unwrap();
    assert_eq!(create_json["featureFlags"], expected);

    let (resume_wire, _) = ResumeSessionConfig::new(SessionId::from("feature-flags-resume"))
        .with_feature_flags(feature_flags)
        .into_wire()
        .expect("no duplicate handlers");
    let resume_json = serde_json::to_value(&resume_wire).unwrap();
    assert_eq!(resume_json["featureFlags"], expected);

    let (unset_create_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("feature-flags-create-unset")))
        .expect("no duplicate handlers");
    let unset_create_json = serde_json::to_value(&unset_create_wire).unwrap();
    assert!(unset_create_json.get("featureFlags").is_none());

    let (unset_resume_wire, _) =
        ResumeSessionConfig::new(SessionId::from("feature-flags-resume-unset"))
            .into_wire()
            .expect("no duplicate handlers");
    let unset_resume_json = serde_json::to_value(&unset_resume_wire).unwrap();
    assert!(unset_resume_json.get("featureFlags").is_none());
}

fn sample_exp_assignments(context: &str) -> CopilotExpAssignmentResponse {
    CopilotExpAssignmentResponse {
        features: vec!["copilot_exp_flag".to_string()],
        flights: HashMap::from([("copilot_exp_flag".to_string(), "treatment".to_string())]),
        configs: vec![ExpConfigEntry {
            id: "cfg-1".to_string(),
            parameters: HashMap::from([
                ("threshold".to_string(), ExpFlagValue::Integer(5)),
                ("enabled".to_string(), ExpFlagValue::Bool(true)),
            ]),
        }],
        assignment_context: context.to_string(),
        ..Default::default()
    }
}

#[test]
fn exp_flag_value_round_trips_all_variants() {
    let values = serde_json::json!({
        "s": "text",
        "i": 7,
        "f": 1.5,
        "b": true,
        "n": null,
    });
    let parsed: HashMap<String, ExpFlagValue> = serde_json::from_value(values.clone()).unwrap();
    assert_eq!(parsed["s"], ExpFlagValue::String("text".to_string()));
    assert_eq!(parsed["i"], ExpFlagValue::Integer(7));
    assert_eq!(parsed["f"], ExpFlagValue::Float(1.5));
    assert_eq!(parsed["b"], ExpFlagValue::Bool(true));
    assert_eq!(parsed["n"], ExpFlagValue::Null);
    assert_eq!(serde_json::to_value(&parsed).unwrap(), values);
}

#[test]
fn session_config_with_exp_assignments_serializes() {
    let assignments = sample_exp_assignments("ctx-123");
    let expected = serde_json::to_value(&assignments).unwrap();
    let (wire, _runtime) = SessionConfig::default()
        .with_exp_assignments(assignments)
        .into_wire(Some(SessionId::from("exp-on")))
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["expAssignments"], expected);
    assert_eq!(json["expAssignments"]["AssignmentContext"], "ctx-123");
    assert_eq!(
        json["expAssignments"]["Flights"]["copilot_exp_flag"],
        "treatment"
    );

    // Unset exp assignments are omitted on the wire.
    let (empty_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("exp-unset")))
        .expect("no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("expAssignments").is_none());
}

#[test]
fn resume_session_config_with_exp_assignments_serializes() {
    let assignments = sample_exp_assignments("ctx-456");
    let expected = serde_json::to_value(&assignments).unwrap();
    let (wire, _runtime) = ResumeSessionConfig::new(SessionId::from("resume-exp-on"))
        .with_exp_assignments(assignments)
        .into_wire()
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["expAssignments"], expected);

    // Unset exp assignments are omitted on the wire.
    let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("resume-exp-unset"))
        .into_wire()
        .expect("no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("expAssignments").is_none());
}

#[test]
fn session_config_clone_preserves_exp_assignments() {
    let assignments = sample_exp_assignments("ctx-clone");
    let config = SessionConfig::default().with_exp_assignments(assignments.clone());
    let cloned = config.clone();

    assert_eq!(cloned.exp_assignments.as_ref(), Some(&assignments));

    let (wire, _runtime) = cloned
        .into_wire(Some(SessionId::from("exp-clone")))
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(
        json["expAssignments"],
        serde_json::to_value(&assignments).unwrap()
    );
}

#[test]
fn resume_session_config_clone_preserves_exp_assignments() {
    let assignments = sample_exp_assignments("ctx-clone-resume");
    let config = ResumeSessionConfig::new(SessionId::from("resume-exp-clone"))
        .with_exp_assignments(assignments.clone());
    let cloned = config.clone();

    assert_eq!(cloned.exp_assignments.as_ref(), Some(&assignments));

    let (wire, _runtime) = cloned.into_wire().expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(
        json["expAssignments"],
        serde_json::to_value(&assignments).unwrap()
    );
}

#[test]
#[allow(clippy::field_reassign_with_default)]
fn session_config_into_wire_serializes_bucket_b_fields() {
    use std::path::PathBuf;

    use super::{CloudSessionOptions, CloudSessionRepository};

    let mut cfg = SessionConfig::default();
    cfg.config_directory = Some(PathBuf::from("/tmp/cfg"));
    cfg.working_directory = Some(PathBuf::from("/tmp/work"));
    cfg.github_token = Some("ghs_secret".to_string());
    cfg.include_sub_agent_streaming_events = Some(false);
    cfg.enable_session_telemetry = Some(false);
    cfg.reasoning_summary = Some(ReasoningSummary::Concise);
    cfg.remote_session = Some(crate::generated::api_types::RemoteSessionMode::Export);
    cfg.enable_on_demand_instruction_discovery = Some(false);
    cfg.cloud = Some(CloudSessionOptions::with_repository(
        CloudSessionRepository::new("github", "copilot-sdk").with_branch("main"),
    ));

    let (wire, _runtime) = cfg
        .into_wire(Some(SessionId::from("custom-id")))
        .expect("no duplicate handlers");
    let wire_json = serde_json::to_value(&wire).unwrap();
    assert_eq!(wire_json["sessionId"], "custom-id");
    assert_eq!(wire_json["configDir"], "/tmp/cfg");
    assert_eq!(wire_json["workingDirectory"], "/tmp/work");
    assert_eq!(wire_json["gitHubToken"], "ghs_secret");
    assert_eq!(wire_json["includeSubAgentStreamingEvents"], false);
    assert_eq!(wire_json["enableSessionTelemetry"], false);
    assert_eq!(wire_json["reasoningSummary"], "concise");
    assert_eq!(wire_json["remoteSession"], "export");
    assert_eq!(wire_json["enableOnDemandInstructionDiscovery"], false);
    assert_eq!(wire_json["cloud"]["repository"]["owner"], "github");
    assert_eq!(wire_json["cloud"]["repository"]["name"], "copilot-sdk");
    assert_eq!(wire_json["cloud"]["repository"]["branch"], "main");

    // Unset fields are omitted on the wire.
    let (empty_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("empty")))
        .expect("default has no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("gitHubToken").is_none());
    assert!(empty_json.get("enableSessionTelemetry").is_none());
    assert!(empty_json.get("reasoningSummary").is_none());
    assert!(empty_json.get("remoteSession").is_none());
    assert!(
        empty_json
            .get("enableOnDemandInstructionDiscovery")
            .is_none()
    );
    assert!(empty_json.get("cloud").is_none());
}

#[test]
fn session_config_into_wire_serializes_named_providers_and_models() {
    let cfg = SessionConfig::default()
        .with_providers(vec![
            NamedProviderConfig::new("my-openai", "https://api.example.com/v1")
                .with_provider_type("openai")
                .with_wire_api("responses")
                .with_model_provider("ollama")
                .with_api_key("sk-test"),
        ])
        .with_models(vec![
            ProviderModelConfig::new("gpt-x", "my-openai")
                .with_wire_model("gpt-x-2025")
                .with_max_output_tokens(2048),
        ]);

    let (wire, _) = cfg
        .into_wire(Some(SessionId::from("sess-providers")))
        .expect("no duplicate handlers");
    let wire_json = serde_json::to_value(&wire).unwrap();
    assert_eq!(wire_json["providers"][0]["name"], "my-openai");
    assert_eq!(
        wire_json["providers"][0]["baseUrl"],
        "https://api.example.com/v1"
    );
    assert_eq!(wire_json["providers"][0]["type"], "openai");
    assert_eq!(wire_json["providers"][0]["wireApi"], "responses");
    assert_eq!(wire_json["providers"][0]["modelProvider"], "ollama");
    assert_eq!(wire_json["providers"][0]["apiKey"], "sk-test");
    assert_eq!(wire_json["models"][0]["id"], "gpt-x");
    assert_eq!(wire_json["models"][0]["provider"], "my-openai");
    assert_eq!(wire_json["models"][0]["wireModel"], "gpt-x-2025");
    assert_eq!(wire_json["models"][0]["maxOutputTokens"], 2048);

    let (empty_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("empty")))
        .expect("default has no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("providers").is_none());
    assert!(empty_json.get("models").is_none());
}

#[test]
fn resume_config_into_wire_serializes_named_providers_and_models() {
    let cfg = ResumeSessionConfig::new(SessionId::from("sess-resume"))
        .with_providers(vec![
            NamedProviderConfig::new("my-azure", "https://example.openai.azure.com")
                .with_provider_type("azure")
                .with_azure(AzureProviderOptions {
                    api_version: Some("2024-10-21".to_string()),
                }),
        ])
        .with_models(vec![
            ProviderModelConfig::new("deploy-1", "my-azure").with_model_id("gpt-4o"),
        ]);

    let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
    let wire_json = serde_json::to_value(&wire).unwrap();
    assert_eq!(wire_json["providers"][0]["name"], "my-azure");
    assert_eq!(wire_json["providers"][0]["type"], "azure");
    assert_eq!(
        wire_json["providers"][0]["azure"]["apiVersion"],
        "2024-10-21"
    );
    assert_eq!(wire_json["models"][0]["id"], "deploy-1");
    assert_eq!(wire_json["models"][0]["provider"], "my-azure");
    assert_eq!(wire_json["models"][0]["modelId"], "gpt-4o");

    let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("empty"))
        .into_wire()
        .expect("default has no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("providers").is_none());
    assert!(empty_json.get("models").is_none());
}

#[test]
fn session_config_into_wire_serializes_plugin_directories_and_large_output() {
    use std::path::PathBuf;

    let cfg = SessionConfig {
        plugin_directories: Some(vec![PathBuf::from("/tmp/plugins")]),
        disabled_mcp_servers: Some(vec!["local-files".to_string(), "remote-github".to_string()]),
        large_output: Some(
            LargeToolOutputConfig::new()
                .with_enabled(true)
                .with_max_size_bytes(1024)
                .with_output_directory(PathBuf::from("/tmp/large-output")),
        ),
        ..Default::default()
    };

    let (wire, _) = cfg
        .into_wire(Some(SessionId::from("sess-1")))
        .expect("no duplicate handlers");
    let wire_json = serde_json::to_value(&wire).unwrap();
    assert_eq!(wire_json["pluginDirectories"][0], "/tmp/plugins");
    assert_eq!(
        wire_json["disabledMcpServers"],
        serde_json::json!(["local-files", "remote-github"])
    );
    assert_eq!(wire_json["largeOutput"]["enabled"], true);
    assert_eq!(wire_json["largeOutput"]["maxSizeBytes"], 1024);
    assert_eq!(wire_json["largeOutput"]["outputDir"], "/tmp/large-output");

    let (empty_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("empty")))
        .expect("default has no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("pluginDirectories").is_none());
    assert!(empty_json.get("disabledMcpServers").is_none());
    assert!(empty_json.get("largeOutput").is_none());
}

#[test]
fn resume_session_config_into_wire_serializes_bucket_b_fields() {
    use std::path::PathBuf;

    let mut cfg = ResumeSessionConfig::new(SessionId::from("sess-1"));
    cfg.working_directory = Some(PathBuf::from("/tmp/work"));
    cfg.config_directory = Some(PathBuf::from("/tmp/cfg"));
    cfg.github_token = Some("ghs_secret".to_string());
    cfg.include_sub_agent_streaming_events = Some(true);
    cfg.enable_session_telemetry = Some(false);
    cfg.reasoning_summary = Some(ReasoningSummary::Detailed);
    cfg.remote_session = Some(crate::generated::api_types::RemoteSessionMode::On);
    cfg.enable_on_demand_instruction_discovery = Some(false);

    let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
    let wire_json = serde_json::to_value(&wire).unwrap();
    assert_eq!(wire_json["sessionId"], "sess-1");
    assert_eq!(wire_json["workingDirectory"], "/tmp/work");
    assert_eq!(wire_json["configDir"], "/tmp/cfg");
    assert_eq!(wire_json["gitHubToken"], "ghs_secret");
    assert_eq!(wire_json["includeSubAgentStreamingEvents"], true);
    assert_eq!(wire_json["enableSessionTelemetry"], false);
    assert_eq!(wire_json["reasoningSummary"], "detailed");
    assert_eq!(wire_json["remoteSession"], "on");
    assert_eq!(wire_json["enableOnDemandInstructionDiscovery"], false);

    // Unset remote_session is omitted on the wire.
    let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .into_wire()
        .expect("default resume has no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("reasoningSummary").is_none());
    assert!(empty_json.get("remoteSession").is_none());
    assert!(
        empty_json
            .get("enableOnDemandInstructionDiscovery")
            .is_none()
    );
}

#[test]
fn resume_session_config_into_wire_serializes_plugin_directories_and_large_output() {
    use std::path::PathBuf;

    let mut cfg = ResumeSessionConfig::new(SessionId::from("sess-1"));
    cfg.plugin_directories = Some(vec![PathBuf::from("/tmp/plugins-r")]);
    cfg.disabled_mcp_servers = Some(vec!["local-files-r".to_string()]);
    cfg.large_output = Some(
        LargeToolOutputConfig::new()
            .with_enabled(false)
            .with_max_size_bytes(2048)
            .with_output_directory(PathBuf::from("/tmp/large-output-r")),
    );

    let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
    let wire_json = serde_json::to_value(&wire).unwrap();
    assert_eq!(wire_json["pluginDirectories"][0], "/tmp/plugins-r");
    assert_eq!(
        wire_json["disabledMcpServers"],
        serde_json::json!(["local-files-r"])
    );
    assert_eq!(wire_json["largeOutput"]["enabled"], false);
    assert_eq!(wire_json["largeOutput"]["maxSizeBytes"], 2048);
    assert_eq!(wire_json["largeOutput"]["outputDir"], "/tmp/large-output-r");

    let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .into_wire()
        .expect("default resume has no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("pluginDirectories").is_none());
    assert!(empty_json.get("disabledMcpServers").is_none());
    assert!(empty_json.get("largeOutput").is_none());
}

#[test]
fn auth_client_id_metadata_url_reaches_create_and_resume_wire_payloads() {
    let url = "https://example.com/oauth/client-metadata.json";

    let (create_wire, _) = SessionConfig::default()
        .with_auth_client_id_metadata_url(url)
        .into_wire(None)
        .expect("default create has no duplicate handlers");
    let create_json = serde_json::to_value(&create_wire).unwrap();
    assert_eq!(create_json["authClientIdMetadataUrl"], url);

    let (resume_wire, _) = ResumeSessionConfig::new(SessionId::from("sess-1"))
        .with_auth_client_id_metadata_url(url)
        .into_wire()
        .expect("default resume has no duplicate handlers");
    let resume_json = serde_json::to_value(&resume_wire).unwrap();
    assert_eq!(resume_json["authClientIdMetadataUrl"], url);

    let (empty_create_wire, _) = SessionConfig::default()
        .into_wire(None)
        .expect("default create has no duplicate handlers");
    let empty_create_json = serde_json::to_value(&empty_create_wire).unwrap();
    assert!(empty_create_json.get("authClientIdMetadataUrl").is_none());

    let (empty_resume_wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .into_wire()
        .expect("default resume has no duplicate handlers");
    let empty_resume_json = serde_json::to_value(&empty_resume_wire).unwrap();
    assert!(empty_resume_json.get("authClientIdMetadataUrl").is_none());
}

#[test]
fn session_config_clones_disabled_mcp_servers() {
    let create = SessionConfig::default().with_disabled_mcp_servers(["local-files"]);
    let mut create_clone = create.clone();
    create_clone
        .disabled_mcp_servers
        .as_mut()
        .expect("configured disabled MCP servers")
        .push("remote-github".to_string());
    assert_eq!(
        create.disabled_mcp_servers.as_deref(),
        Some(&["local-files".to_string()][..])
    );

    let resume = ResumeSessionConfig::new(SessionId::from("sess-1"))
        .with_disabled_mcp_servers(["local-files"]);
    let mut resume_clone = resume.clone();
    resume_clone
        .disabled_mcp_servers
        .as_mut()
        .expect("configured disabled MCP servers")
        .push("remote-github".to_string());
    assert_eq!(
        resume.disabled_mcp_servers.as_deref(),
        Some(&["local-files".to_string()][..])
    );
}

#[test]
fn session_config_builder_composes() {
    use indexmap::IndexMap;

    let cfg = SessionConfig::default()
        .with_session_id(SessionId::from("sess-1"))
        .with_model("claude-sonnet-4")
        .with_client_name("test-app")
        .with_reasoning_effort("medium")
        .with_reasoning_summary(ReasoningSummary::Concise)
        .with_context_tier("long_context")
        .with_streaming(true)
        .with_tools([Tool::new("greet")])
        .with_available_tools(["bash", "view"])
        .with_excluded_tools(["dangerous"])
        .with_mcp_servers(IndexMap::new())
        .with_mcp_oauth_token_storage("persistent")
        .with_enable_config_discovery(true)
        .with_enable_on_demand_instruction_discovery(true)
        .with_skill_directories([PathBuf::from("/tmp/skills")])
        .with_disabled_skills(["broken-skill"])
        .with_disabled_mcp_servers(["local-files"])
        .with_agent("researcher")
        .with_config_directory(PathBuf::from("/tmp/config"))
        .with_working_directory(PathBuf::from("/tmp/work"))
        .with_additional_directories([PathBuf::from("/tmp/shared")])
        .with_github_token("ghp_test")
        .with_capi(CapiSessionOptions::new().with_enable_web_socket_responses(false))
        .with_enable_session_telemetry(false)
        .with_include_sub_agent_streaming_events(false)
        .with_extension_info(ExtensionInfo::new("github-app", "counter"));

    assert_eq!(cfg.session_id.as_ref().map(|s| s.as_str()), Some("sess-1"));
    assert_eq!(cfg.model.as_deref(), Some("claude-sonnet-4"));
    assert_eq!(cfg.client_name.as_deref(), Some("test-app"));
    assert_eq!(cfg.reasoning_effort.as_deref(), Some("medium"));
    assert_eq!(cfg.reasoning_summary, Some(ReasoningSummary::Concise));
    assert_eq!(cfg.context_tier.as_deref(), Some("long_context"));
    assert_eq!(cfg.streaming, Some(true));
    assert_eq!(cfg.tools.as_ref().map(|t| t.len()), Some(1));
    assert_eq!(
        cfg.available_tools.as_deref(),
        Some(&["bash".to_string(), "view".to_string()][..])
    );
    assert_eq!(
        cfg.excluded_tools.as_deref(),
        Some(&["dangerous".to_string()][..])
    );
    assert!(cfg.mcp_servers.is_some());
    assert_eq!(cfg.mcp_oauth_token_storage.as_deref(), Some("persistent"));
    assert_eq!(cfg.enable_config_discovery, Some(true));
    assert_eq!(cfg.enable_on_demand_instruction_discovery, Some(true));
    assert_eq!(
        cfg.skill_directories.as_deref(),
        Some(&[PathBuf::from("/tmp/skills")][..])
    );
    assert_eq!(
        cfg.disabled_skills.as_deref(),
        Some(&["broken-skill".to_string()][..])
    );
    assert_eq!(
        cfg.disabled_mcp_servers.as_deref(),
        Some(&["local-files".to_string()][..])
    );
    assert_eq!(cfg.agent.as_deref(), Some("researcher"));
    assert_eq!(cfg.config_directory, Some(PathBuf::from("/tmp/config")));
    assert_eq!(cfg.working_directory, Some(PathBuf::from("/tmp/work")));
    assert_eq!(
        cfg.additional_directories.as_deref(),
        Some(&[PathBuf::from("/tmp/shared")][..])
    );
    assert_eq!(cfg.github_token.as_deref(), Some("ghp_test"));
    assert_eq!(
        cfg.capi,
        Some(CapiSessionOptions::new().with_enable_web_socket_responses(false))
    );
    assert_eq!(cfg.enable_session_telemetry, Some(false));
    assert_eq!(cfg.include_sub_agent_streaming_events, Some(false));
    assert_eq!(
        cfg.extension_info,
        Some(ExtensionInfo::new("github-app", "counter"))
    );
}

#[test]
fn resume_session_config_builder_composes() {
    use indexmap::IndexMap;

    let cfg = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .with_client_name("test-app")
        .with_reasoning_summary(ReasoningSummary::None)
        .with_context_tier("default")
        .with_streaming(true)
        .with_tools([Tool::new("greet")])
        .with_available_tools(["bash", "view"])
        .with_excluded_tools(["dangerous"])
        .with_mcp_servers(IndexMap::new())
        .with_mcp_oauth_token_storage("persistent")
        .with_enable_config_discovery(true)
        .with_enable_on_demand_instruction_discovery(false)
        .with_skill_directories([PathBuf::from("/tmp/skills")])
        .with_disabled_skills(["broken-skill"])
        .with_disabled_mcp_servers(["local-files"])
        .with_agent("researcher")
        .with_config_directory(PathBuf::from("/tmp/config"))
        .with_working_directory(PathBuf::from("/tmp/work"))
        .with_additional_directories([PathBuf::from("/tmp/shared")])
        .with_github_token("ghp_test")
        .with_capi(CapiSessionOptions::new().with_enable_web_socket_responses(false))
        .with_enable_session_telemetry(false)
        .with_include_sub_agent_streaming_events(true)
        .with_suppress_resume_event(true)
        .with_continue_pending_work(true)
        .with_extension_info(ExtensionInfo::new("github-app", "counter"));

    assert_eq!(cfg.session_id.as_str(), "sess-2");
    assert_eq!(cfg.client_name.as_deref(), Some("test-app"));
    assert_eq!(cfg.reasoning_summary, Some(ReasoningSummary::None));
    assert_eq!(cfg.context_tier.as_deref(), Some("default"));
    assert_eq!(cfg.streaming, Some(true));
    assert_eq!(cfg.tools.as_ref().map(|t| t.len()), Some(1));
    assert_eq!(
        cfg.available_tools.as_deref(),
        Some(&["bash".to_string(), "view".to_string()][..])
    );
    assert_eq!(
        cfg.excluded_tools.as_deref(),
        Some(&["dangerous".to_string()][..])
    );
    assert!(cfg.mcp_servers.is_some());
    assert_eq!(cfg.mcp_oauth_token_storage.as_deref(), Some("persistent"));
    assert_eq!(cfg.enable_config_discovery, Some(true));
    assert_eq!(cfg.enable_on_demand_instruction_discovery, Some(false));
    assert_eq!(
        cfg.skill_directories.as_deref(),
        Some(&[PathBuf::from("/tmp/skills")][..])
    );
    assert_eq!(
        cfg.disabled_skills.as_deref(),
        Some(&["broken-skill".to_string()][..])
    );
    assert_eq!(
        cfg.disabled_mcp_servers.as_deref(),
        Some(&["local-files".to_string()][..])
    );
    assert_eq!(cfg.agent.as_deref(), Some("researcher"));
    assert_eq!(cfg.config_directory, Some(PathBuf::from("/tmp/config")));
    assert_eq!(cfg.working_directory, Some(PathBuf::from("/tmp/work")));
    assert_eq!(
        cfg.additional_directories.as_deref(),
        Some(&[PathBuf::from("/tmp/shared")][..])
    );
    assert_eq!(cfg.github_token.as_deref(), Some("ghp_test"));
    assert_eq!(
        cfg.capi,
        Some(CapiSessionOptions::new().with_enable_web_socket_responses(false))
    );
    assert_eq!(cfg.enable_session_telemetry, Some(false));
    assert_eq!(cfg.include_sub_agent_streaming_events, Some(true));
    assert_eq!(cfg.suppress_resume_event, Some(true));
    assert_eq!(cfg.continue_pending_work, Some(true));
    assert_eq!(
        cfg.extension_info,
        Some(ExtensionInfo::new("github-app", "counter"))
    );
}

/// `continue_pending_work` must serialize to wire as `continuePendingWork`
/// — the runtime keys off this exact field name to opt into the
/// pending-work-handoff pattern.
#[test]
fn resume_session_config_serializes_continue_pending_work_to_camel_case() {
    let cfg = ResumeSessionConfig::new(SessionId::from("sess-1")).with_continue_pending_work(true);
    let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["continuePendingWork"], true);

    // Unset case — skip_serializing_if must omit the field.
    let (wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .into_wire()
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert!(json.get("continuePendingWork").is_none());
}

#[test]
fn resume_policy_and_recovery_report_round_trip() {
    let config =
        ResumeSessionConfig::new(SessionId::from("sess-1")).with_allow_transcript_recovery(false);
    let (wire, _) = config.into_wire().unwrap();
    let value = serde_json::to_value(&wire).unwrap();
    assert_eq!(value["allowTranscriptRecovery"], false);

    let (wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .into_wire()
        .unwrap();
    assert!(
        serde_json::to_value(&wire)
            .unwrap()
            .get("allowTranscriptRecovery")
            .is_none()
    );

    let result: crate::types::ResumeSessionResult = serde_json::from_value(serde_json::json!({
        "sessionId": "sess-1",
        "transcriptRecovery": {
            "plannedBackupPath": "events.jsonl.backup",
            "invalidLineNumbers": [2],
            "sessionStartMoved": false
        }
    }))
    .unwrap();
    let recovery = result.transcript_recovery.unwrap();
    assert_eq!(recovery.invalid_line_numbers, vec![2]);
    assert_eq!(recovery.planned_backup_path, "events.jsonl.backup");
}

#[test]
fn session_configs_serialize_additional_directories() {
    let create = SessionConfig::default().with_additional_directories([
        PathBuf::from("/tmp/shared"),
        PathBuf::from("/tmp/generated"),
    ]);
    let (create_wire, _) = create.into_wire(None).expect("no duplicate handlers");
    let create_json = serde_json::to_value(&create_wire).unwrap();
    assert_eq!(
        create_json["additionalDirectories"],
        serde_json::json!(["/tmp/shared", "/tmp/generated"])
    );

    let resume = ResumeSessionConfig::new(SessionId::from("sess-1"))
        .with_additional_directories([PathBuf::from("/tmp/resumed")]);
    let (resume_wire, _) = resume.into_wire().expect("no duplicate handlers");
    let resume_json = serde_json::to_value(&resume_wire).unwrap();
    assert_eq!(
        resume_json["additionalDirectories"],
        serde_json::json!(["/tmp/resumed"])
    );
}

/// The Rust field is `suppress_resume_event`, but the wire field stays
/// `disableResume` to preserve compatibility with the runtime and other
/// SDKs.
#[test]
fn resume_session_config_serializes_suppress_resume_event_to_disable_resume_on_wire() {
    let cfg = ResumeSessionConfig::new(SessionId::from("sess-1")).with_suppress_resume_event(true);
    let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(json["disableResume"], true);
    assert!(json.get("suppressResumeEvent").is_none());
}

/// `instruction_directories` must serialize to wire as
/// `instructionDirectories` on `SessionConfig`.
#[test]
fn session_config_serializes_instruction_directories_to_camel_case() {
    let cfg = SessionConfig::default().with_instruction_directories([PathBuf::from("/tmp/instr")]);
    let (wire, _) = cfg
        .into_wire(Some(SessionId::from("instr-on")))
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(
        json["instructionDirectories"],
        serde_json::json!(["/tmp/instr"])
    );

    // Unset case — skip_serializing_if must omit the field.
    let (wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("instr-off")))
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert!(json.get("instructionDirectories").is_none());
}

/// Same check on the resume path. Forwarded to the CLI on
/// `session.resume`.
#[test]
fn resume_session_config_serializes_instruction_directories_to_camel_case() {
    let cfg = ResumeSessionConfig::new(SessionId::from("sess-1"))
        .with_instruction_directories([PathBuf::from("/tmp/instr")]);
    let (wire, _) = cfg.into_wire().expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(
        json["instructionDirectories"],
        serde_json::json!(["/tmp/instr"])
    );

    let (wire, _) = ResumeSessionConfig::new(SessionId::from("sess-2"))
        .into_wire()
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert!(json.get("instructionDirectories").is_none());
}

#[test]
fn custom_agent_config_builder_composes() {
    use indexmap::IndexMap;

    let cfg = CustomAgentConfig::new("researcher", "You are a research assistant.")
        .with_display_name("Research Assistant")
        .with_description("Investigates technical questions.")
        .with_tools(["bash", "view"])
        .with_mcp_servers(IndexMap::new())
        .with_infer(true)
        .with_skills(["rust-coding-skill"]);

    assert_eq!(cfg.name, "researcher");
    assert_eq!(cfg.prompt, "You are a research assistant.");
    assert_eq!(cfg.display_name.as_deref(), Some("Research Assistant"));
    assert_eq!(
        cfg.description.as_deref(),
        Some("Investigates technical questions.")
    );
    assert_eq!(
        cfg.tools.as_deref(),
        Some(&["bash".to_string(), "view".to_string()][..])
    );
    assert!(cfg.mcp_servers.is_some());
    assert_eq!(cfg.infer, Some(true));
    assert_eq!(
        cfg.skills.as_deref(),
        Some(&["rust-coding-skill".to_string()][..])
    );
}

#[test]
fn mcp_servers_serialize_in_insertion_order() {
    use indexmap::IndexMap;

    // Regression: `mcp_servers` was a `HashMap`, so the server keys (and
    // thus the `session.create` payload) serialized in a per-process
    // random order; `IndexMap` pins them to insertion order. The long
    // sequence makes a `HashMap` regression reproduce this exact order by
    // chance only 1/N!, avoiding a flaky false pass.
    let order = [
        "zebra", "quartz", "delta", "ivy", "mango", "bravo", "xenon", "amber", "falcon", "ceres",
        "nova", "kelp", "otter", "yodel", "plum", "garnet",
    ];
    let mut servers = IndexMap::new();
    for name in order {
        servers.insert(
            name.to_string(),
            McpServerConfig::Stdio(McpStdioServerConfig {
                command: "run".to_string(),
                ..Default::default()
            }),
        );
    }

    let (wire, _runtime) = SessionConfig::default()
        .with_mcp_servers(servers)
        .into_wire(None)
        .expect("into_wire should succeed");
    let json = serde_json::to_string(&wire).expect("serialize wire");

    let positions: Vec<usize> = order
        .iter()
        .map(|name| {
            json.find(&format!("\"{name}\""))
                .unwrap_or_else(|| panic!("server {name} missing from wire JSON"))
        })
        .collect();
    let mut ascending = positions.clone();
    ascending.sort_unstable();
    assert_eq!(
        positions, ascending,
        "mcp server keys must serialize in insertion order: {json}"
    );
}

#[test]
fn infinite_session_config_builder_composes() {
    let cfg = InfiniteSessionConfig::new()
        .with_enabled(true)
        .with_background_compaction_threshold(0.75)
        .with_buffer_exhaustion_threshold(0.92);

    assert_eq!(cfg.enabled, Some(true));
    assert_eq!(cfg.background_compaction_threshold, Some(0.75));
    assert_eq!(cfg.buffer_exhaustion_threshold, Some(0.92));
}

#[test]
fn provider_config_builder_composes() {
    use std::collections::HashMap;

    let mut headers = HashMap::new();
    headers.insert("X-Custom".to_string(), "value".to_string());

    let cfg = ProviderConfig::new("https://api.example.com")
        .with_provider_type("openai")
        .with_wire_api("completions")
        .with_transport("websockets")
        .with_model_provider("lm_studio")
        .with_api_key("sk-test")
        .with_bearer_token("bearer-test")
        .with_headers(headers)
        .with_model_id("gpt-4")
        .with_wire_model("azure-gpt-4-deployment")
        .with_max_prompt_tokens(8192)
        .with_max_output_tokens(2048);

    assert_eq!(cfg.base_url, "https://api.example.com");
    assert_eq!(cfg.provider_type.as_deref(), Some("openai"));
    assert_eq!(cfg.wire_api.as_deref(), Some("completions"));
    assert_eq!(cfg.transport.as_deref(), Some("websockets"));
    assert_eq!(cfg.model_provider.as_deref(), Some("lm_studio"));
    assert_eq!(cfg.api_key.as_deref(), Some("sk-test"));
    assert_eq!(cfg.bearer_token.as_deref(), Some("bearer-test"));
    assert_eq!(
        cfg.headers
            .as_ref()
            .and_then(|h| h.get("X-Custom"))
            .map(String::as_str),
        Some("value"),
    );
    assert_eq!(cfg.model_id.as_deref(), Some("gpt-4"));
    assert_eq!(cfg.wire_model.as_deref(), Some("azure-gpt-4-deployment"));
    assert_eq!(cfg.max_prompt_tokens, Some(8192));
    assert_eq!(cfg.max_output_tokens, Some(2048));

    // Wire-shape: camelCase, skip_serializing_if when unset.
    let wire = serde_json::to_value(&cfg).unwrap();
    assert_eq!(wire["modelId"], "gpt-4");
    assert_eq!(wire["wireModel"], "azure-gpt-4-deployment");
    assert_eq!(wire["modelProvider"], "lm_studio");
    assert_eq!(wire["maxPromptTokens"], 8192);
    assert_eq!(wire["maxOutputTokens"], 2048);

    let unset = ProviderConfig::new("https://api.example.com");
    let wire_unset = serde_json::to_value(&unset).unwrap();
    assert!(wire_unset.get("modelId").is_none());
    assert!(wire_unset.get("wireModel").is_none());
    assert!(wire_unset.get("modelProvider").is_none());
    assert!(wire_unset.get("maxPromptTokens").is_none());
    assert!(wire_unset.get("maxOutputTokens").is_none());
}

#[test]
fn capi_session_options_builder_composes_and_serializes() {
    let cfg = CapiSessionOptions::new().with_enable_web_socket_responses(false);

    assert_eq!(cfg.enable_web_socket_responses, Some(false));

    let wire = serde_json::to_value(&cfg).unwrap();
    assert_eq!(
        wire,
        serde_json::json!({ "enableWebSocketResponses": false })
    );

    let unset = CapiSessionOptions::new();
    let wire_unset = serde_json::to_value(&unset).unwrap();
    assert!(wire_unset.get("enableWebSocketResponses").is_none());
    assert!(wire_unset.get("autoTier").is_none());
    assert_eq!(wire_unset, json!({}));
}

#[test]
fn capi_auto_tier_canonical_values_round_trip_and_forward() {
    for (tier, value) in [
        (AutoTier::Efficiency, "efficiency"),
        (AutoTier::Balance, "balance"),
        (AutoTier::Intelligence, "intelligence"),
        (AutoTier::Fast, "fast"),
    ] {
        let exported: crate::AutoTier = tier.clone();
        let capi = CapiSessionOptions::new().with_auto_tier(exported);
        assert_eq!(capi.auto_tier, Some(tier));
        assert_eq!(
            serde_json::to_value(&capi).unwrap(),
            json!({"autoTier": value})
        );
        assert_eq!(
            serde_json::from_value::<CapiSessionOptions>(json!({"autoTier": value})).unwrap(),
            capi
        );

        let capi = capi.with_enable_web_socket_responses(false);
        let expected = json!({"autoTier": value, "enableWebSocketResponses": false});
        let (create, _) = SessionConfig::default()
            .with_model("auto")
            .with_capi(capi.clone())
            .into_wire(Some(SessionId::from("capi-create")))
            .unwrap();
        assert_eq!(serde_json::to_value(create).unwrap()["capi"], expected);

        let (resume, _) = ResumeSessionConfig::new(SessionId::from("capi-resume"))
            .with_capi(capi)
            .into_wire()
            .unwrap();
        assert_eq!(serde_json::to_value(resume).unwrap()["capi"], expected);
    }
}

#[test]
fn capi_auto_tier_accepts_unknown_values_for_forward_compatibility() {
    for value in ["balanced", "Balance", "unknown"] {
        assert_eq!(
            serde_json::from_value::<AutoTier>(json!(value)).unwrap(),
            AutoTier::Unknown
        );
    }
    let capi: CapiSessionOptions = serde_json::from_value(json!({})).unwrap();
    assert_eq!(capi.auto_tier, None);
}

#[test]
fn session_config_with_capi_serializes() {
    let (wire, _) = SessionConfig::default()
        .with_capi(CapiSessionOptions::new().with_enable_web_socket_responses(false))
        .into_wire(Some(SessionId::from("capi-create")))
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(
        json["capi"],
        serde_json::json!({ "enableWebSocketResponses": false })
    );

    let (empty_wire, _) = SessionConfig::default()
        .into_wire(Some(SessionId::from("capi-create-unset")))
        .expect("no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("capi").is_none());
}

#[test]
fn resume_session_config_with_capi_serializes() {
    let (wire, _) = ResumeSessionConfig::new(SessionId::from("capi-resume"))
        .with_capi(CapiSessionOptions::new().with_enable_web_socket_responses(false))
        .into_wire()
        .expect("no duplicate handlers");
    let json = serde_json::to_value(&wire).unwrap();
    assert_eq!(
        json["capi"],
        serde_json::json!({ "enableWebSocketResponses": false })
    );

    let (empty_wire, _) = ResumeSessionConfig::new(SessionId::from("capi-resume-unset"))
        .into_wire()
        .expect("no duplicate handlers");
    let empty_json = serde_json::to_value(&empty_wire).unwrap();
    assert!(empty_json.get("capi").is_none());
}

#[test]
fn system_message_config_builder_composes() {
    use std::collections::HashMap;

    let cfg = SystemMessageConfig::new()
        .with_mode("replace")
        .with_content("Custom system message.")
        .with_sections(HashMap::new());

    assert_eq!(cfg.mode.as_deref(), Some("replace"));
    assert_eq!(cfg.content.as_deref(), Some("Custom system message."));
    assert!(cfg.sections.is_some());
}

#[test]
fn delivery_mode_serializes_to_kebab_case_strings() {
    assert_eq!(
        serde_json::to_string(&DeliveryMode::Enqueue).unwrap(),
        "\"enqueue\""
    );
    assert_eq!(
        serde_json::to_string(&DeliveryMode::Immediate).unwrap(),
        "\"immediate\""
    );
    let parsed: DeliveryMode = serde_json::from_str("\"immediate\"").unwrap();
    assert_eq!(parsed, DeliveryMode::Immediate);
}

#[test]
fn agent_mode_serializes_to_kebab_case_strings() {
    assert_eq!(
        serde_json::to_string(&AgentMode::Interactive).unwrap(),
        "\"interactive\""
    );
    assert_eq!(serde_json::to_string(&AgentMode::Plan).unwrap(), "\"plan\"");
    assert_eq!(
        serde_json::to_string(&AgentMode::Autopilot).unwrap(),
        "\"autopilot\""
    );
    assert_eq!(
        serde_json::to_string(&AgentMode::Shell).unwrap(),
        "\"shell\""
    );
    let parsed: AgentMode = serde_json::from_str("\"plan\"").unwrap();
    assert_eq!(parsed, AgentMode::Plan);
}

#[test]
fn connection_state_distinguishes_variants() {
    // ConnectionState is now an internal type; verify we can construct
    // and compare the variants used by the lifecycle code paths.
    assert_ne!(ConnectionState::Connected, ConnectionState::Disconnected);
}

/// `agentId` is the sub-agent attribution field added in copilot-sdk
/// commit f8cf846 ("Derive session event envelopes from schema").
/// Every other SDK (Node, Python, Go, .NET) carries it on the event
/// envelope; Rust must too or sub-agent events lose attribution at
/// the deserialization boundary. Cross-SDK parity test.
#[test]
fn session_event_round_trips_agent_id_on_envelope() {
    let wire = json!({
        "id": "evt-1",
        "timestamp": "2026-04-30T12:00:00Z",
        "parentId": null,
        "agentId": "sub-agent-42",
        "type": "assistant.message",
        "data": { "message": "hi" }
    });

    let event: SessionEvent = serde_json::from_value(wire.clone()).unwrap();
    assert_eq!(event.agent_id.as_deref(), Some("sub-agent-42"));

    // Round-trip preserves the field on the wire.
    let roundtripped = serde_json::to_value(&event).unwrap();
    assert_eq!(roundtripped["agentId"], "sub-agent-42");

    // Absent agentId remains absent (skip_serializing_if).
    let main_agent_event: SessionEvent = serde_json::from_value(json!({
        "id": "evt-2",
        "timestamp": "2026-04-30T12:00:01Z",
        "parentId": null,
        "type": "session.idle",
        "data": {}
    }))
    .unwrap();
    assert!(main_agent_event.agent_id.is_none());
    let roundtripped = serde_json::to_value(&main_agent_event).unwrap();
    assert!(roundtripped.get("agentId").is_none());
}

/// Same parity for the typed event envelope produced by the codegen.
#[test]
fn typed_session_event_round_trips_agent_id_on_envelope() {
    let wire = json!({
        "id": "evt-1",
        "timestamp": "2026-04-30T12:00:00Z",
        "parentId": null,
        "agentId": "sub-agent-42",
        "type": "session.idle",
        "data": {}
    });

    let event: TypedSessionEvent = serde_json::from_value(wire).unwrap();
    assert_eq!(event.agent_id.as_deref(), Some("sub-agent-42"));

    let roundtripped = serde_json::to_value(&event).unwrap();
    assert_eq!(roundtripped["agentId"], "sub-agent-42");
}

#[test]
fn connection_state_variants_compile() {
    // Defensive smoke test: all variants must be constructable from
    // within the crate. (The enum was demoted from pub to pub(crate)
    // in Phase D; this test guards against accidental removal.)
    let _ = ConnectionState::Disconnected;
    let _ = ConnectionState::Connecting;
    let _ = ConnectionState::Connected;
    let _ = ConnectionState::Error;
}

#[test]
fn deserializes_runtime_attachment_variants() {
    let attachments: Vec<Attachment> = serde_json::from_value(json!([
        {
            "type": "file",
            "path": "/tmp/file.rs",
            "displayName": "file.rs",
            "lineRange": { "start": 7, "end": 12 }
        },
        {
            "type": "directory",
            "path": "/tmp/project",
            "displayName": "project"
        },
        {
            "type": "selection",
            "filePath": "/tmp/lib.rs",
            "displayName": "lib.rs",
            "text": "fn main() {}",
            "selection": {
                "start": { "line": 1, "character": 2 },
                "end": { "line": 3, "character": 4 }
            }
        },
        {
            "type": "blob",
            "data": "Zm9v",
            "mimeType": "image/png",
            "displayName": "image.png"
        },
        {
            "type": "github_reference",
            "number": 42,
            "title": "Fix rendering",
            "referenceType": "issue",
            "state": "open",
            "url": "https://github.com/example/repo/issues/42"
        },
        {
            "type": "extension_context",
            "capturedAt": "2026-09-18T11:00:00Z",
            "extensionId": "example:extension",
            "title": "Unbound context"
        }
    ]))
    .expect("attachments should deserialize");

    assert_eq!(attachments.len(), 6);
    assert!(matches!(
        &attachments[0],
        Attachment::File {
            path,
            display_name,
            line_range: Some(AttachmentLineRange { start: 7, end: 12 }),
        } if path == &PathBuf::from("/tmp/file.rs") && display_name.as_deref() == Some("file.rs")
    ));
    assert!(matches!(
        &attachments[1],
        Attachment::Directory { path, display_name }
            if path == &PathBuf::from("/tmp/project") && display_name.as_deref() == Some("project")
    ));
    assert!(matches!(
        &attachments[2],
        Attachment::Selection {
            file_path,
            display_name,
            selection:
                AttachmentSelectionRange {
                    start: AttachmentSelectionPosition { line: 1, character: 2 },
                    end: AttachmentSelectionPosition { line: 3, character: 4 },
                },
            ..
        } if file_path == &PathBuf::from("/tmp/lib.rs") && display_name.as_deref() == Some("lib.rs")
    ));
    assert!(matches!(
        &attachments[3],
        Attachment::Blob {
            data,
            mime_type,
            display_name,
        } if data == "Zm9v" && mime_type == "image/png" && display_name.as_deref() == Some("image.png")
    ));
    assert!(matches!(
        &attachments[4],
        Attachment::GitHubReference {
            number: 42,
            title,
            reference_type: GitHubReferenceType::Issue,
            state,
            url,
        } if title == "Fix rendering"
            && state == "open"
            && url == "https://github.com/example/repo/issues/42"
    ));
    assert!(matches!(
        &attachments[5],
        Attachment::ExtensionContext {
            captured_at,
            extension_id,
            canvas_id: None,
            instance_id: None,
            title,
            payload: None,
        } if captured_at == "2026-09-18T11:00:00Z"
            && extension_id == "example:extension"
            && title == "Unbound context"
    ));
    assert_eq!(
        serde_json::to_value(&attachments[5]).expect("serialize extension context"),
        json!({
            "type": "extension_context",
            "capturedAt": "2026-09-18T11:00:00Z",
            "extensionId": "example:extension",
            "title": "Unbound context"
        })
    );
}

#[test]
fn ensures_display_names_for_variants_that_support_them() {
    let mut attachments = vec![
        Attachment::File {
            path: PathBuf::from("/tmp/file.rs"),
            display_name: None,
            line_range: None,
        },
        Attachment::Selection {
            file_path: PathBuf::from("/tmp/src/lib.rs"),
            display_name: None,
            text: "fn main() {}".to_string(),
            selection: AttachmentSelectionRange {
                start: AttachmentSelectionPosition {
                    line: 0,
                    character: 0,
                },
                end: AttachmentSelectionPosition {
                    line: 0,
                    character: 10,
                },
            },
        },
        Attachment::Blob {
            data: "Zm9v".to_string(),
            mime_type: "image/png".to_string(),
            display_name: None,
        },
        Attachment::GitHubReference {
            number: 7,
            title: "Track regressions".to_string(),
            reference_type: GitHubReferenceType::Issue,
            state: "open".to_string(),
            url: "https://example.com/issues/7".to_string(),
        },
    ];

    ensure_attachment_display_names(&mut attachments);

    assert_eq!(attachments[0].display_name(), Some("file.rs"));
    assert_eq!(attachments[1].display_name(), Some("lib.rs"));
    assert_eq!(attachments[2].display_name(), Some("attachment"));
    assert_eq!(attachments[3].display_name(), None);
    assert_eq!(
        attachments[3].label(),
        Some("Track regressions".to_string())
    );
}

#[test]
fn github_anchored_attachment_variants_round_trip() {
    let cases = vec![
        (
            "github_commit",
            json!({
                "type": "github_commit",
                "message": "Fix the thing",
                "oid": "abc123",
                "repo": { "id": 1, "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/commit/abc123"
            }),
        ),
        (
            "github_release",
            json!({
                "type": "github_release",
                "name": "v1.2.3",
                "repo": { "name": "repo", "owner": "octocat" },
                "tagName": "v1.2.3",
                "url": "https://github.com/octocat/repo/releases/tag/v1.2.3"
            }),
        ),
        (
            "github_actions_job",
            json!({
                "type": "github_actions_job",
                "conclusion": "failure",
                "jobId": 99,
                "jobName": "build",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/actions/runs/1/job/99",
                "workflowName": "CI"
            }),
        ),
        (
            "github_repository",
            json!({
                "type": "github_repository",
                "description": "An example repository",
                "ref": "main",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo"
            }),
        ),
        (
            "github_file_diff",
            json!({
                "type": "github_file_diff",
                "base": {
                    "path": "src/lib.rs",
                    "ref": "main",
                    "repo": { "name": "repo", "owner": "octocat" }
                },
                "head": {
                    "path": "src/lib.rs",
                    "ref": "feature",
                    "repo": { "name": "repo", "owner": "octocat" }
                },
                "url": "https://github.com/octocat/repo/compare/main...feature"
            }),
        ),
        (
            "github_tree_comparison",
            json!({
                "type": "github_tree_comparison",
                "base": {
                    "repo": { "name": "repo", "owner": "octocat" },
                    "revision": "main"
                },
                "head": {
                    "repo": { "name": "repo", "owner": "octocat" },
                    "revision": "feature"
                },
                "url": "https://github.com/octocat/repo/compare/main...feature"
            }),
        ),
        (
            "github_url",
            json!({
                "type": "github_url",
                "url": "https://github.com/octocat/repo/wiki"
            }),
        ),
        (
            "github_file",
            json!({
                "type": "github_file",
                "path": "src/main.rs",
                "ref": "main",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/blob/main/src/main.rs"
            }),
        ),
        (
            "github_snippet",
            json!({
                "type": "github_snippet",
                "lineRange": { "start": 10, "end": 20 },
                "path": "src/main.rs",
                "ref": "main",
                "repo": { "name": "repo", "owner": "octocat" },
                "url": "https://github.com/octocat/repo/blob/main/src/main.rs#L10-L20"
            }),
        ),
    ];

    for (expected_type, input) in cases {
        let attachment: Attachment = serde_json::from_value(input.clone())
            .unwrap_or_else(|err| panic!("{expected_type} should deserialize: {err}"));

        // Serialize to a string first: parsing into `serde_json::Value` would
        // silently dedupe a duplicate `type` key, hiding the exact regression
        // this test guards against (e.g. a wrapped generated struct emitting its
        // own `type` alongside the enum tag).
        let serialized_string = serde_json::to_string(&attachment)
            .unwrap_or_else(|err| panic!("{expected_type} should serialize: {err}"));

        // Exactly one `type` key, carrying the expected discriminator.
        assert_eq!(
            serialized_string.matches("\"type\":").count(),
            1,
            "{expected_type} must serialize a single `type` key"
        );

        let serialized: serde_json::Value = serde_json::from_str(&serialized_string)
            .unwrap_or_else(|err| panic!("{expected_type} should reparse: {err}"));
        assert_eq!(
            serialized.get("type").and_then(|value| value.as_str()),
            Some(expected_type),
            "{expected_type} must serialize the correct discriminator"
        );

        // Round-trips without dropping fields.
        assert_eq!(
            serialized, input,
            "{expected_type} should round-trip without data loss"
        );
        let reparsed: Attachment = serde_json::from_value(serialized)
            .unwrap_or_else(|err| panic!("{expected_type} should re-deserialize: {err}"));
        assert_eq!(
            reparsed, attachment,
            "{expected_type} should re-deserialize to the same value"
        );
    }
}
