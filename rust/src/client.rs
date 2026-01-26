use crate::jsonrpc::JsonRpcClient;
use crate::session::Session;
use crate::types::*;
use anyhow::{Context, Result, anyhow};
use serde_json::json;
use std::collections::HashMap;
use std::io::{BufRead, BufReader, Write};
use std::process::{Child, Command, Stdio};
use std::sync::{Arc, Mutex as StdMutex};

// Type alias for the boxed RPC client type to reduce complexity
type BoxedRpcClient = Arc<JsonRpcClient<Box<dyn Write + Send>, Box<dyn BufRead + Send>>>;

/// Main client for interacting with the Copilot CLI
pub struct Client {
    options: ClientOptions,
    rpc_client: Option<BoxedRpcClient>,
    cli_process: Option<Child>,
    state: Arc<StdMutex<ConnectionState>>,
    sessions: Arc<StdMutex<HashMap<String, Arc<Session>>>>,
}

impl Client {
    /// Create a new client with the given options
    pub fn new(options: Option<ClientOptions>) -> Self {
        let options = options.unwrap_or_default();

        Self {
            options,
            rpc_client: None,
            cli_process: None,
            state: Arc::new(StdMutex::new(ConnectionState::Disconnected)),
            sessions: Arc::new(StdMutex::new(HashMap::new())),
        }
    }

    /// Start the client and connect to the CLI server
    pub fn start(&mut self) -> Result<()> {
        {
            let mut state = self.state.lock().unwrap();
            if *state != ConnectionState::Disconnected {
                return Err(anyhow!("Client is already started or connecting"));
            }
            *state = ConnectionState::Connecting;
        }

        // Start CLI server if needed
        if let Some(cli_url) = self.options.cli_url.clone() {
            // Connect to external server
            self.connect_to_external_server(&cli_url)?;
        } else if self.options.use_stdio {
            // Start CLI server with stdio
            self.start_cli_server()?;
        } else {
            // Start CLI server with TCP (not yet implemented)
            return Err(anyhow!("TCP transport not yet implemented"));
        }

        // Set state to connected
        {
            let mut state = self.state.lock().unwrap();
            *state = ConnectionState::Connected;
        }

        Ok(())
    }

    /// Stop the client and cleanup
    pub fn stop(&mut self) -> Vec<anyhow::Error> {
        let mut errors = Vec::new();

        // Stop RPC client
        if let Some(ref rpc_client) = self.rpc_client {
            rpc_client.stop();
        }

        // Stop CLI process
        if let Some(ref mut process) = self.cli_process {
            if let Err(e) = process.kill() {
                errors.push(anyhow!("Failed to kill CLI process: {}", e));
            }
            if let Err(e) = process.wait() {
                errors.push(anyhow!("Failed to wait for CLI process: {}", e));
            }
        }

        self.rpc_client = None;
        self.cli_process = None;

        let mut state = self.state.lock().unwrap();
        *state = ConnectionState::Disconnected;

        errors
    }

    /// Forcefully stop the client
    pub fn force_stop(&mut self) {
        let _ = self.stop();
    }

    /// Get the current connection state
    pub fn get_state(&self) -> ConnectionState {
        *self.state.lock().unwrap()
    }

    /// Send a ping request
    pub async fn ping(&self, message: &str) -> Result<PingResponse> {
        let rpc_client = self
            .rpc_client
            .as_ref()
            .ok_or_else(|| anyhow!("Client not started"))?;

        let result = rpc_client
            .request("ping", json!({ "message": message }))
            .await
            .map_err(|e| anyhow!("Ping failed: {}", e))?;

        serde_json::from_value(result).context("Failed to parse ping response")
    }

    /// Get server status
    pub async fn get_status(&self) -> Result<GetStatusResponse> {
        let rpc_client = self
            .rpc_client
            .as_ref()
            .ok_or_else(|| anyhow!("Client not started"))?;

        let result = rpc_client
            .request("status.get", json!({}))
            .await
            .map_err(|e| anyhow!("Get status failed: {}", e))?;

        serde_json::from_value(result).context("Failed to parse status response")
    }

    /// Get authentication status
    pub async fn get_auth_status(&self) -> Result<GetAuthStatusResponse> {
        let rpc_client = self
            .rpc_client
            .as_ref()
            .ok_or_else(|| anyhow!("Client not started"))?;

        let result = rpc_client
            .request("auth.getStatus", json!({}))
            .await
            .map_err(|e| anyhow!("Get auth status failed: {}", e))?;

        serde_json::from_value(result).context("Failed to parse auth status response")
    }

    /// List available models
    pub async fn list_models(&self) -> Result<Vec<ModelInfo>> {
        let rpc_client = self
            .rpc_client
            .as_ref()
            .ok_or_else(|| anyhow!("Client not started"))?;

        let result = rpc_client
            .request("models.list", json!({}))
            .await
            .map_err(|e| anyhow!("List models failed: {}", e))?;

        let response: GetModelsResponse =
            serde_json::from_value(result).context("Failed to parse models response")?;

        Ok(response.models)
    }

