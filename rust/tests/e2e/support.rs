use std::ffi::OsString;
use std::future::Future;
use std::io::{BufRead, BufReader, Read, Write};
use std::net::TcpStream;
use std::path::{Path, PathBuf};
use std::pin::Pin;
use std::process::{Child, Command, Stdio};
use std::sync::LazyLock;
use std::time::Duration;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::session::Session;
use github_copilot_sdk::subscription::{EventSubscription, LifecycleSubscription};
use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, SessionConfig, SessionEvent, SessionId,
    SessionLifecycleEvent, Transport,
};
use serde_json::json;
use tokio::sync::Semaphore;

static E2E_CONCURRENCY: LazyLock<Semaphore> = LazyLock::new(|| Semaphore::new(e2e_concurrency()));

pub const DEFAULT_TEST_TOKEN: &str = "rust-e2e-token";

type TestFuture<'a> = Pin<Box<dyn Future<Output = ()> + 'a>>;

pub async fn with_e2e_context<F>(category: &str, snapshot_name: &str, test: F)
where
    F: for<'a> FnOnce(&'a mut E2eContext) -> TestFuture<'a>,
{
    let _permit = E2E_CONCURRENCY
        .acquire()
        .await
        .expect("E2E concurrency semaphore should stay open");
    let mut ctx = E2eContext::new(category, snapshot_name)
        .await
        .unwrap_or_else(|err| panic!("create E2E context: {err}"));

    let timed_out = tokio::time::timeout(default_test_timeout(), test(&mut ctx))
        .await
        .is_err();
    ctx.cleanup(timed_out)
        .await
        .unwrap_or_else(|err| panic!("clean up E2E context: {err}"));
    assert!(
        !timed_out,
        "timed out after {:?} running E2E test {category}/{snapshot_name}",
        default_test_timeout()
    );
}

pub struct E2eContext {
    repo_root: PathBuf,
    cli_path: PathBuf,
    home_dir: tempfile::TempDir,
    work_dir: tempfile::TempDir,
    proxy: Option<CapiProxy>,
}

impl E2eContext {
    async fn new(category: &str, snapshot_name: &str) -> std::io::Result<Self> {
        let repo_root = repo_root();
        let cli_path = cli_path(&repo_root)?;
        let home_dir = tempfile::tempdir()?;
        let work_dir = tempfile::tempdir()?;
        let proxy_root = repo_root.clone();
        let proxy = tokio::task::spawn_blocking(move || CapiProxy::start(&proxy_root))
            .await
            .map_err(|err| std::io::Error::other(format!("proxy startup task failed: {err}")))??;
        let mut ctx = Self {
            repo_root,
            cli_path,
            home_dir,
            work_dir,
            proxy: Some(proxy),
        };
        ctx.configure(category, snapshot_name)?;
        Ok(ctx)
    }

    #[expect(dead_code, reason = "used by follow-on E2E ports")]
    pub fn repo_root(&self) -> &Path {
        &self.repo_root
    }

    pub fn work_dir(&self) -> &Path {
        self.work_dir.path()
    }

    pub fn proxy_url(&self) -> &str {
        self.proxy().url()
    }

    pub fn snapshot_path(&self, category: &str, snapshot_name: &str) -> PathBuf {
        self.repo_root
            .join("test")
            .join("snapshots")
            .join(category)
            .join(format!("{snapshot_name}.yaml"))
    }

    pub fn client_options(&self) -> ClientOptions {
        ClientOptions::new()
            .with_program(CliProgram::Path(PathBuf::from(node_program())))
            .with_prefix_args([self.cli_path.as_os_str().to_owned()])
            .with_cwd(self.work_dir.path())
            .with_env(self.environment())
            .with_use_logged_in_user(false)
    }

    pub fn client_options_with_transport(&self, transport: Transport) -> ClientOptions {
        self.client_options().with_transport(transport)
    }

    pub async fn start_client(&self) -> Client {
        Client::start(self.client_options())
            .await
            .expect("start E2E client")
    }

