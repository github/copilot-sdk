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
    CliProgram, Client, ClientOptions, CopilotRequestHandler, SessionConfig, SessionEvent,
    SessionId, SessionLifecycleEvent, Transport,
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

    // In-process hosting: the runtime loads into this test process and its worker
    // inherits the ambient environment (per-client env is not honored in-process, see
    // https://github.com/github/copilot-sdk/issues/1934), so mirror this context's env
    // onto the process for the duration of the test and restore on drop. Safe because
    // E2E_CONCURRENCY is 1 in-process, serializing the whole critical section.
    let _env_guard = InProcessEnvGuard::activate(&ctx);

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

/// Like [`with_e2e_context`] but starts the CapiProxy without loading a
/// recorded snapshot. Used by the LLM inference callback tests, whose
/// registered provider fabricates every model-layer response so no CAPI
/// replay is needed — only the auth/user endpoints are served by the proxy.
pub async fn with_e2e_context_no_snapshot<F>(test: F)
where
    F: for<'a> FnOnce(&'a mut E2eContext) -> TestFuture<'a>,
{
    let _permit = E2E_CONCURRENCY
        .acquire()
        .await
        .expect("E2E concurrency semaphore should stay open");
    let mut ctx = E2eContext::new_no_snapshot()
        .await
        .unwrap_or_else(|err| panic!("create E2E context: {err}"));

    // See `with_e2e_context` for why the in-process transport mirrors env onto the
    // process (restored on drop).
    let _env_guard = InProcessEnvGuard::activate(&ctx);

    let timed_out = tokio::time::timeout(default_test_timeout(), test(&mut ctx))
        .await
        .is_err();
    ctx.cleanup(timed_out)
        .await
        .unwrap_or_else(|err| panic!("clean up E2E context: {err}"));
    assert!(
        !timed_out,
        "timed out after {:?} running no-snapshot E2E test",
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
        ctx.set_default_copilot_user();
        Ok(ctx)
    }

    async fn new_no_snapshot() -> std::io::Result<Self> {
        let repo_root = repo_root();
        let cli_path = cli_path(&repo_root)?;
        let home_dir = tempfile::tempdir()?;
        let work_dir = tempfile::tempdir()?;
        let proxy_root = repo_root.clone();
        let proxy = tokio::task::spawn_blocking(move || CapiProxy::start(&proxy_root))
            .await
            .map_err(|err| std::io::Error::other(format!("proxy startup task failed: {err}")))??;
        let ctx = Self {
            repo_root,
            cli_path,
            home_dir,
            work_dir,
            proxy: Some(proxy),
        };
        // Initialize proxy state without replaying any recorded exchanges: the
        // snapshot path intentionally does not exist, so `/copilot_internal/user`
        // and the default `/models` catalog are served while all model-layer
        // traffic is fabricated by the registered inference callback.
        let dummy_snapshot = ctx.work_dir.path().join("__no_snapshot__.yaml");
        ctx.proxy()
            .configure(&dummy_snapshot, ctx.work_dir.path())
            .map_err(|err| {
                std::io::Error::other(format!("configure proxy without snapshot failed: {err}"))
            })?;
        ctx.set_default_copilot_user();
        Ok(ctx)
    }

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
        client_options_for_cli(&self.cli_path, self.work_dir.path(), self.environment())
    }

    pub fn client_options_with_transport(&self, transport: Transport) -> ClientOptions {
        self.client_options().with_transport(transport)
    }

    pub fn client_options_with_github_token(&self, token: &str) -> ClientOptions {
        let options = self.client_options();
        if is_inprocess_default() {
            // SAFETY: the in-process E2E suite is serialized for the full
            // lifetime of InProcessEnvGuard.
            unsafe {
                std::env::set_var("GH_TOKEN", token);
                std::env::set_var("GITHUB_TOKEN", token);
            }
            options
        } else {
            options.with_github_token(token)
        }
    }

    pub async fn start_client(&self) -> Client {
        Client::start(self.client_options())
            .await
            .expect("start E2E client")
    }

    /// Start a client that hosts the runtime in-process over FFI
    /// ([`Transport::InProcess`]). Unlike the stdio harness, the CLI
    /// entrypoint is passed as the program directly (the FFI host builds the
    /// `node <entrypoint> --embedded-host` argv itself and loads the sibling
    /// runtime cdylib), so a `.js` entrypoint is not split into node +
    /// prefix_args here.
    pub async fn start_inprocess_client(&self) -> Client {
        let options = ClientOptions::new()
            .with_use_logged_in_user(false)
            .with_program(CliProgram::Path(self.cli_path.clone()))
            .with_transport(Transport::InProcess);
        Client::start(options)
            .await
            .expect("start in-process FFI E2E client")
    }

    /// Start a client wired to a Copilot request handler, appending `extra_env`
    /// to the spawned runtime's environment (used to flip the WebSocket ExP
    /// flag for the WS transport tests).
    pub async fn start_llm_client<H>(&self, handler: H, extra_env: &[(&str, &str)]) -> Client
    where
        H: CopilotRequestHandler,
    {
        let mut env = self.environment();
        env.extend(
            extra_env
                .iter()
                .map(|(key, value)| (OsString::from(*key), OsString::from(*value))),
        );
        let options = client_options_for_cli(&self.cli_path, self.work_dir.path(), env)
            .with_request_handler(handler);
        Client::start(options).await.expect("start E2E LLM client")
    }

    #[expect(dead_code, reason = "used by follow-on E2E ports")]
    pub async fn start_tcp_client(&self, port: u16, token: &str) -> Client {
        Client::start(self.client_options_with_transport(Transport::Tcp {
            port,
            connection_token: Some(token.to_string()),
        }))
        .await
        .expect("start TCP E2E client")
    }

    pub fn approve_all_session_config(&self) -> SessionConfig {
        SessionConfig::default()
            .with_permission_handler(std::sync::Arc::new(ApproveAllHandler))
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
            ("COPILOT_MCP_APPS".into(), "true".into()),
            ("MCP_APPS".into(), "true".into()),
            ("GH_TOKEN".into(), DEFAULT_TEST_TOKEN.into()),
            ("GITHUB_TOKEN".into(), DEFAULT_TEST_TOKEN.into()),
            ("GH_ENTERPRISE_TOKEN".into(), "".into()),
            ("GITHUB_ENTERPRISE_TOKEN".into(), "".into()),
            ("COPILOT_HMAC_KEY".into(), "".into()),
            ("CAPI_HMAC_KEY".into(), "".into()),
        ]);
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
                    == github_copilot_sdk::session_events::SessionEventType::SessionError
                && event.data.get("errorType").and_then(|value| value.as_str())
                    == Some("rate_limit");
            if event.parsed_type()
                == github_copilot_sdk::session_events::SessionEventType::SessionError
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
                == github_copilot_sdk::session_events::SessionEventType::SessionIdle;
            if event.parsed_type()
                == github_copilot_sdk::session_events::SessionEventType::SessionError
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
        event.parsed_type() == github_copilot_sdk::session_events::SessionEventType::SessionIdle
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
        .get_events()
        .await
        .expect("get session messages")
        .into_iter()
        .rev()
        .find(|event| {
            event.parsed_type()
                == github_copilot_sdk::session_events::SessionEventType::AssistantMessage
        })
        .expect("assistant.message event")
}

