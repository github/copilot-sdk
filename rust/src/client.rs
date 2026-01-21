//! CopilotClient implementation for managing the Copilot CLI connection.

use crate::error::{CopilotError, Result};
use crate::generated::SessionEvent;
use crate::jsonrpc::JsonRpcClient;
use crate::session::CopilotSession;
use crate::tool::{ToolInvocation, ToolResult};
use crate::types::{
    ClientOptions, ConnectionState, PingResponse, ProviderConfig,
    ResumeSessionConfig, SessionConfig, SessionMetadata, get_sdk_protocol_version,
};
use once_cell::sync::Lazy;
use regex::Regex;
use serde_json::{json, Value};
use std::collections::HashMap;
use std::env;
use std::process::Stdio;
use std::sync::{Arc, Weak};
use tokio::io::{BufReader, BufWriter};
use tokio::net::TcpStream;
use tokio::process::{Child, Command};
use tokio::sync::{RwLock, Semaphore};

/// Maximum number of concurrent event dispatch tasks to prevent unbounded task spawning.
const MAX_CONCURRENT_EVENT_TASKS: usize = 100;

/// Semaphore to limit concurrent event dispatch tasks.
static EVENT_SEMAPHORE: Lazy<Semaphore> = Lazy::new(|| Semaphore::new(MAX_CONCURRENT_EVENT_TASKS));

/// Client for interacting with the Copilot CLI server.
///
/// The client manages the connection to the CLI server and provides methods
/// for creating and managing sessions.
///
/// # Example
///
/// ```ignore
/// use copilot_sdk::{CopilotClient, ClientOptions};
///
/// #[tokio::main]
/// async fn main() -> Result<(), Box<dyn std::error::Error>> {
///     let client = CopilotClient::new(None)?;
///     client.start().await?;
///
///     let session = client.create_session(None).await?;
///     // Use the session...
///
///     client.stop().await;
///     Ok(())
/// }
/// ```
pub struct CopilotClient {
    options: ClientOptions,
    process: RwLock<Option<Child>>,
    rpc_client: RwLock<Option<Arc<JsonRpcClient>>>,
    state: RwLock<ConnectionState>,
    sessions: Arc<RwLock<HashMap<String, Arc<CopilotSession>>>>,
    actual_port: RwLock<u16>,
    actual_host: RwLock<String>,
    is_external_server: bool,
    auto_start: bool,
    auto_restart: bool,
    /// Weak self-reference for use in callbacks. Set by `start_arc()`.
    self_ref: RwLock<Option<Weak<Self>>>,
}

impl CopilotClient {
    /// Create a new CopilotClient with the given options.
    ///
    /// If options is None, default options are used (spawns CLI server using stdio).
    ///
    /// # Errors
    ///
    /// Returns `CopilotError::InvalidConfig` if the configuration is invalid:
    /// - `cli_url` is provided along with `use_stdio` or `cli_path` (mutually exclusive)
    /// - `cli_url` has an invalid format
    pub fn new(options: Option<ClientOptions>) -> Result<Self> {
        let mut opts = ClientOptions {
            cli_path: Some("copilot".to_string()),
            use_stdio: Some(true),
            log_level: Some("info".to_string()),
            ..Default::default()
        };

        let mut is_external_server = false;
        let mut auto_start = true;
        let mut auto_restart = true;
        let mut actual_host = "localhost".to_string();
        let mut actual_port = 0u16;

        if let Some(user_opts) = options {
            // Validate mutually exclusive options
            if user_opts.cli_url.is_some()
                && (user_opts.use_stdio.unwrap_or(false) || user_opts.cli_path.is_some())
            {
                return Err(CopilotError::InvalidConfig(
                    "cli_url is mutually exclusive with use_stdio and cli_path".to_string(),
                ));
            }

            // Parse cli_url if provided
            if let Some(ref url) = user_opts.cli_url {
                let (host, port) = parse_cli_url(url)?;
                actual_host = host;
                actual_port = port;
                is_external_server = true;
                opts.use_stdio = Some(false);
                opts.cli_url = user_opts.cli_url;
            }

            if let Some(path) = user_opts.cli_path {
                opts.cli_path = Some(path);
            }
            if let Some(cwd) = user_opts.cwd {
                opts.cwd = Some(cwd);
            }
            if let Some(port) = user_opts.port {
                opts.port = Some(port);
                opts.use_stdio = Some(false);
            }
            if let Some(log_level) = user_opts.log_level {
                opts.log_level = Some(log_level);
            }
            if let Some(env) = user_opts.env {
                opts.env = Some(env);
            }
            if let Some(auto) = user_opts.auto_start {
                auto_start = auto;
            }
            if let Some(auto) = user_opts.auto_restart {
                auto_restart = auto;
            }
        }

        // Check environment variable for CLI path
        if let Ok(cli_path) = env::var("COPILOT_CLI_PATH") {
            opts.cli_path = Some(cli_path);
        }

        Ok(Self {
            options: opts,
            process: RwLock::new(None),
            rpc_client: RwLock::new(None),
            state: RwLock::new(ConnectionState::Disconnected),
            sessions: Arc::new(RwLock::new(HashMap::new())),
            actual_port: RwLock::new(actual_port),
            actual_host: RwLock::new(actual_host),
            is_external_server,
            auto_start,
            auto_restart,
            self_ref: RwLock::new(None),
        })
    }