    /// Create a new session
    pub async fn create_session(&self, config: Option<SessionConfig>) -> Result<Arc<Session>> {
        let rpc_client = self
            .rpc_client
            .as_ref()
            .ok_or_else(|| anyhow!("Client not started"))?;

        let config = config.unwrap_or_default();

        // Build session create params
        let mut params = json!({});
        if let Some(ref session_id) = config.session_id {
            params["sessionId"] = json!(session_id);
        }
        if let Some(ref model) = config.model {
            params["model"] = json!(model);
        }
        if let Some(ref system_message) = config.system_message {
            params["systemMessage"] = serde_json::to_value(system_message)?;
        }
        if config.streaming {
            params["streaming"] = json!(true);
        }

        let result = rpc_client
            .request("session.create", params)
            .await
            .map_err(|e| anyhow!("Create session failed: {}", e))?;

        let response: SessionCreateResponse =
            serde_json::from_value(result).context("Failed to parse session create response")?;

        let session = Arc::new(Session::new(
            response.session_id.clone(),
            Arc::clone(rpc_client),
            response.workspace_path,
        ));

        // Store session
        let mut sessions = self.sessions.lock().unwrap();
        sessions.insert(response.session_id.clone(), Arc::clone(&session));

        Ok(session)
    }

    /// Resume an existing session
    pub async fn resume_session(&self, session_id: &str) -> Result<Arc<Session>> {
        self.resume_session_with_options(session_id, None).await
    }

    /// Resume a session with configuration options
    pub async fn resume_session_with_options(
        &self,
        session_id: &str,
        config: Option<ResumeSessionConfig>,
    ) -> Result<Arc<Session>> {
        let rpc_client = self
            .rpc_client
            .as_ref()
            .ok_or_else(|| anyhow!("Client not started"))?;

        let mut params = json!({
            "sessionId": session_id
        });

        if let Some(config) = config
            && config.streaming
        {
            params["streaming"] = json!(true);
        }

        let result = rpc_client
            .request("session.resume", params)
            .await
            .map_err(|e| anyhow!("Resume session failed: {}", e))?;

        let response: SessionCreateResponse =
            serde_json::from_value(result).context("Failed to parse session resume response")?;

        let session = Arc::new(Session::new(
            response.session_id.clone(),
            Arc::clone(rpc_client),
            response.workspace_path,
        ));

        // Store session
        let mut sessions = self.sessions.lock().unwrap();
        sessions.insert(response.session_id.clone(), Arc::clone(&session));

        Ok(session)
    }

    // ========================================================================
    // Private helper methods
    // ========================================================================

    fn start_cli_server(&mut self) -> Result<()> {
        let cli_path = self.options.cli_path.as_deref().unwrap_or("copilot");

        let mut cmd = Command::new(cli_path);
        cmd.arg("agent")
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::inherit());

        // Set working directory
        if let Some(ref cwd) = self.options.cwd {
            cmd.current_dir(cwd);
        }

        // Set log level
        if let Some(ref log_level) = self.options.log_level {
            cmd.arg("--log-level").arg(log_level);
        }

        // Set environment variables
        if let Some(ref env_vars) = self.options.env {
            for (key, value) in env_vars {
                cmd.env(key, value);
            }
        }

        // Spawn the process
        let mut child = cmd.spawn().context("Failed to spawn CLI process")?;

        // Get stdin and stdout handles
        let stdin = child
            .stdin
            .take()
            .ok_or_else(|| anyhow!("Failed to get stdin"))?;
        let stdout = child
            .stdout
            .take()
            .ok_or_else(|| anyhow!("Failed to get stdout"))?;

        // Create RPC client with boxed writers/readers
        let boxed_writer: Box<dyn Write + Send> = Box::new(stdin);
        let boxed_reader: Box<dyn BufRead + Send> = Box::new(BufReader::new(stdout));
        let rpc_client = JsonRpcClient::new(boxed_writer, boxed_reader);

        // Set up notification handler for session events
        let sessions_for_handler = Arc::clone(&self.sessions);
        rpc_client.set_notification_handler(Arc::new(move |method, params| {
            if method == "session.event" {
                // Extract sessionId and event from params
                if let Some(session_id) = params.get("sessionId").and_then(|v| v.as_str())
                    && let Some(event_value) = params.get("event")
                    && let Ok(event) = serde_json::from_value::<SessionEvent>(event_value.clone())
                {
                    // Dispatch to the session
                    let sessions = sessions_for_handler.lock().unwrap();
                    if let Some(session) = sessions.get(session_id) {
                        session.dispatch_event(event);
                    }
                }
            }
        }));

        // Start the RPC client
        rpc_client.start();

        self.rpc_client = Some(Arc::new(rpc_client));
        self.cli_process = Some(child);

        Ok(())
    }

    fn connect_to_external_server(&mut self, _url: &str) -> Result<()> {
        // TODO: Implement TCP connection to external server
        Err(anyhow!("External server connection not yet implemented"))
    }
}

impl Drop for Client {
    fn drop(&mut self) {
        let _ = self.stop();
    }
}
