//! Test-only binary that hosts a single [`github_copilot_sdk::Client`] and then
//! blocks forever, so an external test can terminate *this* process abruptly
//! (simulating an SDK-embedding app process crashing) without ever running any
//! of this process's own cleanup code (`Client::stop`, `force_stop`, or
//! `Drop`).
//!
//! Configuration is passed entirely through environment variables so the
//! caller doesn't need this crate's non-`pub` types:
//! - `HOST_CRASH_FIXTURE_PROGRAM`: CLI program path.
//! - `HOST_CRASH_FIXTURE_PREFIX_ARGS_JSON`: JSON array of prefix args.
//! - `HOST_CRASH_FIXTURE_CWD`: working directory for the spawned CLI.
//! - `HOST_CRASH_FIXTURE_ENV_JSON`: JSON array of `[key, value]` pairs to set
//!   on the spawned CLI's environment.
//! - `HOST_CRASH_FIXTURE_PID_FILE`: path this process writes the CLI child's
//!   OS process id to, once the client finishes starting.

use std::path::PathBuf;

use github_copilot_sdk::{CliProgram, Client, ClientOptions, Transport};

#[tokio::main(flavor = "current_thread")]
async fn main() {
    let program = std::env::var("HOST_CRASH_FIXTURE_PROGRAM").expect("HOST_CRASH_FIXTURE_PROGRAM");
    let prefix_args: Vec<String> = serde_json::from_str(
        &std::env::var("HOST_CRASH_FIXTURE_PREFIX_ARGS_JSON")
            .expect("HOST_CRASH_FIXTURE_PREFIX_ARGS_JSON"),
    )
    .expect("parse HOST_CRASH_FIXTURE_PREFIX_ARGS_JSON");
    let cwd = std::env::var("HOST_CRASH_FIXTURE_CWD").expect("HOST_CRASH_FIXTURE_CWD");
    let env_pairs: Vec<(String, String)> = serde_json::from_str(
        &std::env::var("HOST_CRASH_FIXTURE_ENV_JSON").expect("HOST_CRASH_FIXTURE_ENV_JSON"),
    )
    .expect("parse HOST_CRASH_FIXTURE_ENV_JSON");
    let pid_file = PathBuf::from(
        std::env::var("HOST_CRASH_FIXTURE_PID_FILE").expect("HOST_CRASH_FIXTURE_PID_FILE"),
    );

    let options = ClientOptions::new()
        .with_program(CliProgram::Path(PathBuf::from(program)))
        .with_prefix_args(prefix_args)
        .with_cwd(PathBuf::from(cwd))
        .with_env(env_pairs)
        .with_use_logged_in_user(false)
        .with_transport(Transport::Stdio);

    let client = Client::start(options).await.expect("start CLI client");
    let pid = client.pid().expect("client reports spawned CLI pid");
    std::fs::write(&pid_file, pid.to_string()).expect("write pid file");

    // Deliberately leak the client so nothing in this process — including its
    // `Drop` impls — ever runs cleanup code. The external test process
    // terminates this process abruptly (e.g. `TerminateProcess` on Windows)
    // to simulate an SDK-embedding host crashing, and asserts that the CLI
    // still dies via the OS containment primitive alone.
    std::mem::forget(client);

    loop {
        std::thread::sleep(std::time::Duration::from_secs(3600));
    }
}