    /// Start the CLI server and establish a connection.
    pub async fn start(&self) -> Result<()> {
        {
            let state = self.state.read().await;
            if *state == ConnectionState::Connected {
                return Ok(());
            }
        }

        {
            let mut state = self.state.write().await;
            *state = ConnectionState::Connecting;
        }

        // Only start CLI server process if not connecting to external server
        if !self.is_external_server {
            if let Err(e) = self.start_cli_server().await {
                let mut state = self.state.write().await;
                *state = ConnectionState::Error;
                return Err(e);
            }
        }

        // Connect to the server
        if let Err(e) = self.connect_to_server().await {
            let mut state = self.state.write().await;
            *state = ConnectionState::Error;
            return Err(e);
        }

        // Verify protocol version
        if let Err(e) = self.verify_protocol_version().await {
            let mut state = self.state.write().await;
            *state = ConnectionState::Error;
            return Err(e);
        }

        {
            let mut state = self.state.write().await;
            *state = ConnectionState::Connected;
        }

        Ok(())
    }

    /// Stop the CLI server and close all active sessions.
    ///
    /// Returns a vector of errors encountered during cleanup.
    pub async fn stop(&self) -> Vec<CopilotError> {
        let mut errors = Vec::new();

        // Destroy all active sessions
        let sessions: Vec<Arc<CopilotSession>> = {
            let sessions = self.sessions.read().await;
            sessions.values().cloned().collect()
        };

        for session in sessions {
            if let Err(e) = session.destroy().await {
                errors.push(CopilotError::Session(format!(
                    "failed to destroy session {}: {}",
                    session.session_id(),
                    e
                )));
            }
        }

        {
            let mut sessions = self.sessions.write().await;
            sessions.clear();
        }

        // Kill CLI process (only if we spawned it)
        if !self.is_external_server {
            let mut process = self.process.write().await;
            if let Some(mut child) = process.take() {
                if let Err(e) = child.kill().await {
                    errors.push(CopilotError::Process(format!(
                        "failed to kill CLI process: {}",
                        e
                    )));
                }
            }
        }

        // Close JSON-RPC client
        {
            let mut client = self.rpc_client.write().await;
            if let Some(rpc) = client.take() {
                rpc.stop().await;
            }
        }

        {
            let mut state = self.state.write().await;
            *state = ConnectionState::Disconnected;
        }

        if !self.is_external_server {
            let mut port = self.actual_port.write().await;
            *port = 0;
        }

        errors
    }

    /// Forcefully stop the CLI server without graceful cleanup.
    pub async fn force_stop(&self) {
        // Clear sessions immediately
        {
            let mut sessions = self.sessions.write().await;
            sessions.clear();
        }

        // Kill CLI process (only if we spawned it)
        if !self.is_external_server {
            let mut process = self.process.write().await;
            if let Some(mut child) = process.take() {
                let _ = child.kill().await;
            }
        }

        // Close JSON-RPC client
        {
            let mut client = self.rpc_client.write().await;
            if let Some(rpc) = client.take() {
                rpc.stop().await;
            }
        }

        {
            let mut state = self.state.write().await;
            *state = ConnectionState::Disconnected;
        }

        if !self.is_external_server {
            let mut port = self.actual_port.write().await;
            *port = 0;
        }
    }

