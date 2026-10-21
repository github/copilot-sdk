#![cfg(windows)]

use std::io;
use std::os::windows::io::{AsRawHandle, FromRawHandle, OwnedHandle};
use std::path::Path;
use std::time::Duration;

use github_copilot_sdk::{CliProgram, Client, ClientOptions, Transport};
use windows_sys::Win32::System::Threading::{
    OpenProcess, PROCESS_SYNCHRONIZE, PROCESS_TERMINATE, TerminateProcess, WaitForSingleObject,
};

use super::ShutdownCli;

struct FixtureProcess(OwnedHandle);

impl FixtureProcess {
    fn open(pid: u32) -> Self {
        // SAFETY: this PID was published by this test's owned fixture.
        let handle = unsafe { OpenProcess(PROCESS_SYNCHRONIZE | PROCESS_TERMINATE, 0, pid) };
        assert!(!handle.is_null(), "{}", io::Error::last_os_error());
        // SAFETY: OpenProcess returned a new independently owned handle.
        Self(unsafe { OwnedHandle::from_raw_handle(handle) })
    }

    fn alive(&self) -> bool {
        // SAFETY: the retained process handle is valid even after the process exits.
        match unsafe { WaitForSingleObject(self.0.as_raw_handle().cast(), 0) } {
            0 => false,
            258 => true,
            result => panic!(
                "process wait failed ({result}): {}",
                io::Error::last_os_error()
            ),
        }
    }

    fn terminate(&self) {
        // SAFETY: this handle uniquely identifies a fixture-owned process, not a reused PID.
        assert_ne!(
            unsafe { TerminateProcess(self.0.as_raw_handle().cast(), 1) },
            0,
            "{}",
            io::Error::last_os_error()
        );
    }

    async fn assert_exited(&self) {
        tokio::time::timeout(Duration::from_secs(5), async {
            while self.alive() {
                tokio::time::sleep(Duration::from_millis(10)).await;
            }
        })
        .await
        .expect("owned root and descendant must terminate");
    }
}

impl Drop for FixtureProcess {
    fn drop(&mut self) {
        // SAFETY: fallback cleanup targets only this fixture's retained process handle.
        unsafe {
            if WaitForSingleObject(self.0.as_raw_handle().cast(), 0) == 258 {
                if TerminateProcess(self.0.as_raw_handle().cast(), 1) == 0 {
                    eprintln!(
                        "fixture fallback termination failed: {}",
                        io::Error::last_os_error()
                    );
                }
                if WaitForSingleObject(self.0.as_raw_handle().cast(), 5000) != 0 {
                    eprintln!(
                        "fixture fallback reap failed: {}",
                        io::Error::last_os_error()
                    );
                }
            }
        }
    }
}

fn options(fake: &ShutdownCli, tcp: bool) -> ClientOptions {
    let mut options = fake.options();
    options.prefix_args.push("--fixture-descendant".into());
    if tcp {
        options.prefix_args.push("--fixture-tcp".into());
        options.transport = Transport::Tcp {
            port: 0,
            connection_token: None,
        };
    }
    options
}

