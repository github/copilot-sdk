use std::path::Path;
use std::process::Command;

use github_copilot_sdk::rpc::{
    WorkspaceDiffFileChangeType, WorkspaceDiffMode, WorkspacesDiffRequest,
    WorkspacesReadCheckpointRequest, WorkspacesReadFileRequest, WorkspacesSaveLargePasteRequest,
};

#[tokio::test]
async fn should_list_no_checkpoints_for_fresh_session() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_workspace_checkpoints",
        "should_list_no_checkpoints_for_fresh_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let checkpoints = session
                    .rpc()
                    .workspaces()
                    .list_checkpoints()
                    .await
                    .expect("list checkpoints");
                assert!(checkpoints.checkpoints.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_null_or_empty_content_for_unknown_checkpoint() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_workspace_checkpoints",
        "should_return_null_or_empty_content_for_unknown_checkpoint",
        |ctx| {
            Box::pin(async move {
                // Count this invocation for shared-group teardown even when the
                // native runtime cannot represent the checkpoint sentinel.
                if super::support::skip_inprocess("readCheckpoint decodes the id as u32 in-process")
                {
                    return;
                }
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let checkpoint = session
                    .rpc()
                    .workspaces()
                    .read_checkpoint(WorkspacesReadCheckpointRequest { number: i64::MAX })
                    .await
                    .expect("read missing checkpoint");
                assert!(checkpoint.content.as_deref().unwrap_or_default().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_return_typed_workspace_diff_result() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_workspace_checkpoints",
        "should_return_typed_workspace_diff_result",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                init_git_repository(ctx.work_dir());
                let changed_path = ctx.work_dir().join("rust-workspace-diff.txt");
                std::fs::write(&changed_path, "diff content\n").expect("write diff file");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let diff = session
                    .rpc()
                    .workspaces()
                    .diff(WorkspacesDiffRequest {
                        mode: WorkspaceDiffMode::Unstaged,
                        ..Default::default()
                    })
                    .await
                    .expect("workspace diff");
                assert_eq!(diff.requested_mode, WorkspaceDiffMode::Unstaged);
                assert!(matches!(
                    diff.mode,
                    WorkspaceDiffMode::Unstaged | WorkspaceDiffMode::Branch
                ));
                if let Some(change) = diff.changes.iter().find(|change| {
                    normalize_path(&change.path).ends_with("rust-workspace-diff.txt")
                }) {
                    assert_eq!(change.change_type, WorkspaceDiffFileChangeType::Added);
                    assert!(change.diff.contains("diff content") || change.diff.is_empty());
                } else {
                    assert!(
                        diff.changes.is_empty(),
                        "unexpected diff changes: {:?}",
                        diff.changes
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
async fn should_save_large_paste_and_expose_readable_content() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_workspace_checkpoints",
        "should_save_large_paste_and_expose_readable_content",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let content = "large paste rust content\n".repeat(512);

                let saved = session
                    .rpc()
                    .workspaces()
                    .save_large_paste(WorkspacesSaveLargePasteRequest {
                        content: content.clone(),
                    })
                    .await
                    .expect("save large paste")
                    .saved
                    .expect("saved paste descriptor");
                assert!(saved.filename.ends_with(".txt"));
                assert_eq!(saved.size_bytes, content.len() as i64);
                assert_eq!(
                    std::fs::read_to_string(&saved.file_path).expect("read saved paste"),
                    content
                );
                let read = session
                    .rpc()
                    .workspaces()
                    .read_file(WorkspacesReadFileRequest {
                        path: saved.filename,
                    })
                    .await
                    .expect("read saved paste through workspace");
                assert_eq!(read.content, content);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn normalize_path(path: &str) -> String {
    path.replace('\\', "/")
}

fn init_git_repository(path: &Path) {
    let status = Command::new("git")
        .arg("init")
        .arg("--quiet")
        .current_dir(path)
        .status()
        .expect("run git init");
    assert!(status.success(), "git init should succeed");
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("rpc_workspace_checkpoints", 4);
