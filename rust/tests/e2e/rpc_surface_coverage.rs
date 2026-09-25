use std::collections::HashMap;
use std::path::PathBuf;

use github_copilot_sdk::rpc::*;
use github_copilot_sdk::{CliProgram, Client, ClientOptions, ErrorKind, SessionConfig, Transport};
use serde::Serialize;
use serde_json::{Value, json};
use tempfile::TempDir;

macro_rules! rpc_ok {
    ($call:expr) => {
        $call
            .await
            .unwrap_or_else(|error| panic!("{} failed: {error}", stringify!($call)))
    };
}

#[tokio::test]
async fn client_rpc_surface_uses_typed_namespaces_and_round_trips_results() {
    let mut results = ResponseMap::default();
    results.insert(
        "account.getQuota",
        AccountGetQuotaResult {
            quota_snapshots: HashMap::from([(
                "premium_interactions".to_string(),
                AccountQuotaSnapshot {
                    entitlement_requests: 300,
                    remaining_percentage: 62.5,
                    reset_date: Some("2026-10-01T00:00:00Z".to_string()),
                    used_requests: 112,
                    ..Default::default()
                },
            )]),
        },
    );
    results.insert(
        "catalog.search",
        CatalogSearchResult::Succeeded(CatalogSearchSucceeded {
            search_id: "search-17".to_string(),
            truncated: true,
            ..Default::default()
        }),
    );
    results.insert_default::<DiscoveredExtensions>("extensions.discover");
    results.insert_default::<HooksDiscoverResult>("hooks.discover");
    results.insert_default::<LlmInferenceSetProviderResult>("llmInference.setProvider");
    results.insert_default::<ManagedSettingsReadResult>("managedSettings.read");
    results.insert(
        "mcp.planInstall",
        McpPlanInstallResult::Unavailable(CatalogUnavailableError {
            message: "catalog intentionally offline".to_string(),
            ..Default::default()
        }),
    );
    results.insert_default::<ModelList>("models.list");
    results.insert_default::<BuiltInModelCatalog>("models.getBuiltInCatalog");
    results.insert_default::<MarketplaceRefreshResult>("plugins.marketplaces.refresh");
    results.insert_value(
        "sessions.getClientMetadata",
        json!([{"clientName": "fixture-host", "capabilities": ["rpc"]}]),
    );
    results.insert_default::<EventsReadResult>("sessions.readPersistedEvents");

    let fake = FakeCli::new(results, ErrorMap::default());
    let client = fake.start_client().await;

    rpc_ok!(client.rpc().register_extension_launch_provider());
    let quota = rpc_ok!(
        client
            .rpc()
            .account()
            .get_quota_with_params(AccountGetQuotaRequest {
                git_hub_token: Some("quota-token".to_string()),
                selection_id: Some("account-42".to_string()),
            })
    );
    let snapshot = quota
        .quota_snapshots
        .get("premium_interactions")
        .expect("premium quota snapshot");
    assert_eq!(snapshot.entitlement_requests, 300);
    assert_eq!(snapshot.used_requests, 112);
    assert_eq!(snapshot.remaining_percentage, 62.5);
    assert_eq!(snapshot.reset_date.as_deref(), Some("2026-10-01T00:00:00Z"));

    let search = rpc_ok!(client.rpc().catalog().search(CatalogSearchRequest {
        contract: CatalogClientContract {
            protocol_version: 7,
            required_capabilities: vec!["install-plans".to_string()],
        },
        kinds: None,
        limit: Some(4),
        page: None,
        query: "offline catalog".to_string(),
    }));
    let CatalogSearchResult::Succeeded(search) = search else {
        panic!("expected successful catalog search");
    };
    assert_eq!(search.search_id, "search-17");
    assert!(search.truncated);

    rpc_ok!(client.rpc().extensions().discover());
    rpc_ok!(client.rpc().hooks().discover(HooksDiscoverRequest {
        exclude_host_hooks: Some(true),
        project_paths: Some(vec!["project-a".to_string(), "project-b".to_string()]),
    }));
    rpc_ok!(client.rpc().llm_inference().set_provider());
    rpc_ok!(client.rpc().managed_settings().read());

    let plan = rpc_ok!(client.rpc().mcp().plan_install(McpPlanInstallRequest {
        contract: CatalogClientContract {
            protocol_version: 7,
            required_capabilities: vec!["install-plans".to_string()],
        },
        scope: None,
        source: McpPlanInstallSource::Candidate(McpPlanInstallSourceCandidate {
            candidate_handle: "candidate-handle".to_string(),
            kind: McpPlanInstallSourceCandidateKind::Candidate,
            search_id: "search-17".to_string(),
        }),
    }));
    let McpPlanInstallResult::Unavailable(unavailable) = plan else {
        panic!("expected typed unavailable install plan");
    };
    assert_eq!(unavailable.message, "catalog intentionally offline");

    rpc_ok!(
        client
            .rpc()
            .models()
            .list_with_params(ModelsListRequest::default())
    );
    rpc_ok!(client.rpc().models().get_built_in_catalog());
    rpc_ok!(
        client
            .rpc()
            .plugins()
            .builtin()
            .set(PluginsBuiltinSetRequest::default())
    );
    rpc_ok!(client.rpc().plugins().marketplaces().refresh());
    let metadata = rpc_ok!(
        client
            .rpc()
            .sessions()
            .get_client_metadata(SessionsGetClientMetadataRequest::default())
    );
    assert_eq!(metadata[0]["clientName"], "fixture-host");
    assert_eq!(metadata[0]["capabilities"][0], "rpc");
    rpc_ok!(
        client
            .rpc()
            .sessions()
            .read_persisted_events(SessionsReadPersistedEventsRequest::default())
    );
    rpc_ok!(
        client
            .rpc()
            .skills()
            .config()
            .set_skill_disabled(SkillsConfigSetSkillDisabledRequest::default())
    );

    client.stop().await.expect("stop fake CLI");

    fake.assert_target_methods(&[
        "registerExtensionLaunchProvider",
        "account.getQuota",
        "catalog.search",
        "extensions.discover",
        "hooks.discover",
        "llmInference.setProvider",
        "managedSettings.read",
        "mcp.planInstall",
        "models.list",
        "models.getBuiltInCatalog",
        "plugins.builtin.set",
        "plugins.marketplaces.refresh",
        "sessions.getClientMetadata",
        "sessions.readPersistedEvents",
        "skills.config.setSkillDisabled",
    ]);
    fake.assert_params(
        "account.getQuota",
        0,
        json!({
            "gitHubToken": "quota-token",
            "selectionId": "account-42"
        }),
    );
    fake.assert_params(
        "catalog.search",
        0,
        json!({
            "contract": {
                "protocolVersion": 7,
                "requiredCapabilities": ["install-plans"]
            },
            "limit": 4,
            "query": "offline catalog"
        }),
    );
    fake.assert_params(
        "mcp.planInstall",
        0,
        json!({
            "contract": {
                "protocolVersion": 7,
                "requiredCapabilities": ["install-plans"]
            },
            "source": {
                "candidateHandle": "candidate-handle",
                "kind": "candidate",
                "searchId": "search-17"
            }
        }),
    );
}

