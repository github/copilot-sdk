//! Copilot CLI client for managing connections and sessions.

use std::collections::HashMap;
use std::process::Stdio;
use std::sync::Arc;

use serde_json::{json, Value};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::net::TcpStream;
use tokio::process::{Child, ChildStdin, ChildStdout, Command};
use tokio::sync::RwLock;

use crate::error::{CopilotError, JsonRpcError, Result};
use crate::generated::SessionEvent;
use crate::jsonrpc::JsonRpcClient;
use crate::session::Session;
use crate::types::*;

/// SDK protocol version. Must match the server's expected version.
pub const SDK_PROTOCOL_VERSION: i32 = 2;

/// Returns the SDK protocol version.
pub fn get_sdk_protocol_version() -> i32 {
    SDK_PROTOCOL_VERSION
}

/// Type alias for the JSON-RPC client over stdio.
type StdioJsonRpcClient = JsonRpcClient<ChildStdout, ChildStdin>;

/// Type alias for the JSON-RPC client over TCP.
type TcpJsonRpcClient = JsonRpcClient<tokio::net::tcp::OwnedReadHalf, tokio::net::tcp::OwnedWriteHalf>;

/// Internal enum to hold either transport type.
enum Transport {
    Stdio(StdioJsonRpcClient),
    Tcp(TcpJsonRpcClient),
}

/// Client for interacting with the GitHub Copilot CLI.
///
/// The client manages the connection to the CLI server and provides
/// methods to create sessions, ping the server, and query status.
///
/// # Example
///
/// ```no_run
/// use copilot_sdk::{Client, ClientOptions, SessionConfig, MessageOptions};
///
/// #[tokio::main]
/// async fn main() -> copilot_sdk::Result<()> {
///     let mut client = Client::new(ClientOptions::new().log_level("error"));
///
///     // Start the client (spawns CLI server)
///     client.start().await?;
///
///     // Create a session
///     let session = client.create_session(SessionConfig::new().model("gpt-5")).await?;
///
///     // Send a message
///     let message_id = session.send(MessageOptions::new("What is 2+2?")).await?;
///
///     // Clean up
///     session.destroy().await?;
///     client.stop().await;
///
///     Ok(())
/// }
/// ```
pub struct Client {
    /// Client options.
    options: ResolvedClientOptions,
    /// CLI process (if spawned).
    process: Option<Child>,
    /// JSON-RPC transport.
    transport: Option<Transport>,
    /// Connection state.
    state: ConnectionState,
    /// Active sessions.
    sessions: Arc<RwLock<HashMap<String, Arc<Session>>>>,
    /// Whether connected to external server.
    is_external_server: bool,
    /// Actual port (for TCP mode).
    actual_port: u16,
    /// Actual host (for TCP mode).
    actual_host: String,
    /// Read loop task handle.
    read_loop_handle: Option<tokio::task::JoinHandle<()>>,
}

/// Resolved client options with defaults applied.
#[derive(Debug, Clone)]
struct ResolvedClientOptions {
    cli_path: String,
    cwd: Option<String>,
    port: u16,
    use_stdio: bool,
    cli_url: Option<String>,
    log_level: String,
    auto_start: bool,
    auto_restart: bool,
    env: Option<Vec<(String, String)>>,
}

impl Default for ResolvedClientOptions {
    fn default() -> Self {
        Self {
            cli_path: "copilot".to_string(),
            cwd: None,
            port: 0,
            use_stdio: true,
            cli_url: None,
            log_level: "info".to_string(),
            auto_start: true,
            auto_restart: true,
            env: None,
        }
    }
}