    #[expect(dead_code, reason = "used by follow-on E2E ports")]
    pub async fn start_tcp_client(&self, port: u16, token: &str) -> Client {
        Client::start(
            self.client_options_with_transport(Transport::Tcp { port })
                .with_tcp_connection_token(token),
        )
        .await
        .expect("start TCP E2E client")
    }

    pub fn approve_all_session_config(&self) -> SessionConfig {
        SessionConfig::default()
            .with_handler(std::sync::Arc::new(ApproveAllHandler))
            .with_github_token(DEFAULT_TEST_TOKEN)
    }

    pub fn set_default_copilot_user(&self) {
        self.set_copilot_user_by_token(DEFAULT_TEST_TOKEN);
    }

    pub fn set_copilot_user_by_token(&self, token: &str) {
        self.set_copilot_user_by_token_with_login(token, "rust-e2e-user");
    }

    pub fn set_copilot_user_by_token_with_login(&self, token: &str, login: &str) {
        self.set_copilot_user_by_token_with_login_and_quota(token, login, None);
    }

    pub fn set_copilot_user_by_token_with_login_and_quota(
        &self,
        token: &str,
        login: &str,
        quota_snapshots: Option<serde_json::Value>,
    ) {
        let mut user = json!({
            "login": login,
            "copilot_plan": "individual_pro",
            "endpoints": {
                "api": self.proxy_url(),
                "telemetry": "https://localhost:1/telemetry"
            },
            "analytics_tracking_id": "rust-e2e-tracking-id"
        });
        if let Some(quota_snapshots) = quota_snapshots {
            user["quota_snapshots"] = quota_snapshots;
        }
        self.proxy()
            .set_copilot_user_by_token(token, user)
            .expect("configure copilot user");
    }

    pub fn exchanges(&self) -> Vec<serde_json::Value> {
        self.proxy()
            .get_json("/exchanges")
            .expect("get captured proxy exchanges")
    }

    pub async fn cleanup(&mut self, skip_writing_cache: bool) -> std::io::Result<()> {
        if let Some(mut proxy) = self.proxy.take() {
            tokio::task::spawn_blocking(move || proxy.stop(skip_writing_cache))
                .await
                .map_err(|err| {
                    std::io::Error::other(format!("proxy shutdown task failed: {err}"))
                })??;
        }
        Ok(())
    }

    fn configure(&mut self, category: &str, snapshot_name: &str) -> std::io::Result<()> {
        let snapshot_path = self.snapshot_path(category, snapshot_name);
        self.proxy()
            .configure(&snapshot_path, self.work_dir.path())
            .map_err(|err| {
                std::io::Error::other(format!(
                    "configure proxy for {} failed: {err}",
                    snapshot_path.display()
                ))
            })
    }

    fn environment(&self) -> Vec<(OsString, OsString)> {
        let mut env = self.proxy().proxy_env();
        env.extend([
            ("COPILOT_API_URL".into(), self.proxy_url().into()),
            (
                "COPILOT_DEBUG_GITHUB_API_URL".into(),
                self.proxy_url().into(),
            ),
            (
                "COPILOT_HOME".into(),
                canonical_temp_path(self.home_dir.path())
                    .as_os_str()
                    .to_owned(),
            ),
            (
                "GH_CONFIG_DIR".into(),
                canonical_temp_path(self.home_dir.path())
                    .as_os_str()
                    .to_owned(),
            ),
            (
                "XDG_CONFIG_HOME".into(),
                canonical_temp_path(self.home_dir.path())
                    .as_os_str()
                    .to_owned(),
            ),
            (
                "XDG_STATE_HOME".into(),
                canonical_temp_path(self.home_dir.path())
                    .as_os_str()
                    .to_owned(),
            ),
        ]);
        if std::env::var("GITHUB_ACTIONS").as_deref() == Ok("true") {
            env.push(("GH_TOKEN".into(), "fake-token-for-e2e-tests".into()));
            env.push(("GITHUB_TOKEN".into(), "fake-token-for-e2e-tests".into()));
        }
        env
    }

