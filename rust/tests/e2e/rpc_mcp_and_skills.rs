use std::collections::HashMap;
use std::path::Path;

use github_copilot_sdk::generated::api_types::{
    ExtensionsDisableRequest, ExtensionsEnableRequest, McpAppsCallToolRequest,
    McpAppsDiagnoseRequest, McpAppsListToolsRequest, McpAppsReadResourceRequest,
    McpAppsSetHostContextDetails, McpAppsSetHostContextDetailsAvailableDisplayMode,
    McpAppsSetHostContextDetailsDisplayMode, McpAppsSetHostContextDetailsPlatform,
    McpAppsSetHostContextDetailsTheme, McpAppsSetHostContextRequest,
    McpCancelSamplingExecutionParams, McpDisableRequest, McpEnableRequest,
    McpExecuteSamplingParams, McpExecuteSamplingRequest, McpOauthLoginRequest,
    McpSamplingExecutionAction, McpSetEnvValueModeDetails, McpSetEnvValueModeParams,
    SkillsDisableRequest, SkillsEnableRequest,
};
use github_copilot_sdk::{McpServerConfig, McpStdioServerConfig};

use super::support::with_e2e_context;

#[tokio::test]
async fn should_list_and_toggle_session_skills() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_list_and_toggle_session_skills",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skill_name = "session-rpc-skill-rust";
                let skills_dir = create_skill_directory(
                    ctx.work_dir(),
                    skill_name,
                    "Session skill controlled by RPC.",
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir])
                            .with_disabled_skills([skill_name]),
                    )
                    .await
                    .expect("create session");

                assert_skill(
                    session.rpc().skills().list().await.expect("list disabled"),
                    skill_name,
                    false,
                );
                session
                    .rpc()
                    .skills()
                    .enable(SkillsEnableRequest {
                        name: skill_name.to_string(),
                    })
                    .await
                    .expect("enable skill");
                assert_skill(
                    session.rpc().skills().list().await.expect("list enabled"),
                    skill_name,
                    true,
                );
                session
                    .rpc()
                    .skills()
                    .disable(SkillsDisableRequest {
                        name: skill_name.to_string(),
                    })
                    .await
                    .expect("disable skill");
                assert_skill(
                    session
                        .rpc()
                        .skills()
                        .list()
                        .await
                        .expect("list disabled again"),
                    skill_name,
                    false,
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_ensure_skills_are_loaded_and_list_invoked_skills() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_ensure_skills_are_loaded_and_list_invoked_skills",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skill_name = "ensure-loaded-rpc-skill-rust";
                let skills_dir = create_skill_directory(
                    ctx.work_dir(),
                    skill_name,
                    "Skill available to ensureLoaded tests.",
                );
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir]),
                    )
                    .await
                    .expect("create session");

                session
                    .rpc()
                    .skills()
                    .ensure_loaded()
                    .await
                    .expect("ensure loaded");
                assert_skill(
                    session.rpc().skills().list().await.expect("list skills"),
                    skill_name,
                    true,
                );
                let invoked = session
                    .rpc()
                    .skills()
                    .get_invoked()
                    .await
                    .expect("get invoked skills");
                assert!(invoked.skills.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reload_session_skills() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_reload_session_skills",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let skills_dir = ctx.work_dir().join("reloadable-rpc-skills");
                std::fs::create_dir_all(&skills_dir).expect("create skills dir");
                let skill_name = "reload-rpc-skill-rust";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_skill_directories([skills_dir.clone()]),
                    )
                    .await
                    .expect("create session");

                let before = session.rpc().skills().list().await.expect("list before");
                assert!(!before.skills.iter().any(|skill| skill.name == skill_name));

                create_skill(
                    &skills_dir,
                    skill_name,
                    "Skill added after session creation.",
                );
                session
                    .rpc()
                    .skills()
                    .reload()
                    .await
                    .expect("reload skills");
                let after = session.rpc().skills().list().await.expect("list after");
                let skill = assert_skill(after, skill_name, true);
                assert_eq!(skill.description, "Skill added after session creation.");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_list_mcp_servers_with_configured_server() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_list_mcp_servers_with_configured_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "rpc-list-mcp-server";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers(ctx.repo_root(), server_name)),
                    )
                    .await
                    .expect("create session");

                let result = session.rpc().mcp().list().await.expect("mcp list");
                assert!(
                    result
                        .servers
                        .iter()
                        .any(|server| server.name == server_name)
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_set_mcp_env_value_mode_and_remove_github_server() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_set_mcp_env_value_mode_and_remove_github_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let mode = session
                    .rpc()
                    .mcp()
                    .set_env_value_mode(McpSetEnvValueModeParams {
                        mode: McpSetEnvValueModeDetails::Direct,
                    })
                    .await
                    .expect("set env value mode");
                assert_eq!(mode.mode, McpSetEnvValueModeDetails::Direct);
                let removed = session
                    .rpc()
                    .mcp()
                    .remove_git_hub()
                    .await
                    .expect("remove github mcp");
                assert!(!removed.removed);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_mcp_sampling_failure_and_cancel_missing_sampling() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_mcp_sampling_failure_and_cancel_missing_sampling",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert!(
                    !session
                        .rpc()
                        .mcp()
                        .cancel_sampling_execution(McpCancelSamplingExecutionParams {
                            request_id: "missing-sampling".into(),
                        })
                        .await
                        .expect("cancel missing sampling")
                        .cancelled
                );
                match session
                    .rpc()
                    .mcp()
                    .execute_sampling(McpExecuteSamplingParams {
                        mcp_request_id: serde_json::json!("sampling-request"),
                        request: McpExecuteSamplingRequest {},
                        request_id: "sampling-request".into(),
                        server_name: "missing-server".to_string(),
                    })
                    .await
                {
                    Ok(result) => {
                        assert_ne!(result.action, McpSamplingExecutionAction::Success);
                        assert!(result.result.is_none());
                    }
                    Err(err) => {
                        assert!(
                            !err.to_string()
                                .contains("Unhandled method session.mcp.executeSampling")
                        );
                    }
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_list_plugins() {
    with_e2e_context("rpc_mcp_and_skills", "should_list_plugins", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let result = session.rpc().plugins().list().await.expect("plugins list");
            assert!(
                result.plugins.iter().all(|plugin| !plugin.name.is_empty()),
                "plugins should have names: {:?}",
                result.plugins
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_list_extensions() {
    with_e2e_context("rpc_mcp_and_skills", "should_list_extensions", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client =
                github_copilot_sdk::Client::start(ctx.client_options().with_extra_args(["--yolo"]))
                    .await
                    .expect("start yolo client");
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let result = session
                .rpc()
                .extensions()
                .list()
                .await
                .expect("extensions list");
            assert!(
                result
                    .extensions
                    .iter()
                    .all(|extension| !extension.id.is_empty() && !extension.name.is_empty()),
                "extensions should have ids and names: {:?}",
                result.extensions
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_round_trip_mcp_app_host_context() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_round_trip_mcp_app_host_context",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .rpc()
                    .mcp()
                    .apps()
                    .set_host_context(McpAppsSetHostContextRequest {
                        context: McpAppsSetHostContextDetails {
                            available_display_modes: Some(vec![
                                McpAppsSetHostContextDetailsAvailableDisplayMode::Inline,
                                McpAppsSetHostContextDetailsAvailableDisplayMode::Fullscreen,
                            ]),
                            display_mode: Some(McpAppsSetHostContextDetailsDisplayMode::Inline),
                            locale: Some("en-US".to_string()),
                            platform: Some(McpAppsSetHostContextDetailsPlatform::Desktop),
                            theme: Some(McpAppsSetHostContextDetailsTheme::Dark),
                            time_zone: Some("Etc/UTC".to_string()),
                            user_agent: Some("rust-e2e".to_string()),
                        },
                    })
                    .await
                    .expect("set host context");
                let context = session
                    .rpc()
                    .mcp()
                    .apps()
                    .get_host_context()
                    .await
                    .expect("get host context")
                    .context;
                assert_eq!(context.locale.as_deref(), Some("en-US"));
                assert_eq!(context.time_zone.as_deref(), Some("Etc/UTC"));
                assert_eq!(context.user_agent.as_deref(), Some("rust-e2e"));
                assert_eq!(
                    context
                        .available_display_modes
                        .as_ref()
                        .map_or(0, Vec::len),
                    2
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_diagnose_and_report_mcp_app_capability_errors() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_diagnose_and_report_mcp_app_capability_errors",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "missing-app-server";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let diagnose = session
                    .rpc()
                    .mcp()
                    .apps()
                    .diagnose(McpAppsDiagnoseRequest {
                        server_name: server_name.to_string(),
                    })
                    .await
                    .expect("diagnose mcp apps");
                assert!(!diagnose.server.connected);
                assert_eq!(diagnose.server.tool_count, 0.0);
                assert!(diagnose.server.sample_tool_names.is_empty());
                let _capability = diagnose.capability;

                expect_err_contains(
                    session
                        .rpc()
                        .mcp()
                        .apps()
                        .list_tools(McpAppsListToolsRequest {
                            server_name: server_name.to_string(),
                            origin_server_name: server_name.to_string(),
                        }),
                    "mcp",
                )
                .await;
                expect_err_contains(
                    session
                        .rpc()
                        .mcp()
                        .apps()
                        .call_tool(McpAppsCallToolRequest {
                            arguments: Some(HashMap::new()),
                            server_name: server_name.to_string(),
                            origin_server_name: server_name.to_string(),
                            tool_name: "missing-tool".to_string(),
                        }),
                    "mcp",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_app_resource_is_not_available() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_app_resource_is_not_available",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let err = session
                    .rpc()
                    .mcp()
                    .apps()
                    .read_resource(McpAppsReadResourceRequest {
                        server_name: "missing-app-server".to_string(),
                        uri: "ui://missing/resource.html".to_string(),
                    })
                    .await
                    .expect_err("missing resource should fail");
                let message = err.to_string().to_ascii_lowercase();
                assert!(
                    message.contains("resource")
                        || message.contains("not found")
                        || message.contains("method not found")
                        || message.contains("mcp"),
                    "unexpected readResource error: {err}"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_host_is_not_initialized() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_host_is_not_initialized",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().mcp().enable(McpEnableRequest {
                        server_name: "missing-server".to_string(),
                    }),
                    "No MCP host initialized",
                )
                .await;
                expect_err_contains(
                    session.rpc().mcp().disable(McpDisableRequest {
                        server_name: "missing-server".to_string(),
                    }),
                    "No MCP host initialized",
                )
                .await;
                expect_err_contains(
                    session.rpc().mcp().reload(),
                    "MCP config reload not available",
                )
                .await;
                expect_err_contains(
                    session.rpc().mcp().oauth().login(McpOauthLoginRequest {
                        server_name: "missing-server".to_string(),
                        callback_success_message: None,
                        client_name: None,
                        force_reauth: None,
                    }),
                    "MCP host is not available",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_oauth_server_is_not_configured() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_oauth_server_is_not_configured",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_mcp_servers(
                        test_mcp_servers(ctx.repo_root(), "configured-stdio-server"),
                    ))
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().mcp().oauth().login(McpOauthLoginRequest {
                        server_name: "missing-server".to_string(),
                        callback_success_message: None,
                        client_name: None,
                        force_reauth: None,
                    }),
                    "is not configured",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_mcp_oauth_server_is_not_remote() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_mcp_oauth_server_is_not_remote",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "configured-stdio-server";
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(test_mcp_servers(ctx.repo_root(), server_name)),
                    )
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().mcp().oauth().login(McpOauthLoginRequest {
                        server_name: server_name.to_string(),
                        callback_success_message: Some("Done".to_string()),
                        client_name: Some("SDK E2E".to_string()),
                        force_reauth: Some(true),
                    }),
                    "not a remote server",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_extensions_are_not_available() {
    with_e2e_context(
        "rpc_mcp_and_skills",
        "should_report_error_when_extensions_are_not_available",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = github_copilot_sdk::Client::start(
                    ctx.client_options().with_extra_args(["--yolo"]),
                )
                .await
                .expect("start client");
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                expect_err_contains(
                    session.rpc().extensions().enable(ExtensionsEnableRequest {
                        id: "missing-extension".to_string(),
                    }),
                    "Extensions not available",
                )
                .await;
                expect_err_contains(
                    session
                        .rpc()
                        .extensions()
                        .disable(ExtensionsDisableRequest {
                            id: "missing-extension".to_string(),
                        }),
                    "Extensions not available",
                )
                .await;
                expect_err_contains(
                    session.rpc().extensions().reload(),
                    "Extensions not available",
                )
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn create_skill_directory(
    work_dir: &std::path::Path,
    skill_name: &str,
    description: &str,
) -> std::path::PathBuf {
    let skills_dir = work_dir.join("session-rpc-skills");
    create_skill(&skills_dir, skill_name, description);
    skills_dir
}

fn create_skill(skills_dir: &std::path::Path, skill_name: &str, description: &str) {
    let skill_dir = skills_dir.join(skill_name);
    std::fs::create_dir_all(&skill_dir).expect("create skill dir");
    std::fs::write(
        skill_dir.join("SKILL.md"),
        format!(
            "---\nname: {skill_name}\ndescription: {description}\n---\n\n# {skill_name}\n\nThis skill is used by RPC E2E tests.\n"
        ),
    )
    .expect("write skill");
}

fn assert_skill(
    list: github_copilot_sdk::generated::api_types::SkillList,
    skill_name: &str,
    enabled: bool,
) -> github_copilot_sdk::generated::api_types::Skill {
    let skill = list
        .skills
        .into_iter()
        .find(|skill| skill.name == skill_name)
        .unwrap_or_else(|| panic!("skill {skill_name} not found"));
    assert_eq!(skill.enabled, enabled);
    assert!(
        skill
            .path
            .as_deref()
            .is_some_and(|path| path.contains(skill_name) && path.ends_with("SKILL.md"))
    );
    skill
}

fn test_mcp_servers(repo_root: &Path, server_name: &str) -> HashMap<String, McpServerConfig> {
    let harness_dir = repo_root.join("test").join("harness");
    let server_path = harness_dir
        .join("test-mcp-server.mjs")
        .to_string_lossy()
        .to_string();

    HashMap::from([(
        server_name.to_string(),
        McpServerConfig::Stdio(McpStdioServerConfig {
            tools: Some(vec!["*".to_string()]),
            command: if cfg!(windows) {
                "node.exe".to_string()
            } else {
                "node".to_string()
            },
            args: vec![server_path],
            working_directory: Some(harness_dir.to_string_lossy().to_string()),
            ..McpStdioServerConfig::default()
        }),
    )])
}

async fn expect_err_contains<T>(
    future: impl std::future::Future<Output = Result<T, github_copilot_sdk::Error>>,
    expected: &str,
) {
    let err = match future.await {
        Ok(_) => panic!("expected RPC failure"),
        Err(err) => err,
    };
    assert!(
        err.to_string()
            .to_ascii_lowercase()
            .contains(&expected.to_ascii_lowercase()),
        "expected error to contain {expected:?}, got {err}"
    );
}