pub fn assistant_message_content(event: &SessionEvent) -> String {
    event
        .typed_data::<github_copilot_sdk::session_events::AssistantMessageData>()
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
    // The in-process transport mirrors per-test environment onto the shared process
    // environment (see `InProcessEnvGuard`), which is only coherent when one test runs
    // at a time. Force serial execution in-process; otherwise honor RUST_E2E_CONCURRENCY.
    if is_inprocess_default() {
        return 1;
    }
    std::env::var("RUST_E2E_CONCURRENCY")
        .ok()
        .and_then(|value| value.parse::<usize>().ok())
        .filter(|&value| value > 0)
        .unwrap_or(4)
}

/// True when the E2E suite runs over the in-process (FFI) transport, i.e. the SDK
/// resolves `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` to [`Transport::InProcess`].
pub fn is_inprocess_default() -> bool {
    std::env::var("COPILOT_SDK_DEFAULT_CONNECTION")
        .map(|value| value.eq_ignore_ascii_case("inprocess"))
        .unwrap_or(false)
}

/// Skip guard for E2E tests exercising features the in-process (FFI) transport does not
/// support (the runtime loads into the shared host process). Returns `true` — and logs —
/// when running in-process so the caller can `return` early; such tests remain covered
/// by the default (stdio) transport. See <https://github.com/github/copilot-sdk/issues/1934>.
pub fn skip_inprocess(reason: &str) -> bool {
    if is_inprocess_default() {
        eprintln!("skipping test over the in-process (FFI) transport: {reason}");
        true
    } else {
        false
    }
}

/// Mirrors an [`E2eContext`]'s environment onto the real process environment for the
/// in-process transport, whose worker inherits this process's ambient environment
/// rather than a per-client env block. Restores the previous values on drop. Only the
/// in-process transport needs this; for stdio/tcp the environment is handed to the
/// spawned child directly. Auth flows via GH_TOKEN/GITHUB_TOKEN and HMAC is disabled so
/// host-side auth resolution picks the token the replay snapshots expect.
struct InProcessEnvGuard {
    saved: Vec<(OsString, Option<OsString>)>,
    previous_cwd: PathBuf,
}

