#![allow(clippy::unwrap_used)]

use std::io::{BufRead, BufReader, Read, Write};
use std::net::TcpStream;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::generated::session_events::{
    AutoModeSwitchCompletedData, AutoModeSwitchRequestedData, ExitPlanModeCompletedData,
    ExitPlanModeRequestedData, SessionEventType, SessionModelChangeData,
};
use github_copilot_sdk::handler::{AutoModeSwitchResponse, ExitPlanModeResult, SessionHandler};
use github_copilot_sdk::subscription::EventSubscription;
use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, ExitPlanModeData, SessionConfig, SessionEvent, SessionId,
};
use serde_json::json;
use tokio::sync::mpsc;

const MODE_HANDLER_TOKEN: &str = "mode-handler-token";
const PLAN_SUMMARY: &str = "Greeting file implementation plan";
const PLAN_PROMPT: &str = "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode.";
const AUTO_MODE_PROMPT: &str =
    "Explain that auto mode recovered from a rate limit in one short sentence.";

#[derive(Debug)]
struct ModeHandler {
    requests: mpsc::UnboundedSender<(SessionId, ExitPlanModeData)>,
}

#[derive(Debug)]
struct AutoModeHandler {
    requests: mpsc::UnboundedSender<(SessionId, Option<String>, Option<f64>)>,
}

#[async_trait]
impl SessionHandler for ModeHandler {
    async fn on_exit_plan_mode(
        &self,
        session_id: SessionId,
        data: ExitPlanModeData,
    ) -> ExitPlanModeResult {
        let _ = self.requests.send((session_id, data));
        ExitPlanModeResult {
            approved: true,
            selected_action: Some("interactive".to_string()),
            feedback: Some("Approved by the Rust E2E test".to_string()),
        }
    }
}

