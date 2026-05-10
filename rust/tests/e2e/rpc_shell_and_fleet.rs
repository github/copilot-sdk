use github_copilot_sdk::generated::api_types::{ShellExecRequest, ShellKillRequest};

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_execute_shell_command() {
    with_e2e_context(
        "rpc_shell_and_fleet",
        "should_execute_shell_command",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let marker_path = ctx.work_dir().join("shell-rpc-marker.txt");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .shell()
                    .exec(ShellExecRequest {
                        command: write_file_command(&marker_path, "copilot-sdk-shell-rpc"),
                        cwd: Some(ctx.work_dir().display().to_string()),
                        timeout: None,
                    })
                    .await
                    .expect("execute shell command");

                assert!(!result.process_id.trim().is_empty());
                wait_for_file_text(&marker_path, "copilot-sdk-shell-rpc").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_kill_shell_process() {
    with_e2e_context("rpc_shell_and_fleet", "should_kill_shell_process", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let exec = session
                .rpc()
                .shell()
                .exec(ShellExecRequest {
                    command: long_running_command(),
                    cwd: Some(ctx.work_dir().display().to_string()),
                    timeout: None,
                })
                .await
                .expect("start shell process");
            assert!(!exec.process_id.trim().is_empty());

            let killed = session
                .rpc()
                .shell()
                .kill(ShellKillRequest {
                    process_id: exec.process_id,
                    signal: None,
                })
                .await
                .expect("kill shell process");
            assert!(killed.killed);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

async fn wait_for_file_text(path: &std::path::Path, expected: &'static str) {
    wait_for_condition("shell command output file", || async {
        match std::fs::read_to_string(path) {
            Ok(content) => content.contains(expected),
            Err(_) => false,
        }
    })
    .await;
}

#[cfg(windows)]
fn write_file_command(path: &std::path::Path, marker: &str) -> String {
    format!(
        "powershell -NoLogo -NoProfile -Command \"Set-Content -LiteralPath '{}' -Value '{}'\"",
        path.display(),
        marker
    )
}

#[cfg(not(windows))]
fn write_file_command(path: &std::path::Path, marker: &str) -> String {
    format!("sh -c \"printf '%s' '{}' > '{}'\"", marker, path.display())
}

#[cfg(windows)]
fn long_running_command() -> String {
    "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 30\"".to_string()
}

#[cfg(not(windows))]
fn long_running_command() -> String {
    "sleep 30".to_string()
}