impl Client {
    /// Create a new Copilot CLI client with the given options.
    ///
    /// The client is not connected after creation; call [`start`](Self::start) to connect.
    pub fn new(options: ClientOptions) -> Self {
        let mut resolved = ResolvedClientOptions::default();
        let mut is_external_server = false;
        let mut actual_host = "localhost".to_string();
        let mut actual_port = 0u16;

        // Check environment variable for CLI path
        if let Ok(cli_path) = std::env::var("COPILOT_CLI_PATH") {
            resolved.cli_path = cli_path;
        }

        if let Some(cli_path) = options.cli_path {
            resolved.cli_path = cli_path;
        }

        if let Some(cwd) = options.cwd {
            resolved.cwd = Some(cwd);
        }

        if let Some(port) = options.port {
            resolved.port = port;
            resolved.use_stdio = false;
        }

        if let Some(use_stdio) = options.use_stdio {
            resolved.use_stdio = use_stdio;
        }

        if let Some(ref cli_url) = options.cli_url {
            let (host, port) = parse_cli_url(cli_url);
            actual_host = host;
            actual_port = port;
            is_external_server = true;
            resolved.use_stdio = false;
            resolved.cli_url = Some(cli_url.clone());
        }

        if let Some(log_level) = options.log_level {
            resolved.log_level = log_level;
        }

        if let Some(auto_start) = options.auto_start {
            resolved.auto_start = auto_start;
        }

        if let Some(auto_restart) = options.auto_restart {
            resolved.auto_restart = auto_restart;
        }

        if let Some(env) = options.env {
            resolved.env = Some(env);
        }

        Self {
            options: resolved,
            process: None,
            transport: None,
            state: ConnectionState::Disconnected,
            sessions: Arc::new(RwLock::new(HashMap::new())),
            is_external_server,
            actual_port,
            actual_host,
            read_loop_handle: None,
        }
    }

    /// Start the CLI server and establish a connection.
    ///
    /// This method is called automatically when creating a session if auto_start is enabled.
    pub async fn start(&mut self) -> Result<()> {
        if self.state == ConnectionState::Connected {
            return Ok(());
        }

        self.state = ConnectionState::Connecting;

        // Only start CLI server process if not connecting to external server
        if !self.is_external_server {
            self.start_cli_server().await?;
        }

        // Connect to the server
        self.connect_to_server().await?;

        // Verify protocol version
        self.verify_protocol_version().await?;

        self.state = ConnectionState::Connected;
        Ok(())
    }

    /// Stop the CLI server and close all active sessions.
    pub async fn stop(&mut self) -> Vec<CopilotError> {
        let mut errors = Vec::new();

        // Destroy all active sessions
        let sessions: Vec<Arc<Session>> = {
            let guard = self.sessions.read().await;
            guard.values().cloned().collect()
        };

        for session in sessions {
            if let Err(e) = session.destroy().await {
                errors.push(CopilotError::session(format!(
                    "failed to destroy session {}: {}",
                    session.session_id(),
                    e
                )));
            }
        }

        {
            let mut guard = self.sessions.write().await;
            guard.clear();
        }

        // Kill CLI process
        if let Some(ref mut process) = self.process {
            if !self.is_external_server {
                let _ = process.kill().await;
            }
        }
        self.process = None;

        // Stop JSON-RPC client
        if let Some(ref mut transport) = self.transport {
            match transport {
                Transport::Stdio(client) => client.stop(),
                Transport::Tcp(client) => client.stop(),
            }
        }
        self.transport = None;

        // Wait for read loop to finish
        if let Some(handle) = self.read_loop_handle.take() {
            let _ = handle.await;
        }

        self.state = ConnectionState::Disconnected;
        if !self.is_external_server {
            self.actual_port = 0;
        }

        errors
    }

    /// Forcefully stop the CLI server without graceful cleanup.
    pub async fn force_stop(&mut self) {
        // Clear sessions without destroying them
        {
            let mut guard = self.sessions.write().await;
            guard.clear();
        }

        // Kill CLI process
        if let Some(ref mut process) = self.process {
            if !self.is_external_server {
                let _ = process.kill().await;
            }
        }
        self.process = None;

        // Stop JSON-RPC client
        if let Some(ref mut transport) = self.transport {
            match transport {
                Transport::Stdio(client) => client.stop(),
                Transport::Tcp(client) => client.stop(),
            }
        }
        self.transport = None;

        if let Some(handle) = self.read_loop_handle.take() {
            handle.abort();
        }

        self.state = ConnectionState::Disconnected;
        if !self.is_external_server {
            self.actual_port = 0;
        }
    }

