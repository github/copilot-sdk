//! Real-boundary coverage for hostless extension discovery and lifecycle.
//!
//! Router tests can prove provider dispatch without exercising runtime session
//! setup, while an empty extension-list smoke test never requires the runtime
//! to install extension services. This test requires both boundaries and is
//! ignored until a real wrapper/runtime.node pair is supplied explicitly.

#![cfg(unix)]
#![allow(clippy::unwrap_used)]

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::sync::Arc;
use std::time::{Duration, Instant};

use async_trait::async_trait;
use github_copilot_sdk::extension_launch_provider::{
    ExtensionLaunchProfile, ExtensionLaunchProvider, ExtensionLaunchProviderResolveRequest,
    ExtensionLaunchProviderResolveResult,
};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::rpc::{
    ExtensionSource, ExtensionStatus, ExtensionsDisableRequest, ExtensionsEnableRequest,
    ToolResult, ToolResultType, ToolsExecuteRequest,
};
use github_copilot_sdk::{CliProgram, Client, ClientOptions, SessionConfig, Transport};
use serde_json::{Value, json};
use tokio::sync::mpsc;
use tokio::time::{sleep, timeout};

const TEST_TIMEOUT: Duration = Duration::from_secs(15);
const RUNTIME_PATH_ENV: &str = "COPILOT_RUNTIME_E2E_PATH";

struct RecordingProvider {
    executable: PathBuf,
    state_path: PathBuf,
    requests: mpsc::UnboundedSender<ExtensionLaunchProviderResolveRequest>,
}

#[async_trait]
impl ExtensionLaunchProvider for RecordingProvider {
    async fn resolve(
        &self,
        request: ExtensionLaunchProviderResolveRequest,
    ) -> github_copilot_sdk::Result<ExtensionLaunchProviderResolveResult> {
        self.requests.send(request.clone()).unwrap();
        Ok(ExtensionLaunchProviderResolveResult {
            launch: Some(ExtensionLaunchProfile {
                executable: self.executable.to_string_lossy().into_owned(),
                args: Vec::new(),
                env: HashMap::from([
                    ("EXTENSION_PATH".to_string(), request.module_path.clone()),
                    (
                        "FIXTURE_STATE_PATH".to_string(),
                        self.state_path.to_string_lossy().into_owned(),
                    ),
                    ("COPILOT_AUTO_UPDATE".to_string(), "false".to_string()),
                    (
                        "COPILOT_SDK_PATH".to_string(),
                        "provider-must-not-win".to_string(),
                    ),
                    (
                        "SESSION_ID".to_string(),
                        "provider-must-not-win".to_string(),
                    ),
                    (
                        "COPILOT_EXTENSION_PARENT_PID".to_string(),
                        "provider-must-not-win".to_string(),
                    ),
                ]),
            }),
        })
    }
}

