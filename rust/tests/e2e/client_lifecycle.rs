#[cfg(windows)]
use github_copilot_sdk::CliProgram;
use github_copilot_sdk::SessionLifecycleEventType;
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

// This test represents github/app#2303: the SDK-hosting GitHub Copilot app
// process exits abruptly, so Client cleanup never runs. The helper starts a
// real CLI client, is terminated through `TerminateProcess`, and relies only
// on Job Object kill-on-close behavior to terminate the CLI.
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

                // `Child::kill` maps to `TerminateProcess`, which runs none
                // of the target process's cleanup code.
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