#[async_trait]
impl SessionHandler for AutoModeHandler {
    async fn on_auto_mode_switch(
        &self,
        session_id: SessionId,
        error_code: Option<String>,
        retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse {
        let _ = self
            .requests
            .send((session_id, error_code, retry_after_seconds));
        AutoModeSwitchResponse::Yes
    }
}

#[tokio::test]
#[ignore] // requires the Node CLI and shared replay proxy dependencies
async fn should_invoke_exit_plan_mode_handler_when_model_uses_tool() {
    let repo_root = repo_root();
    let cli_path = repo_root
        .join("nodejs")
        .join("node_modules")
        .join("@github")
        .join("copilot")
        .join("index.js");
    assert!(
        cli_path.exists(),
        "CLI not found at {}; run npm install in nodejs first",
        cli_path.display()
    );

    let home_dir = tempfile::tempdir().expect("create home dir");
    let work_dir = tempfile::tempdir().expect("create work dir");
    let mut proxy = CapiProxy::start(&repo_root).expect("start replay proxy");
    proxy
        .configure(
            &repo_root
                .join("test")
                .join("snapshots")
                .join("mode_handlers")
                .join("should_invoke_exit_plan_mode_handler_when_model_uses_tool.yaml"),
            work_dir.path(),
        )
        .expect("configure replay proxy");
    proxy
        .set_copilot_user_by_token(
            MODE_HANDLER_TOKEN,
            json!({
                "login": "mode-handler-user",
                "copilot_plan": "individual_pro",
                "endpoints": {
                    "api": proxy.url(),
                    "telemetry": "https://localhost:1/telemetry"
                },
                "analytics_tracking_id": "mode-handler-tracking-id"
            }),
        )
        .expect("configure copilot user");

    let mut env = proxy.proxy_env();
    env.extend([
        ("COPILOT_API_URL".into(), proxy.url().into()),
        ("COPILOT_DEBUG_GITHUB_API_URL".into(), proxy.url().into()),
        (
            "COPILOT_HOME".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
        (
            "GH_CONFIG_DIR".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
        (
            "XDG_CONFIG_HOME".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
        (
            "XDG_STATE_HOME".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
    ]);

    let client = Client::start(
        ClientOptions::new()
            .with_program(CliProgram::Path(PathBuf::from(node_program())))
            .with_prefix_args([cli_path.as_os_str().to_owned()])
            .with_cwd(work_dir.path())
            .with_env(env)
            .with_use_logged_in_user(false),
    )
    .await
    .expect("start client");

    let (request_tx, mut request_rx) = mpsc::unbounded_channel();
    let session = client
        .create_session(
            SessionConfig::default()
                .with_github_token(MODE_HANDLER_TOKEN)
                .with_handler(Arc::new(ModeHandler {
                    requests: request_tx,
                }))
                .approve_all_permissions(),
        )
        .await
        .expect("create session");

    let requested_event = tokio::spawn(wait_for_event(
        session.subscribe(),
        "exit_plan_mode.requested event",
        |event| {
            event.parsed_type() == SessionEventType::ExitPlanModeRequested
                && event
                    .typed_data::<ExitPlanModeRequestedData>()
                    .is_some_and(|data| data.summary == PLAN_SUMMARY)
        },
    ));
    let completed_event = tokio::spawn(wait_for_event(
        session.subscribe(),
        "exit_plan_mode.completed event",
        |event| {
            event.parsed_type() == SessionEventType::ExitPlanModeCompleted
                && event
                    .typed_data::<ExitPlanModeCompletedData>()
                    .is_some_and(|data| {
                        data.approved == Some(true)
                            && data.selected_action.as_deref() == Some("interactive")
                    })
        },
    ));
    let idle_event = tokio::spawn(wait_for_event(
        session.subscribe(),
        "session.idle event",
        |event| event.parsed_type() == SessionEventType::SessionIdle,
    ));

    let send_result = session
        .client()
        .call(
            "session.send",
            Some(json!({
                "sessionId": session.id().as_str(),
                "prompt": PLAN_PROMPT,
                "mode": "plan",
            })),
        )
        .await
        .expect("send plan-mode prompt");
    assert!(
        send_result.get("messageId").is_some(),
        "expected messageId in send result"
    );

    let (session_id, request) = tokio::time::timeout(Duration::from_secs(10), request_rx.recv())
        .await
        .expect("timed out waiting for exit-plan-mode request")
        .expect("exit-plan-mode request channel closed");
    assert_eq!(session_id, session.id().clone());
    assert_eq!(request.summary, PLAN_SUMMARY);
    assert_eq!(
        request.actions,
        ["interactive", "autopilot", "exit_only"].map(str::to_string)
    );
    assert_eq!(request.recommended_action, "interactive");

    let requested = requested_event
        .await
        .expect("requested task")
        .expect("requested event");
    let requested_data = requested
        .typed_data::<ExitPlanModeRequestedData>()
        .expect("typed requested event");
    assert_eq!(requested_data.summary, request.summary);
    assert_eq!(requested_data.actions, request.actions);
    assert_eq!(
        requested_data.recommended_action,
        request.recommended_action
    );

    let completed = completed_event
        .await
        .expect("completed task")
        .expect("completed event");
    let completed_data = completed
        .typed_data::<ExitPlanModeCompletedData>()
        .expect("typed completed event");
    assert_eq!(completed_data.approved, Some(true));
    assert_eq!(
        completed_data.selected_action.as_deref(),
        Some("interactive")
    );
    assert_eq!(
        completed_data.feedback.as_deref(),
        Some("Approved by the Rust E2E test")
    );
    idle_event.await.expect("idle task").expect("idle event");

    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");
    proxy.stop(true).expect("stop replay proxy");
}

#[tokio::test]
#[ignore] // requires the Node CLI and shared replay proxy dependencies
async fn should_invoke_auto_mode_switch_handler_when_rate_limited() {
    let repo_root = repo_root();
    let cli_path = repo_root
        .join("nodejs")
        .join("node_modules")
        .join("@github")
        .join("copilot")
        .join("index.js");
    assert!(
        cli_path.exists(),
        "CLI not found at {}; run npm install in nodejs first",
        cli_path.display()
    );

    let home_dir = tempfile::tempdir().expect("create home dir");
    let work_dir = tempfile::tempdir().expect("create work dir");
    let mut proxy = CapiProxy::start(&repo_root).expect("start replay proxy");
    proxy
        .configure(
            &repo_root
                .join("test")
                .join("snapshots")
                .join("mode_handlers")
                .join("should_invoke_auto_mode_switch_handler_when_rate_limited.yaml"),
            work_dir.path(),
        )
        .expect("configure replay proxy");
    proxy
        .set_copilot_user_by_token(
            MODE_HANDLER_TOKEN,
            json!({
                "login": "mode-handler-user",
                "copilot_plan": "individual_pro",
                "endpoints": {
                    "api": proxy.url(),
                    "telemetry": "https://localhost:1/telemetry"
                },
                "analytics_tracking_id": "mode-handler-tracking-id"
            }),
        )
        .expect("configure copilot user");

    let mut env = proxy.proxy_env();
    env.extend([
        ("COPILOT_API_URL".into(), proxy.url().into()),
        ("COPILOT_DEBUG_GITHUB_API_URL".into(), proxy.url().into()),
        (
            "COPILOT_HOME".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
        (
            "GH_CONFIG_DIR".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
        (
            "XDG_CONFIG_HOME".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
        (
            "XDG_STATE_HOME".into(),
            home_dir.path().as_os_str().to_owned(),
        ),
    ]);

    let client = Client::start(
        ClientOptions::new()
            .with_program(CliProgram::Path(PathBuf::from(node_program())))
            .with_prefix_args([cli_path.as_os_str().to_owned()])
            .with_cwd(work_dir.path())
            .with_env(env)
            .with_use_logged_in_user(false),
    )
    .await
    .expect("start client");

    let (request_tx, mut request_rx) = mpsc::unbounded_channel();
    let session = client
        .create_session(
            SessionConfig::default()
                .with_github_token(MODE_HANDLER_TOKEN)
                .with_handler(Arc::new(AutoModeHandler {
                    requests: request_tx,
                }))
                .approve_all_permissions(),
        )
        .await
        .expect("create session");

    let requested_event = tokio::spawn(wait_for_event_allowing_rate_limit(
        session.subscribe(),
        "auto_mode_switch.requested event",
        |event| {
            event.parsed_type() == SessionEventType::AutoModeSwitchRequested
                && event
                    .typed_data::<AutoModeSwitchRequestedData>()
                    .is_some_and(|data| {
                        data.error_code.as_deref() == Some("user_weekly_rate_limited")
                            && data.retry_after_seconds == Some(1.0)
                    })
        },
    ));
    let completed_event = tokio::spawn(wait_for_event_allowing_rate_limit(
        session.subscribe(),
        "auto_mode_switch.completed event",
        |event| {
            event.parsed_type() == SessionEventType::AutoModeSwitchCompleted
                && event
                    .typed_data::<AutoModeSwitchCompletedData>()
                    .is_some_and(|data| data.response == "yes")
        },
    ));
    let model_change_event = tokio::spawn(wait_for_event_allowing_rate_limit(
        session.subscribe(),
        "rate-limit auto-mode model change",
        |event| {
            event.parsed_type() == SessionEventType::SessionModelChange
                && event
                    .typed_data::<SessionModelChangeData>()
                    .is_some_and(|data| data.cause.as_deref() == Some("rate_limit_auto_switch"))
        },
    ));
    let idle_event = tokio::spawn(wait_for_event_allowing_rate_limit(
        session.subscribe(),
        "session.idle after auto-mode switch",
        |event| event.parsed_type() == SessionEventType::SessionIdle,
    ));

    let message_id = session
        .send(AUTO_MODE_PROMPT)
        .await
        .expect("send auto-mode-switch prompt");
    assert!(!message_id.is_empty(), "expected message ID");

    let (session_id, error_code, retry_after_seconds) =
        tokio::time::timeout(Duration::from_secs(10), request_rx.recv())
            .await
            .expect("timed out waiting for auto-mode-switch request")
            .expect("auto-mode-switch request channel closed");
    assert_eq!(session_id, session.id().clone());
    assert_eq!(error_code.as_deref(), Some("user_weekly_rate_limited"));
    assert_eq!(retry_after_seconds, Some(1.0));

    let requested = requested_event
        .await
        .expect("requested task")
        .expect("requested event");
    let requested_data = requested
        .typed_data::<AutoModeSwitchRequestedData>()
        .expect("typed requested event");
    assert_eq!(requested_data.error_code, error_code);
    assert_eq!(requested_data.retry_after_seconds, retry_after_seconds);

    let completed = completed_event
        .await
        .expect("completed task")
        .expect("completed event");
    let completed_data = completed
        .typed_data::<AutoModeSwitchCompletedData>()
        .expect("typed completed event");
    assert_eq!(completed_data.response, "yes");

    let model_change = model_change_event
        .await
        .expect("model change task")
        .expect("model change event");
    let model_change_data = model_change
        .typed_data::<SessionModelChangeData>()
        .expect("typed model change event");
    assert_eq!(
        model_change_data.cause.as_deref(),
        Some("rate_limit_auto_switch")
    );
    idle_event.await.expect("idle task").expect("idle event");

    session.disconnect().await.expect("disconnect session");
    client.stop().await.expect("stop client");
    proxy.stop(true).expect("stop replay proxy");
}

async fn wait_for_event(
    mut events: EventSubscription,
    description: &'static str,
    predicate: fn(&SessionEvent) -> bool,
) -> Result<SessionEvent, String> {
    tokio::time::timeout(Duration::from_secs(30), async {
        loop {
            let event = events.recv().await.map_err(|err| {
                format!("event stream closed while waiting for {description}: {err}")
            })?;
            if event.parsed_type() == SessionEventType::SessionError {
                return Err(format!(
                    "session.error while waiting for {description}: {}",
                    event.data
                ));
            }
            if predicate(&event) {
                return Ok(event);
            }
        }
    })
    .await
    .map_err(|_| format!("timed out waiting for {description}"))?
}

async fn wait_for_event_allowing_rate_limit(
    mut events: EventSubscription,
    description: &'static str,
    predicate: fn(&SessionEvent) -> bool,
) -> Result<SessionEvent, String> {
    tokio::time::timeout(Duration::from_secs(30), async {
        loop {
            let event = events.recv().await.map_err(|err| {
                format!("event stream closed while waiting for {description}: {err}")
            })?;
            if event.parsed_type() == SessionEventType::SessionError
                && event.data.get("errorType").and_then(|value| value.as_str())
                    != Some("rate_limit")
            {
                return Err(format!(
                    "session.error while waiting for {description}: {}",
                    event.data
                ));
            }
            if predicate(&event) {
                return Ok(event);
            }
        }
    })
    .await
    .map_err(|_| format!("timed out waiting for {description}"))?
}

fn repo_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .expect("rust package has parent repo")
        .to_path_buf()
}

struct CapiProxy {
    child: Option<Child>,
    proxy_url: String,
    connect_proxy_url: String,
    ca_file_path: String,
}

impl CapiProxy {
    fn start(repo_root: &Path) -> std::io::Result<Self> {
        let mut child = Command::new(npx_program())
            .args(["tsx", "server.ts"])
            .current_dir(repo_root.join("test").join("harness"))
            .stdin(Stdio::null())
            .stdout(Stdio::piped())
            .stderr(Stdio::inherit())
            .spawn()?;

        let stdout = child.stdout.take().expect("proxy stdout");
        let reader = BufReader::new(stdout);
        let re = regex::Regex::new(r"Listening: (http://[^\s]+)\s+(\{.*\})$").unwrap();
        for line in reader.lines() {
            let line = line?;
            if let Some(captures) = re.captures(&line) {
                let metadata: serde_json::Value =
                    serde_json::from_str(captures.get(2).unwrap().as_str())?;
                let connect_proxy_url = metadata
                    .get("connectProxyUrl")
                    .and_then(|value| value.as_str())
                    .expect("connectProxyUrl")
                    .to_string();
                let ca_file_path = metadata
                    .get("caFilePath")
                    .and_then(|value| value.as_str())
                    .expect("caFilePath")
                    .to_string();
                return Ok(Self {
                    child: Some(child),
                    proxy_url: captures.get(1).unwrap().as_str().to_string(),
                    connect_proxy_url,
                    ca_file_path,
                });
            }
            if line.contains("Listening: ") {
                return Err(std::io::Error::other(format!(
                    "proxy startup line missing metadata: {line}"
                )));
            }
        }

        Err(std::io::Error::other("proxy exited before startup"))
    }

    fn url(&self) -> &str {
        &self.proxy_url
    }

    fn configure(&self, file_path: &Path, work_dir: &Path) -> std::io::Result<()> {
        self.post_json(
            "/config",
            &json!({
                "filePath": file_path,
                "workDir": work_dir,
            })
            .to_string(),
        )
    }

    fn set_copilot_user_by_token(
        &self,
        token: &str,
        response: serde_json::Value,
    ) -> std::io::Result<()> {
        self.post_json(
            "/copilot-user-config",
            &json!({
                "token": token,
                "response": response,
            })
            .to_string(),
        )
    }

    fn stop(&mut self, skip_writing_cache: bool) -> std::io::Result<()> {
        let path = if skip_writing_cache {
            "/stop?skipWritingCache=true"
        } else {
            "/stop"
        };
        let result = self.post_json(path, "");
        if let Some(mut child) = self.child.take() {
            let _ = child.wait();
        }
        result
    }

    fn proxy_env(&self) -> Vec<(std::ffi::OsString, std::ffi::OsString)> {
        let no_proxy = "127.0.0.1,localhost,::1";
        [
            ("HTTP_PROXY", self.connect_proxy_url.as_str()),
            ("HTTPS_PROXY", self.connect_proxy_url.as_str()),
            ("http_proxy", self.connect_proxy_url.as_str()),
            ("https_proxy", self.connect_proxy_url.as_str()),
            ("NO_PROXY", no_proxy),
            ("no_proxy", no_proxy),
            ("NODE_EXTRA_CA_CERTS", self.ca_file_path.as_str()),
            ("SSL_CERT_FILE", self.ca_file_path.as_str()),
            ("REQUESTS_CA_BUNDLE", self.ca_file_path.as_str()),
            ("CURL_CA_BUNDLE", self.ca_file_path.as_str()),
            ("GIT_SSL_CAINFO", self.ca_file_path.as_str()),
            ("GH_TOKEN", ""),
            ("GITHUB_TOKEN", ""),
            ("GH_ENTERPRISE_TOKEN", ""),
            ("GITHUB_ENTERPRISE_TOKEN", ""),
        ]
        .into_iter()
        .map(|(key, value)| (key.into(), value.into()))
        .collect()
    }

    fn post_json(&self, path: &str, body: &str) -> std::io::Result<()> {
        let (host, port) = parse_http_url(&self.proxy_url)?;
        let mut stream = TcpStream::connect((host.as_str(), port))?;
        write!(
            stream,
            "POST {path} HTTP/1.1\r\nHost: {host}:{port}\r\nContent-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
            body.len()
        )?;

        let mut response = String::new();
        stream.read_to_string(&mut response)?;
        if !response.starts_with("HTTP/1.1 200") && !response.starts_with("HTTP/1.1 204") {
            return Err(std::io::Error::other(format!(
                "proxy POST {path} failed: {response}"
            )));
        }
        Ok(())
    }
}

impl Drop for CapiProxy {
    fn drop(&mut self) {
        if self.child.is_some() {
            let _ = self.stop(true);
        }
    }
}

fn node_program() -> &'static str {
    if cfg!(windows) { "node.exe" } else { "node" }
}

fn npx_program() -> &'static str {
    if cfg!(windows) { "npx.cmd" } else { "npx" }
}

fn parse_http_url(url: &str) -> std::io::Result<(String, u16)> {
    let without_scheme = url
        .strip_prefix("http://")
        .ok_or_else(|| std::io::Error::other(format!("expected http URL, got {url}")))?;
    let authority = without_scheme.split('/').next().unwrap_or(without_scheme);
    let (host, port) = authority
        .rsplit_once(':')
        .ok_or_else(|| std::io::Error::other(format!("missing port in URL {url}")))?;
    let port = port
        .parse()
        .map_err(|err| std::io::Error::other(format!("invalid port in URL {url}: {err}")))?;
    Ok((host.to_string(), port))
}