async fn published_pid(path: &Path) -> u32 {
    tokio::time::timeout(Duration::from_secs(10), async {
        loop {
            match std::fs::read_to_string(path) {
                Ok(value) => {
                    if let Ok(pid) = value.trim().parse() {
                        return pid;
                    }
                }
                Err(error) if error.kind() == io::ErrorKind::NotFound => {}
                Err(error) => panic!("read fixture PID: {error}"),
            }
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    })
    .await
    .expect("fixture must publish its PID")
}

async fn retain_tree(fake: &ShutdownCli) -> (FixtureProcess, FixtureProcess) {
    let root = FixtureProcess::open(published_pid(&fake.pid_file).await);
    let descendant_path = fake.pid_file.with_file_name("child.pid.descendant");
    let descendant = FixtureProcess::open(published_pid(&descendant_path).await);
    assert!(root.alive());
    assert!(descendant.alive());
    (root, descendant)
}

#[tokio::test]
async fn stop_force_stop_and_drop_terminate_owned_descendants() {
    for tcp in [false, true] {
        for action in ["stop", "force_stop", "drop"] {
            let fake = ShutdownCli::new("force");
            let client = Client::start(options(&fake, tcp))
                .await
                .expect("start fixture CLI");
            let (root, descendant) = retain_tree(&fake).await;
            match action {
                "stop" => client.stop().await.expect("stop fixture CLI"),
                "force_stop" => client.force_stop(),
                "drop" => {}
                _ => unreachable!(),
            }
            drop(client);
            root.assert_exited().await;
            descendant.assert_exited().await;
        }
    }
}

#[tokio::test]
async fn cancelled_startup_terminates_root_and_descendant() {
    for (tcp, mode) in [
        (false, "connect-stall"),
        (true, "connect-stall"),
        (true, "port-stall"),
    ] {
        let fake = ShutdownCli::new(mode);
        let starting = tokio::spawn(Client::start(options(&fake, tcp)));
        let (root, descendant) = retain_tree(&fake).await;
        starting.abort();
        assert!(starting.await.expect_err("cancel startup").is_cancelled());
        root.assert_exited().await;
        descendant.assert_exited().await;
    }
}

#[tokio::test]
async fn startup_process_exit_terminates_descendant() {
    for tcp in [false, true] {
        let fake = ShutdownCli::new("connect-stall");
        let starting = tokio::spawn(Client::start(options(&fake, tcp)));
        let (root, descendant) = retain_tree(&fake).await;
        root.terminate();
        let result = tokio::time::timeout(Duration::from_secs(5), starting)
            .await
            .expect("startup failure must be bounded")
            .expect("join startup task");
        assert!(result.is_err());
        root.assert_exited().await;
        descendant.assert_exited().await;
    }
}

#[tokio::test]
async fn last_clone_drop_is_private_to_its_client_job() {
    for tcp in [false, true] {
        let first = ShutdownCli::new("force");
        let client = Client::start(options(&first, tcp))
            .await
            .expect("start first client");
        let retained_client = client.clone();
        let first_tree = retain_tree(&first).await;
        let second = ShutdownCli::new("force");
        let second_client = Client::start(options(&second, tcp))
            .await
            .expect("start second client");
        let second_tree = retain_tree(&second).await;

        drop(client);
        assert!(first_tree.0.alive());
        assert!(first_tree.1.alive());
        drop(retained_client);
        first_tree.0.assert_exited().await;
        first_tree.1.assert_exited().await;
        assert!(second_tree.0.alive());
        assert!(second_tree.1.alive());
        second_client
            .ping(Some("private-job-isolation"))
            .await
            .expect("other client's Job must remain alive");
        second_client.force_stop();
        second_tree.0.assert_exited().await;
        second_tree.1.assert_exited().await;
    }
}

#[tokio::test]
async fn abrupt_host_exit_terminates_root_and_descendant_without_rust_cleanup() {
    for tcp in [false, true] {
        let fake = ShutdownCli::new("force");
        let options = options(&fake, tcp);
        let CliProgram::Path(program) = options.program else {
            panic!("fixture uses an explicit program");
        };
        let prefix: Vec<_> = options
            .prefix_args
            .iter()
            .map(|arg| arg.to_str().expect("fixture argument is UTF-8"))
            .collect();
        let host_pid_file = fake.dir.path().join("host-cli.pid");
        let mut host =
            tokio::process::Command::new(env!("CARGO_BIN_EXE_copilot-host-crash-fixture"))
                .env("HOST_CRASH_FIXTURE_PROGRAM", program)
                .env(
                    "HOST_CRASH_FIXTURE_PREFIX_ARGS_JSON",
                    serde_json::to_string(&prefix).expect("fixture args"),
                )
                .env("HOST_CRASH_FIXTURE_CWD", fake.dir.path())
                .env("HOST_CRASH_FIXTURE_ENV_JSON", "[]")
                .env("HOST_CRASH_FIXTURE_PID_FILE", &host_pid_file)
                .env(
                    "HOST_CRASH_FIXTURE_TRANSPORT",
                    if tcp { "tcp" } else { "stdio" },
                )
                .kill_on_drop(true)
                .spawn()
                .expect("spawn SDK host fixture");
        let reported_root = published_pid(&host_pid_file).await;
        assert_eq!(reported_root, published_pid(&fake.pid_file).await);
        let (root, descendant) = retain_tree(&fake).await;
        tokio::time::timeout(Duration::from_secs(5), host.kill())
            .await
            .expect("bound abrupt host termination")
            .expect("kill and reap SDK host");
        root.assert_exited().await;
        descendant.assert_exited().await;
    }
}