    fn proxy(&self) -> &CapiProxy {
        self.proxy.as_ref().expect("proxy already stopped")
    }
}

impl Drop for E2eContext {
    fn drop(&mut self) {
        if let Some(mut proxy) = self.proxy.take() {
            let _ = proxy.stop(true);
        }
    }
}

pub async fn wait_for_event<P>(
    events: EventSubscription,
    description: &'static str,
    predicate: P,
) -> SessionEvent
where
    P: Fn(&SessionEvent) -> bool,
{
    wait_for_event_core(events, description, predicate, false).await
}

pub async fn wait_for_event_allowing_rate_limit<P>(
    events: EventSubscription,
    description: &'static str,
    predicate: P,
) -> SessionEvent
where
    P: Fn(&SessionEvent) -> bool,
{
    wait_for_event_core(events, description, predicate, true).await
}

async fn wait_for_event_core<P>(
    mut events: EventSubscription,
    description: &'static str,
    predicate: P,
    allow_rate_limit_error: bool,
) -> SessionEvent
where
    P: Fn(&SessionEvent) -> bool,
{
    tokio::time::timeout(default_event_timeout(), async {
        loop {
            let event = events.recv().await.unwrap_or_else(|err| {
                panic!("event stream closed while waiting for {description}: {err}")
            });
            let is_allowed_rate_limit = allow_rate_limit_error
                && event.parsed_type()
                    == github_copilot_sdk::generated::session_events::SessionEventType::SessionError
                && event.data.get("errorType").and_then(|value| value.as_str())
                    == Some("rate_limit");
            if event.parsed_type()
                == github_copilot_sdk::generated::session_events::SessionEventType::SessionError
                && !is_allowed_rate_limit
            {
                panic!(
                    "session.error while waiting for {description}: {}",
                    event.data
                );
            }
            if predicate(&event) {
                return event;
            }
        }
    })
    .await
    .unwrap_or_else(|_| panic!("timed out waiting for {description}"))
}

pub async fn recv_with_timeout<T>(
    receiver: &mut tokio::sync::mpsc::UnboundedReceiver<T>,
    description: &'static str,
) -> T {
    tokio::time::timeout(default_event_timeout(), receiver.recv())
        .await
        .unwrap_or_else(|_| panic!("timed out waiting for {description}"))
        .unwrap_or_else(|| panic!("{description} channel closed"))
}

pub async fn wait_for_lifecycle_event<P>(
    mut events: LifecycleSubscription,
    description: &'static str,
    predicate: P,
) -> SessionLifecycleEvent
where
    P: Fn(&SessionLifecycleEvent) -> bool,
{
    tokio::time::timeout(default_event_timeout(), async {
        loop {
            let event = events.recv().await.unwrap_or_else(|err| {
                panic!("lifecycle stream closed while waiting for {description}: {err}")
            });
            if predicate(&event) {
                return event;
            }
        }
    })
    .await
    .unwrap_or_else(|_| panic!("timed out waiting for {description}"))
}

