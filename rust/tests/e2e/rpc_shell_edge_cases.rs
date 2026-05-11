use std::path::Path;

use github_copilot_sdk::generated::api_types::{
    ShellExecRequest, ShellKillRequest, ShellKillSignal,
};

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn shell_exec_with_timeout_kills_long_running_command() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_exec_with_timeout_kills_long_running_command",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let started_path = ctx.work_dir().join("shell-timeout-started.txt");
                let marker_path = ctx.work_dir().join("shell-timeout-marker.txt");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .shell()
                    .exec(ShellExecRequest {
                        command: delayed_write_command(&started_path, &marker_path),
                        cwd: Some(ctx.work_dir().display().to_string()),
                        timeout: Some(200),
                    })
                    .await
                    .expect("execute timed command");
                assert!(!result.process_id.trim().is_empty());

                wait_for_exists(&started_path).await;
                wait_for_process_cleanup(&session, result.process_id, "timed-out command").await;
                assert!(
                    !marker_path.exists(),
                    "timeout should kill before marker is written"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn shell_exec_with_custom_cwd_honors_override() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_exec_with_custom_cwd_honors_override",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let subdir = ctx.work_dir().join("shell-cwd");
                std::fs::create_dir_all(&subdir).expect("create shell cwd");
                let marker_path = subdir.join("marker.txt");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .shell()
                    .exec(ShellExecRequest {
                        command: write_relative_marker_command("shell-cwd-marker"),
                        cwd: Some(subdir.display().to_string()),
                        timeout: None,
                    })
                    .await
                    .expect("execute cwd command");

                assert!(!result.process_id.trim().is_empty());
                wait_for_file_text(&marker_path, "shell-cwd-marker").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn shell_exec_with_nonexistent_command_returns_processid_and_cleans_up() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_exec_with_nonexistent_command_returns_processid_and_cleans_up",
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
                    .shell()
                    .exec(ShellExecRequest {
                        command: nonexistent_command(),
                        cwd: Some(ctx.work_dir().display().to_string()),
                        timeout: None,
                    })
                    .await
                    .expect("execute nonexistent command");

                assert!(!result.process_id.trim().is_empty());
                wait_for_process_cleanup(&session, result.process_id, "nonexistent command").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn shell_kill_unknown_processid_returns_false() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_kill_unknown_processid_returns_false",
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
                    .shell()
                    .kill(ShellKillRequest {
                        process_id: "unknown-rust-process".to_string(),
                        signal: None,
                    })
                    .await
                    .expect("kill unknown process");

                assert!(!result.killed);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn shell_kill_cleans_up_after_terminating_signal() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_kill_cleans_up_after_terminating_signal",
        |ctx| {
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
                    .expect("start shell");

                let killed = session
                    .rpc()
                    .shell()
                    .kill(ShellKillRequest {
                        process_id: exec.process_id.clone(),
                        signal: Some(ShellKillSignal::SIGTERM),
                    })
                    .await
                    .expect("kill shell");
                assert!(killed.killed);
                wait_for_process_cleanup(&session, exec.process_id, "killed command").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn shell_exec_with_stderr_output_cleans_up() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_exec_with_stderr_output_cleans_up",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let marker_path = ctx.work_dir().join("shell-stderr-marker.txt");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .shell()
                    .exec(ShellExecRequest {
                        command: stderr_command(&marker_path),
                        cwd: Some(ctx.work_dir().display().to_string()),
                        timeout: None,
                    })
                    .await
                    .expect("execute stderr command");

                wait_for_exists(&marker_path).await;
                wait_for_process_cleanup(&session, result.process_id, "stderr command").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn shell_exec_with_large_stdout_cleans_up() {
    with_e2e_context(
        "rpc_shell_edge_cases",
        "shell_exec_with_large_stdout_cleans_up",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let marker_path = ctx.work_dir().join("shell-stdout-marker.txt");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let result = session
                    .rpc()
                    .shell()
                    .exec(ShellExecRequest {
                        command: large_stdout_command(&marker_path),
                        cwd: Some(ctx.work_dir().display().to_string()),
                        timeout: None,
                    })
                    .await
                    .expect("execute large stdout command");

                wait_for_exists(&marker_path).await;
                wait_for_process_cleanup(&session, result.process_id, "large stdout command").await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

async fn wait_for_exists(path: &Path) {
    wait_for_condition("shell marker file", || async { path.exists() }).await;
}

async fn wait_for_file_text(path: &Path, expected: &'static str) {
    wait_for_condition("shell marker text", || async {
        match std::fs::read_to_string(path) {
            Ok(content) => content.contains(expected),
            Err(_) => false,
        }
    })
    .await;
}

async fn wait_for_process_cleanup(
    session: &github_copilot_sdk::session::Session,
    process_id: String,
    _scenario: &'static str,
) {
    tokio::time::sleep(std::time::Duration::from_secs(1)).await;
    let result = session
        .rpc()
        .shell()
        .kill(ShellKillRequest {
            process_id,
            signal: None,
        })
        .await
        .expect("probe process cleanup");
    assert!(!result.killed);
}

#[cfg(windows)]
fn delayed_write_command(started_path: &Path, marker_path: &Path) -> String {
    format!(
        "powershell -NoLogo -NoProfile -Command \"Set-Content -LiteralPath '{}' -Value started; Start-Sleep -Seconds 30; Set-Content -LiteralPath '{}' -Value should-not-exist\"",
        started_path.display(),
        marker_path.display()
    )
}

#[cfg(not(windows))]
fn delayed_write_command(started_path: &Path, marker_path: &Path) -> String {
    format!(
        "sh -c \"printf started > '{}'; sleep 30; printf should-not-exist > '{}'\"",
        started_path.display(),
        marker_path.display()
    )
}

#[cfg(windows)]
fn write_relative_marker_command(marker: &str) -> String {
    format!(
        "powershell -NoLogo -NoProfile -Command \"Set-Content -LiteralPath 'marker.txt' -Value '{marker}'\""
    )
}

#[cfg(not(windows))]
fn write_relative_marker_command(marker: &str) -> String {
    format!("sh -c \"printf '%s' '{marker}' > marker.txt\"")
}

#[cfg(windows)]
fn long_running_command() -> String {
    "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 60\"".to_string()
}

#[cfg(not(windows))]
fn long_running_command() -> String {
    "sleep 60".to_string()
}

#[cfg(windows)]
fn nonexistent_command() -> String {
    "cmd /C definitely-not-a-real-command-rust-12345".to_string()
}

#[cfg(not(windows))]
fn nonexistent_command() -> String {
    "sh -c 'definitely-not-a-real-command-rust-12345'".to_string()
}

#[cfg(windows)]
fn stderr_command(marker_path: &Path) -> String {
    format!(
        "powershell -NoLogo -NoProfile -Command \"[Console]::Error.WriteLine('boom'); Set-Content -LiteralPath '{}' -Value done; exit 2\"",
        marker_path.display()
    )
}

#[cfg(not(windows))]
fn stderr_command(marker_path: &Path) -> String {
    format!(
        "sh -c \"echo boom 1>&2; printf done > '{}'; exit 2\"",
        marker_path.display()
    )
}

#[cfg(windows)]
fn large_stdout_command(marker_path: &Path) -> String {
    format!(
        "powershell -NoLogo -NoProfile -Command \"Write-Host ('x' * 204800); Set-Content -LiteralPath '{}' -Value done\"",
        marker_path.display()
    )
}

#[cfg(not(windows))]
fn large_stdout_command(marker_path: &Path) -> String {
    format!(
        "sh -c \"python3 - <<'PY'\nprint('x' * 204800)\nPY\nprintf done > '{}'\"",
        marker_path.display()
    )
}