#[tokio::test(flavor = "multi_thread")]
#[ignore = "requires COPILOT_RUNTIME_E2E_PATH to name a real wrapper/runtime.node pair"]
async fn real_wrapper_installs_and_runs_hostless_extensions() {
    let runtime_path = std::env::var_os(RUNTIME_PATH_ENV)
        .map(PathBuf::from)
        .expect("COPILOT_RUNTIME_E2E_PATH must name a copilot-runtime executable");
    assert_runtime_pair(&runtime_path);

    let home = tempfile::tempdir().unwrap();
    let workspace = tempfile::tempdir().unwrap();
    let extension_dir = workspace
        .path()
        .join(".github")
        .join("extensions")
        .join("lifecycle");
    std::fs::create_dir_all(&extension_dir).unwrap();
    let module_path = extension_dir.join("extension.mjs");
    std::fs::write(&module_path, "export default {};\n").unwrap();
    let module_path = std::fs::canonicalize(module_path).unwrap();
    let sdk_path = home.path().join("extension-sdk");
    std::fs::create_dir_all(&sdk_path).unwrap();
    let sdk_path = std::fs::canonicalize(sdk_path).unwrap();
    let state_path = home.path().join("extension-state.jsonl");
    let (request_tx, mut request_rx) = mpsc::unbounded_channel();

    let options = ClientOptions::new()
        .with_program(CliProgram::Path(runtime_path))
        .with_transport(Transport::Stdio)
        .with_cwd(workspace.path())
        .with_base_directory(home.path())
        .with_use_logged_in_user(false)
        .with_env_remove(["COPILOT_CLI_DIST_DIR"])
        .with_extension_launch_provider(RecordingProvider {
            executable: PathBuf::from(env!("CARGO_BIN_EXE_copilot-extension-test-fixture")),
            state_path: state_path.clone(),
            requests: request_tx,
        });
    let client = Client::start(options).await.unwrap();
    let session = client
        .create_session(
            SessionConfig::default()
                .with_request_extensions(true)
                .with_extension_sdk_path(sdk_path.to_string_lossy())
                .with_permission_handler(Arc::new(ApproveAllHandler)),
        )
        .await
        .unwrap();

    let first_request = recv_request(&mut request_rx).await;
    assert_request(&first_request, &module_path);
    let first_start = wait_for_starts(&state_path, 1).await.remove(0);
    assert_runtime_owned_environment(&first_start, session.id(), &sdk_path, &module_path);
    let first_pid = json_pid(&first_start);
    let wrapper_pid = json_parent_pid(&first_start);
    assert_eq!(direct_child_pids(wrapper_pid), vec![first_pid]);

    let listed = session.rpc().extensions().list().await.unwrap();
    assert_eq!(listed.extensions.len(), 1);
    assert_eq!(listed.extensions[0].id, "project:lifecycle");
    assert_eq!(listed.extensions[0].status, ExtensionStatus::Running);

    let result = session
        .rpc()
        .tools()
        .execute(ToolsExecuteRequest {
            arguments: json!({ "text": "sdk-boundary" }),
            name: "fixture_echo".to_string(),
            tool_call_id: Some("fixture-call".to_string()),
        })
        .await
        .unwrap();
    match result {
        ToolResult::String(value) => assert_eq!(value, "echoed"),
        ToolResult::ToolResultExpanded(result) => {
            assert_eq!(result.text_result_for_llm, "echoed");
            assert_eq!(result.result_type, ToolResultType::Success);
        }
    }
    wait_for_invocations(&state_path, 1).await;

    session
        .rpc()
        .extensions()
        .disable(ExtensionsDisableRequest {
            id: "project:lifecycle".to_string(),
        })
        .await
        .unwrap();
    wait_for_process_exit(first_pid).await;
    let disabled = session.rpc().extensions().list().await.unwrap();
    assert_eq!(disabled.extensions[0].status, ExtensionStatus::Disabled);

    session
        .rpc()
        .extensions()
        .enable(ExtensionsEnableRequest {
            id: "project:lifecycle".to_string(),
        })
        .await
        .unwrap();
    let second_request = recv_request(&mut request_rx).await;
    assert_request(&second_request, &module_path);
    let starts = wait_for_starts(&state_path, 2).await;
    let second_pid = json_pid(&starts[1]);
    assert_ne!(second_pid, first_pid);
    assert!(process_exists(second_pid));
    assert_eq!(json_parent_pid(&starts[1]), wrapper_pid);
    assert_eq!(direct_child_pids(wrapper_pid), vec![second_pid]);
    let enabled = session.rpc().extensions().list().await.unwrap();
    assert_eq!(enabled.extensions[0].status, ExtensionStatus::Running);

    client.stop().await.unwrap();
    wait_for_process_exit(second_pid).await;
    wait_for_process_exit(wrapper_pid).await;
    assert!(request_rx.try_recv().is_err());
}

fn assert_runtime_pair(runtime_path: &Path) {
    assert!(runtime_path.is_file(), "missing {}", runtime_path.display());
    let runtime_node = runtime_path.parent().unwrap().join("runtime.node");
    assert!(runtime_node.is_file(), "missing {}", runtime_node.display());
}

