use github_copilot_sdk::generated::api_types::{
    ModeSetRequest, NameSetRequest, PermissionsSetApproveAllRequest, PlanUpdateRequest,
    SessionMode, ShellExecRequest, WorkspacesCreateFileRequest, WorkspacesReadFileRequest,
};

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn shell_exec_with_zero_timeout_does_not_kill_long_running_command() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "shell_exec_with_zero_timeout_does_not_kill_long_running_command",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let marker_path = ctx.work_dir().join("shell-zero-timeout-marker.txt");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .shell()
                    .exec(ShellExecRequest {
                        command: delayed_marker_command(&marker_path),
                        cwd: Some(ctx.work_dir().display().to_string()),
                        timeout: Some(0),
                    })
                    .await
                    .expect("execute shell command");

                assert!(!result.process_id.trim().is_empty());
                wait_for_condition("zero-timeout shell marker", || async {
                    marker_path.exists()
                })
                .await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn workspaces_create_file_with_empty_content_round_trips() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "workspaces_create_file_with_empty_content_round_trips",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let path = "empty-rust.txt";

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: String::new(),
                    })
                    .await
                    .expect("create file");
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: path.to_string(),
                    })
                    .await
                    .expect("read file");
                assert_eq!(read.content, "");
                let listed = session
                    .rpc()
                    .workspaces()
                    .list_files()
                    .await
                    .expect("list files");
                assert!(listed.files.iter().any(|file| file == path));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn workspaces_create_file_with_unicode_content_round_trips() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "workspaces_create_file_with_unicode_content_round_trips",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let path = "unicode-rust.txt";
                let payload = "Hello, 世界! 🚀✨ Привет\u{0000}end";

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: payload.to_string(),
                    })
                    .await
                    .expect("create file");
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: path.to_string(),
                    })
                    .await
                    .expect("read file");
                assert_eq!(read.content, payload);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn workspaces_create_file_with_large_content_round_trips() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "workspaces_create_file_with_large_content_round_trips",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let path = "large-rust.txt";
                let payload: String = (0..256 * 1024)
                    .map(|i| (b'a' + (i % 26) as u8) as char)
                    .collect();

                session
                    .rpc()
                    .workspaces()
                    .create_file(WorkspacesCreateFileRequest {
                        path: path.to_string(),
                        content: payload.clone(),
                    })
                    .await
                    .expect("create file");
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: path.to_string(),
                    })
                    .await
                    .expect("read file");
                assert_eq!(read.content.len(), payload.len());
                assert_eq!(read.content, payload);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn plan_update_with_empty_content_then_read_returns_empty() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "plan_update_with_empty_content_then_read_returns_empty",
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
                    .plan()
                    .update(PlanUpdateRequest {
                        content: String::new(),
                    })
                    .await
                    .expect("update plan");
                let read = session.rpc().plan().read().await.expect("read plan");
                assert_eq!(read.content.as_deref(), Some(""));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn plan_delete_when_none_exists_is_idempotent() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "plan_delete_when_none_exists_is_idempotent",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session.rpc().plan().delete().await.expect("delete plan");
                session
                    .rpc()
                    .plan()
                    .delete()
                    .await
                    .expect("delete plan again");
                let read = session.rpc().plan().read().await.expect("read plan");
                assert!(read.content.as_deref().unwrap_or_default().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn mode_set_to_same_value_multiple_times_stays_stable() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "mode_set_to_same_value_multiple_times_stays_stable",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                for _ in 0..3 {
                    session
                        .rpc()
                        .mode()
                        .set(ModeSetRequest {
                            mode: SessionMode::Plan,
                        })
                        .await
                        .expect("set mode");
                }
                assert_eq!(
                    session.rpc().mode().get().await.expect("get mode"),
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
async fn name_set_with_unicode_round_trips() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "name_set_with_unicode_round_trips",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let name = "セッション 名前 ☕ – test";

                session
                    .rpc()
                    .name()
                    .set(NameSetRequest {
                        name: name.to_string(),
                    })
                    .await
                    .expect("set name");
                let read = session.rpc().name().get().await.expect("get name");
                assert_eq!(read.name.as_deref(), Some(name));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn usage_get_metrics_on_fresh_session_returns_zero_tokens() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "usage_get_metrics_on_fresh_session_returns_zero_tokens",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let metrics = session.rpc().usage().get_metrics().await.expect("metrics");
                assert_eq!(metrics.last_call_input_tokens, 0);
                assert_eq!(metrics.last_call_output_tokens, 0);
                assert_eq!(metrics.total_user_requests, 0);
                assert!(metrics.session_start_time > 0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn permissions_reset_session_approvals_on_fresh_session_is_noop() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "permissions_reset_session_approvals_on_fresh_session_is_noop",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .permissions()
                    .reset_session_approvals()
                    .await
                    .expect("reset approvals");
                assert!(result.success);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn permissions_set_approve_all_toggle_round_trips() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "permissions_set_approve_all_toggle_round_trips",
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
                        .permissions()
                        .set_approve_all(PermissionsSetApproveAllRequest { enabled: true })
                        .await
                        .expect("enable approve all")
                        .success
                );
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .set_approve_all(PermissionsSetApproveAllRequest { enabled: true })
                        .await
                        .expect("enable approve all again")
                        .success
                );
                assert!(
                    session
                        .rpc()
                        .permissions()
                        .set_approve_all(PermissionsSetApproveAllRequest { enabled: false })
                        .await
                        .expect("disable approve all")
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
async fn workspaces_createfile_then_listfiles_returns_sorted_or_stable_order() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "workspaces_createfile_then_listfiles_returns_sorted_or_stable_order",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                for path in ["b-rust.txt", "a-rust.txt", "c-rust.txt"] {
                    session
                        .rpc()
                        .workspaces()
                        .create_file(WorkspacesCreateFileRequest {
                            path: path.to_string(),
                            content: path.to_string(),
                        })
                        .await
                        .expect("create workspace file");
                }

                let first = session
                    .rpc()
                    .workspaces()
                    .list_files()
                    .await
                    .expect("list files");
                let second = session
                    .rpc()
                    .workspaces()
                    .list_files()
                    .await
                    .expect("list files again");
                assert_eq!(first.files, second.files);
                for expected in ["a-rust.txt", "b-rust.txt", "c-rust.txt"] {
                    assert!(first.files.iter().any(|file| file == expected));
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn workspaces_getworkspace_returns_stable_result_across_calls() {
    with_e2e_context(
        "rpc_additional_edge_cases",
        "workspaces_getworkspace_returns_stable_result_across_calls",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let first = session
                    .rpc()
                    .workspaces()
                    .get_workspace()
                    .await
                    .expect("get workspace");
                let second = session
                    .rpc()
                    .workspaces()
                    .get_workspace()
                    .await
                    .expect("get workspace again");

                assert_eq!(
                    first.workspace.as_ref().map(|workspace| &workspace.id),
                    second.workspace.as_ref().map(|workspace| &workspace.id)
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[cfg(windows)]
fn delayed_marker_command(marker_path: &std::path::Path) -> String {
    format!(
        "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 2; Set-Content -LiteralPath '{}' -Value done\"",
        marker_path.display()
    )
}

#[cfg(not(windows))]
fn delayed_marker_command(marker_path: &std::path::Path) -> String {
    format!(
        "sh -c \"sleep 2; printf done > '{}'\"",
        marker_path.display()
    )
}