    /// Get the current connection state.
    pub fn state(&self) -> ConnectionState {
        self.state
    }

    /// Create a new conversation session.
    pub async fn create_session(&mut self, config: SessionConfig) -> Result<Arc<Session>> {
        self.ensure_connected().await?;

        let mut params = serde_json::Map::new();

        if let Some(ref model) = config.model {
            params.insert("model".to_string(), json!(model));
        }

        if let Some(ref session_id) = config.session_id {
            params.insert("sessionId".to_string(), json!(session_id));
        }

        if !config.tools.is_empty() {
            let tool_defs: Vec<Value> = config
                .tools
                .iter()
                .filter(|t| !t.name.is_empty())
                .map(|t| {
                    let mut def = serde_json::Map::new();
                    def.insert("name".to_string(), json!(t.name));
                    def.insert("description".to_string(), json!(t.description));
                    if let Some(ref params) = t.parameters {
                        def.insert("parameters".to_string(), params.clone());
                    }
                    Value::Object(def)
                })
                .collect();
            if !tool_defs.is_empty() {
                params.insert("tools".to_string(), json!(tool_defs));
            }
        }

        if let Some(ref system_message) = config.system_message {
            params.insert("systemMessage".to_string(), serde_json::to_value(system_message)?);
        }

        if let Some(ref available_tools) = config.available_tools {
            params.insert("availableTools".to_string(), json!(available_tools));
        }

        if let Some(ref excluded_tools) = config.excluded_tools {
            params.insert("excludedTools".to_string(), json!(excluded_tools));
        }

        if config.streaming {
            params.insert("streaming".to_string(), json!(true));
        }

        if let Some(ref provider) = config.provider {
            params.insert("provider".to_string(), serde_json::to_value(provider)?);
        }

        if let Some(ref mcp_servers) = config.mcp_servers {
            params.insert("mcpServers".to_string(), json!(mcp_servers));
        }

        if let Some(ref custom_agents) = config.custom_agents {
            params.insert("customAgents".to_string(), serde_json::to_value(custom_agents)?);
        }

        if let Some(ref config_dir) = config.config_dir {
            params.insert("configDir".to_string(), json!(config_dir));
        }

        if let Some(ref skill_directories) = config.skill_directories {
            params.insert("skillDirectories".to_string(), json!(skill_directories));
        }

        if let Some(ref disabled_skills) = config.disabled_skills {
            params.insert("disabledSkills".to_string(), json!(disabled_skills));
        }

        // Check if there's a permission handler configured
        // For now, we'll handle this through the session
        let has_tools = !config.tools.is_empty();
        if has_tools {
            params.insert("requestPermission".to_string(), json!(false));
        }

        let result = self.request("session.create", Value::Object(params)).await?;

        let session_id = result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| CopilotError::session("invalid response: missing sessionId"))?
            .to_string();

        let session = Arc::new(Session::new(session_id.clone(), self));

        // Register tool handlers
        session.register_tools(config.tools).await;

        // Store session
        {
            let mut guard = self.sessions.write().await;
            guard.insert(session_id, Arc::clone(&session));
        }