async fn recv_request(
    requests: &mut mpsc::UnboundedReceiver<ExtensionLaunchProviderResolveRequest>,
) -> ExtensionLaunchProviderResolveRequest {
    timeout(TEST_TIMEOUT, requests.recv())
        .await
        .expect(
            "runtime never invoked extensionLaunchProvider.resolve; hostless extension services \
             may not be installed",
        )
        .expect("provider request channel closed")
}

fn assert_request(request: &ExtensionLaunchProviderResolveRequest, module_path: &Path) {
    assert_eq!(request.id, "project:lifecycle");
    assert_eq!(request.name, "lifecycle");
    assert_eq!(request.source, ExtensionSource::Project);
    assert_eq!(Path::new(&request.module_path), module_path);
}

fn read_state(path: &Path) -> Vec<Value> {
    let Ok(contents) = std::fs::read_to_string(path) else {
        return Vec::new();
    };
    contents
        .lines()
        .map(|line| serde_json::from_str(line).unwrap())
        .collect()
}

async fn wait_for_starts(path: &Path, count: usize) -> Vec<Value> {
    wait_for_state(path, "start", count).await
}

async fn wait_for_invocations(path: &Path, count: usize) -> Vec<Value> {
    wait_for_state(path, "invoke", count).await
}

async fn wait_for_state(path: &Path, kind: &str, count: usize) -> Vec<Value> {
    let deadline = Instant::now() + TEST_TIMEOUT;
    loop {
        let matching: Vec<_> = read_state(path)
            .into_iter()
            .filter(|entry| entry["kind"] == kind)
            .collect();
        if matching.len() >= count {
            return matching;
        }
        assert!(
            Instant::now() < deadline,
            "timed out waiting for {count} {kind} entries; state: {:?}",
            read_state(path)
        );
        sleep(Duration::from_millis(25)).await;
    }
}

fn assert_runtime_owned_environment(
    start: &Value,
    session_id: &str,
    sdk_path: &Path,
    module_path: &Path,
) {
    assert_eq!(start["sessionId"], session_id);
    assert_eq!(
        start["sdkPath"].as_str(),
        Some(sdk_path.to_string_lossy().as_ref())
    );
    assert_eq!(
        start["extensionPath"].as_str(),
        Some(module_path.to_string_lossy().as_ref())
    );
    assert_eq!(start["autoUpdate"], "false");
    assert!(start["cliDistDir"].is_null());
    let parent_pid = start["parentPid"].as_str().unwrap();
    assert_ne!(parent_pid, "provider-must-not-win");
    assert!(parent_pid.parse::<u32>().is_ok());
}

fn json_pid(value: &Value) -> u32 {
    value["pid"].as_u64().unwrap() as u32
}

fn json_parent_pid(value: &Value) -> u32 {
    value["parentPid"].as_str().unwrap().parse().unwrap()
}

fn direct_child_pids(parent_pid: u32) -> Vec<u32> {
    let output = Command::new("ps")
        .args(["-axo", "pid=,ppid="])
        .output()
        .expect("list processes");
    assert!(output.status.success(), "ps failed: {output:?}");
    let mut children: Vec<_> = String::from_utf8(output.stdout)
        .unwrap()
        .lines()
        .filter_map(|line| {
            let mut columns = line.split_whitespace();
            let pid = columns.next()?.parse::<u32>().ok()?;
            let ppid = columns.next()?.parse::<u32>().ok()?;
            (ppid == parent_pid).then_some(pid)
        })
        .collect();
    children.sort_unstable();
    children
}

fn process_exists(pid: u32) -> bool {
    Command::new("kill")
        .args(["-0", &pid.to_string()])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .status()
        .is_ok_and(|status| status.success())
}

async fn wait_for_process_exit(pid: u32) {
    let deadline = Instant::now() + TEST_TIMEOUT;
    while process_exists(pid) {
        assert!(Instant::now() < deadline, "process {pid} did not exit");
        sleep(Duration::from_millis(25)).await;
    }
}