    /// Create a new session.
    pub async fn create_session(
        &self,
        config: Option<SessionConfig>,
    ) -> Result<Arc<CopilotSession>> {
        self.ensure_connected().await?;

        let config = config.unwrap_or_default();
        let mut params = json!({});

        if let Some(ref model) = config.model {
            params["model"] = json!(model);
        }
        if let Some(ref session_id) = config.session_id {
            params["sessionId"] = json!(session_id);
        }
        if !config.tools.is_empty() {
            let tool_defs: Vec<Value> = config
                .tools
                .iter()
                .filter(|t| !t.name.is_empty())
                .map(|t| {
                    let mut def = json!({
                        "name": t.name,
                        "description": t.description,
                    });
                    if let Some(ref params) = t.parameters {
                        def["parameters"] = params.clone();
                    }
                    def
                })
                .collect();
            if !tool_defs.is_empty() {
                params["tools"] = json!(tool_defs);
            }
        }
        if let Some(ref sys_msg) = config.system_message {
            let mut system_message = json!({});
            if let Some(ref mode) = sys_msg.mode {
                system_message["mode"] = json!(mode);
            }
            if let Some(ref content) = sys_msg.content {
                system_message["content"] = json!(content);
            }
            params["systemMessage"] = system_message;
        }
        if let Some(ref available) = config.available_tools {
            params["availableTools"] = json!(available);
        }
        if let Some(ref excluded) = config.excluded_tools {
            params["excludedTools"] = json!(excluded);
        }
        if let Some(streaming) = config.streaming {
            params["streaming"] = json!(streaming);
        }
        if let Some(ref provider) = config.provider {
            params["provider"] = build_provider_params(provider);
        }
        if let Some(ref mcp_servers) = config.mcp_servers {
            params["mcpServers"] = json!(mcp_servers);
        }
        if let Some(ref custom_agents) = config.custom_agents {
            params["customAgents"] = json!(custom_agents);
        }
        if let Some(ref config_dir) = config.config_dir {
            params["configDir"] = json!(config_dir);
        }
        if let Some(ref skill_dirs) = config.skill_directories {
            params["skillDirectories"] = json!(skill_dirs);
        }
        if let Some(ref disabled) = config.disabled_skills {
            params["disabledSkills"] = json!(disabled);
        }

        let rpc = self.get_rpc_client().await?;
        let result = rpc.request("session.create", params).await?;

        let session_id = result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| CopilotError::InvalidResponse("missing sessionId".to_string()))?
            .to_string();

        let session = Arc::new(CopilotSession::new(session_id.clone(), rpc.clone()));
        session.register_tools(config.tools).await;

        {
            let mut sessions = self.sessions.write().await;
            sessions.insert(session_id, session.clone());
        }

