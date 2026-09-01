#[cfg(unix)]
use github_copilot_sdk::Transport;
use github_copilot_sdk::{CliProgram, SessionLifecycleEventType};
use serde_json::json;

use super::support::{wait_for_lifecycle_event, with_e2e_context};

#[tokio::test]
async fn should_receive_session_created_lifecycle_event() {
    with_e2e_context(
        "client_lifecycle",
        "should_receive_session_created_lifecycle_event",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let created = client.subscribe_lifecycle();
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let event =
                    wait_for_lifecycle_event(created, "session.created lifecycle event", |event| {
                        event.event_type == SessionLifecycleEventType::Created
                    })
                    .await;
                assert_eq!(event.event_type, SessionLifecycleEventType::Created);
                assert_eq!(&event.session_id, session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_filter_session_lifecycle_events_by_type() {
    with_e2e_context(
        "client_lifecycle",
        "should_filter_session_lifecycle_events_by_type",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let created = client.subscribe_lifecycle();
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let event = wait_for_lifecycle_event(
                    created,
                    "filtered session.created lifecycle event",
                    |event| event.event_type == SessionLifecycleEventType::Created,
                )
                .await;
                assert_eq!(&event.session_id, session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn disposing_lifecycle_subscription_stops_receiving_events() {
    with_e2e_context(
        "client_lifecycle",
        "disposing_lifecycle_subscription_stops_receiving_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                drop(client.subscribe_lifecycle());
                let created = client.subscribe_lifecycle();
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let event = wait_for_lifecycle_event(
                    created,
                    "active session.created lifecycle event",
                    |event| event.event_type == SessionLifecycleEventType::Created,
                )
                .await;
                assert_eq!(event.session_id, *session.id());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn dispose_disconnects_client_and_disposes_rpc_surface_async() {
    with_e2e_context(
        "client_lifecycle",
        "dispose_disconnects_client_and_disposes_rpc_surface_async_true",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                client.stop().await.expect("stop client");
                assert!(
                    client.call("rpc.ping", Some(json!({}))).await.is_err(),
                    "stopped client should reject RPC calls"
                );
            })
        },
    )
    .await;
}

#[tokio::test]
async fn dispose_disconnects_client_and_disposes_rpc_surface_drop() {
    with_e2e_context(
        "client_lifecycle",
        "dispose_disconnects_client_and_disposes_rpc_surface_async_false",
        |ctx| {
            Box::pin(async move {
                let client = ctx.start_client().await;
                client.force_stop();
                assert!(
                    client.call("rpc.ping", Some(json!({}))).await.is_err(),
                    "force-stopped client should reject RPC calls"
                );
            })
        },
    )
    .await;
}

#[cfg(unix)]
#[tokio::test]
async fn stop_terminates_real_cli_wrapper_descendants() {
    with_e2e_context(
        "client_lifecycle",
        "stop_terminates_real_cli_wrapper_descendants",
        |ctx| {
            Box::pin(async move {
                if super::support::skip_inprocess(
                    "process-tree ownership only applies to SDK-spawned child-process transports",
                ) {
                    return;
                }

                let descendant_pid_path = ctx.work_dir().join("wrapper-descendant.pid");
                let mut options = ctx.client_options().with_transport(Transport::Stdio);
                let original_program = match &options.program {
                    CliProgram::Path(path) => path.clone(),
                    CliProgram::Resolve => {
                        panic!("E2E client options should resolve to an explicit CLI path")
                    }
                };
                let mut wrapper_args = vec![
                    "-c".into(),
                    "sleep 120 >/dev/null 2>&1 & echo $! > \"$SDK_DESCENDANT_PID\"; exec \"$@\""
                        .into(),
                    "sdk-cli-wrapper".into(),
                    original_program.into_os_string(),
                ];
                wrapper_args.extend(options.prefix_args);
                options.program = CliProgram::Path(std::path::PathBuf::from("sh"));
                options.prefix_args = wrapper_args;
                options.env.push((
                    "SDK_DESCENDANT_PID".into(),
                    descendant_pid_path.clone().into(),
                ));

                let client = github_copilot_sdk::Client::start(options)
                    .await
                    .expect("start wrapped real CLI");
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session through wrapped real CLI");
                session.disconnect().await.expect("disconnect session");

                let descendant_pid = wait_for_pid_file(&descendant_pid_path).await;
                assert!(
                    process_alive(descendant_pid),
                    "wrapper descendant should be alive before client stop"
                );

                client.stop().await.expect("stop wrapped real CLI");
                let exited = wait_for_process_exit(descendant_pid).await;
                if !exited {
                    kill_process(descendant_pid);
                }
                assert!(exited, "real CLI wrapper descendant survived Client::stop");
            })
        },
    )
    .await;
}

// This test represents github/app#2303: an SDK-embedding host process
// (there, the "Agency" process) is killed or crashes abruptly, without ever
// running any of its own cleanup code — so `Client::stop`/`force_stop`/`Drop`
// never execute. On Windows, the CLI is contained in a Job Object with
// `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`, so the OS itself terminates the CLI
// when the last handle to the job closes, which happens automatically when
// the owning process exits for any reason, including an abrupt, uncatchable
// termination. This test spawns a separate helper process that starts a real
// CLI client and then never calls any SDK cleanup code, terminates that
// helper process abruptly (`TerminateProcess` via `Child::kill`, which runs
// none of the helper's own code), and asserts the CLI still dies.
//
// Unix process groups have no equivalent auto-kill-on-owner-death guarantee
// (a `SIGKILL`ed owner leaves `Drop` un-run and `killpg` never called), so
// this test is Windows-only; it validates the property this PR's Windows
// implementation specifically targets.
//
// Manually reproducing this same abrupt-kill scenario on Linux with
// `Transport::Stdio` (a host process started, spawned a real CLI, then was
// `SIGKILL`ed with no cleanup code running) showed the CLI still exiting on
// its own within ~200ms, driven entirely by stdin EOF once the OS closed the
// dead host's end of the pipe — with none of this crate's code involved.
// That is a real, pre-existing, code-free safety net specific to
// stdio-piped processes on Unix; it's further evidence Unix process-group
// containment isn't needed to fix #2303's failure mode.
#[cfg(windows)]
#[tokio::test]
async fn abrupt_host_termination_still_kills_cli_via_job_object() {
    with_e2e_context(
        "client_lifecycle",
        "abrupt_host_termination_still_kills_cli_via_job_object",
        |ctx| {
            Box::pin(async move {
                let options = ctx.client_options();
                let program = match &options.program {
                    CliProgram::Path(path) => path
                        .to_str()
                        .expect("CLI program path is valid UTF-8")
                        .to_owned(),
                    CliProgram::Resolve => {
                        panic!("E2E client options should resolve to an explicit CLI path")
                    }
                };
                let prefix_args: Vec<String> = options
                    .prefix_args
                    .iter()
                    .map(|arg| arg.to_str().expect("prefix arg is valid UTF-8").to_owned())
                    .collect();
                let env_pairs: Vec<(String, String)> = options
                    .env
                    .iter()
                    .map(|(k, v)| {
                        (
                            k.to_str().expect("env key is valid UTF-8").to_owned(),
                            v.to_str().expect("env value is valid UTF-8").to_owned(),
                        )
                    })
                    .collect();
                let cwd = options
                    .working_directory
                    .to_str()
                    .expect("cwd is valid UTF-8")
                    .to_owned();
                let pid_file = ctx.work_dir().join("host-crash-fixture-cli.pid");

                let mut host =
                    std::process::Command::new(env!("CARGO_BIN_EXE_copilot-host-crash-fixture"))
                        .env("HOST_CRASH_FIXTURE_PROGRAM", &program)
                        .env(
                            "HOST_CRASH_FIXTURE_PREFIX_ARGS_JSON",
                            serde_json::to_string(&prefix_args).expect("serialize prefix args"),
                        )
                        .env("HOST_CRASH_FIXTURE_CWD", &cwd)
                        .env(
                            "HOST_CRASH_FIXTURE_ENV_JSON",
                            serde_json::to_string(&env_pairs).expect("serialize env pairs"),
                        )
                        .env("HOST_CRASH_FIXTURE_PID_FILE", &pid_file)
                        .spawn()
                        .expect("spawn host-crash fixture process");

                let cli_pid = wait_for_pid_file_windows(&pid_file).await;
                assert!(
                    process_alive_windows(cli_pid),
                    "CLI should be alive before its host process is terminated"
                );

                // Abruptly terminate the fixture process itself — the
                // Windows analogue of the Agency process dying in #2303.
                // `Child::kill` maps to `TerminateProcess`, which runs none
                // of the target process's own code (no `Drop`, no `main`
                // unwind).
                host.kill().expect("terminate host-crash fixture process");
                host.wait().expect("reap host-crash fixture process");

                let cli_exited = wait_for_process_exit_windows(cli_pid).await;
                if !cli_exited {
                    kill_process_windows(cli_pid);
                }
                assert!(
                    cli_exited,
                    "CLI survived its abruptly terminated host process; Job Object \
                     kill-on-close did not terminate it"
                );
            })
        },
    )
    .await;
}

#[cfg(windows)]
async fn wait_for_pid_file_windows(path: &std::path::Path) -> u32 {
    super::support::wait_for_condition("host-crash fixture CLI pid file", || async {
        path.exists()
    })
    .await;
    std::fs::read_to_string(path)
        .expect("read host-crash fixture CLI pid")
        .trim()
        .parse()
        .expect("parse host-crash fixture CLI pid")
}

#[cfg(windows)]
async fn wait_for_process_exit_windows(pid: u32) -> bool {
    let deadline = std::time::Instant::now() + std::time::Duration::from_secs(15);
    while process_alive_windows(pid) {
        if std::time::Instant::now() >= deadline {
            return false;
        }
        tokio::time::sleep(std::time::Duration::from_millis(25)).await;
    }
    true
}

#[cfg(windows)]
fn process_alive_windows(pid: u32) -> bool {
    use windows_sys::Win32::Foundation::{CloseHandle, WAIT_TIMEOUT};
    use windows_sys::Win32::System::Threading::{
        OpenProcess, PROCESS_SYNCHRONIZE, WaitForSingleObject,
    };

    // SAFETY: the process handle is closed before returning.
    unsafe {
        let process = OpenProcess(PROCESS_SYNCHRONIZE, 0, pid);
        if process.is_null() {
            return false;
        }
        let alive = WaitForSingleObject(process, 0) == WAIT_TIMEOUT;
        CloseHandle(process);
        alive
    }
}

#[cfg(windows)]
fn kill_process_windows(pid: u32) {
    use windows_sys::Win32::Foundation::CloseHandle;
    use windows_sys::Win32::System::Threading::{OpenProcess, PROCESS_TERMINATE, TerminateProcess};

    // SAFETY: the pid came from this test's controlled fixture-spawned CLI.
    unsafe {
        let process = OpenProcess(PROCESS_TERMINATE, 0, pid);
        if !process.is_null() {
            TerminateProcess(process, 1);
            CloseHandle(process);
        }
    }
}

#[tokio::test]
async fn should_receive_session_updated_lifecycle_event_for_non_ephemeral_activity() {
    with_e2e_context(
        "client_lifecycle",
        "should_receive_session_updated_lifecycle_event_for_non_ephemeral_activity",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let updated = client.subscribe_lifecycle();

                session
                    .client()
                    .call(
                        "session.mode.set",
                        Some(json!({
                            "sessionId": session.id().as_str(),
                            "mode": "plan",
                        })),
                    )
                    .await
                    .expect("set session mode");

                let event =
                    wait_for_lifecycle_event(updated, "session.updated lifecycle event", |event| {
                        event.event_type == SessionLifecycleEventType::Updated
                            && event.session_id == *session.id()
                    })
                    .await;
                assert_eq!(event.event_type, SessionLifecycleEventType::Updated);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[cfg(unix)]
async fn wait_for_pid_file(path: &std::path::Path) -> u32 {
    super::support::wait_for_condition("wrapper descendant pid file", || async { path.exists() })
        .await;
    std::fs::read_to_string(path)
        .expect("read wrapper descendant pid")
        .trim()
        .parse()
        .expect("parse wrapper descendant pid")
}

#[cfg(unix)]
async fn wait_for_process_exit(pid: u32) -> bool {
    let deadline = std::time::Instant::now() + std::time::Duration::from_secs(15);
    while process_alive(pid) {
        if std::time::Instant::now() >= deadline {
            return false;
        }
        tokio::time::sleep(std::time::Duration::from_millis(25)).await;
    }
    true
}

#[cfg(unix)]
fn process_alive(pid: u32) -> bool {
    #[cfg(target_os = "linux")]
    if let Ok(stat) = std::fs::read_to_string(format!("/proc/{pid}/stat"))
        && stat
            .rsplit_once(") ")
            .and_then(|(_, fields)| fields.chars().next())
            .is_some_and(|state| matches!(state, 'Z' | 'X'))
    {
        return false;
    }

    // SAFETY: signal 0 probes process existence without modifying it.
    unsafe { libc::kill(pid as i32, 0) == 0 }
}

#[cfg(unix)]
fn kill_process(pid: u32) {
    // SAFETY: the pid came from this test's controlled descendant process.
    unsafe {
        libc::kill(pid as i32, libc::SIGKILL);
    }
}

#[tokio::test]
async fn should_receive_session_deleted_lifecycle_event_when_deleted() {
    with_e2e_context(
        "client_lifecycle",
        "should_receive_session_deleted_lifecycle_event_when_deleted",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session_id = session.id().clone();
                session
                    .send_and_wait("Say SESSION_DELETED_OK exactly.")
                    .await
                    .expect("send");
                let deleted = client.subscribe_lifecycle();

                client
                    .delete_session(&session_id)
                    .await
                    .expect("delete session");

                let event =
                    wait_for_lifecycle_event(deleted, "session.deleted lifecycle event", |event| {
                        event.event_type == SessionLifecycleEventType::Deleted
                            && event.session_id == session_id
                    })
                    .await;
                assert_eq!(event.event_type, SessionLifecycleEventType::Deleted);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