impl InProcessEnvGuard {
    /// Returns `Some` guard (having applied the env) when in-process, else `None`.
    fn activate(ctx: &E2eContext) -> Option<Self> {
        if !is_inprocess_default() {
            return None;
        }
        let mut pairs: Vec<(OsString, OsString)> = ctx.environment();
        pairs.push(("COPILOT_SDK_AUTH_TOKEN".into(), "".into()));
        // Some tests opt into gated runtime APIs via per-client `options.env`, which the
        // in-process transport does not pass to the shared worker (see issue #1934).
        // These are process-global runtime gates (not per-client behavior), so applying
        // them to the host process for the serial in-process suite is equivalent and
        // inert for tests that don't exercise the gated API.
        pairs.push(("COPILOT_ALLOW_GET_PROVIDER_ENDPOINT".into(), "true".into()));
        pairs.push((
            "COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES".into(),
            "true".into(),
        ));
        pairs.push((
            "COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS".into(),
            "true".into(),
        ));

        let mut saved: Vec<(OsString, Option<OsString>)> = Vec::new();
        for (key, value) in &pairs {
            saved.push((key.clone(), std::env::var_os(key)));
            // SAFETY: the E2E suite runs serially in-process (concurrency 1), so no
            // other thread races these process-wide env mutations.
            unsafe { std::env::set_var(key, value) };
        }
        let previous_cwd = std::env::current_dir().expect("read in-process test cwd");
        std::env::set_current_dir(ctx.work_dir()).expect("set in-process test cwd");
        Some(Self {
            saved,
            previous_cwd,
        })
    }
}

impl Drop for InProcessEnvGuard {
    fn drop(&mut self) {
        std::env::set_current_dir(&self.previous_cwd).expect("restore in-process test cwd");
        for (key, previous) in self.saved.iter().rev() {
            // SAFETY: as in `activate` — serial execution in-process.
            match previous {
                Some(value) => unsafe { std::env::set_var(key, value) },
                None => unsafe { std::env::remove_var(key) },
            }
        }
    }
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

    // The `@github/copilot` package is a thin loader; the runnable `index.js`
    // ships in a platform-specific `@github/copilot-<platform>-<arch>` package,
    // exactly one of which is installed. Resolve whichever one is present.
    let github_dir = repo_root
        .join("nodejs")
        .join("node_modules")
        .join("@github");
    if let Ok(entries) = std::fs::read_dir(&github_dir) {
        for entry in entries.flatten() {
            if entry.file_name().to_string_lossy().starts_with("copilot-") {
                let candidate = entry.path().join("index.js");
                if candidate.exists() {
                    return Ok(candidate);
                }
            }
        }
    }

    Err(std::io::Error::new(
        std::io::ErrorKind::NotFound,
        format!(
            "CLI not found under {}; run npm install in nodejs first",
            github_dir.display()
        ),
    ))
}

#[allow(deprecated)]
fn client_options_for_cli(
    cli_path: &Path,
    cwd: &Path,
    env: Vec<(OsString, OsString)>,
) -> ClientOptions {
    // When the in-process FFI transport is the default (matrix cell that sets
    // COPILOT_SDK_DEFAULT_CONNECTION=inprocess), pass the CLI entrypoint
    // directly: the FFI host builds the `node <entrypoint> --embedded-host`
    // argv itself and loads the sibling runtime cdylib. Splitting a `.js`
    // entrypoint into node + prefix_args (the stdio layout) would point the
    // library resolver at node's directory instead.
    let inprocess_default = std::env::var("COPILOT_SDK_DEFAULT_CONNECTION")
        .map(|value| value.eq_ignore_ascii_case("inprocess"))
        .unwrap_or(false);
    if inprocess_default {
        return ClientOptions::new().with_program(CliProgram::Path(cli_path.to_path_buf()));
    }
    let options = ClientOptions::new()
        .with_cwd(cwd)
        .with_env(env)
        .with_use_logged_in_user(false);
    if cli_path
        .extension()
        .and_then(|extension| extension.to_str())
        .is_some_and(|extension| extension.eq_ignore_ascii_case("js"))
    {
        options
            .with_program(CliProgram::Path(PathBuf::from(node_program())))
            .with_prefix_args([cli_path.as_os_str().to_owned()])
    } else {
        options.with_program(CliProgram::Path(cli_path.to_path_buf()))
    }
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