        Ok(session)
    }

    /// Resume an existing session.
    pub async fn resume_session(
        &self,
        session_id: &str,
        config: Option<ResumeSessionConfig>,
    ) -> Result<Arc<CopilotSession>> {
        self.ensure_connected().await?;

        let config = config.unwrap_or_default();
        let mut params = json!({
            "sessionId": session_id,
        });

        if !config.tools.is_empty() {
            let tool_defs: Vec<Value> = config
                .tools
                .iter()
                .filter(|t| !t.name.is_empty())
                .map(|t| {
                    let mut def = json!({
                        "name": t.name,
                        "description": t.description,
                    });
                    if let Some(ref params) = t.parameters {
                        def["parameters"] = params.clone();
                    }
                    def
                })
                .collect();
            if !tool_defs.is_empty() {
                params["tools"] = json!(tool_defs);
            }
        }
        if let Some(ref provider) = config.provider {
            params["provider"] = build_provider_params(provider);
        }
        if let Some(streaming) = config.streaming {
            params["streaming"] = json!(streaming);
        }
        if let Some(ref mcp_servers) = config.mcp_servers {
            params["mcpServers"] = json!(mcp_servers);
        }
        if let Some(ref custom_agents) = config.custom_agents {
            params["customAgents"] = json!(custom_agents);
        }
        if let Some(ref skill_dirs) = config.skill_directories {
            params["skillDirectories"] = json!(skill_dirs);
        }
        if let Some(ref disabled) = config.disabled_skills {
            params["disabledSkills"] = json!(disabled);
        }

        let rpc = self.get_rpc_client().await?;
        let result = rpc.request("session.resume", params).await?;

        let resumed_session_id = result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| CopilotError::InvalidResponse("missing sessionId".to_string()))?
            .to_string();

        let session = Arc::new(CopilotSession::new(resumed_session_id.clone(), rpc.clone()));
        session.register_tools(config.tools).await;

        {
            let mut sessions = self.sessions.write().await;
            sessions.insert(resumed_session_id, session.clone());
        }

        Ok(session)
    }

    /// Get the current connection state.
    pub async fn get_state(&self) -> ConnectionState {
        *self.state.read().await
    }

    /// Ping the server to verify connectivity.
    pub async fn ping(&self, message: Option<&str>) -> Result<PingResponse> {
        let rpc = self.get_rpc_client().await?;

        let mut params = json!({});
        if let Some(msg) = message {
            params["message"] = json!(msg);
        }

        let result = rpc.request("ping", params).await?;

        Ok(PingResponse {
            message: result
                .get("message")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            timestamp: result
                .get("timestamp")
                .and_then(|v| v.as_i64())
                .unwrap_or(0),
            protocol_version: result
                .get("protocolVersion")
                .and_then(|v| v.as_i64())
                .map(|v| v as i32),
        })
    }

    /// Delete a session by ID.
    pub async fn delete_session(&self, session_id: &str) -> Result<()> {
        let rpc = self.get_rpc_client().await?;

        let params = json!({
            "sessionId": session_id,
        });

        rpc.request("session.delete", params).await?;

        {
            let mut sessions = self.sessions.write().await;
            sessions.remove(session_id);
        }

        Ok(())
    }

    /// List all sessions.
    pub async fn list_sessions(&self) -> Result<Vec<SessionMetadata>> {
        let rpc = self.get_rpc_client().await?;

        let result = rpc.request("session.list", json!({})).await?;

        let sessions = result
            .get("sessions")
            .and_then(|v| v.as_array())
            .map(|arr| {
                arr.iter()
                    .filter_map(|v| serde_json::from_value(v.clone()).ok())
                    .collect()
            })
            .unwrap_or_default();

        Ok(sessions)
    }

    /// Start the CLI server with auto-reconnect support.
    ///
    /// This method is similar to [`start()`](Self::start) but requires the client to be
    /// wrapped in an `Arc`. When `auto_restart` is enabled in the client options, this
    /// method sets up automatic reconnection when the connection is lost.
    ///
    /// # Example
    ///
    /// ```ignore
    /// use copilot_sdk::{CopilotClient, ClientOptions};
    /// use std::sync::Arc;
    ///
    /// let client = Arc::new(CopilotClient::new(Some(ClientOptions {
    ///     auto_restart: Some(true),
    ///     ..Default::default()
    /// }))?);
    ///
    /// client.start_arc().await?;
    /// // Connection will automatically reconnect if lost
    /// ```
    pub async fn start_arc(self: &Arc<Self>) -> Result<()> {
        // Store weak self-reference for use in callbacks
        {
            let mut self_ref = self.self_ref.write().await;
            *self_ref = Some(Arc::downgrade(self));
        }

        // Call the regular start method
        self.start().await?;

        // Set up disconnect handler if auto_restart is enabled
        if self.auto_restart {
            self.setup_disconnect_handler(Arc::clone(self)).await;
        }

        Ok(())
    }

    /// Set up the disconnect handler on the RPC client.
    ///
    /// This spawns a background task that listens for disconnect events
    /// and handles reconnection.
    async fn setup_disconnect_handler(&self, client_arc: Arc<Self>) {
        // Get RPC client, ensuring the guard is dropped before the next await
        let rpc = {
            let guard = self.rpc_client.read().await;
            guard.clone()
        };

        let Some(rpc) = rpc else {
            return;
        };

        // Create a channel for disconnect notifications
        let (tx, rx) = tokio::sync::mpsc::channel::<()>(1);

        // Set up the callback to send on the channel (sync operation)
        rpc.set_on_disconnect(Arc::new(move || {
            // try_send is non-blocking and won't fail if receiver is ready
            let _ = tx.try_send(());
        }))
        .await;

        // Spawn the handler task with the Arc reference
        Self::spawn_disconnect_handler(client_arc, rx);
    }

    /// Spawn a task to handle disconnect events.
    ///
    /// This is a separate function to ensure the spawned future doesn't capture
    /// any non-Send types from the calling context.
    fn spawn_disconnect_handler(
        client: Arc<Self>,
        mut rx: tokio::sync::mpsc::Receiver<()>,
    ) {
        tokio::spawn(async move {
            // Wait for disconnect notification
            if rx.recv().await.is_some() {
                client.handle_disconnect().await;
            }
        });
    }

    /// Handle a disconnection event.
    ///
    /// Called when the RPC connection is lost. Triggers reconnection if
    /// `auto_restart` is enabled and the client was in Connected state.
    async fn handle_disconnect(&self) {
        let should_reconnect = {
            let state = self.state.read().await;
            self.auto_restart && *state == ConnectionState::Connected
        };

        if should_reconnect {
            self.reconnect().await;
        }
    }

    /// Attempt to reconnect to the server.
    ///
    /// Notifies all active sessions of the disconnection, stops the current
    /// connection, and attempts to establish a new one.
    async fn reconnect(&self) {
        // Notify sessions of disconnection
        self.invalidate_sessions().await;

        // Update state
        {
            let mut state = self.state.write().await;
            *state = ConnectionState::Disconnected;
        }

        // Stop the current connection
        let _ = self.stop().await;

        // Attempt to restart
        // Use the stored weak reference to call start_arc if available
        let self_ref = self.self_ref.read().await.clone();
        if let Some(weak_self) = self_ref {
            if let Some(arc_self) = weak_self.upgrade() {
                if let Err(e) = arc_self.start_arc().await {
                    eprintln!("Reconnection failed: {}", e);
                    let mut state = arc_self.state.write().await;
                    *state = ConnectionState::Error;
                }
                return;
            }
        }

        // Fallback to regular start if no Arc reference available
        if let Err(e) = self.start().await {
            eprintln!("Reconnection failed: {}", e);
            let mut state = self.state.write().await;
            *state = ConnectionState::Error;
        }
    }

    /// Notify all sessions that the connection has been lost.
    async fn invalidate_sessions(&self) {
        // Collect sessions first to avoid holding the lock across await points
        let sessions: Vec<Arc<CopilotSession>> = {
            let guard = self.sessions.read().await;
            guard.values().cloned().collect()
        };

        for session in sessions {
            session.dispatch_error("Connection lost").await;
        }
    }

    // Private methods

    async fn ensure_connected(&self) -> Result<()> {
        let state = self.state.read().await;
        if *state == ConnectionState::Connected {
            return Ok(());
        }
        drop(state);

        if self.auto_start {
            self.start().await
        } else {
            Err(CopilotError::NotConnected)
        }
    }

    async fn get_rpc_client(&self) -> Result<Arc<JsonRpcClient>> {
        let client = self.rpc_client.read().await;
        client.clone().ok_or(CopilotError::NotConnected)
    }

    async fn start_cli_server(&self) -> Result<()> {
        let cli_path = self
            .options
            .cli_path
            .as_deref()
            .unwrap_or("copilot")
            .to_string();
        let log_level = self
            .options
            .log_level
            .as_deref()
            .unwrap_or("info")
            .to_string();

        let mut args = vec![
            "--server".to_string(),
            "--log-level".to_string(),
            log_level,
        ];

        let use_stdio = self.options.use_stdio.unwrap_or(true);
        if use_stdio {
            args.push("--stdio".to_string());
        } else if let Some(port) = self.options.port {
            args.push("--port".to_string());
            args.push(port.to_string());
        }

        // Determine command - if CLI path is a .js file, run with node
        let (command, final_args) = if cli_path.ends_with(".js") {
            let mut new_args = vec![cli_path];
            new_args.extend(args);
            ("node".to_string(), new_args)
        } else {
            (cli_path, args)
        };

        let mut cmd = Command::new(&command);
        cmd.args(&final_args)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped());

        if let Some(ref cwd) = self.options.cwd {
            cmd.current_dir(cwd);
        }

        if let Some(ref env_vars) = self.options.env {
            for (key, value) in env_vars {
                cmd.env(key, value);
            }
        }

        let mut child = cmd
            .spawn()
            .map_err(|e| CopilotError::Process(format!("failed to start CLI server: {}", e)))?;

        if use_stdio {
            // For stdio mode, create JSON-RPC client from stdin/stdout
            let stdin = child
                .stdin
                .take()
                .ok_or_else(|| CopilotError::Process("failed to get stdin".to_string()))?;
            let stdout = child
                .stdout
                .take()
                .ok_or_else(|| CopilotError::Process("failed to get stdout".to_string()))?;

            let reader = BufReader::new(stdout);
            let writer = BufWriter::new(stdin);

            let rpc = Arc::new(JsonRpcClient::new(reader, writer));
            self.setup_notification_handler(&rpc).await;

            {
                let mut client = self.rpc_client.write().await;
                *client = Some(rpc);
            }

            {
                let mut process = self.process.write().await;
                *process = Some(child);
            }

            Ok(())
        } else {
            // For TCP mode, wait for port announcement
            let stdout = child
                .stdout
                .take()
                .ok_or_else(|| CopilotError::Process("failed to get stdout".to_string()))?;

            let mut reader = BufReader::new(stdout);
            let port_regex = Regex::new(r"listening on port (\d+)").unwrap();

            let mut line = String::new();
            let timeout = tokio::time::timeout(
                std::time::Duration::from_secs(10),
                async {
                    loop {
                        line.clear();
                        use tokio::io::AsyncBufReadExt;
                        if reader.read_line(&mut line).await.is_err() {
                            break Err(CopilotError::Process("failed to read from CLI".to_string()));
                        }
                        if let Some(caps) = port_regex.captures(&line) {
                            if let Some(port_str) = caps.get(1) {
                                if let Ok(port) = port_str.as_str().parse::<u16>() {
                                    break Ok(port);
                                }
                            }
                        }
                    }
                },
            )
            .await;

            let port = match timeout {
                Ok(Ok(port)) => port,
                Ok(Err(e)) => return Err(e),
                Err(_) => {
                    return Err(CopilotError::Timeout);
                }
            };

            {
                let mut actual_port = self.actual_port.write().await;
                *actual_port = port;
            }

            {
                let mut process = self.process.write().await;
                *process = Some(child);
            }

            Ok(())
        }
    }

    async fn connect_to_server(&self) -> Result<()> {
        let use_stdio = self.options.use_stdio.unwrap_or(true);
        if use_stdio && !self.is_external_server {
            // Already connected via stdio in start_cli_server
            return Ok(());
        }

        // Connect via TCP
        self.connect_via_tcp().await
    }

    async fn connect_via_tcp(&self) -> Result<()> {
        let port = *self.actual_port.read().await;
        if port == 0 {
            return Err(CopilotError::Connection(
                "server port not available".to_string(),
            ));
        }

        let host = self.actual_host.read().await.clone();
        let addr = format!("{}:{}", host, port);

        let stream = tokio::time::timeout(
            std::time::Duration::from_secs(10),
            TcpStream::connect(&addr),
        )
        .await
        .map_err(|_| CopilotError::Timeout)?
        .map_err(|e| {
            CopilotError::Connection(format!("failed to connect to CLI server at {}: {}", addr, e))
        })?;

        let (reader, writer) = stream.into_split();
        let reader = BufReader::new(reader);
        let writer = BufWriter::new(writer);

        let rpc = Arc::new(JsonRpcClient::new(reader, writer));
        self.setup_notification_handler(&rpc).await;

        {
            let mut client = self.rpc_client.write().await;
            *client = Some(rpc);
        }

        Ok(())
    }

    async fn setup_notification_handler(&self, rpc: &Arc<JsonRpcClient>) {
        let sessions = self.sessions.clone();

        // Set up notification handler for session events
        rpc.set_notification_handler(Arc::new(move |method, params| {
            if method == "session.event" {
                if let Some(session_id) = params.get("sessionId").and_then(|v| v.as_str()) {
                    if let Some(event_value) = params.get("event") {
                        if let Ok(event) = serde_json::from_value::<SessionEvent>(event_value.clone())
                        {
                            let sessions = sessions.clone();
                            let session_id = session_id.to_string();
                            // Use semaphore to limit concurrent event dispatch tasks
                            tokio::spawn(async move {
                                let _permit = EVENT_SEMAPHORE.acquire().await.unwrap();
                                let sessions = sessions.read().await;
                                if let Some(session) = sessions.get(&session_id) {
                                    session.dispatch_event(event).await;
                                }
                            });
                        }
                    }
                }
            }
        }))
        .await;

        // Set up tool call handler
        let sessions_for_tools = self.sessions.clone();
        rpc.set_request_handler(
            "tool.call",
            Arc::new(move |params| {
                let sessions = sessions_for_tools.clone();

                let session_id = params
                    .get("sessionId")
                    .and_then(|v| v.as_str())
                    .unwrap_or("")
                    .to_string();
                let tool_call_id = params
                    .get("toolCallId")
                    .and_then(|v| v.as_str())
                    .unwrap_or("")
                    .to_string();
                let tool_name = params
                    .get("toolName")
                    .and_then(|v| v.as_str())
                    .unwrap_or("")
                    .to_string();
                let arguments = params.get("arguments").cloned().unwrap_or(Value::Null);

                Box::pin(async move {
                    if session_id.is_empty() || tool_call_id.is_empty() || tool_name.is_empty() {
                        return Err(CopilotError::InvalidResponse(
                            "invalid tool call payload".to_string(),
                        ));
                    }

                    let sessions = sessions.read().await;
                    let session = sessions.get(&session_id);

                    let result = if let Some(session) = session {
                        let inv = ToolInvocation {
                            session_id: session_id.clone(),
                            tool_call_id,
                            tool_name: tool_name.clone(),
                            arguments,
                        };
                        session.execute_tool(&tool_name, inv).await
                    } else {
                        Ok(ToolResult::unsupported(&tool_name))
                    };

                    match result {
                        Ok(tool_result) => Ok(json!({ "result": tool_result })),
                        Err(e) => Ok(json!({ "result": ToolResult::failure(e.to_string()) })),
                    }
                })
            }),
        )
        .await;
    }

    async fn verify_protocol_version(&self) -> Result<()> {
        let expected_version = get_sdk_protocol_version();
        let ping_result = self.ping(None).await?;

        match ping_result.protocol_version {
            None => Err(CopilotError::ProtocolVersionNotReported {
                expected: expected_version,
            }),
            Some(version) if version != expected_version => {
                Err(CopilotError::ProtocolVersionMismatch {
                    expected: expected_version,
                    actual: version,
                })
            }
            Some(_) => Ok(()),
        }
    }
}