        Ok(session)
    }

    /// Resume an existing session.
    pub async fn resume_session(&mut self, session_id: &str) -> Result<Arc<Session>> {
        self.resume_session_with_options(session_id, ResumeSessionConfig::default()).await
    }

    /// Resume an existing session with additional configuration.
    pub async fn resume_session_with_options(
        &mut self,
        session_id: &str,
        config: ResumeSessionConfig,
    ) -> Result<Arc<Session>> {
        self.ensure_connected().await?;

        let mut params = serde_json::Map::new();
        params.insert("sessionId".to_string(), json!(session_id));

        if !config.tools.is_empty() {
            let tool_defs: Vec<Value> = config
                .tools
                .iter()
                .filter(|t| !t.name.is_empty())
                .map(|t| {
                    let mut def = serde_json::Map::new();
                    def.insert("name".to_string(), json!(t.name));
                    def.insert("description".to_string(), json!(t.description));
                    if let Some(ref params) = t.parameters {
                        def.insert("parameters".to_string(), params.clone());
                    }
                    Value::Object(def)
                })
                .collect();
            if !tool_defs.is_empty() {
                params.insert("tools".to_string(), json!(tool_defs));
            }
        }

        if let Some(ref provider) = config.provider {
            params.insert("provider".to_string(), serde_json::to_value(provider)?);
        }

        if config.streaming {
            params.insert("streaming".to_string(), json!(true));
        }

        if let Some(ref mcp_servers) = config.mcp_servers {
            params.insert("mcpServers".to_string(), json!(mcp_servers));
        }

        if let Some(ref custom_agents) = config.custom_agents {
            params.insert("customAgents".to_string(), serde_json::to_value(custom_agents)?);
        }

        if let Some(ref skill_directories) = config.skill_directories {
            params.insert("skillDirectories".to_string(), json!(skill_directories));
        }

        if let Some(ref disabled_skills) = config.disabled_skills {
            params.insert("disabledSkills".to_string(), json!(disabled_skills));
        }

        let result = self.request("session.resume", Value::Object(params)).await?;

        let resumed_session_id = result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| CopilotError::session("invalid response: missing sessionId"))?
            .to_string();

        let session = Arc::new(Session::new(resumed_session_id.clone(), self));

        // Register tool handlers
        session.register_tools(config.tools).await;

        // Store session
        {
            let mut guard = self.sessions.write().await;
            guard.insert(resumed_session_id, Arc::clone(&session));
        }

        Ok(session)
    }

    /// Ping the server.
    pub async fn ping(&self, message: &str) -> Result<PingResponse> {
        let transport = self.transport.as_ref().ok_or(CopilotError::NotConnected)?;

        let mut params = serde_json::Map::new();
        if !message.is_empty() {
            params.insert("message".to_string(), json!(message));
        }

        let result = match transport {
            Transport::Stdio(client) => client.request("ping", Value::Object(params)).await?,
            Transport::Tcp(client) => client.request("ping", Value::Object(params)).await?,
        };

        Ok(PingResponse {
            message: result.get("message").and_then(|v| v.as_str()).unwrap_or("").to_string(),
            timestamp: result.get("timestamp").and_then(|v| v.as_i64()).unwrap_or(0),
            protocol_version: result.get("protocolVersion").and_then(|v| v.as_i64()).map(|v| v as i32),
        })
    }

    /// Get CLI status.
    pub async fn get_status(&self) -> Result<GetStatusResponse> {
        let result = self.request("status.get", json!({})).await?;

        Ok(GetStatusResponse {
            version: result.get("version").and_then(|v| v.as_str()).unwrap_or("").to_string(),
            protocol_version: result.get("protocolVersion").and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        })
    }

    /// Get authentication status.
    pub async fn get_auth_status(&self) -> Result<GetAuthStatusResponse> {
        let result = self.request("auth.getStatus", json!({})).await?;

        Ok(GetAuthStatusResponse {
            is_authenticated: result.get("isAuthenticated").and_then(|v| v.as_bool()).unwrap_or(false),
            auth_type: result.get("authType").and_then(|v| v.as_str()).map(|s| s.to_string()),
            host: result.get("host").and_then(|v| v.as_str()).map(|s| s.to_string()),
            login: result.get("login").and_then(|v| v.as_str()).map(|s| s.to_string()),
            status_message: result.get("statusMessage").and_then(|v| v.as_str()).map(|s| s.to_string()),
        })
    }

    /// List available models.
    pub async fn list_models(&self) -> Result<Vec<ModelInfo>> {
        let result = self.request("models.list", json!({})).await?;

        let response: GetModelsResponse = serde_json::from_value(result)?;
        Ok(response.models)
    }

    /// List active sessions.
    pub async fn list_sessions(&self) -> Result<Vec<SessionListItem>> {
        let result = self.request("session.list", json!({})).await?;

        let response: ListSessionsResponse = serde_json::from_value(result)?;
        Ok(response.sessions)
    }

    /// Delete a session by ID.
    pub async fn delete_session(&self, session_id: &str) -> Result<()> {
        self.request("session.delete", json!({ "sessionId": session_id })).await?;
        Ok(())
    }

    /// Internal method to send a JSON-RPC request.
    pub(crate) async fn request(&self, method: &str, params: Value) -> Result<Value> {
        let transport = self.transport.as_ref().ok_or(CopilotError::NotConnected)?;

        match transport {
            Transport::Stdio(client) => client.request(method, params).await,
            Transport::Tcp(client) => client.request(method, params).await,
        }
    }

    /// Ensure the client is connected, starting if auto_start is enabled.
    async fn ensure_connected(&mut self) -> Result<()> {
        if self.transport.is_none() {
            if self.options.auto_start {
                self.start().await?;
            } else {
                return Err(CopilotError::NotConnected);
            }
        }
        Ok(())
    }

    /// Start the CLI server process.
    async fn start_cli_server(&mut self) -> Result<()> {
        let mut args = vec![
            "--server".to_string(),
            "--log-level".to_string(),
            self.options.log_level.clone(),
        ];

        if self.options.use_stdio {
            args.push("--stdio".to_string());
        } else if self.options.port > 0 {
            args.push("--port".to_string());
            args.push(self.options.port.to_string());
        }

        // Determine command
        let (command, final_args) = if self.options.cli_path.ends_with(".js") {
            let mut new_args = vec![self.options.cli_path.clone()];
            new_args.extend(args);
            ("node".to_string(), new_args)
        } else {
            (self.options.cli_path.clone(), args)
        };

        let mut cmd = Command::new(&command);
        cmd.args(&final_args)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped());

        if let Some(ref cwd) = self.options.cwd {
            cmd.current_dir(cwd);
        }

        if let Some(ref env) = self.options.env {
            for (key, value) in env {
                cmd.env(key, value);
            }
        }

        let mut child = cmd.spawn()?;

        if self.options.use_stdio {
            // For stdio mode, get stdin/stdout pipes
            let stdin = child.stdin.take().ok_or_else(|| {
                CopilotError::connection("failed to get stdin pipe")
            })?;
            let stdout = child.stdout.take().ok_or_else(|| {
                CopilotError::connection("failed to get stdout pipe")
            })?;

            // Create JSON-RPC client
            let mut client = JsonRpcClient::new(stdout, stdin);
            self.setup_notification_handler(&mut client).await;
            let handle = client.start();

            self.transport = Some(Transport::Stdio(client));
            self.read_loop_handle = Some(handle);
            self.process = Some(child);
        } else {
            // For TCP mode, wait for port announcement
            let stdout = child.stdout.take().ok_or_else(|| {
                CopilotError::connection("failed to get stdout pipe")
            })?;

            let mut reader = BufReader::new(stdout);
            let port_regex = regex_lite::Regex::new(r"listening on port (\d+)").unwrap();

            let port = tokio::time::timeout(std::time::Duration::from_secs(10), async {
                let mut line = String::new();
                loop {
                    line.clear();
                    if reader.read_line(&mut line).await? == 0 {
                        return Err(CopilotError::connection("CLI exited before announcing port"));
                    }
                    if let Some(caps) = port_regex.captures(&line) {
                        if let Some(port_match) = caps.get(1) {
                            if let Ok(port) = port_match.as_str().parse::<u16>() {
                                return Ok(port);
                            }
                        }
                    }
                }
            })
            .await
            .map_err(|_| CopilotError::Timeout(std::time::Duration::from_secs(10)))??;

            self.actual_port = port;
            self.process = Some(child);
        }

        Ok(())
    }

    /// Connect to the server (for TCP mode or external server).
    async fn connect_to_server(&mut self) -> Result<()> {
        if self.options.use_stdio {
            // Already connected via stdio in start_cli_server
            return Ok(());
        }

        // Connect via TCP
        let address = format!("{}:{}", self.actual_host, self.actual_port);
        let stream = TcpStream::connect(&address).await.map_err(|e| {
            CopilotError::connection(format!("failed to connect to CLI server at {}: {}", address, e))
        })?;

        let (read_half, write_half) = stream.into_split();

        let mut client = JsonRpcClient::new(read_half, write_half);
        self.setup_notification_handler_tcp(&mut client).await;
        let handle = client.start();

        self.transport = Some(Transport::Tcp(client));
        self.read_loop_handle = Some(handle);

        Ok(())
    }

    /// Set up notification handler for stdio transport.
    async fn setup_notification_handler(&self, client: &mut StdioJsonRpcClient) {
        let sessions = Arc::clone(&self.sessions);

        client
            .set_notification_handler(Box::new(move |method, params| {
                if method == "session.event" {
                    let sessions = Arc::clone(&sessions);
                    tokio::spawn(async move {
                        Self::handle_session_event(&sessions, params).await;
                    });
                }
            }))
            .await;

        // Set up tool.call handler
        let sessions_for_tool = Arc::clone(&self.sessions);
        client
            .set_request_handler(
                "tool.call",
                Box::new(move |params| {
                    let sessions = Arc::clone(&sessions_for_tool);
                    Box::pin(async move {
                        Self::handle_tool_call(&sessions, params).await
                    })
                }),
            )
            .await;

        // Set up permission.request handler
        let sessions_for_perm = Arc::clone(&self.sessions);
        client
            .set_request_handler(
                "permission.request",
                Box::new(move |params| {
                    let sessions = Arc::clone(&sessions_for_perm);
                    Box::pin(async move {
                        Self::handle_permission_request(&sessions, params).await
                    })
                }),
            )
            .await;
    }

    /// Set up notification handler for TCP transport.
    async fn setup_notification_handler_tcp(&self, client: &mut TcpJsonRpcClient) {
        let sessions = Arc::clone(&self.sessions);

        client
            .set_notification_handler(Box::new(move |method, params| {
                if method == "session.event" {
                    let sessions = Arc::clone(&sessions);
                    tokio::spawn(async move {
                        Self::handle_session_event(&sessions, params).await;
                    });
                }
            }))
            .await;

        // Set up tool.call handler
        let sessions_for_tool = Arc::clone(&self.sessions);
        client
            .set_request_handler(
                "tool.call",
                Box::new(move |params| {
                    let sessions = Arc::clone(&sessions_for_tool);
                    Box::pin(async move {
                        Self::handle_tool_call(&sessions, params).await
                    })
                }),
            )
            .await;

        // Set up permission.request handler
        let sessions_for_perm = Arc::clone(&self.sessions);
        client
            .set_request_handler(
                "permission.request",
                Box::new(move |params| {
                    let sessions = Arc::clone(&sessions_for_perm);
                    Box::pin(async move {
                        Self::handle_permission_request(&sessions, params).await
                    })
                }),
            )
            .await;
    }

    /// Handle a session.event notification.
    async fn handle_session_event(
        sessions: &Arc<RwLock<HashMap<String, Arc<Session>>>>,
        params: Value,
    ) {
        let session_id = match params.get("sessionId").and_then(|v| v.as_str()) {
            Some(id) => id.to_string(),
            None => return,
        };

        let event_value = match params.get("event") {
            Some(v) => v.clone(),
            None => return,
        };

        let event: SessionEvent = match serde_json::from_value(event_value) {
            Ok(e) => e,
            Err(_) => return,
        };

        let session = {
            let guard = sessions.read().await;
            guard.get(&session_id).cloned()
        };

        if let Some(session) = session {
            session.dispatch_event(event).await;
        }
    }

    /// Handle a tool.call request.
    async fn handle_tool_call(
        sessions: &Arc<RwLock<HashMap<String, Arc<Session>>>>,
        params: Value,
    ) -> std::result::Result<Value, JsonRpcError> {
        let session_id = params
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| JsonRpcError::new(JsonRpcError::INVALID_PARAMS, "missing sessionId"))?
            .to_string();

        let tool_call_id = params
            .get("toolCallId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| JsonRpcError::new(JsonRpcError::INVALID_PARAMS, "missing toolCallId"))?
            .to_string();

        let tool_name = params
            .get("toolName")
            .and_then(|v| v.as_str())
            .ok_or_else(|| JsonRpcError::new(JsonRpcError::INVALID_PARAMS, "missing toolName"))?
            .to_string();

        let arguments = params.get("arguments").cloned().unwrap_or(Value::Null);

        let session = {
            let guard = sessions.read().await;
            guard.get(&session_id).cloned()
        };

        let session = session.ok_or_else(|| {
            JsonRpcError::new(JsonRpcError::INVALID_PARAMS, format!("unknown session {}", session_id))
        })?;

        let invocation = ToolInvocation {
            session_id,
            tool_call_id,
            tool_name: tool_name.clone(),
            arguments,
        };

        let result = session.execute_tool(&tool_name, invocation).await;

        Ok(json!({ "result": result }))
    }

    /// Handle a permission.request.
    async fn handle_permission_request(
        sessions: &Arc<RwLock<HashMap<String, Arc<Session>>>>,
        params: Value,
    ) -> std::result::Result<Value, JsonRpcError> {
        let session_id = params
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| JsonRpcError::new(JsonRpcError::INVALID_PARAMS, "missing sessionId"))?
            .to_string();

        let permission_request = params
            .get("permissionRequest")
            .cloned()
            .unwrap_or(Value::Null);

        let session = {
            let guard = sessions.read().await;
            guard.get(&session_id).cloned()
        };

        let session = session.ok_or_else(|| {
            JsonRpcError::new(JsonRpcError::INVALID_PARAMS, format!("unknown session {}", session_id))
        })?;

        let result = session.handle_permission_request(permission_request).await;

        Ok(json!({ "result": result }))
    }

    /// Verify protocol version compatibility.
    async fn verify_protocol_version(&self) -> Result<()> {
        let expected = get_sdk_protocol_version();
        let ping_result = self.ping("").await?;

        match ping_result.protocol_version {
            Some(actual) if actual == expected => Ok(()),
            Some(actual) => Err(CopilotError::ProtocolMismatch { expected, actual }),
            None => Err(CopilotError::ProtocolMismatch {
                expected,
                actual: 0,
            }),
        }
    }
}

/// Parse a CLI URL into host and port.
fn parse_cli_url(url: &str) -> (String, u16) {
    // Remove protocol if present
    let clean_url = url
        .strip_prefix("https://")
        .or_else(|| url.strip_prefix("http://"))
        .unwrap_or(url);

    // Check if it's just a port number
    if let Ok(port) = clean_url.parse::<u16>() {
        return ("localhost".to_string(), port);
    }

    // Parse host:port format
    if let Some((host, port_str)) = clean_url.split_once(':') {
        let host = if host.is_empty() { "localhost" } else { host };
        if let Ok(port) = port_str.parse::<u16>() {
            return (host.to_string(), port);
        }
    }

    panic!("Invalid CLIUrl format: {}. Expected 'host:port', 'http://host:port', or 'port'", url);
}