#[tokio::test]
async fn session_lifecycle_workflow_and_history_rpc_surface_is_typed() {
    let mut results = ResponseMap::default();
    results.insert_default::<SendMessagesResult>("session.sendMessages");
    results.insert_default::<AbortResult>("session.abort");
    results.insert_default::<InterruptMainTurnResult>("session.interruptMainTurn");
    results.insert_value("session.cancelAllBackgroundAgents", json!(3));
    results.insert_default::<AgentList>("session.agent.list");
    results.insert(
        "session.autopilotObjective.getState",
        AutopilotObjectiveGetStateResult {
            state: Some(AutopilotObjectiveState::default()),
        },
    );
    results.insert_default::<CompletionsGetTriggerCharactersResult>(
        "session.completions.getTriggerCharacters",
    );
    results
        .insert_default::<ContentExclusionCheckPathsResult>("session.contentExclusion.checkPaths");
    results.insert(
        "session.workflow.run",
        WorkflowRunResult {
            attempt: Some(2),
            result: Some(json!({"artifact": "workflow-output"})),
            run_id: "run-123".to_string(),
            ..Default::default()
        },
    );
    results.insert(
        "session.workflow.resume",
        WorkflowResumeResult {
            workflow_name: "coverage-workflow".to_string(),
            run: WorkflowRunResult {
                run_id: "run-123".to_string(),
                ..Default::default()
            },
        },
    );
    results.insert_default::<WorkflowRunResult>("session.workflow.getRun");
    results.insert_default::<WorkflowListRunsResult>("session.workflow.listRuns");
    results.insert_default::<WorkflowRunDetail>("session.workflow.getRunDetail");
    results.insert_default::<WorkflowProgressPage>("session.workflow.getRunProgress");
    results.insert_default::<WorkflowRunResult>("session.workflow.cancel");
    results.insert_default::<WorkflowRunResult>("session.workflow.pause");
    results.insert_default::<WorkflowAckResult>("session.workflow.log");
    results.insert_default::<WorkflowAgentResult>("session.workflow.agent");
    results.insert_default::<WorkflowJournalGetResult>("session.workflow.journal.get");
    results.insert_default::<WorkflowAckResult>("session.workflow.journal.put");
    results.insert_default::<FleetStartResult>("session.fleet.start");
    results.insert(
        "session.history.compact",
        HistoryCompactResult {
            messages_removed: 8,
            success: true,
            summary_content: Some("deterministic summary".to_string()),
            tokens_removed: 144,
            ..Default::default()
        },
    );
    results.insert(
        "session.history.clearContext",
        HistoryClearContextResult {
            messages_cleared: 5,
        },
    );
    results.insert(
        "session.limitPrediction.predict",
        SessionLimitPredictionResult::Unavailable(SessionLimitPredictionResultUnavailable {
            reason: SessionLimitPredictionUnavailableReason::NoModel,
            ..Default::default()
        }),
    );

    let fake = FakeCli::new(results, ErrorMap::default());
    let client = fake.start_client().await;
    let session = fake.create_session(&client).await;

    rpc_ok!(session.rpc().suspend());
    rpc_ok!(session.rpc().send_messages(SendMessagesRequest::default()));
    rpc_ok!(session.rpc().abort(AbortRequest::default()));
    rpc_ok!(
        session
            .rpc()
            .interrupt_main_turn(InterruptMainTurnRequest::default())
    );
    let cancelled = rpc_ok!(session.rpc().cancel_all_background_agents());
    assert_eq!(cancelled, 3);
    rpc_ok!(
        session
            .rpc()
            .agent()
            .list_with_params(AgentListRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .agent()
            .set_prompt(AgentSetPromptRequest::default())
    );
    let objective = rpc_ok!(session.rpc().autopilot_objective().get_state());
    assert!(objective.state.is_some());
    rpc_ok!(session.rpc().completions().get_trigger_characters());
    rpc_ok!(
        session
            .rpc()
            .content_exclusion()
            .check_paths(ContentExclusionCheckPathsRequest {
                paths: vec![
                    "C:\\workspace\\one.rs".to_string(),
                    "/workspace/two.rs".to_string()
                ],
            })
    );

    let run = rpc_ok!(session.rpc().workflow().run(WorkflowRunRequest {
        args: json!({"mode": "offline", "count": 2}),
        name: "coverage-workflow".to_string(),
        options: None,
    }));
    assert_eq!(run.run_id, "run-123");
    assert_eq!(run.attempt, Some(2));
    assert_eq!(run.result, Some(json!({"artifact": "workflow-output"})));
    let resumed = rpc_ok!(session.rpc().workflow().resume(WorkflowResumeRequest {
        run_id: "run-123".to_string(),
        notify_on_complete: Some(false),
        ..Default::default()
    }));
    assert_eq!(resumed.workflow_name, "coverage-workflow");
    assert_eq!(resumed.run.run_id, "run-123");
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .get_run(WorkflowGetRunRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .list_runs(WorkflowListRunsRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .get_run_detail(WorkflowGetRunRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .get_run_progress(WorkflowGetRunProgressRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .cancel(WorkflowCancelRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .pause(WorkflowPauseRequest::default())
    );
    rpc_ok!(session.rpc().workflow().log(WorkflowLogRequest::default()));
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .agent(WorkflowAgentRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .journal()
            .get(WorkflowJournalGetRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workflow()
            .journal()
            .put(WorkflowJournalPutRequest::default())
    );
    rpc_ok!(session.rpc().fleet().start(FleetStartRequest::default()));

    let compact = rpc_ok!(session.rpc().history().compact());
    assert!(compact.success);
    assert_eq!(compact.messages_removed, 8);
    assert_eq!(compact.tokens_removed, 144);
    assert_eq!(
        compact.summary_content.as_deref(),
        Some("deterministic summary")
    );
    rpc_ok!(
        session
            .rpc()
            .history()
            .compact_with_params(HistoryCompactRequest::default())
    );
    let cleared = rpc_ok!(
        session
            .rpc()
            .history()
            .clear_context(HistoryClearContextRequest::default())
    );
    assert_eq!(cleared.messages_cleared, 5);
    let prediction = rpc_ok!(session.rpc().limit_prediction().predict());
    assert!(matches!(
        prediction,
        SessionLimitPredictionResult::Unavailable(SessionLimitPredictionResultUnavailable {
            reason: SessionLimitPredictionUnavailableReason::NoModel,
            ..
        })
    ));
    rpc_ok!(
        session
            .rpc()
            .limit_prediction()
            .predict_with_params(SessionLimitPredictionRequest {
                model_id: Some("fixture-model".to_string()),
                ..Default::default()
            })
    );

    session.disconnect().await.expect("disconnect fake session");
    client.stop().await.expect("stop fake CLI");

    fake.assert_target_methods(&[
        "session.suspend",
        "session.sendMessages",
        "session.abort",
        "session.interruptMainTurn",
        "session.cancelAllBackgroundAgents",
        "session.agent.list",
        "session.agent.setPrompt",
        "session.autopilotObjective.getState",
        "session.completions.getTriggerCharacters",
        "session.contentExclusion.checkPaths",
        "session.workflow.run",
        "session.workflow.resume",
        "session.workflow.getRun",
        "session.workflow.listRuns",
        "session.workflow.getRunDetail",
        "session.workflow.getRunProgress",
        "session.workflow.cancel",
        "session.workflow.pause",
        "session.workflow.log",
        "session.workflow.agent",
        "session.workflow.journal.get",
        "session.workflow.journal.put",
        "session.fleet.start",
        "session.history.compact",
        "session.history.compact",
        "session.history.clearContext",
        "session.limitPrediction.predict",
        "session.limitPrediction.predict",
    ]);
    fake.assert_params(
        "session.workflow.run",
        0,
        json!({
            "sessionId": "rpc-surface-session",
            "args": {"mode": "offline", "count": 2},
            "name": "coverage-workflow"
        }),
    );
    fake.assert_params(
        "session.limitPrediction.predict",
        1,
        json!({
            "sessionId": "rpc-surface-session",
            "modelId": "fixture-model"
        }),
    );
}

#[tokio::test]
async fn session_mcp_metadata_model_and_permission_rpc_surface_is_typed() {
    let mut results = ResponseMap::default();
    results
        .insert_default::<MoveMcpLoadingToBackgroundResult>("session.mcp.moveLoadingToBackground");
    results.insert_default::<McpAppsReadResourceResult>("session.mcp.apps.readResource");
    results.insert(
        "session.mcp.oauth.probe",
        McpOauthProbeResult::Failed(McpOauthProbeResultFailed {
            error: "offline probe fixture".to_string(),
            ..Default::default()
        }),
    );
    results.insert(
        "session.mcp.oauth.respond",
        McpOauthRespondResult { success: true },
    );
    results.insert_default::<McpResourcesListResult>("session.mcp.resources.list");
    results
        .insert_default::<McpResourcesListTemplatesResult>("session.mcp.resources.listTemplates");
    results.insert_value(
        "session.metadata.getClientMetadata",
        json!({"clientName": "rust-fixture", "version": "1"}),
    );
    results.insert_value(
        "session.metadata.updateClientMetadata",
        json!({"clientName": "updated-fixture", "version": "2"}),
    );
    results.insert_default::<ModelSwitchAutoTierResult>("session.model.switchAutoTier");
    results.insert_default::<SessionModelList>("session.model.list");
    results.insert_default::<PermissionsConfigureResult>("session.permissions.configure");
    results.insert_default::<PendingPermissionRequestList>("session.permissions.pendingRequests");
    results.insert_default::<PermissionsModifyRulesResult>("session.permissions.modifyRules");
    results.insert_default::<PermissionsSetRequiredResult>("session.permissions.setRequired");
    results.insert_default::<PermissionsNotifyPromptShownResult>(
        "session.permissions.notifyPromptShown",
    );
    results.insert_default::<FolderTrustCheckResult>("session.permissions.folderTrust.isTrusted");
    results.insert_default::<PermissionsFolderTrustAddTrustedResult>(
        "session.permissions.folderTrust.addTrusted",
    );
    results
        .insert_default::<PermissionLocationResolveResult>("session.permissions.locations.resolve");
    results.insert(
        "session.permissions.locations.apply",
        PermissionLocationApplyResult {
            applied_directory_count: 2,
            applied_rule_count: 3,
            changed: true,
            location_key: "repo-key".to_string(),
            location_type: PermissionLocationType::Repo,
            ..Default::default()
        },
    );
    results.insert_default::<PermissionsLocationsAddToolApprovalResult>(
        "session.permissions.locations.addToolApproval",
    );
    results.insert_default::<PermissionPathsList>("session.permissions.paths.list");
    results.insert_default::<PermissionsPathsAddResult>("session.permissions.paths.add");
    results.insert_default::<PermissionsPathsUpdatePrimaryResult>(
        "session.permissions.paths.updatePrimary",
    );
    results.insert_default::<PermissionPathsAllowedCheckResult>(
        "session.permissions.paths.isPathWithinAllowedDirectories",
    );
    results.insert_default::<PermissionPathsWorkspaceCheckResult>(
        "session.permissions.paths.isPathWithinWorkspace",
    );
    results.insert_default::<PermissionsUrlsSetUnrestrictedModeResult>(
        "session.permissions.urls.setUnrestrictedMode",
    );
    results.insert_default::<SkillsLoadDiagnostics>("session.customizations.reload");
    results.insert(
        "session.provider.getEndpoint",
        ProviderEndpoint {
            api_key: Some("fixture-api-key".to_string()),
            base_url: "https://offline.invalid/v1".to_string(),
            headers: HashMap::from([("x-fixture".to_string(), "rust".to_string())]),
            ..Default::default()
        },
    );

    let mut errors = ErrorMap::default();
    errors.insert(
        "session.debug.collectLogs",
        -32077,
        "fixture diagnostics unavailable",
        json!({"retryable": false, "source": "offline"}),
    );
    let fake = FakeCli::new(results, errors);
    let client = fake.start_client().await;
    let session = fake.create_session(&client).await;

    let error = session
        .rpc()
        .debug()
        .collect_logs(DebugCollectLogsRequest {
            additional_entries: None,
            destination: DebugCollectLogsDestination::Directory(
                DebugCollectLogsDestinationDirectory {
                    output_directory: "fixture-debug-output".to_string(),
                    ..Default::default()
                },
            ),
            include: None,
        })
        .await
        .expect_err("debug collection should return fixture RPC error");
    assert_eq!(error.rpc_code(), Some(-32077));
    assert_eq!(error.kind(), &ErrorKind::Rpc { code: -32077 });
    assert!(
        error
            .to_string()
            .contains("fixture diagnostics unavailable")
    );

    rpc_ok!(session.rpc().mcp().move_loading_to_background());
    rpc_ok!(
        session
            .rpc()
            .mcp()
            .start_server(McpStartServerRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .mcp()
            .restart_server(McpRestartServerRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .mcp()
            .apps()
            .read_resource(McpAppsReadResourceRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .mcp()
            .oauth()
            .authentication_state_changed(McpOauthAuthenticationStateChangedRequest::default())
    );
    let probe = rpc_ok!(
        session
            .rpc()
            .mcp()
            .oauth()
            .probe(McpOauthProbeRequest::default())
    );
    let McpOauthProbeResult::Failed(failed) = probe else {
        panic!("expected typed failed OAuth probe");
    };
    assert_eq!(failed.error, "offline probe fixture");
    let responded = rpc_ok!(
        session
            .rpc()
            .mcp()
            .oauth()
            .respond(McpOauthRespondRequest::default())
    );
    assert!(responded.success);
    rpc_ok!(
        session
            .rpc()
            .mcp()
            .resources()
            .list(McpResourcesListRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .mcp()
            .resources()
            .list_templates(McpResourcesListTemplatesRequest::default())
    );

    let metadata = rpc_ok!(session.rpc().metadata().get_client_metadata());
    assert_eq!(metadata["clientName"], "rust-fixture");
    let updated = rpc_ok!(
        session
            .rpc()
            .metadata()
            .update_client_metadata(MetadataUpdateClientMetadataRequest::default())
    );
    assert_eq!(updated["clientName"], "updated-fixture");
    rpc_ok!(
        session
            .rpc()
            .model()
            .switch_auto_tier(ModelSwitchAutoTierRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .model()
            .list_with_params(ModelListRequest::default())
    );

    rpc_ok!(
        session
            .rpc()
            .permissions()
            .configure(PermissionsConfigureParams::default())
    );
    rpc_ok!(session.rpc().permissions().pending_requests());
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .modify_rules(PermissionsModifyRulesParams::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .set_required(PermissionsSetRequiredRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .notify_prompt_shown(PermissionPromptShownNotification::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .folder_trust()
            .is_trusted(FolderTrustCheckParams::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .folder_trust()
            .add_trusted(FolderTrustAddParams::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .locations()
            .resolve(PermissionLocationResolveParams {
                working_directory: "fixture-worktree".to_string(),
            })
    );
    let applied = rpc_ok!(session.rpc().permissions().locations().apply(
        PermissionLocationApplyParams {
            working_directory: "fixture-worktree".to_string(),
        }
    ));
    assert!(applied.changed);
    assert_eq!(applied.applied_directory_count, 2);
    assert_eq!(applied.applied_rule_count, 3);
    assert_eq!(applied.location_key, "repo-key");
    assert_eq!(applied.location_type, PermissionLocationType::Repo);
    rpc_ok!(session.rpc().permissions().locations().add_tool_approval(
        PermissionLocationAddToolApprovalParams {
            approval: PermissionsLocationsAddToolApprovalDetails::Read(
                PermissionsLocationsAddToolApprovalDetailsRead::default(),
            ),
            location_key: "repo-key".to_string(),
        }
    ));
    rpc_ok!(session.rpc().permissions().paths().list());
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .paths()
            .add(PermissionPathsAddParams {
                path: "allowed-dir".to_string(),
            })
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .paths()
            .update_primary(PermissionPathsUpdatePrimaryParams::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .paths()
            .is_path_within_allowed_directories(PermissionPathsAllowedCheckParams {
                path: "allowed-dir/file.rs".to_string(),
            })
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .paths()
            .is_path_within_workspace(PermissionPathsWorkspaceCheckParams::default())
    );
    rpc_ok!(
        session
            .rpc()
            .permissions()
            .urls()
            .set_unrestricted_mode(PermissionUrlsSetUnrestrictedModeParams::default())
    );
    rpc_ok!(session.rpc().instructions().reload());
    rpc_ok!(session.rpc().customizations().reload());
    rpc_ok!(
        session
            .rpc()
            .plugins()
            .reload_with_params(PluginsReloadRequest::default())
    );
    let endpoint = rpc_ok!(session.rpc().provider().get_endpoint_with_params(
        ProviderGetEndpointRequest {
            model_id: Some("fixture-model".to_string()),
        }
    ));
    assert_eq!(endpoint.base_url, "https://offline.invalid/v1");
    assert_eq!(endpoint.api_key.as_deref(), Some("fixture-api-key"));
    assert_eq!(endpoint.headers["x-fixture"], "rust");

    session.disconnect().await.expect("disconnect fake session");
    client.stop().await.expect("stop fake CLI");

    fake.assert_target_methods(&[
        "session.debug.collectLogs",
        "session.mcp.moveLoadingToBackground",
        "session.mcp.startServer",
        "session.mcp.restartServer",
        "session.mcp.apps.readResource",
        "session.mcp.oauth.authenticationStateChanged",
        "session.mcp.oauth.probe",
        "session.mcp.oauth.respond",
        "session.mcp.resources.list",
        "session.mcp.resources.listTemplates",
        "session.metadata.getClientMetadata",
        "session.metadata.updateClientMetadata",
        "session.model.switchAutoTier",
        "session.model.list",
        "session.permissions.configure",
        "session.permissions.pendingRequests",
        "session.permissions.modifyRules",
        "session.permissions.setRequired",
        "session.permissions.notifyPromptShown",
        "session.permissions.folderTrust.isTrusted",
        "session.permissions.folderTrust.addTrusted",
        "session.permissions.locations.resolve",
        "session.permissions.locations.apply",
        "session.permissions.locations.addToolApproval",
        "session.permissions.paths.list",
        "session.permissions.paths.add",
        "session.permissions.paths.updatePrimary",
        "session.permissions.paths.isPathWithinAllowedDirectories",
        "session.permissions.paths.isPathWithinWorkspace",
        "session.permissions.urls.setUnrestrictedMode",
        "session.instructions.reload",
        "session.customizations.reload",
        "session.plugins.reload",
        "session.provider.getEndpoint",
    ]);
    fake.assert_params(
        "session.permissions.locations.addToolApproval",
        0,
        json!({
            "sessionId": "rpc-surface-session",
            "approval": {"kind": "read"},
            "locationKey": "repo-key"
        }),
    );
    fake.assert_params(
        "session.provider.getEndpoint",
        0,
        json!({
            "sessionId": "rpc-surface-session",
            "modelId": "fixture-model"
        }),
    );
}

#[tokio::test]
async fn session_queue_tasks_tools_ui_and_workspace_rpc_surface_is_typed() {
    let task = || TaskClientInfo {
        execution_mode: TaskClientExecutionMode::Background,
        owner: TaskClientOwner {
            kind: TaskClientOwnerKind::Sdk,
            presence: TaskClientOwnerPresence::Connected,
            ..Default::default()
        },
        status: TaskClientStatus::Running,
        r#type: TaskClientType::Client,
        ..Default::default()
    };
    let mut results = ResponseMap::default();
    results.insert_default::<QueueMoveItemResult>("session.queue.moveItem");
    results.insert(
        "session.queue.insertAt",
        QueueInsertAtResult {
            id: "queue-item-9".to_string(),
        },
    );
    results.insert_default::<QueueRemoveAtResult>("session.queue.removeAt");
    results.insert_default::<QueueUpdateTextResult>("session.queue.updateText");
    results.insert_default::<QueueDuplicateAtResult>("session.queue.duplicateAt");
    results.insert(
        "session.queue.sendNow",
        QueueSendNowResult { steered: true },
    );
    results.insert_default::<SandboxEnforcementStatus>("session.sandbox.getEnforcementStatus");
    results.insert_default::<SandboxDisableForSessionResult>("session.sandbox.disableForSession");
    results.insert(
        "session.tasks.register",
        TasksRegisterResult {
            created: true,
            task: task(),
            ..Default::default()
        },
    );
    results.insert(
        "session.tasks.update",
        TasksUpdateResult {
            applied: true,
            task: task(),
            ..Default::default()
        },
    );
    results
        .insert_default::<ToolsGetBuiltinDescriptorsResult>("session.tools.getBuiltinDescriptors");
    results.insert(
        "session.tools.taskCompleteEventData",
        TaskCompleteData {
            objective_id: Some(41),
            success: Some(true),
            summary: Some("coverage complete".to_string()),
            ..Default::default()
        },
    );
    results.insert_default::<ToolsSetResult>("session.tools.set");
    results.insert(
        "session.ui.elicitation",
        UIElicitationResponse {
            action: UIElicitationResponseAction::Accept,
            content: Some(HashMap::from([(
                "answer".to_string(),
                json!("deterministic"),
            )])),
            ..Default::default()
        },
    );
    results.insert(
        "session.workspaces.updateMetadata",
        workspace_result("workspace-updated"),
    );
    results.insert(
        "session.workspaces.ensure",
        workspace_result("workspace-ensured"),
    );
    results.insert(
        "session.workspaces.statFile",
        WorkspacesStatFileResult {
            is_file: true,
            mtime_ms: 1_234.0,
            size: 88.0,
            ..Default::default()
        },
    );
    results.insert(
        "session.workspaces.addSummary",
        WorkspacesAddSummaryResult {
            summary: Some(json!({"title": "offline summary", "number": 4})),
            workspace: Some(json!({"id": "workspace-ensured"})),
        },
    );
    results.insert(
        "session.workspaces.truncateSummaries",
        workspace_result("workspace-truncated"),
    );
    results.insert(
        "session.workspaces.readAutopilotObjective",
        WorkspacesReadAutopilotObjectiveResult {
            content: Some("Ship deterministic coverage".to_string()),
        },
    );
    results.insert(
        "session.workspaces.writeAutopilotObjective",
        WorkspacesWriteAutopilotObjectiveResult {
            operation: "created".to_string(),
        },
    );
    results.insert_default::<WorkspacesDeleteAutopilotObjectiveResult>(
        "session.workspaces.deleteAutopilotObjective",
    );
    results.insert(
        "session.workspaces.autopilotObjectiveExists",
        WorkspacesAutopilotObjectiveExistsResult { exists: true },
    );

    let fake = FakeCli::new(results, ErrorMap::default());
    let client = fake.start_client().await;
    let session = fake.create_session(&client).await;

    rpc_ok!(session.rpc().queue().move_item(QueueMoveItemRequest {
        id: "queue-item-1".to_string(),
        to_position: 2,
    }));
    let inserted = rpc_ok!(session.rpc().queue().insert_at(QueueInsertAtRequest {
        message: QueueInsertMessage {
            billable: Some(false),
            display_prompt: Some("Fixture display".to_string()),
            prompt: "Queue this deterministically".to_string(),
            request_headers: Some(HashMap::from([(
                "x-test".to_string(),
                "rpc-surface".to_string(),
            )])),
            ..Default::default()
        },
        position: 1,
    }));
    assert_eq!(inserted.id, "queue-item-9");
    rpc_ok!(
        session
            .rpc()
            .queue()
            .remove_at(QueueRemoveAtRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .queue()
            .update_text(QueueUpdateTextRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .queue()
            .duplicate_at(QueueDuplicateAtRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .queue()
            .set_drain_paused(QueueSetDrainPausedRequest { paused: true })
    );
    assert!(
        rpc_ok!(
            session
                .rpc()
                .queue()
                .send_now(QueueSendNowRequest::default())
        )
        .steered
    );
    rpc_ok!(session.rpc().sandbox().get_enforcement_status());
    rpc_ok!(
        session
            .rpc()
            .sandbox()
            .disable_for_session(SandboxDisableForSessionRequest::default())
    );

    let registered = rpc_ok!(session.rpc().tasks().register(TasksRegisterRequest {
        cancellable: true,
        client_task_id: "client-task-7".to_string(),
        description: "deterministic external work".to_string(),
        display_name: Some("Coverage task".to_string()),
        expected_sequence: Some(0),
        r#type: TaskClientType::Client,
    }));
    assert!(registered.created);
    let updated = rpc_ok!(session.rpc().tasks().update(TasksUpdateRequest {
        id: "task-7".to_string(),
        sequence: 1,
        update: TaskClientUpdate::Completed(TaskClientUpdateCompleted {
            message: Some("done".to_string()),
            result: Some(json!({"files": 2})),
            ..Default::default()
        }),
    }));
    assert!(updated.applied);

    rpc_ok!(
        session
            .rpc()
            .tools()
            .get_builtin_descriptors(ToolsGetBuiltinDescriptorsRequest::default())
    );
    let completed = rpc_ok!(
        session
            .rpc()
            .tools()
            .task_complete_event_data(ToolsTaskCompleteEventDataRequest::default())
    );
    assert_eq!(completed.objective_id, Some(41));
    assert_eq!(completed.success, Some(true));
    assert_eq!(completed.summary.as_deref(), Some("coverage complete"));
    rpc_ok!(session.rpc().tools().set(ToolsSetRequest::default()));
    let elicitation = rpc_ok!(
        session
            .rpc()
            .ui()
            .elicitation(UIElicitationRequest::default())
    );
    assert_eq!(elicitation.action, UIElicitationResponseAction::Accept);
    assert_eq!(
        elicitation
            .content
            .as_ref()
            .and_then(|content| content.get("answer")),
        Some(&json!("deterministic"))
    );

    let updated_workspace = rpc_ok!(
        session
            .rpc()
            .workspaces()
            .update_metadata(WorkspacesUpdateMetadataRequest::default())
    );
    assert_eq!(
        updated_workspace
            .workspace
            .as_ref()
            .expect("updated workspace")
            .id,
        "workspace-updated"
    );
    rpc_ok!(
        session
            .rpc()
            .workspaces()
            .ensure(WorkspacesEnsureRequest::default())
    );
    let stat = rpc_ok!(
        session
            .rpc()
            .workspaces()
            .stat_file(WorkspacesStatFileRequest::default())
    );
    assert!(stat.is_file);
    assert_eq!(stat.size, 88.0);
    assert_eq!(stat.mtime_ms, 1_234.0);
    rpc_ok!(
        session
            .rpc()
            .workspaces()
            .create_directory(WorkspacesCreateDirectoryRequest {
                path: "nested/output".to_string(),
                recursive: Some(true),
            })
    );
    rpc_ok!(
        session
            .rpc()
            .workspaces()
            .remove_path(WorkspacesRemovePathRequest::default())
    );
    rpc_ok!(
        session
            .rpc()
            .workspaces()
            .rename_path(WorkspacesRenamePathRequest::default())
    );
    let summary = rpc_ok!(
        session
            .rpc()
            .workspaces()
            .add_summary(WorkspacesAddSummaryRequest::default())
    );
    assert_eq!(summary.summary.as_ref().expect("summary")["number"], 4);
    rpc_ok!(
        session
            .rpc()
            .workspaces()
            .truncate_summaries(WorkspacesTruncateSummariesRequest { keep_count: 2 })
    );
    let objective = rpc_ok!(session.rpc().workspaces().read_autopilot_objective());
    assert_eq!(
        objective.content.as_deref(),
        Some("Ship deterministic coverage")
    );
    let write = rpc_ok!(session.rpc().workspaces().write_autopilot_objective(
        WorkspacesWriteAutopilotObjectiveRequest {
            content: "Updated deterministic objective".to_string(),
        }
    ));
    assert_eq!(write.operation, "created");
    rpc_ok!(session.rpc().workspaces().delete_autopilot_objective());
    assert!(rpc_ok!(session.rpc().workspaces().autopilot_objective_exists()).exists);

    session.disconnect().await.expect("disconnect fake session");
    client.stop().await.expect("stop fake CLI");

    fake.assert_target_methods(&[
        "session.queue.moveItem",
        "session.queue.insertAt",
        "session.queue.removeAt",
        "session.queue.updateText",
        "session.queue.duplicateAt",
        "session.queue.setDrainPaused",
        "session.queue.sendNow",
        "session.sandbox.getEnforcementStatus",
        "session.sandbox.disableForSession",
        "session.tasks.register",
        "session.tasks.update",
        "session.tools.getBuiltinDescriptors",
        "session.tools.taskCompleteEventData",
        "session.tools.set",
        "session.ui.elicitation",
        "session.workspaces.updateMetadata",
        "session.workspaces.ensure",
        "session.workspaces.statFile",
        "session.workspaces.createDirectory",
        "session.workspaces.removePath",
        "session.workspaces.renamePath",
        "session.workspaces.addSummary",
        "session.workspaces.truncateSummaries",
        "session.workspaces.readAutopilotObjective",
        "session.workspaces.writeAutopilotObjective",
        "session.workspaces.deleteAutopilotObjective",
        "session.workspaces.autopilotObjectiveExists",
    ]);
    fake.assert_params(
        "session.queue.insertAt",
        0,
        json!({
            "sessionId": "rpc-surface-session",
            "message": {
                "billable": false,
                "displayPrompt": "Fixture display",
                "prompt": "Queue this deterministically",
                "requestHeaders": {"x-test": "rpc-surface"}
            },
            "position": 1
        }),
    );
    fake.assert_params(
        "session.tasks.update",
        0,
        json!({
            "sessionId": "rpc-surface-session",
            "id": "task-7",
            "sequence": 1,
            "update": {
                "kind": "completed",
                "message": "done",
                "result": {"files": 2}
            }
        }),
    );
    fake.assert_params(
        "session.workspaces.writeAutopilotObjective",
        0,
        json!({
            "sessionId": "rpc-surface-session",
            "content": "Updated deterministic objective"
        }),
    );
}

fn workspace_result(id: &str) -> WorkspacesGetWorkspaceResult {
    WorkspacesGetWorkspaceResult {
        path: Some("fixture-workspace".to_string()),
        workspace: Some(WorkspacesGetWorkspaceResultWorkspace {
            id: id.to_string(),
            ..Default::default()
        }),
    }
}

#[derive(Default)]
struct ResponseMap(HashMap<&'static str, Value>);

impl ResponseMap {
    fn insert<T: Serialize>(&mut self, method: &'static str, result: T) {
        self.insert_value(
            method,
            serde_json::to_value(result).expect("serialize fake RPC result"),
        );
    }

    fn insert_default<T: Default + Serialize>(&mut self, method: &'static str) {
        self.insert(method, T::default());
    }

    fn insert_value(&mut self, method: &'static str, result: Value) {
        self.0.insert(method, result);
    }
}

#[derive(Default)]
struct ErrorMap(HashMap<&'static str, Value>);

impl ErrorMap {
    fn insert(&mut self, method: &'static str, code: i32, message: &str, data: Value) {
        self.0.insert(
            method,
            json!({
                "code": code,
                "message": message,
                "data": data
            }),
        );
    }
}

struct FakeCli {
    _dir: TempDir,
    script_path: PathBuf,
    capture_path: PathBuf,
    config_path: PathBuf,
    work_dir: PathBuf,
}

impl FakeCli {
    fn new(results: ResponseMap, errors: ErrorMap) -> Self {
        let dir = tempfile::tempdir().expect("create fake CLI temp dir");
        let script_path = dir.path().join("fake-rpc-cli.js");
        let capture_path = dir.path().join("captured-requests.json");
        let config_path = dir.path().join("responses.json");
        let work_dir = dir.path().join("cwd");
        std::fs::create_dir(&work_dir).expect("create fake CLI cwd");
        std::fs::write(&script_path, FAKE_STDIO_CLI_SCRIPT).expect("write fake CLI script");
        std::fs::write(
            &config_path,
            serde_json::to_vec(&json!({
                "results": results.0,
                "errors": errors.0,
            }))
            .expect("serialize fake CLI config"),
        )
        .expect("write fake CLI config");
        Self {
            _dir: dir,
            script_path,
            capture_path,
            config_path,
            work_dir,
        }
    }

    async fn start_client(&self) -> Client {
        Client::start(
            ClientOptions::new()
                .with_program(CliProgram::Path(PathBuf::from("node")))
                .with_prefix_args([self.script_path.as_os_str().to_owned()])
                .with_cwd(&self.work_dir)
                .with_extra_args([
                    "--capture-file".to_string(),
                    self.capture_path.to_string_lossy().into_owned(),
                    "--response-config".to_string(),
                    self.config_path.to_string_lossy().into_owned(),
                ])
                .with_github_token("offline-rpc-token")
                .with_use_logged_in_user(false)
                .with_transport(Transport::Stdio),
        )
        .await
        .expect("start fake CLI client")
    }

    async fn create_session(&self, client: &Client) -> github_copilot_sdk::session::Session {
        client
            .create_session(
                SessionConfig::default()
                    .with_session_id("rpc-surface-session")
                    .with_working_directory(&self.work_dir),
            )
            .await
            .expect("create fake session")
    }

    fn assert_target_methods(&self, expected: &[&str]) {
        let actual: Vec<_> = self
            .capture()
            .into_iter()
            .filter(|request| {
                !matches!(
                    request.method.as_str(),
                    "connect" | "runtime.shutdown" | "session.create" | "session.detach"
                )
            })
            .map(|request| request.method)
            .collect();
        assert_eq!(actual, expected);
    }

    fn assert_params(&self, method: &str, occurrence: usize, expected: Value) {
        let request = self
            .capture()
            .into_iter()
            .filter(|request| request.method == method)
            .nth(occurrence)
            .unwrap_or_else(|| panic!("missing occurrence {occurrence} of {method}"));
        assert_eq!(request.params, expected, "unexpected params for {method}");
    }

    fn capture(&self) -> Vec<CapturedRequest> {
        let bytes = std::fs::read(&self.capture_path).expect("read fake CLI capture");
        serde_json::from_slice(&bytes).expect("parse fake CLI capture")
    }
}

#[derive(serde::Deserialize)]
struct CapturedRequest {
    method: String,
    #[serde(default)]
    params: Value,
}

const FAKE_STDIO_CLI_SCRIPT: &str = r#"
const fs = require("fs");

function argument(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

const captureFile = argument("--capture-file");
const config = JSON.parse(fs.readFileSync(argument("--response-config"), "utf8"));
const requests = [];

function saveCapture() {
  fs.writeFileSync(captureFile, JSON.stringify(requests));
}

saveCapture();

let buffer = Buffer.alloc(0);
process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\r\n\r\n");
    if (headerEnd < 0) return;
    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\s*(\d+)/i.exec(header);
    if (!match) throw new Error("Missing Content-Length header");
    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) return;
    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handleMessage(JSON.parse(body));
  }
}

function handleMessage(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) return;

  requests.push({ method: message.method, params: message.params });
  saveCapture();

  if (message.method === "connect") {
    writeResult(message.id, { ok: true, protocolVersion: 3, version: "offline-fixture" });
    return;
  }
  if (message.method === "session.create") {
    writeResult(message.id, {
      sessionId: message.params.sessionId,
      workspacePath: null,
      capabilities: null,
    });
    return;
  }
  if (message.method === "session.detach") {
    writeResult(message.id, { success: true });
    return;
  }
  if (Object.prototype.hasOwnProperty.call(config.errors, message.method)) {
    writeError(message.id, config.errors[message.method]);
    return;
  }

  const result = Object.prototype.hasOwnProperty.call(config.results, message.method)
    ? config.results[message.method]
    : {};
  writeResult(message.id, result);
}

function writeResult(id, result) {
  writeMessage({ jsonrpc: "2.0", id, result });
}

function writeError(id, error) {
  writeMessage({ jsonrpc: "2.0", id, error });
}

function writeMessage(message) {
  const body = JSON.stringify(message);
  process.stdout.write(
    "Content-Length: " + Buffer.byteLength(body, "utf8") + "\r\n\r\n" + body,
  );
}
"#;