/// Parse a CLI URL into host and port components.
fn parse_cli_url(url: &str) -> Result<(String, u16)> {
    // Remove protocol if present
    let clean_url = Regex::new(r"^https?://")
        .unwrap()
        .replace(url, "")
        .to_string();

    // Check if it's just a port number
    if Regex::new(r"^\d+$").unwrap().is_match(&clean_url) {
        let port: u16 = clean_url.parse().map_err(|_| {
            CopilotError::InvalidConfig(format!("Invalid port in cli_url: {}", url))
        })?;
        return Ok(("localhost".to_string(), port));
    }

    // Parse host:port format
    let parts: Vec<&str> = clean_url.splitn(2, ':').collect();
    if parts.len() != 2 {
        return Err(CopilotError::InvalidConfig(format!(
            "Invalid cli_url format: {}. Expected 'host:port', 'http://host:port', or 'port'",
            url
        )));
    }

    let host = if parts[0].is_empty() {
        "localhost".to_string()
    } else {
        parts[0].to_string()
    };

    let port: u16 = parts[1].parse().map_err(|_| {
        CopilotError::InvalidConfig(format!("Invalid port in cli_url: {}", url))
    })?;

    Ok((host, port))
}

/// Build provider params for JSON-RPC.
fn build_provider_params(provider: &ProviderConfig) -> Value {
    let mut params = json!({});

    if let Some(ref t) = provider.provider_type {
        params["type"] = json!(t);
    }
    if let Some(ref w) = provider.wire_api {
        params["wireApi"] = json!(w);
    }
    params["baseUrl"] = json!(provider.base_url);
    if let Some(ref k) = provider.api_key {
        params["apiKey"] = json!(k);
    }
    if let Some(ref t) = provider.bearer_token {
        params["bearerToken"] = json!(t);
    }
    if let Some(ref azure) = provider.azure {
        let mut azure_params = json!({});
        if let Some(ref v) = azure.api_version {
            azure_params["apiVersion"] = json!(v);
        }
        params["azure"] = azure_params;
    }

    params
}
