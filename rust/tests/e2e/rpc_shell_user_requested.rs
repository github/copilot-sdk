use std::path::Path;
use std::sync::Arc;
use std::time::Duration;

use github_copilot_sdk::RequestId;
use github_copilot_sdk::rpc::{ShellCancelUserRequestedRequest, ShellExecuteUserRequestedRequest};

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_execute_user_requested_shell_command() {
    with_e2e_context(
        "rpc_shell_user_requested",
        "should_execute_user_requested_shell_command",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let marker = format!("copilotusershell{}", uuid::Uuid::new_v4().simple());
                let request_id = RequestId::new(format!("req-{}", uuid::Uuid::new_v4().simple()));

                let result = session
                    .rpc()
                    .shell()
                    .execute_user_requested(ShellExecuteUserRequestedRequest {
                        request_id,
                        command: format!("echo {marker}"),
                    })
                    .await
                    .expect("execute user-requested shell command");

                assert!(
                    result.success,
                    "expected shell command to succeed: {:?}",
                    result.error
                );
                assert_eq!(result.exit_code, Some(0));
                assert!(result.output.contains(&marker));
                assert!(!result.tool_call_id.trim().is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_cancel_user_requested_shell_command() {
    with_e2e_context(
        "rpc_shell_user_requested",
        "should_cancel_user_requested_shell_command",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = Arc::new(
                    client
                        .create_session(ctx.approve_all_session_config())
                        .await
                        .expect("create session"),
                );

                let missing = session
                    .rpc()
                    .shell()
                    .cancel_user_requested(ShellCancelUserRequestedRequest {
                        request_id: RequestId::new(format!(
                            "missing-{}",
                            uuid::Uuid::new_v4().simple()
                        )),
                    })
                    .await
                    .expect("cancel missing request");
                assert!(!missing.cancelled);

                let request_id = RequestId::new(format!("req-{}", uuid::Uuid::new_v4().simple()));
                let marker_dir = tempfile::Builder::new()
                    .prefix("shell-cancel-")
                    .tempdir()
                    .expect("create shell cancel marker directory");
                let marker_path = marker_dir.path().join("marker.txt");
                let command = create_marker_then_sleep_command(&marker_path, 60);
                let execute_session = Arc::clone(&session);
                let execute_request_id = request_id.clone();
                let mut execute_task = tokio::spawn(async move {
                    execute_session
                        .rpc()
                        .shell()
                        .execute_user_requested(ShellExecuteUserRequestedRequest {
                            request_id: execute_request_id,
                            command,
                        })
                        .await
                });

                wait_for_file_text(&marker_path, "running").await;
                wait_for_condition("user-requested shell command cancellation", || {
                    let session = Arc::clone(&session);
                    let request_id = request_id.clone();
                    async move {
                        session
                            .rpc()
                            .shell()
                            .cancel_user_requested(ShellCancelUserRequestedRequest { request_id })
                            .await
                            .expect("cancel running request")
                            .cancelled
                    }
                })
                .await;

                // Await the spawned task by mutable reference so a timeout can abort it instead of
                // dropping the handle. A dropped JoinHandle detaches the task, leaving the shell
                // command running in the background where it can keep file handles open and
                // destabilize later tests.
                let result =
                    match tokio::time::timeout(Duration::from_secs(30), &mut execute_task).await {
                        Ok(joined) => joined
                            .expect("shell execution task should not panic")
                            .expect("execute user-requested shell command"),
                        Err(_elapsed) => {
                            execute_task.abort();
                            panic!("cancelled shell command did not finish within 30s");
                        }
                    };
                assert!(!result.success);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

async fn wait_for_file_text(path: &Path, expected: &'static str) {
    wait_for_condition("shell marker text", || async {
        std::fs::read_to_string(path).is_ok_and(|content| content.contains(expected))
    })
    .await;
}

#[cfg(windows)]
fn create_marker_then_sleep_command(marker_path: &Path, seconds: u64) -> String {
    format!(
        "Set-Content -LiteralPath {} -Value 'running'; Start-Sleep -Seconds {seconds}",
        powershell_quote(marker_path)
    )
}

#[cfg(not(windows))]
fn create_marker_then_sleep_command(marker_path: &Path, seconds: u64) -> String {
    format!(
        "echo running > {}; sleep {seconds}",
        posix_shell_quote(marker_path)
    )
}

#[cfg(windows)]
fn powershell_quote(path: &Path) -> String {
    format!("'{}'", path.display().to_string().replace('\'', "''"))
}

#[cfg(not(windows))]
fn posix_shell_quote(path: &Path) -> String {
    format!("'{}'", path.display().to_string().replace('\'', "'\\''"))
}
