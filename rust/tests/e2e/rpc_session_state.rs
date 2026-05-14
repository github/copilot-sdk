use github_copilot_sdk::generated::api_types::{
    HistoryTruncateRequest, McpOauthLoginRequest, ModeSetRequest, ModelSwitchToRequest,
    NameSetRequest, PermissionsSetApproveAllRequest, PlanUpdateRequest, SessionMode,
    SessionsForkRequest, WorkspacesCreateFileRequest, WorkspacesReadFileRequest,
};
use github_copilot_sdk::generated::session_events::{
    AssistantMessageData, SessionEventType, SessionTitleChangedData,
    SessionWorkspaceFileChangedData, UserMessageData, WorkspaceFileChangedOperation,
};

use super::support::{assistant_message_content, wait_for_event, with_e2e_context};

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
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_model("claude-sonnet-4.5"),
                    )
                    .await
                    .expect("create session");

                let current = session
                    .rpc()
                    .model()
                    .get_current()
                    .await
                    .expect("get current model");
                assert_eq!(current.model_id.as_deref(), Some("claude-sonnet-4.5"));

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
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_model("claude-sonnet-4.5"),
                    )
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .model()
                    .switch_to(ModelSwitchToRequest {
                        model_id: "gpt-4.1".to_string(),
                        reasoning_effort: Some("high".to_string()),
                        model_capabilities: None,
                    })
                    .await
                    .expect("switch model");
                assert_eq!(result.model_id.as_deref(), Some("gpt-4.1"));

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
                    session.rpc().mode().get().await.expect("get mode"),
                    SessionMode::Interactive
                );
                session
                    .rpc()
                    .mode()
                    .set(ModeSetRequest {
                        mode: SessionMode::Plan,
                    })
                    .await
                    .expect("set plan");
                assert_eq!(
                    session.rpc().mode().get().await.expect("get mode"),
                    SessionMode::Plan
                );
                session
                    .rpc()
                    .mode()
                    .set(ModeSetRequest {
                        mode: SessionMode::Interactive,
                    })
                    .await
                    .expect("set interactive");
                assert_eq!(
                    session.rpc().mode().get().await.expect("get mode"),
                    SessionMode::Interactive
                );

                session.disconnect().await.expect("disconnect session");
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
                let content = "# Test Plan\n\n- Step 1\n- Step 2";

                let initial = session.rpc().plan().read().await.expect("read initial");
                assert!(!initial.exists);
                assert!(initial.content.is_none());
                session
                    .rpc()
                    .plan()
                    .update(PlanUpdateRequest {
                        content: content.to_string(),
                    })
                    .await
                    .expect("update plan");
                let updated = session.rpc().plan().read().await.expect("read updated");
                assert!(updated.exists);
                assert_eq!(updated.content.as_deref(), Some(content));
                session.rpc().plan().delete().await.expect("delete plan");
                let deleted = session.rpc().plan().read().await.expect("read deleted");
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

                let initial = session
                    .rpc()
                    .workspaces()
                    .list_files()
                    .await
                    .expect("list files");
                assert!(initial.files.is_empty());
                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: "test.txt".to_string(),
                        content: "Hello, workspace!".to_string(),
                    })
                    .await
                    .expect("create file");
                let listed = session
                    .rpc()
                    .workspaces()
                    .list_files()
                    .await
                    .expect("list files");
                assert!(listed.files.iter().any(|file| file == "test.txt"));
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: "test.txt".to_string(),
                    })
                    .await
                    .expect("read file");
                assert_eq!(read.content, "Hello, workspace!");
                let workspace = session
                    .rpc()
                    .workspaces()
                    .get_workspace()
                    .await
                    .expect("get workspace");
                assert!(workspace.workspace.is_some());

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

                let err = session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: "../escaped.txt".to_string(),
                        content: "outside".to_string(),
                    })
                    .await
                    .expect_err("path traversal should fail");
                assert!(err.to_string().contains("workspace"));

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
                let path = "nested-rust/subdir/file.txt";

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "nested content".to_string(),
                    })
                    .await
                    .expect("create nested file");
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: path.to_string(),
                    })
                    .await
                    .expect("read nested file");
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
                            path: "never-exists-rust.txt".to_string(),
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
                let path = "reused-rust.txt";
                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "v1".to_string(),
                    })
                    .await
                    .expect("create file");

                let update_event =
                    wait_for_event(session.subscribe(), "workspace update event", |event| {
                        if event.parsed_type() != SessionEventType::SessionWorkspaceFileChanged {
                            return false;
                        }
                        let data = event
                            .typed_data::<SessionWorkspaceFileChangedData>()
                            .expect("workspace file changed data");
                        data.path == path && data.operation == WorkspaceFileChangedOperation::Update
                    });
                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: "v2".to_string(),
                    })
                    .await
                    .expect("update file");
                update_event.await;
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: path.to_string(),
                    })
                    .await
                    .expect("read updated");
                assert_eq!(read.content, "v2");

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

                for name in ["", "   ", "\t\n  \r"] {
                    let err = session
                        .rpc()
                        .name()
                        .set(NameSetRequest {
                            name: name.to_string(),
                        })
                        .await
                        .expect_err("empty name should fail");
                    assert!(err.to_string().to_lowercase().contains("empty"));
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

                for title in ["Title-A-Rust", "Title-B-Rust"] {
                    let event = wait_for_event(session.subscribe(), "title changed", |event| {
                        if event.parsed_type() != SessionEventType::SessionTitleChanged {
                            return false;
                        }
                        event
                            .typed_data::<SessionTitleChangedData>()
                            .expect("title data")
                            .title
                            == title
                    });
                    session
                        .rpc()
                        .name()
                        .set(NameSetRequest {
                            name: title.to_string(),
                        })
                        .await
                        .expect("set name");
                    event.await;
                }

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
        "should_get_and_set_session_metadata",
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
                        name: "SDK test session".to_string(),
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
                    Some("SDK test session")
                );
                let sources = session
                    .rpc()
                    .instructions()
                    .get_sources()
                    .await
                    .expect("get instruction sources");
                assert!(sources.sources.is_empty() || !sources.sources.is_empty());

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

                let metrics = session.rpc().usage().get_metrics().await.expect("metrics");
                assert!(metrics.session_start_time > 0);
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .set_approve_all(PermissionsSetApproveAllRequest { enabled: true })
                        .await
                        .expect("set approve all")
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
                session
                    .rpc()
                    .permissions()
                    .set_approve_all(PermissionsSetApproveAllRequest { enabled: false })
                    .await
                    .expect("disable approve all");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
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
                    .expect("send source")
                    .expect("source answer");
                assert!(assistant_message_content(&answer).contains("FORK_SOURCE_ALPHA"));
                let fork = client
                    .rpc()
                    .sessions()
                    .fork(SessionsForkRequest {
                        name: None,
                        session_id: session.id().clone(),
                        to_event_id: None,
                    })
                    .await
                    .expect("fork session");
                assert_ne!(fork.session_id, *session.id());
                let forked = client
                    .resume_session(
                        github_copilot_sdk::ResumeSessionConfig::new(fork.session_id)
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            )),
                    )
                    .await
                    .expect("resume fork");
                let forked_messages = forked.get_messages().await.expect("forked messages");
                assert!(contains_user_message(
                    &forked_messages,
                    "Say FORK_SOURCE_ALPHA exactly."
                ));
                assert!(contains_assistant_message(
                    &forked_messages,
                    "FORK_SOURCE_ALPHA"
                ));

                let fork_answer = forked
                    .send_and_wait("Now say FORK_CHILD_BETA exactly.")
                    .await
                    .expect("send fork")
                    .expect("fork answer");
                assert!(assistant_message_content(&fork_answer).contains("FORK_CHILD_BETA"));
                let source_after = session.get_messages().await.expect("source messages");
                assert!(!contains_user_message(
                    &source_after,
                    "Now say FORK_CHILD_BETA exactly."
                ));

                forked.disconnect().await.expect("disconnect fork");
                session.disconnect().await.expect("disconnect source");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_forking_session_without_persisted_events() {
    with_e2e_context(
        "rpc_session_state",
        "should_handle_forking_session_without_persisted_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                match client
                    .rpc()
                    .sessions()
                    .fork(SessionsForkRequest {
                        name: None,
                        session_id: session.id().clone(),
                        to_event_id: None,
                    })
                    .await
                {
                    Ok(fork) => {
                        assert!(!fork.session_id.as_str().trim().is_empty());
                        assert_ne!(fork.session_id, *session.id());
                        let forked = client
                            .resume_session(
                                github_copilot_sdk::ResumeSessionConfig::new(fork.session_id)
                                    .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                                    .with_handler(std::sync::Arc::new(
                                        github_copilot_sdk::handler::ApproveAllHandler,
                                    )),
                            )
                            .await
                            .expect("resume fork");
                        assert!(
                            !forked
                                .get_messages()
                                .await
                                .expect("forked messages")
                                .iter()
                                .any(|event| {
                                    matches!(
                                        event.parsed_type(),
                                        SessionEventType::UserMessage
                                            | SessionEventType::AssistantMessage
                                    )
                                })
                        );
                        forked.disconnect().await.expect("disconnect fork");
                    }
                    Err(err) => {
                        let message = err.to_string();
                        assert!(
                            message.contains("not found or has no persisted events"),
                            "unexpected sessions.fork error: {message}"
                        );
                        assert!(
                            !message.contains("Unhandled method sessions.fork"),
                            "expected implemented error for sessions.fork, got {message}"
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
async fn should_fork_session_to_event_id_excluding_boundary_event() {
    with_e2e_context(
        "rpc_session_state",
        "should_fork_session_to_event_id_excluding_boundary_event",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Say FORK_BOUNDARY_FIRST exactly.")
                    .await
                    .expect("send first");
                session
                    .send_and_wait("Say FORK_BOUNDARY_SECOND exactly.")
                    .await
                    .expect("send second");
                let source_events = session.get_messages().await.expect("messages");
                let boundary_id = source_events
                    .iter()
                    .find(|event| {
                        event.parsed_type() == SessionEventType::UserMessage
                            && event.typed_data::<UserMessageData>().is_some_and(|data| {
                                data.content == "Say FORK_BOUNDARY_SECOND exactly."
                            })
                    })
                    .expect("second user message")
                    .id
                    .clone();
                let fork = client
                    .rpc()
                    .sessions()
                    .fork(SessionsForkRequest {
                        name: None,
                        session_id: session.id().clone(),
                        to_event_id: Some(boundary_id.clone()),
                    })
                    .await
                    .expect("fork to boundary");
                let forked = client
                    .resume_session(
                        github_copilot_sdk::ResumeSessionConfig::new(fork.session_id)
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            )),
                    )
                    .await
                    .expect("resume fork");
                let forked_events = forked.get_messages().await.expect("forked messages");
                assert!(contains_user_message(
                    &forked_events,
                    "Say FORK_BOUNDARY_FIRST exactly."
                ));
                assert!(!forked_events.iter().any(|event| event.id == boundary_id));
                assert!(!contains_user_message(
                    &forked_events,
                    "Say FORK_BOUNDARY_SECOND exactly."
                ));

                forked.disconnect().await.expect("disconnect fork");
                session.disconnect().await.expect("disconnect source");
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
                session
                    .send_and_wait("Say FORK_UNKNOWN_EVENT_OK exactly.")
                    .await
                    .expect("send source");
                let bogus_event_id = "00000000-0000-0000-0000-000000000000";

                assert_implemented_error(
                    client
                        .rpc()
                        .sessions()
                        .fork(SessionsForkRequest {
                            name: None,
                            session_id: session.id().clone(),
                            to_event_id: Some(bogus_event_id.to_string()),
                        })
                        .await,
                    "sessions.fork",
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

                assert_implemented_error(
                    session
                        .rpc()
                        .history()
                        .truncate(HistoryTruncateRequest {
                            event_id: "missing-event".to_string(),
                        })
                        .await,
                    "session.history.truncate",
                );
                assert_implemented_error(
                    session
                        .rpc()
                        .mcp()
                        .oauth()
                        .login(McpOauthLoginRequest {
                            server_name: "missing-server".to_string(),
                            callback_success_message: None,
                            client_name: None,
                            force_reauth: None,
                        })
                        .await,
                    "session.mcp.oauth.login",
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
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("What is 2+2?")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains('4'));

                let compact = session
                    .rpc()
                    .history()
                    .compact()
                    .await
                    .expect("compact history");
                assert!(compact.success);
                assert!(compact.messages_removed >= 0);
                session.rpc().name().get().await.expect("name still works");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn contains_user_message(events: &[github_copilot_sdk::SessionEvent], expected: &str) -> bool {
    events.iter().any(|event| {
        event.parsed_type() == SessionEventType::UserMessage
            && event
                .typed_data::<UserMessageData>()
                .is_some_and(|data| data.content == expected)
    })
}

fn contains_assistant_message(events: &[github_copilot_sdk::SessionEvent], expected: &str) -> bool {
    events.iter().any(|event| {
        event.parsed_type() == SessionEventType::AssistantMessage
            && event
                .typed_data::<AssistantMessageData>()
                .is_some_and(|data| data.content.contains(expected))
    })
}

fn assert_implemented_error<T>(result: Result<T, github_copilot_sdk::Error>, method: &str) {
    let err = match result {
        Ok(_) => panic!("RPC should fail"),
        Err(err) => err,
    };
    let message = err.to_string();
    assert!(
        !message.contains(&format!("Unhandled method {method}")),
        "expected implemented error for {method}, got {message}"
    );
}