pub async fn wait_for_condition<F, Fut>(description: &'static str, mut predicate: F)
where
    F: FnMut() -> Fut,
    Fut: Future<Output = bool>,
{
    let deadline = tokio::time::Instant::now() + default_event_timeout();
    loop {
        if predicate().await {
            return;
        }
        assert!(
            tokio::time::Instant::now() < deadline,
            "timed out waiting for {description}"
        );
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
}

pub async fn collect_until_idle(mut events: EventSubscription) -> Vec<SessionEvent> {
    let mut observed = Vec::new();
    tokio::time::timeout(default_event_timeout(), async {
        loop {
            let event = events
                .recv()
                .await
                .unwrap_or_else(|err| panic!("event stream closed while collecting events: {err}"));
            let is_idle = event.parsed_type()
                == github_copilot_sdk::generated::session_events::SessionEventType::SessionIdle;
            if event.parsed_type()
                == github_copilot_sdk::generated::session_events::SessionEventType::SessionError
            {
                panic!("session.error while collecting events: {}", event.data);
            }
            observed.push(event);
            if is_idle {
                return;
            }
        }
    })
    .await
    .expect("timed out collecting events through session.idle");
    observed
}

pub fn event_types(events: &[SessionEvent]) -> Vec<&str> {
    events
        .iter()
        .map(|event| event.event_type.as_str())
        .collect()
}

#[allow(dead_code, reason = "used by follow-on E2E ports")]
pub async fn wait_for_idle(session: &Session) -> SessionEvent {
    wait_for_event(session.subscribe(), "session.idle event", |event| {
        event.parsed_type()
            == github_copilot_sdk::generated::session_events::SessionEventType::SessionIdle
    })
    .await
}

#[allow(dead_code, reason = "used by follow-on E2E ports")]
pub async fn wait_for_final_assistant_message(session: &Session) -> SessionEvent {
    wait_for_idle(session).await;
    last_assistant_message(session).await
}

#[allow(dead_code, reason = "used by follow-on E2E ports")]
pub async fn last_assistant_message(session: &Session) -> SessionEvent {
    session
        .get_messages()
        .await
        .expect("get session messages")
        .into_iter()
        .rev()
        .find(|event| {
            event.parsed_type()
                == github_copilot_sdk::generated::session_events::SessionEventType::AssistantMessage
        })
        .expect("assistant.message event")
}

pub fn assistant_message_content(event: &SessionEvent) -> String {
    event
        .typed_data::<github_copilot_sdk::generated::session_events::AssistantMessageData>()
        .expect("assistant.message data")
        .content
}

pub fn assert_uuid_like(session_id: &SessionId) {
    let text = session_id.as_str();
    let parsed = uuid::Uuid::parse_str(text).expect("session id should be UUID-shaped");
    assert_eq!(
        parsed.hyphenated().to_string(),
        text,
        "session id should use canonical hyphenated UUID formatting"
    );
}

fn default_event_timeout() -> Duration {
    if cfg!(windows) {
        Duration::from_secs(120)
    } else {
        Duration::from_secs(60)
    }
}

fn default_test_timeout() -> Duration {
    if cfg!(windows) {
        Duration::from_secs(300)
    } else {
        Duration::from_secs(180)
    }
}

fn e2e_concurrency() -> usize {
    std::env::var("RUST_E2E_CONCURRENCY")
        .ok()
        .and_then(|value| value.parse::<usize>().ok())
        .filter(|&value| value > 0)
        .unwrap_or(4)
}

pub fn get_system_message(exchange: &serde_json::Value) -> String {
    exchange
        .get("request")
        .and_then(|request| request.get("messages"))
        .and_then(serde_json::Value::as_array)
        .and_then(|messages| {
            messages.iter().find_map(|message| {
                let role = message.get("role").and_then(serde_json::Value::as_str)?;
                if role == "system" {
                    message
                        .get("content")
                        .and_then(serde_json::Value::as_str)
                        .map(str::to_string)
                } else {
                    None
                }
            })
        })
        .unwrap_or_default()
}

pub fn get_tool_names(exchange: &serde_json::Value) -> Vec<String> {
    exchange
        .get("request")
        .and_then(|request| request.get("tools"))
        .and_then(serde_json::Value::as_array)
        .map(|tools| {
            tools
                .iter()
                .filter_map(|tool| {
                    tool.get("function")
                        .and_then(|function| function.get("name"))
                        .and_then(serde_json::Value::as_str)
                        .map(str::to_string)
                })
                .collect()
        })
        .unwrap_or_default()
}

fn repo_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .expect("rust package has parent repo")
        .to_path_buf()
}

fn cli_path(repo_root: &Path) -> std::io::Result<PathBuf> {
    if let Some(path) = std::env::var_os("COPILOT_CLI_PATH") {
        let path = PathBuf::from(path);
        if path.exists() {
            return Ok(path);
        }
    }

    let path = repo_root
        .join("nodejs")
        .join("node_modules")
        .join("@github")
        .join("copilot")
        .join("index.js");
    if path.exists() {
        return Ok(path);
    }

    Err(std::io::Error::new(
        std::io::ErrorKind::NotFound,
        format!(
            "CLI not found at {}; run npm install in nodejs first",
            path.display()
        ),
    ))
}

fn canonical_temp_path(path: &Path) -> PathBuf {
    std::fs::canonicalize(path).unwrap_or_else(|_| path.to_path_buf())
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
            .env("GITHUB_ACTIONS", "true")
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

    fn proxy_env(&self) -> Vec<(OsString, OsString)> {
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
        let response = self.request("POST", path, body)?;
        if !response.starts_with("HTTP/1.1 200") && !response.starts_with("HTTP/1.1 204") {
            return Err(std::io::Error::other(format!(
                "proxy POST {path} failed: {response}"
            )));
        }
        Ok(())
    }

    fn get_json<T: serde::de::DeserializeOwned>(&self, path: &str) -> std::io::Result<T> {
        let response = self.request("GET", path, "")?;
        if !response.starts_with("HTTP/1.1 200") {
            return Err(std::io::Error::other(format!(
                "proxy GET {path} failed: {response}"
            )));
        }
        let body = response_body(&response)?;
        serde_json::from_str(&body).map_err(std::io::Error::other)
    }

    fn request(&self, method: &str, path: &str, body: &str) -> std::io::Result<String> {
        let (host, port) = parse_http_url(&self.proxy_url)?;
        let mut stream = TcpStream::connect((host.as_str(), port))?;
        write!(
            stream,
            "{method} {path} HTTP/1.1\r\nHost: {host}:{port}\r\nContent-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
            body.len()
        )?;

        let mut response = String::new();
        stream.read_to_string(&mut response)?;
        Ok(response)
    }
}

impl Drop for CapiProxy {
    fn drop(&mut self) {
        if self.child.is_some() {
            let _ = self.stop(true);
        }
    }
}

fn response_body(response: &str) -> std::io::Result<String> {
    let Some((headers, body)) = response.split_once("\r\n\r\n") else {
        return Ok(String::new());
    };
    if headers
        .lines()
        .any(|line| line.eq_ignore_ascii_case("Transfer-Encoding: chunked"))
    {
        return decode_chunked_body(body);
    }
    Ok(body.to_string())
}

fn decode_chunked_body(body: &str) -> std::io::Result<String> {
    let mut rest = body;
    let mut decoded = String::new();
    loop {
        let Some((size_line, after_size)) = rest.split_once("\r\n") else {
            return Err(std::io::Error::other("malformed chunked response"));
        };
        let size_text = size_line
            .split_once(';')
            .map_or(size_line, |(size, _)| size);
        let size = usize::from_str_radix(size_text.trim(), 16)
            .map_err(|err| std::io::Error::other(format!("invalid chunk size: {err}")))?;
        if size == 0 {
            return Ok(decoded);
        }
        if after_size.len() < size + 2 {
            return Err(std::io::Error::other("truncated chunked response"));
        }
        decoded.push_str(&after_size[..size]);
        rest = &after_size[size + 2..];
    }
}

fn parse_http_url(url: &str) -> std::io::Result<(String, u16)> {
    let without_scheme = url
        .strip_prefix("http://")
        .ok_or_else(|| std::io::Error::other(format!("unsupported proxy URL: {url}")))?;
    let (host, port) = without_scheme
        .rsplit_once(':')
        .ok_or_else(|| std::io::Error::other(format!("proxy URL missing port: {url}")))?;
    let port = port
        .parse::<u16>()
        .map_err(|err| std::io::Error::other(format!("invalid proxy URL port: {err}")))?;
    Ok((host.to_string(), port))
}

fn node_program() -> &'static str {
    if cfg!(windows) { "node.exe" } else { "node" }
}

fn npx_program() -> &'static str {
    if cfg!(windows) { "npx.cmd" } else { "npx" }
}
