use std::path::Path;
use std::process::Command;
use std::sync::Arc;

use github_copilot_sdk::ResumeSessionConfig;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::rpc::{
    SessionsSaveRequest, WorkspaceDiffFileChangeType, WorkspaceDiffMode, WorkspacesDiffRequest,
    WorkspacesReadCheckpointRequest, WorkspacesReadFileRequest, WorkspacesSaveLargePasteRequest,
    WorkspacesWorkspaceDetailsHostType,
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
    if super::support::skip_shared_e2e_inprocess(
        &E2E,
        "readCheckpoint decodes the id as u32 in-process",
    )
    .await
    {
        return;
    }
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_workspace_checkpoints",
        "should_return_null_or_empty_content_for_unknown_checkpoint",
        |ctx| {
            Box::pin(async move {
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

#[tokio::test]
async fn should_record_git_context_in_a_new_session_workspace() {
    // Reuse session.rs::should_have_stateful_conversation's cassette to create persisted history;
    // keep the first-turn prompt below aligned with that owner.
    super::support::with_e2e_context("session", "should_have_stateful_conversation", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            init_git_repository(ctx.work_dir());
            run_git(
                ctx.work_dir(),
                &[
                    "remote",
                    "add",
                    "origin",
                    "https://github.com/test-org/test-repo.git",
                ],
            );
            run_git(
                ctx.work_dir(),
                &["commit", "--quiet", "--allow-empty", "-m", "initial"],
            );
            run_git(
                ctx.work_dir(),
                &["checkout", "--quiet", "-b", "feature/test-branch"],
            );
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_client_name("rust-e2e-client"),
                )
                .await
                .expect("create session");

            let workspace = session
                .rpc()
                .workspaces()
                .get_workspace()
                .await
                .expect("get workspace")
                .workspace
                .expect("workspace");

            assert_eq!(workspace.repository.as_deref(), Some("test-org/test-repo"));
            assert_eq!(workspace.branch.as_deref(), Some("feature/test-branch"));
            // Git may expand Windows 8.3 paths or resolve platform directory aliases.
            // Compare directory identities, not the spellings returned by Git and the host.
            let work_dir = ctx.work_dir().canonicalize().expect("canonical work dir");
            let git_root = Path::new(workspace.git_root.as_deref().expect("git root"))
                .canonicalize()
                .expect("canonical git root");
            assert_eq!(git_root, work_dir);
            assert_eq!(
                Path::new(workspace.cwd.as_deref().expect("workspace cwd"))
                    .canonicalize()
                    .expect("canonical workspace cwd"),
                work_dir
            );
            assert_eq!(
                workspace.host_type,
                Some(WorkspacesWorkspaceDetailsHostType::GitHub)
            );
            assert_eq!(workspace.client_name.as_deref(), Some("rust-e2e-client"));

            // The recorded context survives a resume rather than being dropped
            // or re-derived into something else.
            let session_id = session.id().clone();
            // Empty sessions are not persisted; complete a replay-backed turn before resuming.
            let answer = session
                .send_and_wait("What is 1+1?")
                .await
                .expect("send")
                .expect("assistant message");
            // Validate the final assistant response arrived (guards against truncated captures).
            assert!(super::support::assistant_message_content(&answer).contains('2'));
            client
                .rpc()
                .sessions()
                .save(SessionsSaveRequest {
                    session_id: session_id.clone(),
                })
                .await
                .expect("persist session before disconnect");
            session.disconnect().await.expect("disconnect session");
            let resumed = client
                .resume_session(
                    ResumeSessionConfig::new(session_id)
                        .with_permission_handler(Arc::new(ApproveAllHandler))
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                )
                .await
                .expect("resume session");
            let resumed_workspace = resumed
                .rpc()
                .workspaces()
                .get_workspace()
                .await
                .expect("get workspace after resume")
                .workspace
                .expect("workspace after resume");
            assert_eq!(
                resumed_workspace.repository.as_deref(),
                Some("test-org/test-repo")
            );
            assert_eq!(
                resumed_workspace.branch.as_deref(),
                Some("feature/test-branch")
            );
            assert_eq!(resumed_workspace.git_root, workspace.git_root);
            assert_eq!(resumed_workspace.cwd, workspace.cwd);
            assert_eq!(resumed_workspace.host_type, workspace.host_type);
            assert_eq!(resumed_workspace.client_name, workspace.client_name);

            resumed.disconnect().await.expect("disconnect resumed");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_not_record_git_context_when_host_git_operations_are_disabled() {
    super::support::with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            init_git_repository(ctx.work_dir());
            run_git(
                ctx.work_dir(),
                &[
                    "remote",
                    "add",
                    "origin",
                    "https://github.com/test-org/test-repo.git",
                ],
            );
            run_git(
                ctx.work_dir(),
                &["commit", "--quiet", "--allow-empty", "-m", "initial"],
            );
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_enable_host_git_operations(false),
                )
                .await
                .expect("create session");

            let workspace = session
                .rpc()
                .workspaces()
                .get_workspace()
                .await
                .expect("get workspace")
                .workspace
                .expect("workspace");

            assert_eq!(workspace.repository, None);
            assert_eq!(workspace.branch, None);
            assert_eq!(workspace.git_root, None);
            assert_eq!(workspace.host_type, None);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

/// Runs git with the ambient global and system configuration disabled, so a
/// developer's or runner's `commit.gpgsign` / `core.hooksPath` cannot fail the
/// setup of a test that is about recording the workspace's git context.
fn run_git(path: &Path, args: &[&str]) {
    let output = Command::new("git")
        .args(args)
        .current_dir(path)
        .env("GIT_CONFIG_NOSYSTEM", "1")
        // A path that is never created, so git reads an empty global config.
        .env("GIT_CONFIG_GLOBAL", path.join("absent-global-gitconfig"))
        .env("GIT_TERMINAL_PROMPT", "0")
        .env("GIT_AUTHOR_NAME", "Test")
        .env("GIT_AUTHOR_EMAIL", "test@test.com")
        .env("GIT_COMMITTER_NAME", "Test")
        .env("GIT_COMMITTER_EMAIL", "test@test.com")
        .output()
        .expect("run git");
    assert!(
        output.status.success(),
        "git {args:?} should succeed: {}",
        String::from_utf8_lossy(&output.stderr)
    );
}

fn init_git_repository(path: &Path) {
    run_git(path, &["init", "--quiet"]);
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("rpc_workspace_checkpoints", 4);
