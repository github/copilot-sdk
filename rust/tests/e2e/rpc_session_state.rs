use std::collections::HashMap;

use github_copilot_sdk::rpc::{
    AuthInfoType, HistoryTruncateRequest, LspInitializeRequest, MetadataContextInfoRequest,
    MetadataRecomputeContextTokensRequest, MetadataRecordContextChangeRequest,
    MetadataSetWorkingDirectoryRequest, MetadataSnapshotCurrentMode, ModeSetRequest,
    ModelSetReasoningEffortRequest, ModelSwitchToRequest, NameSetAutoRequest, NameSetRequest,
    PermissionsSetApproveAllRequest, PlanUpdateRequest, SessionSetCredentialsParams,
    SessionUpdateOptionsParams, SessionWorkingDirectoryContext,
    SessionWorkingDirectoryContextHostType, SessionsForkRequest, ShutdownRequest,
    TelemetrySetFeatureOverridesRequest, WorkspacesCreateFileRequest, WorkspacesReadFileRequest,
};
use github_copilot_sdk::session_events::{
    SessionContextChangedData, SessionEventType, SessionMode, SessionShutdownData,
    SessionTitleChangedData, SessionWorkspaceFileChangedData, ShutdownType,
    WorkspaceFileChangedOperation,
};
use serde_json::json;

use super::support::{
    assistant_message_content, wait_for_condition, wait_for_event, with_e2e_context,
};

const MODEL_ID: &str = "claude-sonnet-4.5";

