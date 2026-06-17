use github_copilot_sdk::Client;
use github_copilot_sdk::rpc::{
    ConnectRemoteSessionParams, LocalSessionMetadataValue, McpDiscoverRequest, NameSetRequest,
    PingRequest, SecretsAddFilterValuesRequest, SessionContext, SessionFsSetProviderConventions,
    SessionFsSetProviderRequest, SessionListFilter, SessionsBulkDeleteRequest,
    SessionsCheckInUseRequest, SessionsCloseRequest, SessionsEnrichMetadataRequest,
    SessionsFindByPrefixRequest, SessionsFindByTaskIDRequest, SessionsGetLastForContextRequest,
    SessionsListRequest, SessionsLoadDeferredRepoHooksRequest, SessionsPruneOldRequest,
    SessionsReleaseLockRequest, SessionsReloadPluginHooksRequest, SessionsSaveRequest,
    SessionsSetAdditionalPluginsRequest, SkillsConfigSetDisabledSkillsRequest,
    SkillsDiscoverRequest, ToolsListRequest,
};
use serde_json::json;

use super::support::with_e2e_context;

#[tokio::test]
async fn should_call_rpc_ping_with_typed_params_and_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_ping_with_typed_params_and_result",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .ping(PingRequest {
                        message: Some("typed rpc test".to_string()),
                    })
                    .await
                    .expect("ping");

                assert_eq!(result.message, "pong: typed rpc test");
                assert!(!result.timestamp.is_empty());
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_models_list_with_typed_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_models_list_with_typed_result",
        |ctx| {
            Box::pin(async move {
                let token = "rpc-models-token";
                ctx.set_copilot_user_by_token_with_login(token, "rpc-user");
                let client = Client::start(ctx.client_options().with_github_token(token))
                    .await
                    .expect("start client");

                let result = client.rpc().models().list().await.expect("models list");

                assert!(
                    result
                        .models
                        .iter()
                        .any(|model| model.id == "claude-sonnet-4.5")
                );
                assert!(result.models.iter().all(|model| !model.name.is_empty()));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_account_get_quota_when_authenticated() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_account_get_quota_when_authenticated",
        |ctx| {
            Box::pin(async move {
                let token = "rpc-quota-token";
                ctx.set_copilot_user_by_token_with_login_and_quota(
                    token,
                    "rpc-user",
                    Some(json!({
                        "chat": {
                            "entitlement": 100,
                            "overage_count": 2,
                            "overage_permitted": true,
                            "percent_remaining": 75,
                            "timestamp_utc": "2026-04-30T00:00:00Z"
                        }
                    })),
                );
                let client = Client::start(ctx.client_options().with_github_token(token))
                    .await
                    .expect("start client");

                let result = client.rpc().account().get_quota().await.expect("quota");
                let chat = result.quota_snapshots.get("chat").expect("chat quota");

                assert_eq!(chat.entitlement_requests, 100);
                assert_eq!(chat.used_requests, 25);
                assert_eq!(chat.remaining_percentage, 75.0);
                assert_eq!(chat.overage, 2.0);
                assert!(chat.usage_allowed_with_exhausted_quota);
                assert!(chat.overage_allowed_with_exhausted_quota);
                assert_eq!(chat.reset_date.as_deref(), Some("2026-04-30T00:00:00Z"));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_tools_list_with_typed_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_tools_list_with_typed_result",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .tools()
                    .list(ToolsListRequest { model: None })
                    .await
                    .expect("tools list");

                assert!(!result.tools.is_empty());
                assert!(result.tools.iter().all(|tool| !tool.name.is_empty()));
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_discover_server_mcp_and_skills() {
    with_e2e_context(
        "rpc_server",
        "should_discover_server_mcp_and_skills",
        |ctx| {
            Box::pin(async move {
                let skill_name = "server-rpc-skill-rust";
                let skill_directory = create_skill_directory(
                    ctx.work_dir(),
                    skill_name,
                    "Skill discovered by server-scoped RPC tests.",
                );
                let client = ctx.start_client().await;

                let mcp = client
                    .rpc()
                    .mcp()
                    .discover(McpDiscoverRequest {
                        working_directory: Some(ctx.work_dir().to_string_lossy().to_string()),
                    })
                    .await
                    .expect("mcp discover");
                assert!(mcp.servers.iter().all(|server| !server.name.is_empty()));

                let skills = client
                    .rpc()
                    .skills()
                    .discover(SkillsDiscoverRequest {
                        exclude_host_skills: None,
                        project_paths: None,
                        skill_directories: Some(vec![
                            skill_directory.to_string_lossy().to_string(),
                        ]),
                    })
                    .await
                    .expect("skills discover");
                let discovered = assert_server_skill(skills, skill_name, true);
                assert_eq!(
                    discovered.description,
                    "Skill discovered by server-scoped RPC tests."
                );

                client
                    .rpc()
                    .skills()
                    .config()
                    .set_disabled_skills(SkillsConfigSetDisabledSkillsRequest {
                        disabled_skills: vec![skill_name.to_string()],
                    })
                    .await
                    .expect("disable skill globally");
                let disabled_skills = client
                    .rpc()
                    .skills()
                    .discover(SkillsDiscoverRequest {
                        exclude_host_skills: None,
                        project_paths: None,
                        skill_directories: Some(vec![
                            skill_directory.to_string_lossy().to_string(),
                        ]),
                    })
                    .await
                    .expect("skills discover disabled");
                assert_server_skill(disabled_skills, skill_name, false);

                client
                    .rpc()
                    .skills()
                    .config()
                    .set_disabled_skills(SkillsConfigSetDisabledSkillsRequest {
                        disabled_skills: Vec::new(),
                    })
                    .await
                    .expect("clear disabled skills");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_rpc_sessionfs_setprovider_with_typed_result() {
    with_e2e_context(
        "rpc_server",
        "should_call_rpc_sessionfs_setprovider_with_typed_result",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let result = client
                    .rpc()
                    .session_fs()
                    .set_provider(SessionFsSetProviderRequest {
                        capabilities: None,
                        conventions: if cfg!(windows) {
                            SessionFsSetProviderConventions::Windows
                        } else {
                            SessionFsSetProviderConventions::Posix
                        },
                        initial_cwd: ctx.work_dir().display().to_string(),
                        session_state_path: ctx
                            .work_dir()
                            .join("session-state")
                            .display()
                            .to_string(),
                    })
                    .await
                    .expect("set session fs provider");
                assert!(result.success);

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_add_secret_filter_values() {
    with_e2e_context("rpc_server", "should_add_secret_filter_values", |ctx| {
        Box::pin(async move {
            let client = ctx.start_client().await;

            let result = client
                .rpc()
                .secrets()
                .add_filter_values(SecretsAddFilterValuesRequest {
                    values: vec!["rust-secret-value".to_string()],
                })
                .await;
            match result {
                Ok(result) => assert!(result.ok),
                Err(err) => {
                    let message = err.to_string();
                    assert!(message.contains("COPILOT_ENABLE_SECRET_FILTERING"));
                    assert!(!message.contains("Unhandled method secrets.addFilterValues"));
                }
            }

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_list_find_and_inspect_persisted_session_state() {
    with_e2e_context(
        "rpc_server",
        "should_list_find_and_inspect_persisted_session_state",
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
                    .name()
                    .set(NameSetRequest {
                        name: "Rust persisted session".to_string(),
                    })
                    .await
                    .expect("set session name");
                let session_id = session.id().clone();
                client
                    .rpc()
                    .sessions()
                    .save(SessionsSaveRequest {
                        session_id: session_id.clone(),
                    })
                    .await
                    .expect("save session");
                session.disconnect().await.expect("disconnect session");

                let list = client.rpc().sessions().list().await.expect("list sessions");
                assert!(list.sessions.iter().all(|metadata| {
                    metadata
                        .get("sessionId")
                        .and_then(serde_json::Value::as_str)
                        .is_some_and(|id| !id.is_empty())
                }));
                let filtered = client
                    .rpc()
                    .sessions()
                    .list_with_params(SessionsListRequest {
                        filter: Some(SessionListFilter {
                            cwd: Some(ctx.work_dir().display().to_string()),
                            branch: None,
                            git_root: None,
                            repository: None,
                        }),
                        include_detached: None,
                        metadata_limit: Some(10),
                        source: None,
                        throw_on_error: None,
                    })
                    .await
                    .expect("filtered sessions");
                assert!(filtered.sessions.iter().all(|metadata| {
                    metadata
                        .get("context")
                        .and_then(|context| context.get("cwd"))
                        .and_then(serde_json::Value::as_str)
                        .is_none_or(|cwd| cwd == ctx.work_dir().display().to_string())
                }));
                assert!(
                    client
                        .rpc()
                        .sessions()
                        .find_by_prefix(SessionsFindByPrefixRequest {
                            prefix: "0000000".to_string(),
                        })
                        .await
                        .expect("find missing prefix")
                        .session_id
                        .is_none()
                );
                assert!(
                    client
                        .rpc()
                        .sessions()
                        .find_by_task_id(SessionsFindByTaskIDRequest {
                            task_id: "missing-rust-task".to_string(),
                        })
                        .await
                        .expect("find by task id")
                        .session_id
                        .is_none()
                );
                client
                    .rpc()
                    .sessions()
                    .get_last_for_context(SessionsGetLastForContextRequest { context: None })
                    .await
                    .expect("last for context");
                assert!(
                    client
                        .rpc()
                        .sessions()
                        .get_sizes()
                        .await
                        .expect("session sizes")
                        .sizes
                        .values()
                        .all(|size| *size >= 0)
                );
                let in_use = client
                    .rpc()
                    .sessions()
                    .check_in_use(SessionsCheckInUseRequest {
                        session_ids: vec![session_id.to_string(), "missing-session-id".to_string()],
                    })
                    .await
                    .expect("check in use");
                assert!(!in_use.in_use.iter().any(|id| id == "missing-session-id"));

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_enrich_basic_session_metadata() {
    with_e2e_context(
        "rpc_server",
        "should_enrich_basic_session_metadata",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                let metadata = LocalSessionMetadataValue {
                    client_name: None,
                    context: Some(SessionContext {
                        branch: None,
                        cwd: ctx.work_dir().display().to_string(),
                        git_root: None,
                        host_type: None,
                        repository: None,
                    }),
                    is_detached: None,
                    is_remote: false,
                    mc_task_id: None,
                    modified_time: "2026-01-01T00:00:00.000Z".to_string(),
                    name: Some("Rust metadata".to_string()),
                    session_id: session_id.clone(),
                    start_time: "2026-01-01T00:00:00.000Z".to_string(),
                    summary: None,
                };

                let enriched = client
                    .rpc()
                    .sessions()
                    .enrich_metadata(SessionsEnrichMetadataRequest {
                        sessions: vec![metadata],
                    })
                    .await
                    .expect("enrich metadata");
                let enriched = enriched.sessions.first().expect("enriched session");
                assert_eq!(enriched.session_id, session_id);
                assert!(!enriched.is_remote);
                assert!(enriched.context.is_some());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_close_active_session_and_release_lock() {
    with_e2e_context(
        "rpc_server",
        "should_close_active_session_and_release_lock",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();

                client
                    .rpc()
                    .sessions()
                    .close(SessionsCloseRequest {
                        session_id: session_id.clone(),
                    })
                    .await
                    .expect("close session");
                client
                    .rpc()
                    .sessions()
                    .release_lock(SessionsReleaseLockRequest {
                        session_id: session_id.clone(),
                    })
                    .await
                    .expect("release lock");
                assert!(
                    !client
                        .rpc()
                        .sessions()
                        .check_in_use(SessionsCheckInUseRequest {
                            session_ids: vec![session_id.to_string()],
                        })
                        .await
                        .expect("check after release")
                        .in_use
                        .contains(&session_id.to_string())
                );

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_prune_dryrun_and_bulkdelete_persisted_session() {
    with_e2e_context(
        "rpc_server",
        "should_prune_dryrun_and_bulkdelete_persisted_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                session.disconnect().await.expect("disconnect session");

                let prune = client
                    .rpc()
                    .sessions()
                    .prune_old(SessionsPruneOldRequest {
                        older_than_days: 0,
                        dry_run: Some(true),
                        include_named: Some(true),
                        exclude_session_ids: Some(vec![session_id.to_string()]),
                    })
                    .await
                    .expect("dry-run prune");
                assert!(prune.dry_run);
                assert!(prune.deleted.is_empty());
                assert!(!prune.candidates.iter().any(|id| id == session_id.as_str()));
                let deleted = client
                    .rpc()
                    .sessions()
                    .bulk_delete(SessionsBulkDeleteRequest {
                        session_ids: vec![session_id.to_string()],
                    })
                    .await
                    .expect("bulk delete");
                assert!(deleted.freed_bytes.contains_key(session_id.as_str()));

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_set_additional_plugins_and_reload_deferred_hooks() {
    with_e2e_context(
        "rpc_server",
        "should_set_additional_plugins_and_reload_deferred_hooks",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                client
                    .rpc()
                    .sessions()
                    .set_additional_plugins(SessionsSetAdditionalPluginsRequest {
                        plugins: Vec::new(),
                    })
                    .await
                    .expect("set additional plugins");
                client
                    .rpc()
                    .sessions()
                    .reload_plugin_hooks(SessionsReloadPluginHooksRequest {
                        session_id: session.id().clone(),
                        defer_repo_hooks: Some(true),
                    })
                    .await
                    .expect("reload plugin hooks");
                let loaded = client
                    .rpc()
                    .sessions()
                    .load_deferred_repo_hooks(SessionsLoadDeferredRepoHooksRequest {
                        session_id: session.id().clone(),
                    })
                    .await
                    .expect("load deferred hooks");
                assert!(loaded.startup_prompts.is_empty());
                assert_eq!(loaded.hook_count, 0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_save_and_get_event_file_path() {
    with_e2e_context("rpc_server", "should_save_and_get_event_file_path", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            client
                .rpc()
                .sessions()
                .save(SessionsSaveRequest {
                    session_id: session.id().clone(),
                })
                .await
                .expect("save session");

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_report_implemented_error_when_connecting_unknown_remote_session() {
    with_e2e_context(
        "rpc_server",
        "should_report_implemented_error_when_connecting_unknown_remote_session",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;

                let err = client
                    .rpc()
                    .sessions()
                    .connect(ConnectRemoteSessionParams {
                        session_id: github_copilot_sdk::SessionId::from(
                            "00000000-0000-0000-0000-000000000000",
                        ),
                    })
                    .await
                    .expect_err("unknown remote session should fail");
                assert!(
                    !err.to_string()
                        .contains("Unhandled method sessions.connect")
                );

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
    let skills_dir = work_dir.join("server-rpc-skills");
    let skill_dir = skills_dir.join(skill_name);
    std::fs::create_dir_all(&skill_dir).expect("create skill dir");
    std::fs::write(
        skill_dir.join("SKILL.md"),
        format!(
            "---\nname: {skill_name}\ndescription: {description}\n---\n\n# {skill_name}\n\nThis skill is used by RPC E2E tests.\n"
        ),
    )
    .expect("write skill");
    skills_dir
}

fn assert_server_skill(
    list: github_copilot_sdk::rpc::ServerSkillList,
    skill_name: &str,
    enabled: bool,
) -> github_copilot_sdk::rpc::ServerSkill {
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