#[tokio::test]
async fn should_call_session_rpc_model_getcurrent() {
    with_e2e_context(
        "rpc_session_state",
        "should_call_session_rpc_model_getcurrent",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_model(MODEL_ID))
                    .await
                    .expect("create session");

                let current = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model");
                assert_eq!(current.model_id.as_deref(), Some(MODEL_ID));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_session_rpc_model_switchto() {
    with_e2e_context(
        "rpc_session_state",
        "should_call_session_rpc_model_switchto",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_model(MODEL_ID))
                    .await
                    .expect("create session");

                let before = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model before switch");
                assert!(before.model_id.is_some(), "expected a model before switch");

                let switched = session
                    .rpc()
                    .model()
                    .switch_to(ModelSwitchToRequest {
                        model_id: "gpt-5.4".to_string(),
                        reasoning_effort: Some("high".to_string()),
                        model_capabilities: None,
                        reasoning_summary: None,
                        ..Default::default()
                    })
                    .await
                    .expect("switch model");
                assert_eq!(switched.model_id.as_deref(), Some("gpt-5.4"));

                let after = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model after switch");
                assert_eq!(after.model_id.as_deref(), Some("gpt-5.4"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_and_set_session_mode() {
    with_e2e_context(
        "rpc_session_state",
        "should_get_and_set_session_mode",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert_eq!(
                    session.rpc().mode().get().await.expect("get initial mode"),
                    SessionMode::Interactive
                );
                session
                    .rpc()
                    .mode()
                    .set(ModeSetRequest {
                        mode: SessionMode::Plan,
                    })
                    .await
                    .expect("set plan mode");
                assert_eq!(
                    session.rpc().mode().get().await.expect("get plan mode"),
                    SessionMode::Plan
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_shutdown_session_with_routine_type() {
    with_e2e_context(
        "rpc_session_state",
        "should_shutdown_session_with_routine_type",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let shutdown = wait_for_event(session.subscribe(), "session shutdown", |event| {
                    event.parsed_type() == SessionEventType::SessionShutdown
                });

                session
                    .rpc()
                    .shutdown(ShutdownRequest {
                        reason: Some("routine rust rpc test".to_string()),
                        r#type: Some(ShutdownType::Routine),
                    })
                    .await
                    .expect("shutdown session");
                let data = shutdown
                    .await
                    .typed_data::<SessionShutdownData>()
                    .expect("shutdown data");
                assert_eq!(data.shutdown_type, ShutdownType::Routine);

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_set_and_get_each_session_mode_value() {
    with_e2e_context(
        "rpc_session_state",
        "should_set_and_get_each_session_mode_value",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                for mode in [
                    SessionMode::Interactive,
                    SessionMode::Plan,
                    SessionMode::Autopilot,
                ] {
                    session
                        .rpc()
                        .mode()
                        .set(ModeSetRequest { mode: mode.clone() })
                        .await
                        .expect("set mode");
                    assert_eq!(session.rpc().mode().get().await.expect("get mode"), mode);
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_read_update_and_delete_plan() {
    with_e2e_context(
        "rpc_session_state",
        "should_read_update_and_delete_plan",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let initial = session
                    .rpc()
                    .plan()
                    .read()
                    .await
                    .expect("read initial plan");
                assert!(!initial.exists);
                assert!(initial.content.is_none());

                let content = "# Rust RPC plan\n- verify plan state";
                session
                    .rpc()
                    .plan()
                    .update(PlanUpdateRequest {
                        content: content.to_string(),
                    })
                    .await
                    .expect("update plan");
                let updated = session
                    .rpc()
                    .plan()
                    .read()
                    .await
                    .expect("read updated plan");
                assert!(updated.exists);
                assert_eq!(updated.content.as_deref(), Some(content));
                assert!(
                    updated
                        .path
                        .as_deref()
                        .is_some_and(|path| path.ends_with("plan.md"))
                );

                session.rpc().plan().delete().await.expect("delete plan");
                let deleted = session
                    .rpc()
                    .plan()
                    .read()
                    .await
                    .expect("read deleted plan");
                assert!(!deleted.exists);
                assert!(deleted.content.is_none());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_workspace_file_rpc_methods() {
    with_e2e_context(
        "rpc_session_state",
        "should_call_workspace_file_rpc_methods",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let before = session
                    .rpc()
                    .workspaces()
                    .list_files()
                    .await
                    .expect("list files before");
                assert!(before.files.is_empty());

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: "rpc-state-rust.txt".to_string(),
                        content: "workspace rpc content".to_string(),
                    })
                    .await
                    .expect("create workspace file");
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: "rpc-state-rust.txt".to_string(),
                    })
                    .await
                    .expect("read workspace file");
                assert_eq!(read.content, "workspace rpc content");
                let workspace = session
                    .rpc()
                    .workspaces()
                    .get_workspace()
                    .await
                    .expect("get workspace");
                let workspace = workspace.workspace.expect("workspace details");
                assert!(!workspace.id.trim().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reject_workspace_file_path_traversal() {
    with_e2e_context(
        "rpc_session_state",
        "should_reject_workspace_file_path_traversal",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                expect_err_contains(
                    session
                        .rpc()
                        .workspaces()
                        .create_file(WorkspacesCreateFileRequest {
                            path: "../escape.txt".to_string(),
                            content: "nope".to_string(),
                        })
                        .await,
                    "workspace files directory",
                );
                expect_err_contains(
                    session
                        .rpc()
                        .workspaces()
                        .read_file(WorkspacesReadFileRequest {
                            path: "../../escape.txt".to_string(),
                        })
                        .await,
                    "workspace files directory",
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_create_workspace_file_with_nested_path_auto_creating_dirs() {
    with_e2e_context(
        "rpc_session_state",
        "should_create_workspace_file_with_nested_path_auto_creating_dirs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let path = "nested/rust/path/file.txt";

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "nested content".to_string(),
                    })
                    .await
                    .expect("create nested workspace file");
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: path.to_string(),
                    })
                    .await
                    .expect("read nested workspace file");
                assert_eq!(read.content, "nested content");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_reading_nonexistent_workspace_file() {
    with_e2e_context(
        "rpc_session_state",
        "should_report_error_reading_nonexistent_workspace_file",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                assert!(
                    session
                        .rpc()
                        .workspaces()
                        .read_file(WorkspacesReadFileRequest {
                            path: "missing-rust-file.txt".to_string(),
                        })
                        .await
                        .is_err()
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_update_existing_workspace_file_with_update_operation() {
    with_e2e_context(
        "rpc_session_state",
        "should_update_existing_workspace_file_with_update_operation",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let path = "updated-rust.txt";

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "first".to_string(),
                    })
                    .await
                    .expect("create workspace file");
                let updated =
                    wait_for_event(session.subscribe(), "workspace file updated", |event| {
                        if event.parsed_type() != SessionEventType::SessionWorkspaceFileChanged {
                            return false;
                        }
                        event
                            .typed_data::<SessionWorkspaceFileChangedData>()
                            .is_some_and(|data| {
                                data.path == path
                                    && data.operation == WorkspaceFileChangedOperation::Update
                            })
                    });
                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "second".to_string(),
                    })
                    .await
                    .expect("update workspace file");
                updated.await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reject_empty_or_whitespace_session_name() {
    with_e2e_context(
        "rpc_session_state",
        "should_reject_empty_or_whitespace_session_name",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                for name in ["", "   \t"] {
                    expect_err_contains(
                        session
                            .rpc()
                            .name()
                            .set(NameSetRequest {
                                name: name.to_string(),
                            })
                            .await,
                        "empty",
                    );
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_title_changed_event_each_time_name_set_is_called() {
    with_e2e_context(
        "rpc_session_state",
        "should_emit_title_changed_event_each_time_name_set_is_called",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                for title in ["Rust RPC title", "Rust RPC title"] {
                    let changed = wait_for_event(session.subscribe(), "title changed", |event| {
                        event.parsed_type() == SessionEventType::SessionTitleChanged
                            && event
                                .typed_data::<SessionTitleChangedData>()
                                .is_some_and(|data| data.title == title)
                    });
                    session
                        .rpc()
                        .name()
                        .set(NameSetRequest {
                            name: title.to_string(),
                        })
                        .await
                        .expect("set title");
                    changed.await;
                }
                assert_eq!(
                    session
                        .rpc()
                        .name()
                        .get()
                        .await
                        .expect("get title")
                        .name
                        .as_deref(),
                    Some("Rust RPC title")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_get_and_set_session_metadata() {
    with_e2e_context(
        "rpc_session_state",
        "should_call_metadata_snapshot_setworkingdirectory_and_recordcontextchange",
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
                        name: "Rust metadata name".to_string(),
                    })
                    .await
                    .expect("set name");
                assert_eq!(
                    session
                        .rpc()
                        .name()
                        .get()
                        .await
                        .expect("get name")
                        .name
                        .as_deref(),
                    Some("Rust metadata name")
                );
                let sources = session
                    .rpc()
                    .instructions()
                    .get_sources()
                    .await
                    .expect("instruction sources");
                assert!(sources.sources.iter().all(|source| !source.id.is_empty()));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_metadata_snapshot_setworkingdirectory_and_recordcontextchange() {
    with_e2e_context(
        "rpc_session_state",
        "should_get_and_set_session_metadata",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let subdir = ctx.work_dir().join("metadata-cwd");
                std::fs::create_dir_all(&subdir).expect("create metadata cwd");

                let snapshot = session
                    .rpc()
                    .metadata()
                    .snapshot()
                    .await
                    .expect("metadata snapshot");
                assert_eq!(snapshot.session_id, session.id().clone());
                assert_eq!(
                    snapshot.current_mode,
                    MetadataSnapshotCurrentMode::Interactive
                );
                assert!(!snapshot.start_time.is_empty());
                assert!(!snapshot.modified_time.is_empty());

                let set = session
                    .rpc()
                    .metadata()
                    .set_working_directory(MetadataSetWorkingDirectoryRequest {
                        working_directory: subdir.display().to_string(),
                    })
                    .await
                    .expect("set working directory");
                assert_paths_equal(&set.working_directory, &subdir);

                let changed = wait_for_event(session.subscribe(), "context changed", |event| {
                    event.parsed_type() == SessionEventType::SessionContextChanged
                        && event
                            .typed_data::<SessionContextChangedData>()
                            .is_some_and(|data| {
                                data.repository.as_deref() == Some("github/copilot-sdk")
                            })
                });
                session
                    .rpc()
                    .metadata()
                    .record_context_change(MetadataRecordContextChangeRequest {
                        context: SessionWorkingDirectoryContext {
                            base_commit: None,
                            branch: Some("rust-rpc-e2e".to_string()),
                            cwd: subdir.display().to_string(),
                            git_root: Some(ctx.repo_root().display().to_string()),
                            head_commit: None,
                            host_type: Some(SessionWorkingDirectoryContextHostType::GitHub),
                            repository: Some("github/copilot-sdk".to_string()),
                            repository_host: Some("github.com".to_string()),
                        },
                    })
                    .await
                    .expect("record context change");
                let data = changed
                    .await
                    .typed_data::<SessionContextChangedData>()
                    .expect("context changed data");
                assert_paths_equal(&data.cwd, &subdir);
                assert_eq!(data.branch.as_deref(), Some("rust-rpc-e2e"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_update_options_and_initialize_session_services() {
    with_e2e_context(
        "rpc_session_state",
        "should_update_options_and_initialize_session_services",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let options = session
                    .rpc()
                    .options()
                    .update(SessionUpdateOptionsParams {
                        ask_user_disabled: Some(true),
                        available_tools: Some(vec!["view".to_string()]),
                        client_name: Some("rust-rpc-e2e".to_string()),
                        enable_streaming: Some(true),
                        model: Some(MODEL_ID.to_string()),
                        working_directory: Some(ctx.work_dir().display().to_string()),
                        ..SessionUpdateOptionsParams::default()
                    })
                    .await
                    .expect("update options");
                assert!(options.success);
                session
                    .rpc()
                    .lsp()
                    .initialize(LspInitializeRequest {
                        force: Some(true),
                        git_root: Some(ctx.repo_root().display().to_string()),
                        working_directory: Some(ctx.work_dir().display().to_string()),
                    })
                    .await
                    .expect("initialize lsp");
                session
                    .rpc()
                    .telemetry()
                    .set_feature_overrides(TelemetrySetFeatureOverridesRequest {
                        features: HashMap::from([(
                            "rust-rpc-e2e".to_string(),
                            "enabled".to_string(),
                        )]),
                    })
                    .await
                    .expect("set telemetry overrides");
                session
                    .rpc()
                    .tools()
                    .initialize_and_validate()
                    .await
                    .expect("initialize tools");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_set_reasoningeffort_and_auto_name() {
    with_e2e_context(
        "rpc_session_state",
        "should_set_reasoningeffort_and_auto_name",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let effort = session
                    .rpc()
                    .model()
                    .set_reasoning_effort(ModelSetReasoningEffortRequest {
                        reasoning_effort: "none".to_string(),
                    })
                    .await
                    .expect("set reasoning effort");
                assert_eq!(effort.reasoning_effort, "none");
                let auto = session
                    .rpc()
                    .name()
                    .set_auto(NameSetAutoRequest {
                        summary: "Rust auto title".to_string(),
                    })
                    .await
                    .expect("set auto name");
                assert!(auto.applied);
                session
                    .rpc()
                    .name()
                    .set(NameSetRequest {
                        name: "Explicit Rust title".to_string(),
                    })
                    .await
                    .expect("set explicit name");
                let not_applied = session
                    .rpc()
                    .name()
                    .set_auto(NameSetAutoRequest {
                        summary: "Ignored auto title".to_string(),
                    })
                    .await
                    .expect("set ignored auto name");
                assert!(!not_applied.applied);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_set_auth_credentials() {
    with_e2e_context("rpc_session_state", "should_set_auth_credentials", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let token = "rpc-session-auth-token";
            ctx.set_copilot_user_by_token_with_login(token, "rpc-session-user");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let set = session
                .rpc()
                .auth()
                .set_credentials(SessionSetCredentialsParams {
                    credentials: Some(json!({
                        "type": "user",
                        "host": "github.com",
                        "login": "rpc-session-user"
                    })),
                })
                .await
                .expect("set credentials");
            assert!(set.success);
            let status = session
                .rpc()
                .auth()
                .get_status()
                .await
                .expect("auth status");
            assert!(status.is_authenticated);
            assert_eq!(status.auth_type, Some(AuthInfoType::User));
            assert_eq!(status.host.as_deref(), Some("github.com"));
            assert_eq!(status.login.as_deref(), Some("rpc-session-user"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_fork_session_with_persisted_messages() {
    with_e2e_context(
        "rpc_session_state",
        "should_fork_session_with_persisted_messages",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let answer = session
                    .send_and_wait("Say FORK_SOURCE_ALPHA exactly.")
                    .await
                    .expect("send")
                    .expect("assistant response");
                assert!(assistant_message_content(&answer).contains("FORK_SOURCE_ALPHA"));

                let fork = client
                    .rpc()
                    .sessions()
                    .fork(SessionsForkRequest {
                        session_id: session.id().clone(),
                        to_event_id: None,
                        name: Some("Rust fork".to_string()),
                    })
                    .await
                    .expect("fork session");
                assert_ne!(fork.session_id, session.id().clone());
                assert_eq!(fork.name.as_deref(), Some("Rust fork"));
                let forked = client
                    .resume_session(
                        github_copilot_sdk::ResumeSessionConfig::new(fork.session_id.clone())
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                    )
                    .await
                    .expect("resume fork");
                assert!(
                    forked
                        .get_events()
                        .await
                        .expect("fork events")
                        .iter()
                        .any(|event| assistant_message_content_if_present(event)
                            .is_some_and(|content| content.contains("FORK_SOURCE_ALPHA")))
                );

                forked.disconnect().await.expect("disconnect fork");
                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_error_when_forking_session_to_unknown_event_id() {
    with_e2e_context(
        "rpc_session_state",
        "should_report_error_when_forking_session_to_unknown_event_id",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let err = client
                    .rpc()
                    .sessions()
                    .fork(SessionsForkRequest {
                        session_id: session.id().clone(),
                        to_event_id: Some("missing-event-id".to_string()),
                        name: None,
                    })
                    .await
                    .expect_err("unknown boundary should fail");
                let message = err.to_string();
                assert!(message.contains("missing-event-id") || message.contains("not found"));
                assert!(!message.contains("Unhandled method"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_call_session_usage_and_permission_rpcs() {
    with_e2e_context(
        "rpc_session_state",
        "should_call_session_usage_and_permission_rpcs",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let metrics = session.rpc().usage().get_metrics().await.expect("usage");
                assert!(!metrics.session_start_time.is_empty());
                assert_eq!(metrics.total_user_requests, 0);
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .set_approve_all(PermissionsSetApproveAllRequest {
                            enabled: true,
                            source: None,
                        })
                        .await
                        .expect("enable approve all")
                        .success
                );
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .reset_session_approvals()
                        .await
                        .expect("reset approvals")
                        .success
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_report_implemented_errors_for_unsupported_session_rpc_paths() {
    with_e2e_context(
        "rpc_session_state",
        "should_report_implemented_errors_for_unsupported_session_rpc_paths",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let truncate = session
                    .rpc()
                    .history()
                    .truncate(HistoryTruncateRequest {
                        event_id: "missing-event-id".to_string(),
                    })
                    .await
                    .expect_err("truncate missing event should fail");
                assert!(
                    !truncate
                        .to_string()
                        .contains("Unhandled method session.history.truncate")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_compact_session_history_after_messages() {
    with_e2e_context(
        "rpc_session_state",
        "should_compact_session_history_after_messages",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_model(MODEL_ID))
                    .await
                    .expect("create session");

                assert!(
                    !session
                        .rpc()
                        .metadata()
                        .is_processing()
                        .await
                        .expect("processing before send")
                        .processing
                );
                session
                    .send("Reply with exactly: RUST_CONTEXT_INFO")
                    .await
                    .expect("send");
                wait_for_condition("session processing started", || async {
                    session
                        .rpc()
                        .metadata()
                        .is_processing()
                        .await
                        .expect("processing poll")
                        .processing
                })
                .await;
                wait_for_condition("session processing completed", || async {
                    !session
                        .rpc()
                        .metadata()
                        .is_processing()
                        .await
                        .expect("processing poll")
                        .processing
                })
                .await;
                let context = session
                    .rpc()
                    .metadata()
                    .context_info(MetadataContextInfoRequest {
                        prompt_token_limit: 200_000,
                        output_token_limit: 4096,
                        selected_model: Some(MODEL_ID.to_string()),
                    })
                    .await
                    .expect("context info");
                let context_info = context.context_info.expect("context info");
                assert_eq!(context_info.model_name, MODEL_ID);
                let recomputed = session
                    .rpc()
                    .metadata()
                    .recompute_context_tokens(MetadataRecomputeContextTokensRequest {
                        model_id: MODEL_ID.to_string(),
                    })
                    .await
                    .expect("recompute context tokens");
                assert!(recomputed.total_tokens >= recomputed.messages_token_count);
                assert!(recomputed.total_tokens >= recomputed.system_token_count);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn expect_err_contains<T>(result: Result<T, github_copilot_sdk::Error>, expected: &str) {
    let err = match result {
        Ok(_) => panic!("expected error containing {expected:?}"),
        Err(err) => err,
    };
    assert!(
        err.to_string()
            .to_ascii_lowercase()
            .contains(&expected.to_ascii_lowercase()),
        "expected error to contain {expected:?}, got {err}"
    );
}

fn assert_paths_equal(actual: &str, expected: &std::path::Path) {
    let actual = std::path::Path::new(actual);
    assert_eq!(
        std::fs::canonicalize(actual).unwrap_or_else(|_| actual.to_path_buf()),
        std::fs::canonicalize(expected).unwrap_or_else(|_| expected.to_path_buf())
    );
}

fn assistant_message_content_if_present(
    event: &github_copilot_sdk::SessionEvent,
) -> Option<String> {
    if event.parsed_type() == SessionEventType::AssistantMessage {
        Some(assistant_message_content(event))
    } else {
        None
    }
}
